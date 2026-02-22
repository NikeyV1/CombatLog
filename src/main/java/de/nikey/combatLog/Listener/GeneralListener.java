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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
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

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    // ------------------------------------------------------------
    // Config helpers (new config structure)
    // ------------------------------------------------------------

    private boolean isIgnoredWorld(Player p) {
        List<String> ignored = CombatLog.getPlugin(CombatLog.class).getConfig().getStringList("combat-log.ignored-worlds");
        return ignored.contains(p.getWorld().getName());
    }

    private boolean isIgnoredWorld(World w) {
        List<String> ignored = CombatLog.getPlugin(CombatLog.class).getConfig().getStringList("combat-log.ignored-worlds");
        return ignored.contains(w.getName());
    }

    private int timerDurationSeconds() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getInt("combat-log.timer.duration-seconds", 15);
    }

    private String timerDisplayType() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getString("combat-log.timer.display", "actionbar").toLowerCase();
    }

    private double punishmentDamage() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getDouble("combat-log.punishment.damage", 0.0);
    }

    private boolean killOnLogout() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.punishment.kill-on-logout", false);
    }

    private boolean explosionsSetCombat() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.restrictions.explosions.set-combat-on-explosion", true);
    }

    private boolean teleportingDisabledInCombat() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.restrictions.teleporting.disabled-in-combat", false);
    }

    private boolean elytraDisabledInCombat() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.restrictions.elytra.disabled-in-combat", true);
    }

    private boolean mendingDisabledInCombat() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.restrictions.mending.disabled-in-combat", true);
    }

    private boolean stopRiptidingInCombat() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.restrictions.riptide.stop", false);
    }

    private int riptideCooldown() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getInt("combat-log.restrictions.riptide.cooldown", 10000);
    }

    private boolean combatZoneEnabled() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.combat-zone.enabled", false);
    }

    private double combatZoneRadius() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getDouble("combat-log.combat-zone.radius", 10.0);
    }

    private boolean enderpearlSetCombatOnLand() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.triggers.enderpearl.set-combat-on-land", true);
    }

    private boolean enderpearlOnlyIfAlreadyInCombat() {
        return CombatLog.getPlugin(CombatLog.class).getConfig().getBoolean("combat-log.triggers.enderpearl.only-if-already-in-combat", false);
    }

    private Component msg(String path, String def) {
        String raw = CombatLog.getPlugin(CombatLog.class).getConfig().getString(path, def);
        return LEGACY.deserialize(raw);
    }

    // ------------------------------------------------------------
    // Combat triggers
    // ------------------------------------------------------------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (isIgnoredWorld(event.getEntity().getWorld())) return;

        if (event.getEntity() instanceof Player damaged && event.getDamager() instanceof Player damager) {
            if (damaged == damager) return;
            tagBoth(damaged, damager);
            return;
        }

        if (event.getDamager() instanceof Arrow arrow) {
            ProjectileSource shooter = arrow.getShooter();
            if (shooter instanceof Player damager && event.getEntity() instanceof Player damaged) {
                if (damager == damaged) return;
                tagBoth(damaged, damager);
            }
            return;
        }

        if (event.getDamager() instanceof Trident trident) {
            ProjectileSource shooter = trident.getShooter();
            if (shooter instanceof Player damager && event.getEntity() instanceof Player damaged) {
                if (damager == damaged) return;
                tagBoth(damaged, damager);
            }
            return;
        }

        if (event.getDamager() instanceof WitherSkull skull) {
            ProjectileSource shooter = skull.getShooter();
            if (shooter instanceof Player damager && event.getEntity() instanceof Player damaged) {
                if (damager == damaged) return;
                tagBoth(damaged, damager);
            }
            return;
        }

        if (event.getDamager() instanceof EnderCrystal) {
            if (!explosionsSetCombat()) return;
            if (event.getEntity() instanceof Player damaged) {
                cancelCombatTimer(damaged);
                startCombatTimer(damaged);
                damaged.setGliding(false);
            }
        }
    }

    private void tagBoth(Player damaged, Player damager) {
        cancelCombatTimer(damaged);
        cancelCombatTimer(damager);
        startCombatTimer(damaged);
        startCombatTimer(damager);
        damaged.setGliding(false);
        damager.setGliding(false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDamageByBlock(EntityDamageByBlockEvent event) {
        if (!explosionsSetCombat()) return;
        if (isIgnoredWorld(event.getEntity().getWorld())) return;

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

    // ------------------------------------------------------------
    // WorldGuard region entry denial while in combat
    // ------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMoveIntoRegion(PlayerMoveEvent event) {
        if (!CombatLog.isWorldGuardEnabled()) return;

        Player player = event.getPlayer();
        if (!combatTimers.containsKey(player.getUniqueId())) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()) return;

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        if (!query.testState(BukkitAdapter.adapt(event.getTo()), localPlayer, WorldGuardHook.ALLOW_COMBAT_ENTRY)) {
            event.setCancelled(true);
            player.teleport(event.getFrom());

            player.sendMessage(msg(
                    "combat-log.messages.region-entry-denied",
                    "§cYou can't enter this region in combat"
            ));
        }
    }

    // ------------------------------------------------------------
    // Combat zone tagging on join (kept as-is, just new paths)
    // ------------------------------------------------------------

    private boolean isAfk(Player player) {
        return player.hasMetadata("afk") && player.getMetadata("afk").getFirst().asBoolean();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!combatZoneEnabled()) return;

        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) return;
                if (isIgnoredWorld(player)) return;

                double radius = combatZoneRadius();

                for (Player nearby : player.getWorld().getNearbyPlayers(player.getLocation(), radius)) {
                    if (player == nearby) continue;
                    if (nearby.isDead()) continue;
                    if (nearby.getGameMode() == GameMode.SPECTATOR || nearby.getGameMode() == GameMode.CREATIVE) continue;
                    if (isIgnoredWorld(nearby)) continue;

                    cancelCombatTimer(nearby);
                    cancelCombatTimer(player);
                    startCombatTimer(player);
                    startCombatTimer(nearby);
                }
            }
        }.runTaskTimer(CombatLog.getPlugin(CombatLog.class), 0L, 20L);
    }

    // ------------------------------------------------------------
    // Enderpearl trigger (new paths)
    // ------------------------------------------------------------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEnderPearlLand(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;
        if (isIgnoredWorld(player)) return;

        if (!enderpearlSetCombatOnLand()) return;

        boolean onlyIfAlready = enderpearlOnlyIfAlreadyInCombat();
        boolean isInCombat = combatTimers.containsKey(player.getUniqueId());
        if (onlyIfAlready && !isInCombat) return;

        cancelCombatTimer(player);
        startCombatTimer(player);
        player.setGliding(false);
    }

    // ------------------------------------------------------------
    // Riptide stop + cooldown (new paths)
    // ------------------------------------------------------------

    private final Map<UUID, Long> playerCooldowns = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!stopRiptidingInCombat()) return;
        if (!combatTimers.containsKey(player.getUniqueId())) return;

        if (player.isRiptiding()) {
            long now = System.currentTimeMillis();
            long last = playerCooldowns.getOrDefault(player.getUniqueId(), 0L);

            if (now - last >= riptideCooldown()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        playerCooldowns.put(player.getUniqueId(), now);
                    }
                }.runTaskLater(CombatLog.getPlugin(CombatLog.class), 20L * 2);
            } else {
                event.setCancelled(true);
                double dmg = punishmentDamage();
                if (dmg > 0) player.damage(dmg);
            }
        }
    }

    // ------------------------------------------------------------
    // Logout during combat (new paths)
    // ------------------------------------------------------------

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (combatTimers.containsKey(player.getUniqueId())) {
            String raw = CombatLog.getPlugin(CombatLog.class)
                    .getConfig()
                    .getString("combat-log.messages.combat-log", "&c{player} has combat logged!")
                    .replace("{player}", player.getName());

            Bukkit.broadcast(LEGACY.deserialize(raw));

            if (killOnLogout()) {
                player.setHealth(0);
            }

            cancelCombatTimer(player);
        }
    }

    // ------------------------------------------------------------
    // Teleport blocking (new paths)
    // ------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (!teleportingDisabledInCombat()) return;
        if (!combatTimers.containsKey(player.getUniqueId())) return;

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) return;

        event.setCancelled(true);
        player.sendMessage(msg(
                "combat-log.messages.teleporting-denied",
                "§dYou can't teleport in combat"
        ));

        double dmg = punishmentDamage();
        if (dmg > 0) player.damage(dmg);
    }

    // ------------------------------------------------------------
    // Blocked commands (new paths; also fixes component sending)
    // ------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combatTimers.containsKey(player.getUniqueId())) return;

        String[] args = event.getMessage().split(" ");
        String cmd = args[0].startsWith("/") ? args[0].substring(1) : args[0];

        List<String> blocked = CombatLog.getPlugin(CombatLog.class).getConfig().getStringList("combat-log.blocked-commands");
        if (blocked.isEmpty()) return;

        if (blocked.contains(cmd)) {
            event.setCancelled(true);
            player.sendMessage(msg(
                    "combat-log.messages.blocked-command",
                    "§cYou can't use this command in combat"
            ));
        }
    }

    // ------------------------------------------------------------
    // Elytra restrictions (new paths)
    // ------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (!elytraDisabledInCombat()) return;
        if (!combatTimers.containsKey(player.getUniqueId())) return;
        if (!event.isGliding()) return;

        player.setGliding(false);
        event.setCancelled(true);

        double dmg = punishmentDamage();
        if (dmg > 0) player.damage(dmg);

        player.sendMessage(msg(
                "combat-log.messages.elytra-use-denied",
                "&dYou can't use elytra in combat"
        ));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerElytraBoost(PlayerElytraBoostEvent event) {
        Player player = event.getPlayer();

        if (!elytraDisabledInCombat()) return;
        if (!combatTimers.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);

        double dmg = punishmentDamage();
        if (dmg > 0) player.damage(dmg);

        player.sendMessage(msg(
                "combat-log.messages.elytra-use-denied",
                "&dYou can't use elytra in combat"
        ));
    }

    // ------------------------------------------------------------
    // Mending disabled (new paths)
    // ------------------------------------------------------------

    @EventHandler
    public void onMend(PlayerItemMendEvent event) {
        if (!mendingDisabledInCombat()) return;

        Player player = event.getPlayer();
        if (combatTimers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------
    // Timer system (new paths + message nesting)
    // ------------------------------------------------------------

    private void startCombatTimer(Player player) {
        UUID playerId = player.getUniqueId();

        if (elytraDisabledInCombat()) player.setGliding(false);

        int duration = timerDurationSeconds();
        String displayType = timerDisplayType();

        // refresh timer if already in combat
        if (combatTimers.containsKey(playerId)) {
            combatTimers.put(playerId, duration);
            return;
        }

        if (isAfk(player)) {
            player.showTitle(Title.title(
                    Component.empty(),
                    Component.text("Please disable afk, you are in combat!").color(NamedTextColor.RED)
            ));
        }

        combatTimers.put(playerId, duration);

        if (displayType.equals("bossbar")) {
            BossBar bossBar = BossBar.bossBar(Component.text(""), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
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
                        String raw = CombatLog.getPlugin(CombatLog.class)
                                .getConfig()
                                .getString("combat-log.messages.timer.actionbar", "&c{timeLeft}/{maxTime}");

                        raw = raw.replace("{timeLeft}", String.valueOf(timeLeft))
                                .replace("{maxTime}", String.valueOf(duration));

                        player.sendActionBar(LEGACY.deserialize(raw));
                    } else if (displayType.equals("bossbar")) {
                        BossBar bar = bossBars.get(playerId);
                        if (bar != null) {
                            String raw = CombatLog.getPlugin(CombatLog.class)
                                    .getConfig()
                                    .getString("combat-log.messages.timer.bossbar-title", "&cIn Combat: {timeLeft}s")
                                    .replace("{timeLeft}", String.valueOf(timeLeft));

                            bar.name(LEGACY.deserialize(raw));
                            bar.progress((float) timeLeft / (float) duration);
                        }
                    }
                } else {
                    cleanup(playerId);
                    cancel();
                }
            }
        };

        timerTask.runTaskTimer(CombatLog.getPlugin(CombatLog.class), 0L, 20L);
        activeTimers.put(playerId, timerTask);
    }

    private void cancelCombatTimer(Player player) {
        cleanup(player.getUniqueId());
    }

    private void cleanup(UUID playerId) {
        combatTimers.remove(playerId);

        BukkitRunnable task = activeTimers.remove(playerId);
        if (task != null) task.cancel();

        BossBar bar = bossBars.remove(playerId);
        if (bar != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                bar.removeViewer(online);
            }
        }
    }
}
