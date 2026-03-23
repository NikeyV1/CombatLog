package de.nikey.combatLog.Utils;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.plugin.Plugin;

public class WorldGuardHook implements WorldGuardBridge {

    public static StateFlag ALLOW_COMBAT_ENTRY;
    private static boolean enabled = false;

    private final Plugin plugin;

    public WorldGuardHook(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
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

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}