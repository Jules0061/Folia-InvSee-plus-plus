package com.janboerman.invsee.spigot.internal.resolve;

import java.util.ArrayList;
import java.util.List;

public enum ResolveStrategyType {

    ONLINE_PLAYER("online player"),

    LOGGED_OUT_PLAYERS_CACHE("logged out players cache"),

    PAPER_OFFLINE_PLAYER_CACHE("paper offline player cache"),

    PERMISSION_PLUGIN("permission plugin"),

    PROXY("proxy"),

    MOJANG_REST_API_CALL("mojang rest api call"),

    PLAYER_DATA_SAVE_FILES("player data save files"),

    SPOOF("spoof"),

    ELECTROID_REST_API_CALL("electroid mojang api call");

    private final String humanReadableName;

    private ResolveStrategyType(String configValue) {
        this.humanReadableName = configValue;
    }

    public static ResolveStrategyType fromString(String configValue) {
        for (ResolveStrategyType strategy : values()) {
            if (strategy.humanReadableName.equals(configValue)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unrecognized resolve strategy: " + configValue);
    }

    public static List<ResolveStrategyType> defaultStrategies(boolean onlineMode) {
        List<ResolveStrategyType> result = new ArrayList<>();
        result.add(ONLINE_PLAYER);
        result.add(LOGGED_OUT_PLAYERS_CACHE);
        result.add(PAPER_OFFLINE_PLAYER_CACHE);
        result.add(PERMISSION_PLUGIN);
        result.add(PROXY);
        if (onlineMode) {
            result.add(MOJANG_REST_API_CALL);
        } else {
            result.add(SPOOF);
        }
        result.add(PLAYER_DATA_SAVE_FILES);
        return result;
    }
}
