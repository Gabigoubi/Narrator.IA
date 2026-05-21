package com.gabigoubi.narradoria;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class GameEventListener {

    // --- CONSTANTES (Configurações do Sistema) ---
    private static final String VOICE_MODEL = "pm_alex";
    private static final int EVENT_TRIGGER_TICKS = 200; 
    private static final int MAX_RECENT_ACTIONS = 10;
    
    // Limiares Críticos
    private static final float CRITICAL_HEALTH = 4.0f;
    private static final int CRITICAL_HUNGER = 4;
    private static final int Y_LEVEL_DEEP = 15;
    private static final int Y_LEVEL_HIGH = 120;

    // --- ESTADO INTERNO ---
    private static final List<String> recentActions = new ArrayList<>();
    private static int tickCounter = 0;

    public static void register() {
        registerConnectionEvents();
        registerInteractionEvents();
        registerCombatEvents();
        registerTickEvent();
    }

    // ==========================================
    // 1. EVENTOS DE CONEXÃO E ESTADO DO JOGADOR
    // ==========================================
    private static void registerConnectionEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player != null) {
                player.sendMessage(Text.literal("§a[Narrador.IA v1.3] §fSistema de Telemetria Avançada Ativado!"), false);
                sendImmediateEvent("player_join", "O jogador " + player.getName().getString() + " entrou no mundo.", player);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            sendImmediateEvent("player_death", "O jogador morreu tragicamente e renasceu.", newPlayer);
        });
    }

    // ==========================================
    // 2. EVENTOS DE INTERAÇÃO COM O MUNDO
    // ==========================================
    private static void registerInteractionEvents() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            String block = state.getBlock().getName().getString();
            String item = getItemInMainHand(player);
            addAction("Quebrou: " + block + " (Usando: " + item + ")");
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient() && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity) {
                if (!player.getStackInHand(hand).isEmpty()) {
                    String item = player.getStackInHand(hand).getItem().getName().getString();
                    addAction("Usou/Colocou: " + item);
                }
            }
            return ActionResult.PASS;
        });
    }

    // ==========================================
    // 3. EVENTOS DE COMBATE E DANO
    // ==========================================
    private static void registerCombatEvents() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity) {
                String target = entity.getName().getString();
                String item = getItemInMainHand((ServerPlayerEntity) player);
                addAction("Atacou: " + target + " (Usando: " + item + ")");
            }
            return ActionResult.PASS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity) {
                addAction("Sofreu dano de: " + source.getName());
            }
            return true;
        });
    }

    // ==========================================
    // 4. MOTOR DE TELEMETRIA (TICK EVENT)
    // ==========================================
    private static void registerTickEvent() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter >= EVENT_TRIGGER_TICKS) {
                tickCounter = 0;
                if (!server.getPlayerManager().getPlayerList().isEmpty()) {
                    processTelemetryBuffer(server.getPlayerManager().getPlayerList().get(0));
                }
            }
        });
    }

    // ==========================================
    // MÉTODOS AUXILIARES E DE PROCESSAMENTO
    // ==========================================
    private static String getItemInMainHand(ServerPlayerEntity player) {
        return player.getMainHandStack().isEmpty() ? "Mão Nua" : player.getMainHandStack().getItem().getName().getString();
    }

    private static void addAction(String action) {
        if (recentActions.size() < MAX_RECENT_ACTIONS) {
            recentActions.add(action);
        }
    }

    private static void sendImmediateEvent(String eventType, String eventContext, ServerPlayerEntity player) {
        recentActions.clear();
        addAction(eventContext);
        processTelemetryBuffer(player);
    }

    private static void processTelemetryBuffer(ServerPlayerEntity player) {
        List<String> criticalStates = new ArrayList<>();
        float health = player.getHealth();
        int hunger = player.getHungerManager().getFoodLevel();
        int yLevel = (int) player.getY();

        if (health <= CRITICAL_HEALTH) criticalStates.add("Vida Crítica: " + health + "/20");
        if (hunger <= CRITICAL_HUNGER) criticalStates.add("Fome Extrema: " + hunger + "/20");
        if (yLevel <= Y_LEVEL_DEEP) criticalStates.add("Localização: Y=" + yLevel + " (Caverna/Profundezas)");
        else if (yLevel >= Y_LEVEL_HIGH) criticalStates.add("Localização: Y=" + yLevel + " (Altitude Elevada)");

        if (recentActions.isEmpty() && criticalStates.isEmpty()) {
            return; // Aborta silenciosamente se não houver contexto relevante
        }

        JsonArray hotbarArray = new JsonArray();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            hotbarArray.add(stack.isEmpty() ? "Vazio" : stack.getItem().getName().getString());
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("voice_model", VOICE_MODEL);
        
        JsonArray statesArray = new JsonArray();
        criticalStates.forEach(statesArray::add);
        payload.add("critical_states", statesArray);
        
        payload.add("hotbar", hotbarArray);

        JsonArray actionsArray = new JsonArray();
        recentActions.forEach(actionsArray::add);
        payload.add("recent_actions", actionsArray);

        // Dispara para o Python 
        HttpAssistant.sendStructuredTelemetry(payload.toString());

        recentActions.clear();
    }
}
