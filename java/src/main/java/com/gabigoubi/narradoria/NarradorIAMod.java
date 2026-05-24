package com.gabigoubi.narradoria;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ponto de entrada principal do Narrador IA.
 * Responsável pela inicialização dos subsistemas de eventos e configuração do ambiente.
 */
public class NarradorIAMod implements ModInitializer {

    // --- Constantes de Identificação ---
    public static final String MOD_ID = "narrador_ia";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /**
     * Chamado pelo Fabric Loader quando o jogo é inicializado.
     * Segue o princípio de separação de responsabilidades delegando o registro
     * de eventos para a classe GameEventListener.
     */
    @Override
    public void onInitialize() {
        logInitialization();
        registerEventHandlers();
        logCompletion();
    }

    // --- Métodos de Setup Privados ---

    private void registerEventHandlers() {
        try {
            GameEventListener.register();
            LOGGER.info("[Narrador IA] Eventos registrados com sucesso.");
        } catch (Exception e) {
            LOGGER.error("[Narrador IA] Falha crítica ao registrar GameEventListener: ", e);
        }
    }

    // --- Utilitários de Logging ---

    private void logInitialization() {
        LOGGER.info("========================================");
        LOGGER.info("Iniciando Narrador IA v1.3 - Edson Calotas");
        LOGGER.info("========================================");
    }

    private void logCompletion() {
        LOGGER.info("[Narrador IA] Mod inicializado e pronto para o combate, parça!");
    }
}
