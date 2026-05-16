# Mod Narrador IA para Minecraft

## Objetivo do Projeto
Este mod tem como objetivo capturar as ações e eventos de telemetria do jogador em tempo real dentro do Minecraft, enviando esses dados para uma inteligência artificial executada localmente. A IA processa os acontecimentos e gera uma resposta em formato de texto com uma personalidade sarcástica, julgadora e agressiva (porém sem infringir diretrizes de respeito fundamental). Essa resposta é convertida em fala (Text-to-Speech) através do modelo Kokoro-82M. Todo o fluxo ocorre em segundo plano e de forma assíncrona, eliminando a necessidade de comandos manuais e oferecendo uma experiência interativa humorística durante a gameplay.

## Estrutura do Repositório
O projeto está organizado na seguinte estrutura de diretórios:
- `/server-ia`: Código-fonte do servidor backend em Python, responsável pela orquestração do modelo de linguagem (LLM) e pela síntese de voz (TTS).
- `/mod-java`: Código-fonte do mod desenvolvido em Java para o Minecraft, encarregado de coletar os eventos do jogo e reproduzir as faixas de áudio recebidas.

## Fase 1: API-First, TTS e Laboratório de Persona
**Objetivo da Fase:** Estabelecer um servidor local funcional em Python que receba dados de contexto de telemetria, execute a geração de texto baseada em uma persona específica e realize a síntese do arquivo de áudio correspondente de forma rápida e otimizada.

### Lista de Tarefas (Checklist)

- [ ] **1. Setup do Repositório**
  - Criar o repositório oficial no GitHub.
  - Estruturar as pastas principais do projeto (`/server-ia` e `/mod-java`).
  - Configurar o quadro Kanban para acompanhamento progressivo das atividades.

- [ ] **2. Instalação do Cérebro (Ollama)**
  - Baixar e instalar o ecossistema Ollama no ambiente de desenvolvimento.
  - Baixar um modelo de linguagem leve e quantizado através do terminal (recomendações: `ollama run phi3` ou `qwen2.5:3b`).
  - Validar o comportamento do modelo via prompt de comando para analisar o impacto no consumo de memória RAM do sistema.

- [ ] **3. Setup do Ambiente Python**
  - Criar um ambiente virtual isolado para o ecossistema do servidor (`python -m venv venv`).
  - Ativar o ambiente virtual e realizar a instalação das dependências base necessárias: `fastapi`, `uvicorn`, e `requests`.

- [ ] **4. Instalação das Cordas Vocais (Kokoro-82M)**
  - Clonar ou baixar os arquivos de arquitetura do Kokoro-82M juntamente com o arquivo de modelo de voz masculina em português.
  - Desenvolver um script Python simples para validar de forma isolada a conversão correta de um texto estático para um arquivo de áudio no formato `.wav`.

- [ ] **5. Criação da API (FastAPI)**
  - Desenvolver o arquivo principal da aplicação (`main.py`).
  - Implementar um endpoint do tipo POST sob o caminho `/narrar`, estruturado para receber um payload JSON que simule os dados de telemetria coletados no jogo.
  - Integrar e encadear o fluxo de dados do endpoint: Recebimento do JSON -> Formatação customizada do Prompt -> Chamada à API local do Ollama -> Captura do texto gerado -> Encaminhamento do texto ao Kokoro-82M -> Retorno do arquivo de áudio gerado.

- [ ] **6. Laboratório de Persona (Ajuste Fino)**
  - Definir o `System Prompt` detalhado contendo as regras de conduta da IA (comportamento agressivo, julgador e sarcástico, evitando travas excessivas de censura padrão, porém sem ofender minorias, termos de raça ou credo).
  - Simular disparos de testes com payloads reais de eventos do Minecraft (exemplo: "Jogador quebrou bloco de diamante utilizando uma picareta de pedra").
  - Ajustar a engenharia de prompt até garantir que o tempo total de resposta de ponta a ponta seja inferior a 30 segundos e possua alto teor humorístico.
