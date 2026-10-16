package com.aquaticstudios.aquabook.redis;

import com.aquaticstudios.aquabook.AquaBook;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

public final class RedisManager {
    public static final char SEPARATOR = '|';

    private static final String CHANNEL = "aquabook";
    private static final int DATABASE = 0;
    private static final int TIMEOUT = 2000;
    private static final int POOL_SIZE = 8;

    private final AquaBook plugin;
    private final String serverId;
    private final String channel;

    private JedisPool pool;
    private JedisPubSub subscriber;
    private Thread subscriberThread;
    private volatile boolean enabled;
    private volatile boolean closing;

    public RedisManager(AquaBook plugin) {
        this.plugin = plugin;

        final String configured = AquaBook.CONFIG.getString("redis.server-id", "");
        this.serverId = (configured == null || configured.trim().isEmpty())
                ? "srv-" + UUID.randomUUID().toString().substring(0, 8)
                : configured.trim();
        this.channel = CHANNEL;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getServerId() {
        return serverId;
    }

    public String getChannel() {
        return channel;
    }

    public void connect() {
        if (!AquaBook.CONFIG.getBoolean("redis.enabled", false)) {
            enabled = false;
            return;
        }

        final String host = AquaBook.CONFIG.getString("redis.host", "127.0.0.1");
        final int port = AquaBook.CONFIG.getInt("redis.port", 6379);
        final String password = AquaBook.CONFIG.getString("redis.password", "");

        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(POOL_SIZE);
        poolConfig.setMaxIdle(2);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);

        try {
            this.pool = (password == null || password.isEmpty())
                    ? new JedisPool(poolConfig, host, port, TIMEOUT, null, DATABASE, false)
                    : new JedisPool(poolConfig, host, port, TIMEOUT, password, DATABASE, false);

            try (Jedis jedis = pool.getResource()) {
                jedis.ping();
            }
        } catch (Exception exception) {
            plugin.getLogger().severe("Could not connect to Redis: " + exception.getMessage());
            plugin.getLogger().severe("AquaBook keeps running, but without cross server sync.");
            enabled = false;
            closePool();
            return;
        }

        enabled = true;
        startSubscriber();
        plugin.getLogger().info("Redis connected as '" + serverId + "' on channel '" + channel + "'.");
    }

    private void startSubscriber() {
        this.subscriber = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                handle(message);
            }
        };

        this.subscriberThread = new Thread(() -> {
            while (!closing && enabled) {
                try (Jedis jedis = pool.getResource()) {
                    jedis.subscribe(subscriber, channel);
                } catch (Exception exception) {
                    if (closing) return;
                    plugin.getLogger().warning("Redis subscription dropped: " + exception.getMessage()
                            + ". Retrying in 5s.");
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "AquaBook-Redis-Subscriber");

        this.subscriberThread.setDaemon(true);
        this.subscriberThread.start();
    }

    public void publish(String action, String... args) {
        if (!enabled || pool == null) return;

        final StringBuilder builder = new StringBuilder(serverId).append(SEPARATOR).append(action);
        for (String arg : args) builder.append(SEPARATOR).append(arg);
        final String message = builder.toString();

        plugin.getStorageQueue().submit(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.publish(channel, message);
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not publish to Redis: " + exception.getMessage());
            }
        });
    }

    private void handle(String message) {
        if (message == null || message.isEmpty()) return;

        final String[] parts = message.split("\\" + SEPARATOR, -1);
        if (parts.length < 2) return;
        if (serverId.equals(parts[0])) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> apply(parts));
    }

    private void apply(String[] parts) {
        final String action = parts[1].toUpperCase();
        try {
            switch (action) {
                case "LIKE":
                    if (parts.length < 5) return;
                    plugin.getNews().applyRemoteLike(parts[2], UUID.fromString(parts[3]), "1".equals(parts[4]));
                    return;
                case "READ":
                    if (parts.length < 4) return;
                    plugin.getPlayers().applyRemoteRead(UUID.fromString(parts[3]), parts[2]);
                    return;
                case "VIEW":
                    if (parts.length < 3) return;
                    plugin.getNews().applyRemoteView(parts[2]);
                    return;
                case "LIKES":
                    if (parts.length < 4) return;
                    plugin.getNews().applyRemoteLikes(parts[2], Integer.parseInt(parts[3]));
                    return;
                case "PURGE":
                    if (parts.length < 3) return;
                    plugin.getNews().applyRemotePurge(parts[2]);
                    return;
                case "RELOAD":
                    plugin.getLogger().info("Reload requested by another server.");
                    plugin.reloadAll();
                    return;
                default:
            }
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid Redis message: " + String.join("|", parts));
        }
    }

    public void close() {
        closing = true;
        enabled = false;

        if (subscriber != null) {
            try {
                if (subscriber.isSubscribed()) subscriber.unsubscribe();
            } catch (Exception ignored) {
            }
        }
        if (subscriberThread != null) subscriberThread.interrupt();
        closePool();
    }

    private void closePool() {
        if (pool != null && !pool.isClosed()) pool.close();
        pool = null;
    }
}
