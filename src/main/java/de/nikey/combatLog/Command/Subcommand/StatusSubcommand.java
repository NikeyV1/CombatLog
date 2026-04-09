package de.nikey.combatLog.Command.Subcommand;

import de.nikey.combatLog.CombatLog;
import de.nikey.combatLog.Config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class StatusSubcommand implements Subcommand {

    private final CombatLog plugin;

    public StatusSubcommand(CombatLog plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "status";
    }

    @Override
    public String usage() {
        return "status <player>";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPluginConfig().colorize("<yellow>Usage: /" + label + " status <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getPluginConfig().colorize("<red>Player not found: <white>" + args[1]));
            return true;
        }

        PluginConfig config = plugin.getPluginConfig();
        Integer timeLeft = plugin.getCombatManager().getRemainingCombatSeconds(target).orElse(-1);

        sender.sendMessage(config.colorize("<gold>Combat status for <white>" + target.getName()));
        sender.sendMessage(config.colorize("<gray>World: <white>" + target.getWorld().getName()));
        sender.sendMessage(config.colorize("<gray>Gamemode: <white>" + target.getGameMode().name()));
        sender.sendMessage(config.colorize("<gray>In combat: <white>" + yesNo(plugin.getCombatManager().isInCombat(target))));
        sender.sendMessage(config.colorize("<gray>Time left: <white>" + formatTime(timeLeft)));
        sender.sendMessage(config.colorize("<gray>Ignored world: <white>" + yesNo(config.isIgnoredWorld(target.getWorld().getName()))));
        sender.sendMessage(config.colorize("<gray>Combat zone enabled: <white>" + yesNo(config.combatZoneEnabled())));
        sender.sendMessage(config.colorize("<gray>WorldGuard active: <white>" + yesNo(CombatLog.isWorldGuardEnabled())));
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

    private String formatTime(int timeLeftSeconds) {
        return timeLeftSeconds < 0 ? "n/a" : timeLeftSeconds + "s";
    }

    private String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
