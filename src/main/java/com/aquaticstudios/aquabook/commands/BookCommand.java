package com.aquaticstudios.aquabook.commands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.commands.subcommands.CreateSubcommand;
import com.aquaticstudios.aquabook.commands.subcommands.DeleteSubcommand;
import com.aquaticstudios.aquabook.commands.subcommands.HelpSubcommand;
import com.aquaticstudios.aquabook.commands.subcommands.LikeSubcommand;
import com.aquaticstudios.aquabook.commands.subcommands.ListSubcommand;
import com.aquaticstudios.aquabook.commands.subcommands.MenuSubcommand;
import com.aquaticstudios.aquabook.commands.subcommands.OpenSubcommand;
import com.aquaticstudios.aquabook.commands.subcommands.ReadSubcommand;
import com.aquaticstudios.aquabook.commands.subcommands.ReloadSubcommand;
import com.aquaticstudios.aquabook.commands.subcommands.SetLikesSubcommand;
import com.aquaticstudios.aquabook.commands.subcommands.StorageSubcommand;
import com.aquaticstudios.aquabook.commands.subcommands.Subcommand;
import com.aquaticstudios.aquabook.commands.subcommands.UnreadSubcommand;
import com.aquaticstudios.aquabook.utils.CC;
import com.aquaticstudios.aquabook.utils.MessageUtils;
import com.aquaticstudios.aquabook.utils.PermissionUtils;
import com.aquaticstudios.aquabook.utils.VersionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookCommand extends Command {
    private final Map<String, Subcommand> subcommands = new LinkedHashMap<>();

    public BookCommand(@NotNull String label, @NotNull List<String> aliases) {
        super(label);
        setAliases(aliases);
        setDescription("Main command.");

        register(new HelpSubcommand());
        register(new MenuSubcommand());
        register(new OpenSubcommand());
        register(new LikeSubcommand());
        register(new ReadSubcommand());
        register(new ListSubcommand());
        register(new StorageSubcommand());
        register(new CreateSubcommand());
        register(new DeleteSubcommand());
        register(new SetLikesSubcommand());
        register(new UnreadSubcommand());
        register(new ReloadSubcommand());
    }

    private void register(@NotNull Subcommand subcommand) {
        subcommands.put(subcommand.name().toLowerCase(), subcommand);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            banner(sender);
            return true;
        }

        final Subcommand subcommand = subcommands.get(args[0].toLowerCase());
        if (subcommand == null) {
            subcommands.get("open").execute(sender, new String[]{"open", args[0]});
            return true;
        }

        if (subcommand.permission() != null && !PermissionUtils.hasPermission(sender, subcommand.permission())) {
            MessageUtils.send(sender, "messages.no-permission");
            return true;
        }

        subcommand.execute(sender, args);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            final String typed = args[0].toLowerCase();
            final List<String> results = new ArrayList<>();

            for (Subcommand subcommand : subcommands.values()) {
                if (subcommand.hidden()) continue;
                if (!subcommand.name().startsWith(typed)) continue;
                if (subcommand.permission() != null && !PermissionUtils.hasPermission(sender, subcommand.permission())) continue;
                results.add(subcommand.name());
            }
            for (String id : AquaBook.getInstance().getNews().getIds()) {
                if (id.startsWith(typed)) results.add(id);
            }
            return results;
        }

        final Subcommand subcommand = subcommands.get(args[0].toLowerCase());
        if (subcommand == null) return new ArrayList<>();
        if (subcommand.permission() != null && !PermissionUtils.hasPermission(sender, subcommand.permission())) return new ArrayList<>();
        return subcommand.tabComplete(sender, args);
    }

    private void banner(@NotNull CommandSender sender) {
        final String version = AquaBook.getInstance().getDescription().getVersion();

        sender.sendMessage(CC.format(" "));
        sender.sendMessage(CC.format("            &#33CCFF&lAquaBook &fversion &#8DFF87[" + version
                + "] &7(" + VersionUtil.getDisplayVersion() + ")"));
        sender.sendMessage(CC.format("           &fPowered by &#8BD5FFSenkex @ Aquatic Studios"));
        sender.sendMessage(CC.format(" "));
    }
}
