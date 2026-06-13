# 生日权益触达

| Field | Value |
| --- | --- |
| key | `birthday-benefit` |
| category | Lifecycle |
| riskLevel | LOW |
| requiredPlugins | `canvas-plugin-schedule`, `canvas-plugin-coupon`, `canvas-plugin-message` |
| docs | `docs/open-source/templates/birthday-benefit.md` |

## Use Case

Run a daily birthday check, issue a birthday benefit, and send a greeting to
the user on the same day.

## Canvas Outline

```yaml
trigger:
  type: schedule
  cron: 0 10 * * *
nodes:
  - id: birthday
    type: condition
    config:
      expression: user.birthday == today
  - id: benefit
    type: coupon.grant
    config:
      couponKey: BIRTHDAY_GIFT
  - id: message
    type: message.send
    config:
      channel: sms
      template: birthday_benefit
edges:
  - from: birthday
    to: benefit
    when: true
  - from: benefit
    to: message
```

## Sample Payload

```json
{
  "user": {
    "id": "u_6006",
    "birthday": "today",
    "phone": "+8613800000006"
  }
}
```

## Expected Trace

| Node | Outcome | Summary |
| --- | --- | --- |
| `birthday` | MATCHED | 用户生日命中当天 |
| `benefit` | SENT | 发放生日权益 |
| `message` | SENT | 发送生日祝福短信 |
