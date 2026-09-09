package org.mmga.uglobal.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.mmga.uglobal.client.RpgViewEffects;
import org.mmga.uglobal.client.ShotEnvelope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow public abstract float getYaw();
    @Shadow public abstract float getPitch();
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void uglobalweapon$shotShake(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) { RpgViewEffects.reset(); org.mmga.uglobal.client.AnnihilationView.reset(); return; }
        RpgViewEffects.observe(client.player.getMainHandStack());
        if (!client.options.getPerspective().isFirstPerson() || client.isPaused()) return;
        double time = RpgViewEffects.elapsed();
        // Camera.update rebuilt the base pose this frame. Never change player yaw/pitch.
        setRotation(getYaw() + ShotEnvelope.yaw(time) + org.mmga.uglobal.client.AnnihilationView.yaw(), getPitch() + ShotEnvelope.pitch(time) + org.mmga.uglobal.client.AnnihilationView.pitch());
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private void uglobalweapon$clearShake(CallbackInfo ci) { RpgViewEffects.reset(); }
}
