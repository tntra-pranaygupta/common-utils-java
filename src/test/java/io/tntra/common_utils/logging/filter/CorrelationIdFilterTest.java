package io.tntra.common_utils.logging.filter;

import io.tntra.common_utils.util.CorrelationIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        
        MDC.clear();
        CorrelationIdHolder.setCurrentCorrelationId(null);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        CorrelationIdHolder.setCurrentCorrelationId(null);
    }

    /**
     * Should use provided correlation ID from header and clean up state after filter execution.
     */
    @Test
    void doFilterInternalWithExistingHeaderShouldUseProvidedIdTest() throws ServletException, IOException {
        String existingId = "client-trace-123";
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        
        FilterChain mockChain = mock(FilterChain.class);
        
        filter.doFilterInternal(request, response, mockChain);
        
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(existingId);
        
        verify(mockChain).doFilter(request, response);
        
        assertThat(MDC.get(CorrelationIdHolder.CORRELATION_ID_KEY)).isNull();
        assertThat(CorrelationIdHolder.getCurrentCorrelationId()).isNull();
    }

    /**
     * Should generate a new correlation ID if header is missing and clean up state after filter execution.
     */
    @Test
    void doFilterInternalWithNoHeaderShouldGenerateNewIdTest() throws ServletException, IOException {
        FilterChain mockChain = mock(FilterChain.class);
        
        filter.doFilterInternal(request, response, mockChain);
        
        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedId).isNotBlank().hasSizeGreaterThan(10);
        
        verify(mockChain).doFilter(request, response);
        
        assertThat(MDC.get(CorrelationIdHolder.CORRELATION_ID_KEY)).isNull();
        assertThat(CorrelationIdHolder.getCurrentCorrelationId()).isNull();
    }
    
    /**
     * Should expose MDC and CorrelationIdHolder during filter execution and clean up after.
     */
    @Test
    void doFilterInternalShouldExposeMdcWhileExecutingTest() throws ServletException, IOException {
        String existingId = "client-trace-123";
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        
        FilterChain customChain = (req, res) -> {
            assertThat(MDC.get(CorrelationIdHolder.CORRELATION_ID_KEY)).isEqualTo(existingId);
            assertThat(CorrelationIdHolder.getCurrentCorrelationId()).isEqualTo(existingId);
        };
        
        filter.doFilterInternal(request, response, customChain);
        
        assertThat(MDC.get(CorrelationIdHolder.CORRELATION_ID_KEY)).isNull();
        assertThat(CorrelationIdHolder.getCurrentCorrelationId()).isNull();
    }
}
