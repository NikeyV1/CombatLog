package de.nikey.combatLog.Command;

import de.nikey.combatLog.CombatLog;
import de.nikey.combatLog.Command.Subcommand.ReloadSubcommand;
import de.nikey.combatLog.Command.Subcommand.StatusSubcommand;
import de.nikey.combatLog.Command.Subcommand.Subcommand;
import de.nikey.combatLog.Command.Subcommand.UntagSubcommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CombatLogCommand implements CommandExecutor, TabCompleter {

    private final CombatLog plugin;
    private final Map<String, Subcommand> subcommands = new LinkedHashMap<>();

    public CombatLogCommand(CombatLog plugin) {
        this.plugin = plugin;
        register(new ReloadSubcommand(plugin));
        register(new StatusSubcommand(plugin));
        register(new UntagSubcommand(plugin));
    }

    private void register(Subcommand subcommand) {
        subcommands.put(subcommand.name(), subcommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("combatlog.admin")) {
            sender.sendMessage(plugin.getPluginConfig().colorize("<red>You do not have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        Subcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null) {
            sendUsage(sender, label);
            return true;
        }

        return subcommand.execute(sender, label, args);
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(plugin.getPluginConfig().colorize("<yellow>Usage:"));
        for (Subcommand subcommand : subcommands.values()) {
            sender.sendMessage(plugin.getPluginConfig().colorize("<gray>/" + label + " " + subcommand.usage()));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("combatlog.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return subcommands.keySet().stream()
                    .filter(name -> name.startsWith(input))
                    .toList();
        }

        Subcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null) {
            return List.of();
        }

        return subcommand.tabComplete(sender, args);
    }
}
