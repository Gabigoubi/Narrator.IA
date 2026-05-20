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
        if "mistral:latest" in models or "mistral" in models:
            return "mistral"
        if "qwen2.5:3b" in models:
            return "qwen2.5:3b"
        if models:
            print(f"[BOOT] Modelos preferenciais nao encontrados. Utilizando fallback primario: {models[0]}")
            return models[0]
        print("[BOOT] Nenhum modelo listado no Ollama. Forçando fallback estático 'mistral'")
        return "mistral"
    except Exception as e:
        print(f"[AVISO BOOT] Conexao com Ollama falhou na inicializacao. Erro: {str(e)}")
        print("[AVISO BOOT] Definindo padrao de seguranca para 'mistral'")
        return "mistral"

MODELO_ATUAL = get_best_local_model()
print(f"[SISTEMA] Servidor iniciado. Modelo ativo para processamento: {MODELO_ATUAL}")

app = FastAPI()
OLLAMA_URL = "http://localhost:11434/api/generate"
EVENT_HISTORY = []

class PlayerTelemetry(BaseModel):
    event_type: str
    context_details: str
    voice_model: str

def format_telemetry(events_list: list) -> tuple:
    if not events_list:
        return "No past events.", "No recent events."
    slice_history = events_list[-15:]
    if len(slice_history) <= 4:
        past_events = "No past events."
        recent_events = "\n".join(f"- {e}" for e in slice_history)
    else:
        past = slice_history[:-4]
        recent = slice_history[-4:]
        past_events = "\n".join(f"- {e}" for e in past)
        recent_events = "\n".join(f"- {e}" for e in recent)
    return past_events, recent_events

@app.post("/narrate")
def generate_narration(telemetry: PlayerTelemetry):
    global EVENT_HISTORY
    try:
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
        print(f"[LOG REQUISICAO] Payload estruturado enviado ao Ollama:")
        print(f"Modelo Alvo: {MODELO_ATUAL}")
        print(f"Janela de Contexto Passado:\n{past_str}")
        print(f"Gatilhos Atuais Selecionados:\n{recent_str}")
        print("="*50)

        print("[OLLAMA] Enviando requisicao POST para a API do Ollama...")
        response = requests.post(OLLAMA_URL, json=payload, timeout=300)
        print(f"[OLLAMA] Resposta recebida. Codigo de Status HTTP: {response.status_code}")
        response.raise_for_status()
        
        response_data = response.json()
        ai_text = response_data.get("response", "")
        print(f"[LOG IA] Texto purificado gerado pela IA: '{ai_text}'")
        
        if not ai_text or ai_text.strip() == "":
            print("[ERRO] A IA gerou uma resposta nula ou composta por espacos em branco.")
            raise ValueError("Ollama retornou string de texto vazia.")

        print("[TTS] Enviando texto para o motor de sintese de voz...")
        audio_buffer = generate_speech_stream(ai_text, telemetry.voice_model)
        print("[TTS] Fluxo de audio em formato WAV gerado com sucesso.")

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
        print("Exibindo rastro completo do erro abaixo para depuracao:")
        traceback.print_exc(file=sys.stdout)
        print("!"*50 + "\n")
        raise HTTPException(status_code=500, detail=f"Erro interno no servidor de narracao: {str(e_internal)}")
