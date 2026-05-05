package com.backend.nmcomputercare.utils;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralised exception factory and logger.
 *
 * <p>Call {@link #throwExceptionAndAdvice} to:
 * <ol>
 *   <li>Record the exception timestamp in {@link ExceptionHandlerReporter}.</li>
 *   <li>Log a structured warning containing the error message and resolution hint.</li>
 *   <li>Return the exception so the caller can {@code throw} it inline.</li>
 * </ol>
 *
 * <p>The method returns the exception rather than throwing it so that the
 * compiler sees a guaranteed throw at the call-site, allowing methods with
 * non-void return types to compile without a dummy return statement:
 * <pre>{@code throw advice.throwExceptionAndAdvice(new SomeException(msg), msg, hint); }</pre>
 */
@AllArgsConstructor
@NoArgsConstructor
@Component
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Optional helper; may be null when this bean is instantiated directly. */
    private CommonMethods commonMethod;

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Records, logs, and returns {@code ex} ready to be thrown by the caller.
     *
     * @param ex                  the exception to record (may be {@code null};
     *                            a generic {@link RuntimeException} is used as fallback)
     * @param errorMessage        human-readable description of what went wrong
     * @param resolveIssueDetails advice on how the caller can resolve the issue
     * @return the (non-null) exception instance
     */
    public RuntimeException throwExceptionAndAdvice(
            RuntimeException ex,
            String           errorMessage,
            String           resolveIssueDetails) {

        LocalDateTime thrownAt = LocalDateTime.now();

        // Guard: never let a null exception reach the throw statement.
        RuntimeException safeEx = (ex != null) ? ex : new RuntimeException(errorMessage);

        try {
            ExceptionHandlerReporter.setExceptionDate(thrownAt);
            ExceptionHandlerReporter.setResolveIssueDetails(resolveIssueDetails);
            throw safeEx;

        } catch (RuntimeException reported) {
            logger.warn(
                    "Exception raised | type={} thrownAt={} message={} resolution={}",
                    reported.getClass().getSimpleName(),
                    formatSafe(thrownAt),
                    errorMessage,
                    resolveIssueDetails);
            return reported;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Formats a datetime; falls back to the static formatter if commonMethod is absent. */
    private String formatSafe(LocalDateTime dt) {
        if (commonMethod != null) {
            try { return commonMethod.formatDateTime(dt); } catch (Exception ignored) {}
        }
        return dt.format(FMT);
    }
}