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

public class GameEventListener {

    private static final String VOICE_MODEL = "pm_alex";
    private static final int MAX_BUFFER_SIZE = 30; 
    private static final long FLUSH_INTERVAL_MS = 90000L;
    private static final long IDLE_TIMEOUT_MS = 180000L;
    private static final long IDLE_COOLDOWN_MS = 600000L;
    private static final long SESSION_INTERVAL_MS = 600000L;

    private static final float CRITICAL_HEALTH_THRESHOLD = 4.0f;
    private static final int CRITICAL_HUNGER_THRESHOLD = 4;
    private static final long EASTER_EGG_TIME_MS = 900000L;
    
    private static final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> musicPlayed = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ActionEntry>> playerBuffers = new ConcurrentHashMap<>();
    
    // NOVO: Buffer Especial de Tier 1 durante a narração
    private static final Map<UUID, List<ActionEntry>> tempTier1Buffers = new ConcurrentHashMap<>();
    
    private static final Map<UUID, Long> lastFlushTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastIdleTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, List<String>> hotbarCaches = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastEatTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ActionEntry>> sessionBuffers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSessionFlushTimes = new ConcurrentHashMap<>();
    
    private static final Map<UUID, Boolean> inDeepDark = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> isRainingCache = new ConcurrentHashMap<>();

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
                tempTier1Buffers.putIfAbsent(uuid, new ArrayList<>()); // Inicializa o buffer especial
                lastFlushTimes.putIfAbsent(uuid, System.currentTimeMillis());
                lastIdleTimes.putIfAbsent(uuid, 0L);
                sessionBuffers.putIfAbsent(uuid, new ArrayList<>());
                lastSessionFlushTimes.putIfAbsent(uuid, System.currentTimeMillis());
                inDeepDark.putIfAbsent(uuid, false);
                isRainingCache.putIfAbsent(uuid, false);

                player.sendMessage(Text.literal("§a[Narrador.IA v1.4.1] §fSistema Anti-Perda de Eventos Online!"), false);
                addActionAndCheckFlush("BOAS-VINDAS", "Entrou no mundo", player, false);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            playerBuffers.remove(uuid);
            tempTier1Buffers.remove(uuid);
            lastFlushTimes.remove(uuid);
            lastIdleTimes.remove(uuid);
            hotbarCaches.remove(uuid);
            sessionBuffers.remove(uuid);
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
                addActionAndCheckFlush("Broke", state.getBlock().getName().getString(), serverPlayer, false);
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
                    long now = System.currentTimeMillis();
                    if (stack.getItem().getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD)) {
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

    private static int determineTier(String actionType) {
        return switch (actionType) {
            case "Morreu", "Chat", "Achievement", "Dimension Changed", "BOAS-VINDAS", "Deep Dark" -> 1; 
            case "Crafted", "Slept", "Took Damage", "Woke Up", "Ociosidade", "Tool Broke", "Tamed", "Breeding", "Traded", "Deep Dark Exit" -> 2; 
            default -> 3; 
        };
    }

    public static void addActionAndCheckFlush(String actionType, String target, ServerPlayerEntity player, boolean isCritical) {
        if (player.isCreative() || player.isSpectator()) return;
        
        UUID uuid = player.getUuid();
        int eventTier = determineTier(actionType); 
        long now = System.currentTimeMillis();

        // ---------------------------------------------------------
        // LÓGICA DO BUFFER ESPECIAL DURANTE NARRAÇÃO (TTS LOCK)
        // ---------------------------------------------------------
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
            return; // Ignora e descarta silenciosamente eventos Tier 2 e 3
        }

        // Se o sistema NÃO está narrando, despeja eventos pendentes do Tier 1 no fluxo normal
        List<ActionEntry> tempBuffer = tempTier1Buffers.get(uuid);
        List<ActionEntry> buffer = playerBuffers.get(uuid);

        if (buffer != null) {
            synchronized (buffer) {
                if (tempBuffer != null && !tempBuffer.isEmpty()) {
                    synchronized (tempBuffer) {
                        buffer.addAll(tempBuffer);
                        NarradorIAMod.LOGGER.info("[Narrador IA - DEBUG] Buffer Especial esvaziado para o principal. (" + tempBuffer.size() + " eventos críticos restaurados).");
                        tempBuffer.clear();
                    }
                }

                // Fluxo Normal
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
                        boolean reachedVolume = buffer.size() >= 20;
                        boolean reachedTime = timeElapsed >= FLUSH_INTERVAL_MS;

                        if ((reachedVolume || reachedTime) && hasTier1or2) {
                            // TRAVA DE SEGURANÇA: Só esvazia e envia se o Edson NÃO estiver falando.
                            if (HttpAssistant.isNarrating()) {
                                if (isOneSecondTick) { // Loga apenas a cada 1 seg para não floodar o console
                                    NarradorIAMod.LOGGER.info("[Narrador IA - DEBUG] Retendo o disparo do Buffer: O Edson ainda está falando.");
                                }
                            } else {
                                prepareAndFlushPayload(player, buffer, now);
                            }
                        } else if (timeElapsed >= IDLE_TIMEOUT_MS && !hasTier1or2) {
                            if (!HttpAssistant.isNarrating()) { // Não dá esporro se já estiver falando algo
                                long lastIdle = lastIdleTimes.getOrDefault(uuid, 0L);
                                if (now - lastIdle >= IDLE_COOLDOWN_MS) {
                                    addActionAndCheckFlush("Ociosidade", "Jogador nao fez nenhum progresso util nos ultimos minutos", player, false);
                                    lastIdleTimes.put(uuid, now);
                                } else {
                                    buffer.clear();
                                    lastFlushTimes.put(uuid, now);
                                }
                            }
                        }
                    }
                }

                List<ActionEntry> sessionBuffer = sessionBuffers.get(uuid);
                if (sessionBuffer != null && !sessionBuffer.isEmpty()) {
                    long lastSessionFlush = lastSessionFlushTimes.getOrDefault(uuid, now);
                    if (now - lastSessionFlush >= SESSION_INTERVAL_MS) {
                        synchronized (sessionBuffer) {
                            if (!sessionBuffer.isEmpty() && !HttpAssistant.isNarrating()) {
                                prepareAndFlushSessionPayload(player, sessionBuffer, now);
                          