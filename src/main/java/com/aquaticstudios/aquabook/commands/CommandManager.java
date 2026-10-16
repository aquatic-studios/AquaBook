package com.aquaticstudios.aquabook.commands;

import com.aquaticstudios.aquabook.AquaBook;
import com.aquaticstudios.aquabook.utils.PermissionUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public final class CommandManager {
    private static BookCommand command;
    private static CommandMap commandMap;

    public static void registerCommands() {
        registerPermissions();
        registerCommand();
    }

    public static void unregisterCommands() {
        if (command == null || commandMap == null) return;
        command.unregister(commandMap);

        try {
            final Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);
            @SuppressWarnings("unchecked") final Map<String, Command> known = (Map<String, Command>) field.get(commandMap);
            known.values().removeIf(entry -> entry == command);
        } catch (Exception ignored) {
        }

        command = null;
        commandMap = null;
    }

    private static void registerCommand() {
        commandMap = resolveCommandMap();
        if (commandMap == null) {
            AquaBook.getInstance().getLogger().severe("Could not access the server command map. The command was not registered.");
            return;
        }

        command = new BookCommand("aquabook", Arrays.asList("abook", "news", "novedades"));
        commandMap.register(AquaBook.getInstance().getName().toLowerCase(), command);
    }

    private static CommandMap resolveCommandMap() {
        final AquaBook plugin = AquaBook.getInstance();
        try {
            final Method method = plugin.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) method.invoke(plugin.getServer());
        } catch (Exception ignored) {
        }

        try {
            final Field field = plugin.getServer().getPluginManager().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(plugin.getServer().getPluginManager());
        } catch (Exception ignored) {
        }

        return null;
    }

    private static void registerPermissions() {
        final String root = PermissionUtils.ROOT;
        final Permission admin = define(root + ".admin", PermissionDefault.OP);

        admin.getChildren().put(define(root + ".use", PermissionDefault.TRUE).getName(), true);
        admin.getChildren().put(define(root + ".reload", PermissionDefault.OP).getName(), true);
        admin.getChildren().put(define(root + ".create", PermissionDefault.OP).getName(), true);
        admin.getChildren().put(define(root + ".delete", PermissionDefault.OP).getName(), true);
        admin.getChildren().put(define(root + ".list", PermissionDefault.OP).getName(), true);
        admin.getChildren().put(define(root + ".storage", PermissionDefault.OP).getName(), true);
        admin.getChildren().put(define(root + ".setlikes", PermissionDefault.OP).getName(), true);
        admin.getChildren().put(define(root + ".unread", PermissionDefault.OP).getName(), true);
        admin.recalculatePermissibles();
    }

    private static Permission define(String node, PermissionDefault fallback) {
        final PluginManager manager = AquaBook.getInstance().getServer().getPluginManager();
        Permission permission = manager.getPermission(node);
        if (permission != null) return permission;

        permission = new Permission(node, fallback);
        manager.addPermission(permission);
        return permission;
    }
}
