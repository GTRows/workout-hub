package com.workouthub.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.WorkoutHubApplication;
import com.workouthub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

/**
 * Exercises the real Spring Boot 3.5 native ECS structured logging pipeline
 * with the prod profile active. Two contract assertions:
 *
 * <ol>
 *   <li>JSON shape: stdout lines are valid JSON containing message and an
 *       ISO-8601 @timestamp.</li>
 *   <li>Deny-list masking: an MDC entry whose key is in the deny-list
 *       (e.g. authorization) never leaks its value into the JSON output.</li>
 * </ol>
 *
 * <p>Boots the full {@link WorkoutHubApplication} via Testcontainers Postgres
 * (inherited from {@link AbstractIntegrationTest}) so Spring Boot's
 * {@code LoggingApplicationListener} reconfigures the logging system to ECS
 * format. The previous {@code @SpringBootConfiguration}-only minimal harness
 * (closed-out failure path documented in {@code .planning/ISSUES.md} i-9) did
 * not trigger that listener and therefore ran with the default human-readable
 * Logback layout.
 *
 * <p>Uses Spring Boot's OutputCaptureExtension so capture is wired before the
 * Logback appender starts. Setting System.out from a JUnit @BeforeEach does not
 * work because the ConsoleAppender holds the original PrintStream reference.
 */
@SpringBootTest(
        classes = WorkoutHubApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.main.banner-mode=off"})
@ActiveProfiles("prod")
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingTest extends AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(StructuredLoggingTest.class);

    @Test
    void prodEmitsJsonWithMessageAndTimestamp(CapturedOutput output) throws Exception {
        log.info("hello structured world");

        String line = lastLineMatching(output.getOut(), "hello structured world");
        assertThat(line).as("captured JSON line").isNotNull();

        JsonNode json = new ObjectMapper().readTree(line);
        assertThat(json.get("message").asText()).isEqualTo("hello structured world");
        assertThat(json.get("@timestamp").asText())
                .matches("\\d{4}-\\d{2}-\\d{2}T.*");
    }

    @Test
    void prodMasksDenyListedMdcFields(CapturedOutput output) {
        try {
            MDC.put("authorization", "Bearer secret-token-value");
            log.info("login attempt with authorization mdc");
        } finally {
            MDC.remove("authorization");
        }
        assertThat(output.getOut()).doesNotContain("Bearer secret-token-value");
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
}
