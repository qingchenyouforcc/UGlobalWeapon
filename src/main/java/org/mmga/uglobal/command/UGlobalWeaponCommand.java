package org.mmga.uglobal.command;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.mmga.uglobal.Weapon;
import org.mmga.uglobal.utils.WarningSoundSummon;
import org.mmga.uglobal.weapon.Missile;
import org.mmga.uglobal.weapon.Fireball;

public class UGlobalWeaponCommand implements CommandExecutor {
    Player MyPlayer;

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(ChatColor.RED + "You must be a player to use this command!");
            return true;
        }

        MyPlayer = player;

        // 检查是否有参数
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /uglobalweapon <WeaponType> (<x> <y> <z> <power>)");
            return false;
        }

        // 获取指令的第一个参数
        String parameter = args[0];

        // 根据参数执行不同的逻辑
        try {
            switch (parameter.toLowerCase()) {
                case "rpg":
                    // 获得rpg
                    switch (args[1]) {
                        case "null":
                            player.getInventory().addItem(CreateRPG("null"));
                            break;
                        case "normal":
                            player.getInventory().addItem(CreateRPG("normal"));
                            break;
                        case "small":
                            player.getInventory().addItem(CreateRPG("small"));
                            break;
                        case "medium":
                            player.getInventory().addItem(CreateRPG("medium"));
                            break;
                        case "large":
                            player.getInventory().addItem(CreateRPG("large"));
                            break;
                        default:
                            player.sendMessage(ChatColor.RED + "Invalid parameter!");
                            break;
                    }
                    break;

                case "nuclear":
                    // 核弹生成 (temp)
                    Location location = new Location(player.getWorld(), Double.parseDouble(LocationPX(args[1])), Double.parseDouble(LocationPY(args[2])), Double.parseDouble(LocationPZ(args[3])));
                    player.sendMessage(ChatColor.GREEN + "You have chosen \"nuclear\".");
                    Fireball nuclear = new Fireball();
                    nuclear.summonRanged(player.getWorld(), location, Float.parseFloat(args[4]));
                    // 玩家提示（temp)
                    player.sendTitle(ChatColor.RED + "!!警告：已启用核打击!!", "将在" + location.getX() + " " + location.getZ() + "进行核打击", 10, 150, 10);
                    WarningSoundSummon.playNuclearSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 70);
                    break;

                case "missile":
                    // 导弹生成
                    Location missileLocation = new Location(player.getWorld(), Double.parseDouble(LocationPX(args[1])), Double.parseDouble(LocationPY(args[2])), Double.parseDouble(LocationPZ(args[3])));
                    player.sendMessage(ChatColor.GREEN + "You have chosen \"missile\".");
                    Missile missile = new Missile();
                    missile.summonRanged(player.getWorld(), missileLocation, Float.parseFloat(args[4]));
                    // 玩家提示
                    player.sendTitle(ChatColor.RED + "!!你已发射导弹!!", "将在" + missileLocation.getX() + " " + missileLocation.getZ() + "进行精准打击", 10, 100, 10);
                    WarningSoundSummon.playNuclearSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 45);
                    break;

                default:
                    player.sendMessage(ChatColor.RED + "Unknown parameter: " + parameter);
                    break;
            }
        } catch (Exception e) {
            commandSender.sendMessage(ChatColor.RED + "命令执行出错：" + e.getMessage());
            return false;
        }

        return true; // 表示指令已正确处理，无需显示帮助信息
    }

    // 处理生成物品
    public ItemStack CreateRPG(String type) {
        ItemStack RPGItem = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta RPG_Mate = RPGItem.getItemMeta();
        if (RPG_Mate != null) {
            // 使用 NamespacedKey 定义一个唯一标识符
            NamespacedKey key = new NamespacedKey(Weapon.getInstance(), "special_item");
            PersistentDataContainer container = RPG_Mate.getPersistentDataContainer();
            // 存储一个字符串作为自定义标记
            container.set(key, PersistentDataType.STRING, "RPG");
            RPG_Mate.setDisplayName(ChatColor.RED + "RPG_" + type);
            RPGItem.setItemMeta(RPG_Mate);
        }
        RPGItem.setItemMeta(RPG_Mate);
        return RPGItem;
    }

    // 处理坐标
    public String LocationPX(String arg) {
        float loc;

        // 判断输入是否以 "~" 开头（代表相对坐标）
        if (arg.startsWith("~")) {
            // 去掉前面的 "~"
            double offset = 0.0;
            loc = (float) (MyPlayer.getLocation().getX() + offset);
            return String.valueOf(loc);
        }
        return arg;
    }

    public String LocationPY(String arg) {
        float loc;

        // 判断输入是否以 "~" 开头（代表相对坐标）
        if (arg.startsWith("~")) {
            // 去掉前面的 "~"
            double offset = 0.0;
            loc = (float) (MyPlayer.getLocation().getY() + offset);
            return String.valueOf(loc);
        }
        return arg;
    }

    public String LocationPZ(String arg) {
        float loc;

        // 判断输入是否以 "~" 开头（代表相对坐标）
        if (arg.startsWith("~")) {
            // 去掉前面的 "~"
            double offset = 0.0;
            loc = (float) (MyPlayer.getLocation().getZ() + offset);
            return String.valueOf(loc);
        }
        return arg;
    }
}
