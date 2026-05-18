# Mod Narrador IA para Minecraft

## Objetivo do Projeto
Este mod captura ações e eventos de telemetria do jogador em tempo real dentro do Minecraft e envia esses dados para uma inteligência artificial executada localmente. A IA processa os acontecimentos e gera uma resposta em formato de texto com uma personalidade sarcástica, julgadora e agressiva (sem infringir diretrizes de respeito fundamental). A resposta é convertida em fala (Text-to-Speech) através do modelo Kokoro-82M e reproduzida no jogo. Todo o fluxo ocorre em segundo plano e de forma assíncrona, garantindo humor ácido sem impactar a performance da gameplay.

## Estrutura do Repositório
O projeto está organizado na seguinte estrutura de diretórios:
- `/server-ia`: Código-fonte do servidor backend em Python, responsável pela orquestração do modelo de linguagem (LLM) e pela síntese de voz (TTS).
- `/java`: Código-fonte do mod desenvolvido em Java para o Minecraft (Fabric), encarregado de coletar os eventos do jogo e reproduzir as faixas de áudio recebidas.

---

## Cronograma de Desenvolvimento

### Fase 1: API-First, TTS e Laboratório de Persona
**Objetivo da Fase:** Estabelecer um servidor local funcional em Python que receba dados de contexto de telemetria, valide a estrutura, execute a geração de texto baseada em uma persona específica e realize a síntese e o streaming do áudio de forma ultra-rápida.

- [x] **1. Setup do Repositório**
  - [x] Criar o repositório oficial no GitHub (`Gabigoubi/narrador-ia`).
  - [x] Estruturar as pastas principais do projeto (`/server-ia` e `/java`).

- [ ] **2. Instalação do Cérebro (Ollama - Executar no PC)**
  - [ ] Baixar e instalar o ecossistema Ollama no ambiente de desenvolvimento.
  - [ ] Baixar um modelo de linguagem leve e quantizado (`ollama run qwen2.5:3b`).
  - [ ] Validar o comportamento do modelo via prompt de comando para analisar o impacto no consumo de memória RAM do sistema.

- [ ] **3. Setup do Ambiente Python (Executar no PC)**
  - [ ] Criar o ambiente virtual isolado para o ecossistema do servidor (`python -m venv venv`).
  - [ ] Ativar o ambiente virtual e realizar a instalação das dependências base via `requirements.txt`.
  - [x] Criar o arquivo de mapeamento de dependências (`requirements.txt`).

- [ ] **4. Instalação das Cordas Vocais (Kokoro-82M - Executar no PC)**
  - [ ] Clonar ou baixar os arquivos de arquitetura do Kokoro-82M juntamente com o arquivo de modelo de voz masculina em português.
  - [ ] Desenvolver um script Python simples para validar de forma isolada a conversão correta de um texto estático para uma sequência de bytes de áudio.

- [/] **5. Criação da API de Alta Performance (FastAPI)**
  - [x] Desenvolver o arquivo principal da aplicação (`main.py`).
  - [x] Criar uma rota de checagem simples (`GET /health`) para validação rápida de inicialização do servidor no fim de semana.
  - [x] Criar o schema de validação de dados usando `Pydantic` para garantir que o JSON enviado pelo Minecraft esteja correto.
  - [x] Implementar o endpoint principal do tipo POST sob o caminho `/narrate`.
  - [x] Integrar e encadear o fluxo de dados em memória: Recebimento do JSON -> Formatação do Prompt -> Chamada à API local do Ollama -> Captura do texto -> Encaminhamento ao Kokoro-82M -> Retorno do áudio via `StreamingResponse` (sem salvar arquivo em disco).

- [x] **6. Laboratório de Persona e Otimização de Latência**
  - [x] Definir o `System Prompt` detalhado contendo as regras de conduta da IA de 5ª série de SP (comportamento agressivo, julgador e sarcástico, com gírias nativas e limite de 50 palavras).
  - [x] Simular disparos de testes com payloads reais de eventos do Minecraft (Morte, Chat, Diamante com picareta de pedra).
  - [x] Ajustar a engenharia de prompt até garantir alta dinamicidade nas respostas.

---

### Fase 2: O Mod em Java (Conexão e Áudio)
**Objetivo da Fase:** Desenvolver a estrutura do Mod para Minecraft utilizando Java (Fabric), focando na captura limpa de eventos e na execução assíncrona do áudio recebido para evitar congelamentos na gameplay.

- [/] **1. Setup do Projeto Minecraft**
  - [x] Configurar o arquivo de manifesto do Mod (`fabric.mod.json`) cravado para Minecraft 1.21.1 e Java 21.
  - [x] Configurar a automação do Gradle (`build.gradle` e `settings.gradle`) para gerenciar as dependências do Fabric.
  - [x] Criar a classe principal de inicialização (`NarradorIAMod.java`) com a assinatura única do pacote `com.gabigoubi.narradoria`.
  - [ ] Validar a compilação do mod no jogo rodando o ambiente pela primeira vez no PC.

- [/] **2. Captura de Eventos de Telemetria**
  - [x] Implementar os listeners de eventos base do jogador (`GameEventListener.java`) focando em morte (`AFTER_RESPAWN`) e chat do jogo (`CHAT_MESSAGE`).
  - [x] Estruturar os dados coletados no formato exato do JSON esperado pela API Python e converter objetos de texto usando `.getString()`.

- [/] **3. Conexão Assíncrona via HTTP**
  - [x] Implementar o cliente HTTP nativo do Java (`HttpAssistant.java`) utilizando requisições assíncronas (`sendAsync`).
  - [x] Garantir que o envio dos dados para a API Python ocorra via `CompletableFuture` (Thread separada) para evitar congelamentos (*stuttering*) na tela do jogador.

- [/] **4. Reprodutor de Áudio In-Game**
  - [x] Criar o sistema capaz de receber o fluxo de bytes de áudio binários brutos (`AudioPlayer.java`).
  - [x] Desenvolver a abertura de linha de áudio nativa do Java (`SourceDataLine`) a 24000Hz em background para reproduzir o áudio sem travar a gameplay.

---

### Fase 3: Polimento, Debug de Campo e Integração Total
**Objetivo da Fase:** Unificar os dois sistemas no PC, ajustar o balanço de áudio e caçar os bugs reais de runtime durante a gameplay.

- [ ] **1. Teste de Estresse em Gameplay (Executar no PC)**
  - [ ] Jogar por 30 minutos monitorando o consumo de RAM simultâneo (Minecraft + Ollama + FastAPI).
  - [ ] Tratar possíveis erros de concorrência e exceções de rede (`try/except` no Python e `.exceptionally` no Java) caso múltiplos eventos aconteçam ao mesmo tempo no jogo.

- [ ] **2. Ajustes Finais de Áudio (Executar no PC)**
  - [ ] Configurar os canais de áudio para que a voz do narrador seja clara mesmo com sons de explosões ou música de fundo do jogo.