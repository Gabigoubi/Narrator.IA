
package com.gabigoubi.narradoria.mixin;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(VillagerEntity.class)
public class MixinVillagerEntity {


    @Inject(method = "interactMob", at = @At("HEAD"))
    private void onInteractWithVillager(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {


        if (!player.getWorld().isClient() && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity serverPlayer) {

        }
    }
}