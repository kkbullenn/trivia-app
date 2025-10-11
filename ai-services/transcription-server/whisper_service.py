#/
# Whisper AI Transcriber API via POST @ /transcribe
#
# This transcriber can only transcribe audio into English text.
#
# Expected multipart form data (body):
# - file: audio file (any format supported by FFmpeg)
# - source_lang (optional): source language code (e.g., 'fr', 'es', 'hi', 'auto')
#
# Whisper will automatically detect the language by default or if 'auto' is specified.
#
# Response JSON:
# - detected_language: language code detected by Whisper (e.g., 'fr', 'es', 'hi')
# - duration_sec: duration of the audio in seconds
# - translated_text: transcribed text in English
#/

from fastapi import FastAPI, UploadFile, File, Form
from faster_whisper import WhisperModel
import tempfile
import os
import ffmpeg
import uvicorn

# server constants
PORT = 8888
HOST = "localhost"

# model constants
MODEL_SIZE = "base"
DEFAULT_FILE_FORMAT = "wav"

app = FastAPI(title="Whisper AI Transcriber")
model = WhisperModel(MODEL_SIZE, device="cpu", compute_type="int8")

@app.post("/transcribe")
async def transcribe_audio(
    # expected multipart form data
    file: UploadFile = File(...),
    source_lang: str = Form("auto")
):
    """
    Transcribe an uploaded audio file into English text.
    - file: audio file (any format supported by FFmpeg)
    - source_lang: source language code (e.g., 'fr', 'es', 'hi', 'auto')
    """

    # Save uploaded audio temporarily
    with tempfile.NamedTemporaryFile(delete=False, suffix=os.path.splitext(file.filename)[1]) as tmp:
        tmp.write(await file.read())
        input_path = tmp.name

    # Convert to a uniform WAV format for whisper (16kHz mono)
    wav_path = input_path + "." + DEFAULT_FILE_FORMAT
    try:
        (
            ffmpeg
            .input(input_path)
            .output(wav_path, format=DEFAULT_FILE_FORMAT, ac=1, ar="16k")
            .overwrite_output()
            .run(quiet=True)
        )
    except Exception as e:
        os.remove(input_path)
        return {"error": f"Audio conversion failed: {str(e)}"}

    try:
        # Transcribe to English regardless of input language
        segments, info = model.transcribe(
            wav_path,
            language=None if source_lang == "auto" else source_lang,
            task="translate"  # Always translate to English
        )

        text = "".join([seg.text.strip() for seg in segments])
                
        # Response return
        return {
            "detected_language": info.language,
            "duration_sec": round(info.duration, 2),
            "translated_text": text or "(no transcription detected)"
        }

    except Exception as e:
        return {"error": f"Transcription failed: {str(e)}"}

    finally:
        # Clean up
        if os.path.exists(input_path): os.remove(input_path)
        if os.path.exists(wav_path): os.remove(wav_path)

if __name__ == "__main__":
    # Run the service
    uvicorn.run(app, host=HOST, port=PORT)
