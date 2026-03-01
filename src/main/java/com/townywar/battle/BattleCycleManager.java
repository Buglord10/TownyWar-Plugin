package com.townywar.battle;

import com.townywar.model.War;
import com.townywar.model.WarState;
import com.townywar.towny.TownyHook;
import com.townywar.war.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;

public class BattleCycleManager {
    private final JavaPlugin plugin;
    private final WarManager warManager;
    private final TownyHook townyHook;
    private final FileConfiguration config;

    public BattleCycleManager(JavaPlugin plugin, WarManager warManager, TownyHook townyHook, FileConfiguration config) {
        this.plugin = plugin;
        this.warManager = warManager;
        this.townyHook = townyHook;
        this.config = config;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L * 15);
    }

    private void tick() {
        java.util.List<War> snapshot = new java.util.ArrayList<>(warManager.getWars());
        for (War war : snapshot) {
            if (war.getState() == WarState.WARMUP && war.getWarmupEnd() != null && Instant.now().isAfter(war.getWarmupEnd())) {
                war.setState(WarState.ACTIVE);
                Bukkit.broadcastMessage("§c[War] " + war.getAttacker() + " vs " + war.getDefender() + " is now active.");
                warManager.save(war);
            }
            if (war.getState() != WarState.ACTIVE) continue;

            long period = config.getLong("war.battle_interval_minutes", 10) * 60;
            long activeDuration = config.getLong("war.battle_duration_minutes", 5) * 60;
            long elapsed = Instant.now().getEpochSecond() - war.getCreatedAt().getEpochSecond();
            boolean active = (elapsed % period) < activeDuration;

            if (active != war.isActiveBattleWindow()) {
                war.setActiveBattleWindow(active);
                townyHook.forcePvp(war.getAttacker(), war.getDefender(), active);
                Bukkit.broadcastMessage("§6[War] Active battle window for " + war.getAttacker() + " vs " + war.getDefender() + ": " + (active ? "OPEN" : "CLOSED"));
                warManager.save(war);
            }

            if (war.getEndDeadline() != null && Instant.now().isAfter(war.getEndDeadline())) {
                Bukkit.broadcastMessage("§e[War] " + war.getAttacker() + " vs " + war.getDefender() + " reached max duration and is canceled.");
                warManager.endWar(war);
            }
        }
    }
}
