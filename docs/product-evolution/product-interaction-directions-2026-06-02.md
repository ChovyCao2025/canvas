# 产品交互设计演进方向（23-29）（2026-06-02）

> 方向23-29：前端性能优化/无障碍与包容性/动效与微交互/错误处理与韧性/表单与数据录入/表格与数据展示/数据可视化与报表
> 原则：**全做优先 → 配置项 → 脑暴选最优**
> 基于源码级扫描，70项评估（26已有/18部分/26缺失），每项缺项有具体代码位置和文件证据

---

## 总览

| # | 演进方向 | 已有 | 部分 | 缺失 | 配置项数 | 阶段 |
|---|----------|------|------|------|----------|------|
| 23 | 前端性能优化 | 3 | 2 | 5 | 10 | 1 |
| 24 | 无障碍与包容性 | 2 | 3 | 5 | 10 | 0 |
| 25 | 动效与微交互 | 1 | 5 | 4 | 10 | 1 |
| 26 | 错误处理与韧性 | 3 | 4 | 3 | 10 | 0 |
| 27 | 表单与数据录入 | 5 | 3 | 2 | 10 | 1-2 |
| 28 | 表格与数据展示 | 5 | 0 | 5 | 10 | 1 |
| 29 | 数据可视化与报表 | 7 | 1 | 2 | 10 | 2 |

**总计：70项评估（26已有/18部分/26缺失），70个配置项**

---

## 方向23：前端性能优化

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 代码分割 | `App.tsx:14-58`，23个React.lazy()路由级懒加载，`<Suspense fallback={<RouteFallback />}>` | ✅ 完整 |
| useCallback/useMemo | `canvas-editor/index.tsx`，~25+ useCallback + ~5 useMemo，autoSave定时器正确清理 | ✅ 完整 |
| React.memo | `CanvasNode.tsx:76`，`React.memo()` with `displayName = 'CanvasNode'` | ✅ 完整 |
| 防抖/节流 | autoSave 3秒防抖（`index.tsx:500-507`） | ⚠️ 搜索/滚动/表单输入无节流 |
| 图片优化 | antd Avatar懒加载 | ⚠️ 无图片压缩/WebP/lazy loading |
| Bundle优化 | `vite.config.ts:1-29`，仅react()插件+dev proxy | ❌ 无build.rollupOptions/manualChunks |
| 虚拟滚动 | — | ❌ 全局0使用，长列表全量渲染 |
| 乐观更新 | — | ❌ 所有API操作请求→等待→刷新模式 |
| 数据预取 | — | ❌ 无react-query/SWR/prefetching |
| 请求缓存 | `api.ts:20-44`，单一axios实例 | ❌ 无请求去重/缓存/重试 |

### 解决方案（全做）

#### 23.1 Bundle优化（5配置项全做）

| 配置项 | 说明 | 配置键 |
|--------|------|--------|
| **manualChunks** | vendor/react-flow/antd/recharts 独立chunk | `build.manualChunks` |
| **chunkSizeWarningLimit** | 500KB警告阈值 | `build.chunkSizeWarningLimit` |
| **terserOptions** | 生产环境移除console/debugger | `build.terserOptions` |
| **build.target** | es2020+ 减少polyfill | `build.target` |
| **build.cssCodeSplit** | CSS代码分割 | `build.cssCodeSplit` |

**技术方案**：
- 修改 `vite.config.ts`，新增 `build.rollupOptions.output.manualChunks`：
  ```js
  manualChunks: {
    'vendor-react': ['react', 'react-dom', 'react-router-dom'],
    'vendor-flow': ['@xyflow/react'],
    'vendor-antd': ['antd', '@ant-design/icons'],
    'vendor-charts': ['recharts'],
    'vendor-query': ['react-querybuilder'],
  }
  ```
- 新增 `build.chunkSizeWarningLimit: 500`（500KB）
- 新增 `build.minify: 'terser'`，`terserOptions.compress.drop_console: true`
- 新增 `build.target: 'es2020'`
- 配置项：`build.analyze.enabled`（默认false，开启rollup-plugin-visualizer）

#### 23.2 虚拟滚动（3配置项全做）

| 场景 | 当前状态 | 解决方案 |
|------|----------|----------|
| **画布列表** | 全量渲染 | react-window FixedSizeList |
| **标签列表** | 全量渲染 | react-window VariableSizeList |
| **用户/受众列表** | 全量渲染 | antd Table 内置虚拟滚动（5.15+） |

**技术方案**：
- 安装 `react-window`（轻量，2KB gzipped）
- 新增 `VirtualList` 通用组件 — 封装 react-window，支持动态高度
- 画布列表：替换现有 `.map()` 渲染 → `<VirtualList itemCount={...} itemSize={72}>`
- 标签/用户列表：antd Table 5.15+ 已内置 `virtual` 属性，直接启用 `<Table virtual />`
- 配置项：`virtual-scroll.threshold`（默认100，超过100条启用虚拟滚动）、`virtual-scroll.overscan`（默认5）

#### 23.3 乐观更新（3场景全做）

| 场景 | 操作 | 乐观行为 |
|------|------|----------|
| **画布删除** | DELETE /canvas/{id} | 列表即刻移除，失败恢复 |
| **画布启停** | PUT /canvas/{id}/status | 开关即刻切换，失败回滚 |
| **节点配置保存** | PUT /canvas/{id}/nodes | 面板即刻关闭，后台保存 |

**技术方案**：
- 新增 `useOptimisticMutation` hook — 封装乐观更新模式
  ```ts
  const { execute, isRolledBack } = useOptimisticMutation({
    mutation: (id) => api.deleteCanvas(id),
    optimisticUpdate: (list, id) => list.filter(c => c.id !== id),
    rollback: (previousList) => setList(previousList),
  });
  ```
- 画布删除：列表项淡出动画（300ms）→ 实际删除API → 失败时淡入恢复+toast提示
- 画布启停：Switch onChange 即刻响应 → API调用 → 失败回滚Switch
- 配置项：`optimistic.enabled`（默认true）、`optimistic.rollback-toast-duration`（默认5s）

#### 23.4 数据预取与请求缓存（3配置项全做）

| 能力 | 当前状态 | 解决方案 |
|------|----------|----------|
| **数据预取** | 无 | react-query prefetchQuery |
| **请求去重** | 无 | react-query 内置请求去重 |
| **请求重试** | 无 | react-query retry（指数退避3次） |

**技术方案**：
- 安装 `@tanstack/react-query`（react-query v5）
- 新增 `QueryProvider` — 全局 QueryClientProvider 包裹
  ```ts
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,       // 30秒内不重新请求
        gcTime: 5 * 60_000,      // 5分钟垃圾回收
        retry: 3,                 // 失败重试3次
        retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 30000),
        refetchOnWindowFocus: false,
      },
    },
  });
  ```
- 首页预取：`usePrefetchQuery` 在画布列表hover时预加载详情
- 画布编辑器预取：进入编辑器前预加载节点配置schema
- 配置项：`query.stale-time`（默认30s）、`query.retry`（默认3）、`query.prefetch.enabled`（默认true）

#### 23.5 防抖/节流系统化（3场景全做）

| 场景 | 策略 | 延迟 |
|------|------|------|
| **搜索输入** | 防抖（debounce） | 300ms |
| **滚动事件** | 节流（throttle） | 100ms |
| **表单输入** | 防抖（debounce） | 200ms |

**技术方案**：
- 新增 `useDebounce` hook — `import { useDebounce } from '@/hooks/useDebounce'`
- 新增 `useThrottle` hook — `import { useThrottle } from '@/hooks/useThrottle'`
- 搜索框：`const debouncedSearch = useDebounce(searchTerm, 300)`
- 画布列表滚动：`const throttledScroll = useThrottle(scrollHandler, 100)`
- 配置项：`debounce.search-ms`（默认300）、`throttle.scroll-ms`（默认100）、`debounce.form-ms`（默认200）

---

## 方向24：无障碍与包容性

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| ARIA属性片段 | `HoverEdge.tsx:105`，`aria-label`；`NotificationBell.tsx:54,71,88,135,230`，aria-hidden+aria-label | ✅ 片段存在 |
| 语义化展开状态 | `canvas-editor/index.tsx:2001-2002`，`aria-expanded`+`aria-controls` | ✅ 存在 |
| 表单标签 | antd Form.Item 自带label htmlFor绑定 | ⚠️ 自定义组件缺少关联 |
| 键盘导航 | antd Modal/Dropdown 自带键盘支持 | ⚠️ 画布编辑器无Tab键导航 |
| 焦点管理 | antd Modal 打开时自动聚焦 | ⚠️ 路由切换/面板展开无焦点管理 |
| 屏幕阅读器模式 | — | ❌ 全局0个 `.sr-only`/`visually-hidden` |
| 跳过导航链接 | — | ❌ 无 skip-to-content |
| 语义化HTML | `AppLayout.tsx`，无header/main/nav/aside/footer | ❌ 完全依赖antd Layout |
| 表单错误关联 | — | ❌ 无 aria-describedby |
| 实时区域 | — | ❌ 无 aria-live/aria-atomic/role="alert" |

### 解决方案（全做）

#### 24.1 语义化HTML结构（5标签全做）

**技术方案**：
- 修改 `AppLayout.tsx` — 将 antd Layout 包裹在语义化标签内：
  ```tsx
  <div className="app-root">
    <header>
      <a href="#main-content" className="sr-only">跳过导航</a>
      {/* 顶部导航栏 */}
    </header>
    <nav aria-label="主导航">
      <Sider /> {/* 侧边栏 */}
    </nav>
    <main id="main-content" tabIndex={-1}>
      <Outlet />
    </main>
    <footer>
      {/* 页脚信息 */}
    </footer>
  </div>
  ```
- 配置项：`a11y.semantic-html.enabled`（默认true）、`a11y.skip-link.enabled`（默认true）

#### 24.2 屏幕阅读器工具类（4类全做）

**技术方案**：
- 新增 `src/styles/a11y.css` — 全局无障碍工具类：
  ```css
  .sr-only {
    position: absolute; width: 1px; height: 1px;
    padding: 0; margin: -1px; overflow: hidden;
    clip: rect(0,0,0,0); white-space: nowrap; border: 0;
  }
  .sr-only-focusable:focus {
    position: static; width: auto; height: auto;
    clip: auto; white-space: normal;
  }
  .visually-hidden { /* 同上，别名 */ }
  ```
- 在 `main.tsx` 中全局引入 `import '@/styles/a11y.css'`
- 配置项：`a11y.sr-only.enabled`（默认true）

#### 24.3 焦点管理（3场景全做）

| 场景 | 焦点行为 |
|------|----------|
| **路由切换** | `main#main-content` 接收焦点（`tabIndex={-1}` + `focus()`） |
| **面板展开** | 面板第一个可聚焦元素接收焦点 |
| **Modal关闭** | 焦点回到触发Modal的按钮 |

**技术方案**：
- 新增 `useFocusManagement` hook — 路由切换后自动聚焦main区域
  ```ts
  const location = useLocation();
  useEffect(() => {
    document.getElementById('main-content')?.focus();
  }, [location.pathname]);
  ```
- 面板展开：使用 `useRef` + `useEffect` 在面板展开后聚焦第一个输入框
- 配置项：`a11y.focus.route-change`（默认true）、`a11y.focus.panel-open`（默认true）

#### 24.4 表单错误关联（aria-describedby全做）

**技术方案**：
- 修改所有 `Form.Item` — 自动生成 `aria-describedby` 关联错误消息：
  ```tsx
  <Form.Item
    name="canvasName"
    rules={[{ required: true }]}
    aria-describedby="canvasName-error"
  >
    <Input aria-invalid={hasError} />
  </Form.Item>
  // antd Form.Item 自动渲染错误div，需为其添加id
  ```
- 新增 `useFormErrorA11y` hook — 自动为表单字段注入 `aria-describedby` 和 `aria-invalid`
- 配置项：`a11y.form.aria-describedby`（默认true）

#### 24.5 实时区域通知（3类型全做）

| 通知类型 | aria-live | role |
|----------|-----------|------|
| **操作成功/失败** | `polite` | `status` |
| **列表更新** | `polite` | `status` |
| **紧急错误** | `assertive` | `alert` |

**技术方案**：
- 新增 `LiveRegion` 组件 — 固定在页面中，视觉隐藏但屏幕阅读器可读：
  ```tsx
  <div id="live-region" aria-live="polite" aria-atomic="true" className="sr-only" />
  ```
- 新增 `useAnnounce` hook — `announce('画布保存成功')` → 写入 live-region
- 全局错误拦截器：401/500/网络错误自动 announce
- 配置项：`a11y.live-region.enabled`（默认true）

---

## 方向25：动效与微交互

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| hover效果 | `CanvasNode.tsx:20-29,138`，useHover（300ms）；`settingsPanel.css:80`，transition | ✅ 存在 |
| 加载状态 | antd Spin + Skeleton 可用 | ⚠️ 多页面仅用Spin |
| 过渡动画 | `NodeLibraryItem.tsx:51`，inline transition | ⚠️ 散落的inline style |
| 按钮反馈 | antd Button 内置涟漪 | ⚠️ 自定义组件缺反馈 |
| 展开/折叠 | CSS transition存在 | ⚠️ 无弹性/缓出 |
| 通知动画 | notification toast 默认入场 | ⚠️ 无自定义 |
| @keyframes动画 | — | ❌ 全局src/ 0个@keyframes |
| 页面过渡 | — | ❌ 路由切换无过渡动画 |
| 骨架屏 | — | ❌ 全局为0，所有加载用Spin |
| 数字动画 | — | ❌ KPI数字直接显示 |
| 注意力引导 | — | ❌ 无pulse/shake/glow |
| 动画库 | — | ❌ framer-motion未安装 |

### 解决方案（全做）

#### 25.1 CSS动画基础库（6动画全做）

| 动画 | 用途 | 实现 |
|------|------|------|
| **fade-in** | 元素入场 | `@keyframes fadeIn { from { opacity: 0; } }` |
| **slide-up** | 面板/弹窗入场 | `@keyframes slideUp { from { opacity:0; transform:translateY(8px); } }` |
| **pulse** | 注意力引导 | `@keyframes pulse { 0%,100% { opacity:1; } 50% { opacity:0.6; } }` |
| **shake** | 错误反馈 | `@keyframes shake { 0%,100% { transform:translateX(0); } 25% { transform:translateX(-4px); } 75% { transform:translateX(4px); } }` |
| **spin-slow** | 加载指示 | `@keyframes spinSlow { from { transform:rotate(0deg); } to { transform:rotate(360deg); } }` |
| **scale-in** | 弹窗/工具提示 | `@keyframes scaleIn { from { opacity:0; transform:scale(0.95); } }` |

**技术方案**：
- 新增 `src/styles/animations.css` — 集中管理所有 @keyframes 和动画工具类
  ```css
  @keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
  @keyframes slideUp { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
  @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }
  @keyframes shake { 0%, 100% { transform: translateX(0); } 25% { transform: translateX(-4px); } 75% { transform: translateX(4px); } }
  @keyframes scaleIn { from { opacity: 0; transform: scale(0.95); } to { opacity: 1; transform: scale(1); } }
  @keyframes countUp { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

  .animate-fade-in { animation: fadeIn 0.2s ease; }
  .animate-slide-up { animation: slideUp 0.25s ease; }
  .animate-pulse { animation: pulse 2s ease-in-out infinite; }
  .animate-shake { animation: shake 0.3s ease; }
  .animate-scale-in { animation: scaleIn 0.15s ease; }
  ```
- 在 `main.tsx` 中全局引入
- 配置项：`animation.default-duration`（默认200ms）、`animation.easing`（默认ease）

#### 25.2 页面过渡动画（路由级全做）

**技术方案**：
- 安装 `framer-motion`（动画库，React生态标准）
- 新增 `PageTransition` 组件 — 包裹每个路由页面：
  ```tsx
  import { motion, AnimatePresence } from 'framer-motion';

  const pageVariants = {
    initial: { opacity: 0, y: 8 },
    animate: { opacity: 1, y: 0, transition: { duration: 0.2 } },
    exit: { opacity: 0, y: -8, transition: { duration: 0.15 } },
  };

  <AnimatePresence mode="wait">
    <motion.div key={location.pathname} variants={pageVariants}
      initial="initial" animate="animate" exit="exit">
      <Outlet />
    </motion.div>
  </AnimatePresence>
  ```
- 配置项：`animation.page-transition.enabled`（默认true）、`animation.page-transition.duration`（默认200ms）

#### 25.3 骨架屏系统（5页面全做）

| 页面 | 骨架屏组件 | 骨架结构 |
|------|-----------|----------|
| **画布列表** | CanvasListSkeleton | 3行卡片骨架（标题+描述+时间） |
| **标签列表** | TagListSkeleton | 5行表格骨架 |
| **画布统计** | StatsSkeleton | 4个KPI卡片 + 图表区域骨架 |
| **系统设置** | SettingsSkeleton | 表单骨架（标签+输入框） |
| **模板列表** | TemplateSkeleton | 2列卡片骨架 |

**技术方案**：
- 新增 `src/components/skeletons/` 目录，每页面一个骨架屏组件
- 使用 antd `Skeleton` 组件组合（`Skeleton.Input`、`Skeleton.Button`、`Skeleton.Avatar`）
- 配合 CSS `shimmer` 动画（`@keyframes shimmer` 线性渐变移动）
- 各页面 Suspense fallback 替换为对应骨架屏：
  ```tsx
  const CanvasListPage = React.lazy(() => import('./pages/canvas-list'));
  // Route fallback:
  <Suspense fallback={<CanvasListSkeleton />}>
  ```
- 配置项：`skeleton.enabled`（默认true）、`skeleton.shimmer.enabled`（默认true）

#### 25.4 数字滚动动画（KPI全做）

**技术方案**：
- 新增 `AnimatedNumber` 组件 — 数字从0递增到目标值：
  ```tsx
  import { motion, useSpring, useTransform } from 'framer-motion';

  const AnimatedNumber = ({ value, duration = 1.5 }) => {
    const spring = useSpring(0, { duration: duration * 1000 });
    useEffect(() => { spring.set(value); }, [value]);
    const display = useTransform(spring, (v) => Math.floor(v).toLocaleString());
    return <motion.span>{display}</motion.span>;
  };
  ```
- 替换 `home/index.tsx:166-208` 中KPI卡片数字 → `<AnimatedNumber value={kpiValue} />`
- 替换 `canvas-stats/index.tsx:109-115` 中统计数字 → `<AnimatedNumber value={statValue} />`
- 配置项：`animation.count-up.enabled`（默认true）、`animation.count-up.duration`（默认1.5s）

#### 25.5 注意力引导动画（3场景全做）

| 场景 | 动画 | 触发条件 |
|------|------|----------|
| **新功能提示** | pulse + badge | 首次访问某页面 |
| **表单校验失败** | shake | 提交时校验失败 |
| **关键操作确认** | scale-in + glow | 删除/发布等危险操作弹窗 |

**技术方案**：
- 新增 `useAttentionAnimation` hook — 统一管理引导动画触发
- 表单校验失败：`<Form.Item className={hasError ? 'animate-shake' : ''}>`
- 新功能badge：`<Badge dot className="animate-pulse">新功能</Badge>`
- 危险操作Modal：`<Modal wrapClassName="animate-scale-in">`
- 配置项：`animation.attention.enabled`（默认true）

---

## 方向26：错误处理与韧性

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 401拦截 | `api.ts:35-43`，axios响应拦截器，清除token/user→重定向/login | ✅ 完整 |
| WebSocket重连 | `NotificationContext.tsx:152`，指数退避重连 | ✅ 完整 |
| 安全渲染 | `CanvasNode.tsx:153,161`，`{d.name \|\| '未命名'}` 等null安全 | ✅ 存在 |
| 表单验证 | antd Form rules实时校验 | ⚠️ 服务端错误无字段级映射 |
| 加载/空/错误三态 | 部分页面有 | ⚠️ 不一致 |
| toast通知 | message.error() 用于API错误 | ⚠️ 未分类（网络/业务/权限） |
| 提交防重 | `api-config/index.tsx:208,367`，submitting+confirmLoading | ⚠️ 仅部分页面 |
| ErrorBoundary | — | ❌ **CRITICAL** 全局0个，React异常=白屏 |
| 请求超时/取消 | — | ❌ 0个AbortController/axios timeout |
| 离线检测 | — | ❌ 0个 navigator.onLine |
| 404页面 | — | ❌ 无全局通配路由 |

### 解决方案（全做）

#### 26.1 ErrorBoundary（3层全做，CRITICAL）

| 层级 | 组件 | 范围 | 降级UI |
|------|------|------|--------|
| **全局** | AppErrorBoundary | 整个App | 全屏错误页+"刷新页面"按钮 |
| **路由级** | RouteErrorBoundary | 每个路由页面 | 页面内错误卡片+"重试"按钮 |
| **组件级** | WidgetErrorBoundary | 独立Widget（KPI卡片/图表） | 内联错误提示，不影响父组件 |

**技术方案**：
- 新增 `src/components/AppErrorBoundary.tsx` — 全局错误边界：
  ```tsx
  class AppErrorBoundary extends React.Component<Props, State> {
    state = { hasError: false, error: null };

    static getDerivedStateFromError(error: Error) {
      return { hasError: true, error };
    }

    componentDidCatch(error: Error, info: React.ErrorInfo) {
      console.error('[AppErrorBoundary]', error, info.componentStack);
      // 上报错误到服务端
      api.reportError({ message: error.message, stack: error.stack, componentStack: info.componentStack });
    }

    render() {
      if (this.state.hasError) {
        return <ErrorFallback error={this.state.error} onRetry={() => this.setState({ hasError: false })} />;
      }
      return this.props.children;
    }
  }
  ```
- 新增 `ErrorFallback` 组件 — 友好的错误展示页面（错误图标+描述+刷新/重试按钮+错误详情展开）
- `main.tsx` 包裹：`<AppErrorBoundary><App /></AppErrorBoundary>`
- 各路由页面包裹：`<RouteErrorBoundary><PageComponent /></RouteErrorBoundary>`
- 配置项：`error-boundary.global.enabled`（默认true）、`error-boundary.route.enabled`（默认true）、`error-boundary.report-error.enabled`（默认true）

#### 26.2 404页面与路由兜底

**技术方案**：
- 新增 `src/pages/NotFound.tsx` — 404页面设计：
  ```tsx
  const NotFound = () => (
    <Result
      status="404"
      title="页面未找到"
      subTitle="您访问的页面不存在或已被移除"
      extra={[
        <Button type="primary" onClick={() => navigate('/')}>返回首页</Button>,
        <Button onClick={() => navigate(-1)}>返回上一页</Button>,
      ]}
    />
  );
  ```
- `App.tsx` 新增通配路由：
  ```tsx
  <Route path="*" element={<NotFound />} />
  ```
- 配置项：`routing.404.enabled`（默认true）

#### 26.3 请求超时/取消（3机制全做）

| 机制 | 实现 |
|------|------|
| **axios timeout** | 全局默认30s超时 |
| **AbortController** | 组件卸载时取消未完成请求 |
| **请求去重** | 相同请求去重（react-query内置） |

**技术方案**：
- 修改 `api.ts:20-44`，axios实例添加超时：
  ```ts
  const api = axios.create({
    baseURL: '/',
    timeout: 30_000,  // 30秒超时
  });
  ```
- 新增 `useAbortController` hook — 组件卸载时自动 abort：
  ```ts
  const useAbortController = () => {
    const controllerRef = useRef<AbortController>();
    useEffect(() => {
      return () => controllerRef.current?.abort();
    }, []);
    const getSignal = () => {
      controllerRef.current = new AbortController();
      return controllerRef.current.signal;
    };
    return getSignal;
  };
  ```
- 全局axios拦截器：aborted 请求不弹出错误提示
- 配置项：`api.timeout-ms`（默认30000）、`api.abort-on-unmount`（默认true）

#### 26.4 离线检测

**技术方案**：
- 新增 `useOnlineStatus` hook：
  ```ts
  const useOnlineStatus = () => {
    const [isOnline, setIsOnline] = useState(navigator.onLine);
    useEffect(() => {
      const goOnline = () => setIsOnline(true);
      const goOffline = () => setIsOnline(false);
      window.addEventListener('online', goOnline);
      window.addEventListener('offline', goOffline);
      return () => {
        window.removeEventListener('online', goOnline);
        window.removeEventListener('offline', goOffline);
      };
    }, []);
    return isOnline;
  };
  ```
- 离线提示条：页面顶部固定banner"当前处于离线状态，部分功能不可用"
- 画布编辑时离线：切换为localStorage草稿模式，在线后提示同步
- 配置项：`offline.detection.enabled`（默认true）、`offline.banner.enabled`（默认true）

#### 26.5 错误通知分类（3类型全做）

| 错误类型 | 图标 | 颜色 | 行为 |
|----------|------|------|------|
| **网络错误** | WifiOutlined | orange | toast + 重试按钮 |
| **业务错误** | ExclamationCircleOutlined | red | toast + 错误详情 |
| **权限错误** | LockOutlined | red | toast + 跳转403 |

**技术方案**：
- 新增 `useErrorHandler` hook — 统一错误分类和提示：
  ```ts
  const handleError = (error: ApiError) => {
    if (!error.response) {
      message.error({ content: '网络连接失败，请检查网络', icon: <WifiOutlined />, duration: 5 });
    } else if (error.response.status === 403) {
      message.error({ content: '无权限访问，请联系管理员', icon: <LockOutlined /> });
      navigate('/403');
    } else if (error.response.status >= 500) {
      message.error({ content: `服务器错误（${error.response.status}），请稍后重试` });
    } else {
      message.error({ content: error.response.data?.message || '操作失败' });
    }
  };
  ```
- 全局axios响应拦截器集成错误分类
- 配置项：`error-handler.classify.enabled`（默认true）、`error-handler.retry.enabled`（默认true）

---

## 方向27：表单与数据录入

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 条件字段 | `config-panel/index.tsx:330-331`，`f.showWhen` 条件渲染 | ✅ 完整 |
| 字段帮助 | `config-panel/index.tsx:1391-1393`，QuestionCircleOutlined tooltip | ✅ 完整 |
| 代码编辑器 | `config-panel/index.tsx:520-522`，Input.TextArea rows={8} | ✅ 完整 |
| 草稿保存 | `localDraft.ts:74`，专用draft模块 | ✅ 完整 |
| 提交防重 | `api-config/index.tsx:208,367`，submitting+confirmLoading | ✅ 存在 |
| 表单布局 | `config-panel/index.tsx:428`，layout="vertical" | ⚠️ 无分组/分栏 |
| 实时校验 | antd Form rules存在 | ⚠️ 无异步校验（名称唯一性） |
| 自动保存 | 画布级有 | ⚠️ 表单级无 |
| 多步骤/向导表单 | — | ❌ 全局0个Steps+Form |
| 离开确认 | — | ❌ 0个useBeforeUnload/useBlocker |
| 高级输入组件 | — | ❌ 无标签输入器/JSON编辑器/富文本 |

### 解决方案（全做）

#### 27.1 多步骤/向导表单

**技术方案**：
- 新增 `StepForm` 通用组件 — 封装 antd Steps + Form：
  ```tsx
  interface StepFormProps {
    steps: { title: string; description?: string; fields: FormField[] }[];
    onFinish: (values: any) => Promise<void>;
    draftKey?: string;  // localStorage草稿键
  }
  ```
- 应用场景：
  - 创建画布流程：步骤1（基本信息）→ 步骤2（选择模板）→ 步骤3（配置触发器）
  - API配置流程：步骤1（连接信息）→ 步骤2（映射配置）→ 步骤3（测试验证）
- 步骤间自动保存草稿到 localStorage
- 配置项：`step-form.enabled`（默认true）、`step-form.draft.enabled`（默认true）

#### 27.2 表单离开确认（2机制全做）

| 机制 | 触发场景 | 实现 |
|------|----------|------|
| **useBeforeUnload** | 关闭/刷新标签页 | 浏览器原生beforeunload事件 |
| **useBlocker** | 路由切换 | react-router v6 useBlocker |

**技术方案**：
- 新增 `useFormDirtyGuard` hook：
  ```ts
  const useFormDirtyGuard = (isDirty: boolean) => {
    // 浏览器关闭/刷新拦截
    useEffect(() => {
      if (!isDirty) return;
      const handler = (e: BeforeUnloadEvent) => { e.preventDefault(); e.returnValue = ''; };
      window.addEventListener('beforeunload', handler);
      return () => window.removeEventListener('beforeunload', handler);
    }, [isDirty]);

    // 路由切换拦截
    const blocker = useBlocker(isDirty);
    useEffect(() => {
      if (blocker.state === 'blocked') {
        Modal.confirm({
          title: '表单未保存',
          content: '当前表单有未保存的更改，确定离开吗？',
          onOk: () => blocker.proceed(),
          onCancel: () => blocker.reset(),
        });
      }
    }, [blocker.state]);
  };
  ```
- 所有表单页面集成：`useFormDirtyGuard(form.isFieldsTouched())`
- 配置项：`form.guard.beforeunload.enabled`（默认true）、`form.guard.route-change.enabled`（默认true）

#### 27.3 高级输入组件（4组件全做）

| 组件 | 用途 | 实现 |
|------|------|------|
| **TagInput** | 标签/关键词输入 | antd Select mode="tags" |
| **JsonEditor** | JSON配置编辑 | @monaco-editor/react（代码高亮+校验） |
| **DateRangeShortcut** | 日期范围快捷选择 | antd DatePicker.RangePicker + presets |
| **RichTextEditor** | 富文本编辑（模板/消息内容） | @tiptap/react（轻量） |

**技术方案**：
- 新增 `src/components/form/` 目录，集中管理高级输入组件
- `TagInput`：封装 antd Select `mode="tags"` + 最大标签数限制 + 去重
- `JsonEditor`：Monaco Editor 封装，JSON schema校验，格式化按钮，错误行高亮
  ```tsx
  import Editor from '@monaco-editor/react';
  <Editor height="300px" language="json" theme="vs-dark"
    options={{ minimap: { enabled: false }, lineNumbers: 'on' }} />
  ```
- `DateRangeShortcut`：预设 `今天/本周/本月/最近7天/最近30天`
- `RichTextEditor`：基于 Tiptap，支持加粗/斜体/链接/图片/变量插入 `{{变量}}`
- 配置项：`form.json-editor.enabled`（默认true）、`form.rich-text.enabled`（默认false，按需安装）

#### 27.4 异步校验（名称唯一性）

**技术方案**：
- 新增 `useAsyncValidation` hook — 封装antd Form异步校验规则：
  ```ts
  const checkNameUnique = async (_, value: string) => {
    if (!value) return;
    const { exists } = await api.checkCanvasNameUnique(value, currentId);
    if (exists) throw new Error('名称已存在，请使用其他名称');
  };

  <Form.Item name="name" rules={[{ required: true }, { validator: checkNameUnique }]}>
  ```
- 添加debounce（300ms）防止频繁请求
- 配置项：`form.async-validation.enabled`（默认true）、`form.async-validation.debounce-ms`（默认300）

#### 27.5 表单自动保存（表单级）

**技术方案**：
- 新增 `useFormAutoSave` hook — 表单级自动保存（画布编辑器已有此模式）：
  ```ts
  const useFormAutoSave = (form, draftKey: string, enabled: boolean) => {
    const isDirty = useRef(false);

    const save = useCallback(() => {
      if (!isDirty.current) return;
      const values = form.getFieldsValue();
      localDraft.save(draftKey, values);
      isDirty.current = false;
    }, [form, draftKey]);

    // 3秒防抖自动保存
    useEffect(() => {
      if (!enabled) return;
      const timer = setInterval(save, 3000);
      return () => { clearInterval(timer); save(); };
    }, [save, enabled]);
  };
  ```
- 应用场景：API配置表单、系统设置表单、模板编辑表单
- 页面加载时检查草稿：`const draft = localDraft.read(draftKey); if (draft) { showRestorePrompt(); }`
- 配置项：`form.auto-save.enabled`（默认true）、`form.auto-save.interval-ms`（默认3000）

---

## 方向28：表格与数据展示

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| 排序 | `canvas-stats/index.tsx:178`，单列sorter | ✅ 存在 |
| 分页大小 | `system-options/index.tsx:176`，`showSizeChanger: true` | ✅ 存在 |
| 横向滚动 | `system-options/index.tsx:176`，`scroll={{ x: 1180 }}` | ✅ 存在 |
| 展开行 | `tagImportBatchList.tsx:103-131`，`expandedRowRender` | ✅ 存在 |
| 加载/空状态 | antd Table 内置 loading + empty | ✅ 存在 |
| 列过滤/筛选 | — | ❌ 0个filterDropdown/filters/onFilter |
| 行选择/批量操作 | — | ❌ 0个rowSelection |
| 数据导出 | — | ❌ 0个CSV/Excel导出 |
| 列自定义 | — | ❌ 0个列显示/隐藏/列宽拖拽/列排序 |
| 固定列/表头 | — | ❌ 无fixed属性使用 |

### 解决方案（全做）

#### 28.1 列过滤与筛选（全表启用）

**技术方案**：
- 新增 `useTableFilters` hook — 统一表格筛选配置：
  ```ts
  const filterConfig: ColumnFilterConfig[] = [
    {
      key: 'status',
      type: 'select',
      options: [{ text: '启用', value: 'active' }, { text: '停用', value: 'inactive' }],
    },
    {
      key: 'name',
      type: 'search',  // 文本搜索过滤
      placeholder: '搜索名称...',
    },
    {
      key: 'createTime',
      type: 'dateRange',  // 日期范围过滤
    },
  ];
  ```
- 渲染 antd Table `filterDropdown` + `filteredValue` + `onFilter`
- 支持筛选状态持久化到 URL params（`?status=active&search=xxx`）
- 目标表格：画布列表、标签列表、用户列表、模板列表、操作日志
- 配置项：`table.filter.enabled`（默认true）、`table.filter.persist-url`（默认true）

#### 28.2 行选择与批量操作（3批量全做）

| 操作 | 适用表格 | 确认方式 |
|------|----------|----------|
| **批量删除** | 画布/标签/用户列表 | Modal.confirm + 进度提示 |
| **批量导出** | 画布/标签/操作日志 | 异步下载 + 通知 |
| **批量状态变更** | 画布列表 | 批量启用/停用 |

**技术方案**：
- 新增 `useBatchSelection` hook — 统一批量操作逻辑：
  ```tsx
  const { selectedRowKeys, selectedRows, clearSelection } = useBatchSelection();

  <Table
    rowSelection={{
      selectedRowKeys,
      onChange: (keys, rows) => { setSelectedRowKeys(keys); setSelectedRows(rows); },
    }}
  />

  {selectedRowKeys.length > 0 && (
    <BatchActionBar
      count={selectedRowKeys.length}
      actions={[
        { label: '批量删除', danger: true, onClick: handleBatchDelete },
        { label: '批量导出', onClick: handleBatchExport },
      ]}
      onClear={clearSelection}
    />
  )}
  ```
- 新增 `BatchActionBar` 组件 — 选中行后浮动的批量操作栏（固定在表头上方）
- 配置项：`table.batch.delete.enabled`（默认true）、`table.batch.export.enabled`（默认true）

#### 28.3 数据导出（CSV/Excel）

**技术方案**：
- 安装 `exceljs`（Excel导出） + 使用浏览器原生CSV（轻量场景）
- 新增 `useTableExport` hook — 统一导出逻辑：
  ```ts
  const { exportCSV, exportExcel } = useTableExport({
    filename: `canvas-list-${dayjs().format('YYYY-MM-DD')}`,
    columns: tableColumns,
    dataSource: tableData,
  });
  ```
- CSV导出：客户端构建（使用 `﻿` BOM解决中文乱码）
- Excel导出：exceljs 构建 .xlsx，支持列宽自适应、表头样式（加粗+背景色）、筛选下拉
- 导出按钮位置：表格工具栏（右侧）+ 批量操作栏
- 配置项：`table.export.csv.enabled`（默认true）、`table.export.excel.enabled`（默认true）、`table.export.max-rows`（默认10000）

#### 28.4 列自定义（显示/隐藏/排序/宽度）

**技术方案**：
- 新增 `ColumnCustomizer` 组件 — 列配置面板（齿轮图标触发下拉）：
  ```tsx
  const [visibleColumns, setVisibleColumns] = useLocalStorage('table:canvas-list:columns', defaultColumns);

  <Dropdown trigger={['click']} dropdownRender={() => (
    <ColumnCustomizer
      columns={allColumns}
      visible={visibleColumns}
      onChange={setVisibleColumns}
      onReset={() => setVisibleColumns(defaultColumns)}
    />
  )}>
    <Button icon={<SettingOutlined />} />
  </Dropdown>
  ```
- 功能：勾选显示/隐藏列、拖拽排序列顺序、重置为默认
- 持久化到 localStorage（按表格页面区分）
- 列宽拖拽：antd Table 已支持 `resizable`（需配合 `react-resizable`）
- 配置项：`table.column-customizer.enabled`（默认true）、`table.column-customizer.persist`（默认true）

#### 28.5 固定列与表头

**技术方案**：
- 操作列固定右侧：`{ title: '操作', fixed: 'right', width: 120, render: ... }`
- 名称/ID列固定左侧：`{ title: '名称', fixed: 'left', width: 200 }`
- 表头固定：antd Table `scroll={{ x: maxWidth, y: 'calc(100vh - 300px)' }}`
- 目标表格：所有超过6列的表格（操作日志、标签导入批次等）
- 配置项：`table.fixed-column.enabled`（默认true）

---

## 方向29：数据可视化与报表

### 代码扫描现状

| 能力 | 实现位置 | 状态 |
|------|----------|------|
| KPI卡片 | `home/index.tsx:166-208`，flex row布局，4个KPI | ✅ 完整 |
| 面积图 | `home/index.tsx:219-240`，AreaChart with gradient fills，双系列 | ✅ 完整 |
| 时间范围切换 | `home/index.tsx:139-143`，Segmented（7/14/30天） | ✅ 完整 |
| 周期对比 | `canvas-stats/index.tsx:89-96`，CompBadge with period-over-period | ✅ 完整 |
| 指标定义 | `canvas-stats/index.tsx:109-115`，5 KPI with icons | ✅ 完整 |
| 日期预设 | `canvas-stats/index.tsx:121-127`，RangePicker presets（7/30/90/180/365天） | ✅ 完整 |
| 交互图表 | `canvas-stats/index.tsx:266-286`，AreaChart with activeDot | ✅ 完整 |
| Recharts生态 | 已安装但仅用AreaChart | ⚠️ Bar/Pie/Line/Funnel未使用 |
| 报表构建器 | — | ❌ 无自定义报表/看板编辑器 |
| 图表导出 | — | ❌ 无图表导出为图片/PDF |
| 高级交互 | — | ❌ 无brush/zoom/drill-down/cross-chart联动 |
| 图表表格联动 | — | ❌ 表格和图表独立，无联动过滤 |

### 解决方案（全做）

#### 29.1 Recharts图表类型扩展（4图表全做）

| 图表 | 用途 | 页面 |
|------|------|------|
| **BarChart** | 画布执行次数排名 | 画布统计 |
| **PieChart** | 节点类型分布/触发来源占比 | 画布统计 |
| **LineChart** | 执行次数趋势（多画布对比） | 首页 |
| **FunnelChart**（可选） | 用户转化漏斗 | 营销活动分析 |

**技术方案**：
- 新增 `src/components/charts/` 目录 — 通用图表组件库：
  - `BarChartCard` — 柱状图（支持堆叠/分组，tooltip + legend）
  - `PieChartCard` — 饼图（支持环形图 innerRadius，中心文字）
  - `LineChartCard` — 折线图（支持多系列 + 区域渐变填充）
  - `ChartCard` — 通用图表容器（标题+时间范围+导出按钮）
- 画布统计页面新增图表Tab切换（面积图/柱状图/饼图）
- 首页新增画布执行排名（BarChart Top 10）
- 配置项：`charts.bar-chart.enabled`（默认true）、`charts.pie-chart.enabled`（默认true）

#### 29.2 图表导出（图片/PDF）

**技术方案**：
- 新增 `useChartExport` hook — 基于 `html2canvas` 截图导出：
  ```ts
  const exportChartAsImage = async (chartRef: RefObject<HTMLDivElement>, format: 'png' | 'jpeg') => {
    const canvas = await html2canvas(chartRef.current!);
    const url = canvas.toDataURL(`image/${format}`);
    const link = document.createElement('a');
    link.download = `chart-${Date.now()}.${format}`;
    link.href = url;
    link.click();
  };
  ```
- 图表卡片右上角下拉菜单：导出为PNG、导出为JPEG、导出为PDF（jsPDF）
- 配置项：`charts.export.enabled`（默认true）、`charts.export.formats`（默认["png","jpeg"]）

#### 29.3 高级图表交互（3交互全做）

| 交互 | 实现 | 用途 |
|------|------|------|
| **Brush/Zoom** | recharts Brush 组件 | 时间范围缩放选择 |
| **Drill-Down** | 点击图表元素下钻 | 从汇总→详情 |
| **Cross-Chart联动** | 共享filter状态 | 点击饼图扇区→柱状图联动过滤 |

**技术方案**：
- Brush缩放：AreaChart/BarChart 添加 `<Brush dataKey="date" height={30} stroke="#8884d8" />`
- Drill-Down：点击柱状图bar → 弹出Modal展示该维度详情表格
  ```tsx
  const handleBarClick = (data) => {
    setDrillDownData(data);
    setDrillDownVisible(true);
  };
  ```
- Cross-Chart联动：使用共享 `activeFilter` state，点击饼图扇区 → 更新filter → 柱状图过滤显示
  ```tsx
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  // PieChart onClick → setActiveFilter(entry.name)
  // BarChart data → data.filter(d => !activeFilter || d.category === activeFilter)
  ```
- 配置项：`charts.brush.enabled`（默认true）、`charts.drill-down.enabled`（默认true）、`charts.cross-chart-filter.enabled`（默认true）

#### 29.4 自定义报表构建器（阶段2）

**技术方案**：
- 新增 `ReportBuilder` 页面 — 拖拽式报表/看板编辑器：
  - 左侧：可用图表类型面板（KPI卡片/柱状图/饼图/折线图/面积图/表格）
  - 中间：看板画布（react-grid-layout 可拖拽调整卡片大小和位置）
  - 右侧：选中卡片配置面板（数据源/指标/维度/时间范围/样式）
- 新增 `DashboardView` 组件 — 渲染已保存的看板
- 看板数据持久化到后端（新表 `canvas_dashboard`）
- 首页替换为可自定义看板（默认看板=当前4 KPI + 面积图）
- 配置项：`report-builder.enabled`（默认true，阶段2开启）、`dashboard.custom.enabled`（默认true）

#### 29.5 图表表格联动

**技术方案**：
- 新增 `useChartTableFilter` hook — 图表与表格共享filter状态：
  ```tsx
  const [chartFilter, setChartFilter] = useState<ChartFilter | null>(null);

  // 图表点击 → 设置filter → 表格数据过滤
  <PieChart onClick={(entry) => setChartFilter({ type: entry.name })} />

  <Table
    dataSource={tableData.filter(row =>
      !chartFilter || row.type === chartFilter.type
    )}
  />
  ```
- 点击图表清除过滤：filter状态旁显示"清除过滤"链接
- 应用到画布统计页面：点击节点类型饼图 → 下方执行记录表联动过滤
- 配置项：`charts.table-link.enabled`（默认true）

---

## 实施路线图

### 阶段0：体验止血（第1-2周）

| 方向 | 配置项 | 工作量 | 影响 |
|------|--------|--------|------|
| 26 | 26.1 ErrorBoundary（3层） | 1d | **CRITICAL** 消除React异常白屏 |
| 26 | 26.2 404页面与路由兜底 | 0.5d | 消除路由空白 |
| 26 | 26.3 请求超时/取消（AbortController） | 0.5d | 内存泄漏防护 |
| 26 | 26.4 离线检测 | 0.5d | 断网友好提示 |
| 26 | 26.5 错误通知分类 | 0.5d | 错误信息可操作 |
| 24 | 24.1 语义化HTML结构 | 0.5d | 无障碍基础 |
| 24 | 24.2 屏幕阅读器工具类 | 0.5d | 无障碍基础 |
| 24 | 24.3 焦点管理（路由切换） | 0.5d | 键盘导航 |
| 24 | 24.4 表单错误关联（aria-describedby） | 0.5d | 表单无障碍 |
| 24 | 24.5 实时区域通知 | 0.5d | 动态内容通知 |

**阶段0小计：5.5天**

### 阶段1：交互质量升级（第2-6周）

| 方向 | 配置项 | 工作量 | 影响 |
|------|--------|--------|------|
| 23 | 23.1 Bundle优化（manualChunks/terser） | 1d | 首屏加载-30% |
| 23 | 23.2 虚拟滚动（3列表） | 1d | 长列表性能 |
| 23 | 23.3 乐观更新（3场景） | 1d | 交互即时感 |
| 23 | 23.4 数据预取与请求缓存（react-query） | 2d | 数据层现代化 |
| 23 | 23.5 防抖/节流系统化 | 0.5d | 性能基础 |
| 25 | 25.1 CSS动画基础库（6动画） | 0.5d | 动效基础 |
| 25 | 25.2 页面过渡动画（framer-motion） | 1d | 路由切换体验 |
| 25 | 25.3 骨架屏系统（5页面） | 1.5d | 加载体验 |
| 25 | 25.4 数字滚动动画 | 0.5d | KPI展示升级 |
| 25 | 25.5 注意力引导动画 | 0.5d | 用户引导 |
| 27 | 27.2 表单离开确认 | 0.5d | 数据安全 |
| 27 | 27.3 高级输入组件（TagInput/JsonEditor） | 1d | 输入体验 |
| 27 | 27.4 异步校验 | 0.5d | 表单校验完整 |
| 27 | 27.5 表单自动保存 | 1d | 数据安全 |
| 28 | 28.1 列过滤与筛选 | 1d | 表格可用性 |
| 28 | 28.2 行选择与批量操作 | 1d | 批量效率 |
| 28 | 28.3 数据导出（CSV/Excel） | 1d | 数据可移植 |
| 28 | 28.4 列自定义 | 1d | 表格灵活性 |
| 28 | 28.5 固定列与表头 | 0.5d | 宽表可用性 |

**阶段1小计：18天**

### 阶段2：数据深化与高级体验（第6-10周）

| 方向 | 配置项 | 工作量 | 影响 |
|------|--------|--------|------|
| 27 | 27.1 多步骤/向导表单 | 1.5d | 复杂表单简化 |
| 27 | 27.3 高级输入组件（RichTextEditor） | 1d | 模板编辑 |
| 29 | 29.1 Recharts图表类型扩展（Bar/Pie/Line） | 1.5d | 图表丰富度 |
| 29 | 29.2 图表导出（图片/PDF） | 1d | 数据分享 |
| 29 | 29.3 高级图表交互（Brush/Drill-Down/Cross-Chart） | 2d | 分析深度 |
| 29 | 29.4 自定义报表构建器 | 3d | 个性化分析 |
| 29 | 29.5 图表表格联动 | 1d | 数据探索 |

**阶段2小计：11天**

---

## 关键发现与建议

### 发现

1. **7方向70项评估：26已有（37%）/18部分（26%）/26缺失（37%）**，交互质量层成熟度明显高于方向16-22（18%已有），说明平台在"功能可用"基础上已关注部分"体验优质"
2. **方向29（数据可视化）是最强方向**：7已有/1部分/2缺失，KPI卡片+面积图+周期对比设计扎实，Recharts基础设施已部署但未充分利用
3. **方向26（错误处理）有1个CRITICAL**：ErrorBoundary全站为0，React渲染异常直接白屏——这是阶段0最高优先级
4. **方向25（动效）是最大空白**：0个@keyframes定义，0个页面过渡动画，framer-motion等动画库均未安装——从0到1的建设
5. **方向23（性能）基础扎实但链路不完整**：代码分割（23个lazy）、React.memo、useCallback/useMemo均到位，但bundle优化、虚拟滚动、optimistic UI、react-query全链路缺失
6. **方向28（表格）哑铃型分布**：基础功能（排序/分页/展开行/空状态）完善，高级功能（过滤/批量/导出/列自定义）全部缺失——5个方向最容易补齐
7. **方向24（无障碍）有片段但无体系**：ARIA属性零散存在，但语义化HTML、screen reader、skip link、form error association全链路缺失——需要从架构层面建立无障碍体系

### 14方向全景统计（方向16-29）

| 指标 | 方向16-22 | 方向23-29 | 合计 |
|------|-----------|-----------|------|
| 已有 | 15（18%） | 26（37%） | **41（27%）** |
| 部分 | 22（26%） | 18（26%） | **40（26%）** |
| 缺失 | 47（56%） | 26（37%） | **73（47%）** |
| 配置项 | 84 | 70 | **154** |

### 与方向1-15的关系

- 方向14（产品体验设计）：已覆盖ErrorBoundary/404/500/设计系统概念，方向26是方向14的具体落地实现
- 方向4（智能化）：方向29的自定义报表构建器可与AI分析联动（AI生成报表配置 → 报表构建器渲染）
- 方向10（国际化）：方向24的无障碍ARIA属性需要考虑多语言场景
- 方向3（数据资产）：方向29的报表构建器可应用于CDP数据分析看板
- 方向8（技术架构）：方向23的bundle优化/react-query是前端架构现代化的关键基础设施

### 阶段依赖

```
阶段0（5.5天）← ErrorBoundary/404/无障碍止血，无前置依赖
  └→ 阶段1（18天）← react-query是多个方向的基础设施
      ├→ 方向23.4 react-query → 方向26错误分类增强、方向28数据导出
      ├→ 方向25.1 CSS动画基础库 → 方向25.2-25.5全部动效
      └→ 方向28.1 列过滤 → 方向29.5 图表表格联动
  └→ 阶段2（11天）← 依赖阶段1的react-query + CSS动画库
      ├→ 方向29.4 报表构建器 → 依赖方向23.4 react-query数据预取
      └→ 方向29.3 高级图表交互 → 依赖方向29.1 图表类型扩展
```

---

> **文档状态**：待评审
> **下一步**：阶段0详细实施计划（ErrorBoundary/404/离线检测/无障碍止血）
> **参考文档**：
> - `docs/optimization/product-interaction-directions-2026-06-01.md`（方向16-22）
> - `docs/optimization/product-evolution-directions-2026-05-31.md`（方向1-10）
> - `docs/optimization/product-evolution-directions-ext-2026-05-31.md`（方向11-15）
> - `.claude/projects/-Users-photonpay-project-canvas/memory/product_interaction_design_scan_2026_06_pt2.md`（方向23-29扫描结果）
