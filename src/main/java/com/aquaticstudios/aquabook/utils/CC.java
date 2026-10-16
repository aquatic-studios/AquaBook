package com.aquaticstudios.aquabook.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CC {
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .character('§')
            .hexCharacter('#')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();

    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern BUNGEE_HEX = Pattern.compile("&x(&[0-9a-fA-F]){6}", Pattern.CASE_INSENSITIVE);

    private static final String[] NAMED = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white"
    };
    private static final String CODES = "0123456789abcdef";

    public static Component parse(String input) {
        return parse(input, null);
    }

    public static Component parse(String input, OfflinePlayer player) {
        if (input == null || input.isEmpty()) return Component.empty();

        final String resolved = PlaceholderHook.apply(player, input);
        try {
            return MINI.deserialize(toMiniMessage(resolved));
        } catch (Exception exception) {
            return AMPERSAND.deserialize(resolved);
        }
    }

    public static String format(String input) {
        return format(input, null);
    }

    public static String format(String input, OfflinePlayer player) {
        if (input == null || input.isEmpty()) return "";
        return SECTION.serialize(parse(input, player));
    }

    public static List<String> format(List<String> input) {
        return format(input, null);
    }

    public static List<String> format(List<String> input, OfflinePlayer player) {
        final List<String> out = new ArrayList<>();
        if (input == null) return out;
        for (String line : input) out.add(format(line, player));
        return out;
    }

    public static BaseComponent[] toBungee(Component component) {
        return ComponentSerializer.parse(GsonComponentSerializer.gson().serialize(component));
    }

    public static String strip(String input) {
        final String stripped = org.bukkit.ChatColor.stripColor(format(input));
        return stripped == null ? "" : stripped;
    }

    public static String unformat(String input) {
        if (input == null) return "";
        return input.replace(org.bukkit.ChatColor.COLOR_CHAR, '&');
    }

    public static String truncate(String input, int max) {
        if (input == null) return "";
        return input.length() <= max ? input : input.substring(0, max);
    }

    private static String toMiniMessage(String input) {
        if (input == null || input.isEmpty()) return "";

        String work = normalizeBungeeHex(input);
        work = expandHex(work);

        final StringBuilder out = new StringBuilder(work.length() + 16);
        final char[] chars = work.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            final char current = chars[i];
            if ((current == '&' || current == '§') && i + 1 < chars.length) {
                final String tag = translate(Character.toLowerCase(chars[i + 1]));
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
            }
            out.append(current);
        }
        return out.toString();
    }

    private static String expandHex(String input) {
        final Matcher matcher = HEX.matcher(input);
        final StringBuffer buffer = new StringBuffer(input.length() + 16);
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("<reset><#" + matcher.group(1) + ">"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String normalizeBungeeHex(String input) {
        final Matcher matcher = BUNGEE_HEX.matcher(input);
        final StringBuffer buffer = new StringBuffer(input.length());
        while (matcher.find()) {
            final String raw = matcher.group();
            final StringBuilder hex = new StringBuilder(8).append("&#");
            for (int i = 3; i < raw.length(); i += 2) hex.append(raw.charAt(i));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(hex.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String translate(char code) {
        final int index = CODES.indexOf(code);
        if (index != -1) return "<reset><" + NAMED[index] + ">";

        switch (code) {
            case 'l':
                return "<bold>";
            case 'o':
                return "<italic>";
            case 'n':
                return "<underlined>";
            case 'm':
                return "<strikethrough>";
            case 'k':
                return "<obfuscated>";
            case 'r':
                return "<reset>";
            default:
                return null;
        }
    }
}
