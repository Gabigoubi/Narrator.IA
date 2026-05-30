# 🎙️ Mod Narrador IA para Minecraft (Fabric 1.21.1) - Versão 1.5

> 🔗 **LINKS OFICIAIS**
>
> - **Download do Mod (CurseForge):** [https://www.curseforge.com/minecraft/mc-mods/narrator-ia](https://www.curseforge.com/minecraft/mc-mods/narrator-ia)
> - **Comunidade no Discord:** [https://discord.gg/G6tNE5bQbH](https://discord.gg/G6tNE5bQbH)

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

## 🚀 O que mudou na v1.5 (A Dieta de Contexto e Foco Absoluto)

A versão 1.5 traz uma revolução silenciosa na arquitetura do mod. Aplicamos uma "Dieta de Contexto" rigorosa no motor do Edson para eliminar confusões narrativas, acelerar a reatividade e forçar a inteligência artificial a ser impiedosamente direta.

- **A Dieta de Contexto:** O motor agora dispara a cada 30 segundos (antes 60s) e avalia um buffer reduzido de eventos recentes, mantendo apenas 1 entrada de memória histórica. Resultado? O Edson foca estritamente no que você acabou de fazer, sem alucinar com o passado.
- **Agrupamento Inteligente de Inventário:** O Java agora varre todo o histórico de ações não-consecutivas para agrupar o que você faz de repetitivo. Em vez de ler que você quebrou pedra várias vezes isoladas, o motor consolida tudo em um único evento ("Pedra 13 vezes"), economizando tokens e melhorando a fluidez.
- **Regente "Show, Don't Tell":** Abandonamos ordens robóticas no prompt. O motor Python agora descreve o _absurdo situacional_ (ex: "Achando que é o rei do subsolo, mas só está catando tralha"). Isso dá total liberdade criativa para a IA escolher como te ofender com base no contexto.
- **O Fim da Síndrome do "Como se":** Utilizando técnicas avançadas de _Few-Shot Prompting_, erradicamos os vícios de linguagem da LLM (como as comparações "como se fosse" ou "parece que"). O Edson agora ataca afirmando os fatos diretamente e sem rodeios.
- **Correção de Falsos-Positivos:** Atacar galinhas ou lulas não aciona mais a cena de Pânico/Combate. O motor agora é treinado para rir da sua cara apenas quando você _toma dano_ real de inimigos, além de possuir formatação natural de log anti-confusão.
- **Ajustes de Áudio e Quality of Life:** O limite de geração foi ampliado para 350 tokens, acabando de vez com frases cortadas. A memória do Edson sofre um _wipe_ completo toda vez que você loga no mundo, e o Easter Egg musical teve seu volume equalizado e tempo estendido para um ritmo natural de gameplay.

---

## 💻 Requisitos do Sistema

O sistema possui um **Dev Mode** que permite rodar a inteligência via Cloud (Groq), reduzindo drasticamente o consumo de RAM local.

### Modo Local (Ollama - Padrão)

- **Memória RAM:** Mínimo de 12GB (Lock de segurança imposto para evitar BSoD).
- **GPU:** Dedicada com 6GB+ VRAM.

### Modo Cloud (Dev Mode - Groq API)

- **Memória RAM:** 4GB+ (O processamento pesado ocorre na nuvem).
- **Necessário:** API Key do Groq configurada no arquivo `.env`.

---

## ⚙️ Como Instalar e Jogar

1. **Baixe o Mod:** Instale o `.jar` pela [nossa página oficial no CurseForge](https://www.curseforge.com/minecraft/mc-mods/narrator-ia).
2. **Baixe o Servidor IA:** Baixe o código deste repositório (botão `Code > Download ZIP`) e extraia a pasta no seu PC.
3. **Instalação:** Execute o arquivo `1_PRIMEIRA_VEZ.bat`. _(Lembre-se de marcar "Add Python 3.11 to PATH" durante a instalação do Python!)_.
4. **Ligar e Jogar:** Sempre que for jogar, execute o arquivo **`2_INICIAR_IA.bat`**.
   > _Deixe a tela aberta em segundo plano, abra o Minecraft e divirta-se!_

> **⚠️ AVISO IMPORTANTE:** Sempre delete a pasta antiga antes de atualizar para uma nova versão. A versão do arquivo `.jar` deve casar exatamente com a versão da pasta baixada.

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
3. **Ator (LLM):** Guiado por _Few-Shot Prompting_ e um limite seguro de tokens, a IA recebe uma descrição situacional do absurdo ocorrido e gera a resposta operando com uma janela estreita de contexto, impedindo _hallucinations_ narrativas.

**Anti-Pattern & Performance:** O sistema utiliza `ConcurrentHashMap` no Java para evitar _thread-blocking_ e `FastAPI` no Python para streaming de áudio.

---

## 💡 Créditos

Inspirado no trabalho de _parmenashp_: [Repositório: minecraft-narrator](https://github.com/parmenashp/minecraft-narrator/tree/main).
