# 架构整改方案 — frontend
> 详见 [README.md](./README.md) 获取完整索引


## 第三部分：前端架构问题（23项）

> 以下问题来自对前端代码的状态管理/API层/路由/组件复用/类型安全/性能深度扫描。

---

### 问题十一：无全局状态管理

#### 现状
- 无 Redux/Zustand/Jotai，仅 3 个 React Context + 页面本地 useState
- 画布编辑器 `canvas-editor/index.tsx` 2084行，30+ useState
- 无请求缓存层（React Query / SWR），同一接口跨页面重复调用

#### 实施方案

**Step 1: 引入 Zustand（轻量状态管理）**

```bash
npm install zustand
```

```typescript
// stores/canvasStore.ts
import { create } from 'zustand';

interface CanvasStore {
  nodes: CanvasNodeData[];
  edges: Edge[];
  setNodes: (nodes: CanvasNodeData[]) => void;
  setEdges: (edges: Edge[]) => void;
  // ... 撤销/重做栈
}

export const useCanvasStore = create<CanvasStore>((set) => ({
  nodes: [],
  edges: [],
  setNodes: (nodes) => set({ nodes }),
  setEdges: (edges) => set({ edges }),
}));
```

**Step 2: 拆分画布编辑器巨型组件**

将 2084 行的 `canvas-editor/index.tsx` 拆分为：

```
canvas-editor/
├── index.tsx              (主入口，~200行)
├── useCanvasEditor.ts     (状态编排Hook)
├── useAutoSave.ts         (自动保存Hook)
├── useKeyboardShortcuts.ts(快捷键Hook)
├── CanvasToolbar.tsx      (工具栏)
├── TestModal.tsx          (测试弹窗)
├── GrayscaleModal.tsx     (灰度弹窗)
├── VersionDrawer.tsx      (版本历史)
└── SettingsModal.tsx      (设置弹窗)
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 引入 Zustand + 迁移 AuthContext | 4h |
| 2 | 创建 useCanvasStore | 4h |
| 3 | 拆分画布编辑器 | 8h |
| 4 | 引入 React Query 缓存层 | 4h |
| 5 | 全量测试 | 4h |

**总工时**: ~24h

---

### 问题十二：API 层缺陷

#### 现状
- `apiDefinitionApi`/`tagDefinitionApi` 参数和返回值全用 `any`
- 响应拦截器仅处理 401，其他错误全靠页面各自 catch
- 90+ 处重复的 `loading/setLoading + fetchList` 模式

#### 实施方案

**Step 1: API 类型化**

```typescript
// services/types.ts
export interface ApiDefinition {
  id: number;
  name: string;
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  headers?: Record<string, string>;
  bodyTemplate?: string;
}

export interface CreateApiDefinitionRequest {
  name: string;
  url: string;
  method: string;
}

// services/api.ts — 替换 any
export const apiDefinitionApi = {
  list: () => http.get<R<ApiDefinition[]>>('/canvas/api-definitions'),
  create: (data: CreateApiDefinitionRequest) => http.post<R<ApiDefinition>>('/canvas/api-definitions', data),
  // ...
};
```

**Step 2: 统一错误处理**

```typescript
// services/api.ts
http.interceptors.response.use(
  (res) => res.data,
  (error) => {
    if (error.response?.status === 401) {
      // 跳转登录
    } else if (error.response?.status === 403) {
      message.error('无权限访问');
    } else if (error.response?.status >= 500) {
      message.error('服务器错误，请稍后重试');
    } else {
      message.error(error.response?.data?.msg || '请求失败');
    }
    return Promise.reject(error);
  }
);
```

**Step 3: 通用 CRUD Hook**

```typescript
// hooks/useCrudPage.ts
export function useCrudPage<T>(api: { list: () => Promise<R<T[]>>; delete: (id: number) => Promise<R<void>> }) {
  const [data, setData] = useState<T[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.list();
      setData(res.data);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }, [api]);

  return { data, loading, error, fetchList };
}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | API 类型定义 | 8h |
| 2 | 统一错误拦截器 | 2h |
| 3 | useCrudPage Hook | 4h |
| 4 | 迁移 90+ 处重复模式 | 12h |
| 5 | 全量测试 | 4h |

**总工时**: ~30h

---

### 问题十三：路由与权限守卫缺陷

#### 现状
- 无 404 兜底路由
- 权限不足仅渲染纯文本 `<div>无权限</div>`
- 无路由级 ErrorBoundary
- 菜单高亮硬编码 if-else

#### 实施方案

**Step 1: 404 + 403 页面**

```tsx
// App.tsx
<Routes>
  {/* ... 现有路由 ... */}
  <Route path="/403" element={<ForbiddenPage />} />
  <Route path="/404" element={<NotFoundPage />} />
  <Route path="*" element={<Navigate to="/404" replace />} />
</Routes>
```

**Step 2: 路由级 ErrorBoundary**

```tsx
// components/RouteErrorBoundary.tsx
class RouteErrorBoundary extends React.Component {
  state = { hasError: false };
  static getDerivedStateFromError() { return { hasError: true }; }
  render() {
    if (this.state.hasError) return <ErrorFallback onRetry={() => this.setState({ hasError: false })} />;
    return this.props.children;
  }
}
```

**Step 3: 菜单高亮配置化**

```tsx
// 替代硬编码 if-else
const menuHighlightMap: Record<string, string> = {
  '/canvas-list': 'canvas',
  '/canvas-editor': 'canvas',
  '/audience': 'audience',
  '/cdp-users': 'cdp',
  // ...
};
const activeMenu = menuHighlightMap[location.pathname] ?? '';
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 404/403 页面 | 2h |
| 2 | 路由级 ErrorBoundary | 2h |
| 3 | 菜单高亮配置化 | 1h |
| 4 | 全量测试 | 1h |

**总工时**: ~6h

---

### 问题十四：TypeScript any 泛滥

#### 现状
- `config-panel/index.tsx` 17 处 `any`
- `audience-edit/index.tsx` 规则序列化全 `any`
- `canvas-editor/index.tsx` 9 处 `as any`
- `cdpApi.ts` 返回 `R<any[]>`

#### 实施方案

**策略**: 优先修复 API 层类型（影响面最大），再逐页面修复

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | API 层类型定义（同问题十二） | 含在问题十二中 |
| 2 | config-panel 类型化 | 4h |
| 3 | audience-edit 规则引擎类型 | 4h |
| 4 | CanvasNodeData 与 React Flow Node 类型对齐 | 4h |
| 5 | 全量测试 | 2h |

**总工时**: ~14h

---

### 问题十五：前端性能问题

#### 现状
- 画布编辑器 useEffect 无依赖数组，每次渲染重设 timer/事件监听
- WebSocket 重连无最大次数限制
- 无虚拟滚动

#### 实施方案

```tsx
// Before: 无依赖数组
useEffect(() => {
  if (isDirty) {
    const timer = setTimeout(() => save(), 3000);
    return () => clearTimeout(timer);
  }
}); // ← 缺少 deps

// After: 正确依赖
useEffect(() => {
  if (!isDirty) return;
  const timer = setTimeout(() => save(), 3000);
  return () => clearTimeout(timer);
}, [isDirty, save]);
```

```tsx
// WebSocket 最大重连次数
const MAX_RECONNECT_ATTEMPTS = 10;
if (reconnectAttemptRef.current >= MAX_RECONNECT_ATTEMPTS) {
  console.warn('[WS] Max reconnect attempts reached');
  return;
}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 修复 useEffect 依赖数组 | 2h |
| 2 | WebSocket 重连限制 | 1h |
| 3 | 列表虚拟滚动（如需要） | 4h |
| 4 | 性能测试 | 2h |

**总工时**: ~9h

