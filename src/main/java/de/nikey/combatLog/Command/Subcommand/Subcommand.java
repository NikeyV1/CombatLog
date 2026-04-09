package de.nikey.combatLog.Command.Subcommand;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface Subcommand {
    String name();

    String usage();

    boolean execute(CommandSender sender, String label, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
