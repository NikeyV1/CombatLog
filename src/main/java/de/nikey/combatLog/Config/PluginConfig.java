package de.nikey.combatLog.Config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Central access point for all config values.
 * Eliminates scattered getConfig() calls throughout listeners.
 */
public class PluginConfig {

    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final FileConfiguration config;

    public PluginConfig(FileConfiguration config) {
        this.config = config;
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

    // ── Messages ──────────────────────────────────────────────────────────────

    public Component message(String path, String def) {
        return LEGACY.deserialize(rawMessage(path, def));
    }

    public String rawMessage(String path, String def) {
        return config.getString(path, def);
    }
}