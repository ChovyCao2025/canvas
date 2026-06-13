# 沉睡用户召回

| Field | Value |
| --- | --- |
| key | `dormant-user-winback` |
| category | Retention |
| riskLevel | MEDIUM |
| requiredPlugins | `canvas-plugin-schedule`, `canvas-plugin-coupon`, `canvas-plugin-message`, `canvas-plugin-risk` |
| docs | `docs/open-source/templates/dormant-user-winback.md` |

## Use Case

Run a daily winback journey for users who have not been active for at least 30
days. A risk policy protects the tenant from over-contacting the same user.

## Canvas Outline

```yaml
trigger:
  type: schedule
  cron: 0 9 * * *
nodes:
  - id: segment
    type: condition
    config:
      expression: user.daysSinceLastActive >= 30
  - id: risk
    type: risk.check
    config:
      policy: WINBACK_DAILY_CAP
  - id: coupon
    type: coupon.grant
    config:
      couponKey: WINBACK_20
  - id: message
    type: message.send
    config:
      channel: push
      template: winback_coupon
edges:
  - from: segment
    to: risk
    when: true
  - from: risk
    to: coupon
    when: allowed
  - from: coupon
    to: message
```

## Sample Payload

```json
{
  "user": {
    "id": "u_2002",
    "daysSinceLastActive": 45,
    "pushEnabled": true
  },
  "policy": {
    "dailyTouches": 0
  }
}
```

## Expected Trace

| Node | Outcome | Summary |
| --- | --- | --- |
| `segment` | MATCHED | 用户 45 天未活跃 |
| `risk` | MATCHED | 通过召回频控 |
| `coupon` | SENT | 发放 `WINBACK_20` 召回券 |
| `message` | SENT | 发送 Push 召回消息 |
