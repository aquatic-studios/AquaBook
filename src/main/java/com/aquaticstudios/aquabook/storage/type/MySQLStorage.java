package com.aquaticstudios.aquabook.storage.type;

import com.aquaticstudios.aquabook.AquaBook;
import com.zaxxer.hikari.HikariConfig;

public final class MySQLStorage extends SqlStorage {
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final int POOL_SIZE = 6;
    private static final int MINIMUM_IDLE = 2;
    private static final long CONNECTION_TIMEOUT = 8000L;
    private static final long MAX_LIFETIME = 1800000L;

    @Override
    public String getType() {
        return "MySQL";
    }

    @Override
    protected String driverClass() {
        return DRIVER;
    }

    @Override
    protected String jdbcUrl() {
        final String host = AquaBook.CONFIG.getString("storage.mysql.host", "127.0.0.1");
        final int port = AquaBook.CONFIG.getInt("storage.mysql.port", 3306);
        final String database = AquaBook.CONFIG.getString("storage.mysql.database", "aquabook");
        final String properties = AquaBook.CONFIG.getString("storage.mysql.properties", "");

        final StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(host).append(':').append(port).append('/').append(database)
                .append("?useUnicode=true&characterEncoding=UTF-8");

        if (properties != null && !properties.isEmpty()) {
            url.append('&').append(properties.startsWith("&") ? properties.substring(1) : properties);
        }
        return url.toString();
    }

    @Override
    protected void tune(HikariConfig config) {
        config.setUsername(AquaBook.CONFIG.getString("storage.mysql.username", "root"));
        config.setPassword(AquaBook.CONFIG.getString("storage.mysql.password", ""));
        config.setMaximumPoolSize(POOL_SIZE);
        config.setMinimumIdle(MINIMUM_IDLE);
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setMaxLifetime(MAX_LIFETIME);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("useServerPrepStmts", "true");
    }
}
