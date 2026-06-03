# PRD

> 本文档为营销画布平台产品需求文档，对应 BMAD 产品设计审查报告 — 前端UX微交互补齐（表单UX）

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-57 |
| **需求名称** | 未保存变更导航警告 — audience-edit 无未保存变更导航警告 |
| **优先级** | P2 |
| **所属类别** | 前端UX微交互 — 表单UX |
| **提出日期** | 2026-06-02 |
| **来源** | BMAD 产品设计审查报告 — 二次审核新增项 |
| **竞品对标** | 前端框架（React Router 6 + React 18）提供的路由守卫能力 |

---

## 1. 问题描述

### 1.1 现状

当前受众编辑页面（`audience-edit.tsx`）**完全缺乏未保存变更保护**，用户在未保存的情况下切换路由（如返回列表页、访问其他页面）会导致已编辑的受众配置数据丢失。

虽然点击 "保存" 或 "取消" 按钮可以正常返回，但用户**无法察觉**自己是否仍有未保存的变更，完全依赖手动操作，体验与上一个 PRD P2-56 类似但范围更窄。

### 1.2 痛点

- **无预判机制**：用户不知道切换页面时当前编辑是否已保存
- **手动点击保存**：必须主动点击 "保存" 才能离开，增加操作负担
- **误操作返回列表页**：直接点击浏览器返回按钮，受众配置被丢弃
- **用户困惑**：受众编辑页面无 "已保存" 状态显示，用户完全盲操作

### 1.3 竞品对标

| 应用 | 能力 |
|------|------|
| Froala Editor | 编辑中有未保存变更时切换主题会提示 "您有未保存的内容" |
| TinyMCE | 自动保存 + 离开页面确认 |
| React Router 6 | useLocation 用法做页面级别状态管理，可用 useEffect 实现路由守卫 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为受众管理员，我希望在未保存的情况下切换页面时获得提示，以免离开页面后丢失已编辑的受众配置数据。

### 2.2 成功指标

- 受众编辑页面实现 `beforeunload` 警告（与 PRD-P2-56 类似）
- 或通过 React Router 路由守卫实现离开发送页面的确认逻辑
- 用户离开页面前未保存变更的丢失率降低 100%

### 2.3 不做会怎样

- 用户在未保存状态点击返回按钮或导航到其他页面，受众配置丢失
- 无任何保护提示，用户完全依赖记忆手动保存
- 形成与 PRD-P2-56 类似的数据安全漏洞

---

## 3. 功能需求

### 3.1 核心功能

1. **配置变更追踪**：追踪受众字段的每处修改（名称、标签、规则、人群策略等）
2. **未保存状态显示**：使用 Prompt 或 BeforeUnload 警告文本显示未保存提示
3. **路由守卫**：捕获用户离开受众编辑页的请求（按钮点击、浏览器返回、链接导航）
4. **状态持久化**：即使在切换路由前被中断，未保存状态可通过 URL 参数或本地存储恢复

### 3.2 详细描述

#### 3.2.1 变更追踪范围

受众编辑包含多个可变字段，需要追踪以下变更：

- **基本信息**：受众名称、描述、标签
- **规则条件**：IF 条件组合（`condition: IF_CONDITION`）
- **频率限制**：FREQUENCY_CAP 配置
- **人群策略**：ONLINE/HYBRID/COHORT 策略
- **模板关联**：是否关联受众模板
- **静态人数**：手动指定人数
- **可选字段**：头像、调用背景图等

#### 3.2.2 实现方式选择

**方式 A：BeforeUnload 警告（推荐）**
- 支持 Chrome/Firefox/Safari/Edge
- 浏览器原生支持，无需额外依赖
- 与 PRD-P2-56 保持一致性

**方式 B：React Router Prompt 组件**
- React Router 6 提供的 `<Prompt when={...}>` 组件
- 适合应用内部路由跳转（如点击列表页按钮）
- 不适合浏览器原生导航（前进/后退按钮）

**方式 C：Custom Hook 路由防护**
- 使用 `useLocation` + `useNavigate`
- 完全自定义确认逻辑
- 支持复杂场景（如两层导航模式）

**推荐**：方式 A + 方式 C 组合
- BeforeUnload 覆盖浏览器原生导航
- Custom Hook 覆盖应用内部路由跳转

#### 3.2.3 变更状态管理

```typescript
// hooks/useAudienceChanges.ts
interface AudienceChanges {
  hasChanges: boolean;
  changedFields: string[];
  dirty: boolean;
}

export const useAudienceChanges = (initialValues: Audience) => {
  const [hasChanges, setHasChanges] = useState(false);
  const [changedFields, setChangedFields] = useState<string[]>([]);
  const dirty = useRef(false);

  const markFieldAsChanged = (fieldId: string) => {
    setHasChanges(true);
    setChangedFields(prev => [
      ...prev.filter(f => f !== fieldId),
      fieldId,
    ]);
    dirty.current = true;
  };

  const markAsClean = () => {
    setHasChanges(false);
    setChangedFields([]);
    dirty.current = false;
  };

  return {
    hasChanges,
    changedFields,
    dirty: dirty.current,
    markFieldAsChanged,
    markAsClean,
  };
};
```

### 3.3 交互流程

1. 用户进入受众编辑页
2. 系统使用当前表单值初始化变更追踪器，初始状态 "未变更"
3. 用户修改任何受监控字段（如受众名称）
4. 系统 "标记字段变更"（`markFieldAsChanged('name')`）
5. 离开发送页：
   - **场景 1**（应用内导航）：点击 "返回列表" 按钮 → 全局 Prompt 组件拦截 → 弹出确认框 "您有未保存的受众配置，确定离开吗？" → [取消] 或 [离开]
   - **场景 2**（浏览器导航）：点击浏览器的退回按钮 → BeforeUnload 事件触发 → 浏览器原生弹窗 "您有未保存的变更，确定要离开吗？"
6. 用户点击 "保存" → 清除所有未保存标记 → 返回列表页无确认提示

---

## 4. 非功能需求

- **性能要求**：变更检知的即时性（用户输入时 100ms 内响应）
- **状态恢复**：页面刷新后，"未保存" 状态通过 URL 参数 `?unsaved=true` 或本地存储 `localStorage` 恢复
- **兼容性**：支持所有现代浏览器
- **无障碍性**：清晰、简洁的确认文本，屏幕阅读器可正确朗读

---

## 5. 验收标准

- [ ] 新增 `useAudienceChanges` Hook 追踪受众编辑页变更
- [ ] 监涉以下字段的变更：
  - [ ] 受众名称
  - [ ] 受众描述
  - [ ] 标签
  - [ ] IF 条件规则
  - [ ] FREQUENCY_CAP 配置
  - [ ] 人群策略（ONLINE/HYBRID/COHORT）
  - [ ] 模板关联
- [ ] 未保存时点击 "返回列表" 按钮触发全局 Prompt 确认
- [ ] 未保存时点击浏览器归回按钮触发 BeforeUnload 警告
- [ ] 已保存状态离开页面不触发确认框
- [ ] 用户点击 "取消" 并清空表单 → 返回列表页无确认
- [ ] 页面刷新后，显示红色 "您有未保存的变更" 提示（如 Figma 提供）

---

## 6. 技术建议

### 6.1 涉及模块

- **前端模块**：
  - `frontend/src/pages/audience-edit/`（受众编辑主页面）
  - `frontend/src/hooks/useAudienceChanges.ts`（新增变更追踪 Hook）
  - `frontend/src/components/AudienceChangePrompt/`（新增路由守卫组件）

### 6.2 技术要点

#### 6.2.1 受众表单规范化

受众编辑使用 `react-hook-form` + `@kyvg/vue3-form` 或类似表单库，建议：

1. 初始化 `defaultValues` 时使用后端 API 返回的完整受众对象
2. 使用 `watch` 或 `useFormState` 监听表单字段变化
3. 每次字段变化触发 `markFieldAsChanged(fieldId)`

#### 6.2.2 BeforeUnload + Router Prompt 组合

```typescript
// components/AudienceChangePrompt.tsx
import { Prompt } from 'react-router-dom';
import { useLocation } from 'react-router-dom';

export const AudienceChangePrompt = ({ hasChanges }: { hasChanges: boolean }) => {
  const location = useLocation();
  const navigate = useNavigate();

  const handleBlock = (nextLocation: Location) => {
    if (hasChanges) {
      return ''; // 触发浏览器 BeforeUnload 警告
    }
    return true; // 允许导航
  };

  // 浏览器导航（前进/后退按钮）由 BeforeUnload 处理
  // 应用内导航由 React Router Prompt 处理

  return (
    <>
      <Prompt when={hasChanges} message="您有未保存的受众配置，确定离开吗？">
        {({ when, render }) => (
          <>{when && render({ onNavigate: () => false })}</>
        )}
      </Prompt>

      {/* BeforeUnload 在父组件 实现 */}
    </>
  );
};
```

#### 6.2.3 状态持久化

```typescript
// utils/audiencePersistence.ts
export const saveUnsavedState = (audienceId: string, formData: Audience) => {
  localStorage.setItem(`audience-unsaved-${audienceId}`, JSON.stringify(formData));
};

export const loadUnsavedState = (audienceId: string) => {
  const saved = localStorage.getItem(`audience-unsaved-${audienceId}`);
  return saved ? JSON.parse(saved) : null;
};
```

### 6.3 预估工作量

- **开发**：4 人天
  - 2 人天：`useAudienceChanges` Hook 实现 + 状态持久化
  - 1 人天：路由守卫组件 + 与 React Router 集成
  - 1 人天：BeforeUnload 实现（复用 PRD-P2-56 逻辑）
- **测试**：2 人天
  - 功能测试（返回列表、浏览器归回、动画取消）
  - 状态恢复测试（刷新页面、关闭重新打开）
- **总计**：6 人天

---

## 7. 依赖与风险

### 7.1 前置依赖

- React Router 6 已安装（当前项目使用 `react-router-dom@6.x`）
- `react-hook-form` 已在受众编辑中使用（无需额外依赖）
- 与 PRD-P2-56 共享 `useUnsavedChanges` Hook（可复用）

### 7.2 风险

| 风险 | 级别 | 缓解措施 |
|------|------|----------|
| React Router Prompt 兼容性问题 | 低 | 降级为纯 BeforeUnload 实现，或使用自定义 Hook |
| 多级导航（如编辑 → 查看受众 → 返回编辑）导致状态丢失 | 中 | 使用 URL 参数 `?edit=true&unsaved=true` 传递状态 |
| localStorage 容量限制（1-5MB） | 低 | 仅存储受众配置 JSON，不超过几 KB |
| HMR 热替换时状态丢失 | 中 | 禁用 HMR 或在 HMR 重建后恢复状态 |
| 屏幕阅读器无障碍提示不清晰 | 低 | 使用 aria-live 区域反馈未保存状态 |

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 二次审核新增项
- React Router v6 Prompt: https://reactrouter.com/main/components/prompt
- BeforeUnload (Observer): https://kentcdodds.com/blog/use-beforeunload-with-react-router
