package de.nikey.combatLog;

import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.PluginConfig;
import de.nikey.combatLog.Listener.*;
import de.nikey.combatLog.Utils.CombatPlaceholders;
import de.nikey.combatLog.Utils.Metrics;
import de.nikey.combatLog.Utils.ModrinthUpdateChecker;
import de.nikey.combatLog.Utils.WorldGuardBridge;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CombatLog extends JavaPlugin {

    private CombatManager combatManager;
    private WorldGuardBridge worldGuardBridge = null;

    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            initWorldGuard();
        }
    }

    private void initWorldGuard() {
        de.nikey.combatLog.Utils.WorldGuardHook hook =
                new de.nikey.combatLog.Utils.WorldGuardHook(this);
        hook.register();
        worldGuardBridge = hook;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginConfig pluginConfig = new PluginConfig(getConfig());
        combatManager = new CombatManager(this, pluginConfig);

        registerListeners(pluginConfig);
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

        pm.registerEvents(new CombatTagListener(combatManager, config), this);
        pm.registerEvents(new CombatRestrictionListener(combatManager, config), this);
        pm.registerEvents(new CombatZoneListener(this, combatManager, config), this);
        pm.registerEvents(new CombatLogoutListener(combatManager, config), this);
        pm.registerEvents(new AntiKillAbuse(this), this);

        if (worldGuardBridge != null && worldGuardBridge.isEnabled()) {
            registerWorldGuardListener(pm, config);
        }
    }

    private void registerWorldGuardListener(PluginManager pm, PluginConfig config) {
        pm.registerEvents(
                new de.nikey.combatLog.Listener.WorldGuardListener(combatManager, config), this);
    }

    /**
     * Registers the PlaceholderAPI expansion only if PAPI is present.
     * Isolated into its own method so CombatPlaceholders (and PlaceholderExpansion)
     * are never loaded when PAPI is absent — same pattern as WorldGuard.
     */
    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return;
        initPlaceholders();
    }

    private void initPlaceholders() {
        new CombatPlaceholders(combatManager).register();
        getLogger().info("PlaceholderAPI detected: placeholders registered.");
    }

    public static boolean isWorldGuardEnabled() {
        CombatLog instance = getPlugin(CombatLog.class);
        return instance.worldGuardBridge != null && instance.worldGuardBridge.isEnabled();
    }
}