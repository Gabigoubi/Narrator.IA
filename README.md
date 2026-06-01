📋 Backlog v1.6: Evolução Multicontexto do Regente
O Problema (Estado Atual)
O regente atua como um funil muito estreito. Se o jogador faz três coisas diferentes no mesmo ciclo (ex: ameaça a IA no chat, consegue uma conquista e minera pedras), a arquitetura atual força o sistema a escolher apenas uma delas e ignorar o resto. Isso gera uma narração rígida e desperdiça o potencial da IA.

A Solução (Visão Arquitetural)
Transformar o regente em um "curador de contexto". Sem reescrever os cálculos ou o log que já funcionam, o sistema passará a identificar múltiplos eventos simultâneos, selecionará os 3 mais relevantes e entregará as instruções combinadas para a LLM. O formato final do prompt permanece intacto, delegando para a IA o trabalho de cruzar os cenários.

🏗️ ÉPICO 1: Motor de Composição Multicontexto
Tarefa 1: Mapeamento de Instruções Diretas (Variáveis de Contexto)

Lógica: Substituir a ideia de marcadores de estado (verdadeiro/falso) por comandos diretos.

Fluxo: Criar as variáveis que representam os eventos do jogo (ex: has_chatted, is_mining, epic_triumph). Em vez de receberem um simples "sim", elas devem ser programadas para guardar a diretriz exata que a LLM deve seguir caso o evento ocorra (ex: has_chatted guarda a frase "1 frase comentando sobre a mensagem do usuário, apenas zoe").

Tarefa 2: Coleta Simultânea de Cenários (Fim da Decisão Única)

Lógica: Permitir que o sistema reconheça tudo o que aconteceu de importante no ciclo, quebrando a regra de "um evento anula o outro".

Fluxo: Ajustar o avaliador de scores para que ele não pare na primeira condição verdadeira. Ele deve passar por todos os eventos possíveis e preencher com texto todas as variáveis de instrução (da Tarefa 1) que baterem com os scores lidos naquele momento.

Tarefa 3: Filtro de Prioridade e Segurança (A Regra dos Top 3)

Lógica: Proteger a LLM contra o excesso de informações, garantindo que ela foque apenas no que é vital.

Fluxo: Pegar todas as instruções que foram preenchidas na Tarefa 2 e ordená-las usando a força da sua relevância natural (onde eventos de Tier 1/Perigo têm prioridade sobre Tier 3/Tédio). Após ordenar, cortar a lista para manter estritamente as 3 diretrizes mais importantes, descartando o resto.

Tarefa 4: Composição e Injeção Transparente (Preservação do Prompt)

Lógica: Entregar o contexto combinado para a LLM sem quebrar a integração ou o formato visual que o sistema já utiliza para se comunicar.

Fluxo: Unir os textos das até 3 instruções vencedoras em um único bloco. Injetar esse texto combinado diretamente no campo que define o foco da cena atual. O pipeline que formata o prompt final não sofre nenhuma alteração; ele apenas passará a processar um comando mais rico e cruzado.

ÉPICO 2: Buffer Inteligente de Chat (Gatilho de Antecipação)
Objetivo: Consolidar múltiplas mensagens e forçar um disparo antecipado de toda a telemetria (Early Flush) para garantir responsividade natural em interações verbais.

Tarefa 3.1: Temporizador de Interrupção (Fast-Track Timer)

Lógica: Criar uma contagem regressiva isolada acionada por eventos de chat.

Fluxo: Ao detectar a primeira mensagem de chat do jogador, o sistema inicia uma janela absoluta de 10 segundos. Essa janela não reinicia caso novas mensagens cheguem.

Tarefa 3.2: Agrupamento Sequencial Simultâneo

Lógica: Acumular as falas do jogador enquanto outros eventos continuam ocorrendo normalmente no fundo.

Fluxo: Durante os 10 segundos, as mensagens de chat adicionais são concatenadas em um único bloco de texto. Paralelamente, se o jogador tomar dano, minerar ou craftar algo, essas ações continuam entrando no buffer principal normalmente.

Tarefa 3.3: Disparo Forçado (Early Flush) e Reset Global

Lógica: Ignorar o ciclo padrão de 30 segundos e despachar o pacote imediatamente.

Fluxo: Assim que os 10 segundos do chat expirarem, o cliente força o disparo de todo o buffer atual (o chat consolidado + as pedras quebradas + o dano tomado, etc.) para o backend Python. Imediatamente após esse disparo, o relógio padrão de 30 segundos do jogo é zerado/resetado, evitando que um segundo disparo vazio ou duplicado ocorra logo em seguida.

🛡️ ÉPICO 3: Validação de Versão no Boot (Handshake Antecipado)
Objetivo: Garantir a compatibilidade entre cliente e servidor assim que o Minecraft é aberto, utilizando uma checagem isolada que bloqueia o funcionamento e gera alertas massivos no terminal em caso de defasagem.

Tarefa 4.1: Criação do Validador Dedicado (Isolamento de Responsabilidade)

Lógica: Prevenir o antipadrão God Object mantendo a classe principal limpa.

Fluxo: Criar um novo arquivo/classe no Java (ex: VersionValidator) com a responsabilidade única de gerenciar o handshake. Essa classe será chamada no momento da inicialização do mod (onInitialize), antes de qualquer registro de evento de gameplay.

Tarefa 4.2: Rota de Handshake e Fonte da Verdade (Backend Python)

Lógica: Estabelecer uma via de checagem exclusiva, separada do pipeline de narração.

Fluxo: Criar uma variável SERVER_VERSION no Python. Implementar um endpoint rápido (ex: /handshake) que receba a versão do cliente, compare com a do servidor e devolva o diagnóstico exato: "Compatível", "Cliente Desatualizado" ou "Servidor Desatualizado".

Tarefa 4.3: Disparo da Checagem de Boot (Cliente Java)

Lógica: Antecipar a detecção de problemas para antes do jogador entrar no mundo.

Fluxo: Assim que o mod carregar, o Validador Dedicado faz um POST para a rota de handshake informando sua própria versão. O sistema aguarda a resposta para decidir se autoriza ou não o carregamento do motor de telemetria.

Tarefa 4.4: Alerta Massivo no Terminal e Bloqueio de Execução

Lógica: Informar o jogador de forma inconfundível e prevenir falhas em cascata.

Fluxo: Se o backend acusar incompatibilidade, o Java imprime um erro colossal no log/terminal (um bloco visualmente destacado com avisos, indicando claramente se o usuário deve baixar um novo .jar ou uma nova pasta do servidor). Além disso, o Validador atua como uma trava: em caso de erro ou servidor offline, ele aborta o registro do GameEventListener, desativando a telemetria completamente para aquela sessão sem causar crash no jogo.
