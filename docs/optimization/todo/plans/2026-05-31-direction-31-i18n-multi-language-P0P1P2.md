# 方向㉛：国际化与多语言本地化平台 — 功能清单

> 定位：从"仅日期本地化"升级为"全球多语言SaaS平台"——i18n框架+翻译管理+本地化内容+多语言营销+RTL支持
> 策略评估：国际化是SaaS出海标配，TMS(Translation Management System)+i18n框架+本地化营销是2026营销平台必备能力
> 竞品对标：Phrase(TMS+50+集成+AI翻译)、Ortto(多语言营销自动化)、Locize(持续本地化CDN交付)、XTM AI(智能本地化生态+AI Pack)
> 建议：**P2建议做**，⑫多租户国际化+㉘模板引擎完善后启动，出海场景刚需但国内场景优先级低于P0/P1

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Phrase: Enterprise Localization Platform 2026 | TMS核心：50+集成(CMS/CRM/营销)、AI翻译引擎、翻译记忆库、术语管理、质量评分、持续本地化CI/CD | https://phrase.com/ |
| Locize: i18n Guide 2026 | 持续本地化：CDN交付翻译文件、实时更新无需重新部署、按需加载语言包、翻译分析 | https://locize.com/ |
| Lingoport: Localization Trends 2026 | 2026本地化趋势：AI翻译+人工审校混合工作流、持续本地化DevOps集成、i18n linting自动化、多语言UX设计 | https://www.lingoport.com/ |
| Ortto: Multi-Lingual Marketing Automation | 多语言营销：自动语言检测+多语言旅程+多语言SMS/邮件触达+受众语言偏好+本地化报表 | https://ortto.com/ |
| XTM AI: Intelligent Localization Ecosystem | AI Pack：神经机器翻译+翻译质量评估+术语提取+多语言SEO+内容本地化 | https://xtm.cloud/ |
| Astro i18n Guide 2026 | 前端i18n最佳实践：路由级语言切换+SSR/SSG多语言+SEO hreflang+语言检测+命名空间翻译文件 | https://docs.astro.build/en/guides/internationalization/ |
| Localize: Localized Marketing Guide 2026 | 本地化营销：地域化内容+文化适配+多语言A/B测试+本地化营销自动化+本地化内容策略 | https://localizejs.com/ |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 日期本地化 | **部分** | SysUserService.java — `Locale.CHINA`/`Locale.US`用于日期格式化 | 仅日期Locale切换，无完整i18n |
| 前端国际化 | **不存在** | — | 零i18n框架：无react-intl/i18next/formatMessage/FormattedMessage |
| 后端国际化 | **不存在** | — | 零properties文件/ResourceBundle/MessageSource |
| 消息多语言 | **不存在** | — | 所有触达消息仅支持单语言内容 |
| 翻译管理 | **不存在** | — | 无翻译工作流/翻译记忆库/术语表 |
| 语言检测 | **不存在** | — | 无浏览器语言检测/IP地理位置语言推断 |
| RTL支持 | **不存在** | — | 无阿拉伯语/希伯来语RTL布局 |
| 多语言营销 | **不存在** | — | 无法按用户语言偏好发送不同语言内容 |
| 本地化资源 | **不存在** | — | 无翻译文件管理/语言包/CDN交付 |

### 关键洞察

代码扫描发现的i18n现状：
- **后端Java**：grep `locale|i18n|Locale|I18n|properties|ResourceBundle|message.*bundle|translation|多语言|国际化` 返回结果仅为Spring `@ConfigurationProperties`/`@EnableConfigurationProperties`/`Properties`类引用——**零i18n基础设施**
- **前端React**：grep `locale|i18n|I18n|intl|formatMessage|FormattedMessage|useIntl|react-intl|i18next|translation` 返回**零结果**——完全空白
- **唯一本地化痕迹**：SysUserService.java 中使用 `Locale.CHINA`/`Locale.US` 切换日期格式，仅此一处

Ortto多语言营销模式的启示：
- 营销平台的多语言不仅是UI翻译，更是**触达内容的多语言**
- 同一画布需支持按用户语言自动选择不同内容变体
- 邮件/SMS/推送模板支持多语言版本+语言自动匹配

Phrase TMS与营销平台的集成模式：
- 翻译文件 → CI/CD流水线 → 构建多语言消息模板
- 翻译记忆库 → 降低重复翻译成本
- 术语表 → 品牌术语一致性

---

## 功能清单

### P0 — i18n框架核心

---

#### 1. 前端i18n框架 [中复杂度 | 1.5人月]

**现状**：零i18n

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| i18n框架集成 | react-intl或i18next集成，FormatMessage/useIntl全局可用 | Astro i18n Guide: 路由级语言切换 |
| 语言检测 | 浏览器Accept-Language + localStorage + URL参数自动检测 | Ortto: 自动语言检测 |
| 语言切换 | 导航栏语言切换器+用户偏好持久化 | Locize: 语言切换器 |
| 翻译文件结构 | JSON/YAML命名空间翻译文件(按页面/模块组织) | Locize: 命名空间翻译文件 |
| 消息提取 | Babel/CLI扫描代码中所有硬编码文本→生成翻译键 | Lingoport: i18n linting自动化 |
| 日期/数字/货币 | 根据locale格式化日期/数字/货币 | Intl API + date-fns |
| 复数规则 | 多语言复数形式（如阿拉伯语6种复数形式） | CLDR复数规则 |
| 缺失翻译回退 | 翻译键缺失→显示默认语言+标记未翻译 | Phrase: fallback机制 |

**翻译文件结构示例**：

```json
// zh-CN/common.json
{
  "common.save": "保存",
  "common.cancel": "取消",
  "common.delete": "删除",
  "common.confirm": "确认",
  "common.search": "搜索"
}

// en-US/common.json
{
  "common.save": "Save",
  "common.cancel": "Cancel",
  "common.delete": "Delete",
  "common.confirm": "Confirm",
  "common.search": "Search"
}
```

---

#### 2. 后端i18n框架 [中复杂度 | 1.5人月]

**现状**：零i18n

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| MessageSource配置 | Spring MessageSource集成，ResourceBundle+properties文件 |
| 多语言异常消息 | API异常/错误消息支持多语言 |
| 多语言校验消息 | Bean Validation消息支持多语言 |
| API语言参数 | API接受`Accept-Language` header或`lang`参数 |
| 审计日志语言 | 审计日志记录原始语言+用户语言偏好 |
| 翻译管理API | 翻译键CRUD+批量导入导出+版本对比 |

**数据库DDL**：

```sql
CREATE TABLE message_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    msg_key VARCHAR(200) NOT NULL COMMENT '翻译键',
    locale VARCHAR(10) NOT NULL COMMENT '语言区域(zh-CN/en-US/ja-JP/ko-KR/ar-SA)',
    msg_value TEXT NOT NULL COMMENT '翻译文本',
    module VARCHAR(50) NOT NULL COMMENT '模块(common/canvas/engine/dashboard)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/REVIEWED/APPROVED',
    translator VARCHAR(64) COMMENT '翻译者',
    reviewed_by VARCHAR(64) COMMENT '审校者',
    last_translated_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_key_locale (msg_key, locale),
    INDEX idx_module (module),
    INDEX idx_locale (locale),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '多语言消息资源';

CREATE TABLE supported_locale (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    locale VARCHAR(10) NOT NULL COMMENT '语言区域',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名(中文)',
    native_name VARCHAR(100) NOT NULL COMMENT '原生名(English)',
    flag_emoji VARCHAR(10) COMMENT '国旗emoji',
    direction VARCHAR(5) NOT NULL DEFAULT 'LTR' COMMENT 'LTR/RTL',
    date_format VARCHAR(20) COMMENT '日期格式',
    time_format VARCHAR(20) COMMENT '时间格式',
    number_format VARCHAR(20) COMMENT '数字格式',
    currency_code VARCHAR(5) COMMENT '货币代码',
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_locale (locale),
    INDEX idx_tenant (tenant_id)
) COMMENT '支持的语言区域';
```

---

### P1 — 多语言营销与内容

---

#### 3. 多语言消息模板 [中复杂度 | 1.5人月]

**现状**：所有消息仅单语言

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 多语言邮件模板 | 同一邮件模板支持多语言版本 | Ortto: 多语言旅程+SMS |
| 多语言SMS/Push | 短信/推送按用户语言偏好发送 | Ortto: 多语言触达 |
| 多语言InApp通知 | App内消息多语言自动匹配 | Phrase: 多语言营销 |
| 语言自动匹配 | 根据用户preferred_language自动选择模板语言 | Ortto: 受众语言偏好 |
| 语言回退链 | zh-CN→zh→en-US→en(default) 回退链 | Locize: fallback |
| 模板语言版本对比 | 同一模板不同语言版本对比查看 | Phrase: 双语编辑器 |

**多语言模板扩展示例**：

```
# 邮件模板支持多语言

template_key: "welcome_email"
versions:
  zh-CN:
    subject: "欢迎来到{{ brand.name }}！"
    body: "尊敬的{{ user.name }}，感谢您的注册..."
  en-US:
    subject: "Welcome to {{ brand.name }}!"
    body: "Dear {{ user.name }}, thank you for signing up..."
  ja-JP:
    subject: "{{ brand.name }}へようこそ！"
    body: "{{ user.name }}様、ご登録ありがとうございます..."
```

---

#### 4. 本地化内容规则 [中复杂度 | 1.0人月]

**现状**：无本地化内容能力

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| Locale感知条件 | 画布分支按用户语言分流 | Localize: 本地化营销自动化 |
| 本地化时间窗口 | 按用户时区计算发送时间 | Ortto: 本地化报表 |
| 本地化频率控制 | 按地区配置不同的触达频率限制 | 合规要求 |
| 时区感知画布 | 同一画布按用户时区自动偏移执行时间 | 跨时区营销 |
| 本地化节假日 | 不同国家节假日日历(按地区和语言) | 微盟/有赞: 本地化营销 |
| 文化适配警告 | 检测可能引起文化不适的内容(颜色/符号/图片) | Localize: 本地化内容策略 |

---

#### 5. RTL支持 [低复杂度 | 0.8人月]

**现状**：零RTL

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| RTL布局切换 | CSS direction: rtl 全局切换 |
| 组件RTL适配 | antd RTL ConfigProvider |
| 画布RTL | 画布编辑器RTL模式(节点从左→右变为右→左) |
| 双向文本处理 | 中英混排+阿拉伯语的正确显示 |
| 邮件RTL | 邮件模板HTML自动添加dir="rtl" |
| 预览RTL | 所有预览支持RTL模式 |

---

### P2 — 翻译管理与自动化

---

#### 6. 翻译管理工作流 [中复杂度 | 1.0人月]

**描述**：翻译→审校→发布的完整工作流

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 翻译任务分配 | 待翻译/待审校任务分配+提醒 | Phrase: TMS工作流 |
| 双语编辑器 | 原文与译文并排编辑器 | Phrase: 双语编辑 |
| 翻译记忆库(TM) | 已翻译句段复用+一致性提示 | Phrase: 翻译记忆库 |
| 术语库 | 品牌/行业术语管理+术语一致性检查 | Phrase: 术语管理 |
| 翻译进度 | 按模块/语言显示翻译完成度 | Locize: 翻译分析 |
| 审校流程 | 翻译→初审→终审→发布 | Phrase: 质量评分 |

**数据库DDL**：

```sql
CREATE TABLE translation_memory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_text TEXT NOT NULL COMMENT '源文本',
    source_locale VARCHAR(10) NOT NULL COMMENT '源语言',
    target_text TEXT NOT NULL COMMENT '目标文本',
    target_locale VARCHAR(10) NOT NULL COMMENT '目标语言',
    quality_score INT DEFAULT 100 COMMENT '质量评分0-100',
    usage_count INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    domain VARCHAR(50) COMMENT '领域(marketing/legal/technical)',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_source (source_locale),
    INDEX idx_target (target_locale),
    FULLTEXT INDEX ft_source (source_text)
) COMMENT '翻译记忆库';

CREATE TABLE term_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    term_key VARCHAR(100) NOT NULL COMMENT '术语标识',
    locale VARCHAR(10) NOT NULL COMMENT '语言',
    term_value VARCHAR(200) NOT NULL COMMENT '术语文本',
    definition TEXT COMMENT '术语定义',
    usage_note TEXT COMMENT '使用说明',
    forbidden BOOLEAN NOT NULL DEFAULT 0 COMMENT '禁用词(不应使用的翻译)',
    domain VARCHAR(50) COMMENT '领域',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_term_locale (term_key, locale),
    INDEX idx_locale (locale),
    INDEX idx_tenant (tenant_id)
) COMMENT '术语库';
```

---

#### 7. 机器翻译集成 [低复杂度 | 0.5人月]

**描述**：AI辅助翻译+自动翻译

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| MT引擎接入 | DeepL/Google Cloud Translation/Azure Translator API | XTM AI: 神经机器翻译 |
| 预翻译 | 新增翻译键自动机器翻译→进入审校 | Phrase: AI翻译引擎 |
| MT质量评估 | 机器翻译置信度评分+低质量标记 | XTM AI: 翻译质量评估 |
| 术语约束MT | MT翻译时优先使用术语库术语 | Phrase: 术语管理 |
| 自适应学习 | 审校修正→MT模型持续学习改善 | XTM AI: AI Pack |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 前端i18n框架 | 0.3 | 1.2 | 0.2 | 1.7 |
| P0 | 后端i18n框架 | 1.2 | 0.3 | 0.2 | 1.7 |
| P1 | 多语言消息模板 | 1.2 | 0.3 | 0.2 | 1.7 |
| P1 | 本地化内容规则 | 0.7 | 0.3 | 0.1 | 1.1 |
| P1 | RTL支持 | 0.3 | 0.5 | 0.1 | 0.9 |
| P2 | 翻译管理工作流 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 机器翻译集成 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **4.7** | **3.1** | **1.0** | **8.8** |

---

## 执行顺序

```
Sprint 1 (P0-后端): 后端i18n框架 — 1.7人月
  → 产出：MessageSource+翻译管理API+多语言异常/校验+语言检测

Sprint 2 (P0-前端): 前端i18n框架 — 1.7人月
  → 产出：react-intl集成+语言切换+翻译文件+日期/数字/货币格式化

Sprint 3 (P1-模板): 多语言消息模板 — 1.7人月
  → 产出：邮件/SMS/Push/InApp多语言+自动匹配+回退链

Sprint 4 (P1-规则): 本地化内容规则+RTL — 2.0人月
  → 产出：时区感知+本地化条件+Locale条件+RTL布局

Sprint 5 (P2-TMS): 翻译管理+MT集成 — 1.7人月
  → 产出：翻译工作流+TM+术语库+MT引擎+预翻译
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 翻译覆盖不全 | 部分页面中英文混合→体验差 | 翻译覆盖率监控+CI检查+强制翻译键 |
| UI溢出 | 翻译文本过长→布局溢出(德语/俄语常见) | 弹性布局+文本截断+翻译指南(长度限制) |
| RTL兼容 | antd组件+自定义组件RTL适配不到位 | 组件级RTL测试+Visual Regression |
| 文化失误 | 颜色/符号/图片跨文化不当 | 文化适配检查+本地化审查+本地团队review |
| 数据膨胀 | message_resource表数据快速增长 | 按语言按模块分区+CDN缓存+按需加载 |
| 翻译一致性 | 同一术语在不同模块翻译不一致 | 术语库强制+TM复用+翻译检查工具 |

---

## 与其他方向的关系

| 方向 | 与㉛的关系 |
|------|----------|
| ㉘ 动态内容渲染引擎 | Liquid模板需支持多语言变量+Locale感知条件渲染 |
| ⑫ 多租户SaaS化 | 不同租户启用不同语言子集+自定义翻译 |
| ⑮ 营销资源中心 | 多语言素材+多语言模板管理 |
| ㉗ 偏好与同意管理 | 用户语言偏好是偏好中心核心属性 |
| ㉚ 多品牌代理平台 | 不同品牌可能面向不同语言市场 |
| ① 营销自动化深度 | 多语言触达是营销深化的基础 |
| ㉓ 对话式营销 | 多语言对话+WhatsApp多语言模板 |
