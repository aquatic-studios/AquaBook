package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.news.NewsBook;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Subcommands {
    static Player player(@NotNull CommandSender sender) {
        if (sender instanceof Player) return (Player) sender;
        MessageUtils.send(sender, "messages.players-only");
        return null;
    }

    static NewsBook find(@NotNull Player player, @NotNull String id) {
        final NewsBook book = AquaBook.getInstance().getNews().get(id);
        if (book == null || !AquaBook.getInstance().getNews().canSee(player, book)) {
            MessageUtils.send(player, "messages.news-not-found", placeholders("%id%", id));
            return null;
        }
        return book;
    }

    static NewsBook findAny(@NotNull CommandSender sender, @NotNull String id) {
        final NewsBook book = AquaBook.getInstance().getNews().get(id);
        if (book == null) {
            MessageUtils.send(sender, "messages.news-not-found", placeholders("%id%", id));
            return null;
        }
        return book;
    }

    static Map<String, String> placeholders(String... pairs) {
        final Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) map.put(pairs[i], pairs[i + 1]);
        return map;
    }

    static List<String> completeIds(String typed) {
        final List<String> out = new ArrayList<>();
        for (String id : AquaBook.getInstance().getNews().getIds()) {
            if (id.startsWith(typed.toLowerCase(Locale.ROOT))) out.add(id);
        }
        return out;
    }
}
