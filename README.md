
---

# 🎙️ Mod Narrador IA para Minecraft (Fabric 1.21.1) - Versão 1.3

Bem-vindo ao projeto! O Narrador IA evoluiu. O que começou como uma zoeira para me humilhar enquanto jogo, tornou-se um **sistema de direção narrativa procedural** que assiste sua gameplay, interpreta o drama e te esculacha em tempo real.

Conheça o **Edson Calotas**, nosso parceiro virtual da Zona Leste. Ele não é mais um bot que lê logs do sistema; ele é um ator que recebe direção de cena e interpreta sua mediocridade no jogo com sarcasmo e deboche.

**Página Oficial e Download do Mod:** [Narrador IA no CurseForge](https://www.curseforge.com/minecraft/mc-mods/narrator-ia)

---

## 🚀 O que mudou na v1.3 (Arquitetura Cognitiva)

Diferente da v1.0, onde a IA era um simples chatbot, a v1.3 introduziu o **Regente (Drama Engine)**.

* **Behavior Orchestration:** Paramos de fazer *Prompt Engineering* complexo e passamos a usar "Direção de Atuação".
* **Regente Determinístico:** Um pipeline em Python que analisa a telemetria (combate, tédio, rotina) e injeta "Intent Tags" (tipo de cena, tom, e densidade de fala).
* **Cloud & Local Support:** Agora suporta inferência via **Groq Cloud (Llama 3.3 70B)** para uma experiência de nível AAA, ou execução 100% local via Ollama.
* **Memória de Punchlines:** O sistema não lembra mais do que você fez no passado, ele lembra do que o Edson *já disse*, evitando o vício de repetição (anti-eco).

---

## 💻 Requisitos do Sistema

O sistema agora possui um **Dev Mode** que permite rodar a inteligência via Cloud (Groq), reduzindo drasticamente o consumo de RAM local.

**Modo Local (Ollama - Padrão)**

* **Memória RAM:** Mínimo de 12GB (Lock de segurança imposto para evitar BSoD).
* **GPU:** Dedicada com 6GB+ VRAM.

**Modo Cloud (Dev Mode - Groq API)**

* **Memória RAM:** 4GB+ (O processamento pesado ocorre na nuvem).
* **Necessário:** API Key do Groq configurada no arquivo `.env`.

---

## ⚙️ Como Instalar e Jogar

1. **Baixe o Mod:** Instale o `.jar` diretamente pela [nossa página oficial no CurseForge](https://www.curseforge.com/minecraft/mc-mods/narrator-ia).
2. **Baixe o Servidor IA:** Baixe o código deste repositório (clique no botão verde `Code > Download ZIP`) e extraia a pasta no seu PC.
3. **Primeira Instalação:** Dê dois cliques no arquivo `1_PRIMEIRA_VEZ.bat` e siga as instruções na tela. *(Regra de Ouro: Lembre-se de marcar a caixa "Add Python 3.11 to PATH" durante a instalação do Python!)*
4. **Ligar e Jogar:** Sempre que for jogar, abra a pasta baixada e execute o arquivo **`2_INICIAR_IA.bat`**.
* Deixe a tela preta minimizada rodando em segundo plano, abra o Minecraft e divirta-se!



---

## 🛡️ Diretrizes Éticas e Segurança

* **Foco na Gameplay:** O Edson Calotas vai zombar estritamente das suas **decisões lógicas dentro do jogo** (ex: quebrar blocos com a mão tendo a ferramenta certa na hotbar, passar fome tendo comida, ignorar perigos óbvios).
* **Segurança:** A IA possui travas comportamentais severas e está terminantemente proibida de proferir qualquer tipo de ofensa pessoal ou preconceituosa.
* **Vacina contra Injeção de Prompt:** O sistema foi blindado contra hacks de contexto. Qualquer tentativa de enviar comandos via chat do Minecraft para "controlar" a IA será ignorada.

---

## 💡 Créditos e Inspiração

A ideia central e a base da arquitetura original deste mod foram inspiradas no incrível trabalho de *parmenashp*:
[Repositório: minecraft-narrator](https://github.com/parmenashp/minecraft-narrator/tree/main).
Fica aqui o nosso muito obrigado e todos os créditos ao criador original por abrir as portas para essa loucura!

---

## 📂 Informações Técnicas (Para Desenvolvedores)

A v1.3 foi desenhada para resolver o problema de latência cognitiva e alucinação de modelos LLM. O pipeline é dividido em três camadas:

1. **Java Sensor (O Observador Cego):** Captura telemetria bruta e empacota eventos em buffers de 90s.
2. **Regente (O Drama Engine):** Um motor determinístico em Python que recebe o JSON do Java e gera metadados de direção:
* `scene_type` (ex: `combat_panic` vs `routine`)
* `response_density` (ex: `explosao_indignada` vs `julgamento_direto`)
* `absurdity_index` (interpretação da falha do jogador)


3. **Ator (LLM Roleplay):** O prompt de sistema é blindado contra *prompt injection*. Ele recebe um roteiro formatado: `[ESTILO OBRIGATÓRIO] + [CENÁRIO] + [ALVO DO DEBOCHE]`.

* **Anti-Pattern:** O sistema utiliza `ConcurrentHashMap` no Java para evitar thread-blocking e `FastAPI` no Python para streaming de áudio assíncrono.

---
