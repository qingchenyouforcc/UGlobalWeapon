package org.mmga.uglobal.client.mixin;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.mmga.uglobal.client.AnnihilationView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class CinematicSoundMixin {
    @Inject(method="onPlaySound",at=@At("TAIL"))
    private void uglobalweapon$cinematicSignal(PlaySoundS2CPacket packet,CallbackInfo ci) {
        AnnihilationView.signal(packet.getSound().value().getId().toString(),packet.getX(),packet.getY(),packet.getZ());
    }
}
