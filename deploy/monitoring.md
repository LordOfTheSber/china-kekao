# Monitoring (TASK-035)

The backend exposes three Actuator endpoints in prod:

| Endpoint               | Purpose                                              |
|------------------------|------------------------------------------------------|
| `GET /actuator/health` | Aggregated readiness/liveness probe.                |
| `GET /actuator/info`   | Build info (currently empty — populate in CI).      |
| `GET /actuator/prometheus` | Prometheus scrape endpoint.                     |

## Uptime Kuma (recommended)

Cheapest external watchdog. Run on a separate VPS or container — pointing at the
same host you're trying to watch defeats the purpose.

1. `docker run -d --name uptime-kuma -p 3001:3001 -v uptime-kuma:/app/data louislam/uptime-kuma:1`
2. Add a monitor:
   - Type: **HTTP(s) - Keyword**
   - URL: `https://${KEKAO_PUBLIC_HOST}/actuator/health`
   - Keyword: `"status":"UP"`
   - Heartbeat interval: 60s, retries: 3 → alerts within ~5 minutes (matches the DoD).
3. Wire a notification channel (Telegram / Slack / email).

## Prometheus

A minimal scrape config — Prometheus itself is intentionally not part of the
deploy stack right now, this just documents the contract.

```yaml
scrape_configs:
  - job_name: kekao-backend
    metrics_path: /actuator/prometheus
    scrape_interval: 30s
    static_configs:
      - targets: ['kekao.example.com']
```

## JSON logs

`logback-spring.xml` switches to a Logstash JSON encoder whenever the `json`
profile is active. The prod compose file defaults `SPRING_PROFILES_ACTIVE` to
`prod,json`, so by default every line on stdout looks like:

```json
{"@timestamp":"2026-05-13T11:22:33.456Z","level":"INFO","logger":"...","thread":"...","message":"...","service":"china-kekao-backend"}
```

Ship them anywhere stdout-aware (Loki + promtail, Vector, Fluent Bit, or even
just `docker logs` + grep) — no app-side changes required.
