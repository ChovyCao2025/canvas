# 方向㉞：Website/In-App实时个性化引擎 — 功能清单

> 定位：从"固定内容触达"升级为"实时行为驱动的1:1个性化体验"——会话内实时决策+动态内容交换+AI推荐+Generative UI+A/B验证
> 策略评估：2026年个性化已从"nice-to-have"升级为"baseline"，VWO/Optimizely/CoreMedia/StackAdapt均验证实时个性化是营销自动化核心差异化能力
> 竞品对标：Optimizely(Web Experimentation+Personalization+45+AI Agent)、VWO(实时个性化+实验平台)、CoreMedia(DXP+CSAT/NPS驱动个性化)、StackAdapt(2026个性化趋势报告)
> 建议：**P1建议做**，依赖⑬实时画像引擎+㉜事件追踪SDK+⑭A/B测试平台成熟后启动，是营销从"批量触达"到"1:1个性化"的质变

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| VWO: Real-Time Website Personalization Guide 2026 | 实时个性化4步循环：Listen(事件流采集)→Think(AI决策)→Act(动态内容交付)→Learn(持续反馈)；ms级响应 | https://vwo.com/blog/real-time-personalization/ |
| StackAdapt: State of Personalization 2026 | 2026个性化趋势：从人口统计→心理行为定向、Generative UI(DOM实时重组)、零方数据为竞争优势、隐私-个性化平衡 | https://www.stackadapt.com/resources/downloads/personalization-trends-2026 |
| Optimizely: Digital Experience Platform 2026 | 完整DXP：内容管理+Web Experimentation+个性化+45+Opal AI Agent+多视图Campaign日历 | https://www.optimizely.com/ |
| CoreMedia: CSAT & NPS Surveys + Personalization 2026 | 反馈驱动个性化：NPS/CSAT→实时触发个性化规则→Detractor恢复/Promoter激活、反馈写入统一客户画像 | https://www.coremedia.com/personalized-experiences/surveys-csat-nps |
| Authencio: Website Personalization E-commerce Strategy 2026 | Generative UI：AI根据propensity score实时重组DOM结构、心理行为触发(Conversion Wax: dwell time+exit intent)、正向强化替代稀缺恐吓 | https://www.authencio.com/blog/website-personalization-e-commerce-strategy-guide |
| Klaviyo: 8 Marketing Automation Trends 2026 | AI从copilot→autonomous orchestration、self-optimizing systems实时调整creative/timing/channel mix、1:1个性化对话 | https://www.klaviyo.com/blog/marketing-automation-trends |
| Epsilon: Website Personalization E-commerce Growth 2026 | 网站个性化驱动增长：提升转化率+建立忠诚度+电商增长引擎 | https://www.epsilon.com/us/insights/blog/website-personalization-ecommerce-growth |
| Magic Logix: 7 Powerful Website Personalization Examples 2026 | Optimizely+Adobe Target作为头部个性化引擎案例 | https://www.magiclogix.com/theories/website-personalization-examples/ |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 画布内容触达 | **完整** | 60+ NodeHandler(Email/SMS/Push/InApp) | 仅"推送"触达，无"拉取"个性化(网站/App内) |
| AI推荐节点 | **存根** | AiNextBestActionHandler — 仅fallback | 无真实AI推荐引擎，无个性化决策 |
| 推荐常量和上下文 | **仅常量** | MapFieldKeys.RECOMMENDATIONS = "recommendations" | 仅键名定义，无推荐计算逻辑 |
| 实时行为采集 | **不存在** | — | 无网站端session级行为追踪(浏览/点击/停留/退出意图) |
| 实时决策引擎 | **不存在** | — | 无会话内ms级决策(内容交换/弹窗/推荐) |
| 动态内容交换 | **不存在** | — | 无网站/App内动态内容替换(横幅/CTA/推荐/弹窗) |
| Generative UI | **不存在** | — | 无AI驱动的DOM结构实时重组 |
| 个性化A/B验证 | **不存在** | — | 无个性化vs对照组的实验验证框架 |
| 心理行为触发 | **不存在** | — | 无dwell time/exit intent/scroll depth等行为触发 |

### 关键洞察

内容触达 vs 网站个性化的本质差异：
- **内容触达(现有)**：Push模式——主动推送消息给用户(Email/SMS/Push)，用户被动接收
- **网站个性化(缺失)**：Pull模式——用户访问网站/App时，实时动态调整展示内容，用户主动感知

VWO的实时个性化4步循环对Canvas的启示：
- **Listen**：SDK采集session级行为流(浏览/点击/停留/退出意图) → ㉜事件追踪SDK
- **Think**：AI引擎毫秒级判断用户意图+预测Next Best Experience → ③实时决策引擎
- **Act**：动态交换页面内容(横幅/CTA/推荐/弹窗) → 本方向核心
- **Learn**：A/B实验验证个性化效果→持续优化 → ⑭A/B测试平台

CoreMedia反馈驱动个性化的启示：
- CSAT=1分→实时弹窗："抱歉未能满足您的期望，需要在线客服吗？"
- NPS=10分→动态CTA："推荐给朋友，双方各得$10优惠"
- 反馈不再只是报表数据，而是实时个性化触发器

---

## 功能清单

### P0 — 实时个性化决策与内容交换

---

#### 1. 实时个性化决策引擎 [高复杂度 | 2.5人月]

**现状**：零实时个性化决策

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| Session级行为追踪 | 网站/App端实时采集浏览/点击/停留/滚动/退出意图 | VWO: unified data collection |
| 意图识别 | AI引擎根据session行为实时分类用户意图(浏览/比较/购买/流失) | VWO: AI-driven decisioning - intent identification |
| Next Best Experience | 预测用户最可能响应的体验(折扣/社交证明/产品对比/客服) | VWO: predictive modeling → NBE |
| 决策规则引擎 | 可视化规则配置: IF 用户行为 + 画像属性 THEN 个性化动作 | Optimizely: rule-based + ML hybrid |
| ms级决策API | 低延迟决策API(<50ms)，供网站/App前端实时调用 | VWO: millisecond response |
| 决策日志 | 每次决策记录(用户/上下文/决策结果/置信度)→用于效果分析 | 可解释性+效果归因 |

**决策引擎规则示例**：

```
IF user.current_session.page_views >= 3 
   AND user.current_session.time_on_pricing > 30s
   AND user.profile.lifetime_value == "HIGH"
THEN action = "show_sales_call_cta" priority=80

IF user.current_session.exit_intent == true
   AND user.cart.value > 100
   AND user.cart.abandoned_before == true
THEN action = "show_discount_popup" priority=90  // 高于客服CTA

IF user.current_session.dwell_time > 60s
   AND user.behavior.category == "comparison_shopping"
THEN action = "show_comparison_table" priority=70
```

**数据库DDL**：

```sql
CREATE TABLE personalization_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_key VARCHAR(100) NOT NULL COMMENT '规则标识',
    name VARCHAR(200) NOT NULL COMMENT '规则名称',
    description TEXT,
    conditions JSON NOT NULL COMMENT '触发条件(session行为+画像属性+上下文)',
    action_type VARCHAR(30) NOT NULL COMMENT 'POPUP/BANNER/CTA/RECOMMENDATION/INLINE_CONTENT/REDIRECT',
    action_config JSON NOT NULL COMMENT '动作配置(内容ID/模板/参数)',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级(0-100, 同用户命中多条规则时取最高)',
    cooldown_seconds INT NOT NULL DEFAULT 3600 COMMENT '冷却时间(同一用户不重复触发)',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_key (rule_key, tenant_id),
    INDEX idx_priority (priority),
    INDEX idx_tenant (tenant_id)
) COMMENT '个性化决策规则';

CREATE TABLE personalization_decision_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    rule_id BIGINT COMMENT '触发的规则ID',
    action_type VARCHAR(30) NOT NULL,
    action_config JSON COMMENT '执行的个性化动作',
    context_snapshot JSON COMMENT '决策时的用户上下文快照',
    confidence DECIMAL(3,2) COMMENT 'AI决策置信度',
    response_ms INT COMMENT '决策响应耗时(ms)',
    user_engaged TINYINT(1) COMMENT '用户是否响应(点击/转化)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_session (session_id),
    INDEX idx_rule (rule_id),
    INDEX idx_created (created_at)
) COMMENT '个性化决策日志';
```

---

#### 2. 动态内容交换SDK [高复杂度 | 2.0人月]

**现状**：零网站端个性化SDK

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| Web SDK (JS) | 网站端轻量SDK：内容交换+行为追踪+决策请求 | VWO: dynamic experience delivery |
| Mobile SDK | iOS/Android SDK：App内动态内容交换 | Native SDK |
| 内容占位符 | HTML data-attribute标注可个性化区域(`data-cv-personalize="hero_banner"`) | 声明式个性化 |
| 动态内容API | REST API: 传入user_id+context→返回个性化内容列表 | ms级低延迟 |
| 内容交换类型 | 横幅/CTA按钮/推荐商品/弹窗/内联内容/导航/定价表 | VWO: 多类型交换 |
| 缓存策略 | CDN边缘缓存+个性化内容差异化缓存(key=user_segment+content_id) | 性能优化 |
| A/B分组注入 | SDK支持将用户分配到个性化vs对照组→效果可测量 | VWO/StackAdapt: experimentation |
| 预览模式 | 运营人员可预览不同用户画像下的个性化效果 | 运营工具 |

**Web SDK API设计**：

```javascript
// 初始化个性化SDK
const cvPersonalize = CanvasPersonalize.init({
  apiKey: "pk_xxx",
  endpoint: "https://personalize.yourdomain.com",
  autoTrack: true,  // 自动追踪行为
  respectConsent: true
});

// 声明式内容占位符 (HTML)
// <div data-cv-personalize="hero_banner" data-cv-default="default_banner_v1">
//   <!-- 默认内容 -->
// </div>

// 程序化内容交换
cvPersonalize.onDecision((decisions) => {
  decisions.forEach(d => {
    switch(d.actionType) {
      case 'BANNER':
        document.querySelector(`[data-cv-personalize="${d.slot}"]`)
          .innerHTML = d.content;
        break;
      case 'POPUP':
        showModal(d.content, d.config);
        break;
      case 'CTA':
        updateCTA(d.slot, d.content);
        break;
      case 'RECOMMENDATION':
        renderRecommendations(d.slot, d.items);
        break;
    }
  });
});

// 行为信号发送
cvPersonalize.signal('exit_intent', { cartValue: 299 });
cvPersonalize.signal('dwell_time', { seconds: 45, page: 'pricing' });
cvPersonalize.signal('scroll_depth', { percent: 75 });
```

---

### P1 — AI推荐与智能优化

---

#### 3. AI推荐引擎 [高复杂度 | 2.0人月]

**现状**：零AI推荐

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 商品推荐 | 协同过滤+基于内容的推荐(浏览历史+购买历史+相似用户) | Amazon Personalize模式 |
| 内容推荐 | 基于用户兴趣画像推荐文章/视频/案例 | VWO: AI-powered recommendation |
| 实时行为推荐 | 当前session行为→实时推荐(看了A推荐B) | Session-based CF |
| 冷启动策略 | 新用户/新商品冷启动(热门+多样性+探索) | 推荐系统基础 |
| Propensity Score | 预测用户对特定动作的倾向性(购买/注册/流失) | Authencio: propensity modeling |
| 推荐解释 | "因为你浏览过X"——推荐理由展示 | 可解释AI |

---

#### 4. Generative UI (生成式界面) [中复杂度 | 1.5人月]

**现状**：零Generative UI

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| AI布局重组 | 根据用户propensity score实时重组页面布局(价格敏感→突出性价比) | Authencio: Generative UI |
| 动态文案生成 | AI根据用户画像实时生成个性化标题/描述/CTA文案 | StackAdapt: psychographic targeting |
| 个性化落地页 | 广告→落地页内容连续性: 同步广告文案/图片/优惠 | Authencio: Contextual Continuity |
| 正向强化触发 | 替代"仅剩2件"的稀缺恐吓——"你选得很好！已解锁免运费" | Authencio: Positive Reinforcement Nudges |
| 心理行为触发 | Dwell time异常→退出意图→智能弹窗；犹豫行为→社会证明 | Conversion Wax: neuro-marketing triggers |
| 频控与冷却 | 同一用户不重复触发相同个性化内容 | 体验保护 |

---

### P2 — 高级个性化能力

---

#### 5. 跨渠道个性化一致性 [中复杂度 | 1.0人月]

**描述**：用户在不同渠道看到一致的个性化体验

| 子功能 | 描述 |
|--------|------|
| 跨渠道会话拼接 | 网站浏览→App推送→邮件→网站回归——同一用户连续体验 |
| 体验连续性 | 用户在App加了购物车→打开网站时续接购物车状态 |
| 渠道偏好学习 | 学习用户偏好渠道(邮件响应好/推送不响应)→优化渠道组合 |
| 频率全局控制 | 跨渠道个性化曝光频率控制(用户一天最多看到3次个性化弹窗) |

---

#### 6. 个性化效果分析与优化 [低复杂度 | 0.5人月]

**描述**：个性化效果可测量、可优化

| 子功能 | 描述 |
|--------|------|
| 个性化Lift分析 | 个性化组vs对照组关键指标提升(转化率/ARPU/留存) |
| 规则效果排行 | 每条个性化规则的效果排行(触发次数/点击率/转化率/收入) |
| A/B建议 | AI检测规则效果→建议A/B测试→自动生成实验 |
| 个性化ROI | 个性化带来的增量收入vs个性化成本 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | AI/ML人月 | 测试人月 | 总计 |
|--------|------|---------|---------|----------|---------|------|
| P0 | 实时决策引擎 | 1.2 | 0.5 | 0.8 | 0.3 | 2.8 |
| P0 | 动态内容交换SDK | 0.8 | 1.0 | 0.2 | 0.2 | 2.2 |
| P1 | AI推荐引擎 | 0.5 | 0.3 | 1.2 | 0.2 | 2.2 |
| P1 | Generative UI | 0.5 | 0.7 | 0.3 | 0.2 | 1.7 |
| P2 | 跨渠道一致性 | 0.5 | 0.3 | 0.2 | 0.1 | 1.1 |
| P2 | 效果分析与优化 | 0.3 | 0.2 | 0.1 | 0.1 | 0.7 |
| | **合计** | **3.8** | **3.0** | **2.8** | **1.1** | **10.7** |

---

## 执行顺序

```
Sprint 1 (P0-Decision): 实时个性化决策引擎 — 2.8人月
  → 产出：Session行为追踪+意图识别+NBE决策+ms级API+决策日志

Sprint 2 (P0-SDK): 动态内容交换SDK — 2.2人月
  → 产出：Web/Mobile SDK+内容占位符+内容交换API+A/B注入+预览模式

Sprint 3 (P1-Recommend): AI推荐引擎 — 2.2人月
  → 产出：商品/内容/实时推荐+协同过滤+Propensity Score+推荐解释

Sprint 4 (P1-GenUI): Generative UI — 1.7人月
  → 产出：AI布局重组+动态文案+个性化落地页+正向强化触发

Sprint 5 (P2-高级): 跨渠道一致性+效果分析 — 1.8人月
  → 产出：跨渠道会话拼接+体验连续性+Lift分析+规则效果排行
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 决策延迟 | API响应>100ms→用户体验下降→放弃个性化 | CDN边缘决策+预计算+降级策略(展示默认内容) |
| 过度个性化 | 用户感觉被"监视"→不信任→流失 | 透明度+频率控制+opt-out+渐进式个性化 |
| 冷启动 | 新用户无行为数据→个性化不准确 | 热门推荐+多样性探索+快速学习+分段默认规则 |
| 内容碎片化 | 个性化规则爆炸→运营维护成本高 | 规则模板+AI自动优化+定期清理低效规则 |
| 隐私合规 | 实时行为追踪→GDPR/CCPA合规风险 | Consent检查+匿名化+数据最小化+Region-aware |
| 算法偏见 | 推荐算法产生偏见→不公平体验 | 算法审计+多样性约束+偏见检测+公平性指标 |

---

## 与其他方向的关系

| 方向 | 与㉞的关系 |
|------|----------|
| ⑬ 实时用户画像引擎 | 画像是决策输入(用户属性/标签/分群→个性化条件) |
| ㉜ 事件追踪SDK | 网站/App端行为数据采集是实时个性化的"Listen"环节 |
| ⑭ A/B测试平台 | 个性化效果必须通过A/B实验验证(个性化组vs对照组) |
| ⑨ 营销数据中台 | 个性化效果数据+推荐训练数据 |
| ④ AI原生平台 | AI推荐+意图识别+NBE预测是AI平台的核心应用 |
| ⑮ 营销资源中心 | 个性化内容(横幅/弹窗/推荐)素材管理 |
| ㉗ 偏好与同意管理 | Consent状态决定是否启用行为追踪和个性化 |
| ㉝ No-Code AI构建器 | AI可以推荐个性化规则+自动生成个性化内容变体 |
| ㉘ 动态内容引擎 | Liquid模板渲染个性化内容(画像属性→模板变量) |
