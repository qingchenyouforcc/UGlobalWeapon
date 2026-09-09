package org.mmga.uglobal.client.mixin;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.mmga.uglobal.client.BlackHoleLens;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class BlackHoleLensMixin {
    // Apply before GameRenderer clears world depth and draws the first-person weapon.
    @Inject(method="render",at=@At("TAIL"))
    private void uglobalweapon$lens(RenderTickCounter counter,boolean outline,Camera camera,GameRenderer renderer,
            LightmapTextureManager lightmap,Matrix4f view,Matrix4f projection,CallbackInfo ci) {
        BlackHoleLens.render(counter,camera,view,projection);
    }
    @Inject(method="close",at=@At("HEAD"))
    private void uglobalweapon$closeLens(CallbackInfo ci) {BlackHoleLens.reset();}
}
