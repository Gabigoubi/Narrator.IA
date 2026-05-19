@echo off
title Servidor Narrador IA (PC FORTE) - Mistral 7B
echo ====================================================================
echo 🚀 INICIANDO MODO PC FORTE (MODELO: Mistral 7B)
echo ====================================================================
echo.
echo Verificando/Baixando a IA no Ollama (Pode demorar na primeira vez)...
ollama pull mistral
echo.
echo Ligando o motor virtual...
call .\venv\Scripts\activate.bat

echo.
echo 🔥 LIGANDO O SERVIDOR DE AUDIO FASTAPI...
echo Pode abrir o seu Minecraft e jogar! (Nao feche esta janela)
echo ====================================================================
uvicorn app.main:app --reload

pause