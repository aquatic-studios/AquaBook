package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.news.NewsBook;
import com.aquaticstudios.aquabook.utils.CC;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReadSubcommand implements Subcommand {
    @NotNull
    @Override
    public String name() {
        return "read";
    }

    @Nullable
    @Override
    public String permission() {
        return null;
    }

    @Override
    public boolean hidden() {
        return true;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        final Player player = Subcommands.player(sender);
        if (player == null || args.length < 2) return;

        final NewsBook book = Subcommands.find(player, args[1]);
        if (book == null) return;

        final AquaBook plugin = AquaBook.getInstance();
        if (plugin.getPlayers().markRead(player.getUniqueId(), book.getId())) {
            MessageUtils.send(player, "messages.marked-read",
                    Subcommands.placeholders("%title%", CC.strip(book.getTitle())));
        }

        plugin.getNews().afterAction(player, book);
    }
}
