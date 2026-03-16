package io.tntra.common_utils.logging.filter;

import io.tntra.common_utils.util.CorrelationIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates the SLF4J MDC with a {@code correlation_id}
 * for every incoming HTTP request.
 *
 * <h2>Behaviour</h2>
 * <ol>
 *   <li>Reads the {@code X-Correlation-Id} request header if present.</li>
 *   <li>Falls back to a freshly generated UUID v4 when the header is absent.</li>
 *   <li>Stores the value in both the MDC (for log patterns) and
 *       {@link CorrelationIdHolder} (for programmatic access in handlers).</li>
 *   <li>Propagates the correlation ID back to the caller via the
 *       {@code X-Correlation-Id} response header.</li>
 *   <li>Guarantees MDC cleanup via finally block to prevent thread-pool leakage.</li>
 * </ol>
 *
 * <h2>PCI/DSS note</h2>
 * The correlation ID is an opaque trace token — it must never contain PAN, PII,
 * or any card-holder data.
 *
 * <p>Order {@code Integer.MIN_VALUE} ensures this filter runs first, before any
 * security or business filter that may emit log statements.</p>
 */
@Component
@Order(Integer.MIN_VALUE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** HTTP header name as per common observability conventions. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Populate MDC so all log statements in this thread carry correlation_id
        MDC.put(CorrelationIdHolder.CORRELATION_ID_KEY, correlationId);
        // Also expose via holder so non-logging code can read it without MDC coupling
        CorrelationIdHolder.setCurrentCorrelationId(correlationId);

        // Echo the ID back to the caller for client-side tracing
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // CRITICAL: always clean up MDC to avoid value bleed between pooled threads
            MDC.remove(CorrelationIdHolder.CORRELATION_ID_KEY);
            CorrelationIdHolder.setCurrentCorrelationId(null);
        }
    }
}
