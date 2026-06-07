# P2-089 - Growth Activity Center Spec

Priority: P2
Sequence: 089
Source: user-requested marketing activity and referral closed-loop design, local capability audit, and official market references
Implementation plan: `../plans/p2-089-growth-activity-center-plan.md`

## Goal

Add a tenant-scoped Growth Activity Center that turns the existing marketing middle-platform capabilities into launchable, measurable growth activities. The center owns activity type, rules, reward pools, participants, grant ledger, referral relations, activity readiness, and performance closure. It does not replace the journey canvas, campaign master ledger, coupon provider, loyalty account module, content hub, delivery outbox, risk engine, or BI workbench.

## Current Baseline

The repository already has important marketing foundations:

- Journey execution, triggers, audience branches, wait nodes, message sends, and side-effect idempotency.
- `COMMIT_ACTION` can issue coupons and points through `CouponHandler` and `PointsOperationHandler`.
- Coupon issuing exists as an execution action that calls an external coupon provider. It is not an activity-management product surface.
- Loyalty has member accounts, rules, transaction journals, redemption, and benefit eligibility APIs under `/canvas/loyalty`.
- P2-086 exposes the marketing middle-platform control plane.
- P2-087 adds the `marketing_campaign_master` and campaign resource links.
- P2-088 adds integration contract registry and runtime probe governance.
- Content hub, message templates, delivery outbox, BI, attribution, experiment governance, SCRM, private-domain, paid-media, search, creator, and DSP slices exist as independent modules or planned production slices.

The missing layer is an activity business object. Operators cannot yet define an invite campaign, task incentive, member-day activity, coupon promotion, winback offer, or content challenge as one governed activity with rules, rewards, participants, budgets, journey links, grants, reconciliation, and analytics.

## Market Reference Inputs

- Talon.One treats campaigns as the primary promotion setup entity and exposes official docs for campaign types, coupons, referrals, campaign budgets, achievements, giveaways, loyalty programs, rules, effects, and analytics: https://docs.talon.one/docs/product/campaigns/overview and https://docs.talon.one/docs/product/campaigns/referrals/referral-overview
- Shopify separates automatic discounts and code discounts, and covers percentage/fixed amount, Buy X Get Y, free shipping, discount classes, custom discount functions, admin configuration, and checkout execution: https://shopify.dev/docs/apps/build/discounts
- Braze Canvas is a multi-message user journey builder with audience entry, schedule/action triggers, steps, variants, conversion events, and analytics; Braze promotion code lists are unique, time-limited code inventories deducted before messages are sent: https://www.braze.com/docs/user_guide/messaging/canvas/canvas_basics/ and https://www.braze.com/docs/user_guide/messaging/design_and_edit/personalize/sources/promotion_codes/
- Salesforce Loyalty Management centers loyalty around member accounts, programs, ledgers, transaction journals, tiers, and benefits instead of a flat points counter: https://help.salesforce.com/s/articleView?id=sf.loyalty_mgmt.htm
- Adobe Journey Optimizer offer decisioning separates offers from journeys and uses eligibility, ranking, placement, and caps as decision-time controls: https://experienceleague.adobe.com/en/docs/journey-optimizer

The practical pattern is consistent: mature marketing platforms do not stop at coupon sending. They connect campaign/activity metadata, audience eligibility, reward inventory, limits and budgets, referral or task progress, journey execution, delivery evidence, conversion analytics, and operational reconciliation.

## Product Principles

- Activity Center is a business orchestration layer above existing modules.
- Every growth activity links to one `marketing_campaign_master` row.
- Journey canvases remain the execution path for triggers, waits, messages, branches, and `COMMIT_ACTION`.
- Reward pools manage eligibility, allocation, budget, inventory, pre-consumption, grants, and reconciliation; the actual benefit can still be issued by coupon, loyalty, points, external provider, or manual fulfillment.
- Risk checks and approval gates are consumed as dependencies, not reimplemented here.
- BI and attribution remain the reporting backplane; Activity Center exposes operational metrics and links to dashboards.
- Activity types are first-class only when they need different participant state or grant semantics. More specific business scenes are templates.

## Activity Taxonomy

### First-Class Activity Types

1. `BENEFIT_PROMOTION`
   - Purpose: discount, coupon, cashback, free shipping, Buy X Get Y, fixed amount off, product/category/store-specific offer, limited-time promotion.
   - Examples: new-user coupon pack, flash sale coupon, order-threshold discount, abandoned-cart coupon, product-line promo.
   - Core state: audience eligibility, reward pool, per-user/per-order limits, budget counters, grant ledger, redemption feedback.

2. `REFERRAL_INVITE`
   - Purpose: invite a user, customer, merchant, member, or account to complete a qualifying action and reward one or both sides.
   - Examples: invite new user to register, invite friend to first order, invite tenant admin, invite merchant to activate, share product link for reward.
   - Core state: referral code/link, inviter, invitee, relationship state, qualification event, anti-abuse status, inviter reward, invitee reward, tiered milestone reward.

3. `TASK_INCENTIVE`
   - Purpose: reward completion of one or more tasks or milestones.
   - Examples: complete profile, bind phone, place first order, browse content, check in, complete onboarding, submit feedback, invite three users, purchase three times.
   - Core state: task definitions, progress ledger, completion policy, reset period, reward grant, badge/achievement outcome.

4. `LOYALTY_MEMBER_ACTIVITY`
   - Purpose: run member-centric activities on top of the existing loyalty account and benefit module.
   - Examples: member day, birthday benefit, tier upgrade challenge, double-points window, points redemption sale, expiring points reminder.
   - Core state: member tier eligibility, loyalty rule link, points earn/burn journal link, benefit eligibility, redemption/grant evidence.

5. `RETENTION_WINBACK`
   - Purpose: recover churn risk, inactive users, abandoned carts, dropped funnels, or lapsed members with targeted journeys and offers.
   - Examples: lapsed-user coupon, 7-day inactive recall, abandoned checkout, churn-risk retention, failed-payment rescue.
   - Core state: CDP/audience trigger, suppression policy, offer decision, journey link, conversion window, lift/ROI tracking.

6. `CONTENT_PRIVATE_DOMAIN_ACTIVITY`
   - Purpose: activity surfaces that depend on content, SCRM, community, or private-domain interaction rather than only purchase.
   - Examples: group campaign, content share challenge, UGC submission reward, KOL/KOC co-created activity, live/event signup reward.
   - Core state: content release link, SCRM group/contact link, task/event progress, moderation/risk dependency, reward grant.

### Template Scenes

These are not separate persistence types in the first slice. They are templates composed from the first-class types:

- New-user acquisition package: `BENEFIT_PROMOTION` plus onboarding journey.
- First-order conversion: `BENEFIT_PROMOTION` plus `TASK_INCENTIVE`.
- Referral with both-side reward: `REFERRAL_INVITE`.
- Referral milestone ladder: `REFERRAL_INVITE` plus `TASK_INCENTIVE`.
- Member day: `LOYALTY_MEMBER_ACTIVITY`.
- Double-points campaign: `LOYALTY_MEMBER_ACTIVITY`.
- Birthday or anniversary benefit: `LOYALTY_MEMBER_ACTIVITY`.
- Abandoned cart or checkout rescue: `RETENTION_WINBACK`.
- Lapsed-user recall: `RETENTION_WINBACK`.
- Private-domain group mission: `CONTENT_PRIVATE_DOMAIN_ACTIVITY` plus `TASK_INCENTIVE`.
- Creator/KOC collaboration reward: `CONTENT_PRIVATE_DOMAIN_ACTIVITY`.
- Paid-media acquisition offer: `BENEFIT_PROMOTION` plus paid-media audience sync.
- Search landing-page promotion: `BENEFIT_PROMOTION` plus search marketing campaign link.
- Product launch campaign: `BENEFIT_PROMOTION` plus content release and journey.

## Closed-Loop Model

Every activity follows the same lifecycle:

1. Draft
   - Operator creates activity, picks first-class type, links or creates campaign master, sets objective and owner.
2. Configure
   - Operator configures audience, schedule, rule sets, reward pools, budget, risk dependency, content, journey links, and BI target.
3. Readiness
   - System evaluates campaign status, journey publication, reward inventory, budget, provider contract health, risk gate, content release, delivery readiness, and analytics link.
4. Publish
   - Activity becomes eligible for participant entry and grants. Publishing is blocked when readiness is `BLOCKED`.
5. Participate
   - Events create or update participants, referral relations, task progress, and qualification states.
6. Grant
   - Reward pool pre-consumes budget or inventory, calls the configured grant channel, persists idempotent grant rows, and records provider or loyalty evidence.
7. Reconcile
   - Provider receipts, loyalty journals, coupon results, delivery receipts, redemption events, and conversion events update grant and activity state.
8. Analyze
   - Activity reports participation, qualification, grant, redemption, conversion, cost, ROI, referral funnel, task completion, cohort, and lift metrics.
9. Close
   - Operator archives or closes activity, unused budget is released, open grants are reconciled, and final report evidence is linked to campaign master.

## Domain Model

Additive tables:

- `growth_activity`
  - Tenant, activity key, name, type, status, campaign id, objective, owner, schedule, channel scope, audience refs, risk policy ref, experiment ref, dashboard ref, metadata, created/updated actors, timestamps.
- `growth_activity_rule_set`
  - Eligibility, qualification, suppression, limit, and reward-selection rule JSON with revision and status.
- `growth_reward_pool`
  - Reward pool key, reward type, grant channel, coupon type key, loyalty reward key, points type, external contract key, inventory mode, total inventory, per-user limit, per-referral limit, budget amount, cost currency, status.
- `growth_budget_counter`
  - Pool-level and activity-level counters for reserved, granted, failed, canceled, redeemed, expired, and cost totals.
- `growth_activity_participant`
  - User/profile/account identity, activity state, entry source, entry event, first/last participation time, qualification state, suppression reason, metadata.
- `growth_reward_grant`
  - Idempotent grant ledger with activity id, pool id, participant id, referral relation id, task progress id, grant reason, status, idempotency key, provider request/response evidence, cost, timestamps.
- `growth_activity_event`
  - Append-only event log for entry, progress, qualification, grant, redemption, conversion, reconciliation, risk decision, and operator action.
- `growth_referral_code`
  - Code/link, inviter identity, activity id, max uses, expiry, status, share metadata.
- `growth_referral_relation`
  - Inviter, invitee, code, relation state, qualification event, risk result, inviter grant id, invitee grant id, timestamps.
- `growth_task_definition`
  - Activity task key, event key, completion policy, target count/value, reset policy, reward pool link, display/order metadata.
- `growth_task_progress`
  - Participant task progress, progress count/value, completed state, completed time, grant id, evidence JSON.

## APIs

Add authenticated APIs under `/canvas/growth-activities`:

- `POST /canvas/growth-activities`
- `GET /canvas/growth-activities`
- `GET /canvas/growth-activities/{activityId}`
- `PUT /canvas/growth-activities/{activityId}`
- `POST /canvas/growth-activities/{activityId}/readiness`
- `POST /canvas/growth-activities/{activityId}/publish`
- `POST /canvas/growth-activities/{activityId}/pause`
- `POST /canvas/growth-activities/{activityId}/close`
- `POST /canvas/growth-activities/{activityId}/reward-pools`
- `GET /canvas/growth-activities/{activityId}/reward-pools`
- `POST /canvas/growth-activities/{activityId}/participants`
- `GET /canvas/growth-activities/{activityId}/participants`
- `POST /canvas/growth-activities/{activityId}/events`
- `GET /canvas/growth-activities/{activityId}/events`
- `POST /canvas/growth-activities/{activityId}/grants/evaluate`
- `POST /canvas/growth-activities/{activityId}/grants`
- `GET /canvas/growth-activities/{activityId}/grants`
- `POST /canvas/growth-activities/{activityId}/grants/reconcile`
- `POST /canvas/growth-activities/{activityId}/referral-codes`
- `GET /canvas/growth-activities/{activityId}/referral-codes`
- `POST /canvas/growth-activities/{activityId}/referrals`
- `GET /canvas/growth-activities/{activityId}/referrals`
- `POST /canvas/growth-activities/{activityId}/tasks`
- `GET /canvas/growth-activities/{activityId}/tasks`
- `POST /canvas/growth-activities/{activityId}/task-progress`
- `GET /canvas/growth-activities/{activityId}/report`

## Frontend Surface

Add `/growth-activities` and link it from the marketing platform page.

Required views:

- Activity list with type, status, campaign, objective, schedule, readiness, participants, grants, budget, conversion, and owner.
- Activity create/edit wizard with first-class type presets.
- Activity detail with overview, readiness, linked resources, reward pools, rules, participants, grants, referral relations, tasks, event timeline, and report.
- Reward pool panel with budget and inventory counters.
- Referral panel with code generation, invite relationships, qualification, reward state, and abuse flags.
- Task panel with definitions, participant progress, completion, and reward grants.
- Operational grant ledger with retry, reconcile, cancel, and provider evidence.
- Report panel with funnel, cost, conversion, ROI, redemption, referral, and task-completion metrics.

## Readiness Gate

Activity readiness returns `READY`, `DEGRADED`, or `BLOCKED`.

Blockers:

- Missing or inactive campaign master link.
- Missing published journey when the activity needs journey execution.
- Missing active reward pool when rewards are enabled.
- Reward pool has no inventory, budget, or valid grant channel.
- External coupon, loyalty, or provider contract is blocked by P2-088 evidence.
- Required content release is not active.
- Required risk-control dependency is unavailable.
- Required audience or CDP source is unavailable.
- Activity has no analytics dashboard or metric target.
- Open failed grants exceed the configured threshold.

Warnings:

- Activity uses API-only capability without an operator surface.
- Reward pool inventory is below warning threshold.
- Conversion window is missing but activity can still launch.
- Experiment or attribution link is missing for a non-critical activity.
- Provider probe is fresh but degraded.

## Execution Flows

### New-User Coupon Pack

1. Activity type: `BENEFIT_PROMOTION`.
2. Link campaign master and onboarding canvas.
3. Configure new-user audience, coupon reward pool, per-user limit, and budget.
4. Publish after readiness passes.
5. Canvas `COMMIT_ACTION` issues coupon through existing coupon handler.
6. Grant ledger records idempotency key, provider response, delivery evidence, redemption, and conversion.

### Invite Both-Side Reward

1. Activity type: `REFERRAL_INVITE`.
2. Generate referral code or share link for inviter.
3. Invitee registration or purchase event creates relation and checks qualification.
4. Risk dependency evaluates self-invite, duplicate device, repeated payment instrument, and abnormal velocity signals.
5. Invitee receives welcome benefit and inviter receives coupon, points, or loyalty reward.
6. Report shows invite funnel, qualified invites, reward cost, conversion, and abuse blocks.

### Task Incentive

1. Activity type: `TASK_INCENTIVE`.
2. Define task events and completion policy.
3. Events update task progress.
4. Completion creates a grant request and one idempotent reward grant.
5. Task report shows progress, completion, grant, and conversion.

### Member Day

1. Activity type: `LOYALTY_MEMBER_ACTIVITY`.
2. Configure member tier eligibility and loyalty benefit/rule link.
3. Journey notifies eligible members and records grant or redemption.
4. Loyalty journals and benefit eligibility remain owned by the loyalty module.

### Lapsed-User Winback

1. Activity type: `RETENTION_WINBACK`.
2. Use CDP audience for lapsed users or churn-risk recommendations.
3. Use journey canvas for multi-step reach, wait, suppression, and offer decision.
4. Reward grant, message delivery, conversion, and holdout metrics feed the activity report.

## Non-Goals

- Do not introduce a standalone coupon provider or coupon admin surface.
- Do not replace loyalty account, tier, journal, redemption, or benefit APIs.
- Do not duplicate campaign master ledger.
- Do not duplicate BI, attribution, experiment governance, content release, delivery outbox, or risk-control engines.
- Do not add real external provider adapters in this slice.
- Do not model every marketing scene as a new activity type.

## Acceptance Criteria

- Operators can create, list, edit, publish, pause, close, and inspect tenant-scoped growth activities.
- Every activity can link to a `marketing_campaign_master` row and appears in the campaign resource graph.
- Readiness evaluates campaign, journey, reward, budget, provider contract, content, risk, audience, delivery, and analytics dependencies.
- Benefit promotion activities can configure reward pools and create idempotent grant rows that call existing coupon or points execution paths.
- Referral activities can create codes, relations, qualification events, both-side reward grants, and abuse-block evidence.
- Task activities can define tasks, update progress from events, complete tasks, and grant rewards once.
- Loyalty member activities can consume existing loyalty account, benefit, earn, and redemption APIs without duplicating loyalty state.
- Retention winback activities can link CDP/audience, journey, reward, and conversion reports.
- Grant reconciliation updates failed, successful, redeemed, expired, and canceled states without double-granting.
- Frontend activity center exposes list, wizard, readiness, reward pool, referral, task, grant ledger, and report panels.
- Control plane includes Growth Activity Center as a capability and readiness input.
- Focused backend and frontend tests cover schema, services, controllers, grant idempotency, readiness, API client, and page rendering.
