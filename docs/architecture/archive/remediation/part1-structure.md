# 架构整改方案 — structure
> 详见 [README.md](README.md) 获取完整索引


## 问题一：单体巨石无服务边界

### 现状
- 65个DO实体、30个Controller、49个Handler、50+个Mapper全部堆在 `canvas-engine` 单模块
- 6个业务限界上下文零隔离：Canvas Core / Execution Engine / CDP-User Profile / Audience / Notification / Platform Meta
- 所有域共享同一个Spring容器、同一个HikariCP连接池、同一条Flyway迁移链

### 影响
- 无法独立扩缩容（CDP批量计算吃连接池会饿死Canvas Core）
- 无法独立部署/回滚
- 代码边界模糊，跨域调用随意
- 团队协作冲突率高

### 实施方案：Phase 1 模块化拆分

#### 1.1 目标结构

```
canvas-parent
├── canvas-cache-sdk              (已有，不变)
├── canvas-common                 (新增：枚举、工具、共享DTO、异常定义)
├── canvas-engine-core            (DAG引擎：scheduler/handler/context/dag/disruptor/lane)
├── canvas-canvas-api             (画布CRUD/版本/模板)
├── canvas-cdp-api                (用户画像/标签/身份)
├── canvas-audience-api           (人群计算/规则/Bitmap)
├── canvas-notification-api       (通知/WebSocket/实时推送)
├── canvas-meta-api               (元数据/数据源/MQ定义/系统配置)
└── canvas-engine-app             (Spring Boot启动器，聚合所有模块)
```

#### 1.2 模块依赖规则

```
canvas-engine-app → canvas-canvas-api, canvas-cdp-api, canvas-audience-api, ...
canvas-canvas-api → canvas-engine-core, canvas-common
canvas-engine-core → canvas-common
canvas-common → (无内部依赖)
```

**禁止**: `canvas-cdp-api` 直接依赖 `canvas-canvas-api` 的 DO/Mapper。跨域调用必须通过接口或事件。

#### 1.3 实施步骤

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 创建 `canvas-common` 模块，迁移 `common/` 包下所有类 | 2h |
| 2 | 创建 `canvas-engine-core` 模块，迁移 `engine/` 包（scheduler/handler/context/dag/disruptor/lane） | 4h |
| 3 | 创建 `canvas-meta-api` 模块，迁移 Meta/DataSource/MQ/Event/Tag 相关DO/Mapper/Service/Controller | 4h |
| 4 | 创建 `canvas-canvas-api` 模块，迁移 Canvas/CanvasVersion/CanvasTemplate 相关类 | 4h |
| 5 | 创建 `canvas-cdp-api` 模块，迁移 CdpUser/CdpTag/CdpIdentity 相关类 | 4h |
| 6 | 创建 `canvas-audience-api` 模块，迁移 Audience/AudienceCompute 相关类 | 3h |
| 7 | 创建 `canvas-notification-api` 模块，迁移 Notification/WebSocket 相关类 | 3h |
| 8 | 创建 `canvas-engine-app` 启动器，聚合所有模块，迁移 `CanvasEngineApplication.java` | 2h |
| 9 | 调整所有 `@ComponentScan`、`@MapperScan`、Flyway配置 | 4h |
| 10 | 全量测试 + 修复循环依赖 | 8h |

**总工时**: ~38h（约5人日）

#### 1.4 验收标准
- [ ] `mvn clean install` 全部模块通过
- [ ] 无循环依赖警告
- [ ] 所有现有测试通过
- [ ] 应用启动正常，功能回归测试通过
- [ ] 每个模块有独立的 `pom.xml` 和 `README.md`

---

## 问题二：WebFlux + MyBatis-Plus 根本矛盾

### 现状
- 使用 Spring WebFlux（Netty事件循环）
- 使用 MyBatis-Plus（JDBC阻塞IO）
- 检测到 **30+个文件** 中存在 `.block()` 调用
- 仅 **5个文件** 正确使用 `Schedulers.boundedElastic()`

### 影响
- Netty事件循环被阻塞，高并发下会饥饿
- WebFlux的非阻塞优势完全丧失
- MyBatis-Plus的便利性也因Reactor包装而打折
- 两边生态优势都没发挥

### 实施方案：Phase 2 解决Reactor矛盾

#### 2.1 方案对比

| 方案 | 改动量 | 风险 | 效果 |
|------|--------|------|------|
| **A. 全面Reactor化** | 大（所有Mapper改R2DBC） | 高（MyBatis-Plus生态不支持） | 真正非阻塞 |
| **B. 回退Spring MVC** | 中（WebFlux→MVC） | 低（成熟方案） | 简单可靠，虚拟线程解决并发 |

#### 2.2 推荐方案B：回退Spring MVC + 虚拟线程

**理由**:
1. Java 21虚拟线程让Spring MVC也能轻松处理数千并发
2. MyBatis-Plus是阻塞生态，强行Reactor化成本极高
3. 当前30+个`.block()`已证明Reactor没带来实际收益
4. 营销画布的瓶颈在DB/外部API调用，不在Web层吞吐

#### 2.3 实施步骤

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 修改 `pom.xml`：`spring-boot-starter-webflux` → `spring-boot-starter-web` | 0.5h |
| 2 | 修改 `application.yml`：`web-application-type: reactive` → `servlet` | 0.5h |
| 3 | 全局替换 `Mono<T>` → `T`，移除 `.block()` 调用 | 8h |
| 4 | 全局替换 `Flux<T>` → `List<T>` 或 `Stream<T>` | 4h |
| 5 | 修改 `WebClient` 调用为同步或使用 `RestClient` | 4h |
| 6 | 启用虚拟线程：`spring.threads.virtual.enabled=true` | 0.5h |
| 7 | 调整 `DagEngine` 核心调度逻辑（移除Reactor链式调用） | 8h |
| 8 | 全量测试 + 性能基准对比 | 8h |

**总工时**: ~33h（约4人日）

#### 2.4 虚拟线程配置

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true  # Java 21 虚拟线程
  mvc:
    async:
      request-timeout: 600000  # 10分钟超时
```

#### 2.5 验收标准
- [ ] 应用启动为Servlet模式（日志显示Tomcat而非Netty）
- [ ] 无 `.block()` 调用残留
- [ ] 所有API响应正常
- [ ] 压测QPS不低于原WebFlux版本
- [ ] CPU利用率在高并发下更平滑（虚拟线程优势）

---

## 问题三：单库单连接池全域耦合

### 现状
- 65张表共享一个HikariCP池（max=33）
- 所有业务域共用同一个数据库连接池
- Flyway迁移链全局共享

### 影响
- CDP人群计算批量SQL会吃掉连接池
- Canvas Core画布保存可能超时
- 无法按域独立扩缩容
- 一个域的慢查询影响全局

### 实施方案：多数据源隔离

#### 3.1 目标架构

```
┌─────────────────────────────────────────────────────────┐
│                    canvas-engine-app                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │ canvas-core │  │  cdp-api    │  │ audience-api│ ...  │
│  │  DataSource │  │  DataSource │  │  DataSource │      │
│  │  (HikariCP) │  │  (HikariCP) │  │  (HikariCP) │      │
│  │  max=10     │  │  max=20     │  │  max=15     │      │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘      │
│         │                │                │              │
│  ┌──────┴────────────────┴────────────────┴──────┐      │
│  │              canvas_db (共享)                  │      │
│  │  或未来拆分为 canvas_core_db / cdp_db / ...   │      │
│  └───────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────┘
```

#### 3.2 实施步骤

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 配置多数据源：`canvasCoreDataSource` / `cdpDataSource` / `audienceDataSource` | 4h |
| 2 | 为每个模块配置独立的 `@Configuration` 类 | 4h |
| 3 | 配置独立的 `SqlSessionFactory` 和 `@MapperScan` | 4h |
| 4 | 配置独立的 `PlatformTransactionManager` | 2h |
| 5 | Flyway多数据源迁移配置 | 4h |
| 6 | 调整跨域事务处理（Saga或最终一致性） | 8h |
| 7 | 全量测试 | 4h |

**总工时**: ~30h（约4人日）

#### 3.3 数据源配置示例

```java
@Configuration
@MapperScan(basePackages = "org.chovy.canvas.canvas.dal.mapper",
           sqlSessionFactoryRef = "canvasCoreSqlSessionFactory")
public class CanvasCoreDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.canvas-core")
    public DataSource canvasCoreDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    public SqlSessionFactory canvasCoreSqlSessionFactory(
            @Qualifier("canvasCoreDataSource") DataSource dataSource) throws Exception {
        MybatisConfiguration configuration = new MybatisConfiguration();
        // ... 配置
        return new MybatisSqlSessionFactoryBuilder()
                .build(dataSource);
    }
}
```

#### 3.4 验收标准
- [ ] 每个模块有独立的数据源配置
- [ ] 连接池监控显示各域独立计数
- [ ] 一个域的连接池耗尽不影响其他域
- [ ] Flyway迁移在各数据源正确执行

---

## 问题四：49个Handler平铺单包

### 现状
- `engine/handlers/` 包下49个类平铺
- 无功能域分组
- 随节点类型增长，包越来越不可维护

### 影响
- 查找特定Handler困难
- 新增Handler时难以判断归属
- 代码审查时难以聚焦

### 实施方案：Handler功能域分组

#### 4.1 目标结构

```
engine/handlers/
├── trigger/           (触发器类：8个)
│   ├── MqTriggerHandler.java
│   ├── EventTriggerHandler.java
│   ├── ScheduledTriggerHandler.java
│   ├── ApiTriggerHandler.java
│   ├── AudienceTriggerHandler.java
│   ├── CanvasTriggerHandler.java
│   └── DirectCallHandler.java
├── condition/         (条件判断类：6个)
│   ├── IfConditionHandler.java
│   ├── SelectorHandler.java
│   ├── PriorityHandler.java
│   ├── LogicRelationHandler.java
│   ├── ConditionEvaluator.java
│   └── GoalCheckHandler.java
├── action/            (动作执行类：15个)
│   ├── SendSmsHandler.java
│   ├── SendPushHandler.java
│   ├── SendEmailHandler.java
│   ├── SendInAppHandler.java
│   ├── SendWechatHandler.java
│   ├── SendMqHandler.java
│   ├── CouponHandler.java
│   ├── PointsOperationHandler.java
│   ├── ApiCallHandler.java
│   ├── InAppNotifyHandler.java
│   ├── CommitActionHandler.java
│   ├── CreateTaskHandler.java
│   ├── TrackEventHandler.java
│   ├── UpdateProfileHandler.java
│   └── TagOperationHandler.java
├── control/           (流程控制类：10个)
│   ├── DelayHandler.java
│   ├── WaitHandler.java
│   ├── LoopHandler.java
│   ├── GotoHandler.java
│   ├── SubflowHandler.java
│   ├── SubFlowRefHandler.java
│   ├── EndHandler.java
│   ├── StartHandler.java
│   ├── HubHandler.java
│   └── MergeHandler.java
├── data/              (数据操作类：6个)
│   ├── TaggerHandler.java
│   ├── TaggerRealtimeHandler.java
│   ├── TaggerOfflineHandler.java
│   ├── CdpTagWriteHandler.java
│   ├── ReachPlatformHandler.java
│   └── RecommendationHandler.java
├── experiment/        (实验分析类：4个)
│   ├── AbSplitHandler.java
│   ├── ExperimentHandler.java
│   ├── ScoringHandler.java
│   └── AiNextBestActionHandler.java
└── utility/           (工具辅助类：6个)
    ├── GroovyHandler.java
    ├── GroovyScriptCache.java
    ├── FrequencyCapHandler.java
    ├── QuietHoursHandler.java
    ├── SuppressionCheckHandler.java
    └── ChannelAvailabilityHandler.java
```

#### 4.2 实施步骤

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 创建子包结构 | 0.5h |
| 2 | 移动Handler到对应子包 | 2h |
| 3 | 更新所有import语句 | 2h |
| 4 | 更新 `@NodeHandlerType` 注解扫描路径 | 0.5h |
| 5 | 全量测试 | 2h |

**总工时**: ~7h（约1人日）

#### 4.3 验收标准
- [ ] 所有Handler按功能域分组
- [ ] `HandlerRegistry` 正确扫描所有Handler
- [ ] 所有测试通过
- [ ] IDE导航清晰

---

## 问题五：Service层几乎不存在

### 现状
- 整个项目只有1个Service接口+1个实现（`EventDefinitionService`）
- 业务逻辑直接散落在Controller和Domain Service中
- 缺少应用服务层编排

### 影响
- Controller承担过多职责
- 业务逻辑难以复用
- 事务边界不清晰
- 测试困难

### 实施方案：引入应用服务层

#### 5.1 分层架构

```
Controller (Web层)
    ↓ 调用
ApplicationService (应用服务层) ← 新增
    ↓ 编排
DomainService (领域服务层)
    ↓ 调用
Repository/Mapper (数据访问层)
```

#### 5.2 实施步骤

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 为每个业务域创建 `ApplicationService` 接口和实现 | 8h |
| 2 | 将Controller中的业务逻辑迁移到ApplicationService | 16h |
| 3 | 明确事务边界，在ApplicationService方法上添加 `@Transactional` | 4h |
| 4 | 重构Controller为薄层（参数校验+调用ApplicationService） | 8h |
| 5 | 全量测试 | 4h |

**总工时**: ~40h（约5人日）

#### 5.3 示例

**Before (Controller直接操作Mapper)**:
```java
@RestController
public class CanvasController {
    @Autowired
    private CanvasMapper canvasMapper;

    @PostMapping("/canvas")
    public Canvas create(@RequestBody CanvasCreateRequest request) {
        Canvas canvas = new Canvas();
        // ... 大量业务逻辑
        canvasMapper.insert(canvas);
        return canvas;
    }
}
```

**After (引入ApplicationService)**:
```java
@RestController
public class CanvasController {
    @Autowired
    private CanvasApplicationService canvasApplicationService;

    @PostMapping("/canvas")
    public CanvasDTO create(@RequestBody @Valid CanvasCreateRequest request) {
        return canvasApplicationService.createCanvas(request);
    }
}

@Service
public class CanvasApplicationServiceImpl implements CanvasApplicationService {
    @Autowired
    private CanvasDomainService canvasDomainService;
    @Autowired
    private CanvasVersionDomainService versionDomainService;

    @Transactional
    @Override
    public CanvasDTO createCanvas(CanvasCreateRequest request) {
        // 编排多个领域服务
        Canvas canvas = canvasDomainService.create(request);
        versionDomainService.createInitialVersion(canvas.getId());
        return CanvasDTO.from(canvas);
    }
}
```

#### 5.4 验收标准
- [ ] 每个业务域有独立的ApplicationService
- [ ] Controller方法不超过20行
- [ ] 事务边界清晰（@Transactional在ApplicationService方法上）
- [ ] 所有测试通过

---

## 问题六：DTO/DO直接暴露

### 现状
- Controller直接操作DO实体
- API契约和数据库schema紧耦合
- 没有独立的DTO/VO层做视图隔离

### 影响
- API变更影响DB schema
- 敏感字段可能泄露
- 无法做API版本管理
- 前端需要的数据结构可能与DB不一致

### 实施方案：引入DTO/VO层

#### 6.1 分层模型

```
Controller
    ↓ 接收/返回
RequestDTO / ResponseDTO (API契约层)
    ↓ 转换
DO (数据对象层)
    ↓ 映射
DB Table
```

#### 6.2 实施步骤

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 为每个Controller创建RequestDTO和ResponseDTO | 16h |
| 2 | 使用MapStruct或手动实现DTO-DO转换 | 8h |
| 3 | 修改Controller使用DTO而非DO | 8h |
| 4 | 添加敏感字段脱敏逻辑 | 4h |
| 5 | 全量测试 | 4h |

**总工时**: ~40h（约5人日）

#### 6.3 示例

**Before**:
```java
@GetMapping("/canvas/{id}")
public CanvasDO getCanvas(@PathVariable Long id) {
    return canvasMapper.selectById(id); // 直接返回DO
}
```

**After**:
```java
@GetMapping("/canvas/{id}")
public CanvasResponseDTO getCanvas(@PathVariable Long id) {
    CanvasDO canvas = canvasMapper.selectById(id);
    return CanvasResponseDTO.from(canvas); // 转换为DTO
}

@Data
public class CanvasResponseDTO {
    private Long id;
    private String name;
    private String status;
    // 不包含内部字段如 createdBy, updatedBy 等

    public static CanvasResponseDTO from(CanvasDO canvas) {
        CanvasResponseDTO dto = new CanvasResponseDTO();
        dto.setId(canvas.getId());
        dto.setName(canvas.getName());
        dto.setStatus(canvas.getStatus());
        return dto;
    }
}
```

#### 6.4 验收标准
- [ ] 所有Controller使用DTO而非DO
- [ ] API文档（Swagger）显示DTO结构
- [ ] 敏感字段不在API响应中出现
- [ ] 所有测试通过

---
