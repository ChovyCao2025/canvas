# Testing Strategy

## Overview

后端测试覆盖合理 (112 文件)，前端测试严重不足 (30 文件, 仅纯函数)。

## Backend Testing

### Test Distribution

| Category | Count | Pattern |
|----------|-------|---------|
| Controller | 21 | WebTestClient + @SpringBootTest |
| Handler | 20 | Mockito mock ExecutionContext, verify NodeResult |
| Engine Core | 8 | DagEngine*Test (commit, depth, priority, pending) |
| Trigger/Execution | 7 | CanvasExecutionService*Test |
| Infrastructure | 6 | MqTriggerConsumer, OverflowRetry, Cache |
| Auth/Security | 3 | JwtAuthFilter, SecurityConfig, SysUserService |
| Domain Service | 10 | CdpUserService, TenantService, etc. |
| Rule Engine | 4 | AST, SQL Compiler, Graph Validator |
| Context/Perf | 3 | ConcurrencyTest, PerfRunTest |
| Audience | 5 | ComputeRunner, RuleEvaluator, SqlWhereGenerator |

### Test Patterns

**Handler Unit Test (典型模式):**
```java
@Test
void shouldReturnSuccessWhenConditionMet() {
    Map<String, Object> config = Map.of("field", "value");
    ExecutionContext ctx = mock(ExecutionContext.class);
    when(ctx.getFlatContext()).thenReturn(Map.of("field", "resolved"));
    
    NodeResult result = handler.executeAsync(config, ctx).block();
    
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getNextNodeId()).isEqualTo("next-node-id");
}
```

**Controller Integration Test (典型模式):**
```java
@Test
void shouldReturnCanvasList() {
    webTestClient.get().uri("/canvas")
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.code").isEqualTo(0);
}
```

### Coverage Requirements

- 无覆盖率门控 (建议添加 JaCoCo + 80% 最低门槛)
- 关键路径 (DagEngine, CanvasExecutionService) 需要更高覆盖

## Frontend Testing

### Current State

| Metric | Value |
|--------|-------|
| Test files | 30 |
| Component tests | **0** |
| E2E tests | **0** |
| Coverage config | None |
| Framework | Vitest (env=node) |

### Test File Pattern

所有30个测试文件测试纯函数：

```
branchHandles.test.ts        # 分支句柄派生逻辑
outletSchema.test.ts         # 出口Schema解析
outletRouting.test.ts        # 边↔bizConfig双向映射
insertNode.test.ts           # 边中插入节点
localDraft.test.ts           # localStorage草稿管理
connectionInteraction.test.ts # 连接验证
graphHydration.test.ts       # 图数据水合
formValues.test.ts           # 表单值归一化
presentation.test.ts         # 配置面板展示模型
...
```

### Recommended Testing Additions

1. **React Testing Library** — canvas-editor, config-panel 关键交互
2. **MSW (Mock Service Worker)** — API 层 mock，集成测试
3. **Playwright/Cypress** — 画布编辑器 E2E (拖拽、连线、保存、发布)
4. **ErrorBoundary 测试** — 验证错误边界捕获和恢复

## Integration Testing

### Backend Integration Points

| Integration | Test Approach | Current Coverage |
|-------------|---------------|------------------|
| MySQL | @SpringBootTest + Testcontainers | 部分覆盖 |
| Redis | Embedded Redis | CacheConfigTest |
| RocketMQ | Mock | MqTriggerConsumerTest |
| WireMock | 仅 dev 环境 | 无集成测试 |

### Missing Integration Tests

1. 端到端画布执行 (触发→节点→投递→完成)
2. 跨车道执行隔离验证
3. 灰度/金丝雀版本路由
4. DLQ 重试完整链路
5. 前后端集成 (API 契约验证)

## Performance Testing

- `ExecutionContextPerfRunTest` — 上下文并发性能测试
- `PerfRunContextTest` — 性能运行上下文测试
- 无系统级压测框架 (建议添加 Gatling/k6)

## Security Testing

| Area | Current | Needed |
|------|---------|--------|
| JWT 认证 | JwtAuthFilterTest | Token 过期/伪造/重放测试 |
| RBAC 授权 | SecurityConfigRoleTest | 跨角色访问拒绝测试 |
| SSRF 防护 | 无 | OutboundUrlValidator 边界测试 |
| SQL 注入 | MyBatis-Plus参数化查询 | 显式注入尝试测试 |
| XSS | 无 | 存储型XSS测试 |
| Groovy 沙箱 | GroovyHandlerValidationTest | 沙箱逃逸尝试测试 |