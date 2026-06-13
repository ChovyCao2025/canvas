# 优惠券审批发布

| Field | Value |
| --- | --- |
| key | `coupon-approval-release` |
| category | Promotion Governance |
| riskLevel | HIGH |
| requiredPlugins | `canvas-plugin-approval`, `canvas-plugin-coupon`, `canvas-plugin-message` |
| docs | `docs/open-source/templates/coupon-approval-release.md` |

## Use Case

Require manual approval before issuing high-value coupons. This protects budget
and keeps campaign release evidence visible in the execution trace.

## Canvas Outline

```yaml
trigger:
  type: manual
  event: campaign.release.requested
nodes:
  - id: approval
    type: approval.request
    config:
      approvalCode: HIGH_VALUE_COUPON
  - id: coupon
    type: coupon.grant
    config:
      couponKey: VIP_50
  - id: message
    type: message.send
    config:
      channel: email
      template: coupon_release
edges:
  - from: approval
    to: coupon
    when: approved
  - from: coupon
    to: message
```

## Sample Payload

```json
{
  "campaign": {
    "id": "cmp_3003",
    "couponKey": "VIP_50",
    "budget": 50000
  },
  "operator": {
    "id": "op_1",
    "team": "growth"
  }
}
```

## Expected Trace

| Node | Outcome | Summary |
| --- | --- | --- |
| `approval` | APPROVED | 审批 `HIGH_VALUE_COUPON` 通过 |
| `coupon` | SENT | 发放 `VIP_50` 优惠券 |
| `message` | SENT | 发送发布通知邮件 |
