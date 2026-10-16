package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.gui.NewsMenu;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import com.aquaticstudios.aquabook.utils.PermissionUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MenuSubcommand implements Subcommand {
    @NotNull
    @Override
    public String name() {
        return "menu";
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
        if (AquaBook.getInstance().getNews().getVisible(player).isEmpty()) {
            MessageUtils.send(player, "messages.no-news");
            return;
        }

        new NewsMenu(AquaBook.getInstance(), player, 0).open();
    }
}
