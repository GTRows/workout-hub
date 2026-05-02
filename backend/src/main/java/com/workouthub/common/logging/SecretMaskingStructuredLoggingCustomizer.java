package com.workouthub.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.Set;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * Replaces the value of any emitted JSON member whose path leaf name matches
 * the deny-list from docs/SELF_HOSTED_CONTRACT.md section 8 with the literal
 * string "&lt;redacted&gt;". Wired in only on the prod profile via the
 * {@code logging.structured.json.customizer} property in
 * {@code application-prod.yml} - dev/test profiles emit human-readable logs
 * and never reach this code.
 *
 * <p>Spring Boot 3.4 instantiates this class directly through its internal
 * {@code Instantiator} (not the Spring application context), so it must
 * expose a public no-arg constructor and must NOT be a {@code @Component}.
 *
 * <p>The deny-list is matched on the leaf segment of the JSON member path
 * (case-insensitive). This catches both top-level MDC keys and any nested
 * JSON object members the formatter emits with one of the deny-listed names.
 */
public class SecretMaskingStructuredLoggingCustomizer
        implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    private static final Set<String> DENY_LIST = Set.of(
            "password", "token", "secret", "authorization",
            "cookie", "set_cookie", "api_key", "client_secret", "private_key");
    private static final String REDACTED = "<redacted>";

    @Override
    public void customize(Members<ILoggingEvent> members) {
        members.applyingValueProcessor((path, value) -> {
            String leaf = path.name();
            if (leaf == null) {
                return value;
            }
            return DENY_LIST.contains(leaf.toLowerCase()) ? REDACTED : value;
        });
    }
}
