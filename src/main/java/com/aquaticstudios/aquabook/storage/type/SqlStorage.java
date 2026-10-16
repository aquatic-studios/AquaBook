package com.aquaticstudios.aquabook.storage.type;

import com.aquaticstudios.aquabook.data.PlayerNews;
import com.aquaticstudios.aquabook.storage.NewsStats;
import com.aquaticstudios.aquabook.storage.Storage;
import com.aquaticstudios.aquabook.storage.StorageException;
import com.aquaticstudios.aquabook.storage.StorageStats;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class SqlStorage implements Storage {
    protected static final String PLAYERS = "aquabook_players";
    protected static final String NEWS = "aquabook_news";

    protected HikariDataSource source;

    protected abstract String driverClass();

    protected abstract String jdbcUrl();

    protected abstract void tune(HikariConfig config);

    protected String[] afterConnect() {
        return new String[0];
    }

    @Override
    public void setup() {
        try {
            Class.forName(driverClass());
        } catch (ClassNotFoundException exception) {
            throw new StorageException("Missing driver " + driverClass()
                    + ". Check the libraries folder or the server internet connection.", exception);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl());
        config.setDriverClassName(driverClass());
        config.setPoolName("AquaBook-" + getType());
        config.setInitializationFailTimeout(-1);
        tune(config);

        this.source = new HikariDataSource(config);

        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : afterConnect()) statement.execute(sql);

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + PLAYERS + " ("
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "news_id VARCHAR(64) NOT NULL, "
                    + "read_at BIGINT NOT NULL DEFAULT 0, "
                    + "liked INT NOT NULL DEFAULT 0, "
                    + "liked_at BIGINT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (uuid, news_id))");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + NEWS + " ("
                    + "news_id VARCHAR(64) NOT NULL, "
                    + "likes_base INT NOT NULL DEFAULT 0, "
                    + "views BIGINT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (news_id))");

            try {
                statement.executeUpdate("CREATE INDEX idx_aquabook_players_news ON " + PLAYERS + " (news_id)");
            } catch (SQLException ignored) {
            }
            try {
                statement.executeUpdate("CREATE INDEX idx_aquabook_players_liked ON " + PLAYERS + " (liked)");
            } catch (SQLException ignored) {
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not create the AquaBook tables", exception);
        }
    }

    @Override
    public PlayerNews loadPlayer(UUID uuid) {
        PlayerNews news = new PlayerNews();
        String sql = "SELECT news_id, read_at, liked FROM " + PLAYERS + " WHERE uuid = ?";

        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String id = result.getString(1);
                    if (result.getLong(2) > 0L) news.markRead(id);
                    if (result.getInt(3) == 1) news.markLiked(id);
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not read the data of " + uuid, exception);
        }
        return news;
    }

    @Override
    public void setRead(UUID uuid, String newsId, long when) {
        try (Connection connection = source.getConnection()) {
            if (updateRead(connection, uuid, newsId, when) > 0) return;
            try {
                insertPlayer(connection, uuid, newsId, when, false, 0L);
            } catch (SQLException duplicated) {
                updateRead(connection, uuid, newsId, when);
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not mark as read " + newsId, exception);
        }
    }

    private int updateRead(Connection connection, UUID uuid, String newsId, long when) throws SQLException {
        String sql = "UPDATE " + PLAYERS + " SET read_at = ? WHERE uuid = ? AND news_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, when);
            statement.setString(2, uuid.toString());
            statement.setString(3, newsId);
            return statement.executeUpdate();
        }
    }

    @Override
    public void setLiked(UUID uuid, String newsId, boolean liked, long when) {
        try (Connection connection = source.getConnection()) {
            if (updateLiked(connection, uuid, newsId, liked, when) > 0) return;
            try {
                insertPlayer(connection, uuid, newsId, when, liked, liked ? when : 0L);
            } catch (SQLException duplicated) {
                updateLiked(connection, uuid, newsId, liked, when);
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not save the like for " + newsId, exception);
        }
    }

    private int updateLiked(Connection connection, UUID uuid, String newsId, boolean liked, long when)
            throws SQLException {
        String sql = "UPDATE " + PLAYERS + " SET liked = ?, liked_at = ? WHERE uuid = ? AND news_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, liked ? 1 : 0);
            statement.setLong(2, liked ? when : 0L);
            statement.setString(3, uuid.toString());
            statement.setString(4, newsId);
            return statement.executeUpdate();
        }
    }

    private void insertPlayer(Connection connection, UUID uuid, String newsId, long readAt, boolean liked, long likedAt)
            throws SQLException {
        String sql = "INSERT INTO " + PLAYERS + " (uuid, news_id, read_at, liked, liked_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, newsId);
            statement.setLong(3, readAt);
            statement.setInt(4, liked ? 1 : 0);
            statement.setLong(5, likedAt);
            statement.executeUpdate();
        }
    }

    @Override
    public void clearPlayer(UUID uuid, String newsId) {
        String sql = "DELETE FROM " + PLAYERS + " WHERE uuid = ? AND news_id = ?";
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, newsId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Could not clear the row for " + newsId, exception);
        }
    }

    @Override
    public Map<String, NewsStats> loadStats() {
        Map<String, Integer> base = new HashMap<>();
        Map<String, Long> views = new HashMap<>();
        Map<String, Integer> likes = new HashMap<>();

        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery(
                    "SELECT news_id, likes_base, views FROM " + NEWS)) {
                while (result.next()) {
                    base.put(result.getString(1), result.getInt(2));
                    views.put(result.getString(1), result.getLong(3));
                }
            }
            try (ResultSet result = statement.executeQuery(
                    "SELECT news_id, COUNT(*) FROM " + PLAYERS + " WHERE liked = 1 GROUP BY news_id")) {
                while (result.next()) {
                    likes.put(result.getString(1), result.getInt(2));
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not read the statistics", exception);
        }

        Map<String, NewsStats> out = new HashMap<>();
        for (String id : base.keySet()) {
            out.put(id, new NewsStats(base.get(id), base.get(id) + likes.getOrDefault(id, 0),
                    views.getOrDefault(id, 0L)));
        }
        for (Map.Entry<String, Integer> entry : likes.entrySet()) {
            if (out.containsKey(entry.getKey())) continue;
            out.put(entry.getKey(), new NewsStats(0, entry.getValue(), 0L));
        }
        return out;
    }

    @Override
    public void addView(String newsId) {
        try (Connection connection = source.getConnection()) {
            String sql = "UPDATE " + NEWS + " SET views = views + 1 WHERE news_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, newsId);
                if (statement.executeUpdate() > 0) return;
            }
            try {
                insertNews(connection, newsId, 0, 1L);
            } catch (SQLException duplicated) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, newsId);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not count the view for " + newsId, exception);
        }
    }

    @Override
    public void setLikesBase(String newsId, int base) {
        try (Connection connection = source.getConnection()) {
            String sql = "UPDATE " + NEWS + " SET likes_base = ? WHERE news_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, base);
                statement.setString(2, newsId);
                if (statement.executeUpdate() > 0) return;
            }
            try {
                insertNews(connection, newsId, base, 0L);
            } catch (SQLException duplicated) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, base);
                    statement.setString(2, newsId);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not set the likes for " + newsId, exception);
        }
    }

    private void insertNews(Connection connection, String newsId, int base, long views) throws SQLException {
        String sql = "INSERT INTO " + NEWS + " (news_id, likes_base, views) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newsId);
            statement.setInt(2, base);
            statement.setLong(3, views);
            statement.executeUpdate();
        }
    }

    @Override
    public void purgeNews(String newsId) {
        try (Connection connection = source.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM " + PLAYERS + " WHERE news_id = ?")) {
                statement.setString(1, newsId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM " + NEWS + " WHERE news_id = ?")) {
                statement.setString(1, newsId);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new StorageException("Could not purge " + newsId, exception);
        }
    }

    @Override
    public StorageStats stats() {
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            long players = count(statement, "SELECT COUNT(*) FROM " + PLAYERS);
            long news = count(statement, "SELECT COUNT(*) FROM " + NEWS);
            long likes = count(statement, "SELECT COUNT(*) FROM " + PLAYERS + " WHERE liked = 1");
            return new StorageStats(players, news, likes);
        } catch (SQLException exception) {
            throw new StorageException("Could not query the backend status", exception);
        }
    }

    private long count(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getLong(1) : 0L;
        }
    }

    @Override
    public void disable() {
        if (source != null && !source.isClosed()) source.close();
    }
}
