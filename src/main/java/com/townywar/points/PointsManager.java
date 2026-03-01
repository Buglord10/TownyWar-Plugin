package com.townywar.points;

import com.townywar.model.War;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PointsManager {
    private final FileConfiguration config;
    private final Map<UUID, Instant> killCooldown = new HashMap<>();

    public PointsManager(FileConfiguration config) {
        this.config = config;
    }

    public boolean canScore(UUID killer) {
        Instant until = killCooldown.get(killer);
        return until == null || until.isBefore(Instant.now());
    }

    public void registerScoreCooldown(UUID killer) {
        killCooldown.put(killer, Instant.now().plusSeconds(config.getLong("war.points.kill_cooldown_seconds", 120)));
    }

    public void addKillPoints(War war, String side, boolean inZone) {
        int points = inZone ? config.getInt("war.points.kill_in_active_zone", 1) : config.getInt("war.points.kill_outside_zone", 2);
        war.addPoints(side, points);
    }

    public int getWinPoints() {
        return config.getInt("war.win_points", 100);
    }
}
