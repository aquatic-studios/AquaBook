package com.aquaticstudios.aquabook.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class PlaceholderHook {
    private static boolean available;

    public static void detect() {
        available = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public static boolean isAvailable() {
        return available;
    }

    public static String apply(OfflinePlayer player, String input) {
        if (!available || input == null || input.isEmpty()) return input;
        if (input.indexOf('%') < 0) return input;

        try {
            return PlaceholderAPI.setPlaceholders(player, input);
        } catch (Throwable throwable) {
            return input;
        }
    }
}
