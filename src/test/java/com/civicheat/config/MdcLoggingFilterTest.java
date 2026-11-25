package com.civicheat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MdcLoggingFilterTest {

    private final MdcLoggingFilter filter = new MdcLoggingFilter();

    @Test
    @DisplayName("Assigns new request ID and clears MDC after filter completion")
    void doFilterInternal_generatesAndClearsMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            // Inside filter execution, MDC must contain requestId
            String currentRequestId = MDC.get(MdcLoggingFilter.REQUEST_ID_MDC_KEY);
            assertThat(currentRequestId).isNotBlank();
        });

        // After completion, MDC must be cleared
        assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID_MDC_KEY)).isNull();
        // Response header must contain the assigned request ID
        assertThat(response.getHeader(MdcLoggingFilter.REQUEST_ID_HEADER)).isNotBlank();
    }

    @Test
    @DisplayName("Propagates incoming X-Request-Id header value")
    void doFilterInternal_propagatesExistingHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcLoggingFilter.REQUEST_ID_HEADER, "custom-trace-id-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID_MDC_KEY)).isEqualTo("custom-trace-id-12345");
        });

        assertThat(response.getHeader(MdcLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("custom-trace-id-12345");
    }
}
