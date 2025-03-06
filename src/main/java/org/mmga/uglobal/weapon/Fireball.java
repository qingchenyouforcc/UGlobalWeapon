package org.mmga.uglobal.weapon;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.mmga.uglobal.Weapon;

import java.util.logging.Logger;

public class Fireball {
    // 生成核弹
    public void summonRanged(World world, Location location, float Yield) {
        summonFireball(world, location, Yield + 50);
        for (int i = -(10 + 20 * 4); i <= 10 + 20 * 4; i += 20) {
            for (int j = -(10 + 20 * 4); j <= 10 + 20 * 4; j += 20) {
                Location summonlocation = new Location(world, location.getX() + i, location.getY(), location.getZ() + j);
                summonFireball(world, summonlocation, Yield);
                //生成位置:System.out.println("i:"+i+" j:"+j+" location:"+summonlocation);
            }
        }
    }

    // 生成基础火球
    public void summonFireball(World world, Location location, float Yield) {
        org.bukkit.entity.Fireball fireball = world.spawn(location, org.bukkit.entity.Fireball.class);
        fireball.setDirection(new Vector(0, -1, 0));
        fireball.setYield(Yield);
        fireball.setMetadata("Weapon", new FixedMetadataValue(Weapon.getInstance(), "Nuclear"));
        Logger.getLogger("UGlobalWeapon").info("Summoning Fireball: " + fireball.getMetadata("Weapon") + " at " + location.getX() + "," + location.getY() + "," + location.getZ());

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
