package org.mmga.uglobal.weapon;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Fireball;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.mmga.uglobal.Weapon;

import java.util.logging.Logger;

public class Nuclear {
    public void summonNuclear(World world, Location location, float Yield) {
        summonFireball(world, location, Yield+50);
        for (int i =-(10+20*4) ;i <= 10+20*4; i+=20) {
            for (int j =-(10+20*4) ;j <= 10+20*4; j+=20) {
                Location summonlocation = new Location(world, location.getX()+i, location.getY(), location.getZ()+j);
                summonFireball(world, summonlocation, Yield);
                //System.out.println("i:"+i+" j:"+j+" location:"+summonlocation);
            }
        }
    }

    public void summonFireball(World world, Location location,float Yield) {
        Fireball fireball = world.spawn(location, Fireball.class);
        fireball.setDirection(new Vector(0, -1, 0));
        fireball.setYield(Yield);
        fireball.setMetadata("Weapon", new FixedMetadataValue(Weapon.getInstance(),"Nuclear"));
        Logger.getLogger("UGlobalWeapon").info("Summoning Nuclear: " + fireball.getMetadata("Weapon") + " at " + location.getX() + "," + location.getY() + "," + location.getZ());
    }
}
