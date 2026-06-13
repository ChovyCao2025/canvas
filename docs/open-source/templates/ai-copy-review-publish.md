# AI 文案生成与人工审核

| Field | Value |
| --- | --- |
| key | `ai-copy-review-publish` |
| category | AI Operations |
| riskLevel | MEDIUM |
| requiredPlugins | `canvas-plugin-ai`, `canvas-plugin-approval`, `canvas-plugin-message` |
| docs | `docs/open-source/templates/ai-copy-review-publish.md` |

## Use Case

Generate campaign copy with an AI operator, then require a human approval step
before the message can be sent.

## Canvas Outline

```yaml
trigger:
  type: manual
  event: copy.requested
nodes:
  - id: generate
    type: ai.generate-copy
    config:
      promptKey: seasonal_offer
  - id: review
    type: approval.request
    config:
      approvalCode: AI_COPY_REVIEW
  - id: message
    type: message.send
    config:
      channel: sms
      template: ai_reviewed_copy
edges:
  - from: generate
    to: review
  - from: review
    to: message
    when: approved
```

## Sample Payload

```json
{
  "campaign": {
    "id": "cmp_4004",
    "offer": "summer bundle"
  },
  "audience": {
    "segment": "recent_buyers"
  }
}
```

## Expected Trace

| Node | Outcome | Summary |
| --- | --- | --- |
| `generate` | GENERATED | 生成活动短信候选文案 |
| `review` | APPROVED | 人工审核通过 |
| `message` | SENT | 发送审核后的短信 |
