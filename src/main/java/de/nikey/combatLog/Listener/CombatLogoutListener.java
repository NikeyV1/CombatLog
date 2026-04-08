package de.nikey.combatLog.Listener;

import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.MessagesConfig;
import de.nikey.combatLog.Config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles players disconnecting while in combat.
 * Broadcasts a message and optionally kills the player.
 */
public class CombatLogoutListener implements Listener {

    private final CombatManager combat;
    private final PluginConfig config;
    private final MessagesConfig messages;

    public CombatLogoutListener(CombatManager combat, PluginConfig config, MessagesConfig messages) {
        this.combat = combat;
        this.config = config;
        this.messages = messages;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!combat.isInCombat(player)) return;

        String message = messages.colorizedMessage("combat-log.combat-log", "&c{player} has combat logged!",
                "{player}", player.getName());

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));

        if (config.killOnLogout()) {
            player.setHealth(0);
        }

        combat.untag(player);
    }
}
