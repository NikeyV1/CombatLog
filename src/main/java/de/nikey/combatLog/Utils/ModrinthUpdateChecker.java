package de.nikey.combatLog.Utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.nikey.combatLog.CombatLog;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ModrinthUpdateChecker {

    private final JavaPlugin plugin;
    private final String projectId;

    public ModrinthUpdateChecker(String projectId) {
        this.plugin = CombatLog.getPlugin(CombatLog.class);
        this.projectId = projectId;
    }

    public void checkForUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                String currentVersion = plugin.getDescription().getVersion();
                String urlString = "https://api.modrinth.com/v2/project/" + projectId + "/version";

                try {
                    String latestVersion = fetchLatestVersionWithRetry(urlString, 2);
                    if (latestVersion == null) {
                        return;
                    }

                    if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                        plugin.getLogger().info("A new version is available!");
                        plugin.getLogger().info("Current: " + currentVersion + " | Latest: " + latestVersion);
                        plugin.getLogger().info("https://modrinth.com/plugin/simple-combatlog/versions");
                    }
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Could not check for updates on Modrinth", ex);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private String fetchLatestVersionWithRetry(String urlString, int retries) throws IOException {
        IOException last = null;

        for (int attempt = 0; attempt <= retries; attempt++) {
            try {
                return fetchLatestVersion(urlString);
            } catch (SocketTimeoutException timeout) {
                plugin.getLogger().fine("[UpdateCheck] Modrinth request timed out (attempt " + (attempt + 1) + "/" + (retries + 1) + ")");
                last = timeout;
            } catch (IOException io) {
                last = io;
                String msg = io.getMessage() == null ? "" : io.getMessage().toLowerCase();

                boolean looksTemporary = msg.contains("429") || msg.contains("503") || msg.contains("502") || msg.contains("504");
                if (!looksTemporary) {
                    throw io;
                }
            }

            if (attempt < retries) {
                try {
                    Thread.sleep(600L * (attempt + 1));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (last != null) {
            plugin.getLogger().fine("[UpdateCheck] Modrinth update check failed: " + last.getClass().getSimpleName() + " - " + last.getMessage());
        }
        return null;
    }

    private String fetchLatestVersion(String urlString) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);

        // Modrinth: sinnvoller UA (Name/Version)
        String ua = plugin.getName() + "/" + plugin.getDescription().getVersion();
        connection.setRequestProperty("User-Agent", ua);
        connection.setRequestProperty("Accept", "application/json");

        int code = connection.getResponseCode();
        InputStream stream = (code >= 200 && code < 300) ? connection.getInputStream() : connection.getErrorStream();

        if (code == 429) {
            throw new IOException("429 Rate Limited");
        }
        if (code < 200 || code >= 300) {
            String err = readAll(stream);
            throw new IOException("HTTP " + code + " from Modrinth: " + (err == null ? "" : err));
        }

        JsonElement parsed = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        JsonArray versions = parsed.getAsJsonArray();
        if (versions.isEmpty()) return null;

        JsonObject latest = versions.get(0).getAsJsonObject();
        return latest.has("version_number") ? latest.get("version_number").getAsString() : null;
    }

    private String readAll(InputStream in) {
        if (in == null) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (IOException ignored) {
            return null;
        }
    }
}