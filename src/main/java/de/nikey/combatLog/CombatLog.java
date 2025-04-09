package de.nikey.combatLog;

import de.nikey.combatLog.Listener.GeneralListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import static de.nikey.combatLog.Listener.GeneralListener.activeTimers;
import static de.nikey.combatLog.Listener.GeneralListener.combatTimers;

public final class CombatLog extends JavaPlugin {
    public static boolean isBuffSMP = false;
    public static boolean isTrust = false;

    @Override
    public void onEnable() {

        if (getServer().getPluginManager().getPlugin("BuffSMP") != null) {
            isBuffSMP = true;
        }

        if (getServer().getPluginManager().getPlugin("Trust") != null) {
            isTrust = true;
        }
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(new GeneralListener(), this);
    }

    @Override
    public void onDisable() {
        combatTimers.clear();
        activeTimers.values().forEach(BukkitRunnable::cancel);
        activeTimers.clear();
    }
}
