/* ---------------------------
   Inject toast CSS globally
----------------------------- */
const toastCSS = `
.toast {
  position: absolute;
  padding: 10px 14px;
  background: #333;
  color: white;
  border-radius: 8px;
  font-size: 14px;
  white-space: nowrap;
  z-index: 99999;
  box-shadow: 0 4px 12px rgba(0,0,0,0.25);
}
.toast-arrow {
  position: absolute;
  bottom: -7px; /* toast sits ABOVE the target */
  width: 0;
  height: 0;
  border-left: 7px solid transparent;
  border-right: 7px solid transparent;
  border-top: 7px solid #333; /* arrow points downward */
}
`;

(function injectToastStyles() {
    const style = document.createElement("style");
    style.textContent = toastCSS;
    document.head.appendChild(style);
})();

/* ---------------------------
   Toast template
----------------------------- */
const templateHTML = `
  <div class="toast">
    <div class="toast-arrow"></div>
    <span class="toast-text"></span>
  </div>
`;

function createToastElement(message) {
    const wrapper = document.createElement("div");
    wrapper.innerHTML = templateHTML.trim();
    const toast = wrapper.firstElementChild;
    toast.querySelector(".toast-text").textContent = message;
    return toast;
}

/* ---------------------------
   Public toast function
----------------------------- */

export function showPointingToast(
    target,
    message,
    ms = 3000,
    arrowPosition = "left"
) {
    const toast = createToastElement(message);
    const arrow = toast.querySelector(".toast-arrow");

    document.body.appendChild(toast);

    const targetRect = target.getBoundingClientRect();
    const toastRect = toast.getBoundingClientRect();

    /* --------------------------
       Position ABOVE the target
    -------------------------- */
    toast.style.left = targetRect.left + window.scrollX + "px";
    toast.style.top =
        targetRect.top + window.scrollY - toastRect.height - 12 + "px";

    /* --------------------------
       Dynamic arrow positioning
       ("left", "middle", "right")
    -------------------------- */
    switch (arrowPosition) {
        case "left":
            arrow.style.left = "14px"; // slightly inset from the left edge
            arrow.style.transform = "none";
            break;

        case "right":
            arrow.style.left = toastRect.width - 14 + "px"; // near right edge
            arrow.style.transform = "translateX(-100%)";
            break;

        default: // "middle"
            arrow.style.left = "50%";
            arrow.style.transform = "translateX(-50%)";
    }

    /* Auto remove */
    setTimeout(() => toast.remove(), ms);
}

window.showPointingToast = showPointingToast;
