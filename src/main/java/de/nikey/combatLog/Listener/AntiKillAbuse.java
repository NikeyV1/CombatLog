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

public class AntiKillAbuse implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Map<UUID, List<Long>>> killHistory = new HashMap<>();

    private final boolean enabled;
    private final int maxKills;
    private final int removeKillAfterSeconds;
    private final List<String> punishCommands;

    public AntiKillAbuse(JavaPlugin plugin) {
        this.plugin = plugin;

        FileConfiguration config = plugin.getConfig();

        this.enabled = config.getBoolean("anti-kill-abuse.enabled", true);

        this.maxKills = config.getInt("anti-kill-abuse.limits.max-kills", 5);
        this.removeKillAfterSeconds = config.getInt("anti-kill-abuse.limits.remove-kill-after-seconds", 60);
        this.punishCommands = config.getStringList("anti-kill-abuse.punish-commands");

        if (enabled) {
            startCheckTask();
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

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

    private void startCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long windowMs = removeKillAfterSeconds * 1000L;

                for (UUID killerId : new HashSet<>(killHistory.keySet())) {
                    Map<UUID, List<Long>> victimKills = killHistory.get(killerId);
                    if (victimKills == null) continue;

                    for (Map.Entry<UUID, List<Long>> entry : victimKills.entrySet()) {
                        UUID victimId = entry.getKey();
                        List<Long> kills = entry.getValue();
                        if (kills == null) continue;

                        kills.removeIf(t -> now - t > windowMs);

                        if (kills.size() > maxKills) {
                            Player killer = Bukkit.getPlayer(killerId);

                            String killerName = (killer != null) ? killer.getName() : "Unknown";
                            String victimName = Optional.ofNullable(Bukkit.getPlayer(victimId))
                                    .map(Player::getName)
                                    .orElse("Unknown");

                            for (String cmd : punishCommands) {
                                Bukkit.dispatchCommand(
                                        Bukkit.getConsoleSender(),
                                        cmd.replace("{killer}", killerName).replace("{victim}", victimName)
                                );
                            }

                            kills.clear();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 25, 20L * 25);
    }
}
