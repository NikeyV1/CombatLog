package de.nikey.combatLog;

import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Command.CombatLogCommand;
import de.nikey.combatLog.Config.PluginConfig;
import de.nikey.combatLog.Listener.*;
import de.nikey.combatLog.Utils.Metrics;
import de.nikey.combatLog.Utils.ModrinthUpdateChecker;
import de.nikey.combatLog.Utils.WorldGuardBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class CombatLog extends JavaPlugin {

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
        ensureMessagesFileExists();

        FileConfiguration messagesConfig = loadMessagesConfig();
        pluginConfig = new PluginConfig(getConfig(), messagesConfig);
        combatManager = new CombatManager(this, pluginConfig);

        registerListeners(pluginConfig);
        registerCommands();

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

    private void registerCommands() {
        PluginCommand command = Objects.requireNonNull(getCommand("combatlog"), "combatlog command missing in plugin.yml");
        CombatLogCommand executor = new CombatLogCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
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

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public void reloadPluginSettings() {
        reloadConfig();
        pluginConfig.reload(getConfig(), loadMessagesConfig());
    }

    private FileConfiguration loadMessagesConfig() {
        ensureMessagesFileExists();
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
    }

    private void ensureMessagesFileExists() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
    }
}
