package de.nikey.combatLog;

import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.PluginConfig;
import de.nikey.combatLog.Listener.*;
import de.nikey.combatLog.Utils.Metrics;
import de.nikey.combatLog.Utils.ModrinthUpdateChecker;
import de.nikey.combatLog.Utils.WorldGuardBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;

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

        pluginConfig = new PluginConfig(getConfig());
        combatManager = new CombatManager(this, pluginConfig);

        registerListeners(pluginConfig);

        new ModrinthUpdateChecker("LI8sodAD").checkForUpdates();
        new Metrics(this, 28071);
    }

    @Override
    public void onDisable() {
        combatManager.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("combatlog.reload")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            reloadConfig();
            pluginConfig = new PluginConfig(getConfig());

            // Preserve existing combat state
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            ArrayList<Player> inCombat = new ArrayList<>();
            for (Player p : onlinePlayers) {
                if (combatManager.isInCombat(p)) {
                    inCombat.add(p);
                }
            }

            combatManager.shutdown();
            combatManager = new CombatManager(this, pluginConfig);

            // Re-register listeners with new config
            HandlerList.unregisterAll(this);
            registerListeners(pluginConfig);

            // Restore combat tags for players who were in combat
            for (Player player : inCombat) {
                if (player.isOnline()) {
                    combatManager.tag(player);
                }
            }

            sender.sendMessage("§aCombatLog config reloaded successfully!");
            getLogger().info("Config reloaded by " + sender.getName());
            return true;
        }
        return false;
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
}