package org.mmga.uglobal.weapon;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.mmga.uglobal.Weapon;
import java.util.*;

/** A few real, non-spreading ground flames that burn out after six seconds. */
public final class BlastAfterfire implements Listener {
    private static final Map<Location,UUID> FIRES=new HashMap<>();
    public static int activeCount() { return FIRES.size(); }
    public static void ignite(Location center,float power,Entity source) {
        igniteRing(center,Math.min(12,Math.max(1.5,power*.65)),8,120,source);
    }
    public static void igniteRing(Location center,double radius,int maxPatches,int duration,Entity source) {
        igniteArea(center,radius,radius,maxPatches,duration,source);
    }
    public static void igniteArea(Location center,double innerRadius,double outerRadius,int maxPatches,int duration,Entity source) {
        World world=center.getWorld();UUID owner=UUID.randomUUID();List<Location> patches=new ArrayList<>();
        for(int i=0;i<maxPatches*2 && patches.size()<maxPatches;i++) {
            double angle=i*2.399963229728653;
            double fraction=Integer.toUnsignedLong(Integer.reverse(i+1))*0x1.0p-32;
            double radius=Math.sqrt(innerRadius*innerRadius+(outerRadius*outerRadius-innerRadius*innerRadius)*fraction);
            int x=center.getBlockX()+(int)Math.round(Math.cos(angle)*radius),z=center.getBlockZ()+(int)Math.round(Math.sin(angle)*radius);
            if(!world.isChunkLoaded(x>>4,z>>4)) continue;
            for(int y=Math.min(world.getMaxHeight()-2,center.getBlockY()+24);y>=Math.max(world.getMinHeight()+1,center.getBlockY()-96);y--) {
                var block=world.getBlockAt(x,y,z);
                if(!block.getType().isAir() || !world.getBlockAt(x,y-1,z).getType().isSolid()) continue;
                var event=new BlockIgniteEvent(block,BlockIgniteEvent.IgniteCause.EXPLOSION,source);
                Bukkit.getPluginManager().callEvent(event);
                if(!event.isCancelled()) { block.setType(Material.FIRE,false);Location point=block.getLocation();FIRES.put(point,owner);patches.add(point); }
                break;
            }
        }
        if(patches.isEmpty()) return;
        new BukkitRunnable() {
            int ticks;
            @Override public void run() {
                ticks+=4;
                for(Location point:patches) {
                    if(!owner.equals(FIRES.get(point))) continue;
                    if(ticks>=duration) { if(point.getBlock().getType()==Material.FIRE) point.getBlock().setType(Material.AIR,false);FIRES.remove(point); }
                    else if(point.getBlock().getType()!=Material.FIRE) FIRES.remove(point);
                    else world.spawnParticle(Particle.SMALL_FLAME,point.clone().add(.5,.15,.5),2,.18,.03,.18,.01);
                }
                if(ticks>=duration) cancel();
            }
        }.runTaskTimer(Weapon.getInstance(),4,4);
    }
    @EventHandler(ignoreCancelled=true)
    public void onSpread(BlockSpreadEvent event) {
        if(FIRES.containsKey(event.getSource().getLocation())) event.setCancelled(true);
    }
    public static void shutdown() {
        for(Location point:FIRES.keySet()) if(point.getBlock().getType()==Material.FIRE) point.getBlock().setType(Material.AIR,false);
        FIRES.clear();
    }
}
