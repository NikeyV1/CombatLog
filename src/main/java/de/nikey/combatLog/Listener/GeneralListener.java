package de.nikey.combatLog.Listener;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import de.nikey.combatLog.CombatLog;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class GeneralListener implements Listener {

    public static final HashMap<UUID, Integer> combatTimers = new HashMap<>();
    public static final HashMap<UUID, BukkitRunnable> activeTimers = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Load the list of ignored worlds from the config
        List<String> ignoredWorlds = CombatLog.getPlugin(CombatLog.class).getConfig().getStringList("combat-log.ignored-worlds");

        // Check if the event occurs in one of the ignored worlds
        if (ignoredWorlds.contains(event.getEntity().getWorld().getName())) return;

        if (event.getEntity() instanceof Player damaged && event.getDamager() instanceof Player damager) {
            cancelCombatTimer(damaged);
            cancelCombatTimer(damager);
            startCombatTimer(damaged);
            startCombatTimer(damager);
        } else if (event.getDamager() instanceof Arrow damager) {
            ProjectileSource shooter = damager.getShooter();
            if (shooter instanceof Player player && event.getEntity() instanceof Player damaged) {
                cancelCombatTimer(damaged);
                cancelCombatTimer(player);
                startCombatTimer(damaged);
                startCombatTimer(player);
            }
        } else if (event.getDamager() instanceof Trident damager) {
            ProjectileSource shooter = damager.getShooter();
            if (shooter instanceof Player player && event.getEntity() instanceof Player damaged) {
                cancelCombatTimer(damaged);
                cancelCombatTimer(player);
                startCombatTimer(damaged);
                startCombatTimer(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Broadcast a combat log message if the player leaves while in combat
        if (combatTimers.containsKey(player.getUniqueId())) {
            String combatLogMessage = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.combat-log")
                    .replace("{player}", player.getName());
            String codes = ChatColor.translateAlternateColorCodes('&', combatLogMessage);
            Bukkit.broadcast(Component.text(codes));
            cancelCombatTimer(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player) {
            boolean elytraDisabled = CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.elytra-disabled-in-combat");

            // Prevent the player from using Elytra while in combat
            if (elytraDisabled && combatTimers.containsKey(player.getUniqueId()) && event.isGliding()) {
                player.setGliding(false);
                event.setCancelled(true);
                String message = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.elytra-use-denied");
                String codes = ChatColor.translateAlternateColorCodes('&', message);
                player.sendMessage(Component.text(codes));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerElytraBoost(PlayerElytraBoostEvent event) {
        Player player = event.getPlayer();

        if (CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.elytra-disabled-in-combat") && combatTimers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String message = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.elytra-use-denied");
            String codes = ChatColor.translateAlternateColorCodes('&', message);
            player.sendMessage(Component.text(codes));
        }
    }


    private void startCombatTimer(Player player) {
        UUID playerId = player.getUniqueId();
        int timerDuration = CombatLog.getPlugin(CombatLog.class).getConfig().getInt("combat-log.timer-duration");

        // Check if the player is already in combat
        if (combatTimers.containsKey(playerId)) {
            combatTimers.put(playerId, timerDuration); // Just reset the timer count
            return;
        }

        // Set initial timer value
        combatTimers.put(playerId, timerDuration);

        // Create and start a new timer task
        BukkitRunnable timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isValid()){
                    combatTimers.remove(playerId);
                    activeTimers.remove(playerId);
                    cancel();
                }

                if (combatTimers.get(playerId) == null) {
                    combatTimers.remove(playerId);
                    activeTimers.remove(playerId);
                    cancel();
                    return;
                }

                int timeLeft = combatTimers.get(playerId);
                if (timeLeft > 0) {
                    combatTimers.put(playerId, timeLeft - 1);
                    String actionBarMessage = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.action-bar-timer")
                            .replace("{timeLeft}", String.valueOf(timeLeft))
                            .replace("{maxTime}",String.valueOf(timerDuration));

                    String codes = ChatColor.translateAlternateColorCodes('&', actionBarMessage);
                    player.sendActionBar(Component.text(codes));
                } else {
                    combatTimers.remove(playerId);
                    activeTimers.remove(playerId);
                    cancel(); // Stop the task once the timer reaches 0
                }
            }
        };
        timerTask.runTaskTimer(CombatLog.getPlugin(CombatLog.class), 0, 20); // Runs every second (20 ticks)

        // Store the active timer task so it can be canceled later
        activeTimers.put(playerId, timerTask);
    }

    private void cancelCombatTimer(Player player) {
        UUID playerId = player.getUniqueId();
        combatTimers.remove(playerId);

        // Cancel the player's active timer, if it exists
        if (activeTimers.containsKey(playerId)) {
            activeTimers.get(playerId).cancel();
            activeTimers.remove(playerId);
        }
    }
}
