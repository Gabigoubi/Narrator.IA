package com.gabigoubi.narradoria;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntityPickupItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class GameEventListener {

    private static final List<String> acoesRecentes = new ArrayList<>();
    private static int tickCounter = 0;
    private static final int TEMPO_NARRACAO_TICKS = 600;

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

                String contextDetails = String.format("O jogador %s acabou de entrar no mundo. Receba ele com sarcasmo.", player.getName().getString());
                HttpAssistant.sendNarrateRequest("player_join", contextDetails, ModCommands.currentAiModel, ModCommands.currentVoiceModel);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            HttpAssistant.sendNarrateRequest("player_death", "O jogador morreu, perdeu tudo e acabou de renascer.", ModCommands.currentAiModel, ModCommands.currentVoiceModel);
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            ServerPlayerEntity player = sender.getPlayer();
            if (player != null) {
                String texto = message.getContent().getString();
                HttpAssistant.sendNarrateRequest("player_chat", "O jogador disse no chat: " + texto, ModCommands.currentAiModel, ModCommands.currentVoiceModel);
            }
        });

        ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
            String texto = message.getString();
            if (texto.contains("conseguiu a conquista") || texto.contains("fez o progresso") || texto.contains("advancement") || texto.contains("challenge")) {
                HttpAssistant.sendNarrateRequest("player_advancement", "O jogador conseguiu essa conquista: " + texto, ModCommands.currentAiModel, ModCommands.currentVoiceModel);
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            String bloco = state.getBlock().getName().getString();
            adicionarAcao("Quebrou " + bloco);
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient() && hand == Hand.MAIN_HAND) {
                if (!player.getStackInHand(hand).isEmpty()) {
                    String item = player.getStackInHand(hand).getItem().getName().getString();
                    adicionarAcao("Usou/Colocou " + item);
                }
            }
            return ActionResult.PASS;
        });

        EntityPickupItemCallback.EVENT.register((entity, itemEntity) -> {
            if (entity instanceof ServerPlayerEntity) {
                String item = itemEntity.getStack().getItem().getName().getString();
                adicionarAcao("Pegou " + item);
            }
            return ActionResult.PASS;
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
                        HttpAssistant.sendNarrateRequest("gameplay_update", contexto.toString(), ModCommands.currentAiModel, ModCommands.currentVoiceModel);
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
