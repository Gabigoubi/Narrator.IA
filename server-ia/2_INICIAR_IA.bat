@echo off
title Narrador IA - Servidor de Conexao
cd /d "%~dp0"
color 0F

echo ====================================================================
echo  INICIANDO O SISTEMA DO NARRADOR IA...
echo ====================================================================
echo.

:: 1. Verificacao de Instalacao Previa
if not exist "venv\Scripts\activate.bat" (
    color 0C
    echo [ERRO CRITICO] O ambiente virtual nao foi encontrado.
    echo Voce pulou uma etapa! Execute o arquivo '1_PRIMEIRA_VEZ.bat' primeiro.
    echo.
    pause
    exit
)

:: 2. Ativacao do Ambiente
echo [1/3] Ativando o ambiente virtual Python...
call venv\Scripts\activate.bat

:: 3. Verificacao do Ollama (Ping local)
echo [2/3] Procurando o motor do Ollama no seu PC...
curl -s http://localhost:11434/api/tags >nul
if %errorlevel% neq 0 (
    color 0C
    echo.
    echo [ERRO CRITICO] O Ollama nao esta rodando!
    echo O Narrador precisa do Ollama aberto para funcionar.
    echo Procure por "Ollama" no menu Iniciar do Windows e abra ele.
    echo Depois, feche esta janela e tente iniciar de novo.
    echo.
    pause
    exit
)

echo [3/3] Iniciando o servidor de inteligencia artificial...
echo.
color 0A
echo ====================================================================
echo  TUDO PRONTO! O PAINEL DE DEBUG SERA CARREGADO ABAIXO.
echo  Deixe esta janela aberta enquanto estiver jogando.
echo ====================================================================
echo.

:: 4. Execucao do Servidor FastAPI
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload

:: 5. Tratamento de Crash (Se o uvicorn fechar/quebrar, o script chega aqui)
color 0C
echo.
echo ====================================================================
echo [ALERTA] O servidor foi encerrado ou sofreu um erro critico.
echo Role o terminal para cima e tire um print do erro vermelho.
echo Envie na aba de Issues do GitHub ou no CurseForge para suporte.
echo ====================================================================
pause
