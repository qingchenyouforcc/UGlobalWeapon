package org.mmga.uglobal.weapon;

import org.bukkit.Location;
import org.bukkit.World;

public class Missile extends Fireball {
    // 生成导弹
    @Override
    public void summonRanged(World world, Location location, float Yield) {
        summonFireball(world, location, Yield);
    }
}
