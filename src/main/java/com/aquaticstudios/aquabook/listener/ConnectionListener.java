package com.aquaticstudios.aquabook.listener;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.news.NewsBook;
import com.aquaticstudios.aquabook.utils.CC;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import com.aquaticstudios.aquabook.utils.PermissionUtils;
import com.aquaticstudios.aquabook.utils.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConnectionListener implements Listener {
    private final AquaBook plugin;

    public ConnectionListener(AquaBook plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        plugin.getPlayers().load(player.getUniqueId()).whenComplete((data, error) -> {
            if (error != null) {
                plugin.getLogger().severe("Could not load the data of " + player.getName()
                        + ": " + error.getMessage());
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> onDataReady(player));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayers().unload(event.getPlayer().getUniqueId());
    }

    private void onDataReady(Player player) {
        if (!player.isOnline() || !PermissionUtils.canUse(player)) return;

        final List<NewsBook> unread = plugin.getNews().getUnread(player);

        if (Settings.autoOpenEnabled) {
            final List<NewsBook> pending = plugin.getNews().getAutoOpenQueue(player);
            if (!pending.isEmpty()) {
                openNext(player, new ArrayList<>(pending.subList(0, Math.min(Settings.autoOpenMax, pending.size()))),
                        Settings.autoOpenDelay);
            }
        }

        if (Settings.joinNotify) {
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> notifyPlayer(player, unread.size()), Settings.joinDelay);
        }
    }

    private void openNext(Player player, List<NewsBook> queue, long delay) {
        if (queue.isEmpty()) return;
        final NewsBook book = queue.remove(0);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            if (plugin.getPlayers().isRead(player.getUniqueId(), book.getId())) {
                openNext(player, queue, 1L);
                return;
            }
            plugin.getNews().open(player, book);
            openNext(player, queue, Settings.autoOpenGap);
        }, delay);
    }

    private void notifyPlayer(Player player, int unread) {
        if (!player.isOnline()) return;
        if (unread <= 0 && Settings.joinOnlyIfUnread) return;

        final Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%unread%", String.valueOf(unread));
        placeholders.put("%total%", String.valueOf(plugin.getNews().getVisible(player).size()));
        placeholders.put("%player%", player.getName());

        final String hover = MessageUtils.raw("join.hover", placeholders);

        for (String line : MessageUtils.rawList("join.message", placeholders)) {
            Component component = CC.parse(line, player);

            if (Settings.joinClickCommand != null && !Settings.joinClickCommand.isEmpty()) {
                component = component.clickEvent(ClickEvent.runCommand(Settings.joinClickCommand));
            }
            if (!hover.isEmpty()) {
                component = component.hoverEvent(HoverEvent.showText(CC.parse(hover, player)));
            }
            player.spigot().sendMessage(CC.toBungee(component));
        }

        if (Settings.joinSound != null && !Settings.joinSound.isEmpty()) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf(Settings.joinSound.toUpperCase()), 1f, 1f);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
