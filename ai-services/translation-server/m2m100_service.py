from fastapi import FastAPI, Form
from transformers import M2M100ForConditionalGeneration, M2M100Tokenizer
import torch
import uvicorn

app = FastAPI(title="M2M100 Translator API", version="1.0")

# -------------------------------------------------------------
# Load model once at startup
# -------------------------------------------------------------
MODEL_NAME = "facebook/m2m100_418M"

print(f"🔄 Loading model {MODEL_NAME} (this may take a minute the first time)...")
tokenizer = M2M100Tokenizer.from_pretrained(MODEL_NAME)
model = M2M100ForConditionalGeneration.from_pretrained(MODEL_NAME)
model.to("cpu")
print("✅ Model loaded and ready.")

# -------------------------------------------------------------
# Endpoint: POST /translate
# -------------------------------------------------------------
@app.post("/translate")
def translate(
    text: str = Form(...),
    target: str = Form(...)
):
    """
    Translate English text into the target language.
    Target should be an ISO 639-1 code (e.g., 'fr', 'es', 'tl', 'ja').
    """
    try:
        # Set the tokenizer language (source English, target user-chosen)
        tokenizer.src_lang = "en"
        tokenizer.tgt_lang = target

        inputs = tokenizer(text, return_tensors="pt")
        with torch.no_grad():
            generated_tokens = model.generate(
                **inputs,
                forced_bos_token_id=tokenizer.get_lang_id(target),
                max_length=512
            )
        translation = tokenizer.decode(generated_tokens[0], skip_special_tokens=True)
        return {"translation": translation}
    except Exception as e:
        return {"error": str(e)}

# -------------------------------------------------------------
# Health check
# -------------------------------------------------------------
@app.get("/")
def root():
    return {"message": "M2M100 Translator is running."}

# -------------------------------------------------------------
# Run server with fixed host/port
# -------------------------------------------------------------
if __name__ == "__main__":
    HOST = "0.0.0.0"
    PORT = 8892
    print(f"🚀 Starting M2M100 Translator on http://{HOST}:{PORT}")
    uvicorn.run("m2m_service:app", host=HOST, port=PORT, reload=False)
