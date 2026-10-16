package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.storage.StorageException;
import com.aquaticstudios.aquabook.storage.StorageStats;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class StorageSubcommand implements Subcommand {
    @NotNull
    @Override
    public String name() {
        return "storage";
    }

    @Nullable
    @Override
    public String permission() {
        return "storage";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        final AquaBook plugin = AquaBook.getInstance();
        MessageUtils.send(sender, "messages.gathering-stats");

        plugin.getStorageQueue().submit(() -> {
            final StorageStats stats;
            try {
                stats = plugin.getStorage().stats();
            } catch (StorageException exception) {
                exception.printStackTrace();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> MessageUtils.send(sender, "messages.storage-read-failed"));
                return;
            }

            final int backlog = plugin.getStorageQueue().pending();
            final int cached = plugin.getPlayers().size();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                final Map<String, String> placeholders = Subcommands.placeholders(
                        "%backend%", plugin.getStorage().getType(),
                        "%redis%", plugin.getRedis().isEnabled() ? plugin.getRedis().getServerId() : "disabled",
                        "%channel%", plugin.getRedis().getChannel(),
                        "%entries%", String.valueOf(plugin.getNews().getBooks().size()),
                        "%rows%", String.valueOf(stats.getPlayerRows()),
                        "%tracked%", String.valueOf(stats.getNewsRows()),
                        "%likes%", String.valueOf(stats.getLikeRows()),
                        "%cached%", String.valueOf(cached),
                        "%backlog%", String.valueOf(backlog));
                MessageUtils.sendList(sender, "storage", placeholders);
            });
        });
    }
}
