package org.mmga.uglobal.client.mixin;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.mmga.uglobal.client.AnnihilationView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class CinematicRendererMixin {
    @Inject(method="renderWorld",at=@At("HEAD"),cancellable=true)
    private void uglobalweapon$holdFrame(RenderTickCounter counter,CallbackInfo ci) {
        if(AnnihilationView.renderHeldFrame(counter)) ci.cancel();
    }
    @Inject(method="renderWorld",at=@At("TAIL"))
    private void uglobalweapon$cinematic(RenderTickCounter counter,CallbackInfo ci) {AnnihilationView.render(counter);}
    @Inject(method="close",at=@At("HEAD"))
    private void uglobalweapon$closeCinematic(CallbackInfo ci) {AnnihilationView.reset();}
}
