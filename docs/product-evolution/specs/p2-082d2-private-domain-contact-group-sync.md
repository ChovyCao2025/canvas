# P2-082D2 - Private-Domain Contact And Group Sync Spec

Priority: P2
Sequence: 082D2
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082d2-private-domain-contact-group-sync-plan.md`

## Goal

Add a production-usable private-domain sync foundation for SCRM contacts, relationship owners, customer groups, group members, sync runs, and audit evidence so the conversation workspace can operate on real WeCom/private-domain customer assets instead of only ad hoc conversation sessions.

## Research Inputs

- WeCom customer contact APIs expose member-scoped external-contact lists and detail payloads with follow-user relationship data.
- WeCom customer group APIs expose customer group list and detail pulls with owner filters, cursor pagination, group metadata, and members.
- The local implementation must not require real WeCom credentials in tests; provider collectors can normalize upstream payloads into this sync contract.

Reference documentation:

- https://developer.work.weixin.qq.com/document/path/92113
- https://developer.work.weixin.qq.com/document/path/92114
- https://developer.work.weixin.qq.com/document/path/92120
- https://developer.work.weixin.qq.com/document/path/92122

## Current Baseline

P2-080 and P2-082D now provide:

- Durable `conversation_session` and `conversation_message` records.
- Reply adapter normalization for sandbox, Web Chat, WhatsApp, social DM, and RCS style inbound replies.
- Tenant-scoped contact profiles, inbox work items, assignment, reminders, SOP tasks, timeline, and audit history.
- Frontend SCRM operator workspace under `/conversations`.

What remains missing is the durable private-domain asset model:

- No provider-scoped external contact records.
- No contact owner/follow-user relationship records.
- No customer group records.
- No group member records.
- No sync run ledger with counters, cursor, error, and actor evidence.
- No API for connector jobs or operators to ingest normalized private-domain snapshots.

## Product Design

The first sync slice is provider-agnostic but WeCom-compatible. It accepts normalized snapshots from a collector and persists them under the current tenant:

- `conversation_private_contact`: one row per provider external contact.
- `conversation_private_contact_owner`: relationship owner/follow-user state for a contact.
- `conversation_private_group`: customer group metadata.
- `conversation_private_group_member`: customer or staff member rows per group.
- `conversation_private_sync_run`: immutable-ish run ledger with status, counters, cursor, actor, and metadata.

The service also upserts `conversation_contact_profile` for each synced contact using stable user ids like `WECOM:external_userid`, so the operator workspace timeline can link future messages to private-domain profiles.

## API Contract

### Ingest Snapshot

`POST /canvas/conversations/private-domain/sync-runs`

```json
{
  "provider": "WECOM",
  "syncType": "FULL",
  "sourceCursor": "cursor-1",
  "contacts": [
    {
      "externalContactId": "wm-001",
      "displayName": "Alice Zhang",
      "avatar": "https://cdn.example.com/a.png",
      "corpName": "Example Ltd",
      "gender": "FEMALE",
      "unionIdHash": "sha256:abc",
      "ownerUserId": "sales-1",
      "remark": "VIP lead",
      "state": "followup:campaign-a",
      "addWay": "GROUP_QR",
      "tags": ["vip", "demo"],
      "attributes": { "city": "Shanghai" },
      "rawPayload": { "external_userid": "wm-001" }
    }
  ],
  "groups": [
    {
      "externalGroupId": "wr-001",
      "name": "VIP Leads Group",
      "ownerUserId": "sales-1",
      "status": "ACTIVE",
      "memberCount": 2,
      "createdAtRemote": "2026-06-06T09:00:00",
      "members": [
        {
          "memberUserId": "wm-001",
          "memberType": "EXTERNAL",
          "displayName": "Alice Zhang",
          "joinTime": "2026-06-06T09:30:00",
          "rawPayload": { "userid": "wm-001" }
        }
      ],
      "rawPayload": { "chat_id": "wr-001" }
    }
  ],
  "metadata": { "collector": "wecom-cron" }
}
```

Returns a `PrivateDomainSyncRunView` with status, counters, cursor, and timestamps.

### Contacts

`GET /canvas/conversations/private-domain/contacts?provider=WECOM&ownerUserId=sales-1&keyword=Alice&limit=50`

Returns provider contacts joined with owner relationship state.

### Groups

`GET /canvas/conversations/private-domain/groups?provider=WECOM&ownerUserId=sales-1&limit=50`

Returns customer groups with members.

### Sync Runs

`GET /canvas/conversations/private-domain/sync-runs?provider=WECOM&limit=20`

Returns recent run ledger entries.

## Functional Requirements

1. Snapshot ingestion must be tenant scoped.
2. Provider names must be normalized to uppercase.
3. Contact external ids and group external ids must be non-blank.
4. Contact upsert must be idempotent by `tenant_id + provider + external_contact_id`.
5. Contact owner upsert must be idempotent by `tenant_id + provider + external_contact_id + owner_user_id`.
6. Group upsert must be idempotent by `tenant_id + provider + external_group_id`.
7. Group member upsert must be idempotent by `tenant_id + provider + external_group_id + member_user_id`.
8. Contact ingestion must upsert `conversation_contact_profile` with `private_domain_source=provider` and stable `user_id=provider:externalContactId`.
9. Sync runs must record requested actor, status, counts, cursor, error message, and metadata.
10. Query limits must be bounded to 1..100 rows.
11. Query APIs must never return cross-tenant contacts, groups, or run records.

## Out Of Scope

- Calling live WeCom APIs from local tests.
- Storing provider secrets.
- Real-time webhook signature validation.
- Bidirectional writeback to WeCom tags/groups.
- Operator UI changes beyond backend API readiness.

## Acceptance Criteria

- This spec and plan are indexed after P2-082D.
- Migration `V307__private_domain_contact_group_sync.sql` creates all five tables with tenant/provider uniqueness and query indexes.
- Schema test proves table and index names exist.
- Service tests prove idempotent contact/owner/group/member upserts, contact-profile projection, sync-run counters, validation, and tenant-scoped reads.
- Controller tests prove current tenant/operator context is passed and query limits are bounded.
- Existing P2-082D workspace tests still pass.
- Focused backend tests pass with Java 21.
