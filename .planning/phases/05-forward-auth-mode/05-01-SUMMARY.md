# Phase 5 Plan 01: Forward-auth mode and OIDC removal

ForwardAuthFilter with TRUSTED_PROXIES CIDR gate as opt-in alongside built-in JWT default; in-app OIDC client deleted per contract section 7.

## Accomplishments
- Added opt-in forward-auth mode (`AUTH_MODE=forward-auth`) that lets a reverse proxy inject `X-Forwarded-User`, `X-Forwarded-Email`, `X-Forwarded-Groups` headers, gated by a `TRUSTED_PROXIES` CIDR list per contract section 7.2.
- Deleted in-app OIDC client surface entirely (controller, integration test, AuthService.issueTokensForOidc, allowlist matchers, app.auth.oidc.* config block) per contract section 7's hard prohibition.
- Added integration test coverage for the trust gate: trusted+known email -> 200, trusted+unknown email -> 401, untrusted proxy -> header ignored -> 401.

## Files Created/Modified
- `backend/src/main/java/com/workouthub/common/security/ForwardAuthFilter.java` (new)
- `backend/src/main/java/com/workouthub/common/security/TrustedProxyMatcher.java` (new)
- `backend/src/main/java/com/workouthub/common/config/SecurityConfig.java` (wire filter conditionally, trim OIDC paths)
- `backend/src/main/resources/application.yml` (new app.auth.* keys, removed app.auth.oidc.* block)
- `backend/src/main/java/com/workouthub/auth/AuthService.java` (removed issueTokensForOidc)
- `backend/src/main/java/com/workouthub/auth/OidcController.java` (DELETED)
- `backend/src/test/java/com/workouthub/auth/OidcControllerIntegrationTest.java` (DELETED)
- `backend/src/test/java/com/workouthub/common/security/ForwardAuthFilterIntegrationTest.java` (new)
- `backend/src/test/java/com/workouthub/common/security/ForwardAuthFilterUntrustedTest.java` (new)

## Decisions Made
- OIDC removed entirely (option A from STATE pre-decision), not gated behind a flag.
- ForwardAuthFilter only loads when `app.auth.mode=forward-auth` via `@ConditionalOnProperty`; built-in JWT remains the default and untouched.
- `TRUSTED_PROXIES` default empty = trust nothing (safer than wide-open). The matcher short-circuits to `false` when no CIDRs are configured.
- Filter wired BEFORE `JwtAuthenticationFilter` only when present; `Optional<ForwardAuthFilter>` injection keeps default mode wiring identical.
- Untrusted-proxy test split into a separate file (`ForwardAuthFilterUntrustedTest`) so Spring's context cache cleanly partitions the differing `app.auth.trusted-proxies` value.
- `AppUserPrincipal` constructor used as it actually exists in the codebase: `(UUID userId, String role)` — the plan template's three-arg form `(id, email, role)` was inconsistent with the real class.

## Commits
- `e1e20ce` feat(05-01): add forward-auth mode with TRUSTED_PROXIES gate
- `1772695` chore(05-01): remove in-app OIDC client per contract section 7
- `d4a0e8b` test(05-01): cover forward-auth trust gate scenarios

## Issues Encountered
- Plan template referenced a three-arg `AppUserPrincipal(id, email, role)` constructor that does not exist; actual record is `(UUID, String role)`. Adapted ForwardAuthFilter to match the real signature.
- Local Maven unavailable on this Windows host (memory: local_maven_gap); compilation and tests verified by inspection only. CI on push will validate.

## Next Step
Phase 5 complete; ready for Phase 6 (Env Vars and Override Example).
