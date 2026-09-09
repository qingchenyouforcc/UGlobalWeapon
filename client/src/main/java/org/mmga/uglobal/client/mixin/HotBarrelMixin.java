package org.mmga.uglobal.client.mixin;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import org.mmga.uglobal.client.HeatLight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemRenderer.class)
public abstract class HotBarrelMixin {
    @Redirect(method="renderBakedItemQuads",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/VertexConsumer;quad(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;FFFFII)V"))
    private void uglobalweapon$warmSurface(VertexConsumer vertices,MatrixStack.Entry matrix,BakedQuad quad,
                                         float red,float green,float blue,float alpha,int light,int overlay) {
        var sprite=quad.getSprite().getContents().getId();
        int glow=HeatLight.minimumBlockLight(sprite.getNamespace(),sprite.getPath());
        if (glow>0) light=LightmapTextureManager.pack(Math.max(glow,LightmapTextureManager.getBlockLightCoordinates(light)),
                LightmapTextureManager.getSkyLightCoordinates(light));
        vertices.quad(matrix,quad,red,green,blue,alpha,light,overlay);
    }
}
