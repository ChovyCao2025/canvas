# 架构整改方案 — logic-testing
> 详见 [README.md](./README.md) 获取完整索引


### 问题二十五：业务逻辑正确性缺陷（34项）

#### 25.1 状态机违规 — 画布生命周期无守卫（7项）

**文件**: `domain/canvas/CanvasTransactionService.java`, `domain/canvas/CanvasService.java`, `domain/canvas/CanvasOpsService.java`

| 位置 | 问题 |
|------|------|
| `CanvasTransactionService.publishDb():47-68` | 无当前状态检查，PUBLISHED/OFFLINE/KILLED/ARCHIVED的画布都能重新发布 |
| `CanvasTransactionService.offlineDb():92-102` | 无状态检查，DRAFT或ARCHIVED画布可被下线 |
| `CanvasTransactionService.killDb():118-128` | 无状态检查，DRAFT或OFFLINE画布可被终止 |
| `CanvasTransactionService.archiveDb():140-147` | 无状态检查，DRAFT或PUBLISHED画布可被归档 |
| `CanvasService.updateDraft():132-135` | 不检查DRAFT状态，PUBLISHED/KILLED画布的元数据和graphJson可被静默修改 |
| `CanvasOpsService.rollback():189-198` | 不检查PUBLISHED状态，回滚DRAFT/OFFLINE画布会损坏数据 |
| `CanvasOpsService.startCanary():127-146` | 不检查已有canary部署，第二次startCanary静默覆盖 |

**核心问题**: KILLED画布可被静默重新发布——KILLED应是紧急终端态。OFFLINE画布无明确重发路径。已发布画布的配额/有效期/触发器配置可在运行中被修改。

#### 25.2 幂等性缺口（7项）

| 位置 | 问题 |
|------|------|
| `TagOperationHandler:62-66` | REMOVE是硬删除，DagEngine重试时第二次执行changed=0，不一致；且无审计轨迹 |
| `MarketingPolicyService:159-162` | INCR+EXPIRE非原子，进程崩溃时key无TTL永久占用频率位 |
| `FrequencyCapHandler:62-63` | consumeFrequency()始终递增，DagEngine重试导致频率计数双重消耗 |
| `MarketingPolicyService:164-167` | 超限回滚DECR与INCR非原子，并发请求可能读到膨胀计数被误拒 |
| `CdpTagService:168-169` | idempotencyKey为null时DuplicateKeyException被重新抛出而非视为重复 |
| `ManualApprovalHandler:104-121` | 通知失败时删除审批记录，重试时可能DuplicateKeyException |
| `ManualApprovalHandler:67` | approvalId无DB唯一约束，repeat机制下可能重复插入 |

#### 25.3 数据完整性违规（6项）

| 位置 | 问题 |
|------|------|
| `CanvasVersionCleanupJob:73-79` | 清理旧版本时置null graphJson，不检查是否有运行中执行引用该版本——WAIT/PAUSED节点恢复时将失败 |
| `TagOperationHandler:64` vs `CdpTagService:105-108` | 同一概念（用户标签）两种删除模式：引擎硬删除无历史 vs CDP域软删除写历史 |
| `CanvasOpsService:108-116` | KILLED画布的PAUSED执行不清理关联审批记录/去重key/InFlight注册 |
| `CanvasTransactionService:160-167` | nextVersionNumber()是读后算，并发发布时两个事务可能读到相同max版本 |
| `CanvasController` | 无删除端点——测试/错误画布只能ARCHIVED，永久积累 |
| 全系统 | 用户删除后CDP/客户/通知记录全部残留为孤儿 |

#### 25.4 业务规则强制缺失（6项）

| 位置 | 问题 |
|------|------|
| `CanvasService:132-165` | updateDraft()允许修改已发布画布的maxTotalExecutions/validEnd等，与已准入执行冲突 |
| `CanvasService:217-224` | publish()仅验证入口节点和子画布依赖，不验证Handler类型有效/边引用存在/配置完整/孤立子图 |
| `TriggerPreCheckService` vs `MarketingPolicyService` | 频率控制由两个服务分别实施，不同原子性保证/不同Redis key模式/不同回滚机制 |
| `CanvasExecutionService:1119-1147` | canary分流用 `userId.hashCode() % 100`，String.hashCode()非均匀分布，实际流量偏移 |
| 无代码守卫 | 删除运行中画布仅靠"无删除API"约定，无代码层防护 |
| `CanvasService:218` | 零节点画布可保存，发布时才报错，应在保存时警告 |

#### 25.5 审计轨迹缺口（6项）

| 位置 | 问题 |
|------|------|
| 全系统 | 无AuditLog表/OperationLog实体/AuditService，`NotificationEventService.canvasChanged()`仅发通知不留可查审计 |
| `CanvasTransactionService:92-147` | offline/kill/archive操作不记录操作人，`updatedBy`是通用自动填充非显式记录 |
| `TagOperationHandler:64` | 引擎级标签删除无审计记录，绕过CDP域的审计保障 |
| `FrequencyCapHandler:64-68` | 频率控制决策不持久化，无法查询用户被限制次数及原因 |
| `CanvasService:139-146` | 已发布画布配置变更无审计，无法追溯谁在何时改了什么 |
| `CanvasOpsService:91` | Kill操作不记录操作人/模式/影响执行数，仅Redis Pub/Sub瞬时消息 |

#### 25.6 边界场景处理（8项）

| 位置 | 问题 |
|------|------|
| `CanvasExecutionService:224,424` | 全局超时不取消底层Reactor订阅，节点Handler继续运行完成可能产生副作用（发消息/发券） |
| `CanvasExecutionService:893-894` | globalTimeoutSec运行时变更导致dedup TTL不匹配，可能双触发或阻断合法重触发 |
| 人群解析层 | 用户属于多个受众时，同一画布通过不同受众路径可被触发多次 |
| 无机制 | 全DAG节点失败仅标记FAILED，无系统性检测和告警——配置错误的画布对每用户静默失败 |
| `CanvasService:202-207` | 发布锁30s TTL，但Groovy预编译/子画布验证可能超时，锁过期后并发发布致双版本 |
| `TriggerPreCheckService:246-249` | quota回滚DECR失败时计数永久膨胀，不重试不记录失败key |
| `TriggerPreCheckService:106-123` | soft check读DB的last_trigger_at由虚拟线程异步写，并发触发时stale read浪费处理 |
| `InFlightExecutionRegistry:207-210` | Redis不可用时activeCount()返回本地计数，多实例部署严重低估活跃数 |

#### 实施方案

**Step 1: 画布状态机（25.1）**

```java
public class Canvas {
    private CanvasStatus status;

    public void transitionTo(CanvasStatus target) {
        Set<CanvasStatus> allowed = TRANSITIONS.get(status);
        if (allowed == null || !allowed.contains(target))
            throw new InvalidStateTransitionException(status, target);
        this.status = target;
    }

    private static final Map<CanvasStatus, Set<CanvasStatus>> TRANSITIONS = Map.of(
        CanvasStatus.DRAFT,     Set.of(CanvasStatus.PUBLISHED, CanvasStatus.ARCHIVED),
        CanvasStatus.PUBLISHED, Set.of(CanvasStatus.OFFLINE, CanvasStatus.KILLED),
        CanvasStatus.OFFLINE,   Set.of(CanvasStatus.PUBLISHED, CanvasStatus.ARCHIVED),
        CanvasStatus.KILLED,    Set.of(CanvasStatus.ARCHIVED)
    );
}
```

**Step 2: 原子频率控制（25.2）**

```java
// 统一使用Lua脚本，替代INCR+EXPIRE
private static final String CONSUME_FREQUENCY_SCRIPT =
    "local current = redis.call('INCR', KEYS[1]) " +
    "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
    "return current";
```

**Step 3: 审计日志（25.5）**

```java
@Entity
@Table(name = "audit_log")
public class AuditLogDO {
    private Long id;
    private String operation;    // PUBLISH, KILL, CONFIG_CHANGE, TAG_REMOVE...
    private String targetType;   // CANVAS, TAG, USER...
    private Long targetId;
    private String operatorId;
    private String beforeState;  // JSON
    private String afterState;   // JSON
    private LocalDateTime operatedAt;
}
```

**Step 4: 执行超时取消（25.6）**

```java
// 使用 Disposable 替代纯 timeout
Disposable execution = canvasMono
    .timeout(Duration.ofSeconds(globalTimeoutSec))
    .subscribe();
// 超时时主动取消
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | Canvas状态机 + 全部transition点修复 | 8h |
| 2 | 频率控制原子化（Lua脚本统一） | 6h |
| 3 | TagOperationHandler改用CDP域软删除 | 4h |
| 4 | 审计日志基础设施 | 8h |
| 5 | CanvasVersionCleanupJob安全检查 | 2h |
| 6 | 发布DAG验证增强 | 4h |
| 7 | 执行超时主动取消 | 4h |
| 8 | canary分流算法改进（MurmurHash） | 2h |
| 9 | 全量测试 | 6h |

**总工时**: ~44h

---

### 问题二十六：测试架构与覆盖缺口（28项）

#### 26.1 后端测试覆盖概况

| 层 | 源文件 | 测试文件 | 覆盖率 |
|----|--------|---------|--------|
| Handlers | ~61 | 18 | **~30%** |
| Controllers | ~29 | 13 | **~45%** |
| Services | ~43 | 22 | **~51%** |
| Engine Core | ~25 | 14 | **~56%** |
| Infrastructure | ~10 | 5 | **~50%** |

**43/61 Handler无测试。16/29 Controller无测试。**

#### 26.2 零集成测试

- 无 `@SpringBootTest`
- 无 TestContainers
- 无 H2/`@DataJpaTest`
- 无 WireMock
- 无 `@WebMvcTest`
- 唯一的准集成测试 `AudienceComputeTaskRunnerSpringTest` 手动创建 `AnnotationConfigApplicationContext`，不做HTTP调用或DB断言

#### 26.3 DagEngine 关键路径未测试

| 特性 | 测试？ |
|------|--------|
| 6阶段节点执行管道 | 否 |
| NodeGate CAS + repeatPending并发分支收敛 | 否 |
| LOGIC_RELATION特殊节点+超时调度 | 否 |
| HUB/AGGREGATE扇入 | 否 |
| THRESHOLD节点+超时 | 否 |
| DLQ写入 | 否 |
| TraceBuffer集成 | 否 |
| START到终端节点全图执行 | 否 |
| 并行分支并发执行 | 否 |
| 指数退避重试 | 否 |
| 熔断器集成 | 否 |

#### 26.4 CircuitBreakerRegistry — 零测试

自定义熔断器实现（OPEN/CLOSED状态转换、失败计数、半开恢复）——完全未测试。熔断器错误打开时所有节点执行将静默失败。

#### 26.5 前端测试缺口

| 类别 | 测试文件 | 源文件 | 覆盖率 |
|------|---------|--------|--------|
| 组件(.tsx) | 0 | 44 | **0%** |
| Hooks | 2 | ~10 | **~20%** |
| 工具函数 | ~8 | ~15 | **~53%** |
| 集成/E2E | 0 | — | **0%** |

缺少依赖：`@testing-library/react`、`jsdom`/`happy-dom`、`msw`、`cypress`/`playwright` 均未安装。

#### 26.6 测试质量问题

| 文件 | 质量 | 问题 |
|------|------|------|
| `CanvasExecutionServiceCdpTest` | Poor | 反射断言字段存在——实现测试而非行为测试 |
| `CanvasStatsControllerTest` | Poor | 单断言，验证mock无交互——trivial覆盖 |
| `CanvasDisruptorServiceTest` | Mixed | ReflectionTestUtils注入mock——脆弱 |
| `DagEngineCommitActionTest` | Mixed | 仅2个测试用例——不足 |
| 60%的测试 | — | 方法名而非行为名命名 |
| 全部 | — | 按类组织而非按特性，无 `@Tag` 分组 |

#### 实施方案

**Step 1: 集成测试基础设施（26.2）**

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
```

**Step 2: DagEngine关键路径测试（26.3）**

优先级：
1. 全图执行（START→TERMINAL）
2. NodeGate CAS并发
3. 特殊节点（Hub/LogicRelation/Aggregate）
4. 超时+DLQ路径

**Step 3: 前端测试基础设施（26.5）**

```bash
npm install -D @testing-library/react @testing-library/jest-dom jsdom msw
```

```typescript
// vite.config.ts
test: {
  environment: 'jsdom',
  setupFiles: ['./src/test/setup.ts'],
}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | TestContainers集成测试基础设施 | 8h |
| 2 | DagEngine关键路径测试（10项） | 16h |
| 3 | CircuitBreakerRegistry测试 | 4h |
| 4 | Handler覆盖补全（20个未测Handler） | 20h |
| 5 | 前端测试基础设施（jsdom+testing-library） | 4h |
| 6 | 前端核心组件测试 | 12h |
| 7 | E2E测试框架（Playwright） | 8h |
| 8 | 关键E2E流程测试 | 12h |

**总工时**: ~84h

---

### 问题二十七：技术债与代码质量（30项）

#### 27.1 代码重复（7类）

| 重复模式 | 副本数 | 位置 |
|----------|--------|------|
| `currentUser()`/`currentUsername()`/`currentUserId()` | 7个变体 | 7个Controller |
| `defaultIfBlank()` 工具方法 | 6+ | 6个Controller |
| `clamp()` 方法 | 2 | 2个Controller |
| `publishBestEffort()` / `publishRequestBestEffort()` | 2 | DLQ/MQ Rejected Controller |
| DLQ vs MQ Trigger Rejected 重放逻辑 | 2 | 结构相同的重放模式 |
| `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` | 174次 | Controllers/Services |
| Controller直接调Mapper | 43处/14个Controller | 绕过Service层 |

#### 27.2 命名不一致（5类）

| 问题 | 详情 |
|------|------|
| Enum后缀不一致 | 仅`CanvasStatusEnum`有"Enum"后缀，其余无 |
| Service接口模式不一致 | 仅`EventDefinitionService`有interface+impl，其余全concrete |
| DO/DTO后缀不规范 | `StubOption`/`TagImportRow`/`TagImportResult`缺DTO后缀 |
| "My"前缀 | `MyMetaObjectHandler`应改为`CanvasMetaObjectHandler` |
| 硬编码角色字符串 | `"ADMIN"`未用`RoleNames.ADMIN`常量（2处） |

#### 27.3 硬编码魔法值（50+处）

关键项：
- `canvas-event-report-secret-2026!!` — 事件上报密钥硬编码
- `CacheConfig.java` — 缓存参数全部硬编码，未外部化到application.yml
- `CanvasController` — 9种事件类型/4种严重度全部硬编码字符串
- `AuthController` — `MAX_FAIL_COUNT=5`/`LOCK_TTL=15min`/Redis key前缀硬编码
- `CanvasExecutionService` — `globalTimeoutSec + 600` magic number
- `Thread.sleep(2000)` / `Thread.sleep(50)` — 在Reactive上下文中阻塞

#### 27.4 依赖管理问题（4项）

| 问题 | 详情 |
|------|------|
| POM属性重复 | parent和child pom.xml定义相同properties |
| 无dependencyManagement | parent pom仅有pluginManagement |
| 过时依赖 | Disruptor 3.4.4(2020)/Spring Boot 3.2.5(非最新3.3.x) |
| 双表达式引擎 | Aviator+QLExpress共存，可能只需一个 |

#### 27.5 文档债务

- 40+公共类无Javadoc
- 大量自动生成无意义Javadoc（"方法会结合入参..."）
- application.yml配置项缺少用途说明

#### 27.6 死代码

- `InAppNotifyHandler:43` — `// TODO: 接入 MQTT 推送客户端` Mock实现
- `CanvasExecutionService:903` — `// FIXME: 过期时间会发生变化`
- `OverflowRetryMessage` — `@Deprecated`常量仍存在
- `CanvasService:369` — `@Deprecated`方法 `getVersions()` 未移除
- ~30文件通配符import

#### 实施方案

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 提取共享SecurityUtil/defaultIfBlank/clamp | 4h |
| 2 | CacheConfig外部化到application.yml | 2h |
| 3 | 消除Thread.sleep → Mono.delay | 2h |
| 4 | CanvasStatusEnum→CanvasStatus + RoleNames常量 | 2h |
| 5 | POM整理（dependencyManagement+去重） | 2h |
| 6 | 移除@Deprecated代码+通配符import | 2h |
| 7 | 魔法值常量化（事件类型/严重度/角色） | 4h |
| 8 | Javadoc补全（公共API优先） | 4h |

**总工时**: ~22h
