# 新用户欢迎旅程

| Field | Value |
| --- | --- |
| key | `new-user-welcome` |
| category | Lifecycle |
| riskLevel | LOW |
| requiredPlugins | `canvas-plugin-webhook`, `canvas-plugin-coupon`, `canvas-plugin-message` |
| docs | `docs/open-source/templates/new-user-welcome.md` |

## Use Case

When a user registers, confirm the user is in the new lifecycle stage, grant a
first-order coupon, and send a welcome message.

## Canvas Outline

```yaml
trigger:
  type: webhook
  event: user.registered
nodes:
  - id: segment
    type: condition
    config:
      expression: user.lifecycleStage == "new"
  - id: coupon
    type: coupon.grant
    config:
      couponKey: WELCOME_10
  - id: message
    type: message.send
    config:
      channel: sms
      template: welcome_coupon
edges:
  - from: segment
    to: coupon
    when: true
  - from: coupon
    to: message
```

## Sample Payload

```json
{
  "event": "user.registered",
  "user": {
    "id": "u_1001",
    "lifecycleStage": "new",
    "phone": "+8613800000001"
  }
}
```

## Expected Trace

| Node | Outcome | Summary |
| --- | --- | --- |
| `segment` | MATCHED | 用户处于 new 生命周期阶段 |
| `coupon` | SENT | 发放 `WELCOME_10` 首单券 |
| `message` | SENT | 发送欢迎短信 |
