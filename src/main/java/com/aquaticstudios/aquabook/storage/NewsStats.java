package com.aquaticstudios.aquabook.storage;

public final class NewsStats {
    private final int likesBase;
    private final int likes;
    private final long views;

    public NewsStats(int likesBase, int likes, long views) {
        this.likesBase = likesBase;
        this.likes = likes;
        this.views = views;
    }

    public static NewsStats empty() {
        return new NewsStats(0, 0, 0L);
    }

    public int getLikes() {
        return likes;
    }

    public long getViews() {
        return views;
    }

    public NewsStats withLikes(int newLikes) {
        return new NewsStats(likesBase, Math.max(0, newLikes), views);
    }

    public NewsStats withViews(long newViews) {
        return new NewsStats(likesBase, likes, Math.max(0L, newViews));
    }
}
