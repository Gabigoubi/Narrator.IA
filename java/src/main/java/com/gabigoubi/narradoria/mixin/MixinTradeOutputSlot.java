package com.gabigoubi.narradoria.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.TradeOutputSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.gabigoubi.narradoria.GameEventListener;

@Mixin(TradeOutputSlot.class)
public class MixinTradeOutputSlot {

    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void onTradeCompleted(PlayerEntity player, ItemStack stack, CallbackInfo ci) {

        if (!player.getWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {

            String itemName = stack.getName().getString();
            int quantity = stack.getCount();

            GameEventListener.addActionAndCheckFlush("Comércio", "Comprou " + quantity + "x " + itemName + " de um Aldeão", serverPlayer, false);
        }
    }
}