package de.nikey.combatLog.Utils;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Registers and exposes the custom {@code allow-combat-entry} WorldGuard flag.
 * Must be called from {@code onLoad()} before WorldGuard initialises its regions.
 */
public class WorldGuardHook {

    public static StateFlag ALLOW_COMBAT_ENTRY;
    private static boolean enabled = false;

    public static void tryRegisterFlag(Plugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            plugin.getLogger().info("WorldGuard not found – flag not registered.");
            return;
        }

        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            StateFlag flag = new StateFlag("allow-combat-entry", true);
            registry.register(flag);
            ALLOW_COMBAT_ENTRY = flag;
            enabled = true;
            plugin.getLogger().info("WorldGuard detected: custom flag 'allow-combat-entry' registered.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register WorldGuard flag: " + e.getMessage());
        }
    }

    public static boolean isWorldGuardEnabled() {
        return enabled;
    }
}

