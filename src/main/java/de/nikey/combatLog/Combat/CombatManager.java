package de.nikey.combatLog.Combat;

import de.nikey.combatLog.CombatLog;
import de.nikey.combatLog.Config.PluginConfig;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns all combat state (timers, boss bars) and exposes a clean API
 * for tagging/untagging players. Listeners never touch the maps directly.
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

    public CombatManager(CombatLog plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean isInCombat(Player player) {
        return combatTimers.containsKey(player.getUniqueId());
    }

    /**
     * Tags one player into combat. Refreshes the timer if already tagged.
     * Stops gliding if elytra is disabled in combat.
     */
    public void tag(Player player) {
        if (config.elytraDisabledInCombat()) {
            player.setGliding(false);
        }

        UUID id = player.getUniqueId();
        int duration = config.timerDurationSeconds();

        if (combatTimers.containsKey(id)) {
            combatTimers.put(id, duration); // refresh
            return;
        }

        notifyAfkIfNeeded(player);
        combatTimers.put(id, duration);
        scheduleTimerTask(player, duration);
    }

    /**
     * Convenience: untag both, then tag both, and stop gliding.
     */
    public void tagBoth(Player a, Player b) {
        untag(a);
        untag(b);
        tag(a);
        tag(b);
    }

    /** Removes a player from combat and cancels their timer/bossbar. */
    public void untag(Player player) {
        cleanup(player.getUniqueId());
    }

    /** Called on plugin shutdown – cancels all running tasks cleanly. */
    public void shutdown() {
        activeTimers.values().forEach(BukkitRunnable::cancel);
        activeTimers.clear();
        combatTimers.clear();
        bossBars.forEach((id, bar) -> Bukkit.getOnlinePlayers().forEach(bar::removeViewer));
        bossBars.clear();
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

        BukkitRunnable task = activeTimers.remove(id);
        if (task != null) task.cancel();

        BossBar bar = bossBars.remove(id);
        if (bar != null) Bukkit.getOnlinePlayers().forEach(bar::removeViewer);
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