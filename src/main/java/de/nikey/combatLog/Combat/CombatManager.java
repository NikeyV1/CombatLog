package de.nikey.combatLog.Combat;

import de.nikey.combatLog.CombatLog;
import de.nikey.combatLog.Config.PluginConfig;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns all combat state (timers, boss bars, opponents, effects) and exposes a
 * clean API for tagging/untagging players. Listeners never touch the maps directly.
 */
public class CombatManager {

    private final CombatLog plugin;
    private final PluginConfig config;

    /** Remaining seconds for each player in combat. */
    private final Map<UUID, Integer> combatTimers = new HashMap<>();
    /** Running ticker tasks. */
    private final Map<UUID, BukkitRunnable> activeTimers = new HashMap<>();
    /** Active boss bars per player. */
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    /** Most recent opponent per player — used by PlaceholderAPI. */
    private final Map<UUID, UUID> opponents = new HashMap<>();
    /** Potion effect types applied by CombatLog — removed on untag. */
    private final Map<UUID, List<PotionEffectType>> appliedEffects = new HashMap<>();

    public CombatManager(CombatLog plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean isInCombat(Player player) {
        return combatTimers.containsKey(player.getUniqueId());
    }

    /** Returns the remaining combat seconds, or 0 if not in combat. */
    public int getTimeLeft(Player player) {
        return combatTimers.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Returns the most recent opponent of a tagged player, or {@code null} if
     * the opponent is offline, unknown, or the player is not in combat.
     */
    public Player getOpponent(Player player) {
        UUID oppId = opponents.get(player.getUniqueId());
        if (oppId == null) return null;
        return Bukkit.getPlayer(oppId);
    }

    /**
     * Tags a player into combat without a known opponent.
     * Refreshes the timer if already tagged.
     */
    public void tag(Player player) {
        tag(player, null);
    }

    /**
     * Tags a player into combat and records their opponent.
     * Refreshes timer and updates opponent on subsequent calls.
     */
    public void tag(Player player, Player opponent) {
        if (config.elytraDisabledInCombat()) {
            player.setGliding(false);
        }

        UUID id = player.getUniqueId();
        int duration = config.timerDurationSeconds();

        if (opponent != null) {
            opponents.put(id, opponent.getUniqueId());
        }

        if (combatTimers.containsKey(id)) {
            combatTimers.put(id, duration); // refresh
            return;
        }

        // First tag only
        notifyAfkIfNeeded(player);
        applyTagEffects(player);
        combatTimers.put(id, duration);
        scheduleTimerTask(player, duration);
    }

    /**
     * Untags both players, then tags both with each other as opponent.
     */
    public void tagBoth(Player a, Player b) {
        untag(a);
        untag(b);
        tag(a, b);
        tag(b, a);
    }

    /** Removes a player from combat and cleans up all associated state. */
    public void untag(Player player) {
        cleanup(player.getUniqueId());
    }

    /** Called on plugin shutdown — cancels all running tasks cleanly. */
    public void shutdown() {
        activeTimers.values().forEach(BukkitRunnable::cancel);
        activeTimers.clear();
        combatTimers.clear();
        bossBars.forEach((id, bar) -> Bukkit.getOnlinePlayers().forEach(bar::removeViewer));
        bossBars.clear();
        opponents.clear();
        appliedEffects.clear();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void scheduleTimerTask(Player player, int duration) {
        UUID id = player.getUniqueId();
        String displayType = config.timerDisplayType();

        if (displayType.equals("bossbar")) {
            BossBar bar = BossBar.bossBar(Component.text(""), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
            bar.addViewer(player);
            bossBars.put(id, bar);
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isValid()) {
                    cleanup(id);
                    cancel();
                    return;
                }

                Integer timeLeft = combatTimers.get(id);
                if (timeLeft == null) {
                    cleanup(id);
                    cancel();
                    return;
                }

                if (timeLeft > 0) {
                    combatTimers.put(id, timeLeft - 1);
                    updateDisplay(player, id, displayType, timeLeft, duration);
                } else {
                    cleanup(id);
                    cancel();
                }
            }
        };

        task.runTaskTimer(plugin, 0L, 20L);
        activeTimers.put(id, task);
    }

    private void updateDisplay(Player player, UUID id, String displayType, int timeLeft, int duration) {
        switch (displayType) {
            case "actionbar" -> {
                String raw = config.rawMessage("combat-log.messages.timer.actionbar", "&c{timeLeft}/{maxTime}")
                        .replace("{timeLeft}", String.valueOf(timeLeft))
                        .replace("{maxTime}", String.valueOf(duration));
                player.sendActionBar(PluginConfig.LEGACY.deserialize(raw));
            }
            case "bossbar" -> {
                BossBar bar = bossBars.get(id);
                if (bar == null) return;
                String raw = config.rawMessage("combat-log.messages.timer.bossbar-title", "&cIn Combat: {timeLeft}s")
                        .replace("{timeLeft}", String.valueOf(timeLeft));
                bar.name(PluginConfig.LEGACY.deserialize(raw));
                bar.progress((float) timeLeft / (float) duration);
            }
        }
    }

    private void cleanup(UUID id) {
        combatTimers.remove(id);
        opponents.remove(id);

        BukkitRunnable task = activeTimers.remove(id);
        if (task != null) task.cancel();

        BossBar bar = bossBars.remove(id);
        if (bar != null) Bukkit.getOnlinePlayers().forEach(bar::removeViewer);

        // Only remove effects that CombatLog itself applied
        Player player = Bukkit.getPlayer(id);
        List<PotionEffectType> applied = appliedEffects.remove(id);
        if (player != null && applied != null) {
            applied.forEach(player::removePotionEffect);
        }
    }

    private void applyTagEffects(Player player) {
        List<PluginConfig.PotionEffectEntry> effects = config.combatTagEffects();
        if (effects.isEmpty()) return;

        List<PotionEffectType> appliedTypes = new ArrayList<>();

        for (PluginConfig.PotionEffectEntry entry : effects) {
            NamespacedKey key = NamespacedKey.minecraft(entry.type().toLowerCase());
            PotionEffectType type = Registry.EFFECT.get(key);
            if (type == null) {
                plugin.getLogger().warning("Unknown potion effect in config: " + entry.type());
                continue;
            }
            if (player.hasPotionEffect(type)) continue;

            int durationTicks = entry.durationSeconds() * 20;
            player.addPotionEffect(new PotionEffect(type, durationTicks, entry.amplifier(), true, entry.showParticles()));
            appliedTypes.add(type);
        }

        if (!appliedTypes.isEmpty()) {
            appliedEffects.put(player.getUniqueId(), appliedTypes);
        }
    }

    private void notifyAfkIfNeeded(Player player) {
        boolean isAfk = player.hasMetadata("afk")
                && !player.getMetadata("afk").isEmpty()
                && player.getMetadata("afk").getFirst().asBoolean();

        if (isAfk) {
            player.showTitle(Title.title(
                    Component.empty(),
                    Component.text("Please disable afk, you are in combat!").color(NamedTextColor.RED)
            ));
        }
    }
}