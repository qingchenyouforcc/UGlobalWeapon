package org.mmga.uglobal.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;

/** Depth-aware screen-space lens around the actual, interpolated server black-hole entity. */
public final class BlackHoleLens {
    private static PostEffectProcessor processor;
    private static Object world;
    private static int width,height;
    private static boolean failed;
    private BlackHoleLens() {}
    public static void render(RenderTickCounter counter,Camera camera,Matrix4f view,Matrix4f projection) {
        var client=MinecraftClient.getInstance();
        if(world!=client.world) {reset();world=client.world;}
        if(client.world==null || failed) return;
        float bestRadius=0,lensX=0,lensY=0,lensDepth=1;
        float delta=counter.getTickDelta(false);
        for(var entity:client.world.getEntities()) {
            if(!(entity instanceof DisplayEntity.ItemDisplayEntity display) || display.isRemoved()
                    || display.getData()==null || display.getRenderState()==null) continue;
            var model=display.getData().itemStack().get(DataComponentTypes.CUSTOM_MODEL_DATA);
            if(model==null || model.value()!=4001) continue;
            var transform=display.getRenderState().transformation().interpolate(display.getLerpProgress(delta));
            float radius=Math.abs(transform.getScale().x)*.5F;
            if(radius<.15F) continue;
            var relative=display.getLerpedPos(delta).subtract(camera.getPos());
            Vector4f eye=view.transform(new Vector4f((float)relative.x,(float)relative.y,(float)relative.z,1));
            if(-eye.z<=radius*1.01F) continue;
            Vector4f clip=projection.transform(new Vector4f(eye));
            if(clip.w<=0) continue;
            float projectedRadius=Math.min(.85F,Math.abs(projection.m11())*radius/(2*(float)Math.sqrt(eye.z*eye.z-radius*radius)));
            float x=clip.x/clip.w*.5F+.5F,y=clip.y/clip.w*.5F+.5F;
            if(x < -projectedRadius*5 || x>1+projectedRadius*5 || y < -projectedRadius*5 || y>1+projectedRadius*5) continue;
            if(projectedRadius<=bestRadius) continue;
            Vector4f front=projection.transform(new Vector4f(eye.x,eye.y,eye.z+radius,1));
            bestRadius=projectedRadius;lensX=x;lensY=y;lensDepth=front.z/front.w*.5F+.5F;
        }
        if(bestRadius<.001F) return;
        try {
            var framebuffer=client.getFramebuffer();
            if(processor==null) {
                processor=new PostEffectProcessor(client.getTextureManager(),client.getResourceManager(),framebuffer,
                        Identifier.of("uglobalweapon","shaders/post/black_hole_lens.json"));
            }
            if(width!=framebuffer.textureWidth || height!=framebuffer.textureHeight) {
                width=framebuffer.textureWidth;height=framebuffer.textureHeight;processor.setupDimensions(width,height);
            }
            processor.setUniforms("LensX",lensX);processor.setUniforms("LensY",lensY);
            processor.setUniforms("LensRadius",bestRadius);processor.setUniforms("LensDepth",lensDepth);
            processor.setUniforms("Aspect",(float)width/height);
            Vector3f normal=view.transformDirection(new Vector3f(.12F,1,.18F).normalize()).normalize();
            float projectedNormal=(float)Math.sqrt(normal.x*normal.x+normal.y*normal.y);
            processor.setUniforms("DiskAxisX",projectedNormal>.0001F?normal.y/projectedNormal:1);
            processor.setUniforms("DiskAxisY",projectedNormal>.0001F?-normal.x/projectedNormal:0);
            processor.setUniforms("DiskInclination",Math.abs(normal.z));
            processor.setUniforms("DiskSide",normal.z>=0?1:-1);
            processor.setUniforms("OrbitTime",(client.world.getTime()%24000+delta)/20F);
            RenderSystem.disableBlend();RenderSystem.disableDepthTest();RenderSystem.resetTextureMatrix();
            processor.render(delta);
        } catch(Exception error) {
            failed=true;LoggerFactory.getLogger("UGlobalWeapon").error("Unable to render black-hole lens",error);
        } finally {
            client.getFramebuffer().beginWrite(true);RenderSystem.enableDepthTest();
        }
    }
    public static void reset() {
        if(processor!=null) {processor.close();processor=null;}
        width=height=0;world=null;failed=false;
    }
}
