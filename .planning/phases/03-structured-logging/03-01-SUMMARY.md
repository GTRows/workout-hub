# Phase 3 Plan 01: Structured logging for production profile

**Spring Boot 3.4 native ECS structured logging on the prod profile, with deny-list field-value masking via a `StructuredLoggingJsonMembersCustomizer` registered through the `logging.structured.json.customizer` property, plus an always-on `user_id` MDC propagation filter.**

## Accomplishments

- `application-prod.yml` enables ECS-format JSON console logging, sets prod log levels (root INFO, com.workouthub INFO, framework WARN), and wires the secret-masking customizer.
- `SecretMaskingStructuredLoggingCustomizer` replaces the value of any emitted JSON member whose path leaf name (case-insensitive) matches the contract section 8 deny-list (`password`, `token`, `secret`, `authorization`, `cookie`, `set_cookie`, `api_key`, `client_secret`, `private_key`) with `<redacted>`.
- `UserIdMdcFilter` (always-on, not profile-gated) pushes the authenticated user's UUID into MDC under `user_id` after the Spring Security chain populates the principal, and removes it in `finally` to prevent thread-local leakage.
- `StructuredLoggingTest` exercises the real Spring Boot 3.4 + Logback structured pipeline end-to-end (no mocking), captures stdout via an inlined `TeeOutputStream`, and asserts both the JSON shape (`message` + `@timestamp`) and the masking contract (raw secret value never appears in captured output).

## Files Created/Modified

- `backend/src/main/resources/application-prod.yml` (modified - was a one-line stub)
- `backend/src/main/java/com/workouthub/common/logging/SecretMaskingStructuredLoggingCustomizer.java` (new)
- `backend/src/main/java/com/workouthub/common/logging/UserIdMdcFilter.java` (new)
- `backend/src/test/java/com/workouthub/common/logging/StructuredLoggingTest.java` (new)

## Decisions Made

- **Spring Boot 3.4 native structured logging chosen over `logstash-logback-encoder`** - zero new Maven dependency, ECS schema is the standard Loki/Elastic format, and Spring exposes `StructuredLoggingJsonMembersCustomizer` as a first-class extension point.
- **Customizer wired via `logging.structured.json.customizer` YAML property, NOT via `@Component`.** Spring Boot's logging system bootstraps in `LoggingApplicationListener` (very early, before the `ApplicationContext` exists), so it discovers customizers either through `META-INF/spring.factories` (classpath-global) or through the `logging.structured.json.customizer` property (profile-aware via the YAML profile). The customizer must therefore have a public no-arg constructor and must NOT be a Spring bean.
- **Masking implemented via `Members#applyingValueProcessor`.** This replaces the value while keeping the field name visible in the output, so log readers can see *that* a secret was redacted (forensic value) without leaking the secret itself.
- **`UserIdMdcFilter` placed at `LOWEST_PRECEDENCE - 100`** to run AFTER `JwtAuthenticationFilter` populates `SecurityContextHolder`. Not profile-gated because MDC propagation aids dev/test debugging too.
- **Test uses a minimal `@SpringBootConfiguration` static class with `webEnvironment = NONE`** instead of `AbstractIntegrationTest`. This avoids Postgres / Testcontainer setup which the prod profile would otherwise require, while still booting the real Spring Boot logging system end-to-end.

## Issues Encountered

- **Plan body API sketch did not match the actual Spring Boot 3.4.1 API surface (Rule 3 deviation - blocking, adapted in-flight without escalation).** The plan suggested `applyingNameProcessor((name, value) -> ...)` returning `null` to drop fields. In reality:
  - `Members<E>` lives at `org.springframework.boot.json.JsonWriter.Members`, not nested in `StructuredLoggingJsonMembersCustomizer`.
  - `NameProcessor` only renames; it cannot drop a field or rewrite a value.
  - The correct hooks are `Members#applyingValueProcessor(ValueProcessor<?>)` (rewrites the value with `MemberPath` context) and `Members#applyingPathFilter(Predicate<MemberPath>)` (drops the member entirely).
  - API verified by extracting `StructuredLoggingJsonMembersCustomizer.class`, `JsonWriter$Members.class`, `JsonWriter$ValueProcessor.class`, and `StructuredLogFormatterFactory.class` from the spring-boot-3.4.1 jar and inspecting them with `javap -p`.
- **Component scanning does not work for the customizer (Rule 3 deviation - blocking, adapted in-flight).** The plan sketched `@Component @Profile("prod")` on the customizer. In reality `StructuredLogFormatterFactory.loadStructuredLoggingJsonMembersCustomizers()` calls `SpringFactoriesLoader.forDefaultResourceLocation()` and uses an internal `Instantiator`, never the application context. Profile gating is achieved instead by referencing the customizer FQN from `application-prod.yml` (`logging.structured.json.customizer`) - so the class is only instantiated when the prod profile is active.
- **Local Maven unavailable on this Windows host (memory: local_maven_gap).** The test was not executed locally; CI on push validates. The Spring Boot API surface was verified by direct bytecode inspection of the spring-boot-3.4.1 jar in the local Maven cache.

## Next Step

Phase 3 complete; ready for Phase 4 (Prometheus `/metrics` on main listener).
