from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import requests

from app.prompt import SYSTEM_PROMPT
from app.tts import generate_speech_stream

app = FastAPI()

OLLAMA_URL = "http://localhost:11434/api/generate"

class PlayerTelemetry(BaseModel):
    event_type: str
    context_details: str

@app.get("/health")
def check_health():
    return {"status": "healthy"}

@app.post("/narrate")
def generate_narration(telemetry: PlayerTelemetry):
    user_message = f"Event: {telemetry.event_type}. Details: {telemetry.context_details}"
    
    payload = {
        "model": "qwen2.5:3b",
        "prompt": f"{SYSTEM_PROMPT}\n\nUser Event: {user_message}",
        "stream": False
    }
    
    try:
        response = requests.post(OLLAMA_URL, json=payload, timeout=10)
        response.raise_for_status()
        response_data = response.json()
        ai_text = response_data.get("response", "")
    except Exception:
        raise HTTPException(status_code=500, detail="Ollama local server error")
        
    if not ai_text:
        raise HTTPException(status_code=500, detail="AI returned empty text")
        
    audio_buffer = generate_speech_stream(ai_text)
    
    return StreamingResponse(audio_buffer, media_type="audio/wav")