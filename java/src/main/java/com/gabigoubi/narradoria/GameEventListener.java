package com.gabigoubi.narradoria;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
    private static final long FLUSH_INTERVAL_MS = 45000L; // 45 seconds accumulator time

    // --- Threshold Constants ---
    private static final float CRITICAL_HEALTH_THRESHOLD = 4.0f;
    private static final int CRITICAL_HUNGER_THRESHOLD = 4;

    // --- Thread-Safe Encapsulated State (Player Isolation) ---
    private static final Map<UUID, List<ActionEntry>> playerBuffers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastFlushTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, List<String>> hotbarCaches = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastEatTimes = new ConcurrentHashMap<>();

    public static void register() {
        registerConnectionEvents();
        registerInteractionEvents();
        registerCombatEvents();
        registerChatAndAdvancements();
        registerTickEvent(); // NOVO: Relógio de varredura independente
    }

    // --- Event Registrations ---

    private static void registerConnectionEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player != null) {
                // Initialize player isolated state
                UUID uuid = player.getUuid();
                playerBuffers.putIfAbsent(uuid, new ArrayList<>());
                lastFlushTimes.putIfAbsent(uuid, System.currentTimeMillis());

                player.sendMessage(Text.literal("§a[Narrador.IA v1.3] §fAdvanced Telemetry Online!"), false);

                // Prompt Injection within Java: Forcing the LLM to output the specific welcome vibe
                String playerName = player.getName().getString();
                String welcomeInstruction = String.format("O jogador %s acabou de entrar. Receba-o EXATAMENTE com esta vibe: 'Eita, o jogador %s entrou no mundo....... o que será que esse noiao vai fazer hein...'", playerName, playerName);

                // We keep isCritical = true so it flushes immediately while the world is rendering
                addActionAndCheckFlush("BOAS-VINDAS", welcomeInstruction, player, true);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // Clean up memory to prevent leaks
            UUID uuid = handler.getPlayer().getUuid();
            playerBuffers.remove(uuid);
            lastFlushTimes.remove(uuid);
            hotbarCaches.remove(uuid);
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
                        // Trava de 2 segundos (tempo de animação de comer) para evitar spam de eventos
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

                // Enhanced damage context
                String attackerName = source.getAttacker() != null ? source.getAttacker().getName().getString() : "Environment";
                String damageContext = String.format("%s (Source: %s)", source.getName(), attackerName);

                addActionAndCheckFlush("Took Damage", damageContext, serverPlayer, isCritical);
            }
            return true;
        });
    }

    // --- Core Telemetry Logic ---

    // NOVO: Varredura de Tempo Independente baseada no relógio do Servidor
    private static void registerTickEvent() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = System.currentTimeMillis();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                List<ActionEntry> buffer = playerBuffers.get(uuid);

                if (buffer != null && !buffer.isEmpty()) {
                    long lastFlush = lastFlushTimes.getOrDefault(uuid, now);

                    // Se passou o tempo configurado (45s) desde o último envio E tem ações acumuladas
                    if (now - lastFlush >= FLUSH_INTERVAL_MS) {
                        synchronized (buffer) {
                            if (!buffer.isEmpty()) {
                                prepareAndFlushPayload(player, buffer, now);
                            }
                        }
                    }
                }
            }
        });
    }

    private static void addActionAndCheckFlush(String actionType, String target, ServerPlayerEntity player, boolean isCritical) {
        UUID uuid = player.getUuid();
        playerBuffers.putIfAbsent(uuid, new ArrayList<>());
        List<ActionEntry> buffer = playerBuffers.get(uuid);

        long now = System.currentTimeMillis();

        // Strict thread synchronization for the specific player's buffer
        synchronized (buffer) {
            if (!buffer.isEmpty()) {
                ActionEntry lastEntry = buffer.get(buffer.size() - 1);
                
                // Se for a mesma ação no mesmo alvo, entra a validação
                if (lastEntry.getActionType().equals(actionType) && lastEntry.getTarget().equals(target)) {
                    
                    // 1. O ESCUDO ANTI-SPAM (Debounce de 250ms)
                    if (now - lastEntry.getLastTimestamp() >= 250L) {
                        
                        // 2. O TETO DE AGREGAÇÃO (Cap = 30)
                        if (lastEntry.getCount() < 30) {
                            lastEntry.incrementCount();
                        }
                        
                        // Atualiza o relógio independente de ter batido no teto ou não
                        lastEntry.updateTimestamp(now); 
                    }
                } else {
                    buffer.add(new ActionEntry(actionType, target, now));
                }
            } else {
                buffer.add(new ActionEntry(actionType, target, now));
            }

            // O gatilho principal agora é ESTRITAMENTE o tempo (TickEvent).
            // APENAS risco crítico fura a fila.
            if (isCritical) {
                prepareAndFlushPayload(player, buffer, now);
            }
        }
    }

    /**
     * Extracts thread-sensitive data on the Main Thread and creates an immutable snapshot.
     */
    private static void prepareAndFlushPayload(ServerPlayerEntity player, List<ActionEntry> buffer, long flushTime) {
        UUID uuid = player.getUuid();

        // 1. Thread-safe snapshot of the buffer
        List<ActionEntry> snapshot = new ArrayList<>(buffer);
        buffer.clear();
        lastFlushTimes.put(uuid, flushTime);

        // 2. Main-Thread Data Extraction (Cannot be done in CompletableFuture)
        float health = player.getHealth();
        int hunger = player.getHungerManager().getFoodLevel();
        int yLevel = (int) player.getY();

        List<String> currentHotbar = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            currentHotbar.add(stack.isEmpty() ? "Empty" : stack.getItem().getName().getString());
        }

        // 3. Hotbar Diff Strategy
        boolean hotbarChanged = false;
        List<String> cachedHotbar = hotbarCaches.get(uuid);
        if (cachedHotbar == null || !cachedHotbar.equals(currentHotbar)) {
            hotbarChanged = true;
            hotbarCaches.put(uuid, currentHotbar);
        }

        // Pass variables to async scope
        final boolean sendHotbar = hotbarChanged;

        CompletableFuture.runAsync(() -> {
            buildAndSendJson(snapshot, health, hunger, yLevel, currentHotbar, sendHotbar);
        });
    }

    /**
     * Heavy JSON stringification executed entirely on worker thread.
     */
    private static void buildAndSendJson(List<ActionEntry> snapshot, float health, int hunger, int yLevel, List<String> hotbar, boolean sendHotbar) {
        JsonObject payload = new JsonObject();
        payload.addProperty("voice_model", VOICE_MODEL);

        JsonArray statesArray = new JsonArray();
        if (health <= CRITICAL_HEALTH_THRESHOLD) statesArray.add("Risco de Morte (Vida Crítica): " + health + "/20");
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
