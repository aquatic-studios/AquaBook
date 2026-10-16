package com.aquaticstudios.aquabook.gui;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.news.NewsBook;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import com.aquaticstudios.aquabook.utils.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NewsMenu implements InventoryHolder {
    private final AquaBook plugin;
    private final Player player;
    private final int page;

    private final Map<Integer, String> slots = new HashMap<>();
    private final Inventory inventory;

    private int nextSlot = -1;
    private int previousSlot = -1;
    private int closeSlot = -1;

    public NewsMenu(AquaBook plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = Math.max(0, page);

        this.inventory = Bukkit.createInventory(this, Settings.guiRows * 9,
                MessageUtils.colored("gui.title", Collections.<String, String>emptyMap(), player));
        build();
    }

    private void build() {
        final List<NewsBook> books = plugin.getNews().getVisible(player);
        final List<Integer> contentSlots = Settings.guiSlots;

        final int perPage = Math.max(1, contentSlots.size());
        final int maxPage = Math.max(0, (books.size() - 1) / perPage);
        final int current = Math.min(page, maxPage);

        final int from = current * perPage;
        for (int i = 0; i < contentSlots.size(); i++) {
            final int index = from + i;
            if (index >= books.size()) break;

            final int slot = contentSlots.get(i);
            if (slot < 0 || slot >= inventory.getSize()) continue;

            final NewsBook book = books.get(index);
            inventory.setItem(slot, plugin.getNews().buildIcon(player, book));
            slots.put(slot, book.getId());
        }

        final Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%page%", String.valueOf(current + 1));
        placeholders.put("%pages%", String.valueOf(maxPage + 1));

        if (place(Settings.closeSlot)) {
            closeSlot = Settings.closeSlot;
            inventory.setItem(closeSlot, navItem("close", Settings.closeMaterial, placeholders));
        }
        if (current > 0 && place(Settings.previousSlot)) {
            previousSlot = Settings.previousSlot;
            inventory.setItem(previousSlot, navItem("previous", Settings.previousMaterial, placeholders));
        }
        if (current < maxPage && place(Settings.nextSlot)) {
            nextSlot = Settings.nextSlot;
            inventory.setItem(nextSlot, navItem("next", Settings.nextMaterial, placeholders));
        }
    }

    private boolean place(int slot) {
        return slot >= 0 && slot < inventory.getSize();
    }

    private ItemStack navItem(String key, String materialName, Map<String, String> placeholders) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.ARROW;

        return simpleItem(material,
                MessageUtils.colored("gui.navigation." + key + ".name", placeholders, player),
                MessageUtils.coloredList("gui.navigation." + key + ".lore", placeholders, player));
    }

    private ItemStack simpleItem(Material material, String name, List<String> lore) {
        final ItemStack item = new ItemStack(material == null ? Material.PAPER : material);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public String bookAt(int slot) {
        return slots.get(slot);
    }

    public int getPage() {
        return page;
    }

    public int getNextSlot() {
        return nextSlot;
    }

    public int getPreviousSlot() {
        return previousSlot;
    }

    public int getCloseSlot() {
        return closeSlot;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
