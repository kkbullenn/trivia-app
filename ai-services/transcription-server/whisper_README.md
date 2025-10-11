# Whisper AI Transcriber API

A FastAPI-based microservice using **faster-whisper** to transcribe audio into **English** text. Supports automatic language detection and any audio format supported by **FFmpeg**.

---

## Setup

### Requirements
- Python 3.9+
- FFmpeg installed and available in PATH
    - Linux environments will probably have this (maybe MacOS too)
    - Windows users must install manually    

### Install & Run

Whisper will not work without these requirements and dependencies.

```bash
cd ai-services/trancription-server # go to location
python -m venv venv # create env
source venv/bin/activate # or venv\Scripts\activate on Windows
pip install fastapi uvicorn faster-whisper ffmpeg-python # deps
python whisper_service.py # run the server locally
