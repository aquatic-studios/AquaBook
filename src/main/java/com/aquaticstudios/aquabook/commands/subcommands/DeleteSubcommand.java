package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.news.NewsBook;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class DeleteSubcommand implements Subcommand {
    @NotNull
    @Override
    public String name() {
        return "delete";
    }

    @Nullable
    @Override
    public String permission() {
        return "delete";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            MessageUtils.send(sender, "messages.usage-delete");
            return;
        }

        final NewsBook book = Subcommands.findAny(sender, args[1]);
        if (book == null) return;

        final AquaBook plugin = AquaBook.getInstance();
        final File file = new File(plugin.getNews().getFolder(), book.getId() + ".yml");
        if (file.exists() && !file.delete()) {
            MessageUtils.send(sender, "messages.delete-failed", Subcommands.placeholders("%id%", book.getId()));
            return;
        }

        plugin.getNews().purge(book.getId());
        plugin.reloadAll();
        plugin.getRedis().publish("RELOAD");

        MessageUtils.send(sender, "messages.deleted", Subcommands.placeholders("%id%", book.getId()));
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return args.length == 2 ? Subcommands.completeIds(args[1]) : Collections.emptyList();
    }
}
