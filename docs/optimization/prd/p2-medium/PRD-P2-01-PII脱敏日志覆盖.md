# PRD-P2-01-PII脱敏日志覆盖

> 本文档为营销画布平台个人信息保护需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-01 |
| **需求名称** | PII脱敏日志覆盖 |
| **优先级** | P2 |
| **所属类别** | 安全合规 MEDIUM |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | Braze/Pendo: 自动对PII字段脱敏 |

---

## 1. 问题描述

### 1.1 现状

当前系统仅有 `DataMaskingUtil` 一个脱敏工具类，但仅被 2 个地方调用，覆盖日志范围有限：
- 调用点1: 用户登录日志（Email/手机号）
- 调用点2: 隐私条款确认记录（ConfirmedAt时间戳）

其他日志（执行日志、审计日志、触发日志等）均未集成脱敏逻辑，原始 PII 数据可能被不完全脱敏地记录到：
- 应用日志（`application.log`）
- 数据库日志表（如 ExecutionLog, TriggerLog, NodeExecutionResult）
- 文件系统日志文件

### 1.2 痛点

1. **隐私合规风险**：GDPR Art.32(1)(a), PIPL Art.51 要求对 PII 数据加密存储，日志本身是静态加密的需求扩展
2. **数据泄露敏感**：运维人员可从日志系统读取完整用户邮箱、手机号、设备号等敏感信息
3. **安全审计不足**：关键系统日志（如 `/canvas/execute`、`/canvas/trigger/`）未脱敏，易被内部人员滥用
4. **第三方合规**：部分业务场景（营销许可用户列表）中泄露 PII 可能违反数据保护法

### 1.3 竞品对标

| 竞品 | 能力 |
|------|------|
| Braze | 自动对 `email`, `phone`, `msisdn`, `device_id` 字段脱敏，支持正则配置 |
| Pendo | 日志脱敏策略：邮箱 `***@example.com`；手机号 `138****1234` |
| Iterable | 全量日志自动脱敏，支持自定义 PII 字段列表 |

---

## 2. 目标与价值

### 2.1 用户故事

- **产品经理**：作为产品负责人，我希望所有涉及用户个人信息（邮箱、手机号、设备号、身份ID）的日志都经过脱敏处理，以便在满足日志监控需求的同时符合 GDPR/PIPL 法规要求。

- **运维人员**：作为系统运维，我希望能在日志中查看业务信息但无法识别具体用户身份，以便在发现问题时快速定位但无需访问原始 PII 数据。

- **安全工程师**：作为安全负责人，我希望系统能自动识别并脱敏 PII 字段，而不是人工硬编码，以减少人工误操作和审计遗漏。

### 2.2 成功指标

- 覆盖率：100% 的生产日志句柄（包括 Logback/Log4j2 配置的日志输出）
- 覆盖字段：至少 10 个 PII 类型（邮箱、手机号、设备ID、姓名、身份证、OpenID、UnionID、真实姓名、家庭住址、职业等）
- 脱敏效果：邮箱 `user@example.com` → `u***@example.com`；手机号 `13812345678` → `138****5678`
- 审计追踪：所有脱敏操作可追溯（脱敏前/后值记录在元数据中）

### 2.3 不做会怎样

- 日志泄露可能导致用户隐私被恶意读取，触发监管处罚（GDPR 2000欧/人，PIPL 100万-5000万罚金）
- 运维人员无意识泄露 PII，可能违反公司数据安全策略
- 无法通过隐私保护审查（如 App 审核合规）

---

## 3. 功能需求

### 3.1 核心功能

1. **PII 字段自动识别**：通过配置文件/数据库模式定义 PII 字段映射（字段Key → 脱敏策略）
2. **BSP 日志脱敏过滤器**：基于 Logback/Log4j2 全局过滤器，对所有日志输出进行脱敏
3. **数据库日志脱敏**：字段级数据脱敏（MyBatis-Plus 拦截器 + 脱敏注解）
4. **脱敏配置管理 UI**：管理员可动态配置 PII 字段及脱敏规则
5. **脱敏审计日志**：记录哪些字段被脱敏、脱敏前/后值对比（仅管理员可见）

### 3.2 详细描述

#### 3.2.1 PII 字段定义

通过配置支持以下字段类型及脱敏策略：

| 字段类型 | 脱敏策略 | 示例 |
|---------|---------|------|
| `email` | 保留前1后1字符 + `***` | `user@example.com` → `u***@example.com` |
| `phone_mdn` | 保留前3后4字符 + `****` | `13812345678` → `138****5678` |
| `phone_msisdn` | 保留前3后4字符 + `****` | `+86138****5678` → `+861** ****5678` |
| `device_id` | 保留前8后4字符 + `****` | `12345678901234567` → `12345678****4567` |
| `open_id` | 保留前5后5字符 + `*****` | `oc_1234567890abcdef` → `oc_1234******cdef` |
| `union_id` | 保留前5后5字符 + `*****` | `ou_1234567890abcdef` → `ou_1234******cdef` |
| `name_real` | 保留1字符 + `**` | `张三` → `张**` |
| `name_alias` | 保留前1后1字符 + `**` | `王小明` → `王**明` |
| `email_address` | 同 email | — |
| `applicant_num` | 保留前3后3 + `*****` | `11010119900101********` → `110101****012**` |

#### 3.2.2 日志脱敏过滤器（Logback 示例）

配置文件 `logback-spring.xml`：

```xml
<configuration>
    <appender name="PII_FILTERED" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/canvas-application.log</file>
        <filter class="com.canvas.common.log.PiiLogingFilter" />
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <maxFileSize>100MB</maxFileSize>
            <totalSizeCap>10GB</totalSizeCap>
            <fileNamePattern>${LOG_PATH}/archive/canvas-application-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="PII_FILTERED" />
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

过滤规则：

```java
public class PiiLogingFilter extends ThresholdFilter {
    private static final Pattern[] PII_PATTERNS = {
        Pattern.compile("(\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b)"),  // email
        Pattern.compile("(\\b1[3-9]\\d{9}\\b)"),                                        // 手机号
        Pattern.compile("(\\b\\+861[3-9]\\d{9}\\b)"),                                    // 手机号(+86)
        Pattern.compile("(\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b)"), // 设备ID
        Pattern.compile("(\\boc_[0-9a-fA-F]{20,}\\b)"),                                   // OpenID
        Pattern.compile("(\\bou_[0-9a-fA-F]{20,}\\b)"),                                   // UnionID
    };

    @Override
    public boolean decide(FilterReply reply) {
        String message = getLogEvent().getFormattedMessage();
        for (Pattern pattern : PII_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                // 替换为脱敏版本
                String masked = formatPii(masker, matcher.group());
                return decide(ACCEPT_AND_STRIP); // 接受日志但进行脱敏
            }
        }
        return super.decide(reply);
    }
}
```

#### 3.2.3 数据库日志表脱敏

MyBatis-Plus 插件：

```java
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class PiiDataMaskingInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object parameter = invocation.getArgs()[1];
        if (parameter instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) parameter;
            for (String key : map.keySet()) {
                Object value = map.get(key);
                if (value != null && isPiiField(key)) {
                    map.put(key, DataMaskingUtil.mask(value.toString()));
                }
            }
        }
        return invocation.proceed();
    }

    private boolean isPiiField(String key) {
        return PII_FIELD_KEYS.contains(key) ||
               key.toLowerCase().contains("email") ||
               key.toLowerCase().contains("phone") ||
               key.toLowerCase().contains("identity");
    }
}
```

### 3.3 交互流程

**配置管理流程**：

1. 系统管理员登录后台
2. 进入「安全设置 → 日志脱敏配置」页面
3. 点击「添加 PII 字段」
4. 填写字段名称、字段Key、脱敏策略、是否启用
5. 保存后立即生效

**日志查看流程参考**：

1. 运维人员使用 ELK/Splunk 搜索日志
2. 系统自动对结果中的 PII 字段进行脱敏预览
3. 如需查看原始值，申请「脱敏审计权限」

---

## 4. 非功能需求

### 4.1 性能要求

- 脱敏延迟：单个日志元素的脱敏处理 < 1ms
- 日志注入延迟：< 0.1ms（不影响原有日志性能）
- 配置变更热更新：无需重启服务

### 4.2 安全要求

- 脱敏配置加密存储（密钥 AES-256-GCM）
- 脱敏操作内部审计，记录执行者、时间、脱敏字段列表
- 默认策略：所有 production 日志强制脱敏，dev/staging 可通过配置开关关闭

### 4.3 可用性要求

- 支持 100% 的日志输出渠道（Logback/Log4j2/Log4j/MDC）
- 配置错误时自动降级为脱敏失败（记录原始值+WARNING 日志）

---

## 5. 验收标准

- [ ] `canvas-common` 模块新增 `DataMaskingUtil.mask()` 和 `PiiLogFilter` 类
- [ ] `application.yml` 配置 `canvas.log.pii.enabled=true`
- [ ] 至少 10 个 PII 类型的正则规则已配置到 `PiiLogFilter`
- [ ] MyBatis-Plus 拦截器已集成，日志表查询自动脱敏
- [ ] 测试日志输出：
  - Email: `user@example.com` → `u***@example.com`
  - 手机号: `13812345678` → `138****5678`
- [ ] 配置管理 UI 可新增/编辑/删除脱敏规则，且配置变更热生效
- [ ] 运行 `mvn test` 100% 通过，包括 LogFilter 单元测试
- [ ] PII 脱敏对性能无影响（Benchmark 日志 < 1ms 延迟增）

---

## 6. 技术建议

### 6.1 涉及模块

- **前端**：`admin-frontend` - 脱敏配置管理页面
- **后端**：
  - `canvas-common` - 脱敏工具类、日志拦截器
  - `canvas-engine` - 数据库拦截器集成
  - `canvas-api` - 配置管理接口

### 6.2 技术要点

1. **脱敏策略配置**：`application.yml` 中配置全局规则，支持字段级别的正则覆盖
2. **性能优化**：脱敏规则编译后缓存为 Pattern 对象，避免每次分词
3. **过滤深度**：支持 MDC（Mapped Diagnostic Context）变量脱敏（`%X{userId}`）
4. **版本兼容**：最小支持 Java 11（JEP 274 正则别名优化）

### 6.3 预估工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 脱敏工具类 + 日志拦截器开发 | 5 |
| 数据库拦截器 + 脱敏注解 | 3 |
| 配置管理 UI 开发 | 4 |
| 单元测试 + 集成测试 | 2 |
| 配置文档 + 运维指南 | 1 |
| **总计** | **15** |

---

## 7. 依赖与风险

### 7.1 前置依赖

- 需要收集现有日志句柄（`Logback`/`Log4j2`）清单
- 确认数据库日志表结构（`ExecutionLog`、`NodeExecutionResult` 等）

### 7.2 风险

1. **误脱敏风险**：非 PII 字段被错误脱敏（如业务 ID）
   - 缓解：配置集测试 + 生产灰度验证
2. **性能影响**：多层脱敏可能增加日志开销
   - 缓解：Benchmarks 基准测试 + 使用 `isDebugEnabled()` 避免
3. **存量数据**：已存在的 PII 日志无法自动修正
   - 缓解：定期数据清理脚本（GDPR 30天规则）

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 安全合规 MEDIUM
- GDPR Art.32(1)(a): 数据完整性和保密性
- PIPL Art.51: 个人生物识别、住址、电话号码、行踪轨迹等敏感信息的保护
- Braze Log Masking: https://www.braze.com/help/basics/article/airship-log-limits-and-exemption
- Logback Filter 示例: https://logback.qos.ch/manual/filters.html
