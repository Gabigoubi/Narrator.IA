package com.gabigoubi.narradoria.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gabigoubi.narradoria.GameEventListener;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"))
    private void onDropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        Object self = this;

        if (!stack.isEmpty() && self instanceof ServerPlayerEntity serverPlayer && !serverPlayer.getWorld().isClient()) {


            if (serverPlayer.isAlive() && serverPlayer.getHealth() > 0.0f &&
                    !stack.getItem().getName().getString().toLowerCase().contains("air")) {

                String itemName = stack.getItem().getName().getString();
                GameEventListener.addActionAndCheckFlush("Dropped", itemName, serverPlayer, false);
            }
        }
    }
}