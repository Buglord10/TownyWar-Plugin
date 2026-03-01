package com.townywar.war;

import com.townywar.model.War;
import com.townywar.model.WarState;
import com.townywar.model.WarType;
import com.townywar.storage.WarStorage;
import com.townywar.towny.TownyHook;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class WarManager {
    private final Map<UUID, War> activeWars = new HashMap<>();
    private final WarStorage storage;
    private final TownyHook townyHook;
    private final FileConfiguration config;

    public WarManager(WarStorage storage, TownyHook townyHook, FileConfiguration config) {
        this.storage = storage;
        this.townyHook = townyHook;
        this.config = config;
    }

    public void load() throws SQLException {
        for (War war : storage.loadWars()) {
            activeWars.put(war.getId(), war);
        }
    }

    public Collection<War> getWars() {
        return activeWars.values();
    }

    public Optional<War> getWarForSide(String side) {
        return activeWars.values().stream().filter(w -> w.getAttacker().equalsIgnoreCase(side) || w.getDefender().equalsIgnoreCase(side)).findFirst();
    }

    public Optional<War> declareWar(WarType type, String attacker, String defender) throws SQLException {
        if (getWarForSide(attacker).isPresent() || getWarForSide(defender).isPresent()) return Optional.empty();
        boolean nation = type == WarType.NATION;
        if (!townyHook.exists(attacker, nation) || !townyHook.exists(defender, nation)) return Optional.empty();
        if (townyHook.onlineResidents(defender, nation) < 1) return Optional.empty();
        War war = new War(UUID.randomUUID(), type, attacker, defender, WarState.WARMUP);
        war.setWarmupEnd(Instant.now().plus(config.getInt("war.warmup_minutes", 2), ChronoUnit.MINUTES));
        war.setEndDeadline(Instant.now().plus(config.getInt("war.max_duration_minutes", 60), ChronoUnit.MINUTES));
        activeWars.put(war.getId(), war);
        storage.saveWar(war);
        return Optional.of(war);
    }

    public void save(War war) {
        try {
            storage.saveWar(war);
        } catch (SQLException ignored) {
        }
    }

    public void endWar(War war) {
        war.setState(WarState.ENDED);
        activeWars.remove(war.getId());
        try {
            storage.deleteWar(war.getId());
        } catch (SQLException ignored) {
        }
    }
}
