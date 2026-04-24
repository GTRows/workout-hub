package com.workouthub.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void assignsFreshTraceIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse res = new MockHttpServletResponse();
        CapturingChain chain = new CapturingChain();

        filter.doFilter(req, res, chain);

        assertThat(chain.capturedMdc)
                .as("MDC trace_id is set during chain.doFilter")
                .isNotNull()
                .isNotBlank();
        assertThat(res.getHeader(TraceIdFilter.HEADER)).isEqualTo(chain.capturedMdc);
        // Cleared after the chain.
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void propagatesIncomingTraceIdHeader() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        req.addHeader(TraceIdFilter.HEADER, "trace-from-upstream");
        MockHttpServletResponse res = new MockHttpServletResponse();
        CapturingChain chain = new CapturingChain();

        filter.doFilter(req, res, chain);

        assertThat(chain.capturedMdc).isEqualTo("trace-from-upstream");
        assertThat(res.getHeader(TraceIdFilter.HEADER)).isEqualTo("trace-from-upstream");
    }

    private static final class CapturingChain extends MockFilterChain {
        String capturedMdc;

        @Override
        public void doFilter(
                jakarta.servlet.ServletRequest request,
                jakarta.servlet.ServletResponse response) throws ServletException, IOException {
            capturedMdc = MDC.get(TraceIdFilter.MDC_KEY);
            super.doFilter(request, response);
        }
    }
}
