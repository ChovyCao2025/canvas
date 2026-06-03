# 方向㉘：动态内容渲染引擎 — 功能清单

> 定位：从"$field替换"升级为"全功能模板引擎"——Liquid/Handlebars模板+条件渲染+循环+动态内容块+内容片段+预测内容
> 策略评估：2026营销平台标配：动态内容块(Adobe Marketo) / Connected Content(Braze) / Liquid(Klaviyo) / 模板引擎是实时个性化的基础
> 竞品对标：Adobe Marketo(Dynamic Content+Predictive Content)、Braze(Connected Content)、Klaviyo(Liquid模板)、Dynamic Yield(实时个性化)
> 建议：**P1建议做**，①营销自动化深度的核心组件，所有触达渠道的个性化依赖模板引擎

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| House of MarTech: Real-Time Personalization Engine Architecture 2026 | 实时个性化引擎架构：事件流+规则引擎+模板渲染+CDP集成，跨渠道动态内容 | https://houseofmartech.com/blog/real-time-personalization-engine-architecture-building-dynamic-content-systems-that-scale-across-channels |
| Dynamic Yield: 2026 Gartner Leader Personalization | 多模态AI+共情个性化+实时行为信号推断情感状态，Gartner 8年Leader | https://www.dynamicyield.com/blog/mastercard-dynamic-yield-recognized-as-a-leader-in-the-2026-gartner-magic-quadrant-for-personalization-engines/ |
| Contentful: Real-Time Personalization 2026 | Vibe Personalization：AI辅助分析+自动化→动态内容适配 | https://www.contentful.com/blog/real-time-personalization/ |
| Blings: Marketing Automation Trends 2026 | "Moment of Open"渲染：内容在打开瞬间从CRM实时数据渲染，消灭Data Decay | https://www.blings.io/blog/news/marketing-automation-trends-to-watch-in-2026-ai-and-beyond/ |
| Adobe Marketo: Dynamic Content & Personalization | Token-based个性化+预测内容+动态内容块+内容片段 | https://business.adobe.com/products/marketo/dynamic-content-personalization.html |
| Shopify Liquid | 安全、客户可用的模板语言，支持变量/条件/循环/过滤器 | https://shopify.github.io/liquid/ |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 变量替换 | **部分** | AbstractSendMessageHandler.resolve() — `$field`从上下文取值 | 仅支持简单$field替换，无模板语法 |
| 消息内容 | **部分** | AbstractSendMessageHandler.content() — subject/body/title/imageUrl/clickUrl | 静态内容字段，无动态渲染 |
| Groovy脚本 | **完整** | GroovyHandler(GroovySandbox) | 可编程但非模板，安全风险+学习门槛高 |
| 模板引擎 | **不存在** | — | 无Liquid/Handlebars等模板引擎 |
| 条件渲染 | **不存在** | — | 无法在模板中if/else |
| 循环渲染 | **不存在** | — | 无法在模板中for-each |
| 动态内容块 | **不存在** | — | 无法按受众展示不同内容 |
| 内容片段 | **不存在** | — | 无法复用公共内容块 |
| 预测内容 | **不存在** | — | 无AI驱动的内容推荐 |

### 关键洞察

AbstractSendMessageHandler.resolve()现状：
1. **仅支持`$field`语法**：`$.userName`从ExecutionContext取值，无模板语法
2. **无条件逻辑**：无法在内容中写`{% if vip %}尊敬的VIP{% else %}亲爱的用户{% endif %}`
3. **无循环**：无法渲染列表数据(如最近3笔订单)
4. **无过滤器**：无法格式化日期/金额/截断文本
5. **无模板复用**：每条消息内容硬编码在节点配置中

"Data Decay"问题(Blings提出)：
- 当前：发送时渲染内容→用户打开时数据已过期(库存/价格/积分已变)
- 理想：打开时实时渲染→内容始终反映最新数据
- Canvas的`$field`替换在发送时执行，无法做到"Moment of Open"渲染

---

## 功能清单

### P0 — 模板引擎核心

---

#### 1. Liquid模板引擎 [中复杂度 | 2.0人月]

**现状**：仅$field替换

**需补齐**：

| 子功能 | 描述 | 示例 |
|--------|------|------|
| 变量输出 | 输出用户属性/上下文变量 | `{{ user.name }}` |
| 条件渲染 | 根据条件展示不同内容 | `{% if user.vip %}VIP{% endif %}` |
| 循环渲染 | 遍历列表数据 | `{% for item in orders %}{{ item.name }}{% endfor %}` |
| 过滤器 | 格式化变量 | `{{ user.birthday \| date: "%Y年%m月" }}` |
| 默认值 | 变量为空时的兜底 | `{{ user.name \| default: "用户" }}` |
| 数学运算 | 简单计算 | `{{ price \| times: 0.9 }}` |
| 赋值 | 模板内变量 | `{% assign discount = 10 %}` |
| 安全沙箱 | 禁止危险操作(文件/网络/反射) | 无System.exit/Runtime.exec |

**模板变量数据源**：

```
{{ user.name }}          ← CustomerProfileDO
{{ user.email }}         ← CustomerChannelDO
{{ user.vip_level }}     ← 用户标签
{{ canvas.name }}        ← 画布信息
{{ order.items }}        ← 上下文变量(flatContext)
{{ preference.topic }}   ← 用户偏好
{{ points.balance }}     ← 积分余额
```

**数据库DDL**：

```sql
CREATE TABLE content_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_key VARCHAR(100) NOT NULL COMMENT '模板标识',
    name VARCHAR(200) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(20) NOT NULL COMMENT 'EMAIL/SMS/PUSH/WEWORK/INAPP',
    subject_template TEXT COMMENT '主题模板(邮件)',
    body_template TEXT NOT NULL COMMENT '正文模板(Liquid)',
    preview_text_template TEXT COMMENT '预览文本模板',
    available_variables JSON COMMENT '可用变量列表+描述',
    sample_data JSON COMMENT '预览用示例数据',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/ARCHIVED',
    version INT NOT NULL DEFAULT 1,
    created_by VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_key (template_key),
    INDEX idx_type (template_type),
    INDEX idx_tenant (tenant_id)
) COMMENT '内容模板';

CREATE TABLE content_template_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    version INT NOT NULL,
    body_template TEXT NOT NULL,
    subject_template TEXT,
    change_note VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_template (template_id)
) COMMENT '内容模板版本';
```

---

#### 2. 动态内容块 [中复杂度 | 1.5人月]

**现状**：无动态内容

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 受众内容块 | 不同受众看到不同内容(同一条消息) | Adobe Marketo: Dynamic Content Blocks |
| 条件内容块 | 基于用户属性的条件渲染 | Braze: Connected Content |
| 实时内容 | 发送时从外部API获取最新数据 | Braze: Connected Content |
| 产品推荐 | 基于用户行为推荐产品 | Klaviyo: Product Feed |
| 库存感知 | 展示实时库存(仅显示有货商品) | Blings: Moment of Open |

**动态内容块示例**：

```
# 同一封邮件，不同用户看到不同内容

{% dynamic_block "hero_section" %}
  {% if user.vip_level == "GOLD" %}
    尊敬的黄金会员，为您推荐新品:
    {% for product in recommended_products limit: 3 %}
      - {{ product.name }} ¥{{ product.price }}
    {% endfor %}
  {% elsif user.vip_level == "SILVER" %}
    亲爱的银卡会员，限时优惠:
    {% for product in hot_products limit: 2 %}
      - {{ product.name }} ¥{{ product.price }}
    {% endfor %}
  {% else %}
    新用户专享: 首单9折
  {% endif %}
{% enddynamic_block %}
```

**数据库DDL**：

```sql
CREATE TABLE dynamic_content_block (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    block_key VARCHAR(100) NOT NULL COMMENT '内容块标识',
    name VARCHAR(200) NOT NULL,
    default_content TEXT NOT NULL COMMENT '默认内容(Liquid)',
    targeting_rules JSON COMMENT '定向规则(属性→内容)',
    data_source VARCHAR(30) COMMENT 'INTERNAL/API/RECOMMENDATION',
    data_source_config JSON COMMENT '数据源配置(API URL/推荐引擎参数)',
    cache_seconds INT NOT NULL DEFAULT 300 COMMENT '缓存时间(秒)',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_key (block_key),
    INDEX idx_tenant (tenant_id)
) COMMENT '动态内容块';
```

---

### P1 — 内容管理与复用

---

#### 3. 内容片段与复用 [低复杂度 | 0.8人月]

**现状**：无内容复用

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 内容片段 | 可复用的内容块(如页脚/免责声明/品牌Header) |
| 片段嵌入 | 模板中`{% include 'footer' %}`引用片段 |
| 片段更新 | 片段修改后所有引用自动更新 |
| 品牌元素 | Logo/颜色/字体等品牌资产 |
| 合规片段 | 退订链接/隐私政策/物理地址等合规内容 |

---

#### 4. 内容预览与测试 [中复杂度 | 1.0人月]

**现状**：无内容预览

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 实时预览 | 输入用户ID→实时渲染预览 |
| 多设备预览 | Desktop/Mobile/不同邮件客户端预览 |
| 样本数据渲染 | 用示例数据渲染预览(无需真实用户) |
| 垃圾邮件检测 | 检测内容是否触发垃圾邮件过滤器 |
| 链接检查 | 检测模板中链接是否有效 |
| 渲染性能 | 模板渲染耗时统计+慢模板告警 |

---

### P2 — 高级内容能力

---

#### 5. 预测内容 [中复杂度 | 1.0人月]

**描述**：AI驱动的内容推荐

| 子功能 | 描述 |
|--------|------|
| 智能内容选择 | 基于用户历史行为自动选择最佳内容变体 |
| 文案变体生成 | AI生成多版本文案+自动选择最优 |
| 发送时间优化 | 预测每个用户的最佳发送时间 |
| 渠道偏好预测 | 预测用户最可能响应的渠道 |
| 个性化推荐 | 基于浏览/购买历史的商品推荐 |

---

#### 6. Moment-of-Open渲染 [中复杂度 | 1.0人月]

**描述**：打开时实时渲染(消灭Data Decay)

| 子功能 | 描述 |
|--------|------|
| 延迟渲染标记 | 标记需打开时渲染的内容块 |
| 实时数据拉取 | 打开时从CDP/API获取最新数据 |
| 缓存策略 | 按内容块设置缓存时间(价格=5s/库存=30s/积分=60s) |
| 降级策略 | 数据源不可用时展示缓存内容 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | Liquid模板引擎 | 1.5 | 0.5 | 0.2 | 2.2 |
| P0 | 动态内容块 | 1.2 | 0.3 | 0.2 | 1.7 |
| P1 | 内容片段与复用 | 0.5 | 0.3 | 0.1 | 0.9 |
| P1 | 内容预览与测试 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 预测内容 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | Moment-of-Open渲染 | 0.7 | 0.3 | 0.1 | 1.1 |
| | **合计** | **5.3** | **2.0** | **0.8** | **8.1** |

---

## 执行顺序

```
Sprint 1 (P0-引擎): Liquid模板引擎 — 2.2人月
  → 产出：Liquid解析器+安全沙箱+变量绑定+过滤器+预览

Sprint 2 (P0-动态): 动态内容块 — 1.7人月
  → 产出：受众内容块+条件渲染+实时内容API+产品推荐

Sprint 3 (P1-片段): 内容片段与复用 — 0.9人月
  → 产出：片段CRUD+include引用+品牌元素+合规片段

Sprint 4 (P1-预览): 内容预览与测试 — 1.1人月
  → 产出：实时预览+多设备+垃圾邮件检测+链接检查

Sprint 5 (P2-高级): 预测内容+Moment渲染 — 2.2人月
  → 产出：AI内容选择+发送时间优化+打开时渲染
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 模板安全 | 恶意模板注入/资源耗尽 | 安全沙箱+白名单过滤器+渲染超时+资源限制 |
| 渲染性能 | 复杂模板渲染耗时影响发送速度 | 模板编译缓存+异步渲染+性能监控 |
| 模板迁移 | 现有$field语法需迁移到Liquid | 兼容层+渐进迁移+双引擎并行 |
| 内容一致性 | 不同渠道渲染结果不一致 | 跨渠道预览+一致性测试+快照对比 |
| 实时数据依赖 | 外部API不可用时内容渲染失败 | 缓存兜底+降级策略+健康检查 |

---

## 与其他方向的关系

| 方向 | 与㉘的关系 |
|------|----------|
| ① 营销自动化深度 | 模板引擎是触达个性化的基础 |
| ㉑ 优惠券与促销引擎 | 动态内容块可渲染优惠券/促销信息 |
| ㉒ 会员积分体系 | 积分余额/等级作为模板变量 |
| ⑬ 实时用户画像 | 画像属性→模板变量→个性化内容 |
| ④ AI原生平台 | 预测内容是AI平台的应用场景 |
| ⑮ 营销资源中心 | 模板是营销资源的重要类型 |
| ㉗ 偏好与同意管理 | 偏好数据作为模板变量(如用户语言) |
