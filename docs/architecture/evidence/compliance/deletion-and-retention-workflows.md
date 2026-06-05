# Deletion And Retention Workflows

Status date: 2026-06-05

This workflow covers GDPR/PIPL-style user erasure requests while preserving required compliance evidence. It complements `docs/architecture/evidence/capacity/retention-policy.md`.

## Delete Request Flow

1. Intake: capture requester, actor, tenant, user id, request id, reason, and authorization evidence.
2. Authorization: only tenant admins or compliance operators can run deletion. Super-admin cross-tenant execution requires explicit tenant id.
3. Identity matching: resolve CDP user profile and identities. Matching must include `cdp_user_profile`, `cdp_user_identity`, tags, marketing consent, suppression, message send records, and execution traces that may contain the same user id.
4. dry-run: run `DataDeletionService` in `dryRun=true` mode and return table-level matched counts without deleting.
5. Approval: compliance owner reviews dry-run counts, legal hold state, and retention conflicts.
6. Execute: run `DataDeletionService` in `dryRun=false` mode. The current implementation deletes governed rows from `cdp_user_profile`, `cdp_user_identity`, `cdp_user_tag`, `marketing_consent`, `marketing_suppression`, `message_send_record`, and `canvas_execution_trace` rows whose input/output/error payloads contain the requested user id.
7. tombstone: retain a minimal non-PII request record with request id, tenant, hashed user id, action, table counts, actor, and timestamp.
8. audit: write `deletion dry-run`, `deletion execute`, or `deletion rejected` to the audit log with masked metadata.
9. Evidence capture: store command output, table counts, approval record, and audit row id in the compliance evidence checklist.
10. Rollback limit: deletion is not reversible after execute. Recovery is limited to backups under incident/legal approval.

## Table Handling

| Table family | Current action | Retention note |
| --- | --- | --- |
| CDP user profile | Delete rows by tenant and user id | Profile PII is erased unless legal hold applies |
| CDP user identity | Delete rows by tenant and user id | Identity links are erased with the profile |
| CDP user tag | Delete current tag rows by tenant and user id | Tag history requires retention/legal-hold review |
| marketing consent | Delete rows after tombstone/evidence capture | Consent evidence may be retained in minimized tombstone form |
| marketing suppression | Delete rows after legal-hold check | Active suppression may be retained only if legally required |
| message send records | Delete rows by tenant and user id | Delivery dispute evidence must be minimized before deletion |
| execution traces | Delete rows whose input/output/error payloads match the requested user id | Online retention is 30 days; incident archives follow legal hold |
| audit logs | Retain | Audit rows are compliance evidence and should not carry raw PII |

## Retention Controls

- Runtime execution tables follow the online retention windows in `docs/architecture/evidence/capacity/retention-policy.md`.
- Audit logs are retained for at least 365 days.
- Legal hold pauses deletion and retention cleanup for affected table families.
- Cleanup jobs must support dry-run first, then write row counts and archive manifests before destructive actions.
