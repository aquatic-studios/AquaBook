package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HelpSubcommand implements Subcommand {
    @NotNull
    @Override
    public String name() {
        return "help";
    }

    @Nullable
    @Override
    public String permission() {
        return null;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        MessageUtils.sendList(sender, "help");
    }
}
