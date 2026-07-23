package de.nikey.combatLog.Listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.PluginConfig;
import de.nikey.combatLog.Utils.SafeZoneBarrierManager;
import de.nikey.combatLog.Utils.WorldGuardHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Prevents players from entering WorldGuard regions that have
 * the {@code allow-combat-entry} flag set to DENY while in combat.
 * Continuously updates a visual barrier as the player moves near the
 * region edge, rather than only showing it for a moment after a
 * rejected move.
 *
 * Only registered when WorldGuard is present.
 */
public class WorldGuardListener implements Listener {

    /** Only recompute barrier blocks this often per player, to avoid a WorldGuard query storm. */
    private static final long BARRIER_UPDATE_INTERVAL_MS = 250;

    private final CombatManager combat;
    private final PluginConfig config;
    private final SafeZoneBarrierManager barrierManager;

    private final Map<UUID, Long> lastBarrierUpdate = new HashMap<>();

    public WorldGuardListener(CombatManager combat, PluginConfig config, SafeZoneBarrierManager barrierManager) {
        this.combat = combat;
        this.config = config;
        this.barrierManager = barrierManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!combat.isInCombat(player)) {
            barrierManager.removeBarrier(player);
            return;
        }
        if (!hasMovedBlock(event)) return;

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        boolean entryAllowed = query.testState(BukkitAdapter.adapt(event.getTo()), localPlayer, WorldGuardHook.ALLOW_COMBAT_ENTRY);

        if (!entryAllowed) {
            event.setCancelled(true);
            player.teleport(event.getFrom());
            player.sendMessage(config.message("combat-log.messages.region-entry-denied", "&cYou can't enter this region in combat"));
        }

        updateBarrierThrottled(player);
    }

    private void updateBarrierThrottled(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastBarrierUpdate.get(id);

        if (last == null || now - last >= BARRIER_UPDATE_INTERVAL_MS) {
            barrierManager.updateBarrier(player, WorldGuardHook.ALLOW_COMBAT_ENTRY);
            lastBarrierUpdate.put(id, now);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBarrierBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (barrierManager.isBarrierBlock(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBarrierBlockInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        if (barrierManager.isBarrierBlock(player, event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        barrierManager.removeBarrier(event.getPlayer());
        lastBarrierUpdate.remove(event.getPlayer().getUniqueId());
    }

    /** Returns {@code true} only when the player has actually crossed a block boundary. */
    private boolean hasMovedBlock(PlayerMoveEvent event) {
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }
}