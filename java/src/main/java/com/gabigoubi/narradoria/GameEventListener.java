package com.gabigoubi.narradoria;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.damage.DamageSource;

public class GameEventListener {

    public static void registerEvents() {
        

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            String eventType = "player_death";
            String contextDetails = String.format(
                "Player %s died in world %s and just respawned.", 
                newPlayer.getName().getString(),
                newPlayer.getWorld().getRegistryKey().getValue().toString()
            );
            
  
            HttpAssistant.sendTelemetry(eventType, contextDetails);
        });


        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            ServerPlayerEntity player = sender.getPlayer();
            if (player != null) {
                String eventType = "player_chat";
                String contextDetails = String.format(
                    "Player %s typed in chat: '%s'", 
                    player.getName().getString(),
                    message.getContent().getString()
                );
                
                HttpAssistant.sendTelemetry(eventType, contextDetails)
    .thenAccept(audioBytes -> AudioPlayer.playWavBytes(audioBytes));
            }
        });
    }
}