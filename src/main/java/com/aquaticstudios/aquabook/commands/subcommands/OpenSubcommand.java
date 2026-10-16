package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.news.NewsBook;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import com.aquaticstudios.aquabook.utils.PermissionUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class OpenSubcommand implements Subcommand {
    @NotNull
    @Override
    public String name() {
        return "open";
    }

    @Nullable
    @Override
    public String permission() {
        return null;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        final Player player = Subcommands.player(sender);
        if (player == null) return;

        if (!PermissionUtils.canUse(player)) {
            MessageUtils.send(player, "messages.no-permission");
            return;
        }
        if (args.length < 2) {
            MessageUtils.send(player, "messages.usage-open");
            return;
        }

        final NewsBook book = Subcommands.find(player, args[1]);
        if (book == null) return;

        AquaBook.getInstance().getNews().open(player, book);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return args.length == 2 ? Subcommands.completeIds(args[1]) : Collections.emptyList();
    }
}
