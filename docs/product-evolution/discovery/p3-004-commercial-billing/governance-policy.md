# Commercial Billing Governance Policy

## Required Gates

- Billing capabilities require finance, legal, support, product, and engineering owners before child specs.
- Discovery metrics are not billable commitments.
- Customer charging, entitlement enforcement, invoices, payments, renewals, and upgrade recommendations require separate child specs.
- Tenant-scoped measurement and dispute handling must be proven before a metric becomes billable.

## No Runtime Rollout

This slice has no Flyway migration and no customer charging behavior. Rollback is reverting or amending the discovery package and keeping capability statuses below `Accepted For Child Spec`.
