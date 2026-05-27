# 本机容器化容量压测与最大流量外推方案

## 1. 目标

回答 canvas-engine 服务在目标部署形态下可以承接的最大稳定流量。由于当前只能在本机处理，先用本机 Docker 固定资源测出单后端容器容量，再通过 CPU、MySQL、Redis、RocketMQ、下游接口和 Disruptor/worker 约束做保守外推。

本方案不把本机结果直接等价为生产容量。最终结论必须同时给出：

- 本机固定资源下的单容器稳定容量。
- 目标环境按资源和依赖瓶颈折算后的建议最大流量。
- 告警线和限流线。
- 主要瓶颈和扩容建议。

## 2. 最大流量口径

“最大流量”只认稳定值，不认瞬时峰值。

一个流量档位必须同时满足以下条件，才可以作为稳定容量候选：

- 连续运行至少 30 分钟，短场景阶梯压测只能用于找边界。
- HTTP 失败率低于约定阈值，建议首轮使用 `0`，后续最多放宽到 `< 0.1%`。
- `tools/perf/verifier.mjs` 返回 `PASS`。
- 无持续增长的 RocketMQ backlog、执行请求 backlog 或重试 backlog。
- p95 没有连续恶化；p99 可从 Prometheus/网关指标补充。当前 `tools/perf/perf-runner.mjs` 原生输出 p95，不原生输出 p99。
- 后端容器无 OOM、无频繁 Full GC、无长时间 CPU 饱和。
- MySQL 连接池、Redis 延迟、RocketMQ 消费线程、下游 mock RT 均未持续恶化。

核心输出口径：

```text
入口 QPS
execution 并发
p95，p99 可选
错误率
verifier 结果
MySQL 写 QPS
Redis OPS
RocketMQ 发送/消费/积压
下游调用 QPS
```

## 3. 推荐拓扑

本机采用“后端容器化，发压留在宿主机”的拓扑：

```text
宿主机:
  Node.js perf-runner / verifier / capacity-report
  curl / mysql client / docker CLI

Docker:
  canvas-backend-perf，固定 2C2G、4C4G 等资源档
  canvas-mysql
  canvas-redis
  canvas-wiremock
  canvas-rocketmq-namesrv
  rocketmq-broker service，容器名以 docker compose 实际生成为准
```

不建议把发压端也放进 Docker。发压端和后端容器同处 Docker 资源池时，容易把瓶颈测成 Docker 调度或本机资源争抢。

## 4. 新增 Dockerfile

压测使用 runtime-only 镜像，先在宿主机 Maven 打包，再把 Jar 放进镜像。这样可以避开当前多模块 Maven 项目在 Docker 构建阶段解析父 POM 和 `canvas-cache-sdk` 的问题。

文件位置：

```text
backend/canvas-engine/Dockerfile.perf
```

构建上下文必须是项目根目录：

```bash
docker build -f backend/canvas-engine/Dockerfile.perf -t canvas-engine:perf .
```

## 5. 前置准备

确认本机工具：

```bash
java -version
mvn -version
node --version
docker --version
```

要求：

```text
JDK 21
Maven 3.9+
Node.js 18+
Docker Desktop 可分配至少 8G 内存，推荐 12G+
```

进入项目根目录：

```bash
cd /Users/photonpay/project/canvas
```

记录本机环境，后续报告必须写入：

```bash
sysctl -n machdep.cpu.brand_string
sysctl -n hw.ncpu
sysctl -n hw.memsize
docker version
docker info | sed -n '1,80p'
```

## 6. 启动本地依赖

启动 MySQL、Redis、WireMock、RocketMQ：

```bash
docker compose -f docker-compose.local.yml up -d
docker compose -f docker-compose.local.yml ps
```

确认数据库可用：

```bash
docker exec canvas-mysql mysql -uroot -proot -e "SELECT VERSION();"
docker exec canvas-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS canvas_db CHARACTER SET utf8mb4;"
```

确认 Redis 可用：

```bash
docker exec canvas-redis redis-cli ping
```

确认 WireMock 可用：

```bash
curl -sS http://localhost:8099/__admin/mappings | head
```

## 7. 打包 Jar 和构建镜像

打包：

```bash
cd /Users/photonpay/project/canvas/backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -q -pl canvas-engine -am clean package -DskipTests
```

如果当前工作区存在测试代码编译失败，但需要先产出本机压测镜像，可以改用下面命令。该命令会跳过测试编译，只应用于本机压测镜像，不代表测试通过：

```bash
mvn -q -pl canvas-engine -am clean package -Dmaven.test.skip=true
```

确认 Jar 存在：

```bash
ls -lh /Users/photonpay/project/canvas/backend/canvas-engine/target/canvas-engine-*.jar
```

构建镜像：

```bash
cd /Users/photonpay/project/canvas
docker build -f backend/canvas-engine/Dockerfile.perf -t canvas-engine:perf .
docker image inspect canvas-engine:perf --format='{{.Id}} {{.Size}}'
```

## 8. 启动后端压测容器

### 8.1 2C2G 档

```bash
docker rm -f canvas-backend-perf 2>/dev/null || true

docker run -d \
  --name canvas-backend-perf \
  --cpus=2 \
  --memory=2g \
  --memory-swap=2g \
  -p 8080:8080 \
  -e JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=70 -Xlog:gc*:stdout -Djava.security.egd=file:/dev/./urandom" \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/canvas_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e ROCKETMQ_NAME_SERVER=host.docker.internal:9876 \
  -e CANVAS_INTEGRATION_COUPON_SERVICE_URL=http://host.docker.internal:8099/mock/coupon \
  -e CANVAS_INTEGRATION_TAGGER_SERVICE_URL=http://host.docker.internal:8099/mock/tagger \
  -e CANVAS_INTEGRATION_REACH_PLATFORM_URL=http://host.docker.internal:8099/mock/reach \
  -e CANVAS_INTEGRATION_API_CALL_BASE_URL=http://host.docker.internal:8099/mock/api \
  -e CANVAS_EXECUTION_MAX_CONCURRENCY=1000 \
  canvas-engine:perf
```

健康检查：

```bash
docker logs -f canvas-backend-perf
curl -sS http://localhost:8080/actuator/health
curl -sS http://localhost:8080/actuator/prometheus | head
docker stats --no-stream canvas-backend-perf canvas-mysql canvas-redis
```

注意：当前 `MqTriggerConsumer` 的 `consumeThreadNumber` 在注解中固定为 `20`，不是通过 `CANVAS_MQ_CONSUME_THREAD` 环境变量动态读取。MQ 消费线程数如需调节，需要先改代码或单独补配置化能力。

### 8.2 4C4G 档

停止 2C2G 容器后，用相同环境变量启动 4C4G：

```bash
docker rm -f canvas-backend-perf 2>/dev/null || true

docker run -d \
  --name canvas-backend-perf \
  --cpus=4 \
  --memory=4g \
  --memory-swap=4g \
  -p 8080:8080 \
  -e JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=70 -Xlog:gc*:stdout -Djava.security.egd=file:/dev/./urandom" \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/canvas_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e ROCKETMQ_NAME_SERVER=host.docker.internal:9876 \
  -e CANVAS_INTEGRATION_COUPON_SERVICE_URL=http://host.docker.internal:8099/mock/coupon \
  -e CANVAS_INTEGRATION_TAGGER_SERVICE_URL=http://host.docker.internal:8099/mock/tagger \
  -e CANVAS_INTEGRATION_REACH_PLATFORM_URL=http://host.docker.internal:8099/mock/reach \
  -e CANVAS_INTEGRATION_API_CALL_BASE_URL=http://host.docker.internal:8099/mock/api \
  -e CANVAS_EXECUTION_MAX_CONCURRENCY=1000 \
  canvas-engine:perf
```

## 9. 创建压测 Fixture

只在本地压测库创建 `PERF_` 命名的事件、MQ 定义和轻量画布。不要混入真实业务数据。

设置基础变量：

```bash
cd /Users/photonpay/project/canvas
export BASE_URL=http://localhost:8080
mkdir -p tmp/perf-fixtures
```

登录获取 token：

```bash
export TOKEN=$(
  curl -sS -X POST "$BASE_URL/auth/login" \
    -H 'content-type: application/json' \
    -d '{"username":"admin","password":"Admin@123"}' \
  | node -e "let s='';process.stdin.on('data',c=>s+=c);process.stdin.on('end',()=>{const r=JSON.parse(s); if(!r.data || !r.data.token){console.error(s); process.exit(1)} console.log(r.data.token)})"
)
```

先确认没有已发布的旧 `PERF_` 画布。事件和 MQ 压测必须保证同一个 code/tag 只路由到一个已发布画布，否则 `matchedCanvasCount` 要按实际发布数量填写。

```bash
docker exec canvas-mysql mysql -uroot -proot -Dcanvas_db -e \
"SELECT id,name,status,published_version_id FROM canvas WHERE name LIKE 'PERF_%' ORDER BY id;"
```

创建或更新事件定义和 MQ 定义：

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

生成三个轻量画布请求：

```bash
node <<'NODE'
const fs = require('fs')
fs.mkdirSync('tmp/perf-fixtures', { recursive: true })

function write(name, body) {
  fs.writeFileSync(`tmp/perf-fixtures/${name}.json`, `${JSON.stringify(body, null, 2)}\n`)
}

function endNode() {
  return {
    id: 'end',
    type: 'END',
    name: '结束',
    category: '流程控制',
    x: 420,
    y: 260,
    config: {},
    bizConfig: {},
  }
}

write('direct-canvas', {
  name: 'PERF_DIRECT_LIGHT',
  description: '本机容量压测：直调轻链路',
  triggerType: 'REALTIME',
  createdBy: 'perf',
  graphJson: JSON.stringify({
    nodes: [
      {
        id: 'direct',
        type: 'DIRECT_CALL',
        name: '直调触发',
        category: '触发器',
        x: 420,
        y: 80,
        config: { nextNodeId: 'end' },
        bizConfig: { nextNodeId: 'end' },
      },
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
      {
        id: 'event',
        type: 'EVENT_TRIGGER',
        name: '压测事件',
        category: '行为策略',
        x: 420,
        y: 80,
        config: { eventCode: 'PERF_ORDER_PAID', nextNodeId: 'end' },
        bizConfig: { eventCode: 'PERF_ORDER_PAID', nextNodeId: 'end' },
      },
      endNode(),
    ],
  }),
})

write('mq-canvas', {
  name: 'PERF_MQ_LIGHT',
  description: '本机容量压测：MQ 轻链路',
  triggerType: 'REALTIME',
  createdBy: 'perf',
  graphJson: JSON.stringify({
    nodes: [
      {
        id: 'mq',
        type: 'MQ_TRIGGER',
        name: '压测 MQ',
        category: '行为策略',
        x: 420,
        y: 80,
        config: {
          messageCodeKey: 'PERF_MQ',
          topicKey: 'PERF_MQ',
          validateResult: false,
          nextNodeId: 'end',
        },
        bizConfig: {
          messageCodeKey: 'PERF_MQ',
          topicKey: 'PERF_MQ',
          validateResult: false,
          nextNodeId: 'end',
        },
      },
      endNode(),
    ],
  }),
})
NODE
```

创建画布并提取 ID：

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

export MQ_CANVAS_ID=$(
  curl -sS -X POST "$BASE_URL/canvas" \
    -H "authorization: Bearer $TOKEN" \
    -H 'content-type: application/json' \
    -d @tmp/perf-fixtures/mq-canvas.json \
  | node -e "let s='';process.stdin.on('data',c=>s+=c);process.stdin.on('end',()=>{const r=JSON.parse(s); if(!r.data || !r.data.id){console.error(s); process.exit(1)} console.log(r.data.id)})"
)

echo "DIRECT_CANVAS_ID=$DIRECT_CANVAS_ID"
echo "EVENT_CANVAS_ID=$EVENT_CANVAS_ID"
echo "MQ_CANVAS_ID=$MQ_CANVAS_ID"
```

发布画布。发布动作会把事件和 MQ 路由注册到 Redis，不能只改 DB 状态。

```bash
curl -sS -X POST "$BASE_URL/canvas/$DIRECT_CANVAS_ID/publish?operator=perf" \
  -H "authorization: Bearer $TOKEN" | tee tmp/perf-fixtures/direct-publish.json

curl -sS -X POST "$BASE_URL/canvas/$EVENT_CANVAS_ID/publish?operator=perf" \
  -H "authorization: Bearer $TOKEN" | tee tmp/perf-fixtures/event-publish.json

curl -sS -X POST "$BASE_URL/canvas/$MQ_CANVAS_ID/publish?operator=perf" \
  -H "authorization: Bearer $TOKEN" | tee tmp/perf-fixtures/mq-publish.json
```

确认发布状态：

```bash
docker exec canvas-mysql mysql -uroot -proot -Dcanvas_db -e \
"SELECT id,name,status,published_version_id FROM canvas WHERE id IN ($DIRECT_CANVAS_ID,$EVENT_CANVAS_ID,$MQ_CANVAS_ID);"
```

## 10. Smoke 验证

每个资源档正式压测前都要先跑 smoke。smoke 的目标是验证 fixture、后端、依赖、对账、清理链路正确，不是测吞吐。

### 10.1 Direct smoke

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_direct_smoke

node tools/perf/perf-runner.mjs \
  --mode direct \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id "$DIRECT_CANVAS_ID" \
  --count 50 \
  --concurrency 5 \
  --summary-file "tmp/perf-$PERF_RUN_ID.json"

node tools/perf/verifier.mjs \
  --mode direct \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 50 \
  --matched-canvas-count 1
```

### 10.2 Event smoke

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_event_smoke

node tools/perf/perf-runner.mjs \
  --mode event \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --event-code PERF_ORDER_PAID \
  --count 100 \
  --concurrency 10 \
  --summary-file "tmp/perf-$PERF_RUN_ID.json"

sleep 10

node tools/perf/verifier.mjs \
  --mode event \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 100 \
  --matched-canvas-count 1
```

### 10.3 MQ smoke

MQ 场景要额外注意 RocketMQ broker 广播地址。当前 `rocketmq/broker.conf` 使用 `brokerIP1=127.0.0.1`，适合宿主机后端；后端放进 Docker 后，消费者可能拿到容器内不可达的 `127.0.0.1`。如果 MQ smoke 失败，先把 MQ 场景单独处理，不要影响 direct/event 结论。

推荐处理方式：

```text
1. 先跑 direct/event，拿到 HTTP 执行链路容量。
2. MQ smoke 失败时，把 brokerIP1 调整为后端容器可访问的宿主机 IP。
3. 重启 docker compose 中的 rocketmq-broker service 和 canvas-backend-perf。
4. 再单独跑 MQ 场景。
```

macOS 获取宿主机局域网 IP：

```bash
ipconfig getifaddr en0
```

MQ producer 预热依赖：

```bash
cd /Users/photonpay/project/canvas/tools/perf/mq-producer
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -q -DskipTests dependency:go-offline
mvn -q test
```

发送 MQ smoke：

```bash
cd /Users/photonpay/project/canvas
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_mq_smoke

cd tools/perf/mq-producer
mvn -q exec:java \
  -Dexec.mainClass=org.chovy.canvas.perf.mq.PerfMqProducer \
  -Dexec.args="--name-server localhost:9876 --topic CANVAS_MQ_TRIGGER --tag PERF_MQ --perf-run-id $PERF_RUN_ID --count 100 --user-modulo 1000"

cd /Users/photonpay/project/canvas
sleep 10

node tools/perf/verifier.mjs \
  --mode mq \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 100 \
  --matched-canvas-count 1
```

## 11. 阶梯压测矩阵

每个资源档分别跑 direct、event、MQ。第一轮建议先跑 direct 和 event，MQ 在 broker 网络确认后补跑。

如果只想手工控制每一档，按 11.2 到 11.4 的单档命令执行。如果希望脚本自动逐档执行、自动调用 verifier，并在达到不稳定点时停止，使用 11.5 的阈值脚本。

### 11.1 阶梯

| 档位 | count | concurrency | 用途 |
| --- | ---: | ---: | --- |
| S1 | 1,000 | 10 | 低压稳定性 |
| S2 | 5,000 | 50 | 常规吞吐 |
| S3 | 10,000 | 100 | 中高压 |
| S4 | 30,000 | 200 | 接近边界 |
| S5 | 50,000 | 400 | 探顶 |
| S6 | 100,000 | 800 | 只在 S5 完全稳定后执行 |

停止条件：

- runner `failed > 0`。
- verifier 返回 `FAIL`。
- p95 连续两档显著上升；若额外采集 p99，p99 连续恶化也应停止。
- 后端容器 CPU 长时间超过 `85%`。
- Hikari active 接近最大连接数且出现等待。
- Redis 命令超时或 ops/latency 明显恶化。
- RocketMQ backlog 持续增长。
- MySQL 出现大量慢 SQL、锁等待或连接耗尽。

### 11.2 Direct 阶梯命令

示例 S3：

```bash
cd /Users/photonpay/project/canvas
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_direct_c100

node tools/perf/perf-runner.mjs \
  --mode direct \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id "$DIRECT_CANVAS_ID" \
  --count 10000 \
  --concurrency 100 \
  --summary-file "tmp/perf-$PERF_RUN_ID.json"

node tools/perf/verifier.mjs \
  --mode direct \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 10000 \
  --matched-canvas-count 1
```

幂等重复验证：

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_direct_dup

node tools/perf/perf-runner.mjs \
  --mode direct \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id "$DIRECT_CANVAS_ID" \
  --count 10000 \
  --concurrency 100 \
  --duplicate-rate 0.01 \
  --summary-file "tmp/perf-$PERF_RUN_ID.json"

node tools/perf/verifier.mjs \
  --mode direct \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 10000 \
  --matched-canvas-count 1 \
  --intentional-duplicates 100
```

### 11.3 Event 阶梯命令

示例 S3：

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_event_c100

node tools/perf/perf-runner.mjs \
  --mode event \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --event-code PERF_ORDER_PAID \
  --count 10000 \
  --concurrency 100 \
  --summary-file "tmp/perf-$PERF_RUN_ID.json"

sleep 10

node tools/perf/verifier.mjs \
  --mode event \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 10000 \
  --matched-canvas-count 1
```

### 11.4 MQ 阶梯命令

示例 10,000 条：

```bash
cd /Users/photonpay/project/canvas
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_mq_10000

cd tools/perf/mq-producer
mvn -q exec:java \
  -Dexec.mainClass=org.chovy.canvas.perf.mq.PerfMqProducer \
  -Dexec.args="--name-server localhost:9876 --topic CANVAS_MQ_TRIGGER --tag PERF_MQ --perf-run-id $PERF_RUN_ID --count 10000 --user-modulo 1000"

cd /Users/photonpay/project/canvas
sleep 30

node tools/perf/verifier.mjs \
  --mode mq \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 10000 \
  --matched-canvas-count 1
```

### 11.5 自动阈值脚本

`tools/perf/threshold-runner.mjs` 会按 `count:concurrency` 阶梯运行 `perf-runner.mjs`，再调用 `verifier.mjs` 对账。遇到第一个不稳定档时停止，并输出最后一个稳定档。

Event 自动阈值：

```bash
node tools/perf/threshold-runner.mjs \
  --mode event \
  --base-url "$BASE_URL" \
  --event-code PERF_ORDER_PAID \
  --stages 1000:10,5000:50,10000:100,30000:200,50000:400 \
  --matched-canvas-count 1 \
  --max-failed 0 \
  --max-p95-ms 1000 \
  --wait-after-run-ms 10000 \
  --out-dir tmp/perf-threshold \
  --run-id-prefix "perf_$(date +%Y%m%d_%H%M%S)"
```

Direct 自动阈值：

```bash
node tools/perf/threshold-runner.mjs \
  --mode direct \
  --base-url "$BASE_URL" \
  --canvas-id "$DIRECT_CANVAS_ID" \
  --stages 1000:10,5000:50,10000:100,30000:200,50000:400 \
  --matched-canvas-count 1 \
  --max-failed 0 \
  --max-p95-ms 1000 \
  --wait-after-run-ms 0 \
  --out-dir tmp/perf-threshold \
  --run-id-prefix "perf_$(date +%Y%m%d_%H%M%S)"
```

输出中的关键字段：

```text
verdict = MAX_STAGE_STABLE
  所有配置档都稳定。说明还没打到阈值，可以继续加更高档。

verdict = THRESHOLD_FOUND
  已找到阈值。stableStage 是最后稳定档，failedStage 是第一个不稳定档。

verdict = NO_STABLE_STAGE
  第一档就失败。先修环境、fixture 或准确性问题，不做容量外推。
```

失败原因：

```text
RUNNER_FAILED   HTTP failed 超过 --max-failed。
P95_EXCEEDED    p95 超过 --max-p95-ms。
VERIFIER_FAIL   数据对账失败，不能作为容量数据。
```

## 12. 长稳压测

选择阶梯压测中最高稳定 QPS 的 `70%` 作为长稳目标。当前 `perf-runner` 是固定 count 模型，不是固定时长模型，因此长稳用“大 count + 合理 concurrency”实现。

示例：

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_event_soak

node tools/perf/perf-runner.mjs \
  --mode event \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --event-code PERF_ORDER_PAID \
  --count 300000 \
  --concurrency 100 \
  --summary-file "tmp/perf-$PERF_RUN_ID.json"

sleep 60

node tools/perf/verifier.mjs \
  --mode event \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 300000 \
  --matched-canvas-count 1
```

如果这轮不足 30 分钟，降低 concurrency 或提高 count。长稳期间每 30 秒记录一次监控快照。

## 13. 监控采集

### 13.1 容器资源

```bash
docker stats --no-stream canvas-backend-perf canvas-mysql canvas-redis
docker compose -f docker-compose.local.yml ps rocketmq-broker
docker inspect canvas-backend-perf --format='CPUs={{.HostConfig.NanoCpus}} Memory={{.HostConfig.Memory}}'
```

### 13.2 Actuator / Prometheus

```bash
curl -sS http://localhost:8080/actuator/metrics/jvm.memory.used
curl -sS http://localhost:8080/actuator/metrics/jvm.gc.pause
curl -sS http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl -sS http://localhost:8080/actuator/metrics/hikaricp.connections.pending
curl -sS http://localhost:8080/actuator/prometheus > "tmp/prom-$PERF_RUN_ID.txt"
```

### 13.3 MySQL

```bash
docker exec canvas-mysql mysql -uroot -proot -Dcanvas_db -e "
SHOW GLOBAL STATUS LIKE 'Threads_connected';
SHOW GLOBAL STATUS LIKE 'Questions';
SHOW GLOBAL STATUS LIKE 'Innodb_row_lock%';
SHOW FULL PROCESSLIST;
"
```

统计本轮写入：

```bash
docker exec canvas-mysql mysql -uroot -proot -Dcanvas_db -e "
SELECT COUNT(*) event_log_rows FROM event_log WHERE perf_run_id = '$PERF_RUN_ID';
SELECT COUNT(*) execution_rows FROM canvas_execution WHERE perf_run_id = '$PERF_RUN_ID';
SELECT status, COUNT(*) FROM canvas_execution WHERE perf_run_id = '$PERF_RUN_ID' GROUP BY status;
SELECT COUNT(*) request_rows FROM canvas_execution_request WHERE perf_run_id = '$PERF_RUN_ID';
SELECT COUNT(*) dlq_rows FROM canvas_execution_dlq WHERE perf_run_id = '$PERF_RUN_ID';
"
```

### 13.4 Redis

```bash
docker exec canvas-redis redis-cli INFO stats | egrep 'instantaneous_ops_per_sec|total_commands_processed|rejected_connections|expired_keys|evicted_keys'
docker exec canvas-redis redis-cli INFO clients | egrep 'connected_clients|blocked_clients'
docker exec canvas-redis redis-cli --latency -i 1 -c 5
```

### 13.5 RocketMQ

```bash
docker compose -f docker-compose.local.yml exec rocketmq-broker sh mqadmin consumerProgress \
  -n rocketmq-namesrv:9876 \
  -g GID_CANVAS_ENGINE
```

如果命令不可用，至少记录：

```bash
docker compose -f docker-compose.local.yml logs --tail=200 rocketmq-broker
docker logs --tail=200 canvas-backend-perf | egrep 'MQ_CONSUMER|执行请求|backlog|retry|DLQ'
```

## 14. 结果计算

从 runner summary 计算本机稳定 QPS：

```bash
node -e "const fs=require('fs'); const f=process.argv[1]; const s=JSON.parse(fs.readFileSync(f,'utf8')); console.log((s.success/(s.durationMs/1000)).toFixed(2));" \
  "tmp/perf-$PERF_RUN_ID.json"
```

把满足稳定条件的最高 QPS 记为：

```text
localStableQps
```

本机结果表：

| 资源档 | 场景 | count | concurrency | success | failed | QPS | p95 | p99 | verifier | 结论 |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 2C2G | direct |  |  |  |  |  |  |  |  |  |
| 2C2G | event |  |  |  |  |  |  |  |  |  |
| 4C4G | direct |  |  |  |  |  |  |  |  |  |
| 4C4G | event |  |  |  |  |  |  |  |  |  |

## 15. 最大流量外推

应用层容量：

```text
appCapacity =
localStableQps
* 目标后端总核心数 / 本机后端容器核心数
* CPU 折损系数
```

首轮建议：

```text
CPU 折损系数 = 0.75
安全系数 = 0.5
```

最终服务建议最大流量：

```text
serviceMaxTraffic =
min(
  appCapacity,
  mysqlSafeWriteQps / writesPerRequest,
  redisSafeOps / redisOpsPerRequest,
  rocketmqSafeThroughput,
  downstreamRateLimit / downstreamCallsPerRequest,
  disruptorWorkerCapacity
) * safetyFactor
```

使用项目脚本计算：

```bash
node tools/perf/capacity-report.mjs \
  --verifier-verdict PASS \
  --local-stable-qps 1200 \
  --local-app-cores 4 \
  --prod-app-cores-total 32 \
  --writes-per-event 4 \
  --prod-db-safe-write-qps 12000 \
  --redis-ops-per-event 3 \
  --prod-redis-safe-ops 30000 \
  --rocketmq-capacity 7000 \
  --disruptor-worker-capacity 9000 \
  --downstream-rate-limit-per-sec 5000 \
  --downstream-calls-per-event 1
```

如果没有目标环境参数，只输出本机可用结论：

```text
本机单容器最大稳定流量 = localStableQps
本机建议告警线 = localStableQps * 0.7
本机保守可用容量 = localStableQps * 0.5
```

## 16. 清理

清理分两类：压测账本清理和 fixture 清理。容量测试期间建议优先只清理当前 `perfRunId` 的账本数据，不要反复删除 `PERF_` 事件定义、MQ 定义和已发布画布，否则下一轮 event/MQ 压测需要重新补 fixture 和重新发布路由。

### 16.1 单轮账本清理

只删除当前 `perfRunId` 关联的执行、事件日志、请求、DLQ、人群计算记录和 trace：

```bash
docker exec canvas-mysql mysql -uroot -proot -Dcanvas_db -e "
DROP TEMPORARY TABLE IF EXISTS perf_cleanup_execution_ids;
CREATE TEMPORARY TABLE perf_cleanup_execution_ids (
  id VARCHAR(64) NOT NULL PRIMARY KEY
);
INSERT INTO perf_cleanup_execution_ids (id)
SELECT id FROM canvas_execution WHERE perf_run_id = '$PERF_RUN_ID';

DELETE FROM canvas_execution_trace
WHERE execution_id IN (SELECT id FROM perf_cleanup_execution_ids);
DELETE FROM canvas_execution_dlq WHERE perf_run_id = '$PERF_RUN_ID';
DELETE FROM canvas_execution_request WHERE perf_run_id = '$PERF_RUN_ID';
DELETE FROM canvas_execution WHERE perf_run_id = '$PERF_RUN_ID';
DELETE FROM event_log WHERE perf_run_id = '$PERF_RUN_ID';
DELETE FROM audience_compute_run WHERE perf_run_id = '$PERF_RUN_ID';

DROP TEMPORARY TABLE IF EXISTS perf_cleanup_execution_ids;
"
```

### 16.2 全量压测清理

`tools/perf/cleanup.mjs` 会额外删除 `PERF_%` event definitions 和 MQ definitions。只建议在所有压测结束、准备恢复本地环境时执行。

先 dry-run：

```bash
node tools/perf/cleanup.mjs --perf-run-id "$PERF_RUN_ID"
```

确认输出符合预期后执行：

```bash
node tools/perf/cleanup.mjs --perf-run-id "$PERF_RUN_ID" --execute true
```

全量清理后，如果还要继续 event/MQ 压测，需要重新执行第 9 节中的事件定义和 MQ 定义 SQL，并重新确认画布发布路由。

当前清理脚本不删除 canvas definitions。为了避免重复创建多个已发布 `PERF_EVENT_LIGHT` 或 `PERF_MQ_LIGHT`，fixture 画布建议创建一次后复用。若必须重建，先通过接口下线旧 `PERF_` 画布。

下线旧画布：

```bash
curl -sS -X POST "$BASE_URL/canvas/$EVENT_CANVAS_ID/offline?operator=perf" \
  -H "authorization: Bearer $TOKEN"
```

## 17. 最终报告模板

```text
测试目标:
  评估 canvas-engine 服务最大稳定承接流量，并基于本机容器结果保守外推。

测试环境:
  本机 CPU:
  本机内存:
  Docker Desktop 资源:
  后端镜像:
  后端容器资源:
  JVM 参数:
  MySQL/Redis/RocketMQ/WireMock 版本和部署方式:

本机稳定容量:
  2C2G direct:
  2C2G event:
  4C4G direct:
  4C4G event:
  MQ:
  长稳:

准确性:
  verifier:
  unexpected loss:
  duplicate execution:
  retry pending:
  DLQ:

瓶颈:
  应用 CPU:
  MySQL:
  Redis:
  RocketMQ:
  下游:
  Disruptor/worker:

外推参数:
  localStableQps:
  localAppCores:
  prodAppCoresTotal:
  writesPerRequest:
  redisOpsPerRequest:
  downstreamCallsPerRequest:
  safetyFactor:

外推结果:
  appCapacity:
  mysqlCapacity:
  redisCapacity:
  rocketmqCapacity:
  downstreamCapacity:
  bottleneck:
  serviceMaxTraffic:
  alertThreshold:
  rateLimitThreshold:

结论:
  服务建议最大稳定承接流量为 X QPS。
  建议告警线为 Y QPS。
  建议限流线为 X QPS。
  当前主要瓶颈是 Z。
```

## 18. 验收标准

这份压测方案完成的标准：

- 可以在本机从零启动依赖、构建镜像、启动固定资源后端容器。
- 可以创建或复用 `PERF_` fixture。
- direct/event smoke 均能通过 verifier。
- 至少完成 `2C2G` 和 `4C4G` 两个资源档的阶梯压测。
- 至少完成一个场景的 30 分钟长稳压测。
- 最终报告能给出本机最大稳定容量、外推最大流量、告警线、限流线和瓶颈说明。
