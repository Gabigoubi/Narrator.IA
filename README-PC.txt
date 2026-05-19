=====================================================================
GUIA DE INSTALAÇÃO DO BETA - NARRADOR IA (1.21.1 FABRIC)
​📢 AVISO RECONFORTANTE: Eu sei que olhando assim parece muita coisa, mas esse processo de configuração longa só precisa ser feito UMA VEZ. Depois disso, para jogar é só dar dois comandos rápidos. Além disso, eu vou acompanhar cada um dos 3 testadores de perto! Se travar em qualquer passo ou surgir qualquer dúvida, me dá um toque que eu te ajudo a resolver na hora.
​PRÉ-REQUISITO OBRIGATÓRIO: Instalar o Python
​A Inteligência Artificial precisa do Python instalado no seu sistema para funcionar.
​Acesse o site oficial do Python: https://www.python.org/downloads/
​Procure pela versão Python 3.11 (ou superior) e baixe o instalador para Windows.
​⚠️ ATENÇÃO MÁXIMA NA INSTALAÇÃO: Na primeiríssima tela do instalador, você OBRIGATORIAMENTE precisa marcar a caixinha que diz "Add Python to PATH" lá embaixo antes de clicar em "Install Now". Se não marcar isso, o mod não funciona!
​PASSO 1: INSTALAR O MOTOR DA IA (Ollama)
​O Ollama é o programa gratuito que vai rodar os modelos de Inteligência Artificial direto na sua máquina.
​Acesse o site oficial: https://ollama.com e baixe a versão para Windows.
​Instale como um programa comum. Após acabar, veja se o ícone do Ollama (uma lhamazinha) apareceu perto do relógio do Windows.
​PASSO 2: BAIXAR O MODELO (Escolha o seu Cérebro)
​Acesse o link do projeto no GitHub para pegar os arquivos do servidor: https://github.com/Gabigoubi/narrador-ia
​Clique no botão verde "<> Code" e depois em "Download ZIP".
​Extraia essa pasta na sua Área de Trabalho. Abra a pasta e entre em server-ia.
​Clique com o botão direito do mouse em um espaço em branco dentro da pasta server-ia e selecione "Abrir no Terminal".
​Agora, escolha o comando de acordo com o seu computador:
​Opção A (Para PC Médio/Padrão): Se você quer velocidade sem pesar nada, rode o comando:
ollama run qwen2.5:3b
​Opção B (Para PC Monstro/Placa de Vídeo Forte): Se você tem um hardware brabo (16GB+ de RAM e boa GPU) e quer respostas complexas e ainda mais caóticas, rode o modelo Mistral 7B:
ollama run mistral
​Espere o download do modelo terminar. Digite um "oi" para testar, e depois digite /exit para liberar o terminal. Não feche a janela!
​PASSO 3: CONFIGURAR O SERVIDOR DE VOZ (Primeira Vez)
​Nesse mesmo terminal que ficou aberto na pasta server-ia, cole estes comandos na ordem exata:
​Crie o ambiente isolado do Python:
python -m venv venv
​Ative o ambiente virtual (vai aparecer um (venv) verde no terminal):
.\venv\Scripts\Activate.ps1
(Se o Windows der erro de permissão vermelha, mude a política abrindo o PowerShell como Admin e digitando: Set-ExecutionPolicy Unrestricted, depois tente ativar de novo).
​Instale as bibliotecas de som com a venv ativada:
pip install -r requirements.txt
​Ligue o servidor de áudio:
uvicorn app.main:app --reload
(Abra no navegador http://localhost:8000/health para conferir se aparece {"status": "healthy"}). Mantenha essa tela aberta!
​PASSO 4: INSTALAR O MOD NO MINECRAFT
​Certifique-se de que você tem o Fabric Loader 1.21.1 instalado no seu Launcher.
​Baixe o arquivo do mod direto na nossa página oficial do CurseForge (vou te passar o link privado).
​Coloque o arquivo .jar baixado dentro da sua pasta de mods do Minecraft (pressione Win + R, digite %appdata%\.minecraft\mods e dê Enter).
​Abra o jogo!
​=====================================================================
🔄 COMO JOGAR NAS PRÓXIMAS VEZES (MUITO MAIS SIMPLES!)
​Nas próximas vezes que for jogar Minecraft com o Narrador, você não precisa baixar nada. Só faça isso:
​Abra a pasta server-ia, clique com o botão direito e vá em "Abrir no Terminal".
​Ative o ambiente digitando: .\venv\Scripts\Activate.ps1
​Ligue o servidor digitando: uvicorn app.main:app --reload
​Abra o seu Minecraft e se divirta!