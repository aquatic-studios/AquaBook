package com.aquaticstudios.aquabook.storage.type;

import com.aquaticstudios.aquabook.AquaBook;
import com.zaxxer.hikari.HikariConfig;

import java.io.File;

public final class SQLiteStorage extends SqlStorage {
    @Override
    public String getType() {
        return "SQLite";
    }

    @Override
    protected String driverClass() {
        return "org.sqlite.JDBC";
    }

    @Override
    protected String jdbcUrl() {
        File folder = AquaBook.getInstance().getDataFolder();
        if (!folder.exists()) folder.mkdirs();
        return "jdbc:sqlite:" + new File(folder, "data.db").getAbsolutePath();
    }

    @Override
    protected void tune(HikariConfig config) {
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTestQuery("SELECT 1");
    }

    @Override
    protected String[] afterConnect() {
        return new String[]{
                "PRAGMA journal_mode=WAL;",
                "PRAGMA synchronous=NORMAL;",
                "PRAGMA busy_timeout=5000;"
        };
    }
}
