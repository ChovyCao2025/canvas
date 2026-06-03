# Plan: Data Security And Tenant Isolation

1. Design credential storage.
   - Add encrypted credential field or credential reference table.
   - Support key rotation and migration from existing plaintext rows.

2. Clean seed data.
   - Replace root/root demos with local-only sample values or disabled examples.

3. Normalize tenant fields.
   - Add `tenantId` to core DO classes.
   - Backfill and migrate nullable columns toward NOT NULL where appropriate.

4. Enforce tenant scope.
   - Add shared query helpers or interceptors.
   - Update list/get/update/delete paths.

5. Add tests.
   - Cross-tenant read/write denial.
   - Credential masking in API responses.
   - Migration/backfill behavior.
