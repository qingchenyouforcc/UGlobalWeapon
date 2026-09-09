package org.mmga.uglobal.weapon;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.mmga.uglobal.Weapon;
import java.util.*;

/** Budgeted, water-independent terrain fracture; normal explosion events may filter every batch. */
public final class AnnihilationDestruction {
    public static final class Batch {
        private int pending;
        public boolean done() {return pending==0;}
    }
    private static final Deque<Job> JOBS=new ArrayDeque<>();
    private static BukkitTask worker;
    private AnnihilationDestruction() {}
    public static void blast(Location center,float power,Player owner,Batch batch,boolean vertical) {
        World world=center.getWorld();
        if(!world.isChunkLoaded(center.getBlockX()>>4,center.getBlockZ()>>4)) return;
        if(!world.createExplosion(center,power,false,true,owner)) return;
        // A wider, shallower core and broad side blasts form a bowl rather than a narrow shaft.
        double horizontal=power*(vertical?.8:.45),height=power*(vertical?.9:.45);
        JOBS.add(new Job(center,owner,power,horizontal,height,batch));batch.pending++;
        for(var entity:world.getNearbyEntities(center,power,power*1.4,power)) {
            if(!(entity instanceof LivingEntity living) || !living.getLocation().getBlock().isLiquid()) continue;
            if(living instanceof Player p && (p.getGameMode()==GameMode.CREATIVE || p.getGameMode()==GameMode.SPECTATOR)) continue;
            double distance=living.getLocation().distance(center);
            if(distance<power*1.4) living.damage(Math.min(120,(1-distance/(power*1.4))*power*1.5),owner);
        }
        if(worker==null) worker=new BukkitRunnable() {
            public void run() {
                int scanBudget=18000,breakBudget=3500;
                while(scanBudget>0 && breakBudget>0 && !JOBS.isEmpty()) {
                    Job job=JOBS.peek();List<Block> blocks=new ArrayList<>();
                    while(scanBudget>0 && blocks.size()<Math.min(breakBudget,1200) && !job.finished) {
                        Block block=job.next();scanBudget--;
                        if(block==null || block.getType().isAir() || block.getType().getHardness()<0) continue;
                        // Water and waterlogged blocks never shield the terrain behind them.
                        if(block.getType().getBlastResistance()>job.power*40 && !block.isLiquid()) continue;
                        blocks.add(block);
                    }
                    if(!blocks.isEmpty()) {
                        EntityExplodeEvent event=new EntityExplodeEvent(job.owner,job.center,blocks,0,ExplosionResult.DESTROY);
                        Bukkit.getPluginManager().callEvent(event);
                        if(!event.isCancelled()) for(Block block:event.blockList()) {
                            if(block.getType().getHardness()>=0) block.setType(Material.AIR,false);
                        }
                        breakBudget-=blocks.size();
                    }
                    if(job.finished) {JOBS.remove();job.batch.pending--;}
                }
                if(JOBS.isEmpty()) {worker=null;cancel();}
            }
        }.runTaskTimer(Weapon.getInstance(),1,1);
    }
    private static final class Job {
        final Location center;final Player owner;final float power;final Batch batch;
        final int radius,minY,maxY;final double horizontal,vertical;
        int x,z,y,rowEnd;boolean finished;
        Job(Location center,Player owner,float power,double horizontal,double vertical,Batch batch) {
            this.center=center.clone();this.owner=owner;this.power=power;this.batch=batch;
            this.horizontal=horizontal;this.vertical=vertical;radius=(int)Math.ceil(horizontal);
            minY=Math.max(-(int)Math.ceil(vertical),center.getWorld().getMinHeight()-center.getBlockY());
            maxY=Math.min((int)Math.ceil(vertical),center.getWorld().getMaxHeight()-1-center.getBlockY());
            y=minY;z=-radius-1;x=1;rowEnd=0;
        }
        Block next() {
            while(x>rowEnd) {
                if(++z>radius) {z=-radius;y++;}
                if(y>maxY) {finished=true;return null;}
                double normalizedY=y/vertical;
                double remaining=1-Math.pow(normalizedY,4)-z*z/(horizontal*horizontal);
                if(remaining<=0) continue;
                rowEnd=(int)Math.floor(horizontal*Math.sqrt(remaining));x=-rowEnd;
            }
            int bx=center.getBlockX()+x++,by=center.getBlockY()+y,bz=center.getBlockZ()+z;
            World world=center.getWorld();
            return world.isChunkLoaded(bx>>4,bz>>4)?world.getBlockAt(bx,by,bz):null;
        }
    }
    public static void shutdown() {
        if(worker!=null) {worker.cancel();worker=null;}
        for(Job job:JOBS) job.batch.pending--;
        JOBS.clear();
    }
}
