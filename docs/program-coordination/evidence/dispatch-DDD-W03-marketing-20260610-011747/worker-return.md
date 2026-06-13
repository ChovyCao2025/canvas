status: DONE_WITH_CONCERNS
task id: DDD-W03
dispatch id: dispatch-DDD-W03-marketing-20260610-011747
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004
assigned task pack: docs/ddd-rewrite/task-packs/03-worker-marketing.md
files changed:
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingCampaignCommand.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingCampaignFacade.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingCampaignLinkCommand.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingCampaignLinkView.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingCampaignReadinessFinding.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingCampaignReadinessView.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingCampaignView.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingCampaignApplicationService.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/CampaignBudget.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/CampaignDateRange.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/CampaignKey.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/CampaignLinkStatus.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/CampaignStatus.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingCampaign.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingCampaignLink.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingCampaignReadinessIssue.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingCampaignReadinessPolicy.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingCampaignReadinessReport.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingCampaignRepository.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/adapter/persistence/MarketingCampaignLinkDO.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/adapter/persistence/MarketingCampaignLinkMapper.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/adapter/persistence/MarketingCampaignMasterDO.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/adapter/persistence/MarketingCampaignMasterMapper.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/adapter/persistence/MarketingCampaignPersistenceConverter.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/adapter/persistence/MybatisMarketingCampaignRepository.java
  - backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/**/package-info.java
  - backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/adapter/persistence/MarketingCampaignPersistenceConverterTest.java
  - backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MarketingCampaignApplicationServiceTest.java
  - backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/domain/CampaignDateRangeTest.java
  - backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/domain/CampaignKeyTest.java
  - backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/domain/MarketingCampaignReadinessPolicyTest.java
contracts changed:
  - Added canvas-context-marketing public API records and MarketingCampaignFacade.
  - No backend parent POM, canvas-web, canvas-boot, canvas-common, CDP, or execution contract files changed.
old classes migrated:
  - org.chovy.canvas.domain.marketing.MarketingCampaignCommand -> org.chovy.canvas.marketing.api.MarketingCampaignCommand
  - org.chovy.canvas.domain.marketing.MarketingCampaignView -> org.chovy.canvas.marketing.api.MarketingCampaignView
  - org.chovy.canvas.domain.marketing.MarketingCampaignLinkCommand -> org.chovy.canvas.marketing.api.MarketingCampaignLinkCommand
  - org.chovy.canvas.domain.marketing.MarketingCampaignLinkView -> org.chovy.canvas.marketing.api.MarketingCampaignLinkView
  - org.chovy.canvas.domain.marketing.MarketingCampaignReadinessFinding -> org.chovy.canvas.marketing.api.MarketingCampaignReadinessFinding
  - org.chovy.canvas.domain.marketing.MarketingCampaignReadinessView -> org.chovy.canvas.marketing.api.MarketingCampaignReadinessView
  - org.chovy.canvas.domain.marketing.MarketingCampaignService -> MarketingCampaignApplicationService plus domain policy/repository port
  - org.chovy.canvas.dal.dataobject.MarketingCampaignMasterDO -> org.chovy.canvas.marketing.adapter.persistence.MarketingCampaignMasterDO
  - org.chovy.canvas.dal.dataobject.MarketingCampaignLinkDO -> org.chovy.canvas.marketing.adapter.persistence.MarketingCampaignLinkDO
  - org.chovy.canvas.dal.mapper.MarketingCampaignMasterMapper -> org.chovy.canvas.marketing.adapter.persistence.MarketingCampaignMasterMapper
  - org.chovy.canvas.dal.mapper.MarketingCampaignLinkMapper -> org.chovy.canvas.marketing.adapter.persistence.MarketingCampaignLinkMapper
new public api:
  - MarketingCampaignFacade#upsertCampaign
  - MarketingCampaignFacade#listCampaigns
  - MarketingCampaignFacade#linkResource
  - MarketingCampaignFacade#listLinks
  - MarketingCampaignFacade#readiness
  - MarketingCampaignFacade#unlinkResource
domain model changes:
  - Added CampaignKey normalization value object.
  - Added CampaignDateRange validation value object.
  - Added CampaignBudget currency/default amount validation value object.
  - Added CampaignStatus and CampaignLinkStatus normalization enums.
  - Added MarketingCampaign and MarketingCampaignLink domain records.
  - Added MarketingCampaignReadinessPolicy with blocker/warning status calculation.
persistence ownership changes:
  - Marketing campaign master/link DOs and mappers now live under canvas-context-marketing adapter.persistence.
  - MybatisMarketingCampaignRepository is the only production class using campaign mappers.
  - Channel*, Message*, and other ambiguous marketing persistence rows were not migrated.
tests run:
  - cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing
  - cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=MarketingCampaignPersistenceConverterTest
  - bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
verification result:
  - Marketing module tests passed: 18 tests, 0 failures, 0 errors.
  - Persistence converter focused test passed: 3 tests, 0 failures, 0 errors.
  - DDD guardrail checks passed; advisory matches remain in pre-existing risk validator TypeCompatibility names.
verification output summary/path:
  - Maven output showed BUILD SUCCESS for canvas-context-marketing.
  - Surefire reports under backend/canvas-context-marketing/target/surefire-reports/.
evidence artifact paths:
  - docs/program-coordination/evidence/dispatch-DDD-W03-marketing-20260610-011747/worker-return.md
guardrail checks:
  - Domain has no MyBatis, Spring Web, Redis, RocketMQ, or ObjectMapper imports.
  - Application service does not import Mapper or DO classes.
  - No new module imports old canvas-engine domain/engine/dal/infrastructure internals.
failure modes reviewed:
  - Null command rejection.
  - Campaign key/resource key normalization and blank rejection.
  - Unsupported campaign/link status rejection.
  - Date range end-before-start rejection.
  - Default budget amount and currency normalization.
  - Negative budget rejection.
  - Tenant-scoped campaign and link ownership.
  - Limit clamping.
  - Required launch links, inactive required links, primary dependency, measurement dependency, and optional triage readiness behavior.
  - Nested/null JSON metadata and brief map conversion without adding a dependency.
compatibility evidence:
  - MarketingCampaignApplicationServiceTest ports old MarketingCampaignService behavior for upsert, list, link, readiness, and unlink flows.
  - MarketingCampaignPersistenceConverterTest verifies old table names, mapper inheritance, launch flag mapping, and JSON field mapping.
  - CampaignKeyTest, CampaignDateRangeTest, and MarketingCampaignReadinessPolicyTest cover the pilot domain rules from the child spec.
temporary bridges:
  - None.
open risks:
  - MarketingCampaignController compatibility tests and canvas-web delegation were not implemented because DDD-W03 explicitly forbids backend/canvas-web/** changes and http-api-inventory assigns HTTP adapter migration to DDD-C09.
  - The persistence JSON codec is intentionally local because this dispatch had no module POM dependency exception; it covers map/list/scalar/null payloads used by the campaign pilot.
coordinator actions needed:
  - Carry MarketingCampaignController compatibility and web delegation into DDD-C09 or a coordinator-approved bridge dispatch with explicit canvas-web scope.
  - Decide whether DDD-W03 DONE_WITH_CONCERNS is sufficient for first-wave G5 integration or whether a narrow controller compatibility follow-up is required before G5.
ledger update:
  - Clear active dispatch dispatch-DDD-W03-marketing-20260610-011747.
  - Mark DDD-W03 as DONE_WITH_CONCERNS.
  - Record marketing module Maven tests and DDD guardrail evidence.
rollback path:
  - Revert files under backend/canvas-context-marketing/**; backup/pre-ddd-osg-20260609-222054 is the pre-rewrite restore point.
