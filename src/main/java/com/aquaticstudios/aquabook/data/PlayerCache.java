package com.aquaticstudios.aquabook.data;

import com.aquaticstudios.aquabook.AquaBook;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerCache {
    private final AquaBook plugin;
    private final Map<UUID, PlayerNews> cache = new ConcurrentHashMap<>();

    public PlayerCache(AquaBook plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<PlayerNews> load(UUID uuid) {
        PlayerNews cached = cache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return plugin.getStorageQueue().submitAsync(() -> {
            PlayerNews loaded = plugin.getStorage().loadPlayer(uuid);
            cache.put(uuid, loaded);
            return loaded;
        });
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public boolean isRead(UUID uuid, String newsId) {
        PlayerNews news = cache.get(uuid);
        return news != null && news.isRead(newsId);
    }

    public boolean isLiked(UUID uuid, String newsId) {
        PlayerNews news = cache.get(uuid);
        return news != null && news.isLiked(newsId);
    }

    public boolean markRead(UUID uuid, String newsId) {
        PlayerNews news = cache.computeIfAbsent(uuid, key -> new PlayerNews());
        if (!news.markRead(newsId)) return false;

        long now = System.currentTimeMillis();
        plugin.getStorageQueue().submit(() -> plugin.getStorage().setRead(uuid, newsId, now));
        plugin.getRedis().publish("READ", newsId, uuid.toString());
        return true;
    }

    public boolean setLiked(UUID uuid, String newsId, boolean liked) {
        PlayerNews news = cache.computeIfAbsent(uuid, key -> new PlayerNews());
        boolean changed = liked ? news.markLiked(newsId) : news.removeLike(newsId);
        if (!changed) return false;

        long now = System.currentTimeMillis();
        plugin.getStorageQueue().submit(() -> plugin.getStorage().setLiked(uuid, newsId, liked, now));
        plugin.getRedis().publish("LIKE", newsId, uuid.toString(), liked ? "1" : "0");
        return true;
    }

    public void reset(UUID uuid, String newsId) {
        PlayerNews news = cache.get(uuid);
        if (news != null) {
            news.markUnread(newsId);
            news.removeLike(newsId);
        }
        plugin.getStorageQueue().submit(() -> plugin.getStorage().clearPlayer(uuid, newsId));
        plugin.getRedis().publish("LIKE", newsId, uuid.toString(), "0");
    }

    public void applyRemoteRead(UUID uuid, String newsId) {
        PlayerNews news = cache.get(uuid);
        if (news != null) news.markRead(newsId);
    }

    public void applyRemoteLike(UUID uuid, String newsId, boolean liked) {
        PlayerNews news = cache.get(uuid);
        if (news == null) return;
        if (liked) {
            news.markLiked(newsId);
            news.markRead(newsId);
        } else {
            news.removeLike(newsId);
        }
    }

    public void purge(String newsId) {
        for (PlayerNews news : cache.values()) {
            news.markUnread(newsId);
            news.removeLike(newsId);
        }
    }

    public int size() {
        return cache.size();
    }
}
