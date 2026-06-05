# 架构整改方案 — ops
> 详见 [README.md](./README.md) 获取完整索引


## 第四部分：运维与基础设施问题（19项）

> 以下问题来自对 Docker/监控/配置/数据库/MQ 运维维度扫描。

---

### 问题十六：无生产级部署方案

#### 现状
- 仅有 `docker-compose.local.yml`，无 `docker-compose.prod.yml`
- 无 Kubernetes manifests 或 Helm charts
- 部署文档标注 "Missing: CI/CD Pipeline, Staging Environment"
- Dockerfile 无资源限制、非 root 用户

#### 实施方案

**Step 1: 生产级 Docker Compose**

```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  canvas-engine:
    image: canvas-engine:${VERSION}
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 512M
    environment:
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - CANVAS_JWT_SECRET=${JWT_SECRET}
      - SPRING_PROFILES_ACTIVE=prod
    user: "1000:1000"  # 非 root
```

**Step 2: Kubernetes manifests**

```
k8s/
├── namespace.yaml
├── deployment.yaml
├── service.yaml
├── configmap.yaml
├── secret.yaml
├── hpa.yaml          # 水平自动扩缩
└── ingress.yaml
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 生产级 docker-compose | 4h |
| 2 | Dockerfile 安全加固 | 2h |
| 3 | Kubernetes manifests | 8h |
| 4 | CI/CD pipeline (GitHub Actions) | 8h |
| 5 | 部署文档更新 | 2h |

**总工时**: ~24h

---

### 问题十七：配置管理缺陷

#### 现状
- 数据库密码硬编码（P0，同S1）
- JWT Secret 无启动校验（P0，同S3）
- 事件上报密钥弱默认值（P0，同S2）
- 无 Profile 特定配置（缺少 application-prod.yml）
- 无配置加密方案

#### 实施方案

```
src/main/resources/
├── application.yml           # 公共配置（无敏感值）
├── application-dev.yml       # 开发环境（默认值、宽松安全）
├── application-staging.yml   # 预发布环境
└── application-prod.yml      # 生产环境（必须设置环境变量）
```

```yaml
# application-prod.yml
spring:
  datasource:
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
canvas:
  jwt:
    secret: ${CANVAS_JWT_SECRET}
  cors:
    allowed-origins: ${CANVAS_CORS_ALLOWED_ORIGINS}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 创建 Profile 配置文件 | 4h |
| 2 | 敏感值外部化 | 2h |
| 3 | 启动校验 | 2h |
| 4 | 测试各 Profile 启动 | 2h |

**总工时**: ~10h

---

### 问题十八：可观测性缺失

#### 现状
- 无分布式追踪（Micrometer Tracing + Jaeger）
- 无日志聚合（ELK/Loki）
- 无告警系统（Prometheus AlertManager）
- Actuator health 暴露过多细节

#### 实施方案

**Step 1: 分布式追踪**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

**Step 2: Prometheus 告警规则**

```yaml
# alerting-rules.yml
groups:
  - name: canvas-alerts
    rules:
      - alert: HighExecutionFailureRate
        expr: rate(canvas_execution_failed_total[5m]) / rate(canvas_execution_total[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Canvas execution failure rate > 10%"
      - alert: CircuitBreakerOpen
        expr: canvas_circuit_breaker_state{state="OPEN"} == 1
        for: 1m
        labels:
          severity: warning
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 分布式追踪集成 | 8h |
| 2 | Prometheus 告警规则 | 4h |
| 3 | 日志聚合配置（Loki） | 4h |
| 4 | Grafana Dashboard | 4h |

**总工时**: ~20h

---

### 问题十九：RocketMQ 配置不完整

#### 现状
- 消费者线程数硬编码
- 未配置 `maxReconsumeTimes`
- 消息幂等依赖下游实现

#### 实施方案

```yaml
# application.yml
rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:localhost:9876}
  consumer:
    group: GID_CANVAS_ENGINE
    max-reconsume-times: 5          # 新增
    consume-thread-number: ${MQ_CONSUME_THREADS:20}  # 可配置化
```

```java
// MqTriggerConsumer — 消费者端幂等去重
@RocketMQMessageListener(
    topic = "${canvas.mq.trigger-topic}",
    consumerGroup = "GID_CANVAS_ENGINE",
    maxReconsumeTimes = 5
)
public class MqTriggerConsumer implements RocketMQListener<MessageExt> {
    @Override
    public void onMessage(MessageExt msg) {
        String msgId = msg.getMsgId();
        if (dedupCache.putIfAbsent(msgId, true) != null) {
            log.info("[MQ] Duplicate message skipped: {}", msgId);
            return;  // 消费者端去重
        }
        // ... 处理逻辑
    }
}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | MQ 配置外部化 | 1h |
| 2 | 消费者端幂等去重 | 4h |
| 3 | maxReconsumeTimes 配置 | 1h |
| 4 | 测试 | 2h |

**总工时**: ~8h

---

### 问题二十：Flyway 无回滚策略

#### 现状
- 89个迁移脚本全为正向 SQL，无 undo 脚本
- 生产迁移失败无法自动回滚

#### 实施方案

**策略**: Flyway Community 版不支持 undo，采用以下替代方案：

1. **基线快照**: 每次重大迁移前对数据库做逻辑备份
2. **可逆迁移**: 关键迁移脚本配套回滚 SQL 文档
3. **蓝绿部署**: 新版本用新数据库，验证通过后切换

```
db/migration/
├── V90__add_campaign_table.sql
└── rollback/
    └── V90_rollback.sql          # 手动回滚脚本（非自动执行）
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 建立迁移回滚规范文档 | 2h |
| 2 | 为 V80+ 迁移编写回滚 SQL | 4h |
| 3 | 迁移前自动备份脚本 | 2h |

**总工时**: ~8h

