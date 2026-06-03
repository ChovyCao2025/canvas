# Plan: Observability And Ops

1. Add request/execution correlation ID propagation.
2. Populate MDC consistently across boundedElastic and async execution paths.
3. Decide tracing implementation: Micrometer Tracing with Brave/OTel or a simpler internal trace table first.
4. Add deployable Prometheus alert rules and dashboard JSON/YAML.
5. Add runbooks for DLQ, route rebuild, cache invalidation, and shutdown.
6. Validate by running the app locally and checking logs plus `/actuator/prometheus`.
