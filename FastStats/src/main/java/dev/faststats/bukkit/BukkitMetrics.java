package dev.faststats.bukkit;

import dev.faststats.core.Metrics;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;

public interface BukkitMetrics extends Metrics {

    static Factory factory() {
        return new BukkitMetricsImpl.Factory();
    }

    @Override
    void ready() throws IllegalPluginAccessException;

    interface Factory extends Metrics.Factory<Plugin, Factory> {
        @Override
        BukkitMetrics create(Plugin object) throws IllegalStateException;
    }
}
