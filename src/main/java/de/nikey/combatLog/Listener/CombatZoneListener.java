package de.nikey.combatLog.Listener;

import de.nikey.combatLog.CombatLog;
import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.MessagesConfig;
import de.nikey.combatLog.Config.PluginConfig;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tags players in combat when they are near other players on join.
 * Only active when the combat-zone feature is enabled.
 */
public class CombatZoneListener implements Listener {

    private final CombatLog plugin;
    private final CombatManager combat;
    private final PluginConfig config;
    private final MessagesConfig messages;

    public CombatZoneListener(CombatLog plugin, CombatManager combat, PluginConfig config, MessagesConfig messages) {
        this.plugin = plugin;
        this.combat = combat;
        this.config = config;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.combatZoneEnabled()) return;

        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (isExempt(player)) return;

                double radius = config.combatZoneRadius();

                for (Player nearby : player.getWorld().getNearbyPlayers(player.getLocation(), radius)) {
                    if (nearby == player) continue;
                    if (nearby.isDead()) continue;
                    if (isExempt(nearby)) continue;

                    combat.tagBoth(player, nearby);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private boolean isExempt(Player player) {
        GameMode gm = player.getGameMode();
        return gm == GameMode.SPECTATOR
                || gm == GameMode.CREATIVE
                || config.isIgnoredWorld(player.getWorld().getName());
    }
}
