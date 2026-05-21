# 🎙️ Mod Narrador IA para Minecraft (Fabric 1.21.1) - Versão 1.3

Bem-vindo ao projeto! Este mod traz uma inteligência artificial que assiste à sua gameplay e te esculacha em tempo real!

Conheça o **Edson Calotas**, um parceiro virtual folgado, sarcástico e debochado da Zona Leste de São Paulo que vai julgar tudo o que você faz no Minecraft, desde quebrar terra com a mão até apanhar de zumbi lerdão. Tudo isso rodando **100% localmente** no seu PC, de forma gratuita, segura e sem depender de internet.

*(Aviso: As documentações técnicas e arquiteturais do código para desenvolvedores estão localizadas no final desta página).*

**Página Oficial e Download do Mod:** [Narrador IA no CurseForge](https://www.curseforge.com/minecraft/mc-mods/narrador-ia)

---

## 💻 Requisitos do Sistema

Como o "cérebro" da IA roda diretamente no seu computador, o mod exige hardware potente para processar a voz e o texto simultaneamente com o Minecraft sem prejudicar a sua gameplay.

**Mínimos (Possui Trava de Segurança Automática)**
* **SO:** Windows 10 ou 11
* **Processador:** Intel Core i5 (8ª Ger.) / AMD Ryzen 5 (4 Núcleos / 8 Threads)
* **Memória RAM:** 12 GB (O mod **NÃO ABRIRÁ** se você tiver menos de 12GB para evitar o travamento do seu Windows).
* **Placa de Vídeo:** Dedicada com **6GB de VRAM** (ex: GTX 1660 Ti, RTX 2060, RX 5600 XT).

**Recomendados (Para a experiência ideal e fluida)**
* **Memória RAM:** 16 GB+
* **Placa de Vídeo:** Dedicada com **8GB+ de VRAM** (ex: RTX 3060, RX 6600 ou superior).

---

## ⚙️ Como Instalar e Jogar

O projeto foi pensado para ser "Plug and Play". O mod em si é baixado pelo CurseForge, você só precisa ligar o motor da IA na sua máquina antes de jogar.

1. **Baixe o Mod:** Instale o `.jar` diretamente pela nossa página oficial no CurseForge.
2. **Baixe o Servidor IA:** Baixe o código deste repositório (clique no botão verde `Code > Download ZIP`) e extraia a pasta no seu PC.
3. **Primeira Instalação:** Dê dois cliques no arquivo `1_PRIMEIRA_VEZ.bat` e siga as instruções na tela. *(Regra de Ouro: Lembre-se de marcar a caixa "Add Python 3.11 to PATH" durante a instalação do Python!)*
4. **Ligar e Jogar:** Sempre que for jogar, abra a pasta baixada e execute o arquivo **`2_INICIAR_IA.bat`**. 
   * *Nota Importante:* Na primeira vez que você abrir este arquivo, ele fará o download do motor da IA automaticamente (Aprox. 4.1 GB). Aguarde a mensagem verde de sucesso.
   * Deixe a tela preta minimizada rodando em segundo plano, abra o Minecraft e divirta-se!

---

## 💡 Créditos e Inspiração

A ideia central e a base da arquitetura deste mod foram fortemente inspiradas no incrível trabalho da "equipe do Felps" e do projeto open-source: **minecraft-narrator** desenvolvido por *parmenashp*: https://github.com/parmenashp/minecraft-narrator/tree/main 
Fica aqui o nosso muito obrigado e todos os créditos ao criador original por abrir as portas para essa loucura!

---

## 📂 Informações Técnicas (Para Desenvolvedores)

Se você é dev e quer entender como a arquitetura da v1.3 funciona por baixo dos panos, o sistema foi desenhado visando performance extrema, segurança de hardware e telemetria baseada em eventos (Event-Driven).

* **Proteção de Hardware (Hard Lock):** O backend em Python utiliza a biblioteca `psutil` antes do boot para inferir a memória RAM física. Em caso de hardware insuficiente (<12GB), o processo recebe um `sys.exit(1)` bloqueando o motor para evitar falhas de paginação (memory swapping) no SO do usuário.
* **Backend em Python (FastAPI):** Atua como o cérebro orquestrador. Gerencia automaticamente a presença do Ollama no ambiente local e executa chamadas para o modelo parametrizado (`mistral:latest`). A síntese de voz ocorre via TTS (Kokoro) gerando os áudios dinamicamente.
* **Mod Java (Fabric 1.21.1):** O motor de captura descarta a varredura baseada em *ticks* contínuos (que causaria gargalo de TPS) em favor de gatilhos condicionais. O Java empacota um JSON estruturado contendo: `critical_states` (Vida <= 4, Altitude Extrema), `hotbar` (Captura instantânea do inventário rápido) e `recent_actions`. 
* **Otimização de Contexto:** A IA não sofre de alucinação ou *Lost in the Middle*, pois a Engenharia de Prompt traduz o JSON em uma ficha técnica situacional de leitura rápida, processando o estado do jogador em uma única inferência HTTP. O áudio retornado é decodificado e reproduzido no Minecraft utilizando semáforos de concorrência e downsampling para 16-bit PCM.