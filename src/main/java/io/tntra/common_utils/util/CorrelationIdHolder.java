package io.tntra.common_utils.util;

import org.slf4j.MDC;

/**
 * Utility for accessing the current request correlation ID from MDC.
 * The ID itself is managed by a servlet filter in the logging module.
 */


public final class CorrelationIdHolder {

    public static final String CORRELATION_ID_KEY = "correlation_id";

    private CorrelationIdHolder() {
    }

    public static String getCurrentCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    public static void setCurrentCorrelationId(String correlationId) {
        if (correlationId == null) {
            MDC.remove(CORRELATION_ID_KEY);
        } else {
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }
}
