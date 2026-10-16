package com.aquaticstudios.aquabook.news;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NewsBook {
    private final String id;
    private final String title;
    private final String author;
    private final String date;
    private final long timestamp;
    private final Material icon;
    private final int customModelData;
    private final String permission;
    private final boolean footer;
    private final boolean autoOpen;
    private final List<String> lore;
    private final List<List<NewsComponent>> pages;

    private NewsBook(String id, String title, String author, String date, long timestamp, Material icon,
                     int customModelData, String permission, boolean footer, boolean autoOpen, List<String> lore,
                     List<List<NewsComponent>> pages) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.date = date;
        this.timestamp = timestamp;
        this.icon = icon;
        this.customModelData = customModelData;
        this.permission = permission;
        this.footer = footer;
        this.autoOpen = autoOpen;
        this.lore = lore;
        this.pages = pages;
    }

    public static NewsBook load(String id, FileConfiguration config, String dateFormat, long fallbackTimestamp) {
        String title = config.getString("title", id);
        String author = config.getString("author", "Original");
        String date = config.getString("date", "");
        long timestamp = config.getLong("timestamp", 0L);
        if (timestamp <= 0L) timestamp = parseDate(date, dateFormat, fallbackTimestamp);

        Material icon = Material.matchMaterial(config.getString("icon", "ENCHANTED_BOOK"));
        if (icon == null) icon = Material.ENCHANTED_BOOK;

        int customModelData = config.getInt("custom-model-data", 0);
        String permission = config.getString("permission", "");
        boolean footer = config.getBoolean("footer", true);
        boolean autoOpen = config.getBoolean("auto-open", true);
        List<String> lore = config.getStringList("lore");

        List<List<NewsComponent>> pages = new ArrayList<>();
        List<?> rawPages = config.getList("pages");
        if (rawPages != null) {
            for (Object rawPage : rawPages) {
                List<NewsComponent> page = new ArrayList<>();
                if (rawPage instanceof List) {
                    for (Object entry : (List<?>) rawPage) {
                        NewsComponent component = NewsComponent.fromObject(entry);
                        if (component != null) page.add(component);
                    }
                } else {
                    NewsComponent component = NewsComponent.fromObject(rawPage);
                    if (component != null) page.add(component);
                }
                pages.add(page);
            }
        }
        if (pages.isEmpty()) pages.add(new ArrayList<>());

        return new NewsBook(id, title, author, date, timestamp, icon, customModelData, permission, footer,
                autoOpen, lore, pages);
    }

    private static long parseDate(String date, String pattern, long fallback) {
        if (date == null || date.isEmpty()) return fallback;
        try {
            return new SimpleDateFormat(pattern).parse(date).getTime();
        } catch (ParseException | IllegalArgumentException ex) {
            return fallback;
        }
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Material getIcon() {
        return icon;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getPermission() {
        return permission;
    }

    public boolean hasFooter() {
        return footer;
    }

    public boolean isAutoOpen() {
        return autoOpen;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public List<List<NewsComponent>> getPages() {
        return Collections.unmodifiableList(pages);
    }
}
