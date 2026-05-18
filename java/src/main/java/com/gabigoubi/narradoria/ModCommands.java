package com.gabigoubi.narradoria;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ModCommands {
   public static String currentAiModel = "qwen2.5:3b";
    public static String currentVoiceModel = "pm_alex";

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("persona")
                .then(CommandManager.argument("tipo", StringArgumentType.string())
                .executes(context -> {
                    String tipo = StringArgumentType.getString(context, "tipo");
                    ServerCommandSource source = context.getSource();

                    switch (tipo) {
                        case "mulher.agressiva":
                            currentVoiceModel = "pf_bruna"; 
                            source.sendFeedback(() -> Text.literal("§c[Narrador IA] Persona alterada para Mulher Agressiva!"), false);
                            break;
                        case "mulher.amigavel":
                            currentVoiceModel = "pf_bruna"; 
                            source.sendFeedback(() -> Text.literal("§a[Narrador IA] Persona alterada para Mulher Amigável!"), false);
                            break;
                        case "homem.agressivo":
                            currentVoiceModel = "pm_alex"; 
                            source.sendFeedback(() -> Text.literal("§c[Narrador IA] Persona alterada para Homem Agressivo!"), false);
                            break;
                        case "homem.amigavel":
                            currentVoiceModel = "pm_alex"; 
                            source.sendFeedback(() -> Text.literal("§a[Narrador IA] Persona alterada para Homem Amigável!"), false);
                            break;
                        default:
                            source.sendError(Text.literal("[Narrador IA] Opção inválida! Escolha entre: mulher.agressiva, mulher.amigavel, homem.agressivo ou homem.amigavel."));
                            break;
                    }
                    return 1;
                })));
        });
    }
}