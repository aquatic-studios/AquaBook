package com.aquaticstudios.aquabook.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface Subcommand {
    @NotNull
    String name();

    @Nullable
    String permission();

    void execute(@NotNull CommandSender sender, @NotNull String[] args);

    default boolean hidden() {
        return false;
    }

    default List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
