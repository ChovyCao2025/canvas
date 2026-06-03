# Coding Standards

## Java (Backend)

### 命名约定

| 类型 | 约定 | 示例 |
|------|------|------|
| 类 | PascalCase | `CanvasExecutionService` |
| 方法 | camelCase | `triggerDownstream()` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY` |
| 包 | 小写无下划线 | `org.chovy.canvas.engine.handler` |
| Mapper | 实体名+Mapper | `CanvasExecutionMapper` |
| DO | 实体名+DO | `CanvasExecutionDO` |

### 注解模式

- `@Data` (Lombok) 用于 DO/DTO
- `@Slf4j` (Lombok) 用于日志
- `@Component` / `@Service` 用于 Spring Bean
- `@NodeHandlerType("TYPE_KEY")` 用于 Handler 注册
- `@TableName("table_name")` 用于 MyBatis-Plus DO
- `@TableId(type = IdType.AUTO)` 用于自增主键
- `@TableField(select = false)` 用于敏感字段 (password)

### 响应式编程规则

1. **Controller 返回 `Mono<R<T>>`**
2. **阻塞 DB 调用必须包装**: `Mono.fromCallable(() -> mapper.select()).subscribeOn(Schedulers.boundedElastic())`
3. **Handler 返回 `Mono<NodeResult>`**
4. **禁止在 Netty 事件循环上执行阻塞操作**

### Linting & 静态分析

- 无 Checkstyle/SpotBugs 配置 (建议添加)
- 代码注释中文为主
- 日志使用 SLF4J: `log.error("[DAG] nodeId={} error={}", nodeId, e.getMessage())`

## TypeScript (Frontend)

### 命名约定

| 类型 | 约定 | 示例 |
|------|------|------|
| 组件 | PascalCase | `CanvasNode` |
| Hook | camelCase + use前缀 | `useSystemOptions` |
| 文件 | camelCase | `outletRouting.ts` |
| 类型 | PascalCase | `CanvasNodeData` |
| 常量 | UPPER_SNAKE_CASE | `DEFAULT_NODE_SEPARATION` |

### 组件模式

- 页面组件在 `src/pages/<page-name>/index.tsx`
- 辅助函数提取到同目录 `.ts` 模块
- 测试文件同目录 `.test.ts`
- 所有页面使用 `React.lazy()` 懒加载

### API 调用模式

```typescript
try {
  const result = await canvasApi.publish(canvasId);
  message.success('发布成功');
} catch (e: any) {
  message.error(e?.response?.data?.message || '发布失败');
}
```

### 样式

- 全部 inline `style={{}}`
- 唯一 CSS 文件: `settingsPanel.css`
- antd 主题通过 ConfigProvider (zhCN)

## Git Conventions

- Commit message: `feat:`, `fix:`, `refactor:`, `docs:`, `chore:` 前缀
- 最近示例: `feat: add CommitAction node handler and related configurations`

## Testing Conventions

### Backend

- 测试文件: `src/test/java/.../*Test.java`
- Controller: WebTestClient + `@SpringBootTest`
- Handler: Mockito mock ExecutionContext, verify NodeResult
- 并发: ExecutionContextConcurrencyTest

### Frontend

- 测试文件: 同目录 `.test.ts`
- 仅纯函数测试
- 无组件渲染测试 (无 React Testing Library)

## Architecture Rules

1. **Handler 严禁直接注入 Mapper** — 必须通过 Repository 接口 (新规范)
2. **新代码不允许 @Lazy** — 循环依赖说明架构设计有问题
3. **Java 类不超过 500 行** — 超过需拆分
4. **React 组件不超过 500 行** — 超过需提取 hooks + 子组件
5. **Flyway 迁移仅增量** — 禁止修改已有迁移文件
6. **敏感配置走环境变量** — `CANVAS_JWT_SECRET`, `CANVAS_EVENT_REPORT_SECRET`