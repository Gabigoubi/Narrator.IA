import os
import sys
import psutil
import requests
import traceback
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from app.prompt import get_system_instructions, format_user_telemetry
from app.tts import generate_speech_stream
from app.memory import SlidingMemory
from app.regente import analisar_telemetria

# --- CONFIGURATION CONSTANTS ---
OLLAMA_GENERATE_URL = "http://localhost:11434/api/generate"
OLLAMA_TAGS_URL = "http://localhost:11434/api/tags"
OLLAMA_PULL_URL = "http://localhost:11434/api/pull"
GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

TIMEOUT_OLLAMA = 300
DEFAULT_MODEL = "mistral"

# --- DEV MODE DETECTOR ---
GROQ_API_KEY = None
IS_DEV_MODE = False

if os.path.exists(".env"):
    with open(".env", "r", encoding="utf-8") as f:
        for line in f:
            if line.strip() and line.startswith("GROQ_API_KEY="):
                GROQ_API_KEY = line.strip().split("=", 1)[1].strip().strip('"').strip("'")
            elif line.strip() and line.startswith("IS_DEV_MODE="):
                val = line.strip().split("=", 1)[1].strip().lower()
                IS_DEV_MODE = (val == "true")

if GROQ_API_KEY and IS_DEV_MODE:
    print("\n[BOOT] 🚀 [DEV MODE] Cloud infrastructure active. Debug logs ON.")
else:
    IS_DEV_MODE = False # Força false se não estiver explícito

# --- DATA MODELS ---
class PlayerTelemetry(BaseModel):
    voice_model: str
    critical_states: list[str]
    hotbar: list[str] | None = None
    recent_actions: list[str]

# --- HARDWARE PROTECTION ---
def check_hardware_requirements():
    if IS_DEV_MODE:
        return
    ram_gb = psutil.virtual_memory().total / (1024 ** 3)
    if ram_gb < 11.5:
        print("\n❌ [CRITICAL HARDWARE ERROR] Narrador IA requires 12GB RAM minimum.")
        sys.exit(1)

def ensure_model_exists():
    if IS_DEV_MODE: return
    try:
        response = requests.get(OLLAMA_TAGS_URL, timeout=5)
        response.raise_for_status()
        models = [m["name"] for m in response.json().get("models", [])]
        if DEFAULT_MODEL not in models and f"{DEFAULT_MODEL}:latest" not in models:
            print(f"[BOOT] ⚠️ Model '{DEFAULT_MODEL}' not found. Downloading (~4.1GB)...")
            requests.post(OLLAMA_PULL_URL, json={"name": DEFAULT_MODEL, "stream": False}, timeout=3600)
    except Exception as e:
        print(f"\n[BOOT FATAL] ❌ Failed to communicate with Ollama: {str(e)}")
        sys.exit(1)

check_hardware_requirements()
ensure_model_exists()

print("\n" + "=" * 50)
print(" 🎙️ Edson Calotas - Servidor Iniciado")
print("=" * 50 + "\n")

app = FastAPI()
edson_memory = SlidingMemory(max_history=3)

# --- LOGGING HELPER (ÉPICO 5) ---
def log_step(step_num: int, message: str, dev_dump: str = None):
    if not IS_DEV_MODE:
        print(f" [{step_num}/4] {message}")
    else:
        print(f"\n▼ [PASSO {step_num}] {message}")
        if dev_dump:
            print(f"--- DADOS ---\n{dev_dump}\n-------------")

# --- ROUTES ---
@app.post("/narrate")
def generate_narration(telemetry: PlayerTelemetry):
    try:
        log_step(1, "Recebendo telemetria...", dev_dump=telemetry.model_dump_json(indent=2))

        # 1. Regente Analisa (ÉPICO 4)
        direcao = analisar_telemetria(telemetry.recent_actions, telemetry.critical_states)
        log_step(2, f"Escrevendo roteiro (Cena: {direcao['scene_type']})...", dev_dump=direcao['action_focus_str'])

        # 2. Monta Prompt
        current_memory = edson_memory.get_context_string()
        critical_states_str = "\n".join(f"- {s}" for s in telemetry.critical_states)
        hotbar_str = ", ".join(telemetry.hotbar) if telemetry.hotbar else "Vazio"

        system_rules = get_system_instructions()
        user_data = format_user_telemetry(
            memory_context=current_memory,
            critical_states=critical_states_str,
            hotbar=hotbar_str,
            recent_actions=direcao['action_focus_str'],
            scene_type=direcao['scene_type'],
            tone=direcao['tone'],
            focus_target=direcao['focus_target'],
            response_density=direcao['response_density']
        )

        # 3. Inferência LLM
        log_step(3, "Gerando atuação do Edson (LLM + TTS)...", dev_dump=user_data)
        ai_text = fetch_ai_response(system_rules, user_data)
        
        # O Edson lembra do comportamento que criticou, e não do texto bruto
        edson_memory.add_interaction(direcao['focus_target']['behavior'], ai_text)

        # 4. Áudio
        audio_buffer = generate_speech_stream(ai_text)
        log_step(4, "Áudio enviado para o Minecraft com sucesso!\n" + "-" * 50)

        return StreamingResponse(audio_buffer, media_type="audio/wav")
        
    except Exception as e: 
        print(f"\n❌ [ERRO NO PIPELINE]: {str(e)}")
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

def fetch_ai_response(system_prompt: str, user_prompt: str) -> str:
    if IS_DEV_MODE:
        headers = {"Authorization": f"Bearer {GROQ_API_KEY}", "Content-Type": "application/json"}
        payload = {"model": "llama-3.3-70b-versatile", "messages": [{"role": "system", "content": system_prompt}, {"role": "user", "content": user_prompt}], "temperature": 0.75, "max_tokens": 120}
        response = requests.post(GROQ_API_URL, json=payload, headers=headers, timeout=30)
        response.raise_for_status()
        ai_text = response.json()["choices"][0]["message"]["content"].strip()
    else:
        payload = {"model": DEFAULT_MODEL, "messages": [{"role": "system", "content": system_prompt}, {"role": "user", "content": user_prompt}], "stream": False, "options": {"temperature": 0.8, "top_p": 0.9, "num_predict": 120}}
        response = requests.post(OLLAMA_GENERATE_URL, json=payload, timeout=TIMEOUT_OLLAMA)
        response.raise_for_status()
        ai_text = response.json().get("response", "").strip()
        
    if not ai_text: raise ValueError("A LLM retornou um texto vazio.")
    if IS_DEV_MODE: print(f"\n🗣️ [FALA GERADA]:\n{ai_text}\n")
    return ai_text
