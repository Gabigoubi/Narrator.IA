import requests
import traceback
import sys
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from app.prompt import get_system_prompt
from app.tts import generate_speech_stream

def get_best_local_model() -> str:
    try:
        print("[BOOT] Solicitando lista de modelos ao Ollama local...")
        response = requests.get("http://localhost:11434/api/tags", timeout=5)
        response.raise_for_status()
        models = [m["name"] for m in response.json().get("models", [])]
        print(f"[BOOT] Modelos locais encontrados: {models}")
        
        # Ordem de prioridade para a v1.1 (Focado em modelos mais robustos)
        if "mistral:latest" in models or "mistral" in models:
            return "mistral"
        if "llama3:latest" in models or "llama3" in models:
            return "llama3"
        if "qwen2.5:3b" in models:
            return "qwen2.5:3b"
            
        if models:
            print(f"[BOOT] Modelos preferenciais nao encontrados. Fallback: {models[0]}")
            return models[0]
            
        return "mistral"
    except Exception as e:
        print(f"[AVISO BOOT] Conexao com Ollama falhou na inicializacao. Erro: {str(e)}")
        return "mistral"

MODELO_ATUAL = get_best_local_model()
print(f"[SISTEMA] Servidor iniciado. Modelo ativo: {MODELO_ATUAL}")

app = FastAPI()
OLLAMA_URL = "http://localhost:11434/api/generate"
EVENT_HISTORY = []

class PlayerTelemetry(BaseModel):
    event_type: str
    context_details: str
    voice_model: str

@app.post("/narrate")
def generate_narration(telemetry: PlayerTelemetry):
    global EVENT_HISTORY
    try:
        # Adiciona o evento que acabou de chegar na lista global
        current_event_str = f"[{telemetry.event_type}] {telemetry.context_details}"
        EVENT_HISTORY.append(current_event_str)
        
        # Limita o histórico a 15 eventos no total
        if len(EVENT_HISTORY) > 15:
            EVENT_HISTORY.pop(0)

        # FATIAMENTO MÁGICO: 11 no passado, 4 no presente
        past_list = EVENT_HISTORY[:-4] if len(EVENT_HISTORY) > 4 else []
        recent_list = EVENT_HISTORY[-4:] if len(EVENT_HISTORY) > 0 else []

        past_str = "\n".join(f"- {e}" for e in past_list) if past_list else "Sem eventos passados."
        recent_str = "\n".join(f"- {e}" for e in recent_list) if recent_list else "Sem eventos recentes."

        system_prompt = get_system_prompt(past_events=past_str, recent_events=recent_str, persona_id=telemetry.voice_model)
        
        payload = {
            "model": MODELO_ATUAL, 
            "prompt": system_prompt, 
            "stream": False,
            "options": {
                "temperature": 0.5, # Temperatura balanceada para modelos maiores (Mistral/Llama3)
                "top_p": 0.9,       
                "top_k": 40     
            }
        }

        print("\n" + "="*50)
        print(f"[LOG REQUISICAO] Payload estruturado enviado ao Ollama:")
        print(f"Modelo Alvo: {MODELO_ATUAL}")
        print(f"Passado (Contexto de 11):\n{past_str}")
        print(f"Agora (Foco nos 4 ultimos):\n{recent_str}")
        print("="*50)

        print("[OLLAMA] Enviando requisicao POST...")
        response = requests.post(OLLAMA_URL, json=payload, timeout=300)
        print(f"[OLLAMA] Resposta recebida. Codigo HTTP: {response.status_code}")
        response.raise_for_status()
        
        response_data = response.json()
        ai_text = response_data.get("response", "")
        print(f"[LOG IA] Texto gerado: '{ai_text}'")
        
        if not ai_text or ai_text.strip() == "":
            raise ValueError("Ollama retornou string vazia.")

        print("[TTS] Enviando texto para sintese de voz...")
        audio_buffer = generate_speech_stream(ai_text, telemetry.voice_model)
        print("[TTS] Audio gerado com sucesso.")

        return StreamingResponse(audio_buffer, media_type="audio/wav")

    except requests.exceptions.RequestException as e_req:
        print("\n" + "!"*50)
        print("[ERRO CRITICO] Falha na conexao HTTP/Rede com o Ollama local.")
        print(f"Detalhes do erro: {str(e_req)}")
        print("!"*50 + "\n")
        raise HTTPException(status_code=500, detail=f"Erro de comunicacao com Ollama: {str(e_req)}")

    except Exception as e_internal:
        print("\n" + "!"*50)
        print("[ERRO CRITICO INTERNO] Falha geral detectada na execucao da rota /narrate.")
        traceback.print_exc(file=sys.stdout)
        print("!"*50 + "\n")
        raise HTTPException(status_code=500, detail=f"Erro interno: {str(e_internal)}")