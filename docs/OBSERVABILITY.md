# Observability

WorkoutHub plugs into the homelab observability stack running on the
tailscale node at `100.104.41.21`: Prometheus, Alertmanager, Loki,
Grafana, node-exporter, and cAdvisor (all under
`homelab/stacks/observability/`). This doc captures every integration
point so you can wire the backend into that stack without hunting.

## Prometheus metrics

The backend exposes `/actuator/prometheus` publicly (no auth) so the
homelab Prometheus can scrape it. Metrics include JVM memory, GC,
HikariCP pool, Spring Web request rate and p95/p99 latency, plus any
custom counters we add through Micrometer.

### Scrape job

Append this to
`homelab/stacks/observability/config/prometheus/prometheus.yml` under
`scrape_configs:`:

```yaml
  - job_name: workouthub
    metrics_path: /actuator/prometheus
    scrape_interval: 15s
    static_configs:
      - targets: ['<workouthub-host>:8080']
        labels:
          app: workouthub
          env: home
```

Reload Prometheus:

```sh
docker compose -f homelab/stacks/observability/docker-compose.yml \
    exec prometheus wget -qO- --post-data='' http://localhost:9090/-/reload
```

### Example useful queries

- Request rate: `sum(rate(http_server_requests_seconds_count{app="workouthub"}[1m]))`
- 5xx rate: `sum(rate(http_server_requests_seconds_count{app="workouthub",status=~"5.."}[1m]))`
- p95 latency: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{app="workouthub"}[5m])) by (le,uri))`
- DB pool in use: `hikaricp_connections_usage{app="workouthub",state="active"}`

## Logs -> Loki

The production profile emits structured JSON logs on stdout with a
`trace_id` MDC field on every event. Ship them to Loki by setting the
docker logging driver on the backend service:

```yaml
# compose.prod.yml - backend service
logging:
  driver: loki
  options:
    loki-url: http://100.104.41.21:3100/loki/api/v1/push
    loki-retries: "5"
    loki-batch-size: "400"
    labels: app,env,component
    env: env
    labels-regex: ".*"
```

Set the labels on the container:

```yaml
    labels:
      app: workouthub
      component: backend
```

The Loki driver plugin must be installed on the host:

```sh
docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions
```

## Grafana dashboard

Import `observability/grafana/workouthub.json` into the homelab
Grafana (at `http://100.104.41.21:3000`) via **Dashboards -> Import**.
Or copy the JSON to
`homelab/stacks/observability/config/grafana/provisioning/dashboards/workouthub.json`
to pick it up on next Grafana restart.

The dashboard expects the Prometheus datasource to be named
`Prometheus` and the Loki datasource `Loki`.

## Alertmanager rules

Copy `observability/alerts/workouthub.rules.yml` to
`homelab/stacks/observability/config/prometheus/rules/` and add the
filename to the `rule_files:` block in `prometheus.yml`:

```yaml
rule_files:
  - /etc/prometheus/rules/workouthub.rules.yml
```

Validate before reloading:

```sh
docker compose -f homelab/stacks/observability/docker-compose.yml \
    exec prometheus promtool check rules /etc/prometheus/rules/workouthub.rules.yml
```

## Uptime-Kuma

Import `observability/uptime-kuma/workouthub-monitor.json` via
Uptime-Kuma's **Settings -> Backup -> Import**. It creates two
monitors:

1. `/actuator/health` with a `"status":"UP"` keyword check at 60s.
2. `/actuator/prometheus` with an `application="workouthub"` keyword
   check at 120s - confirms the Prometheus scrape path stays open.

Or add manually:

- **Monitor type**: HTTP(s)
- **URL**: `https://<your-domain>/actuator/health`
- **Heartbeat interval**: 60s
- **Accepted status codes**: 200
- **Keyword**: `"status":"UP"`

## End-to-end smoke

After the stack is up:

```sh
curl -sf http://<host>:8080/actuator/prometheus | head
curl -sf http://<host>:8080/actuator/health
```

Both must return non-empty content and a 200.
