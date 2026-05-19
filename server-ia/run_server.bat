@echo off
title Servidor Narrador IA - FastAPI
echo ====================================================
echo 🚀 ATIVANDO AMBIENTE VIRTUAL PYTHON...
echo ====================================================

:: Tenta ativar a venv de forma automática no Windows
call .\venv\Scripts\activate.bat

:: Checa se o comando deu certo e se a venv está ativa
if %ERRORLEVEL% NEQ 0 (
    echo ❌ ERRO: Nao foi possivel ativar a venv. Execute o Passo 3 do manual antes!
    pause
    exit /b
)

echo ====================================================
echo 🔥 LIGANDO O SERVIDOR DE AUDIO (UVICORN)...
echo ====================================================
uvicorn app.main:app --reload

pause