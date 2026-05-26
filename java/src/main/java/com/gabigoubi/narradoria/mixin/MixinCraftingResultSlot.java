package com.gabigoubi.narradoria.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gabigoubi.narradoria.GameEventListener;

@Mixin(CraftingResultSlot.class)
public class MixinCraftingResultSlot {

    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void onCrafted(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (!player.getWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            String itemName = stack.getItem().getName().getString();
            // Envia para o nosso Buffer Inteligente (Tier 2 - Progresso)
            GameEventListener.addActionAndCheckFlush("Crafted", itemName, serverPlayer, false);
        }
    }
}