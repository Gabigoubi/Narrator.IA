@echo off
title Servidor Narrador IA (PC FRACO) - Qwen 3B
echo ====================================================================
echo 🚀 INICIANDO MODO PC FRACO (MODELO: Qwen 2.5 3B)
echo ====================================================================
echo.
echo Verificando/Baixando a IA no Ollama (Pode demorar na primeira vez)...
ollama pull qwen2.5:3b


echo.
echo Ligando o motor virtual...
call .\venv\Scripts\activate.bat

echo.
echo 🔥 LIGANDO O SERVIDOR DE AUDIO FASTAPI...
echo Pode abrir o seu Minecraft e jogar! (Nao feche esta janela)
echo ====================================================================
set AI_MODEL=qwen2.5:3b
uvicorn app.main:app --reload

pause