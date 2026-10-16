package com.aquaticstudios.aquabook.data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerNews {
    private final Set<String> read = ConcurrentHashMap.newKeySet();
    private final Set<String> liked = ConcurrentHashMap.newKeySet();

    public boolean isRead(String newsId) {
        return read.contains(newsId);
    }

    public boolean isLiked(String newsId) {
        return liked.contains(newsId);
    }

    public boolean markRead(String newsId) {
        return read.add(newsId);
    }

    public boolean markUnread(String newsId) {
        return read.remove(newsId);
    }

    public boolean markLiked(String newsId) {
        return liked.add(newsId);
    }

    public boolean removeLike(String newsId) {
        return liked.remove(newsId);
    }
}
