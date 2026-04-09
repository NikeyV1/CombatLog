package de.nikey.combatLog.Command.Subcommand;

import de.nikey.combatLog.CombatLog;
import de.nikey.combatLog.Config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            sender.sendMessage(plugin.getPluginConfig().message(
                    "commands.combatlog.usages.status",
                    "<gray>/{label} status <player>",
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

        PluginConfig config = plugin.getPluginConfig();
        Integer timeLeft = plugin.getCombatManager().getRemainingCombatSeconds(target).orElse(-1);
        String yes = config.rawMessage("commands.combatlog.values.yes", "yes");
        String no = config.rawMessage("commands.combatlog.values.no", "no");
        String notApplicable = config.rawMessage("commands.combatlog.values.not-applicable", "n/a");

        sender.sendMessage(config.message(
                "commands.combatlog.status.title",
                "<gold>Combat status for <white>{player}",
                Map.of("player", target.getName())
        ));
        sender.sendMessage(config.message(
                "commands.combatlog.status.world",
                "<gray>World: <white>{world}",
                Map.of("world", target.getWorld().getName())
        ));
        sender.sendMessage(config.message(
                "commands.combatlog.status.gamemode",
                "<gray>Gamemode: <white>{gamemode}",
                Map.of("gamemode", target.getGameMode().name())
        ));
        sender.sendMessage(config.message(
                "commands.combatlog.status.in-combat",
                "<gray>In combat: <white>{value}",
                Map.of("value", yesNo(plugin.getCombatManager().isInCombat(target), yes, no))
        ));
        sender.sendMessage(config.message(
                "commands.combatlog.status.time-left",
                "<gray>Time left: <white>{value}",
                Map.of("value", formatTime(timeLeft, notApplicable))
        ));
        sender.sendMessage(config.message(
                "commands.combatlog.status.ignored-world",
                "<gray>Ignored world: <white>{value}",
                Map.of("value", yesNo(config.isIgnoredWorld(target.getWorld().getName()), yes, no))
        ));
        sender.sendMessage(config.message(
                "commands.combatlog.status.combat-zone-enabled",
                "<gray>Combat zone enabled: <white>{value}",
                Map.of("value", yesNo(config.combatZoneEnabled(), yes, no))
        ));
        sender.sendMessage(config.message(
                "commands.combatlog.status.worldguard-active",
                "<gray>WorldGuard active: <white>{value}",
                Map.of("value", yesNo(CombatLog.isWorldGuardEnabled(), yes, no))
        ));
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

    private String formatTime(int timeLeftSeconds, String notApplicable) {
        return timeLeftSeconds < 0 ? notApplicable : timeLeftSeconds + "s";
    }

    private String yesNo(boolean value, String yes, String no) {
        return value ? yes : no;
    }
}
