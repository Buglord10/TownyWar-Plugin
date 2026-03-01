package com.townywar.command;

import com.townywar.towny.TownyHook;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class WarTabCompleter implements TabCompleter {
    private final TownyHook townyHook;
    private final FileConfiguration config;

    public WarTabCompleter(TownyHook townyHook, FileConfiguration config) {
        this.townyHook = townyHook;
        this.config = config;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("declare", "end", "reward"));
        }

        if (args[0].equalsIgnoreCase("declare")) {
            if (args.length == 2) {
                return filter(args[1], List.of("town", "nation"));
            }
            if (args.length == 3) {
                if (args[1].equalsIgnoreCase("town")) {
                    return filter(args[2], townyHook.listTownNames());
                }
                if (args[1].equalsIgnoreCase("nation")) {
                    return filter(args[2], townyHook.listNationNames());
                }
            }
            return Collections.emptyList();
        }

        if (args[0].equalsIgnoreCase("reward")) {
            if (args.length == 2) {
                return filter(args[1], List.of("money"));
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("money")) {
                int min = config.getInt("rewards.money.min_percentage", 5);
                int max = config.getInt("rewards.money.max_percentage", 50);
                List<String> percentages = new ArrayList<>();
                for (int p : new int[]{5, 10, 15, 20, 25, 30, 40, 50}) {
                    if (p >= min && p <= max) {
                        percentages.add(Integer.toString(p));
                    }
                }
                if (percentages.isEmpty()) {
                    percentages.add(Integer.toString(min));
                    if (max != min) {
                        percentages.add(Integer.toString(max));
                    }
                }
                return filter(args[2], percentages);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(String token, List<String> values) {
        String lowered = token.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lowered))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}
