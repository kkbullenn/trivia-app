// translation.js
// Handles quiz question/answer translation via M2M100 backend.
//
// Uses:
//   - /quiz/translated?target=<lang>&categoryId=<id>
//   - hidden #questionId input from quiz.html
//   - global toast: window.showPointingToast (from toast.js)
//
// Does three things:
//   1) Start with language icon: first click swaps to dropdown + auto TL
//   2) Shows "Translating…" using toast + statusBanner
//   3) Disables Prev/Next + dropdown while translation is loading

import { getContextPath } from "./helpers.js";

// language options for dropdown
const LANGUAGE_OPTIONS = [
  { value: "en", label: "EN" },
  { value: "tl", label: "TL" },
  { value: "es", label: "ES" },
  { value: "zh", label: "ZH" },
  { value: "ko", label: "KO" },
  { value: "fr", label: "FR" },
];

let currentLang = "en";

// Cache: `${lang}::${categoryId}` -> Map(questionId -> translatedQuestionObj)
const translationCache = new Map();

let languageSelectEl = null;
let languageButtonRef = null;

function cacheKey(lang, categoryId) {
  return `${lang}::${categoryId || ""}`;
}

/**
 * Helper to show/hide the existing status banner
 */
function setStatus(message) {
  const banner = document.getElementById("statusBanner");
  if (!banner) return;
  if (!message) {
    banner.classList.add("hidden");
    banner.textContent = "";
  } else {
    banner.classList.remove("hidden");
    banner.textContent = message;
  }
}

/**
 * 
 * Helper to disable navigation + dropdown while translating
 */
function setLoading(isLoading) {
  const disabled = !!isLoading;

  if (languageSelectEl) {
    languageSelectEl.disabled = disabled;
  }

  const prev = document.getElementById("prevButton");
  const next = document.getElementById("nextButton");

  if (prev) prev.disabled = disabled;
  if (next) next.disabled = disabled;
}

/**
 * Tries multiple places to find category id on the page.
 */
function findCategoryIdFromDom() {
  const byId = document.getElementById("categoryId");
  if (byId && byId.value) return String(byId.value);

  const byClass = document.querySelector(".category-id");
  if (byClass && (byClass.value || byClass.dataset?.value)) {
    return String(byClass.value || byClass.dataset.value);
  }

  const dataAttrEl =
    document.querySelector("[data-category-id]") ||
    document.querySelector("[data-category]");
  if (dataAttrEl) {
    return String(
      dataAttrEl.dataset?.categoryId ||
        dataAttrEl.dataset?.category ||
        dataAttrEl.getAttribute("data-category-id") ||
        ""
    );
  }

  if (document.body?.dataset?.categoryId) {
    return String(document.body.dataset.categoryId);
  }

  return "";
}

/**
 * Ensure translations for a given language are loaded into cache.
 */
async function ensureTranslationsLoaded(lang) {
  console.log("[translation.js] ensureTranslationsLoaded:", lang);

  const contextPath = getContextPath();
  let categoryId = findCategoryIdFromDom();
  let key = cacheKey(lang, categoryId);

  if (translationCache.has(key)) {
    console.log(
      "[translation.js] translations already cached for",
      key
    );
    return;
  }

  // If we don't know categoryId yet, ask /quiz/data (same as quiz.html)
  if (!categoryId) {
    console.log(
      "[translation.js] no categoryId in DOM, fetching /quiz/data"
    );
    try {
      const dataRes = await fetch(`${contextPath}/quiz/data`, {
        method: "GET",
        credentials: "same-origin",
        headers: {
          Accept: "application/json",
          "AJAX-Requested-With": "fetch",
        },
      });
      if (dataRes.ok) {
        const qObj = await dataRes.json();
        if (qObj && (qObj.category_id != null || qObj.category_id === 0)) {
          categoryId = String(qObj.category_id);
          console.log(
            "[translation.js] got categoryId from /quiz/data:",
            categoryId
          );

          // create hidden categoryId for future lookups
          let catEl = document.getElementById("categoryId");
          if (!catEl) {
            catEl = document.createElement("input");
            catEl.type = "hidden";
            catEl.id = "categoryId";
            catEl.name = "categoryId";
            document.body.appendChild(catEl);
          }
          catEl.value = categoryId;

          // also populate questionId if available
          if (qObj.question_id != null) {
            const qidEl = document.getElementById("questionId");
            if (qidEl) qidEl.value = String(qObj.question_id);
          }
        }
      }
    } catch (e) {
      console.warn(
        "[translation.js] failed to fetch /quiz/data for categoryId",
        e
      );
    }

    key = cacheKey(lang, categoryId);
    if (translationCache.has(key)) {
      // maybe another call already filled it
      return;
    }
  }

  let url = `${contextPath}/quiz/translated?target=${encodeURIComponent(
    lang
  )}`;
  if (categoryId) {
    url += `&categoryId=${encodeURIComponent(categoryId)}`;
  }

  console.log(
    "[translation.js] fetching translations from",
    url
  );

  try {
    const res = await fetch(url, {
      method: "GET",
      credentials: "same-origin",
      headers: {
        Accept: "application/json",
        "AJAX-Requested-With": "fetch",
      },
    });

    if (!res.ok) {
      console.error(
        "[translation.js] failed to fetch translations",
        res.status,
        await res.text()
      );
      translationCache.set(key, new Map());
      return;
    }

    const arr = await res.json();
    const byId = new Map();
    if (Array.isArray(arr)) {
      arr.forEach((q) => {
        if (q && (q.question_id != null || q.question_id === 0)) {
          byId.set(String(q.question_id), q);
        }
      });
    }

    console.log(
      "[translation.js] stored translations for",
      key,
      "with",
      byId.size,
      "questions"
    );
    translationCache.set(key, byId);
  } catch (err) {
    console.error("[translation.js] error fetching translations", err);
    translationCache.set(key, new Map());
  }
}

/**
 * Apply translation to the *current* question shown in quiz.html
 * using hidden #questionId and #answersContainer.
 */
function applyTranslationForCurrentQuestion() {
  console.log(
    "[translation.js] applyTranslationForCurrentQuestion called, currentLang =",
    currentLang
  );

  const questionIdInput = document.getElementById("questionId");
  const questionTextEl = document.getElementById("questionText");
  const answersContainer = document.getElementById("answersContainer");
  const categoryEl = document.getElementById("categoryId");

  if (!questionIdInput || !questionTextEl || !answersContainer) {
    console.log(
      "[translation.js] missing DOM elements, skipping applyTranslation"
    );
    return;
  }

  const qId = String(questionIdInput.value || "");
  if (!qId) {
    console.log(
      "[translation.js] no questionId yet, skipping applyTranslation"
    );
    return;
  }

  const categoryId =
    categoryEl && categoryEl.value ? String(categoryEl.value) : "";
  const key = cacheKey(currentLang, categoryId);
  const byId = translationCache.get(key);

  if (!byId) {
    console.log(
      "[translation.js] no translation cache for key",
      key
    );
    return;
  }

  const translated = byId.get(qId);
  if (!translated) {
    console.log(
      "[translation.js] no translated entry for questionId",
      qId
    );
    return;
  }

  console.log(
    "[translation.js] applying translation for questionId",
    qId,
    translated
  );

  if (translated.text) {
    questionTextEl.textContent = translated.text;
  }

  const opts = Array.isArray(translated.options)
    ? translated.options
    : Array.isArray(translated.opts)
    ? translated.opts
    : [];

  if (opts.length) {
    const buttons = answersContainer.querySelectorAll("button[data-key]");
    buttons.forEach((btn) => {
      const key = btn.dataset.key;
      const opt = opts.find((o) => String(o.key) === String(key));
      if (opt) {
        const spans = btn.querySelectorAll("span");
        if (spans.length >= 2) {
          spans[1].textContent = opt.text;
        } else {
          btn.textContent = opt.text;
        }
      }
    });
  }
}

/**
 * Main entry for switching languages
 * - If lang === "en": reload page to restore original English.
 * - Otherwise: show toast + banner, disable controls, fetch translations, apply.
 */
async function runTranslation(lang, targetEl) {
  currentLang = lang;

  if (lang === "en") {
    // go back to original English content
    window.location.reload();
    return;
  }

  const friendly = lang.toUpperCase();
  const toastTarget =
    targetEl || languageSelectEl || languageButtonRef || document.body;

  console.log("[translation.js] runTranslation start:", lang);

  if (window.showPointingToast && toastTarget) {
    window.showPointingToast(
      toastTarget,
      `Translating to ${friendly}…`,
      2500,
      "middle"
    );
  }

  setLoading(true);
  setStatus(`Translating to ${friendly}…`);

  try {
    await ensureTranslationsLoaded(lang);
    applyTranslationForCurrentQuestion();

    if (window.showPointingToast && toastTarget) {
      window.showPointingToast(
        toastTarget,
        `Now in ${friendly}`,
        2000,
        "middle"
      );
    }
  } finally {
    setLoading(false);
    setStatus("");
    console.log("[translation.js] runTranslation done:", lang);
  }
}

/**
 * Initial setup:
 *  - keep languageButton icon
 *  - on first click, hide icon and show <select> dropdown in its place
 *  - auto-translate to Tagalog once when dropdown appears
 */
async function init() {
  console.log("[translation.js] init called");

  const languageBtn = document.getElementById("languageButton");
  const questionIdInput = document.getElementById("questionId");
  const answersContainer = document.getElementById("answersContainer");
  const questionText = document.getElementById("questionText");

  if (!languageBtn || !questionIdInput || !answersContainer || !questionText) {
    console.log(
      "[translation.js] required elements missing; exiting init"
    );
    return;
  }

  languageButtonRef = languageBtn;
  console.log(
    "[translation.js] languageButton found, setting up button->dropdown"
  );

  // Build a small <select> that will replace the icon on first click
  const select = document.createElement("select");
  select.id = "languageSelect";
  select.style.display = "none";
  select.style.backgroundColor = "#0b2460ff";
  select.style.border = "1px solid rgba(250, 204, 21, 0.7)";
  select.style.color = "#FDE68A";
  select.style.borderRadius = "9999px";
  select.style.padding = "4px 12px";
  select.style.fontSize = "0.875rem";
  select.style.marginLeft = "4px";
  select.style.cursor = "pointer";

  LANGUAGE_OPTIONS.forEach((opt) => {
    const o = document.createElement("option");
    o.value = opt.value;
    o.textContent = opt.label;
    select.appendChild(o);
  });

  languageSelectEl = select;

  // Insert dropdown right after the button
  languageBtn.parentNode.insertBefore(select, languageBtn.nextSibling);

  // First click: swap button -> dropdown, auto TL
  languageBtn.addEventListener("click", async () => {
    console.log(
      "[translation.js] languageButton clicked, currentLang =",
      currentLang
    );

    if (select.style.display === "none") {
      languageBtn.style.display = "none";
      select.style.display = "inline-block";

      select.value = "tl";
      console.log(
        "[translation.js] swapping button->dropdown, auto TL"
      );

      await runTranslation("tl", languageBtn);
    } else {
      select.focus();
    }
  });

  // On dropdown change: run translation
  select.addEventListener("change", async (e) => {
    const lang = e.target.value;
    console.log("[translation.js] languageSelect changed to", lang);
    await runTranslation(lang, select);
  });

  // Observe answersContainer for when quiz loads a new question
  try {
    if (answersContainer && window.MutationObserver) {
      const mo = new MutationObserver(() => {
        // Small delay so questionId + buttons are ready
        setTimeout(() => {
          if (currentLang && currentLang !== "en") {
            applyTranslationForCurrentQuestion();
          }
        }, 10);
      });
      mo.observe(answersContainer, { childList: true, subtree: false });
    }
  } catch (e) {
    console.warn("[translation.js] MutationObserver setup failed", e);
  }

  // Light prefetch of Tagalog in background so it's ready sooner
  // (does not block quiz)
  ensureTranslationsLoaded("tl").catch((e) =>
    console.warn("[translation.js] prefetch TL failed", e)
  );
}

// expose a tiny API in case quiz.html ever wants to prefetch/apply manually
window.translation = {
  ensureTranslationsLoaded,
  applyTranslationForCurrentQuestion,
};

// run immediately
init();
