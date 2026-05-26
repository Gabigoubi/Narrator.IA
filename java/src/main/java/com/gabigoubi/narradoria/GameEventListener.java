package com.gabigoubi.narradoria;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
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

/**
 * GameEventListener serves as the primary telemetry sensor for the Narrador IA mod.
 * It listens to in-game events, categorizes their semantic priority, and batches them
 * into short-term (90s) and long-term (10m) memory buffers to prevent network spam
 * while maintaining deep narrative context for the AI.
 */
public class GameEventListener {

    // --- Configuration Constants ---
    private static final String VOICE_MODEL = "pm_alex";
    private static final int MAX_BUFFER_SIZE = 30; // Maximum unique actions per short-term window
    private static final long FLUSH_INTERVAL_MS = 90000L; // 90 seconds short-term window
    private static final long SESSION_INTERVAL_MS = 600000L; // 10 minutes session window

    private static final float CRITICAL_HEALTH_THRESHOLD = 4.0f; // 2 Hearts
    private static final int CRITICAL_HUNGER_THRESHOLD = 4; // 2 Drumsticks
    private static final long EASTER_EGG_TIME_MS = 900000L; // 15 minutos (900.000 ms)
    private static final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> musicPlayed = new ConcurrentHashMap<>();

    // --- State Management (Thread-Safe Maps) ---
    private static final Map<UUID, List<ActionEntry>> playerBuffers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastFlushTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, List<String>> hotbarCaches = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastEatTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ActionEntry>> sessionBuffers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSessionFlushTimes = new ConcurrentHashMap<>();

    // ========================================================================
    // 1. EVENT REGISTRATION (INITIALIZATION)
    // ========================================================================

    /**
     * Bootstraps all game event listeners. Called once during mod initialization.
     */
    public static void register() {
        registerConnectionEvents();
        registerInteractionEvents();
        registerCombatEvents();
        registerChatAndAdvancements();
        registerWorldAndEntityEvents();
        registerTickEvent();
    }

    private static void registerConnectionEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player != null) {
                UUID uuid = player.getUuid();
                joinTimes.put(uuid, System.currentTimeMillis());
                musicPlayed.put(uuid, false);
                // Initialize memory states for the new player
                playerBuffers.putIfAbsent(uuid, new ArrayList<>());
                lastFlushTimes.putIfAbsent(uuid, System.currentTimeMillis());
                sessionBuffers.putIfAbsent(uuid, new ArrayList<>());
                lastSessionFlushTimes.putIfAbsent(uuid, System.currentTimeMillis());

                // Welcome message and Telemetry status
                player.sendMessage(Text.literal("§a[Narrador.IA v1.3.1] §fAdvanced Telemetry Online!"), false);

                // Critical Update Warning (UI Notification)
                player.sendMessage(Text.literal("§c[ATENÇÃO] §eSE VOCÊ ATUALIZOU A VERSÃO, DELETE A PASTA INTEIRA QUE VOCÊ BAIXOU NO GITHUB HUB, E INICIE A INSTALAÇÃO NOVAMENTE (VAI INSTALAR MUITO RÁPIDO)"), false);
                player.sendMessage(Text.literal("§eA VERSÃO DO ARQUIVO TEM QUE ESTAR DE ACORDO COM A VERSÃO DO MOD. EX: Narrator.IA-dev-v1.3.1, versão do mod: narrador_ia-v1.3.1.jar"), false);
                player.sendMessage(Text.literal("§cSENÃO O MOD NÃO VAI FUNCIONAR!"), false);

                // Inject initial directive to the LLM
                String welcomeInstruction = String.format("O jogador %s entrou no mundo, duvide da capacidade cognitiva dele, e humilhe ele!", player.getName().getString());
                addActionAndCheckFlush("BOAS-VINDAS", welcomeInstruction, player, true);
            }
        });


        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            // Cleanup memory to prevent memory leaks when players leave
            playerBuffers.remove(uuid);
            lastFlushTimes.remove(uuid);
            hotbarCaches.remove(uuid);
            sessionBuffers.remove(uuid);
            lastSessionFlushTimes.remove(uuid);
            joinTimes.remove(uuid);
            musicPlayed.remove(uuid);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            addActionAndCheckFlush("System", "Player respawned after death", newPlayer, true);
        });
    }

    private static void registerWorldAndEntityEvents() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            String originName = origin.getRegistryKey().getValue().getPath();
            String destName = destination.getRegistryKey().getValue().getPath();
            String context = String.format("Viajou de '%s' para '%s'", originName, destName);
            addActionAndCheckFlush("Dimension Changed", context, (ServerPlayerEntity) player, true);
        });

        EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayerEntity sleeper) {
                addActionAndCheckFlush("Slept", "Cama", sleeper, false);
            }
        });

        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayerEntity sleeper) {
                addActionAndCheckFlush("Woke Up", "Cama", sleeper, false);
            }
        });
    }

    private static void registerChatAndAdvancements() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender != null) {
                addActionAndCheckFlush("Chat", message.getContent().getString(), sender, true); // Elevei para TRUE para disparo imediato
            }
        });

        ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
            String text = message.getString();

            if (text.contains("alcançou o progresso") || text.contains("fez o progresso") || text.contains("has made the advancement")) {
                if (!server.getPlayerManager().getPlayerList().isEmpty()) {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayerList().get(0);


                    String achievementName = text.substring(text.indexOf("[") + 1, text.lastIndexOf("]"));

                    addActionAndCheckFlush("Achievement", achievementName, player, false);
                }
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
                        // Prevent spamming eat events by enforcing a 2-second cooldown
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

                // Forces immediate transmission if health reaches critical levels
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

    // ========================================================================
    // 2. CORE LOGIC (BATCHING & PRIORITIZATION)
    // ========================================================================

    /**
     * Determines the narrative value of an action to prevent critical events
     * from being overwritten by trivial block-breaking spam in the buffer.
     */
    private static int determinePriority(String actionType) {
        return switch (actionType) {
            case "Morreu", "Took Damage", "Chat", "Achievement", "Woke Up", "Slept" -> 3; // High Tier
            case "Attacked", "Dimension Changed", "Crafted", "Consumed", "Picked Up", "Dropped" -> 2; // Mid Tier
            default -> 1; // Low Tier (e.g., Broke, Placed, Used)
        };
    }

    /**
     * Core router for all game events. Handles deduplication, priority-based eviction,
     * and immediate critical flushes.
     */
    public static void addActionAndCheckFlush(String actionType, String target, ServerPlayerEntity player, boolean isCritical) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        UUID uuid = player.getUuid();
        playerBuffers.putIfAbsent(uuid, new ArrayList<>());
        List<ActionEntry> buffer = playerBuffers.get(uuid);

        long now = System.currentTimeMillis();
        int eventPriority = determinePriority(actionType); 

        synchronized (buffer) {
            boolean isDuplicate = false;

            // 1. Deduplication Logic: Group sequential repetitive actions
            if (!buffer.isEmpty()) {
                ActionEntry lastEntry = buffer.get(buffer.size() - 1);

                if (lastEntry.getActionType().equals(actionType) && lastEntry.getTarget().equals(target)) {
                    if (now - lastEntry.getLastTimestamp() >= 250L) {
                        if (lastEntry.getCount() < 30) {
                            lastEntry.incrementCount();
                        }
                        lastEntry.updateTimestamp(now);
                        addToSessionBuffer(uuid, actionType, target, eventPriority, now);
                    }
                    isDuplicate = true;
                }
            }

            // 2. Priority Eviction Logic: Manage buffer constraints
            if (!isDuplicate) {
                if (buffer.size() >= MAX_BUFFER_SIZE) {
                    int lowestPriorityIndex = -1;
                    int lowestPriorityValue = Integer.MAX_VALUE;

                    // Scan for the least important event currently in the buffer
                    for (int i = 0; i < buffer.size(); i++) {
                        if (buffer.get(i).getPriority() < lowestPriorityValue) {
                            lowestPriorityValue = buffer.get(i).getPriority();
                            lowestPriorityIndex = i;
                        }
                    }

                    // Eject weakest event to make room if the new event is important enough
                    if (eventPriority >= lowestPriorityValue && lowestPriorityIndex != -1) {
                        buffer.remove(lowestPriorityIndex);
                        buffer.add(new ActionEntry(actionType, target, eventPriority, now));
                        addToSessionBuffer(uuid, actionType, target, eventPriority, now);
                    }
                } else {
                    buffer.add(new ActionEntry(actionType, target, eventPriority, now));
                    addToSessionBuffer(uuid, actionType, target, eventPriority, now);
                }
            }

            // 3. Immediate bypass for high-stakes events (e.g., Death)
            if (isCritical) {
                prepareAndFlushPayload(player, buffer, now);
            }
        }
    }

    private static void addToSessionBuffer(UUID uuid, String actionType, String target, int priority, long now) {
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
                sessionBuffer.add(new ActionEntry(actionType, target, priority, now));
            }
        }
    }

    // ========================================================================
    // 3. CLOCK CYCLES & TRANSPORT PIPELINE
    // ========================================================================

    private static void registerTickEvent() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = System.currentTimeMillis();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                Long joinTime = joinTimes.get(uuid);
                Boolean played = musicPlayed.getOrDefault(uuid, true);

                if (joinTime != null && !played) {
                    if (now - joinTime >= EASTER_EGG_TIME_MS) {
                        musicPlayed.put(uuid, true); // Trava para não tocar em loop infinito
                        playEdsonHitSong();
                    }
                }
                // Check Short-term Window (90 seconds)
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

                // Check Long-term Session Window (10 minutes)
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

    private static void prepareAndFlushPayload(ServerPlayerEntity player, List<ActionEntry> buffer, long flushTime) {
        UUID uuid = player.getUuid();
        
        // Take a snapshot and clear the main buffer safely
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
        
        // Offload JSON building and HTTP transport to an async thread 
        // to prevent Server Tick freezing (lag).
        CompletableFuture.runAsync(() -> {
            buildAndSendJson(snapshot, health, hunger, yLevel, currentHotbar);
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

            // Only forward actions that were highly repetitive or critically narrative
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

    private static void buildAndSendJson(List<ActionEntry> snapshot, float health, int hunger, int yLevel, List<String> hotbar) {
        JsonObject payload = new JsonObject();
        payload.addProperty("voice_model", VOICE_MODEL);

        JsonArray statesArray = new JsonArray();
        if (health <= CRITICAL_HEALTH_THRESHOLD) statesArray.add("Risco de Morte (Vida Crítica): " + (int) health + " de vida");
        if (hunger <= CRITICAL_HUNGER_THRESHOLD) statesArray.add("Fome Extrema: " + hunger + "/20");

        // Spatial interpretation based on Y coordinates
        if (yLevel >= 120) {
            statesArray.add("Local: Montanhas altas e picos nevados");
        } else if (yLevel >= 80) {
            statesArray.add("Local: Platôs, colinas e subidas");
        } else if (yLevel >= 55) {
            statesArray.add("Local: Nível do mar, planícies e terra firme");
        } else if (yLevel >= 1) {
            statesArray.add("Local: Subsolo e cavernas comuns");
        } else if (yLevel == 0) {
            statesArray.add("Local: Transição para ardósia profunda");
        } else if (yLevel >= -63) {
            statesArray.add("Local: Cavernas profundas (Deepslate)");
        } else {
            statesArray.add("Local: Fim do mundo (Bedrock)");
        }

        payload.add("critical_states", statesArray);

        JsonArray hotbarArray = new JsonArray();
        hotbar.forEach(hotbarArray::add);
        payload.add("hotbar", hotbarArray);

        JsonArray actionsArray = new JsonArray();
        for (ActionEntry entry : snapshot) {
            actionsArray.add(entry.formatOutput());
        }
        payload.add("recent_actions", actionsArray);

        HttpAssistant.sendStructuredTelemetry(payload.toString());
    }

    // ========================================================================
    // 4. DATA STRUCTURES
    // ========================================================================

    /**
     * Represents a single, encapsulable in-game action.
     * Contains built-in counter logic and priority classification.
     */
    private static void playEdsonHitSong() {
        // Executa em uma Thread assíncrona isolada para o Minecraft não congelar (lagar)
        CompletableFuture.runAsync(() -> {
            try {
                // Busca o arquivo diretamente das entranhas do JAR do Mod
                java.io.InputStream resourceStream = GameEventListener.class.getResourceAsStream("/edson_hit.wav");

                if (resourceStream == null) {
                    NarradorIAMod.LOGGER.warn("[Narrador IA] Easter Egg falhou: edson_hit.wav nao encontrado dentro do JAR!");
                    return;
                }

                // Transforma em um buffer stream. O Java precisa disso para conseguir processar o WAV corretamente
                java.io.InputStream bufferedStream = new java.io.BufferedInputStream(resourceStream);
                javax.sound.sampled.AudioInputStream audioStream = javax.sound.sampled.AudioSystem.getAudioInputStream(bufferedStream);
                javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();

                clip.open(audioStream);

                // Eleva o ganho do volume para garantir que vai tocar no hype total
                if (clip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                    javax.sound.sampled.FloatControl volume = (javax.sound.sampled.FloatControl) clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                    volume.setValue(2.0f);
                }

                System.out.println("[Narrador IA] 15 Minutos de sobrevivência! SOLTA O BEAT, DJ EDSON!!!");
                clip.start();

            } catch (Exception e) {
                NarradorIAMod.LOGGER.error("[Narrador IA] Erro ao reproduzir o hit embutido do Edson: ", e);
            }
        });
    }
    private static class ActionEntry {
        private final String actionType;
        private final String target;
        private final int priority; 
        private int count;
        private long lastTimestamp;

        public ActionEntry(String actionType, String target, int priority, long timestamp) {
            this.actionType = actionType;
            this.target = target;
            this.priority = priority;
            this.count = 1;
            this.lastTimestamp = timestamp;
        }

        public String getActionType() { return actionType; }
        public String getTarget() { return target; }
        public int getPriority() { return priority; } 
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
