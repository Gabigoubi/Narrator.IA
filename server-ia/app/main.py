from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import requests
from app.prompt import get_system_prompt
from app.tts import generate_speech_stream

def get_best_local_model() -> str:
    """Consulta o Ollama e retorna o melhor modelo disponível no PC do usuário."""
    try:
        response = requests.get("http://localhost:11434/api/tags", timeout=5)
        response.raise_for_status()
        
        models = [m["name"] for m in response.json().get("models", [])]
        
        if "mistral:latest" in models or "mistral" in models:
            return "mistral"
        if "qwen2.5:3b" in models:
            return "qwen2.5:3b"
            
        return models[0] if models else "mistral"
        
    except Exception as e:
        print(f"\n[AVISO] Erro ao detectar modelo no Ollama. Forçando 'mistral'. Erro: {e}")
        return "mistral"

MODELO_ATUAL = get_best_local_model()
print(f"[SISTEMA] Modelo selecionado para a sessão: {MODELO_ATUAL}")

app = FastAPI()

OLLAMA_URL = "http://localhost:11434/api/generate"

# MEMÓRIA GLOBAL: Guarda os últimos 15 eventos enquanto o cara joga
EVENT_HISTORY = []

class PlayerTelemetry(BaseModel):
    event_type: str
    context_details: str
    voice_model: str

def format_telemetry(events_list: list) -> tuple:
    """Fatia a lista de eventos em passado (máx 11) e recente (máx 4)."""
    if not events_list:
        return "No past events.", "No recent events."
    
    # Garante máximo de 15 eventos
    events_list = events_list[-15:]
    
    if len(events_list) <= 4:
        past_events = "No past events."
        recent_events = "\n".join(f"- {e}" for e in events_list)
    else:
        past = events_list[:-4]
        recent = events_list[-4:]
        
        past_events = "\n".join(f"- {e}" for e in past)
        recent_events = "\n".join(f"- {e}" for e in recent)
        
    return past_events, recent_events


@app.post("/narrate")
def generate_narration(telemetry: PlayerTelemetry):
    global EVENT_HISTORY
    
  
    current_event = f"Event: {telemetry.event_type}. Details: {telemetry.context_details}"
    
    
    EVENT_HISTORY.append(current_event)
    if len(EVENT_HISTORY) > 15:
        EVENT_HISTORY.pop(0)
        
    
    past_str, recent_str = format_telemetry(EVENT_HISTORY)
    
    
    system_prompt = get_system_prompt(past_events=past_str, recent_events=recent_str, persona_id=telemetry.voice_model)

    
    payload = {
        "model": MODELO_ATUAL, 
        "prompt": system_prompt, 
        "stream": False,
        "options": {
            "temperature": 0.5,
            "top_p": 0.9,       
            "top_k": 40     
        }
    }

    
    print("\n" + "="*50)
    print(f"[DEBUG IA] Memória Total: {len(EVENT_HISTORY)} eventos")
    print(f"[DEBUG IA] Eventos Recentes na mira da IA:\n{recent_str}")
    print("="*50 + "\n")

    try:
        response = requests.post(OLLAMA_URL, json=payload, timeout=300)
        response.raise_for_status()
        response_data = response.json()
        ai_text = response_data.get("response", "")
        
        
        print(f"[DEBUG IA] Resposta da IA: {ai_text}")
        
    except Exception as e:
        print(f"Erro na comunicação com Ollama: {e}")
        raise HTTPException(status_code=500, detail="Ollama local server error")

    if not ai_text:
        raise HTTPException(status_code=500, detail="AI returned empty text")

    audio_buffer = generate_speech_stream(ai_text, telemetry.voice_model)

    return StreamingResponse(audio_buffer, media_type="audio/wav")
