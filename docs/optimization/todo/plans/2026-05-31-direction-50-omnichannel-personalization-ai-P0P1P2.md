# 方向㊿：全渠道个人化AI — 功能清单

> 定位：从"手动选择渠道+时间+内容"升级为"AI统一优化全渠道个人化"——Send Time Optimization(STO)+Channel Optimization+Content Optimization+Frequency Optimization
> 策略评估：Klaviyo 2026趋势"Self-optimizing systems: 实时调整creative/timing/channel mix"，Braze/CleverTap已将STO作为标配。这是营销自动化的"自动驾驶"
> 竞品对标：Braze(STO+Canvas+Content Cards)、CleverTap(STO+Live Personalization)、Klaviyo(AI Copilot: 自动调优)、Iterable(Brand Affinity+STO)
> 建议：**P2建议做**，依赖④AI原生平台+⑨数据中台+①营销深度+㉞个性化引擎成熟后启动

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Klaviyo: Marketing Automation Trends 2026 | Self-optimizing systems: 预测模型+实时调整creative/timing/channel mix | https://www.klaviyo.com/blog/marketing-automation-trends |
| Braze: Send Time Optimization | STO: ML分析用户历史打开/点击行为→推荐个性化最佳发送时间 | https://www.braze.com/ |
| CleverTap: Send Time Optimization + Live Personalization | STO+实时个性化+多渠道优化 | https://clevertap.com/ |

---

## 现状盘点

| 功能 | 实现程度 | 差距 |
|------|----------|------|
| 画布定时触发 | **完整** | 定时触发(Cron/ScheduledTrigger)，所有人同一时间 |
| 频率控制 | **完整** | MarketingPolicyService频控，但所有人同一规则 |
| 个人化时机 | **不存在** | 无法为每个用户选择最佳发送时间 |
| 个人化渠道 | **不存在** | 无法AI学习用户渠道偏好(邮件vs Push vs SMS) |
| 个人化频率 | **不存在** | 同频率规则→一个用户可承受3封/周，另一个可能只接受1封 |

---

## 功能清单

### P0 — 发送时机优化(STO) [2.0人月]

| 子功能 | 描述 |
|--------|------|
| 个人化STO | ML学习每个用户的历史打开/点击时间→预测最佳发送时间窗口 |
| 时区智能 | 自动识别用户时区+STO在本地最佳时间发送 |
| 冷启动策略 | 新用户无历史→使用人群级最佳时间+快速学习 |
| STO效果分析 | STO vs 固定时间的效果对比(打开率/点击率/转化率提升) |

### P1 — 渠道与频率优化 [1.5人月]

| 子功能 | 描述 |
|--------|------|
| 渠道偏好学习 | ML分析每个用户对Email/Push/SMS/InApp的响应率→推荐最佳渠道 |
| 渠道组合优化 | 同一消息→先Push→2h未打开→自动Email→24h未打开→自动SMS |
| 个人化频率 | ML学习每个用户可承受的触达频率→差异化的频控上限 |
| 抑制智能 | 检测到用户进入沉默期→自动降低触达频率→防止退订 |

### P2 — 内容与全链路优化 [1.5人月]

| 子功能 | 描述 |
|--------|------|
| 内容智能匹配 | AI根据用户偏好→从素材库选择最佳创意(A版vs B版图/短文案vs长文案) |
| 全链路调优 | STO(时机)+Channel Opt(渠道)+Content Opt(内容)+Freq Opt(频率)→统一优化模型 |
| 实时自适应 | Campaign执行中→实时监测效果→自动调整参数→持续优化 |

---

## 工作量: 5.0人月 | 依赖: ④AI原生平台+⑨数据中台+①营销深度+⑬画像+㉞个性化引擎
