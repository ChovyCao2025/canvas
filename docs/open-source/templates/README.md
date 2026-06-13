# Official Template Catalog

This directory contains the public Template Pack v1 sidecar content for the
Marketing Canvas open source growth track. These files are documentation and
sample data only. Backend import, draft creation, dependency enforcement, and
dry-run validation remain owned by the later OSG-W09 backend work.

Each template follows the contract in
`docs/open-source-growth/contracts/template-pack-v1.md`:

- stable `key`
- display `title`
- `category`
- `riskLevel`
- `requiredPlugins`
- canvas outline
- `samplePayload`
- `expectedTrace`
- docs path

## Catalog

| Key | Title | Category | Risk | Docs |
| --- | --- | --- | --- | --- |
| `new-user-welcome` | 新用户欢迎旅程 | Lifecycle | LOW | [new-user-welcome.md](new-user-welcome.md) |
| `dormant-user-winback` | 沉睡用户召回 | Retention | MEDIUM | [dormant-user-winback.md](dormant-user-winback.md) |
| `coupon-approval-release` | 优惠券审批发布 | Promotion Governance | HIGH | [coupon-approval-release.md](coupon-approval-release.md) |
| `ai-copy-review-publish` | AI 文案生成与人工审核 | AI Operations | MEDIUM | [ai-copy-review-publish.md](ai-copy-review-publish.md) |
| `lead-capture-assignment` | 表单线索分配 | Lead Management | LOW | [lead-capture-assignment.md](lead-capture-assignment.md) |
| `birthday-benefit` | 生日权益触达 | Lifecycle | LOW | [birthday-benefit.md](birthday-benefit.md) |
| `vip-retention` | 高价值用户维护 | VIP | MEDIUM | [vip-retention.md](vip-retention.md) |
| `ab-message-experiment` | A/B 实验触达 | Experiment | MEDIUM | [ab-message-experiment.md](ab-message-experiment.md) |
| `risk-blocked-outreach` | 风险触达拦截 | Risk | HIGH | [risk-blocked-outreach.md](risk-blocked-outreach.md) |
| `private-domain-follow-up` | 私域跟进旅程 | Private Domain | MEDIUM | [private-domain-follow-up.md](private-domain-follow-up.md) |

## Import Preconditions

When backend import is implemented, every template must be blocked before draft
creation if any required plugin is missing or disabled. Repeated imports should
either be idempotent by template key or produce an explicit clone.
