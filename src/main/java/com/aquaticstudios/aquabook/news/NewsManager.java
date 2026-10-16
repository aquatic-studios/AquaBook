package com.aquaticstudios.aquabook.news;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.gui.NewsMenu;
import com.aquaticstudios.aquabook.storage.NewsStats;
import com.aquaticstudios.aquabook.utils.CC;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import com.aquaticstudios.aquabook.utils.Settings;
import com.aquaticstudios.aquabook.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NewsManager {
    private final AquaBook plugin;
    private final Map<String, NewsBook> books = new LinkedHashMap<>();
    private final Map<String, NewsStats> stats = new ConcurrentHashMap<>();

    private File folder;

    public NewsManager(AquaBook plugin) {
        this.plugin = plugin;
    }

    public void load() {
        books.clear();

        folder = new File(plugin.getDataFolder(), "news");
        if (!folder.exists()) {
            folder.mkdirs();
            plugin.saveResource("news/hi.yml", false);
        }

        final File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                final String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
                try {
                    books.put(id, NewsBook.load(id, YamlConfiguration.loadConfiguration(file),
                            Settings.dateFormat, file.lastModified()));
                } catch (Exception exception) {
                    plugin.getLogger().severe("Could not load news entry " + id + ": " + exception.getMessage());
                }
            }
        }

        sort();
        refreshStats();
    }

    private void sort() {
        final boolean newestFirst = !"oldest".equalsIgnoreCase(Settings.guiSort);
        final List<NewsBook> sorted = new ArrayList<>(books.values());
        sorted.sort(Comparator.comparingLong(NewsBook::getTimestamp));
        if (newestFirst) Collections.reverse(sorted);

        books.clear();
        for (NewsBook book : sorted) books.put(book.getId(), book);
    }

    public void refreshStats() {
        plugin.getStorageQueue().submit(() -> {
            final Map<String, NewsStats> loaded = plugin.getStorage().loadStats();
            stats.clear();
            stats.putAll(loaded);
        });
    }

    public NewsBook get(String id) {
        return id == null ? null : books.get(id.toLowerCase());
    }

    public List<NewsBook> getBooks() {
        return new ArrayList<>(books.values());
    }

    public List<String> getIds() {
        return new ArrayList<>(books.keySet());
    }

    public List<NewsBook> getVisible(Player player) {
        final List<NewsBook> visible = new ArrayList<>();
        for (NewsBook book : books.values()) {
            if (canSee(player, book)) visible.add(book);
        }
        return visible;
    }

    public List<NewsBook> getUnread(Player player) {
        final List<NewsBook> unread = new ArrayList<>();
        for (NewsBook book : getVisible(player)) {
            if (!plugin.getPlayers().isRead(player.getUniqueId(), book.getId())) unread.add(book);
        }
        return unread;
    }

    public List<NewsBook> getAutoOpenQueue(Player player) {
        final List<NewsBook> queue = new ArrayList<>();
        for (NewsBook book : getUnread(player)) {
            if (book.isAutoOpen()) queue.add(book);
        }
        queue.sort(Comparator.comparingLong(NewsBook::getTimestamp).reversed());

        if (Settings.autoOpenOnlyLatest && queue.size() > 1) {
            return new ArrayList<>(queue.subList(0, 1));
        }
        return queue;
    }

    public boolean canSee(Player player, NewsBook book) {
        final String permission = book.getPermission();
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }

    public NewsStats getStats(String id) {
        return stats.getOrDefault(id, NewsStats.empty());
    }

    public int getLikes(String id) {
        return getStats(id).getLikes();
    }

    public long getViews(String id) {
        return getStats(id).getViews();
    }

    public File getFolder() {
        return folder;
    }

    public void applyLocalLike(String newsId, boolean liked) {
        final NewsStats current = getStats(newsId);
        stats.put(newsId, current.withLikes(current.getLikes() + (liked ? 1 : -1)));
    }

    public void addView(String newsId) {
        final NewsStats current = getStats(newsId);
        stats.put(newsId, current.withViews(current.getViews() + 1));

        plugin.getStorageQueue().submit(() -> plugin.getStorage().addView(newsId));
        plugin.getRedis().publish("VIEW", newsId);
    }

    public void setLikesBase(String newsId, int base) {
        plugin.getStorageQueue().submit(() -> {
            plugin.getStorage().setLikesBase(newsId, base);
            final Map<String, NewsStats> loaded = plugin.getStorage().loadStats();
            stats.clear();
            stats.putAll(loaded);
            plugin.getRedis().publish("LIKES", newsId, String.valueOf(getLikes(newsId)));
        });
    }

    public void purge(String newsId) {
        stats.remove(newsId);
        plugin.getPlayers().purge(newsId);
        plugin.getStorageQueue().submit(() -> plugin.getStorage().purgeNews(newsId));
        plugin.getRedis().publish("PURGE", newsId);
    }

    public void applyRemoteLike(String newsId, UUID uuid, boolean liked) {
        plugin.getPlayers().applyRemoteLike(uuid, newsId, liked);
        applyLocalLike(newsId, liked);
    }

    public void applyRemoteView(String newsId) {
        final NewsStats current = getStats(newsId);
        stats.put(newsId, current.withViews(current.getViews() + 1));
    }

    public void applyRemoteLikes(String newsId, int total) {
        stats.put(newsId, getStats(newsId).withLikes(total));
    }

    public void applyRemotePurge(String newsId) {
        stats.remove(newsId);
        plugin.getPlayers().purge(newsId);
    }

    public Map<String, String> placeholders(Player player, NewsBook book) {
        final boolean read = plugin.getPlayers().isRead(player.getUniqueId(), book.getId());
        final boolean liked = plugin.getPlayers().isLiked(player.getUniqueId(), book.getId());

        final Map<String, String> map = new HashMap<>();
        map.put("%id%", book.getId());
        map.put("%title%", book.getTitle());
        map.put("%author%", book.getAuthor());
        map.put("%date%", book.getDate());
        map.put("%likes%", String.valueOf(getLikes(book.getId())));
        map.put("%views%", String.valueOf(getViews(book.getId())));
        map.put("%player%", player == null ? "" : player.getName());
        map.put("%version%", plugin.getDescription().getVersion());
        map.put("%server-version%", VersionUtil.getDisplayVersion());
        map.put("%state%", MessageUtils.raw(read ? "gui.state-read" : "gui.state-unread"));
        map.put("%like-state%", MessageUtils.raw(liked ? "gui.state-liked" : "gui.state-not-liked"));
        return map;
    }

    public ItemStack buildBook(Player player, NewsBook book) {
        final ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        final BookMeta meta = (BookMeta) item.getItemMeta();
        if (meta == null) return item;

        meta.setTitle(CC.truncate(CC.format(book.getTitle(), player), 32));
        meta.setAuthor(CC.format(book.getAuthor(), player));

        final Map<String, String> placeholders = placeholders(player, book);
        final TextColor defaultColor = defaultColor();

        final boolean footer = Settings.footerEnabled && book.hasFooter();
        final List<List<NewsComponent>> pages = book.getPages();
        final List<BaseComponent[]> built = new ArrayList<>();

        for (int i = 0; i < pages.size(); i++) {
            Component page = Component.empty();
            for (NewsComponent component : pages.get(i)) {
                page = page.append(component.build(placeholders, player));
            }
            final boolean last = i == pages.size() - 1;
            if (footer && (Settings.footerOnEveryPage || last)) {
                page = page.append(buildFooter(player, book, placeholders));
            }
            built.add(CC.toBungee(page.colorIfAbsent(defaultColor)));
        }

        meta.spigot().setPages(built);
        item.setItemMeta(meta);
        return item;
    }

    private TextColor defaultColor() {
        final NamedTextColor named = NamedTextColor.NAMES.value(
                Settings.bookDefaultColor.toLowerCase(Locale.ROOT));
        return named == null ? NamedTextColor.BLACK : named;
    }

    private Component buildFooter(Player player, NewsBook book, Map<String, String> placeholders) {
        final boolean liked = plugin.getPlayers().isLiked(player.getUniqueId(), book.getId());
        final boolean read = plugin.getPlayers().isRead(player.getUniqueId(), book.getId());
        final String separator = MessageUtils.raw("book.footer-separator");

        Component footer = CC.parse(MessageUtils.raw("book.footer-prefix"), player);

        footer = footer.append(NewsComponent.of(
                MessageUtils.raw(liked ? "book.liked" : "book.like"),
                MessageUtils.raw(liked ? "book.liked-hover" : "book.like-hover"),
                (!liked || Settings.allowUnlike) ? "/aquabook like " + book.getId() : null)
                .build(placeholders, player));

        footer = footer.append(CC.parse(separator, player));

        footer = footer.append(NewsComponent.of(
                MessageUtils.raw(read ? "book.already-read" : "book.read"),
                MessageUtils.raw(read ? "book.already-read-hover" : "book.read-hover"),
                read ? null : "/aquabook read " + book.getId())
                .build(placeholders, player));

        if (Settings.menuButtonEnabled && (read || !Settings.menuButtonOnlyWhenRead)) {
            footer = footer.append(CC.parse(separator, player));
            footer = footer.append(NewsComponent.of(
                    MessageUtils.raw("book.menu"),
                    MessageUtils.raw("book.menu-hover"),
                    "/aquabook menu")
                    .build(placeholders, player));
        }

        return footer;
    }

    public ItemStack buildIcon(Player player, NewsBook book) {
        final boolean read = plugin.getPlayers().isRead(player.getUniqueId(), book.getId());

        final ItemStack item = new ItemStack(iconMaterial(book, read));
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        final Map<String, String> placeholders = placeholders(player, book);

        if (meta instanceof BookMeta) {
            final BookMeta bookMeta = (BookMeta) meta;
            bookMeta.setTitle(CC.truncate(CC.format(book.getTitle(), player), 32));
            bookMeta.setAuthor(CC.format(book.getAuthor(), player));
            bookMeta.setPages(" ");
        }

        meta.setDisplayName(MessageUtils.colored(
                read ? "gui.item.read-name" : "gui.item.name", placeholders, player));

        final List<String> lore = book.getLore().isEmpty()
                ? MessageUtils.coloredList(read ? "gui.item.read-lore" : "gui.item.lore", placeholders, player)
                : CC.format(applyAll(book.getLore(), placeholders), player);
        meta.setLore(lore);

        if (book.getCustomModelData() > 0) meta.setCustomModelData(book.getCustomModelData());

        if (!read && Settings.glowUnread && item.getType() != Material.ENCHANTED_BOOK) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS);

        item.setItemMeta(meta);
        return item;
    }

    private Material iconMaterial(NewsBook book, boolean read) {
        if (!read) return book.getIcon();

        final String configured = Settings.readMaterial;
        if (configured == null || configured.isEmpty()) return book.getIcon();

        final Material material = Material.matchMaterial(configured);
        return material == null ? book.getIcon() : material;
    }

    private List<String> applyAll(List<String> lines, Map<String, String> placeholders) {
        final List<String> out = new ArrayList<>();
        for (String line : lines) {
            String parsed = line;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                parsed = parsed.replace(entry.getKey(), entry.getValue());
            }
            out.add(parsed);
        }
        return out;
    }

    public void open(Player player, NewsBook book) {
        addView(book.getId());
        player.openBook(buildBook(player, book));
    }

    public void afterAction(Player player, NewsBook book) {
        final AfterAction action = Settings.afterAction;

        if (action == AfterAction.CLOSE) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (action == AfterAction.MENU) {
                new NewsMenu(plugin, player, 0).open();
            } else {
                player.openBook(buildBook(player, book));
            }
        }, Settings.reopenDelay);
    }
}
