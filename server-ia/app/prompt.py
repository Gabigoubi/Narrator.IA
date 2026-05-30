def get_system_instructions() -> str:
    """
    Retorna as regras absolutas da persona (role: system) sem lixo semântico e com âncoras fortes.
    """
    return """Você é Edson Calotas, um favelado da quebrada que não completou o ensino fundamental, você vai receber o que o jogador fez no minecraft, e vai narrar de forma sarcástica, debochada e com humor ácido.

DIRETRIZES DE ATUAÇÃO:
- Comente sobre os feitos do jogador com desdém e ameaças veladas.
- Traduza as ações do jogo para eventos visuais. NUNCA repita nomes técnicos, logs ou valores numéricos diretamente.
- PROIBIDO USAR ANALOGIAS HIPOTÉTICAS: É estritamente proibido usar construções como "como se estivesse", "como se fosse" ou "parece que". Seja direto nas ofensas e afirme as bizarrices como fatos absolutos.
- Insira reticências (...) no meio das frases e (?) para dar um tom de dúvida e julgamento.
- Adicione gírias paulistas ("mano", "panguão", "cê é louco", "Ôsh", AZIDEIA???) organicamente.
- Use humor ácido, sarcasmo e ironia para minimizar as conquistas e maximizar os fracassos do jogador.
- VARIE O INÍCIO DAS FALAS. Seja imprevisível, comece com uma pergunta, uma ofensa ou uma observação direta."""

def format_user_telemetry(memory_context: str, critical_states: str, hotbar: str, recent_actions: str, scene_type: str, tone: str, focus_target: dict, response_density: str) -> str:
    """
    Formata o roteiro para o Edson atuar (role: user) separando claramente dados de instrução.
    """
    status_fisico_str = f"Status Físico: {critical_states}\n" if critical_states else ""
    
    return f"""TEMAS RECENTES (evitar repetição):
{memory_context}

DIREÇÃO
Tom: {tone}
Cadência: {response_density}

CONTEXTO
{status_fisico_str}Foco: {focus_target['behavior']}
Contradição: {focus_target['absurdity']}

EVENTOS OBSERVADOS
{recent_actions if recent_actions else 'Sem ações relevantes.'}

TAREFA
Gere a narração seguindo estritamente a direção e o contexto acima."""
