# PRD-P2-04-Swagger及Actuator生产关闭

> 本文档为营销画布平台 API 文档及监控接口生产环境关闭需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-04 |
| **需求名称** | Swagger及Actuator生产关闭 |
| **优先级** | P2 |
| **所属类别** | 安全合规 MEDIUM |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

当前配置 `springdoc/swagger-ui.enabled: true`，公开 Swagger 文档和 Actuator 监控接口：

```yaml
springdoc:
  swagger-ui:
    enabled: true
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

**暴露的风险接口**：

| 接口 | 风险 | 影响 |
|------|------|------|
| `/swagger-ui/index.html` | API 文档被爬虫获取，敏感字段暴露 | 用户画像、营销策略泄露 |
| `/v3/api-docs` | OpenAPI 规范，提示调用方可快速开发恶意脚本 | API 滥用 |
| `/actuator/health` | 系统健康状态暴露 | 完整监控信息泄露 |
| `/actuator/env` | 环境变量（包括数据库密码、API 密钥） | 完全接管系统 |
| `/actuator/metrics` | 完整的 JVM、GC、线程池指标 | 通过拟合分析推断业务规模 |

### 1.2 痛点

1. **API 安全漏洞**：OWASP Top 10 - API 安全 Top 3：未授权访问、敏感数据泄露、DDoS 操纵
2. **恶意脚本开发**：前端工程师可快速调用 API，但恶意人员可利用 Swagger 自动生成脚本
3. **部署泄露**：开发调试时生成 Bean 校验配置 showError（Test/Dev 环境），生产误部署

### 1.3 竞品对标

| 竞品 | 策略 |
|------|------|
| Braze | Actuator 仅暴露 `health`，密钥全部环境变量，日志中脱敏 |
| Iterable | 无 Actuator，仅内部 Zabbix 监控 |

---

## 2. 目标与价值

### 2.1 用户故事

- **运维人员**：作为系统管理员，我希望生产环境关闭 Swagger 和 Actuator 的所有端点，只保留 `health` 和 `prometheus` 指标。

### 2.2 成功指标

- 生产环境 `/swagger-ui/index.html` 返回 404 Forbidden
- 生产环境 `/actuator/*` 仅开放 `health`, `prometheus`, `info` 三个端点
- 调试时通过 `?debug=springboot` 参数临时启用（仅内部 IP）

### 2.3 不做会怎样

- 被 OWASP ZAP 自动扫描发现 API 拼写错误、参数泄露等问题
- 竞品/爬虫抓取 API 规范，快速开发自动化攻击脚本

---

## 3. 功能需求

### 3.1 核心功能

1. **生产模式检测**：通过配置 `app.env: PRODUCTION` 或 `spring.profiles.active: prod` 自动判断环境
2. **Swagger 禁用开关**：生产模式禁用 Swagger UI 和 API 文档
3. **Actuator 端点过滤**：生产环境只开放安全端点（`actuator.trace.enabled=false`, `actuator.endpoint.health.show-details=never`）

#### 3.1.1 配置文件示例

```yaml
spring:
  profiles: prod

# 关闭 Swagger
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false

# Actuator 限制
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info
  endpoint:
    health:
      show-details: never  # 不暴露完整 health 详细信息
    env:
      enabled: false
  trace:
    enabled: false
  health:
    defaults:
      enabled: false

app:
  env: PRODUCTION
  secure-mode-enabled: true
```

#### 3.1.2 安全层 Kotlin 校验

```kotlin
@Aspect
@Component
class ApiSecurityAspect {
    @Before("@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    fun validateEnvironment(joinPoint: JoinPoint) {
        if (!isProductionMode()) {
            return
        }

        // 禁止访问 /swagger-ui/ 和 /actuator/
        val path = ServletRequestAttributes().getRequest().requestURI
        if (path.startsWith("/swagger-ui") || path.startsWith("/actuator/")) {
            throw AccessDeniedException("Swagger and Actuator are disabled in PRODUCTION mode")
        }
    }
}
```

### 3.2 详细描述

**接口访问策略**：

| 环境 | `/swagger-ui/index.html` | `/v3/api-docs` | `/actuator/*` | 验算方式 |
|------|------------------------|----------------|---------------|----------|
| Development | ✅ 开放 | ✅ 开放 | ✅ 开放（全量） | `app.env = DEVELOPMENT` |
| Testing | ✅ 开放 | ✅ 开放 | ⚠️ 开放部分端点 | `app.env = TESTING` |
| Production | ❌ 404 | ❌ 404 | ✅ 仅 `health`, `prometheus`, `info` | `app.env = PRODUCTION` |

### 3.3 交互流程

**部署前的静态检查**：

1. CI/CD 流水线中集成 `mvn checksecurity` 脚本
2. 自动扫描配置文件中是否误留 `swagger-ui.enabled=true` 或 `actuator.exposure.include=*`
3. 如果扫描到误留，阻塞合并请求（不允许构建）

---

## 4. 非功能需求

### 4.1 性能要求

- 启用 Actuator 增加的 CPU 开销 < 5%（仅保留 3 个端点时）

### 4.2 安全要求

| 端点 | 生产模式暴露内容 | 非生产模式暴露内容 |
|------|----------------|-------------------|
| `/actuator/health` | `{status: "UP"}` | `{status: "UP", details: {...}}` |
| `/actuator/beans` | ❌ | ✓ (所有 Bean 列表) |
| `/actuator/mappings` | ❌ | ✓ (所有 URL 映射) |
| `/actuator/env` | ❌ | ✓ (所有环境变量，包括密码) |

### 4.3 可用性要求

- 支持「紧急调试端口」配置（如 `canvas.secure-debug-port: 8080`，只允许 10.0.0.x 访问）

---

## 5. 验收标准

- [ ] `application.yml` (prod) 中 `springdoc.swagger-ui.enabled=false`
- [ ] `application.yml` (prod) 中 `actuator.exposure.include=health,prometheus,info`
- [ ] `mvn verify` 集成 `SwaggerActuatorCheckPlugin`，扫描到误留配置时失败
- [ ] 测试访问 `/swagger-ui/index.html` 返回 404 Forbidden
- [ ] 测试访问 `/v3/api-docs` 返回 404 Forbidden
- [ ] 测试访问 `/actuator/env` 返回 403 Forbidden
- [ ] 测试访问 `/actuator/health` 返回 `{status: "UP"}`

---

## 6. 技术建议

### 6.1 涉及模块

- **后端**：`canvas-web` - Actuator 配置；`canvas-security` - 接口访问校验
- **CI/CD**：Jenkins/GitHub Actions - 静态安全扫描

### 6.2 技术要点

1. **Bean 定义顺序**：确保 `CorsConfig` 在 `SecurityConfig` 后定义，避免跨域检查过早失败
2. **多环境配置分离**：
   - `src/main/resources/application-dev.yml`
   - `src/main/resources/application-prod.yml`
   - 避免在同一 `application.yml` 中混合
3. **Swagger 版本兼容**：`springdoc-openapi` > 1.7.0 支持 `api-docs.enabled` 配置

### 6.3 预估工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 配置文件整理 | 2 |
| CI/CD 集成 | 1 |
| 必要单元测试 | 2 |
| 文档更新 | 0.5 |
| **总计** | **5.5** |

---

## 7. 依赖与风险

### 7.1 前置依赖

- 明确区分 Dev/Test/Pro 三个环境
- 确认 Actuator 监控指标投影方式（Prometheus 原生）

### 7.2 风险

1. **监控缺失**：关闭 Actuator 后，部分自定义指标可能无法直接暴露
   - 缓解：对接 Prometheus Exporter（Spring Boot Actuator + Micrometer）

2. **安全事故发现困难**：开发时使用 Swagger 调试，生产时关闭，可能导致测试路径遗漏
   - 缓解：使用 `ghproxy` 或 `internal-test` 集群进行全量测试

---

## 8. 参考资料

- OWASP API Security Top 10 - ID8: Excessive Data Exposure
- BMAD 产品设计审查报告 (2026-05-31) - 安全合规 MEDIUM

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-04-Swagger及Actuator生产关闭.md`）**
