# A/B 实验触达

| Field | Value |
| --- | --- |
| key | `ab-message-experiment` |
| category | Experiment |
| riskLevel | MEDIUM |
| requiredPlugins | `canvas-plugin-experiment`, `canvas-plugin-message` |
| docs | `docs/open-source/templates/ab-message-experiment.md` |

## Use Case

Split an audience into experiment variants and send the matching message copy
while recording which branch was skipped.

## Canvas Outline

```yaml
trigger:
  type: manual
  event: campaign.experiment.started
nodes:
  - id: split
    type: experiment.split
    config:
      experimentKey: WELCOME_COPY_AB
  - id: messageA
    type: message.send
    config:
      channel: sms
      template: welcome_a
  - id: messageB
    type: message.send
    config:
      channel: sms
      template: welcome_b
edges:
  - from: split
    to: messageA
    when: variant=A
  - from: split
    to: messageB
    when: variant=B
```

## Sample Payload

```json
{
  "user": {
    "id": "u_8008"
  },
  "experiment": {
    "key": "WELCOME_COPY_AB",
    "assignedVariant": "A"
  }
}
```

## Expected Trace

| Node | Outcome | Summary |
| --- | --- | --- |
| `split` | MATCHED | 用户进入 A 版实验组 |
| `messageA` | SENT | 发送 A 版欢迎消息 |
| `messageB` | SKIPPED | B 版分支未命中 |
