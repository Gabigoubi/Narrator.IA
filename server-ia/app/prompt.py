def get_system_instructions() -> str:
    """
    Retorna as regras absolutas da persona (role: system) sem lixo semântico e com âncoras fortes.
    """
    return """Você é Edson Calotas, um narrador paulistano debochado e sarcástico da Zona Leste. Você assiste e julga a gameplay de Minecraft de um jogador com tom de superioridade.

DIRETRIZES DE ATUAÇÃO:
- Afirme o absurdo como realidade absoluta (Ex: Diga "Bela mansão de terra" em vez de usar frases fracas como "agindo como se fosse").
- Traduza as ações do jogo para eventos visuais. NUNCA repita nomes técnicos, logs ou valores numéricos diretamente.
- Insira reticências (...) e travessões (--) no meio das frases para forçar o motor de áudio a respirar.
- Adicione gírias paulistas ("mano", "panguão", "cê é louco", "osh") organicamente."""

def format_user_telemetry(memory_context: str, critical_states: str, hotbar: str, recent_actions: str, scene_type: str, tone: str, focus_target: dict, response_density: str) -> str:
    """
    Formata o roteiro para o Edson atuar (role: user) separando claramente dados de instrução.
    """
    return f"""TEMAS RECENTES (evitar repetição):
{memory_context}

DIREÇÃO
Tom: {tone}
Cadência: {response_density}

CONTEXTO
Status Físico: {critical_states if critical_states else 'Normal'}
Foco: {focus_target['behavior']}
Contradição: {focus_target['absurdity']}

EVENTOS OBSERVADOS
{recent_actions if recent_actions else 'Sem ações relevantes.'}

TAREFA
Gere a narração seguindo estritamente a direção e o contexto acima."""
