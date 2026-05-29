package com.gabigoubi.narradoria.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.gabigoubi.narradoria.GameEventListener;
import com.gabigoubi.narradoria.NarradorIAMod;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    @Inject(method = "sendEquipmentBreakStatus", at = @At("HEAD"))
    private void onEquipmentBreak(Item item, EquipmentSlot slot, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            String itemName = item.getName().getString();
            NarradorIAMod.LOGGER.info("[Narrador IA - Mixin] Detectou item quebrado: " + itemName);
            GameEventListener.addActionAndCheckFlush("Tool Broke", itemName, player, false);
        }
    }
}
