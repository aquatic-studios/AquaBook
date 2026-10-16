package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.news.NewsBook;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SetLikesSubcommand implements Subcommand {
    @NotNull
    @Override
    public String name() {
        return "setlikes";
    }

    @Nullable
    @Override
    public String permission() {
        return "setlikes";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            MessageUtils.send(sender, "messages.usage-setlikes");
            return;
        }

        final NewsBook book = Subcommands.findAny(sender, args[1]);
        if (book == null) return;

        final int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            MessageUtils.send(sender, "messages.invalid-number", Subcommands.placeholders("%input%", args[2]));
            return;
        }

        AquaBook.getInstance().getNews().setLikesBase(book.getId(), amount);
        MessageUtils.send(sender, "messages.likes-set",
                Subcommands.placeholders("%id%", book.getId(), "%amount%", String.valueOf(amount)));
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return args.length == 2 ? Subcommands.completeIds(args[1]) : Collections.emptyList();
    }
}
