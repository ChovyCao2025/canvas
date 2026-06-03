# PRD

> 本文档为营销画布平台产品需求文档，对应 BMAD 产品设计审查报告 — 前端UX微交互补齐

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-58 |
| **需求名称** | 自动保存状态指示器 — 画布编辑器无自动保存状态显示 |
| **优先级** | P2 |
| **所属类别** | 前端UX微交互 — 表单UX |
| **提出日期** | 2026-06-02 |
| **来源** | BMAD 产品设计审查报告 — 二次审核新增项 |
| **竞品对标** | Notion, Figma, Miro |

---

## 1. 问题描述

### 1.1 现状

画布编辑器（`canvas-editor.tsx`）缺乏自动保存状态的显式显示，用户无法感知：
- 画布当前是 "已保存"、"正在保存" 还是 "未保存"
- 自动保存是否正常工作
- 是否需要手动点击保存按钮

### 1.2 痛点

- **无心理预期**：用户不知道画布数据是否已安全写入数据库
- **无法判断状态**：出现异常（如保存失败）时用户无感知
- **手动保存负担**：担心数据丢失，用户可能频繁点击保存按钮，降低效率
- **协作冲突风险**：多人协作时无保存状态提示，可能导致数据覆盖

### 1.3 竞品对标

| 应用 | 能力 |
|------|------|
| Notion | 页面标题末尾 "已保存" 点对勾，或顶部 Toast 提示 "已保存至数据库" |
| Figma | 右上角显示 "已保存"，离线时显示 "Unsaved" |
| Miro | 自动保存 + 暂停/恢复按钮 + 离线状态显示 |
| Google Docs | 顶部显示 "您有未保存的更改" |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为画布管理员，我希望在画布编辑器顶部看到清晰的保存状态指示器，以便了解当前数据是否已安全保存。

### 2.2 成功指标

- 画布编辑器右上角显示自动保存状态（已保存/正在保存/未保存/保存失败）
- 保存状态每秒刷新一次（在保存完成后）
- 误操作触发自动保存时，用户能立即感知状态变化
- 用户无保存负担，成功率从当前 ~30%（手动保存率）提升至 95%（自动保存率）

### 2.3 不做会怎样

- 用户无感知系统保存状态，只能依赖手动保存
- 出现保存失败等异常时用户无法及时察觉
- 误操作关闭浏览器标签页导致数据丢失（与 PRD-P2-56 联动）

---

## 3. 功能需求

### 3.1 核心功能

1. **保存状态指示器组件**：显示当前保存状态（图标 + 文本）
2. **自动保存机制**：用户停止编辑 3 秒后自动触发保存
3. **手动保存按钮**：提供显式保存入口，位于保存状态指示器旁
4. **状态实时反馈**：保存过程中的状态切换可实时反映在界面上

### 3.2 详细描述

#### 3.2.1 状态类型定义

```typescript
// types/saveState.ts
export enum SaveState {
  SAVED = 'saved',           // 已保存
  SAVING = 'saving',         // 正在保存
  UNSAVED = 'unsaved',       // 未保存
  FAILED = 'failed',         // 保存失败
  OFFLINE = 'offline',       // 离线模式
}
```

#### 3.2.2 状态指示器实现

```typescript
// components/SaveIndicator.tsx
import { SaveState } from './types';
import { SaveOutlined, LoadingOutlined, WarningOutlined, CloseOutlined } from '@ant-design/icons';

interface SaveIndicatorProps {
  state: SaveState;
  hasUnsavedChanges: boolean;
  onManualSave: () => void;
  onClick?: () => void; // 允许点击传递自定义操作
}

export const SaveIndicator: React.FC<SaveIndicatorProps> = ({
  state,
  hasUnsavedChanges,
  onManualSave,
  onClick,
}) => {
  const handleClick = () => {
    onClick?.();
    if (!hasUnsavedChanges) {
      onManualSave();
    }
  };

  const renderIcon = () => {
    switch (state) {
      case SaveState.SAVED:
        return <SaveOutlined style={{ color: '#52c41a' }} />;
      case SaveState.SAVING:
        return <LoadingOutlined spin style={{ color: '#1890ff' }} />;
      case SaveState.UNSAVED:
        return <WarningOutlined style={{ color: '#faad14' }} />;
      case SaveState.FAILED:
        return <CloseOutlined style={{ color: '#ff4d4f' }} />;
      case SaveState.OFFLINE:
        return <AlertOutlined style={{ color: '#8c8c8c' }} />;
      default:
        return null;
    }
  };

  const renderText = () => {
    switch (state) {
      case SaveState.SAVED:
        return '已保存';
      case SaveState.SAVING:
        return '正在保存...';
      case SaveState.UNSAVED:
        return '未保存';
      case SaveState.FAILED:
        return '保存失败，点击重试';
      case SaveState.OFFLINE:
        return '离线模式';
      default:
        return '';
    }
  };

  return (
    <Badge
      count={
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, cursor: 'pointer' }}>
          {renderIcon()}
          <span>{renderText()}</span>
        </div>
      }
      onClick={handleClick}
      style={{ cursor: 'pointer' }}
    >
      {/* 画布编辑器的内容区域 */}
    </Badge>
  );
};
```

#### 3.2.3 自动保存逻辑

```typescript
// hooks/useAutoSave.ts
import { useEffect, useRef, useCallback } from 'react';
import { SaveState } from '../types';

interface UseAutoSaveProps {
  saveCanvas: (canvasId: string, config: CanvasConfig) => Promise<void>;
  delayMs?: number;
  enabled?: boolean;
}

export const useAutoSave = ({
  saveCanvas,
  delayMs = 3000,
  enabled = true,
}: UseAutoSaveProps) => {
  const isSavingRef = useRef(false);
  const saveTimeoutRef = useRef<NodeJS.Timeout>();
  const canvasIdRef = useRef<string | null>(null);
  const currentConfigRef = useRef<CanvasConfig | null>(null);

  const triggerAutoSave = useCallback(async () => {
    if (!isSavingRef.current && canvasIdRef.current && enabled) {
      try {
        isSavingRef.current = true;
        await saveCanvas(canvasIdRef.current, currentConfigRef.current!);
      } catch (error) {
        console.error('自动保存失败:', error);
        // 上报保存失败到监控系统
      } finally {
        isSavingRef.current = false;
      }
    }
  }, [saveCanvas, enabled]);

  useEffect(() => {
    if (!enabled) return;

    saveTimeoutRef.current = setTimeout(() => {
      triggerAutoSave();
    }, delayMs);

    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }
    };
  }, [delayMs, triggerAutoSave]);

  return {
    setSaveContext: (canvasId: string, config: CanvasConfig) => {
      canvasIdRef.current = canvasId;
      currentConfigRef.current = config;
    },
    isSaving: isSavingRef.current,
  };
};
```

#### 3.2.4 离线模式支持

```typescript
// hooks/useOfflineStatus.ts
export const useOfflineStatus = () => {
  const [isOffline, setIsOffline] = useState(!navigator.onLine);

  useEffect(() => {
    const handleOnline = () => {
      setIsOffline(false);
      // 恢复网络后，如果有未保存数据，提示保存
      if (hasUnsavedData()) {
        Modal.confirm({
          title: '网络已恢复',
          content: '您有未保存的数数据，是否立即保存？',
          onOk: () => triggerAutoSave(),
        });
      }
    };

    const handleOffline = () => {
      setIsOffline(true);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  return isOffline;
};
```

### 3.3 交互流程

1. 用户进入画布编辑页，系统调用 API 加载画布配置
2. 自动保存逻辑初始化，显示 "已保存" 状态
3. 用户编辑画布（拖动节点、修改配置等）
4. 停止编辑 3 秒后，`markUnsaved()` 触发 → 显示 "未保存"
5. 3 秒无编辑后，`triggerAutoSave()` 触发 → 状态变为 "正在保存" → 模拟加载动画
6. 保存成功 → 状态变为 "已保存" + 绿色对勾
7. 保存过程中用户继续编辑 → 不触发新的自动保存（防抖）
8. 保存失败 → 状态变为 "保存失败" + 失败图标，用户点击可重试
9. 离线时 → 显示 "离线模式" → 用户可继续编辑，恢复网络后提示保存

---

## 4. 非功能需求

- **性能要求**：状态指示器使用轻量级组件，不阻塞主线程
- **响应性**：保存状态切换 100ms 内反映在界面上
- **离线支持**：离线状态下仍可显示 "离线模式" 状态
- **无障碍性**：aria-live 区域反馈保存状态，使用语义化 HTML
- **移动端**：适配移动端单列布局，状态指示器移至顶部工具栏

---

## 5. 验收标准

- [ ] 新增 `SaveIndicator` 组件支持 5 种状态（已保存/正在保存/未保存/保存失败/离线模式）
- [ ] 自动保存逻辑正确（停止编辑 3 秒后触发）
- [ ] 状态切换实时反映在界面上（无明显延迟）
- [ ] 离线模式下显示 "离线模式" 状态
- [ ] 网络恢复后，如有未保存数据，弹窗提示保存
- [ ] 手动保存按钮可立即触发保存
- [ ] 保存失败时可重试（点击失败图标）
- [ ] 移动端适配（状态指示器不遮挡内容区）

---

## 6. 技术建议

### 6.1 涉及模块

- **前端模块**：
  - `frontend/src/components/SaveIndicator/`（新增组件）
  - `frontend/src/hooks/useAutoSave.ts`（新增 Hook）
  - `frontend/src/hooks/useOfflineStatus.ts`（新增 Hook）
  - `frontend/src/pages/canvas-editor/`（集成状态指示器）

### 6.2 技术要点

1. **状态持久化**：使用 URL 参数 `?status=unsaved` 或 localStorage 恢复下次访问时的状态
2. **防抖优化**：延长时间（3秒）避免频繁触发自动保存
3. **保存上下文保存**：使用 `useRef` 保存当前画布 ID 和配置，避免闭包陷阱
4. **离线检测**：使用 `navigator.onLine` 事件监听网络状态
5. **状态同步**：与 `useUnsavedChanges` Hook 共享保存逻辑，避免重复代码

### 6.3 预估工作量

- **开发**：5 人天
  - 2 人天：`SaveIndicator` 组件 + 5 种状态实现
  - 1 人天：自动保存逻辑 + 防抖优化
  - 1 人天：离线模式支持 + 网络恢复提示
  - 1 人天：移动端适配 + 集成到画布编辑器
- **测试**：2 人天
  - 功能测试（所有状态切换、离线模式）
  - 边界测试（快速点击、网络抖动、长时间编辑）
- **总计**：7 人天

---

## 7. 依赖与风险

### 7.1 前置依赖

- React 18（已安装）
- Ant Design 5（已安装）
- 后端 Canvas API 已支持 CRUD 操作（无需修改后端）

### 7.2 风险

| 风险 | 级别 | 缓解措施 |
|------|------|----------|
| 自动保存触发过于频繁导致性能问题 | 中 | 增加防抖延迟（3秒），保存过程中忽略后续触发 |
| 离线模式下用户编辑的数据在恢复网络后丢失 | 中 | 离线期间仅在 localStorage 中暂存，恢复网络后提示保存 |
| 保存失败时用户重复点击重试导致多次网络请求 | 低 | 增加 "冷却时间"（重试间隔 5 秒） |
| 移动端空间受限，状态指示器遮挡内容 | 中 | 移动端移至顶部工具栏，使用 Tooltip 显示完整文本 |
| 与 PRD-P2-56 重叠，可能造成冗余开发 | 低 | 复用 PRD-P2-56 的 `useUnsavedChanges` Hook，减少重复实现 |

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 二次审核新增 17 项前端UX微交互
- Ant Design Badge 组件文档: https://ant.design/components/badge
- React Hooks: useAutoSave 实现: https://reacthooks.dev/examples/#use-autosave
- Offline API: https://developer.mozilla.org/en-US/docs/Web/API/Navigator/onLine
