# PRD

> 本文档为营销画布平台产品需求文档，对应 BMAD 产品设计审查报告 — 前端UX微交互补齐（表单UX）

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-56 |
| **需求名称** | BeforeUnload 警告 — 关闭浏览器标签丢失未保存工作 |
| **优先级** | P2 |
| **所属类别** | 前端UX微交互 — 表单UX |
| **提出日期** | 2026-06-02 |
| **来源** | BMAD 产品设计审查报告 — 二次审核新增项 |
| **竞品对标** | 有经验的产品（如 Notion、Figma 等）均提供保存状态提醒 |

---

## 1. 问题描述

### 1.1 现状

当前画布编辑器（`canvas-editor.tsx` 及相关组件）在用户修改内容后，**没有任何自动保存或手动保存按钮**。用户在未保存的情况下关闭浏览器标签页时，已进入的编辑工作会**完全丢失**，没有任何提示或恢复机制。

### 1.2 痛点

- **用户丢失工作**：误操作关闭标签页，辛辛苦苦创建的营销画布配置瞬间消失
- **无心理预期**：用户不知道是否已保存，以为已完成工作但实际上未写入数据库
- **重复劳动**：已编辑的画布需要重新从头开始，严重影响生产效率
- **用户不信任**：无法感知系统保存状态，导致用户对平台可靠性存疑

### 1.3 竞品对标

| 竞品 | 能力 |
|------|------|
| Notion | 自动保存 + Toast 提示 "已保存" 或 "版本历史" |
| Figma | 实时协作 + 保存状态指示器 + 危险操作确认 |
| Miro | 自动保存 + 暂停/恢复按钮 + 离线状态显示 |
| Jira | 工作区标签提示 "未保存" + 自动保存倒计时 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为画布管理员，我希望在关闭浏览器标签页前获得未保存工作提示，以便在误操作丢失数据前有机会保存或回退。

### 2.2 成功指标

- 所有画布编辑页面实现 `beforeunload` 警告（包括：画布编辑器、受众编辑、节点配置等）
- 首次打开页面显示自动保存状态（"已保存" / "正在保存" / "未保存"）
- 误关闭标签页导致的数据丢失率降低 100%（从 100% → 0%）

### 2.3 不做会怎样

- 用户误关闭标签页后，已编辑内容彻底丢失，无任何恢复入口
- 用户无法感知保存状态，导致对平台可靠性的严重质疑
- 形成用户体验重大漏洞，降低用户对产品的信任度

---

## 3. 功能需求

### 3.1 核心功能

1. **BeforeUnload 警告**：页面卸载时检查是否有未保存变更，若有则显示确认对话框
2. **自动保存状态指示器**：页面顶部显示当前的保存状态（已保存 / 正在保存 / 未保存）
3. **手动保存按钮**：提供显式保存入口，确保关键变更可立即保存
4. **变更痕迹追踪**：追踪画布配置的变更，判断是否需要触发 beforeunload 警告

### 3.2 详细描述

#### 3.2.1 BeforeUnload 警告机制

- 页面 `beforeunload` 事件监听，检查是否存在未保存变更
- 未保存变更信号由变更追踪器提供（JSON 轻量级 diff）
- 警告对话框支持浏览器原生对话框，避免覆盖自身UI逻辑
- 允许用户选择："保留未保存内容"（忽略 beforeunload，直接关闭）、"取消关闭"（保持页面打开）

#### 3.2.2 自动保存状态指示器

- **已保存**（绿色对勾）：页面加载完成且最后一次保存成功
- **正在保存**（蓝色旋转图标）：用户编辑触发自动保存，数据库写入中
- **未保存**（黄色警告图标）：用户已编辑但尚未保存

状态变化逻辑：
1. 页面加载 → 检查最后保存时间，显示 "已保存"
2. 用户修改画布配置 → 更新内部变更标记，显示 "未保存"
3. 自动保存触发（延时 3 秒无编辑）→ 显示 "正在保存" → 保存完成显示 "已保存"
4. 保存失败 → 显示 "保存失败"（红色），保留在 "未保存" 状态

#### 3.2.3 变更追踪器实现

- 维护 `modified: boolean` 状态
- 监听 React 状态变化（如 `canvasNodes`、`canvasConfig`）
- 使用 `useEffect` 跟踪用户编辑动作
- 关键变更事件触发：
  - 节点添加/删除/修改
  - 画布配置更新（名称、描述、触发器等）
  - 连线重连/断开

### 3.3 交互流程

1. 用户打开画布编辑器
2. 系统显示 "已保存" 状态指示器
3. 用户编辑画布（拖动节点、修改配置等）
4. 状态变为 "未保存"，页面标题追加 " *"
5. 用户停止编辑 3 秒，触发自动保存
6. 保存成功，显示 "已保存"
7. 保存过程中显示 "正在保存"
8. 用户尝试关闭标签页，系统弹出确认框：
   - "您有未保存的变更，确定要离开吗？"
   - [取消] → 保持页面打开
   - [离开] → 前往浏览器默认行为（关闭页面）

---

## 4. 非功能需求

- **性能要求**：BeforeUnload 事件监听器不阻塞页面加载（使用 `useEffect` cleanup）
- **浏览器兼容性**：支持 Chrome 72+、Firefox 65+、Safari 12.1+、Edge 79+
- **无障碍性**：状态指示器使用 aria-live 通知屏幕阅读器
- **离线支持**：无网络时仍可检测变更，离线期间显示 "本地未保存"，恢复网络后提示保存

---

## 5. 验收标准

- [ ] 所有画布编辑相关页面实现 BeforeUnload 警告（7 个页面）
  - [ ] 画布编辑器 (`/canvas/:canvasId`)
  - [ ] 受众编辑 (`/audience/:audienceId`)
  - [ ] 节点配置面板（所有节点类型）
  - [ ] 发布配置页（`/canvas/:canvasId/publish`）
  - [ ] 模板编辑页（`/template/:templateId`）
  - [ ] 旅程编辑页（`/journey/:journeyId`）
  - [ ] 定时任务编辑页（`/schedule/:scheduleId`）
- [ ] 自动保存状态指示器在所有上述页面正常显示
- [ ] 状态切换正确："已保存" → "正在保存" → "已保存"
- [ ] 尝试关闭页面时，有未保存变更则弹出原生确认框
- [ ] 无未保存变更时，关闭页面无确认框
- [ ] 离线状态下，仍能检测变更并显示 "未保存"
- [ ] 手动保存按钮可立即触发保存，消除 "未保存" 状态

---

## 6. 技术建议

### 6.1 涉及模块

- **前端模块**：
  - `frontend/src/pages/canvas-editor/`（根组件）
  - `frontend/src/pages/audience-edit/`（受众编辑）
  - `frontend/src/components/NodeConfig/`（节点配置面板）
  - `frontend/src/components/SaveIndicator/`（新增保存状态指示器组件）

### 6.2 技术要点

#### 6.2.1 BeforeUnload 实现方式

```typescript
// hooks/useUnsavedChanges.ts
import { useEffect } from 'react';

interface UseUnsavedChangesOptions {
  hasUnsavedChanges: boolean;
}

export const useUnsavedChanges = (options: UseUnsavedChangesOptions) => {
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (options.hasUnsavedChanges) {
        e.preventDefault();
        e.returnValue = ''; // Chrome 要求返回值
        return '';
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [options.hasUnsavedChanges]);
};
```

#### 6.2.2 变更追踪器

```typescript
// components/SaveIndicator/types.ts
export type SaveState = 'saved' | 'saving' | 'unsaved' | 'failed';

interface SaveIndicatorProps {
  state: SaveState;
  onManualSave: () => void;
}

// hooks/useSaveState.ts
import { useState, useCallback } from 'react';

export const useSaveState = () => {
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [saveState, setSaveState] = useState<SaveState>('saved');

  const markUnsaved = useCallback(() => {
    setHasUnsavedChanges(true);
    setSaveState('unsaved');
  }, []);

  const startSaving = useCallback(() => {
    setSaveState('saving');
  }, []);

  const markSaved = useCallback(() => {
    setHasUnsavedChanges(false);
    setSaveState('saved');
  }, []);

  const markFailed = useCallback(() => {
    setSaveState('failed');
  }, []);

  return {
    hasUnsavedChanges,
    saveState,
    markUnsaved,
    startSaving,
    markSaved,
    markFailed,
  };
};
```

#### 6.2.3 自动保存触发器

```typescript
// hooks/useAutoSave.ts
import { useEffect, useRef } from 'react';

export const useAutoSave = (
  onSave: () => Promise<void>,
  delayMs: number = 3000
) => {
  const saveTimeoutRef = useRef<NodeJS.Timeout>();
  const isSavingRef = useRef(false);

  useEffect(() => {
    if (isSavingRef.current) return;

    saveTimeoutRef.current = setTimeout(async () => {
      try {
        await onSave();
      } catch (error) {
        console.error('自动保存失败:', error);
      }
    }, delayMs);

    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }
    };
  }, []);
};
```

### 6.3 预估工作量

- **开发**：5 人天
  - 3 人天：SaveIndicator 组件 + 变更追踪器
  - 1 人天：BeforeUnload 集成到 7 个页面
  - 1 人天：自动保存逻辑 + 手动保存按钮
- **测试**：2 人天
  - 功能测试（所有页面、所有状态）
  - 边界测试（离线、保存失败、快速切换页面）
- **总计**：7 人天

---

## 7. 依赖与风险

### 7.1 前置依赖

- 无前置依赖
- 后端已具备完整的 CRUD API，无需修改后端

### 7.2 风险

| 风险 | 级别 | 缓解措施 |
|------|------|----------|
| BeforeUnload 浏览器策略限制，无法完全阻止关闭 | 低 | 浏览器策略层面限制，仅提供警告提示 |
| 自动保存触发时机误判（频繁触发导致性能问题） | 中 | 增加防抖延迟（3秒），保存中忽略后续触发 |
| 离线状态检测和恢复逻辑复杂 | 中 | 先实现离线检测（navigator.onLine），离线期间仅提示，恢复网络后提示保存 |
| 状态指示器视觉干扰用户编辑 | 低 | 使用非侵入式样式（右上角徽章），避免影响主操作区 |

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 二次审核新增 17 项前端UX微交互
- Crumb: https://reactcrumb.com/docs/sqlite-tutorial.html?sig=beforeunload
- BeforeUnload Event Specification: https://html.spec.whatwg.org/multipage/browsing-the-web/webappapis.html#beforeunload
