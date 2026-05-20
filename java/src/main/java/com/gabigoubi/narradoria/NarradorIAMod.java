package com.gabigoubi.narradoria;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NarradorIAMod implements ModInitializer {
    public static final String MOD_ID = "narrador_ia";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

   @Override
    public void onInitialize() {
        LOGGER.info("Narrador IA - Mod inicializado com sucesso, parça!");
        GameEventListener.register();
    }
}
