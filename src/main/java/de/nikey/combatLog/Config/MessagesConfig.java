package de.nikey.combatLog.Config;

import de.nikey.combatLog.Colorizer.ColorizerProvider;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Central access point for all messages in messages.yml.
 */
public class MessagesConfig {

    private final FileConfiguration config;

    public MessagesConfig(FileConfiguration config) {
        this.config = config;
    }

    /** Returns a colorized Component message with placeholder replacement. */
    public org.bukkit.ChatColor message(String path, String def) {
        return org.bukkit.ChatColor.valueOf(colorizedMessage(path, def));
    }

    /** Returns a colorized string with placeholder replacement. */
    public String colorizedMessage(String path, String def) {
        return ColorizerProvider.colorize(rawMessage(path, def));
    }

    /** Returns a colorized string with placeholders replaced. */
    public String colorizedMessage(String path, String def, String... replacements) {
        String raw = rawMessage(path, def);
        for (int i = 0; i < replacements.length; i += 2) {
            String key = replacements[i];
            String value = i + 1 < replacements.length ? replacements[i + 1] : "";
            raw = raw.replace(key, value);
        }
        return ColorizerProvider.colorize(raw);
    }

    /** Returns a raw string message without colorization. */
    public String rawMessage(String path, String def) {
        return config.getString(path, def);
    }

    /** Returns a raw string message with placeholder replacement, without colorization. */
    public String rawMessage(String path, String def, String... replacements) {
        String raw = config.getString(path, def);
        for (int i = 0; i < replacements.length; i += 2) {
            String key = replacements[i];
            String value = i + 1 < replacements.length ? replacements[i + 1] : "";
            raw = raw.replace(key, value);
        }
        return raw;
    }
}
