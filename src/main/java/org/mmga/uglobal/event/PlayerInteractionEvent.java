package org.mmga.uglobal.event;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.mmga.uglobal.Weapon;
import org.mmga.uglobal.manager.CooldownManager;
import org.mmga.uglobal.weapon.RPG;

import java.util.Objects;

public class PlayerInteractionEvent implements Listener {
    // 设置冷却时间
    private final CooldownManager RPGcooldownManager = new CooldownManager(5);

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 检查玩家是否右键点击
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        Player player = event.getPlayer();

        // 获取主手物品
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            return;
        }

        // 判断rpg(temp)
        if (item.getType() == Material.NETHER_STAR && item.hasItemMeta()) {
            NamespacedKey key = new NamespacedKey(Weapon.getInstance(), "special_item");
            PersistentDataContainer container = Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer();
            if (container.has(key, PersistentDataType.STRING)) {
                // 获取 tag 的值
                String tagValue = container.get(key, PersistentDataType.STRING);
                if ("RPG".equals(tagValue)) {
                    RPGcooldownManager.setCooldown(player);
                    // 检查玩家是否在冷却中
                    if (RPGcooldownManager.isOnCooldown(player)) {
                        long remaining = RPGcooldownManager.getRemainingTime(player);
                        player.sendMessage("技能正在冷却中，请等待 " + remaining + " 秒！");
                        event.setCancelled(true);
                    }

                    RPG rpg = new RPG();
                    rpg.summonRanged(player.getWorld(), player.getEyeLocation(), 5);

                    // 停止继续执行默认行为
                    event.setCancelled(true);
                }
            }
        }
    }
}
