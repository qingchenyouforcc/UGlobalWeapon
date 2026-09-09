package org.mmga.uglobal.weapon;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.mmga.uglobal.Weapon;
import org.mmga.uglobal.manager.CooldownManager;
import org.mmga.uglobal.utils.RpgAnimation;
import java.util.*;

public final class RpgEffects implements Listener {
    private static final Map<UUID, BukkitTask> COOLING = new HashMap<>();
    private static Location muzzle(Player player) {
        return WeaponGeometry.muzzle(player);
    }
    public static void fired(Player player, CooldownManager cooldown) {
        Location muzzle = muzzle(player);
        World world = player.getWorld();
        world.spawnParticle(Particle.FLASH, muzzle, 1);
        world.spawnParticle(Particle.FLAME, muzzle, 18, 0.12, 0.12, 0.12, 0.09);
        world.spawnParticle(Particle.CLOUD, muzzle, 32, 0.18, 0.18, 0.18, 0.12);
        world.spawnParticle(Particle.SMOKE, muzzle, 24, 0.15, 0.15, 0.15, 0.06);
        world.spawnParticle(Particle.CLOUD, WeaponGeometry.breech(player), 12, .08,.08,.08,.06);
        world.playSound(muzzle, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.3F, 1.35F);
        world.playSound(muzzle, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.2F, 0.65F);
        stopCooling(player.getUniqueId());
        updateHeat(player,org.mmga.uglobal.utils.WeaponModel.coolingTier(cooldown.remainingFraction(player)));
        if (!cooldown.isOnCooldown(player)) return;
        BukkitTask task = new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline() || player.isDead() || !cooldown.isOnCooldown(player)) {
                    updateHeat(player,0);
                    COOLING.remove(player.getUniqueId()); cancel(); return;
                }
                updateHeat(player,org.mmga.uglobal.utils.WeaponModel.coolingTier(cooldown.remainingFraction(player)));
                if (RpgAnimation.isRpg(player.getInventory().getItemInMainHand())) {
                    // Heat haze comes from the whole forward tube, not only its opening.
                    Location front=muzzle(player);
                    Vector backwards=player.getEyeLocation().getDirection().multiply(-.18);
                    for(int point=0;point<3;point++) {
                        player.getWorld().spawnParticle(Particle.SMOKE,front,1,.045,.055,.045,.006);
                        front.add(backwards);
                    }
                }
            }
        }.runTaskTimer(Weapon.getInstance(), 3L, 3L);
        COOLING.put(player.getUniqueId(), task);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onImpact(ExplosionPrimeEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball) || !RPG.isProjectile(fireball)) return;
        event.setCancelled(true);
        Location impact = fireball.getLocation().clone();
        BlastProfile profile = BlastProfile.forPower(fireball.getYield());
        Entity source = fireball.getShooter() instanceof Entity entity ? entity : null;
        fireball.removeMetadata("Weapon", Weapon.getInstance());
        fireball.remove();
        // Run after the original collision has finished; no duplicate vanilla blast.
        Bukkit.getScheduler().runTask(Weapon.getInstance(), () -> detonate(impact, profile, source));
    }
    private static boolean loaded(Location point) {
        return point.getWorld() != null && point.getWorld().isChunkLoaded(point.getBlockX() >> 4, point.getBlockZ() >> 4);
    }
    public static void detonate(Location center, BlastProfile profile, Entity source) {
        if (!loaded(center)) return;
        World world = center.getWorld();
        if (!world.createExplosion(center, profile.primary(), false, true, source)) return;
        world.spawnParticle(Particle.FLASH, center, 1);
        world.spawnParticle(Particle.GUST_EMITTER_LARGE,center,1);
        world.playSound(center,Sound.ENTITY_WIND_CHARGE_WIND_BURST,SoundCategory.BLOCKS,2F,.65F);
        for (int burst = 0; burst < 3; burst++) {
            double angle = burst * Math.PI * 2 / 3;
            double offset = Math.min(3, profile.radius() * 0.15);
            Location secondary = center.clone().add(Math.cos(angle) * offset, 0.25, Math.sin(angle) * offset);
            Bukkit.getScheduler().runTaskLater(Weapon.getInstance(), () -> {
                if (loaded(secondary)) world.createExplosion(secondary, profile.secondary(), false, true, source);
            }, 4L * (burst + 1));
        }
        Bukkit.getScheduler().runTaskLater(Weapon.getInstance(),() -> {
            if(loaded(center)) BlastAfterfire.ignite(center,profile.primary(),source);
        },16L);
        new BukkitRunnable() {
            int step;
            final Set<UUID> pushed = new HashSet<>();
            @Override public void run() {
                if (!loaded(center) || ++step > 5) { cancel(); return; }
                double radius = profile.radius() * step / 5;
                for (int point = 0; point < 12; point++) {
                    double angle = point * Math.PI * 2 / 12;
                    Location ring = center.clone().add(Math.cos(angle) * radius, 0.35, Math.sin(angle) * radius);
                    if(step%2==1) world.spawnParticle(Particle.GUST,ring,0,Math.cos(angle),.05,Math.sin(angle),.32);
                }
                for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
                    if (!(entity instanceof LivingEntity living) || living.isDead() || pushed.contains(entity.getUniqueId())) continue;
                    if (living instanceof Player p && (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE)) continue;
                    Vector delta = living.getEyeLocation().toVector().subtract(center.toVector());
                    double distance = delta.length();
                    if (distance > radius) continue;
                    pushed.add(entity.getUniqueId());
                    if (distance > 0.01 && world.rayTraceBlocks(center, delta.clone().normalize(), distance, FluidCollisionMode.NEVER, true) != null) continue;
                    double force = profile.impulse(distance);
                    Vector impulse = delta.setY(0);
                    if (impulse.lengthSquared() > 1e-8) impulse.normalize().multiply(force);
                    impulse.setY(force * 0.35);
                    living.setVelocity(living.getVelocity().add(impulse));
                }
            }
        }.runTaskTimer(Weapon.getInstance(), 0L, 1L);
    }
    private static void stopCooling(UUID playerId) {
        BukkitTask task = COOLING.remove(playerId);
        if (task != null) task.cancel();
    }
    private static void updateHeat(Player player,int heat) {
        for (int slot=0;slot<player.getInventory().getSize();slot++) {
            var item=player.getInventory().getItem(slot);
            if (RpgAnimation.isRpg(item)) {
                var updated=item.clone();RpgAnimation.setHeat(updated,heat);
                if (!updated.equals(item)) player.getInventory().setItem(slot,updated);
            }
        }
    }
    @EventHandler public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        var item=event.getItemDrop().getItemStack();RpgAnimation.setHeat(item,0);event.getItemDrop().setItemStack(item);
    }
    @EventHandler public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) { updateHeat(event.getPlayer(),0); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { updateHeat(event.getPlayer(),0);stopCooling(event.getPlayer().getUniqueId()); }
    public static void shutdown() {
        COOLING.values().forEach(BukkitTask::cancel);
        COOLING.clear();
        Bukkit.getOnlinePlayers().forEach(player -> updateHeat(player,0));
    }
}
