# Moondream Caption API

A Flask-based microservice using vikhyatk/moondream2 to generate captions for images. Supports automatic image processing and common image formats supported by PIL (Pillow).

---

## Setup

### Requirements
- **Python 3.10+**
- Internet connection (the model downloads automatically on first run)

### Install & Run


```bash
cd ai-services/image-transcription-server # go to location
python -m venv venv # create env
source venv/bin/activate # or venv\Scripts\activate on Windows
pip install flask transformers pillow torch opencv-python moviepy # deps
python moondream_service.py # run the server locally
