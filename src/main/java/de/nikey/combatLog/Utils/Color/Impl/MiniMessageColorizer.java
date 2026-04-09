package de.nikey.combatLog.Utils.Color.Impl;

import de.nikey.combatLog.Utils.Color.Colorizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;

public class MiniMessageColorizer implements Colorizer {

    private static final Pattern MINI_MESSAGE_TAG = Pattern.compile("<[/!?#]?[a-zA-Z][^>]*>");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    @Override
    public Component colorize(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        if (MINI_MESSAGE_TAG.matcher(message).find()) {
            return MINI_MESSAGE.deserialize(message);
        }

        return LEGACY.deserialize(message);
    }
}
