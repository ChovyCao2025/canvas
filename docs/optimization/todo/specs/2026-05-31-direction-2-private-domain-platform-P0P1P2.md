# 方向②：私域运营中台 — 功能清单

> 定位：从"触达工具"升级为"用户价值管理平台"，主打企微+小程序+私域
> 策略评估：护城河最深（DAG画布+企微SCRM无人做）、引擎复用80%、市场时间窗最宽
> 推荐：**主方向**，合规+渠道为底座，AI为远期
> 竞品对标：Convertlab（SCRM+MA但画布弱20节点）、致趣百川（SCRM强但无画布）、神策（画布弱30节点）

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 对标Convertlab |
|------|----------|-------------|----------------|
| 企微集成 | **不存在** | — | 0% |
| 企微客户管理 | **不存在** | — | 0% |
| 企微群聊运营 | **不存在** | — | 0% |
| 企微朋友圈 | **不存在** | — | 0% |
| 企微欢迎语 | **不存在** | — | 0% |
| 小程序触达 | **不存在** | — | 0% |
| 小程序事件追踪 | **不存在** | — | 0% |
| 裂变营销引擎 | **不存在** | — | 0% |
| 分享解锁 | **不存在** | — | 0% |
| 会员等级体系 | **不存在** | CustomerProfileDO.lifecycleStage（仅字段） | 0% |
| 积分系统 | **部分** | PointsOperationHandler + CustomerPointsLedgerDO（流水层，无余额） | 30% |
| RFM/RFE分层 | **不存在** | ScoringHandler（通用评分，无RFM预置） | 0% |
| 优惠券全生命周期 | **部分** | CouponHandler（发券客户端，无券管理） | 15% |
| 生命周期阶段 | **部分** | lifecycleStage字段+UpdateProfileHandler+V55/V75示例 | 10% |
| 运营SOP模板 | **不存在** | — | 0% |

**总体评估**：14项功能中10项完全不存在，4项有基础支撑。现有实现集中在引擎通用基础设施层，非面向私域场景的业务闭环。

---

## P0 — 必须有，否则不是私域中台

---

### 1. 企微SCRM模块 [高复杂度 | 8.0人月]

**现状**：完全不存在。SendWechatHandler仅是通用微信渠道，通过ReachDeliveryService调用外部触达平台，不包含企微API SDK。

**竞品对标**：
- 致趣百川：企微SCRM完整（客户管理+群运营+朋友圈+欢迎语+话术库+素材库）
- Convertlab：企微SCRM+MA联动
- 尘锋SCRM：纯SCRM（无画布）

#### 1.1 企微API SDK集成

**描述**：集成企业微信服务端API，作为所有企微功能的基础。

**需集成的API模块**：

| API模块 | 核心接口 | 用途 |
|---------|---------|------|
| 通讯录 | 获取部门/成员列表 | 员工管理 |
| 外部联系人 | 获取/编辑/批量获取外部联系人 | 客户管理 |
| 客户群 | 获取群列表/群详情/群成员 | 群运营 |
| 客户朋友圈 | 发表/获取朋友圈 | 朋友圈营销 |
| 消息推送 | 发送应用消息/欢迎语 | 触达 |
| 素材管理 | 上传临时/永久素材 | 素材库 |
| 客户联系 | 配置客户联系规则/欢迎语 | 客户运营 |
| 回调通知 | 接收事件通知 | 事件驱动 |

**技术方案**：
```java
// 企微SDK核心组件
public class WeComClient {
    private final String corpId;
    private final String corpSecret;
    private final String agentId;
    private final RedisTemplate redisTemplate; // access_token缓存

    // access_token管理（有效期7200秒，提前300秒刷新）
    public String getAccessToken() {
        String token = redisTemplate.opsForValue().get("wecom:access_token:" + corpId);
        if (token == null) {
            token = refreshAccessToken();
        }
        return token;
    }

    // 通用API调用
    public <T> T call(String url, Object body, Class<T> responseType) {
        // 1. 获取access_token
        // 2. 发送HTTP请求
        // 3. 检查errcode
        // 4. 返回结果
    }
}
```

**access_token管理**：
- Redis缓存：`wecom:access_token:{corpId}` → token值，TTL 6900秒
- 自动刷新：定时任务每6800秒刷新一次
- 多应用支持：每个agentId独立token

**回调验签**：
```java
public class WeComCallbackVerifier {
    // 验证URL有效性（GET请求）
    public String verifyUrl(String msgSignature, String timestamp, String nonce, String echostr) {
        // SHA1(sort(token, timestamp, nonce, echostr)) == msgSignature
        // AES解密echostr → 返回明文
    }

    // 解密回调数据（POST请求）
    public String decryptData(String msgSignature, String timestamp, String nonce, String encrypt) {
        // SHA1(sort(token, timestamp, nonce, encrypt)) == msgSignature
        // AES解密encrypt → 返回XML明文
    }
}
```

**配置项**：
```yaml
canvas:
  wecom:
    corp-id: ${WECOM_CORP_ID}
    corp-secret: ${WECOM_CORP_SECRET}
    agent-id: ${WECOM_AGENT_ID}
    token: ${WECOM_CALLBACK_TOKEN}
    encoding-aes-key: ${WECOM_ENCODING_AES_KEY}
    callback-url: ${WECOM_CALLBACK_URL}
```

**数据库DDL**：
```sql
-- V91__wecom_integration.sql

CREATE TABLE wecom_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    corp_id VARCHAR(64) NOT NULL COMMENT '企业ID',
    corp_secret VARCHAR(200) NOT NULL COMMENT '应用Secret（加密存储）',
    agent_id INT NOT NULL COMMENT '应用AgentId',
    callback_token VARCHAR(100) NOT NULL COMMENT '回调Token',
    encoding_aes_key VARCHAR(100) NOT NULL COMMENT '回调EncodingAESKey',
    callback_url VARCHAR(500) COMMENT '回调URL',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_corp_tenant (corp_id, tenant_id)
) COMMENT '企微配置';

CREATE TABLE wecom_event_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型',
    external_userid VARCHAR(64) COMMENT '外部联系人ID',
    user_id VARCHAR(64) COMMENT '企微成员ID',
    raw_data JSON NOT NULL COMMENT '原始回调数据',
    processed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已处理',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event_type (event_type),
    INDEX idx_external_userid (external_userid),
    INDEX idx_processed (processed),
    INDEX idx_created (created_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '企微事件日志';
```

**API接口**：
```
# 企微配置
POST   /api/wecom/config                      创建/更新企微配置
GET    /api/wecom/config                      查询企微配置
POST   /api/wecom/config/verify               验证回调URL

# 企微回调（企微服务器调用）
GET    /api/wecom/callback                    验证URL有效性
POST   /api/wecom/callback                    接收事件回调
```

---

#### 1.2 企微客户管理

**描述**：同步和管理企微外部联系人，打通企微客户与系统用户画像。

**核心功能**：

| 子功能 | 描述 | 技术方案 |
|--------|------|---------|
| 外部联系人同步 | 定时同步企微外部联系人到本地 | 定时任务调用企微API → 批量upsert |
| 客户-员工关系 | 记录哪个员工添加了哪个客户 | 跟进记录表 |
| 客户画像打通 | 企微external_userid ↔ 系统user_id映射 | identity_map表 |
| 客户标签同步 | 企微客户标签 ↔ 系统用户标签双向同步 | 标签映射+同步任务 |
| 客户流失检测 | 客户删除员工时自动标记 | 回调事件处理 |
| 客户分配 | 员工离职后客户自动分配给其他员工 | 离职分配规则 |

**客户-用户身份映射**：
```
企微场景下的身份链：
  external_userid (企微外部联系人)
    ↔ union_id (微信开放平台)
    ↔ open_id (公众号/小程序)
    ↔ user_id (系统内部)

映射表：customer_identity_map
  (user_id, identity_type, identity_value)
  identity_type: WECOM_EXTERNAL / WECHAT_OPENID / WECHAT_UNIONID / MINI_OPENID
```

**数据库DDL**：
```sql
CREATE TABLE wecom_external_contact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    external_userid VARCHAR(64) NOT NULL COMMENT '企微外部联系人ID',
    name VARCHAR(100) COMMENT '姓名',
    avatar VARCHAR(500) COMMENT '头像URL',
    type TINYINT NOT NULL DEFAULT 0 COMMENT '0-微信用户 1-企微用户',
    gender TINYINT COMMENT '0-未知 1-男 2-女',
    union_id VARCHAR(64) COMMENT '微信UnionID',
    system_user_id VARCHAR(64) COMMENT '关联系统用户ID',
    tags JSON COMMENT '企微标签列表',
    description VARCHAR(500) COMMENT '描述',
    last_synced_at DATETIME COMMENT '最近同步时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_external_userid (external_userid, tenant_id),
    INDEX idx_system_user (system_user_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '企微外部联系人';

CREATE TABLE wecom_follow_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    external_userid VARCHAR(64) NOT NULL COMMENT '外部联系人ID',
    follow_user_id VARCHAR(64) NOT NULL COMMENT '跟进员工企微ID',
    follow_user_name VARCHAR(100) COMMENT '跟进员工姓名',
    remark VARCHAR(100) COMMENT '备注名',
    remark_corp VARCHAR(100) COMMENT '备注企业',
    remark_mobiles JSON COMMENT '备注手机号',
    add_way VARCHAR(30) COMMENT '添加方式',
    tags JSON COMMENT '跟进人标签',
    status VARCHAR(20) NOT NULL DEFAULT 'FOLLOWING' COMMENT 'FOLLOWING/DELETED/TRANSFERRED',
    createtime DATETIME COMMENT '添加时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_external_follow (external_userid, follow_user_id, tenant_id),
    INDEX idx_follow_user (follow_user_id),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '企微客户跟进关系';

CREATE TABLE customer_identity_map (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL COMMENT '系统用户ID',
    identity_type VARCHAR(30) NOT NULL COMMENT 'WECOM_EXTERNAL/WECHAT_OPENID/WECHAT_UNIONID/MINI_OPENID',
    identity_value VARCHAR(128) NOT NULL COMMENT '外部ID值',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_type_value (identity_type, identity_value, tenant_id),
    INDEX idx_user (user_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '用户身份映射';
```

**API接口**：
```
# 外部联系人
GET    /api/wecom/contacts                    外部联系人列表（分页+搜索+标签筛选）
GET    /api/wecom/contacts/{externalUserId}   联系人详情
POST   /api/wecom/contacts/sync               手动触发同步
PUT    /api/wecom/contacts/{externalUserId}/tags 更新标签

# 跟进关系
GET    /api/wecom/contacts/{externalUserId}/followers 跟进人列表
GET    /api/wecom/followers/{userId}/contacts  员工的客户列表

# 身份映射
GET    /api/wecom/identity/{userId}           查询用户身份映射
POST   /api/wecom/identity/bind               手动绑定身份
```

---

#### 1.3 企微群聊运营

**描述**：管理企微客户群，支持群SOP、群自动回复、群数据统计。

**核心功能**：

| 子功能 | 描述 | 技术方案 |
|--------|------|---------|
| 群列表同步 | 定时同步企微客户群 | 企微API → wecom_group_chat表 |
| 群成员管理 | 查看群成员、自动踢人规则 | 群成员表+规则引擎 |
| 群SOP | 定时提醒群主执行运营动作（发消息/发活动/跟进） | SOP模板+定时任务+企微应用消息提醒 |
| 群自动回复 | 关键词自动回复 | 关键词规则表+企微消息API |
| 群欢迎语 | 新成员入群自动欢迎 | 欢迎语模板+回调触发 |
| 群数据统计 | 群人数趋势、活跃度、流失率 | 统计任务+Dashboard |

**群SOP流程**：
```
1. 运营创建SOP模板（如"每日早安问候"）
2. 绑定到指定群
3. 定时任务触发 → 企微应用消息提醒群主
4. 群主点击 → 跳转到SOP详情页 → 一键发送
5. 记录执行状态
```

**数据库DDL**：
```sql
CREATE TABLE wecom_group_chat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chat_id VARCHAR(64) NOT NULL COMMENT '企微群ID',
    name VARCHAR(200) NOT NULL COMMENT '群名',
    owner VARCHAR(64) NOT NULL COMMENT '群主企微ID',
    owner_name VARCHAR(100) COMMENT '群主姓名',
    member_count INT NOT NULL DEFAULT 0 COMMENT '群成员数',
    max_member_count INT NOT NULL DEFAULT 0 COMMENT '群上限',
    notice VARCHAR(500) COMMENT '群公告',
    create_time DATETIME COMMENT '创建时间',
    last_synced_at DATETIME COMMENT '最近同步时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_chat_id (chat_id, tenant_id),
    INDEX idx_owner (owner),
    INDEX idx_tenant (tenant_id)
) COMMENT '企微客户群';

CREATE TABLE wecom_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chat_id VARCHAR(64) NOT NULL COMMENT '群ID',
    user_id VARCHAR(64) NOT NULL COMMENT '成员企微ID',
    user_name VARCHAR(100) COMMENT '成员名称',
    is_owner TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否群主',
    join_time DATETIME COMMENT '入群时间',
    leave_time DATETIME COMMENT '离群时间',
    status VARCHAR(20) NOT NULL DEFAULT 'IN' COMMENT 'IN/OUT',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat (chat_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '企微群成员';

CREATE TABLE wecom_group_sop (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT 'SOP名称',
    steps JSON NOT NULL COMMENT 'SOP步骤 [{"time":"09:00","action":"SEND_MESSAGE","content":"早安问候"}]',
    chat_ids JSON COMMENT '绑定的群ID列表',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
) COMMENT '群SOP模板';

CREATE TABLE wecom_group_auto_reply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chat_id VARCHAR(64) COMMENT '群ID（空=全局规则）',
    keyword VARCHAR(100) NOT NULL COMMENT '关键词',
    match_type VARCHAR(20) NOT NULL DEFAULT 'CONTAINS' COMMENT 'EXACT/CONTAINS/REGEX',
    reply_type VARCHAR(20) NOT NULL COMMENT 'TEXT/IMAGE/LINK/MINI_PROGRAM',
    reply_content JSON NOT NULL COMMENT '回复内容',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat (chat_id),
    INDEX idx_keyword (keyword),
    INDEX idx_tenant (tenant_id)
) COMMENT '群自动回复规则';
```

---

#### 1.4 企微朋友圈

**描述**：通过企微员工朋友圈发布营销内容。

**核心功能**：

| 子功能 | 描述 |
|--------|------|
| 朋友圈发布 | 创建朋友圈内容（文本+图片/视频/网页），指定员工发布 |
| 素材管理 | 朋友圈素材库（图片/视频/网页链接） |
| 发布任务 | 创建发布任务 → 企微应用消息提醒员工 → 员工确认发布 |
| 互动统计 | 点赞/评论数统计 |

**数据库DDL**：
```sql
CREATE TABLE wecom_moments_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT NOT NULL COMMENT '朋友圈文本内容',
    media_type VARCHAR(20) COMMENT 'IMAGE/VIDEO/LINK',
    media_urls JSON COMMENT '素材URL列表（最多9张图）',
    link_url VARCHAR(500) COMMENT '网页链接URL',
    link_title VARCHAR(200) COMMENT '网页链接标题',
    target_users JSON NOT NULL COMMENT '发布员工列表',
    publish_time DATETIME COMMENT '计划发布时间',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/PUBLISHED/CANCELLED',
    created_by VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '企微朋友圈发布任务';
```

---

#### 1.5 企微欢迎语

**描述**：新客户添加员工时自动发送欢迎语。

**核心功能**：

| 子功能 | 描述 |
|--------|------|
| 欢迎语模板 | 文本+图片/网页/小程序卡片 |
| 分时段欢迎语 | 不同时段不同欢迎语（如工作时间/非工作时间） |
| 员工个性化 | 不同员工不同欢迎语 |
| 画布联动 | 欢迎语事件触发画布执行（如新客欢迎旅程） |

**与画布引擎联动**：
```
1. 企微回调：添加外部联系人事件
2. 发送欢迎语（企微API）
3. 触发EVENT_TRIGGER → 启动"新客欢迎旅程"画布
4. 画布执行：打标签→发优惠券→邀请进群→...
```

**数据库DDL**：
```sql
CREATE TABLE wecom_welcome_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '欢迎语名称',
    type VARCHAR(20) NOT NULL DEFAULT 'DEFAULT' COMMENT 'DEFAULT/TIME_BASED/USER_BASED',
    text_content VARCHAR(500) NOT NULL COMMENT '文本内容',
    attachment_type VARCHAR(20) COMMENT 'IMAGE/LINK/MINI_PROGRAM',
    attachment_content JSON COMMENT '附件内容',
    time_rules JSON COMMENT '分时段规则 [{"start":"09:00","end":"18:00","message_id":1}]',
    target_users JSON COMMENT '适用员工列表（空=全部）',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
) COMMENT '企微欢迎语';
```

---

### 2. 小程序触达+事件追踪 [中复杂度 | 3.0人月]

**现状**：完全不存在

**竞品对标**：
- 有赞：小程序+企微+SCRM全链路
- 微盟：小程序商城+营销+SCRM
- Convertlab：小程序触达+事件追踪

#### 2.1 小程序订阅消息

**描述**：通过微信小程序订阅消息API触达用户。

**技术方案**：
```java
public class MiniProgramSubscribeMessageHandler extends AbstractSendMessageHandler {
    // 调用微信小程序订阅消息API
    // POST https://api.weixin.qq.com/cgi-bin/message/subscribe/send
    // 参数：touser(openid), template_id, page, data, miniprogram_state
}
```

**订阅消息流程**：
```
1. 用户在小程序中点击"允许通知" → 获取一次性订阅授权
2. 系统记录用户订阅状态
3. 画布执行到小程序发送节点 → 调用订阅消息API
4. 用户点击消息 → 跳转到小程序指定页面
```

**限制**：
- 每次订阅只能发一条消息（一次性订阅）
- 长期订阅需微信审核通过
- 模板需在微信后台申请

**新增NodeHandler**：
- `MINI_SUBSCRIBE_MESSAGE` — 小程序订阅消息发送节点
- 注册到 `SEND_ACTION` 节点类型

#### 2.2 小程序事件追踪SDK

**描述**：提供小程序端埋点SDK，采集用户行为事件。

**SDK功能**：
```javascript
// 小程序SDK（npm包）
import { CanvasTracker } from '@canvas/mini-tracker';

const tracker = new CanvasTracker({
  appId: 'wx_xxx',
  serverUrl: 'https://api.canvas.com/track',
  autoTrack: {
    pageView: true,      // 自动采集页面浏览
    share: true,         // 自动采集分享事件
    launch: true,        // 自动采集小程序启动
    show: true           // 自动采集小程序切前台
  }
});

// 自定义事件
tracker.track('product_view', { productId: 'P001', price: 99.9 });
tracker.track('add_to_cart', { productId: 'P001', quantity: 2 });
tracker.track('purchase', { orderId: 'O001', amount: 199.8 });
```

**事件采集端点**：
```
POST /api/track/mini
Headers: X-App-Id, X-SDK-Version
Body: {
  "events": [
    { "type": "page_view", "path": "/pages/product", "timestamp": 1717200000000 },
    { "type": "custom", "name": "purchase", "properties": {"amount": 199.8} }
  ],
  "userId": "openid_xxx",
  "deviceId": "device_xxx"
}
```

**与画布引擎联动**：
- 小程序事件 → EVENT_TRIGGER → 启动画布
- 如：用户浏览商品3次未购买 → 触发"购物车挽回"画布

**数据库DDL**：
```sql
CREATE TABLE mini_subscribe_auth (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    openid VARCHAR(64) NOT NULL COMMENT '小程序openid',
    template_id VARCHAR(64) NOT NULL COMMENT '订阅消息模板ID',
    auth_count INT NOT NULL DEFAULT 0 COMMENT '剩余可发送次数',
    last_auth_time DATETIME COMMENT '最近授权时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_openid_template (openid, template_id, tenant_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '小程序订阅授权';

CREATE TABLE mini_event_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_name VARCHAR(100) NOT NULL COMMENT '事件名称',
    event_key VARCHAR(100) NOT NULL COMMENT '事件标识',
    properties JSON COMMENT '事件属性定义',
    is_auto TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否自动采集',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_key_tenant (event_key, tenant_id)
) COMMENT '小程序事件配置';
```

---

## P1 — 应该有，形成私域闭环

---

### 3. 裂变营销引擎 [中复杂度 | 4.0人月]

**现状**：完全不存在

**竞品对标**：
- 星耀裂变：专注裂变营销（邀请有礼/拼团/砍价/助力）
- 有赞：裂变+拼团+砍价
- 致趣百川：裂变海报+邀请有礼

#### 3.1 邀请有礼

**描述**：老用户邀请新用户，双方获得奖励。

**核心流程**：
```
1. 运营创建邀请活动（邀请人奖励/被邀请人奖励/邀请上限）
2. 系统为每个邀请人生成专属邀请码/海报
3. 被邀请人通过邀请链接注册/购买
4. 系统追踪邀请关系 → 发放奖励
5. 画布联动：邀请成功事件 → 触发感谢画布
```

**邀请关系链**：
```
inviter_id → invitee_id → invite_time → channel → reward_status
支持多级：A邀请B，B邀请C → A获得二级奖励（可配置是否支持）
```

#### 3.2 拼团

**描述**：多人成团享受优惠价格。

**核心流程**：
```
1. 运营创建拼团活动（商品/价格/成团人数/有效期）
2. 用户开团/参团 → 分享邀请好友参团
3. 成团 → 发放优惠/发货
4. 超时未成团 → 自动退款
```

#### 3.3 分享解锁

**描述**：用户分享给N个好友后解锁内容/优惠。

**核心流程**：
```
1. 运营创建分享解锁活动（解锁条件/解锁内容/有效期）
2. 用户分享 → 系统追踪分享点击
3. 达到N次点击 → 解锁内容
4. 画布联动：解锁事件 → 触发后续画布
```

#### 3.4 裂变海报生成

**描述**：为每个用户生成带专属二维码的裂变海报。

**技术方案**：
- 后端：Java2D/Thumbnails生成图片，嵌入用户专属二维码
- 二维码内容：`https://mini.canvas.com/invite?code=USER_CODE`
- 海报模板：背景图+头像+昵称+二维码+文案

**反作弊**：
| 策略 | 描述 |
|------|------|
| IP频次限制 | 同一IP每天最多点击5次邀请链接 |
| 设备指纹 | 同一设备只计1次有效点击 |
| 时间窗口 | 邀请链接点击后需在30分钟内完成注册 |
| 异常检测 | 单用户日邀请超过20人自动审核 |

**数据库DDL**：
```sql
CREATE TABLE fission_campaign (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '活动名称',
    type VARCHAR(20) NOT NULL COMMENT 'INVITE/GROUP_BUY/SHARE_UNLOCK',
    config JSON NOT NULL COMMENT '活动配置（不同类型结构不同）',
    poster_template_id BIGINT COMMENT '海报模板ID',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RUNNING/PAUSED/ENDED',
    anti_fraud_config JSON COMMENT '反作弊配置',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type_status (type, status),
    INDEX idx_time (start_time, end_time),
    INDEX idx_tenant (tenant_id)
) COMMENT '裂变活动';

CREATE TABLE fission_invite_chain (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    campaign_id BIGINT NOT NULL,
    inviter_id VARCHAR(64) NOT NULL COMMENT '邀请人',
    invitee_id VARCHAR(64) COMMENT '被邀请人',
    invite_code VARCHAR(32) NOT NULL COMMENT '邀请码',
    channel VARCHAR(20) COMMENT '邀请渠道',
    level INT NOT NULL DEFAULT 1 COMMENT '邀请层级 1=直接 2=间接',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/REGISTERED/CONVERTED/REWARDED/EXPIRED',
    reward_detail JSON COMMENT '奖励详情',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_campaign (campaign_id),
    INDEX idx_inviter (inviter_id),
    INDEX idx_invitee (invitee_id),
    INDEX idx_code (invite_code),
    INDEX idx_tenant (tenant_id)
) COMMENT '裂变邀请链';

CREATE TABLE fission_group_buy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    campaign_id BIGINT NOT NULL,
    group_id VARCHAR(32) NOT NULL COMMENT '团ID',
    leader_id VARCHAR(64) NOT NULL COMMENT '团长',
    target_count INT NOT NULL COMMENT '成团人数',
    current_count INT NOT NULL DEFAULT 1 COMMENT '当前人数',
    status VARCHAR(20) NOT NULL DEFAULT 'FORMING' COMMENT 'FORMING/SUCCESS/FAILED/EXPIRED',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_campaign (campaign_id),
    INDEX idx_group (group_id),
    INDEX idx_leader (leader_id),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '拼团';

CREATE TABLE fission_poster_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    background_url VARCHAR(500) NOT NULL COMMENT '背景图URL',
    layout JSON NOT NULL COMMENT '布局配置（头像/昵称/二维码/文案位置和样式）',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '裂变海报模板';
```

**与画布引擎联动**：
- 新增NodeHandler：`FISSION_INVITE` — 裂变邀请节点（生成邀请码+海报）
- 新增NodeHandler：`FISSION_CHECK` — 裂变条件检查节点（检查邀请人数/拼团状态）
- 事件触发：`FISSION_INVITE_SUCCESS` / `GROUP_BUY_SUCCESS` / `SHARE_UNLOCK_COMPLETE`

---

### 4. 生命周期阶段识别+自动化编排 [中复杂度 | 3.0人月]

**现状**：CustomerProfileDO.lifecycleStage字段存在（NEW/ACTIVE/CHURN_RISK），UpdateProfileHandler可更新，但无自动流转引擎。

#### 4.1 生命周期模型定义

**标准5阶段模型**：
| 阶段 | 定义 | 典型行为 |
|------|------|---------|
| NEW | 新用户（首次注册/添加7天内） | 注册、首次浏览 |
| ACTIVE | 活跃用户（近期有互动） | 定期购买、浏览 |
| LOYAL | 忠实用户（高频+高价值） | 复购、推荐、高客单 |
| AT_RISK | 流失风险（活跃度下降） | 互动频次降低 |
| CHURNED | 已流失（长期无互动） | 超过30天无任何行为 |

**阶段转换规则**：
```json
{
  "transitions": [
    {
      "from": "NEW",
      "to": "ACTIVE",
      "condition": "days_since_register > 7 AND purchase_count >= 1",
      "auto": true
    },
    {
      "from": "ACTIVE",
      "to": "LOYAL",
      "condition": "purchase_count_30d >= 3 AND avg_order_value >= 200",
      "auto": true
    },
    {
      "from": "ACTIVE",
      "to": "AT_RISK",
      "condition": "days_since_last_purchase > 14 AND days_since_last_interaction > 7",
      "auto": true
    },
    {
      "from": "AT_RISK",
      "to": "CHURNED",
      "condition": "days_since_last_interaction > 30",
      "auto": true
    },
    {
      "from": "CHURNED",
      "to": "ACTIVE",
      "condition": "purchase_count_7d >= 1",
      "auto": true
    }
  ]
}
```

#### 4.2 自动阶段流转引擎

**技术方案**：
```
定时任务（每日凌晨）：
1. 查询所有用户画像
2. 对每个用户评估转换规则
3. 满足条件 → 更新lifecycleStage
4. 阶段变更 → 触发EVENT_TRIGGER → 启动对应画布
```

**与画布引擎联动**：
- 阶段变更事件：`LIFECYCLE_STAGE_CHANGED`
- EVENT_TRIGGER监听此事件 → 启动对应旅程画布
- 预置画布模板：
  - "新客培育旅程"（NEW→ACTIVE）
  - "流失挽回旅程"（AT_RISK→ACTIVE）
  - "忠诚客户关怀"（LOYAL阶段定期关怀）

#### 4.3 生命周期分析看板

**指标**：
| 指标 | 计算 |
|------|------|
| 各阶段人数 | 按lifecycleStage分组统计 |
| 阶段转化率 | N日内从A阶段转到B阶段的用户数 / A阶段总人数 |
| 阶段停留时长 | 用户在各阶段的平均停留天数 |
| 流失率 | AT_RISK→CHURNED转化率 |
| 挽回率 | CHURNED→ACTIVE转化率 |

**数据库DDL**：
```sql
CREATE TABLE lifecycle_model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '模型名称',
    stages JSON NOT NULL COMMENT '阶段定义 [{"name":"NEW","color":"#52c41a"}]',
    transitions JSON NOT NULL COMMENT '转换规则',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
) COMMENT '生命周期模型';

CREATE TABLE lifecycle_stage_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    from_stage VARCHAR(30) COMMENT '原阶段',
    to_stage VARCHAR(30) NOT NULL COMMENT '新阶段',
    trigger_rule VARCHAR(200) COMMENT '触发规则',
    trigger_type VARCHAR(20) NOT NULL COMMENT 'AUTO/MANUAL/CANVAS',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_from_to (from_stage, to_stage),
    INDEX idx_created (created_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '生命周期阶段变更日志';
```

---

### 5. 优惠券全生命周期管理 [中复杂度 | 3.0人月]

**现状**：CouponHandler仅是发券客户端（调用外部券系统/WireMock），无券管理能力。

#### 5.1 券模板管理

**券类型**：
| 类型 | 描述 | 核心字段 |
|------|------|---------|
| CASH | 代金券 | 面额、最低消费 |
| DISCOUNT | 折扣券 | 折扣率、最高优惠 |
| GIFT | 赠品券 | 赠品名称、数量 |
| SHIPPING | 免邮券 | — |
| FIXED | 减价券 | 固定减价金额 |

**券生命周期**：
```
创建 → 审核 → 上架 → 发放 → 领取 → 使用(核销) → 过期/作废
```

#### 5.2 券库存管理

**库存控制**：
- 总库存限制
- 每人限领数量
- 日发放上限
- Redis原子扣减（DECR）防超发

#### 5.3 券核销回调

**核销流程**：
```
1. 业务系统调用核销API → POST /api/coupon/redeem/{code}
2. 校验券状态（未使用+未过期+属于该用户）
3. 更新券状态为USED
4. 记录核销时间+核销订单号
5. 触发COUPON_REDEEMED事件 → 可触发画布
```

**数据库DDL**：
```sql
CREATE TABLE coupon_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '券名称',
    type VARCHAR(20) NOT NULL COMMENT 'CASH/DISCOUNT/GIFT/SHIPPING/FIXED',
    value DECIMAL(12,2) NOT NULL COMMENT '面额/折扣率',
    min_spend DECIMAL(12,2) COMMENT '最低消费',
    max_discount DECIMAL(12,2) COMMENT '最高优惠（折扣券）',
    total_stock INT NOT NULL COMMENT '总库存',
    remaining_stock INT NOT NULL COMMENT '剩余库存',
    per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每人限领',
    daily_limit INT COMMENT '日发放上限',
    valid_type VARCHAR(20) NOT NULL COMMENT 'FIXED_DATE/RELATIVE_DAYS',
    valid_start DATE COMMENT '固定有效期开始',
    valid_end DATE COMMENT '固定有效期结束',
    valid_days INT COMMENT '相对有效天数',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/APPROVED/ACTIVE/EXHAUSTED/DISABLED',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type_status (type, status),
    INDEX idx_tenant (tenant_id)
) COMMENT '券模板';

CREATE TABLE coupon_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL COMMENT '券码',
    user_id VARCHAR(64) COMMENT '持有人',
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED/CLAIMED/USED/EXPIRED/CANCELLED',
    issued_at DATETIME COMMENT '发放时间',
    claimed_at DATETIME COMMENT '领取时间',
    used_at DATETIME COMMENT '使用时间',
    expired_at DATETIME COMMENT '过期时间',
    order_id VARCHAR(64) COMMENT '核销订单号',
    source VARCHAR(30) COMMENT '来源 CANVAS/MANUAL/ACTIVITY',
    canvas_id BIGINT COMMENT '关联画布ID',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_code (code, tenant_id),
    INDEX idx_template (template_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_expired (expired_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '券实例';
```

---

### 6. 积分/会员等级体系 [中复杂度 | 3.5人月]

**现状**：PointsOperationHandler支持GRANT/DEDUCT，CustomerPointsLedgerDO有流水，但无余额表、无会员等级、无兑换商城。

#### 6.1 积分余额表

**改动**：新增 `customer_points_account` 表，实时维护积分余额

```sql
CREATE TABLE customer_points_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    points_type VARCHAR(30) NOT NULL DEFAULT 'DEFAULT' COMMENT '积分类型 DEFAULT/GROWTH/EXCHANGE',
    balance INT NOT NULL DEFAULT 0 COMMENT '当前余额',
    total_earned INT NOT NULL DEFAULT 0 COMMENT '累计获得',
    total_spent INT NOT NULL DEFAULT 0 COMMENT '累计消费',
    frozen INT NOT NULL DEFAULT 0 COMMENT '冻结积分',
    expiring_soon INT NOT NULL DEFAULT 0 COMMENT '即将过期（30天内）',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_type (user_id, points_type, tenant_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '积分账户';
```

#### 6.2 会员等级体系

**等级定义**：
| 等级 | 条件 | 权益 |
|------|------|------|
| 普通会员 | 注册即得 | 基础权益 |
| 银卡会员 | 累计消费≥1000 或 累计积分≥5000 | 9.5折+生日券 |
| 金卡会员 | 累计消费≥5000 或 累计积分≥20000 | 9折+专属客服+优先发货 |
| 钻石会员 | 累计消费≥20000 或 累计积分≥100000 | 8.5折+专属顾问+新品试用 |

**升级/降级规则**：
- 升级：满足条件立即升级
- 降级：年度考核（每年1月1日重算），不满足则降级
- 保级：考核期内消费达到保级线则保级

**数据库DDL**：
```sql
CREATE TABLE member_level (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '等级名称',
    level_code VARCHAR(30) NOT NULL COMMENT '等级编码 NORMAL/SILVER/GOLD/DIAMOND',
    level_order INT NOT NULL COMMENT '等级排序（越大越高）',
    upgrade_condition JSON COMMENT '升级条件 {"min_spent":1000,"min_points":5000}',
    downgrade_condition JSON COMMENT '降级条件',
    retain_condition JSON COMMENT '保级条件',
    benefits JSON COMMENT '权益列表',
    validity_days INT COMMENT '等级有效期（天）',
    icon_url VARCHAR(500) COMMENT '等级图标',
    color VARCHAR(7) COMMENT '等级颜色 #RRGGBB',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (level_order),
    INDEX idx_tenant (tenant_id)
) COMMENT '会员等级定义';

CREATE TABLE member_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    level_code VARCHAR(30) NOT NULL DEFAULT 'NORMAL' COMMENT '当前等级',
    level_achieved_at DATETIME COMMENT '等级达成时间',
    level_expire_at DATETIME COMMENT '等级过期时间',
    total_spent DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '累计消费金额',
    total_orders INT NOT NULL DEFAULT 0 COMMENT '累计订单数',
    total_points INT NOT NULL DEFAULT 0 COMMENT '累计积分',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user (user_id, tenant_id),
    INDEX idx_level (level_code),
    INDEX idx_tenant (tenant_id)
) COMMENT '会员账户';
```

---

### 7. RFM/RFE用户价值分层 [低复杂度 | 1.5人月]

**现状**：ScoringHandler是通用评分节点，无RFM预置逻辑。

#### 7.1 RFM模型

**指标定义**：
| 指标 | 含义 | 计算方式 |
|------|------|---------|
| R (Recency) | 最近一次消费距今天数 | days_since_last_purchase |
| F (Frequency) | 消费频次 | purchase_count_90d |
| M (Monetary) | 消费金额 | total_spent_90d |

**分层规则**：
```
每个指标分5档（1-5分，5为最优）：
R: ≤7天=5, 8-14=4, 15-30=3, 31-60=2, >60=1
F: ≥10次=5, 7-9=4, 4-6=3, 2-3=2, 1=1
M: ≥5000=5, 3000-4999=4, 1000-2999=3, 500-999=2, <500=1

RFM总分 = R×权重 + F×权重 + M×权重（默认1:1:1）
用户分层：
  重要价值客户: R≥4, F≥4, M≥4
  重要发展客户: R≥4, F<4, M≥4
  重要保持客户: R<4, F≥4, M≥4
  重要挽留客户: R<4, F<4, M≥4
  一般价值客户: R≥4, F≥4, M<4
  一般发展客户: R≥4, F<4, M<4
  一般保持客户: R<4, F≥4, M<4
  一般挽留客户: R<4, F<4, M<4
```

#### 7.2 RFE模型（适用于非电商场景）

| 指标 | 含义 | 计算方式 |
|------|------|---------|
| R (Recency) | 最近一次互动距今天数 | days_since_last_interaction |
| F (Frequency) | 互动频次 | interaction_count_90d |
| E (Engagement) | 互动深度 | avg_interaction_depth（页面数/时长/分享数） |

**数据库DDL**：
```sql
CREATE TABLE rfm_model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '模型名称',
    type VARCHAR(10) NOT NULL COMMENT 'RFM/RFE',
    r_config JSON NOT NULL COMMENT 'R指标分档配置',
    f_config JSON NOT NULL COMMENT 'F指标分档配置',
    m_or_e_config JSON NOT NULL COMMENT 'M/E指标分档配置',
    weights JSON NOT NULL DEFAULT '{"r":1,"f":1,"m":1}' COMMENT '权重',
    segment_rules JSON NOT NULL COMMENT '分层规则',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
) COMMENT 'RFM/RFE模型';

CREATE TABLE rfm_user_score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    model_id BIGINT NOT NULL,
    r_score TINYINT NOT NULL COMMENT 'R得分 1-5',
    f_score TINYINT NOT NULL COMMENT 'F得分 1-5',
    m_or_e_score TINYINT NOT NULL COMMENT 'M/E得分 1-5',
    total_score DECIMAL(5,2) NOT NULL COMMENT '加权总分',
    segment VARCHAR(50) NOT NULL COMMENT '分层结果',
    calculated_at DATETIME NOT NULL COMMENT '计算时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_user_model (user_id, model_id, tenant_id),
    INDEX idx_segment (segment),
    INDEX idx_score (total_score),
    INDEX idx_tenant (tenant_id)
) COMMENT '用户RFM/RFE评分';
```

---

### 8. 运营SOP模板 [低复杂度 | 1.0人月]

**现状**：不存在

**描述**：预置常见私域运营场景的画布模板，降低使用门槛。

**预置模板清单**：

| 模板名 | 场景 | 包含节点 |
|--------|------|---------|
| 新客欢迎旅程 | 新添加企微好友7天培育 | EVENT_TRIGGER→欢迎语→打标签→发券→邀请进群 |
| 流失挽回旅程 | 14天未互动用户挽回 | AUDIENCE_TRIGGER→IF_CONDITION→发券→企微消息→等待→检查 |
| 会员升级关怀 | 会员升级时自动关怀 | EVENT_TRIGGER→恭喜消息→专属权益→积分赠送 |
| 购物车挽回 | 加购未付款用户 | EVENT_TRIGGER→等待2h→PUSH→等待24h→SMS→等待48h→优惠券 |
| 生日关怀 | 会员生日当天 | SCHEDULED_TRIGGER→生日检查→生日券→企微祝福→积分赠送 |
| 节日营销 | 节日促销活动 | SCHEDULED_TRIGGER→人群筛选→AB_SPLIT→渠道触达→回执追踪 |
| 裂变活动 | 邀请有礼 | EVENT_TRIGGER→生成海报→分享追踪→奖励发放→感谢消息 |
| 群SOP提醒 | 群运营日常提醒 | SCHEDULED_TRIGGER→群列表→SOP提醒→执行确认 |

---

## P2 — 锦上添花

---

### 9. 企微话术库+素材库 [中复杂度 | 2.0人月]

**描述**：统一管理企微沟通话术和营销素材。

**话术库**：
- 分类管理（问候/产品/活动/售后/通用）
- 支持变量插值（{{user.name}}）
- 使用频次统计
- 话术搜索

**素材库**：
- 图片/视频/文档/PDF
- 分类管理
- 企微素材上传同步
- 使用统计

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 企微SCRM模块 | 5.0 | 3.0 | 1.0 | 9.0 |
| P0 | 小程序触达+事件追踪 | 2.0 | 1.0 | 0.5 | 3.5 |
| P1 | 裂变营销引擎 | 2.5 | 1.5 | 0.5 | 4.5 |
| P1 | 生命周期阶段+编排 | 2.0 | 1.0 | 0.3 | 3.3 |
| P1 | 优惠券全生命周期 | 2.0 | 1.0 | 0.3 | 3.3 |
| P1 | 积分/会员等级 | 2.0 | 1.5 | 0.3 | 3.8 |
| P1 | RFM/RFE分层 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 运营SOP模板 | 0.3 | 0.7 | 0.2 | 1.2 |
| P2 | 话术库+素材库 | 1.0 | 1.0 | 0.2 | 2.2 |
| | **合计** | **17.8** | **11.2** | **3.5** | **32.5** |

---

## 执行顺序建议

```
Phase 0 (0-3月): 生产止血 + 合规底座
  ├── 方向①P0: 疲劳度策略 + 合规门接入
  ├── 企微SDK集成（1.1）
  └── 产出: 可安全上线的合规营销引擎 + 企微API连通

Phase 1 (3-9月): 私域筑基
  ├── 企微客户管理（1.2）+ 欢迎语（1.5）
  ├── 小程序触达+事件追踪（2）
  ├── 优惠券全生命周期（5）
  ├── 积分/会员等级（6）
  └── 产出: 私域运营中台MVP

Phase 2 (9-18月): 运营闭环
  ├── 企微群聊运营（1.3）+ 朋友圈（1.4）
  ├── 裂变营销引擎（3）
  ├── 生命周期阶段+编排（4）
  ├── RFM/RFE分层（7）
  ├── 运营SOP模板（8）
  └── 产出: 完整私域运营中台

Phase 3 (18-36月): AI加持
  ├── 流失预测模型
  ├── 智能触达时机
  ├── AI内容个性化
  └── 产出: AI原生私域运营平台
```

---

## 与方向①的依赖关系

| 方向②功能 | 依赖方向①功能 | 依赖原因 |
|-----------|-------------|---------|
| 企微SCRM | 合规门(P0) | 企微触达必须合规 |
| 小程序触达 | 渠道回执(P1) | 小程序消息回执追踪 |
| 裂变引擎 | Webhook(P1) | 裂变事件外部推送 |
| 优惠券 | 内容管理(P1) | 优惠券模板渲染 |
| 生命周期 | 归因引擎(P1) | 生命周期ROI量化 |
| RFM分层 | 渠道回执(P1) | RFM指标依赖行为数据 |

方向①的P0功能是方向②的**必要前置**，P1功能是**增强依赖**。
