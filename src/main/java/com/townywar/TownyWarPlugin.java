package com.townywar;

import com.townywar.battle.BattleCycleManager;
import com.townywar.command.WarCommand;
import com.townywar.economy.EconomyHook;
import com.townywar.listener.WarListener;
import com.townywar.lock.LockManager;
import com.townywar.points.PointsManager;
import com.townywar.reward.RewardManager;
import com.townywar.storage.WarStorage;
import com.townywar.towny.TownyHook;
import com.townywar.war.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TownyWarPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("Towny") == null) {
            getLogger().severe("Towny is required. Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        EconomyHook economyHook = new EconomyHook(this);
        if (!economyHook.setup()) {
            getLogger().severe("Vault economy provider not found. Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        TownyHook townyHook = new TownyHook(this);
        if (!townyHook.setup()) {
            getLogger().severe("Failed to hook Towny API. Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        try {
            WarStorage storage = new WarStorage(getDataFolder().toPath());
            storage.init();
            LockManager lockManager = new LockManager(storage);
            WarManager warManager = new WarManager(storage, townyHook, getConfig());
            warManager.load();
            PointsManager pointsManager = new PointsManager(getConfig());
            RewardManager rewardManager = new RewardManager(warManager, townyHook, lockManager, economyHook, getConfig());

            BattleCycleManager battleCycleManager = new BattleCycleManager(this, warManager, townyHook, getConfig());
            battleCycleManager.start();

            getCommand("war").setExecutor(new WarCommand(warManager, townyHook, rewardManager, economyHook, getConfig()));
            Bukkit.getPluginManager().registerEvents(new WarListener(this, warManager, pointsManager, townyHook, rewardManager, getConfig()), this);
            getLogger().info("TownyWar enabled with Vault + Towny integration.");
        } catch (Exception e) {
            getLogger().severe("Startup failure: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
}
