# 🎙️ Mod Narrador IA para Minecraft (Fabric 1.21.1)
Este mod traz uma inteligência artificial que assiste à sua gameplay e te esculacha em tempo real!


Conheça o **Edson Calotas**, um "mecânico" virtual folgado, sarcástico e imaturo da Zona Leste de São Paulo que vai julgar tudo o que você faz no Minecraft, desde quebrar terra com a mão até apanhar de zumbi lerdão. Tudo isso rodando **100% localmente** no seu PC, de forma gratuita e sem depender de internet.
**Página Oficial e Download do Mod:** Narrador IA no CurseForge


## 💻 Requisitos do Sistema
Como o "cérebro" da IA roda diretamente no seu computador, o mod exige um pouco de hardware para processar a voz e o texto simultaneamente com o Minecraft.


### **Mínimos** *(Respostas curtas, focado em PCs de entrada)*
 * **SO:** Windows 10 ou 11
 * **Processador:** Qualquer CPU moderna (Intel Core i3/Ryzen 3 ou superior)
 * **Memória:** 8 GB de RAM
 * **Placa de Vídeo:** Vídeo Integrado ou Placa de Vídeo Básica
 * **Modelo IA Utilizado:** Qwen 2.5 (3B)


### **Recomendados** *(Experiência completa, rápida e zoeira extrema)*
 * **SO:** Windows 10 ou 11
 * **Processador:** Intel Core i5/Ryzen 5 ou superior (ex: Ryzen 7 5700)
 * **Memória:** 16 GB de RAM
 * **Placa de Vídeo:** Placa de Vídeo Dedicada com 8GB+ de VRAM (ex: RX 7600, RTX 3060 ou superior)


 * **Modelo IA Utilizado:** Mistral (7B)
## ⚙️ Como Instalar e Jogar
O projeto foi pensado para ser "Plug and Play". O mod em si é baixado pelo CurseForge, você só precisa ligar o motor da IA na sua máquina antes de jogar.

 1. **Baixe o Mod:** Instale o .jar diretamente pela nossa página oficial no CurseForge.
 2. **Baixe o Servidor IA:** Baixe o código deste repositório (clique no botão verde Code > Download ZIP) e extraia a pasta no seu PC.
 3. **Primeira Instalação:** Dê dois cliques no arquivo 1_PRIMEIRA_VEZ.bat e siga as instruções na tela. *(Aviso: Lembre-se de marcar a caixa "Add Python to PATH" durante a instalação do Python!)*
 4. **Ligar e Jogar:** Sempre que for jogar, abra a pasta baixada e execute o .bat correspondente ao seu PC (2_Iniciar_PC_Fraco_8GB_RAM.bat ou 2_Iniciar_PC_Forte_16GB_RAM.bat). Deixe a tela preta minimizada rodando em segundo plano, abra o Minecraft e divirta-se!


## 💡 Créditos e Inspiração
A ideia central e a base da arquitetura deste mod foram fortemente inspiradas no incrível trabalho da "equipe do Felps" e do projeto open-source:
**minecraft-narrator desenvolvido por parmenashp:** https://github.com/parmenashp/minecraft-narrator/tree/main
Fica aqui o nosso muito obrigado e todos os créditos ao criador original por abrir as portas para essa loucura!

## 📂 Informações Técnicas (Para Desenvolvedores)
Se você é dev e quer entender como a mágica acontece por baixo dos panos, o mod captura as ações e eventos de telemetria do jogador em tempo real e realiza disparos assíncronos via HTTP para o backend, garantindo o funcionamento fluido sem impactar a performance (TPS) do jogo.
A estrutura do repositório está dividida em:
 * **/server-ia:** Código-fonte do servidor backend em Python (FastAPI). Responsável pela orquestração do modelo de linguagem (LLM via Ollama) para gerar as respostas sarcásticas, e pela síntese de voz (TTS via Kokoro-82M) gerando o áudio no formato .wav em tempo real.
 * **/java:** Código-fonte do mod desenvolvido em Java para o Minecraft (Fabric 1.21.1). Encarregado de coletar os eventos do jogo em uma "Mochila de Memória" (Tick Events, Block Breaks, Advancements) e reproduzir as faixas de áudio recebidas limitando picos de decibéis.
