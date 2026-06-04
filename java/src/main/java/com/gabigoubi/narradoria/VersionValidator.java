package com.gabigoubi.narradoria;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Sistema de Handshake Antecipado (Épico 3)
 * Responsável por validar as versões e gerenciar a degradação elegante (Graceful Degradation).
 */
public class VersionValidator {

    // ========================================================================
    // CONFIGURATION & STATE VARIABLES
    // ========================================================================
    
    // A fonte da verdade do lado do cliente
    public static final String CLIENT_VERSION = "1.6";
    
    // Armazena a mensagem caso o handshake falhe
    private static String pendingErrorMessage = null;
    
    // Flag de autorização para o motor de telemetria
    private static boolean telemetryAuthorized = false;

    // ========================================================================
    // PHASE 1: SYNCHRONOUS BOOT VALIDATION
    // ========================================================================
    
    /**
     * Executed during onInitialize. Blocks telemetry if validation fails.
     * @return boolean indicating if telemetry should start.
     */
    public static boolean validateAtBoot() {
        NarradorIAMod.LOGGER.info("[VersionValidator] Iniciando handshake com o Backend Python. Versão do Cliente: " + CLIENT_VERSION);
        
        // Chamada síncrona que implementaremos no próximo passo no HttpAssistant
        String serverResponse = HttpAssistant.checkVersionWithServer(CLIENT_VERSION);
        
        if (serverResponse == null) {
            // Backend offline ou inacessível
            pendingErrorMessage = "O servidor de IA está offline ou inacessível. Inicie o backend Python (main.py) antes de abrir o mapa.";
            telemetryAuthorized = false;
            NarradorIAMod.LOGGER.error("[VersionValidator] Falha no Handshake: Servidor inoperante.");
            
        } else if (serverResponse.equals("OK")) {
            // Sucesso absoluto
            telemetryAuthorized = true;
            NarradorIAMod.LOGGER.info("[VersionValidator] Handshake aprovado! As versões estão perfeitamente sincronizadas.");
            
        } else {
            // Incompatibilidade de versão (O Python envia o texto exato do erro)
            pendingErrorMessage = serverResponse;
            telemetryAuthorized = false;
            NarradorIAMod.LOGGER.error("[VersionValidator] Falha no Handshake: " + serverResponse);
        }
        
        return telemetryAuthorized;
    }

    // ========================================================================
    // PHASE 2: DELAYED PLAYER NOTIFICATION
    // ========================================================================
    
    /**
     * Registers an event to warn the player in the chat if the handshake failed.
     */
    public static void registerLoginListener() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (pendingErrorMessage != null) {
                ServerPlayerEntity player = handler.getPlayer();
                
                // Dispara o alerta massivo no chat do jogo usando códigos de cor (Formatting)
                player.sendMessage(Text.literal("§c========================================"), false);
                player.sendMessage(Text.literal("§4[ERRO CRÍTICO] Falha no Narrador IA"), false);
                player.sendMessage(Text.literal("§c========================================"), false);
                player.sendMessage(Text.literal("§eMotivo: §f" + pendingErrorMessage), false);
                player.sendMessage(Text.literal("§eAção: §fA telemetria foi desativada por segurança. O jogo rodará normalmente, mas Edson não falará."), false);
                player.sendMessage(Text.literal("§c========================================"), false);
            }
        });
    }
}
