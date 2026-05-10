package com.workouthub.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SecurityConfigTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void protectedApiReturns401WithoutAuth() throws Exception {
        mvc.perform(get("/api/anything"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflightForConfiguredOriginIsAllowed() throws Exception {
        mvc.perform(options("/api/anything")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void corsPreflightForUnknownOriginIsRejected() throws Exception {
        mvc.perform(options("/api/anything")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
