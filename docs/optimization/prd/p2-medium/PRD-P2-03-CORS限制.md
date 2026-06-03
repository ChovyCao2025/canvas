# PRD-P2-03-CORS限制

> 本文档为营销画布平台 CORS 限制需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-03 |
| **需求名称** | CORS限制 |
| **优先级** | P2 |
| **所属类别** | 安全合规 MEDIUM |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

`application.yml` 中配置 `canvas.cors.allowed-origins: "*"`，允许所有域名跨域访问：

```yaml
canvas:
  cors:
    allowed-origins: "*"
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: "*"
```

### 1.2 痛点

1. **CSRF 攻击风险**：开放 CORS 允许恶意网站调用 `/canvas/execute/direct/*` 接口
2. **未授权访问**：任何第三方网站可读取画布/节点配置
3. **API 滥用**：竞品/恶意脚本可批量触发画布执行

### 1.3 竞品对标

| 竞品 | CORS 策略 |
|------|----------|
| Braze | 限制白名单域名，支持动态添加 |
| Iterable | 租户级配置 CORS 策略 |

---

## 2. 目标与价值

### 2.1 用户故事

- **运维人员**：作为安全负责人，我希望限制只有指定前端域名可调用 API，以免被恶意网站滥用。

### 2.2 成功指标

- 默认策略：只允许当前应用的 Origin（`null` 表示仅同源请求）
- 支持管理后台配置白名单域名（最多 20 个）
- 所有非白名单请求被拦截，JS 控台返回预定义错误

### 2.3 不做会怎样

- 遭受 CSRF 攻击，导致画布被恶意编辑/执行
- 消费者数据（如用户列表、执行日志）被第三方爬虫抓取

---

## 3. 功能需求

### 3.1 核心功能

1. **默认 CORS 策略**：`allowed-origins: "https://your-app.com,https://internal.your-dns.com"`（不填则禁止跨域）
2. **动态白名单配置**：通过后台「安全设置 → CORS 配置」管理允许的前端域名
3. **代理场景支持**：允许无 Host 的内网代理请求（如 `deployment/run`）
4. **CORS 检查开关**：通过配置 `canvas.cors.enabled: true/false` 控制是否启用

### 3.2 详细描述

#### 3.2.1 CORS 拦截器配置

```java
@Component
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        config.setAllowedHeaders(Arrays.asList("*"));

        // 从配置读取白名单
        List<String> allowedOrigins = Arrays.asList(
            environment.getProperty("canvas.cors.allowed-origins").split(",")
        );

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        CorsFilter filter = new CorsFilter(source);
        filter.setCorsConfigurations(Collections.singletonMap("/**", config));
        return filter;
    }
}
```

#### 3.2.2 前端配置示例

```typescript
// vite.config.ts - 前端代理
server: {
  proxy: {
    '/api': {
      target: 'https://canvas-api.your-dns.com',
      changeOrigin: true,
      secure: true
    }
  }
}
```

### 3.3 交互流程

1. 运维人员在后台「安全设置 → CORS 配置」添加白名单域名
2. 所有 Origin 不在白名单中的请求返回 403 Forbidden
3. 调试时使用 `?debug-cors=true` 参数绕过检查（仅开发者模式）

---

## 4. 非功能需求

### 4.1 性能要求

- CORS 检查延迟：< 2ms

### 4.2 安全要求

- Headers 限制：禁止发送 `Content-Type: application/json` 以外的敏感请求头
- Cookie 安全：只允许同名域的 Cookie

### 4.3 可用性要求

- 白名单变更后立即生效（无需重启）

---

## 5. 验收标准

- [ ] 配置文件移除星号，改写为具体域名
- [ ] 测试跨域请求是否被拦截（Origin 为 https://evil.com）
- [ ] 支持后台上报名证白名单管理 UI
- [ ] 运行 `curl -H "Origin: https://evil.com" https://api.canvas.com/api/v1/canvas` 返回 403

---

## 6. 技术建议

### 6.1 涉及模块

- **前端**：`admin-frontend` - CORS 配置 UI
- **后端**：`canvas-web` - CORS 拦截器配置

### 6.2 技术要点

1. 使用 Spring 的 `CorsFilter` 全局过滤
2. 白名单校验：支持正则表达式（如 `https://*.your-dns.com`）
3. 调试模式：`canvas.cors.debug: true` 返回详细 CORS 响应头结构

### 6.3 预估工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 配置校验 | 1 |
| 后端接口开发 | 2 |
| 前端 CORS 配置 UI | 1 |
| 文档更新 | 0.5 |
| **总计** | **4.5** |

---

## 7. 依赖与风险

### 7.1 前置依赖

- 确认所有已部署的前端域名
- 区分生产/测试环境 CORS 配置（生产必须严谨）

### 7.2 风险

1. **代理配置复杂**：微服务架构中会涉及多个服务
   - 缓解：Nginx 层统一配置 CORS

---

## 8. 参考资料

- 维基百科 CORS 规范
- Braze 安全最佳实践

---

**（内容精简，剩余 51 个 PRD 见续篇）**
