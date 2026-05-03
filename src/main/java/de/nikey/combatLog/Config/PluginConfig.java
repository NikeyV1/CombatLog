package de.nikey.combatLog.Config;

import de.nikey.combatLog.Utils.Color.Colorizer;
import de.nikey.combatLog.Utils.Color.Impl.MiniMessageColorizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Central access point for all config values.
 */
public class PluginConfig {

    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Colorizer COLORIZER = new MiniMessageColorizer();

    private FileConfiguration config;
    private FileConfiguration messages;

    public PluginConfig(FileConfiguration config, FileConfiguration messages) {
        this.config = config;
        this.messages = messages;
    }

    public void reload(FileConfiguration config, FileConfiguration messages) {
        this.config = config;
        this.messages = messages;
    }

    // ── Timer ────────────────────────────────────────────────────────────────

    public int timerDurationSeconds() {
        return config.getInt("combat-log.timer.duration-seconds", 15);
    }

    public String timerDisplayType() {
        return config.getString("combat-log.timer.display", "actionbar").toLowerCase();
    }

    // ── Punishment ───────────────────────────────────────────────────────────

    public double punishmentDamage() {
        return config.getDouble("combat-log.punishment.damage", 0.0);
    }

    public boolean killOnLogout() {
        return config.getBoolean("combat-log.punishment.kill-on-logout", false);
    }

    // ── Restrictions ─────────────────────────────────────────────────────────

    public boolean elytraDisabledInCombat() {
        return config.getBoolean("combat-log.restrictions.elytra.disabled-in-combat", true);
    }

    public boolean teleportingDisabledInCombat() {
        return config.getBoolean("combat-log.restrictions.teleporting.disabled-in-combat", false);
    }

    public boolean mendingDisabledInCombat() {
        return config.getBoolean("combat-log.restrictions.mending.disabled-in-combat", true);
    }

    public boolean stopRiptidingInCombat() {
        return config.getBoolean("combat-log.restrictions.riptide.stop", false);
    }

    public int riptideCooldownMs() {
        return config.getInt("combat-log.restrictions.riptide.cooldown", 10000);
    }

    public boolean explosionsSetCombat() {
        return config.getBoolean("combat-log.restrictions.explosions.set-combat-on-explosion", true);
    }

    // ── Triggers ─────────────────────────────────────────────────────────────

    public boolean enderpearlSetCombatOnLand() {
        return config.getBoolean("combat-log.triggers.enderpearl.set-combat-on-land", true);
    }

    public boolean enderpearlOnlyIfAlreadyInCombat() {
        return config.getBoolean("combat-log.triggers.enderpearl.only-if-already-in-combat", false);
    }

    // ── World / Zone ──────────────────────────────────────────────────────────

    public List<String> ignoredWorlds() {
        return config.getStringList("combat-log.ignored-worlds");
    }

    public boolean isIgnoredWorld(String worldName) {
        return ignoredWorlds().contains(worldName);
    }

    public boolean combatZoneEnabled() {
        return config.getBoolean("combat-log.combat-zone.enabled", false);
    }

    public double combatZoneRadius() {
        return config.getDouble("combat-log.combat-zone.radius", 10.0);
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    public List<String> blockedCommands() {
        return config.getStringList("combat-log.blocked-commands");
    }

    // ── Potion effects on tag ─────────────────────────────────────────────────

    /**
     * Returns the list of potion effects to apply when a player first enters combat.
     * Config format:
     * <pre>
     * combat-log:
     *   tag-effects:
     *     - type: GLOWING
     *       duration-seconds: 15
     *       amplifier: 0
     *       show-particles: false
     * </pre>
     */
    public List<PotionEffectEntry> combatTagEffects() {
        List<?> raw = config.getList("combat-log.tag-effects");
        if (raw == null || raw.isEmpty()) return List.of();

        List<PotionEffectEntry> result = new ArrayList<>();
        for (Object obj : raw) {
            if (!(obj instanceof Map<?, ?> rawMap)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;

            String type           = String.valueOf(map.getOrDefault("type", "")).toUpperCase();
            int durationSeconds   = toInt(map.getOrDefault("duration-seconds", 15), 15);
            int amplifier         = toInt(map.getOrDefault("amplifier", 0), 0);
            boolean showParticles = Boolean.parseBoolean(String.valueOf(map.getOrDefault("show-particles", false)));

            if (!type.isBlank()) {
                result.add(new PotionEffectEntry(type, durationSeconds, amplifier, showParticles));
            }
        }
        return result;
    }

    private int toInt(Object value, int def) {
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException e) { return def; }
    }

    /** Immutable record for a single configured potion effect entry. */
    public record PotionEffectEntry(
            String type,
            int durationSeconds,
            int amplifier,
            boolean showParticles
    ) {}

    // ── SafeZone Barrier ─────────────────────────────────────────────────────

    public boolean safeZoneBarrierEnabled() {
        return config.getBoolean("combat-log.worldguard.safe-zone-barrier.enabled", true);
    }

    public Material safeZoneBarrierMaterial() {
        String name = config.getString("combat-log.worldguard.safe-zone-barrier.material", "RED_STAINED_GLASS");
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.RED_STAINED_GLASS;
        }
    }

    public int safeZoneBarrierRadius() {
        return config.getInt("combat-log.worldguard.safe-zone-barrier.radius", 1);
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    public Component message(String path, String def) {
        return COLORIZER.colorize(rawMessage(path, def));
    }

    public Component message(String path, String def, Map<String, String> placeholders) {
        return COLORIZER.colorize(rawMessage(path, def, placeholders));
    }

    public Component colorize(String rawMessage) {
        return COLORIZER.colorize(rawMessage);
    }

    public String rawMessage(String path, String def) {
        return messages.getString(path, def);
    }

    public String rawMessage(String path, String def, Map<String, String> placeholders) {
        String message = rawMessage(path, def);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
}