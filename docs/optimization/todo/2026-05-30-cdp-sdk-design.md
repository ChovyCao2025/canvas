# CDP 埋点数据采集 SDK 设计规格（2026-05-30）

全量设计，覆盖3个子系统：
1. **Web JS SDK** — 浏览器端埋点SDK
2. **Mobile SDK (iOS/Android)** — 移动端埋点SDK
3. **服务端接收管线 + 可视化埋点管理** — batch接收API、SDK字段扩展、去重限流、可视化埋点配置后台

参考标杆：Segment Analytics-next（插件管线架构）、神策SDK（全埋点+可视化埋点+合规）、Mixpanel JS SDK（opt-out/隐私）。

---

## 一、Web JS SDK

### 1.1 架构：Plugin Pipeline

事件流经四类插件，每类可注册多个，按注册顺序执行：

```
用户调用 API (track/identify/page/group/alias)
  → EventQueue (内存 + localStorage 持久化)
    → Before Plugins (校验/过滤/采样/PII脱敏)
    → Enrichment Plugins (设备信息/UTM参数/会话/上下文注入)
    → Destination Plugins (发送到Canvas服务端/第三方)
    → After Plugins (控制台日志/性能指标)
```

**插件类型定义：**

```typescript
interface Plugin {
  name: string
  type: 'before' | 'enrichment' | 'destination' | 'after'
  version: string
  isLoaded: () => boolean
  load: (ctx: SDKContext) => Promise<void>
  // 按事件类型钩子，返回 null 则丢弃事件
  track?: (ctx: EventContext) => EventContext | null
  identify?: (ctx: EventContext) => EventContext | null
  page?: (ctx: EventContext) => EventContext | null
  group?: (ctx: EventContext) => EventContext | null
  alias?: (ctx: EventContext) => EventContext | null
}
```

**核心类：**

```typescript
class CanvasAnalytics {
  private queue: EventQueue
  private pipeline: PluginPipeline
  private config: SDKConfig
  private storage: PersistenceLayer  // localStorage + cookie

  // 核心 API
  track(event: string, properties?: Record<string, any>): void
  identify(userId: string, traits?: Record<string, any>): void
  page(name?: string, properties?: Record<string, any>): void
  group(groupId: string, traits?: Record<string, any>): void
  alias(newId: string, previousId?: string): void

  // 插件管理
  register(...plugins: Plugin[]): Promise<void>
  deregister(pluginName: string): Promise<void>

  // 合规
  optIn(options?: OptInOptions): void
  optOut(options?: OptOutOptions): void
  hasOptedOut(): boolean

  // 手动控制
  flush(): Promise<void>
  reset(): void  // 清除 userId/traits/anonymousId
}
```

### 1.2 初始化与配置

**立即加载模式：**

```typescript
import { CanvasAnalytics } from '@canvas/analytics-web'

const analytics = CanvasAnalytics.load({
  writeKey: 'YOUR_WRITE_KEY',
  serverUrl: 'https://your-domain.com/cdp/events/track',
  // 批量发送
  flushAt: 20,              // 攒够20条发送
  flushInterval: 3000,      // 3秒定时发送
  // 发送方式
  sendType: 'beacon',       // beacon | xhr | image（自动降级）
  // 持久化
  disableClientPersistence: false,  // localStorage 持久化离线事件
  retryQueue: true,         // 离线重试队列
  maxRetries: 3,            // 失败重试次数
  // 全埋点
  autoTrack: {
    pageview: true,         // 自动采集 $pageview
    click: true,            // 自动采集 $WebClick
    stay: true,             // 自动采集 $WebStay（停留时长）
  },
  // 热图
  heatmap: {
    clickmap: 'default',    // 'default' | 'not_collect'
    scrollmap: 'not_collect',
  },
  // 隐私
  isComplianceEnabled: false,  // 合规模式（延迟初始化）
  // 采样
  sampleRate: 1.0,          // 0~1，1=全量采集
})
```

**延迟加载模式（合规场景）：**

```typescript
// 先创建实例，事件入内存队列但不发送
const analytics = new CanvasAnalytics()

// 用户同意隐私条款后
if (userConsented) {
  await analytics.load({
    writeKey: 'YOUR_WRITE_KEY',
    serverUrl: 'https://your-domain.com/cdp/events/track',
  })
  // 队列中的事件开始flush
}
```

### 1.3 事件队列与发送

**EventQueue 设计：**

```
track() 调用
  → 构造 EventContext
  → 入内存队列 (Array)
  → 同步写 localStorage (key: canvas_sdk_queue)
  → 检查 flushAt / flushInterval
    → 满足条件 → flush()
      → 批量序列化
      → 选择发送方式 (beacon > xhr > image)
      → 成功 → 清除 localStorage 对应条目
      → 失败 → 指数退避重试 (1s, 2s, 4s, 最多3次)
```

**发送方式优先级：**
1. **navigator.sendBeacon** — 页面关闭时也能发送，优先使用
2. **XMLHttpRequest** — beacon 不可用时降级，支持更大payload
3. **Image pixel** — 最终降级，1x1 gif，payload放query string（有长度限制）

**离线持久化：**
- 事件写入 localStorage（key: `canvas_sdk_queue`），格式为 JSON 数组
- 每条事件带 `messageId`（UUID v4）用于去重
- localStorage 溢出时丢弃最旧事件（FIFO）
- 页面恢复在线时自动 flush 持久化队列

**批量请求格式：**

```json
POST /cdp/events/track
Content-Type: application/json
Authorization: Basic <base64(writeKey:)>

{
  "batch": [
    {
      "messageId": "uuid-v4",
      "type": "track",
      "event": "OrderComplete",
      "userId": "user-123",
      "anonymousId": "anon-uuid",
      "context": { ... },
      "properties": { "amount": 99.9, "currency": "CNY" },
      "timestamp": "2026-05-30T10:00:00.000Z",
      "sentAt": "2026-05-30T10:00:01.000Z"
    }
  ],
  "sentAt": "2026-05-30T10:00:01.000Z"
}
```

### 1.4 三种埋点方式

#### 1.4.1 代码埋点

开发者手动调用SDK API：

```typescript
// 追踪事件
analytics.track('OrderComplete', {
  orderId: 'ORD-001',
  amount: 99.9,
  currency: 'CNY',
  items: [{ sku: 'P001', name: '商品A', qty: 2 }]
})

// 识别用户
analytics.identify('user-123', {
  name: '张三',
  email: 'zhangsan@example.com',
  vipLevel: 'gold',
  registeredAt: '2025-01-15'
})

// 页面浏览
analytics.page('ProductDetail', {
  productId: 'P001',
  category: '电子产品',
  price: 49.9
})

// 用户分组
analytics.group('company-001', {
  name: 'ABC公司',
  industry: '互联网',
  employeeCount: 500
})

// ID关联（匿名→实名）
analytics.alias('user-123')
```

#### 1.4.2 全埋点（AutoTrack）

SDK自动采集以下事件，无需开发者手动调用：

| 事件 | 触发时机 | 自动属性 |
|------|---------|---------|
| `$pageview` | 页面加载/SPA路由切换 | `$url`, `$title`, `$referrer`, `$url_path` |
| `$WebClick` | 用户点击可交互元素 | `$element_type`, `$element_content`, `$element_path`, `$url` |
| `$WebStay` | 用户离开页面时 | `$stay_duration`(ms), `$url`, `$title` |

**全埋点配置：**

```typescript
autoTrack: {
  pageview: {
    enabled: true,
    singlePage: true,        // SPA 路由切换自动触发
    urlParams: ['utm_source', 'utm_medium', 'utm_campaign'],  // 采集URL参数
  },
  click: {
    enabled: true,
    elementFilter: (el) => {  // 过滤不需要采集的元素
      return el.tagName !== 'BODY' && el.tagName !== 'HTML'
    },
  },
  stay: {
    enabled: true,
    heartbeat: 30,           // 30秒心跳，最长记录时长
  },
}
```

**$WebClick 元素路径算法：**

```
document → html → body → div.container → div#main → button.buy-btn
```

生成CSS选择器路径：`div.container > div#main > button.buy-btn`，用于可视化埋点规则匹配。

**SPA路由监听：**

- 监听 `popstate` + 拦截 `history.pushState/replaceState`
- 路由变化时自动触发 `$pageview`
- 支持配置 `urlParams` 采集特定URL参数

#### 1.4.3 可视化埋点

**双模式设计：**

**模式A：服务端配置模式（生产环境）**

1. 运营在管理后台的"可视化埋点"页面，输入目标网站URL
2. 管理后台通过 iframe 加载目标网站，注入圈选工具JS
3. 运营点击页面元素，系统自动生成元素选择器规则
4. 规则保存到服务端 `visual_track_rule` 表
5. SDK初始化时从服务端拉取规则，运行时动态匹配元素并采集

**模式B：App内圈选模式（开发/测试环境）**

1. SDK初始化时开启 `visualMode: true`
2. 页面右下角显示"圈选"悬浮按钮
3. 点击后进入圈选模式，鼠标悬停高亮元素
4. 点击元素弹出配置面板（事件名、属性映射）
5. 配置保存到服务端，同时本地立即生效

**可视化埋点规则格式：**

```json
{
  "ruleId": "vtr_001",
  "eventCode": "BUTTON_CLICK_BUY",
  "eventName": "购买按钮点击",
  "selector": "button.buy-btn",
  "urlPattern": "/product/*",
  "properties": [
    { "from": "element.dataset.productId", "to": "productId", "type": "string" },
    { "from": "element.textContent", "to": "buttonText", "type": "string" }
  ],
  "enabled": true
}
```

**SDK端规则匹配流程：**

```
页面加载/路由切换
  → 从缓存加载 visual_track_rules
  → 遍历规则，对每条规则：
    → 检查 urlPattern 是否匹配当前URL
    → document.querySelectorAll(selector) 查找目标元素
    → 对匹配元素绑定 click 事件监听
  → 用户点击匹配元素
    → 提取 properties（从 element.dataset / element.textContent / element.attributes）
    → 构造 track 事件并发送
```

**规则热更新：**
- SDK每5分钟轮询服务端获取规则更新
- 支持服务端通过 SSE 推送规则变更（可选）
- 规则变更后，移除旧监听、绑定新监听

### 1.5 会话管理

```typescript
interface SessionConfig {
  timeout: 30 * 60 * 1000,  // 30分钟无活动则新会话
  cookieName: 'canvas_session_id',
  cookieDomain: '',          // 默认当前域名
  cookieSecure: false,
}
```

**会话生命周期：**
1. SDK初始化时检查 cookie `canvas_session_id`
2. 存在且未过期 → 复用 sessionId，更新 `lastActivityAt`
3. 不存在或已过期 → 生成新 sessionId（UUID v4），写入 cookie
4. 每次事件发送时更新 `lastActivityAt`
5. 页面关闭时通过 beacon 发送 `$WebStay` 事件

**会话信息注入到每个事件的 context：**

```json
{
  "context": {
    "sessionId": "sess-uuid",
    "sessionCount": 5,
    "sessionStart": false
  }
}
```

### 1.6 身份识别

**anonymousId：**
- SDK初始化时自动生成 UUID v4
- 存储在 localStorage（key: `canvas_anonymous_id`）
- 跨页面持久化，清除浏览器数据才丢失

**userId：**
- 调用 `identify(userId, traits)` 后设置
- 存储在 localStorage（key: `canvas_user_id`）
- 后续所有事件同时携带 `userId` 和 `anonymousId`

**alias：**
- `alias(newId)` 将当前 anonymousId 关联到 newId
- 服务端负责合并两个ID的用户画像
- 事件格式：`{ type: "alias", userId: "newId", previousId: "anon-uuid" }`

**身份合并流程：**

```
访客浏览 → anonymousId = "anon-abc"
  → track('PageView', ...)  // userId=null, anonymousId="anon-abc"
  → track('AddToCart', ...) // userId=null, anonymousId="anon-abc"
访客登录 → identify("user-123", {name: "张三"})
  → 服务端收到 identify 事件，关联 anon-abc → user-123
  → 后续事件 userId="user-123", anonymousId="anon-abc"
```

### 1.7 隐私合规

**GDPR/PIPL 合规模式：**

```typescript
// 延迟初始化 — 用户同意前不采集
const analytics = new CanvasAnalytics()
// 事件入内存队列但不发送

// 用户同意后
analytics.load({ writeKey, serverUrl, ... })

// 用户拒绝 — 清除所有数据
analytics.optOut({ clearPersistence: true, deleteUser: true })

// 检查状态
analytics.hasOptedOut()  // true/false
```

**PII脱敏插件（内置）：**

```typescript
const piiScrubber: Plugin = {
  name: 'PII Scrubber',
  type: 'before',
  version: '1.0.0',
  isLoaded: () => true,
  load: () => Promise.resolve(),
  track: (ctx) => {
    const scrubbed = scrubPII(ctx.event.properties, piiFields)
    ctx.event.properties = scrubbed
    return ctx
  },
  identify: (ctx) => {
    const scrubbed = scrubPII(ctx.event.traits, piiFields)
    ctx.event.traits = scrubbed
    return ctx
  },
}
```

**默认脱敏字段：** `password`, `creditCard`, `ssn`, `idCard`, `phone`（中间4位替换为*）

**采样控制：**

```typescript
// 全局采样
sampleRate: 0.1  // 只采集10%流量

// 按事件类型采样（通过 Before 插件）
const samplingPlugin: Plugin = {
  name: 'Event Sampling',
  type: 'before',
  version: '1.0.0',
  isLoaded: () => true,
  load: () => Promise.resolve(),
  track: (ctx) => {
    const rates = { 'PageView': 0.1, 'ButtonClick': 1.0 }
    const rate = rates[ctx.event.event] ?? 1.0
    return Math.random() < rate ? ctx : null  // null = 丢弃
  },
}
```

### 1.8 Enrichment 插件（内置）

SDK内置以下Enrichment插件，自动注入到每个事件的 `context` 字段：

| 插件 | 注入字段 | 说明 |
|------|---------|------|
| **DevicePlugin** | `context.device` | userAgent解析：os/browser/browserVersion/deviceType |
| **PagePlugin** | `context.page` | url/path/referrer/title/search/hash |
| **UtmPlugin** | `context.campaign` | utm_source/utm_medium/utm_campaign/utm_term/utm_content |
| **SessionPlugin** | `context.session` | sessionId/sessionCount/sessionStart |
| **LibraryPlugin** | `context.library` | name="@canvas/analytics-web", version="1.0.0" |
| **IpPlugin** | `context.ip` | 由服务端注入，SDK不采集 |

**context 完整结构：**

```json
{
  "context": {
    "library": { "name": "@canvas/analytics-web", "version": "1.0.0" },
    "device": { "type": "desktop", "os": "macOS", "browser": "Chrome", "browserVersion": "125" },
    "page": { "url": "https://example.com/product/P001", "path": "/product/P001", "referrer": "https://google.com", "title": "商品详情" },
    "campaign": { "utm_source": "google", "utm_medium": "cpc", "utm_campaign": "summer_sale" },
    "session": { "sessionId": "sess-uuid", "sessionCount": 5, "sessionStart": false },
    "ip": "1.2.3.4",
    "timezone": "Asia/Shanghai",
    "locale": "zh-CN",
    "screenWidth": 1920,
    "screenHeight": 1080
  }
}
```

### 1.9 SDK包结构

```
@canvas/analytics-web/
├── src/
│   ├── core/
│   │   ├── CanvasAnalytics.ts      # 主入口类
│   │   ├── EventQueue.ts           # 事件队列（内存+localStorage）
│   │   ├── PluginPipeline.ts       # 插件管线执行器
│   │   ├── PersistenceLayer.ts     # localStorage/cookie 封装
│   │   └── Sender.ts              # 发送器（beacon/xhr/image降级）
│   ├── plugins/
│   │   ├── enrichment/
│   │   │   ├── DevicePlugin.ts
│   │   │   ├── PagePlugin.ts
│   │   │   ├── UtmPlugin.ts
│   │   │   ├── SessionPlugin.ts
│   │   │   └── LibraryPlugin.ts
│   │   ├── autotrack/
│   │   │   ├── PageviewTracker.ts  # $pageview 自动采集
│   │   │   ├── ClickTracker.ts     # $WebClick 自动采集
│   │   │   └── StayTracker.ts      # $WebStay 停留时长
│   │   ├── visual/
│   │   │   ├── VisualTracker.ts    # 可视化埋点规则匹配
│   │   │   └── VisualEditor.ts     # App内圈选模式
│   │   └── compliance/
│   │       ├── PIIScrubber.ts      # PII脱敏
│   │       └── ConsentManager.ts   # 隐私合规
│   ├── utils/
│   │   ├── uuid.ts
│   │   ├── cookie.ts
│   │   ├── ua-parser.ts
│   │   └── selector.ts            # CSS选择器生成
│   └── types/
│       └── index.ts                # TypeScript 类型定义
├── package.json
├── tsconfig.json
├── rollup.config.js                # UMD + ESM + CJS 三格式输出
└── README.md
```

**构建输出：**
- `dist/canvas-analytics.min.js` — UMD格式，`<script>` 标签引入
- `dist/canvas-analytics.esm.js` — ESM格式，`import` 引入
- `dist/canvas-analytics.cjs.js` — CJS格式，SSR使用
- Gzip后目标 < 25KB

---

## 二、Mobile SDK (iOS/Android)

### 2.1 统一协议

Mobile SDK与Web SDK共享相同的事件协议，确保服务端接收管线统一处理。

**事件模型：**

```json
{
  "messageId": "uuid-v4",
  "type": "track|identify|page|group|alias",
  "event": "EventName",
  "userId": "user-123",
  "anonymousId": "anon-uuid",
  "properties": { ... },
  "traits": { ... },
  "context": {
    "library": { "name": "@canvas/analytics-ios", "version": "1.0.0" },
    "device": { "type": "ios", "model": "iPhone 15", "os": "iOS 17.5", "osVersion": "17.5" },
    "app": { "name": "MyApp", "version": "2.1.0", "build": "210" },
    "screen": { "width": 1170, "height": 2532, "density": 3 },
    "network": { "type": "wifi", "carrier": "中国移动" },
    "location": { "latitude": 39.9, "longitude": 116.4 },  // 需用户授权
    "timezone": "Asia/Shanghai",
    "locale": "zh-Hans"
  },
  "timestamp": "2026-05-30T10:00:00.000Z",
  "sentAt": "2026-05-30T10:00:01.000Z"
}
```

### 2.2 iOS SDK (Swift)

**包名：** `CanvasAnalytics` (Swift Package / CocoaPods)

**核心类：**

```swift
class CanvasAnalytics {
    static var shared: CanvasAnalytics

    // 初始化
    func configure(writeKey: String, serverUrl: String, config: SDKConfig)

    // 核心 API
    func track(_ event: String, properties: [String: Any]? = nil)
    func identify(_ userId: String, traits: [String: Any]? = nil)
    func screen(_ name: String, properties: [String: Any]? = nil)
    func group(_ groupId: String, traits: [String: Any]? = nil)
    func alias(_ newId: String)

    // 合规
    func optIn()
    func optOut(clearPersistence: Bool = false)
    var hasOptedOut: Bool { get }

    // 手动控制
    func flush()
    func reset()
}
```

**SDKConfig：**

```swift
struct SDKConfig {
    var flushAt: Int = 20
    var flushInterval: TimeInterval = 30  // 秒
    var maxQueueSize: Int = 1000
    var autoTrack: AutoTrackConfig = .default
    var isComplianceEnabled: Bool = false
    var enableLog: Bool = false
}

struct AutoTrackConfig {
    var appStart: Bool = true       // $AppStart
    var appEnd: Bool = true         // $AppEnd
    var appClick: Bool = true       // $AppClick
    var appViewScreen: Bool = true  // $AppViewScreen (UIViewController appear)
}
```

**iOS全埋点事件：**

| 事件 | 触发时机 | 自动属性 |
|------|---------|---------|
| `$AppStart` | App从后台切到前台 | `$is_first_time`, `$resume_from_background` |
| `$AppEnd` | App切到后台/终止 | `$duration`(前台停留秒数) |
| `$AppClick` | 用户点击UIControl | `$element_type`, `$element_content`, `$screen_name` |
| `$AppViewScreen` | UIViewController viewDidAppear | `$screen_name`, `$title` |

**iOS全埋点实现方式：**
- `$AppStart/$AppEnd`：监听 `UIApplication.didBecomeActiveNotification` / `willResignActiveNotification`
- `$AppClick`：Method Swizzling 替换 `UIApplication.sendEvent`，过滤 `UITouch` 事件
- `$AppViewScreen`：Method Swizzling 替换 `UIViewController.viewDidAppear`

**iOS可视化埋点：**
- 圈选模式：在App内叠加一个透明 `VisualEditorWindow`，触摸时高亮元素
- 元素路径：从目标View沿superview链生成路径（类似 `UIViewController > UIView > UIButton`）
- 规则匹配：从服务端拉取规则，运行时通过 `view.tag` / `accessibilityIdentifier` / class+path 匹配

**iOS离线持久化：**
- 事件队列写入 SQLite（`canvas_events.db`）
- 网络恢复时自动 flush
- 数据库大小上限 10MB，超出丢弃最旧事件

**iOS包结构：**

```
CanvasAnalytics/
├── Core/
│   ├── CanvasAnalytics.swift       # 主入口
│   ├── EventQueue.swift            # 事件队列
│   ├── SQLitePersistence.swift     # SQLite持久化
│   ├── Sender.swift                # URLSession批量发送
│   └── SDKConfig.swift
├── Plugins/
│   ├── AutoTrackPlugin.swift       # 全埋点
│   ├── VisualTrackPlugin.swift     # 可视化埋点
│   └── ConsentPlugin.swift         # 隐私合规
├── Utils/
│   ├── UUID.swift
│   ├── KeychainStore.swift         # anonymousId/userId存储
│   └── ViewPath.swift              # 元素路径生成
└── Types/
    └── Event.swift
```

### 2.3 Android SDK (Kotlin)

**包名：** `com.canvas.analytics` (Maven / Gradle)

**核心类：**

```kotlin
object CanvasAnalytics {
    fun configure(writeKey: String, serverUrl: String, config: SDKConfig = SDKConfig())
    fun track(event: String, properties: Map<String, Any>? = null)
    fun identify(userId: String, traits: Map<String, Any>? = null)
    fun screen(name: String, properties: Map<String, Any>? = null)
    fun group(groupId: String, traits: Map<String, Any>? = null)
    fun alias(newId: String)
    fun optIn()
    fun optOut(clearPersistence: Boolean = false)
    val hasOptedOut: Boolean
    fun flush()
    fun reset()
}
```

**Android全埋点事件：**

| 事件 | 触发时机 | 实现方式 |
|------|---------|---------|
| `$AppStart` | Application.onCreate | LifecycleObserver ON_START |
| `$AppEnd` | App切后台 | LifecycleObserver ON_STOP |
| `$AppClick` | 用户点击View | ASM插桩 `View.OnClickListener.onClick` |
| `$AppViewScreen` | Activity.onResume | LifecycleObserver ON_RESUME |

**Android全埋点实现方式：**
- `$AppStart/$AppEnd`：`ProcessLifecycleOwner` 生命周期监听
- `$AppClick`：Gradle插件ASM字节码插桩，在 `View.OnClickListener.onClick` 方法入口注入埋点代码
- `$AppViewScreen`：`Application.ActivityLifecycleCallbacks` 监听 `onActivityResumed`

**Android可视化埋点：**
- 圈选模式：在Activity上叠加 `VisualEditorOverlay`（FrameLayout），触摸时高亮View
- 元素路径：从目标View沿parent链生成路径（`Activity > DecorView > LinearLayout > Button`）
- 规则匹配：通过 `view.id`（资源ID转名称） / `view.tag` / class+path 匹配

**Android离线持久化：**
- 事件队列写入 Room SQLite
- 网络恢复时通过 `ConnectivityManager.NetworkCallback` 触发 flush
- 数据库大小上限 10MB

**Android包结构：**

```
canvas-analytics-android/
├── sdk/
│   ├── src/main/java/com/canvas/analytics/
│   │   ├── CanvasAnalytics.kt       # 主入口
│   │   ├── EventQueue.kt
│   │   ├── RoomPersistence.kt       # Room SQLite持久化
│   │   ├── Sender.kt                # OkHttp批量发送
│   │   └── SDKConfig.kt
│   ├── plugins/
│   │   ├── AutoTrackPlugin.kt
│   │   ├── VisualTrackPlugin.kt
│   │   └── ConsentPlugin.kt
│   └── utils/
│       ├── UUID.kt
│       ├── SharedPreferencesStore.kt
│       └── ViewPath.kt
├── plugin/                           # Gradle ASM 插桩插件
│   └── src/main/java/com/canvas/analytics/plugin/
│       ├── AnalyticsPlugin.kt        # Gradle Transform
│       └── ClickInjector.kt          # ASM插桩逻辑
├── build.gradle
└── README.md
```

### 2.4 Mobile SDK 共同设计点

**批量发送：**
- 默认攒20条或30秒发送一次
- 使用系统网络库（iOS: URLSession, Android: OkHttp）
- 发送失败指数退避重试（1s, 2s, 4s, 最多3次）
- App切后台时立即 flush（iOS: `applicationDidEnterBackground`, Android: `onStop`）

**会话管理：**
- 30秒无事件则新会话（Mobile场景比Web短）
- sessionId 存储在 Keychain(iOS) / SharedPreferences(Android)
- App切后台超过30秒再切回 = 新会话

**身份识别：**
- anonymousId 存储在 Keychain(iOS) / SharedPreferences(Android)
- userId 调用 identify 后设置
- alias 关联匿名ID到实名ID

**隐私合规：**
- iOS: `ATTrackingManager` 请求追踪授权
- Android: `AdvertisingIdClient` 获取GAID（需用户授权）
- 合规模式下延迟初始化，用户同意后才开始采集
- `optOut` 清除所有本地数据

---

## 三、服务端接收管线 + 可视化埋点管理

### 3.1 服务端接收管线

#### 3.1.1 新增 Batch 接收 API

**现有接口：** `POST /canvas/events/report` — 单条事件上报（画布触发专用）

**新增接口：** `POST /cdp/events/track` — SDK批量事件上报（CDP数据采集专用）

两个接口职责分离：
- `/canvas/events/report` — 业务系统上报，触发画布执行
- `/cdp/events/track` — SDK上报，写入event_log + 发MQ供下游消费

**请求格式：**

```
POST /cdp/events/track
Content-Type: application/json
Authorization: Basic <base64(writeKey:)>

{
  "batch": [
    {
      "messageId": "uuid-v4",
      "type": "track",
      "event": "OrderComplete",
      "userId": "user-123",
      "anonymousId": "anon-uuid",
      "properties": { "amount": 99.9 },
      "context": { "library": {...}, "device": {...}, "session": {...} },
      "timestamp": "2026-05-30T10:00:00.000Z",
      "sentAt": "2026-05-30T10:00:01.000Z"
    },
    ...
  ],
  "sentAt": "2026-05-30T10:00:01.000Z"
}
```

**响应格式：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "accepted": 20,
    "rejected": 0,
    "errors": []
  }
}
```

**处理流程：**

```
请求到达
  → 验证 writeKey（Basic Auth）
  → 限流检查（Redis令牌桶，每writeKey 100 QPS）
  → 批量解析事件
  → 逐条处理：
    → 去重检查（Redis SET: canvas:event:dedup:{messageId}, TTL 24h）
    → 事件校验（type必填、event必填当type=track、timestamp不超24h）
    → 注入服务端context（IP、serverReceivedAt）
    → 写入 event_log 表
    → 发MQ（topic: cdp-event-ingested）
  → 返回 accepted/rejected 计数
```

#### 3.1.2 event_log 表扩展

现有 `event_log` 表缺少SDK所需字段，需新增列：

```sql
-- V91: Extend event_log for SDK ingestion
ALTER TABLE event_log
  ADD COLUMN message_id VARCHAR(36) NULL COMMENT '事件唯一ID，SDK生成UUID v4，用于去重',
  ADD COLUMN event_type VARCHAR(20) NOT NULL DEFAULT 'track' COMMENT '事件类型: track/identify/page/group/alias',
  ADD COLUMN anonymous_id VARCHAR(64) NULL COMMENT '匿名用户ID',
  ADD COLUMN session_id VARCHAR(64) NULL COMMENT '会话ID',
  ADD COLUMN platform VARCHAR(20) NULL COMMENT '平台: web/ios/android',
  ADD COLUMN device_id VARCHAR(64) NULL COMMENT '设备ID',
  ADD COLUMN sdk_context JSON NULL COMMENT 'SDK上下文(device/page/campaign/session等)',
  ADD COLUMN client_timestamp DATETIME(3) NULL COMMENT '客户端事件时间',
  ADD COLUMN server_received_at DATETIME(3) NULL COMMENT '服务端接收时间',
  ADD COLUMN write_key VARCHAR(64) NULL COMMENT 'SDK写入密钥标识',
  ADD UNIQUE KEY uk_message_id (message_id),
  ADD INDEX idx_event_type_platform (event_type, platform),
  ADD INDEX idx_anonymous_id (anonymous_id),
  ADD INDEX idx_session_id (session_id),
  ADD INDEX idx_client_timestamp (client_timestamp);
```

#### 3.1.3 writeKey 管理

```sql
-- V92: SDK writeKey management
CREATE TABLE IF NOT EXISTS sdk_write_key (
  id BIGINT NOT NULL AUTO_INCREMENT,
  write_key VARCHAR(64) NOT NULL COMMENT 'SDK写入密钥，base64编码',
  name VARCHAR(100) NOT NULL COMMENT '密钥名称，如"官网Web SDK"',
  platform VARCHAR(20) NOT NULL COMMENT '平台: web/ios/android',
  enabled TINYINT NOT NULL DEFAULT 1,
  rate_limit_qps INT NOT NULL DEFAULT 100 COMMENT '每秒限流上限',
  daily_quota INT NULL COMMENT '每日事件配额上限，NULL=不限',
  description VARCHAR(500) NULL,
  created_by VARCHAR(64) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_write_key (write_key),
  INDEX idx_platform_enabled (platform, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SDK写入密钥管理';
```

#### 3.1.4 CdpEventIngestionController

```java
@RestController
@RequestMapping("/cdp")
@RequiredArgsConstructor
public class CdpEventIngestionController {

    private final CdpEventIngestionService ingestionService;

    @PostMapping("/events/track")
    public Mono<R<IngestionResult>> track(
            ServerHttpRequest request,
            @RequestBody Mono<BatchTrackReq> body) {
        return body.flatMap(req -> {
            String writeKey = extractWriteKey(request);
            return ingestionService.ingestBatch(writeKey, req);
        }).map(R::ok);
    }

    private String extractWriteKey(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Basic ")) {
            String decoded = new String(Base64.getDecoder().decode(auth.substring(6)));
            return decoded.split(":")[0];  // writeKey: 格式
        }
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
}
```

#### 3.1.5 CdpEventIngestionService

```java
@Service
@RequiredArgsConstructor
public class CdpEventIngestionService {

    private final SdkWriteKeyMapper writeKeyMapper;
    private final EventLogMapper eventLogMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    public Mono<IngestionResult> ingestBatch(String writeKey, BatchTrackReq req) {
        return Mono.fromCallable(() -> {
            // 1. 验证 writeKey
            SdkWriteKeyDO keyDO = writeKeyMapper.selectByWriteKey(writeKey);
            if (keyDO == null || keyDO.getEnabled() != 1) {
                throw new IllegalArgumentException("Invalid writeKey");
            }

            // 2. 限流
            checkRateLimit(writeKey, keyDO.getRateLimitQps());

            int accepted = 0, rejected = 0;
            List<String> errors = new ArrayList<>();

            for (TrackEvent event : req.getBatch()) {
                try {
                    // 3. 去重
                    if (isDuplicate(event.getMessageId())) {
                        rejected++;
                        continue;
                    }

                    // 4. 校验
                    validateEvent(event);

                    // 5. 注入服务端context
                    event.setServerReceivedAt(LocalDateTime.now());

                    // 6. 写入 event_log
                    EventLogDO logDO = toEventLogDO(event, keyDO);
                    eventLogMapper.insert(logDO);

                    // 7. 标记去重
                    markDedup(event.getMessageId());

                    // 8. 发MQ
                    rocketMQTemplate.convertAndSend("cdp-event-ingested", event);

                    accepted++;
                } catch (Exception e) {
                    rejected++;
                    errors.add(event.getMessageId() + ": " + e.getMessage());
                }
            }

            return new IngestionResult(accepted, rejected, errors);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private boolean isDuplicate(String messageId) {
        if (messageId == null) return false;
        String key = "canvas:event:dedup:" + messageId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void markDedup(String messageId) {
        if (messageId == null) return;
        String key = "canvas:event:dedup:" + messageId;
        redisTemplate.opsForValue().set(key, "1", Duration.ofHours(24));
    }

    private void checkRateLimit(String writeKey, int qps) {
        String key = "canvas:ratelimit:" + writeKey;
        Long current = redisTemplate.opsForValue().increment(key);
        if (current != null && current == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(1));
        }
        if (current != null && current > qps) {
            throw new RateLimitExceededException("Rate limit exceeded for writeKey: " + writeKey);
        }
    }
}
```

### 3.2 可视化埋点管理

#### 3.2.1 数据表

```sql
-- V93: Visual tracking rules
CREATE TABLE IF NOT EXISTS visual_track_rule (
  id BIGINT NOT NULL AUTO_INCREMENT,
  rule_id VARCHAR(64) NOT NULL COMMENT '规则唯一ID',
  write_key VARCHAR(64) NOT NULL COMMENT '关联的SDK writeKey',
  event_code VARCHAR(64) NOT NULL COMMENT '映射的事件编码',
  event_name VARCHAR(100) NOT NULL COMMENT '事件显示名称',
  platform VARCHAR(20) NOT NULL COMMENT '平台: web/ios/android',
  selector VARCHAR(500) NOT NULL COMMENT '元素选择器(CSS/XPath/ViewPath)',
  url_pattern VARCHAR(500) NULL COMMENT 'URL匹配模式(Web专用)，支持通配符',
  screen_pattern VARCHAR(500) NULL COMMENT '页面匹配模式(Mobile专用)',
  properties JSON NULL COMMENT '属性映射配置',
  enabled TINYINT NOT NULL DEFAULT 1,
  priority INT NOT NULL DEFAULT 0 COMMENT '优先级，数字越大越优先',
  created_by VARCHAR(64) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rule_id (rule_id),
  INDEX idx_write_key_enabled (write_key, enabled),
  INDEX idx_platform_enabled (platform, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可视化埋点规则';

-- V94: Visual tracking session (圈选会话)
CREATE TABLE IF NOT EXISTS visual_track_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id VARCHAR(64) NOT NULL COMMENT '圈选会话ID',
  write_key VARCHAR(64) NOT NULL COMMENT 'SDK writeKey',
  platform VARCHAR(20) NOT NULL COMMENT '平台',
  target_url VARCHAR(500) NULL COMMENT '目标页面URL(Web)',
  status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/completed/expired',
  created_by VARCHAR(64) NULL,
  created_at DATETIME NULL,
  expired_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_id (session_id),
  INDEX idx_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可视化埋点圈选会话';
```

#### 3.2.2 管理API

```java
@RestController
@RequestMapping("/cdp/visual-track")
@RequiredArgsConstructor
public class VisualTrackController {

    // 圈选会话管理
    @PostMapping("/sessions")
    public Mono<R<VisualTrackSessionDO>> createSession(@RequestBody CreateSessionReq req);

    @GetMapping("/sessions/{sessionId}/rules")
    public Mono<R<List<VisualTrackRuleDO>>> getSessionRules(@PathVariable String sessionId);

    // 规则CRUD
    @PostMapping("/rules")
    public Mono<R<VisualTrackRuleDO>> createRule(@RequestBody VisualTrackRuleDO rule);

    @PutMapping("/rules/{ruleId}")
    public Mono<R<Void>> updateRule(@PathVariable String ruleId, @RequestBody VisualTrackRuleDO rule);

    @DeleteMapping("/rules/{ruleId}")
    public Mono<R<Void>> deleteRule(@PathVariable String ruleId);

    @GetMapping("/rules")
    public Mono<R<PageResult<VisualTrackRuleDO>>> listRules(
            @RequestParam String writeKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size);

    // SDK拉取规则
    @GetMapping("/sdk/rules")
    public Mono<R<List<VisualTrackRuleDO>>> sdkFetchRules(
            @RequestParam String writeKey,
            @RequestParam String platform,
            @RequestParam(required = false) String urlPattern);
}
```

#### 3.2.3 圈选工具（Web端）

管理后台内嵌iframe + postMessage通信：

```
管理后台页面
  → iframe 加载目标网站（带 canvas_visual_editor=1 参数）
  → 目标网站SDK检测到 visual_editor=1
    → 加载 VisualEditor 插件
    → 进入圈选模式（元素高亮、点击选中）
  → 用户点击元素
    → SDK通过 postMessage 发送元素信息到父窗口
      { type: 'element_selected', selector: 'button.buy-btn', content: '购买', ... }
  → 管理后台接收消息
    → 显示配置面板（事件名、属性映射）
    → 保存规则到服务端
  → 服务端推送规则更新
    → SDK实时加载新规则
```

**Mobile端圈选：**

```
App内开启圈选模式（摇一摇或特定手势触发）
  → SDK加载 VisualEditorPlugin
  → 叠加透明圈选层
  → 用户触摸元素 → 高亮 → 弹出配置面板
  → 配置保存到服务端
  → 其他设备上的SDK通过轮询获取新规则
```

### 3.3 事件属性自动发现

SDK上报的事件可能包含未在 `event_definition` 中定义的属性。服务端自动发现并注册：

```java
// 在 CdpEventIngestionService.ingestBatch 中增加
private void discoverNewAttributes(TrackEvent event) {
    if (event.getType() != EventType.track) return;

    EventDefinitionDO def = eventDefinitionMapper.selectByEventCode(event.getEvent());
    if (def == null) {
        // 自动注册新事件
        def = new EventDefinitionDO();
        def.setEventCode(event.getEvent());
        def.setName(event.getEvent());
        def.setAttributes("[]");
        def.setEnabled(1);
        def.setStatus("PENDING_REVIEW");  // 待审核
        eventDefinitionMapper.insert(def);
    }

    // 检查新属性
    Set<String> knownAttrs = parseAttributeNames(def.getAttributes());
    Set<String> newAttrs = new HashSet<>(event.getProperties().keySet());
    newAttrs.removeAll(knownAttrs);

    if (!newAttrs.isEmpty()) {
        // 注册新属性为待审核
        for (String attr : newAttrs) {
            registerPendingAttribute(def.getId(), attr, inferType(event.getProperties().get(attr)));
        }
    }
}
```

**event_definition 表扩展：**

```sql
-- V95: Event definition status for auto-discovery
ALTER TABLE event_definition
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PENDING_REVIEW/DISABLED',
  ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/AUTO_DISCOVERED/IMPORTED',
  ADD INDEX idx_status (status);

-- V96: Event attribute auto-discovery
CREATE TABLE IF NOT EXISTS event_attr_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id BIGINT NOT NULL COMMENT '关联event_definition.id',
  attr_name VARCHAR(100) NOT NULL COMMENT '属性名',
  display_name VARCHAR(100) NULL COMMENT '显示名称',
  attr_type VARCHAR(20) NOT NULL DEFAULT 'STRING' COMMENT 'STRING/NUMBER/BOOLEAN/DATE',
  required TINYINT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PENDING_REVIEW/DISABLED',
  source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/AUTO_DISCOVERED',
  first_seen_at DATETIME NULL COMMENT '首次发现时间',
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_event_attr (event_id, attr_name),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件属性定义（含自动发现）';
```

---

## 四、跨系统设计

### 4.1 事件协议统一

所有SDK（Web/iOS/Android）和服务端共享统一的事件协议：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| messageId | string(UUID) | 是 | 事件唯一ID，去重用 |
| type | enum | 是 | track/identify/page/group/alias |
| event | string | 条件必填 | type=track时必填 |
| userId | string | 否 | 实名用户ID |
| anonymousId | string | 是 | 匿名用户ID |
| properties | object | 否 | 事件属性（track/page） |
| traits | object | 否 | 用户属性（identify/group） |
| previousId | string | 否 | 原ID（alias） |
| groupId | string | 否 | 分组ID（group） |
| context | object | 否 | SDK上下文 |
| timestamp | ISO8601 | 是 | 客户端事件时间 |
| sentAt | ISO8601 | 是 | 客户端发送时间 |

### 4.2 实施顺序

```
Phase 0 (1-2周): 服务端接收管线
  → V91-V96 Flyway迁移
  → CdpEventIngestionController + Service
  → writeKey管理CRUD
  → 限流+去重+MQ发送
  → 兼容现有 /canvas/events/report

Phase 1 (2-3周): Web JS SDK
  → 核心管线 + 事件队列 + 批量发送
  → 代码埋点 API
  → 全埋点 (pageview/click/stay)
  → 隐私合规 (延迟初始化/opt-out)
  → npm包发布

Phase 2 (2-3周): 可视化埋点
  → 管理后台圈选页面
  → SDK VisualTracker + VisualEditor
  → 规则CRUD + SDK拉取
  → 规则热更新

Phase 3 (3-4周): Mobile SDK
  → iOS SDK (Swift) 核心管线 + 全埋点
  → Android SDK (Kotlin) 核心管线 + 全埋点
  → Mobile可视化埋点
  → CocoaPods/Maven发布

Phase 4 (1-2周): 事件属性自动发现
  → 上报时自动检测新属性
  → 待审核属性管理页面
  → 属性类型推断
```

### 4.3 与现有系统的集成点

| 集成点 | 现有代码 | SDK对接方式 |
|--------|---------|------------|
| 事件上报 | `EventDefinitionController.reportEvent()` | 新增 `/cdp/events/track`，不修改现有接口 |
| 事件定义 | `EventDefinitionDO` + `EventDefinitionMapper` | 扩展 status/source 字段，新增 `event_attr_definition` 表 |
| 事件日志 | `EventLogDO` + `EventLogMapper` | 扩展列（message_id, event_type, anonymous_id等） |
| 画布触发 | `TrackEventHandler` + Disruptor | `/cdp/events/track` 写event_log后发MQ，画布EVENT_TRIGGER节点消费MQ |
| CDP标签 | `CdpTagService.setTag()` | identify事件可触发标签更新 |
| CDP画像 | `cdp_user_profile.properties_json` | identify事件可触发画像属性更新 |
| 人群圈选 | `AudienceBitmapStore` + RoaringBitmap | 实时人群消费MQ事件，评估规则更新bitmap |
