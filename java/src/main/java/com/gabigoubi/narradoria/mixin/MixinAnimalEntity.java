package com.gabigoubi.narradoria.mixin;

import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.gabigoubi.narradoria.GameEventListener;
import com.gabigoubi.narradoria.NarradorIAMod;

@Mixin(AnimalEntity.class)
public abstract class MixinAnimalEntity {
    @Shadow public abstract ServerPlayerEntity getLovingPlayer();

    @Inject(method = "breed", at = @At("HEAD"))
    private void onBreed(ServerWorld world, AnimalEntity other, CallbackInfo ci) {
        ServerPlayerEntity player = this.getLovingPlayer(); // Identifica quem deu a comida
        if (player != null) {
            AnimalEntity parent = (AnimalEntity) (Object) this;
            String animalName = parent.getType().getName().getString();
            NarradorIAMod.LOGGER.info("[Narrador IA - Mixin] Animais reproduzidos: " + animalName);
            GameEventListener.addActionAndCheckFlush("Breeding", animalName, player, false);
        }
    }
}
