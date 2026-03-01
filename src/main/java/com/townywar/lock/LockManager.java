package com.townywar.lock;

import com.townywar.storage.WarStorage;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class LockManager {
    private final WarStorage storage;

    public LockManager(WarStorage storage) {
        this.storage = storage;
    }

    public void lockSide(String side, int days) throws SQLException {
        storage.saveLock(side, Instant.now().plus(days, ChronoUnit.DAYS));
    }

    public boolean isLocked(String side) {
        try {
            Instant until = storage.getLock(side);
            return until != null && until.isAfter(Instant.now());
        } catch (SQLException e) {
            return false;
        }
    }
}
