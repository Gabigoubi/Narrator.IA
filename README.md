# 🎙️ Mod Narrador IA para Minecraft (Fabric 1.21.1) - Versão 1.4.1

> 🔗 **LINKS OFICIAIS**
> * **Download do Mod (CurseForge):** [https://www.curseforge.com/minecraft/mc-mods/narrator-ia](https://www.curseforge.com/minecraft/mc-mods/narrator-ia)
> * **Comunidade no Discord:** [https://discord.gg/G6tNE5bQbH](https://discord.gg/G6tNE5bQbH)

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft" />
  <img src="https://img.shields.io/badge/Fabric-DBD8CD?style=for-the-badge&logo=fabric&logoColor=333333" alt="Fabric" />
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/Python-3.11-3776AB?style=for-the-badge&logo=python&logoColor=white" alt="Python" />
  <img src="https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white" alt="FastAPI" />
  <img src="https://img.shields.io/badge/Ollama-000000?style=for-the-badge&logo=ollama&logoColor=white" alt="Ollama" />
  <img src="https://img.shields.io/badge/Groq-F55036?style=for-the-badge&logoColor=white" alt="Groq" />
  <a href="https://discord.gg/G6tNE5bQbH"><img src="https://img.shields.io/badge/Discord-Narrador_IA-7289DA?style=for-the-badge&logo=discord&logoColor=white" alt="Discord" /></a>
</p>

---

Bem-vindo ao projeto! O Narrador IA evoluiu. O que começou como uma zoeira para me humilhar enquanto jogo, tornou-se um **sistema de direção narrativa** que assiste sua gameplay, interpreta e te esculacha em tempo real.

Conheça o **Edson Calotas**, nosso parceiro virtual da Zona Leste. Ele não é mais um bot que lê logs do sistema; ele é um ator que recebe direção de cena e interpreta sua mediocridade no jogo com sarcasmo, deboche e ameaças veladas.

---

## 🚀 O que mudou na v1.4.1 (A Refinação Cognitiva)

A versão 1.4.1 sela a arquitetura inteligente do sistema, otimizando o processamento da IA e garantindo fluidez total na comunicação entre o jogo e o cérebro em Python.

* **Isolamento de Concorrência (Boas-Vindas):** O evento de login agora roda em uma *thread* assíncrona blindada de 10 segundos do lado do Java. Zero risco de travamento do servidor durante o carregamento pesado do mundo.
* **Cura da Diluição de Atenção:** Reduzimos a janela de memória deslizante de 3 para 2 interações. O Edson agora foca estritamente no presente, sem alucinar ou misturar contextos antigos nas piadas novas.
* **Overhaul de Identidade e Prompt:** Removemos o lixo semântico e injetamos a verdadeira alma do personagem. O Edson usa fonética paulistana pesada ("azideia???", "ooosh") perfeitamente calibrada para o motor de TTS interpretar com naturalidade.
* **Motor Hierárquico de Decisão (Waterfall):** O Edson não se confunde mais com eventos simultâneos. Uma morte trágica sempre sobreporá uma conquista, e uma conquista interromperá uma bronca de ociosidade perfeitamente.
* **Expansão de Tokens e Fôlego:** Subimos o limite cirurgicamente para 200 tokens. O Regente agora exige de 3 a 4 frases por cena, permitindo que a IA construa discursos mais elaborados e com pausas dramáticas adequadas.
* **Blindagem Cognitiva (Anti-Injection):** Se você tentar hackear a IA dando "comandos de sistema" pelo chat, o motor neutraliza a ordem e transforma sua audácia em munição para deboche.

---

## 💻 Requisitos do Sistema

O sistema possui um **Dev Mode** que permite rodar a inteligência via Cloud (Groq), reduzindo drasticamente o consumo de RAM local.

### Modo Local (Ollama - Padrão)
* **Memória RAM:** Mínimo de 12GB (Lock de segurança imposto para evitar BSoD).
* **GPU:** Dedicada com 6GB+ VRAM.

### Modo Cloud (Dev Mode - Groq API)
* **Memória RAM:** 4GB+ (O processamento pesado ocorre na nuvem).
* **Necessário:** API Key do Groq configurada no arquivo `.env`.

---

## ⚙️ Como Instalar e Jogar

1. **Baixe o Mod:** Instale o `.jar` pela [nossa página oficial no CurseForge](https://www.curseforge.com/minecraft/mc-mods/narrator-ia).
2. **Baixe o Servidor IA:** Baixe o código deste repositório (botão `Code > Download ZIP`) e extraia a pasta no seu PC.
3. **Instalação:** Execute o arquivo `1_PRIMEIRA_VEZ.bat`. *(Lembre-se de marcar "Add Python 3.11 to PATH" durante a instalação do Python!)*.
4. **Ligar e Jogar:** Sempre que for jogar, execute o arquivo **`2_INICIAR_IA.bat`**.
   > *Deixe a tela aberta em segundo plano, abra o Minecraft e divirta-se!*

> **⚠️ AVISO IMPORTANTE:** Sempre delete a pasta antiga antes de atualizar para uma nova versão. A versão do arquivo `.jar` deve casar exatamente com a versão da pasta baixada.

---

## 🛡️ Diretrizes Éticas e Segurança

* **Foco na Gameplay:** O Edson Calotas zomba apenas de decisões lógicas dentro do jogo e do seu inventário.
* **Segurança:** A IA possui travas comportamentais severas e está proibida de ofender o usuário pessoalmente.
* **Blindagem Passiva:** O sistema envelopa qualquer texto do chat em uma "jaula semântica", separando estritamente o que é dado técnico do que é comando de instrução, eliminando o risco de sobreposição de regras.

---

## 📂 Informações Técnicas (Para Desenvolvedores)

O pipeline utiliza uma "Fronteira Seca" rigorosa para separar a coleta de dados da tomada de decisão:

1. **Java Sensor (Client-side):** Captura telemetria bruta via *Mixins* (sem vazamento de memória) e despacha *Raw Data* (JSON puro, coordenadas inteiras e flags) via HTTP Assíncrono com timers de *debounce* de 60 e 180 segundos.
2. **Regente (Python Engine):** Motor lógico de hierarquia Waterfall. Pesa o perigo, tédio e progresso, aplica entropia matemática (`random.choice`) para curar padrões repetitivos e gera metadados estruturados (`scene_type`, `focus_target`, `response_density`).
3. **Ator (LLM):** Limitado a 200 tokens e 2 memórias de interação para impedir "Template Overfitting". O prompt em conformidade crua (Markdown limpo) garante obediência instantânea à cadência exigida sem processar meta-narrativas redundantes.

**Anti-Pattern & Performance:** O sistema utiliza `ConcurrentHashMap` no Java para evitar *thread-blocking* e `FastAPI` no Python para streaming direto de pacotes WAV.

---

## 💡 Créditos

Inspirado no trabalho de *parmenashp*: [Repositório: minecraft-narrator](https://github.com/parmenashp/minecraft-narrator/tree/main).
