package de.nikey.combatLog.Utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import de.nikey.combatLog.Config.PluginConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Renders client-side fake blocks exactly on the edge of a WorldGuard region
 * to give players a visual indicator they cannot enter it while in combat.
 * Blocks are only sent to the client, the world is never modified.
 *
 * Unlike a one-shot scan on rejected movement, this manager is meant to be
 * driven continuously (every move event while in combat): it diffs the
 * newly-found edge blocks against what's already shown to the player, only
 * sending changes for blocks that were added or removed. This keeps the
 * barrier visible the whole time the player is near the border, not just
 * for the single frame after they get pushed back.
 */
public class SafeZoneBarrierManager {

    private final PluginConfig config;

    /** Currently shown barrier locations per player. */
    private final Map<UUID, Set<Location>> playerBarriers = new HashMap<>();
    /** Original block type at each barrier location, so it can be restored. Ref-counted via barrierViewers. */
    private final Map<Location, Material> originalBlocks = new HashMap<>();
    /** Which players are currently seeing a fake block at this location. */
    private final Map<Location, Set<UUID>> barrierViewers = new HashMap<>();

    public SafeZoneBarrierManager(PluginConfig config) {
        this.config = config;
    }

    /**
     * Recalculates the region edge around the player and updates their
     * barrier blocks accordingly (adds new ones, removes stale ones).
     * Call this on every relevant move while the player is in combat,
     * not only when a move gets rejected.
     *
     * @param player     the player to update barriers for
     * @param deniedFlag the WorldGuard flag used to test whether a location is allowed
     */
    public void updateBarrier(Player player, StateFlag deniedFlag) {
        if (!config.safeZoneBarrierEnabled()) {
            removeBarrier(player);
            return;
        }

        Set<Location> newEdgeBlocks = findRegionEdgeBlocks(player, deniedFlag);
        Set<Location> currentBlocks = playerBarriers.getOrDefault(player.getUniqueId(), Set.of());

        Set<Location> toRemove = new HashSet<>(currentBlocks);
        toRemove.removeAll(newEdgeBlocks);
        for (Location loc : toRemove) {
            removeBarrierBlock(loc, player);
        }

        Set<Location> toAdd = new HashSet<>(newEdgeBlocks);
        toAdd.removeAll(currentBlocks);
        for (Location loc : toAdd) {
            addBarrierBlock(loc, player);
        }

        if (newEdgeBlocks.isEmpty()) {
            playerBarriers.remove(player.getUniqueId());
        } else {
            playerBarriers.put(player.getUniqueId(), newEdgeBlocks);
        }
    }

    /**
     * Immediately removes all fake barrier blocks for this player.
     * Call when combat ends or the player leaves.
     */
    public void removeBarrier(Player player) {
        Set<Location> locations = playerBarriers.remove(player.getUniqueId());
        if (locations == null || locations.isEmpty()) return;

        for (Location loc : locations) {
            removeBarrierBlock(loc, player);
        }
    }

    /** Backwards-compatible alias. */
    public void clearBarrier(Player player) {
        removeBarrier(player);
    }

    /**
     * Clears barriers for all tracked players (e.g. on plugin shutdown).
     */
    public void clearAll() {
        for (UUID uuid : new HashSet<>(playerBarriers.keySet())) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null) removeBarrier(player);
            else playerBarriers.remove(uuid);
        }
    }

    /**
     * Returns {@code true} if the given location is currently a fake barrier
     * block shown to this player. Used to block breaking/interacting with it.
     */
    public boolean isBarrierBlock(Player player, Location location) {
        Set<Location> locations = playerBarriers.get(player.getUniqueId());
        if (locations == null || locations.isEmpty()) return false;
        return locations.contains(roundedCopy(location));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Scans a radius around the player (radius = view distance) and returns
     * every block that is denied by the flag but has at least one horizontal
     * neighbour that is allowed, i.e. sits exactly on the region boundary.
     */
    private Set<Location> findRegionEdgeBlocks(Player player, StateFlag deniedFlag) {
        Set<Location> edgeBlocks = new HashSet<>();

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        Location base = player.getLocation();
        int viewDistance = config.safeZoneBarrierViewDistance();
        int barrierHeight = config.safeZoneBarrierHeight();
        double radiusSquared = (double) viewDistance * viewDistance;

        int cx = base.getBlockX();
        int cy = base.getBlockY();
        int cz = base.getBlockZ();

        for (int x = -viewDistance; x <= viewDistance; x++) {
            for (int z = -viewDistance; z <= viewDistance; z++) {
                if (x * x + z * z > radiusSquared) continue; // circular distance check, cheaper than a full box

                for (int y = -2; y <= barrierHeight; y++) {
                    Location loc = new Location(base.getWorld(), cx + x, cy + y, cz + z);

                    if (!isDenied(query, localPlayer, loc, deniedFlag)) continue;
                    if (!hasAllowedNeighbour(query, localPlayer, loc, deniedFlag)) continue;

                    Block block = loc.getBlock();
                    if (block.getType().isSolid()) continue; // never overwrite real terrain

                    edgeBlocks.add(roundedCopy(loc));
                }
            }
        }
        return edgeBlocks;
    }

    /** {@code true} if entry is NOT allowed at this location (i.e. inside the protected region). */
    private boolean isDenied(RegionQuery query, LocalPlayer localPlayer, Location loc, StateFlag deniedFlag) {
        return !query.testState(BukkitAdapter.adapt(loc), localPlayer, deniedFlag);
    }

    /** Checks the 4 horizontal neighbours (N/S/E/W) for at least one that's outside the denied region. */
    private boolean hasAllowedNeighbour(RegionQuery query, LocalPlayer localPlayer, Location loc, StateFlag deniedFlag) {
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : offsets) {
            Location neighbour = loc.clone().add(offset[0], 0, offset[1]);
            if (!isDenied(query, localPlayer, neighbour, deniedFlag)) {
                return true;
            }
        }
        return false;
    }

    private Location roundedCopy(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private void addBarrierBlock(Location loc, Player player) {
        Location normalized = roundedCopy(loc);
        Material material = config.safeZoneBarrierMaterial();

        originalBlocks.putIfAbsent(normalized, normalized.getBlock().getType());
        barrierViewers.computeIfAbsent(normalized, k -> new HashSet<>()).add(player.getUniqueId());
        player.sendBlockChange(normalized, material.createBlockData());
    }

    private void removeBarrierBlock(Location loc, Player player) {
        Location normalized = roundedCopy(loc);
        Set<UUID> viewers = barrierViewers.get(normalized);
        if (viewers == null) return;

        viewers.remove(player.getUniqueId());

        Material original = originalBlocks.get(normalized);
        if (original != null) {
            player.sendBlockChange(normalized, original.createBlockData());
        }

        if (viewers.isEmpty()) {
            barrierViewers.remove(normalized);
            originalBlocks.remove(normalized);
        }
    }
}