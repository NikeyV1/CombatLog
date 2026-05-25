package de.nikey.combatLog;

import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.PluginConfig;
import de.nikey.combatLog.Listener.*;
import de.nikey.combatLog.Utils.Metrics;
import de.nikey.combatLog.Utils.ModrinthUpdateChecker;
import de.nikey.combatLog.Utils.WorldGuardBridge;
import org.bukkit.Bukkit;
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
import java.util.Objects;

public final class CombatLog extends JavaPlugin {
    private static final int CURRENT_CONFIG_VERSION = 2;
    private static final String CONFIG_VERSION_PATH = "config-version";

    private CombatManager combatManager;
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

        PluginConfig pluginConfig = new PluginConfig(getConfig());
        combatManager = new CombatManager(this, pluginConfig);

        registerListeners(pluginConfig);

        new ModrinthUpdateChecker("LI8sodAD").checkForUpdates();
        new Metrics(this, 28071);
    }

    @Override
    public void onDisable() {
        combatManager.shutdown();
    }

    private void registerListeners(PluginConfig config) {
        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(new CombatTagListener(combatManager, config), this);
        pm.registerEvents(new CombatRestrictionListener(combatManager, config), this);
        pm.registerEvents(new CombatZoneListener(this, combatManager, config), this);
        pm.registerEvents(new CombatLogoutListener(combatManager, config), this);
        pm.registerEvents(new AntiKillAbuse(this), this);

        if (worldGuardBridge != null && worldGuardBridge.isEnabled()) {
            registerWorldGuardListener(pm, config);
        }
    }

    // Isolated – WorldGuardListener class only loaded when this method runs
    private void registerWorldGuardListener(PluginManager pm, PluginConfig config) {
        pm.registerEvents(
                new de.nikey.combatLog.Listener.WorldGuardListener(combatManager, config), this);
    }

    public static boolean isWorldGuardEnabled() {
        CombatLog instance = getPlugin(CombatLog.class);
        return instance.worldGuardBridge != null && instance.worldGuardBridge.isEnabled();
    }

    private void updateConfigIfNeeded() {
        int currentVersion = getConfig().getInt(CONFIG_VERSION_PATH, 1);
        if (currentVersion >= CURRENT_CONFIG_VERSION) {
            return;
        }

        backupConfigFile(currentVersion);
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
