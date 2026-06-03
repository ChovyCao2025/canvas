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

## 7. 创建 PERF 测试资源

执行一条命令创建并发布标准测试资源：

```bash
node tools/perf/perf-guide.mjs fixture \
  --base-url "$BASE_URL" \
  --rebuild true | tee tmp/perf-fixtures/fixture.json
```

该命令会登录本地后端，归档旧的同名 `PERF_` 画布，然后创建并发布三个画布：

- `PERF_DIRECT_LIGHT`：direct 冒烟和直调轻链路。
- `PERF_EVENT_LIGHT`：event 阈值和长稳测试，事件编码为 `PERF_ORDER_PAID`。
- `PERF_ENGINE_ACCURACY`：高并发准确性测试，包含 `DIRECT_CALL -> GROOVY -> IF_CONDITION -> SEND_MESSAGE(even/odd) -> HUB -> END`。

如果本地管理员账号不是默认值，先设置环境变量再执行 fixture：

```bash
export PERF_ADMIN_USERNAME=<local-admin-username>
export PERF_ADMIN_PASSWORD=<local-admin-password>
```

导出后续命令需要的 ID：

```bash
export DIRECT_CANVAS_ID=$(
  node -e "const r=require('./tmp/perf-fixtures/fixture.json'); console.log(r.directCanvasId)"
)
export ENGINE_ACCURACY_CANVAS_ID=$(
  node -e "const r=require('./tmp/perf-fixtures/fixture.json'); console.log(r.engineAccuracyCanvasId)"
)
export MATCHED_CANVAS_COUNT=1
test -n "$DIRECT_CANVAS_ID"
test -n "$ENGINE_ACCURACY_CANVAS_ID"
```

确认 event 压测只匹配一个已发布画布：

```bash
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
```

如果这里不是 `1`，停止测试，重新执行 fixture 或手工下线重复画布。不要通过调大 `MATCHED_CANVAS_COUNT` 掩盖重复路由；那会改变真实工作量。

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

## 9. 高并发准确性验证

容量测试前必须先跑一次准确性验证。这个命令会对 `PERF_ENGINE_ACCURACY` 画布执行 direct 高并发请求，并自动完成三类校验：

- 账本准确性：runner 成功数、执行记录、DLQ、重试队列和去重结果必须对齐。
- Trace 准确性：`DIRECT_CALL`、`GROOVY`、`IF_CONDITION`、偶/奇触达分支、`HUB`、`END` 的成功/跳过次数必须符合预期。
- 副作用准确性：WireMock `/mock/reach/send` 中同一个 `perfInputId + branch` 不能重复投递，偶/奇分支请求数必须符合 `seq` 分布。

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_accuracy
node tools/perf/perf-guide.mjs accuracy \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id "$ENGINE_ACCURACY_CANVAS_ID" \
  --event-secret-env PERF_EVENT_SECRET \
  --count 20000 \
  --concurrency 100 \
  --wiremock-url http://localhost:8099
```

返回结果必须是 `status: "PASS"`。该命令会写入：

- `tmp/perf-runs/$PERF_RUN_ID/runner-summary.json`
- `tmp/perf-runs/$PERF_RUN_ID/verifier.json`
- `tmp/perf-runs/$PERF_RUN_ID/side-effect-verifier.json`

如果失败，先查看 `reason` 和对应 JSON。常见含义如下：

- `traceMismatch > 0`：节点执行轨迹数量与预期不一致，可能存在漏执行、重复执行或跳过记录缺失。
- `traceFailed > 0`：某个节点留下失败 trace，不能进入容量测试。
- `traceDuplicateSuccess > 0`：同一 execution 的同一节点有重复成功 trace。
- `duplicateSideEffects > 0`：同一输入和分支对外投递了多次，核心副作用不准确。
- `branchMismatch > 0`：偶/奇分支数量不符合 `seq` 分布，说明分支执行不可信。

准确性验证失败时，不要继续 threshold 或 soak。先清理本次 `PERF_RUN_ID`，修复问题后换一个新的 `PERF_RUN_ID` 重跑。

## 10. 阈值测试

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

## 11. 长稳测试

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

## 12. 生成报告闸门

```bash
node tools/perf/perf-guide.mjs report \
  --perf-run-id "$PERF_RUN_ID" \
  --report-type capacity \
  --min-duration-min 30
```

report 闸门会检查 runner 和 verifier 证据是否属于同一个 `perfRunId`、请求失败数是否为 0、运行时长是否足够，以及 verifier 证据是否完整可信。

## 13. 监控证据

监控快照必须在 threshold 和 soak 运行期间采集，不能在压测结束、系统空闲后补采。最终报告前确认文件存在：

```bash
ls -lh tmp/perf-monitor
```

如果缺少监控数据，在报告模板中明确记录为证据缺口。

## 14. 清理

预览 ledger 清理 SQL：

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID"
```

执行 ledger 清理：

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID" --execute true
```

ledger 清理会删除该 `perfRunId` 对应的 `event_log`、`canvas_execution`、`canvas_execution_trace`、`canvas_execution_request`、`canvas_execution_dlq`、`audience_compute_run`，以及由这些 execution 产生的 `message_send_record`。

完整清理只在全部压测结束后使用：

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID" --scope all --execute true
```

`--scope all` 会删除 `PERF_%` event 和 MQ 定义。它不会删除已发布的 canvas 测试画布；如需移除或下线这些画布，需要单独处理。

## 15. 完成检查清单

- 工具测试已通过。
- 后端使用了非默认的本地签名密钥。
- direct 和 event 测试画布已发布。
- smoke 返回 `PASS`。
- accuracy 返回 `PASS`，且 trace verifier 与 side-effect verifier 均为 `PASS`。
- threshold 至少找到一个稳定阶段。
- soak 返回 `PASS`，且实际运行不少于 30 分钟。
- guide report 返回 `PASS`。
- 监控快照已保存。
- 报告模板已填写证据路径。
- cleanup 预览和 cleanup 执行记录已保存。
