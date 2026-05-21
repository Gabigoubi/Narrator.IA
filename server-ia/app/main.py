import requests
import traceback
import sys
import psutil 
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from app.prompt import get_system_prompt
from app.tts import generate_speech_stream

# --- CONSTANTES ---
OLLAMA_GENERATE_URL = "http://localhost:11434/api/generate"
OLLAMA_TAGS_URL = "http://localhost:11434/api/tags"
OLLAMA_PULL_URL = "http://localhost:11434/api/pull"
MAX_HISTORY_ACTIONS = 15
TIMEOUT_OLLAMA = 300
MODELO_PADRAO = "mistral" # PADRONIZADO E CHUMBADO NO CÓDIGO

# --- ESTADO INTERNO ---
ACTION_HISTORY = []

# --- MODELOS DE DADOS (PYDANTIC v1.3) ---
class PlayerTelemetry(BaseModel):
    voice_model: str
    critical_states: list[str]
    hotbar: list[str]
    recent_actions: list[str]

# --- VERIFICAÇÃO DE HARDWARE (HARD LOCK) ---
def check_hardware_requirements():
    print("\n[BOOT] 🔍 Verificando hardware da máquina...")
    # Pega a RAM total em Gigabytes
    ram_gb = psutil.virtual_memory().total / (1024 ** 3)
    
    # Verificação de 12GB (usamos 11.5 para compensar memória reservada pelo sistema)
    if ram_gb < 11.5:
        print("\n" + "!"*60)
        print(" ❌ [ERRO CRÍTICO DE HARDWARE] ❌")
        print(f" Seu PC possui apenas {ram_gb:.1f}GB de memória RAM total.")
        print(" O Narrador IA requer no MÍNIMO 12GB de RAM para rodar simultaneamente")
        print(" com o Minecraft sem causar o travamento do seu Windows.")
        print(" Inicialização bloqueada para proteger o seu sistema.")
        print("!"*60 + "\n")
        sys.exit(1) # Mata o Python instantaneamente. O .bat vai segurar a tela.
    else:
        print(f"[BOOT] ✔️ Hardware aprovado: {ram_gb:.1f}GB de RAM detectados.")

# --- GERENCIAMENTO DE MODELO (OLLAMA) ---
def ensure_model_exists():
    try:
        print(f"[BOOT] ⏳ Verificando se o modelo '{MODELO_PADRAO}' está instalado...")
        response = requests.get(OLLAMA_TAGS_URL, timeout=5)
        response.raise_for_status()
        models = [m["name"] for m in response.json().get("models", [])]
        
        # O Ollama costuma adicionar :latest no nome base
        if MODELO_PADRAO in models or f"{MODELO_PADRAO}:latest" in models:
            print(f"[BOOT] ✔️ Modelo '{MODELO_PADRAO}' já está pronto para uso.")
            return

        print(f"\n[BOOT] ⚠️ Modelo '{MODELO_PADRAO}' não encontrado. Iniciando download automático...")
        print("[BOOT] Isso pode demorar dependendo da sua internet (Arquivo de aprox. 4.1GB).")
        
        # Faz o Ollama baixar o Mistral (Stream=False faz o Python esperar o fim do download)
        pull_response = requests.post(OLLAMA_PULL_URL, json={"name": MODELO_PADRAO, "stream": False}, timeout=3600)
        pull_response.raise_for_status()
        
        print(f"[BOOT] ✔️ Download concluído! Modelo '{MODELO_PADRAO}' instalado com sucesso.")
        
    except requests.exceptions.RequestException as e:
        print(f"\n[AVISO BOOT] ❌ Falha ao comunicar com o Ollama para baixar o modelo. Erro: {str(e)}")
        print("Certifique-se de que o aplicativo do Ollama está aberto no Windows.")
        sys.exit(1)

# --- INICIALIZAÇÃO E ROTAS ---
check_hardware_requirements()
ensure_model_exists()

print("="*60)
print(f"🚀 [SISTEMA] Servidor iniciado com sucesso!")
print(f"🧠 [MODELO ATIVO]: {MODELO_PADRAO}")
print("="*60 + "\n")

app = FastAPI()

@app.post("/narrate")
def generate_narration(telemetry: PlayerTelemetry):
    global ACTION_HISTORY
    try:
        critical_states_str = "\n".join(f"- {s}" for s in telemetry.critical_states) if telemetry.critical_states else "Nenhum estado crítico."
        hotbar_str = f"[{', '.join(telemetry.hotbar)}]" if telemetry.hotbar else "[Inventário Vazio]"
        past_actions_str = "\n".join(f"- {a}" for a in ACTION_HISTORY) if ACTION_HISTORY else "Nenhum evento passado."
        current_actions_str = "\n".join(f"- {a}" for a in telemetry.recent_actions) if telemetry.recent_actions else "Nenhuma ação recente."

        # LOG VISUAL PARA DEBUG
        print("\n" + "▼"*60)
        print(" 📥 [NOVO EVENTO RECEBIDO DO MINECRAFT]")
        print(f" ➔ Estados Críticos:\n    {critical_states_str.replace('\n', '\n    ')}")
        print(f" ➔ Ações Recentes (Gatilho):\n    {current_actions_str.replace('\n', '\n    ')}")
        print("▼"*60)

        ACTION_HISTORY.extend(telemetry.recent_actions)
        if len(ACTION_HISTORY) > MAX_HISTORY_ACTIONS:
            ACTION_HISTORY = ACTION_HISTORY[-MAX_HISTORY_ACTIONS:]

        system_prompt = get_system_prompt(
            past_actions=past_actions_str,
            current_actions=current_actions_str,
            critical_states=critical_states_str,
            hotbar=hotbar_str,
            persona_id=telemetry.voice_model
        )

        payload = {
            "model": MODELO_PADRAO, 
            "prompt": system_prompt, 
            "stream": False,
            "options": {"temperature": 0.5, "top_p": 0.9, "top_k": 40}
        }
        
        print(f" 🧠 [IA] Processando contexto com o modelo '{MODELO_PADRAO}'...")
        response = requests.post(OLLAMA_GENERATE_URL, json=payload, timeout=TIMEOUT_OLLAMA)
        response.raise_for_status()
        
        ai_text = response.json().get("response", "").strip()
        print("-" * 60)
        print(f" 💬 [FALA GERADA]:\n    '{ai_text}'")
        print("-" * 60)

        print(" 🔊 [TTS] Gerando síntese de voz...")
        audio_buffer = generate_speech_stream(ai_text, telemetry.voice_model)
        print(" ✔️ [TTS] Áudio gerado e enviado ao jogo!\n")

        return StreamingResponse(audio_buffer, media_type="audio/wav")

    except Exception as e:
        print("\n" + "!"*60)
        print(f" ❌ [ERRO] Falha na execução da rota: {str(e)}")
        print("!"*60 + "\n")
        raise HTTPException(status_code=500, detail=str(e))
