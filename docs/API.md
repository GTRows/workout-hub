# API

WorkoutHub exposes a REST API under `/api/**`. The runtime SpringDoc surface is
the authoritative reference: a live OpenAPI 3.x document at
`GET /v3/api-docs` (JSON, YAML at `/v3/api-docs.yaml`) and an interactive
Swagger UI at `GET /swagger-ui.html`. This document is the navigational entry
point — it links to the runtime catalog, summarizes the auth model and the
project-wide error envelope, and points readers at the per-section contracts
(export wire shape, operator endpoints, self-hosted contract). It does not
attempt to re-document every endpoint by hand; the runtime document is the
single source of truth and the [`OpenApiSurfaceIntegrationTest`](../backend/src/test/java/com/workouthub/common/web/OpenApiSurfaceIntegrationTest.java)
asserts its structural shape on every CI run.

## Runtime endpoints

The OpenAPI document is rendered by SpringDoc 2.x against every
`@RestController` under `/api/**`. Operator-facing endpoints (`/livez`,
`/healthz`, `/metrics`, `/actuator/**`) are excluded from the document via
`springdoc.paths-to-match=/api/**` and stay documented in
[`OBSERVABILITY.md`](OBSERVABILITY.md).

| Endpoint | Purpose |
|----------|---------|
| `GET /v3/api-docs` | OpenAPI 3.x JSON document. The canonical machine-readable contract. |
| `GET /v3/api-docs.yaml` | Same document, YAML serialization. |
| `GET /swagger-ui.html` | Bundled interactive Swagger UI; redirects to `/swagger-ui/index.html`. |

The document declares two security schemes (`bearerAuth` and `forwardAuth`;
see [Authentication](#authentication)), 28 hand-curated tag groupings (one per
user- or admin-facing controller), and the project-wide `ApiError` envelope
schema (see [Error envelope](#error-envelope)). For client codegen, point your
tool of choice (openapi-generator, kiota, etc.) at the JSON URL:

```bash
curl -s http://127.0.0.1:8080/v3/api-docs | jq '.info, (.tags | length), (.paths | length)'
```

## Authentication

WorkoutHub supports the two-mode auth model defined in
[`SELF_HOSTED_CONTRACT.md` Section 7](SELF_HOSTED_CONTRACT.md#7-authentication).
The OpenAPI document declares both schemes; runtime behavior depends on
`APP_AUTH_MODE`.

### Built-in JWT (default)

Default mode (`APP_AUTH_MODE=builtin`). Clients obtain a token from
`POST /api/auth/login` and present it on subsequent requests:

```http
Authorization: Bearer <jwt>
```

OpenAPI scheme: `bearerAuth` (`type: http`, `scheme: bearer`,
`bearerFormat: JWT`). Applied globally; the permitAll endpoints
(`POST /api/auth/login`, `POST /api/auth/refresh`,
`POST /api/auth/reset-password`, and the HMAC-token-gated
`POST /api/webhooks/scale/**`) are reachable without a Bearer token. See
[contract Section 7.1](SELF_HOSTED_CONTRACT.md#71-built-in-auth-default).

### Reverse-proxy forward-auth (opt-in)

Set `APP_AUTH_MODE=forward-auth` when an SSO gateway (Authentik, Authelia,
Keycloak, oauth2-proxy) terminates auth in front of the app. The application
trusts identity headers injected by the proxy:

```
APP_AUTH_HEADER_USER=X-Forwarded-User
APP_AUTH_HEADER_EMAIL=X-Forwarded-Email
APP_AUTH_HEADER_GROUPS=X-Forwarded-Groups
APP_AUTH_TRUSTED_PROXIES=10.0.0.0/8,100.64.0.0/10
```

The headers are trusted **only** when the request source IP matches
`APP_AUTH_TRUSTED_PROXIES`. This is the single safety mechanism that prevents
header injection from external clients; OpenAPI 3.x cannot model the source-IP
gate, so the document declares the `forwardAuth` apiKey scheme (header
`X-Forwarded-Email`) and leaves the trust gate to the operator. See
[contract Section 7.2](SELF_HOSTED_CONTRACT.md#72-reverse-proxy-forward-auth-opt-in).

The application does not ship an OIDC client, an OAuth flow, or a session
backed by an external identity provider — that layer lives in the operator's
reverse proxy.

## Error envelope

Every `4xx` and `5xx` response uses the project-wide `ApiError` envelope. The
shape is rendered to OpenAPI under `components.schemas.ApiError` with each
field annotated; the field-level validation case adds an `errors[]` array of
`FieldError` records.

```json
{
  "timestamp": "2026-05-06T12:34:56Z",
  "status": 409,
  "error": "Conflict",
  "message": "Session already finished",
  "path": "/api/sessions/123/sets",
  "code": "SESSION_FINISHED"
}
```

The `code` field is a typed string populated for conflict responses tied to
known business rules. Phase 15-04 catalogued four values; the OpenAPI schema
enumerates them as `allowableValues` on `ApiError.code`:

| Code | HTTP | Meaning |
|------|------|---------|
| `SESSION_ALREADY_ACTIVE` | 409 | Attempt to start a workout session while another is still active for the same user. |
| `SESSION_ALREADY_FINISHED` | 409 | Attempt to finish a session that has already been finalized. |
| `SESSION_FINISHED` | 409 | Mutation attempt (set add/update) against a finished session. |
| `SET_NUMBER_DUPLICATE` | 409 | Set number collision within the same session-exercise pair. |

`code` may be `null` on legacy conflict paths (eight `ConflictException`
throw sites today emit a 409 without a typed code; Jackson `NON_NULL` strips
the field from the wire). Tooling should branch on `status` and `code`, not
on the human-readable `message` field, which is not stable across releases.

## Export format

The full-export and import endpoints (`GET /api/export/full`,
`POST /api/export/import`) ship a self-describing JSON payload designed for
round-trip with chat-style LLMs. The wire shape, enum values, round-trip
preservation rules, and acceptable drift are documented in
[`EXPORT_FORMAT.md`](EXPORT_FORMAT.md). The runtime OpenAPI document
references the same DTOs (`FullExportDto`, `ClaudeSummaryDto`) under
`components.schemas`.

## Operator endpoints

Operator-facing endpoints are intentionally excluded from the OpenAPI
document and live in [`OBSERVABILITY.md`](OBSERVABILITY.md):

- `GET /livez` — process is alive.
- `GET /healthz` — application is ready to serve traffic (DB reachable,
  migrations applied).
- `GET /metrics` — Prometheus scrape on the main listener.
- `GET /actuator/health` — Spring Boot Actuator health endpoint.
- `GET /actuator/info` — build info (version, commit).
- `GET /actuator/prometheus` — Spring Boot Actuator's Prometheus exporter.

These paths are `permitAll` at the application layer per
[`SELF_HOSTED_CONTRACT.md` Section 9](SELF_HOSTED_CONTRACT.md#9-metrics-and-health);
their access is bounded by `BIND_ADDR` and the operator's reverse proxy, not
by application auth.

## Discoverability

The OpenAPI JSON and Swagger UI are reachable without authentication by
design. The application's public-network exposure is bounded at the operator
layer:

- `BIND_ADDR=127.0.0.1` (compose default) restricts the listener to the
  loopback interface; only the operator's host can reach the OpenAPI surface.
- Operators who set `BIND_ADDR=0.0.0.0` (against the contract) accept the
  discoverability risk explicitly.
- The reverse-proxy layer (Caddy, Traefik, nginx — see [`../README.md`](../README.md)
  "Exposure" section) decides whether `/v3/api-docs` and `/swagger-ui.html`
  are reachable from the public DNS name. Restrict by path, by source IP, or
  by basic-auth at the proxy if the deployment requires it.

This stance is the v0.4 verdict from the [Phase 19
audit](../.planning/phases/19-api-contract-docs/19-01-AUDIT.md) Section 4D,
re-confirmed at v1.0 (Phase 42 plan 01) without source change.

## Related

- [`DATA_SCHEMA.md`](DATA_SCHEMA.md) — Postgres schema overview, migration
  history, and the per-user uniqueness constraints that back
  `SET_NUMBER_DUPLICATE`, `SESSION_ALREADY_ACTIVE`, and the
  `MetricsService` 200/201 upsert split.
