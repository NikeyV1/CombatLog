package de.nikey.combatLog.Colorizer;

public interface Colorizer {

    /**
     * Converts a raw message string with color codes into a chat-compatible string.
     *
     * @param message the raw message with color codes (e.g. {@code &cRed}, {@code &#FF0000Hex})
     * @return the processed message with proper formatting codes
     */
    String colorize(String message);
}
