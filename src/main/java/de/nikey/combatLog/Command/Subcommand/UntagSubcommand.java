package de.nikey.combatLog.Command.Subcommand;

import de.nikey.combatLog.CombatLog;
import de.nikey.combatLog.Combat.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UntagSubcommand implements Subcommand {

    private final CombatLog plugin;

    public UntagSubcommand(CombatLog plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "untag";
    }

    @Override
    public String usage() {
        return "untag <player>";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPluginConfig().message(
                    "commands.combatlog.usages.untag",
                    "<gray>/{label} untag <player>",
                    Map.of("label", label)
            ));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getPluginConfig().message(
                    "commands.combatlog.errors.player-not-found",
                    "<red>Player not found: <white>{player}",
                    Map.of("player", args[1])
            ));
            return true;
        }

        CombatManager combatManager = plugin.getCombatManager();
        boolean wasInCombat = combatManager.isInCombat(target);
        combatManager.untag(target);

        if (wasInCombat) {
            sender.sendMessage(plugin.getPluginConfig().message(
                    "commands.combatlog.untag.success",
                    "<green>Removed combat tag from <white>{player}",
                    Map.of("player", target.getName())
            ));
        } else {
            sender.sendMessage(plugin.getPluginConfig().message(
                    "commands.combatlog.untag.not-in-combat",
                    "<yellow>Player <white>{player} <yellow>was not in combat.",
                    Map.of("player", target.getName())
            ));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return List.of();
        }

        String input = args[1].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                .toList();
    }
}
