package de.nikey.combatLog.Command;

import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.MessagesConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Central command handler for /combatlog with subcommands.
 */
public class CombatLogCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final CombatManager combat;
    private MessagesConfig messages;

    public CombatLogCommand(JavaPlugin plugin, CombatManager combat, MessagesConfig messages) {
        this.plugin = plugin;
        this.combat = combat;
        this.messages = messages;
    }

    /** Updates the messages config reference (called on reload). */
    public void setMessagesConfig(MessagesConfig messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> reload(sender);
            case "status" -> status(sender, args);
            case "clear" -> clear(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("status");
            subcommands.add("clear");
            if (sender.hasPermission("combatlog.reload")) {
                subcommands.add("reload");
            }
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if ("status".equals(subcommand)) {
                if (sender.hasPermission("combatlog.status")) {
                    String search = args[1].toLowerCase();
                    boolean canCheckOthers = sender.hasPermission("combatlog.status.others");
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!canCheckOthers && sender instanceof Player p && !p.getUniqueId().equals(online.getUniqueId())) {
                            continue;
                        }
                        if (online.getName().toLowerCase().startsWith(search)) {
                            completions.add(online.getName());
                        }
                    }
                }
            } else if ("clear".equals(subcommand)) {
                if (sender.hasPermission("combatlog.clear")) {
                    String search = args[1].toLowerCase();
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online.getName().toLowerCase().startsWith(search)) {
                            completions.add(online.getName());
                        }
                    }
                }
            }
        }
        return completions;
    }

    // ── Subcommands ───────────────────────────────────────────────────────────

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("combatlog.reload")) {
            sender.sendMessage(colorize("commands.no-permission", "&cYou don't have permission to use this command."));
            return;
        }

        de.nikey.combatLog.CombatLog main = (de.nikey.combatLog.CombatLog) plugin;
        main.reloadListeners();

        sender.sendMessage(colorize("commands.reload-success", "&aCombatLog config reloaded successfully!"));
        plugin.getLogger().info(colorize("commands.config-reloaded-by", "Config reloaded by {player}",
                "{player}", sender.getName()));
    }

    private void status(CommandSender sender, String[] args) {
        if (!sender.hasPermission("combatlog.status")) {
            sender.sendMessage(colorize("commands.no-permission", "&cYou don't have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            if (sender instanceof Player player) {
                showStatus(sender, player);
            } else {
                sender.sendMessage(colorize("commands.status-usage", "&cUsage: /combatlog status <player>"));
            }
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null) {
            sender.sendMessage(colorize("commands.player-not-found", "&cPlayer not found: {player}",
                    "{player}", args[1]));
            return;
        }

        // Checking others requires combatlog.status.others
        if (sender instanceof Player player && !player.getUniqueId().equals(target.getUniqueId())) {
            if (!sender.hasPermission("combatlog.status.others")) {
                sender.sendMessage(colorize("commands.status-others-denied", "&cYou don't have permission to check other players' status."));
                return;
            }
        }

        showStatus(sender, target);
    }

    private void showStatus(CommandSender sender, OfflinePlayer target) {
        if (!target.isOnline() || !(target instanceof Player player)) {
            sender.sendMessage(colorize("commands.status.not-online", "&e{player} &fis not currently online.",
                    "{player}", target.getName()));
            return;
        }

        Integer timeLeft = combat.getTimeLeft(player);
        if (timeLeft == null) {
            sender.sendMessage(colorize("commands.status.not-in-combat", "&e{player} &fis §anot in combat§f.",
                    "{player}", player.getName()));
        } else {
            int max = combat.getMaxTime();
            sender.sendMessage(colorize("commands.status.in-combat", "&e{player} &fis §cin combat§f — §e{timeLeft}s §fremaining (max: §e{maxTime}s§f).",
                    "{player}", player.getName(),
                    "{timeLeft}", String.valueOf(timeLeft),
                    "{maxTime}", String.valueOf(max)));
        }
    }

    private void clear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("combatlog.clear")) {
            sender.sendMessage(colorize("commands.no-permission", "&cYou don't have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(colorize("commands.clear-usage", "&cUsage: /combatlog clear <player>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(colorize("commands.player-not-found", "&cPlayer not found: {player}",
                    "{player}", args[1]));
            return;
        }

        if (!combat.isInCombat(target)) {
            sender.sendMessage(colorize("commands.clear.not-in-combat", "&e{player} &fis not in combat.",
                    "{player}", target.getName()));
            return;
        }

        combat.untag(target);
        sender.sendMessage(colorize("commands.clear.removed", "&aCombat tag removed from &e{player}&a.",
                "{player}", target.getName()));
        target.sendMessage(colorize("commands.clear.notified", "&aYour combat tag has been removed by an admin."));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("commands.help.header", "§e§m═══════════════════════════════════"));
        sender.sendMessage(colorize("commands.help.title", "§6⚔ §eCombatLog Commands"));
        sender.sendMessage(colorize("commands.help.header", "§e§m═══════════════════════════════════"));
        sender.sendMessage(colorize("commands.help.status", "§f/combatlog status §7- Check your combat status"));
        if (sender.hasPermission("combatlog.status.others")) {
            sender.sendMessage(colorize("commands.help.status-other", "§f/combatlog status <player> §7- Check a player's status"));
        }
        if (sender.hasPermission("combatlog.clear")) {
            sender.sendMessage(colorize("commands.help.clear", "§f/combatlog clear <player> §7- Remove combat tag from player"));
        }
        if (sender.hasPermission("combatlog.reload")) {
            sender.sendMessage(colorize("commands.help.reload", "§f/combatlog reload §7- Reload configuration"));
        }
        sender.sendMessage(colorize("commands.help.footer", "§e§m═══════════════════════════════════"));
    }

    private String colorize(String path, String def, String... replacements) {
        return ChatColor.translateAlternateColorCodes('&', messages.colorizedMessage(path, def, replacements));
    }
}
