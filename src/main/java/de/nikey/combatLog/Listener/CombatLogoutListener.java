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
 * Broadcasts a message and optionally kills the player.
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

        if (config.killOnLogout()) {
            player.setHealth(0);
        }

        combat.untag(player);
    }
}
