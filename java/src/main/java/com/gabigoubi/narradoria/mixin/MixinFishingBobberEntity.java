package com.gabigoubi.narradoria.mixin;

import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.gabigoubi.narradoria.GameEventListener;

/**
 * Mixin responsável por detectar quando o jogador fisga algo na pescaria.
 */
@Mixin(FishingBobberEntity.class)
public abstract class MixinFishingBobberEntity {
    
    @Shadow public abstract PlayerEntity getPlayerOwner();

    @Inject(method = "use", at = @At("RETURN"))
    private void onUse(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        int result = cir.getReturnValue();
        
        // Se o resultado for maior que 0, o jogador recolheu a linha com sucesso (peixe, tesouro ou lixo)
        if (result > 0 && this.getPlayerOwner() instanceof ServerPlayerEntity player) {
            GameEventListener.addActionAndCheckFlush("Pescou", "Objeto fisgado", player, false);
        }
    }
}
