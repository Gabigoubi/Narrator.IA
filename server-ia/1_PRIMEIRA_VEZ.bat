@echo off
title Instalacao do Narrador IA - Passo 1
cd /d "%~dp0"
color 0F

echo ====================================================================
echo  BEM-VINDO A INSTALACAO DO NARRADOR IA!
echo ====================================================================
echo.
echo O Windows abrira seu navegador para o download de dois programas:
echo 1. Ollama (Para rodar a Inteligencia Artificial)
echo 2. Python 3.11 (Para rodar o motor de voz)
echo.
echo [MUITO IMPORTANTE]: Na instalacao do Python, certifique-se de marcar
echo a caixa "Add Python 3.11 to PATH" logo na primeira tela!
echo.
pause
start "" "https://ollama.com/download/windows"
start "" "https://www.python.org/downloads/release/python-3119/"
echo.
echo ====================================================================
echo APOS INSTALAR OS DOIS PROGRAMAS, FECHE ESTA JANELA E ABRA NOVAMENTE.
echo O terminal precisa ser reiniciado para reconhecer o Python no PATH.
echo ====================================================================
echo Se voce ja instalou e reabriu este arquivo, aperte qualquer tecla...
pause >nul

echo.
echo [1/3] Verificando instalacao do Python...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    color 0C
    echo.
    echo [ERRO CRITICO] Python nao encontrado ou PATH nao configurado.
    echo Se a Microsoft Store abriu, feche-a. Voce precisa instalar pelo site.
    echo Feche esta janela, reinstale o Python marcando "Add to PATH" e tente de novo.
    pause
    exit
)

echo [2/3] Criando o ambiente virtual (venv)...
python -m venv venv
if %errorlevel% neq 0 (
    color 0C
    echo.
    echo [ERRO CRITICO] Falha ao criar o ambiente virtual do Python.
    echo Ocorreu um erro de permissao no seu Windows.
    pause
    exit
)

echo [3/3] Ativando ambiente e instalando dependencias (Isso pode demorar)...
call .\venv\Scripts\activate.bat
pip install -r requirements.txt > install_log.txt 2>&1
if %errorlevel% neq 0 (
    color 0C
    echo.
    echo [ERRO CRITICO] Falha ao baixar os pacotes do Python.
    echo Um arquivo chamado 'install_log.txt' foi criado nesta pasta.
    echo Envie este arquivo na aba de Issues do GitHub ou no CurseForge para suporte.
    pause
    exit
)

color 0A
echo.
echo ====================================================================
echo  INSTALACAO CONCLUIDA COM SUCESSO!
echo ====================================================================
echo Voce ja pode fechar este terminal.
echo Para jogar, execute o arquivo 2_INICIAR_IA.bat.
echo.
pause
