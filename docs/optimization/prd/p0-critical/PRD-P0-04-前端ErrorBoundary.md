# PRD-P0-04-前端ErrorBoundary

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P0-04 |
| **需求名称** | 前端ErrorBoundary |
| **优先级** | P0 |
| **所属类别** | 前端稳定性 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | React Error Boundary 最佳实践、Sentry 监控 |

## 1. 问题描述

### 1.1 现状

当前平台 **前端无 ErrorBoundary 设计**，组件崩溃会导致：
- 页面白屏
- React 错误日志暴露（Stack Trace 泄露）
- 用户无法继续操作

React Error Boundary 是 React 官方推荐的异常处理机制，平台未实现会影响用户体验和安全性。

### 1.2 痛点

- **用户体验差**：组件崩溃导致用户无法使用画布编辑器，需刷新浏览器
- **错误信息不可控**：Stack Trace 可能暴露内部实现细节（安全风险）
- **调试困难**：无错误边界捕获，生产环境错误无法快速定位

### 1.3 竞品对标

| 竞品 | 能力描述 |
|------|----------|
| React Error Boundary | 捕获子组件树异常、显示降级 UI、上报错误日志 |
| Sentry 监控 | 错误实时上报、用户访问上下文、聚合分析 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为运营用户，当画布编辑器某个组件崩溃时，我希望看到一个友好的降级提示并提供联系支持的方式，而不是白屏。

### 2.2 成功指标

- **白屏率降低** < 1%（相对基线降低 > 80%）
- **错误捕获率** >= 95%（主要前端组件）
- **错误反馈时间** < 5 分钟（从崩溃到错误通知）

### 2.3 不做会怎样

- 组件崩溃导致严重的用户体验问题（企业级产品无法接受）
- 安全风险：Stack Trace 泄露内部实现细节
- 错误无法追溯，开发团队无法快速修复

---

## 3. 功能需求

### 3.1 核心功能

1. **ErrorBoundary 组件封装**
   - 捕获子组件树异常
   - 显示友好的降级 UI（错误提示、联系支持、刷新按钮）
   - 错误信息脱敏（不暴露 Stack Trace、开发环境代码）

2. **错误上报机制**
   - 错误捕获后自动上报到错误监控服务（Sentry/自定义日志服务）
   - 上报信息：组件路径、错误类型、错误消息、用户会话ID、环境（dev/prod）
   - 支持错误重试（网络失败时本地存储后重试）

3. **错误详情页面**
   - 错误列表展示（按时间排序）
   - 错误详情查看（聚合错误统计、出现频率、受影响用户）
   - 错误响应（开发团队查看并标记已处理）

### 3.2 详细描述

#### 3.2.1 ErrorBoundary 组件实现

```tsx
// frontend/src/components/ErrorBoundary.tsx
import React, { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
  children?: ReactNode;
  fallback?: ReactNode;  // 自定义降级 UI
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // 上报到错误监控服务
    ErrorMonitoringService.report(error, errorInfo);

    // 本地记录
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      // 返回自定义降级 UI
      return this.props.fallback || (
        <div className="error-boundary">
          <h2>出错了</h2>
          <p>很抱歉，页面遇到了一些问题，请稍后再试。</p>
          <button onClick={() => window.location.reload()}>
            刷新页面
          </button>
          <p>
            如问题持续，请联系技术支持：{' '}
            <a href="mailto:support@example.com">support@example.com</a>
          </p>
          {process.env.NODE_ENV === 'development' && (
            <div>
              <p>错误信息：<code>{this.state.error?.message}</code></p>
              {this.state.errorInfo && (
                <details>
                  <summary>查看调试信息</summary>
                  <pre style={{ whiteSpace: 'pre-wrap' }}>
                    {this.serializeError(this.state.errorInfo)}
                  </pre>
                </details>
              )}
            </div>
          )}
        </div>
      );
    }

    return this.props.children;
  }

  // 序列化错误信息（生产环境不暴露完整 Stack Trace）
  private serializeError(errorInfo: ErrorInfo): string {
    const serialized: any = {
      componentStack: errorInfo.componentStack.replace(
        /\n/g,
        '\n'
      ).substring(0, 500) + '...',  // 仅保留前 500 字符
    };

    if (process.env.NODE_ENV === 'development') {
      serialized.error = {
        name: this.state.error?.name,
        message: this.state.error?.message,
        stack: this.state.error?.stack,
      };
    }

    return JSON.stringify(serialized, null, 2);
  }
}

export default ErrorBoundary;
```

#### 3.2.2 错误监控服务接口

```typescript
// frontend/src/services/ErrorMonitoringService.ts
class ErrorMonitoringService {
  private readonly endpoint = '/api/errors/report';

  /**
   * 上报错误到后端
   */
  async report(error: Error, errorInfo: ErrorInfo): Promise<void> {
    const payload = {
      error: {
        name: error.name,
        message: error.message,
        stack: this.isDevelopment()
          ? error.stack
          : undefined,  // 生产环境仅上报错误名称和消息
      },
      componentStack: this.extractComponentStack(errorInfo),
      sessionId: localStorage.getItem('sessionId'),
      userAgent: navigator.userAgent,
      url: window.location.href,
      environment: this.getEnvironment(),
      timestamp: Date.now(),
    };

    try {
      // 尝试上报
      await this.fetchWithRetry(payload, 3, 1000);
    } catch (e) {
      // 上报失败时本地存储（下次尝试）
      this.localStoreError(payload);
    }
  }

  private async fetchWithRetry(
    payload: any,
    maxRetries: number,
    delay: number
  ): Promise<void> {
    for (let i = 0; i < maxRetries; i++) {
      try {
        await fetch(this.endpoint, {
          method: 'POST',
          body: JSON.stringify(payload),
        });
        return;  // 上报成功
      } catch {
        if (i === maxRetries - 1) throw;  // 最后一次尝试失败
        await this.sleep(delay * (i + 1));
      }
    }
  }

  // 从 ErrorInfo 中提取原型链组件栈
  private extractComponentStack(errorInfo: ErrorInfo): string {
    return errorInfo.componentStack
      .replace(/\n/g, '\n')
      .substring(0, 500);
  }

  // 本地存储错误（网络失败时重用）
  private localStoreError(payload: any): void {
    const errors = JSON.parse(localStorage.getItem('errorBuffer') || '[]');
    errors.push(payload);
    if (errors.length > 10) errors.shift();  // 仅保留最近 10 条
    localStorage.setItem('errorBuffer', JSON.stringify(errors));
  }

  // 重试本地错误（下次页面加载时尝试上报）
  private async retryStoredErrors(): Promise<void> {
    const errors = JSON.parse(
      localStorage.getItem('errorBuffer') || '[]'
    );
    for (const error of errors) {
      await this.fetchWithRetry(error, 3, 1000);
    }
    localStorage.removeItem('errorBuffer');
  }
}

export default new ErrorMonitoringService();
```

#### 3.2.3 错误详情页面 API

```java
@RestController
@RequestMapping("/api/errors")
public class ErrorReportController {

    @PostMapping("/report")
    public ResponseEntity<Void> reportError(
        @RequestBody ErrorReportPayload payload
    ) {
        // 1. 写入错误日志表
        errorLogService.save(payload);

        // 2. 推送到错误聚合服务（Elasticsearch/Kafka）
        errorAsyncService.sendToAnalytics(payload);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<Page<ErrorLog>> listErrors(
        @RequestParam(required = false) String component,
        @RequestParam(required = false) String level,
        @RequestParam(required = false) Boolean resolved,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        Page<ErrorLog> errors = errorLogService.findByConditions(
            component, level, resolved, page, pageSize
        );
        return ResponseEntity.ok(errors);
    }

    @GetMapping("/{errorId}")
    public ResponseEntity<ErrorLog> getErrorDetail(
        @PathVariable Long errorId
    ) {
        ErrorLog error = errorLogService.findById(errorId);
        return ResponseEntity.ok(error);
    }

    @PostMapping("/{errorId}/resolve")
    public ResponseEntity<Void> resolveError(
        @PathVariable Long errorId
    ) {
        errorLogService.markResolved(errorId);
        return ResponseEntity.ok().build();
    }
}

// 错误日志表
CREATE TABLE canvas_error_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    component_path VARCHAR(512) NOT NULL,   // 组件路径（如 CanvasEditor）
    error_name VARCHAR(128) NOT NULL,        // 错误名称
    error_message TEXT NOT NULL,             // 错误消息
    component_stack TEXT,                    // 组件栈（生产环境脱敏后）
    session_id VARCHAR(64),                  // 用户会话ID
    user_id VARCHAR(64),                     // 用户ID（匿名）
    environment VARCHAR(32),                 // 环境: dev/staging/prod
    error_level VARCHAR(32),                 // error/warning/info
    resolved BIT DEFAULT 0,                  // 是否已处理

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_component (component_path),
    INDEX idx_environment (environment),
    INDEX idx_resolved (resolved)
);
```

### 3.3 交互流程

**流程 1：组件崩溃触发 ErrorBoundary**

```
前端组件抛出异常
  ↓
React 自动捕获（getDerivedStateFromError）
  ↓
React 更新 state.hasError = true
  ↓
ErrorBoundary 重新渲染，显示降级 UI
  ↓
componentDidCatch 捕获向错误监控服务上报
```

**流程 2：运营查看错误详情**

1. 点击右上角"异常日志" → 进入错误详情页面
2. 查看错误列表（按时间排序，支持筛选）
3. 点击错误 → 查看详情（错误消息、组件栈、影响用户数）
4. 标记"已处理" → 更新 `resolved` 字段
5. 错误聚合服务自动生成周报

---

## 4. 非功能需求

- **性能要求**：
  - ErrorBoundary 内存占用 < 100KB（异常后不保留完整组件树）

- **安全要求**：
  - 生产环境不暴露 Stack Trace（仅上报错误名称+消息）
  - 错误日志访问权限控制（产品/技术团队 RBAC）

- **可用性要求**：
  - 错误明细页面查询延迟 < 1 秒（索引优化）

---

## 5. 验收标准

- [ ] 前端封装 ErrorBoundary 组件（支持自定义降级 UI）
- [ ] 错误监控服务实现上报接口
- [ ] 后端新建 `canvas_error_log` 表（Flyway V86+）
- [ ] 错误详情页面可查看/筛选/解决错误
- [ ] 生产环境 Stack Trace 脱敏
- [ ] 错误访问权限控制（RBAC）
- [ ] 提供刷新页面按钮（降级 UI）
- [ ] 错误上报延迟 < 30 秒（网络成功时）

---

## 6. 技术建议

### 6.1 涉及模块

- **前端**：
  - `frontend/src/components/ErrorBoundary.tsx`
  - `frontend/src/services/ErrorMonitoringService.ts`
  - `frontend/src/pages/ErrorLogList.tsx`
  - 画布编辑器主容器包裹 ErrorBoundary

- **后端**：
  - `backend/canvas-engine/src/main/java/com/canvas/executor/ErrorReportController.java`
  - `backend/canvas-engine/src/main/java/com/canvas/model/ErrorLog.java`
  - 错误聚合服务（Elasticsearch/Kafka）

- **数据库**：
  - Flyway 新增 V86 表

### 6.2 技术要点

1. **渲染性能优化**
   - ErrorBoundary 仅捕获子组件树，不影响整个应用性能
   - 降级 UI 优先渲染（骨架屏 + 简单文本），避免复杂动画

2. **错误信息脱敏**
   - 生产环境仅上报 `errorName` 和 `errorMessage`
   - `componentStack` 剥离第三方库信息（React Router/antd）

3. **本地存储重试**
   - 网络失败时本地存储错误（localStorage）
   - 页面加载时自动重试（V 命中时先重试本地错误）

4. **错误聚合**
   - 按组件路径聚合错误（Elasticsearch Aggregation）
   - 生成日报/周报（用户高频错误类型）

### 6.3 预估工作量

- **第一阶段（ErrorBoundary 核心封装）**：2 天
  - ErrorBoundary 组件实现
  - 降级 UI 设计
  - 错误监控服务接口实现

- **第二阶段（后端错误日志）**：2 天
  - ErrorReportController 实现
  - `canvas_error_log` 表设计
  - 错误聚合服务集成

- **第三阶段（错误详情页面）**：2 天
  - 错误列表页面
  - 错误详情页面
  - 错误筛选/解决功能

- **第四阶段（测试）**：1 天
  - 组件崩溃场景测试
  - 错误上报测试
  - 生产环境脱敏验证

**总计：7 人天（1 周）**

---

## 7. 依赖与风险

### 7.1 前置依赖

- 错误聚合服务（Elasticsearch/Kafka）- 非阻塞，可后续集成

### 7.2 风险

- **性能影响**：大量错误可能导致 ErrorBoundary 内存占用过高（需限制保留的错误日志数量）
- **误报风险**：业务异常触发不应上报为系统错误（需分类规则）

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 前端稳定性层缺项
- React 官方文档 — Error Boundaries
- Sentry React SDK 文档
