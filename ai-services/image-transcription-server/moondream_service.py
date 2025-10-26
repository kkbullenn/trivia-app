from flask import Flask, request, jsonify
from transformers import AutoModelForCausalLM
from PIL import Image
import torch
import io  

#intitialize
app = Flask(__name__)

# Load model 
model = AutoModelForCausalLM.from_pretrained(
    "vikhyatk/moondream2",
    trust_remote_code=True,
    dtype=torch.bfloat16,
    device_map="auto"  
)


# Settings for caption generation
settings = {"temperature": 0.5, "max_tokens": 768, "top_p": 0.3}


#API endpoint for POST requests
@app.route('/caption', methods=['POST'])
def caption_image():
    # Checks for image in request
    if 'image' not in request.files:
        return jsonify({"error": "No image file provided"}), 400
    
    # Check if there is file
    file = request.files['image']
    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400
    
    try:
        # Read the image from the uploaded file
        image_bytes = file.read()
        image = Image.open(io.BytesIO(image_bytes))
        
        # Generate the short caption
        short_result = model.caption(
            image, 
            length="short", 
            settings=settings
        )
        
        # return caption as JSON response
        return jsonify({"caption": short_result})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, port=8082)