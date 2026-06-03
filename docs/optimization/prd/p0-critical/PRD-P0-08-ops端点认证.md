# PRD-P0-08-ops端点认证

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P0-08 |
| **需求名称** | ops端点认证 |
| **优先级** | P0 |
| **所属类别** | 安全合规 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | OAuth 2.0, JWT Bearer Token, Spring Security |

## 1. 问题描述

### 1.1 现状

平台 **/ops/ 端点无认证**，直接访问公开可访问，存在严重安全隐患。操作员可能未授权访问，导致数据泄露、权限误操作。

法规依据：
- **GDPR Art. 32**（数据安全）：采用适当的技术措施，如加密和身份验证
- **PIPL Art. 51**（个人信息保护）：采取必要措施防止个人信息泄露

### 1.2 痛点

- **数据泄露风险**：未授权访问敏感数据（user_key、PII）
- **权限误操作**：操作员可越权执行删除/归档等危险操作
- **合规审计缺失**：无法追溯操作来源（无操作人记录）

### 1.3 竞品对标

| 竞品 | 能力描述 |
|------|----------|
| OAuth 2.0 / OIDC | 统一认证授权，角色权限控制 |
| Spring Security FilterChain | 端点级权限验证（@PreAuthorize） |
| JWT Bearer Token | 敏感接口安全访问 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为系统管理员，我希望所有 /ops/ 端点都需要 JWT Bearer Token 认证，以便确保只有授权用户才能访问和维护系统。

### 2.2 成功指标

- **认证覆盖率** 100%（/ops/ 端点 100% 强制认证）
- **权限覆盖率** > 90%（关键操作需额外 RBAC 校验）
- **非法访问拦截率** 100%（攻击者无法绕过认证）

### 2.3 不做会怎样

- **合规违规**：违反 GDPR/PIPL 数据安全要求
- **数据泄露**：操作员可访问敏感 PII 数据
- **权限泄露**：普通操作员可执行危险操作（删除/归档）

---

## 3. 功能需求

### 3.1 核心功能

1. **JWT Bearer Token 验证**
   - 所有 /ops/ 端点接收 `Authorization: Bearer <token>` 请求头
   - JWT Token 验证（签名、过期时间、权限范围）
   - Token 刷新机制（Access Token 过期自动刷新）

2. **操作权限验证**
   - 关键操作需要角色级权限控制（ADMIN/TECH_OPERATOR）
   - 按端点分配权限（`@PreAuthorize("hasRole('ADMIN')")` 或自定义注解）
   - 权限不足时返回 403 Forbidden

3. **审计日志补充**
   - 记录操作人（通过 JWT `sub` 字段）
   - 记录操作来源（user_agent/IP）
   - 可追溯审计（写入 `canvas_audit_log` 表）

### 3.2 详细描述

#### 3.2.1 认证配置（application.yml）

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          internal:
            provider: internal
            client-id: canvas-ops
            client-secret: ${OPS_CLIENT_SECRET}
            authorization-grant-type: client_credentials
            scope: ops:all
        provider:
          internal:
            token-uri: http://localhost:8080/auth/oauth/token
```

#### 3.2.2 JWT 配置

```java
@Configuration
@EnableWebSecurity
public class OpsSecurityConfig {

    @Bean
    public SecurityFilterChain opsFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/ops/**")  // 仅保护 /ops/ 路径
            .csrf().disable()
            .httpBasic().disable()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()  // 所有 /ops/ 请求需认证
            )
            .oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(jwtConverter());  // 自定义权限转换

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> scopes = jwt.getClaimAsStringList("scopes");
            return scopes.stream()
                .map(scope -> "ROLE_" + scope.toUpperCase().replace(':', '_'))
                .collect(Collectors.toList());
        });
        return converter;
    }
}
```

#### 3.2.3 受保护端点示例

```java
@RestController
@RequestMapping("/ops")
public class OpsHealthController {

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Canvas Engine is healthy");
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('TECH_OPERATOR')")
    public ResponseEntity<ConfigDTO> getConfig() {
        // TODO: 返回配置信息
        return ResponseEntity.ok(new ConfigDTO());
    }

    @DeleteMapping("/canvas/{canvasId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCanvas(@PathVariable Long canvasId) {
        // 删除画布（危险操作）
        canvasService.delete(canvasId);
        auditLogService.log("DELETE_CANVAS", canvasId, "SUCCESS");
        return ResponseEntity.ok().build();
    }
}
```

#### 3.2.4 审计日志补充

```java
@Aspect
@Component
public class OpsAuditLogAspect {

    @Around("@annotation(opsAudit)")
    public Object logOpsAaaoperation(ProceedingJoinPoint joinPoint, OpsAudit opsAudit) throws Throwable {
        StartTimer start = Timer.start();
        String operation = opsAudit.operation();
        String operator = getCurrentUser();  // 从 SecurityContext 获取

        try {
            Object result = joinPoint.proceed();
            auditLogService.log("OP", operation, "SUCCESS", operator, start.duration());
            return result;
        } catch (Exception e) {
            auditLogService.log("OP", operation, "FAILURE", operator, start.duration(), e.getMessage());
            throw e;
        }
    }
}

// 审计日志表（续）
CREATE TABLE canvas_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operation_type VARCHAR(64) NOT NULL,      -- DELETE_CANVAS/RETRY_DLQ/PURGE_CACHE
    entity_type VARCHAR(64),                  -- canvas/user/dlq
    entity_id VARCHAR(64),                    -- 影响实体ID
    operator VARCHAR(64) NOT NULL,            -- 操作人 user_key
    operator_id VARCHAR(64),                  -- 操作人 user_id

    status VARCHAR(32) NOT NULL,              -- SUCCESS/FAILURE
    error_message TEXT,
    execution_time_ms BIGINT,
    ip_address VARCHAR(64),                   -- 来源IP
    user_agent VARCHAR(512),                  -- 浏览器UA

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operator (operator, created_at),
    INDEX idx_operation_type (operation_type, created_at)
);
```

### 3.3 交互流程

**流程 1：操作员访问受保护端点**

```
前端携带 JWT Token 访问 /ops/ 端点
  ↓
OpsSecurityConfig 验证 JWT 签名和过期时间
  ↓
提取权限范围 → 构建 ROLE_ 前缀
  ↓
@PreAuthorize 校验角色权限（如 hasRole('ADMIN')）
  ↓
通过验证 → 执行端点逻辑
  ↓
@OpsAudit 记录审计日志（操作人 + 执行时间）
```

---

## 4. 非功能需求

- **性能要求**：
  - JWT 验证延迟 < 50ms（本地签名验证）
  - 审计日志写入延迟 < 100ms（异步写入）

- **安全要求**：
  - JWT Secret 定期轮换（或使用 KMS）
  - 操作员权限最小化（仅分配所需权限）
  - 敏感操作二次确认（删除/归档）

- **可用性要求**：
  - Token 刷新机制（Access Token 过期前主动刷新新 Token）
  - Token 不可导出（前后端分离，Token 仅存储在前端 axios 拦截器）

---

## 5. 验收标准

- [ ] 配置 Spring Security 仅保护 `/ops/**` 路径
- [ ] JWT Bearer Token 验证机制实现
- [ ] 关键操作 @PreAuthorize 角色控制（至少 5 个操作）
- [ ] 审计日志自动记录（操作人 + 执行时间）
- [ ] 无认证请求返回 401 Unauthorized
- [ ] 无权限请求返回 403 Forbidden
- [ ] 审计日志表字段完整
- [ ] 操作员权限最小化（审计审计现有角色分配）
- [ ] 敏感操作二次确认（删除/归档）

---

## 6. 技术建议

### 6.1 涉及模块

- **后端**：
  - `backend/canvas-engine/src/main/java/com/canvas/config/OpsSecurityConfig.java`
  - `backend/canvas-engine/src/main/java/com/canvas/executor/audit/OpsAuditAspect.java`
  - `backend/canvas-engine/src/main/java/com/canvas/model/OpsAuditLog.java`
  - 配置文件 `application.yml`

- **数据库**：
  - Flyway 新增 V91 表（与现有审计日志表合并，新增 Operator_Id 字段）

### 6.2 技术要点

1. **JWT Token 生成（获取 Token）**
   ```bash
   curl -X POST http://localhost:8080/auth/oauth/token \
     -u canvas-ops:${OPS_CLIENT_SECRET} \
     -d grant_type=client_credentials \
     -d scope=ops:all
   ```

2. **JWT Token 验证优先级**
   - vs OAuth2 Resource Server（JWT 验证优先级高于 Basic Auth）
   - vs Spring Security FilterChain（JWT 验证在 Filter 链早期执行）

3. **权限范围设计**
   - `ops:all` → 拥有所有 ops 端点权限（管理员）
   - `ops:read` → 仅读权限
   - `ops:write` → 写入权限
   - `ops:admin` → 管理权限

4. **审计日志索引**
   - 复合索引：`(operator, created_at)`（按操作员查询最近操作）
   - 复合索引：`(operation_type, created_at)`（按操作类型查询审计）

### 6.3 预估工作量

- **第一阶段（认证配置）**：2 天
  - Spring Security 配置
  - JWT Bearer Token 验证
  - 配置刷新机制

- **第二阶段（权限控制）**：2 天
  - @PreAuthorize 角色控制
  - 权限范围定义
  - 权限测试

- **第三阶段（审计日志）**：2 天
  - @OpsAuditAop 实现
  - 审计日志表扩展
  - 操作人提取

- **第四阶段（测试）**：1 天
   - 未授权访问测试（401）
   - 非法访问测试（403）
   - 敏感操作二次确认测试

**总计：7 人天（1 周）**

---

## 7. 依赖与风险

### 7.1 前置依赖

- Spring Security 依赖（已引入）- 已存在
- JWT Secret 配置 - 已存在
- 审计日志基础设施（MyBatis-Plus）- 已存在

### 7.2 风险

- **开发环境兼容性**：现有 curl 访问方式需更新为 Bearer Token（需文档升级）

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 安全合规层缺项
- GDPR Art. 32 数据安全要求
- PIPL Art. 51 个人信息保护要求
