package de.nikey.combatLog.Listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.PluginConfig;
import de.nikey.combatLog.Utils.SafeZoneBarrierManager;
import de.nikey.combatLog.Utils.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Prevents players from entering WorldGuard regions that have
 * the {@code allow-combat-entry} flag set to DENY while in combat.
 * Entry is blocked on movement, teleports and block placement at the border,
 * and a periodic check pushes out anyone who still ends up inside.
 *
 * Only registered when WorldGuard is present.
 */
public class WorldGuardListener implements Listener {

    private static final long BARRIER_UPDATE_INTERVAL_MS = 250;
    private static final long ENFORCEMENT_INTERVAL_TICKS = 10L;

    private final CombatManager combat;
    private final PluginConfig config;
    private final SafeZoneBarrierManager barrierManager;

    private final Map<UUID, Long> lastBarrierUpdate = new HashMap<>();
    private final Map<UUID, Location> lastAllowedLocation = new HashMap<>();

    public WorldGuardListener(CombatManager combat, PluginConfig config, SafeZoneBarrierManager barrierManager) {
        this.combat = combat;
        this.config = config;
        this.barrierManager = barrierManager;
    }

    public void startEnforcementTask(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!combat.isInCombat(player)) {
                    lastAllowedLocation.remove(player.getUniqueId());
                    continue;
                }
                if (isAllowed(player, player.getLocation())) continue;

                Location target = lastAllowedLocation.get(player.getUniqueId());
                if (target == null || target.getWorld() != player.getWorld()) continue;

                pushBack(player, target);
            }
        }, ENFORCEMENT_INTERVAL_TICKS, ENFORCEMENT_INTERVAL_TICKS);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!combat.isInCombat(player)) {
            barrierManager.removeBarrier(player);
            lastAllowedLocation.remove(player.getUniqueId());
            return;
        }
        if (!hasMovedBlock(event)) return;

        if (isAllowed(player, event.getTo())) {
            lastAllowedLocation.put(player.getUniqueId(), event.getTo().clone());
        } else {
            event.setCancelled(true);
            if (isAllowed(player, event.getFrom())) {
                pushBack(player, event.getFrom());
            }
        }

        updateBarrierThrottled(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!combat.isInCombat(player)) return;
        if (isAllowed(player, event.getTo())) return;
        if (!isAllowed(player, event.getFrom())) return;

        event.setCancelled(true);
        player.sendMessage(config.message("combat-log.messages.region-entry-denied", "&cYou can't enter this region in combat"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!combat.isInCombat(player)) return;

        Location placed = event.getBlockPlaced().getLocation();
        boolean touchesBarrier = barrierManager.isBarrierBlock(player, placed)
                || barrierManager.isBarrierBlock(player, event.getBlockAgainst().getLocation());

        if (touchesBarrier || !isAllowed(player, placed)) {
            event.setCancelled(true);
            barrierManager.resendBarrier(player);
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
        UUID id = event.getPlayer().getUniqueId();
        barrierManager.removeBarrier(event.getPlayer());
        lastBarrierUpdate.remove(id);
        lastAllowedLocation.remove(id);
    }

    private void pushBack(Player player, Location target) {
        Location destination = target.clone();
        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());
        player.teleport(destination, PlayerTeleportEvent.TeleportCause.UNKNOWN);
        player.sendMessage(config.message("combat-log.messages.region-entry-denied", "&cYou can't enter this region in combat"));
    }

    private boolean isAllowed(Player player, Location location) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        return query.testState(BukkitAdapter.adapt(location), localPlayer, WorldGuardHook.ALLOW_COMBAT_ENTRY);
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

    private boolean hasMovedBlock(PlayerMoveEvent event) {
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }
}