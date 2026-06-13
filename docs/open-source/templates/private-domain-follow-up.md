# 私域跟进旅程

| Field | Value |
| --- | --- |
| key | `private-domain-follow-up` |
| category | Private Domain |
| riskLevel | MEDIUM |
| requiredPlugins | `canvas-plugin-webhook`, `canvas-plugin-private-domain`, `canvas-plugin-message` |
| docs | `docs/open-source/templates/private-domain-follow-up.md` |

## Use Case

When a member joins a private-domain community, add the operational tag, create
a follow-up task, and send a workchat welcome message.

## Canvas Outline

```yaml
trigger:
  type: webhook
  event: private_domain.member_joined
nodes:
  - id: tag
    type: private-domain.tag
    config:
      tag: new_community_member
  - id: task
    type: private-domain.task
    config:
      slaHours: 24
  - id: message
    type: message.send
    config:
      channel: workchat
      template: community_welcome
edges:
  - from: tag
    to: task
  - from: task
    to: message
```

## Sample Payload

```json
{
  "member": {
    "id": "pd_1010",
    "source": "qr_campaign"
  },
  "community": {
    "id": "group_1"
  }
}
```

## Expected Trace

| Node | Outcome | Summary |
| --- | --- | --- |
| `tag` | MATCHED | 写入私域成员标签 |
| `task` | ASSIGNED | 创建 24 小时跟进任务 |
| `message` | SENT | 发送企微欢迎消息 |
