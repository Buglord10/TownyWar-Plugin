package com.townywar.reward;

import com.townywar.economy.EconomyHook;
import com.townywar.lock.LockManager;
import com.townywar.model.RewardType;
import com.townywar.model.War;
import com.townywar.model.WarType;
import com.townywar.towny.TownyHook;
import com.townywar.war.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.*;

public class RewardManager {
    public static final String GUI_TITLE = "War Victory - Choose Your Reward";
    public static final String MONEY_GUI_TITLE = "Select Reward %";

    private final WarManager warManager;
    private final TownyHook townyHook;
    private final LockManager lockManager;
    private final EconomyHook economyHook;
    private final FileConfiguration config;
    private final Map<UUID, UUID> chooserToWar = new HashMap<>();

    public RewardManager(WarManager warManager, TownyHook townyHook, LockManager lockManager, EconomyHook economyHook, FileConfiguration config) {
        this.warManager = warManager;
        this.townyHook = townyHook;
        this.lockManager = lockManager;
        this.economyHook = economyHook;
        this.config = config;
    }

    public void openRewardGui(Player player, War war) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        if (war.getType() == WarType.TOWN) {
            inv.setItem(10, named(Material.BEACON, "§aIntegrate Town", "§7Merge losing town into your town."));
        }
        if (war.getType() == WarType.NATION) {
            inv.setItem(12, named(Material.CHAIN, "§bForce Join Nation", "§7Force losing towns into your nation."));
            inv.setItem(14, named(Material.WHITE_BANNER, "§5Force Merge Nation", "§7Merge entire losing nation into yours."));
        }
        inv.setItem(16, named(Material.GOLD_BLOCK, "§6Take Percentage of Bank", "§7Select percent to transfer."));
        chooserToWar.put(player.getUniqueId(), war.getId());
        player.openInventory(inv);
    }

    public void openMoneyGui(Player player, War war) {
        Inventory inv = Bukkit.createInventory(null, 27, MONEY_GUI_TITLE);
        int min = config.getInt("rewards.money.min_percentage", 5);
        int max = config.getInt("rewards.money.max_percentage", 50);
        for (int p : new int[]{10, 25, 50}) {
            if (p >= min && p <= max) {
                inv.addItem(named(Material.GOLD_INGOT, "§e" + p + "%", "§7Click to choose"));
            }
        }
        chooserToWar.put(player.getUniqueId(), war.getId());
        player.openInventory(inv);
    }

    public Optional<War> warFromChooser(Player player) {
        UUID warId = chooserToWar.get(player.getUniqueId());
        if (warId == null) return Optional.empty();
        return warManager.getWars().stream().filter(w -> w.getId().equals(warId)).findFirst();
    }

    public void applyReward(War war, RewardType rewardType, Player executor, int percent) {
        String winner = war.getWinner();
        String loser = war.getLoser();
        if (winner == null || loser == null) return;

        switch (rewardType) {
            case INTEGRATE_TOWN -> townyHook.mergeTownIntoTown(loser, winner);
            case FORCE_JOIN_NATION -> {
                townyHook.forceJoinNation(loser, winner);
                lockWithConfig(loser, "rewards.forced_nation_lock_days");
            }
            case FORCE_MERGE_NATION -> {
                townyHook.mergeNationIntoNation(loser, winner);
                lockWithConfig(loser, "rewards.forced_merge_lock_days");
            }
            case TAKE_MONEY -> executeMoneyReward(executor, winner, loser, percent);
        }

        Bukkit.broadcastMessage("§a[War] " + winner + " won against " + loser + " using reward: " + rewardType.name());
        townyHook.forcePvp(war.getAttacker(), war.getDefender(), false);
        warManager.endWar(war);
        chooserToWar.remove(executor.getUniqueId());
    }

    private void executeMoneyReward(Player executor, String winner, String loser, int percent) {
        int min = config.getInt("rewards.money.min_percentage", 5);
        int max = config.getInt("rewards.money.max_percentage", 50);
        if (percent < min || percent > max) {
            executor.sendMessage("§cInvalid percentage.");
            return;
        }
        UUID winnerId = executor.getUniqueId();
        UUID loserId = Bukkit.getOfflinePlayers().length > 0 ? Bukkit.getOfflinePlayers()[0].getUniqueId() : winnerId;
        double sourceBalance = economyHook.getEconomy().getBalance(Bukkit.getOfflinePlayer(loserId));
        double amount = sourceBalance * (percent / 100.0D);
        if (amount <= 0) {
            executor.sendMessage("§cNo funds available to transfer.");
            return;
        }
        boolean ok = economyHook.transferPlayerToPlayer(loserId, winnerId, amount, "war-money-reward " + winner + " over " + loser);
        executor.sendMessage(ok ? "§aTransferred " + amount + " via Vault." : "§cTransfer failed (insufficient funds or provider issue).");
    }

    private void lockWithConfig(String side, String node) {
        try {
            lockManager.lockSide(side, config.getInt(node, 7));
        } catch (SQLException ignored) {
        }
    }

    private ItemStack named(Material mat, String name, String lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Collections.singletonList(lore));
        i.setItemMeta(m);
        return i;
    }
}
