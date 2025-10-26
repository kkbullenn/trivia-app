#/
# M2M100 Translation API
#
# Translate text or arrays of questions and answers from one language to another.
#
# Endpoints:
# - GET  /m2m100                               : health check
# - GET  /m2m100/languages                     : list language codes for FE dropdowns
# - POST /m2m100/translate                     : translate single text (form-data)
# - POST /m2m100/translate/questions-answers   : translate arrays of questions and answers (JSON)
#
# POST /m2m100/translate (form-data):
#   - text         : string (required)
#   - target       : ISO 639-1 code (e.g., 'es', 'fr') (required)
#   - source       : ISO 639-1 code, default 'en' (optional)
#
# POST /m2m100/translate/questions-answers (JSON):
# {
#   "questions": ["..."],  // optional
#   "answers":   ["..."],  // optional
#   "source": "en",
#   "target": "tl"
# }
#
# Response:
# {
#   "questions_translated": ["..."],
#   "answers_translated":   ["..."],
#   "source": "en",
#   "target": "tl"
# }
#/

from fastapi import FastAPI, Form, HTTPException
from pydantic import BaseModel, Field
from typing import List
from transformers import M2M100ForConditionalGeneration, M2M100Tokenizer
import torch
import uvicorn

# ---------------- server constants ----------------
HOST = "localhost"
PORT = 8892

# ---------------- model setup ----------------
MODEL_NAME = "facebook/m2m100_418M"
print(f"Loading model {MODEL_NAME} ...")

tokenizer = M2M100Tokenizer.from_pretrained(MODEL_NAME)
model = M2M100ForConditionalGeneration.from_pretrained(MODEL_NAME)
model.to("cpu")

print("Model loaded and ready.")

# ---------------- limits & supported languages (temporary for testing) ----------------
# NOTE:
#   The M2M100 model actually supports 100+ languages, but we’re limiting the list
#   below just for testing. We can:
#    - Remove the SUPPORTED check for full open support.
#    - Or expand this list if the project later supports more languages.
#
#   To view all available language codes:
#       print(sorted(tokenizer.lang_code_to_id.keys()))
#
#   max_length below uses tokens (not characters)
#   For now, MAX_CHARS_PER_ITEM is used as a simple guard.
#   If longer translations are needed, add a MAX_TOKENS_PER_ITEM later.

MAX_ITEMS = 200
MAX_CHARS_PER_ITEM = 2000
SUPPORTED = {
    "en",  # English
    "es",  # Spanish
    "fr",  # French
    "pt",  # Portuguese
    "zh",  # Chinese
    "ja",  # Japanese
    "ko",  # Korean
    "tl",  # Tagalog
    "pa",  # Punjabi
    "hi",  # Hindi
    "de"   # German
}

# ---------------- app ----------------
app = FastAPI(title="M2M100 Translation API")

# ---------------- health endpoint ----------------
@app.get("/m2m100")
def health():
    return {
        "message": "M2M100 Translator is running.",
        "mode": "limited",  # set to "open" if SUPPORTED check is removed
        "supported_languages": sorted(SUPPORTED),
        "note": "Remove SUPPORTED check for full 100+ language coverage."
    }

# ---------------- languages endpoint (for FE dropdowns) ----------------
@app.get("/m2m100/languages")
def list_languages():
    """
    Returns language codes the FE can show in a dropdown.
    Default: curated SUPPORTED list for controlled testing.
    To expose all M2M100 languages later, uncomment the dynamic block.
    """
    # --- Curated (current behavior) ---
    langs = sorted(SUPPORTED)
    mode = "limited"

    # --- Open model (future) ---
    # langs = sorted(tokenizer.lang_code_to_id.keys())
    # mode = "open"

    return {
        "mode": mode,
        "count": len(langs),
        "languages": langs,
        "note": "Switch to open by using tokenizer.lang_code_to_id (see commented lines)."
    }

# ---------------- single text translation ----------------
@app.post("/m2m100/translate")
def translate_text(
    text: str = Form(...),
    target: str = Form(...),
    source: str = Form("en")
):
    src = source.lower().strip()
    tgt = target.lower().strip()

    try:
        if src not in SUPPORTED or tgt not in SUPPORTED:
            raise HTTPException(400, f"Supported languages: {sorted(SUPPORTED)}")

        if not text.strip():
            raise HTTPException(400, "`text` cannot be empty.")
        
        if len(text) > MAX_CHARS_PER_ITEM:
            raise HTTPException(413, f"`text` exceeds {MAX_CHARS_PER_ITEM} characters.")

        tokenizer.src_lang = src
        inputs = tokenizer(text, return_tensors="pt", truncation=True, max_length=MAX_CHARS_PER_ITEM)

        with torch.no_grad():
            outputs = model.generate(
                **inputs,
                forced_bos_token_id=tokenizer.get_lang_id(tgt),
                max_length=512
            )

        translated = tokenizer.decode(outputs[0], skip_special_tokens=True)
        return {"translation": translated, "source": src, "target": tgt}

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"Translation failed: {str(e)}")

# ---------------- models for questions-answers endpoint ----------------
class QARequest(BaseModel):
    questions: List[str] = Field(default_factory=list)
    answers:   List[str] = Field(default_factory=list)
    source: str = "en"
    target: str

class QAResponse(BaseModel):
    questions_translated: List[str]
    answers_translated:   List[str]

# ---------------- questions-answers translation endpoint ----------------
@app.post("/m2m100/translate/questions-answers")
def translate_questions_answers(req: QARequest):
    src = req.source.lower().strip()
    tgt = req.target.lower().strip()

    try:
        if src not in SUPPORTED or tgt not in SUPPORTED:
            raise HTTPException(400, f"Supported languages: {sorted(SUPPORTED)}")

        total_items = len(req.questions) + len(req.answers)
        if total_items == 0:
            raise HTTPException(400, "Provide at least one question or answer.")
        if total_items > MAX_ITEMS:
            raise HTTPException(413, f"Too many items (>{MAX_ITEMS}).")

        for s in (*req.questions, *req.answers):
            if len(s) > MAX_CHARS_PER_ITEM:
                raise HTTPException(413, f"An item exceeds {MAX_CHARS_PER_ITEM} characters.")

        combined = [*req.questions, *req.answers]
        tokenizer.src_lang = src
        forced_bos_token_id = tokenizer.get_lang_id(tgt)

        inputs = tokenizer(
            combined,
            return_tensors="pt",
            padding=True,
            truncation=True,
            max_length=MAX_CHARS_PER_ITEM
        )

        with torch.no_grad():
            outputs = model.generate(
                **inputs,
                forced_bos_token_id=forced_bos_token_id,
                max_length=512
            )

        decoded = tokenizer.batch_decode(outputs, skip_special_tokens=True)
        qn_count = len(req.questions)

        return {
            "questions_translated": decoded[:qn_count],
            "answers_translated": decoded[qn_count:],
            "source": src,
            "target": tgt
        }

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"Batch translation failed: {str(e)}")


# ---------------- run server ----------------
if __name__ == "__main__":
    print(f"Starting M2M100 Translator at http://{HOST}:{PORT}")
    uvicorn.run("m2m_service:app", host=HOST, port=PORT, reload=False)