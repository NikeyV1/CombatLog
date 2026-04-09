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
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class CombatLog extends JavaPlugin {

    private CombatManager combatManager;
    private PluginConfig pluginConfig;
    private MessagesConfig messagesConfig;
    private CombatLogCommand combatLogCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        pluginConfig = new PluginConfig(getConfig());
        ColorizerProvider.init(getConfig());
        messagesConfig = loadMessagesConfig();
        combatManager = new CombatManager(this, pluginConfig, messagesConfig);

        registerListeners(pluginConfig, messagesConfig);

        combatLogCommand = new CombatLogCommand(this, combatManager, messagesConfig);
        getCommand("combatlog").setExecutor(combatLogCommand);
        getCommand("combatlog").setTabCompleter(combatLogCommand);

        new ModrinthUpdateChecker("LI8sodAD").checkForUpdates();
        new Metrics(this, 28071);
    }

    @Override
    public void onDisable() {
        combatManager.shutdown();
    }

    /** Called by reload subcommand to re-register listeners with the new config. */
    public void reloadListeners() {
        pluginConfig = new PluginConfig(getConfig());
        ColorizerProvider.init(getConfig());
        FileConfiguration messagesFile = loadMessagesYml();
        messagesConfig = new MessagesConfig(messagesFile);

        // Preserve existing combat state before shutdown
        java.util.List<Player> inCombat = new java.util.ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (combatManager.isInCombat(p)) {
                inCombat.add(p);
            }
        }

        combatManager.shutdown();
        combatManager = new CombatManager(this, pluginConfig, messagesConfig);

        // Update command with new messages config
        combatLogCommand.setMessagesConfig(messagesConfig);

        // Restore combat tags
        for (Player p : inCombat) {
            if (p.isOnline()) {
                combatManager.tag(p);
            }
        }

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
        File file = new File(getDataFolder(), "messages.yml");
        YamlConfiguration config = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            config.load(reader);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            getLogger().warning("Failed to load messages.yml: " + e.getMessage());
        }
        return config;
    }

    private MessagesConfig loadMessagesConfig() {
        return new MessagesConfig(loadMessagesYml());
    }
}
