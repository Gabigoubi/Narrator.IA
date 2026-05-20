@echo off
title Instalacao do Narrador IA - Passo 1
cd /d "%~dp0"

echo ====================================================================
echo  BEM-VINDO A INSTALACAO DO NARRADOR IA!
echo ====================================================================
echo.
echo O Windows vai abrir seu navegador agora com dois sites.
echo.
echo PASSO A: Instale o Ollama.
echo PASSO B: Instale o Python 3.11 (MUITO IMPORTANTE: Marque a caixa "Add Python 3.11 to PATH"!)
echo.
pause
start "" "https://ollama.com/download/windows"
start "" "https://www.python.org/downloads/release/python-3119/"
echo.
echo ====================================================================
echo  ATENCAO: APOS INSTALAR O PYTHON, FECHE ESTA JANELA E ABRA NOVAMENTE
echo  PARA O WINDOWS RECONHECER A INSTALACAO.
echo ====================================================================
echo Se voce ja instalou e reabriu, aperte qualquer tecla para continuar...
pause >nul
echo.

python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERRO CRITICO] Python nao encontrado. 
    echo Feche este terminal, instale o Python marcando "Add to PATH" e abra este arquivo novamente.
    pause
    exit
)

echo Criando o cerebro do projeto (Ambiente Virtual)...
python -m venv venv

echo Ativando...
call .\venv\Scripts\activate.bat

echo Instalando os pacotes e dependencias...
pip install -r requirements.txt

echo.
echo ====================================================================
echo  INSTALACAO FINALIZADA COM SUCESSO!
echo ====================================================================
echo Pode fechar esta janela.
echo Nas proximas vezes, use Iniciar_PC_Fraco ou Iniciar_PC_Forte.
pause
