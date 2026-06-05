# Platform Primitives

Date: 2026-06-05

Status: P3-09 contract artifact. These primitives are required before service extraction or data-platform consumers depend on shared identity, events, or tenant state.

## OneID

OneID is the tenant-scoped canonical customer identity used by CDP, analytics, reach, and future integration read models.

Rules:

- OneID is unique inside one tenant.
- OneID is not the same as `cdp_user_profile.user_id`; existing `userId` remains compatible as the current CDP user identifier.
- OneID must not be generated from a raw PII value.
- OneID survives source identity merge and split operations.
- OneID history is append-only for audit; current mapping is a view over the audited graph.

## Source Identity

A source identity is an observed identifier from a channel or system.

Allowed examples:

- `phone`
- `email`
- `union_id`
- `external_userid`
- `member_id`
- `device_id`
- `anonymous_id`
- `canvas_user_id`
- `cdp_user_id`

Each source identity must record:

- tenant ID;
- identity type;
- normalized identity value hash or encrypted value;
- source system;
- source reference ID;
- observed-at timestamp;
- verification status;
- confidence;
- retention class;
- deletion behavior.

## Merge

Merge creates or updates a OneID graph edge when two source identities are considered the same person.

Merge rules:

- deterministic verified identities may merge immediately;
- probabilistic matches require confidence score, evidence source, and review rule;
- merge never deletes historical source identity edges;
- merge emits an auditable event;
- merge must be reversible through split;
- merge must not cross tenant boundaries.

## Split

Split separates one or more source identities from a OneID when a conflict or correction is accepted.

Split rules:

- split requires reason, operator, evidence, and approval owner;
- split creates a new OneID or moves the source identity to an existing OneID only by explicit rule;
- split emits an auditable event;
- downstream read models must receive a correction event;
- stale analytics aggregates must be marked for backfill or accepted as historical.

## Confidence

Confidence is a numeric score plus reason code.

| Confidence class | Score | Example | Behavior |
|---|---:|---|---|
| verified | 1.00 | login account binds phone, verified WeCom union ID | Automatic merge allowed. |
| strong | 0.90-0.99 | multiple verified identities observed in one authenticated session | Merge allowed with automated audit. |
| weak | 0.50-0.89 | device/IP/time-window signal | No real-time merge; review or offline model required. |
| conflict | below threshold or contradictory evidence | shared phone, reused device, mismatched union ID | Block merge and create conflict record. |

## Conflict

Conflicts must be explicit records, not hidden overwrite behavior.

Conflict record fields:

- tenant ID;
- candidate OneIDs;
- source identities;
- conflict type;
- evidence;
- severity;
- recommended action;
- owner;
- status;
- created-at and resolved-at.

## Audit

Every OneID change must be audit-ready.

Audit fields:

- event ID;
- tenant ID;
- OneID;
- source identity reference;
- action: `LINKED`, `MERGED`, `SPLIT`, `CONFLICT_OPENED`, `CONFLICT_RESOLVED`, `DELETED`;
- actor or system source;
- reason;
- before and after references;
- occurred-at timestamp;
- correlation ID;
- retention class.

## Compatibility With Existing CDP Fields

Existing fields remain authoritative until the OneID graph is implemented:

- `cdp_user_profile.tenant_id`
- `cdp_user_profile.user_id`
- `cdp_user_identity.tenant_id`
- `cdp_user_identity.user_id`
- `cdp_user_identity.identity_type`
- `cdp_user_identity.identity_value`
- `cdp_event_log.user_id`
- `cdp_event_log.anonymous_id`

Compatibility rules:

- `user_id` remains a valid current CDP identifier.
- OneID may be added as a new field or read model, but existing APIs must continue to accept `userId`.
- Event consumers must not assume OneID is present until the compatibility window starts.
- Backfill must map existing `user_id` and source identities into OneID without changing existing row IDs.
- Tenant scope remains mandatory for every identity query.

## Non-Goals

- No physical identity service extraction is approved.
- No global cross-tenant OneID is approved.
- No automatic merge of weak probabilistic identities is approved.
- No WeCom external user ID merge is approved before P3-08 and P3-09 gates pass.
