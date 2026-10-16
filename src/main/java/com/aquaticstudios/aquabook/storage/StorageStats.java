package com.aquaticstudios.aquabook.storage;

public final class StorageStats {
    private final long playerRows;
    private final long newsRows;
    private final long likeRows;

    public StorageStats(long playerRows, long newsRows, long likeRows) {
        this.playerRows = playerRows;
        this.newsRows = newsRows;
        this.likeRows = likeRows;
    }

    public long getPlayerRows() {
        return playerRows;
    }

    public long getNewsRows() {
        return newsRows;
    }

    public long getLikeRows() {
        return likeRows;
    }
}
