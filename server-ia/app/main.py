from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import requests

# Atualizamos a importação para pegar a nova função de prompt
from app.prompt import get_system_prompt
from app.tts import generate_speech_stream

app = FastAPI()

OLLAMA_URL = "http://localhost:11434/api/generate"

class PlayerTelemetry(BaseModel):
    event_type: str
    context_details: str
    ai_model: str      # <-- Adicionado para bater com o Java
    voice_model: str   # <-- Adicionado para bater com o Java

@app.get("/health")
def check_health():
    return {"status": "healthy"}

@app.post("/narrate")
def generate_narration(telemetry: PlayerTelemetry):
    user_message = f"Event: {telemetry.event_type}. Details: {telemetry.context_details}"
    
    # Chama a função nova para pegar a personalidade certa baseada na voz
    system_prompt = get_system_prompt(telemetry.voice_model)
    
    payload = {
        "model": telemetry.ai_model,  # <-- Trocamos o qwen2.5 fixo pela variável do Java
        "prompt": f"{system_prompt}\n\nUser Event: {user_message}",
        "stream": False
    }
    
    try:
        response = requests.post(OLLAMA_URL, json=payload, timeout=300)
        response.raise_for_status()
        response_data = response.json()
        ai_text = response_data.get("response", "")
    except Exception:
        raise HTTPException(status_code=500, detail="Ollama local server error")
        
    if not ai_text:
        raise HTTPException(status_code=500, detail="AI returned empty text")
        
    # Passamos o texto gerado e a voz escolhida pro gerador de áudio
    audio_buffer = generate_speech_stream(ai_text, telemetry.voice_model)
    
    return StreamingResponse(audio_buffer, media_type="audio/wav")