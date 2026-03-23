package de.nikey.combatLog.Listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.PluginConfig;
import de.nikey.combatLog.Utils.WorldGuardHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Prevents players from entering WorldGuard regions that have
 * the {@code allow-combat-entry} flag set to DENY while in combat.
 *
 * Only registered when WorldGuard is present.
 */
public class WorldGuardListener implements Listener {

    private final CombatManager combat;
    private final PluginConfig config;

    public WorldGuardListener(CombatManager combat, PluginConfig config) {
        this.combat = combat;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMoveIntoRegion(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!combat.isInCombat(player)) return;
        if (!hasMovedBlock(event)) return;

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        if (query.testState(BukkitAdapter.adapt(event.getTo()), localPlayer, WorldGuardHook.ALLOW_COMBAT_ENTRY)) {
            return; // entry is allowed
        }

        event.setCancelled(true);
        player.teleport(event.getFrom());
        player.sendMessage(config.message("combat-log.messages.region-entry-denied", "&cYou can't enter this region in combat"));
    }

    /** Returns {@code true} only when the player has actually crossed a block boundary. */
    private boolean hasMovedBlock(PlayerMoveEvent event) {
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }
}
