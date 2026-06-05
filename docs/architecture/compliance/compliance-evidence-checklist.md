# Compliance Evidence Checklist

Status date: 2026-06-05

| Evidence type | owner | Source | Verification |
| --- | --- | --- | --- |
| audit evidence | Compliance owner | `AuditEventService`, `canvas_audit_log`, `docs/architecture/compliance/audit-event-matrix.md`, controller mutation hooks | `mvn test -pl canvas-engine -Dtest=AuditEventServiceTest,CanvasControllerOperatorLoopTest,DataSourceConfigControllerTest` |
| deletion evidence | Compliance owner | `DataDeletionService`, dry-run output, deletion approval record | `mvn test -pl canvas-engine -Dtest=DataDeletionServiceTest` |
| retention evidence | Runtime platform | `docs/architecture/capacity/retention-policy.md`, retention run tables | Review retention run id, cutoff, status, archive target |
| masking evidence | Security owner | `PiiMaskingService`, `GlobalExceptionHandler`, CDP response masking | `mvn test -pl canvas-engine -Dtest=PiiMaskingServiceTest,GlobalExceptionHandlerTest,CdpUserServiceTest` |
| consent/suppression evidence | Marketing platform | `MarketingPolicyService`, `marketing_consent`, `marketing_suppression` | `mvn test -pl canvas-engine -Dtest=MarketingPolicyServiceTest` |
| incident response evidence | SRE / Security owner | Incident ticket, legal hold decision, audit event, retention archive manifest | Link incident id, affected tables, owner, and closure date |
| credential evidence | Security owner | `data_source_config`, `SecretCipher`, data-source audit events | Verify encrypted stored password and masked audit metadata |

## Compliance Test Command

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn test -pl canvas-engine -Dtest=AuditEventServiceTest,DataDeletionServiceTest,PiiMaskingServiceTest,MarketingPolicyServiceTest,GlobalExceptionHandlerTest,CdpUserServiceTest,CanvasControllerOperatorLoopTest,DataSourceConfigControllerTest
```

## Evidence Storage Rules

- Every deletion request must attach dry-run counts, approval, execute counts, and audit row id.
- Every incident response record must name owner, affected table family, legal hold state, and remediation evidence.
- Every credential change must avoid raw password/token capture; only masked values or presence flags are allowed.
