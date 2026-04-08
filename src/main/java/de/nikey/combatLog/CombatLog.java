package de.nikey.combatLog;

import de.nikey.combatLog.Colorizer.ColorizerProvider;
import de.nikey.combatLog.Command.CombatLogCommand;
import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.MessagesConfig;
import de.nikey.combatLog.Config.PluginConfig;
import de.nikey.combatLog.Listener.*;
import de.nikey.combatLog.Utils.Metrics;
import de.nikey.combatLog.Utils.ModrinthUpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CombatLog extends JavaPlugin {

    private CombatManager combatManager;
    private PluginConfig pluginConfig;
    private MessagesConfig messagesConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        pluginConfig = new PluginConfig(getConfig());
        ColorizerProvider.init(getConfig());
        messagesConfig = loadMessagesConfig();
        combatManager = new CombatManager(this, pluginConfig, messagesConfig);

        registerListeners(pluginConfig, messagesConfig);

        getCommand("combatlog").setExecutor(new CombatLogCommand(this, combatManager, messagesConfig));
        getCommand("combatlog").setTabCompleter(new CombatLogCommand(this, combatManager, messagesConfig));

        new ModrinthUpdateChecker("LI8sodAD").checkForUpdates();
        new Metrics(this, 28071);
    }

    @Override
    public void onDisable() {
        combatManager.shutdown();
    }

    /** Called by reload subcommand to re-register listeners with the new config. */
    public void reloadListeners() {
        ColorizerProvider.init(getConfig());
        FileConfiguration messagesFile = loadMessagesYml();
        messagesConfig = new MessagesConfig(messagesFile);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new CombatTagListener(combatManager, pluginConfig), this);
        pm.registerEvents(new CombatRestrictionListener(combatManager, pluginConfig, messagesConfig), this);
        pm.registerEvents(new CombatZoneListener(this, combatManager, pluginConfig, messagesConfig), this);
        pm.registerEvents(new CombatLogoutListener(combatManager, pluginConfig, messagesConfig), this);
        pm.registerEvents(new AntiKillAbuse(this), this);
    }

    private void registerListeners(PluginConfig config, MessagesConfig messages) {
        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(new CombatTagListener(combatManager, config), this);
        pm.registerEvents(new CombatRestrictionListener(combatManager, config, messages), this);
        pm.registerEvents(new CombatZoneListener(this, combatManager, config, messages), this);
        pm.registerEvents(new CombatLogoutListener(combatManager, config, messages), this);
        pm.registerEvents(new AntiKillAbuse(this), this);
    }

    private FileConfiguration loadMessagesYml() {
        return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(getDataFolder(), "messages.yml"));
    }

    private MessagesConfig loadMessagesConfig() {
        return new MessagesConfig(loadMessagesYml());
    }
}
