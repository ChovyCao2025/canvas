# Local Capacity Runbook

This is the only supported execution path for local capacity testing. It uses `tools/perf/perf-guide.mjs` for gates and keeps raw scripts as advanced tools only.

## 1. Stop Conditions

Stop immediately when any condition is true:

- verifier is not `PASS`
- runner `failed` is not `0`
- guide command returns `status: "FAIL"`
- guide `report` rejects the run
- backend container restarts or is OOM-killed
- MySQL, Redis, RocketMQ, or downstream mocks show sustained errors

Do not turn a failed or incomplete run into a capacity number.

## 2. Prepare Environment

```bash
cd /Users/photonpay/project/canvas
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
export BASE_URL=http://localhost:8080
mkdir -p tmp/perf-fixtures tmp/perf-runs tmp/perf-threshold tmp/perf-monitor
```

Create a local request signing secret. Do not use the default sample value from `application.yml`.

```bash
export PERF_EVENT_SECRET="$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 40)"
test ${#PERF_EVENT_SECRET} -ge 32
```

The backend must receive the same value through `CANVAS_EVENT_REPORT_SECRET`. In this project, direct machine requests default to the same backend secret as event reports. The perf tools receive only the env var name through `--event-secret-env PERF_EVENT_SECRET`.

## 3. Verify Tools

```bash
node --test tools/perf/*.test.mjs
node tools/perf/perf-guide.mjs doctor
```

Do not continue when either command fails.

## 4. Start Dependencies

```bash
docker compose -f docker-compose.local.yml up -d
docker compose -f docker-compose.local.yml ps
```

Verify basic dependency health:

```bash
docker exec canvas-mysql mysql -uroot -proot -e "SELECT VERSION();"
docker exec canvas-redis redis-cli ping
curl -sS http://localhost:8099/__admin/mappings | head
```

## 5. Build Backend Image

```bash
cd /Users/photonpay/project/canvas/backend
mvn -q -pl canvas-engine -am clean package -DskipTests
cd /Users/photonpay/project/canvas
docker build -f backend/canvas-engine/Dockerfile.perf -t canvas-engine:perf .
```

## 6. Start Fixed-Resource Backend

```bash
docker rm -f canvas-backend-perf 2>/dev/null || true
docker run -d \
  --name canvas-backend-perf \
  --cpus=2 \
  --memory=2g \
  --memory-swap=2g \
  -p 8080:8080 \
  -e JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom" \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/canvas_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e ROCKETMQ_NAME_SERVER=host.docker.internal:9876 \
  -e CANVAS_EVENT_REPORT_SECRET="$PERF_EVENT_SECRET" \
  -e CANVAS_INTEGRATION_COUPON_SERVICE_URL=http://host.docker.internal:8099/mock/coupon \
  -e CANVAS_INTEGRATION_TAGGER_SERVICE_URL=http://host.docker.internal:8099/mock/tagger \
  -e CANVAS_INTEGRATION_REACH_PLATFORM_URL=http://host.docker.internal:8099/mock/reach \
  -e CANVAS_INTEGRATION_API_CALL_BASE_URL=http://host.docker.internal:8099/mock/api \
  canvas-engine:perf
```

Health check:

```bash
curl -sS http://localhost:8080/actuator/health
docker stats --no-stream canvas-backend-perf canvas-mysql canvas-redis
```

## 7. Create Or Reuse PERF Fixtures

First run the guide fixture safety gate:

```bash
node tools/perf/perf-guide.mjs fixture --rebuild true
```

This command confirms explicit operator intent. It does not create canvases. Create or reuse exactly one published direct fixture and one published event fixture.

Log in:

```bash
export TOKEN=$(
  curl -sS -X POST "$BASE_URL/auth/login" \
    -H 'content-type: application/json' \
    -d '{"username":"admin","password":"Admin@123"}' \
  | node -e "let s='';process.stdin.on('data',c=>s+=c);process.stdin.on('end',()=>{const r=JSON.parse(s); if(!r.data || !r.data.token){console.error(s); process.exit(1)} console.log(r.data.token)})"
)
```

Check existing `PERF_` canvases:

```bash
docker exec canvas-mysql mysql -uroot -proot -Dcanvas_db -e \
"SELECT id,name,status,published_version_id FROM canvas WHERE name LIKE 'PERF_%' ORDER BY id;"
```

If suitable published fixtures already exist, export their IDs:

```bash
export DIRECT_CANVAS_ID=<published-PERF_DIRECT_LIGHT-id>
export MATCHED_CANVAS_COUNT=1
```

If fixtures are missing, create event and MQ definitions:

```bash
docker exec canvas-mysql mysql -uroot -proot -Dcanvas_db -e "
INSERT INTO event_definition
  (name, event_code, attributes, description, enabled, created_by, created_at, updated_at)
VALUES
  ('压测订单支付事件', 'PERF_ORDER_PAID',
   '[{\"name\":\"amount\",\"displayName\":\"金额\",\"type\":\"NUMBER\",\"required\":false},{\"name\":\"perfRunId\",\"displayName\":\"压测批次\",\"type\":\"STRING\",\"required\":false},{\"name\":\"perfInputId\",\"displayName\":\"输入ID\",\"type\":\"STRING\",\"required\":false}]',
   '本机容量压测事件定义', 1, 'perf', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  enabled = VALUES(enabled),
  attributes = VALUES(attributes),
  updated_at = NOW();

INSERT INTO mq_message_definition
  (name, message_code, topic, request_schema, description, enabled, created_by, created_at, updated_at)
VALUES
  ('压测 MQ 消息', 'PERF_MQ', 'PERF_MQ',
   '[{\"name\":\"perfRunId\",\"displayName\":\"压测批次\",\"type\":\"STRING\",\"required\":false},{\"name\":\"perfInputId\",\"displayName\":\"输入ID\",\"type\":\"STRING\",\"required\":false},{\"name\":\"seq\",\"displayName\":\"序号\",\"type\":\"NUMBER\",\"required\":false}]',
   '本机容量压测 MQ 定义', 1, 'perf', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  topic = VALUES(topic),
  request_schema = VALUES(request_schema),
  enabled = VALUES(enabled),
  updated_at = NOW();
"
```

Generate direct and event canvas payloads:

```bash
node <<'NODE'
const fs = require('fs')
fs.mkdirSync('tmp/perf-fixtures', { recursive: true })

function write(name, body) {
  fs.writeFileSync(`tmp/perf-fixtures/${name}.json`, `${JSON.stringify(body, null, 2)}\n`)
}

function endNode() {
  return { id: 'end', type: 'END', name: '结束', category: '流程控制', x: 420, y: 260, config: {}, bizConfig: {} }
}

write('direct-canvas', {
  name: 'PERF_DIRECT_LIGHT',
  description: '本机容量压测：直调轻链路',
  triggerType: 'REALTIME',
  createdBy: 'perf',
  graphJson: JSON.stringify({
    nodes: [
      { id: 'direct', type: 'DIRECT_CALL', name: '直调触发', category: '触发器', x: 420, y: 80, config: { nextNodeId: 'end' }, bizConfig: { nextNodeId: 'end' } },
      endNode(),
    ],
  }),
})

write('event-canvas', {
  name: 'PERF_EVENT_LIGHT',
  description: '本机容量压测：事件上报轻链路',
  triggerType: 'REALTIME',
  createdBy: 'perf',
  graphJson: JSON.stringify({
    nodes: [
      { id: 'event', type: 'EVENT_TRIGGER', name: '压测事件', category: '行为策略', x: 420, y: 80, config: { eventCode: 'PERF_ORDER_PAID', nextNodeId: 'end' }, bizConfig: { eventCode: 'PERF_ORDER_PAID', nextNodeId: 'end' } },
      endNode(),
    ],
  }),
})
NODE
```

Create and publish the canvases:

```bash
export DIRECT_CANVAS_ID=$(
  curl -sS -X POST "$BASE_URL/canvas" \
    -H "authorization: Bearer $TOKEN" \
    -H 'content-type: application/json' \
    -d @tmp/perf-fixtures/direct-canvas.json \
  | node -e "let s='';process.stdin.on('data',c=>s+=c);process.stdin.on('end',()=>{const r=JSON.parse(s); if(!r.data || !r.data.id){console.error(s); process.exit(1)} console.log(r.data.id)})"
)

export EVENT_CANVAS_ID=$(
  curl -sS -X POST "$BASE_URL/canvas" \
    -H "authorization: Bearer $TOKEN" \
    -H 'content-type: application/json' \
    -d @tmp/perf-fixtures/event-canvas.json \
  | node -e "let s='';process.stdin.on('data',c=>s+=c);process.stdin.on('end',()=>{const r=JSON.parse(s); if(!r.data || !r.data.id){console.error(s); process.exit(1)} console.log(r.data.id)})"
)

curl -sS -X POST "$BASE_URL/canvas/$DIRECT_CANVAS_ID/publish?operator=perf" \
  -H "authorization: Bearer $TOKEN" | tee tmp/perf-fixtures/direct-publish.json

curl -sS -X POST "$BASE_URL/canvas/$EVENT_CANVAS_ID/publish?operator=perf" \
  -H "authorization: Bearer $TOKEN" | tee tmp/perf-fixtures/event-publish.json

export MATCHED_CANVAS_COUNT=1
echo "DIRECT_CANVAS_ID=$DIRECT_CANVAS_ID"
echo "EVENT_CANVAS_ID=$EVENT_CANVAS_ID"
```

Confirm exactly one published event fixture matches `PERF_ORDER_PAID`.

```bash
docker exec canvas-mysql mysql -uroot -proot -Dcanvas_db -e \
"SELECT c.id,c.name,c.status,c.published_version_id
 FROM canvas c
 JOIN canvas_version cv ON cv.id = c.published_version_id
 WHERE c.status = 1
   AND c.published_version_id IS NOT NULL
   AND cv.graph_json LIKE '%EVENT_TRIGGER%'
   AND cv.graph_json LIKE '%PERF_ORDER_PAID%'
 ORDER BY c.id;"

export EVENT_MATCH_COUNT=$(
  docker exec canvas-mysql mysql -N -B -uroot -proot -Dcanvas_db -e \
  "SELECT COUNT(*)
   FROM canvas c
   JOIN canvas_version cv ON cv.id = c.published_version_id
   WHERE c.status = 1
     AND c.published_version_id IS NOT NULL
     AND cv.graph_json LIKE '%EVENT_TRIGGER%'
     AND cv.graph_json LIKE '%PERF_ORDER_PAID%';"
)
test "$EVENT_MATCH_COUNT" = "1"

export EVENT_MATCH_ID=$(
  docker exec canvas-mysql mysql -N -B -uroot -proot -Dcanvas_db -e \
  "SELECT c.id
   FROM canvas c
   JOIN canvas_version cv ON cv.id = c.published_version_id
   WHERE c.status = 1
     AND c.published_version_id IS NOT NULL
     AND cv.graph_json LIKE '%EVENT_TRIGGER%'
     AND cv.graph_json LIKE '%PERF_ORDER_PAID%';"
)
test "$EVENT_MATCH_ID" = "$EVENT_CANVAS_ID"
```

If more than one published canvas matches `PERF_ORDER_PAID`, or the matching id is not `$EVENT_CANVAS_ID`, stop and remove or offline duplicates before testing. Do not raise `MATCHED_CANVAS_COUNT` to make verifier pass; that changes the measured workload.

## 8. Smoke

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_smoke
node tools/perf/perf-guide.mjs smoke \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id "$DIRECT_CANVAS_ID" \
  --matched-canvas-count "$MATCHED_CANVAS_COUNT" \
  --event-secret-env PERF_EVENT_SECRET
```

The smoke result must be `PASS`. If it fails, run ledger cleanup for the smoke id and fix the cause before continuing.

## 9. Threshold

Start monitor capture in another terminal before threshold and keep capturing during the run:

```bash
export PERF_MONITOR_TAG=threshold_$(date +%Y%m%d_%H%M%S)
while true; do
  date
  docker stats --no-stream canvas-backend-perf canvas-mysql canvas-redis
  curl -sS http://localhost:8080/actuator/prometheus > "tmp/perf-monitor/prom-$PERF_MONITOR_TAG-$(date +%H%M%S).txt"
  docker exec canvas-mysql mysql -uroot -proot -Dcanvas_db -e "
  SHOW GLOBAL STATUS LIKE 'Threads_connected';
  SHOW GLOBAL STATUS LIKE 'Questions';
  SHOW GLOBAL STATUS LIKE 'Innodb_row_lock%';
  SHOW FULL PROCESSLIST;
  "
  docker exec canvas-redis redis-cli INFO stats | egrep 'instantaneous_ops_per_sec|total_commands_processed|rejected_connections|expired_keys|evicted_keys'
  sleep 30
done | tee "tmp/perf-monitor/live-$PERF_MONITOR_TAG.txt"
```

```bash
node tools/perf/perf-guide.mjs threshold \
  --mode event \
  --base-url "$BASE_URL" \
  --event-code PERF_ORDER_PAID \
  --event-secret-env PERF_EVENT_SECRET \
  --matched-canvas-count "$MATCHED_CANVAS_COUNT" \
  --threshold-root tmp/perf-threshold
```

Use `stableStage` as the current stable ceiling. `NO_STABLE_STAGE` means the environment or fixture is not valid enough for capacity testing.

## 10. Soak

Set the soak concurrency from the threshold `stableStage.concurrency`. Set the count high enough for the run to last at least 30 minutes at that concurrency.

```bash
export SOAK_CONCURRENCY=<stableStage.concurrency>
export SOAK_COUNT=<count-large-enough-for-30-minutes>
```

Start monitor capture in another terminal before starting soak, then repeat it periodically during the 30-minute run:

```bash
export PERF_MONITOR_TAG=soak_$(date +%Y%m%d_%H%M%S)
while true; do
  date
  docker stats --no-stream canvas-backend-perf canvas-mysql canvas-redis
  curl -sS http://localhost:8080/actuator/prometheus > "tmp/perf-monitor/prom-$PERF_MONITOR_TAG-$(date +%H%M%S).txt"
  docker exec canvas-redis redis-cli INFO stats | egrep 'instantaneous_ops_per_sec|total_commands_processed|rejected_connections|expired_keys|evicted_keys'
  sleep 30
done | tee "tmp/perf-monitor/live-$PERF_MONITOR_TAG.txt"
```

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_soak
node tools/perf/perf-guide.mjs soak \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --mode event \
  --event-secret-env PERF_EVENT_SECRET \
  --matched-canvas-count "$MATCHED_CANVAS_COUNT" \
  --count "$SOAK_COUNT" \
  --concurrency "$SOAK_CONCURRENCY" \
  --min-duration-min 30
```

The run is invalid if actual duration is below 30 minutes. If the run finishes too quickly, increase `SOAK_COUNT` and rerun with a new `PERF_RUN_ID`; do not lower `--min-duration-min` for a capacity report.

## 11. Report

```bash
node tools/perf/perf-guide.mjs report \
  --perf-run-id "$PERF_RUN_ID" \
  --report-type capacity \
  --min-duration-min 30
```

The report gate checks that runner and verifier evidence belong to the same `perfRunId`, that request failures are zero, and that duration is long enough.

## 12. Monitor Evidence

Monitor snapshots must be captured during threshold and soak, not after the run is idle. Confirm the files exist before final reporting:

```bash
ls -lh tmp/perf-monitor
```

If monitoring is missing, record it as an evidence gap in the report template.

## 13. Cleanup

Preview ledger cleanup:

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID"
```

Execute ledger cleanup:

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID" --execute true
```

Full cleanup is only for the end of all testing:

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID" --scope all --execute true
```

`--scope all` removes `PERF_%` event and MQ definitions. It does not remove published canvas fixtures; remove or offline those separately if needed.

## 14. Completion Checklist

- Tool tests passed.
- Backend used a non-default local event secret.
- Direct and event fixtures were published.
- Smoke returned `PASS`.
- Threshold found at least one stable stage.
- Soak returned `PASS` for at least 30 minutes.
- Guide report returned `PASS`.
- Monitor snapshots were saved.
- Report template is filled with evidence paths.
- Cleanup preview and cleanup execution were recorded.
