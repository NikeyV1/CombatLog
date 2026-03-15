package de.nikey.combatLog.Listener;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Detects and punishes kill-farming (same player killing the same target
 * more than {@code maxKills} times within a rolling time window).
 */
public class AntiKillAbuse implements Listener {

    /** killerId → victimId → list of kill timestamps */
    private final Map<UUID, Map<UUID, List<Long>>> killHistory = new HashMap<>();

    private final boolean enabled;
    private final int maxKills;
    private final long windowMs;
    private final List<String> punishCommands;

    public AntiKillAbuse(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();

        this.enabled       = config.getBoolean("anti-kill-abuse.enabled", true);
        this.maxKills      = config.getInt("anti-kill-abuse.limits.max-kills", 5);
        this.windowMs      = config.getInt("anti-kill-abuse.limits.remove-kill-after-seconds", 60) * 1000L;
        this.punishCommands = config.getStringList("anti-kill-abuse.punish-commands");

        if (enabled) {
            startCleanupAndCheckTask(plugin);
        }
    }

    // ── Event ─────────────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!enabled) return;
        if (!(event.getEntity().getKiller() instanceof Player killer)) return;

        Player victim = event.getEntity();
        long now = System.currentTimeMillis();

        killHistory
                .computeIfAbsent(killer.getUniqueId(), k -> new HashMap<>())
                .computeIfAbsent(victim.getUniqueId(), v -> new ArrayList<>())
                .add(now);
    }

    // ── Background task ───────────────────────────────────────────────────────

    private void startCleanupAndCheckTask(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (UUID killerId : new HashSet<>(killHistory.keySet())) {
                    Map<UUID, List<Long>> victims = killHistory.get(killerId);
                    if (victims == null) continue;

                    for (Map.Entry<UUID, List<Long>> entry : victims.entrySet()) {
                        List<Long> kills = entry.getValue();
                        if (kills == null) continue;

                        kills.removeIf(t -> now - t > windowMs);

                        if (kills.size() > maxKills) {
                            punish(killerId, entry.getKey());
                            kills.clear();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 25, 20L * 25);
    }

    private void punish(UUID killerId, UUID victimId) {
        String killerName = Optional.ofNullable(Bukkit.getPlayer(killerId))
                .map(Player::getName).orElse("Unknown");
        String victimName = Optional.ofNullable(Bukkit.getPlayer(victimId))
                .map(Player::getName).orElse("Unknown");

        for (String cmd : punishCommands) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    cmd.replace("{killer}", killerName).replace("{victim}", victimName)
            );
        }
    }
}
