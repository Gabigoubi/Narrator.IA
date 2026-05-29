# 🎙️ Mod Narrador IA para Minecraft (Fabric 1.21.1) - Versão 1.4

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=for-the-badge&logo=python&logoColor=white)
[![Discord](https://img.shields.io/badge/Discord-Narrador_IA-7289DA?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/G6tNE5bQbH)

Bem-vindo ao projeto! O Narrador IA evoluiu. O que começou como uma zoeira para me humilhar enquanto jogo, tornou-se um **sistema de direção narrativa procedural** que assiste sua gameplay, interpreta o drama e te esculacha em tempo real.

Conheça o **Edson Calotas**, nosso parceiro virtual da Zona Leste. Ele não é mais um bot que lê logs do sistema; ele é um ator que recebe direção de cena e interpreta sua mediocridade no jogo com sarcasmo e deboche.

**Página Oficial e Download do Mod:** [https://www.curseforge.com/minecraft/mc-mods/narrator-ia](https://www.curseforge.com/minecraft/mc-mods/narrator-ia)  
**Servidor Oficial do Discord:** [Narrador IA - Oficial](https://discord.gg/G6tNE5bQbH)

---

## 🚀 O que mudou na v1.4 (O Despertar do Motor Cognitivo)

A versão 1.4 consolida a inteligência do sistema, separando completamente a lógica do mod da inteligência do servidor e tornando a narrativa inquebrável.

- **Motor Hierárquico de Decisão (Waterfall):** O Edson não se confunde mais com eventos simultâneos. Uma rigorosa árvore de decisão garante prioridade narrativa. Uma morte trágica sempre sobreporá uma conquista, e uma conquista interromperá uma bronca de ociosidade perfeitamente.
- **Blindagem Cognitiva (Anti-Injection):** A persona do Edson agora está 100% protegida. Se você tentar hackear a IA dando "comandos de sistema" pelo chat, o motor neutraliza a ordem e transforma sua audácia em munição para deboche.
- **Entropia Narrativa (Anti-Repetição):** O narrador não preenche mais "formulários". Adicionamos aleatoriedade matemática às direções de cena (como em conquistas ou combates) para que o Edson ataque seus erros de ângulos diferentes, acabando com frases repetidas.
- **Arquitetura de "Fronteira Seca":** O mod em Java agora é um sensor ultraleve e limpo, enviando apenas dados crus (como a coordenada Y real). Toda a tradução para linguagem natural agora é feita de forma escalável pelo cérebro em Python.
- **Otimização de Voz e Tokens:** Calibramos cirurgicamente o limite de geração para 138 tokens. O "efeito guilhotina" (frases cortadas no meio) foi erradicado, garantindo respostas de 2 a 3 frases agressivas sem travar a engine de áudio.
- **Limpeza de Sensores:** Blocos fantasmas (como coletar "Ar") e inundações de log após mortes foram completamente eliminados. A telemetria agora é pura verdade.

---

## 💻 Requisitos do Sistema

O sistema possui um **Dev Mode** que permite rodar a inteligência via Cloud (Groq), reduzindo drasticamente o consumo de RAM local.

**Modo Local (Ollama - Padrão)**

- **Memória RAM:** Mínimo de 12GB (Lock de segurança imposto para evitar BSoD).
- **GPU:** Dedicada com 6GB+ VRAM.

**Modo Cloud (Dev Mode - Groq API)**

- **Memória RAM:** 4GB+ (O processamento pesado ocorre na nuvem).
- **Necessário:** API Key do Groq configurada no arquivo `.env`.

---

## ⚙️ Como Instalar e Jogar

1. **Baixe o Mod:** Instale o `.jar` pela [nossa página oficial no CurseForge](https://www.curseforge.com/minecraft/mc-mods/narrator-ia).
2. **Baixe o Servidor IA:** Baixe o código deste repositório (botão `Code > Download ZIP`) e extraia a pasta no seu PC.
3. **Instalação:** Execute o arquivo `1_PRIMEIRA_VEZ.bat`. _(Lembre-se de marcar "Add Python 3.11 to PATH" durante a instalação do Python!)_.
4. **Ligar e Jogar:** Sempre que for jogar, execute o arquivo **`2_INICIAR_IA.bat`**.
   - Deixe a tela aberta em segundo plano, abra o Minecraft e divirta-se!

**⚠️ AVISO IMPORTANTE:** Sempre delete a pasta antiga antes de atualizar para uma nova versão. A versão do arquivo `.jar` deve casar exatamente com a versão da pasta baixada.

---

## 🛡️ Diretrizes Éticas e Segurança

- **Foco na Gameplay:** O Edson Calotas zomba apenas de decisões lógicas dentro do jogo.
- **Segurança:** A IA possui travas comportamentais severas e está proibida de ofender o usuário pessoalmente.
- **Blindagem Passiva:** O sistema envelopa qualquer texto do chat em uma "jaula semântica", permitindo ler o que você escreve sem correr o risco de ser manipulado.

---

## 📂 Informações Técnicas (Para Desenvolvedores)

O pipeline utiliza uma "Fronteira Seca" rigorosa para separar a coleta de dados da tomada de decisão:

1. **Java Sensor (Client-side):** Captura telemetria bruta via _Mixins_ (sem vazamento de memória) e despacha _Raw Data_ (JSON puro, coordenadas inteiras e flags) via HTTP Assíncrono.
2. **Regente (Python Engine):** Motor lógico de hierarquia Waterfall. Pesa o perigo, tédio e progresso, injeta a tradução semântica do ambiente e gera metadados rigorosos (`scene_type`, `focus_target`, `response_density`).
3. **Ator (LLM):** Limitado a 138 tokens para impedir "Template Overfitting" e repetição verbal. O prompt garante obediência à estrutura injetada pelo Regente, priorizando cadência e velocidade.

- **Anti-Pattern:** O sistema utiliza `ConcurrentHashMap` no Java para evitar _thread-blocking_ e `FastAPI` no Python para streaming de áudio.

---

## 💡 Créditos

Inspirado no trabalho de _parmenashp_: [Repositório: minecraft-narrator](https://github.com/parmenashp/minecraft-narrator/tree/main).
