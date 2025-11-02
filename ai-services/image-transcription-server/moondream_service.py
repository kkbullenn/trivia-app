from flask import Flask, request, jsonify
from transformers import AutoModelForCausalLM
from PIL import Image
import torch
import io  
import cv2  # Added for video frame extraction

# Initialize
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

# API endpoint for image captioning (POST requests)
@app.route('/caption', methods=['POST'])
def caption_image():
    # Check for image in request
    if 'image' not in request.files:
        return jsonify({"error": "No image file provided"}), 400
    
    # Check if there is a file
    file = request.files['image']
    if file.filename == '':
        return jupytext({"error": "No selected file"}), 400
    
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
        
        # Return caption as JSON response
        return jsonify({"caption": short_result})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# New API endpoint for video summarization (POST requests)
@app.route('/summarize_video', methods=['POST'])
def summarize_video():
    # Check for video in request
    if 'video' not in request.files:
        return jsonify({"error": "No video file provided"}), 400
    
    # Check if there is a file
    file = request.files['video']
    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400
    
    try:
        # Save the uploaded video temporarily 
        video_path = 'temp_video.mp4'
        file.save(video_path)
        
        # Extract frames and generate captions
        cap = cv2.VideoCapture(video_path)
        frame_count = 0
        captions = []
        frame_interval = 30  # Extract every 30th frame
        
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break
            if frame_count % frame_interval == 0:
                frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                image = Image.fromarray(frame_rgb)
                
                # Generate caption
                result = model.caption(image, length="short", settings=settings)
                captions.append(result)
            
            frame_count += 1
        
        cap.release()
        
        # Clean up temporary file
        import os
        os.remove(video_path)
        
        # Simple summary: Join captions (you can enhance this with better aggregation)
        full_summary = "Video summary: " + " -> ".join(captions)
        
        # Return summary as JSON response
        return jsonify({"summary": full_summary})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, port=8082)