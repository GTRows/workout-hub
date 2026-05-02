package com.workouthub.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Exercises the real Spring Boot 3.4 native ECS structured logging pipeline
 * with the {@code prod} profile active. Two contract assertions:
 *
 * <ol>
 *   <li>JSON shape: stdout lines are valid JSON containing {@code message}
 *       and an ISO-8601 {@code @timestamp}.</li>
 *   <li>Deny-list masking: an MDC entry whose key is in the deny-list
 *       (e.g. {@code authorization}) never leaks its value into the JSON
 *       output - it is either dropped or replaced with {@code <redacted>}.</li>
 * </ol>
 *
 * <p>The test boots a minimal {@code @SpringBootTest} configuration with no
 * JPA / DataSource so the prod profile can be activated without a Postgres
 * Testcontainer or database properties.
 */
@SpringBootTest(
        classes = StructuredLoggingTest.MinimalConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("prod")
class StructuredLoggingTest {

    private static final Logger log = LoggerFactory.getLogger(StructuredLoggingTest.class);

    private final ByteArrayOutputStream stdoutBuf = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @SpringBootConfiguration
    static class MinimalConfig {
    }

    @BeforeEach
    void captureStdout() {
        originalOut = System.out;
        System.setOut(new PrintStream(
                new TeeOutputStream(originalOut, stdoutBuf), true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    @Test
    void prodEmitsJsonWithMessageAndTimestamp() throws Exception {
        log.info("hello structured world");
        String line = lastLineMatching(stdoutBuf.toString(StandardCharsets.UTF_8),
                "hello structured world");
        assertThat(line).as("captured JSON line").isNotNull();

        JsonNode json = new ObjectMapper().readTree(line);
        assertThat(json.get("message").asText()).isEqualTo("hello structured world");
        assertThat(json.get("@timestamp").asText())
                .matches("\\d{4}-\\d{2}-\\d{2}T.*");
    }

    @Test
    void prodMasksDenyListedMdcFields() {
        try {
            MDC.put("authorization", "Bearer secret-token-value");
            log.info("login attempt with authorization mdc");
        } finally {
            MDC.remove("authorization");
        }
        String captured = stdoutBuf.toString(StandardCharsets.UTF_8);
        // The raw secret value must never appear in any captured stdout line.
        assertThat(captured).doesNotContain("Bearer secret-token-value");
    }

    private static String lastLineMatching(String captured, String needle) {
        String[] lines = captured.split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("{") && trimmed.contains(needle)) {
                return trimmed;
            }
        }
        return null;
    }

    /**
     * Minimal Tee stream so test output still flows to the real console while
     * also being captured for assertions. Inlined to avoid pulling in
     * Apache Commons IO.
     */
    private static final class TeeOutputStream extends OutputStream {
        private final OutputStream a;
        private final OutputStream b;

        TeeOutputStream(OutputStream a, OutputStream b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void write(int byteValue) throws IOException {
            a.write(byteValue);
            b.write(byteValue);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            a.write(buf, off, len);
            b.write(buf, off, len);
        }

        @Override
        public void flush() throws IOException {
            a.flush();
            b.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                a.close();
            } finally {
                b.close();
            }
        }
    }
}
