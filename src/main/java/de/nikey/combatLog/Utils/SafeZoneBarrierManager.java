package de.nikey.combatLog.Utils;

import de.nikey.combatLog.Config.PluginConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Renders client-side fake blocks at WorldGuard safe-zone boundaries
 * to give players a visual indicator they cannot enter during combat.
 * Blocks are only sent to the client – the world is never modified.
 */
public class SafeZoneBarrierManager {

    private final PluginConfig config;

    /** Tracks which fake block locations were sent to each player so we can clean them up. */
    private final Map<UUID, Set<Location>> sentBlocks = new HashMap<>();

    public SafeZoneBarrierManager(PluginConfig config) {
        this.config = config;
    }

    /**
     * Shows fake barrier blocks around the player's current position
     * along the boundary of the safe zone they are trying to enter.
     *
     * @param player   the player to show blocks to
     * @param boundary the location on the region boundary that was blocked
     */
    public void showBarrier(Player player, Location boundary) {
        if (!config.safeZoneBarrierEnabled()) return;

        Material material = config.safeZoneBarrierMaterial();
        BlockData blockData = material.createBlockData();
        int radius = config.safeZoneBarrierRadius();

        Set<Location> locations = collectBarrierLocations(boundary, radius);
        sendFakeBlocks(player, locations, blockData);

        sentBlocks.merge(player.getUniqueId(), locations, (existing, newSet) -> {
            existing.addAll(newSet);
            return existing;
        });
    }

    /**
     * Immediately removes all fake barrier blocks for this player.
     * Call when combat ends or the player leaves.
     */
    public void clearBarrier(Player player) {
        Set<Location> locations = sentBlocks.remove(player.getUniqueId());
        if (locations == null || locations.isEmpty()) return;

        for (Location loc : locations) {
            // Restore the real block that's actually there
            player.sendBlockChange(loc, loc.getBlock().getBlockData());
        }
    }

    /**
     * Clears barriers for all tracked players (e.g. on plugin shutdown).
     */
    public void clearAll() {
        for (UUID uuid : new HashSet<>(sentBlocks.keySet())) {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null) clearBarrier(player);
            else sentBlocks.remove(uuid);
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private Set<Location> collectBarrierLocations(Location center, int radius) {
        Set<Location> locations = new HashSet<>();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {   // player height + one above/below
                for (int z = -radius; z <= radius; z++) {
                    Location loc = new Location(center.getWorld(), cx + x, cy + y, cz + z);
                    // Only replace air/passable blocks so we don't overlay solid terrain
                    if (loc.getBlock().isPassable()) {
                        locations.add(loc);
                    }
                }
            }
        }
        return locations;
    }

    private void sendFakeBlocks(Player player, Set<Location> locations, BlockData blockData) {
        for (Location loc : locations) {
            player.sendBlockChange(loc, blockData);
        }
    }
}