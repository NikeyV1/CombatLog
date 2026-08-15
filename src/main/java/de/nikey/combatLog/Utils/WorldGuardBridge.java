package de.nikey.combatLog.Utils;

import org.bukkit.entity.Player;

public interface WorldGuardBridge {
    void register();
    boolean isEnabled();
    boolean isSafeZone(Player player);
}