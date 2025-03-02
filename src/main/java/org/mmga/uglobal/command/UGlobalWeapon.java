package org.mmga.uglobal.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mmga.uglobal.Weapon;
import org.mmga.uglobal.weapon.Nuclear;

public class UGlobalWeapon implements CommandExecutor {

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(ChatColor.RED + "You must be a player to use this command!");
            return true;
        }

        // 检查是否有参数
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /UGlobalWeapon <parameter>");
            return false; // 表明参数错误，此时插件会显示前面定义的 `usage` 信息
        }

        // 获取指令的第一个参数
        String parameter = args[0];

        // 根据参数执行不同的逻辑
        switch (parameter.toLowerCase()) {
            case "nuclear":
                // 核弹生成 (temp)
                Location location = new Location(player.getWorld(), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                player.sendMessage(ChatColor.GREEN + "You have chosen \"nuclear\".");
                Nuclear nuclear = new Nuclear();
                nuclear.summonNuclear(player.getWorld(), location, Float.parseFloat(args[4]));
                // 玩家提示（temp)
                player.sendTitle(ChatColor.RED+"!!警告：已启用核打击!!", "将在"+location.getX()+" "+location.getZ()+"进行核打击", 10, 150, 10);
                new BukkitRunnable() {
                    int times = 0;

                    @Override
                    public void run() {
                        if (times >= 70) {
                            cancel();
                            return;
                        }
                        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10, 1);
                        times++;
                    }
                }.runTaskTimer(Weapon.getInstance(), 0L, 3L);  // 0 tick 延迟, 100 tick 间隔
                break;

            case "missile":
                player.sendMessage(ChatColor.GREEN + "You have chosen \"missile\".");
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown parameter: " + parameter);
                break;
        }

        return true; // 表示指令已正确处理，无需显示帮助信息
    }
}
