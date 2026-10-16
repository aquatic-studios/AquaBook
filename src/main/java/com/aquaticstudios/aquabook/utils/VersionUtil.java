package com.aquaticstudios.aquabook.utils;

import org.bukkit.Bukkit;

public final class VersionUtil {
    private static final String VERSION = Bukkit.getBukkitVersion().split("-")[0];

    private static final String DISPLAY = display();

    public static String getDisplayVersion() {
        return DISPLAY;
    }

    private static String display() {
        final StringBuilder out = new StringBuilder(8);
        for (String part : VERSION.split("\\.")) {
            if (!part.matches("\\d+")) break;
            if (out.length() > 0) out.append('.');
            out.append(part);
        }
        return out.length() == 0 ? VERSION : out.toString();
    }
}
