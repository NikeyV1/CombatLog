package de.nikey.combatLog.Command.Subcommand;

import de.nikey.combatLog.CombatLog;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.logging.Level;

public class ReloadSubcommand implements Subcommand {

    private final CombatLog plugin;

    public ReloadSubcommand(CombatLog plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String usage() {
        return "reload";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        try {
            plugin.reloadPluginSettings();
            sender.sendMessage(plugin.getPluginConfig().message(
                    "commands.combatlog.reload.success",
                    "<green>CombatLog config and messages reloaded."
            ));
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload CombatLog configuration.", exception);
            sender.sendMessage(plugin.getPluginConfig().message(
                    "commands.combatlog.reload.failed",
                    "<red>Failed to reload CombatLog. Check console for details."
            ));
        }
        return true;
    }
}
