package com.townywar.model;

import java.time.Instant;
import java.util.UUID;

public class War {
    private final UUID id;
    private final WarType type;
    private final String attacker;
    private final String defender;
    private WarState state;
    private int attackerPoints;
    private int defenderPoints;
    private String winner;
    private Instant createdAt;
    private Instant warmupEnd;
    private Instant endDeadline;
    private boolean activeBattleWindow;
    private int selectedMoneyPercent;

    public War(UUID id, WarType type, String attacker, String defender, WarState state) {
        this.id = id;
        this.type = type;
        this.attacker = attacker;
        this.defender = defender;
        this.state = state;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public WarType getType() { return type; }
    public String getAttacker() { return attacker; }
    public String getDefender() { return defender; }
    public WarState getState() { return state; }
    public void setState(WarState state) { this.state = state; }
    public int getAttackerPoints() { return attackerPoints; }
    public int getDefenderPoints() { return defenderPoints; }
    public void addAttackerPoints(int points) { this.attackerPoints += points; }
    public void addDefenderPoints(int points) { this.defenderPoints += points; }
    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getWarmupEnd() { return warmupEnd; }
    public void setWarmupEnd(Instant warmupEnd) { this.warmupEnd = warmupEnd; }
    public Instant getEndDeadline() { return endDeadline; }
    public void setEndDeadline(Instant endDeadline) { this.endDeadline = endDeadline; }
    public boolean isActiveBattleWindow() { return activeBattleWindow; }
    public void setActiveBattleWindow(boolean activeBattleWindow) { this.activeBattleWindow = activeBattleWindow; }
    public int getSelectedMoneyPercent() { return selectedMoneyPercent; }
    public void setSelectedMoneyPercent(int selectedMoneyPercent) { this.selectedMoneyPercent = selectedMoneyPercent; }

    public int pointsFor(String side) {
        return attacker.equalsIgnoreCase(side) ? attackerPoints : defenderPoints;
    }

    public void addPoints(String side, int points) {
        if (attacker.equalsIgnoreCase(side)) addAttackerPoints(points);
        if (defender.equalsIgnoreCase(side)) addDefenderPoints(points);
    }

    public String getLoser() {
        if (winner == null) return null;
        return winner.equalsIgnoreCase(attacker) ? defender : attacker;
    }
}
