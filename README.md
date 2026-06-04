<div align="center">
  <h1>🎙️ Narrador IA para Minecraft</h1>
  <h3>Um Sistema de Direção Narrativa Reativo e em Tempo Real (Edson Calotas)</h3>
  
  
  <p align="center">
    <img src="https://img.shields.io/badge/Versão-v1.6.0_(A_Definitiva)-ED8B00?style=for-the-badge" alt="Versão v1.6.0" />
    <img src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft" />
    <img src="https://img.shields.io/badge/Fabric-DBD8CD?style=for-the-badge&logo=fabric&logoColor=333333" alt="Fabric" />
  </p>

  <p align="center">
    <a href="https://www.curseforge.com/minecraft/mc-mods/narrator-ia">
      <img src="https://img.shields.io/badge/Download-CurseForge-F16436?style=for-the-badge&logo=curseforge&logoColor=white" alt="CurseForge" />
    </a>
    <a href="https://discord.gg/G6tNE5bQbH">
      <img src="https://img.shields.io/badge/Comunidade-Discord-7289DA?style=for-the-badge&logo=discord&logoColor=white" alt="Discord" />
    </a>
  </p>
</div>

---

Bem-vindo ao projeto! O Narrador IA evoluiu. O que começou como uma zoeira para me humilhar enquanto jogo, tornou-se um **sistema de direção narrativa** que assiste sua gameplay, interpreta e te esculacha em tempo real.

Conheça o **Edson Calotas**, nosso parceiro virtual da Zona Leste. Ele não é mais um bot que lê logs do sistema; ele é um ator que recebe direção de cena e interpreta sua mediocridade no jogo com sarcasmo, deboche e ameaças veladas.

---

## 🚀 O Que Há de Novo na v1.6.0 (A Definitiva)

Esta versão coroa o projeto com uma arquitetura de alta performance, cruzamento de dados em tempo real e proteção anti-crash.

*   **Motor Multicontexto (Rules Engine):** O cérebro do Edson deixou de ser linear. Agora ele percebe múltiplas ações simultâneas. Se você tomar dano de um monstro, minerar pedra e falar no chat na mesma janela de tempo, a IA integrará os três cenários em uma única fala orgânica e debochada.
*   **Buffer Inteligente (Early Flush):** Falar no chat agora gera uma resposta quase instantânea. Um gatilho isolado detecta quando você digita, concatena suas mensagens por 10 segundos e força o disparo imediato da telemetria, zerando a latência sem perder as ações do ambiente.
*   **Handshake Defensivo (Graceful Degradation):** Chega de telas de erro! O mod agora verifica automaticamente a compatibilidade de versão no momento do *boot*. Se você esquecer de atualizar o servidor Python, o jogo inicia normalmente, desativa a telemetria com segurança e emite um alerta elegante no seu chat.

---

## 🧰 Stack Tecnológico & Fronteira Seca

<div align="center">

<img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/>
<img src="https://img.shields.io/badge/Python-3.11-3776AB?style=for-the-badge&logo=python&logoColor=white" alt="Python"/>
<img src="https://img.shields.io/badge/FastAPI-005571?style=for-the-badge&logo=fastapi&logoColor=white" alt="FastAPI"/>
<img src="https://img.shields.io/badge/Ollama-000000?style=for-the-badge&logo=ollama&logoColor=white" alt="Ollama"/>
<img src="https://img.shields.io/badge/Groq-F55036?style=for-the-badge&logoColor=white" alt="Groq"/>

</div>

<br>

O pipeline utiliza uma **"Fronteira Seca"** rigorosa para separar a coleta de dados da tomada de decisão:

1.  **Sensor Java (Client-Side):** Captura telemetria bruta via *Mixins* e *Event Listeners* sem vazar memória. Despacha *Raw Data* (JSON puro, coordenadas inteiras e flags) via HTTP Assíncrono utilizando `ConcurrentHashMap` para evitar *thread-blocking*.
2.  **Regente Python (Backend):** Motor lógico guiado a dados. Pesa o perigo, tédio e progresso, injeta a tradução semântica do ambiente e gera metadados estritos (`scene_type`, `focus_target`, `response_density`).
3.  **Ator LLM (Inferência):** Guiado por *Few-Shot Prompting* e limites seguros de tokens, a IA recebe uma descrição situacional do absurdo ocorrido e gera a resposta operando com uma janela estreita de contexto, erradicando alucinações narrativas.

---

## 💻 Requisitos do Sistema

O sistema possui um **Dev Mode** que permite rodar a inteligência via Cloud (Groq), reduzindo drasticamente o consumo de RAM local.

### Modo Local (Ollama - Padrão)
*   **Memória RAM:** Mínimo de 12GB (Lock de segurança imposto para evitar BSoD).
*   **GPU:** Dedicada com 6GB+ VRAM.

### Modo Cloud (Dev Mode - Groq API)
*   **Memória RAM:** 4GB+ (O processamento pesado ocorre na nuvem).
*   **Necessário:** API Key do Groq configurada no arquivo `.env`.

---

## ⚙️ Como Instalar e Jogar

Como o projeto é um ecossistema de duas partes, você precisa rodar o servidor Python e o Mod Java simultaneamente.

1.  **Baixe o Mod:** Instale o arquivo `.jar` pela [página oficial no CurseForge](https://www.curseforge.com/minecraft/mc-mods/narrator-ia) e coloque na pasta `mods` do seu Minecraft 1.21.1 (Fabric).
2.  **Baixe o Servidor IA:** Baixe o código deste repositório (botão `Code > Download ZIP`) e extraia a pasta no seu PC.
3.  **Instalação Inicial:** Execute o arquivo `1_PRIMEIRA_VEZ.bat`. *(Lembre-se de marcar "Add Python 3.11 to PATH" durante a instalação do Python!)*.
4.  **Ligar e Jogar:** Sempre que for jogar, execute o arquivo `2_INICIAR_IA.bat`. Deixe a tela aberta em segundo plano e abra o Minecraft.

> ⚠️ **AVISO IMPORTANTE:** A versão do arquivo `.jar` deve casar exatamente com a versão da pasta Python baixada. O nosso sistema de *Handshake* bloqueará a execução se as versões divergirem.

---

## 🛡️ Diretrizes Éticas e Segurança

*   **Foco na Gameplay:** O Edson Calotas zomba apenas de decisões lógicas dentro do jogo.
*   **Segurança:** A IA possui travas comportamentais severas e está proibida de ofender o usuário pessoalmente.
*   **Blindagem Passiva:** O sistema envelopa qualquer texto do chat em uma "jaula semântica", permitindo ler o que você escreve sem correr o risco de que o motor seja manipulado por comandos no jogo.

---

## 💡 Créditos

Inspirado no excelente trabalho estrutural de *parmenashp*: [minecraft-narrator](https://github.com/parmenashp/minecraft-narrator/tree/main).
