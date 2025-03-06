package org.mmga.uglobal.weapon;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.mmga.uglobal.Weapon;

import java.util.logging.Logger;

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

    @Override
    public void summonFireball(World world, Location location, Vector direction, float Yield) {
        org.bukkit.entity.Fireball fireball = world.spawn(location, org.bukkit.entity.Fireball.class);
        fireball.setDirection(direction);
        fireball.setYield(Yield);
        fireball.setMetadata("Weapon", new FixedMetadataValue(Weapon.getInstance(), "Nuclear"));
        Logger.getLogger("UGlobalWeapon").info("Summoning Nuclear: " + fireball.getMetadata("Weapon") + " at " + location.getX() + "," + location.getY() + "," + location.getZ());

        // 创建一个定时任务，每隔2个 tick 检查火球的位置并生成粒子效果
        new BukkitRunnable() {
            @Override
            public void run() {
                // 当火球实体消失或者已经死亡时，取消任务
                if (fireball.isDead() || !fireball.isValid()) {
                    cancel();
                    return;
                }

                // 在火球当前位置生成粒子效果
                fireball.getWorld().spawnParticle(
                        Particle.FLAME,              // 粒子类型
                        fireball.getLocation(),      // 粒子生成位置
                        50,                          // 粒子数量
                        1, 1, 1,               // X、Y、Z 方向扩散范围
                        0.01                         // 粒子速度
                );
                fireball.getWorld().spawnParticle(
                        Particle.SMOKE,
                        fireball.getLocation(),
                        25,
                        2, 2, 2,
                        0
                );
            }
        }.runTaskTimer(Weapon.getInstance(), 0L, 2L); // 0 tick 延时，2 tick 间隔执行
    }
}
