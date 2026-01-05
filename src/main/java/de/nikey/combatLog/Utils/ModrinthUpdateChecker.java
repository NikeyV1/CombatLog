package de.nikey.combatLog.Utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.nikey.combatLog.CombatLog;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class ModrinthUpdateChecker {

    private final JavaPlugin plugin;
    private final String projectId; // Modrinth project ID

    public ModrinthUpdateChecker(String projectId) {
        this.plugin = CombatLog.getPlugin(CombatLog.class);
        this.projectId = projectId;
    }

    public void checkForUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String currentVersion = plugin.getDescription().getVersion();
                    String urlString = "https://api.modrinth.com/v2/project/" + projectId + "/version";

                    HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setRequestProperty("User-Agent", plugin.getName());

                    JsonArray versions = JsonParser.parseReader(
                            new InputStreamReader(connection.getInputStream())
                    ).getAsJsonArray();

                    if (versions.isEmpty()) return;

                    JsonObject latest = versions.get(0).getAsJsonObject();
                    String latestVersion = latest.get("version_number").getAsString();

                    if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                        plugin.getLogger().info(
                                "A new version is available!"
                        );
                        plugin.getLogger().info(
                                "Current: " + currentVersion +
                                        " | Latest: " + latestVersion
                        );
                        plugin.getLogger().info(
                                "https://modrinth.com/plugin/simple-combatlog/versions"
                        );
                    }
                } catch (Exception ex) {
                    plugin.getLogger().log(
                            Level.WARNING,
                            "Could not check for updates on Modrinth",
                            ex
                    );
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}
