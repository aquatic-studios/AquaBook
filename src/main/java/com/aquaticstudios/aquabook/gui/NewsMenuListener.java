package com.aquaticstudios.aquabook.gui;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.news.NewsBook;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import com.aquaticstudios.aquabook.utils.Settings;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class NewsMenuListener implements Listener {
    private final AquaBook plugin;

    public NewsMenuListener(AquaBook plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof NewsMenu) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        final InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof NewsMenu)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        final Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) return;

        final NewsMenu menu = (NewsMenu) holder;
        final int slot = event.getRawSlot();

        if (slot == menu.getCloseSlot()) {
            player.closeInventory();
            return;
        }
        if (slot == menu.getNextSlot()) {
            new NewsMenu(plugin, player, menu.getPage() + 1).open();
            playSound(player, Settings.pageSound);
            return;
        }
        if (slot == menu.getPreviousSlot()) {
            new NewsMenu(plugin, player, menu.getPage() - 1).open();
            playSound(player, Settings.pageSound);
            return;
        }

        final String id = menu.bookAt(slot);
        if (id == null) return;

        final NewsBook book = plugin.getNews().get(id);
        if (book == null) {
            final Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%id%", id);
            MessageUtils.send(player, "messages.news-not-found", placeholders);
            return;
        }
        if (!plugin.getNews().canSee(player, book)) {
            MessageUtils.send(player, "messages.no-permission", Collections.<String, String>emptyMap());
            return;
        }

        playSound(player, Settings.openBookSound);
        player.closeInventory();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) plugin.getNews().open(player, book);
        });
    }

    private void playSound(Player player, String raw) {
        if (raw == null || raw.isEmpty()) return;
        try {
            player.playSound(player.getLocation(), Sound.valueOf(raw.toUpperCase()), 1f, 1f);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
