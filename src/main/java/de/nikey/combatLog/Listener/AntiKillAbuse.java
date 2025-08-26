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

    private final int maxKills;
    private final int killRemove;
    private final List<String> punishCommands;

    public AntiKillAbuse(JavaPlugin plugin) {
        this.plugin = plugin;

        FileConfiguration config = plugin.getConfig();
        this.maxKills = config.getInt("anti-kill-abuse.max-kills", 3);
        this.punishCommands = config.getStringList("anti-kill-abuse.punish-commands");


        this.killRemove = config.getInt("anti-kill-abuse.remove-kill-after",60);
        if (config.getBoolean("anti-kill-abuse.enabled", true)) {
            startCheckTask();
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
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
                long window = killRemove * 1000L;

                for (UUID killerId : new HashSet<>(killHistory.keySet())) {
                    Map<UUID, List<Long>> victimKills = killHistory.get(killerId);
                    if (victimKills == null) continue;

                    for (Map.Entry<UUID, List<Long>> entry : victimKills.entrySet()) {
                        List<Long> kills = entry.getValue();

                        kills.removeIf(time -> now - time > window);

                        if (kills.size() > maxKills) {
                            Player killer = Bukkit.getPlayer(killerId);
                            if (killer != null && killer.isOnline()) {
                                for (String cmd : punishCommands) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                            cmd.replace("{killer}", killer.getName())
                                                    .replace("{victim}", Optional.ofNullable(Bukkit.getPlayer(entry.getKey()))
                                                            .map(Player::getName).orElse("Unknown")));
                                }
                            }
                            kills.clear();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 25,20L * 25);
    }
}
