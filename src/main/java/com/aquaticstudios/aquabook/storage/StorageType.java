package com.aquaticstudios.aquabook.storage;

public enum StorageType {
    SQLITE,
    MYSQL,
    H2;

    public static StorageType fromName(String name) {
        if (name == null) return SQLITE;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return SQLITE;
        }
    }
}
