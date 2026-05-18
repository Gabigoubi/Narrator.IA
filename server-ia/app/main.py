from fastapi import FastAPI
from pydantic import BaseModel
import requests
from app.prompt import SYSTEM_PROMPT

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
    
    response = requests.post(OLLAMA_URL, json=payload)
    response_data = response.json()
    
    ai_text = response_data.get("response", "")
    
    return {"text": ai_text}