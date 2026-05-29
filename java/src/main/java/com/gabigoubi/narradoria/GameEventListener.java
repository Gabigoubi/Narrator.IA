package com.gabigoubi.narradoria;

// --- Imports do Fabric e Minecraft ---
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
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;

// --- Imports do Java e Bibliotecas ---
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * Motor central de telemetria do Narrador IA.
 * Gerencia buffers, classifica Tiers e controla o ciclo de disparo (Tick).
 */
public class GameEventListener {

    // ========================================================================
    // CONSTANTES E CONFIGURAÇÕES DE TEMPO
    // ========================================================================
    private static final String VOICE_MODEL = "pm_alex";
    private static final int MAX_BUFFER_SIZE = 30;
    private static final long FLUSH_INTERVAL_MS = 90000L;
    private static final long IDLE_TIMEOUT_MS = 180000L;
    private static final long IDLE_COOLDOWN_MS = 600000L;
    private static final long SESSION_INTERVAL_MS = 1200000L; // ATUALIZADO: 20 Minutos
    private static final float CRITICAL_HEALTH_THRESHOLD = 4.0f;
    private static final int CRITICAL_HUNGER_THRESHOLD = 4;
    private static final long EASTER_EGG_TIME_MS = 900000L;

    // ========================================================================
    // MAPAS DE ESTADO (THREAD-SAFE)
    // ========================================================================
    private static final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> musicPlayed = new ConcurrentHashMap<>();

    // Buffers de Eventos
    private static final Map<UUID, List<ActionEntry>> playerBuffers = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ActionEntry>> tempTier1Buffers = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ActionEntry>> sessionBuffers = new ConcurrentHashMap<>();

    // Controle de Tempo e Ociosidade
    private static final Map<UUID, Long> lastFlushTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastIdleTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSessionFlushTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastEatTimes = new ConcurrentHashMap<>();

    // Caches e Sensores
    private static final Map<UUID, List<String>> hotbarCaches = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> inDeepDark = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> isRainingCache = new ConcurrentHashMap<>();

    // ========================================================================
    // INICIALIZAÇÃO DE EVENTOS NATIVOS
    // ========================================================================
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

                playerBuffers.putIfAbsent(uuid, new ArrayList<>());
                tempTier1Buffers.putIfAbsent(uuid, new ArrayList<>());
                sessionBuffers.putIfAbsent(uuid, new ArrayList<>());

                lastFlushTimes.putIfAbsent(uuid, System.currentTimeMillis());
                lastIdleTimes.putIfAbsent(uuid, 0L);
                lastSessionFlushTimes.putIfAbsent(uuid, System.currentTimeMillis());

                inDeepDark.putIfAbsent(uuid, false);
                isRainingCache.putIfAbsent(uuid, false);


                player.sendMessage(Text.literal("§c[Narrador IA] §fEdson Calotas entrou no mundo. Prepare-se para ser esculachado!"), false);
                player.sendMessage(Text.literal("§e[Aviso] §fSe você usava a versão anterior, é OBRIGATÓRIO atualizar a pasta 'server'. Baixe os arquivos novos no GitHub! (O Link está na descrição do mod)"), false);
                player.sendMessage(Text.literal("§b[Suporte] §fQuer uma ajuda direta? Fale comigo através do meu server no Discord! ")
                        .append(Text.literal("§b§n[CLIQUE AQUI PARA ENTRAR]")
                                .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/G6tNE5bQbH")))), false);
                addActionAndCheckFlush("BOAS-VINDAS", "Entrou no mundo", player, false);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            playerBuffers.remove(uuid);
            tempTier1Buffers.remove(uuid);
            sessionBuffers.remove(uuid);
            lastFlushTimes.remove(uuid);
            lastIdleTimes.remove(uuid);
            hotbarCaches.remove(uuid);
            lastSessionFlushTimes.remove(uuid);
            joinTimes.remove(uuid);
            musicPlayed.remove(uuid);
            inDeepDark.remove(uuid);
            isRainingCache.remove(uuid);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            addActionAndCheckFlush("System", "Player respawned after death", newPlayer, false);
        });
    }

    private static void registerWorldAndEntityEvents() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            String originName = origin.getRegistryKey().getValue().getPath();
            String destName = destination.getRegistryKey().getValue().getPath();
            addActionAndCheckFlush("Dimension Changed", String.format("Viajou de '%s' para '%s'", originName, destName), (ServerPlayerEntity) player, false);
        });

        EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayerEntity sleeper) addActionAndCheckFlush("Slept", "Cama", sleeper, false);
        });

        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayerEntity sleeper) addActionAndCheckFlush("Woke Up", "Cama", sleeper, false);
        });
    }

    private static void registerChatAndAdvancements() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender != null) addActionAndCheckFlush("Chat", message.getContent().getString(), sender, false);
        });

        ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
            String text = message.getString();
            if (text.contains("alcançou o progresso") || text.contains("fez o progresso") || text.contains("conseguiu a conquista")) {
                if (!server.getPlayerManager().getPlayerList().isEmpty()) {
                    addActionAndCheckFlush("Achievement", text, server.getPlayerManager().getPlayerList().get(0), false);
                }
            }
        });
    }

    private static void registerInteractionEvents() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                String blockName = state.getBlock().getName().getString();
                int yLevel = pos.getY();

                // Lógica afiada: O cara só está "Minerando" de verdade se acertar o minério nas profundezas!
                if (yLevel <= 50 && (blockName.contains("Ore") || blockName.contains("Minério"))) {
                    addActionAndCheckFlush("Minerando", blockName, serverPlayer, false);
                } else {
                    addActionAndCheckFlush("Broke", blockName, serverPlayer, false);
                }
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient() && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity serverPlayer) {

                Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
                if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL) {
                    addActionAndCheckFlush("Opened", "Baú/Barril", serverPlayer, false);
                } else if (block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) {
                    addActionAndCheckFlush("Used Station", "Bigorna", serverPlayer, false);
                } else if (block == Blocks.SMITHING_TABLE) {
                    addActionAndCheckFlush("Used Station", "Mesa de Ferraria", serverPlayer, false);
                }

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
                    long now = System.currentTimeMillis();

                    if (stack.getItem() instanceof PotionItem) {
                        addActionAndCheckFlush("Consumed", "Poção (" + stack.getName().getString() + ")", serverPlayer, false);
                    }
                    else if (stack.getItem().getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD)) {
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
                String attacker = source.getAttacker() != null ? source.getAttacker().getName().getString() : "Environment";
                addActionAndCheckFlush("Took Damage", String.format("%s (Source: %s)", source.getName(), attacker), serverPlayer, false);
            }
            return true;
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity serverPlayer) {
                addActionAndCheckFlush("Morreu", damageSource.getDeathMessage(serverPlayer).getString(), serverPlayer, false);
            }
            return true;
        });
    }

    // ========================================================================
    // MOTOR LÓGICO E CLASSIFICAÇÃO
    // ========================================================================
    private static int determineTier(String actionType) {
        return switch (actionType) {
            case "Morreu", "Chat", "Achievement", "Dimension Changed", "BOAS-VINDAS", "Deep Dark" -> 1;

            case "Crafted", "Slept", "Took Damage", "Woke Up", "Ociosidade", "Tool Broke", "Tamed", "Breeding", "Traded", "Comércio", "Deep Dark Exit", "Pescou", "Used Station", "Encantou", "Minerando" -> 2;
            default -> 3;
        };
    }

    public static void addActionAndCheckFlush(String actionType, String target, ServerPlayerEntity player, boolean isCritical) {
        if (player.isCreative() || player.isSpectator()) return;


        // 🚨 FILTRO PRECISO ANTI-AR: Descarta apenas se a string for literalmente o vazio ou a entidade "Air"
        if (target != null) {
            String t = target.trim(); // Remove espaços em branco antes ou depois
            if (t.isEmpty() || t.equalsIgnoreCase("Ar") || t.equalsIgnoreCase("Air") || t.equalsIgnoreCase("0x Ar") || t.equalsIgnoreCase("0x Air")) {
                return;
            }
        }


        UUID uuid = player.getUuid();
        int eventTier = determineTier(actionType);
        long now = System.currentTimeMillis();

        if (HttpAssistant.isNarrating()) {
            if (eventTier == 1) {
                List<ActionEntry> tempBuffer = tempTier1Buffers.get(uuid);
                if (tempBuffer != null) {
                    synchronized (tempBuffer) {
                        tempBuffer.add(new ActionEntry(actionType, target, eventTier, now));
                        NarradorIAMod.LOGGER.info(String.format("[Narrador IA - DEBUG] Sistema Ocupado. Salvo no Buffer Especial: [%s] (Tier 1)", actionType));
                    }
                }
            }
            return;
        }

        List<ActionEntry> tempBuffer = tempTier1Buffers.get(uuid);
        List<ActionEntry> buffer = playerBuffers.get(uuid);

        if (buffer != null) {
            synchronized (buffer) {
                if (tempBuffer != null && !tempBuffer.isEmpty()) {
                    synchronized (tempBuffer) {
                        buffer.addAll(tempBuffer);
                        NarradorIAMod.LOGGER.info("[Narrador IA - DEBUG] Buffer Especial esvaziado para o principal. (" + tempBuffer.size() + " eventos restaurados).");
                        tempBuffer.clear();
                    }
                }

                boolean isDuplicate = false;
                if (!buffer.isEmpty()) {
                    ActionEntry lastEntry = buffer.get(buffer.size() - 1);
                    if (lastEntry.getActionType().equals(actionType) && lastEntry.getTarget().equals(target)) {
                        if (now - lastEntry.getLastTimestamp() >= 250L) {
                            if (lastEntry.getCount() < 30) lastEntry.incrementCount();
                            lastEntry.updateTimestamp(now);
                            addToSessionBuffer(uuid, actionType, target, eventTier, now);
                        }
                        isDuplicate = true;
                    }
                }

                if (!isDuplicate) {
                    if (buffer.size() >= MAX_BUFFER_SIZE) {
                        int highestTierIndex = -1;
                        int highestTierValue = -1;
                        for (int i = 0; i < buffer.size(); i++) {
                            if (buffer.get(i).getTier() > highestTierValue) {
                                highestTierValue = buffer.get(i).getTier();
                                highestTierIndex = i;
                            }
                        }
                        if (eventTier <= highestTierValue && highestTierIndex != -1) {
                            buffer.remove(highestTierIndex);
                            buffer.add(new ActionEntry(actionType, target, eventTier, now));
                            addToSessionBuffer(uuid, actionType, target, eventTier, now);
                        }
                    } else {
                        buffer.add(new ActionEntry(actionType, target, eventTier, now));
                        addToSessionBuffer(uuid, actionType, target, eventTier, now);
                    }
                }
            }
        }
    }

    private static void addToSessionBuffer(UUID uuid, String actionType, String target, int tier, long now) {
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
            if (!found) sessionBuffer.add(new ActionEntry(actionType, target, tier, now));
        }
    }

    // ========================================================================
    // RELÓGIO DO SERVIDOR (CONTROLE DE TEMPO E SENSORES ATIVOS)
    // ========================================================================
    private static void registerTickEvent() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = System.currentTimeMillis();
            boolean isOneSecondTick = server.getTicks() % 20 == 0;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();

                if (isOneSecondTick && !player.isCreative() && !player.isSpectator()) {
                    player.getWorld().getBiome(player.getBlockPos()).getKey().ifPresent(biomeKey -> {
                        String biomeName = biomeKey.getValue().getPath();
                        boolean currentlyInDeepDark = biomeName.equals("deep_dark");
                        boolean wasInDeepDark = inDeepDark.getOrDefault(uuid, false);

                        if (currentlyInDeepDark && !wasInDeepDark) {
                            inDeepDark.put(uuid, true);
                            addActionAndCheckFlush("Deep Dark", "Entrou na escuridao do Deep Dark", player, false);
                        } else if (!currentlyInDeepDark && wasInDeepDark) {
                            inDeepDark.put(uuid, false);
                            addActionAndCheckFlush("Deep Dark Exit", "Escapou do Deep Dark", player, false);
                        }
                    });

                    boolean raining = player.getWorld().isRaining();
                    boolean wasRaining = isRainingCache.getOrDefault(uuid, false);
                    if (raining && !wasRaining) {
                        isRainingCache.put(uuid, true);
                        addActionAndCheckFlush("Weather", "Começou a chover forte", player, false);
                    } else if (!raining && wasRaining) {
                        isRainingCache.put(uuid, false);
                    }
                }

                Long joinTime = joinTimes.get(uuid);
                Boolean played = musicPlayed.getOrDefault(uuid, true);
                if (joinTime != null && !played && now - joinTime >= EASTER_EGG_TIME_MS) {
                    musicPlayed.put(uuid, true);
                    playEdsonHitSong();
                }

                List<ActionEntry> buffer = playerBuffers.get(uuid);
                if (buffer != null) {
                    long lastFlush = lastFlushTimes.getOrDefault(uuid, now);
                    long timeElapsed = now - lastFlush;

                    synchronized (buffer) {
                        boolean hasTier1or2 = buffer.stream().anyMatch(e -> e.getTier() <= 2);
                        boolean hasMinimumVolume = buffer.size() >= 20;

                        if (timeElapsed >= FLUSH_INTERVAL_MS) {

                            if (hasMinimumVolume && hasTier1or2) {
                                if (HttpAssistant.isNarrating()) {
                                    if (isOneSecondTick) {
                                        NarradorIAMod.LOGGER.info("[CICLO INTERNO] ⏳ Retendo disparo: Edson esta falando.");
                                    }
                                } else {
                                    NarradorIAMod.LOGGER.info("==================================================");
                                    NarradorIAMod.LOGGER.info("[CICLO INTERNO] 🚀 DISPARO PERFEITO (90s + 20 Eventos + Tier 1/2)");
                                    NarradorIAMod.LOGGER.info("==================================================");
                                    prepareAndFlushPayload(player, buffer, now);
                                }
                            }
                            else if (timeElapsed >= IDLE_TIMEOUT_MS) {
                                if (!hasTier1or2) {
                                    if (!HttpAssistant.isNarrating()) {
                                        long lastIdle = lastIdleTimes.getOrDefault(uuid, 0L);
                                        if (now - lastIdle >= IDLE_COOLDOWN_MS) {
                                            addActionAndCheckFlush("Ociosidade", "Jogador nao fez nenhum progresso util nos ultimos minutos", player, false);
                                            lastIdleTimes.put(uuid, now);
                                        } else {
                                            buffer.clear();
                                            lastFlushTimes.put(uuid, now);
                                        }
                                    }
                                } else {
                                    if (!HttpAssistant.isNarrating()) {
                                        NarradorIAMod.LOGGER.info("[CICLO INTERNO] 🚀 DISPARO FORÇADO (180s - Baixo volume, mas com relevância)");
                                        prepareAndFlushPayload(player, buffer, now);
                                    }
                                }
                            }
                        }
                    }
                }

                List<ActionEntry> sessionBuffer = sessionBuffers.get(uuid);
                if (sessionBuffer != null && !sessionBuffer.isEmpty()) {
                    long lastSessionFlush = lastSessionFlushTimes.getOrDefault(uuid, now);
                    if (now - lastSessionFlush >= SESSION_INTERVAL_MS) {
                        // Anti-Race Condition garantido com cópia segura antes da execução assíncrona
                        if (!HttpAssistant.isNarrating()) {
                            prepareAndFlushSessionPayload(player, sessionBuffer, now);
                        }
                    }
                }
            }
        });
    }

    // ========================================================================
    // REDE E COMUNICAÇÃO HTTP
    // ========================================================================
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

        CompletableFuture.runAsync(() -> buildAndSendJson(snapshot, health, hunger, yLevel, currentHotbar));
    }

  private static void prepareAndFlushSessionPayload(ServerPlayerEntity player, List<ActionEntry> sessionBuffer, long flushTime) {
        UUID uuid = player.getUuid();
        List<ActionEntry> snapshot;

        // Tranca o buffer principal da sessão, copia rápido e reseta tudo
        synchronized (sessionBuffer) {
            snapshot = new ArrayList<>(sessionBuffer);
            sessionBuffer.clear();
        }

        lastSessionFlushTimes.put(uuid, flushTime);

        CompletableFuture.runAsync(() -> {
            boolean hasMeaningfulData = false;
            JsonArray actionsArray = new JsonArray();

            for (ActionEntry entry : snapshot) {
                if (entry.getCount() >= 10 || entry.getActionType().equals("Morreu") || entry.getActionType().equals("Achievement")) {
                    actionsArray.add(entry.formatOutput());
                    hasMeaningfulData = true;
                }
            }

            if (!hasMeaningfulData) return;

            JsonObject payload = new JsonObject();
            payload.addProperty("voice_model", VOICE_MODEL);
            
            // FLAG ARQUITETURAL: Avisa o Python que isso é um resumo, sem usar linguagem natural
            payload.addProperty("is_session_summary", true);
            
            payload.add("recent_actions", actionsArray);

            HttpAssistant.sendStructuredTelemetry(payload.toString());
        });
    }

    private static void buildAndSendJson(List<ActionEntry> snapshot, float health, int hunger, int yLevel, List<String> hotbar) {
        JsonObject payload = new JsonObject();
        payload.addProperty("voice_model", VOICE_MODEL);
        
        // DADO CRU: Envia apenas o número. O Python que se vire para interpretar.
        payload.addProperty("y_level", yLevel);

        JsonArray statesArray = new JsonArray();
        if (health <= CRITICAL_HEALTH_THRESHOLD) statesArray.add("Risco de Morte (Vida Crítica): " + (int) health + " de vida");
        if (hunger <= CRITICAL_HUNGER_THRESHOLD) statesArray.add("Fome Extrema: " + hunger + "/20");

        payload.add("critical_states", statesArray);

        JsonArray hotbarArray = new JsonArray();
        hotbar.forEach(hotbarArray::add);
        payload.add("hotbar", hotbarArray);

        JsonArray actionsArray = new JsonArray();
        for (ActionEntry entry : snapshot) actionsArray.add(entry.formatOutput());
        payload.add("recent_actions", actionsArray);

        String finalJson = payload.toString();
        NarradorIAMod.LOGGER.info("[Narrador IA - DEBUG] Payload Estruturado Gerado e Enviado.");
        HttpAssistant.sendStructuredTelemetry(finalJson);
    }

    private static void playEdsonHitSong() {
        CompletableFuture.runAsync(() -> {
            try {
                java.io.InputStream resourceStream = GameEventListener.class.getResourceAsStream("/edson_hit.wav");
                if (resourceStream == null) return;
                java.io.InputStream bufferedStream = new java.io.BufferedInputStream(resourceStream);
                javax.sound.sampled.AudioInputStream audioStream = javax.sound.sampled.AudioSystem.getAudioInputStream(bufferedStream);
                javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
                clip.open(audioStream);
                if (clip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                    javax.sound.sampled.FloatControl volume = (javax.sound.sampled.FloatControl) clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                    volume.setValue(2.0f);
                }
                clip.start();
            } catch (Exception e) {
                NarradorIAMod.LOGGER.error("[Narrador IA] Erro ao reproduzir o hit: ", e);
            }
        });
    }

    // ========================================================================
    // CLASSES INTERNAS DE DADOS
    // ========================================================================
    private static class ActionEntry {
        private final String actionType;
        private final String target;
        private final int tier;
        private int count;
        private long lastTimestamp;

        public ActionEntry(String actionType, String target, int tier, long timestamp) {
            this.actionType = actionType;
            this.target = target;
            this.tier = tier;
            this.count = 1;
            this.lastTimestamp = timestamp;
        }

        public String getActionType() { return actionType; }
        public String getTarget() { return target; }
        public int getTier() { return tier; }
        public int getCount() { return count; }
        public long getLastTimestamp() { return lastTimestamp; }
        public void incrementCount() { this.count++; }
        public void updateTimestamp(long timestamp) { this.lastTimestamp = timestamp; }

        public String formatOutput() {
            return count > 1
                    ? String.format("[Tier %d] [%s] %dx %s", tier, actionType, count, target)
                    : String.format("[Tier %d] [%s] %s", tier, actionType, target);
        }
    }
}
