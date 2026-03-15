package de.nikey.combatLog;

import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.PluginConfig;
import de.nikey.combatLog.Listener.*;
import de.nikey.combatLog.Utils.Metrics;
import de.nikey.combatLog.Utils.ModrinthUpdateChecker;
import de.nikey.combatLog.Utils.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CombatLog extends JavaPlugin {

    private CombatManager combatManager;

    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuardHook.tryRegisterFlag(this);
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void registerListeners(PluginConfig config) {
        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(new CombatTagListener(combatManager, config), this);
        pm.registerEvents(new CombatRestrictionListener(combatManager, config), this);
        pm.registerEvents(new CombatZoneListener(this, combatManager, config), this);
        pm.registerEvents(new CombatLogoutListener(combatManager, config), this);
        pm.registerEvents(new AntiKillAbuse(this), this);

        if (WorldGuardHook.isWorldGuardEnabled()) {
            pm.registerEvents(new WorldGuardListener(combatManager, config), this);
        }
    }

    public static boolean isWorldGuardEnabled() {
        return WorldGuardHook.isWorldGuardEnabled();
    }
}