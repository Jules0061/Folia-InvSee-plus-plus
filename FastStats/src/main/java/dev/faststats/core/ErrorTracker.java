package dev.faststats.core;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public interface ErrorTracker {

    static ErrorTracker contextAware() {
        final SimpleErrorTracker tracker = new SimpleErrorTracker();
        tracker.attachErrorContext(ErrorTracker.class.getClassLoader());
        return tracker;
    }

    static ErrorTracker contextUnaware() {
        return new SimpleErrorTracker();
    }

    void trackError(String message);

    void trackError(Throwable error);

    void trackError(String message, boolean handled);

    void trackError(Throwable error, boolean handled);

    ErrorTracker ignoreErrorType(Class<? extends Throwable> type);

    ErrorTracker ignoreError(Pattern pattern);

    default ErrorTracker ignoreError( final String pattern) {
        return ignoreError(Pattern.compile(pattern));
    }

    ErrorTracker ignoreError(Class<? extends Throwable> type, Pattern pattern);

    default ErrorTracker ignoreError(final Class<? extends Throwable> type,  final String pattern) {
        return ignoreError(type, Pattern.compile(pattern));
    }

    void attachErrorContext( ClassLoader loader) throws IllegalStateException;

    void detachErrorContext();

    boolean isContextAttached();

    void setContextErrorHandler( BiConsumer< ClassLoader, Throwable> errorEvent);

    Optional<BiConsumer< ClassLoader, Throwable>> getContextErrorHandler();

    static boolean isSameLoader(final ClassLoader loader, final Throwable error) {
        return ErrorHelper.isSameLoader(loader, error);
    }
}
