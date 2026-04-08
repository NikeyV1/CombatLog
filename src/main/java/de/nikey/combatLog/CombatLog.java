package de.nikey.combatLog;

import de.nikey.combatLog.Command.CombatLogCommand;
import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.PluginConfig;
import de.nikey.combatLog.Listener.*;
import de.nikey.combatLog.Utils.Metrics;
import de.nikey.combatLog.Utils.ModrinthUpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CombatLog extends JavaPlugin {

    private CombatManager combatManager;
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        pluginConfig = new PluginConfig(getConfig());
        combatManager = new CombatManager(this, pluginConfig);

        registerListeners(pluginConfig);

        getCommand("combatlog").setExecutor(new CombatLogCommand(this, combatManager));
        getCommand("combatlog").setTabCompleter(new CombatLogCommand(this, combatManager));

        new ModrinthUpdateChecker("LI8sodAD").checkForUpdates();
        new Metrics(this, 28071);
    }

    @Override
    public void onDisable() {
        combatManager.shutdown();
    }

    /** Called by reload subcommand to re-register listeners with the new config. */
    public void reloadListeners() {
        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(new CombatTagListener(combatManager, pluginConfig), this);
        pm.registerEvents(new CombatRestrictionListener(combatManager, pluginConfig), this);
        pm.registerEvents(new CombatZoneListener(this, combatManager, pluginConfig), this);
        pm.registerEvents(new CombatLogoutListener(combatManager, pluginConfig), this);
        pm.registerEvents(new AntiKillAbuse(this), this);
    }

    private void registerListeners(PluginConfig config) {
        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(new CombatTagListener(combatManager, config), this);
        pm.registerEvents(new CombatRestrictionListener(combatManager, config), this);
        pm.registerEvents(new CombatZoneListener(this, combatManager, config), this);
        pm.registerEvents(new CombatLogoutListener(combatManager, config), this);
        pm.registerEvents(new AntiKillAbuse(this), this);
    }
}
