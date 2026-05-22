# 灰度发布 UI 设计（优化点 #11）

## 背景

后端灰度发布能力完整：
- `POST /canvas/{id}/canary?percent=N` — 从当前草稿创建灰度版本，按 hash 分桶将 N% 流量导入灰度
- `POST /canvas/{id}/promote-canary` — 晋升灰度版本为正式版本
- `POST /canvas/{id}/rollback-canary` — 回滚灰度，恢复全量走正式版本
- `resolveVersionId()` — `hash(userId:canvasId) % 100 < canaryPercent` 决定走哪个版本

前端缺少：TypeScript 类型字段、`rollbackCanary` API、全部灰度 UI 入口。

---

## 灰度状态机

```
PUBLISHED（无灰度）
    ↓ 点击"灰度发布" → 填写比例
PUBLISHED（灰度中）: publishedVersionId=A, canaryVersionId=B, canaryPercent=N%
    ↓ 点击"晋升全量"         ↓ 点击"回滚"
PUBLISHED（无灰度）         PUBLISHED（无灰度）
publishedVersionId=B        publishedVersionId=A（不变）
canaryVersionId=null        canaryVersionId=null
```

---

## 边界情况

| 场景 | 处理 |
|------|------|
| 草稿为空（无法创建灰度版本） | "灰度发布"按钮置灰，tooltip："请先保存草稿" |
| 已有灰度时再次点击"灰度发布" | 弹窗提示"当前已有灰度版本，是否覆盖？" |
| 画布未发布（DRAFT/OFFLINE）| 不显示灰度相关按钮 |
| canaryPercent = 0 或 100 | 前端校验禁止，必须在 1~99 之间 |
| 灰度中发布新草稿 | 允许，晋升时以最新草稿为准（后端逻辑） |
| 正在执行中的 execution | 灰度只影响新触发，不中断已运行实例 |

---

## UI 设计

### 编辑页顶栏

**无灰度状态（已发布）**：
```
[保存] [发布] [灰度发布▾]
```

**灰度中状态**：
```
[保存]  🟡 灰度中 20%  [晋升全量] [回滚]
```

- 灰度比例 badge 颜色：黄色（提醒注意）
- "灰度发布" 仅在 `status === 1`（已发布）且存在草稿时显示
- "晋升全量"/"回滚" 仅在 `canaryVersionId != null` 时显示

### 开始灰度弹窗

```
灰度发布

灰度比例: [────●────] 20%   1% ─── 99%

说明：
  · 20% 的用户将收到新版本画布
  · 用户分配基于 hash，同一用户始终命中同一版本
  · 灰度期间可随时晋升或回滚

[取消]  [确认灰度发布]
```

已有灰度时追加警告：
```
⚠ 当前已有灰度版本（20%），确认将覆盖为新灰度？
```

---

## 前端改动清单

### 1. TypeScript 类型补充（types/index.ts）

```typescript
export interface Canvas {
  // ... 现有字段 ...
  canaryVersionId?: number
  canaryPercent?: number
}
```

### 2. API 补充（services/api.ts）

```typescript
rollbackCanary: (id: number) =>
  http.post<R<void>, R<void>>(`/canvas/${id}/rollback-canary`),
```

### 3. 编辑页改动（canvas-editor/index.tsx）

- 从 `detail.canvas` 读取 `canaryVersionId` / `canaryPercent`
- State：`const [canaryPercent, setCanaryPercent] = useState(50)`
- State：`const [canaryModalOpen, setCanaryModalOpen] = useState(false)`
- `handleStartCanary()`：调用 `canvasApi.canary(id, percent)`，成功后刷新 detail
- `handlePromoteCanary()`：Modal.confirm → `canvasApi.promoteCanary(id)` → 刷新
- `handleRollbackCanary()`：Modal.confirm → `canvasApi.rollbackCanary(id)` → 刷新
- 顶栏根据 `canvas.canaryVersionId` 条件渲染：灰度按钮组 vs 普通发布按钮

---

## 不在范围内

- 灰度人群白名单（当前是 hash 分桶，未来可扩展为指定 userId 列表）
- 灰度期间的效果对比看板（A/B 数据分析）
- 灰度版本与正式版本执行数据的分层统计
