# Cross-Context Dependency Inventory

Initial direct source coupling rows that must be replaced by public APIs, ports, or events during migration.

Generated on 2026-06-09 from the current `backend/canvas-engine` source tree and `context-ownership-seed.md`.
source path:
  org.chovy.canvas.domain.analytics.AudienceMaterializationService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationService.java

target module:
  canvas-context-cdp

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-cdp must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  AudienceMaterializationServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.audience.StableUserIndexService

source path:
  org.chovy.canvas.domain.approval.CanvasPublishApprovalAutoActionHandler -> canvas-context-canvas

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/CanvasPublishApprovalAutoActionHandler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-canvas with api/application facade, port, or event after migration

required tests:
  CanvasPublishApprovalAutoActionHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.canvas.CanvasService

source path:
  org.chovy.canvas.domain.approval.HttpLarkApprovalClient -> canvas-context-marketing

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/HttpLarkApprovalClient.java

target module:
  canvas-platform

target role:
  cross-context dependency review

coordinator decision:
  canvas-platform must replace direct source coupling to canvas-context-marketing with api/application facade, port, or event after migration

required tests:
  HttpLarkApprovalClientContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialResolver

source path:
  org.chovy.canvas.domain.bi.ai.LlmBiAskDataPlanner -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/LlmBiAskDataPlanner.java

target module:
  canvas-context-bi

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-bi must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  LlmBiAskDataPlannerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.llm.AiLlmGateway

source path:
  org.chovy.canvas.domain.bi.ai.LlmBiDashboardDraftPlanner -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/LlmBiDashboardDraftPlanner.java

target module:
  canvas-context-bi

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-bi must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  LlmBiDashboardDraftPlannerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.llm.AiLlmGateway

source path:
  org.chovy.canvas.domain.bi.ai.LlmBiInsightPlanner -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/LlmBiInsightPlanner.java

target module:
  canvas-context-bi

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-bi must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  LlmBiInsightPlannerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.llm.AiLlmGateway

source path:
  org.chovy.canvas.domain.bi.ai.LlmBiInterpretationPlanner -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/LlmBiInterpretationPlanner.java

target module:
  canvas-context-bi

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-bi must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  LlmBiInterpretationPlannerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.llm.AiLlmGateway

source path:
  org.chovy.canvas.domain.bi.ai.LlmBiReportPlanner -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/LlmBiReportPlanner.java

target module:
  canvas-context-bi

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-bi must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  LlmBiReportPlannerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.llm.AiLlmGateway

source path:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceOnboardingService.java

target module:
  canvas-context-bi

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-bi must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  BiDatasourceOnboardingServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.datasource.DataSourceCredentialCipher

source path:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceRuntimeService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceRuntimeService.java

target module:
  canvas-context-bi

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-bi must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  BiDatasourceRuntimeServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.datasource.DataSourceCredentialCipher

source path:
  org.chovy.canvas.domain.bi.query.BiQueryExecutionService -> canvas-context-cdp

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionService.java

target module:
  canvas-context-bi

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-bi must replace direct source coupling to canvas-context-cdp with api/application facade, port, or event after migration

required tests:
  BiQueryExecutionServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService, org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService, org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService

source path:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryRuntimeService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java

target module:
  canvas-context-bi

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-bi must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  BiDeliveryRuntimeServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.notification.NotificationCreateCommand, org.chovy.canvas.domain.notification.NotificationService

source path:
  org.chovy.canvas.domain.canvas.CanvasOpsService -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java

target module:
  canvas-context-canvas

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-canvas must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  CanvasOpsServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.trigger.TriggerPreCheckService

source path:
  org.chovy.canvas.domain.canvas.CanvasPrePublishCheckService -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasPrePublishCheckService.java

target module:
  canvas-context-canvas

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-canvas must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  CanvasPrePublishCheckServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.dag.DagGraph, org.chovy.canvas.engine.dag.DagParser

source path:
  org.chovy.canvas.domain.canvas.CanvasService -> canvas-context-cdp, canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java

target module:
  canvas-context-canvas

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-canvas must replace direct source coupling to canvas-context-cdp, canvas-context-execution with api/application facade, port, or event after migration

required tests:
  CanvasServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.audience.AudienceSnapshotService, org.chovy.canvas.engine.dag.DagGraph, org.chovy.canvas.engine.dag.DagParser, org.chovy.canvas.engine.handlers.GroovyHandler, org.chovy.canvas.engine.trigger.CanvasExecutionService, org.chovy.canvas.engine.trigger.CanvasSchedulerService, org.chovy.canvas.engine.trigger.TriggerPreCheckService

source path:
  org.chovy.canvas.domain.canvas.CanvasVersionCleanupJob -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJob.java

target module:
  canvas-context-canvas

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-canvas must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  CanvasVersionCleanupJobContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.wait.WaitSubscriptionService

source path:
  org.chovy.canvas.domain.canvas.TestUserRerunService -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/TestUserRerunService.java

target module:
  canvas-platform

target role:
  cross-context dependency review

coordinator decision:
  canvas-platform must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  TestUserRerunServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.trigger.CanvasExecutionService

source path:
  org.chovy.canvas.domain.canvas.UserInputService -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/UserInputService.java

target module:
  canvas-context-canvas

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-canvas must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  UserInputServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.context.ExecutionContext, org.chovy.canvas.engine.trigger.CanvasExecutionService

source path:
  org.chovy.canvas.domain.cdp.CdpEventIngestionService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java

target module:
  canvas-context-cdp

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-cdp must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  CdpEventIngestionServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.meta.EventDefinitionCacheService

source path:
  org.chovy.canvas.domain.cdp.CdpTagOperationService -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperationService.java

target module:
  canvas-context-cdp

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-cdp must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  CdpTagOperationServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.concurrent.BackgroundTaskExecutor

source path:
  org.chovy.canvas.domain.cdp.CdpUserService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserService.java

target module:
  canvas-context-cdp

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-cdp must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  CdpUserServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.compliance.PiiMaskingService

source path:
  org.chovy.canvas.domain.conversation.ConversationIngressService -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressService.java

target module:
  canvas-context-conversation

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-conversation must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  ConversationIngressServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.wait.WaitResumeService

source path:
  org.chovy.canvas.domain.conversation.LlmConversationAiReplyGenerator -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/LlmConversationAiReplyGenerator.java

target module:
  canvas-context-conversation

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-conversation must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  LlmConversationAiReplyGeneratorContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.llm.AiLlmGateway

source path:
  org.chovy.canvas.domain.conversation.WhatsAppWebhookPayloadMapper -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapper.java

target module:
  canvas-context-conversation

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-conversation must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  WhatsAppWebhookPayloadMapperContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.delivery.DeliveryReceiptRequest

source path:
  org.chovy.canvas.domain.marketing.GrowthBenefitPromotionGrantAdapter -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthBenefitPromotionGrantAdapter.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  GrowthBenefitPromotionGrantAdapterContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.context.ExecutionContext, org.chovy.canvas.engine.handler.NodeHandler, org.chovy.canvas.engine.handler.NodeResult

source path:
  org.chovy.canvas.domain.marketing.GrowthLoyaltyAdapter -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthLoyaltyAdapter.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  GrowthLoyaltyAdapterContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.loyalty.LoyaltyService

source path:
  org.chovy.canvas.domain.marketing.MarketingFormService -> canvas-context-canvas, canvas-context-cdp

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingFormService.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-context-canvas, canvas-context-cdp with api/application facade, port, or event after migration

required tests:
  MarketingFormServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.cdp.CdpUserService, org.chovy.canvas.engine.disruptor.CanvasDisruptorService

source path:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeScheduler -> canvas-context-cdp

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeScheduler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-cdp with api/application facade, port, or event after migration

required tests:
  MarketingIntegrationContractProbeSchedulerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService

source path:
  org.chovy.canvas.domain.monitoring.LlmMarketingMonitorInferenceGenerator -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/LlmMarketingMonitorInferenceGenerator.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  LlmMarketingMonitorInferenceGeneratorContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.llm.AiLlmGateway

source path:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertFanoutService.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  MarketingMonitorAlertFanoutServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.cdp.WebhookRetryPolicy

source path:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollingScheduler -> canvas-context-cdp

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingScheduler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-cdp with api/application facade, port, or event after migration

required tests:
  MarketingMonitorPollingSchedulerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService

source path:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialRefreshScheduler -> canvas-context-cdp

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialRefreshScheduler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-cdp with api/application facade, port, or event after migration

required tests:
  MarketingMonitorProviderCredentialRefreshSchedulerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService

source path:
  org.chovy.canvas.domain.notification.NotificationRealtimeService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationRealtimeService.java

target module:
  canvas-context-cdp

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-cdp must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  NotificationRealtimeServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.reactive.BackgroundSubscriptionRegistry

source path:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationService.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  ProgrammaticDspMutationServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer

source path:
  org.chovy.canvas.domain.programmatic.SandboxProgrammaticDspProviderWriteClient -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/SandboxProgrammaticDspProviderWriteClient.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SandboxProgrammaticDspProviderWriteClientContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport

source path:
  org.chovy.canvas.domain.search.GoogleAdsSearchMarketingProviderWriteClient -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/GoogleAdsSearchMarketingProviderWriteClient.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  GoogleAdsSearchMarketingProviderWriteClientContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer

source path:
  org.chovy.canvas.domain.search.MicrosoftAdsSearchMarketingProviderWriteClient -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/MicrosoftAdsSearchMarketingProviderWriteClient.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  MicrosoftAdsSearchMarketingProviderWriteClientContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer

source path:
  org.chovy.canvas.domain.search.SandboxSearchMarketingProviderReadClient -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SandboxSearchMarketingProviderReadClient.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SandboxSearchMarketingProviderReadClientContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer, org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport

source path:
  org.chovy.canvas.domain.search.SandboxSearchMarketingProviderWriteClient -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SandboxSearchMarketingProviderWriteClient.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SandboxSearchMarketingProviderWriteClientContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport

source path:
  org.chovy.canvas.domain.search.SearchMarketingCredentialRef -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingCredentialRef.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SearchMarketingCredentialRefContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer

source path:
  org.chovy.canvas.domain.search.SearchMarketingCredentialResolver -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingCredentialResolver.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SearchMarketingCredentialResolverContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer, org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport

source path:
  org.chovy.canvas.domain.search.SearchMarketingImpactWindowService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingImpactWindowService.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SearchMarketingImpactWindowServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer

source path:
  org.chovy.canvas.domain.search.SearchMarketingMutationService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationService.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SearchMarketingMutationServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer

source path:
  org.chovy.canvas.domain.search.SearchMarketingProviderSyncResult -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderSyncResult.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SearchMarketingProviderSyncResultContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer

source path:
  org.chovy.canvas.domain.search.SearchMarketingReadinessService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingReadinessService.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SearchMarketingReadinessServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer

source path:
  org.chovy.canvas.domain.search.SearchMarketingReconciliationService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingReconciliationService.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SearchMarketingReconciliationServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer

source path:
  org.chovy.canvas.domain.search.SearchMarketingSyncRunService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSyncRunService.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SearchMarketingSyncRunServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer

source path:
  org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService -> canvas-context-bi

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseFieldGovernanceService.java

target module:
  canvas-context-cdp

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-cdp must replace direct source coupling to canvas-context-bi with api/application facade, port, or event after migration

required tests:
  CdpWarehouseFieldGovernanceServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.bi.query.BiDatasetSpec, org.chovy.canvas.domain.bi.query.BiFilter, org.chovy.canvas.domain.bi.query.BiMetricSpec, org.chovy.canvas.domain.bi.query.BiQueryContext, org.chovy.canvas.domain.bi.query.BiQueryRequest, org.chovy.canvas.domain.bi.query.BiSort

source path:
  org.chovy.canvas.domain.warehouse.CdpWarehouseReadinessService -> canvas-context-bi

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessService.java

target module:
  canvas-context-cdp

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-cdp must replace direct source coupling to canvas-context-bi with api/application facade, port, or event after migration

required tests:
  CdpWarehouseReadinessServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.bi.query.BiDatasourceHealth, org.chovy.canvas.domain.bi.query.BiDatasourceHealthProvider

source path:
  org.chovy.canvas.engine.audience.AudienceComputeTaskRunner -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunner.java

target module:
  canvas-context-cdp

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-cdp must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  AudienceComputeTaskRunnerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.notification.NotificationService, org.chovy.canvas.domain.task.AsyncTaskService

source path:
  org.chovy.canvas.engine.delivery.ReachDeliveryService -> canvas-context-marketing

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java

target module:
  canvas-platform

target role:
  cross-context dependency review

coordinator decision:
  canvas-platform must replace direct source coupling to canvas-context-marketing with api/application facade, port, or event after migration

required tests:
  ReachDeliveryServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.channel.ChannelConnector, org.chovy.canvas.engine.channel.ChannelConnectorRegistry, org.chovy.canvas.engine.policy.MarketingPolicyService, org.chovy.canvas.engine.policy.MarketingPolicyService.PolicyDecision

source path:
  org.chovy.canvas.engine.disruptor.CanvasDisruptorService -> canvas-context-bi, canvas-context-execution, canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java

target module:
  canvas-context-canvas

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-canvas must replace direct source coupling to canvas-context-bi, canvas-context-execution, canvas-platform with api/application facade, port, or event after migration

required tests:
  CanvasDisruptorServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.lane.ExecutionLane, org.chovy.canvas.engine.lane.ExecutionLaneWorkerRegistry, org.chovy.canvas.engine.lifecycle.ExecutionLifecycleGate, org.chovy.canvas.engine.reactive.BackgroundSubscriptionRegistry, org.chovy.canvas.engine.request.CanvasExecutionRequestExecutor, org.chovy.canvas.engine.scheduler.CanvasMetrics, org.chovy.canvas.engine.trigger.CanvasExecutionService

source path:
  org.chovy.canvas.engine.handlers.AbstractSendMessageHandler -> canvas-context-marketing, canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-marketing, canvas-platform with api/application facade, port, or event after migration

required tests:
  AbstractSendMessageHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.content.MarketingContentReleaseService, org.chovy.canvas.engine.channel.ChannelConnector, org.chovy.canvas.engine.channel.ChannelConnectorRegistry, org.chovy.canvas.engine.channel.ChannelDedupeService, org.chovy.canvas.engine.channel.ChannelFallbackService, org.chovy.canvas.engine.channel.ProviderBackpressureService, org.chovy.canvas.engine.delivery.ReachDeliveryService

source path:
  org.chovy.canvas.engine.handlers.AiLlmHandler -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiLlmHandler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  AiLlmHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.llm.AiLlmGateway

source path:
  org.chovy.canvas.engine.handlers.ApiCallPayloadBuilder -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallPayloadBuilder.java

target module:
  canvas-platform

target role:
  cross-context dependency review

coordinator decision:
  canvas-platform must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  ApiCallPayloadBuilderContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.context.ExecutionContext

source path:
  org.chovy.canvas.engine.handlers.ConditionEvaluator -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ConditionEvaluator.java

target module:
  canvas-platform

target role:
  cross-context dependency review

coordinator decision:
  canvas-platform must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  ConditionEvaluatorContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.context.ExecutionContext

source path:
  org.chovy.canvas.engine.handlers.ConnectedContentHandler -> canvas-context-marketing

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ConnectedContentHandler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-marketing with api/application facade, port, or event after migration

required tests:
  ConnectedContentHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.canvas.ConnectedContentGateway

source path:
  org.chovy.canvas.engine.handlers.CouponHandler -> canvas-context-marketing, canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/CouponHandler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-marketing, canvas-platform with api/application facade, port, or event after migration

required tests:
  CouponHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.channel.ChannelDedupeService, org.chovy.canvas.engine.channel.ProviderBackpressureService

source path:
  org.chovy.canvas.engine.handlers.GroovyHandler -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  GroovyHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.expression.ExpressionEngine, org.chovy.canvas.engine.expression.GroovyExpressionEngine

source path:
  org.chovy.canvas.engine.handlers.ManualApprovalHandler -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ManualApprovalHandler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  ManualApprovalHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.approval.ApprovalInstanceView, org.chovy.canvas.domain.approval.ApprovalSubmitCommand, org.chovy.canvas.domain.approval.ApprovalWorkflowService

source path:
  org.chovy.canvas.engine.handlers.PointsOperationHandler -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/PointsOperationHandler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  PointsOperationHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.cdp.CustomerPointsLedgerService

source path:
  org.chovy.canvas.engine.handlers.RiskDecisionHandler -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/RiskDecisionHandler.java

target module:
  canvas-context-risk

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-risk must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  RiskDecisionHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.context.ExecutionContext, org.chovy.canvas.engine.handler.NodeHandler, org.chovy.canvas.engine.handler.NodeHandlerType, org.chovy.canvas.engine.handler.NodeResult

source path:
  org.chovy.canvas.engine.handlers.SendMessageHandler -> canvas-context-marketing, canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMessageHandler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-marketing, canvas-platform with api/application facade, port, or event after migration

required tests:
  SendMessageHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.content.MarketingContentReleaseService, org.chovy.canvas.engine.channel.ChannelConnectorRegistry, org.chovy.canvas.engine.channel.ChannelDedupeService, org.chovy.canvas.engine.channel.ChannelFallbackService, org.chovy.canvas.engine.channel.ProviderBackpressureService, org.chovy.canvas.engine.delivery.ReachDeliveryService

source path:
  org.chovy.canvas.engine.handlers.SubFlowRefHandler -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SubFlowRefHandler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  SubFlowRefHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.canvas.SubFlowLookupService

source path:
  org.chovy.canvas.engine.handlers.TaggerHandler -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TaggerHandler.java

target module:
  canvas-context-cdp

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-cdp must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  TaggerHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.context.ExecutionContext, org.chovy.canvas.engine.handler.NodeHandler, org.chovy.canvas.engine.handler.NodeHandlerType, org.chovy.canvas.engine.handler.NodeResult, org.chovy.canvas.engine.trigger.CanvasExecutionService, org.chovy.canvas.engine.trigger.CanvasSchedulerService

source path:
  org.chovy.canvas.engine.handlers.UserInputHandler -> canvas-context-canvas

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/UserInputHandler.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-canvas with api/application facade, port, or event after migration

required tests:
  UserInputHandlerContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.canvas.UserInputService

source path:
  org.chovy.canvas.engine.insights.MauticInspiredInsightService -> canvas-context-cdp

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/insights/MauticInspiredInsightService.java

target module:
  canvas-context-marketing

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-marketing must replace direct source coupling to canvas-context-cdp with api/application facade, port, or event after migration

required tests:
  MauticInspiredInsightServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.audience.AudienceBitmapStore

source path:
  org.chovy.canvas.engine.request.CanvasExecutionRequestBacklogMetrics -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java

target module:
  canvas-context-bi

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-bi must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  CanvasExecutionRequestBacklogMetricsContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.execution.CanvasExecutionRequestStatusCount, org.chovy.canvas.domain.execution.CanvasExecutionRequestStatusCount

source path:
  org.chovy.canvas.engine.request.CanvasExecutionRequestDispatcher -> canvas-context-bi, canvas-context-canvas

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestDispatcher.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-bi, canvas-context-canvas with api/application facade, port, or event after migration

required tests:
  CanvasExecutionRequestDispatcherContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.disruptor.CanvasDisruptorService, org.chovy.canvas.engine.scheduler.CanvasMetrics

source path:
  org.chovy.canvas.engine.request.CanvasExecutionRequestExecutor -> canvas-context-bi

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutor.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-bi with api/application facade, port, or event after migration

required tests:
  CanvasExecutionRequestExecutorContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.scheduler.CanvasMetrics

source path:
  org.chovy.canvas.engine.rule.CanvasRuleGraphValidator -> canvas-context-execution

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/CanvasRuleGraphValidator.java

target module:
  canvas-context-canvas

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-canvas must replace direct source coupling to canvas-context-execution with api/application facade, port, or event after migration

required tests:
  CanvasRuleGraphValidatorContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.dag.DagGraph, org.chovy.canvas.engine.dag.DagParser

source path:
  org.chovy.canvas.engine.trigger.CanvasExecutionService -> canvas-context-canvas, canvas-context-cdp

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-canvas, canvas-context-cdp with api/application facade, port, or event after migration

required tests:
  CanvasExecutionServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.cdp.CdpUserService, org.chovy.canvas.engine.disruptor.CanvasDisruptorService, org.chovy.canvas.engine.rule.CanvasRuleGraphValidator

source path:
  org.chovy.canvas.engine.trigger.CanvasSchedulerService -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  CanvasSchedulerServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.schedule.ScheduleKey, org.chovy.canvas.engine.schedule.ScheduleRegistrar, org.chovy.canvas.engine.schedule.ScheduleRegistration

source path:
  org.chovy.canvas.engine.trigger.ExecutionWatchdog -> canvas-platform

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ExecutionWatchdog.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-platform with api/application facade, port, or event after migration

required tests:
  ExecutionWatchdogContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.approval.ApprovalWorkflowService

source path:
  org.chovy.canvas.engine.trigger.InFlightExecutionRegistry -> canvas-context-bi

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-bi with api/application facade, port, or event after migration

required tests:
  InFlightExecutionRegistryContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.engine.scheduler.CanvasMetrics

source path:
  org.chovy.canvas.engine.trigger.TriggerPreCheckService -> canvas-context-canvas

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java

target module:
  canvas-context-execution

target role:
  cross-context dependency review

coordinator decision:
  canvas-context-execution must replace direct source coupling to canvas-context-canvas with api/application facade, port, or event after migration

required tests:
  TriggerPreCheckServiceContractTest

compatibility notes:
  review imported classes: org.chovy.canvas.domain.canvas.CanvasControlGroupService
