# 方向⑮：营销资源中心 — 功能清单

> 定位：从"无素材管理"升级为"统一营销资源中心"——素材库+内容模板+变量插入+审批关联
> 策略评估：当前短信/邮件/Push内容直接写在节点配置中，无复用、无版本、无审核；5-7人月可完成核心
> 竞品对标：Braze Content Blocks+Templates、Iterable Template Studio、HubSpot Marketing Asset Manager
> 建议：**P2建议做**，运营效率提升，与⑧审批流协同

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 消息内容配置 | **部分** | 各SendMessageHandler读取节点config中的content字段 | 内容硬编码在节点，无复用 |
| 画布模板 | **部分** | CanvasTemplateDO(画布结构模板) | 有画布模板，无消息内容模板 |
| 素材管理 | **不存在** | — | 无素材上传/存储/检索 |
| 内容模板 | **不存在** | — | 无短信/邮件/Push内容模板 |
| 变量插入 | **部分** | ExecutionContext flatContext支持${key}引用 | 支持运行时变量，缺模板变量定义 |
| 内容预览 | **不存在** | — | 无内容渲染预览 |
| 素材审批 | **不存在** | — | 无素材审核流程 |
| 素材版本 | **不存在** | — | 无素材版本管理 |

### 关键洞察

当前消息内容的问题：
1. **硬编码**：短信内容直接写在节点配置中，修改需要编辑画布
2. **无复用**：相同内容在不同画布中需要重复编写
3. **无版本**：修改内容后无法追溯历史版本
4. **无预览**：发送前无法看到渲染后的效果
5. **无审核**：内容无人审核，敏感词/合规风险

---

## 功能清单

### P0 — 素材库与内容模板

---

#### 1. 素材库管理 [中复杂度 | 2.0人月]

**现状**：无素材管理

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 素材上传 | 图片/文件上传到对象存储 |
| 素材分类 | 按类型/标签/用途分类 |
| 素材搜索 | 关键词+标签搜索素材 |
| 素材预览 | 图片缩略图/文件信息预览 |
| 素材引用 | 查看素材被哪些模板/画布引用 |
| 素材删除 | 删除未被引用的素材 |
| 素材配额 | 租户素材存储配额 |

**素材类型**：

| 类型 | 格式 | 大小限制 | 用途 |
|------|------|---------|------|
| 图片 | JPG/PNG/GIF/SVG | 5MB | 邮件图片/企微图文/Push大图 |
| 文件 | PDF/DOC/XLS | 20MB | 附件 |
| 视频 | MP4 | 50MB | 企微视频消息 |
| 音频 | MP3/WAV | 10MB | 语音消息 |

**数据库DDL**：

```sql
CREATE TABLE asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '素材名称',
    type VARCHAR(20) NOT NULL COMMENT 'IMAGE/FILE/VIDEO/AUDIO',
    mime_type VARCHAR(50) NOT NULL,
    size_bytes BIGINT NOT NULL COMMENT '文件大小(字节)',
    storage_key VARCHAR(500) NOT NULL COMMENT '对象存储Key',
    storage_url VARCHAR(500) NOT NULL COMMENT '访问URL',
    thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
    width INT COMMENT '图片宽度',
    height INT COMMENT '图片高度',
    duration_ms INT COMMENT '音视频时长(毫秒)',
    tags JSON COMMENT '标签 ["618","banner","首页"]',
    folder_id BIGINT COMMENT '所属文件夹',
    reference_count INT NOT NULL DEFAULT 0 COMMENT '引用次数',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_folder (folder_id),
    INDEX idx_tenant (tenant_id),
    FULLTEXT idx_name (name)
) COMMENT '营销素材';

CREATE TABLE asset_folder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT COMMENT '父文件夹',
    sort_order INT NOT NULL DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_parent (parent_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '素材文件夹';
```

---

#### 2. 内容模板管理 [中复杂度 | 2.0人月]

**现状**：无消息内容模板

**需补齐**：

| 模板类型 | 描述 | 示例 |
|---------|------|------|
| 短信模板 | 短信内容+签名+变量 | 【品牌】{{userName}}，您的验证码是{{code}} |
| 邮件模板 | HTML邮件+变量+样式 | 欢迎邮件/促销邮件/通知邮件 |
| Push模板 | 标题+内容+图片 | {{userName}}，您有一张优惠券待领取 |
| 企微模板 | 文本/图文/卡片 | 活动通知/服务提醒 |
| In-App模板 | 弹窗/Banner/通知 | 应用内消息 |

**模板变量**：

```json
{
  "name": "欢迎邮件",
  "type": "EMAIL",
  "subject": "{{brandName}} — 欢迎加入",
  "body": "<h1>Hi {{userName}}</h1><p>欢迎注册{{brandName}}...</p>",
  "variables": [
    {"key": "brandName", "type": "TEXT", "defaultValue": "Canvas", "source": "SYSTEM"},
    {"key": "userName", "type": "TEXT", "defaultValue": "用户", "source": "PROFILE"},
    {"key": "couponCode", "type": "TEXT", "defaultValue": "", "source": "CANVAS"}
  ],
  "previewData": {
    "brandName": "Canvas",
    "userName": "张三",
    "couponCode": "WELCOME50"
  }
}
```

**数据库DDL**：

```sql
CREATE TABLE content_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '模板名称',
    type VARCHAR(20) NOT NULL COMMENT 'SMS/EMAIL/PUSH/WEWORK/IN_APP',
    subject VARCHAR(500) COMMENT '标题(邮件/Push)',
    body TEXT NOT NULL COMMENT '正文(HTML/纯文本)',
    extra_config JSON COMMENT '扩展配置(邮件发件人/Push图片等)',
    variables JSON COMMENT '变量定义',
    preview_data JSON COMMENT '预览数据',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVED/REJECTED/ARCHIVED',
    version INT NOT NULL DEFAULT 1,
    category VARCHAR(50) COMMENT '分类 营销/通知/验证码',
    tags JSON COMMENT '标签',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_tenant (tenant_id)
) COMMENT '内容模板';

CREATE TABLE content_template_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    version INT NOT NULL,
    body TEXT NOT NULL,
    subject VARCHAR(500),
    extra_config JSON,
    change_note VARCHAR(500) COMMENT '变更说明',
    created_by VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_template (template_id),
    INDEX idx_version (template_id, version)
) COMMENT '内容模板版本';
```

---

### P1 — 内容增强

---

#### 3. 内容编辑器 [中复杂度 | 2.0人月]

**现状**：内容编辑在画布节点配置中，无专用编辑器

**需补齐**：

| 编辑器类型 | 描述 | 前端组件 |
|-----------|------|---------|
| 邮件编辑器 | 可视化拖拽邮件编辑器(区块+变量) | 块编辑器(Slate.js/TipTap) |
| 短信编辑器 | 纯文本+变量插入+字数统计 | 文本编辑器+变量按钮 |
| Push编辑器 | 标题+正文+图片选择 | 表单+素材选择器 |
| 企微编辑器 | 文本/图文/卡片/Markdown | 多类型Tab |
| 变量面板 | 变量选择+预览 | 侧边栏变量树 |

**邮件编辑器区块**：

```
┌──────────────────────────────┐
│ [Logo]  [品牌图片选择]        │
├──────────────────────────────┤
│ Hi {{userName}},             │
│                              │
│ 感谢注册！您有一张优惠券：    │
│                              │
│ ┌──────────────┐             │
│ │  WELCOME50   │  ← 优惠券块│
│ │  满100减50    │             │
│ └──────────────┘             │
│                              │
│ [立即使用]  ← CTA按钮块      │
│                              │
│ 退订 | 偏好设置              │
└──────────────────────────────┘
```

---

#### 4. 内容预览与测试 [低复杂度 | 1.0人月]

**现状**：无内容预览

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 变量渲染预览 | 用预览数据渲染模板，展示最终效果 |
| 多端预览 | 邮件：Desktop/Mobile/纯文本；Push：iOS/Android |
| 测试发送 | 发送测试邮件/短信到指定地址 |
| 垃圾邮件评分 | 邮件内容垃圾邮件风险评分 |
| 链接检查 | 检查邮件中所有链接是否有效 |
| 变量高亮 | 高亮显示模板中的变量位置 |

---

### P2 — 高级内容能力

---

#### 5. 智能内容生成 [低复杂度 | 0.5人月]

**描述**：基于AI生成营销文案

| 子功能 | 描述 |
|--------|------|
| 文案生成 | 输入关键词→生成多个文案版本 |
| 标题优化 | 生成多个邮件标题候选 |
| A/B文案 | 自动生成A/B测试的文案变体 |
| 翻译 | 多语言翻译 |

---

#### 6. 内容审批集成 [低复杂度 | 0.5人月]

**描述**：内容模板审批与⑧营销审批流集成

| 子功能 | 描述 |
|--------|------|
| 模板审批 | 新建/修改模板需审批 |
| 内容审核 | 敏感词自动检查+人工审核 |
| 审批联动 | 画布审批时自动检查引用模板的审批状态 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 素材库管理 | 1.5 | 0.5 | 0.2 | 2.2 |
| P0 | 内容模板管理 | 1.5 | 0.5 | 0.2 | 2.2 |
| P1 | 内容编辑器 | 0.5 | 1.5 | 0.2 | 2.2 |
| P1 | 内容预览与测试 | 0.5 | 0.5 | 0.2 | 1.2 |
| P2 | 智能内容生成 | 0.3 | 0.2 | 0.1 | 0.6 |
| P2 | 内容审批集成 | 0.5 | 0.2 | 0.1 | 0.8 |
| | **合计** | **4.8** | **3.4** | **1.0** | **9.2** |

---

## 执行顺序

```
Sprint 1 (P0-素材): 素材库管理 — 2.2人月
  → 产出：素材上传/分类/搜索/引用

Sprint 2 (P0-模板): 内容模板管理 — 2.2人月
  → 产出：5种模板类型+变量定义+版本管理

Sprint 3 (P1-编辑): 内容编辑器 — 2.2人月
  → 产出：邮件可视化编辑+变量面板

Sprint 4 (P1-预览): 内容预览与测试 — 1.2人月
  → 产出：多端预览+测试发送+链接检查

Sprint 5 (P2-高级): 智能生成+审批 — 1.4人月
  → 产出：AI文案+内容审批
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 素材存储成本 | 图片/视频存储费用高 | CDN+压缩+租户配额 |
| 邮件编辑器复杂 | 可视化邮件编辑器开发量大 | 先做基础块编辑器，远期增强 |
| 模板变量冲突 | 不同画布传不同变量 | 变量校验+默认值+缺失告警 |
| 模板修改影响 | 修改模板影响引用画布 | 版本管理+修改通知+灰度更新 |
| 内容安全 | 素材可能含违规内容 | 上传时审核+人工抽检 |

---

## 与其他方向的关系

| 方向 | 与⑮的关系 |
|------|----------|
| ① 营销深度 | 内容模板是消息触达的基础 |
| ⑧ 营销审批 | 内容模板需审批+画布审批检查模板状态 |
| ⑨ 营销数据中台 | 不同模板的效果对比 |
| ⑭ A/B测试 | 模板变体作为A/B测试素材 |
