package de.nikey.combatLog.Utils;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class WorldGuardHook {

    public static StateFlag ALLOW_COMBAT_ENTRY;
    private static boolean worldGuardEnabled = false;

    public static void tryRegisterFlag(Plugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            plugin.getLogger().info("WorldGuard not found!");
            return;
        }

        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            StateFlag flag = new StateFlag("allow-combat-entry", true);
            registry.register(flag);
            ALLOW_COMBAT_ENTRY = flag;
            worldGuardEnabled = true;

            plugin.getLogger().info("WorldGuard detected: Custom Flag 'allow-combat-entry' registered.");
        } catch (Exception e) {
            plugin.getLogger().warning("Error while registering WorldGuard-Flag: " + e.getMessage());
        }
    }

    public static boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public static StateFlag getAllowCombatEntry() {
        return ALLOW_COMBAT_ENTRY;
    }
}

