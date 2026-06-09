# Marketing Pilot Child Spec

This child spec defines the first concrete DDD rewrite pilot. The marketing
campaign slice is the reference implementation for later contexts.

---

## Purpose

Use marketing campaign rewrite to prove:

- API/application/domain/adapter split.
- Rich domain model where useful.
- Persistence ownership under context module.
- Controller compatibility through `canvas-web`.
- Architecture rules can catch forbidden dependencies.

---

## Old Classes

Primary old classes:

```text
org.chovy.canvas.web.MarketingCampaignController
org.chovy.canvas.domain.marketing.MarketingCampaignService
org.chovy.canvas.domain.marketing.MarketingCampaignCommand
org.chovy.canvas.domain.marketing.MarketingCampaignView
org.chovy.canvas.domain.marketing.MarketingCampaignLinkCommand
org.chovy.canvas.domain.marketing.MarketingCampaignLinkView
org.chovy.canvas.domain.marketing.MarketingCampaignReadinessView
org.chovy.canvas.domain.marketing.MarketingCampaignReadinessFinding
org.chovy.canvas.dal.dataobject.MarketingCampaignMasterDO
org.chovy.canvas.dal.mapper.MarketingCampaignMasterMapper
```

Secondary old classes discovered by inventory:

```text
MarketingCampaignLinkDO
MarketingCampaignLinkMapper
MarketingCampaign-related controller tests
MarketingCampaign service tests
MarketingCampaign schema tests
```

---

## Target Classes

```text
canvas-context-marketing
  api.MarketingCampaignCommand
  api.MarketingCampaignView
  api.MarketingCampaignLinkCommand
  api.MarketingCampaignLinkView
  api.MarketingCampaignReadinessView
  api.MarketingCampaignReadinessFinding
  api.MarketingCampaignFacade

  application.MarketingCampaignApplicationService

  domain.MarketingCampaign
  domain.MarketingCampaignLink
  domain.CampaignKey
  domain.CampaignStatus
  domain.CampaignBudget
  domain.CampaignDateRange
  domain.MarketingCampaignReadinessPolicy
  domain.MarketingCampaignRepository

  adapter.persistence.MarketingCampaignMasterDO
  adapter.persistence.MarketingCampaignLinkDO
  adapter.persistence.MarketingCampaignMasterMapper
  adapter.persistence.MarketingCampaignLinkMapper
  adapter.persistence.MybatisMarketingCampaignRepository
  adapter.persistence.MarketingCampaignPersistenceConverter
```

---

## Compatibility Requirements

Preserve:

```text
POST /canvas/marketing-campaigns behavior
GET /canvas/marketing-campaigns behavior
POST /canvas/marketing-campaigns/links behavior
GET /canvas/marketing-campaigns/{campaignId}/links behavior
GET /canvas/marketing-campaigns/{campaignId}/readiness behavior
DELETE /canvas/marketing-campaigns/links/{linkId} behavior
```

Preserve business behavior:

```text
campaignKey normalization
status normalization
currency normalization
default budget amount behavior
date range validation
required launch link readiness blocker
primary dependency readiness blocker
tenant scoping
list limit clamping
```

---

## Domain Rules

`CampaignKey`:

- rejects blank keys
- trims whitespace
- normalizes to lower-case stable key

`CampaignDateRange`:

- allows open start/end when old behavior allowed it
- rejects `endAt` before `startAt`

`CampaignBudget`:

- defaults missing amount to zero when old behavior did
- validates non-negative amount if old behavior already implied it
- normalizes currency using old behavior

`MarketingCampaignReadinessPolicy`:

- campaign must be active for production launch
- launch-required links must exist
- launch-required links must be active
- at least one active required link must be primary

---

## Tests

Required tests:

```text
CampaignKeyTest
CampaignDateRangeTest
MarketingCampaignReadinessPolicyTest
MarketingCampaignApplicationServiceTest
MybatisMarketingCampaignRepositoryTest or mapper-focused equivalent
MarketingCampaignControllerCompatibilityTest
```

Architecture checks:

```text
domain has no MyBatis import
domain has no Spring Web import
application service does not import Mapper directly
controller does not import DO or Mapper
```

---

## Completion Criteria

The pilot is complete when:

- [ ] Marketing campaign slice compiles in `canvas-context-marketing`.
- [ ] Controller can call `MarketingCampaignFacade`.
- [ ] Compatibility tests cover old route behavior.
- [ ] Domain tests cover key value objects and readiness policy.
- [ ] Persistence adapter is the only package using campaign mappers.
- [ ] The result is documented as the reference implementation for other
      context workers.
