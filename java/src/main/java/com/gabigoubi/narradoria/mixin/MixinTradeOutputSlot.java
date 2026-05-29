package com.gabigoubi.narradoria.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.TradeOutputSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.MerchantInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.gabigoubi.narradoria.GameEventListener;

@Mixin(TradeOutputSlot.class)
public abstract class MixinTradeOutputSlot {

    // Criamos uma ponte direta para a memória do inventário do aldeão
    @Shadow @Final private MerchantInventory merchantInventory;

    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void onTradeCompleted(PlayerEntity player, ItemStack stack, CallbackInfo ci) {

        if (!player.getWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {

            // 1. Capturamos a oferta comercial atual diretamente do aldeão
            TradeOffer offer = this.merchantInventory.getTradeOffer();

            if (offer != null) {
                // 2. Extraímos o item real que está sendo vendido na vitrine, imune à manipulação da engine
                ItemStack purchasedItem = offer.getSellItem();
                String itemName = purchasedItem.getName().getString();
                
                // 3. Utilizamos a quantidade padrão do pacote de venda definido pelo aldeão
                int quantity = purchasedItem.getCount();

                GameEventListener.addActionAndCheckFlush("Comércio", "Comprou " + quantity + "x " + itemName + " de um Aldeão", serverPlayer, false);
            }
        }
    }
}
