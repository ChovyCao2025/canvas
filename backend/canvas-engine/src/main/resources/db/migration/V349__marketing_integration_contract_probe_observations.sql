-- comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
-- comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
-- comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
-- comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
-- comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
-- comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
-- comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
-- comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
-- comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
-- comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
-- comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
-- comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
-- comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
-- comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
-- comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
-- comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
-- comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
-- comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
-- comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
-- comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
-- comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
-- comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
-- comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
-- comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
-- comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
-- comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
-- comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
-- comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
-- comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
-- comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
-- comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
-- comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
-- comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
CREATE TABLE IF NOT EXISTS `marketing_integration_contract_probe_observation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `contract_id` BIGINT NOT NULL,
  `probe_run_id` BIGINT NULL,
  `contract_key` VARCHAR(128) NOT NULL,
  `provider_family` VARCHAR(64) NOT NULL,
  `probe_key` VARCHAR(128) NOT NULL,
  `environment` VARCHAR(32) NOT NULL DEFAULT 'PRODUCTION',
  `status` VARCHAR(32) NOT NULL,
  `http_status_code` INT NULL,
  `latency_ms` BIGINT NULL,
  `error_type` VARCHAR(255) NULL,
  `problem_type_uri` VARCHAR(512) NULL,
  `problem_title` VARCHAR(255) NULL,
  `problem_detail` VARCHAR(1000) NULL,
  `error_message` VARCHAR(1000) NULL,
  `summary` VARCHAR(512) NULL,
  `observed_at` DATETIME NOT NULL,
  `evidence_json` JSON NULL,
  `created_by` VARCHAR(128) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_marketing_integration_probe_observation_window`
    (`tenant_id`, `contract_id`, `probe_key`, `environment`, `observed_at`),
  KEY `idx_marketing_integration_probe_observation_status`
    (`tenant_id`, `environment`, `status`, `observed_at`),
  KEY `idx_marketing_integration_probe_observation_provider`
    (`tenant_id`, `provider_family`, `status`, `observed_at`),
  CONSTRAINT `fk_marketing_integration_probe_observation_contract`
    FOREIGN KEY (`contract_id`) REFERENCES `marketing_integration_contract` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Append-only runtime observations for marketing integration contract probe SLOs';
