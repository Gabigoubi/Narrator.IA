package com.gabigoubi.narradoria.mixin;

import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;
import com.gabigoubi.narradoria.GameEventListener;
import com.gabigoubi.narradoria.NarradorIAMod;

@Mixin(TameableEntity.class)
public abstract class MixinTameableEntity {
    @Inject(method = "setOwnerUuid", at = @At("HEAD"))
    private void onTamed(UUID uuid, CallbackInfo ci) {
        if (uuid != null) {
            TameableEntity animal = (TameableEntity) (Object) this;
            if (!animal.getWorld().isClient()) {
                ServerPlayerEntity player = (ServerPlayerEntity) animal.getWorld().getPlayerByUuid(uuid);
                if (player != null) {
                    String animalName = animal.getType().getName().getString();
                    NarradorIAMod.LOGGER.info("[Narrador IA - Mixin] Animal domado: " + animalName);
                    GameEventListener.addActionAndCheckFlush("Tamed", animalName, player, false);
                }
            }
        }
    }
}
