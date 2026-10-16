package com.aquaticstudios.aquabook.utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.aquaticstudios.aquabook.AquaBook.MESSAGES;

public class MessageUtils {
    public static void send(CommandSender target, String path) {
        send(target, path, Collections.<String, String>emptyMap());
    }

    public static void send(CommandSender target, String path, Map<String, String> placeholders) {
        final String message = raw(path, placeholders);
        if (message.isEmpty()) return;
        dispatch(target, message);
    }

    public static void sendList(CommandSender target, String path) {
        sendList(target, path, Collections.<String, String>emptyMap());
    }

    public static void sendList(CommandSender target, String path, Map<String, String> placeholders) {
        for (String line : rawList(path, placeholders)) {
            dispatch(target, line);
        }
    }

    public static void dispatch(CommandSender target, String message) {
        if (target instanceof Player) {
            final Player player = (Player) target;
            player.spigot().sendMessage(CC.toBungee(CC.parse(message, player)));
            return;
        }
        target.sendMessage(CC.format(message));
    }

    public static String raw(String path, Map<String, String> placeholders) {
        if (MESSAGES == null) return "";

        String message = MESSAGES.getString(path, "");
        if (message == null || message.isEmpty()) return "";

        message = message.replace("%prefix%", Settings.prefix);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }
        return message;
    }

    public static String raw(String path) {
        return raw(path, Collections.<String, String>emptyMap());
    }

    public static List<String> rawList(String path, Map<String, String> placeholders) {
        final List<String> out = new ArrayList<>();
        if (MESSAGES == null) return out;

        final List<String> lines = MESSAGES.getStringList(path);
        if (lines == null) return out;

        for (String line : lines) {
            String message = line.replace("%prefix%", Settings.prefix);
            if (placeholders != null) {
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    message = message.replace(entry.getKey(), entry.getValue());
                }
            }
            out.add(message);
        }
        return out;
    }

    public static String colored(String path, Map<String, String> placeholders) {
        return colored(path, placeholders, null);
    }

    public static String colored(String path, Map<String, String> placeholders, OfflinePlayer player) {
        return CC.format(raw(path, placeholders), player);
    }

    public static List<String> coloredList(String path, Map<String, String> placeholders) {
        return coloredList(path, placeholders, null);
    }

    public static List<String> coloredList(String path, Map<String, String> placeholders, OfflinePlayer player) {
        return CC.format(rawList(path, placeholders), player);
    }
}
