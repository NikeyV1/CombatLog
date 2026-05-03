package de.nikey.combatLog.Listener;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import de.nikey.combatLog.CombatLog;
import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.PluginConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces all restrictions that apply while a player is in combat.
 * Players with {@code combatlog.bypass} skip all restrictions.
 */
public class CombatRestrictionListener implements Listener {

    private final CombatManager combat;
    private final PluginConfig config;

    private final Map<UUID, Long> riptideCooldowns = new HashMap<>();

    public CombatRestrictionListener(CombatManager combat, PluginConfig config) {
        this.combat = combat;
        this.config = config;
    }

    // ── Elytra ────────────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!config.elytraDisabledInCombat()) return;
        if (!combat.isInCombat(player)) return;
        if (!event.isGliding()) return;
        if (player.hasPermission("combatlog.bypass")) return;

        event.setCancelled(true);
        player.setGliding(false);
        applyDamage(player);
        player.sendMessage(config.message("combat-log.messages.elytra-use-denied", "&dYou can't use elytra in combat"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerElytraBoost(PlayerElytraBoostEvent event) {
        Player player = event.getPlayer();
        if (!config.elytraDisabledInCombat()) return;
        if (!combat.isInCombat(player)) return;
        if (player.hasPermission("combatlog.bypass")) return;

        event.setCancelled(true);
        applyDamage(player);
        player.sendMessage(config.message("combat-log.messages.elytra-use-denied", "&dYou can't use elytra in combat"));
    }

    // ── Teleport ──────────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!config.teleportingDisabledInCombat()) return;
        if (!combat.isInCombat(player)) return;
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) return;
        if (player.hasPermission("combatlog.bypass")) return;

        event.setCancelled(true);
        applyDamage(player);
        player.sendMessage(config.message("combat-log.messages.teleporting-denied", "&dYou can't teleport in combat"));
    }

    // ── Blocked commands ──────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combat.isInCombat(player)) return;
        if (player.hasPermission("combatlog.bypass")) return;

        List<String> blocked = config.blockedCommands();
        if (blocked.isEmpty()) return;

        String cmd = extractCommand(event.getMessage());
        boolean isBlocked = blocked.stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .anyMatch(cmd::equals);

        if (isBlocked) {
            event.setCancelled(true);
            player.sendMessage(config.message("combat-log.messages.blocked-command", "&cYou can't use this command in combat"));
        }
    }

    private String extractCommand(String message) {
        String[] args = message.split(" ");
        String cmd = args[0].startsWith("/") ? args[0].substring(1) : args[0];
        return cmd.toLowerCase(Locale.ROOT);
    }

    // ── Mending ───────────────────────────────────────────────────────────────

    @EventHandler
    public void onMend(PlayerItemMendEvent event) {
        if (!config.mendingDisabledInCombat()) return;
        if (event.getPlayer().hasPermission("combatlog.bypass")) return;
        if (combat.isInCombat(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // ── Riptide ───────────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        if (!config.stopRiptidingInCombat()) return;
        if (!combat.isInCombat(player)) return;
        if (player.hasPermission("combatlog.bypass")) return;

        long now  = System.currentTimeMillis();
        long last = riptideCooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (now - last >= config.riptideCooldownMs()) {
            riptideCooldowns.put(player.getUniqueId(), now);
        } else {
            event.setCancelled(true);
            applyDamage(player);
            player.sendMessage(config.message("combat-log.messages.riptide-denied", "&cYou can't use riptide in combat"));
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void applyDamage(Player player) {
        double dmg = config.punishmentDamage();
        if (dmg > 0) player.damage(dmg);
    }
}