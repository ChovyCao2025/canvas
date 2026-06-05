# AI Commerce Bets Governance Policy

## Required Gates

- AI may recommend actions in discovery packages, but customer-facing, spend-affecting, privacy-affecting, and partner-facing changes require human approval.
- A bet reaches `Accepted For Child Spec` only with customer evidence, dependency readiness, model-risk review, approval boundary, proof command, rollback path, owner, and child spec path.
- Commercial or industry bets require a named commercial or industry owner before implementation planning.
- Globalization and privacy bets require legal, privacy, and data residency review before implementation planning.

## AI Approval Boundary

AI-generated recommendations must stay read-only until a human approves the action. No autonomous customer communication, spend decision, pricing decision, privacy-impacting data-use change, or partner-facing claim is allowed in this discovery slice.

## No Runtime Rollout

This slice has no Flyway migration and no runtime behavior. Rollback is reverting or amending the discovery package and keeping bet statuses below `Accepted For Child Spec`.
