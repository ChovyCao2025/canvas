# 生产实践补审：技术选型与开源组件对标（2026-06-01）

> **背景**：上一版服务划分方案中，Scheduler-Center 设计为自研调度引擎，这是错误的。本文逐项对标生产实践中的标准组件选型。

---

## 一、定时任务调度：XXL-JOB，不是自研

### 为什么不用自研 Scheduler-Center？

写一个调度引擎很简单，但写出 XXL-JOB 级别的生产可靠性需要 3 年以上：
- 任务分片（多实例下同一个任务只执行一次）
- 失败重试 + 重试衰减
- 任务依赖 DAG
- 执行器注册发现
- 调度日志 + 可视化 Dashboard
- 告警通道（邮件/钉钉/飞书）

### 标准选型

| 组件 | 用途 | 为什么 |
|------|------|--------|
| **XXL-JOB** | 业务定时任务 | Java 生态标准，轻量够用，Dashboard 完善 |
| **DolphinScheduler** | 数据平台任务调度 | 数据管道的 DAG 调度（Spark/Flink/DataX），XXL-JOB 不适合 |

### XXL-JOB 接入方案

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.1</version>
</dependency>
```

```yaml
# application.yml
xxl:
  job:
    admin:
      addresses: http://xxl-job-admin:8080/xxl-job-admin
    executor:
      appname: canvas-engine
      port: 9999
      logpath: /data/applogs/xxl-job
      logretentiondays: 30
```

```java
// 定时任务示例
@XxlJob("canvasScheduledTriggerJob")
public void execute() {
    String param = XxlJobHelper.getJobParam();
    Long tenantId = TenantContext.get();
    // 画布定时触发逻辑
    scheduledTriggerService.executeTrigger(param);
}
```

### 迁移计划

| 原有 | 迁移到 | 方式 |
|------|--------|------|
| `@Scheduled` 注解散落各处 | XXL-JOB 统一管理 | 改为 @XxlJob，在 XXL-JOB Admin 配置 Cron |
| Canvas 定时触发节点 | XXL-JOB 动态创建/删除任务 | 画布发布时自动注册，下线时自动删除 |
| Quartz（如有） | XXL-JOB | 逐步替换 |

---

## 二、完整的生产级技术栈

### 2.1 基础设施组件

| 层面 | 组件 | 版本 | 用途 |
|------|------|------|------|
| 框架 | Spring Boot 3.x | 3.3 | 基础框架 |
| JDK | Java 21 | 21 LTS | 虚拟线程 |
| ORM | MyBatis-Plus | 3.5.7 | ORM + 多数据源 |
| **多数据源** | **dynamic-datasource** | **4.3.1** | **@DS 注解路由，比手写多 SqlSessionFactory 靠谱** |
| 数据库 | MySQL 8.0 | 8.0.35+ | 主库 |
| 缓存 | Redis 7.x + Caffeine | 7.2 / 3.x | 双层缓存 |
| **缓存客户端** | **Redisson** | **3.32.0** | **分布式锁 + 分布式集合，不要只用 Lettuce** |
| 消息队列 | RocketMQ | 5.2 | 事件总线 |
| **调度中心** | **XXL-JOB** | **2.4.1** | **业务定时任务** |
| **数据调度** | **DolphinScheduler** | **3.2** | **数据管道 DAG 调度** |
| **配置中心** | **Nacos** | **2.4.0** | **配置管理 + 服务发现** |
| **网关** | **Spring Cloud Gateway** | **4.1** | **API 网关** |
| 熔断降级 | Resilience4j | 2.2 | 轻量熔断 |
| **API 文档** | **Knife4j** | **4.5** | **Swagger 增强，国内用得最多的 API 文档** |
| **分布式 ID** | **Snowflake (MyBatis-Plus 内置)** | **—** | **IdType.ASSIGN_ID** |

### 2.2 可观测性

| 层面 | 组件 | 用途 |
|------|------|------|
| 指标 | Micrometer + Prometheus | 指标采集 |
| 日志 | Logback + **Logstash** → Elasticsearch | 结构化日志 |
| 日志查看 | **Kibana** 或 Grafana Loki | 日志检索 |
| 链路追踪 | **Micrometer Tracing + Brave + Zipkin** | 比直接上 OpenTelemetry+Jaeger 简单 |
| 告警 | Prometheus Alertmanager → **飞书/钉钉/企业微信** | 告警通知 |
| JVM 监控 | **Arthas + Spring Boot Admin** | 线上诊断 + 健康监控 |

### 2.3 CI/CD 与运维

| 层面 | 组件 | 用途 |
|------|------|------|
| CI/CD | **Jenkins** 或 GitLab CI | 构建流水线 |
| 镜像仓库 | **Harbor** | Docker 镜像管理 |
| 容器编排 | Kubernetes 1.30+ | 生产部署 |
| Helm | Helm 3 | 包管理 |
| **制品管理** | **Nexus** | **Maven 私服，不要直接拉 Maven Central** |
| **代码质量** | **SonarQube** | **静态代码分析，CI 流水线中强制通过** |
| **压力测试** | **JMeter** 或 Locust | 性能基准 |

### 2.4 数据平台

| 层面 | 组件 | 用途 |
|------|------|------|
| 消息队列 | Kafka 3.6 | CDC + 事件流 |
| CDC | **Canal** + Kafka | MySQL binlog → Kafka |
| 数据湖 | Iceberg 1.5 + MinIO | ODS/DWD |
| 实时计算 | Flink 1.18 | 增量计算 |
| 批量计算 | Spark 3.5 | 全量重建 |
| OLAP | ClickHouse 24.x | 快速分析查询 |
| 数据治理 | DataHub | 血缘+质量 |
| **可视化** | **Superset** 或 Metabase | **数据分析看板，不要自研** |

---

## 三、几个之前文档中没说清楚的生产问题

### 3.1 分布式锁：Redisson，不是自己写的 Redis 锁

```java
// 画布同一时刻只能一个实例执行
@Service
public class CanvasExecutionService {
    
    public ExecutionResult execute(Long canvasId) {
        String lockKey = "lock:canvas:execute:" + canvasId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 等待 5 秒，锁 60 秒自动释放（防止死锁）
            if (lock.tryLock(5, 60, TimeUnit.SECONDS)) {
                return doExecute(canvasId);
            } else {
                throw new BusinessException("画布正在执行中，请稍后再试");
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    // 人群计算分布式锁——防止同时计算同一个人群
    public void computeAudience(Long audienceId) {
        String lockKey = "lock:audience:compute:" + audienceId;
        RLock lock = redissonClient.getFairLock(lockKey); // 公平锁，先到先得
        lock.lock(120, TimeUnit.SECONDS); // 人群计算可能很久
        try {
            doCompute(audienceId);
        } finally {
            lock.unlock();
        }
    }
}
```

### 3.2 配置文件管理：Nacos，不是 application.yml 硬编码

```
Nacos Config 配置分层:

命名空间:
  ├── canvas-prod (生产环境)
  ├── canvas-staging (预发环境)
  └── canvas-dev (开发环境)

每个命名空间下的配置:
  ├── canvas-gateway.yaml         # 网关路由
  ├── canvas-engine.yaml          # 引擎配置
  ├── canvas-cdp.yaml             # CDP 配置
  ├── shared-datasource.yaml      # 共享：数据源
  ├── shared-redis.yaml           # 共享：Redis
  ├── shared-rocketmq.yaml        # 共享：RocketMQ
  └── tenant-quotas.yaml          # 租户配额（动态刷新）
```

```yaml
# Nacos 中的 canvas-engine.yaml
canvas:
  execution:
    globalTimeout: 600
    maxConcurrency: 3000
  tenant:
    quotas:
      1001:
        max-canvas: 100
        max-concurrent-exec: 10
      1002:
        max-canvas: 5
        max-concurrent-exec: 1

# @RefreshScope 支持热更新
# 修改租户配额后无需重启，60 秒内生效
```

### 3.3 API 文档：Knife4j，每个服务自动生成

```java
// 每个 Controller 必须有文档
@Tag(name = "画布管理", description = "画布的创建、编辑、发布、执行")
@RestController
@RequestMapping("/api/v1/canvas")
public class CanvasController {
    
    @Operation(summary = "创建画布", description = "创建一个新的营销画布，包含节点和边的定义")
    @PostMapping
    public Result<CanvasVO> createCanvas(@RequestBody @Valid CreateCanvasRequest request) {
        // ...
    }
    
    @Operation(summary = "执行画布", description = "手动触发画布执行，返回执行ID用于追踪")
    @ApiResponse(responseCode = "200", description = "执行已提交")
    @ApiResponse(responseCode = "429", description = "租户并发配额已满")
    @PostMapping("/{id}/execute")
    public Result<ExecutionVO> executeCanvas(@PathVariable Long id) {
        // ...
    }
}
```

### 3.4 结构化日志：Logstash JSON 格式

```xml
<!-- logback-spring.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```xml
<appender name="LOGSTASH" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/var/log/canvas-engine/application.log</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>tenantId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
        <customFields>{"app":"canvas-engine","env":"prod"}</customFields>
    </encoder>
</appender>
```

输出的 JSON 日志格式：
```json
{
  "timestamp": "2026-06-01T10:30:00.123+08:00",
  "level": "INFO",
  "logger": "com.canvas.engine.scheduler.DagEngine",
  "message": "Canvas execution completed",
  "app": "canvas-engine",
  "env": "prod",
  "traceId": "a1b2c3d4e5f6",
  "tenantId": "1001",
  "userId": "67890",
  "executionId": "12345",
  "durationMs": 2300,
  "nodeCount": 15
}
```

### 3.5 服务间调用：Feign + Sentinel 而不是自己写 HTTP 调用

```java
// 之前文档写了 Feign 接口，但没有配熔断
// 生产必须配 Sentinel

@FeignClient(
    name = "cdp-service",
    path = "/api/v1",
    fallbackFactory = CdpFeignClientFallbackFactory.class  // ← 关键: 熔断降级工厂
)
public interface CdpFeignClient {
    
    @GetMapping("/users/{userId}")
    Result<UserVO> getUser(@PathVariable Long userId);
    
    @GetMapping("/users/resolve")
    Result<UserIdentity> resolve(
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String externalUserId
    );
}

// Fallback: 服务不可用时的降级
@Component
public class CdpFeignClientFallbackFactory implements FallbackFactory<CdpFeignClient> {
    @Override
    public CdpFeignClient create(Throwable cause) {
        log.error("CDP-Service 调用失败", cause);
        return new CdpFeignClient() {
            @Override
            public Result<UserVO> getUser(Long userId) {
                return Result.fail("CDP服务暂时不可用，请稍后重试");
            }
            @Override
            public Result<UserIdentity> resolve(...) {
                return Result.fail("身份解析服务暂时不可用");
            }
        };
    }
}
```

### 3.6 Spring Boot Admin：线上服务健康监控

```xml
<!-- 每个服务引入 -->
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-client</artifactId>
    <version>3.3.0</version>
</dependency>
```

```yaml
# 每个服务配置
spring:
  boot:
    admin:
      client:
        url: http://admin-server:8099
        instance:
          service-url: http://${spring.cloud.client.ip-address}:${server.port}
```

Spring Boot Admin Dashboard 可以看：
- 12 个服务的健康状况
- JVM 内存/线程/GC
- 日志级别动态调整（线上查问题时临时开 DEBUG）
- 配置属性查看
- 定时任务执行状态（XXL-JOB 已自带 Dashboard）

### 3.7 数据库优化：哪些查询需要 ClickHouse，哪些走 MySQL

| 查询场景 | 数据量 | 走哪里 | 为什么 |
|---------|--------|--------|--------|
| 单用户标签查询 | 1-N 条 | MySQL cdp_db | 走索引，<5ms |
| 画布 CRUD | 单条 | MySQL canvas_db | 事务支持 |
| 画布执行状态 | 单条 | Redis | 高频更新，无需持久化到 MySQL |
| 人群圈选 (>100万用户) | 千万-亿 | ClickHouse | MySQL 会 OOM |
| 人群圈选 (<100万用户) | 百万 | MySQL cdp_db | 简单规则，走 Bitmap + Redis |
| 标签分布统计 | 聚合 | ClickHouse | 列存，聚合查询比 MySQL 快 10-100x |
| 事件漏斗分析 | 明细+聚合 | ClickHouse | windowFunnel 是 ClickHouse 独占函数 |
| 用户 360 视图 | 单用户+全部标签 | MySQL + Redis | 已有缓存，无需 ClickHouse |
| 数据导出 | 千万+ | MinIO (Spark 生成 CSV) | 不经过应用服务器 |

---

## 四、纠正：哪些之前文档中的设计需要改

| 原设计 | 问题 | 改为 |
|--------|------|------|
| Scheduler-Center 自研 | 生产不可靠 | **XXL-JOB**（业务任务）+ **DolphinScheduler**（数据任务） |
| WebClient/RestClient 直接调用 | 没熔断 | **Feign + Sentinel** |
| application.yml 硬编码配置 | 改配置要重启 | **Nacos Config + @RefreshScope** |
| 自写 Redis 分布式锁 | Lua 脚本可能有 Bug | **Redisson**（锁续期/红锁/读写锁都实现好了） |
| OpenTelemetry + Tempo 全上 | 太重，团队可能不会用 | **Micrometer Tracing + Brave + Zipkin**（Spring 官方方案，更轻） |
| Grafana Loki 收日志 | 可以的，但没配结构化 | **Logback JSON Encoder → Logstash → ES/Kibana** 或 **Loki + Promtail** |
| 自己画定时任务 Dashboard | 重造轮子 | XXL-JOB 自带 Dashboard |
| 自建 CI/CD | 没必要 | **Jenkins** / **GitLab CI**，配好就行 |
| 没有 API 文档方案 | 缺失 | **Knife4j**，每个服务 /doc.html |
| 没有 Maven 私服 | 缺失 | **Nexus**，CI 流水线只拉私服 |
| 没有代码质量门禁 | 缺失 | **SonarQube**，CI 强制通过 |
| 没有分布式 ID 方案 | 缺失 | **MyBatis-Plus IdType.ASSIGN_ID**（Snowflake 内置） |

---

## 五、最终技术栈全景

```
                     ┌──────────────┐
                     │   Nginx/CDN  │
                     └──────┬───────┘
                            │
              ┌─────────────▼─────────────┐
              │  Spring Cloud Gateway     │  ← 网关
              │  + Sentinel 限流           │
              └─────────────┬─────────────┘
                            │
        ┌───────────┬───────┼───────┬───────────┐
        ▼           ▼       ▼       ▼           ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
   │ Canvas  │ │  CDP    │ │  WeCom  │ │ 其他服务 │  ← 12 个 Spring Boot 服务
   │ Engine  │ │ Service │ │ Service │ │         │
   └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘
        │           │           │           │
        └───────────┼───────────┼───────────┘
                    │           │
     ┌──────────────┼───────────┼──────────────┐
     │              ▼           ▼              │
     │  ┌──────────────────────────────┐      │
     │  │        Nacos (配置+注册)      │      │
     │  └──────────────────────────────┘      │
     │  ┌──────────┐  ┌──────────────────┐    │
     │  │ XXL-JOB  │  │ DolphinScheduler │    │
     │  │ (业务调度)│  │ (数据调度)        │    │
     │  └──────────┘  └──────────────────┘    │
     │           共享基础设施                   │
     └────────────────────────────────────────┘
              │           │           │
     ┌────────▼───┐ ┌─────▼──────┐ ┌─▼──────────┐
     │ MySQL 3库  │ │ Redis HA   │ │ RocketMQ    │
     │ (HikariCP) │ │ (Redisson) │ │ 集群         │
     └────────────┘ └────────────┘ └─────────────┘
              │           │           │
     ┌────────▼───┐ ┌─────▼──────┐ ┌─▼──────────┐
     │ Kafka      │ │ ClickHouse │ │ MinIO       │
     │ (CDC+事件) │ │ (OLAP)     │ │ (对象存储)  │
     └────────────┘ └────────────┘ └─────────────┘

     可观测性:
     ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
     │Prometheus│ │  Zipkin  │ │   ELK    │ │Spring Boot│
     │+Grafana  │ │ (链路)   │ │ (日志)   │ │  Admin   │
     └──────────┘ └──────────┘ └──────────┘ └──────────┘

     CI/CD:
     ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
     │ Jenkins  │ │  Nexus   │ │  Harbor  │ │SonarQube │
     │ (流水线) │ │ (Maven)  │ │ (镜像)   │ │ (质量)   │
     └──────────┘ └──────────┘ └──────────┘ └──────────┘
```

---

## 六、相关文档

- [架构师审查报告：8个关键缺失](./architect-critical-review.md)
- [服务划分与新应用搭建方案](./service-architecture-design.md)
- [架构演进路线图](./architecture-evolution-roadmap.md)
- [目标架构总览](./target-architecture-overview.md)
