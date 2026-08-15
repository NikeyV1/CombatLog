package de.nikey.combatLog;

import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Command.CombatLogCommand;
import de.nikey.combatLog.Config.PluginConfig;
import de.nikey.combatLog.Listener.*;
import de.nikey.combatLog.Utils.CombatPlaceholders;
import de.nikey.combatLog.Utils.Metrics;
import de.nikey.combatLog.Utils.ModrinthUpdateChecker;
import de.nikey.combatLog.Utils.SafeZoneBarrierManager;
import de.nikey.combatLog.Utils.WorldGuardBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CombatLog extends JavaPlugin {
    private static final int CURRENT_CONFIG_VERSION = 3;
    private static final String CONFIG_VERSION_PATH = "config-version";

    private static final Map<Integer, List<PathMove>> PATH_MIGRATIONS = Map.of(
            3, List.of(
                    new PathMove(
                            "combat-log.restrictions.explosions.set-combat-on-explosion",
                            "combat-log.triggers.explosions.set-combat-on-explosion"
                    )
            )
    );

    private record PathMove(String from, String to) {}

    private CombatManager combatManager;
    private PluginConfig pluginConfig;
    private WorldGuardBridge worldGuardBridge = null;

    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            initWorldGuard();
        }
    }

    // Isolated – WorldGuardHook class only loaded when this method runs
    private void initWorldGuard() {
        de.nikey.combatLog.Utils.WorldGuardHook hook =
                new de.nikey.combatLog.Utils.WorldGuardHook(this);
        hook.register();
        worldGuardBridge = hook;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        updateConfigIfNeeded();
        ensureMessagesFileExists();

        pluginConfig = new PluginConfig(getConfig(), loadMessagesConfig());
        combatManager = new CombatManager(this, pluginConfig);

        registerListeners(pluginConfig);
        registerCommands();
        registerPlaceholders();

        new ModrinthUpdateChecker("LI8sodAD").checkForUpdates();
        new Metrics(this, 28071);
    }

    @Override
    public void onDisable() {
        combatManager.shutdown();
    }

    private void registerListeners(PluginConfig config) {
        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(new CombatTagListener(combatManager, config, worldGuardBridge), this);
        pm.registerEvents(new CombatRestrictionListener(combatManager, config), this);
        pm.registerEvents(new CombatZoneListener(this, combatManager, config), this);
        pm.registerEvents(new CombatLogoutListener(combatManager, config), this);
        pm.registerEvents(new AntiKillAbuse(this), this);

        if (worldGuardBridge != null && worldGuardBridge.isEnabled()) {
            registerWorldGuardListener(pm, config);
        }
    }

    private void registerCommands() {
        PluginCommand command = Objects.requireNonNull(getCommand("combatlog"), "combatlog command missing in plugin.yml");
        CombatLogCommand executor = new CombatLogCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    // Isolated – CombatPlaceholders (and PlaceholderExpansion) never loaded when PAPI is absent
    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return;
        initPlaceholders();
    }

    private void initPlaceholders() {
        new CombatPlaceholders(combatManager).register();
        getLogger().info("PlaceholderAPI detected: placeholders registered.");
    }

    // Isolated – WorldGuardListener and SafeZoneBarrierManager only loaded when WorldGuard is present
    private void registerWorldGuardListener(PluginManager pm, PluginConfig config) {
        SafeZoneBarrierManager barrierManager = new SafeZoneBarrierManager(config);
        combatManager.setBarrierManager(barrierManager);
        pm.registerEvents(
                new de.nikey.combatLog.Listener.WorldGuardListener(combatManager, config, barrierManager), this);
    }

    public static boolean isWorldGuardEnabled() {
        CombatLog instance = getPlugin(CombatLog.class);
        return instance.worldGuardBridge != null && instance.worldGuardBridge.isEnabled();
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public void reloadPluginSettings() {
        reloadConfig();
        updateConfigIfNeeded();
        pluginConfig.reload(getConfig(), loadMessagesConfig());
    }

    private FileConfiguration loadMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        return YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void ensureMessagesFileExists() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
    }

    private void updateConfigIfNeeded() {
        int currentVersion = getConfig().getInt(CONFIG_VERSION_PATH, 1);
        if (currentVersion >= CURRENT_CONFIG_VERSION) {
            return;
        }

        backupConfigFile(currentVersion);
        applyPathMigrations(currentVersion);

        FileConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(getResource("config.yml")), StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            getLogger().warning("Failed to read default config.yml for migration.");
            return;
        }

        getConfig().setDefaults(defaults);
        getConfig().options().copyDefaults(true);
        getConfig().set(CONFIG_VERSION_PATH, CURRENT_CONFIG_VERSION);
        saveConfig();
        reloadConfig();

        getLogger().info("Updated config.yml from version " + currentVersion + " to " + CURRENT_CONFIG_VERSION + ".");
    }

    private void applyPathMigrations(int fromVersion) {
        for (Map.Entry<Integer, List<PathMove>> entry : PATH_MIGRATIONS.entrySet()) {
            if (entry.getKey() <= fromVersion) continue;
            for (PathMove move : entry.getValue()) {
                movePath(move.from(), move.to());
            }
        }
    }

    private void movePath(String oldPath, String newPath) {
        if (!getConfig().isSet(oldPath)) return;

        getConfig().set(newPath, getConfig().get(oldPath));
        getConfig().set(oldPath, null);
        removeIfEmptyParent(oldPath);
    }

    private void removeIfEmptyParent(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot < 0) return;

        String parentPath = path.substring(0, lastDot);
        ConfigurationSection parent = getConfig().getConfigurationSection(parentPath);
        if (parent != null && parent.getKeys(false).isEmpty()) {
            getConfig().set(parentPath, null);
            removeIfEmptyParent(parentPath);
        }
    }

    private void backupConfigFile(int oldVersion) {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            return;
        }

        File backupFile = new File(getDataFolder(), "config.v" + oldVersion + ".bak.yml");
        if (backupFile.exists()) {
            return;
        }

        try {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException exception) {
            getLogger().warning("Failed to create config backup file: " + backupFile.getName());
        }
    }
}