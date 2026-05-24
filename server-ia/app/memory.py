import json
import os

class SlidingMemory:
    def __init__(self, filepath="edson_memory.json", max_history=3):
        """
        Inicializa a memória do Edson.
        :param filepath: Caminho do arquivo para salvar a memória persistente.
        :param max_history: Quantas interações passadas a IA deve lembrar (ideal: 3 para modelos 8B).
        """
        self.filepath = filepath
        self.max_history = max_history
        self.history = self._load_from_disk()

    def add_interaction(self, player_action_summary: str, edson_response: str):
        """
        Adiciona uma nova interação à memória e remove a mais antiga se passar do limite.
        """
        interaction = {
            "jogador_fez": player_action_summary,
            "edson_falou": edson_response
        }
        
        self.history.append(interaction)
        
        # O "Sliding Window": se o histórico ficar maior que o limite, corta a primeira (mais velha)
        if len(self.history) > self.max_history:
            self.history.pop(0)
            
        self._save_to_disk()

    def get_context_string(self) -> str:
        """
        Retorna apenas as últimas falas do Edson para evitar repetição de assunto.
        """
        if not self.history:
            return "Nenhuma fala anterior."

        context_str = ""
        for entry in self.history:
            context_str += f"- Você já disse: '{entry['edson_falou']}'\n"
            
        return context_str.strip()

    def clear_memory(self):
        """
        Limpa a memória completamente (útil para quando o jogador cria um mundo novo).
        """
        self.history = []
        self._save_to_disk()

    def _save_to_disk(self):
        """Salva a memória no arquivo JSON de forma segura."""
        try:
            with open(self.filepath, "w", encoding="utf-8") as f:
                json.dump(self.history, f, ensure_ascii=False, indent=4)
        except Exception as e:
            print(f"[ERRO DE MEMÓRIA] Não foi possível salvar o arquivo: {e}")

    def _load_from_disk(self) -> list:
        """Carrega a memória do arquivo JSON se existir."""
        if os.path.exists(self.filepath):
            try:
                with open(self.filepath, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception as e:
                print(f"[ERRO DE MEMÓRIA] Arquivo corrompido, iniciando memória limpa: {e}")
                return []
        return []