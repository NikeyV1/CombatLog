package de.nikey.combatLog.Utils;

import de.nikey.combatLog.Combat.CombatManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for CombatLog.
 *
 * Available placeholders:
 *   %combatlog_in_combat%        → "true" / "false"
 *   %combatlog_time_left%        → remaining seconds, or "" if not in combat
 *
 * Only registered when PlaceholderAPI is present — see CombatLog#registerPlaceholders().
 */
public class CombatPlaceholders extends PlaceholderExpansion {

    private final CombatManager combat;

    public CombatPlaceholders(CombatManager combat) {
        this.combat = combat;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "combatlog";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Nikey";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    /** Prevent PlaceholderAPI from unregistering this expansion on /papi reload */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (!(offlinePlayer instanceof Player player)) return "";

        return switch (params.toLowerCase()) {
            case "in_combat" -> String.valueOf(combat.isInCombat(player));
            case "time_left" -> combat.getRemainingCombatSeconds(player)
                    .stream()
                    .mapToObj(String::valueOf)
                    .findFirst()
                    .orElse("");
            default -> null;
        };
    }
}