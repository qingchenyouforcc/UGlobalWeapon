package org.mmga.uglobal.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.slf4j.LoggerFactory;
import org.lwjgl.opengl.GL30;

/** Sound packets are server-timed cinematic markers, also received by nearby observers. */
public final class AnnihilationView {
    private static double fired=-1000, impacted=-1000, collapsed=-1000, blasted=-1000;
    private static double blastEnded=-1000;
    private static Vec3d blastCenter;
    private static double fireStrength=1,blastStrength=1;
    private static PostEffectProcessor processor;
    private static Framebuffer heldFrame;
    private static boolean snapshotValid;
    private static Object world;
    private static int width,height;
    private static boolean failed;
    private AnnihilationView() {}
    private static double now() {
        var client=MinecraftClient.getInstance();
        return client.world==null?0:(client.world.getTime()+client.getRenderTickCounter().getTickDelta(false))/20.0;
    }
    public static void signal(String id,double x,double y,double z) {
        if(!id.equals("uglobalweapon:annihilation_fire") && !id.equals("uglobalweapon:annihilation_impact")
                && !id.equals("uglobalweapon:annihilation_collapse") && !id.equals("uglobalweapon:annihilation_blast")
                && !id.equals("uglobalweapon:annihilation_blast_end")) return;
        var client=MinecraftClient.getInstance();
        if(client.world==null || client.player==null) return;
        if(world!=client.world) {reset();world=client.world;}
        double distance=client.player.getPos().distanceTo(new Vec3d(x,y,z));
        double strength=Math.max(.2,1-distance/256);
        if(id.equals("uglobalweapon:annihilation_fire")) {fired=now();fireStrength=strength;snapshotValid=false;}
        if(id.equals("uglobalweapon:annihilation_impact")) {impacted=now();}
        if(id.equals("uglobalweapon:annihilation_collapse")) {collapsed=now();snapshotValid=false;}
        if(id.equals("uglobalweapon:annihilation_blast")) {blasted=now();blastStrength=strength;blastEnded=-1000;blastCenter=new Vec3d(x,y,z);}
        if(id.equals("uglobalweapon:annihilation_blast_end") && blastCenter!=null && blastCenter.squaredDistanceTo(new Vec3d(x,y,z))<4) blastEnded=now();
    }
    public static float yaw() {
        double t=now()-fired,b=now()-blasted,i=now()-impacted;
        return (float)(shake(t,1.0,2.3)*Math.sin(t*83)*fireStrength+shake(i,1.2,2.4)*Math.sin(i*61)
                +shake(b,6.5,3.8)*(Math.sin(b*67)+.3*Math.sin(b*29))*blastStrength);
    }
    public static float pitch() {
        double t=now()-fired,b=now()-blasted,i=now()-impacted;
        return (float)(shake(t,1.0,1.7)*Math.cos(t*71)*fireStrength+shake(i,1.2,1.8)*Math.cos(i*71)
                +shake(b,6.5,2.8)*(Math.cos(b*91)+.25*Math.sin(b*31))*blastStrength);
    }
    private static double shake(double t,double duration,double amount) {return t<0 || t>duration?0:amount*Math.pow(1-t/duration,2);}
    private static boolean window(double t,double from,double to) {return t>=from && t<to;}
    private static boolean freeze() {return window(now()-fired,.08,.18) || window(now()-collapsed,.08,.34);}
    private static void copyColor(Framebuffer from,Framebuffer to) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,from.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,to.fbo);
        GL30.glBlitFramebuffer(0,0,from.textureWidth,from.textureHeight,0,0,to.textureWidth,to.textureHeight,GL30.GL_COLOR_BUFFER_BIT,GL30.GL_NEAREST);
        to.beginWrite(true);
    }
    /** Freeze only world presentation. Game ticks, networking and input continue normally. */
    public static boolean renderHeldFrame(RenderTickCounter counter) {
        var client=MinecraftClient.getInstance();
        if(client.world==null || world!=client.world) {reset();world=client.world;return false;}
        var main=client.getFramebuffer();
        if(!freeze() || !snapshotValid || heldFrame==null || main.textureWidth!=heldFrame.textureWidth || main.textureHeight!=heldFrame.textureHeight) return false;
        copyColor(heldFrame,main);
        render(counter);
        return true;
    }
    private static void captureFrame() {
        var client=MinecraftClient.getInstance();var main=client.getFramebuffer();
        if(heldFrame==null || heldFrame.textureWidth!=main.textureWidth || heldFrame.textureHeight!=main.textureHeight) {
            if(heldFrame!=null) heldFrame.delete();
            heldFrame=new SimpleFramebuffer(main.textureWidth,main.textureHeight,false,MinecraftClient.IS_SYSTEM_MAC);
        }
        copyColor(main,heldFrame);main.beginWrite(true);snapshotValid=true;
    }
    public static void render(RenderTickCounter counter) {
        var client=MinecraftClient.getInstance();
        if(client.world==null || client.player==null) {reset();return;}
        if(world!=client.world) {reset();world=client.world;return;}
        double t=now()-fired,b=now()-blasted,c=now()-collapsed,i=now()-impacted;
        if(window(t,0,.08) || window(c,0,.08)) captureFrame();
        float mono=window(t,0,.2)?1:window(t,.2,.3)?(float)((.3-t)/.1):0;
        float blackout=window(c,.16,.62)?1:window(c,.62,.7)?(float)((.7-c)/.08):0;
        float flash=(float)Math.max(shake(i,.14,.42),shake(c,.09,.9));
        // Sustained overexposure, then a long fade revealing the expanding fire and fallout.
        double whiteUntil=Math.min(15,Math.max(2.4,blastEnded<blasted?15:blastEnded-blasted+.2));
        if(window(b,0,whiteUntil)) flash=.985F;
        else if(window(b,whiteUntil,whiteUntil+2.2)) flash=Math.max(flash,(float)(.985*Math.pow((whiteUntil+2.2-b)/2.2,.8)));
        float shock=window(b,0,6.4)?(float)(Math.sin(b*10)*.014*(1-b/6.4)):0;
        if((mono<.001 && flash<.001 && blackout<.001 && Math.abs(shock)<.0001) || failed) return;
        try {
            var framebuffer=client.getFramebuffer();
            if(processor==null) {
                processor=new PostEffectProcessor(client.getTextureManager(),client.getResourceManager(),framebuffer,
                        Identifier.of("uglobalweapon","shaders/post/annihilation.json"));
                width=height=0;
            }
            if(width!=framebuffer.textureWidth || height!=framebuffer.textureHeight) {
                width=framebuffer.textureWidth;height=framebuffer.textureHeight;processor.setupDimensions(width,height);
            }
            processor.setUniforms("Mono",mono);processor.setUniforms("Flash",flash);
            processor.setUniforms("Blackout",blackout);processor.setUniforms("Shock",shock);
            RenderSystem.disableBlend();RenderSystem.disableDepthTest();RenderSystem.resetTextureMatrix();
            processor.render(counter.getTickDelta(false));
        } catch(Exception e) {
            failed=true;LoggerFactory.getLogger("UGlobalWeapon").error("Unable to load annihilation cinematic shader",e);
        } finally {
            client.getFramebuffer().beginWrite(true);RenderSystem.enableDepthTest();
        }
    }
    public static void reset() {fired=impacted=collapsed=blasted=blastEnded=-1000;blastCenter=null;world=null;failed=false;close();}
    public static void close() {
        if(processor!=null) {processor.close();processor=null;}
        if(heldFrame!=null) {heldFrame.delete();heldFrame=null;}
        snapshotValid=false;width=height=0;
    }
}
