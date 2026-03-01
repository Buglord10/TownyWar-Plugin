package com.townywar.command;

import com.townywar.economy.EconomyHook;
import com.townywar.model.War;
import com.townywar.model.WarState;
import com.townywar.model.WarType;
import com.townywar.reward.RewardManager;
import com.townywar.towny.TownyHook;
import com.townywar.war.WarManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Optional;

public class WarCommand implements CommandExecutor {
    private final WarManager warManager;
    private final TownyHook townyHook;
    private final RewardManager rewardManager;
    private final EconomyHook economyHook;
    private final FileConfiguration config;

    public WarCommand(WarManager warManager, TownyHook townyHook, RewardManager rewardManager, EconomyHook economyHook, FileConfiguration config) {
        this.warManager = warManager;
        this.townyHook = townyHook;
        this.rewardManager = rewardManager;
        this.economyHook = economyHook;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 1) return false;

        if (args[0].equalsIgnoreCase("declare") && args.length >= 3) {
            WarType type = args[1].equalsIgnoreCase("nation") ? WarType.NATION : WarType.TOWN;
            String attacker = townyHook.sideForPlayer(player, type == WarType.NATION);
            if (attacker == null) {
                player.sendMessage("§cYou are not in a valid Towny side.");
                return true;
            }

            double declareCost = config.getDouble("economy.declaration_cost", 0D);
            if (declareCost > 0 && !economyHook.getEconomy().has(player, declareCost)) {
                player.sendMessage("§cInsufficient funds for declaration cost: " + declareCost);
                return true;
            }
            if (declareCost > 0) {
                economyHook.getEconomy().withdrawPlayer(player, declareCost);
            }

            try {
                Optional<War> war = warManager.declareWar(type, attacker, args[2]);
                if (war.isEmpty()) {
                    player.sendMessage("§cCannot declare war on that target currently.");
                    return true;
                }
                player.sendMessage("§aDeclared war on " + args[2] + ". Warmup started.");
            } catch (SQLException e) {
                player.sendMessage("§cWar declaration failed: " + e.getMessage());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("end")) {
            String sideNation = townyHook.sideForPlayer(player, true);
            String sideTown = townyHook.sideForPlayer(player, false);
            Optional<War> war = warManager.getWarForSide(sideNation == null ? sideTown : sideNation);
            if (war.isEmpty() || war.get().getState() != WarState.AWAITING_REWARD) {
                player.sendMessage("§cNo war awaiting reward selection.");
                return true;
            }
            War target = war.get();
            boolean nation = target.getType() == WarType.NATION;
            String side = townyHook.sideForPlayer(player, nation);
            if (side == null || !side.equalsIgnoreCase(target.getWinner()) || !townyHook.isLeader(player, side, nation)) {
                player.sendMessage("§cOnly the winning mayor/leader may end this war.");
                return true;
            }
            rewardManager.openRewardGui(player, target);
            return true;
        }

        if (args[0].equalsIgnoreCase("reward") && args.length >= 3 && args[1].equalsIgnoreCase("money")) {
            int percent;
            try { percent = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                player.sendMessage("§cPercent must be a number.");
                return true;
            }
            String side = townyHook.sideForPlayer(player, true);
            Optional<War> war = side == null ? Optional.empty() : warManager.getWarForSide(side);
            if (war.isEmpty()) war = warManager.getWarForSide(townyHook.sideForPlayer(player, false));
            if (war.isEmpty()) {
                player.sendMessage("§cNo active war found.");
                return true;
            }
            rewardManager.applyReward(war.get(), com.townywar.model.RewardType.TAKE_MONEY, player, percent);
            return true;
        }

        return false;
    }
}
