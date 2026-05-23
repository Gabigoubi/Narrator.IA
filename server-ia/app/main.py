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

# --- CONFIGURATION CONSTANTS ---
OLLAMA_GENERATE_URL = "http://localhost:11434/api/generate"
OLLAMA_TAGS_URL = "http://localhost:11434/api/tags"
OLLAMA_PULL_URL = "http://localhost:11434/api/pull"
GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

TIMEOUT_OLLAMA = 300  # 5 minutes, matching Java's HttpAssistant
DEFAULT_MODEL = "mistral"

# --- DEV MODE DETECTION (ZERO-DEPENDENCY ENV PARSER) ---
GROQ_API_KEY = None
IS_DEV_MODE = False

if os.path.exists(".env"):
    with open(".env", "r", encoding="utf-8") as f:
        for line in f:
            if line.strip() and line.startswith("GROQ_API_KEY="):
                GROQ_API_KEY = line.strip().split("=", 1)[1].strip().strip('"').strip("'")
                if GROQ_API_KEY:
                    IS_DEV_MODE = True
                break

# --- DATA MODELS (v1.3 Payload) ---
class PlayerTelemetry(BaseModel):
    voice_model: str
    critical_states: list[str]
    hotbar: list[str] | None = None
    recent_actions: list[str]

# --- HARDWARE PROTECTION (HARD LOCK) ---
def check_hardware_requirements():
    if IS_DEV_MODE:
        print("\n[BOOT] 🚀 [DEV MODE] Groq API Key detected. Hardware lock bypassed.")
        return

    print("\n[BOOT] 🔍 Scanning system hardware...")
    ram_gb = psutil.virtual_memory().total / (1024 ** 3)
    
    if ram_gb < 11.5:
        print("\n" + "!" * 60)
        print(" ❌ [CRITICAL HARDWARE ERROR] ❌")
        print(f" System Memory: {ram_gb:.1f}GB RAM detected.")
        print(" Narrador IA requires a STRICT MINIMUM of 12GB RAM to run locally")
        print(" alongside Minecraft without causing OS-level memory swapping (BSoD risk).")
        print(" Boot process halted to protect your machine.")
        print("!" * 60 + "\n")
        sys.exit(1)
    
    print(f"[BOOT] ✔️ Hardware approved: {ram_gb:.1f}GB RAM available.")

# --- OLLAMA MODEL MANAGEMENT ---
def ensure_model_exists():
    if IS_DEV_MODE:
        print("[BOOT] 🚀 [DEV MODE] Cloud infrastructure active. Local model check bypassed.")
        return

    try:
        print(f"[BOOT] ⏳ Verifying local installation of '{DEFAULT_MODEL}'...")
        response = requests.get(OLLAMA_TAGS_URL, timeout=5)
        response.raise_for_status()
        
        models = [m["name"] for m in response.json().get("models", [])]
        if DEFAULT_MODEL in models or f"{DEFAULT_MODEL}:latest" in models:
            print(f"[BOOT] ✔️ Model '{DEFAULT_MODEL}' is ready.")
            return

        print(f"\n[BOOT] ⚠️ Model '{DEFAULT_MODEL}' not found. Initializing auto-download...")
        print("[BOOT] Please wait. This may take a while depending on your bandwidth (Approx. 4.1GB).")
        
        pull_response = requests.post(OLLAMA_PULL_URL, json={"name": DEFAULT_MODEL, "stream": False}, timeout=3600)
        pull_response.raise_for_status()
        
        print(f"[BOOT] ✔️ Download complete! '{DEFAULT_MODEL}' installed successfully.")
        
    except requests.exceptions.RequestException as e:
        print(f"\n[BOOT FATAL] ❌ Failed to communicate with Ollama: {str(e)}")
        print("Action Required: Ensure the Ollama background app is running in Windows.")
        sys.exit(1)

# --- SERVER INITIALIZATION ---
check_hardware_requirements()
ensure_model_exists()

print("=" * 60)
print("🚀 [SYSTEM] API Server successfully started!")
print(f"🧠 [ACTIVE ENGINE]: {'llama-3.3-70b (Groq Cloud)' if IS_DEV_MODE else DEFAULT_MODEL}")
print("=" * 60 + "\n")

app = FastAPI()

# 2. Inicializa a memória do Edson (janela de 3 interações)
edson_memory = SlidingMemory(max_history=3)

# --- ROUTES ---
@app.post("/narrate")
def generate_narration(telemetry: PlayerTelemetry):
    try:
        # 1. Parse JSON lists into formatted strings for the prompt
        critical_states_str = "\n".join(f"- {s}" for s in telemetry.critical_states)
        hotbar_str = ", ".join(telemetry.hotbar) if telemetry.hotbar else ""
        recent_actions_str = "\n".join(f"- {a}" for a in telemetry.recent_actions)

       # 2. Visual Debug Log (Crucial for development)
        formatted_actions = recent_actions_str.replace('\n', '\n    ')
        
        print("\n" + "▼" * 60)
        print(" 📥 [NEW TELEMETRY EVENT FROM JAVA]")
        print(f" ➔ Critical States: {critical_states_str if critical_states_str else 'None'}")
        print(f" ➔ Hotbar: [{hotbar_str}]")
        print(f" ➔ Trigger Actions:\n    {formatted_actions}")
        print("▼" * 60)

      # 3. Pega o histórico imediato da memória
        current_memory = edson_memory.get_context_string()

        # 4. Compila as instruções e os dados SEPARADAMENTE
        system_rules = get_system_instructions()
        user_data = format_user_telemetry(
            memory_context=current_memory,
            critical_states=critical_states_str,
            hotbar=hotbar_str,
            recent_actions=recent_actions_str
        )

        # 5. Inference (LLM) - Passamos as duas partes agora
        ai_text = fetch_ai_response(system_rules, user_data)

        # 7. Synthesis (TTS)
        print(" 🔊 [TTS] Synthesizing speech stream...")
        audio_buffer = generate_speech_stream(ai_text)
        print(" ✔️ [TTS] Audio generated and streamed to Java!\n")

        # 8. Stream directly to Java AudioPlayer
        return StreamingResponse(audio_buffer, media_type="audio/wav")

    except Exception as e:
        print("\n" + "!" * 60)
        print(f" ❌ [RUNTIME ERROR] Pipeline failure: {str(e)}")
        traceback.print_exc() # Added precise stack trace for your debugging
        print("!" * 60 + "\n")
        raise HTTPException(status_code=500, detail=str(e))

# --- LLM ROUTER ---
# --- LLM ROUTER ---
def fetch_ai_response(system_prompt: str, user_prompt: str) -> str:
    if IS_DEV_MODE:
        headers = {
            "Authorization": f"Bearer {GROQ_API_KEY}",
            "Content-Type": "application/json"
        }
        payload = {
            "model": "llama-3.3-70b-versatile",
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            "temperature": 0.75, 
            "max_tokens": 120,   
            "top_p": 0.9
        }
        print(" 🧠 [DEV - GROQ] Pinging cloud engine...")
        response = requests.post(GROQ_API_URL, json=payload, headers=headers, timeout=30)
        response.raise_for_status()
        ai_text = response.json()["choices"][0]["message"]["content"].strip()
    else:
        # Lógica para o Ollama local
        payload = {
            "model": DEFAULT_MODEL, 
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            "stream": False,
            "options": {"temperature": 0.8, "top_p": 0.9, "top_k": 40, "num_predict": 120}
        }
        print(f" 🧠 [LOCAL LLM] Inferring context with '{DEFAULT_MODEL}'...")
   
        response = requests.post(OLLAMA_GENERATE_URL, json=payload, timeout=TIMEOUT_OLLAMA)
        response.raise_for_status()
        ai_text = response.json().get("response", "").strip()
        
    print("-" * 60)
    print(f" 💬 [EDSON'S SCRIPT]:\n    '{ai_text}'")
    print("-" * 60)
    
    if not ai_text:
        raise ValueError("LLM returned an empty string.")
        
    return ai_text