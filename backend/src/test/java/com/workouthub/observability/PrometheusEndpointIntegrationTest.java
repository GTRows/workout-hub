package com.workouthub.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class PrometheusEndpointIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void prometheusEndpointIsReachableWithoutAuthAndExposesAppTag() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("application=\"workouthub\"")));
    }

    @Test
    void prometheusEndpointReportsHttpServerRequestCounterAfterATrafficTouch() throws Exception {
        // Prime a metric by hitting an endpoint.
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());

        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("http_server_requests_seconds_count")));
    }
}
