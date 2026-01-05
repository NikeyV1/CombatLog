package de.nikey.combatLog.Listener;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import de.nikey.combatLog.CombatLog;
import de.nikey.combatLog.Utils.WorldGuardHook;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GeneralListener implements Listener {

    public static final HashMap<UUID, Integer> combatTimers = new HashMap<>();
    public static final HashMap<UUID, BukkitRunnable> activeTimers = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

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
        }else if (event.getDamager() instanceof EnderCrystal) {
            if (!CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.explosion-set-in-combat",true))return;
            if (event.getEntity() instanceof Player damaged) {
                cancelCombatTimer(damaged);
                startCombatTimer(damaged);
                damaged.setGliding(false);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDamageByBlock(EntityDamageByBlockEvent event) {
        if (!CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.explosion-set-in-combat",true))return;
        List<String> ignoredWorlds = CombatLog.getPlugin(CombatLog.class).getConfig().getStringList("combat-log.ignored-worlds");
        if (ignoredWorlds.contains(event.getEntity().getWorld().getName())) return;

        if (event.getEntity() instanceof Player damaged) {
            if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                cancelCombatTimer(damaged);
                startCombatTimer(damaged);
                damaged.setGliding(false);
            } else if (event.getDamager() != null && event.getDamager().getType() == Material.RESPAWN_ANCHOR) {
                cancelCombatTimer(damaged);
                startCombatTimer(damaged);
                damaged.setGliding(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMoveIntoRegion(PlayerMoveEvent event) {
        if (!CombatLog.isWorldGuardEnabled()) return;
        Player player = event.getPlayer();

        if (!combatTimers.containsKey(player.getUniqueId())) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY()) return;

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        if (!query.testState(BukkitAdapter.adapt(event.getTo()), localPlayer, WorldGuardHook.ALLOW_COMBAT_ENTRY)) {
            event.setCancelled(true);
            player.teleport(event.getFrom()); // Sicherer Rückstoß

            String message = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.region-entry-denied", "§cDu darfst diese Region im Kampf nicht betreten!");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
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
                    cancelCombatTimer(players);
                    cancelCombatTimer(player);
                    startCombatTimer(player);
                    startCombatTimer(players);
                }
            }
        }.runTaskTimer(CombatLog.getPlugin(CombatLog.class), 0,20);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEnderPearlLand(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;

        List<String> ignoredWorlds = CombatLog.getPlugin(CombatLog.class)
                .getConfig().getStringList("combat-log.ignored-worlds");
        if (ignoredWorlds.contains(player.getWorld().getName())) return;

        boolean enabled = CombatLog.getPlugin(CombatLog.class)
                .getConfig().getBoolean("combat-log.enderpearl.set-combat-on-land", true);
        if (!enabled) return;

        boolean onlyIfAlreadyInCombat = CombatLog.getPlugin(CombatLog.class)
                .getConfig().getBoolean("combat-log.enderpearl.only-if-already-in-combat", false);

        boolean isInCombat = combatTimers.containsKey(player.getUniqueId());

        if (onlyIfAlreadyInCombat && !isInCombat) return;

        cancelCombatTimer(player);
        startCombatTimer(player);
        player.setGliding(false);
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
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combatTimers.containsKey(player.getUniqueId()))return;
        final String[] args = event.getMessage().split(" ");
        List<String> stringList = CombatLog.getPlugin(CombatLog.class).getConfig().getStringList("combat-log.blocked-commands");
        String message = args[0].substring(1);

        if (stringList.contains(message)) {
            event.setCancelled(true);
            player.sendMessage(CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.blocked-command","§cYou can't use this command in combat"));
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

    @EventHandler
    public void onMend(PlayerItemMendEvent event) {
        if (!CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.mending-disabled-in-combat",false)) return;

        Player player = event.getPlayer();

        if (combatTimers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }


    private void startCombatTimer(Player player) {
        UUID playerId = player.getUniqueId();

        boolean elytraDisabled = CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.elytra-disabled-in-combat");
        if (elytraDisabled) {
            player.setGliding(false);
        }
        int timerDuration = CombatLog.getPlugin(CombatLog.class).getConfig().getInt("combat-log.timer-duration");
        String displayType = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.timer-display", "actionbar").toLowerCase();

        if (combatTimers.containsKey(playerId)) {
            combatTimers.put(playerId, timerDuration);
            return;
        }

        if (isAfk(player)) {
            player.showTitle(Title.title(Component.empty(), Component.text("Please disable afk, you are in combat!").color(NamedTextColor.RED)));
        }

        combatTimers.put(playerId, timerDuration);

        if (displayType.equals("bossbar")) {
            BossBar bossBar = BossBar.bossBar(
                    Component.text(""), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS
            );
            bossBar.addViewer(player);
            bossBars.put(playerId, bossBar);
        }

        BukkitRunnable timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isValid()) {
                    cleanup(playerId);
                    cancel();
                    return;
                }

                Integer timeLeft = combatTimers.get(playerId);
                if (timeLeft == null) {
                    cleanup(playerId);
                    cancel();
                    return;
                }

                if (timeLeft > 0) {
                    combatTimers.put(playerId, timeLeft - 1);

                    if (displayType.equals("actionbar")) {
                        String actionBarMessage = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.action-bar-timer", "&c{timeLeft}/{maxTime}")
                                .replace("{timeLeft}", String.valueOf(timeLeft))
                                .replace("{maxTime}", String.valueOf(timerDuration));
                        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(actionBarMessage));
                    } else if (displayType.equals("bossbar")) {
                        BossBar bar = bossBars.get(playerId);
                        if (bar != null) {
                            String bossBarText = CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.messages.bossbar-title", "&cIm Kampf: {timeLeft}s")
                                    .replace("{timeLeft}", String.valueOf(timeLeft));
                            bar.name(LegacyComponentSerializer.legacyAmpersand().deserialize(bossBarText));
                            bar.progress((float) timeLeft / timerDuration);
                        }
                    }
                } else {
                    cleanup(playerId);
                    cancel();
                }
            }
        };
        timerTask.runTaskTimer(CombatLog.getPlugin(CombatLog.class), 0, 20);
        activeTimers.put(playerId, timerTask);
    }

    private void cancelCombatTimer(Player player) {
        UUID playerId = player.getUniqueId();
        cleanup(playerId);
    }

    private void cleanup(UUID playerId) {
        combatTimers.remove(playerId);

        if (activeTimers.containsKey(playerId)) {
            activeTimers.get(playerId).cancel();
            activeTimers.remove(playerId);
        }

        BossBar bossBar = bossBars.remove(playerId);
        if (bossBar != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                bossBar.removeViewer(online);
            }
        }
    }
}