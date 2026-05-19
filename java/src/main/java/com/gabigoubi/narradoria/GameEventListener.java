package com.gabigoubi.narradoria;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

import java.util.ArrayList;
import java.util.List;

public class GameEventListener {

    private static final List<String> acoesRecentes = new ArrayList<>();
    
    
    private static int tickCounter = 0;
    private static final int TEMPO_NARRACAO_TICKS = 600; 

    public static void register() {
        
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            String bloco = state.getBlock().getName().getString();
            adicionarAcao("Quebrou " + bloco);
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient()) {
                String alvo = entity.getName().getString();
                adicionarAcao("Atacou " + alvo);
            }
            return ActionResult.PASS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity) {
                String motivo = source.getName(); 
                adicionarAcao("Tomou dano de " + motivo);
            }
            return true;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            
            if (tickCounter >= TEMPO_NARRACAO_TICKS) {
                tickCounter = 0; 
               
              
                if (!server.getPlayerManager().getPlayerList().isEmpty()) {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayerList().get(0);
                    
                    
                    float vida = player.getHealth(); 
                    int fome = player.getHungerManager().getFoodLevel(); 
                    String itemMao = player.getMainHandStack().isEmpty() ? "Mao Vazia" : player.getMainHandStack().getItem().getName().getString();
                    
                    
                    StringBuilder contexto = new StringBuilder();
                    contexto.append("STATUS ATUAL: Vida ").append(vida).append("/20, Fome ").append(fome).append("/20, Segurando '").append(itemMao).append("'. ");
                    
                    if (!acoesRecentes.isEmpty()) {
                        contexto.append("O QUE FEZ AGORA POUCO: ").append(String.join(", ", acoesRecentes)).append(".");
                    } else {
                        contexto.append("O QUE FEZ AGORA POUCO: Nao fez nada, ficou moscando.");
                    }
                    
                    
                    if (vida > 0) {
                        String detalhes = contexto.toString();
                        HttpAssistant.sendNarrateRequest("gameplay_update", detalhes, ModCommands.currentAiModel, ModCommands.currentVoiceModel);
                    }
                    
                    
                    acoesRecentes.clear();
                }
            }
        });
    }

  
    private static void adicionarAcao(String acao) {
        
        if (acoesRecentes.size() < 15) {
            acoesRecentes.add(acao);
        }
    }
}
