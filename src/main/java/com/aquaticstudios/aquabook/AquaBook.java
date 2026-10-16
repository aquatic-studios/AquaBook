package com.aquaticstudios.aquabook;

import com.aquaticstudios.aquabook.commands.CommandManager;
import com.aquaticstudios.aquabook.data.PlayerCache;
import com.aquaticstudios.aquabook.gui.NewsMenuListener;
import com.aquaticstudios.aquabook.library.Libraries;
import com.aquaticstudios.aquabook.listener.ConnectionListener;
import com.aquaticstudios.aquabook.news.NewsManager;
import com.aquaticstudios.aquabook.redis.RedisManager;
import com.aquaticstudios.aquabook.storage.Storage;
import com.aquaticstudios.aquabook.storage.StorageQueue;
import com.aquaticstudios.aquabook.storage.StorageType;
import com.aquaticstudios.aquabook.storage.type.H2Storage;
import com.aquaticstudios.aquabook.storage.type.MySQLStorage;
import com.aquaticstudios.aquabook.storage.type.SQLiteStorage;
import com.aquaticstudios.aquabook.utils.AbstractConfig;
import com.aquaticstudios.aquabook.utils.CC;
import com.aquaticstudios.aquabook.utils.Config;
import com.aquaticstudios.aquabook.utils.Messages;
import com.aquaticstudios.aquabook.utils.PlaceholderHook;
import com.aquaticstudios.aquabook.utils.Settings;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.zapper.DependencyManager;
import revxrsal.zapper.classloader.URLClassLoaderWrapper;
import revxrsal.zapper.repository.Repository;

import java.io.File;
import java.net.URLClassLoader;

public final class AquaBook extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 33743;

    public static YamlDocument CONFIG;
    public static YamlDocument MESSAGES;

    private static AquaBook instance;

    private AbstractConfig abstractConfig;
    private AbstractConfig abstractMessages;

    private NewsManager news;
    private PlayerCache players;
    private RedisManager redis;

    private Storage storage;
    private StorageQueue storageQueue;
    private StorageType storageType;

    @Override
    public void onLoad() {
        instance = this;

        saveDefaultConfig();
        this.storageType = StorageType.fromName(getConfig().getString("storage.type", "SQLITE"));

        final File libraries = new File(getDataFolder(), "libraries");
        if (!libraries.exists() && !libraries.mkdirs()) {
            getLogger().warning("Could not create the libraries folder");
        }

        final DependencyManager manager = new DependencyManager(
                libraries,
                URLClassLoaderWrapper.wrap((URLClassLoader) getClassLoader())
        );
        manager.repository(Repository.mavenCentral());

        getLogger().info("Installing runtime driver for backend: " + storageType.name());
        for (Libraries library : Libraries.values()) {
            if (library.isNeededFor(storageType)) manager.dependency(library.dependency());
        }
        manager.load();
    }

    @Override
    public void onEnable() {
        instance = this;

        abstractConfig = new Config();
        abstractConfig.setup();
        CONFIG = abstractConfig.getConfig();

        abstractMessages = new Messages();
        abstractMessages.setup();
        MESSAGES = abstractMessages.getConfig();

        Settings.load();
        PlaceholderHook.detect();

        storageQueue = new StorageQueue("AquaBook-Storage-Thread");
        storage = createStorage();

        try {
            storage.setup();
        } catch (Exception exception) {
            getLogger().severe("Could not start the " + storageType + " backend: " + exception.getMessage());
            getLogger().severe("AquaBook is disabling itself so no data gets lost.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        redis = new RedisManager(this);
        redis.connect();

        players = new PlayerCache(this);
        news = new NewsManager(this);
        news.load();

        getServer().getPluginManager().registerEvents(new NewsMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);

        CommandManager.registerCommands();

        new Metrics(this, BSTATS_PLUGIN_ID);

        getServer().getOnlinePlayers().forEach(player -> players.load(player.getUniqueId()));

        final long sync = Settings.syncIntervalSeconds * 20L;
        getServer().getScheduler().runTaskTimer(this, news::refreshStats, sync, sync);

        Bukkit.getConsoleSender().sendMessage(CC.format("&#33CCFF[AquaBook] Enabled! Storage: &f"
                + storage.getType() + "&#33CCFF, Redis: &f" + (redis.isEnabled() ? redis.getServerId() : "disabled")
                + "&#33CCFF, news: &f" + news.getBooks().size()));
    }

    @Override
    public void onDisable() {
        if (redis != null) redis.close();
        if (storageQueue != null) storageQueue.shutdown(5000L);
        if (storage != null) storage.disable();

        CommandManager.unregisterCommands();
        instance = null;
    }

    private Storage createStorage() {
        switch (storageType) {
            case MYSQL:
                return new MySQLStorage();
            case H2:
                return new H2Storage();
            default:
                return new SQLiteStorage();
        }
    }

    public void reloadAll() {
        abstractConfig.reloadConfig();
        abstractMessages.reloadConfig();
        CONFIG = abstractConfig.getConfig();
        MESSAGES = abstractMessages.getConfig();

        Settings.load();
        PlaceholderHook.detect();
        news.load();
    }

    public static AquaBook getInstance() {
        return instance;
    }

    public NewsManager getNews() {
        return news;
    }

    public PlayerCache getPlayers() {
        return players;
    }

    public RedisManager getRedis() {
        return redis;
    }

    public Storage getStorage() {
        return storage;
    }

    public StorageQueue getStorageQueue() {
        return storageQueue;
    }
}
