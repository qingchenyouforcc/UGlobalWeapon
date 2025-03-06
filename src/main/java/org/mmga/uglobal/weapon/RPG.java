package org.mmga.uglobal.weapon;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class RPG extends Fireball {
    // 生成RPG导弹
    @Override
    public void summonRanged(World world, Location eyeLocation, float Yield) {
        // 获取玩家正在看的方向向量
        Vector direction = eyeLocation.getDirection();
        // 在玩家眼部位置前方一点生成火球，避免直接与玩家重叠
        Location spawnLocation = eyeLocation.add(direction.multiply(1.5));
        summonFireball(world, spawnLocation, Yield);
    }
}
