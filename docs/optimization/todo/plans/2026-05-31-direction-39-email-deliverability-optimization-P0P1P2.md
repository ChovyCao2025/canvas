# 方向㊴：邮件送达率优化平台 — 功能清单

> 定位：从"能发邮件"升级为"邮件能进收件箱"——发件人信誉监控+垃圾邮件评分+收件箱位置测试+DMARC/BIMI认证+专属IP管理+黑名单监控
> 策略评估：邮件送达率(deliverability)是Email营销的"最后一公里"——内容再好，进垃圾箱等于零。2026年Google/Yahoo新认证要求(BIMI/SPF/DKIM/DMARC强制)使送达率优化成为刚需
> 竞品对标：Glock Apps(收件箱位置测试)、Mailtrap(送达率测试+评分)、SendForensics(全功能送达率平台+配置/认证/收件箱评分)、Truelist(送达率工具对比)
> 建议：**P1建议做**，依赖①营销自动化深度的邮件触达完善后启动，送达率直接影响邮件渠道ROI

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Truelist: Best Email Deliverability Tools 2026 | 送达率工具核心：收件箱位置监控+域名认证+发件人信誉保护 | https://truelist.io/blog/best-email-deliverability-tools |
| Mailtrap: 17 Best Email Deliverability Tools 2026 | SendForensics: 全功能送达率平台+配置/认证/收件箱位置综合评分 | https://mailtrap.io/blog/email-deliverability-tools/ |
| EmailToolTester: Best Deliverability Tools 2026 | Glock Apps: 收件箱位置监控(收件箱/垃圾箱/丢失) | https://www.emailtooltester.com/en/blog/best-email-deliverability-tools/ |
| emfluence: Ultimate Guide to Email Deliverability 2026 | 送达率关键: 互动率+认证(SPF/DKIM/DMARC/BIMI)+内容质量 | https://emfluence.com/blog/the-ultimate-guide-to-email-deliverability-in-2026 |
| Digital Applied: Email Deliverability Benchmarks 2026 | 2026基准: 收件箱位置+垃圾邮件率+退信率+发件人信誉+DMARC/BIMI采用率 | https://www.digitalapplied.com/blog/email-deliverability-benchmarks-2026-industry |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 邮件发送 | **完整** | SendEmailHandler+Email Send API | 能发邮件，但无送达率监控 |
| SPF/DKIM | **未确认** | — | 需检查DNS是否配置SPF/DKIM/DMARC |
| 送达率监控 | **不存在** | — | 无收件箱位置/垃圾邮件率/退信率实时监控 |
| 信誉管理 | **不存在** | — | 无发件人信誉评分+IP/域信誉监控 |
| 黑名单监控 | **不存在** | — | 无Spamhaus/Barracuda等黑名单检查 |
| 垃圾评分 | **不存在** | — | 无邮件内容垃圾评分预检(SpamAssassin) |
| 专属IP | **不存在** | — | 无专属发件IP+预热管理 |

---

## 功能清单

### P0 — 送达率监控与诊断

#### 1. 收件箱位置监控 [中复杂度 | 1.0人月]

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 种子邮箱网络 | 部署种子邮箱(Gmail/Outlook/Yahoo/QQ/163等)→定期测试送达 | Glock Apps: inbox placement monitoring |
| 收件箱位置报告 | 收件箱/促销/垃圾箱/丢失——按邮件服务商分组的送达率报告 | Mailtrap: inbox placement tracking |
| 退信分析 | 硬退信(邮箱不存在)/软退信(邮箱满/临时拒绝)→分类统计+趋势 | emfluence: bounce analysis |
| 趋势告警 | 送达率突然下降→实时告警 | Truelist: reputation monitoring |

#### 2. 认证配置管理 [低复杂度 | 0.5人月]

| 子功能 | 描述 |
|--------|------|
| SPF配置检查 | SPF记录验证+配置向导+常见错误修复 |
| DKIM配置检查 | DKIM密钥生成+签名验证+轮转管理 |
| DMARC配置 | DMARC策略(p=none/quarantine/reject)+报告分析 |
| BIMI支持 | Brand Indicators for Message Identification——品牌Logo在收件箱展示 |

### P1 — 垃圾评分与内容优化

#### 3. 垃圾邮件评分引擎 [中复杂度 | 1.0人月]

| 子功能 | 描述 |
|--------|------|
| SpamAssassin集成 | 邮件内容垃圾评分(SpamAssassin规则引擎) |
| 内容分析 | 疑似垃圾词检测+过度使用大写/感叹号/链接检测 |
| 文本:图片比例 | 纯图邮件检测→提醒优化 |
| 个性化程度 | 检测是否包含收件人姓名+动态内容(未个性化邮件更易进垃圾箱) |
| 评分历史 | 每封邮件垃圾评分历史+趋势+优化建议 |

#### 4. 发件人信誉管理 [低复杂度 | 0.5人月]

| 子功能 | 描述 |
|--------|------|
| IP信誉监控 | Sender Score/Cisco Talos/Microsoft SNDS数据整合 |
| 域信誉监控 | 域名信誉(Google Postmaster Tools/Microsoft JMRP) |
| 黑名单检查 | 100+黑名单自动检查(Spamhaus/Barracuda/Invaluement/SORBS) |
| 专属IP预热 | 新IP→逐步增加发送量→达到目标量(预热计划) |

### P2 — 高级优化

#### 5. 送达率优化建议 [低复杂度 | 0.5人月]
AI分析送达率影响因素(内容/频率/认证/信誉)→给出优化建议+最佳实践库

#### 6. 竞品送达率对比 [低复杂度 | 0.3人月]
行业送达率基准对比+同类发送者排名+趋势分析

---

## 工作量估算

| 优先级 | 功能 | 总计 |
|--------|------|------|
| P0 | 收件箱位置监控 | 1.1人月 |
| P0 | 认证配置管理 | 0.6人月 |
| P1 | 垃圾邮件评分引擎 | 1.1人月 |
| P1 | 发件人信誉管理 | 0.6人月 |
| P2 | 优化建议+竞品对比 | 0.9人月 |
| | **合计** | **4.3人月** |

## 关键依赖

①营销自动化深度(邮件触达完善) + ⑨数据中台(送达率数据)
