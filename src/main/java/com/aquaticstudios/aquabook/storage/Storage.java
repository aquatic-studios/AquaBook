package com.aquaticstudios.aquabook.storage;

import com.aquaticstudios.aquabook.data.PlayerNews;

import java.util.Map;
import java.util.UUID;

public interface Storage {
    String getType();

    void setup();

    PlayerNews loadPlayer(UUID uuid);

    void setRead(UUID uuid, String newsId, long when);

    void setLiked(UUID uuid, String newsId, boolean liked, long when);

    void clearPlayer(UUID uuid, String newsId);

    Map<String, NewsStats> loadStats();

    void addView(String newsId);

    void setLikesBase(String newsId, int base);

    void purgeNews(String newsId);

    StorageStats stats();

    void disable();
}
