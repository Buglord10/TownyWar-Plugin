package com.townywar.storage;

import com.townywar.model.War;
import com.townywar.model.WarState;
import com.townywar.model.WarType;

import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarStorage {
    private final String jdbc;

    public WarStorage(Path dataFolder) {
        this.jdbc = "jdbc:sqlite:" + dataFolder.resolve("wars.db");
    }

    public void init() throws SQLException {
        try (Connection c = DriverManager.getConnection(jdbc); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS wars(id TEXT PRIMARY KEY, type TEXT, attacker TEXT, defender TEXT, state TEXT, attacker_points INTEGER, defender_points INTEGER, winner TEXT, created_at INTEGER, warmup_end INTEGER, end_deadline INTEGER, active_window INTEGER, money_percent INTEGER)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS locks(side_name TEXT PRIMARY KEY, lock_until INTEGER)");
        }
    }

    public void saveWar(War war) throws SQLException {
        String sql = "REPLACE INTO wars(id,type,attacker,defender,state,attacker_points,defender_points,winner,created_at,warmup_end,end_deadline,active_window,money_percent) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DriverManager.getConnection(jdbc); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, war.getId().toString());
            ps.setString(2, war.getType().name());
            ps.setString(3, war.getAttacker());
            ps.setString(4, war.getDefender());
            ps.setString(5, war.getState().name());
            ps.setInt(6, war.getAttackerPoints());
            ps.setInt(7, war.getDefenderPoints());
            ps.setString(8, war.getWinner());
            ps.setLong(9, war.getCreatedAt().getEpochSecond());
            ps.setLong(10, war.getWarmupEnd() == null ? 0 : war.getWarmupEnd().getEpochSecond());
            ps.setLong(11, war.getEndDeadline() == null ? 0 : war.getEndDeadline().getEpochSecond());
            ps.setInt(12, war.isActiveBattleWindow() ? 1 : 0);
            ps.setInt(13, war.getSelectedMoneyPercent());
            ps.executeUpdate();
        }
    }

    public List<War> loadWars() throws SQLException {
        List<War> wars = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(jdbc); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM wars WHERE state != 'ENDED'")) {
            while (rs.next()) {
                War war = new War(UUID.fromString(rs.getString("id")), WarType.valueOf(rs.getString("type")), rs.getString("attacker"), rs.getString("defender"), WarState.valueOf(rs.getString("state")));
                war.setWinner(rs.getString("winner"));
                war.setCreatedAt(Instant.ofEpochSecond(rs.getLong("created_at")));
                long w = rs.getLong("warmup_end");
                long d = rs.getLong("end_deadline");
                if (w > 0) war.setWarmupEnd(Instant.ofEpochSecond(w));
                if (d > 0) war.setEndDeadline(Instant.ofEpochSecond(d));
                war.setActiveBattleWindow(rs.getInt("active_window") == 1);
                war.setSelectedMoneyPercent(rs.getInt("money_percent"));
                war.addAttackerPoints(rs.getInt("attacker_points"));
                war.addDefenderPoints(rs.getInt("defender_points"));
                wars.add(war);
            }
        }
        return wars;
    }

    public void deleteWar(UUID id) throws SQLException {
        try (Connection c = DriverManager.getConnection(jdbc); PreparedStatement ps = c.prepareStatement("DELETE FROM wars WHERE id=?")) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        }
    }

    public void saveLock(String sideName, Instant until) throws SQLException {
        try (Connection c = DriverManager.getConnection(jdbc); PreparedStatement ps = c.prepareStatement("REPLACE INTO locks(side_name, lock_until) VALUES(?,?)")) {
            ps.setString(1, sideName.toLowerCase());
            ps.setLong(2, until.getEpochSecond());
            ps.executeUpdate();
        }
    }

    public Instant getLock(String sideName) throws SQLException {
        try (Connection c = DriverManager.getConnection(jdbc); PreparedStatement ps = c.prepareStatement("SELECT lock_until FROM locks WHERE side_name=?")) {
            ps.setString(1, sideName.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return Instant.ofEpochSecond(rs.getLong(1));
        }
    }
}
