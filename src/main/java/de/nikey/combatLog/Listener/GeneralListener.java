package de.nikey.combatLog.Listener;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import de.nikey.buffSMP.General.ShowCooldown;
import de.nikey.combatLog.CombatLog;
import de.nikey.trust.Trust;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GeneralListener implements Listener {

    public static final HashMap<UUID, Integer> combatTimers = new HashMap<>();
    public static final HashMap<UUID, BukkitRunnable> activeTimers = new HashMap<>();

    @EventHandler(ignoreCancelled = true,priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        List<String> ignoredWorlds = CombatLog.getPlugin(CombatLog.class).getConfig().getStringList("combat-log.ignored-worlds");
        if (ignoredWorlds.contains(event.getEntity().getWorld().getName())) return;

        if (event.getEntity() instanceof Player damaged && event.getDamager() instanceof Player damager) {
            if (damaged == damager)return;
            cancelCombatTimer(damaged);
            cancelCombatTimer(damager);
            startCombatTimer(damaged);
            startCombatTimer(damager);
            damaged.setGliding(false);
            damager.setGliding(false);
        } else if (event.getDamager() instanceof Arrow damager) {
            ProjectileSource shooter = damager.getShooter();
            if (shooter instanceof Player player && event.getEntity() instanceof Player damaged) {
                if (player == damaged)return;
                cancelCombatTimer(damaged);
                cancelCombatTimer(player);
                startCombatTimer(damaged);
                startCombatTimer(player);
                damaged.setGliding(false);
                player.setGliding(false);
            }
        } else if (event.getDamager() instanceof Trident damager) {
            ProjectileSource shooter = damager.getShooter();
            if (shooter instanceof Player player && event.getEntity() instanceof Player damaged) {
                if (player == damaged)return;
                cancelCombatTimer(damaged);
                cancelCombatTimer(player);
                startCombatTimer(damaged);
                startCombatTimer(player);
                damaged.setGliding(false);
                player.setGliding(false);
            }
        } else if (event.getDamager() instanceof WitherSkull skull) {
            ProjectileSource shooter = skull.getShooter();
            if (shooter instanceof Player player && event.getEntity() instanceof Player damaged) {
                if (player == damaged)return;
                cancelCombatTimer(damaged);
                cancelCombatTimer(player);
                startCombatTimer(damaged);
                startCombatTimer(player);
                damaged.setGliding(false);
                player.setGliding(false);
            }
        }
    }

    private boolean isAfk(Player player) {
        return player.hasMetadata("afk") && player.getMetadata("afk").get(0).asBoolean();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.combat-zone.enabled",false))return;
        List<String> ignoredWorlds = CombatLog.getPlugin(CombatLog.class).getConfig().getStringList("combat-log.ignored-worlds");
        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                double radius = CombatLog.getPlugin(CombatLog.class).getConfig().getDouble("combat-log.combat-zone.radius", 10);

                if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)return;
                for (Player players : player.getWorld().getNearbyPlayers(player.getLocation(), radius)) {
                    if (player == players)continue;
                    if (players.isDead())continue;
                    if (players.getGameMode() == GameMode.SPECTATOR || players.getGameMode() == GameMode.CREATIVE)continue;
                    if (ignoredWorlds.contains(players.getWorld().getName())) return;
                    if (CombatLog.isTrust) {
                        if (Trust.isTrusted(player.getUniqueId(),players.getUniqueId())) {
                            if (Trust.hasFriendlyFire(player.getUniqueId())) {
                                cancelCombatTimer(players);
                                cancelCombatTimer(player);
                                startCombatTimer(player);
                                startCombatTimer(players);
                            }
                        }else {
                            cancelCombatTimer(players);
                            cancelCombatTimer(player);
                            startCombatTimer(player);
                            startCombatTimer(players);
                        }
                    }else {
                        cancelCombatTimer(players);
                        cancelCombatTimer(player);
                        startCombatTimer(player);
                        startCombatTimer(players);
                    }
                }
            }
        }.runTaskTimer(CombatLog.getPlugin(CombatLog.class), 0,20);
    }


    private final Map<UUID, Long> playerCooldowns = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.stop-riptiding",false))return;
        if (!combatTimers.containsKey(player.getUniqueId()))return;
        if (player.isRiptiding()) {
            long currentTime = System.currentTimeMillis();
            long lastThrowTime = playerCooldowns.getOrDefault(player.getUniqueId(), 0L);

            if (currentTime - lastThrowTime >= CombatLog.getPlugin(CombatLog.class).getConfig().getInt("combat-log.cooldown",10000)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        playerCooldowns.put(player.getUniqueId(), currentTime);
                    }
                }.runTaskLater(CombatLog.getPlugin(CombatLog.class),20*2);
            } else {
                event.setCancelled(true);
                double damage = CombatLog.getPlugin(CombatLog.class).getConfig().getDouble("combat-log.punishment-damage", 0);
                player.damage(damage);
            }
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (combatTimers.containsKey(player.getUniqueId())) {
            String combatLogMessage = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.combat-log","&c{player} has combat logged!")
                    .replace("{player}", player.getName());
            TextComponent component = LegacyComponentSerializer.legacyAmpersand().deserialize(combatLogMessage);
            Bukkit.broadcast(component);
            if (CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.kill_when_log",false)) {
                player.setHealth(0);
            }
            cancelCombatTimer(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        boolean teleporting = CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.teleporting-disabled-in-combat");
        if (teleporting && combatTimers.containsKey(player.getUniqueId())) {
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN)return;
            event.setCancelled(true);
            String message = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.teleporting-denied","§dYou can't teleport in combat");
            TextComponent component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            player.sendMessage(component);
            double damage = CombatLog.getPlugin(CombatLog.class).getConfig().getDouble("combat-log.punishment-damage", 0);
            player.damage(damage);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player) {
            boolean elytraDisabled = CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.elytra-disabled-in-combat");
            if (elytraDisabled && combatTimers.containsKey(player.getUniqueId()) && event.isGliding()) {
                player.setGliding(false);
                event.setCancelled(true);
                double damage = CombatLog.getPlugin(CombatLog.class).getConfig().getDouble("combat-log.punishment-damage", 0);
                player.damage(damage);
                String message = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.elytra-use-denied","&dYou can't use elytra in combat");
                TextComponent component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
                player.sendMessage(component);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerElytraBoost(PlayerElytraBoostEvent event) {
        Player player = event.getPlayer();
        if (CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.elytra-disabled-in-combat") && combatTimers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            double damage = CombatLog.getPlugin(CombatLog.class).getConfig().getDouble("combat-log.punishment-damage", 0);
            player.damage(damage);
            String message = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.elytra-use-denied","&dYou can't use elytra in combat");
            TextComponent component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            player.sendMessage(component);
        }
    }


    private void startCombatTimer(Player player) {
        UUID playerId = player.getUniqueId();
        player.setGliding(false);
        int timerDuration = CombatLog.getPlugin(CombatLog.class).getConfig().getInt("combat-log.timer-duration");

        if (combatTimers.containsKey(playerId)) {
            combatTimers.put(playerId, timerDuration);
            return;
        }

        if (isAfk(player)) {
            player.showTitle(Title.title(Component.empty(), Component.text("Please disable afk, you are in combat!").color(NamedTextColor.RED)));
        }

        combatTimers.put(playerId, timerDuration);

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
                    String actionBarMessage = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.action-bar-timer","&c{timeLeft}/{maxTime}")
                            .replace("{timeLeft}", String.valueOf(timeLeft))
                            .replace("{maxTime}",String.valueOf(timerDuration));

                    TextComponent component = LegacyComponentSerializer.legacyAmpersand().deserialize(actionBarMessage);
                    if (CombatLog.isBuffSMP) {
                        if (!ShowCooldown.viewingPlayers.containsKey(playerId)) {
                            player.sendActionBar(component);
                        }
                    }else {
                        player.sendActionBar(component);
                    }
                } else {
                    combatTimers.remove(playerId);
                    activeTimers.remove(playerId);
                    cancel();
                }
            }
        };
        timerTask.runTaskTimer(CombatLog.getPlugin(CombatLog.class), 0, 20);

        activeTimers.put(playerId, timerTask);
    }

    private void cancelCombatTimer(Player player) {
        UUID playerId = player.getUniqueId();
        combatTimers.remove(playerId);

        if (activeTimers.containsKey(playerId)) {
            activeTimers.get(playerId).cancel();
            activeTimers.remove(playerId);
        }
    }
}