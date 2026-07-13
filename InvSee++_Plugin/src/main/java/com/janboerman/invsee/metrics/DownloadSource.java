package com.janboerman.invsee.metrics;

import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public enum DownloadSource {

    SPIGOTMC,
    MODRINTH,
    GITHUB,
    HANGAR,
    UNKNOWN;

    @Override
    public String toString() {
        return switch (this) {
            case SPIGOTMC -> "SpigotMC";
            case MODRINTH -> "Modrinth";
            case HANGAR -> "Hangar";
            case GITHUB -> "GitHub";
            default -> "Unknown";
        };
    }

    private static DownloadSource fromString(String line) {
        if (line == null) return UNKNOWN;
        return switch (line) {
            case "SpigotMC" -> SPIGOTMC;
            case "Modrinth" -> MODRINTH;
            case "Hangar" -> HANGAR;
            case "GitHub" -> GITHUB;
            default -> UNKNOWN;
        };
    }

    public static DownloadSource detect(Plugin plugin) {
        try (InputStream inputStream = plugin.getClass().getResourceAsStream("/download-source.txt")) {
            if (inputStream == null) return UNKNOWN;
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                return fromString(bufferedReader.readLine());
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read download-source.txt from jar file.", e);
            return UNKNOWN;
        }
    }

}
