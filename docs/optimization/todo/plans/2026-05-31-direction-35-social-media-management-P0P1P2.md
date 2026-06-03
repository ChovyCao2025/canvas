# 方向㉟：社交媒体管理与发布 — 功能清单

> 定位：从"仅自有渠道(Email/SMS/Push)"升级为"全渠道包括社交媒体"——社交账号管理+内容发布与排期+社交监听+UGC管理+社交CRM+社交分析
> 策略评估：社交媒体是2026营销自动化不可缺失的触达渠道，Buffer/Sprout Social/Hootsuite/Later等专业工具已验证市场，营销平台需整合社交能力
> 竞品对标：Sprout Social(全功能社交管理+监听+分析)、Buffer(发布排期+AI内容生成+hashtag研究)、Hootsuite(企业级社交管理)、Later(社交+网红营销+社交监听)
> 建议：**P2建议做**，依赖⑮营销资源中心+㉓对话式营销成熟后启动，社交渠道拓宽是私域→公域的必然延伸

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Sprout Social: 21 Best Social Media Scheduling Tools 2026 | 全功能社交管理：发布排期+社交监听+分析+团队协作+网红营销 | https://sproutsocial.com/insights/social-media-scheduling-tools/ |
| Buffer: Best Social Media Management Tools 2026 | 发布排期+AI内容生成+hashtag研究+分析，$29/月起 | https://buffer.com/resources/best-social-media-management-tools/ |
| Zapier: 10 Best Social Media Management Tools 2026 | Buffer(排期)、Hootsuite(全功能)、Sprout Social(高级排期) | https://zapier.com/blog/best-social-media-management-tools/ |
| Later: Social Media Management + Influencer Marketing 2026 | 全功能：社交管理+网红营销+分析+社交监听+Link in Bio | https://buffer.com/resources/best-social-media-management-tools/ |
| SocialRails: Best All-in-One Social Media Scheduler 2026 | AI内容生成+hashtag研究+排期，$29/月 | https://www.reddit.com/r/buildinpublic/comments/1rf3jnm/ |
| Microposter: 12 Scheduling Tools for Social Media 2026 | 社交排期工具对比：功能/价格/优劣势 | https://microposter.so/blog/12-scheduling-tools-for-social-media-2026 |
| Klaviyo: Marketing Automation Trends 2026 | 全渠道Campaign管理+社交渠道是触达组合的必要部分 | https://www.klaviyo.com/blog/marketing-automation-trends |
| Storyteq: 5 Trends Reshaping Marketing Automation 2026 | 全渠道编排+社交渠道是用户体验连续性的关键一环 | https://storyteq.com/blog/the-future-of-marketing-automation-5-trends/ |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 自有渠道触达 | **完整** | Email/SMS/Push/InApp — 60+ NodeHandler | 仅自有渠道，无社交渠道 |
| 社交账号管理 | **不存在** | — | 无法连接微信公众号/微博/抖音/Instagram/Facebook/LinkedIn等 |
| 社交内容发布 | **不存在** | — | 无法在画布中发布社交帖子/推文 |
| 社交排期 | **不存在** | — | 无内容日历+定时发布 |
| 社交监听 | **不存在** | — | 无法监测品牌提及/关键词/竞品动态 |
| 社交互动管理 | **不存在** | — | 无法管理评论/私信/@回复 |
| UGC管理 | **不存在** | — | 无法收集+管理用户生成内容 |
| 社交分析 | **不存在** | — | 无社交渠道效果分析(曝光/互动/转化) |

### 关键洞察

中国市场 vs 全球市场社交渠道差异：
- **中国市场**：微信公众号(图文+模板消息)、微信视频号、小程序、微博、抖音、小红书、B站、快手
- **全球市场**：Facebook(Page/Group)、Instagram(Feed/Story/Reel)、LinkedIn、X(Twitter)、TikTok、YouTube、Pinterest

社交渠道在Canvas画布中的定位：
- **社交发布节点**：类似于SEND_EMAIL，新增SEND_SOCIAL_POST节点类型
- **社交触发节点**：用户评论/私信/关注→触发画布(类似EVENT_TRIGGER)
- **社交监听节点**：品牌提及/关键词→触发预警画布

---

## 功能清单

### P0 — 社交账号管理与内容发布

---

#### 1. 社交账号连接管理 [中复杂度 | 1.5人月]

**现状**：零社交账号管理

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 多渠道账号绑定 | OAuth连接微信公众号/微博/抖音/小红书/Instagram/Facebook/LinkedIn等 |
| 账号状态监控 | Token有效期/账号状态/API限额监控+到期提醒 |
| 多账号管理 | 同一渠道多账号管理(品牌主号+区域号+客服号) |
| 权限控制 | 谁可以发布到哪些社交账号(按角色/按账号) |
| 账号分组 | 按品牌/按区域/按用途分组管理 |
| 审批集成 | 社交内容发布前审批(对接⑧营销审批流) |

**数据库DDL**：

```sql
CREATE TABLE social_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    brand_id BIGINT COMMENT '所属品牌',
    platform VARCHAR(20) NOT NULL COMMENT 'WECHAT/WECHAT_MINI/WEIBO/DOUYIN/XIAOHONGSHU/INSTAGRAM/FACEBOOK/LINKEDIN/TWITTER/TIKTOK',
    account_name VARCHAR(200) NOT NULL COMMENT '账号名称',
    account_id VARCHAR(200) COMMENT '平台账号ID',
    access_token TEXT COMMENT 'OAuth Access Token(加密存储)',
    refresh_token TEXT COMMENT 'Refresh Token(加密存储)',
    token_expires_at DATETIME COMMENT 'Token过期时间',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/REVOKED/ERROR',
    avatar_url VARCHAR(500),
    follower_count INT DEFAULT 0,
    settings JSON COMMENT '账号级配置(发布频率/内容类型偏好)',
    last_synced_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_platform (tenant_id, platform),
    INDEX idx_brand (brand_id),
    INDEX idx_status (status)
) COMMENT '社交账号';
```

---

#### 2. 社交内容发布引擎 [中复杂度 | 2.0人月]

**现状**：零社交发布

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 多格式内容创建 | 图文/视频/短文案/长文章/Story/Reel/直播预告 |
| 内容编辑器 | 富文本+图片+视频+话题标签+@提及+表情+链接预览 |
| 多平台适配 | 同一内容→各平台格式自动适配(微博140字→公众号长文→Ins图片重点) |
| 定时发布 | 指定时间自动发布(支持时区感知) |
| 批量发布 | 批量创建多条内容+批量排期 |
| 发布预览 | 各平台实际展示效果预览(移动端+桌面端) |
| 内容库复用 | 从⑮营销资源中心引用素材 |
| 发布历史 | 已发布内容查询+编辑+删除+重新发布 |

**社交发布节点类型(画布扩展)**：

```java
// 新增节点类型
public static final String SEND_SOCIAL_POST = "SEND_SOCIAL_POST";

// 新增SocialPostHandler — 类似于SendEmailHandler
@NodeHandlerType(NodeType.SEND_SOCIAL_POST)
public class SocialPostHandler implements NodeHandler {
    // config: { platform, accountId, content, mediaUrls, scheduledAt, ... }
    // 调用各平台API发布内容
    // 返回 { postId, postUrl, platform, publishedAt }
}
```

**社交内容DDL**：

```sql
CREATE TABLE social_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    social_account_id BIGINT NOT NULL,
    post_type VARCHAR(20) NOT NULL COMMENT 'TEXT/IMAGE/VIDEO/CAROUSEL/STORY/ARTICLE/LIVE',
    content TEXT NOT NULL COMMENT '正文',
    media_urls JSON COMMENT '媒体文件URL列表',
    hashtags JSON COMMENT '话题标签',
    mentions JSON COMMENT '@提及用户列表',
    link_url VARCHAR(500) COMMENT '附带链接',
    scheduled_at DATETIME COMMENT '定时发布时间',
    published_at DATETIME COMMENT '实际发布时间',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SCHEDULED/PUBLISHING/PUBLISHED/FAILED/DELETED',
    platform_post_id VARCHAR(200) COMMENT '平台返回的帖子ID',
    platform_post_url VARCHAR(500) COMMENT '帖子链接',
    error_message TEXT COMMENT '发布失败原因',
    canvas_run_id VARCHAR(64) COMMENT '触发的画布执行ID',
    created_by VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account (social_account_id),
    INDEX idx_scheduled (scheduled_at),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '社交帖子';
```

---

### P1 — 社交互动与监听

---

#### 3. 社交互动管理 [中复杂度 | 1.0人月]

**现状**：零社交互动

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 评论管理 | 查看/回复/隐藏/删除评论+自动回复规则 |
| 私信管理 | 统一私信收件箱+自动回复+关键词路由 |
| @提及监控 | 实时监控@提及+触发预警/画布 |
| 互动统计 | 点赞/评论/分享/收藏/点击实时统计 |
| 情感分析 | AI分析评论/私信情感(正面/中性/负面)→自动分流 |
| 社交CRM | 互动用户自动关联画像→打标签→进入旅程 |

---

#### 4. 社交监听 [中复杂度 | 1.0人月]

**现状**：零社交监听

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 品牌提及监听 | 实时监测全平台品牌名称/产品名称提及 |
| 关键词监听 | 行业关键词/竞品名称/热点话题实时监控 |
| 竞品动态 | 竞品社交账号动态追踪+对比分析 |
| 舆情预警 | 负面提及激增→触发告警→启动危机应对画布 |
| 趋势发现 | 热点话题/热门hashtag自动发现→推荐蹭热点内容 |
| 社交聆听报表 | 品牌声量/情感趋势/话题云/竞品对比 |

---

### P2 — 社交分析与UGC

---

#### 5. 社交分析仪表盘 [低复杂度 | 0.5人月]

**描述**：社交渠道效果分析

| 子功能 | 描述 |
|--------|------|
| 发布效果分析 | 帖子级曝光/互动/点击/转化漏斗 |
| 账号增长分析 | 粉丝增长/流失/互动率趋势 |
| 内容效果对比 | 不同内容类型/发布时间/话题效果对比 |
| 最佳发布时间 | AI分析历史数据→推荐最佳发布时间 |
| 跨平台对比 | 各平台效果横向对比+渠道ROI |
| 社交归因 | 社交互动→网站访问→注册→购买全链路归因 |

---

#### 6. 用户生成内容(UGC)管理 [低复杂度 | 0.5人月]

**描述**：UGC收集+管理+复用

| 子功能 | 描述 |
|--------|------|
| UGC收集 | 通过hashtag/@提及/评论收集用户生成内容 |
| UGC审核 | UGC内容审核+授权确认+分类标签 |
| UGC复用 | 审核通过的UGC→进入⑮资源中心→可复用于Campaign |
| UGC激励 | 优质UGC创作者自动奖励(积分/优惠券)→对接㉒积分/㉑券 |
| UGC权益管理 | 内容授权记录+创作者署名+使用范围管理 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 社交账号连接管理 | 1.0 | 0.5 | 0.2 | 1.7 |
| P0 | 社交内容发布引擎 | 1.2 | 0.8 | 0.2 | 2.2 |
| P1 | 社交互动管理 | 0.7 | 0.3 | 0.1 | 1.1 |
| P1 | 社交监听 | 0.5 | 0.5 | 0.1 | 1.1 |
| P2 | 社交分析仪表盘 | 0.3 | 0.2 | 0.1 | 0.6 |
| P2 | UGC管理 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **4.0** | **2.5** | **0.8** | **7.3** |

---

## 执行顺序

```
Sprint 1 (P0-账号): 社交账号连接管理 — 1.7人月
  → 产出：多渠道OAuth+Token管理+多账号+权限+审批集成

Sprint 2 (P0-发布): 社交内容发布引擎 — 2.2人月
  → 产出：多格式内容创建+多平台适配+定时/批量发布+预览+画布节点

Sprint 3 (P1-互动+监听): 社交互动+监听 — 2.2人月
  → 产出：评论/私信管理+情感分析+品牌监听+竞品监测+舆情预警

Sprint 4 (P2-高级): 社交分析+UGC — 1.2人月
  → 产出：社交仪表盘+最佳发布时间+UGC收集+审核+复用
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| API变更 | 社交平台API频繁变更→功能中断 | 抽象层隔离+版本适配+灰度升级+降级策略 |
| 账号安全 | Token泄露→社交账号被恶意操作 | 加密存储+最小权限+操作审计+异常检测 |
| 平台差异 | 各平台API能力差异大→功能不一致 | 平台能力矩阵+降级适配+功能标注+文档透明 |
| 发布失败 | API限流/网络问题→定时发布失败 | 重试机制+失败告警+手动补发+发布前校验 |
| 合规风险 | 社交平台内容规范+广告法+行业规范 | 内容审核+敏感词过滤+发布前合规检查 |

---

## 与其他方向的关系

| 方向 | 与㉟的关系 |
|------|----------|
| ⑮ 营销资源中心 | 社交素材(图片/视频/模板)统一管理+UGC入库 |
| ⑧ 营销审批流 | 社交内容发布前审批(品牌合规check) |
| ㉓ 对话式营销与社交渠道 | 社交私信是对话式营销的重要渠道 |
| ① 营销自动化深度 | SEND_SOCIAL_POST作为新节点类型扩展画布能力 |
| ⑨ 营销数据中台 | 社交效果指标纳入全渠道归因+ROI分析 |
| ㉚ 多品牌代理平台 | 不同品牌独立社交账号+内容隔离 |
| ㉛ 国际化与多语言 | 不同地区的社交平台(微信vs WhatsApp)和多语言内容 |
| ⑦ 合规渠道护城河 | 社交内容合规审核+行业规范检查 |
