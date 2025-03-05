package org.mmga.uglobal.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mmga.uglobal.utils.WarningSoundSummon;
import org.mmga.uglobal.weapon.Missile;
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
            player.sendMessage(ChatColor.RED + "Usage: /UGlobalWeapon <WeaponType> <x> <y> <z> <power>");
            return false;
        }

        // 获取指令的第一个参数
        String parameter = args[0];

        // 根据参数执行不同的逻辑
        try {
            switch (parameter.toLowerCase()) {
                case "nuclear":
                    // 核弹生成 (temp)
                    Location location = new Location(player.getWorld(), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                    player.sendMessage(ChatColor.GREEN + "You have chosen \"nuclear\".");
                    Nuclear nuclear = new Nuclear();
                    nuclear.summonNuclear(player.getWorld(), location, Float.parseFloat(args[4]));
                    // 玩家提示（temp)
                    player.sendTitle(ChatColor.RED + "!!警告：已启用核打击!!", "将在" + location.getX() + " " + location.getZ() + "进行核打击", 10, 150, 10);
                    WarningSoundSummon.playNuclearSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 70);
                    break;

                case "missile":
                    // 导弹生成
                    Location missileLocation = new Location(player.getWorld(), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                    player.sendMessage(ChatColor.GREEN + "You have chosen \"missile\".");
                    Missile missile = new Missile();
                    missile.summonNuclear(player.getWorld(), missileLocation, Float.parseFloat(args[4]));
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
}
