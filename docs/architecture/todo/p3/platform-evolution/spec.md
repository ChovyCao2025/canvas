# Spec: Platform Evolution

## Verification Status

Planning material, not directly verified implementation defects.

## Content Group

The previous `evolution/` documents describe long-term target architecture:

- service split;
- K8s deployment;
- multi-datasource isolation;
- WebFlux to MVC migration;
- data platform;
- WeCom SCRM module;
- production practice review.

These are not immediate bug-fix todo items. They should remain P3 until P0/P1 risks are reduced and product/team capacity is confirmed.

## Acceptance Criteria

- Evolution work is only promoted when it has an owner, success metrics, migration strategy, rollback plan, and dependency analysis.
- No large migration starts before security, consistency, and observability P0/P1 packages are addressed or explicitly accepted as risks.
