package org.mmga.uglobal.manager;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    // 储存玩家冷却时间
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    // 冷却时长（单位：毫秒）
    private long cooldownTimeMillis;

    /**
     * @param cooldownTimeSeconds 冷却时长（单位：秒）
     */
    public CooldownManager(long cooldownTimeSeconds) {
        this.cooldownTimeMillis = cooldownTimeSeconds * 1000;
    }

    /**
     * 判断玩家是否还在冷却中
     *
     * @param player 目标玩家
     * @return 如果玩家在冷却中则返回 true
     */
    public boolean isOnCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        if (cooldowns.containsKey(playerId)) {
            long lastUsed = cooldowns.get(playerId);
            long elapsed = System.currentTimeMillis() - lastUsed;
            return elapsed < cooldownTimeMillis;
        }
        return false;
    }

    /**
     * 获取玩家还需要等待的剩余冷却时间（单位：秒）
     *
     * @param player 目标玩家
     * @return 剩余等待时间（单位：秒）
     */
    public long getRemainingTime(Player player) {
        UUID playerId = player.getUniqueId();
        if (cooldowns.containsKey(playerId)) {
            long elapsed = System.currentTimeMillis() - cooldowns.get(playerId);
            long remaining = cooldownTimeMillis - elapsed;
            return remaining > 0 ? remaining / 1000 : 0;
        }
        return 0;
    }

    /**
     * 设置玩家的冷却，记录当前使用时间
     *
     * @param player 目标玩家
     */
    public void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * 设置玩家的冷却，记录当前使用时间
     *
     * @param seconds 冷却时间
     */
    public void setItemCooldown(long seconds) {
        this.cooldownTimeMillis = seconds;
    }
}
