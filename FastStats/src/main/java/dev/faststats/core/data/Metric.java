package dev.faststats.core.data;

import com.google.gson.JsonElement;

import java.util.Optional;
import java.util.concurrent.Callable;

public interface Metric<T> {

    @SourceId

    String getId();

    Optional<T> compute() throws Exception;

    Optional<JsonElement> getData() throws Exception;

    static Metric<String[]> stringArray(@SourceId final String id, final Callable<String  []> callable) throws IllegalArgumentException {
        return new ArrayMetric<>(id, callable);
    }

    static Metric<Boolean[]> booleanArray(@SourceId final String id, final Callable<Boolean  []> callable) throws IllegalArgumentException {
        return new ArrayMetric<>(id, callable);
    }

    static Metric<Number[]> numberArray(@SourceId final String id, final Callable<Number  []> callable) throws IllegalArgumentException {
        return new ArrayMetric<>(id, callable);
    }

    static Metric<Boolean> bool(@SourceId final String id, final Callable< Boolean> callable) throws IllegalArgumentException {
        return new SingleValueMetric<>(id, callable);
    }

    static Metric<String> string(@SourceId final String id, final Callable< String> callable) throws IllegalArgumentException {
        return new SingleValueMetric<>(id, callable);
    }

    static Metric<Number> number(@SourceId final String id, final Callable< Number> callable) throws IllegalArgumentException {
        return new SingleValueMetric<>(id, callable);
    }
}
