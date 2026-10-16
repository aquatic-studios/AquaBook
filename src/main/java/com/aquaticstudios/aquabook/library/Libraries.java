package com.aquaticstudios.aquabook.library;

import com.aquaticstudios.aquabook.storage.StorageType;
import revxrsal.zapper.Dependency;

public enum Libraries {
    SQLITE(StorageType.SQLITE, new Dependency("org.xerial", "sqlite-jdbc", "3.42.0.0")),
    MYSQL(StorageType.MYSQL, new Dependency("com.mysql", "mysql-connector-j", "8.4.0")),
    H2(StorageType.H2, new Dependency("com.h2database", "h2", "2.2.224"));

    private final StorageType type;
    private final Dependency dependency;

    Libraries(StorageType type, Dependency dependency) {
        this.type = type;
        this.dependency = dependency;
    }

    public boolean isNeededFor(StorageType selected) {
        return type == selected;
    }

    public Dependency dependency() {
        return dependency;
    }
}
