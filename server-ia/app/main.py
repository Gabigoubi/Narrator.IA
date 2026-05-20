from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import requests
import os
from app.prompt import get_system_prompt
from app.tts import generate_speech_stream

app = FastAPI()

OLLAMA_URL = "http://localhost:11434/api/generate"

class PlayerTelemetry(BaseModel):
    event_type: str
    context_details: str
    voice_model: str

@app.post("/narrate")
def generate_narration(telemetry: PlayerTelemetry):
    user_message = f"Event: {telemetry.event_type}. Details: {telemetry.context_details}"
    system_prompt = get_system_prompt(telemetry.voice_model)
    
    model_name = os.getenv('AI_MODEL', 'qwen2.5:3b')

    payload = {
        "model": model_name, 
        "prompt": f"{system_prompt}\n\nUser Event: {user_message}",
        "stream": False,
        "options": {
            "temperature": 0.85
        }
    }

    try:
        response = requests.post(OLLAMA_URL, json=payload, timeout=300)
        response.raise_for_status()
        response_data = response.json()
        ai_text = response_data.get("response", "")
    except Exception as e:
        print(f"Erro na comunicação com Ollama: {e}")
        raise HTTPException(status_code=500, detail="Ollama local server error")

    if not ai_text:
        raise HTTPException(status_code=500, detail="AI returned empty text")

    audio_buffer = generate_speech_stream(ai_text, telemetry.voice_model)

    return StreamingResponse(audio_buffer, media_type="audio/wav")