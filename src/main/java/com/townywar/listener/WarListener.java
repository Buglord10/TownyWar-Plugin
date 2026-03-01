package com.townywar.listener;

import com.townywar.model.RewardType;
import com.townywar.model.War;
import com.townywar.model.WarState;
import com.townywar.model.WarType;
import com.townywar.points.PointsManager;
import com.townywar.reward.RewardManager;
import com.townywar.towny.TownyHook;
import com.townywar.war.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WarListener implements Listener {
    private final JavaPlugin plugin;
    private final WarManager warManager;
    private final PointsManager pointsManager;
    private final TownyHook townyHook;
    private final RewardManager rewardManager;
    private final FileConfiguration config;

    public WarListener(JavaPlugin plugin, WarManager warManager, PointsManager pointsManager, TownyHook townyHook, RewardManager rewardManager, FileConfiguration config) {
        this.plugin = plugin;
        this.warManager = warManager;
        this.pointsManager = pointsManager;
        this.townyHook = townyHook;
        this.rewardManager = rewardManager;
        this.config = config;
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        for (War war : warManager.getWars()) {
            if (war.getState() != WarState.ACTIVE) continue;
            boolean nation = war.getType() == WarType.NATION;
            String killerSide = townyHook.sideForPlayer(killer, nation);
            String victimSide = townyHook.sideForPlayer(victim, nation);
            if (killerSide == null || victimSide == null) continue;
            if (!((killerSide.equalsIgnoreCase(war.getAttacker()) && victimSide.equalsIgnoreCase(war.getDefender())) ||
                    (killerSide.equalsIgnoreCase(war.getDefender()) && victimSide.equalsIgnoreCase(war.getAttacker())))) {
                continue;
            }
            if (!pointsManager.canScore(killer.getUniqueId())) return;

            pointsManager.registerScoreCooldown(killer.getUniqueId());
            pointsManager.addKillPoints(war, killerSide, war.isActiveBattleWindow());
            warManager.save(war);

            int points = war.pointsFor(killerSide);
            int target = pointsManager.getWinPoints();
            Bukkit.broadcastMessage("§6[War] " + killerSide + " scored. " + points + "/" + target);
            if (points >= target && war.getWinner() == null) {
                war.setWinner(killerSide);
                war.setState(WarState.AWAITING_REWARD);
                warManager.save(war);
                Bukkit.broadcastMessage("§a[War] " + killerSide + " has reached victory points! Leader may use /war end");
            }
            break;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(RewardManager.GUI_TITLE) && !title.equals(RewardManager.MONEY_GUI_TITLE)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        rewardManager.warFromChooser(player).ifPresent(war -> {
            if (title.equals(RewardManager.GUI_TITLE)) {
                Material m = event.getCurrentItem().getType();
                if (m == Material.BEACON) rewardManager.applyReward(war, RewardType.INTEGRATE_TOWN, player, 0);
                else if (m == Material.CHAIN) rewardManager.applyReward(war, RewardType.FORCE_JOIN_NATION, player, 0);
                else if (m == Material.WHITE_BANNER) rewardManager.applyReward(war, RewardType.FORCE_MERGE_NATION, player, 0);
                else if (m == Material.GOLD_BLOCK) rewardManager.openMoneyGui(player, war);
            } else if (title.equals(RewardManager.MONEY_GUI_TITLE) && event.getCurrentItem().getItemMeta() != null) {
                String name = event.getCurrentItem().getItemMeta().getDisplayName().replace("§e", "").replace("%", "");
                int percent = Integer.parseInt(name);
                rewardManager.applyReward(war, RewardType.TAKE_MONEY, player, percent);
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(RewardManager.GUI_TITLE)) return;
        if (!config.getBoolean("war.reward_gui.force_selection", true)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> rewardManager.warFromChooser(player).ifPresent(war -> rewardManager.openRewardGui(player, war)), 1L);
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equalsIgnoreCase("Vault")) {
            Bukkit.getLogger().info("[TownyWar] Vault plugin (re)enabled, economy provider may now be refreshed.");
        }
    }
}
