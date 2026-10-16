package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReloadSubcommand implements Subcommand {
    @NotNull
    @Override
    public String name() {
        return "reload";
    }

    @Nullable
    @Override
    public String permission() {
        return "reload";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        final AquaBook plugin = AquaBook.getInstance();
        plugin.reloadAll();
        plugin.getRedis().publish("RELOAD");

        MessageUtils.send(sender, "messages.reload",
                Subcommands.placeholders("%amount%", String.valueOf(plugin.getNews().getBooks().size())));
    }
}
