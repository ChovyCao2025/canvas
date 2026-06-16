# Runtime Verification

These scripts are smoke checks for review items that need a running stack. Run them against a local or staging deployment after automated tests pass.

## Canvas Boot Startup And Flyway

Use Flyway disabled only to isolate Spring wiring failures from local database migration-history drift:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) \
CANVAS_JWT_SECRET=0123456789abcdef0123456789abcdef \
perl -e 'alarm shift; exec @ARGV' 45 \
mvn -pl canvas-boot spring-boot:run \
  -Dspring-boot.run.arguments='--server.port=18080 --spring.flyway.enabled=false'
```

Expected: the bounded run reaches `Started CanvasBootApplication` and `Netty started`.

For a normal Flyway-enabled startup, capture the log and classify validation failures before changing database history:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) \
CANVAS_JWT_SECRET=0123456789abcdef0123456789abcdef \
perl -e 'alarm shift; exec @ARGV' 45 \
mvn -pl canvas-boot spring-boot:run \
  -Dspring-boot.run.arguments='--server.port=18081' \
  > /tmp/canvas-boot-flyway.log 2>&1 || true

../scripts/classify-flyway-validate-failure.sh --input-file /tmp/canvas-boot-flyway.log
```

Expected: checksum mismatches are classified as local `flyway_schema_history` drift. Do not run `flyway repair` automatically; use a disposable database for boot wiring checks, or repair only with operator approval after comparing applied SQL and taking a backup.

## CORS

Run:

```bash
BASE_URL=http://localhost:8080 scripts/verify-cors.sh https://evil.example
```

Expected: the command exits 0 and the response headers do not echo the malicious origin as `Access-Control-Allow-Origin`.

## MQ Idempotency

Publish or replay the same RocketMQ broker `msgId` twice through the MQ trigger path, then verify the persisted execution request boundary:

```bash
SOURCE_MSG_ID=<rocketmq-msg-id> scripts/verify-mq-idempotency.sh
```

Expected: for each matched canvas, `canvas_execution_request` has exactly one row and one deterministic request id for that `source_msg_id`.

## Notification WebSocket Limit

Start the backend with a low per-user limit for an easy smoke test:

```bash
CANVAS_NOTIFICATION_WS_MAX_SESSIONS_PER_USER=1 mvn -f backend/pom.xml -pl canvas-boot spring-boot:run
```

In another shell, use a valid logged-in JWT:

```bash
AUTH_TOKEN=<jwt> MAX_SESSIONS_PER_USER=1 BASE_URL=http://localhost:8080 scripts/verify-ws-limit.sh
```

Expected: the first notification WebSocket connection stays open and the second connection is rejected, so live connections after settle are `<= MAX_SESSIONS_PER_USER`.
