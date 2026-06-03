# Frontend Architecture

## Overview

React 18 SPA + Vite，使用 @xyflow/react 实现可视化 DAG 编辑器。纯客户端渲染，API 通过 Vite proxy 代理到后端 :8080。

## Directory Structure

```
frontend/src/
├── App.tsx                    # Root: providers + lazy routes + guards
├── main.tsx                   # Entry: StrictMode + antd ConfigProvider (zhCN)
├── auth/                      # 认证守卫 + 角色工具
├── components/
│   ├── canvas/                # DAG 编辑器核心组件
│   │   ├── CanvasNode.tsx     # 自定义 ReactFlow 节点 (Memoized)
│   │   ├── BranchPlaceholderNode.tsx  # 未连接分支的虚拟拖拽目标
│   │   ├── HoverEdge.tsx      # 自定义边 (hover+删除+插入)
│   │   └── constants.ts       # 40+节点类型, 12分类, 颜色映射
│   ├── config-panel/          # 右侧属性检查器 (1414行)
│   │   ├── index.tsx          # 主面板，根据后端schema动态渲染表单
│   │   ├── InspectorCards.tsx # HeaderCard, ConfigSectionCard
│   │   ├── CronBuilder.tsx    # Cron 表达式可视化编辑器
│   │   └── controlChrome.ts   # iOS风格表单控件样式
│   ├── layout/                # 侧边栏+内容区布局
│   ├── node-panel/            # 左侧节点库 (搜索+分类+拖拽)
│   └── notifications/         # 通知铃铛 + WebSocket
├── context/                   # 3个React Context
│   ├── AuthContext.tsx         # 认证状态 + localStorage
│   ├── CanvasActionsContext.tsx # 编辑器操作回调
│   └── NotificationContext.tsx # 通知状态 + WS + HTTP轮询
├── hooks/                     # useSystemOptions, useBranchPlaceholders
├── pages/                     # 页面组件 (React.lazy 懒加载)
│   ├── canvas-editor/         # 核心编辑器 (2085行 + 8个辅助模块)
│   ├── canvas-list/           # 画布列表
│   ├── canvas-stats/          # 执行统计
│   ├── cdp-users/             # CDP 用户中心
│   ├── audience-edit/         # 人群规则编辑
│   ├── api-config/            # API 定义管理
│   └── ... (20+ 页面)
├── services/                  # API 客户端
│   ├── api.ts                 # Axios 实例 + 所有端点 (463行)
│   ├── audienceApi.ts         # 人群 CRUD + 计算
│   ├── notificationApi.ts     # 通知 + WebSocket ticket
│   └── systemOptions.ts       # 系统字典
└── types/                     # TypeScript 类型定义
    ├── index.ts               # R<T>, PageResult, Canvas, NodeTypeRegistry
    └── canvas.ts              # BackendNode, BizConfig, CanvasNodeData
```

## State Management

**无外部状态管理库。** 当前方案：

| 层级 | 方式 | 使用场景 |
|------|------|----------|
| App级 | React Context (3个) | Auth, CanvasActions, Notification |
| 编辑器级 | React Flow内部状态 | useNodesState, useEdgesState |
| 组件级 | useState (20+) | 选中节点、脏标记、保存状态、表单值 |
| 可变引用 | useRef (6+) | editVersion, autoSaveTimer, savingPromiseRef |
| 派生状态 | useMemo | displayNodes, displayEdges, liveSettings |

**已知问题**: canvas-editor 20+ useState 已达管理极限，建议迁移到 Zustand。

## Routing

```
/login                          — 匿名
/                               — Redirect → /home

[RequireAuth]
  /home, /canvas, /canvas/:id/edit, /canvas/:id/stats, /canvas/:id/users
  /cdp/users, /cdp/users/:userId

[RequireAdmin]
  /admin/users, /api-config, /data-source-config, /ab-experiments
  /tag-config, /identity-types, /tag-import, /audiences
  /mq-config, /event-config, /api-docs, /system-options

[RequireSuperAdmin]
  /admin/tenants
```

- 所有页面 React.lazy 懒加载
- Canvas editor 无侧边栏 (全宽)
- Guards: RequireAuth / RequireAdmin / RequireSuperAdmin (使用 `<Outlet />`)

## Canvas Editor Architecture

### Edge-as-Derivation Pattern

边不是图的真相来源。它们从节点的 `bizConfig` 字段派生：

- **加载时**: `deriveEdges(backendNodes)` 读取每个节点的 bizConfig，生成 React Flow Edge
- **连线时**: `patchBizConfig(cfg, sourceHandle, target)` 将连接写回源节点的 bizConfig
- **删除时**: `clearEdgeRef(cfg, edge)` 清除引用

这确保后端的节点中心存储模型始终一致。

### Auto Layout

`applyDagreLayout()` 使用 dagre top-to-bottom (48px node sep, 72px rank sep)。自动应用于历史画布（坐标全为0,0时）。

### Undo/Redo

自定义 useHistory hook，存储最多50个 (nodes, edges, actionName) 快照。Ctrl+Z / Ctrl+Shift+Z。

### Auto-Save

3秒 debounce。使用 latestSaveSnapshotRef 避免闭包过期。while 循环在保存期间如有新变更会重新保存。409 Conflict 弹窗提供页面重载选项。

### Local Draft

localStorage 持久化。页面加载时检测本地草稿与服务器差异，弹窗确认是否恢复。

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Ctrl+Z | Undo |
| Ctrl+Shift+Z / Ctrl+Y | Redo |
| Ctrl+S | Save |
| Ctrl+A | Select all (except START) |
| Ctrl+C/V | Copy/paste with offset + ID regeneration |

## API Integration

### Axios Client (services/api.ts)

- `baseURL: '/'` — 依赖 Vite proxy
- Request interceptor: localStorage `canvas_token` → `Authorization: Bearer <token>`
- Response interceptor: 解包 `R<T>` → 401 清除 localStorage + redirect /login

### Vite Proxy (vite.config.ts)

| Path | Target | 特殊处理 |
|------|--------|----------|
| /auth | localhost:8080 | - |
| /admin | localhost:8080 | - |
| /meta | localhost:8080 | - |
| /v3 | localhost:8080 | OpenAPI spec |
| /canvas | localhost:8080 | **SPA bypass**: text/html → index.html |

## Testing

| 指标 | 数值 |
|------|------|
| 测试文件 | 30 |
| 覆盖类型 | 仅纯函数 |
| 组件测试 | **0** |
| E2E 测试 | **0** |
| ErrorBoundary | **0** |
| 框架 | Vitest (env=node) |

## Known Issues

1. **canvas-editor 2085行** — 需拆分为 hooks + 子组件
2. **config-panel 1414行** — 需拆分为独立子组件
3. **零 ErrorBoundary** — 渲染错误白屏崩溃
4. **零组件测试** — 最关键组件无直接测试覆盖
5. **无 CSS Modules** — 全部 inline style={{}}
6. **无响应式设计** — 固定布局
7. **无外部状态管理** — useState 管理已达极限