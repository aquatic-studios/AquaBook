package com.aquaticstudios.aquabook.commands.subcommands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.utils.CC;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import com.aquaticstudios.aquabook.utils.Settings;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateSubcommand implements Subcommand {
    private static final String TEMPLATE = "template";

    @NotNull
    @Override
    public String name() {
        return "create";
    }

    @Nullable
    @Override
    public String permission() {
        return "create";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            MessageUtils.send(sender, "messages.usage-create");
            return;
        }

        if (TEMPLATE.equalsIgnoreCase(args[1])) {
            writeTemplate(sender);
            return;
        }

        fromHeldBook(sender, args[1]);
    }

    private void writeTemplate(CommandSender sender) {
        final AquaBook plugin = AquaBook.getInstance();
        final File folder = plugin.getNews().getFolder();

        String id = TEMPLATE;
        File file = new File(folder, id + ".yml");
        for (int suffix = 2; file.exists(); suffix++) {
            id = TEMPLATE + "-" + suffix;
            file = new File(folder, id + ".yml");
        }

        try (InputStream resource = plugin.getResource("news/template.yml")) {
            if (resource == null) {
                MessageUtils.send(sender, "messages.template-missing");
                return;
            }
            folder.mkdirs();
            Files.copy(resource, file.toPath());
        } catch (IOException exception) {
            MessageUtils.send(sender, "messages.save-failed",
                    Subcommands.placeholders("%error%", String.valueOf(exception.getMessage())));
            return;
        }

        reload(plugin);
        MessageUtils.send(sender, "messages.template-created", Subcommands.placeholders("%id%", id));
    }

    private void fromHeldBook(CommandSender sender, String rawId) {
        final Player player = Subcommands.player(sender);
        if (player == null) return;

        final AquaBook plugin = AquaBook.getInstance();
        final String id = rawId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        if (id.isEmpty()) {
            MessageUtils.send(sender, "messages.invalid-id");
            return;
        }
        if (plugin.getNews().get(id) != null) {
            MessageUtils.send(sender, "messages.already-exists", Subcommands.placeholders("%id%", id));
            return;
        }

        final ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.WRITTEN_BOOK || !(hand.getItemMeta() instanceof BookMeta)) {
            MessageUtils.send(sender, "messages.hold-book");
            return;
        }

        final BookMeta meta = (BookMeta) hand.getItemMeta();
        final YamlConfiguration config = new YamlConfiguration();

        config.set("title", meta.hasTitle() ? CC.unformat(meta.getTitle()) : id);
        config.set("author", meta.hasAuthor() ? CC.unformat(meta.getAuthor()) : "Original");
        config.set("date", new SimpleDateFormat(Settings.dateFormat).format(new Date()));

        config.set("timestamp", System.currentTimeMillis());
        config.set("icon", "ENCHANTED_BOOK");
        config.set("permission", "");
        config.set("auto-open", true);
        config.set("footer", true);

        final List<List<String>> pages = new ArrayList<>();
        for (String page : meta.getPages()) {
            pages.add(Collections.singletonList(CC.unformat(page)));
        }
        if (pages.isEmpty()) pages.add(Collections.singletonList("&0"));
        config.set("pages", pages);

        final File file = new File(plugin.getNews().getFolder(), id + ".yml");
        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException exception) {
            MessageUtils.send(sender, "messages.save-failed",
                    Subcommands.placeholders("%error%", String.valueOf(exception.getMessage())));
            return;
        }

        reload(plugin);
        MessageUtils.send(sender, "messages.created", Subcommands.placeholders("%id%", id));
    }

    private void reload(AquaBook plugin) {
        plugin.reloadAll();
        plugin.getRedis().publish("RELOAD");
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2 && TEMPLATE.startsWith(args[1].toLowerCase(Locale.ROOT))) {
            return Collections.singletonList(TEMPLATE);
        }
        return Collections.emptyList();
    }
}
