package de.nikey.combatLog.Command;

import de.nikey.combatLog.Combat.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Central command handler for /combatlog with subcommands.
 */
public class CombatLogCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final CombatManager combat;

    public CombatLogCommand(JavaPlugin plugin, CombatManager combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String[] args) {
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
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
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
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        plugin.reloadConfig();

        // Preserve existing combat state
        List<Player> inCombat = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (combat.isInCombat(p)) {
                inCombat.add(p);
            }
        }

        combat.shutdown();

        // Re-create listeners via the main plugin class
        de.nikey.combatLog.CombatLog main = (de.nikey.combatLog.CombatLog) plugin;
        main.reloadListeners();

        // Restore combat tags
        for (Player p : inCombat) {
            if (p.isOnline()) {
                combat.tag(p);
            }
        }

        sender.sendMessage("§aCombatLog config reloaded successfully!");
        plugin.getLogger().info("Config reloaded by " + sender.getName());
    }

    private void status(CommandSender sender, String[] args) {
        if (!sender.hasPermission("combatlog.status")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            if (sender instanceof Player player) {
                showStatus(sender, player);
            } else {
                sender.sendMessage("§cUsage: /combatlog status <player>");
            }
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }

        // Checking others requires combatlog.status.others
        if (sender instanceof Player player && !player.getUniqueId().equals(target.getUniqueId())) {
            if (!sender.hasPermission("combatlog.status.others")) {
                sender.sendMessage("§cYou don't have permission to check other players' status.");
                return;
            }
        }

        showStatus(sender, target);
    }

    private void showStatus(CommandSender sender, OfflinePlayer target) {
        if (!target.isOnline() || !(target instanceof Player player)) {
            sender.sendMessage("§e" + target.getName() + " §fis not currently online.");
            return;
        }

        Integer timeLeft = combat.getTimeLeft(player);
        if (timeLeft == null) {
            sender.sendMessage("§e" + player.getName() + " §fis§a not in combat§f.");
        } else {
            int max = combat.getMaxTime();
            sender.sendMessage("§e" + player.getName() + " §fis §cin combat§f — §e" + timeLeft + "s§f remaining (max: §e" + max + "s§f).");
        }
    }

    private void clear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("combatlog.clear")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /combatlog clear <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }

        if (!combat.isInCombat(target)) {
            sender.sendMessage("§e" + target.getName() + " §fis not in combat.");
            return;
        }

        combat.untag(target);
        sender.sendMessage("§aCombat tag removed from §e" + target.getName() + "§a.");
        target.sendMessage("§aYour combat tag has been removed by an admin.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e§m═══════════════════════════════════");
        sender.sendMessage("§6⚔ §eCombatLog Commands");
        sender.sendMessage("§e§m═══════════════════════════════════");
        sender.sendMessage("§f/combatlog status §7- Check your combat status");
        if (sender.hasPermission("combatlog.status.others")) {
            sender.sendMessage("§f/combatlog status <player> §7- Check a player's status");
        }
        if (sender.hasPermission("combatlog.clear")) {
            sender.sendMessage("§f/combatlog clear <player> §7- Remove combat tag from player");
        }
        if (sender.hasPermission("combatlog.reload")) {
            sender.sendMessage("§f/combatlog reload §7- Reload configuration");
        }
        sender.sendMessage("§e§m═══════════════════════════════════");
    }
}
