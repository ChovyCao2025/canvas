# P2-082J - Monitoring Alert Fanout Spec

Priority: P2
Sequence: 082J
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082j-monitoring-alert-fanout-plan.md`

## Goal

Close the next monitoring gap by delivering external alert fanout for sentiment and competitor alerts. P2-082G creates alert workflow rows, P2-082H lets operators triage them, and P2-082I ingests mentions from signed public webhooks. This slice makes open monitoring alerts reach real operating channels through auditable outbound notifications.

## Implementation Status

Delivered backend first slice:

- Additive alert channel and alert delivery log schema.
- Tenant-scoped channel upsert with encrypted secret material and masked views.
- Generic webhook, Slack, Feishu, and Teams payload formatting.
- Canvas HMAC headers for generic webhook fanout and Feishu bot timestamp/sign payloads.
- Retry classification and delivery logs for success, retry, failed, and dead states.
- Manual alert dispatch API and delivery query API.
- Automatic fanout after alert creation without failing monitored item ingestion when delivery is unavailable.

## Current Baseline

Existing monitoring alerts are stored in `marketing_monitor_alert` and can be queried or resolved in the workbench. They do not have tenant-scoped delivery channels, encrypted destination secrets, outbound payload formatting, retry classification, manual resend, or delivery audit logs. That means urgent negative sentiment can remain inside the product until an operator opens the UI.

Existing foundations to reuse:

- `MarketingMonitoringService.ingestItem` for alert creation.
- `SecretCipher` and BCrypt for encrypted and non-displayable secret material.
- `WebhookRetryPolicy` retry classification for 2xx success, 429/5xx retry, hard 4xx failure, and dead-letter after max attempts.
- `WebClient.Builder` test pattern from existing webhook dispatch tests.

## Research Inputs

- Slack incoming webhooks accept JSON POSTs to a generated URL and support simple text or Block Kit payloads: https://api.slack.com/messaging/webhooks
- Lark/Feishu custom bots accept webhook POSTs with `msg_type` and `content`, and signed bots require timestamp/sign fields around the message payload: https://open.larksuite.com/document/client-docs/bot-v3/add-custom-bot
- Microsoft Teams incoming webhooks support JSON card-style posts to a channel webhook URL: https://learn.microsoft.com/microsoftteams/platform/webhooks-and-connectors/how-to/add-incoming-webhook
- PagerDuty Events API v2 is a provider-specific incident event API keyed by routing key, so it should be a later dedicated adapter rather than hidden inside the generic webhook channel: https://developer.pagerduty.com/docs/events-api-v2/overview/

## Approach Decision

Three options were considered:

- Add provider-specific Slack, Feishu, Teams, PagerDuty, email, SMS, and push adapters immediately. This is broader, but it would create many shallow integrations before the alert delivery contract is stable.
- Reuse the CDP webhook subscription table. This avoids new schema, but those subscriptions are event-bus oriented and do not model alert severity filters, alert type filters, channel payload style, or operator resend logs.
- Add monitoring-specific alert channels and delivery logs, with generic webhook plus Slack/Feishu/Teams payload formatting. This closes daily operating needs and leaves PagerDuty, email, SMS, and push as explicit adapters later.

Chosen approach: monitoring-specific alert channels and delivery logs.

## Product Design

Add tenant-scoped alert channels under monitoring:

- `channelKey`: stable operator key.
- `channelType`: `WEBHOOK`, `SLACK`, `FEISHU`, or `TEAMS`.
- `endpointUrl`: provider webhook URL.
- `enabled`: whether fanout can use the channel.
- `minSeverity`: lowest severity delivered to this channel.
- `alertTypes`: optional allowlist such as `NEGATIVE_SENTIMENT` or `COMPETITOR_NEGATIVE`.
- `signingMode`: `NONE`, `CANVAS_HMAC`, or `FEISHU_BOT`.
- `secret`: optional raw secret accepted on create/update and stored encrypted; never returned from list APIs.
- `maxAttempts`: delivery retry cap used by retry classification.

When a monitoring alert is created, the fanout service should attempt enabled matching channels. Failures must not fail mention ingestion or alert persistence. Operators can manually resend an alert through `POST /canvas/marketing-monitoring/alerts/{alertId}/dispatch`.

Each attempt creates a delivery log row with delivery id, alert id, channel id, attempt number, request payload, HTTP status, status, next retry time, terminal reason, and error message.

Payload behavior:

- `WEBHOOK` sends a Canvas event envelope with canonical `X-Canvas-*` headers and optional HMAC signature.
- `SLACK` sends text plus Block Kit sections for title, severity, type, scope, and reason.
- `FEISHU` sends text message payloads and, when configured with a secret, includes `timestamp` and `sign`.
- `TEAMS` sends a simple MessageCard payload.

## Functional Requirements

1. Add additive Flyway migration `V315__monitoring_alert_fanout.sql`.
2. Add `marketing_monitor_alert_channel` for destination configuration and encrypted secrets.
3. Add `marketing_monitor_alert_delivery` for delivery audit logs.
4. Add channel and delivery DOs, mappers, commands, and views.
5. Add `MarketingMonitorAlertFanoutService` with channel upsert, alert dispatch, delivery listing, provider payload formatting, secret encryption, and retry classification.
6. Wire alert fanout into `MarketingMonitoringService` after alert rows are inserted; fanout failures are swallowed after delivery logging.
7. Add monitoring controller APIs for channel upsert, manual dispatch, and delivery listing.
8. Do not return raw channel secrets after the upsert request.
9. Do not deliver disabled channels, below-threshold severities, mismatched alert types, disabled alerts, cross-tenant alerts, or resolved alerts.
10. Focused backend tests must cover schema, channel upsert secret storage, channel filtering, generic HMAC headers, Feishu signed payload, failed delivery logging, auto fanout from alert creation, controller wiring, and monitoring regression.

## Out Of Scope

- PagerDuty Events API v2 incident adapter.
- SMTP/email, SMS, push, WeCom app-message, and Lark tenant app-message delivery.
- Asynchronous retry worker that replays `RETRYING` rows.
- Complex escalation policies, schedules, on-call rotations, and dedup grouping.
- Frontend channel management UI.

## Acceptance Criteria

- This spec and plan are indexed after P2-082I.
- A tenant can configure an enabled webhook-style alert channel with encrypted secret material.
- Creating a negative monitoring item creates alert rows and attempts matching fanout without breaking ingestion if delivery fails.
- Manual dispatch returns per-channel delivery results.
- Delivery logs are tenant-scoped and queryable.
- Focused backend tests pass with Java 21.
