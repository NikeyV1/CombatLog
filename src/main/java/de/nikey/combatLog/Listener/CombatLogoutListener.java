package de.nikey.combatLog.Listener;

import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles players disconnecting while in combat.
 * Broadcasts a message and applies the configured logout punishment.
 */
public class CombatLogoutListener implements Listener {

    private final CombatManager combat;
    private final PluginConfig config;

    public CombatLogoutListener(CombatManager combat, PluginConfig config) {
        this.combat = combat;
        this.config = config;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!combat.isInCombat(player)) return;

        String message = config.rawMessage("combat-log.messages.combat-log", "&c{player} has combat logged!")
                .replace("{player}", player.getName());

        Bukkit.broadcast(config.colorize(message));

        switch (config.logoutPunishmentMode()) {
            case KILL -> player.setHealth(0);
            case COMMANDS -> config.logoutPunishmentCommands().forEach(cmd -> runCommand(cmd, player));
            case NONE -> {}
        }

        combat.untag(player);
    }

    private void runCommand(String command, Player player) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CombatLog] Logout punishment command failed: " + command + " (" + e.getMessage() + ")");
        }
    }
}