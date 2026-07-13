package dev.faststats.core;

import dev.faststats.core.data.Metric;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public interface Metrics {

    @Token

    String getToken();

    Optional<ErrorTracker> getErrorTracker();

    Config getConfig();

    default void ready() {
    }

    void shutdown();

    interface Factory<T, F extends Factory<T, F>> {

        F addMetric(Metric<?> metric) throws IllegalArgumentException;

        F onFlush(Runnable flush);

        F errorTracker(ErrorTracker tracker);

        F debug(boolean enabled);

        F token(@Token String token) throws IllegalArgumentException;

        F url(URI url);

        Metrics create(T object) throws IllegalStateException;
    }

    interface Config {

        UUID serverId();

        boolean enabled();

        boolean errorTracking();

        boolean additionalMetrics();

        boolean debug();
    }
}
