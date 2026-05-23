package com.gabigoubi.narradoria;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class GameEventListener {

    // --- Configuration Constants ---
    private static final String VOICE_MODEL = "pm_alex";
    private static final int MAX_BUFFER_SIZE = 30;
    private static final long FLUSH_INTERVAL_MS = 45000L; // 45 segundos (Curto Prazo)
    private static final long SESSION_INTERVAL_MS = 600000L; // 10 minutos (Longo Prazo)

    // --- Threshold Constants ---
    private static final float CRITICAL_HEALTH_THRESHOLD = 4.0f;
    private static final int CRITICAL_HUNGER_THRESHOLD = 4;

    // --- Thread-Safe Encapsulated State (Player Isolation) ---
    private static final Map<UUID, List<ActionEntry>> playerBuffers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastFlushTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, List<String>> hotbarCaches = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastEatTimes = new ConcurrentHashMap<>();
    
    // NOVO: Controle da Sessão de 10 Minutos
    private static final Map<UUID, List<ActionEntry>> sessionBuffers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSessionFlushTimes = new ConcurrentHashMap<>();

    public static void register() {
        registerConnectionEvents();
        registerInteractionEvents();
        registerCombatEvents();
        registerChatAndAdvancements();
        registerTickEvent();
        registerWorldAndEntityEvents(); // NOVO: Módulo de Mundo e Física
    }

    // --- Event Registrations ---

    private static void registerWorldAndEntityEvents() {
        // 1. Troca de Dimensão (Envia o gatilho na hora, limpo e orgânico)
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            String originName = origin.getRegistryKey().getValue().getPath(); 
            String destName = destination.getRegistryKey().getValue().getPath();
            
            String context = String.format("Viajou de '%s' para '%s'", originName, destName);
            
            // isCritical = true para o Edson comentar a chegada no novo mundo no mesmo instante
            addActionAndCheckFlush("Dimension Changed", context, player, true); 
        });

        // 2. Drop de Itens (Descarte Físico)
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity itemEntity && itemEntity.getThrower() != null) {
                ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(itemEntity.getThrower());
                if (player != null) {
                    addActionAndCheckFlush("Dropped", itemEntity.getStack().getName().getString(), player, false);
                }
            }
        });
    }

    private static void registerConnectionEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player != null) {
                UUID uuid = player.getUuid();
                
                // Inicializa o estado de Curto Prazo
                playerBuffers.putIfAbsent(uuid, new ArrayList<>());
                lastFlushTimes.putIfAbsent(uuid, System.currentTimeMillis());
                
                // Inicializa o estado de Longo Prazo (Sessão)
                sessionBuffers.putIfAbsent(uuid, new ArrayList<>());
                lastSessionFlushTimes.putIfAbsent(uuid, System.currentTimeMillis());

                player.sendMessage(Text.literal("§a[Narrador.IA v1.3] §fAdvanced Telemetry Online!"), false);

                String playerName = player.getName().getString();
                String welcomeInstruction = String.format("O jogador %s acabou de entrar. Receba-o EXATAMENTE com esta vibe: 'Eita, o jogador %s entrou no mundo....... o que será que esse noiao vai fazer hein...'", playerName, playerName);
                addActionAndCheckFlush("BOAS-VINDAS", welcomeInstruction, player, true);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            playerBuffers.remove(uuid);
            lastFlushTimes.remove(uuid);
            hotbarCaches.remove(uuid);
            sessionBuffers.remove(uuid);
            lastSessionFlushTimes.remove(uuid);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            addActionAndCheckFlush("System", "Player respawned after death", newPlayer, true);
        });
    }

    private static void registerChatAndAdvancements() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender != null) {
                addActionAndCheckFlush("Chat", message.getContent().getString(), sender, false);
            }
        });

        ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
            String text = message.getString();
            if (text.contains("conseguiu a conquista") || text.contains("fez o progresso") || text.contains("advancement")) {
                if (!server.getPlayerManager().getPlayerList().isEmpty()) {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayerList().get(0);
                    addActionAndCheckFlush("Achievement", text, player, true);
                }
            }
        });
    }

    private static void registerInteractionEvents() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                String blockName = state.getBlock().getName().getString();
                addActionAndCheckFlush("Broke", blockName, serverPlayer, false);
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            net.fabricmc.fabric.api.entity.event.v1.PlayerEntityEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
    if (entity instanceof ServerPlayerEntity player) {
        // Edson descobre se o cara dormiu, acordou.
        addActionAndCheckFlush("Woke Up", "Cama", player, false);
    }
});
            if (!world.isClient() && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity serverPlayer) {
                ItemStack stack = player.getStackInHand(hand);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                    addActionAndCheckFlush("Placed", stack.getItem().getName().getString(), serverPlayer, false);
                }
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient() && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity serverPlayer) {
                ItemStack stack = player.getStackInHand(hand);
                if (!stack.isEmpty()) {
                    boolean isFood = stack.getItem().getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD);
                    long now = System.currentTimeMillis();

                    if (isFood) {
                        long lastEat = lastEatTimes.getOrDefault(player.getUuid(), 0L);
                        if (now - lastEat >= 2000L) {
                            lastEatTimes.put(player.getUuid(), now);
                            addActionAndCheckFlush("Consumed", stack.getItem().getName().getString(), serverPlayer, false);
                        }
                    } else {
                        addActionAndCheckFlush("Used", stack.getItem().getName().getString(), serverPlayer, false);
                    }
                }
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
    }

    private static void registerCombatEvents() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                addActionAndCheckFlush("Attacked", entity.getName().getString(), serverPlayer, false);
            }
            return ActionResult.PASS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity serverPlayer) {
                float projectedHealth = serverPlayer.getHealth() - amount;
                boolean isCritical = projectedHealth <= CRITICAL_HEALTH_THRESHOLD;

                String attackerName = source.getAttacker() != null ? source.getAttacker().getName().getString() : "Environment";
                String damageContext = String.format("%s (Source: %s)", source.getName(), attackerName);

                addActionAndCheckFlush("Took Damage", damageContext, serverPlayer, isCritical);
            }
            return true;
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity serverPlayer) {
                String deathMessage = damageSource.getDeathMessage(serverPlayer).getString();
                addActionAndCheckFlush("Morreu", deathMessage, serverPlayer, true);
            }
            return true; 
        });
    }

    // --- Core Telemetry Logic ---

    private static void registerTickEvent() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = System.currentTimeMillis();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                
                // 1. Varredura de Curto Prazo (45 Segundos)
                List<ActionEntry> buffer = playerBuffers.get(uuid);
                if (buffer != null && !buffer.isEmpty()) {
                    long lastFlush = lastFlushTimes.getOrDefault(uuid, now);
                    if (now - lastFlush >= FLUSH_INTERVAL_MS) {
                        synchronized (buffer) {
                            if (!buffer.isEmpty()) {
                                prepareAndFlushPayload(player, buffer, now);
                            }
                        }
                    }
                }
                
                // 2. Varredura de Longo Prazo (Sessão de 10 Minutos)
                List<ActionEntry> sessionBuffer = sessionBuffers.get(uuid);
                if (sessionBuffer != null && !sessionBuffer.isEmpty()) {
                    long lastSessionFlush = lastSessionFlushTimes.getOrDefault(uuid, now);
                    if (now - lastSessionFlush >= SESSION_INTERVAL_MS) {
                        synchronized (sessionBuffer) {
                            if (!sessionBuffer.isEmpty()) {
                                prepareAndFlushSessionPayload(player, sessionBuffer, now);
                            }
                        }
                    }
                }
            }
        });
    }

    // Método exposto como PUBLIC para permitir injeção externa dos Mixins (Crafting e Pickup)
    public static void addActionAndCheckFlush(String actionType, String target, ServerPlayerEntity player, boolean isCritical) {
        UUID uuid = player.getUuid();
        playerBuffers.putIfAbsent(uuid, new ArrayList<>());
        List<ActionEntry> buffer = playerBuffers.get(uuid);

        long now = System.currentTimeMillis();

        synchronized (buffer) {
            if (!buffer.isEmpty()) {
                ActionEntry lastEntry = buffer.get(buffer.size() - 1);
                
                if (lastEntry.getActionType().equals(actionType) && lastEntry.getTarget().equals(target)) {
                    if (now - lastEntry.getLastTimestamp() >= 250L) {
                        if (lastEntry.getCount() < 30) {
                            lastEntry.incrementCount();
                        }
                        lastEntry.updateTimestamp(now); 
                        addToSessionBuffer(uuid, actionType, target, now);
                    }
                } else {
                    buffer.add(new ActionEntry(actionType, target, now));
                    addToSessionBuffer(uuid, actionType, target, now);
                }
            } else {
                buffer.add(new ActionEntry(actionType, target, now));
                addToSessionBuffer(uuid, actionType, target, now);
            }

            if (isCritical) {
                prepareAndFlushPayload(player, buffer, now);
            }
        }
    }

    private static void addToSessionBuffer(UUID uuid, String actionType, String target, long now) {
        List<ActionEntry> sessionBuffer = sessionBuffers.get(uuid);
        if (sessionBuffer == null) return;
        
        synchronized (sessionBuffer) {
            boolean found = false;
            for (ActionEntry entry : sessionBuffer) {
                if (entry.getActionType().equals(actionType) && entry.getTarget().equals(target)) {
                    entry.incrementCount(); 
                    entry.updateTimestamp(now);
                    found = true;
                    break;
                }
            }
            if (!found) {
                sessionBuffer.add(new ActionEntry(actionType, target, now));
            }
        }
    }

    private static void prepareAndFlushPayload(ServerPlayerEntity player, List<ActionEntry> buffer, long flushTime) {
        UUID uuid = player.getUuid();
        List<ActionEntry> snapshot = new ArrayList<>(buffer);
        buffer.clear();
        lastFlushTimes.put(uuid, flushTime);

        float health = player.getHealth();
        int hunger = player.getHungerManager().getFoodLevel();
        int yLevel = (int) player.getY();

        List<String> currentHotbar = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            currentHotbar.add(stack.isEmpty() ? "Empty" : stack.getItem().getName().getString());
        }

        boolean hotbarChanged = false;
        List<String> cachedHotbar = hotbarCaches.get(uuid);
        if (cachedHotbar == null || !cachedHotbar.equals(currentHotbar)) {
            hotbarChanged = true;
            hotbarCaches.put(uuid, currentHotbar);
        }

        final boolean sendHotbar = hotbarChanged;
        CompletableFuture.runAsync(() -> {
            buildAndSendJson(snapshot, health, hunger, yLevel, currentHotbar, sendHotbar);
        });
    }

    private static void prepareAndFlushSessionPayload(ServerPlayerEntity player, List<ActionEntry> sessionBuffer, long flushTime) {
        UUID uuid = player.getUuid();
        List<ActionEntry> snapshot = new ArrayList<>(sessionBuffer);
        sessionBuffer.clear();
        lastSessionFlushTimes.put(uuid, flushTime);

        CompletableFuture.runAsync(() -> {
            StringBuilder summary = new StringBuilder("RESUMO DOS ÚLTIMOS 10 MINUTOS:\n");
            boolean hasMeaningfulData = false;
            
            for (ActionEntry entry : snapshot) {
                if (entry.getCount() >= 10 || entry.getActionType().equals("Morreu") || entry.getActionType().equals("Achievement")) {
                    summary.append(entry.formatOutput()).append("\n");
                    hasMeaningfulData = true;
                }
            }
            
            if (!hasMeaningfulData) return; 
            
            JsonObject payload = new JsonObject();
            payload.addProperty("voice_model", VOICE_MODEL);
            
            JsonArray actionsArray = new JsonArray();
            actionsArray.add(summary.toString());
            payload.add("recent_actions", actionsArray);
            
            JsonArray statesArray = new JsonArray();
            statesArray.add("Atenção: Faça uma avaliação geral do progresso (ou falta dele) baseada neste resumo de longo prazo.");
            payload.add("critical_states", statesArray);

            HttpAssistant.sendStructuredTelemetry(payload.toString());
        });
    }

    private static void buildAndSendJson(List<ActionEntry> snapshot, float health, int hunger, int yLevel, List<String> hotbar, boolean sendHotbar) {
        JsonObject payload = new JsonObject();
        payload.addProperty("voice_model", VOICE_MODEL);

        JsonArray statesArray = new JsonArray();
        if (health <= CRITICAL_HEALTH_THRESHOLD) statesArray.add("Risco de Morte (Vida Crítica): " + (int) health + " de vida");
        if (hunger <= CRITICAL_HUNGER_THRESHOLD) statesArray.add("Fome Extrema: " + hunger + "/20");
        
        if (yLevel >= 120) {
            statesArray.add("Local: Montanhas altas e picos nevados (Y=" + yLevel + ")");
        } else if (yLevel >= 80) {
            statesArray.add("Local: Platôs, colinas e subidas (Y=" + yLevel + ")");
        } else if (yLevel >= 55) {
            statesArray.add("Local: Nível do mar, planícies e terra firme (Y=" + yLevel + ")");
        } else if (yLevel >= 1) {
            statesArray.add("Local: Subsolo e cavernas comuns (Y=" + yLevel + ")");
        } else if (yLevel == 0) {
            statesArray.add("Local: Transição para ardósia profunda (Y=0)");
        } else if (yLevel >= -63) {
            statesArray.add("Local: Cavernas profundas (Deepslate) (Y=" + yLevel + ")");
        } else {
            statesArray.add("Local: Fim do mundo (Bedrock) (Y=" + yLevel + ")");
        }
        
        payload.add("critical_states", statesArray);

        if (sendHotbar) {
            JsonArray hotbarArray = new JsonArray();
            hotbar.forEach(hotbarArray::add);
            payload.add("hotbar", hotbarArray);
        }

        JsonArray actionsArray = new JsonArray();
        for (ActionEntry entry : snapshot) {
            actionsArray.add(entry.formatOutput());
        }
        payload.add("recent_actions", actionsArray);

        HttpAssistant.sendStructuredTelemetry(payload.toString());
    }

    // --- Inner Object-Oriented Structures ---

    private static class ActionEntry {
        private final String actionType;
        private final String target;
        private int count;
        private long lastTimestamp;

        public ActionEntry(String actionType, String target, long timestamp) {
            this.actionType = actionType;
            this.target = target;
            this.count = 1;
            this.lastTimestamp = timestamp;
        }

        public String getActionType() { return actionType; }
        public String getTarget() { return target; }
        public int getCount() { return count; } 
        public long getLastTimestamp() { return lastTimestamp; } 
        
        public void incrementCount() { this.count++; }
        public void updateTimestamp(long timestamp) { this.lastTimestamp = timestamp; }

        public String formatOutput() {
            return count > 1
                    ? String.format("[%s] %dx %s", actionType, count, target)
                    : String.format("[%s] %s", actionType, target);
        }
    }
}
