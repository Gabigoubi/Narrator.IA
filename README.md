---

# 🎙️ Mod Narrador IA para Minecraft (Fabric 1.21.1)

## 🎯 Objetivo do Projeto

Este mod captura ações e eventos de telemetria do jogador em tempo real dentro do Minecraft e envia esses dados para uma inteligência artificial executada **localmente** na sua máquina.

A IA processa os acontecimentos da gameplay (vida, fome, blocos, mortes, conquistas) e gera uma resposta em formato de texto com uma personalidade sarcástica, julgadora e com sotaque raiz de São Paulo. A resposta é convertida em fala (Text-to-Speech) através do modelo Kokoro-82M e reproduzida no jogo.

Todo o fluxo ocorre em segundo plano e de forma assíncrona, garantindo o esculacho em tempo real sem impactar a performance do jogo.

---

## 💻 Requisitos do Sistema

Como o "cérebro" da IA roda diretamente no seu computador (sem APIs pagas ou nuvem), o mod exige um pouco de hardware para processar a voz e o texto simultaneamente com o Minecraft.

### **Mínimos** *(Respostas mais curtas)*

* **SO:** Windows 10 ou 11
* **Processador:** Qualquer CPU moderna (Intel Core i3/Ryzen 3 ou superior)
* **Memória:** 8 GB de RAM
* **Placa de Vídeo:** Vídeo Integrado ou Placa de Vídeo Básica
* **Modelo IA Utilizado:** Qwen 2.5 (3B)

### **Recomendados** *(Experiência completa, ágil e zoeira extrema)*

* **SO:** Windows 10 ou 11
* **Processador:** Intel Core i5/Ryzen 5 ou superior (ex: Ryzen 7 5700)
* **Memória:** 16 GB de RAM
* **Placa de Vídeo:** Placa de Vídeo Dedicada com 8GB+ de VRAM (ex: RX 7600, RTX 3060 ou superior)
* **Modelo IA Utilizado:** Mistral (7B)

---

## ⚙️ Como Instalar e Jogar

O projeto foi pensado para ser "Plug and Play". O mod em si é gerenciado pelo CurseForge, você só precisa levantar o servidor da IA na sua máquina antes de jogar.

1. **Baixe o Mod:** Instale o mod diretamente pela nossa página oficial no **CurseForge**.
2. **Baixe o Servidor IA:** Baixe o código deste repositório (botão verde `Code` > `Download ZIP`) e extraia a pasta `server-ia` no seu PC.
3. **Primeira Instalação:** Dê dois cliques no arquivo `1_Instalar_Primeira_Vez.bat` e siga as instruções na tela. *(Lembre-se de marcar "Add Python to PATH" durante a instalação do Python!)*
4. **Ligar e Jogar:** Sempre que for jogar, abra a pasta `server-ia` e execute o `.bat` correspondente ao seu PC (`2_Iniciar_PC_Fraco_8GB_RAM.bat` ou `2_Iniciar_PC_Forte_16GB_RAM.bat`). Deixe a tela preta minimizada, abra o Minecraft pelo CurseForge e divirta-se!

---

## 📂 Estrutura do Repositório (Para Desenvolvedores)

O projeto está organizado na seguinte estrutura de diretórios para quem deseja auditar o código ou contribuir:

* `/server-ia`: Código-fonte do servidor backend em Python (FastAPI). Responsável pela orquestração do modelo de linguagem (LLM via Ollama) e pela síntese de voz (TTS via Kokoro-82M).
* `/java`: Código-fonte do mod desenvolvido em Java para o Minecraft (Fabric). Encarregado de coletar os eventos do jogo em uma "Mochila de Memória", realizar disparos assíncronos via HTTP e reproduzir as faixas de áudio recebidas limitando picos de decibéis.