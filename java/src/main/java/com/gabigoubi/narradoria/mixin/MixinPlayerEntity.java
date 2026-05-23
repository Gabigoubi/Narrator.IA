package com.gabigoubi.narradoria.mixin;

import com.gabigoubi.narradoria.GameEventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
    @Inject(method = "sendPickup", at = @At("TAIL"))
    private void onPickup(Entity item, int count, CallbackInfo ci) {
        if (item instanceof ItemEntity itemEntity && (Object) this instanceof ServerPlayerEntity player) {
            String itemName = itemEntity.getStack().getName().getString();
            GameEventListener.addActionAndCheckFlush("Picked Up", itemName, player, false);
        }
    }
}
