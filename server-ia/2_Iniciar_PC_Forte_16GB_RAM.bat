@echo off
title Servidor Narrador IA (PC FORTE) - Mistral 7B
cd /d "%~dp0"

echo ====================================================================
echo  INICIANDO MODO PC FORTE (MODELO: Mistral 7B)
echo ====================================================================
echo.

echo [SISTEMA] Verificando status do servico Ollama...
powershell -Command "$connect = New-Object System.Net.Sockets.TcpClient; $connect.Connect('127.0.0.1', 11434); if($connect.Connected) { exit 0 } else { exit 1 }" >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERRO CRITICO] O servico Ollama nao esta rodando!
    echo [ERR] Abra o aplicativo Ollama no Windows antes de iniciar este servidor.
    echo.
    pause
    exit
)
echo [SISTEMA] Service Ollama detectado e operando na porta 11434.
echo.

echo [SISTEMA] Verificando/Baixando a IA no Ollama (Pode demorar na primeira vez)...
ollama pull mistral
if %errorlevel% neq 0 (
    echo [ERRO] Falha ao realizar o download do modelo mistral via Ollama.
    pause
    exit
)

echo.
echo [SISTEMA] Verificando integridade do ambiente virtual (venv)...
if not exist ".\venv\Scripts\activate.bat" (
    echo [ERRO CRITICO] Pasta venv nao encontrada! Execute o script de instalacao primeiro.
    pause
    exit
)

echo [SISTEMA] Ativando o motor virtual (venv)...
call .\venv\Scripts\activate.bat

echo.
echo  LIGANDO O SERVIDOR DE AUDIO FASTAPI...
echo Pode abrir o seu Minecraft e jogar! (Nao feche esta janela)
echo ====================================================================
set AI_MODEL=mistral
uvicorn app.main:app --reload
pause
