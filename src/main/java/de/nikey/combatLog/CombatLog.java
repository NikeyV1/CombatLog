package de.nikey.combatLog;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import de.nikey.combatLog.Listener.AntiKillAbuse;
import de.nikey.combatLog.Listener.GeneralListener;
import de.nikey.combatLog.Utils.Metrics;
import de.nikey.combatLog.Utils.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import static de.nikey.combatLog.Listener.GeneralListener.activeTimers;
import static de.nikey.combatLog.Listener.GeneralListener.combatTimers;

public final class CombatLog extends JavaPlugin {

    private static boolean worldGuardEnabled = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(new GeneralListener(), this);
        new AntiKillAbuse(this);

        Metrics metrics = new Metrics(this,	28071);
    }

    @Override
    public void onDisable() {
        combatTimers.clear();
        activeTimers.values().forEach(BukkitRunnable::cancel);
        activeTimers.clear();
    }

    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().info("WorldGuard not found.");
            return;
        }
        WorldGuardHook.tryRegisterFlag(this);
    }


    public static boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
}
