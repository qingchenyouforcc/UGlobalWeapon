package org.mmga.uglobal.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mmga.uglobal.Weapon;

public interface WarningSoundSummon {
    public static void playNuclearSound(Player player, Sound sound, int max_time) {
        new BukkitRunnable() {
            int times = 0;

            @Override
            public void run() {
                if (times >= max_time) {
                    cancel();
                    return;
                }
                player.playSound(player, sound, 10, 1);
                times++;
            }
        }.runTaskTimer(Weapon.getInstance(), 0L, 3L);  // 0 tick 延迟, 100 tick 间隔
    }
}
