# 风险触达拦截

| Field | Value |
| --- | --- |
| key | `risk-blocked-outreach` |
| category | Risk |
| riskLevel | HIGH |
| requiredPlugins | `canvas-plugin-risk`, `canvas-plugin-message` |
| docs | `docs/open-source/templates/risk-blocked-outreach.md` |

## Use Case

Run a risk and consent check before outreach. If the user is not eligible for
the channel, block the send step and retain the decision in trace output.

## Canvas Outline

```yaml
trigger:
  type: webhook
  event: campaign.outreach.requested
nodes:
  - id: risk
    type: risk.check
    config:
      policy: OUTREACH_COMPLIANCE
  - id: message
    type: message.send
    config:
      channel: sms
      template: compliant_offer
edges:
  - from: risk
    to: message
    when: allowed
```

## Sample Payload

```json
{
  "user": {
    "id": "u_9009",
    "consent": false
  },
  "campaign": {
    "id": "cmp_9009",
    "channel": "sms"
  }
}
```

## Expected Trace

| Node | Outcome | Summary |
| --- | --- | --- |
| `risk` | BLOCKED | 用户未授权短信触达 |
| `message` | SKIPPED | 风险阻断后不发送消息 |
