package org.mmga.uglobal.command;

import org.bukkit.Location;
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
        if (!command.getName().equalsIgnoreCase("uglobalweapon")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("missile");
            completions.add("nuclear");
            completions.add("rpg");

            // 根据玩家当前输入过滤（忽略大小写）
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }


        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("rpg")) {
                List<String> completions = new ArrayList<>();
                completions.add("normal");
                completions.add("small");
                completions.add("medium");
                completions.add("large");

                // 根据玩家当前输入过滤（忽略大小写）
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args[0].equalsIgnoreCase("missile") || args[0].equalsIgnoreCase("nuclear")) {
            // 坐标提示
            if (args.length == 2 || args.length == 4 || args.length == 3) {
                List<String> completions = new ArrayList<>();
                completions.add("~");
                switch (args.length) {
                    case 2:
                        return completions.stream()
                                .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    case 3:
                        return completions.stream()
                                .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    case 4:
                        return completions.stream()
                                .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                                .collect(Collectors.toList());
                    default:
                        break;
                }
            }
            // 威力提示
            if (args.length == 5) {
                List<String> completions = new ArrayList<>();
                completions.add("50");
                completions.add("80");
                completions.add("100");
                completions.add("150");

                // 根据玩家当前输入过滤（忽略大小写）
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[4].toLowerCase()))
                        .collect(Collectors.toList());

            }
        }


        // 实现~补全
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        Location loc = player.getLocation();

        // 后续参数视为坐标参数：例如
        // /uglobalweapon missile <x> <y> <z>
        // args[1] 对应 X 坐标，args[2] 对应 Y 坐标，args[3] 对应 Z 坐标
        // 当前补全的是 args[args.length - 1] 对应的参数

        String input = args[args.length - 1];
        String suggestion = "";
        // 根据参数位置来决定补全哪个坐标
        int coordIndex = args.length - 2; // 0 -> X, 1 -> Y, 2 -> Z

        switch (coordIndex) {
            case 0: // X 坐标
                if (input.startsWith("~")) {
                    suggestion = "~" + ((int) loc.getX());
                }
                break;
            case 1: // Y 坐标
                if (input.startsWith("~")) {
                    suggestion = "~" + ((int) loc.getY());
                }
                break;
            case 2: // Z 坐标
                if (input.startsWith("~")) {
                    suggestion = "~" + ((int) loc.getZ());
                }
                break;
            default:
                break;
        }

        // 如果补全建议与输入匹配，则返回建议列表
        if (!suggestion.isEmpty() && suggestion.startsWith(input)) {
            return Collections.singletonList(suggestion);
        }
        return Collections.emptyList();
    }
}
