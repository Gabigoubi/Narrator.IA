package com.gabigoubi.narradoria.mixin;

import com.gabigoubi.narradoria.GameEventListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreenHandler.class)
public class MixinCraftingHandler {
    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void onTakeOutput(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameEventListener.addActionAndCheckFlush("Crafted", stack.getName().getString(), serverPlayer, false);
        }
    }
}
