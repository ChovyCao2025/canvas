# 多数据源隔离方案 (2026-05-31)

> **定位**: 65表共享HikariCP(max=33)→3库隔离，消除批量查询饿死其他域

---

## 一、问题分析

### 1.1 当前问题

| 问题 | 代码位置 | 影响 |
|------|---------|------|
| 单库65表 | canvas_db承载全域 | 批量SQL(人群计算/标签导入)可饿死画布CRUD |
| 单HikariCP max=33 | 全域共享 | CDP批量查询占满连接池，画布操作等待 |
| 无连接池隔离 | 同一个DataSource | 无法按域限流 |
| tenant_id nullable且ORM不可见 | 22.3 | 多租户形同虚设 |

### 1.2 连接池瓶颈量化

```
画布CRUD: 平均3-5个连接 (低延迟)
画布执行: 平均8-10个连接 (节点执行+状态更新)
CDP人群计算: 平均15-20个连接 (批量SQL+Bitmap)
标签导入: 平均10-15个连接 (批量插入)

峰值: 3+10+20+15 = 48 > 33 (连接池溢出)
```

---

## 二、3库隔离方案

### 2.1 数据库划分

| 数据库 | 包含的域 | 核心表 | 连接池 |
|--------|---------|--------|--------|
| **canvas_db** | Canvas Core + Execution + Notification | canvas, node, edge, execution, message, delivery, frequency_cap, circuit_breaker_state | max=20 |
| **cdp_db** | CDP-Profile + Audience | user_profile, tag, tag_group, audience, audience_rule, bitmap_segment, user_tag_rel | max=30 |
| **meta_db** | Platform Meta + Audit | tenant, tenant_config, audit_log, flyway_schema_history, node_type_registry, channel_registry | max=10 |

### 2.2 表归属详细划分

#### canvas_db (保留)

```
Canvas Core:
  - canvas, canvas_version, canvas_node, canvas_edge
  - canvas_execution, execution_context

Notification:
  - message_record, delivery_record
  - channel_config, channel_registry

Execution Engine:
  - circuit_breaker_state
  - frequency_cap_record
  - lane_state
  - wait_state
  - goal_check_state

Policy:
  - marketing_policy
  - suppression_record
  - quiet_hours_record

Business:
  - coupon_record
  - points_record
  - experiment_record
  - ab_split_record
  - scoring_record
```

#### cdp_db (新建，从canvas_db迁移)

```
CDP-Profile:
  - user_profile
  - user_attribute
  - user_tag_rel
  - tag, tag_group
  - tag_import_record

Audience:
  - audience, audience_rule
  - audience_execution_record
  - bitmap_segment
  - segment_rule
  - audience_bitmap_store (RoaringBitmap)

Rule Engine:
  - rule_config
  - rule_execution_log
```

#### meta_db (新建，从canvas_db迁移)

```
Platform Meta:
  - tenant, tenant_config
  - tenant_quota, tenant_usage

Audit:
  - audit_log
  - operation_record

Registry:
  - node_type_registry
  - channel_registry
  - handler_config

Flyway:
  - flyway_schema_history (per DB)

System:
  - scheduled_task
  - system_config
```

---

## 三、技术实现

### 3.1 多数据源配置

```java
@Configuration
public class DataSourceConfig {

    @Primary
    @Bean("canvasDataSource")
    @ConfigurationProperties("spring.datasource.canvas")
    public DataSource canvasDataSource() {
        return HikariDataSourceBuilder.create().build();
    }

    @Bean("cdpDataSource")
    @ConfigurationProperties("spring.datasource.cdp")
    public DataSource cdpDataSource() {
        return HikariDataSourceBuilder.create().build();
    }

    @Bean("metaDataSource")
    @ConfigurationProperties("spring.datasource.meta")
    public DataSource metaDataSource() {
        return HikariDataSourceBuilder.create().build();
    }
}
```

### 3.2 MyBatis-Plus多数据源路由

#### 方案A: 动态数据源 (推荐)

使用 `dynamic-datasource-spring-boot-starter`：

```yaml
spring:
  datasource:
    dynamic:
      primary: canvas
      strict: true
      datasource:
        canvas:
          url: jdbc:mysql://localhost:3306/canvas_db
          username: canvas_user
          password: ${DB_CANVAS_PASSWORD}
          hikari:
            maximum-pool-size: 20
            minimum-idle: 5
            idle-timeout: 30000
            max-lifetime: 1800000
        cdp:
          url: jdbc:mysql://localhost:3306/cdp_db
          username: cdp_user
          password: ${DB_CDP_PASSWORD}
          hikari:
            maximum-pool-size: 30
            minimum-idle: 8
            idle-timeout: 30000
            max-lifetime: 1800000
        meta:
          url: jdbc:mysql://localhost:3306/meta_db
          username: meta_user
          password: ${DB_META_PASSWORD}
          hikari:
            maximum-pool-size: 10
            minimum-idle: 2
            idle-timeout: 30000
            max-lifetime: 1800000
```

#### Mapper路由注解

```java
// canvas域Mapper - 默认数据源
@Mapper
public interface CanvasMapper extends BaseMapper<CanvasDO> {
    // 自动路由到canvas数据源
}

// cdp域Mapper - 明确指定数据源
@DS("cdp")
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfileDO> {
    // 路由到cdp数据源
}

// meta域Mapper - 明确指定数据源
@DS("meta")
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogDO> {
    // 路由到meta数据源
}
```

#### Service层路由

```java
// 跨域Service方法 - 明确指定数据源
@DS("cdp")
@Service
public class AudienceService {
    public AudienceVO computeAudience(Long audienceId) {
        // 所有DB操作路由到cdp数据源
        // 不会影响canvas连接池
    }
}

@DS("meta")
@Service
public class AuditService {
    public void log(AuditEntry entry) {
        // 路由到meta数据源
    }
}
```

#### 方案B: 手动SqlSession路由

```java
@Configuration
public class MybatisMultiDsConfig {

    @Bean("canvasSqlSessionFactory")
    public SqlSessionFactory canvasSqlSessionFactory(@Qualifier("canvasDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(ds);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
            .getResources("classpath*:mapper/canvas/**/*.xml"));
        return bean.getObject();
    }

    @Bean("cdpSqlSessionFactory")
    public SqlSessionFactory cdpSqlSessionFactory(@Qualifier("cdpDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(ds);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
            .getResources("classpath*:mapper/cdp/**/*.xml"));
        return bean.getObject();
    }

    @Bean("metaSqlSessionFactory")
    public SqlSessionFactory metaSqlSessionFactory(@Qualifier("metaDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(ds);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
            .getResources("classpath*:mapper/meta/**/*.xml"));
        return bean.getObject();
    }
}
```

---

## 四、Flyway多库迁移

### 4.1 迁移文件目录结构

```
backend/canvas-engine/src/main/resources/
├── db/migration/
│   ├── canvas/          # canvas_db迁移
│   │   ├── V1__init.sql
│   │   ├── ...
│   │   └── V82__wecom_customer_tables.sql
│   ├── cdp/             # cdp_db迁移
│   │   ├── V1__cdp_init.sql
│   │   ├── ...
│   │   └── V20__bitmap_segment.sql
│   ├── meta/            # meta_db迁移
│   │   ├── V1__meta_init.sql
│   │   ├── ...
│   │   └── V10__tenant_quota.sql
```

### 4.2 多Flyway配置

```java
@Configuration
public class FlywayMultiDsConfig {

    @Bean(initMethod = "migrate")
    public Flyway canvasFlyway(@Qualifier("canvasDataSource") DataSource ds) {
        return Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration/canvas")
            .schemas("canvas_db")
            .placeholderReplacement(false)
            .load();
    }

    @Bean(initMethod = "migrate")
    public Flyway cdpFlyway(@Qualifier("cdpDataSource") DataSource ds) {
        return Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration/cdp")
            .schemas("cdp_db")
            .placeholderReplacement(false)
            .load();
    }

    @Bean(initMethod = "migrate")
    public Flyway metaFlyway(@Qualifier("metaDataSource") DataSource ds) {
        return Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration/meta")
            .schemas("meta_db")
            .placeholderReplacement(false)
            .load();
    }
}
```

---

## 五、跨库事务处理

### 5.1 原则：避免跨库事务

| 场景 | 处理方式 |
|------|---------|
| 单域操作 | 本库@Transactional |
| 跨域读 | 多次查询，无事务 |
| 跨域写 | **Saga模式 / Outbox模式** |
| 画布执行+用户打标签 | Canvas写canvas_db + Tag写cdp_db → Outbox+异步 |

### 5.2 Outbox模式实现

```java
// Canvas域写入Outbox表(canvas_db内)
@Table(name = "canvas_outbox_event")
public class CanvasOutboxEvent {
    private Long id;
    private String eventType;    // "TAG_WRITE" / "AUDIENCE_UPDATE" / "AUDIT_LOG"
    private String payload;      // JSON
    private String targetDs;     // "cdp" / "meta"
    private String status;       // "PENDING" / "SENT" / "CONFIRMED"
    private LocalDateTime createdAt;
}

// CanvasService写入时，同步写Outbox
@Transactional("canvasTransactionManager")
public void publishCanvas(Long canvasId) {
    canvasMapper.updateStatus(canvasId, "PUBLISHED");

    // 写Outbox事件
    outboxEventMapper.insert(new CanvasOutboxEvent(
        "CANVAS_PUBLISHED", toJson(publishEvent), "meta", "PENDING", now
    ));
}

// 定时任务扫描Outbox，投递到对应库
@Scheduled(fixedDelay = 1000)
@DS("canvas")  // 从canvas库读Outbox
public void processOutbox() {
    List<CanvasOutboxEvent> events = outboxEventMapper.selectPending();
    for (CanvasOutboxEvent event : events) {
        switch (event.getTargetDs()) {
            case "cdp" -> processCdpEvent(event);
            case "meta" -> processMetaEvent(event);
        }
        outboxEventMapper.updateStatus(event.getId(), "CONFIRMED");
    }
}

@DS("cdp")
private void processCdpEvent(CanvasOutboxEvent event) {
    // 写入cdp库
}
```

### 5.3 MQ模式（异步解耦）

```java
// Canvas域发布事件到RocketMQ
@Transactional("canvasTransactionManager")
public void publishCanvas(Long canvasId) {
    canvasMapper.updateStatus(canvasId, "PUBLISHED");

    // 先写Outbox（确保事务一致性）
    outboxEventMapper.insert(event);

    // 异步投递到MQ（Outbox扫描器负责）
}

// CDP域订阅MQ事件
@RocketMQMessageListener(topic = "canvas-event", consumerGroup = "cdp-consumer")
public class CdpEventHandler implements RocketMQListener<CanvasEvent> {
    @DS("cdp")
    @Transactional("cdpTransactionManager")
    public void onMessage(CanvasEvent event) {
        // 在cdp库中处理
    }
}
```

---

## 六、数据迁移步骤

### 6.1 从canvas_db迁移表到cdp_db

```sql
-- Step 1: 在cdp_db中创建表结构
-- (通过Flyway迁移脚本)

-- Step 2: 数据迁移（停服迁移）
INSERT INTO cdp_db.user_profile SELECT * FROM canvas_db.user_profile;
INSERT INTO cdp_db.tag SELECT * FROM canvas_db.tag;
INSERT INTO cdp_db.tag_group SELECT * FROM canvas_db.tag_group;
INSERT INTO cdp_db.user_tag_rel SELECT * FROM canvas_db.user_tag_rel;
INSERT INTO cdp_db.audience SELECT * FROM canvas_db.audience;
INSERT INTO cdp_db.audience_rule SELECT * FROM canvas_db.audience_rule;
INSERT INTO cdp_db.bitmap_segment SELECT * FROM canvas_db.bitmap_segment;

-- Step 3: 验证数据一致性
SELECT COUNT(*) FROM canvas_db.user_profile;
SELECT COUNT(*) FROM cdp_db.user_profile;
-- 确认一致后删除canvas_db中的原表

-- Step 4: 清理canvas_db中的迁移表
DROP TABLE canvas_db.user_profile;
DROP TABLE canvas_db.tag;
-- ...
```

### 6.2 从canvas_db迁移表到meta_db

```sql
-- 同上流程，迁移tenant/audit_log/config等表到meta_db
```

---

## 七、连接池监控

### 7.1 HikariCP指标暴露

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    tags:
      datasource: ${spring.datasource.dynamic.primary}
    export:
      prometheus:
        enabled: true
```

### 7.2 告警规则

```yaml
# Prometheus alert
- alert: CanvasConnectionPoolExhausted
  expr: hikaricp_connections_active{pool="canvas"} / hikaricp_connections_max{pool="canvas"} > 0.8
  for: 3m
  labels:
    severity: warning

- alert: CdpConnectionPoolExhausted
  expr: hikaricp_connections_active{pool="cdp"} / hikaricp_connections_max{pool="cdp"} > 0.8
  for: 3m
  labels:
    severity: warning
```

---

## 八、tenant_id强制可见

### 8.1 问题回顾

当前22.3: tenant_id nullable且ORM不可见，多租户形同虚设。

### 8.2 修复方案

```java
// 所有DO实体添加tenant_id字段
@TableField(value = "tenant_id", fill = FieldFill.INSERT)
private Long tenantId;

// MyBatis-Plus自动填充
@Component
public class TenantIdHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "tenantId", Long.class, TenantContext.getCurrentTenantId());
    }
}

// 全局查询条件追加tenant_id
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
        new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContext.getCurrentTenantId());
            }
            @Override
            public boolean ignoreTable(String tableName) {
                return "flyway_schema_history".equals(tableName);
            }
        }
    ));
    return interceptor;
}
```

---

## 九、实施时间表

| 周 | 内容 | 工时 |
|---|------|------|
| Week 1 | 创建3库 + Flyway迁移脚本 | 8h |
| Week 2 | 配置dynamic-datasource + Mapper路由 | 16h |
| Week 3 | 数据迁移 + 验证一致性 | 8h |
| Week 4 | Outbox模式实现 | 8h |
| Week 5 | 跨域事务改造 + 测试 | 8h |
| Week 6 | tenant_id强制可见 + 连接池监控 | 8h |
| **合计** | | **48h** |

---

## 十、相关文档

- [目标架构总览](./target-architecture-overview.md)
- [企微SCRM模块设计](./wecom-scrm-module-design.md)
- [K8s部署方案](./k8s-deployment-plan.md)
- [WebFlux→Spring MVC迁移](./webflux-to-mvc-migration.md)