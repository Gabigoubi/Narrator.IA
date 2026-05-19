@echo off
title Instalacao do Narrador IA - Passo 1
echo ====================================================================
echo 🛠️ BEM-VINDO A INSTALACAO DO NARRADOR IA!
echo ====================================================================
echo.
echo O Windows vai abrir seu navegador agora com dois sites.
echo.
echo PASSO A: Instale o Ollama.
echo PASSO B: Instale o Python 3.11 (MUITO IMPORTANTE: Na primeira tela de
echo instalacao do Python, marque a caixa "Add Python 3.11 to PATH" no rodape!)
echo.
pause
start https://ollama.com/download/windows
start https://www.python.org/downloads/release/python-3119/
echo.
echo ====================================================================
echo ⏳ QUANDO TERMINAR DE INSTALAR OS DOIS, APERTE QUALQUER TECLA AQUI...
echo ====================================================================
pause >nul
echo.
echo Criando o cérebro do projeto (Ambiente Virtual)...
python -m venv venv

echo Ativando...
call .\venv\Scripts\activate.bat

echo Instalando os pacotes e dependencias...
pip install -r requirements.txt

echo.
echo ====================================================================
echo ✅ INSTALACAO FINALIZADA COM SUCESSO!
echo ====================================================================
echo Pode fechar esta janela.
echo Nas proximas vezes, use "2_Iniciar_PC_Fraco_8GB_RAM.bat" ou "2_Iniciar_PC_Forte.bat_16GB_RAM".
pause