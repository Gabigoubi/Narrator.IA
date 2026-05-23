package com.gabigoubi.narradoria.mixin;

import com.gabigoubi.narradoria.GameEventListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.screen.slot.CraftingResultSlot.class)
public class MixinCraftingResultSlot {
    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void onCraftedItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameEventListener.addActionAndCheckFlush("Crafted", stack.getName().getString(), serverPlayer, false);
        }
    }
}