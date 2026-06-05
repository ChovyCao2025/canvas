# ADR-003 Delivery And MQ Topic Split

status: Merged Into Existing Spec
owner: Delivery Platform

## Source Evidence

Optimization plans split delivery queue, delivery outbox, and MQ topic changes without a clear order.

## Current-Code Evidence

The codebase now includes delivery outbox, receipts, reconciliation, channel connector policies, and `CanvasDisruptorService` as a current dispatch lane. RocketMQ dependency remains a shared integration dependency.

## Decision

Merge delivery reliability into P0-003. Treat MQ topic split as a later architecture candidate, not the first implementation slice.

## Expected Benefit

Outbox and receipts improve correctness before topology changes add operational complexity.

## Cost

Medium for outbox rollout; high for MQ topic split because producers, consumers, routing, retry, and observability all change.

## Rollback

Keep current topic routes and outbox fallback. Roll back MQ split by disabling new route registration and replaying from outbox.

## Proof Command

`node tools/perf/runtime-migration-baseline.mjs --format json`

## Accepted Evidence

Required for MQ split: outbox reconciliation success, per-topic backlog metrics, replay evidence, and consumer rollback rehearsal.

## Child Spec

Existing implementation: P0-003. Later child spec required: `p2-018c-mq-topic-split-rollout`.

## Dependency Notes

P0-003 must complete before MQ split rollout. This ADR corrects old delivery queue/outbox sequencing contradictions.
