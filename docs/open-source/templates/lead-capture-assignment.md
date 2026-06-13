# 表单线索分配

| Field | Value |
| --- | --- |
| key | `lead-capture-assignment` |
| category | Lead Management |
| riskLevel | LOW |
| requiredPlugins | `canvas-plugin-webhook`, `canvas-plugin-assignment`, `canvas-plugin-message` |
| docs | `docs/open-source/templates/lead-capture-assignment.md` |

## Use Case

After a form submission, qualify the lead and route it to a sales owner using a
regional round-robin strategy.

## Canvas Outline

```yaml
trigger:
  type: webhook
  event: lead.submitted
nodes:
  - id: qualify
    type: condition
    config:
      expression: lead.score >= 60
  - id: assign
    type: lead.assign
    config:
      strategy: region_round_robin
  - id: notify
    type: message.send
    config:
      channel: workchat
      template: new_lead_assigned
edges:
  - from: qualify
    to: assign
    when: true
  - from: assign
    to: notify
```

## Sample Payload

```json
{
  "lead": {
    "id": "lead_5005",
    "score": 82,
    "region": "east"
  },
  "form": {
    "id": "trial_request"
  }
}
```

## Expected Trace

| Node | Outcome | Summary |
| --- | --- | --- |
| `qualify` | MATCHED | 线索分数满足分配门槛 |
| `assign` | ASSIGNED | 按 east 区域分配销售 |
| `notify` | SENT | 发送销售通知 |
