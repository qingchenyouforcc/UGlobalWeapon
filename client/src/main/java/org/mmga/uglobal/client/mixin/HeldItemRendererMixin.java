package org.mmga.uglobal.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep server-provided RPG pose frames visible without vanilla equip bobbing. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow private ItemStack mainHand;
    @Shadow private ItemStack offHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private float prevEquipProgressOffHand;

    @Unique
    private static boolean uglobalweapon$isRpg(ItemStack stack) {
        return org.mmga.uglobal.client.RpgViewEffects.isRpg(stack);
    }

    @Inject(method = "updateHeldItems", at = @At("HEAD"))
    private void uglobalweapon$acceptPoseBeforeVanilla(CallbackInfo ci) {
        uglobalweapon$keepRpgEquipped();
    }

    @Inject(method = "updateHeldItems", at = @At("TAIL"))
    private void uglobalweapon$removeEquipTranslation(CallbackInfo ci) {
        uglobalweapon$keepRpgEquipped();
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"))
    private void uglobalweapon$refreshBeforeRender(CallbackInfo ci) {
        // Slot packets and input may update the hand after the last game tick.
        // Refresh before interpolation so no frame renders a stale equip offset.
        uglobalweapon$keepRpgEquipped();
    }

    @Inject(method = "resetEquipProgress", at = @At("HEAD"), cancellable = true)
    private void uglobalweapon$ignoreRpgEquipReset(Hand hand, CallbackInfo ci) {
        if (client.player != null && uglobalweapon$isRpg(client.player.getStackInHand(hand))) {
            uglobalweapon$keepRpgEquipped();
            ci.cancel();
        }
    }

    @Unique
    private void uglobalweapon$keepRpgEquipped() {
        if (client.player == null) return;
        ItemStack selected = client.player.getMainHandStack();
        if (uglobalweapon$isRpg(selected)) {
            // Replace the cached render stack immediately, including CustomModelData
            // changes. Vanilla would otherwise keep an old pose until the hand lowers.
            mainHand = selected;
            equipProgressMainHand = prevEquipProgressMainHand = 1.0F;
        }
        ItemStack offhand = client.player.getOffHandStack();
        if (uglobalweapon$isRpg(offhand)) {
            offHand = offhand;
            equipProgressOffHand = prevEquipProgressOffHand = 1.0F;
        }
    }
}
