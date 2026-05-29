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

            // 1. Limpeza e Filtro Anti-Ar Preciso (O mesmo padrão do BUG-002)
            String itemName = stack.getItem().getName().getString();
            String t = itemName.trim();
            if (t.isEmpty() || t.equalsIgnoreCase("Ar") || t.equalsIgnoreCase("Air") || t.equalsIgnoreCase("0x Ar") || t.equalsIgnoreCase("0x Air")) {
                return; // Aborta silenciosamente
            }

            // 2. Guarda de Estado Blindada (State Guard)
            // Verifica se o jogador não foi removido do servidor, se está vivo e garante que a flag de entidade morta não foi levantada.
            if (!serverPlayer.isRemoved() && serverPlayer.isAlive() && !serverPlayer.isDead() && serverPlayer.getHealth() > 0.0f) {
                GameEventListener.addActionAndCheckFlush("Dropped", itemName, serverPlayer, false);
            }
        }
    }}
