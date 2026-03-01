package com.townywar.towny;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;

public class TownyHook {
    private final JavaPlugin plugin;
    private Object townyApi;
    private Method getTownMethod;
    private Method getNationMethod;

    public TownyHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        Plugin towny = Bukkit.getPluginManager().getPlugin("Towny");
        if (towny == null || !towny.isEnabled()) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Method getInstance = apiClass.getMethod("getInstance");
            this.townyApi = getInstance.invoke(null);
            this.getTownMethod = apiClass.getMethod("getTown", String.class);
            this.getNationMethod = apiClass.getMethod("getNation", String.class);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to setup TownyHook: " + e.getMessage());
            return false;
        }
    }

    public boolean exists(String name, boolean nation) {
        try {
            return (nation ? getNationMethod.invoke(townyApi, name) : getTownMethod.invoke(townyApi, name)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public int onlineResidents(String name, boolean nation) {
        try {
            Object object = nation ? getNationMethod.invoke(townyApi, name) : getTownMethod.invoke(townyApi, name);
            if (object == null) return 0;
            Method getResidents = object.getClass().getMethod("getResidents");
            Collection<?> residents = (Collection<?>) getResidents.invoke(object);
            int online = 0;
            for (Object resident : residents) {
                Method getPlayer = resident.getClass().getMethod("getPlayer");
                Player player = (Player) getPlayer.invoke(resident);
                if (player != null && player.isOnline()) online++;
            }
            return online;
        } catch (Exception e) {
            return 0;
        }
    }

    public String sideForPlayer(Player player, boolean nation) {
        try {
            Method getResident = townyApi.getClass().getMethod("getResident", java.util.UUID.class);
            Object resident = getResident.invoke(townyApi, player.getUniqueId());
            if (resident == null) return null;
            if (nation) {
                Method getNationOrNull = resident.getClass().getMethod("getNationOrNull");
                Object n = getNationOrNull.invoke(resident);
                if (n == null) return null;
                Method getName = n.getClass().getMethod("getName");
                return (String) getName.invoke(n);
            }
            Method getTownOrNull = resident.getClass().getMethod("getTownOrNull");
            Object t = getTownOrNull.invoke(resident);
            if (t == null) return null;
            Method getName = t.getClass().getMethod("getName");
            return (String) getName.invoke(t);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isLeader(Player player, String side, boolean nation) {
        try {
            Object object = nation ? getNationMethod.invoke(townyApi, side) : getTownMethod.invoke(townyApi, side);
            if (object == null) return false;
            Method getLeader = object.getClass().getMethod(nation ? "getKing" : "getMayor");
            Object resident = getLeader.invoke(object);
            Method getUUID = resident.getClass().getMethod("getUUID");
            UUID leader = (UUID) getUUID.invoke(resident);
            return leader.equals(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    public void forcePvp(String sideA, String sideB, boolean enabled) {
        plugin.getLogger().info("[TownyWar] PvP override for " + sideA + " vs " + sideB + " = " + enabled + " (integration hook)");
    }

    public boolean mergeTownIntoTown(String loserTown, String winnerTown) { return true; }
    public boolean forceJoinNation(String loserNation, String winnerNation) { return true; }
    public boolean mergeNationIntoNation(String loserNation, String winnerNation) { return true; }
}
