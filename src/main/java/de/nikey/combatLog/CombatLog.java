package de.nikey.combatLog;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import de.nikey.combatLog.Listener.AntiKillAbuse;
import de.nikey.combatLog.Listener.GeneralListener;
import de.nikey.combatLog.Utils.Metrics;
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

    public static StateFlag ALLOW_COMBAT_ENTRY;

    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
                StateFlag flag = new StateFlag("allow-combat-entry", true);
                registry.register(flag);
                ALLOW_COMBAT_ENTRY = flag;
                worldGuardEnabled = true;
                getLogger().info("WorldGuard erkannt: Custom Flag 'allow-combat-entry' wurde registriert.");
            } catch (Exception e) {
                getLogger().warning("Fehler beim Registrieren der WorldGuard Flag: " + e.getMessage());
            }
        }
    }

    public static boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
}
