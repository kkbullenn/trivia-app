# M2M100 Translation API

A FastAPI-based microservice using Facebook’s **M2M100 (418M)** multilingual model to translate English text into other languages.  
Supports ISO 639-1 language codes such as `en` (english), `es` (spanish), `tl` (tagalog), `ja` (japanese), `ko` (korean), `pt` (portuguese), `zh` (chinese), `fr` (french), `pa` (punjabi), and more.

---

## Setup

### Requirements
- **Python 3.9+**
- Internet connection (the model downloads automatically on first run)

### Install & Run

M2M100 will not work without these dependencies.

```bash
cd ai-services/translation-server   # go to location
python -m venv venv                 # create virtual environment
source venv/bin/activate            # or venv\Scripts\activate on Windows
pip install fastapi uvicorn transformers torch sentencepiece python-multipart # install dependencies
python m2m100_service.py            # run the server locally