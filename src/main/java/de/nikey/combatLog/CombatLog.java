package de.nikey.combatLog;

import de.nikey.combatLog.Listener.GeneralListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import static de.nikey.combatLog.Listener.GeneralListener.activeTimers;
import static de.nikey.combatLog.Listener.GeneralListener.combatTimers;

public final class CombatLog extends JavaPlugin {

    @Override
    public void onEnable() {
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
