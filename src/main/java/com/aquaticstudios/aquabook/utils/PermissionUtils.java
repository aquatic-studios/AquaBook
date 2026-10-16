package com.aquaticstudios.aquabook.utils;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class PermissionUtils {
    public static final String ROOT = "aquabook";

    public static boolean hasPermission(@NotNull CommandSender sender, @NotNull String node) {
        return sender.hasPermission(ROOT + ".admin")
                || sender.hasPermission(ROOT + "." + node)
                || sender.hasPermission(ROOT + ".*");
    }

    public static boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission(ROOT + ".use");
    }
}
