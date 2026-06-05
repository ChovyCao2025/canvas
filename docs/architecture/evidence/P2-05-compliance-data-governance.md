# P2-05 Compliance Data Governance Evidence

Status date: 2026-06-05

## Scope Completed In This Pass

- Added compliance data inventory, audit event matrix, deletion/retention workflow, and evidence checklist.
- Added `PiiMaskingService` for phone, email, open_id, token, password, credential, and secret masking.
- Added `CanvasAuditLogDO` and `CanvasAuditLogMapper` for existing `canvas_audit_log`.
- Added `AuditEventService` to write masked metadata into audit detail JSON.
- Added `DataDeletionService` with dry-run and execute modes for governed user-data tables plus `canvas_execution_trace` payload matches.
- Added `MarketingPolicyServiceTest` coverage for opt-in, opt-out, missing consent, active suppression, no active suppression, and expired/all-channel suppression query matching.
- Added exception-message masking through `GlobalExceptionHandler`.
- Added controller audit hooks for canvas lifecycle, tenant mutation, and data-source credential mutation paths.
- Migrated CDP detail response masking to `PiiMaskingService` for phone, email, open_id, and secret-like profile properties.

## TDD / Red-Green Notes

- `PiiMaskingServiceTest`, `AuditEventServiceTest`, and `DataDeletionServiceTest` first failed at testCompile because the service/DO/mapper classes did not exist.
- After the minimum implementation, those three tests passed: 5 tests, 0 failures, 0 errors.
- `GlobalExceptionHandlerTest.clientErrorMessagesMaskSensitiveValues` first failed because 400 responses returned raw phone/email/token values.
- After adding `PiiMaskingService.maskText()` and applying it in `GlobalExceptionHandler.fail`, `GlobalExceptionHandlerTest` passed: 6 tests, 0 failures, 0 errors.
- `MarketingPolicyServiceTest` is characterization coverage for consent and suppression behavior; it passed with 7 tests after adding expired/all-channel suppression query coverage.
- Follow-up coverage added for canvas lifecycle audit hooks, data-source audit hooks, execution-trace deletion matches, CDP response masking, and expired/all-channel suppression SQL-wrapper behavior.

## Verification Commands

Combined compliance verification:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn test -pl canvas-engine -Dtest=AuditEventServiceTest,DataDeletionServiceTest,PiiMaskingServiceTest,MarketingPolicyServiceTest,GlobalExceptionHandlerTest,CdpUserServiceTest,CanvasControllerOperatorLoopTest,DataSourceConfigControllerTest
```

Result: 30 tests, 0 failures, 0 errors, 0 skipped. Verified fresh on 2026-06-05 at 11:38 +08:00.

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn test -pl canvas-engine -Dtest=PiiMaskingServiceTest,AuditEventServiceTest,DataDeletionServiceTest
```

Result: 5 tests, 0 failures, 0 errors, 0 skipped.

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn test -pl canvas-engine -Dtest=MarketingPolicyServiceTest
```

Result: 7 tests, 0 failures, 0 errors, 0 skipped.

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn test -pl canvas-engine -Dtest=GlobalExceptionHandlerTest
```

Result: 6 tests, 0 failures, 0 errors, 0 skipped.

## Verification Blockers Resolved

During the combined Maven verification, unrelated in-progress warehouse/BI tests initially blocked `testCompile`:

- `CdpWarehouseE2eCertificationRunServiceTest` and `CdpWarehouseE2eCertificationRunControllerTest` referenced missing `CdpWarehouseE2eCertificationRun*` production classes.
- The minimal run DO/mapper/service/controller were added so those tests compile and pass.
- Focused verification for the blocker passed: 7 tests, 0 failures, 0 errors.
- A later `BiResourceMovement*` compile blocker was already resolved by the existing BI resource movement files present in the worktree; rerunning after the warehouse classes were present allowed testCompile to proceed.

## Known Follow-Ups

- Auth login/admin-user, execution replay, consent/suppression mutation, deletion request intake, and incident workflow audit hooks remain broader product integration work. The P2-05 local implementation now covers canvas lifecycle, tenant mutation, data-source credential mutation, masked audit writes, trace deletion matches, CDP detail response masking, and suppression query coverage.
- Do not create `V92__compliance_governance.sql`; the repository already has `V92__enforce_core_tenant_not_null.sql` and current migration head is `V241__bi_dataset_portal_version_history.sql`. Use the next available migration version if schema changes are needed.
