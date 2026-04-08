package de.nikey.combatLog.Colorizer.impl;

import de.nikey.combatLog.Colorizer.Colorizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Uses MiniMessage for colorization.
 * Falls back to legacy section serialization for chat compatibility.
 */
public class MiniMessageColorizer implements Colorizer {

    @Override
    public String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        Component component = MiniMessage.miniMessage().deserialize(message);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
