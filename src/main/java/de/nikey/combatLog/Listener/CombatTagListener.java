package de.nikey.combatLog.Listener;


import de.nikey.combatLog.Combat.CombatManager;
import de.nikey.combatLog.Config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Handles all events that trigger combat tagging:
 * direct hits, projectiles (arrow / trident / wither skull),
 * explosions (end crystal, respawn anchor, block), and ender pearls.
 */
public class CombatTagListener implements Listener {

    private final CombatManager combat;
    private final PluginConfig config;

    public CombatTagListener(CombatManager combat, PluginConfig config) {
        this.combat = combat;
        this.config = config;
    }

    // ── Direct & Projectile Damage ────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (config.isIgnoredWorld(event.getEntity().getWorld().getName())) return;
        if (!(event.getEntity() instanceof Player damaged)) return;

        Player damager = resolveAttacker(event.getDamager());
        if (damager == null || damager == damaged) return;

        combat.tagBoth(damaged, damager);
    }

    /**
     * Resolves the actual player attacker from a direct hit or supported projectile.
     * Returns {@code null} when the source is not a player or projectile type is unsupported.
     */
    private Player resolveAttacker(Entity damagerEntity) {
        if (damagerEntity instanceof Player p) return p;

        if (damagerEntity instanceof Arrow arrow)        return shooterAsPlayer(arrow.getShooter());
        if (damagerEntity instanceof Trident trident)   return shooterAsPlayer(trident.getShooter());
        if (damagerEntity instanceof WitherSkull skull) return shooterAsPlayer(skull.getShooter());

        // EnderCrystal handled separately in onEntityDamageByEntity is not needed;
        // crystal explosions arrive as EntityDamageByEntityEvent with EnderCrystal as damager.
        if (damagerEntity instanceof EnderCrystal) {
            return null; // tagged via tagSingleIfExplosions below
        }

        return null;
    }

    private Player shooterAsPlayer(ProjectileSource source) {
        return source instanceof Player p ? p : null;
    }

    // ── Explosion-only: EnderCrystal ──────────────────────────────────────────
    // (Re-checked after resolveAttacker returns null for crystals)

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onCrystalExplosion(EntityDamageByEntityEvent event) {
        if (!config.explosionsSetCombat()) return;
        if (config.isIgnoredWorld(event.getEntity().getWorld().getName())) return;
        if (!(event.getDamager() instanceof EnderCrystal)) return;
        if (!(event.getEntity() instanceof Player damaged)) return;

        combat.untag(damaged);
        combat.tag(damaged);
        damaged.setGliding(false);
    }

    // ── Block Explosions (Respawn Anchor / TNT etc.) ───────────────────────────

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDamageByBlock(EntityDamageByBlockEvent event) {
        if (!config.explosionsSetCombat()) return;
        if (config.isIgnoredWorld(event.getEntity().getWorld().getName())) return;
        if (!(event.getEntity() instanceof Player damaged)) return;

        boolean isBlockExplosion = event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
        boolean isRespawnAnchor  = event.getDamager() != null
                && event.getDamager().getType() == Material.RESPAWN_ANCHOR;

        if (!isBlockExplosion && !isRespawnAnchor) return;

        combat.untag(damaged);
        combat.tag(damaged);
        damaged.setGliding(false);
    }

    // ── Ender Pearl Landing ───────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEnderPearlLand(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;
        if (config.isIgnoredWorld(player.getWorld().getName())) return;
        if (!config.enderpearlSetCombatOnLand()) return;
        if (config.enderpearlOnlyIfAlreadyInCombat() && !combat.isInCombat(player)) return;

        combat.untag(player);
        combat.tag(player);
        player.setGliding(false);
    }
}
