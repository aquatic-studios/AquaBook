package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.news.NewsBook;
import com.aquaticstudios.aquabook.utils.CC;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ListSubcommand implements Subcommand {
    @NotNull
    @Override
    public String name() {
        return "list";
    }

    @Nullable
    @Override
    public String permission() {
        return "list";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        final AquaBook plugin = AquaBook.getInstance();
        final List<NewsBook> books = plugin.getNews().getBooks();

        MessageUtils.sendList(sender, "list-header",
                Subcommands.placeholders("%amount%", String.valueOf(books.size())));

        for (NewsBook book : books) {
            sender.sendMessage(CC.format(MessageUtils.raw("messages.list-entry", Subcommands.placeholders(
                    "%id%", book.getId(),
                    "%title%", book.getTitle(),
                    "%date%", book.getDate(),
                    "%likes%", String.valueOf(plugin.getNews().getLikes(book.getId())),
                    "%views%", String.valueOf(plugin.getNews().getViews(book.getId()))))));
        }
    }
}
