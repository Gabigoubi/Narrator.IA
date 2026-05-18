=====================================================================
GUIA DE EXECUÇÃO DO MVP - NARRADOR IA (1.21.1 / JAVA 21)
=====================================================================

PASSO 1: CLONAR O PROJETO (No Terminal do PC)
---------------------------------------------------------------------
git clone https://github.com
cd narrador-ia


PASSO 2: CONFIGURAR O CÉREBRO (Ollama)
---------------------------------------------------------------------
1. Certifique-se de que o Ollama está aberto na barra de tarefas.
2. Abra um terminal e baixe o modelo executando:
   ollama run qwen2.5:3b
3. Faça um teste rápido digitando algo no terminal do Ollama.
4. Digite /exit para fechar o chat (mas mantenha o Ollama rodando).


PASSO 3: SUBIR O SERVIDOR PYTHON (Na pasta server-ia)
---------------------------------------------------------------------
1. Abra um terminal na pasta 'server-ia' e execute:
   python -m venv venv

2. Ative a memória virtual (Virtualenv):
   - No Windows (PowerShell): .\venv\Scripts\Activate.ps1
   - No Linux/Mac: source venv/bin/activate

3. Instale as dependências com um único comando:
   pip install -r requirements.txt

4. Ligue o servidor FastAPI:
   uvicorn app.main:app --reload

5. Teste no navegador do PC se está vivo: http://localhost:8000/health
   (Tem que aparecer: {"status": "healthy"})


PASSO 4: RODAR E ABRIR O MINECRAFT (Na pasta java)
---------------------------------------------------------------------
1. Instale o JDK 21 no seu computador (Recomendo o da Azul Zulu ou Temurin).
2. Abra a pasta 'java' dentro da sua IDE (IntelliJ IDEA recomendada).
3. Espere a IDE ler o 'build.gradle' e baixar o Minecraft 1.21.1 (pode demorar uns minutos na primeira vez).
4. No terminal da pasta 'java', execute o comando do Gradle para testar o mod:
   - No Windows: .\gradlew runClient
   - No Linux/Mac: ./gradlew runClient

=====================================================================
O jogo vai abrir com o Mod e a API conversando localmente.
=====================================================================