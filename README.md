# 🎙️ Mod Narrador IA para Minecraft (Fabric 1.21.1) - Versão 1.1

Este mod traz uma inteligência artificial que assiste à sua gameplay e te esculacha em tempo real!
Conheça o **Edson Calotas**, um parceiro virtual folgado, sarcástico e debochado da Zona Leste de São Paulo que vai julgar tudo o que você faz no Minecraft, desde quebrar terra com a mão até apanhar de zumbi lerdão. Tudo isso rodando **100% localmente** no seu PC, de forma gratuita e sem depender de internet. 

**Página Oficial e Download do Mod:** [Narrador IA no CurseForge](LINK_DO_SEU_CURSEFORGE_AQUI)

## 💻 Requisitos do Sistema

Como o "cérebro" da IA roda diretamente no seu computador, o mod exige hardware específico para processar a voz e o texto simultaneamente com o Minecraft sem prejudicar a sua gameplay.

**Mínimos (Para a experiência fluida sem travamentos)**
* **SO:** Windows 10 ou 11
* **Processador:** Intel Core i5 (8ª Geração ou superior) / AMD Ryzen 5
* **Memória RAM do Sistema:** 16 GB (Reserve no máximo 4GB para o Minecraft)
* **Placa de Vídeo:** Placa de Vídeo Dedicada com **6GB+ de VRAM** (ex: RX 6600, RTX 3060 ou superior)
* **Armazenamento:** SSD NVMe obrigatório
* **Modelos IA Suportados:** Llama 3 (8B) ou Mistral (7B)

## ⚙️ Como Instalar e Jogar

O projeto foi pensado para ser "Plug and Play". O mod em si é baixado pelo CurseForge, você só precisa ligar o motor da IA na sua máquina antes de jogar.

1.  **Baixe o Mod:** Instale o `.jar` diretamente pela nossa página oficial no CurseForge.
2.  **Baixe o Servidor IA:** Baixe o código deste repositório (clique no botão verde `Code > Download ZIP`) e extraia a pasta no seu PC.
3.  **Primeira Instalação:** Dê dois cliques no arquivo `1_PRIMEIRA_VEZ.bat` e siga as instruções na tela. *(Aviso: Lembre-se de marcar a caixa "Add Python to PATH" durante a instalação do Python!)*
4.  **Ligar e Jogar:** Sempre que for jogar, abra a pasta baixada e execute o `.bat` de inicialização do servidor. Deixe a tela preta minimizada rodando em segundo plano, abra o Minecraft e divirta-se!

## 💡 Créditos e Inspiração

A ideia central e a base da arquitetura deste mod foram fortemente inspiradas no incrível trabalho da "equipe do Felps" e do projeto open-source: **minecraft-narrator** desenvolvido por *parmenashp*: https://github.com/parmenashp/minecraft-narrator/tree/main 
Fica aqui o nosso muito obrigado e todos os créditos ao criador original por abrir as portas para essa loucura!

## 📂 Informações Técnicas (Para Desenvolvedores)

Se você é dev e quer entender como a mágica acontece por baixo dos panos, a v1.1 captura as ações e eventos de telemetria do jogador em tempo real, limita o histórico para evitar alucinações da IA e realiza disparos assíncronos via HTTP para o backend. O sistema possui travas *anti-flooding*, garantindo funcionamento fluido sem impactar a performance (TPS) do jogo. A estrutura do repositório está dividida em:

* **/server-ia:** Código-fonte do servidor backend em Python (FastAPI). Responsável pela orquestração do modelo de linguagem (LLM via Ollama) para gerar as respostas sarcásticas, e pela síntese de voz (TTS via Kokoro-82M) gerando o áudio em tempo real.
* **/java:** Código-fonte do mod desenvolvido em Java para o Minecraft (Fabric 1.21.1). Encarregado de coletar os eventos do jogo em uma "Mochila de Memória" fatiada (11 de contexto, 4 de atenção), realizar downsampling de áudio 32-bit para 16-bit PCM, e reproduzir as faixas recebidas através de semáforos de concorrência.
