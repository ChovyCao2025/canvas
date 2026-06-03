# 本地容量压测操作手册

这是本地容量压测的唯一支持路径。关键闸门由 `tools/perf/perf-guide.mjs` 执行；底层脚本只作为高级调试工具使用。

## 1. 停止条件

只要出现以下任一情况，立即停止：

- verifier 不是 `PASS`
- runner `failed` 不是 `0`
- guide 命令返回 `status: "FAIL"`
- guide `report` 拒绝本次运行
- 后端容器重启或被 OOM kill
- MySQL、Redis、RocketMQ 或下游 mock 持续报错

不要把失败、不完整或证据不足的运行结果整理成容量数字。

## 2. 准备环境

```bash
cd /Users/photonpay/project/canvas
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
export BASE_URL=http://localhost:8080
mkdir -p tmp/perf-fixtures tmp/perf-runs tmp/perf-threshold tmp/perf-monitor
```

创建本地请求签名密钥。不要使用 `application.yml` 里的默认示例值。

```bash
export PERF_EVENT_SECRET="$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 40)"
test ${#PERF_EVENT_SECRET} -ge 32
```

后端必须通过 `CANVAS_EVENT_REPORT_SECRET` 接收同一个值。本项目中，direct 机器请求默认复用 event report 的后端密钥。压测工具只接收环境变量名，也就是 `--event-secret-env PERF_EVENT_SECRET`，不要把密钥明文放进命令行。

## 3. 校验工具

```bash
node --test tools/perf/*.test.mjs
node tools/perf/perf-guide.mjs doctor
```

任一命令失败都不要继续。

## 4. 启动依赖

```bash
docker compose -f docker-compose.local.yml up -d
docker compose -f docker-compose.local.yml ps
```

检查基础依赖是否可用：

```bash
docker exec canvas-mysql mysql -uroot -proot -e "SELECT VERSION();"
docker exec canvas-redis redis-cli ping
curl -sS http://localhost:8099/__admin/mappings | head
```

## 5. 构建后端镜像

```bash
cd /Users/photonpay/project/canvas/backend
mvn -q -pl canvas-engine -am clean package -DskipTests
cd /Users/photonpay/project/canvas
docker build -f backend/canvas-engine/Dockerfile.perf -t canvas-engine:perf .
```

## 6. 启动固定资源后端

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

健康检查：

```bash
curl -sS http://localhost:8080/actuator/health
docker stats --no-stream canvas-backend-perf canvas-mysql canvas-redis
```

## 7. 创建或复用 PERF 测试资源

先执行 guide 的测试资源安全闸门：

```bash
node tools/perf/perf-guide.mjs fixture --rebuild true
```

这个命令只确认操作者明确知道自己要准备测试资源；它不会自动创建画布。你需要创建或复用且只保留一个已发布的 direct 测试画布，以及一个已发布的 event 测试画布。

登录并保存 token：

```bash
export TOKEN=$(
  curl -sS -X POST "$BASE_URL/auth/login" \
    -H 'content-type: application/json' \
    -d '{"username":"admin","password":"Admin@123"}' \
  | node -e "let s='';process.stdin.on('data',c=>s+=c);process.stdin.on('end',()=>{const r=JSON.parse(s); if(!r.data || !r.data.token){console.error(s); process.exit(1)} console.log(r.data.token)})"
)
```

查看已有 `PERF_` 画布：

```bash
docker exec canvas-mysql mysql -uroot -proot -Dcanvas_db -e \
"SELECT id,name,status,published_version_id FROM canvas WHERE name LIKE 'PERF_%' ORDER BY id;"
```

如果已经存在可用且已发布的测试资源，直接导出它们的 ID：

```bash
export DIRECT_CANVAS_ID=<published-PERF_DIRECT_LIGHT-id>
export MATCHED_CANVAS_COUNT=1
```

如果缺少测试资源，先创建 event 和 MQ 定义：

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

生成 direct 和 event 画布 payload：

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

创建并发布画布：

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

确认只有一个已发布 event 测试画布匹配 `PERF_ORDER_PAID`：

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

如果多个已发布画布匹配 `PERF_ORDER_PAID`，或者匹配到的 ID 不是 `$EVENT_CANVAS_ID`，停止测试，先删除或下线重复画布。不要为了让 verifier 通过而调大 `MATCHED_CANVAS_COUNT`；那会改变被测工作量。

## 8. 冒烟测试

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_smoke
node tools/perf/perf-guide.mjs smoke \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id "$DIRECT_CANVAS_ID" \
  --matched-canvas-count "$MATCHED_CANVAS_COUNT" \
  --event-secret-env PERF_EVENT_SECRET
```

冒烟结果必须是 `PASS`。如果失败，先对这个 smoke id 执行 ledger cleanup，再修复原因，不能继续后续压测。

## 9. 阈值测试

阈值测试开始前，在另一个终端启动监控采集，并在压测期间保持运行：

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

执行阈值测试：

```bash
node tools/perf/perf-guide.mjs threshold \
  --mode event \
  --base-url "$BASE_URL" \
  --event-code PERF_ORDER_PAID \
  --event-secret-env PERF_EVENT_SECRET \
  --matched-canvas-count "$MATCHED_CANVAS_COUNT" \
  --threshold-root tmp/perf-threshold
```

把输出中的 `stableStage` 作为当前稳定上限。如果结果是 `NO_STABLE_STAGE`，说明环境或测试资源尚不足以做容量测试，先修复再继续。

## 10. 长稳测试

把 long soak 的并发数设置为阈值测试输出的 `stableStage.concurrency`。`SOAK_COUNT` 要足够大，确保实际运行至少 30 分钟。

```bash
export SOAK_CONCURRENCY=<stableStage.concurrency>
export SOAK_COUNT=<count-large-enough-for-30-minutes>
```

开始 long soak 前，在另一个终端启动监控采集，并在 30 分钟运行期间持续采集：

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

执行 long soak：

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

如果实际运行时间低于 30 分钟，本次运行无效。遇到这种情况时，增大 `SOAK_COUNT`，换一个新的 `PERF_RUN_ID` 重新跑；不要为了让报告通过而降低 `--min-duration-min`。

## 11. 生成报告闸门

```bash
node tools/perf/perf-guide.mjs report \
  --perf-run-id "$PERF_RUN_ID" \
  --report-type capacity \
  --min-duration-min 30
```

report 闸门会检查 runner 和 verifier 证据是否属于同一个 `perfRunId`、请求失败数是否为 0、运行时长是否足够，以及 verifier 证据是否完整可信。

## 12. 监控证据

监控快照必须在 threshold 和 soak 运行期间采集，不能在压测结束、系统空闲后补采。最终报告前确认文件存在：

```bash
ls -lh tmp/perf-monitor
```

如果缺少监控数据，在报告模板中明确记录为证据缺口。

## 13. 清理

预览 ledger 清理 SQL：

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID"
```

执行 ledger 清理：

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID" --execute true
```

完整清理只在全部压测结束后使用：

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID" --scope all --execute true
```

`--scope all` 会删除 `PERF_%` event 和 MQ 定义。它不会删除已发布的 canvas 测试画布；如需移除或下线这些画布，需要单独处理。

## 14. 完成检查清单

- 工具测试已通过。
- 后端使用了非默认的本地签名密钥。
- direct 和 event 测试画布已发布。
- smoke 返回 `PASS`。
- threshold 至少找到一个稳定阶段。
- soak 返回 `PASS`，且实际运行不少于 30 分钟。
- guide report 返回 `PASS`。
- 监控快照已保存。
- 报告模板已填写证据路径。
- cleanup 预览和 cleanup 执行记录已保存。
