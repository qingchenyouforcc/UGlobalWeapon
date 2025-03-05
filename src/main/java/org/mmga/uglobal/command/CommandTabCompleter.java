package org.mmga.uglobal.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTabCompleter implements TabCompleter {

    @SuppressWarnings("NullableProblems")
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // 我们只处理 /uglobalweapon 命令
        if (!command.getName().equalsIgnoreCase("UGlobalWeapon")) {
            return Collections.emptyList();
        }

        // 实现~补全
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (args.length == 0) {
            List<String> completions = new ArrayList<>();
            completions.add("missile");
            completions.add("nuclear");

            // 根据玩家当前输入过滤（忽略大小写）
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // 更多参数
        return new ArrayList<>();
    }
}
