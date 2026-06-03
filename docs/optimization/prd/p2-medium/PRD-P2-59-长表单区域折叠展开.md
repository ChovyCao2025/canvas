# PRD

> 本文档为营销画布平台产品需求文档，对应 BMAD 产品设计审查报告 — 前端UX微交互补齐（表单UX）

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-59 |
| **需求名称** | 长表单区域折叠展开 — 长表单无区域折叠/展开 |
| **优先级** | P2 |
| **所属类别** | 前端UX微交互 — 表单UX |
| **提出日期** | 2026-06-02 |
| **来源** | BMAD 产品设计审查报告 — 二次审核新增项 |
| **竞品对标** | Google Ads Editor, Facebook Ads Manager |

---

## 1. 问题描述

### 1.1 现状

画布编辑器、受众编辑等页面使用长表单展示大量配置项，页面高度可达数千像素。用户滚动到页面底部时，无法快速回到顶部或折叠不需要的内容区域，体验如下：

- **长滚动负担**：从表单顶部滚动到编辑节点配置需要 20-30 秒（100+ 节点）
- **信息过载**：所有字段一次性展示，用户难以聚焦
- **反向浏览困难**：已有配置的情况下，用户需要从上到下遍历所有字段

### 1.2 痛点

- **效率低下**：修改配置需要频繁滚动，操作路径长
- **视觉疲劳**：长时间浏览长表单导致注信息疲劳
- **页面臃肿**：表单填写越复杂，页面越长，用户浏览器内存占用越高
- **移动端不可用**：移动端长表单完全无法操作（水平和垂直滚动都困难）

### 1.3 竞品对标

| 应用 | 能力 |
|------|------|
| Google Ads Editor | 表单分组折叠/展开（账户设置、广告系列、广告组、关键词） |
| Facebook Ads Manager | 卡片式布局 + 收缩/展开功能 |
| Airtable | 可折叠字段组 + 布局预设 |
| Jira | 问题描述表单可折叠特定字段 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为画布管理员，我希望折叠不需要的表单区域以便快速定位到需要的编辑区域，以便提高配置效率。

### 2.2 成功指标

- 表单区域可折叠/展开（默认展开所有或根据用户偏好）
- 长表单页面平均滚动距离减少 60%（从 3000px → 1200px）
- 用户修改配置的平均操作时间减少 25%
- 移动端长表单可正常使用（不再完全不可用）

### 2.3 不做会怎样

- 用户必须逐个展开/折叠表单区域，无法批量操作
- 长表单导致页面高度过高，性能问题（内存占用、渲染延迟）
- 移动端用户体验灾难（长滚动）

---

## 3. 功能需求

### 3.1 核心功能

1. **表单分组组件**：将表单字段按逻辑分组（基本信息、触发器、节点配置、审计算法等）
2. **折叠/展开功能**：每个分组支持独立的折叠/展开操作
3. **状态持久化**：用户折叠/展开偏好通过 localStorage 持久化
4. **移动端适配**：移动端默认全部折叠，点击展开某个分组，避免屏幕过拥挤

### 3.2 详细描述

#### 3.2.1 表单分组设计

**画布编辑器表单分组：**
- **基本信息**：画布名称、描述、创建时间、创建者
- **触发器配置**：启动类型、启动参数、频率限制（FREQUENCY_CAP）
- **全局节点配置**：默认并发数、超时时间、重试策略
- **节点配置（可折叠）**：所有以 + 号展开每个节点的详细配置
- **连线配置**：节点间连线规则、优先级
- **发布配置**：发布渠道、发布时间、标签

**受众编辑表单分组：**
- **基本信息**：受众名称、描述、标签
- **规则条件**：IF 条件、过渡条件
- **人群策略**：ONLINE/HYBRID/COHORT 选择、人群来源
- **频率限制**：每日发送次数、间隔时间
- **高级选项**：头像、背景图、废弃触发器

#### 3.2.2 折叠/展开组件实现

```typescript
// components/AccordionGroup.tsx
import { useState } from 'react';
import { ArrowDownOutlined, ArrowRightOutlined } from '@ant-design/icons';

interface AccordionGroupProps {
  title: string;
  defaultExpanded?: boolean;
  children: React.ReactNode;
  additionalAction?: React.ReactNode;
}

export const AccordionGroup: React.FC<AccordionGroupProps> = ({
  title,
  defaultExpanded = true,
  children,
  additionalAction,
}) => {
  const [expanded, setExpanded] = useState(defaultExpanded);

  const toggle = () => {
    setExpanded(!expanded);
  };

  return (
    <div className="accordion-group">
      <div className="accordion-header" onClick={toggle}>
        <span className="accordion-icon">
          {expanded ? <ArrowDownOutlined /> : <ArrowRightOutlined />}
        </span>
        <span className="accordion-title">{title}</span>
        {additionalAction && <div className="accordion-action">{additionalAction}</div>}
      </div>
      {expanded && <div className="accordion-content">{children}</div>}
    </div>
  );
};
```

#### 3.2.3 状态持久化

```typescript
// hooks/useAccordionState.ts
export const useAccordionState = (sections: string[]) => {
  const STORAGE_KEY = 'canvas-editor-accordion-state';

  const [expandedSections, setExpandedSections] = useState<Set<string>>(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        return new Set(parsed);
      } catch {
        return new Set(sections.map(() => true)); // 解析失败时默认全部展开
      }
    }
    return new Set(sections.map(() => true)); // 默认全部展开
  });

  const toggleSection = (section: string) => {
    const newExpanded = new Set(expandedSections);
    if (newExpanded.has(section)) {
      newExpanded.delete(section);
    } else {
      newExpanded.add(section);
    }
    setExpandedSections(newExpanded);
    localStorage.setItem(STORAGE_KEY, JSON.stringify([...newExpanded]));
  };

  const expandAll = () => {
    setExpandedSections(new Set(sections));
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sections));
  };

  const collapseAll = () => {
    setExpandedSections(new Set());
    localStorage.setItem(STORAGE_KEY, JSON.stringify([]));
  };

  return {
    expandedSections,
    toggleSection,
    expandAll,
    collapseAll,
  };
};
```

#### 3.2.4 移动端适配

```typescript
// hooks/useMobileAccordion.ts
export const useMobileAccordion = (sections: string[]) => {
  const [isMobile, setIsMobile] = useState(false);
  const [expandedSection, setExpandedSection] = useState<string | null>(null);

  useEffect(() => {
    const checkMobile = () => setIsMobile(window.innerWidth < 768);
    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  const toggleSection = (section: string) => {
    if (isMobile) {
      setExpandedSection(section === expandedSection ? null : section);
    } else {
      // 桌面端默认全部展开
      return false;
    }
  };

  const isExpanded = (section: string) => {
    if (isMobile) {
      return expandedSection === section;
    }
    return true; // 桌面端默认全部展开
  };

  return { isMobile, toggleSection, isExpanded, expandedSection };
};
```

### 3.3 交互流程

#### 桌面端：
1. 用户进入长表单页面
2. 默认展开所有表单分组
3. 用户点击某个分组的标题（折叠/展开图标）
4. 分组内容折叠/展开，页面向上/下滚动
5. 用户偏好通过 localStorage 持久化，刷新页面保留折叠状态

#### 移动端：
1. 用户进入长表单页面
2. 默认全部折叠（仅显示分组标题）
3. 用户点击某个分组标题
4. 分组内容展开占满屏幕高度
5. 用户点击其他分组，上一个分组自动折叠
6. 分组高度适合屏幕显示，避免长滚动

---

## 4. 非功能需求

- **性能要求**：折叠/展开动画流畅（< 300ms），不影响表单渲染性能
- **响应式**：支持桌面端（> 768px）、平板端（768-1024px）、移动端（< 768px）
- **无障碍性**：使用 `aria-expanded` 属性，屏幕阅读器可正确解析
- **SEO 友好**：折叠内容在语义上仍属于 DOM，不影响搜索引擎抓取

---

## 5. 验收标准

- [ ] 新增 `AccordionGroup` 组件支持折叠/展开 + 展开/全部折叠按钮
- [ ] 画布编辑器表单分组（6 个分组）
  - [ ] 基本信息
  - [ ] 触发器配置
  - [ ] 全局节点配置
  - [ ] 节点配置（可展开/折叠所有节点）
  - [ ] 连线配置
  - [ ] 发布配置
- [ ] 受众编辑表单分组（5 个分组）
  - [ ] 基本信息
  - [ ] 规则条件
  - [ ] 人群策略
  - [ ] 频率限制
  - [ ] 高级选项
- [ ] 状态持久化到 localStorage
- [ ] 支持 "展开全部" 和 "全部折叠" 操作按钮
- [ ] 移动端默认全部折叠，点击展开某个分组
- [ ] 展开动画流畅（无卡顿）
- [ ] 无障碍测试通过（aria-expanded 属性正确）

---

## 6. 技术建议

### 6.1 涉及模块

- **前端模块**：
  - `frontend/src/components/AccordionGroup/`（新增折叠分组组件）
  - `frontend/src/hooks/useAccordionState.ts`（新增状态管理 Hook）
  - `frontend/src/hooks/useMobileAccordion.ts`（新增移动端适配 Hook）
  - `frontend/src/pages/canvas-editor/` + `frontend/src/pages/audience-edit/`（集成折叠功能）

### 6.2 技术要点

1. **CSS 动画**：使用 `transition: max-height 0.3s ease` 实现平滑展开/折叠
2. **状态持久化**：localStorage key 命名规范（如 `canvas-editor-accordion-state`）
3. **移动端布局**：使用响应式断点（@media query），移动端默认全部折叠
4. **可访问性**：为可折叠组件添加 `tabIndex`、`aria-expanded`、`aria-controls`
5. **性能优化**：折叠时禁用 `useMemo/useCallback` 的条件依赖，避免不必要的重新计算

### 6.3 预估工作量

- **开发**：4 人天
  - 1 人天：`AccordionGroup` 组件 + CSS 动画
  - 1 人天：`useAccordionState` Hook + localStorage 持久化
  - 0.5 人天：移动端适配 + `useMobileAccordion` Hook
  - 1 人天：集成到画布编辑器（6 个分组）
  - 0.5 人天：集成到受众编辑（5 个分组）
- **测试**：2 人天
  - 功能测试（折叠/展开、展开全部、折叠全部）
  - 边界测试（localStorage 清空、移动端尺寸）
  - 无障碍测试（屏幕阅读器验证）
  - 性能测试（长滚动测试）
- **总计**：6 人天

---

## 7. 依赖与风险

### 7.1 前置依赖

- React Hooks（已安装）
- Ant Design Icons（已安装）
- 无后端依赖

### 7.2 风险

| 风险 | 级别 | 缓解措施 |
|------|------|----------|
| 折叠/展开动画卡顿 | 中 | 使用 `transform: translateY` 代替 `height` 动画，GPU 加速 |
| 移动端折叠后用户找不到内容 | 高 | 移动端默认全部折叠，第一次进入提示积极提示 |
| localStorage 容量限制 | 低 | 仅存储分组状态字符串（长度 < 1KB），无风险 |
| 无障碍性测试不通过 | 中 | 参考 WCAG 2.1 AA 标准，使用 a11y-issues 调试工具 |
| 现有表单组件直接修改导致破坏 | 中 | 保留原始表单组件不变，仅外层包裹 `AccordionGroup` |

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 二次审核新增项
- Ant Design Collapse 组件: https://ant.design/components/collapse
- React Hooks 示例: https://react.dev/learn/scaling-up-with-reducer-and-context
- WCAG 2.1 AA 无障碍标准: https://www.w3.org/WAI/WCAG21/quickref/
