package com.aquaticstudios.aquabook.storage.type;

import com.aquaticstudios.aquabook.AquaBook;
import com.zaxxer.hikari.HikariConfig;

import java.io.File;

public final class H2Storage extends SqlStorage {
    @Override
    public String getType() {
        return "H2";
    }

    @Override
    protected String driverClass() {
        return "org.h2.Driver";
    }

    @Override
    protected String jdbcUrl() {
        File folder = AquaBook.getInstance().getDataFolder();
        if (!folder.exists()) folder.mkdirs();
        return "jdbc:h2:file:" + new File(folder, "data").getAbsolutePath()
                + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    }

    @Override
    protected void tune(HikariConfig config) {
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
    }
}
