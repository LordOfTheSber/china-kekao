# Monitoring (TASK-035)

The backend exposes two Actuator endpoints in prod:

| Endpoint               | Purpose                                              |
|------------------------|------------------------------------------------------|
| `GET /actuator/health` | Aggregated readiness/liveness probe.                |
| `GET /actuator/info`   | Build info (currently empty — populate in CI).      |

Prometheus is intentionally not wired up right now: it requires
`micrometer-registry-prometheus` plus an external Prometheus install, and
neither is needed to satisfy the DoD "падение backend замечается в течение
5 минут". Add it later by re-introducing the dependency and exposing
`prometheus` in `management.endpoints.web.exposure.include`.

## Uptime Kuma (recommended)

Cheapest external watchdog. Run on a separate VPS or container — pointing at
the same host you're trying to watch defeats the purpose.

1. `docker run -d --name uptime-kuma -p 3001:3001 -v uptime-kuma:/app/data louislam/uptime-kuma:1`
2. Add a monitor:
   - Type: **HTTP(s) - Keyword**
   - URL: `https://${KEKAO_PUBLIC_HOST}/actuator/health`
   - Keyword: `"status":"UP"`
   - Heartbeat interval: 60s, retries: 3 → alerts within ~5 minutes (matches the DoD).
3. Wire a notification channel (Telegram / Slack / email).

## JSON logs

`application-prod.yml` overrides `logging.pattern.console` so every line on
stdout under the `prod` profile is a single JSON object:

```json
{"@timestamp":"2026-05-13T11:22:33.456Z","level":"INFO","logger":"...","thread":"...","message":"...","exception":""}
```

That keeps the build self-contained (no extra logback encoder dependency)
while staying parseable by Loki + promtail, Vector, Fluent Bit, or even
`docker logs | jq`. If you outgrow the pattern, swap in
`logstash-logback-encoder` and a `logback-spring.xml`.
