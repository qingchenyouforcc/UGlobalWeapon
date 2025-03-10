package org.mmga.uglobal.event;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.mmga.uglobal.Weapon;
import org.mmga.uglobal.manager.CooldownManager;
import org.mmga.uglobal.weapon.RPG;

import java.util.Map;
import java.util.Objects;

public class PlayerInteractionEvent implements Listener {
    // 设置rpg类型 [爆炸范围, 冷却时间]
    Map<String, int[]> RPGConfig = Map.of(
            ChatColor.RED + "RPG_" + "null", new int[]{5, 0},
            ChatColor.RED + "RPG_" + "normal", new int[]{5, 5},
            ChatColor.RED + "RPG_" + "small", new int[]{3, 2},
            ChatColor.RED + "RPG_" + "medium", new int[]{7, 5},
            ChatColor.RED + "RPG_" + "large", new int[]{15, 10}
    );

    // 设置冷却时间
    private final CooldownManager RPGcooldownManager =  new CooldownManager(10);

    public boolean hasItem(Player player, Material material, int amount) {
        if (player == null) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        return inventory.contains(material, amount);
    }

    public void removeItemFromInventory(Player player, Material material, int amount) {
        // 构造要删除的物品
        ItemStack itemToRemove = new ItemStack(material, amount);
        // 尝试从玩家的背包中移除该物品
        player.getInventory().removeItem(itemToRemove);
    }

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
                    // 检查玩家是否在冷却中
                    if (RPGcooldownManager.isOnCooldown(player)) {
                        long remaining = RPGcooldownManager.getRemainingTime(player);
                        player.sendMessage(ChatColor.RED + "RPG正在冷却中，请等待 " + remaining + " 秒！");
                        event.setCancelled(true);
                        return;
                    }
                    if (hasItem(player, Material.FIRE_CHARGE, 1)) {
                        removeItemFromInventory(player, Material.FIRE_CHARGE, 1);
                    } else {
                        player.sendMessage(ChatColor.RED + "你没有足够的火焰弹作为RPG弹药！");
                        event.setCancelled(true);
                        return;
                    }

                    // 获取武器类型
                    String weaponType = item.getItemMeta().getDisplayName();

                    // 检查武器类型是否存在于配置中，若存在则应用相应的设置
                    if (RPGConfig.containsKey(weaponType)) {
                        RPG rpg = new RPG();
                        int[] config = RPGConfig.get(weaponType);
                        int range = config[0];
                        int cooldown = config[1];

                        rpg.summonRanged(player.getWorld(), player.getEyeLocation(), range);
                        RPGcooldownManager.setItemCooldown(cooldown);
                        RPGcooldownManager.setCooldown(player);
                    }

                    // 停止继续执行默认行为
                    event.setCancelled(true);
                }
            }
        }
    }
}
