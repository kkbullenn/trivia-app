from fastapi import FastAPI, Form, HTTPException
from transformers import M2M100ForConditionalGeneration, M2M100Tokenizer
import uvicorn

# Model setup
MODEL_NAME = "facebook/m2m100_418M"

tok = M2M100Tokenizer.from_pretrained(MODEL_NAME)
mdl = M2M100ForConditionalGeneration.from_pretrained(MODEL_NAME)

# supported ISO language codes
SUPPORTED = {
    "en",  # English
    "es",  # Spanish
    "pt",  # Portuguese
    "zh",  # Chinese
    "ja",  # Japanese
    "tl",  # Tagalog
    "ko",  # Korean
    "fr",  # French
    "pa"   # Punjabi
}

# FastAPI app
app = FastAPI(title=f"M2M100 Translator ({MODEL_NAME})")

@app.get("/health")
def health():
    return {"ok": True, "supported": sorted(SUPPORTED)}

@app.post("/translate")
def translate(
    text: str = Form(...),
    source: str = Form("en"),
    target: str = Form(...)
):
    source, target = source.lower().strip(), target.lower().strip()
    if source not in SUPPORTED or target not in SUPPORTED:
        raise HTTPException(400, f"Use languages in {sorted(SUPPORTED)}")

    tok.src_lang = source
    forced = tok.get_lang_id(target)
    inputs = tok(text, return_tensors="pt")
    outputs = mdl.generate(**inputs, forced_bos_token_id=forced, max_length=512)
    out = tok.batch_decode(outputs, skip_special_tokens=True)[0]
    return {"translation": out}

# 👇 Add this part to run with host/port
if __name__ == "__main__":
    HOST = "0.0.0.0"   # or "127.0.0.1" if you want local-only access
    PORT = 8893
    uvicorn.run(app, host=HOST, port=PORT)
