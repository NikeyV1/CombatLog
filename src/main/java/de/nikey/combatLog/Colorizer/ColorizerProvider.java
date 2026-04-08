package de.nikey.combatLog.Colorizer;

import de.nikey.combatLog.Colorizer.impl.LegacyColorizer;
import de.nikey.combatLog.Colorizer.impl.MiniMessageColorizer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/**
 * Provides a singleton Colorizer instance configured from the config.
 */
public final class ColorizerProvider {

    public static Colorizer COLORIZER;

    private ColorizerProvider() {}

    /**
     * Initializes the colorizer based on the config.yml setting.
     * Must be called before any messages are processed.
     *
     * @param config the config to read the serializer setting from
     */
    public static void init(FileConfiguration config) {
        String serializerType = config.getString("colorizer.serializer", "LEGACY").toUpperCase(Locale.ENGLISH);
        COLORIZER = "MINIMESSAGE".equals(serializerType)
                ? new MiniMessageColorizer()
                : new LegacyColorizer();
    }

    /**
     * Shorthand to colorize a message using the active colorizer.
     *
     * @param message the raw message with color codes
     * @return the colorized message
     */
    public static String colorize(String message) {
        return COLORIZER.colorize(message);
    }
}
