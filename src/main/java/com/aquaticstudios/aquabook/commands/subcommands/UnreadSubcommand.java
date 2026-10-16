package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.news.NewsBook;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class UnreadSubcommand implements Subcommand {
    @NotNull
    @Override
    public String name() {
        return "unread";
    }

    @Nullable
    @Override
    public String permission() {
        return "unread";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        final Player player = Subcommands.player(sender);
        if (player == null) return;

        if (args.length < 2) {
            MessageUtils.send(sender, "messages.usage-unread");
            return;
        }

        final NewsBook book = Subcommands.findAny(sender, args[1]);
        if (book == null) return;

        final AquaBook plugin = AquaBook.getInstance();
        final boolean hadLike = plugin.getPlayers().isLiked(player.getUniqueId(), book.getId());

        plugin.getPlayers().reset(player.getUniqueId(), book.getId());
        if (hadLike) plugin.getNews().applyLocalLike(book.getId(), false);

        MessageUtils.send(sender, "messages.marked-unread", Subcommands.placeholders("%id%", book.getId()));
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return args.length == 2 ? Subcommands.completeIds(args[1]) : Collections.emptyList();
    }
}
