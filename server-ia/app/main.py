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

# ========================================================================
# CONFIGURAÇÕES E ESTADO GLOBAL
# ========================================================================
OLLAMA_GENERATE_URL = "http://localhost:11434/api/generate"
OLLAMA_TAGS_URL = "http://localhost:11434/api/tags"
OLLAMA_PULL_URL = "http://localhost:11434/api/pull"
GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

TIMEOUT_OLLAMA = 300
DEFAULT_MODEL = "mistral"

GROQ_API_KEY = None
IS_DEV_MODE = False

# Carregamento do .env (Se existir)
if os.path.exists(".env"):
    with open(".env", "r", encoding="utf-8") as f:
        for line in f:
            if line.strip() and line.startswith("GROQ_API_KEY="):
                GROQ_API_KEY = line.strip().split("=", 1)[1].strip().strip('"').strip("'")
            elif line.strip() and line.startswith("IS_DEV_MODE="):
                val = line.strip().split("=", 1)[1].strip().lower()
                IS_DEV_MODE = (val == "true")

if GROQ_API_KEY and IS_DEV_MODE:
    print("\n[BOOT] 🚀 [DEV MODE] Infraestrutura Groq ativada. Logs de Debug ON.")
else:
    IS_DEV_MODE = False # Força false se a chave não estiver configurada ou modo estiver false

# ========================================================================
# MODELOS DE DADOS (PYDANTIC)
# ========================================================================
class PlayerTelemetry(BaseModel):
    voice_model: str
    critical_states: list[str] = []
    hotbar: list[str] | None = None
    recent_actions: list[str]
    y_level: int | None = None         
    is_session_summary: bool = False

# ========================================================================
# ROTINAS DE PROTEÇÃO E INICIALIZAÇÃO
# ========================================================================
def check_hardware_requirements():
    if IS_DEV_MODE: return
    ram_gb = psutil.virtual_memory().total / (1024 ** 3)
    if ram_gb < 11.5:
        print("\n❌ [ERRO CRITICO DE HARDWARE] O Narrador IA exige no mínimo 12GB de RAM física.")
        sys.exit(1)

def ensure_model_exists():
    if IS_DEV_MODE: return
    try:
        response = requests.get(OLLAMA_TAGS_URL, timeout=5)
        response.raise_for_status()
        models = [m["name"] for m in response.json().get("models", [])]
        if DEFAULT_MODEL not in models and f"{DEFAULT_MODEL}:latest" not in models:
            print(f"[BOOT] ⚠️ Modelo base '{DEFAULT_MODEL}' não encontrado. Baixando (~4.1GB)...")
            requests.post(OLLAMA_PULL_URL, json={"name": DEFAULT_MODEL, "stream": False}, timeout=3600)
    except Exception as e:
        print(f"\n[BOOT FATAL] ❌ Falha na comunicação com o motor local (Ollama): {str(e)}")
        sys.exit(1)

check_hardware_requirements()
ensure_model_exists()

print("\n" + "=" * 50)
print(" 🎙️ Edson Calotas - Motor Cognitivo Iniciado")
print("=" * 50 + "\n")

app = FastAPI()

edson_memory = SlidingMemory(max_history=1)

# ========================================================================
# ROTINAS DE OBSERVABILIDADE E LOGS (ÉPICO 5)
# ========================================================================
def log_step(step_num: int, message: str):
    if not IS_DEV_MODE:
        print(f" [{step_num}/4] {message}")
    else:
        print(f"\n▼ [ETAPA {step_num}] {message}")

def log_dashboard(telemetry, direcao, user_data):
    if not IS_DEV_MODE: return
    print("\n" + "█" * 70)
    print(" 🧠 DUMP DO CÉREBRO (REGENTE) ".center(70, "█"))
    print("█" * 70)
    print(f"\n📥 1. DADOS BRUTOS RECEBIDOS DO JAVA:\n{telemetry.model_dump_json(indent=2)}")
    print(f"\n📊 2. SCORES CALCULADOS PELA LÓGICA:\n   {direcao['debug_scores']}")
    print(f"\n🎭 3. DECISÃO DE CENA E TOM:\n   Tipo de Cena: {direcao['scene_type']}\n   Tom Vocal: {direcao['tone']}")
    print(f"\n📜 4. ROTEIRO FINAL ENVIADO PARA A IA (LLM):\n{user_data}")
    print("\n" + "█" * 70 + "\n")

# ========================================================================
# ROTA PRINCIPAL (API DE NARRATIVA)
# ========================================================================
@app.post("/narrate")
def generate_narration(telemetry: PlayerTelemetry):
    try:
        log_step(1, "Pacote de telemetria recebido do Java.")

        # 1. Regente Analisa e Pontua (Com novos dados de Fronteira Seca)
        direcao = analisar_telemetria(
            telemetry.recent_actions, 
            telemetry.critical_states,
            telemetry.y_level,
            telemetry.is_session_summary
        )
        
        # Garante que a memória inicie zerada a cada nova sessão no jogo
        global edson_memory
        if direcao['scene_type'] == "player_login":
            edson_memory = SlidingMemory(max_history=1)
            if IS_DEV_MODE:
                print("\n🧠 [SISTEMA] Memória de contexto apagada para a nova sessão.")

        log_step(2, f"Roteiro escrito pelo Regente (Cena determinada: {direcao['scene_type']}).")

        # 2. Monta o Prompt Final
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

        # Imprime o Dashboard Completo para o Desenvolvedor
        log_dashboard(telemetry, direcao, user_data)

        # 3. Inferência LLM
        log_step(3, "Enviando roteiro para atuação da IA (Inferência)...")
        ai_text = fetch_ai_response(system_rules, user_data)
        
        # Grava a lição de moral na memória
        edson_memory.add_interaction(direcao['focus_target']['behavior'], ai_text)

        # 4. Geração de Áudio
        log_step(4, "Sintetizando voz e despachando áudio...")
        audio_buffer = generate_speech_stream(ai_text)
        
        if IS_DEV_MODE:
            print("\n[SUCESSO] Pacote de áudio .WAV enviado para o Minecraft.\n" + "-" * 70)

        return StreamingResponse(audio_buffer, media_type="audio/wav")
        
    except Exception as e: 
        print(f"\n❌ [ERRO CRÍTICO NO PIPELINE]: {str(e)}")
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

def fetch_ai_response(system_prompt: str, user_prompt: str) -> str:
    if IS_DEV_MODE:
        headers = {"Authorization": f"Bearer {GROQ_API_KEY}", "Content-Type": "application/json"}
        # AJUSTE v1.5: max_tokens aumentado para 350 para evitar cortes em respostas de 3-4 frases
        payload = {"model": "llama-3.3-70b-versatile", "messages": [{"role": "system", "content": system_prompt}, {"role": "user", "content": user_prompt}], "temperature": 0.8, "max_tokens": 380}
        response = requests.post(GROQ_API_URL, json=payload, headers=headers, timeout=30)
        response.raise_for_status()
        ai_text = response.json()["choices"][0]["message"]["content"].strip()
    else:
        # AJUSTE v1.5: num_predict aumentado para 350 para evitar cortes em respostas de 3-4 frases
        payload = {"model": DEFAULT_MODEL, "messages": [{"role": "system", "content": system_prompt}, {"role": "user", "content": user_prompt}], "stream": False, "options": {"temperature": 0.8, "top_p": 0.9, "num_predict": 380}}
        response = requests.post(OLLAMA_GENERATE_URL, json=payload, timeout=TIMEOUT_OLLAMA)
        response.raise_for_status()
        ai_text = response.json().get("response", "").strip()
        
    if not ai_text: raise ValueError("A LLM retornou um texto vazio ou nulo.")
    
    if IS_DEV_MODE: 
        print(f"\n🗣️ [RESPOSTA DE ÁUDIO GERADA]:\n{ai_text}")
        
    return ai_text
