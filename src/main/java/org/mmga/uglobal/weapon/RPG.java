package org.mmga.uglobal.weapon;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class RPG extends Fireball {
    public static final double PROJECTILE_SPEED=1.65;
    public static boolean isProjectile(Entity entity) { return entity.getScoreboardTags().contains("uglobalweapon_rocket_flight"); }
    @Override public void summonRanged(World world,Location eye,float power) { summonRanged(world,eye,eye.getDirection(),power); }
    public void summonRanged(World world,Location eye,Vector direction,float power) {
        Vector forward=direction.clone().normalize();
        summonFireball(world,eye.clone().add(forward.clone().multiply(1.5)),forward,power);
    }
    /** Compatibility entry point: now creates a custom rocket, never a fireball. */
    public void summonFireball(World world,Location location,Vector direction,float power) {
        RocketProjectiles.launch(location,direction,power,null);
    }
    public boolean launch(Player shooter,Vector direction,float power) {
        Location origin=WeaponGeometry.muzzle(shooter);
        Location eye=shooter.getEyeLocation();
        Vector path=origin.toVector().subtract(eye.toVector());
        var hit=shooter.getWorld().rayTraceBlocks(eye,path.clone().normalize(),path.length());
        if (hit!=null) origin=hit.getHitPosition().toLocation(shooter.getWorld()).subtract(path.normalize().multiply(.05));
        return RocketProjectiles.launch(origin,direction,power,shooter);
    }
}