# 企微SCRM模块详细设计 (2026-05-31)

> **定位**: 私域运营中台核心模块，实现DAG画布+企微原生SCRM的差异化组合

---

## 一、模块概述

### 1.1 业务定位

| 维度 | 说明 |
|------|------|
| **目标用户** | 中国消费品牌/零售/连锁企业的私域运营团队 |
| **核心价值** | DAG级画布编排 + 企微原生SCRM（国内无人提供） |
| **差异化** | Convertlab有SCRM但画布弱，神策画布更弱，我们57节点 vs 竞品20-30节点 |

### 1.2 功能范围

```
企微SCRM模块
├── 客户管理
│   ├── 客户列表/详情/标签
│   ├── 客户分配/转移
│   └── 客户流失预警
├── 群聊运营
│   ├── 群列表/详情/成员管理
│   ├── 群欢迎语配置
│   ├── 群自动回复
│   └── 群裂变活动
├── 朋友圈
│   ├── 朋友圈内容发布
│   ├── 定时发布
│   └── 互动统计
├── 消息触达
│   ├── 单聊消息
│   ├── 群消息
│   └── 模板消息
└── 画布集成
    ├── 企微触达Handler
    ├── 企微事件触发器
    └── 客户旅程编排
```

---

## 二、数据模型设计

### 2.1 核心实体

```sql
-- 企微客户表
CREATE TABLE wecom_customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    external_user_id VARCHAR(64) NOT NULL COMMENT '企微外部联系人ID',
    internal_user_id VARCHAR(64) NOT NULL COMMENT '企微内部联系人ID(员工)',
    name VARCHAR(128) COMMENT '客户名称',
    avatar VARCHAR(512) COMMENT '头像URL',
    type TINYINT DEFAULT 1 COMMENT '1-微信用户 2-企微用户',
    gender TINYINT DEFAULT 0 COMMENT '0-未知 1-男 2-女',
    union_id VARCHAR(64) COMMENT '微信UnionID',
    position VARCHAR(128) COMMENT '职位',
    corp_name VARCHAR(128) COMMENT '企业名称',
    corp_full_name VARCHAR(256) COMMENT '企业全称',
    status TINYINT DEFAULT 1 COMMENT '1-正常 2-删除 3-拉黑',
    user_id BIGINT COMMENT '关联CDP用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME COMMENT '软删除时间',
    UNIQUE KEY uk_tenant_external (tenant_id, external_user_id),
    KEY idx_internal_user (internal_user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微客户表';

-- 企微客户标签关系表
CREATE TABLE wecom_customer_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    tag_id VARCHAR(64) NOT NULL COMMENT '企微标签ID',
    tag_name VARCHAR(128) COMMENT '标签名称',
    tag_group VARCHAR(64) COMMENT '标签分组',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_tag (customer_id, tag_id),
    KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微客户标签关系表';

-- 企微群聊表
CREATE TABLE wecom_group_chat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    chat_id VARCHAR(64) NOT NULL COMMENT '企微群ID',
    name VARCHAR(128) COMMENT '群名称',
    owner_id VARCHAR(64) NOT NULL COMMENT '群主ID',
    owner_name VARCHAR(128) COMMENT '群主名称',
    member_count INT DEFAULT 0 COMMENT '群成员数',
    max_member_count INT DEFAULT 500 COMMENT '最大成员数',
    notice TEXT COMMENT '群公告',
    create_time BIGINT COMMENT '群创建时间(毫秒)',
    status TINYINT DEFAULT 1 COMMENT '1-正常 2-解散',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_chat (tenant_id, chat_id),
    KEY idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微群聊表';

-- 企微群成员表
CREATE TABLE wecom_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    chat_id VARCHAR(64) NOT NULL COMMENT '企微群ID',
    user_id VARCHAR(64) NOT NULL COMMENT '成员ID',
    name VARCHAR(128) COMMENT '成员名称',
    type TINYINT DEFAULT 1 COMMENT '1-企业成员 2-外部联系人',
    join_time BIGINT COMMENT '入群时间',
    join_scene TINYINT COMMENT '入群方式 1-邀请 2-扫码',
    inviter_id VARCHAR(64) COMMENT '邀请人ID',
    status TINYINT DEFAULT 1 COMMENT '1-在群 2-退群',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_user (chat_id, user_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微群成员表';

-- 企微欢迎语配置表
CREATE TABLE wecom_welcome_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL COMMENT '配置名称',
    type TINYINT DEFAULT 1 COMMENT '1-员工欢迎语 2-群欢迎语',
    content TEXT COMMENT '欢迎语内容',
    attachments JSON COMMENT '附件配置(图片/链接/小程序)',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认',
    status TINYINT DEFAULT 1 COMMENT '1-启用 2-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_type (tenant_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微欢迎语配置表';

-- 企微消息发送记录表
CREATE TABLE wecom_message_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    execution_id VARCHAR(64) COMMENT '画布执行ID',
    canvas_id BIGINT COMMENT '画布ID',
    node_id VARCHAR(64) COMMENT '节点ID',
    msg_type VARCHAR(32) NOT NULL COMMENT '消息类型 text/image/link/miniprogram',
    chat_type VARCHAR(32) NOT NULL COMMENT 'single/group',
    external_user_id VARCHAR(64) COMMENT '外部用户ID',
    chat_id VARCHAR(64) COMMENT '群ID',
    internal_user_id VARCHAR(64) COMMENT '发送员工ID',
    content TEXT COMMENT '消息内容',
    attachments JSON COMMENT '附件',
    msg_id VARCHAR(64) COMMENT '企微消息ID',
    send_time DATETIME COMMENT '发送时间',
    status TINYINT DEFAULT 0 COMMENT '0-待发送 1-已发送 2-发送失败',
    error_code VARCHAR(32) COMMENT '错误码',
    error_msg VARCHAR(512) COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_execution (execution_id),
    KEY idx_external_user (external_user_id),
    KEY idx_chat (chat_id),
    KEY idx_status_time (status, send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微消息发送记录表';
```

### 2.2 实体关系图

```
┌─────────────────┐       ┌─────────────────┐
│  wecom_customer │       │   wecom_group   │
│                 │       │     _chat       │
├─────────────────┤       ├─────────────────┤
│ id              │       │ id              │
│ external_user_id│       │ chat_id         │
│ internal_user_id│       │ owner_id        │
│ name            │       │ member_count    │
│ user_id ────────┼──┐    │ status          │
│ status          │  │    └─────────────────┘
└─────────────────┘  │              │
        │            │              │
        ▼            │              ▼
┌─────────────────┐  │    ┌─────────────────┐
│   cdp_user      │  │    │ wecom_group     │
│   (已有)        │  │    │    _member      │
└─────────────────┘  │    ├─────────────────┤
                     │    │ chat_id         │
                     │    │ user_id         │
                     │    │ type            │
                     │    │ join_time       │
                     │    └─────────────────┘
                     │
                     ▼
              ┌─────────────────┐
              │ wecom_customer  │
              │     _tag        │
              ├─────────────────┤
              │ customer_id     │
              │ tag_id          │
              │ tag_name        │
              └─────────────────┘
```

---

## 三、API设计

### 3.1 客户管理API

```yaml
# 客户列表
GET /api/v1/wecom/customers
  params:
    - page: int
    - size: int
    - internalUserId: string (员工ID筛选)
    - tagIds: string[] (标签筛选)
    - status: int (状态筛选)
  response:
    data: WecomCustomerVO[]
    total: int

# 客户详情
GET /api/v1/wecom/customers/{id}
  response:
    WecomCustomerDetailVO

# 同步客户数据
POST /api/v1/wecom/customers/sync
  body:
    internalUserId: string (指定员工，不传则全量)
  response:
    syncCount: int
    taskId: string

# 客户打标签
POST /api/v1/wecom/customers/{id}/tags
  body:
    tagIds: string[]
  response:
    success: boolean

# 客户转移
POST /api/v1/wecom/customers/{id}/transfer
  body:
    toInternalUserId: string
  response:
    success: boolean
```

### 3.2 群聊管理API

```yaml
# 群列表
GET /api/v1/wecom/groups
  params:
    - page: int
    - size: int
    - ownerId: string (群主筛选)
    - status: int
  response:
    data: WecomGroupVO[]
    total: int

# 群详情
GET /api/v1/wecom/groups/{chatId}
  response:
    WecomGroupDetailVO

# 群成员列表
GET /api/v1/wecom/groups/{chatId}/members
  response:
    WecomGroupMemberVO[]

# 同步群数据
POST /api/v1/wecom/groups/sync
  response:
    syncCount: int
    taskId: string
```

### 3.3 欢迎语API

```yaml
# 欢迎语配置列表
GET /api/v1/wecom/welcome-configs
  params:
    - type: int (1-员工 2-群)
  response:
    WecomWelcomeConfigVO[]

# 创建欢迎语配置
POST /api/v1/wecom/welcome-configs
  body:
    name: string
    type: int
    content: string
    attachments: Attachment[]
    isDefault: boolean
  response:
    id: long

# 更新欢迎语配置
PUT /api/v1/wecom/welcome-configs/{id}
  body:
    name: string
    content: string
    attachments: Attachment[]
  response:
    success: boolean
```

### 3.4 消息发送API

```yaml
# 发送单聊消息
POST /api/v1/wecom/messages/send
  body:
    externalUserId: string
    internalUserId: string
    msgType: string (text/image/link/miniprogram)
    content: object
    attachments: Attachment[]
  response:
    recordId: long
    msgId: string

# 发送群消息
POST /api/v1/wecom/groups/{chatId}/send
  body:
    internalUserId: string
    msgType: string
    content: object
  response:
    recordId: long
    msgId: string

# 消息发送记录
GET /api/v1/wecom/messages/records
  params:
    - executionId: string
    - externalUserId: string
    - status: int
    - startTime: datetime
    - endTime: datetime
  response:
    WecomMessageRecordVO[]
```

---

## 四、Handler设计

### 4.1 新增节点类型

```java
// NodeType.java 新增
public static final String SEND_WECOM = "SEND_WECOM";           // 企微单聊消息
public static final String SEND_WECOM_GROUP = "SEND_WECOM_GROUP"; // 企微群消息
public static final String WECOM_WELCOME = "WECOM_WELCOME";     // 企微欢迎语
public static final String WECOM_TAG = "WECOM_TAG";             // 企微打标签
public static final String WECOM_EVENT_TRIGGER = "WECOM_EVENT_TRIGGER"; // 企微事件触发
```

### 4.2 SendWecomHandler

```java
package org.chovy.canvas.engine.handlers;

@Component
@NodeHandlerType(NodeType.SEND_WECOM)
public class SendWecomHandler extends AbstractSendMessageHandler {

    private final WecomMessageService wecomMessageService;

    public SendWecomHandler(ReachDeliveryService deliveryService,
                           WecomMessageService wecomMessageService) {
        super(deliveryService);
        this.wecomMessageService = wecomMessageService;
    }

    @Override
    protected String channel() {
        return "WECOM";
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String externalUserId = resolveExternalUserId(config, ctx);
        String internalUserId = string(config, "internalUserId", null);
        String msgType = string(config, "msgType", "text");
        String successNodeId = string(config, "successNodeId", null);
        String failNodeId = string(config, "failNodeId", null);

        WecomMessageRequest request = WecomMessageRequest.builder()
            .executionId(ctx.getExecutionId())
            .canvasId(ctx.getCanvasId())
            .nodeId(string(config, "__nodeId", "wecom-send"))
            .externalUserId(externalUserId)
            .internalUserId(internalUserId)
            .msgType(msgType)
            .content(buildContent(config, ctx))
            .attachments(buildAttachments(config))
            .build();

        return wecomMessageService.sendSingleMessage(request)
            .map(result -> {
                Map<String, Object> output = new LinkedHashMap<>();
                output.put("msgId", result.msgId());
                output.put("recordId", result.recordId());
                output.put("sendStatus", result.success() ? "SENT" : "FAILED");

                if (result.success()) {
                    return NodeResult.routed("success", successNodeId, output);
                } else if (failNodeId != null) {
                    return NodeResult.routed("fail", failNodeId, output);
                } else {
                    return NodeResult.fail("企微消息发送失败: " + result.errorMsg());
                }
            });
    }

    private String resolveExternalUserId(Map<String, Object> config, ExecutionContext ctx) {
        Object raw = config.get("externalUserId");
        if (raw instanceof String s && s.startsWith("$")) {
            String field = s.startsWith("$.") ? s.substring(2) : s.substring(1);
            return (String) ctx.getContextValue(field);
        }
        return (String) raw;
    }

    @Override
    public boolean isReachNode() {
        return true;
    }
}
```

### 4.3 SendWecomGroupHandler

```java
@Component
@NodeHandlerType(NodeType.SEND_WECOM_GROUP)
public class SendWecomGroupHandler implements NodeHandler {

    private final WecomMessageService wecomMessageService;

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String chatId = string(config, "chatId", null);
        String internalUserId = string(config, "internalUserId", null);
        String msgType = string(config, "msgType", "text");
        String successNodeId = string(config, "successNodeId", null);

        WecomGroupMessageRequest request = WecomGroupMessageRequest.builder()
            .chatId(chatId)
            .internalUserId(internalUserId)
            .msgType(msgType)
            .content(buildContent(config, ctx))
            .build();

        return wecomMessageService.sendGroupMessage(request)
            .map(result -> {
                Map<String, Object> output = new LinkedHashMap<>();
                output.put("msgId", result.msgId());
                return NodeResult.routed("success", successNodeId, output);
            });
    }

    @Override
    public boolean isReachNode() {
        return true;
    }
}
```

### 4.4 WecomWelcomeHandler

```java
@Component
@NodeHandlerType(NodeType.WECOM_WELCOME)
public class WecomWelcomeHandler implements NodeHandler {

    private final WecomWelcomeService welcomeService;

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String welcomeConfigId = string(config, "welcomeConfigId", null);
        String externalUserId = resolveExternalUserId(config, ctx);
        String internalUserId = string(config, "internalUserId", null);
        String successNodeId = string(config, "successNodeId", null);

        return welcomeService.sendWelcome(welcomeConfigId, externalUserId, internalUserId)
            .map(result -> {
                Map<String, Object> output = new LinkedHashMap<>();
                output.put("welcomeSent", result.success());
                return NodeResult.routed("success", successNodeId, output);
            });
    }
}
```

### 4.5 WecomTagHandler

```java
@Component
@NodeHandlerType(NodeType.WECOM_TAG)
public class WecomTagHandler implements NodeHandler {

    private final WecomCustomerService customerService;

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String externalUserId = resolveExternalUserId(config, ctx);
        List<String> addTagIds = list(config, "addTagIds", List.of());
        List<String> removeTagIds = list(config, "removeTagIds", List.of());
        String successNodeId = string(config, "successNodeId", null);

        return customerService.updateCustomerTags(externalUserId, addTagIds, removeTagIds)
            .map(result -> {
                Map<String, Object> output = new LinkedHashMap<>();
                output.put("tagUpdated", result.success());
                output.put("currentTags", result.currentTags());
                return NodeResult.routed("success", successNodeId, output);
            });
    }
}
```

### 4.6 WecomEventTriggerHandler

```java
@Component
@NodeHandlerType(NodeType.WECOM_EVENT_TRIGGER)
public class WecomEventTriggerHandler implements NodeHandler {

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // 触发器节点，由企微事件回调激活
        String eventCode = string(config, "eventCode", null);
        String successNodeId = string(config, "successNodeId", null);

        // 事件数据已在ExecutionContext中
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("eventCode", eventCode);
        output.put("eventType", ctx.getContextValue("eventType"));
        output.put("externalUserId", ctx.getContextValue("externalUserId"));

        return Mono.just(NodeResult.routed("success", successNodeId, output));
    }

    @Override
    public boolean isTriggerNode() {
        return true;
    }
}
```

---

## 五、企微API集成

### 5.1 WecomApiClient

```java
@Service
public class WecomApiClient {

    private final WebClient webClient;
    private final WecomTokenService tokenService;

    @Value("${wecom.corp-id}")
    private String corpId;

    @Value("${wecom.agent-id}")
    private String agentId;

    /**
     * 获取客户列表
     */
    public Mono<WecomCustomerListResponse> getExternalContactList(String internalUserId) {
        return webClient.get()
            .uri("/cgi-bin/externalcontact/list?userid={userid}", internalUserId)
            .header("Authorization", "Bearer " + tokenService.getAccessToken())
            .retrieve()
            .bodyToMono(WecomCustomerListResponse.class);
    }

    /**
     * 获取客户详情
     */
    public Mono<WecomCustomerDetailResponse> getExternalContact(String externalUserId) {
        return webClient.get()
            .uri("/cgi-bin/externalcontact/get?external_userid={external_userid}", externalUserId)
            .header("Authorization", "Bearer " + tokenService.getAccessToken())
            .retrieve()
            .bodyToMono(WecomCustomerDetailResponse.class);
    }

    /**
     * 发送应用消息
     */
    public Mono<WecomSendResponse> sendMessage(WecomMessageRequest request) {
        return webClient.post()
            .uri("/cgi-bin/message/send")
            .header("Authorization", "Bearer " + tokenService.getAccessToken())
            .bodyValue(request)
            .retrieve()
            .bodyToMono(WecomSendResponse.class);
    }

    /**
     * 群欢迎语发送
     */
    public Mono<WecomSendResponse> sendWelcome(String welcomeCode, WecomWelcomeContent content) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("welcome_code", welcomeCode);
        body.putAll(content.toMap());

        return webClient.post()
            .uri("/cgi-bin/externalcontact/send_welcome_msg")
            .header("Authorization", "Bearer " + tokenService.getAccessToken())
            .bodyValue(body)
            .retrieve()
            .bodyToMono(WecomSendResponse.class);
    }

    /**
     * 打标签
     */
    public Mono<WecomResponse> markTag(String externalUserId, List<String> addTag, List<String> removeTag) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("external_userid", externalUserId);
        body.put("add_tag", addTag);
        body.put("remove_tag", removeTag);

        return webClient.post()
            .uri("/cgi-bin/externalcontact/mark_tag")
            .header("Authorization", "Bearer " + tokenService.getAccessToken())
            .bodyValue(body)
            .retrieve()
            .bodyToMono(WecomResponse.class);
    }
}
```

### 5.2 企微回调处理

```java
@RestController
@RequestMapping("/api/v1/wecom/callback")
public class WecomCallbackController {

    private final WecomEventService eventService;
    private final DagEngine dagEngine;

    /**
     * 接收企微事件回调
     */
    @PostMapping
    public Mono<String> handleCallback(
            @RequestParam("msg_signature") String msgSignature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestBody String body) {

        return eventService.parseCallback(msgSignature, timestamp, nonce, body)
            .flatMap(event -> {
                // 根据事件类型触发画布
                return switch (event.eventType()) {
                    case "change_external_contact" -> handleContactChange(event);
                    case "change_external_chat" -> handleChatChange(event);
                    default -> Mono.just("success");
                };
            });
    }

    private Mono<String> handleContactChange(WecomEvent event) {
        // 触发企微事件触发器画布
        return dagEngine.triggerByEvent(
            "WECOM_EVENT_TRIGGER",
            event.changeType(), // add_external_contact/del_external_contact等
            event.toExecutionContext()
        ).thenReturn("success");
    }
}
```

---

## 六、前端组件设计

### 6.1 企微节点配置面板

```tsx
// WecomSendNodeConfig.tsx
export const WecomSendNodeConfig: React.FC<NodeConfigProps> = ({ node, onChange }) => {
  return (
    <div className="wecom-send-config">
      <Form.Item label="消息类型">
        <Select value={node.data.msgType} onChange={(v) => onChange({ msgType: v })}>
          <Select.Option value="text">文本消息</Select.Option>
          <Select.Option value="image">图片消息</Select.Option>
          <Select.Option value="link">链接消息</Select.Option>
          <Select.Option value="miniprogram">小程序消息</Select.Option>
        </Select>
      </Form.Item>

      <Form.Item label="发送员工">
        <UserSelect
          value={node.data.internalUserId}
          onChange={(v) => onChange({ internalUserId: v })}
          placeholder="选择发送消息的员工"
        />
      </Form.Item>

      <Form.Item label="客户ID">
        <VariableInput
          value={node.data.externalUserId}
          onChange={(v) => onChange({ externalUserId: v })}
          placeholder="支持变量如 $.userId"
        />
      </Form.Item>

      {node.data.msgType === 'text' && (
        <Form.Item label="消息内容">
          <TextArea
            value={node.data.content}
            onChange={(e) => onChange({ content: e.target.value })}
            placeholder="支持变量如 {{userName}}"
          />
        </Form.Item>
      )}

      {node.data.msgType === 'miniprogram' && (
        <>
          <Form.Item label="小程序AppID">
            <Input value={node.data.appid} onChange={(e) => onChange({ appid: e.target.value })} />
          </Form.Item>
          <Form.Item label="小程序页面路径">
            <Input value={node.data.page} onChange={(e) => onChange({ page: e.target.value })} />
          </Form.Item>
        </>
      )}
    </div>
  );
};
```

### 6.2 企微客户管理页面

```tsx
// WecomCustomerList.tsx
export const WecomCustomerList: React.FC = () => {
  const { data, isLoading } = useQuery(['wecom-customers'], () =>
    api.get('/api/v1/wecom/customers')
  );

  return (
    <div className="wecom-customer-list">
      <PageHeader title="企微客户管理" />

      <ProTable
        dataSource={data?.data}
        loading={isLoading}
        columns={[
          { title: '客户名称', dataIndex: 'name' },
          { title: '所属员工', dataIndex: 'internalUserName' },
          { title: '标签', dataIndex: 'tags', render: (tags) => tags.map(t => <Tag key={t}>{t}</Tag>) },
          { title: '状态', dataIndex: 'status', render: renderStatus },
          { title: '添加时间', dataIndex: 'createdAt' },
          {
            title: '操作',
            render: (_, record) => (
              <Space>
                <Button size="small" onClick={() => showDetail(record)}>详情</Button>
                <Button size="small" onClick={() => showTagModal(record)}>打标签</Button>
                <Button size="small" onClick={() => showTransferModal(record)}>转移</Button>
              </Space>
            )
          }
        ]}
      />
    </div>
  );
};
```

---

## 七、实施计划

### 7.1 阶段划分

| 阶段 | 内容 | 工时 | 产出 |
|------|------|------|------|
| **Phase 1.1** | 数据模型 + 基础API | 80h | 客户/群表结构、CRUD API |
| **Phase 1.2** | 企微API集成 | 120h | WecomApiClient、Token管理、回调处理 |
| **Phase 1.3** | Handler实现 | 160h | 5个Handler + 测试 |
| **Phase 1.4** | 前端页面 | 120h | 客户管理、群管理、节点配置面板 |
| **Phase 1.5** | 数据同步 | 80h | 定时同步任务、增量同步 |
| **合计** | | **560h** | **企微SCRM MVP** |

### 7.2 依赖关系

```
Phase 1.1 (数据模型)
    │
    ├──→ Phase 1.2 (企微API集成)
    │         │
    │         └──→ Phase 1.3 (Handler实现)
    │                   │
    │                   └──→ Phase 1.4 (前端页面)
    │
    └──→ Phase 1.5 (数据同步)
```

---

## 八、风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| 企微API更新频繁 | 维护成本高 | 封装WecomApiClient，变更集中处理 |
| Token过期 | 消息发送失败 | Token自动刷新 + 提前5分钟预警 |
| 消息发送限流 | 大批量发送失败 | 队列削峰 + 限流控制 |
| 客户数据同步延迟 | 数据不一致 | 增量同步 + 事件驱动实时更新 |

---

## 九、相关文档

- [目标架构总览](target-architecture-overview.md)
- [K8s部署方案](k8s-deployment-plan.md)
- [WebFlux→Spring MVC迁移](webflux-to-mvc-migration.md)
- [多数据源隔离方案](multi-datasource-isolation.md)
