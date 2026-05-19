package com.gabigoubi.narradoria;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class GameEventListener {

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player != null) {
                player.sendMessage(Text.literal("§b========================================"), false);
                player.sendMessage(Text.literal("§6§lNarrador.IA MOD §r- Obrigado por usar meu Mod!"), false);
                player.sendMessage(Text.literal("§eUse os comandos abaixo para alterar as personas:"), false);
                player.sendMessage(Text.literal("§f/persona mulher.agressiva"), false);
                player.sendMessage(Text.literal("§f/persona mulher.amigavel"), false);
                player.sendMessage(Text.literal("§f/persona homem.agressivo"), false);
                player.sendMessage(Text.literal("§f/persona homem.amigavel"), false);
                player.sendMessage(Text.literal("§b========================================"), false);

                String eventType = "player_join";
                String contextDetails = String.format("Player %s just entered the server/world.", player.getName().getString());
                
                HttpAssistant.sendTelemetry(eventType, contextDetails, ModCommands.currentAiModel, ModCommands.currentVoiceModel)
                    .thenAccept(audioBytes -> AudioPlayer.playWavBytes(audioBytes));
            }
        });
        
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            String eventType = "player_death";
            String contextDetails = String.format("Player %s died and respawned.", newPlayer.getName().getString());
            
            HttpAssistant.sendTelemetry(eventType, contextDetails, ModCommands.currentAiModel, ModCommands.currentVoiceModel)
                .thenAccept(audioBytes -> AudioPlayer.playWavBytes(audioBytes));
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            ServerPlayerEntity player = sender.getPlayer();
            if (player != null) {
                String eventType = "player_chat";
                String contextDetails = String.format("Player %s typed: '%s'", player.getName().getString(), message.getContent().getString());
                
                HttpAssistant.sendTelemetry(eventType, contextDetails, ModCommands.currentAiModel, ModCommands.currentVoiceModel)
                    .thenAccept(audioBytes -> AudioPlayer.playWavBytes(audioBytes));
            }
        });
    }
}