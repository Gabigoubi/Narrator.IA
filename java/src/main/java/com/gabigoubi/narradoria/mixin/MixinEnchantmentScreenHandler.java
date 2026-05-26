package com.gabigoubi.narradoria.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.gabigoubi.narradoria.GameEventListener;

/**
 * Mixin responsável por detectar quando o jogador gasta XP e Lápis-lazúli 
 * para encantar um item na Mesa de Encantamentos.
 */
@Mixin(EnchantmentScreenHandler.class)
public abstract class MixinEnchantmentScreenHandler {

    @Inject(method = "onButtonClick", at = @At("RETURN"))
    private void onEnchant(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        
        // cir.getReturnValue() é true se a transação mágica (XP/Lápis) foi um sucesso
        if (cir.getReturnValue() && player instanceof ServerPlayerEntity serverPlayer) {
            
            // Pega a instância da Mesa de Encantamento atual
            EnchantmentScreenHandler handler = (EnchantmentScreenHandler) (Object) this;
            
            // O Slot 0 é sempre onde fica a espada/picareta/armadura do jogador
            ItemStack enchantedItem = handler.getSlot(0).getStack();
            String itemName = enchantedItem.isEmpty() ? "Item Misterioso" : enchantedItem.getName().getString();
            
            GameEventListener.addActionAndCheckFlush("Encantou", itemName, serverPlayer, false);
        }
    }
}
