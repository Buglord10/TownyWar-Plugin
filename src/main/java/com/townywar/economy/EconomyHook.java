package com.townywar.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Logger;

public class EconomyHook {
    private final JavaPlugin plugin;
    private final Logger logger;
    private Economy economy;

    public EconomyHook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return false;
        }
        this.economy = provider.getProvider();
        return this.economy != null;
    }

    public boolean refreshProvider() {
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return false;
        }
        this.economy = provider.getProvider();
        return this.economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean transferPlayerToPlayer(UUID from, UUID to, double amount, String reason) {
        OfflinePlayer src = Bukkit.getOfflinePlayer(from);
        OfflinePlayer dst = Bukkit.getOfflinePlayer(to);
        if (!economy.has(src, amount)) {
            return false;
        }
        EconomyResponse withdraw = economy.withdrawPlayer(src, amount);
        if (!withdraw.transactionSuccess()) {
            return false;
        }
        EconomyResponse deposit = economy.depositPlayer(dst, amount);
        if (!deposit.transactionSuccess()) {
            economy.depositPlayer(src, amount);
            return false;
        }
        logger.info("[TownyWar] Transaction " + reason + ": " + src.getName() + " -> " + dst.getName() + " amount=" + amount);
        return true;
    }
}
