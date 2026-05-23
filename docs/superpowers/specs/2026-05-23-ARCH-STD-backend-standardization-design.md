---
id: ARCH-STD-BACKEND-STANDARDIZATION-2026-05-23
source: docs/optimization/optimization_list_v5.md
scope: backend/canvas-engine
type: architecture-standardization
tags:
  - ARCH-STD
  - optimization-v5
  - backend-standardization
  - modular-monolith
---

# [ARCH-STD] 后端架构与规范化整理设计

## 如何查找本设计

这份设计使用固定标识 `ARCH-STD`，用于和其他优化项 spec 区分。

常用查找方式：

```bash
rg "ARCH-STD" docs/superpowers/specs
rg "backend-standardization" docs/superpowers/specs
rg "optimization-v5" docs/superpowers/specs
```

也可以从索引文件进入：

```bash
sed -n '/ARCH-STD/p' docs/superpowers/specs/INDEX.md
```

## 背景

`docs/optimization/optimization_list_v5.md` 提出三类后端规范问题：

1. 现有 Service 类没有对应 interface。
2. DO 类和 Mapper 存放在同一包，不符合常见分层规范。
3. 类命名与包名需要按大厂 Java 后端规范整理，参考阿里巴巴 Java 开发规范风格。

当前项目已经是一个营销旅程画布的模块化单体，具备画布版本、发布、触发、DAG 执行、节点 Handler、Redis 缓存、RocketMQ、执行轨迹、部分统计、人群圈选与配置化 API/MQ/事件能力。它不适合在本次优化中直接拆成多个微服务。本次目标是先把单体内边界整理清楚，让代码结构符合规范，并为未来拆分控制面、运行面、洞察面留下清晰边界。

## 参考架构的取舍

用户提供的营销旅程架构资料有参考意义，核心价值是确认系统应按三平面理解：

- 控制面：画布编辑、配置、校验、发布、版本快照。
- 运行面：触发、DAG 执行、节点流转、延迟、MQ、外部触达。
- 洞察面：执行轨迹、统计、错误台账、画布叠加数据。

本项目当前阶段不直接拆 `Journey Studio Service`、`Execution Engine`、`Channel Service`、`Analytics Service` 等独立微服务。更合适的落地方式是在 `canvas-engine` 内形成模块化单体边界：

- `web/service/dal` 承载控制面 API 与配置数据。
- `engine` 承载运行面核心执行逻辑。
- `integration` 和 `infrastructure` 承载运行面的外部适配与技术设施。
- `dal.dataobject` 中的执行记录和统计对象支撑洞察面，后续可再拆出 OLAP 或 Analytics 服务。

## 目标

- 将 Spring Service 统一改成 interface + implementation 结构。
- 将 MyBatis-Plus 持久化对象统一命名为 `*DO`。
- 将 DO 与 Mapper 拆到不同包。
- 为每个 Mapper 建立对应 XML 文件。
- 将 controller 层依赖收敛到 service interface，不再直接依赖 Mapper。
- 保持业务行为、接口路径、请求响应语义、数据库结构和 SQL 逻辑不变。
- 将包结构整理成接近阿里风格的大厂 Java 后端分层。

## 非目标

- 不拆微服务。
- 不重写执行引擎。
- 不调整数据库表、字段或 Flyway migration。
- 不修改画布发布、触发、灰度、调度、限流、MQ、缓存、DAG 执行等业务逻辑。
- 不引入 Kafka、ClickHouse、Flink、Saga、Outbox 等新基础设施。
- 不为 `canvas-cache-sdk` 强行套用业务应用分层。

## 目标包结构

`backend/canvas-engine` 的目标结构：

```text
org.chovy.canvas
  web                 # Controller，对外 API
  service             # 应用/业务服务接口
  service.impl        # 应用/业务服务实现
  dal
    dataobject        # MyBatis-Plus 持久化对象，统一 *DO
    mapper            # MyBatis Mapper
  dto                 # 请求/响应 DTO，允许按业务域继续分子包
  query               # 分页与查询条件对象
  vo                  # 面向前端展示对象，当前没有强制新增
  engine              # DAG、执行上下文、调度、节点执行框架
  integration         # 外部系统适配：MQ、触达、Tagger、API 调用等
  infrastructure      # Redis、Cache、RocketMQ、底层技术设施
  auth                # 认证授权工具与安全上下文
  common              # R、PageResult、ErrorCode、通用工具
  config              # Spring/MyBatis/Security 配置
```

当前 `controller` 和 `auth.controller` 迁移到 `web`。当前 `infra` 更名为 `infrastructure`。当前 `domain/*` 不再作为混合包使用，其中 DO 进入 `dal.dataobject`，Mapper 进入 `dal.mapper`，Service 进入 `service` 与 `service.impl`。

`engine` 保留为独立运行面边界，不把节点 Handler、DAG、执行上下文强行塞进 CRUD 分层。`engine` 内如果存在以 `Service` 命名且被其他模块注入的组件，也要采用 interface + implementation；内部纯模型、Handler 接口、注解和枚举不受本规则影响。

## 类命名规范

### DO

所有带 `@TableName` 的 MyBatis-Plus 持久化对象改为 `*DO`，并放入 `org.chovy.canvas.dal.dataobject`。

示例：

| 当前类 | 目标类 |
|--------|--------|
| `Canvas` | `CanvasDO` |
| `CanvasVersion` | `CanvasVersionDO` |
| `CanvasExecutionTrace` | `CanvasExecutionTraceDO` |
| `ApiDefinition` | `ApiDefinitionDO` |
| `SysUser` | `SysUserDO` |

`StubOption` 不是数据库持久化对象，不加 `DO` 后缀。枚举类不加 `DO` 后缀。

### Mapper

Mapper 放入 `org.chovy.canvas.dal.mapper`，泛型绑定对应 DO。

示例：

```java
public interface CanvasMapper extends BaseMapper<CanvasDO> {
}
```

自定义 Mapper 方法签名只更新包名和 DO 类型，不改变 SQL 含义。

### Service

Service interface 保留原名，implementation 使用 `*ServiceImpl` 后缀。

示例：

```text
service/CanvasService.java
service/impl/CanvasServiceImpl.java
service/MetaService.java
service/impl/MetaServiceImpl.java
service/SysUserService.java
service/impl/SysUserServiceImpl.java
```

controller 和跨模块调用方注入 interface。实现类只在 Spring 容器中作为 `@Service` Bean 存在，不被 controller 直接引用。

### DTO、Query、VO

现有请求/响应对象保持字段语义不变。迁移规则：

- `*Req`、`*Resp`、`*DTO` 放在 `dto`。
- `*Query` 放在 `query`。
- 面向前端展示且不等同接口 DTO 的对象后续放在 `vo`。

本次不强制创建 VO，也不改变接口 JSON 字段。

## Mapper XML 规范

当前只有两个 XML：

```text
resources/mapper/CanvasMapper.xml
resources/mapper/CanvasExecutionTraceMapper.xml
```

本次为每个 Mapper 都建立 XML 文件。推荐目录按业务域分组：

```text
resources/mapper/canvas/CanvasMapper.xml
resources/mapper/canvas/CanvasVersionMapper.xml
resources/mapper/execution/CanvasExecutionTraceMapper.xml
resources/mapper/meta/ApiDefinitionMapper.xml
resources/mapper/auth/SysUserMapper.xml
```

已有自定义 SQL 原样迁移，只更新 namespace：

```xml
<mapper namespace="org.chovy.canvas.dal.mapper.CanvasMapper">
</mapper>
```

没有自定义 SQL 的 Mapper 也建立空 namespace XML，作为规范化文件和后续扩展位置，不增加 SQL。

`application.yml` 的 MyBatis-Plus XML 扫描路径改为支持多级目录：

```yaml
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
```

## Controller 分层规则

目标状态是 controller 只依赖 service interface，不直接依赖 Mapper。

当前直接依赖 Mapper 的 controller 包括：

- `ApiDefinitionController`
- `MqDefinitionController`
- `CanvasExecutionManagementController`
- `DlqController`
- `AbExperimentController`
- `MetaController`
- `OpsController`
- `CanvasStatsController`
- `EventDefinitionController`
- `AudienceController`
- `TagDefinitionController`

本次可以通过薄 service 做等价迁移：将原 controller 中直接调用 Mapper 的代码搬到对应 service implementation，controller 只转调 service。迁移过程中不改变查询条件、分页参数、异常语义和返回结构。

## 运行面边界

`engine` 是运行面核心，保留其领域边界：

- `engine.handler`：节点执行接口、注册器、执行结果。
- `engine.handlers`：具体节点 Handler。
- `engine.dag`：DAG 解析与图结构。
- `engine.context`：执行上下文与节点状态。
- `engine.scheduler`：DAG 执行调度与 trace buffer。
- `engine.trigger`：触发、准入、执行入口、watchdog。
- `engine.audience`：人群规则、圈选计算、规则引擎。

`engine` 内类移动时只做包名和 interface 抽取，不改变执行链路。节点 Handler 不是本次 service interface 目标，已有 `NodeHandler` 接口保持不变。

## integration 与 infrastructure 边界

当前 `infra` 包中混有 Redis、MQ、缓存等技术设施。本次整理后：

- `infrastructure.cache`：Caffeine/Redis 缓存实现、缓存同步。
- `infrastructure.redis`：Redis key、路由、Pub/Sub、上下文持久化。
- `infrastructure.mq`：RocketMQ consumer、trigger message。
- `integration`：面向业务语义的外部系统适配，可承载触达、Tagger、外部 API、MQ 消息定义适配。

已有实现如果只是底层技术组件，优先放 `infrastructure`；如果表达外部业务系统语义，后续可移入 `integration`。本次不改调用协议和消息格式。

## canvas-cache-sdk 边界

`backend/canvas-cache-sdk` 是公共缓存 SDK，不包含 DO、Mapper、controller，不适合套用 `web/service/dal` 应用分层。

本次只检查明显命名问题，不进行大规模移动。已有公共接口如 `TieredCache`、`ReactiveTieredCache` 保持不变，避免破坏 SDK 消费方。

## 迁移顺序

1. 建立新包目录和 service interface。
2. 移动并重命名 DO，更新 Mapper 泛型。
3. 移动 Mapper，补齐 XML，更新 namespace 和 `mapper-locations`。
4. 将业务 Service 改为 interface + `*ServiceImpl`。
5. 将 controller 直接 Mapper 调用迁移到薄 service。
6. 更新 `engine`、`infrastructure`、测试代码中的 import 和类型名。
7. 执行编译和测试。
8. 使用 `rg` 检查旧包名、旧 DO 类名和旧 XML namespace 残留。

这个顺序先移动数据访问层，再移动服务层，最后处理调用方，便于每一步发现编译错误。

## 验证

必须执行：

```bash
mvn -pl canvas-engine -am test
```

必须检查：

```bash
rg "org.chovy.canvas.domain" backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java
rg "namespace=\"org.chovy.canvas.domain" backend/canvas-engine/src/main/resources/mapper
rg "BaseMapper<[^>]*(Canvas|ApiDefinition|SysUser|EventDefinition|TagDefinition|AudienceDefinition|CanvasExecution)" backend/canvas-engine/src/main/java
find backend/canvas-engine/src/main/resources/mapper -name '*Mapper.xml' | sort
```

验证标准：

- 后端测试通过。
- 所有 Mapper 都有 XML。
- XML namespace 指向 `org.chovy.canvas.dal.mapper.*`。
- controller 不直接注入 Mapper。
- Spring 注入点依赖 service interface。
- 对外 API 路径、请求参数和响应字段保持不变。

## 风险与处理

| 风险 | 处理 |
|------|------|
| 类重命名导致 import 大面积变化 | 分阶段编译修复，只做机械替换，不夹带业务改动 |
| MyBatis XML namespace 迁移遗漏 | 用 `rg "namespace=\"org.chovy.canvas.domain"` 检查 |
| 空 XML 影响扫描 | 使用标准 `<mapper namespace="...">` 空文件，扫描路径改为 `classpath*:mapper/**/*.xml` |
| controller 薄 service 迁移改变响应 | service 内保留原查询和拼装逻辑，controller 只转调 |
| 当前工作区已有未提交改动 | 实施时只修改规范化相关文件，不回滚或覆盖无关改动 |

## 后续演进

本次规范化完成后，未来可按三平面逐步拆分：

- 控制面：`web/service/dal` 中的画布配置、版本、发布能力可演进为 Journey Studio。
- 运行面：`engine/integration/infrastructure` 可演进为 Execution Engine、Event Collector、Channel Adapter。
- 洞察面：执行 trace 和 stats 可演进为 Analytics/Overlay 服务。

这些演进不属于本次实施范围。本次只建立清晰边界，让未来拆分有稳定落点。
