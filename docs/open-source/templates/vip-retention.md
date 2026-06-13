# 高价值用户维护

| Field | Value |
| --- | --- |
| key | `vip-retention` |
| category | VIP |
| riskLevel | MEDIUM |
| requiredPlugins | `canvas-plugin-coupon`, `canvas-plugin-assignment`, `canvas-plugin-message` |
| docs | `docs/open-source/templates/vip-retention.md` |

## Use Case

Identify high lifetime value users, grant an exclusive benefit, and notify the
assigned account owner for manual follow-up.

## Canvas Outline

```yaml
trigger:
  type: schedule
  cron: 0 11 * * MON
nodes:
  - id: vip
    type: condition
    config:
      expression: user.ltv >= 10000
  - id: benefit
    type: coupon.grant
    config:
      couponKey: VIP_SERVICE
  - id: owner
    type: lead.assign
    config:
      strategy: vip_owner
  - id: notify
    type: message.send
    config:
      channel: workchat
      template: vip_follow_up
edges:
  - from: vip
    to: benefit
    when: true
  - from: benefit
    to: owner
  - from: owner
    to: notify
```

## Sample Payload

```json
{
  "user": {
    "id": "u_7007",
    "ltv": 18800,
    "tier": "diamond"
  }
}
```

## Expected Trace

| Node | Outcome | Summary |
| --- | --- | --- |
| `vip` | MATCHED | 用户 LTV 达到 VIP 门槛 |
| `benefit` | SENT | 发放 `VIP_SERVICE` 权益 |
| `owner` | ASSIGNED | 分配客户经理 |
| `notify` | SENT | 通知客户经理跟进 |
