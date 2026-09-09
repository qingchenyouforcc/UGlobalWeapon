package org.mmga.uglobal.weapon;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.mmga.uglobal.Weapon;
import java.util.*;

/** Independent visible rocket and swept collision simulation; no fireball entity. */
public final class RocketProjectiles {
    public static final int MAX_FLIGHT_TICKS=600;
    private static final Set<Flight> FLIGHTS=new HashSet<>();
    private record ChunkKey(World world,int x,int z) {}
    private static final Map<ChunkKey,Integer> TICKETS=new HashMap<>();
    private static ChunkKey retain(Location point) {
        ChunkKey key=new ChunkKey(point.getWorld(),point.getBlockX()>>4,point.getBlockZ()>>4);
        if (!TICKETS.containsKey(key) && !key.world.addPluginChunkTicket(key.x,key.z,Weapon.getInstance()))
            throw new IllegalStateException("Could not retain rocket chunk");
        TICKETS.merge(key,1,Integer::sum); return key;
    }
    private static void release(ChunkKey key) {
        if (key==null) return;
        int count=TICKETS.getOrDefault(key,0);
        if (count<=1) { TICKETS.remove(key); key.world.removePluginChunkTicket(key.x,key.z,Weapon.getInstance()); }
        else TICKETS.put(key,count-1);
    }
    public static boolean launch(Location origin,Vector direction,float power,Entity shooter) {
        BlastProfile.forPower(power);
        if (FLIGHTS.size()>=64) return false;
        direction.checkFinite();
        if (direction.lengthSquared()<1e-12) return false;
        Flight flight=new Flight(origin.clone(),direction.clone().normalize(),power,shooter);
        FLIGHTS.add(flight); flight.runTaskTimer(Weapon.getInstance(),1,1); return true;
    }
    public static int activeCount() { return FLIGHTS.size(); }
    public static int ticketCount() { return TICKETS.size(); }
    public static void shutdown() {
        for (Flight flight:new ArrayList<>(FLIGHTS)) flight.finish(true);
        for (ChunkKey key:new ArrayList<>(TICKETS.keySet())) key.world.removePluginChunkTicket(key.x,key.z,Weapon.getInstance());
        TICKETS.clear();
    }
    private static final class Flight extends BukkitRunnable {
        Location position;
        final Vector forward;
        final float power;
        final Entity shooter;
        final ItemDisplay visual;
        ChunkKey chunk;
        int age;
        boolean ended;
        Flight(Location position,Vector forward,float power,Entity shooter) {
            this.position=position;this.forward=forward;this.power=power;this.shooter=shooter;
            chunk=retain(position);position.setDirection(forward);
            try {
                visual=position.getWorld().spawn(position,ItemDisplay.class, display -> {
                    RocketAmmo.configureDisplay(display); display.setPersistent(false);
                    display.setTeleportDuration(1);display.setViewRange(8);display.addScoreboardTag("uglobalweapon_rocket_flight");
                });
            } catch (RuntimeException ex) { release(chunk); throw ex; }
        }
        @Override public void run() {
            if (ended) return;
            try { advance(); } catch (RuntimeException ex) {
                Weapon.getInstance().getLogger().warning("Rocket flight stopped: "+ex.getMessage());finish(true);
            }
        }
        private void advance() {
            World world=position.getWorld();
            if (++age>MAX_FLIGHT_TICKS) { finish(true); return; }
            Location next=position.clone().add(forward.clone().multiply(RPG.PROJECTILE_SPEED));
            if (!world.getWorldBorder().isInside(next) || next.getY()<world.getMinHeight() || next.getY()>=world.getMaxHeight()) {
                finish(true); return;
            }
            ChunkKey nextChunk=retain(next);
            try {
                var hit=world.rayTrace(position,forward,RPG.PROJECTILE_SPEED,FluidCollisionMode.NEVER,true,.15,
                        entity -> entity instanceof LivingEntity && entity!=shooter
                                && (!(entity instanceof Player player) || player.getGameMode()!=GameMode.SPECTATOR));
                if (hit!=null) {
                    position=hit.getHitPosition().toLocation(world);
                    ChunkKey impactTicket=retain(position);
                    Bukkit.getScheduler().runTaskLater(Weapon.getInstance(),() -> release(impactTicket),20L);
                    RpgEffects.detonate(position.clone().subtract(forward.clone().multiply(.05)),BlastProfile.forPower(power),shooter);
                    finish(false); return;
                }
                for (int i=0;i<3;i++) {
                    Location exhaust=position.clone().add(forward.clone().multiply(i*RPG.PROJECTILE_SPEED/3-.42));
                    world.spawnParticle(Particle.FLAME,exhaust,2,.025,.025,.025,.003);
                    world.spawnParticle(Particle.SMOKE,exhaust,2,.045,.045,.045,.004);
                }
                position=next;position.setDirection(forward);
                // An external cleanup should leave recoverable ammunition, not an invisible flight.
                if (!visual.isValid()) { finish(true); return; }
                visual.teleport(position);
                release(chunk);chunk=nextChunk;nextChunk=null;
            } finally { release(nextChunk); }
        }
        void finish(boolean recover) {
            if (ended) return;ended=true;
            try {
                visual.remove();
                if (recover) position.getWorld().dropItem(position,RocketAmmo.create(1));
            } finally { release(chunk);FLIGHTS.remove(this);cancel(); }
        }
    }
}
