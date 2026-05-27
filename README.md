# 🎙️ Mod Narrador IA para Minecraft (Fabric 1.21.1) - Versão 1.3.1

Bem-vindo ao projeto! O Narrador IA evoluiu. O que começou como uma zoeira para me humilhar enquanto jogo, tornou-se um **sistema de direção narrativa procedural** que assiste sua gameplay, interpreta o drama e te esculacha em tempo real.

Conheça o **Edson Calotas**, nosso parceiro virtual da Zona Leste. Ele não é mais um bot que lê logs do sistema; ele é um ator que recebe direção de cena e interpreta sua mediocridade no jogo com sarcasmo e deboche.

**Página Oficial e Download do Mod:** [https://www.curseforge.com/minecraft/mc-mods/narrator-ia](https://www.curseforge.com/minecraft/mc-mods/narrator-ia)
**Servidor Oficial do Discord:** [Narrador IA - Oficial](https://discord.gg/G6tNE5bQbH)

---

## 🚀 O que mudou na v1.3.1 (Estabilidade e Blindagem)

A versão 1.3.1 foca em tornar o narrador mais inteligente, resistente a abusos e totalmente integrado ao gameplay.

* **Arquitetura Cognitiva (Regente):** O sistema agora possui uma "Zona de Quarentena" para o chat, tornando o Edson imune a ataques de *Prompt Injection* (tentativas de hackear a IA).
* **Sensores Avançados:** Através de *Mixins*, capturamos agora ações de **Coletar, Dropar e Craftar** itens em tempo real, agrupando tudo para a IA julgar o lixo que você acumula.
* **Modo Silencioso (Blindagem Divina):** O Edson agora ignora jogadores no **Modo Criativo** ou **Espectador**. Pode construir em paz, o narrador só julga a sobrevivência real!
* **Performance:** Implementamos um sistema de fila que descarta requisições enquanto o áudio estiver em reprodução, evitando *spam* e garantindo que o jogo não engasgue.
* **Novidade Secreta (Easter Egg):** Sobreviva por 15 minutos e descubra uma trilha sonora exclusiva que o Edson preparou para os fortes. 🤫🎶

---

## 💻 Requisitos do Sistema

O sistema possui um **Dev Mode** que permite rodar a inteligência via Cloud (Groq), reduzindo drasticamente o consumo de RAM local.

**Modo Local (Ollama - Padrão)**
* **Memória RAM:** Mínimo de 12GB (Lock de segurança imposto para evitar BSoD).
* **GPU:** Dedicada com 6GB+ VRAM.

**Modo Cloud (Dev Mode - Groq API)**
* **Memória RAM:** 4GB+ (O processamento pesado ocorre na nuvem).
* **Necessário:** API Key do Groq configurada no arquivo `.env`.

---

## ⚙️ Como Instalar e Jogar

1. **Baixe o Mod:** Instale o `.jar` pela [nossa página oficial no CurseForge](https://www.curseforge.com/minecraft/mc-mods/narrator-ia).
2. **Baixe o Servidor IA:** Baixe o código deste repositório (botão `Code > Download ZIP`) e extraia a pasta no seu PC.
3. **Instalação:** Execute o arquivo `1_PRIMEIRA_VEZ.bat`. *(Lembre-se de marcar "Add Python 3.11 to PATH" durante a instalação do Python!)*.
4. **Ligar e Jogar:** Sempre que for jogar, execute o arquivo **`2_INICIAR_IA.bat`**.
   * Deixe a tela aberta em segundo plano, abra o Minecraft e divirta-se!

**⚠️ AVISO IMPORTANTE:** Sempre delete a pasta antiga antes de atualizar para uma nova versão. A versão do arquivo `.jar` deve casar exatamente com a versão da pasta baixada.

---

## 🛡️ Diretrizes Éticas e Segurança

* **Foco na Gameplay:** O Edson Calotas zomba apenas de decisões lógicas dentro do jogo.
* **Segurança:** A IA possui travas comportamentais severas e está proibida de ofender o usuário pessoalmente.
* **Blindagem:** O sistema possui um filtro que neutraliza tentativas de manipular a IA via chat do Minecraft.

---

## 📂 Informações Técnicas (Para Desenvolvedores)

O pipeline é dividido em camadas para resolver latência e alucinação:

1. **Java Sensor:** Captura telemetria bruta via *Mixins* e eventos de servidor.
2. **Regente (Python Engine):** Motor determinístico que gera metadados de direção (`scene_type`, `tone`, `absurdity_index`).
3. **Ator (LLM):** Prompt formatado em estrutura: `[ESTILO] + [CENÁRIO] + [ALVO DO DEBOCHE]`.

* **Anti-Pattern:** O sistema utiliza `ConcurrentHashMap` no Java para evitar *thread-blocking* e `FastAPI` no Python para streaming de áudio assíncrono.

---

## 💡 Créditos

Inspirado no trabalho de *parmenashp*: [Repositório: minecraft-narrator](https://github.com/parmenashp/minecraft-narrator/tree/main).
