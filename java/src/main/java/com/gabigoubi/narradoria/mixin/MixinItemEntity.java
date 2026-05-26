package com.gabigoubi.narradoria.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gabigoubi.narradoria.GameEventListener;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity {

    @Shadow public abstract ItemStack getStack();

    @Inject(method = "onPlayerCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;sendPickup(Lnet/minecraft/entity/Entity;I)V"))
    private void onPickup(PlayerEntity player, CallbackInfo ci) {
        if (!player.getWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            ItemStack stack = this.getStack();
            String itemName = stack.getItem().getName().getString();
            // Envia para o nosso Buffer Inteligente
            GameEventListener.addActionAndCheckFlush("Picked Up", itemName, serverPlayer, false);
        }
    }
}