# Runtime Verification

These scripts are smoke checks for review items that need a running stack. Run them against a local or staging deployment after automated tests pass.

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
