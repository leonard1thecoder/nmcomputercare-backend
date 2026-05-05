package com.backend.nmcomputercare.utils;

import com.backend.nmcomputercare.utils.exceptions.ServiceTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Abstract base class for controllers that want timeout + retry behaviour
 * around any {@link ExecuteService#callable} invocation.
 *
 * <h3>Retry policy</h3>
 * <ul>
 *   <li>Each attempt times out after {@value #TIMEOUT_SECONDS} seconds.</li>
 *   <li>On timeout the future is cancelled and the thread sleeps
 *       {@value #RETRY_PAUSE_MS} ms before the next attempt.</li>
 *   <li>After {@value #MAX_RETRIES} failed attempts a {@link ServiceTimeoutException}
 *       is thrown; the controller advice turns this into a 503 response.</li>
 * </ul>
 *
 * <h3>MDC propagation</h3>
 * The current MDC context is copied into the worker thread so that
 * {@code correlationId} / {@code serviceName} appear in every log line,
 * including those emitted from the service layer running in the pool thread.
 */
public abstract class ExecService {

    private static final Logger logger = LoggerFactory.getLogger(ExecService.class);

    private static final long TIMEOUT_SECONDS = 30L;
    private static final int  MAX_RETRIES     = 3;
    private static final long RETRY_PAUSE_MS  = 5_000L;

    private final ExecutorService  exec;
    private final ExceptionAdvice  advice;

    protected ExecService(ExecutorService exec, ExceptionAdvice advice) {
        this.exec   = exec;
        this.advice = advice;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Core dispatch
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Submits a {@code service.callable(serviceName, request)} call to the
     * shared thread pool, waiting up to {@value #TIMEOUT_SECONDS} s per attempt.
     *
     * @param service     the target {@link ExecuteService}
     * @param serviceName logical operation name forwarded to the service
     * @param request     the request contract
     * @return the list returned by the service, or a single-element
     *         {@link ErrorResponse} list on repeated timeouts
     */
    protected List<? extends ResponseContract> exec(
            ExecuteService service,
            String         serviceName,
            RequestContract request) {

        // Snapshot MDC so the worker thread inherits the same correlation data.
        Map<String, String> mdcSnapshot = MDC.getCopyOfContextMap();

        int attempt = 0;

        while (true) {
            attempt++;
            logger.info("Submitting task | service={} attempt={}/{}", serviceName, attempt, MAX_RETRIES);

            Future<List<? extends ResponseContract>> future = exec.submit(
                    () -> withMdc(mdcSnapshot, () -> service.callable(serviceName, request)));

            try {
                List<? extends ResponseContract> result = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                logger.info("Task completed | service={} attempt={}", serviceName, attempt);
                return result;

            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                logger.error("Thread interrupted | service={}", serviceName, e);
                throw advice.throwExceptionAndAdvice(
                        new ServiceTimeoutException("Request interrupted."),
                        "Request was interrupted.",
                        "Retry the request. If the problem persists contact support.");

            } catch (ExecutionException e) {
                future.cancel(true);
                // Unwrap: the real exception was thrown inside the task.
                Throwable cause = e.getCause();
                logger.error("Execution error | service={} cause={}", serviceName, cause.getMessage(), cause);
                if (cause instanceof RuntimeException re) throw re;
                throw new RuntimeException("Unexpected execution error: " + cause.getMessage(), cause);

            } catch (TimeoutException e) {
                future.cancel(true);
                logger.warn("Timeout on attempt {}/{} | service={}", attempt, MAX_RETRIES, serviceName);

                if (attempt >= MAX_RETRIES) {
                    return handleMaxRetriesExceeded(serviceName);
                }

                pauseBeforeRetry(serviceName, attempt);
                // Loop continues → next attempt.
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Private helpers
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Called after all retry attempts have been exhausted.
     * Returns a single {@link ErrorResponse} so callers receive a structured
     * error payload rather than an unhandled exception propagating to the client.
     */
    private List<ErrorResponse> handleMaxRetriesExceeded(String serviceName) {
        final String errorMsg   = String.format(
                "Service '%s' timed out after %d attempts.", serviceName, MAX_RETRIES);
        final String resolveMsg =
                "The server is taking too long to respond. Please try again in a moment.";

        logger.error("Max retries exceeded | service={} attempts={}", serviceName, MAX_RETRIES);

        try {
            throw advice.throwExceptionAndAdvice(
                    new ServiceTimeoutException(errorMsg), errorMsg, resolveMsg);
        } catch (ServiceTimeoutException ex) {
            // Return a typed error response instead of propagating — the caller
            // (controller) can decide how to surface this to the client.
            return List.of(new ErrorResponse(resolveMsg, LocalDateTime.now()));
        }
    }

    /** Sleep between retry attempts; logs and preserves interrupt status. */
    private void pauseBeforeRetry(String serviceName, int completedAttempts) {
        logger.info("Waiting {}ms before retry | service={} attempt={}/{}",
                RETRY_PAUSE_MS, serviceName, completedAttempts, MAX_RETRIES);
        try {
            Thread.sleep(RETRY_PAUSE_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.warn("Retry pause interrupted | service={}", serviceName);
        }
    }

    /**
     * Runs {@code task} with the given MDC context installed, then restores
     * (or clears) the previous context.  Ensures worker threads always carry
     * the same {@code correlationId} as the request thread.
     */
    private static <T> T withMdc(Map<String, String> snapshot, Callable<T> task) throws Exception {
        if (snapshot != null) {
            MDC.setContextMap(snapshot);
        }
        try {
            return task.call();
        } finally {
            MDC.clear();
        }
    }
}