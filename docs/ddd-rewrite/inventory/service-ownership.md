# Service Ownership Inventory

Exact domain, engine, platform, architecture, and architecture strategy service ownership rows.

Generated on 2026-06-09 from the current `backend/canvas-engine` source tree and `context-ownership-seed.md`.
old class:
  org.chovy.canvas.domain.ai.AiDecisionFeedbackCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiDecisionFeedbackCommand.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps AiDecisionFeedbackCommand assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionFeedbackCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.AiDecisionFeedbackView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiDecisionFeedbackView.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps AiDecisionFeedbackView assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionFeedbackViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.AiDecisionModelService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiDecisionModelService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps AiDecisionModelService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionModelServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.AiDecisionRecommendationQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiDecisionRecommendationQuery.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps AiDecisionRecommendationQuery assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionRecommendationQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.AiDecisionRecommendationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiDecisionRecommendationView.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps AiDecisionRecommendationView assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionRecommendationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.AiDecisionRecomputeCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiDecisionRecomputeCommand.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps AiDecisionRecomputeCommand assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionRecomputeCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.AiDecisionRunView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiDecisionRunView.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps AiDecisionRunView assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionRunViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.AiPredictionProperties

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiPredictionProperties.java

target module:
  canvas-platform

target role:
  config

coordinator decision:
  coordinator keeps AiPredictionProperties assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiPredictionPropertiesTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.AiPromptEvaluationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiPromptEvaluationService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps AiPromptEvaluationService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiPromptEvaluationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.AiPromptTemplateService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiPromptTemplateService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps AiPromptTemplateService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiPromptTemplateServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.AiProviderModelRegistryService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiProviderModelRegistryService.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps AiProviderModelRegistryService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiProviderModelRegistryServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.ChurnFeatureSnapshotService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/ChurnFeatureSnapshotService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  ChurnFeatureSnapshotServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.ChurnPredictionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/ChurnPredictionService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  ChurnPredictionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.PredictionProfileWriter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/PredictionProfileWriter.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  PredictionProfileWriterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ai.SmartTimingService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/SmartTimingService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  SmartTimingServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.AnalyticsQueryGuard

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsQueryGuard.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps AnalyticsQueryGuard assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsQueryGuardTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.AnalyticsQueryService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsQueryService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps AnalyticsQueryService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsQueryServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.AudienceMaterializationOperationsService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationOperationsService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  AudienceMaterializationOperationsServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.AudienceMaterializationScheduleService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationScheduleService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  AudienceMaterializationScheduleServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.AudienceMaterializationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  AudienceMaterializationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.AudienceQualityService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceQualityService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  AudienceQualityServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.BehaviorAudienceRuleCompiler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/BehaviorAudienceRuleCompiler.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  BehaviorAudienceRuleCompilerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.CdpWarehouseAudienceMaterializationScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/CdpWarehouseAudienceMaterializationScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAudienceMaterializationSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.MyBatisAudienceDefinitionRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/MyBatisAudienceDefinitionRepository.java

target module:
  canvas-context-cdp

target role:
  adapter.persistence

owning worker:
  DDD-W04

required tests:
  MyBatisAudienceDefinitionRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.MySqlTraceEventSink

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/MySqlTraceEventSink.java

target module:
  canvas-context-execution

target role:
  adapter.messaging

owning worker:
  DDD-W08

required tests:
  MySqlTraceEventSinkTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.RetentionPolicyService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/RetentionPolicyService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  RetentionPolicyServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.analytics.TraceEventSink

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/TraceEventSink.java

target module:
  canvas-context-execution

target role:
  adapter.messaging

owning worker:
  DDD-W08

required tests:
  TraceEventSinkTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalAutoActionHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalAutoActionHandler.java

target module:
  canvas-context-execution

target role:
  domain service

coordinator decision:
  coordinator keeps ApprovalAutoActionHandler assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  ApprovalAutoActionHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalDecisionCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalDecisionCommand.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps ApprovalDecisionCommand assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalDecisionCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalDecisionRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalDecisionRequest.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps ApprovalDecisionRequest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalDecisionRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalExternalProvider

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalExternalProvider.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps ApprovalExternalProvider assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalExternalProviderTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalExternalSubmissionResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalExternalSubmissionResult.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps ApprovalExternalSubmissionResult assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalExternalSubmissionResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalExternalSyncResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalExternalSyncResult.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps ApprovalExternalSyncResult assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalExternalSyncResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalInstanceView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalInstanceView.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps ApprovalInstanceView assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalInstanceViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalLarkUserIdentity

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalLarkUserIdentity.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps ApprovalLarkUserIdentity assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalLarkUserIdentityTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalLarkUserIdentityResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalLarkUserIdentityResolver.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps ApprovalLarkUserIdentityResolver assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalLarkUserIdentityResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalSubmitCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalSubmitCommand.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps ApprovalSubmitCommand assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalSubmitCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalTaskView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalTaskView.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps ApprovalTaskView assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalTaskViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.ApprovalWorkflowService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalWorkflowService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps ApprovalWorkflowService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalWorkflowServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.CanvasPublishApprovalAutoActionHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/CanvasPublishApprovalAutoActionHandler.java

target module:
  canvas-context-execution

target role:
  domain service

coordinator decision:
  coordinator keeps CanvasPublishApprovalAutoActionHandler assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  CanvasPublishApprovalAutoActionHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.CanvasPublishApprovalRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/CanvasPublishApprovalRequest.java

target module:
  canvas-context-canvas

target role:
  domain model or service

coordinator decision:
  coordinator keeps CanvasPublishApprovalRequest assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasPublishApprovalRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.CanvasPublishApprovalService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/CanvasPublishApprovalService.java

target module:
  canvas-context-canvas

target role:
  application service

coordinator decision:
  coordinator keeps CanvasPublishApprovalService assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasPublishApprovalServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.CanvasPublishApprovalStatusView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/CanvasPublishApprovalStatusView.java

target module:
  canvas-context-canvas

target role:
  domain model or service

coordinator decision:
  coordinator keeps CanvasPublishApprovalStatusView assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasPublishApprovalStatusViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.HttpLarkApprovalClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/HttpLarkApprovalClient.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps HttpLarkApprovalClient assigned to canvas-platform until the owning task pack accepts the row

required tests:
  HttpLarkApprovalClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.LarkApprovalClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/LarkApprovalClient.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps LarkApprovalClient assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LarkApprovalClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.LarkApprovalCreateInstanceRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/LarkApprovalCreateInstanceRequest.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps LarkApprovalCreateInstanceRequest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LarkApprovalCreateInstanceRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.LarkApprovalInstanceSnapshot

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/LarkApprovalInstanceSnapshot.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps LarkApprovalInstanceSnapshot assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LarkApprovalInstanceSnapshotTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.LarkApprovalProvider

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/LarkApprovalProvider.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps LarkApprovalProvider assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LarkApprovalProviderTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.LarkApprovalSyncScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/LarkApprovalSyncScheduler.java

target module:
  canvas-context-execution

target role:
  application service

coordinator decision:
  coordinator keeps LarkApprovalSyncScheduler assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  LarkApprovalSyncSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.LarkApprovalTaskActionRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/LarkApprovalTaskActionRequest.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps LarkApprovalTaskActionRequest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LarkApprovalTaskActionRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.LarkApprovalTaskSnapshot

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/LarkApprovalTaskSnapshot.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps LarkApprovalTaskSnapshot assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LarkApprovalTaskSnapshotTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.approval.RuntimeManualApprovalAutoActionHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/RuntimeManualApprovalAutoActionHandler.java

target module:
  canvas-context-execution

target role:
  domain service

coordinator decision:
  coordinator keeps RuntimeManualApprovalAutoActionHandler assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  RuntimeManualApprovalAutoActionHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiAiSemanticValidator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiAiSemanticValidator.java

target module:
  canvas-context-bi

target role:
  domain model or service

coordinator decision:
  coordinator keeps BiAiSemanticValidator assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiAiSemanticValidatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiAskDataAgentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiAskDataAgentService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiAskDataAgentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiAskDataPlan

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiAskDataPlan.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiAskDataPlanTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiAskDataPlanner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiAskDataPlanner.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiAskDataPlannerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiAskDataPlanningContext

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiAskDataPlanningContext.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiAskDataPlanningContextTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiAskDataPlanningResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiAskDataPlanningResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiAskDataPlanningResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiAskDataRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiAskDataRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiAskDataRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiAskDataResponse

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiAskDataResponse.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiAskDataResponseTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiDashboardDraftAgentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiDashboardDraftAgentService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDashboardDraftAgentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiDashboardDraftPlan

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiDashboardDraftPlan.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardDraftPlanTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiDashboardDraftPlanner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiDashboardDraftPlanner.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardDraftPlannerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiDashboardDraftPlanningContext

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiDashboardDraftPlanningContext.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardDraftPlanningContextTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiDashboardDraftPlanningResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiDashboardDraftPlanningResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardDraftPlanningResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiDashboardDraftRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiDashboardDraftRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardDraftRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiDashboardDraftResponse

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiDashboardDraftResponse.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardDraftResponseTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInsightAgentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInsightAgentService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiInsightAgentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInsightPlan

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInsightPlan.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInsightPlanTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInsightPlanner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInsightPlanner.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInsightPlannerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInsightPlanningContext

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInsightPlanningContext.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInsightPlanningContextTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInsightPlanningResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInsightPlanningResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInsightPlanningResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInsightRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInsightRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInsightRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInsightResponse

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInsightResponse.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInsightResponseTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInterpretationAgentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInterpretationAgentService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiInterpretationAgentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInterpretationPlan

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInterpretationPlan.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInterpretationPlanTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInterpretationPlanner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInterpretationPlanner.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInterpretationPlannerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInterpretationPlanningContext

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInterpretationPlanningContext.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInterpretationPlanningContextTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInterpretationPlanningResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInterpretationPlanningResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInterpretationPlanningResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInterpretationRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInterpretationRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInterpretationRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiInterpretationResponse

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiInterpretationResponse.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiInterpretationResponseTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiReportAgentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiReportAgentService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiReportAgentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiReportPlan

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiReportPlan.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiReportPlanTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiReportPlanner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiReportPlanner.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiReportPlannerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiReportPlanningContext

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiReportPlanningContext.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiReportPlanningContextTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiReportPlanningResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiReportPlanningResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiReportPlanningResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiReportRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiReportRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiReportRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiReportResponse

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiReportResponse.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiReportResponseTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiReportSection

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiReportSection.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiReportSectionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.BiReportSectionInput

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/BiReportSectionInput.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiReportSectionInputTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.LlmBiAskDataPlanner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/LlmBiAskDataPlanner.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  LlmBiAskDataPlannerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.LlmBiDashboardDraftPlanner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/LlmBiDashboardDraftPlanner.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  LlmBiDashboardDraftPlannerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.LlmBiInsightPlanner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/LlmBiInsightPlanner.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  LlmBiInsightPlannerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.LlmBiInterpretationPlanner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/LlmBiInterpretationPlanner.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  LlmBiInterpretationPlannerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.ai.LlmBiReportPlanner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/LlmBiReportPlanner.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  LlmBiReportPlannerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.bigscreen.BiBigScreenResource

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/bigscreen/BiBigScreenResource.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiBigScreenResourceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.bigscreen.BiBigScreenResourceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/bigscreen/BiBigScreenResourceService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiBigScreenResourceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.bigscreen.BiBigScreenVersionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/bigscreen/BiBigScreenVersionView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiBigScreenVersionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.chart.BiChartDashboardReference

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartDashboardReference.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiChartDashboardReferenceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.chart.BiChartPortalReference

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartPortalReference.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiChartPortalReferenceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.chart.BiChartReferenceImpact

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartReferenceImpact.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiChartReferenceImpactTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.chart.BiChartReferenceImpactService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartReferenceImpactService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiChartReferenceImpactServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.chart.BiChartResource

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartResource.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiChartResourceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.chart.BiChartResourceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartResourceService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiChartResourceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.chart.BiChartSubscriptionReference

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartSubscriptionReference.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiChartSubscriptionReferenceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.chart.BiChartVersionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/BiChartVersionView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiChartVersionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardCloneCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardCloneCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardCloneCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardExportPackage

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardExportPackage.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardExportPackageTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardFilter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardFilter.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardFilterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardFilterCascade

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardFilterCascade.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardFilterCascadeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardImportCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardImportCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardImportCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardInteraction

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardInteraction.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardInteractionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardPreset.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardPresetTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardResource

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResource.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardResourceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDashboardResourceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardRuntimeStateCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardRuntimeStateCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardRuntimeStateService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDashboardRuntimeStateServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardRuntimeStateView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardRuntimeStateViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardVersionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardVersionView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardVersionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardWidget

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardWidget.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDashboardWidgetTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dashboard.MarketingBiDashboardPresetRegistry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/MarketingBiDashboardPresetRegistry.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  MarketingBiDashboardPresetRegistryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetAccelerationPolicyCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetAccelerationPolicyCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetAccelerationPolicyView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetAccelerationPolicyViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationSchedulerItem

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetAccelerationSchedulerItem.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetAccelerationSchedulerItemTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationSchedulerResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetAccelerationSchedulerResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetAccelerationSchedulerResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationSchedulerService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetAccelerationSchedulerService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDatasetAccelerationSchedulerServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetAccelerationService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDatasetAccelerationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetDraftNormalization

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetDraftNormalization.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetDraftNormalizationTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetExtractCapacitySummaryView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetExtractCapacitySummaryView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetExtractCapacitySummaryViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetExtractCleanupResultView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetExtractCleanupResultView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetExtractCleanupResultViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetExtractMaterializationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetExtractMaterializationResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetExtractMaterializationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetExtractMaterializer

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetExtractMaterializer.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetExtractMaterializerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetExtractRefreshRunView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetExtractRefreshRunView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetExtractRefreshRunViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetFieldResource

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFieldResource.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetFieldResourceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetFromDatasourceCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceGraphCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceGraphCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetFromDatasourceGraphCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceGraphNodeCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceGraphNodeCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetFromDatasourceGraphNodeCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceJoinCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceJoinCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetFromDatasourceJoinCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceJoinConditionCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceJoinConditionCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetFromDatasourceJoinConditionCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceMultiTableCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceMultiTableCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetFromDatasourceMultiTableCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDatasetFromDatasourceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceTableCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceTableCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetFromDatasourceTableCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetResource

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResource.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetResourceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetResourceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDatasetResourceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetVersionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiDatasetVersionView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetVersionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiMetricResource

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiMetricResource.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiMetricResourceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineAdmissionDecision

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineAdmissionDecision.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineAdmissionDecisionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityAlertPolicyCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineCapacityAlertPolicyCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineCapacityAlertPolicyCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityAlertPolicyView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineCapacityAlertPolicyView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineCapacityAlertPolicyViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityCategoryUsageView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineCapacityCategoryUsageView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineCapacityCategoryUsageViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineCapacityService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineCapacityServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacitySummaryView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineCapacitySummaryView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineCapacitySummaryViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityUsageDetailView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineCapacityUsageDetailView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineCapacityUsageDetailViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityUserUsageView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineCapacityUserUsageView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineCapacityUserUsageViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineConcurrencyQueueView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineConcurrencyQueueView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineConcurrencyQueueViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueAdmissionCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueAdmissionCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueAdmissionCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueBacklogView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueBacklogView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueBacklogViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueClaimResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueClaimResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueClaimResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueJobView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueJobView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueJobViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueRecoveryResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueRecoveryResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueRecoveryResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueSchedulerResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueSchedulerResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueSchedulerResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueSchedulerService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueSchedulerService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueSchedulerServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueSnapshotView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueSnapshotView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueSnapshotViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueStatusCount

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueStatusCount.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueStatusCountTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineTenantPoolPolicyCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineTenantPoolPolicyCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

coordinator decision:
  coordinator keeps BiQuickEngineTenantPoolPolicyCommand assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiQuickEngineTenantPoolPolicyCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineTenantPoolPolicyView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineTenantPoolPolicyView.java

target module:
  canvas-context-bi

target role:
  domain model or service

coordinator decision:
  coordinator keeps BiQuickEngineTenantPoolPolicyView assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiQuickEngineTenantPoolPolicyViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiSqlDatasetImpactView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiSqlDatasetImpactView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSqlDatasetImpactViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiSqlDatasetLineageView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiSqlDatasetLineageView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSqlDatasetLineageViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiSqlDatasetPreviewCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSqlDatasetPreviewCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiSqlDatasetPreviewResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSqlDatasetPreviewResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/BiSqlDatasetPreviewService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiSqlDatasetPreviewServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceApiPreview

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceApiPreview.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceApiPreviewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceApiPreviewRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceApiPreviewRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceApiPreviewRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceColumnPreview

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceColumnPreview.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceColumnPreviewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceConnectionTestResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceConnectionTestResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceConnectionTestResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceConnectorCapability

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceConnectorCapability.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceConnectorCapabilityTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceCredentialRotationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceCredentialRotationCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceCredentialRotationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceCredentialRotationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceCredentialRotationView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceCredentialRotationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceFileMaterializationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceFileMaterializationCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceFileMaterializationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceFileMaterializationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceFileMaterializationResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceFileMaterializationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceFileMaterializationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceFileMaterializationService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDatasourceFileMaterializationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceFileUploadCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceFileUploadCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceFileUploadCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceFileUploadService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceFileUploadService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDatasourceFileUploadServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceOnboardingCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceOnboardingCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceOnboardingService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDatasourceOnboardingServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceOnboardingView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceOnboardingViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceRuntimeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceRuntimeService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDatasourceRuntimeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceSchemaPreview

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceSchemaPreview.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceSchemaPreviewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceSchemaSnapshotView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceSchemaSnapshotView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceSchemaSnapshotViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceTablePreview

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceTablePreview.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceTablePreviewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.embed.BiEmbedTicket

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicket.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiEmbedTicketTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.embed.BiEmbedTicketPayload

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketPayload.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiEmbedTicketPayloadTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.embed.BiEmbedTicketRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiEmbedTicketRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.embed.BiEmbedTicketService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiEmbedTicketServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.embed.BiEmbedTicketVerifyRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketVerifyRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiEmbedTicketVerifyRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.embed.BiEmbedTokenCleanupResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/BiEmbedTokenCleanupResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiEmbedTokenCleanupResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.export.BiExportApprovalReviewCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportApprovalReviewCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

coordinator decision:
  coordinator keeps BiExportApprovalReviewCommand assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiExportApprovalReviewCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.export.BiExportCleanupResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportCleanupResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiExportCleanupResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.export.BiExportDownload

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportDownload.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiExportDownloadTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.export.BiExportJobCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportJobCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiExportJobCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.export.BiExportJobDetailView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportJobDetailView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiExportJobDetailViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.export.BiExportJobView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportJobView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiExportJobViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.export.BiExportObjectRestoreResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportObjectRestoreResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiExportObjectRestoreResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.export.BiExportQueueResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportQueueResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiExportQueueResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.export.BiExportRetryResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiExportRetryResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiExportRetryResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.export.BiSelfServiceExportService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiSelfServiceExportServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.export.BiSelfServicePreviewRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/BiSelfServicePreviewRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSelfServicePreviewRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiColumnPermissionCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiColumnPermissionCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiColumnPermissionCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiColumnPermissionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiColumnPermissionView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiColumnPermissionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiPermissionAdminService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionAdminService.java

target module:
  canvas-context-bi

target role:
  application service

coordinator decision:
  coordinator keeps BiPermissionAdminService assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiPermissionAdminServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiPermissionAuditEntry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionAuditEntry.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiPermissionAuditEntryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiPermissionRequestCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionRequestCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiPermissionRequestCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiPermissionRequestReviewCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionRequestReviewCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiPermissionRequestReviewCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiPermissionRequestService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionRequestService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiPermissionRequestServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiPermissionRequestView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionRequestView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiPermissionRequestViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiPermissionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiPermissionService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiPermissionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiResourcePermissionCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiResourcePermissionCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourcePermissionCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiResourcePermissionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiResourcePermissionView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourcePermissionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiRowPermissionCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiRowPermissionCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiRowPermissionCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.permission.BiRowPermissionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/BiRowPermissionView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiRowPermissionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.portal.BiPortalMenuResource

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/BiPortalMenuResource.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiPortalMenuResourceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.portal.BiPortalResource

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/BiPortalResource.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiPortalResourceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.portal.BiPortalResourceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiPortalResourceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.portal.BiPortalRuntimeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/BiPortalRuntimeService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiPortalRuntimeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.portal.BiPortalVersionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/portal/BiPortalVersionView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiPortalVersionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiCompiledQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiCompiledQuery.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiCompiledQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiDatasetSpec

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasetSpec.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetSpecTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasetSpecResolver.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasetSpecResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiDatasourceHealth

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasourceHealth.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceHealthTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiDatasourceHealthProvider

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasourceHealthProvider.java

target module:
  canvas-context-bi

target role:
  adapter.external

owning worker:
  DDD-W05

required tests:
  BiDatasourceHealthProviderTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiDatasourceHealthSloSummary

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasourceHealthSloSummary.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceHealthSloSummaryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiDatasourceHealthSnapshot

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiDatasourceHealthSnapshot.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDatasourceHealthSnapshotTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiFieldSpec

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiFieldSpec.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiFieldSpecTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiFilter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiFilter.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiFilterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiMetricSpec

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiMetricSpec.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiMetricSpecTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryCacheInvalidationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCacheInvalidationCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryCacheInvalidationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryCacheInvalidationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCacheInvalidationResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryCacheInvalidationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryCachePolicy

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCachePolicy.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryCachePolicyTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryCachePolicyService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCachePolicyService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiQueryCachePolicyServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryCachePolicyUpdateCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCachePolicyUpdateCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryCachePolicyUpdateCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryCachePolicyView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCachePolicyView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryCachePolicyViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryCacheStats

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCacheStats.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryCacheStatsTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryCancellationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCancellationResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryCancellationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryColumn

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryColumn.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryColumnTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryCompiler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryCompiler.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryCompilerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryContext

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryContext.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryContextTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryExecutionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiQueryExecutionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryExecutor

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExecutor.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryExecutorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryExplanation

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryExplanation.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryExplanationTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryGovernanceAuditEntry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryGovernanceAuditEntry.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryGovernanceAuditEntryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryGovernanceConfiguration

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryGovernanceConfiguration.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryGovernanceConfigurationTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicy

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryGovernancePolicy.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryGovernancePolicyTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicyService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryGovernancePolicyService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiQueryGovernancePolicyServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicyUpdateCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryGovernancePolicyUpdateCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryGovernancePolicyUpdateCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicyView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryGovernancePolicyView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryGovernancePolicyViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryGovernanceSummary

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryGovernanceSummary.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryGovernanceSummaryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryHistoryDetail

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryHistoryDetail.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryHistoryDetailTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryHistoryEntry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryHistoryEntry.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryHistoryEntryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryHistoryItem

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryHistoryItem.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryHistoryItemTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryHistoryReader

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryHistoryReader.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryHistoryReaderTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryHistoryRecorder

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryHistoryRecorder.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryHistoryRecorderTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiQueryResultCache

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiQueryResultCache.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiQueryResultCacheTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiSort

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiSort.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSortTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.BiSqlParameterSpec

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/BiSqlParameterSpec.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSqlParameterSpecTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.query.MarketingBiDatasetRegistry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/MarketingBiDatasetRegistry.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  MarketingBiDatasetRegistryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiPublishApprovalRequestCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalRequestCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

coordinator decision:
  coordinator keeps BiPublishApprovalRequestCommand assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiPublishApprovalRequestCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiPublishApprovalReviewCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalReviewCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

coordinator decision:
  coordinator keeps BiPublishApprovalReviewCommand assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiPublishApprovalReviewCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiPublishApprovalService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalService.java

target module:
  canvas-context-bi

target role:
  application service

coordinator decision:
  coordinator keeps BiPublishApprovalService assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiPublishApprovalServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiPublishApprovalView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalView.java

target module:
  canvas-context-bi

target role:
  domain model or service

coordinator decision:
  coordinator keeps BiPublishApprovalView assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiPublishApprovalViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceCollaborationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceCollaborationService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiResourceCollaborationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceCommentCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceCommentCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourceCommentCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceCommentView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceCommentView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourceCommentViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceFavoriteCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceFavoriteCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourceFavoriteCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceFavoriteService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceFavoriteService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiResourceFavoriteServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceFavoriteView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceFavoriteView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourceFavoriteViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceLocationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceLocationView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourceLocationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceLockCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceLockCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourceLockCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceLockView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceLockView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourceLockViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceMoveCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceMoveCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourceMoveCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceMovementService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceMovementService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiResourceMovementServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceOwnershipView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceOwnershipView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourceOwnershipViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourcePermissionGuard

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourcePermissionGuard.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourcePermissionGuardTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceTransferCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceTransferCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiResourceTransferCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceTransferService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiResourceTransferService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiResourceTransferServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.spreadsheet.BiSpreadsheetResource

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/spreadsheet/BiSpreadsheetResource.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSpreadsheetResourceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.spreadsheet.BiSpreadsheetResourceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/spreadsheet/BiSpreadsheetResourceService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiSpreadsheetResourceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.spreadsheet.BiSpreadsheetVersionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/spreadsheet/BiSpreadsheetVersionView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSpreadsheetVersionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.storage.BiFileStorage

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/BiFileStorage.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiFileStorageTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.storage.BiFileStorageConfiguration

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/BiFileStorageConfiguration.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiFileStorageConfigurationTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.storage.BiFileStorageWriter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/BiFileStorageWriter.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiFileStorageWriterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.storage.BiStoredFile

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/BiStoredFile.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiStoredFileTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.storage.HttpS3ObjectClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/HttpS3ObjectClient.java

target module:
  canvas-context-bi

target role:
  adapter.external

owning worker:
  DDD-W05

required tests:
  HttpS3ObjectClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.storage.LocalBiFileStorage

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/LocalBiFileStorage.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  LocalBiFileStorageTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.storage.S3BucketLifecycleRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/S3BucketLifecycleRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  S3BucketLifecycleRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.storage.S3CompatibleBiFileStorage

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/S3CompatibleBiFileStorage.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  S3CompatibleBiFileStorageTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.storage.S3CompatibleBiStorageProperties

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/S3CompatibleBiStorageProperties.java

target module:
  canvas-context-bi

target role:
  config

owning worker:
  DDD-W05

required tests:
  S3CompatibleBiStoragePropertiesTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.storage.S3ObjectClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/S3ObjectClient.java

target module:
  canvas-context-bi

target role:
  adapter.external

owning worker:
  DDD-W05

required tests:
  S3ObjectClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.storage.S3ObjectRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/storage/S3ObjectRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  S3ObjectRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiAlertRuleCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiAlertRuleCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiAlertRuleCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiAlertRuleView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiAlertRuleView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiAlertRuleViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryAdapterRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDeliveryAdapterRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryAdapterResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDeliveryAdapterResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryAdapterService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDeliveryAdapterServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentCleanupResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentCleanupResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDeliveryAttachmentCleanupResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentDownload

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentDownload.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDeliveryAttachmentDownloadTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDeliveryAttachmentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDeliveryAttachmentViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryAuditSummary

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAuditSummary.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDeliveryAuditSummaryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryLogView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryLogView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDeliveryLogViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryResourceUrls

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryResourceUrls.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDeliveryResourceUrlsTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryRetryResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRetryResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDeliveryRetryResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryRunResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRunResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDeliveryRunResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryRuntimeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDeliveryRuntimeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerLeaseService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerLeaseService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDeliverySchedulerLeaseServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiDeliverySchedulerResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  BiDeliverySchedulerServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiEmailAttachment

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiEmailAttachment.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiEmailAttachmentTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiEmailDeliveryClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiEmailDeliveryClient.java

target module:
  canvas-context-bi

target role:
  adapter.external

owning worker:
  DDD-W05

required tests:
  BiEmailDeliveryClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiEmailDeliveryRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiEmailDeliveryRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiEmailDeliveryRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiSmtpEmailDeliveryClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSmtpEmailDeliveryClient.java

target module:
  canvas-context-bi

target role:
  adapter.external

owning worker:
  DDD-W05

required tests:
  BiSmtpEmailDeliveryClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiSnapshotRenderRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSnapshotRenderRequest.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSnapshotRenderRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiSnapshotRenderResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSnapshotRenderResult.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSnapshotRenderResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiSnapshotRenderer

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSnapshotRenderer.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSnapshotRendererTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiSubscriptionAdminService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionAdminService.java

target module:
  canvas-context-bi

target role:
  application service

coordinator decision:
  coordinator keeps BiSubscriptionAdminService assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiSubscriptionAdminServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiSubscriptionCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionCommand.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSubscriptionCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.BiSubscriptionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionView.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  BiSubscriptionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.bi.subscription.HttpBiSnapshotRenderer

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/HttpBiSnapshotRenderer.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  HttpBiSnapshotRendererTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasAttributionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasAttributionService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasAttributionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasControlGroupService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasControlGroupService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasControlGroupServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasExampleSeeder

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasExampleSeeder.java

target module:
  canvas-context-canvas

target role:
  domain model or service

owning worker:
  DDD-W07

required tests:
  CanvasExampleSeederTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasExamplesProperties

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasExamplesProperties.java

target module:
  canvas-context-canvas

target role:
  config

owning worker:
  DDD-W07

required tests:
  CanvasExamplesPropertiesTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasImportExportService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasImportExportService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasImportExportServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasListQuerySupport

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasListQuerySupport.java

target module:
  canvas-context-canvas

target role:
  domain model or service

owning worker:
  DDD-W07

required tests:
  CanvasListQuerySupportTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasMessagePreviewService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasMessagePreviewService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasMessagePreviewServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasOpsService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java

target module:
  canvas-context-canvas

target role:
  application service

coordinator decision:
  coordinator keeps CanvasOpsService assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasOpsServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasPrePublishCheckService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasPrePublishCheckService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasPrePublishCheckServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasProjectFolderMetadataService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasProjectFolderMetadataService.java

target module:
  canvas-context-canvas

target role:
  application service

coordinator decision:
  coordinator keeps CanvasProjectFolderMetadataService assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasProjectFolderMetadataServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasStateTransitionPolicy

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasStateTransitionPolicy.java

target module:
  canvas-context-canvas

target role:
  domain model or service

owning worker:
  DDD-W07

required tests:
  CanvasStateTransitionPolicyTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasTransactionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasTransactionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.CanvasVersionCleanupJob

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJob.java

target module:
  canvas-context-canvas

target role:
  domain model or service

owning worker:
  DDD-W07

required tests:
  CanvasVersionCleanupJobTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.ConnectedContentGateway

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/ConnectedContentGateway.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  ConnectedContentGatewayTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.ConnectedContentGatewayService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/ConnectedContentGatewayService.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  ConnectedContentGatewayServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.SubFlowLookupService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/SubFlowLookupService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  SubFlowLookupServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.TestUserRerunService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/TestUserRerunService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  TestUserRerunServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.canvas.UserInputService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/UserInputService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  UserInputServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CanvasUserQueryService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CanvasUserQueryService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasUserQueryServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CdpEventIngestionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java

target module:
  canvas-context-cdp

target role:
  adapter.messaging

owning worker:
  DDD-W04

required tests:
  CdpEventIngestionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CdpEventPublisher

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventPublisher.java

target module:
  canvas-context-cdp

target role:
  adapter.messaging

owning worker:
  DDD-W04

required tests:
  CdpEventPublisherTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CdpLineageService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpLineageService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpLineageServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CdpRuleEvaluator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpRuleEvaluator.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  CdpRuleEvaluatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CdpTagOperationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagOperationService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpTagOperationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CdpTagService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpTagServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CdpUserDirectoryService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserDirectoryService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpUserDirectoryServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CdpUserInsightService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserInsightService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpUserInsightServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CdpUserService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpUserServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CdpWriteKeyAuthService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthService.java

target module:
  canvas-context-cdp

target role:
  application service

coordinator decision:
  coordinator keeps CdpWriteKeyAuthService assigned to canvas-context-cdp until the owning task pack accepts the row

required tests:
  CdpWriteKeyAuthServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.ComputedProfileAttributeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  ComputedProfileAttributeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.ComputedTagService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedTagService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  ComputedTagServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.CustomerPointsLedgerService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CustomerPointsLedgerService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  CustomerPointsLedgerServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.EventAttributeDiscoveryService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoveryService.java

target module:
  canvas-platform

target role:
  adapter.messaging

coordinator decision:
  coordinator keeps EventAttributeDiscoveryService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  EventAttributeDiscoveryServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.RealtimeAudienceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  RealtimeAudienceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.WebhookDeliveryPayload

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookDeliveryPayload.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  WebhookDeliveryPayloadTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.WebhookDispatcherService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookDispatcherService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  WebhookDispatcherServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.WebhookRetryPolicy

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookRetryPolicy.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  WebhookRetryPolicyTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.WebhookSignatureService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookSignatureService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  WebhookSignatureServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.cdp.WebhookSubscriptionValidator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/WebhookSubscriptionValidator.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps WebhookSubscriptionValidator assigned to canvas-platform until the owning task pack accepts the row

required tests:
  WebhookSubscriptionValidatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.collaboration.CanvasCollaborationSummaryService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/CanvasCollaborationSummaryService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasCollaborationSummaryServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.collaboration.MyBatisUserWorkspacePreferenceRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/MyBatisUserWorkspacePreferenceRepository.java

target module:
  canvas-context-marketing

target role:
  adapter.persistence

owning worker:
  DDD-W03

required tests:
  MyBatisUserWorkspacePreferenceRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.collaboration.UserWorkspacePreferenceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  UserWorkspacePreferenceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.compliance.AuditEventService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance/AuditEventService.java

target module:
  canvas-platform

target role:
  adapter.messaging

owning worker:
  DDD-W01

required tests:
  AuditEventServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.compliance.DataDeletionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance/DataDeletionService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  DataDeletionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.compliance.PiiMaskingService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance/PiiMaskingService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  PiiMaskingServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.ContentEntryService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/ContentEntryService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  ContentEntryServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.ContentTemplateService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/ContentTemplateService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  ContentTemplateServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.MarketingAssetS3PresignProperties

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingAssetS3PresignProperties.java

target module:
  canvas-context-marketing

target role:
  config

owning worker:
  DDD-W03

required tests:
  MarketingAssetS3PresignPropertiesTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.MarketingAssetS3Presigner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingAssetS3Presigner.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingAssetS3PresignerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.MarketingAssetService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingAssetService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingAssetServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.MarketingAssetUploadHandoff

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingAssetUploadHandoff.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingAssetUploadHandoffTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.MarketingAssetUploadHandoffRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingAssetUploadHandoffRequest.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingAssetUploadHandoffRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.MarketingAssetUploadHandoffService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingAssetUploadHandoffService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingAssetUploadHandoffServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.MarketingAssetUploadIntentCleanupScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingAssetUploadIntentCleanupScheduler.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  MarketingAssetUploadIntentCleanupSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.MarketingAssetUploadService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingAssetUploadService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingAssetUploadServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.MarketingAssetUploadWebhookSignatureService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingAssetUploadWebhookSignatureService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingAssetUploadWebhookSignatureServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.MarketingContentReleaseService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingContentReleaseService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingContentReleaseServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.content.MarketingContentSupport

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingContentSupport.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingContentSupportTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.AbstractProviderConversationReplyAdapter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/AbstractProviderConversationReplyAdapter.java

target module:
  canvas-context-conversation

target role:
  adapter.external

owning worker:
  DDD-W06

required tests:
  AbstractProviderConversationReplyAdapterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAdapterCatalog

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterCatalog.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationAdapterCatalogTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAdapterContext

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterContext.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationAdapterContextTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAdapterHarness

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAdapterHarness.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationAdapterHarnessTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAiReplyGenerateCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAiReplyGenerateCommand.java

target module:
  canvas-context-conversation

target role:
  domain model or service

coordinator decision:
  coordinator keeps ConversationAiReplyGenerateCommand assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplyGenerateCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAiReplyGenerationContext

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAiReplyGenerationContext.java

target module:
  canvas-context-conversation

target role:
  domain model or service

coordinator decision:
  coordinator keeps ConversationAiReplyGenerationContext assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplyGenerationContextTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAiReplyGenerationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAiReplyGenerationResult.java

target module:
  canvas-context-conversation

target role:
  domain model or service

coordinator decision:
  coordinator keeps ConversationAiReplyGenerationResult assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplyGenerationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAiReplyGenerator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAiReplyGenerator.java

target module:
  canvas-context-conversation

target role:
  domain model or service

coordinator decision:
  coordinator keeps ConversationAiReplyGenerator assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplyGeneratorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAiReplyReviewCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAiReplyReviewCommand.java

target module:
  canvas-context-conversation

target role:
  domain model or service

coordinator decision:
  coordinator keeps ConversationAiReplyReviewCommand assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplyReviewCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAiReplyService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAiReplyService.java

target module:
  canvas-context-conversation

target role:
  application service

coordinator decision:
  coordinator keeps ConversationAiReplyService assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplyServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAiReplySuggestionQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAiReplySuggestionQuery.java

target module:
  canvas-context-conversation

target role:
  domain model or service

coordinator decision:
  coordinator keeps ConversationAiReplySuggestionQuery assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplySuggestionQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAiReplySuggestionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAiReplySuggestionView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

coordinator decision:
  coordinator keeps ConversationAiReplySuggestionView assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplySuggestionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationAssignmentCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationAssignmentCommand.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationAssignmentCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationContactProfileView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationContactProfileView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationContactProfileViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationInboxQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationInboxQuery.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationInboxQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationIngressReq

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressReq.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationIngressReqTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationIngressResp

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressResp.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationIngressRespTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationIngressService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressService.java

target module:
  canvas-context-conversation

target role:
  application service

owning worker:
  DDD-W06

required tests:
  ConversationIngressServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationMessageView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationMessageView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationMessageViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationPrivateDomainSyncService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationPrivateDomainSyncService.java

target module:
  canvas-context-conversation

target role:
  application service

owning worker:
  DDD-W06

required tests:
  ConversationPrivateDomainSyncServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationReplyAdapter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationReplyAdapter.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationReplyAdapterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationReplyAdapterSupport

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationReplyAdapterSupport.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationReplyAdapterSupportTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationRouteCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationRouteCommand.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationRouteCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationRouteResultView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationRouteResultView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationRouteResultViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationRoutingAgentCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationRoutingAgentCommand.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationRoutingAgentCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationRoutingAgentView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationRoutingAgentView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationRoutingAgentViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationRoutingRuleCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationRoutingRuleCommand.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationRoutingRuleCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationRoutingRuleView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationRoutingRuleView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationRoutingRuleViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationRoutingService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationRoutingService.java

target module:
  canvas-context-conversation

target role:
  application service

owning worker:
  DDD-W06

required tests:
  ConversationRoutingServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationSessionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationSessionView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationSessionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationSlaBreachView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationSlaBreachView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationSlaBreachViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationSlaEvaluationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationSlaEvaluationView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationSlaEvaluationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationSopTaskCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationSopTaskCommand.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationSopTaskCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationSopTaskCompletionCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationSopTaskCompletionCommand.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationSopTaskCompletionCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationSopTaskView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationSopTaskView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationSopTaskViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationWorkItemAuditView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationWorkItemAuditView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationWorkItemAuditViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationWorkItemStatusCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationWorkItemStatusCommand.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationWorkItemStatusCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationWorkItemView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationWorkItemView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationWorkItemViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationWorkspaceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationWorkspaceService.java

target module:
  canvas-context-conversation

target role:
  application service

owning worker:
  DDD-W06

required tests:
  ConversationWorkspaceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ConversationWorkspaceTimelineView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationWorkspaceTimelineView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  ConversationWorkspaceTimelineViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.LlmConversationAiReplyGenerator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/LlmConversationAiReplyGenerator.java

target module:
  canvas-context-conversation

target role:
  domain model or service

coordinator decision:
  coordinator keeps LlmConversationAiReplyGenerator assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  LlmConversationAiReplyGeneratorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.PrivateDomainContactQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/PrivateDomainContactQuery.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  PrivateDomainContactQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.PrivateDomainContactSnapshot

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/PrivateDomainContactSnapshot.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  PrivateDomainContactSnapshotTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.PrivateDomainContactView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/PrivateDomainContactView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  PrivateDomainContactViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.PrivateDomainGroupMemberSnapshot

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/PrivateDomainGroupMemberSnapshot.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  PrivateDomainGroupMemberSnapshotTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.PrivateDomainGroupMemberView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/PrivateDomainGroupMemberView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  PrivateDomainGroupMemberViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.PrivateDomainGroupQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/PrivateDomainGroupQuery.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  PrivateDomainGroupQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.PrivateDomainGroupSnapshot

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/PrivateDomainGroupSnapshot.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  PrivateDomainGroupSnapshotTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.PrivateDomainGroupView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/PrivateDomainGroupView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  PrivateDomainGroupViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.PrivateDomainSyncCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/PrivateDomainSyncCommand.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  PrivateDomainSyncCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.PrivateDomainSyncRunView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/PrivateDomainSyncRunView.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  PrivateDomainSyncRunViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.ProviderConversationReplyPayload

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ProviderConversationReplyPayload.java

target module:
  canvas-context-conversation

target role:
  adapter.external

owning worker:
  DDD-W06

required tests:
  ProviderConversationReplyPayloadTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.RcsConversationReplyAdapter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/RcsConversationReplyAdapter.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  RcsConversationReplyAdapterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.RcsConversationReplyPayload

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/RcsConversationReplyPayload.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  RcsConversationReplyPayloadTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.SandboxConversationReplyAdapter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/SandboxConversationReplyAdapter.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  SandboxConversationReplyAdapterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.SandboxConversationReplyPayload

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/SandboxConversationReplyPayload.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  SandboxConversationReplyPayloadTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.SocialDmConversationReplyAdapter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/SocialDmConversationReplyAdapter.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  SocialDmConversationReplyAdapterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.SocialDmConversationReplyPayload

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/SocialDmConversationReplyPayload.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  SocialDmConversationReplyPayloadTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.WebChatConversationReplyAdapter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WebChatConversationReplyAdapter.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  WebChatConversationReplyAdapterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.WebChatConversationReplyPayload

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WebChatConversationReplyPayload.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  WebChatConversationReplyPayloadTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.WhatsAppConversationReplyAdapter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppConversationReplyAdapter.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  WhatsAppConversationReplyAdapterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.WhatsAppConversationReplyPayload

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppConversationReplyPayload.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  WhatsAppConversationReplyPayloadTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.WhatsAppWebhookPayloadMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapper.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  WhatsAppWebhookPayloadMapperTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.conversation.WhatsAppWebhookSecurityService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookSecurityService.java

target module:
  canvas-context-conversation

target role:
  application service

owning worker:
  DDD-W06

required tests:
  WhatsAppWebhookSecurityServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorCampaignCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorCampaignCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps CreatorCampaignCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  CreatorCampaignCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorCampaignView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorCampaignView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps CreatorCampaignView assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  CreatorCampaignViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorCollaborationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorCollaborationCommand.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps CreatorCollaborationCommand assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorCollaborationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorCollaborationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorCollaborationService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps CreatorCollaborationService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorCollaborationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorCollaborationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorCollaborationView.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps CreatorCollaborationView assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorCollaborationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorDeliverableCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorDeliverableCommand.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps CreatorDeliverableCommand assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorDeliverableCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorDeliverableView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorDeliverableView.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps CreatorDeliverableView assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorDeliverableViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorPerformanceSummaryQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorPerformanceSummaryQuery.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps CreatorPerformanceSummaryQuery assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorPerformanceSummaryQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorPerformanceSummaryView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorPerformanceSummaryView.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps CreatorPerformanceSummaryView assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorPerformanceSummaryViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProfileCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProfileCommand.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps CreatorProfileCommand assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProfileCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProfileView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProfileView.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps CreatorProfileView assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProfileViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProviderMutationApprovalCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderMutationApprovalCommand.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps CreatorProviderMutationApprovalCommand assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationApprovalCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProviderMutationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderMutationCommand.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps CreatorProviderMutationCommand assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProviderMutationExecuteCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderMutationExecuteCommand.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps CreatorProviderMutationExecuteCommand assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationExecuteCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProviderMutationQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderMutationQuery.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps CreatorProviderMutationQuery assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProviderMutationRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderMutationRequest.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps CreatorProviderMutationRequest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProviderMutationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderMutationResult.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps CreatorProviderMutationResult assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProviderMutationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderMutationService.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps CreatorProviderMutationService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProviderMutationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderMutationView.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps CreatorProviderMutationView assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProviderWriteClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderWriteClient.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps CreatorProviderWriteClient assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderWriteClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.CreatorProviderWriteGateway

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProviderWriteGateway.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps CreatorProviderWriteGateway assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderWriteGatewayTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.creator.SandboxCreatorProviderWriteClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/SandboxCreatorProviderWriteClient.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps SandboxCreatorProviderWriteClient assigned to canvas-platform until the owning task pack accepts the row

required tests:
  SandboxCreatorProviderWriteClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.datasource.DataSourceConfigService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/datasource/DataSourceConfigService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps DataSourceConfigService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DataSourceConfigServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.datasource.DataSourceCredentialCipher

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/datasource/DataSourceCredentialCipher.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  DataSourceCredentialCipherTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.datasource.DataSourceTableMeta

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/datasource/DataSourceTableMeta.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps DataSourceTableMeta assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DataSourceTableMetaTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.demo.DemoSandboxService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoSandboxService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps DemoSandboxService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DemoSandboxServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.demo.JdbcDemoSandboxRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/JdbcDemoSandboxRepository.java

target module:
  canvas-platform

target role:
  adapter.persistence

coordinator decision:
  coordinator keeps JdbcDemoSandboxRepository assigned to canvas-platform until the owning task pack accepts the row

required tests:
  JdbcDemoSandboxRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.execution.CanvasExecutionRequestStatusCount

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequestStatusCount.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestStatusCountTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.loyalty.LoyaltyService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/loyalty/LoyaltyService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps LoyaltyService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LoyaltyServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthActivityCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityEventCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityEventCommand.java

target module:
  canvas-context-marketing

target role:
  adapter.messaging

owning worker:
  DDD-W03

required tests:
  GrowthActivityEventCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityEventService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityEventService.java

target module:
  canvas-context-marketing

target role:
  adapter.messaging

owning worker:
  DDD-W03

required tests:
  GrowthActivityEventServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityEventView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityEventView.java

target module:
  canvas-context-marketing

target role:
  adapter.messaging

owning worker:
  DDD-W03

required tests:
  GrowthActivityEventViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityReadinessCheckView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityReadinessCheckView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthActivityReadinessCheckViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityReadinessService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityReadinessService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  GrowthActivityReadinessServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityReadinessView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityReadinessView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthActivityReadinessViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityReportService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityReportService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  GrowthActivityReportServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityReportView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityReportView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthActivityReportViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  GrowthActivityServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthActivityView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthActivityViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthBenefitGrantResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthBenefitGrantResult.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthBenefitGrantResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthBenefitPromotionGrantAdapter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthBenefitPromotionGrantAdapter.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthBenefitPromotionGrantAdapterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthLoyaltyAdapter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthLoyaltyAdapter.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps GrowthLoyaltyAdapter assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  GrowthLoyaltyAdapterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthLoyaltyResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthLoyaltyResult.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps GrowthLoyaltyResult assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  GrowthLoyaltyResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthReferralCodeView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthReferralCodeView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthReferralCodeViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthReferralQualificationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthReferralQualificationCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthReferralQualificationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthReferralRelationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthReferralRelationCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthReferralRelationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthReferralRelationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthReferralRelationView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthReferralRelationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthReferralService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthReferralService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  GrowthReferralServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthRewardGrantCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthRewardGrantCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthRewardGrantCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthRewardGrantService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthRewardGrantService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  GrowthRewardGrantServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthRewardGrantView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthRewardGrantView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthRewardGrantViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthRewardPoolCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthRewardPoolCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthRewardPoolCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthRewardPoolService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthRewardPoolService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  GrowthRewardPoolServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthRewardPoolView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthRewardPoolView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthRewardPoolViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthTaskDefinitionCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthTaskDefinitionCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthTaskDefinitionCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthTaskDefinitionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthTaskDefinitionView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthTaskDefinitionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthTaskProgressCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthTaskProgressCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthTaskProgressCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthTaskProgressView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthTaskProgressView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GrowthTaskProgressViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.GrowthTaskService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/GrowthTaskService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  GrowthTaskServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.HttpMarketingIntegrationContractProbeClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/HttpMarketingIntegrationContractProbeClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  HttpMarketingIntegrationContractProbeClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingCampaignCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingCampaignCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingCampaignCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingCampaignLinkCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingCampaignLinkCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingCampaignLinkCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingCampaignLinkView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingCampaignLinkView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingCampaignLinkViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingCampaignReadinessFinding

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingCampaignReadinessFinding.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingCampaignReadinessFindingTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingCampaignReadinessView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingCampaignReadinessView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingCampaignReadinessViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingCampaignService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingCampaignService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingCampaignServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingCampaignView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingCampaignView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingCampaignViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingFormService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingFormService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingFormServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractAuditEventView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractAuditEventView.java

target module:
  canvas-context-marketing

target role:
  adapter.messaging

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractAuditEventViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeAlertService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeAlertService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeAlertServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeAutomationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeAutomationService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeAutomationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeRunCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeRunCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeRunCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeRunView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeRunView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeRunViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeScheduler.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  MarketingIntegrationContractProbeSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractSloEvaluationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractSloEvaluationView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractSloEvaluationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractSloService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractSloService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractSloServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractSloWindowView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractSloWindowView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractSloWindowViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.meta.AbExperimentGovernanceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGovernanceService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps AbExperimentGovernanceService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentGovernanceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.meta.AbExperimentGroupService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGroupService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps AbExperimentGroupService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentGroupServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.meta.EventDefinitionCacheService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/EventDefinitionCacheService.java

target module:
  canvas-platform

target role:
  adapter.messaging

coordinator decision:
  coordinator keeps EventDefinitionCacheService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  EventDefinitionCacheServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.meta.IdentityTypeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/IdentityTypeService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  IdentityTypeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.meta.MetaService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MetaService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps MetaService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  MetaServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.meta.MqMessageDefinitionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MqMessageDefinitionService.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  MqMessageDefinitionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.meta.SystemOptionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps SystemOptionService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  SystemOptionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.meta.TagDefinitionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinitionService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  TagDefinitionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.meta.TagImportService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  TagImportServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.meta.TagImportSourceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportSourceService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  TagImportSourceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.EnvironmentMarketingMonitorProviderCredentialResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/EnvironmentMarketingMonitorProviderCredentialResolver.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  EnvironmentMarketingMonitorProviderCredentialResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.JavaMarketingMonitorProviderHttpTransport

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/JavaMarketingMonitorProviderHttpTransport.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  JavaMarketingMonitorProviderHttpTransportTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.LlmMarketingMonitorInferenceGenerator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/LlmMarketingMonitorInferenceGenerator.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  LlmMarketingMonitorInferenceGeneratorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingCompetitorMentionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingCompetitorMentionView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingCompetitorMentionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAlertChannelCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertChannelCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertChannelCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAlertChannelView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertChannelView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertChannelViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAlertDeliveryView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertDeliveryView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertDeliveryViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAlertDispatchView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertDispatchView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertDispatchViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertFanoutService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertFanoutServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAlertQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAlertView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyDetectionCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyDetectionCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyDetectionService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyDetectionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyDetectionView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyDetectionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyEventQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyEventQuery.java

target module:
  canvas-context-marketing

target role:
  adapter.messaging

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyEventQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyEventView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyEventView.java

target module:
  canvas-context-marketing

target role:
  adapter.messaging

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyEventViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyRuleCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyRuleCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyRuleCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyRuleView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyRuleView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyRuleViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorInferenceCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceGenerationContext

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceGenerationContext.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorInferenceGenerationContextTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceGenerationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceGenerationResult.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorInferenceGenerationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceGenerator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceGenerator.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorInferenceGeneratorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorInferenceQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorInferenceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorInferenceViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorIngestResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorIngestResult.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorIngestResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorItemIngestCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorItemIngestCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorItemIngestCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorItemQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorItemQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorItemQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorItemView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorItemView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorItemViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollItem

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollItem.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollItemTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollRequest.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollResponse

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollResponse.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollResponseTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollRunView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollRunView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollRunViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollingScheduleService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingScheduleService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollingScheduleServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollingScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingScheduler.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  MarketingMonitorPollingSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollingService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollingServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialCommand.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialDueRefreshCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialDueRefreshCommand.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialDueRefreshCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialDueRefreshResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialDueRefreshResult.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialDueRefreshResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialEventQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialEventQuery.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialEventQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialEventView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialEventView.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialEventViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialQuery.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialRefreshCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialRefreshCommand.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialRefreshCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialRefreshScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialRefreshScheduler.java

target module:
  canvas-context-execution

target role:
  adapter.external

owning worker:
  DDD-W08

required tests:
  MarketingMonitorProviderCredentialRefreshSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialResolver.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialRevokeCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialRevokeCommand.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialRevokeCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialService.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialView.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderHttpRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderHttpRequest.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderHttpRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderHttpResponse

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderHttpResponse.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderHttpResponseTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderHttpTransport

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderHttpTransport.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderHttpTransportTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderOAuthAuthorizationCommand.java

target module:
  canvas-context-marketing

target role:
  adapter.external

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationEventQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderOAuthAuthorizationEventQuery.java

target module:
  canvas-context-marketing

target role:
  adapter.external

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationEventQuery assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationEventQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationEventView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderOAuthAuthorizationEventView.java

target module:
  canvas-context-marketing

target role:
  adapter.external

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationEventView assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationEventViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderOAuthAuthorizationQuery.java

target module:
  canvas-context-marketing

target role:
  adapter.external

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationQuery assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderOAuthAuthorizationService.java

target module:
  canvas-context-marketing

target role:
  adapter.external

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationService assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderOAuthAuthorizationView.java

target module:
  canvas-context-marketing

target role:
  adapter.external

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationView assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthCallbackCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderOAuthCallbackCommand.java

target module:
  canvas-context-marketing

target role:
  adapter.external

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthCallbackCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthCallbackCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderPollClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderPollClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderPollClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorSourceCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorSourceCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorSourceCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorSourcePollingCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorSourcePollingCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorSourcePollingCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorSourcePollingView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorSourcePollingView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorSourcePollingViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorSourceView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorSourceView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorSourceViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorTrendSnapshotCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorTrendSnapshotCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorTrendSnapshotQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorTrendSnapshotQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorTrendSnapshotView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorTrendSnapshotViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookIngestView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorWebhookIngestViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookIngestionService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorWebhookIngestionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookPayloadMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookPayloadMapper.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorWebhookPayloadMapperTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookSecretView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookSecretView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorWebhookSecretViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookSignatureService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookSignatureService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingMonitorWebhookSignatureServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitoringService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitoringService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingMonitoringServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.MarketingSentimentAnalysisView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingSentimentAnalysisView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MarketingSentimentAnalysisViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.monitoring.SandboxMarketingMonitorPollClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/SandboxMarketingMonitorPollClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SandboxMarketingMonitorPollClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.notification.NotificationCreateCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationCreateCommand.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps NotificationCreateCommand assigned to canvas-platform until the owning task pack accepts the row

required tests:
  NotificationCreateCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.notification.NotificationEventService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationEventService.java

target module:
  canvas-platform

target role:
  adapter.messaging

coordinator decision:
  coordinator keeps NotificationEventService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  NotificationEventServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.notification.NotificationRealtimeEnvelope

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationRealtimeEnvelope.java

target module:
  canvas-context-cdp

target role:
  domain model or service

coordinator decision:
  coordinator keeps NotificationRealtimeEnvelope assigned to canvas-context-cdp until the owning task pack accepts the row

required tests:
  NotificationRealtimeEnvelopeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.notification.NotificationRealtimePublisher

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationRealtimePublisher.java

target module:
  canvas-context-cdp

target role:
  domain model or service

coordinator decision:
  coordinator keeps NotificationRealtimePublisher assigned to canvas-context-cdp until the owning task pack accepts the row

required tests:
  NotificationRealtimePublisherTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.notification.NotificationRealtimeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationRealtimeService.java

target module:
  canvas-context-cdp

target role:
  application service

coordinator decision:
  coordinator keeps NotificationRealtimeService assigned to canvas-context-cdp until the owning task pack accepts the row

required tests:
  NotificationRealtimeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.notification.NotificationRecipientService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationRecipientService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps NotificationRecipientService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  NotificationRecipientServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.notification.NotificationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps NotificationService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  NotificationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.notification.NotificationWebSocketHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationWebSocketHandler.java

target module:
  canvas-context-execution

target role:
  domain service

coordinator decision:
  coordinator keeps NotificationWebSocketHandler assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  NotificationWebSocketHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.notification.NotificationWebSocketTicketService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationWebSocketTicketService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps NotificationWebSocketTicketService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  NotificationWebSocketTicketServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.ops.OpsAuditEventService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ops/OpsAuditEventService.java

target module:
  canvas-platform

target role:
  adapter.messaging

coordinator decision:
  coordinator keeps OpsAuditEventService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  OpsAuditEventServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.paidmedia.PaidMediaAudienceDestinationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceDestinationView.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceDestinationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.paidmedia.PaidMediaAudienceMemberQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceMemberQuery.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceMemberQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.paidmedia.PaidMediaAudienceMemberView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceMemberView.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceMemberViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.paidmedia.PaidMediaAudienceRunQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceRunQuery.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceRunQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.paidmedia.PaidMediaAudienceSyncCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceSyncCommand.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceSyncCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.paidmedia.PaidMediaAudienceSyncRunView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceSyncRunView.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceSyncRunViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.paidmedia.PaidMediaAudienceSyncService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceSyncService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceSyncServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.paidmedia.PaidMediaDestinationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/PaidMediaDestinationCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  PaidMediaDestinationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.policy.MarketingPreferenceCenterService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/policy/MarketingPreferenceCenterService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingPreferenceCenterServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspCampaignCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspCampaignCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspCampaignCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspCampaignCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspCampaignView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspCampaignView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspCampaignView assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspCampaignViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspLineItemCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspLineItemCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspLineItemCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspLineItemCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspLineItemView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspLineItemView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspLineItemView assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspLineItemViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationApprovalCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationApprovalCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspMutationApprovalCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationApprovalCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspMutationCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationExecuteCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationExecuteCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspMutationExecuteCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationExecuteCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspMutationQuery assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationRequest.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspMutationRequest assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationResult.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspMutationResult assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationService.java

target module:
  canvas-context-marketing

target role:
  application service

coordinator decision:
  coordinator keeps ProgrammaticDspMutationService assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspMutationView assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspProviderWriteClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspProviderWriteClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

coordinator decision:
  coordinator keeps ProgrammaticDspProviderWriteClient assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspProviderWriteClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspProviderWriteGateway

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspProviderWriteGateway.java

target module:
  canvas-context-marketing

target role:
  adapter.external

coordinator decision:
  coordinator keeps ProgrammaticDspProviderWriteGateway assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspProviderWriteGatewayTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspSeatCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspSeatCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspSeatCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSeatCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspSeatView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspSeatView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspSeatView assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSeatViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspService.java

target module:
  canvas-context-marketing

target role:
  application service

coordinator decision:
  coordinator keeps ProgrammaticDspService assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspSnapshotCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspSnapshotCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspSnapshotCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSnapshotCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspSnapshotView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspSnapshotView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspSnapshotView assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSnapshotViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspSummaryQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspSummaryQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspSummaryQuery assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSummaryQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspSummaryView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspSummaryView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspSummaryView assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSummaryViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspSupplyPathCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspSupplyPathCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspSupplyPathCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSupplyPathCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspSupplyPathView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspSupplyPathView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ProgrammaticDspSupplyPathView assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSupplyPathViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.programmatic.SandboxProgrammaticDspProviderWriteClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/SandboxProgrammaticDspProviderWriteClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

coordinator decision:
  coordinator keeps SandboxProgrammaticDspProviderWriteClient assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  SandboxProgrammaticDspProviderWriteClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.project.CanvasProjectAction

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project/CanvasProjectAction.java

target module:
  canvas-context-canvas

target role:
  domain model or service

owning worker:
  DDD-W07

required tests:
  CanvasProjectActionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.project.CanvasProjectPermissionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project/CanvasProjectPermissionService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasProjectPermissionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.project.CanvasProjectRole

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project/CanvasProjectRole.java

target module:
  canvas-context-canvas

target role:
  domain model or service

owning worker:
  DDD-W07

required tests:
  CanvasProjectRoleTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.project.CanvasProjectService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/project/CanvasProjectService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasProjectServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/providerwrite/ProviderWriteEvidenceSanitizer.java

target module:
  canvas-platform

target role:
  adapter.external

owning worker:
  DDD-W01

required tests:
  ProviderWriteEvidenceSanitizerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/providerwrite/ProviderWriteSandboxSupport.java

target module:
  canvas-platform

target role:
  adapter.external

owning worker:
  DDD-W01

required tests:
  ProviderWriteSandboxSupportTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskFactorCatalog

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskFactorCatalog.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskFactorCatalogTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskFactorDefinition

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskFactorDefinition.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskFactorDefinitionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskFeatureAvailability

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskFeatureAvailability.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskFeatureAvailabilityTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskListCatalog

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskListCatalog.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListCatalogTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskListDefinition

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskListDefinition.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListDefinitionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskOperandType

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskOperandType.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskOperandTypeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleConditionNode

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleConditionNode.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleConditionNodeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleGroupNode

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleGroupNode.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleGroupNodeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleLogic

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleLogic.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleLogicTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleNode

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleNode.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleNodeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleOperand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleOperand.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleOperandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleOperator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleOperator.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleOperatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleParseException

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleParseException.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleParseExceptionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleParser

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleParser.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleParserTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleValidationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleValidationResult.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleValidationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleValidator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuleValidator.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleValidatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskRuntimeMode.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuntimeModeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskSubjectType

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskSubjectType.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskSubjectTypeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskValidationError

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskValidationError.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskValidationErrorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskValidationErrorCode

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskValidationErrorCode.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskValidationErrorCodeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.dsl.RiskValueType

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/dsl/RiskValueType.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskValueTypeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.feature.RedisRiskFeatureStore

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/feature/RedisRiskFeatureStore.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  RedisRiskFeatureStoreTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.feature.RiskFeatureCatalogService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/feature/RiskFeatureCatalogService.java

target module:
  canvas-context-risk

target role:
  application service

owning worker:
  DDD-W02

required tests:
  RiskFeatureCatalogServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.feature.RiskFeatureDefinition

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/feature/RiskFeatureDefinition.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskFeatureDefinitionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.feature.RiskFeatureResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/feature/RiskFeatureResolver.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskFeatureResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.feature.RiskFeatureStore

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/feature/RiskFeatureStore.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskFeatureStoreTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.JdbcRiskListStore

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/JdbcRiskListStore.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  JdbcRiskListStoreTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.JdbcRiskSceneStore

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/JdbcRiskSceneStore.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  JdbcRiskSceneStoreTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.JdbcRiskStrategyStateStore

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/JdbcRiskStrategyStateStore.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  JdbcRiskStrategyStateStoreTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskListCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskListCommand.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskListEntryCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskListEntryCommand.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListEntryCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskListEntryView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskListEntryView.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListEntryViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskListHitView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskListHitView.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListHitViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskListImportCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskListImportCommand.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListImportCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskListImportResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskListImportResult.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListImportResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskListService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskListService.java

target module:
  canvas-context-risk

target role:
  application service

owning worker:
  DDD-W02

required tests:
  RiskListServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskListSubjectHasher

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskListSubjectHasher.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListSubjectHasherTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskListView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskListView.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskSceneService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskSceneService.java

target module:
  canvas-context-risk

target role:
  application service

owning worker:
  DDD-W02

required tests:
  RiskSceneServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskSceneView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskSceneView.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskSceneViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskStrategyCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskStrategyCommand.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskStrategyDiffView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskStrategyDiffView.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyDiffViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskStrategyLifecycleStatus

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskStrategyLifecycleStatus.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyLifecycleStatusTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskStrategyService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskStrategyService.java

target module:
  canvas-context-risk

target role:
  application service

owning worker:
  DDD-W02

required tests:
  RiskStrategyServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskStrategyTransitionRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskStrategyTransitionRequest.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyTransitionRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskStrategyVersionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskStrategyVersionView.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyVersionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.governance.RiskStrategyView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/governance/RiskStrategyView.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.graph.RiskGraphConnection

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/graph/RiskGraphConnection.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskGraphConnectionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.graph.RiskGraphListHit

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/graph/RiskGraphListHit.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskGraphListHitTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.graph.RiskGraphListSubject

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/graph/RiskGraphListSubject.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskGraphListSubjectTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.graph.RiskGraphService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/graph/RiskGraphService.java

target module:
  canvas-context-risk

target role:
  application service

owning worker:
  DDD-W02

required tests:
  RiskGraphServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.graph.RiskGraphSubjectSnapshot

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/graph/RiskGraphSubjectSnapshot.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskGraphSubjectSnapshotTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.graph.RiskGraphSummary

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/graph/RiskGraphSummary.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskGraphSummaryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.lab.InMemoryRiskSimulationRunRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/InMemoryRiskSimulationRunRepository.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  InMemoryRiskSimulationRunRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.lab.JdbcRiskSimulationRunRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/JdbcRiskSimulationRunRepository.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  JdbcRiskSimulationRunRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.lab.JdbcRiskSimulationSampleRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/JdbcRiskSimulationSampleRepository.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  JdbcRiskSimulationSampleRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.lab.RiskSimulationActivationGuard

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/RiskSimulationActivationGuard.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskSimulationActivationGuardTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.lab.RiskSimulationHistoryView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/RiskSimulationHistoryView.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskSimulationHistoryViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.lab.RiskSimulationRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/RiskSimulationRequest.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskSimulationRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.lab.RiskSimulationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/RiskSimulationResult.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskSimulationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.lab.RiskSimulationRunRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/RiskSimulationRunRepository.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskSimulationRunRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.lab.RiskSimulationSampleRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/RiskSimulationSampleRepository.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskSimulationSampleRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.lab.RiskSimulationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/RiskSimulationService.java

target module:
  canvas-context-risk

target role:
  application service

owning worker:
  DDD-W02

required tests:
  RiskSimulationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.lab.RiskSimulationStatus

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/lab/RiskSimulationStatus.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskSimulationStatusTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.modeling.RiskModelClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/modeling/RiskModelClient.java

target module:
  canvas-context-risk

target role:
  adapter.external

owning worker:
  DDD-W02

required tests:
  RiskModelClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.modeling.RiskModelClientCall

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/modeling/RiskModelClientCall.java

target module:
  canvas-context-risk

target role:
  adapter.external

owning worker:
  DDD-W02

required tests:
  RiskModelClientCallTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.modeling.RiskModelDefinition

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/modeling/RiskModelDefinition.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskModelDefinitionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.modeling.RiskModelGateway

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/modeling/RiskModelGateway.java

target module:
  canvas-context-risk

target role:
  adapter.external

owning worker:
  DDD-W02

required tests:
  RiskModelGatewayTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.modeling.RiskModelRegistryService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/modeling/RiskModelRegistryService.java

target module:
  canvas-context-risk

target role:
  application service

owning worker:
  DDD-W02

required tests:
  RiskModelRegistryServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.modeling.RiskModelRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/modeling/RiskModelRequest.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskModelRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.modeling.RiskModelResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/modeling/RiskModelResult.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskModelResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.modeling.RiskModelTimeoutException

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/modeling/RiskModelTimeoutException.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskModelTimeoutExceptionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.JdbcRiskDecisionLedger

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/JdbcRiskDecisionLedger.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  JdbcRiskDecisionLedgerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.JdbcRiskDecisionTraceReader

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/JdbcRiskDecisionTraceReader.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  JdbcRiskDecisionTraceReaderTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskActiveStrategyReader

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskActiveStrategyReader.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskActiveStrategyReaderTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskBand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskBand.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskBandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskCompiledRule

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskCompiledRule.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskCompiledRuleTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskCompiledStrategy

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskCompiledStrategy.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskCompiledStrategyTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionAction

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionAction.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskDecisionActionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionLedger

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionLedger.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskDecisionLedgerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionMergeRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionMergeRequest.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskDecisionMergeRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionMerger

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionMerger.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskDecisionMergerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionReplayMismatchException

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionReplayMismatchException.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskDecisionReplayMismatchExceptionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionRequest.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskDecisionRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionResponse.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskDecisionResponseTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionRuleHit

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionRuleHit.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskDecisionRuleHitTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionRunRecord

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionRunRecord.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskDecisionRunRecordTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionService.java

target module:
  canvas-context-risk

target role:
  application service

owning worker:
  DDD-W02

required tests:
  RiskDecisionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionSignal

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionSignal.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskDecisionSignalTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskFailPolicy

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskFailPolicy.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskFailPolicyTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskFeatureResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskFeatureResolver.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskFeatureResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskListEntry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskListEntry.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListEntryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskListEntryRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskListEntryRepository.java

target module:
  canvas-context-risk

target role:
  adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskListEntryRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskListMatchResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskListMatchResult.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListMatchResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskListMatcher

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskListMatcher.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListMatcherTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskListType

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskListType.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskListTypeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskMergedDecision

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskMergedDecision.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskMergedDecisionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskRequestFeatureResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskRequestFeatureResolver.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRequestFeatureResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskResolvedValue

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskResolvedValue.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskResolvedValueTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskRuleEvaluationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskRuleEvaluationResult.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleEvaluationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskRuleEvaluator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskRuleEvaluator.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleEvaluatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskRuleEvidence

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskRuleEvidence.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleEvidenceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskRuleGroupMatchPolicy

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskRuleGroupMatchPolicy.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleGroupMatchPolicyTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskRuleGroupType

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskRuleGroupType.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskRuleGroupTypeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskStrategyCompileErrorCode

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyCompileErrorCode.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyCompileErrorCodeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskStrategyCompileException

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyCompileException.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyCompileExceptionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskStrategyCompileLimits

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyCompileLimits.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyCompileLimitsTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskStrategyCompiler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyCompiler.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyCompilerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskStrategyRuleDefinition

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyRuleDefinition.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyRuleDefinitionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskStrategyRuleGroupDefinition

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyRuleGroupDefinition.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyRuleGroupDefinitionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskStrategyRuntimeCache

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyRuntimeCache.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategyRuntimeCacheTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskStrategySnapshot

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskStrategySnapshot.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskStrategySnapshotTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskSubjectHasher

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskSubjectHasher.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskSubjectHasherTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.risk.runtime.RiskSubjectHashing

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/runtime/RiskSubjectHashing.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskSubjectHashingTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.GoogleAdsSearchMarketingProviderWriteClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/GoogleAdsSearchMarketingProviderWriteClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  GoogleAdsSearchMarketingProviderWriteClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.GoogleAdsSearchMarketingTransport

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/GoogleAdsSearchMarketingTransport.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  GoogleAdsSearchMarketingTransportTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.MicrosoftAdsSearchMarketingProviderWriteClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/MicrosoftAdsSearchMarketingProviderWriteClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MicrosoftAdsSearchMarketingProviderWriteClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.MicrosoftAdsSearchMarketingTransport

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/MicrosoftAdsSearchMarketingTransport.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  MicrosoftAdsSearchMarketingTransportTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SandboxSearchMarketingProviderReadClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SandboxSearchMarketingProviderReadClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SandboxSearchMarketingProviderReadClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SandboxSearchMarketingProviderWriteClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SandboxSearchMarketingProviderWriteClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SandboxSearchMarketingProviderWriteClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingCredentialRef

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingCredentialRef.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingCredentialRefTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingCredentialResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingCredentialResolver.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingCredentialResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingImpactWindowQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingImpactWindowQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingImpactWindowQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingImpactWindowService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingImpactWindowService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  SearchMarketingImpactWindowServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingImpactWindowView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingImpactWindowView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingImpactWindowViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingKeywordCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingKeywordCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingKeywordCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingKeywordQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingKeywordQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingKeywordQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingKeywordView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingKeywordView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingKeywordViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingManualSyncCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingManualSyncCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingManualSyncCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingMutationApprovalCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationApprovalCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps SearchMarketingMutationApprovalCommand assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  SearchMarketingMutationApprovalCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingMutationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingMutationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingMutationExecuteCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationExecuteCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingMutationExecuteCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingMutationQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingMutationQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingMutationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  SearchMarketingMutationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingMutationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingMutationView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingMutationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingOpportunityEvaluationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingOpportunityEvaluationCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingOpportunityEvaluationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingOpportunityMutationCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingOpportunityMutationCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingOpportunityMutationCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingOpportunityQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingOpportunityQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingOpportunityQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingOpportunityStatusCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingOpportunityStatusCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingOpportunityStatusCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingOpportunityView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingOpportunityView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingOpportunityViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingPerformanceRow

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingPerformanceRow.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingPerformanceRowTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderChangeQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderChangeQuery.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderChangeQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderChangeView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderChangeView.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderChangeViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderMutationRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderMutationRequest.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderMutationRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderMutationResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderMutationResult.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderMutationResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderReadClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderReadClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderReadClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderReadGateway

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderReadGateway.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderReadGatewayTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderSyncResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderSyncResult.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderSyncResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderWriteClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderWriteClient.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderWriteClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderWriteGateway

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingProviderWriteGateway.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderWriteGatewayTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingReadinessService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingReadinessService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  SearchMarketingReadinessServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingReadinessView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingReadinessView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingReadinessViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingReconciliationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingReconciliationService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  SearchMarketingReconciliationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingReconciliationView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingReconciliationView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingReconciliationViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  SearchMarketingServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSnapshotCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSnapshotCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSnapshotCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSnapshotQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSnapshotQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSnapshotQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSnapshotView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSnapshotView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSnapshotViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSourceCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSourceCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSourceCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSourceQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSourceQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSourceQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSourceView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSourceView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSourceViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSummaryQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSummaryQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSummaryQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSummaryView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSummaryView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSummaryViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSyncCommand

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSyncCommand.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSyncCommandTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSyncDueRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSyncDueRequest.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSyncDueRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSyncRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSyncRequest.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSyncRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSyncRunQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSyncRunQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSyncRunQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSyncRunService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSyncRunService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSyncRunServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingSyncRunView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSyncRunView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingSyncRunViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingUrlInspectionQuery

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingUrlInspectionQuery.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingUrlInspectionQueryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingUrlInspectionRow

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingUrlInspectionRow.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingUrlInspectionRowTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.search.SearchMarketingUrlInspectionView

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingUrlInspectionView.java

target module:
  canvas-context-marketing

target role:
  domain model or service

owning worker:
  DDD-W03

required tests:
  SearchMarketingUrlInspectionViewTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.task.AsyncTaskCreateResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskCreateResult.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps AsyncTaskCreateResult assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AsyncTaskCreateResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.task.AsyncTaskService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps AsyncTaskService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AsyncTaskServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.task.AsyncTaskStatus

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskStatus.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps AsyncTaskStatus assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AsyncTaskStatusTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.template.JdbcMessageTemplateRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/template/JdbcMessageTemplateRepository.java

target module:
  canvas-context-marketing

target role:
  adapter.persistence

owning worker:
  DDD-W03

required tests:
  JdbcMessageTemplateRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.template.MessageTemplateService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/template/MessageTemplateService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MessageTemplateServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.tenant.TenantService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/tenant/TenantService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps TenantService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  TenantServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseAggregationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAggregationService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAggregationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityIncidentScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAvailabilityIncidentScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAvailabilityIncidentSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityIncidentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAvailabilityIncidentService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAvailabilityIncidentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAvailabilityService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAvailabilityServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseBackfillService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseBackfillService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseBackfillServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseCatalogService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseCatalogService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseCatalogServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityIncidentScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseConsumerAvailabilityIncidentScheduler.java

target module:
  canvas-context-cdp

target role:
  adapter.messaging

owning worker:
  DDD-W04

required tests:
  CdpWarehouseConsumerAvailabilityIncidentSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityIncidentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseConsumerAvailabilityIncidentService.java

target module:
  canvas-context-cdp

target role:
  adapter.messaging

owning worker:
  DDD-W04

required tests:
  CdpWarehouseConsumerAvailabilityIncidentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseConsumerAvailabilityService.java

target module:
  canvas-context-cdp

target role:
  adapter.messaging

owning worker:
  DDD-W04

required tests:
  CdpWarehouseConsumerAvailabilityServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseDorisPrivacyErasureExecutor

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseDorisPrivacyErasureExecutor.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseDorisPrivacyErasureExecutorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseDorisPrometheusMetricsParser

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseDorisPrometheusMetricsParser.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  CdpWarehouseDorisPrometheusMetricsParserTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationGateService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseE2eCertificationGateServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationRunService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationRunService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseE2eCertificationRunServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseE2eCertificationSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapDorisEvidenceClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapDorisEvidenceClient.java

target module:
  canvas-context-cdp

target role:
  adapter.external

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapDorisEvidenceClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceCollectionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceCollectionService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapEvidenceSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapEvidenceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapReadinessService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapReadinessService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapReadinessServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseEventSink

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEventSink.java

target module:
  canvas-context-cdp

target role:
  adapter.messaging

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEventSinkTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseExternalRealtimeJobProbeClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseExternalRealtimeJobProbeClient.java

target module:
  canvas-context-cdp

target role:
  adapter.external

owning worker:
  DDD-W04

required tests:
  CdpWarehouseExternalRealtimeJobProbeClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseExternalRealtimeJobProbeScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseExternalRealtimeJobProbeScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseExternalRealtimeJobProbeSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseExternalRealtimeJobProbeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseExternalRealtimeJobProbeService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseExternalRealtimeJobProbeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseFieldGovernanceService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseFieldGovernanceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseIncidentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseIncidentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseJobLeaseService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseJobLeaseServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseMetricChangeReviewService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseMetricChangeReviewService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  CdpWarehouseMetricChangeReviewServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseMetricLineageService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseMetricLineageService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  CdpWarehouseMetricLineageServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseOperationsService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseOperationsService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseOperationsServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePhysicalE2eCertificationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehousePhysicalE2eCertificationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildAutomationService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyAudienceBitmapRebuildServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyErasureExecutionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyErasureExecutionService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyErasureExecutionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyErasureService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyErasureService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyErasureServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyTombstoneService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyTombstoneService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyTombstoneServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseProductionReadinessProofService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseProductionReadinessProofServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseQualityScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseQualityScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseQualitySchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseQualityService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseQualityService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseQualityServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseReadinessIncidentScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessIncidentScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseReadinessIncidentSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseReadinessIncidentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessIncidentService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseReadinessIncidentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseReadinessService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseReadinessServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCheckpointService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeCheckpointService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeCheckpointServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCutoverReadinessService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeCutoverReadinessService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeCutoverReadinessServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobControlService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeJobControlService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeJobControlServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobIncidentScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeJobIncidentScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeJobIncidentSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobIncidentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeJobIncidentService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeJobIncidentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePipelineIncidentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePipelineIncidentService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimePipelineIncidentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePipelineService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePipelineService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimePipelineServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeRetryScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeRetryScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeRetrySchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeRetryService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeRetryService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeRetryServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeSchemaService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeSchemaService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeSchemaServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRetentionScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRetentionScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRetentionSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRetentionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRetentionService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRetentionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseSemanticMetricService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSemanticMetricService.java

target module:
  canvas-context-bi

target role:
  application service

owning worker:
  DDD-W05

required tests:
  CdpWarehouseSemanticMetricServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseSloPolicyService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSloPolicyService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSloPolicyServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseSyntheticDataPathProbeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSyntheticDataPathProbeService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSyntheticDataPathProbeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseTableDriftIncidentScheduler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableDriftIncidentScheduler.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseTableDriftIncidentSchedulerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseTableDriftIncidentService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableDriftIncidentService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseTableDriftIncidentServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseTableGovernanceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableGovernanceService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpWarehouseTableGovernanceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.HttpCdpWarehouseExternalRealtimeJobProbeClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/HttpCdpWarehouseExternalRealtimeJobProbeClient.java

target module:
  canvas-context-cdp

target role:
  adapter.external

owning worker:
  DDD-W04

required tests:
  HttpCdpWarehouseExternalRealtimeJobProbeClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.domain.warehouse.HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient.java

target module:
  canvas-context-cdp

target role:
  adapter.external

owning worker:
  DDD-W04

required tests:
  HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.AudienceBatchComputeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  AudienceBatchComputeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.AudienceBitmapStore

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java

target module:
  canvas-context-cdp

target role:
  adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceBitmapStoreTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.AudienceComputeResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeResult.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  AudienceComputeResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.AudienceComputeTaskRunner

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunner.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  AudienceComputeTaskRunnerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.AudienceEvaluationContextFetcher

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceEvaluationContextFetcher.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  AudienceEvaluationContextFetcherTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.AudienceSchedulerService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceSchedulerService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  AudienceSchedulerServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.AudienceSnapshotService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceSnapshotService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  AudienceSnapshotServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.AudienceUserResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceUserResolver.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  AudienceUserResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.AviatorRuleEvaluator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AviatorRuleEvaluator.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  AviatorRuleEvaluatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.CdpAudienceSourceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/CdpAudienceSourceService.java

target module:
  canvas-context-cdp

target role:
  application service

owning worker:
  DDD-W04

required tests:
  CdpAudienceSourceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.JdbcConfig

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/JdbcConfig.java

target module:
  canvas-platform

target role:
  config

owning worker:
  DDD-W01

required tests:
  JdbcConfigTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.JdbcConfigResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/JdbcConfigResolver.java

target module:
  canvas-platform

target role:
  adapter.persistence

owning worker:
  DDD-W01

required tests:
  JdbcConfigResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.QLExpressRuleEvaluator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/QLExpressRuleEvaluator.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  QLExpressRuleEvaluatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.RuleEvaluator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/RuleEvaluator.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleEvaluatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.RuleEvaluatorRouter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/RuleEvaluatorRouter.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleEvaluatorRouterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.SqlWhereGenerator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/SqlWhereGenerator.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  SqlWhereGeneratorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.StableUserIndexService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/StableUserIndexService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  StableUserIndexServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.audience.VersionedAudienceBitmapStore

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/VersionedAudienceBitmapStore.java

target module:
  canvas-context-cdp

target role:
  adapter.persistence

owning worker:
  DDD-W04

required tests:
  VersionedAudienceBitmapStoreTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.ChannelConnector

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelConnector.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ChannelConnector assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ChannelConnectorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.ChannelConnectorJdbcRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelConnectorJdbcRepository.java

target module:
  canvas-context-marketing

target role:
  adapter.persistence

coordinator decision:
  coordinator keeps ChannelConnectorJdbcRepository assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ChannelConnectorJdbcRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.ChannelConnectorRegistry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelConnectorRegistry.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps ChannelConnectorRegistry assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ChannelConnectorRegistryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.ChannelDedupeRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelDedupeRepository.java

target module:
  canvas-context-marketing

target role:
  adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelDedupeRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.ChannelDedupeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelDedupeService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  ChannelDedupeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.ChannelFallbackDecisionRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelFallbackDecisionRepository.java

target module:
  canvas-context-marketing

target role:
  adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelFallbackDecisionRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.ChannelFallbackPolicyRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelFallbackPolicyRepository.java

target module:
  canvas-context-marketing

target role:
  adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelFallbackPolicyRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.ChannelFallbackService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelFallbackService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  ChannelFallbackServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.ChannelProviderLimitRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ChannelProviderLimitRepository.java

target module:
  canvas-context-marketing

target role:
  adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelProviderLimitRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.DisabledChannelConnector

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/DisabledChannelConnector.java

target module:
  canvas-context-marketing

target role:
  domain model or service

coordinator decision:
  coordinator keeps DisabledChannelConnector assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  DisabledChannelConnectorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.DownstreamBulkheadRegistry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/DownstreamBulkheadRegistry.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  DownstreamBulkheadRegistryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.HttpWhatsAppCloudApiClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/HttpWhatsAppCloudApiClient.java

target module:
  canvas-context-conversation

target role:
  adapter.external

owning worker:
  DDD-W06

required tests:
  HttpWhatsAppCloudApiClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.InMemoryChannelCounterStore

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/InMemoryChannelCounterStore.java

target module:
  canvas-context-marketing

target role:
  adapter.persistence

owning worker:
  DDD-W03

required tests:
  InMemoryChannelCounterStoreTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.ProviderBackpressureService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/ProviderBackpressureService.java

target module:
  canvas-platform

target role:
  adapter.external

owning worker:
  DDD-W01

required tests:
  ProviderBackpressureServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.WhatsAppCloudApiClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiClient.java

target module:
  canvas-context-conversation

target role:
  adapter.external

owning worker:
  DDD-W06

required tests:
  WhatsAppCloudApiClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.channel.WhatsAppCloudApiConnector

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiConnector.java

target module:
  canvas-context-conversation

target role:
  domain model or service

owning worker:
  DDD-W06

required tests:
  WhatsAppCloudApiConnectorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.concurrent.BackgroundTaskExecutor

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/concurrent/BackgroundTaskExecutor.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  BackgroundTaskExecutorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.context.ContextOverflowException

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ContextOverflowException.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  ContextOverflowExceptionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.context.ExecutionContext

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  ExecutionContextTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.context.NodeGate

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/NodeGate.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  NodeGateTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.context.NodeStatus

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/NodeStatus.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  NodeStatusTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.dag.DagGraph

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/dag/DagGraph.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  DagGraphTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.dag.DagParser

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/dag/DagParser.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  DagParserTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.delivery.DeliveryOutboxDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxDO.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  DeliveryOutboxDOTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.delivery.DeliveryOutboxService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  DeliveryOutboxServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.delivery.DeliveryReceiptLog

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryReceiptLog.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps DeliveryReceiptLog assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DeliveryReceiptLogTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.delivery.DeliveryReceiptRequest

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryReceiptRequest.java

target module:
  canvas-platform

target role:
  domain model or service

coordinator decision:
  coordinator keeps DeliveryReceiptRequest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DeliveryReceiptRequestTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.delivery.DeliveryReconciliationJob

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryReconciliationJob.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  DeliveryReconciliationJobTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.delivery.ReachDeliveryService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  ReachDeliveryServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.disruptor.CanvasDisruptorService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java

target module:
  canvas-context-canvas

target role:
  application service

owning worker:
  DDD-W07

required tests:
  CanvasDisruptorServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.disruptor.CanvasExecutionEvent

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasExecutionEvent.java

target module:
  canvas-context-execution

target role:
  adapter.messaging

owning worker:
  DDD-W08

required tests:
  CanvasExecutionEventTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.expression.ExpressionEngine

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression/ExpressionEngine.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  ExpressionEngineTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.expression.GroovyExpressionEngine

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression/GroovyExpressionEngine.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  GroovyExpressionEngineTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handler.HandlerRegistry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/HandlerRegistry.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  HandlerRegistryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handler.NodeHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  NodeHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handler.NodeHandlerType

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandlerType.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  NodeHandlerTypeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handler.NodeOutcome

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeOutcome.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  NodeOutcomeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handler.NodeResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeResult.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  NodeResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handler.NodeRouteResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeRouteResolver.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  NodeRouteResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.AbstractSendMessageHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  AbstractSendMessageHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.AggregateHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AggregateHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  AggregateHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.AiLlmHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiLlmHandler.java

target module:
  canvas-context-execution

target role:
  domain service

coordinator decision:
  coordinator keeps AiLlmHandler assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  AiLlmHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.ApiCallHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  ApiCallHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.ApiCallPayloadBuilder

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallPayloadBuilder.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  ApiCallPayloadBuilderTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.CommitActionHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/CommitActionHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  CommitActionHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.ConditionEvaluator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ConditionEvaluator.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  ConditionEvaluatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.ConnectedContentHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ConnectedContentHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  ConnectedContentHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.CouponHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/CouponHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  CouponHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.DirectCallHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/DirectCallHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  DirectCallHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.DirectReturnHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/DirectReturnHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  DirectReturnHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.EndHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/EndHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  EndHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.EventTriggerHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/EventTriggerHandler.java

target module:
  canvas-context-execution

target role:
  adapter.messaging

owning worker:
  DDD-W08

required tests:
  EventTriggerHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.GroovyHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  GroovyHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.GroovyScriptCache

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyScriptCache.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  GroovyScriptCacheTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.HubHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/HubHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  HubHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.IfConditionHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/IfConditionHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  IfConditionHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.ManualApprovalHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ManualApprovalHandler.java

target module:
  canvas-context-execution

target role:
  domain service

coordinator decision:
  coordinator keeps ManualApprovalHandler assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  ManualApprovalHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.MqTriggerHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/MqTriggerHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  MqTriggerHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.PointsOperationHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/PointsOperationHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  PointsOperationHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.RiskDecisionHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/RiskDecisionHandler.java

target module:
  canvas-context-risk

target role:
  domain model or service

owning worker:
  DDD-W02

required tests:
  RiskDecisionHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.ScheduledTriggerHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ScheduledTriggerHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  ScheduledTriggerHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.SendMessageHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMessageHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  SendMessageHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.SendMqHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMqHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  SendMqHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.SplitHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SplitHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  SplitHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.StartHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/StartHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  StartHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.SubFlowRefHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SubFlowRefHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  SubFlowRefHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.TaggerHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TaggerHandler.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  TaggerHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.ThresholdHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ThresholdHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  ThresholdHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.TransferJourneyHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TransferJourneyHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  TransferJourneyHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.UserInputHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/UserInputHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  UserInputHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.WaitHandler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/WaitHandler.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  WaitHandlerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.handlers.WeightedChoice

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/WeightedChoice.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  WeightedChoiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.idempotency.NodeSideEffectIdempotencyService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/idempotency/NodeSideEffectIdempotencyService.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  NodeSideEffectIdempotencyServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.idempotency.NodeSideEffectRecord

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/idempotency/NodeSideEffectRecord.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  NodeSideEffectRecordTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.insights.MauticInspiredInsightService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/insights/MauticInspiredInsightService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MauticInspiredInsightServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.lane.DagCostProfiler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/DagCostProfiler.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  DagCostProfilerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.lane.ExecutionLane

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLane.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  ExecutionLaneTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.lane.ExecutionLaneAdmissionResult

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneAdmissionResult.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  ExecutionLaneAdmissionResultTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.lane.ExecutionLaneResolver

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  ExecutionLaneResolverTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.lane.ExecutionLaneWorkerRegistry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneWorkerRegistry.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  ExecutionLaneWorkerRegistryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.lifecycle.ExecutionLifecycleException

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lifecycle/ExecutionLifecycleException.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  ExecutionLifecycleExceptionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.lifecycle.ExecutionLifecycleGate

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lifecycle/ExecutionLifecycleGate.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  ExecutionLifecycleGateTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.llm.AiLlmGateway

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/AiLlmGateway.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps AiLlmGateway assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiLlmGatewayTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.llm.AiUsageAuditService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/AiUsageAuditService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps AiUsageAuditService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiUsageAuditServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.llm.LlmClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmClient.java

target module:
  canvas-platform

target role:
  adapter.external

owning worker:
  DDD-W01

required tests:
  LlmClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.llm.LlmInvalidJsonException

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmInvalidJsonException.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  LlmInvalidJsonExceptionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.llm.LlmProviderType

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmProviderType.java

target module:
  canvas-platform

target role:
  adapter.external

owning worker:
  DDD-W01

required tests:
  LlmProviderTypeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.llm.OpenAiCompatibleLlmClient

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/OpenAiCompatibleLlmClient.java

target module:
  canvas-platform

target role:
  adapter.external

coordinator decision:
  coordinator keeps OpenAiCompatibleLlmClient assigned to canvas-platform until the owning task pack accepts the row

required tests:
  OpenAiCompatibleLlmClientTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.plugin.JdbcPluginRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/JdbcPluginRepository.java

target module:
  canvas-platform

target role:
  adapter.persistence

coordinator decision:
  coordinator keeps JdbcPluginRepository assigned to canvas-platform until the owning task pack accepts the row

required tests:
  JdbcPluginRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.plugin.PluginRegistryService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java

target module:
  canvas-platform

target role:
  application service

coordinator decision:
  coordinator keeps PluginRegistryService assigned to canvas-platform until the owning task pack accepts the row

required tests:
  PluginRegistryServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.policy.ContactabilityExplainerService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/ContactabilityExplainerService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  ContactabilityExplainerServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.policy.MarketingPolicyService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingPolicyServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.reactive.BackgroundSubscriptionRegistry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/reactive/BackgroundSubscriptionRegistry.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  BackgroundSubscriptionRegistryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.request.AdaptiveRetryBackoffPolicy

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/AdaptiveRetryBackoffPolicy.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  AdaptiveRetryBackoffPolicyTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.request.CanvasExecutionReplayRateLimiter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionReplayRateLimiter.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  CanvasExecutionReplayRateLimiterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.request.CanvasExecutionRequestBacklogMetrics

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  CanvasExecutionRequestBacklogMetricsTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.request.CanvasExecutionRequestDispatcher

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestDispatcher.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestDispatcherTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.request.CanvasExecutionRequestExecutor

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutor.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestExecutorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.request.CanvasExecutionRequestPropertiesValidator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestPropertiesValidator.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestPropertiesValidatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.request.CanvasExecutionRequestService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestService.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.request.CanvasExecutionRequestStatus

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestStatus.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestStatusTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.request.CanvasMetricsRetryPressureSource

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasMetricsRetryPressureSource.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  CanvasMetricsRetryPressureSourceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.request.ExecutionRequestRetryPressureSource

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/ExecutionRequestRetryPressureSource.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  ExecutionRequestRetryPressureSourceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.AudienceDefinitionRuleValidator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/AudienceDefinitionRuleValidator.java

target module:
  canvas-context-cdp

target role:
  domain model or service

owning worker:
  DDD-W04

required tests:
  AudienceDefinitionRuleValidatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.CanvasRuleGraphValidator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/CanvasRuleGraphValidator.java

target module:
  canvas-context-canvas

target role:
  domain model or service

owning worker:
  DDD-W07

required tests:
  CanvasRuleGraphValidatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleAstEvaluator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleAstEvaluator.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleAstEvaluatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleCondition

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleCondition.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleConditionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleConfiguration

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleConfiguration.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleConfigurationTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleGroup

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleGroup.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleGroupTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleLogic

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleLogic.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleLogicTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleNode

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleNode.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  RuleNodeTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleOperator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleOperator.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleOperatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleParser

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleParser.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleParserTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleSqlCompiler

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleSqlCompiler.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleSqlCompilerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleValidationException

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleValidationException.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleValidationExceptionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleValidationOptions

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleValidationOptions.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleValidationOptionsTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.rule.RuleValidator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/rule/RuleValidator.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  RuleValidatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.schedule.LocalTaskScheduleRegistrar

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/schedule/LocalTaskScheduleRegistrar.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  LocalTaskScheduleRegistrarTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.schedule.ScheduleKey

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/schedule/ScheduleKey.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  ScheduleKeyTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.schedule.ScheduleRegistrar

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/schedule/ScheduleRegistrar.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  ScheduleRegistrarTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.schedule.ScheduleRegistration

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/schedule/ScheduleRegistration.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  ScheduleRegistrationTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.CanvasMetrics

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java

target module:
  canvas-context-bi

target role:
  domain model or service

owning worker:
  DDD-W05

required tests:
  CanvasMetricsTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.CircuitBreakerRegistry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistry.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  CircuitBreakerRegistryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.CircuitBreakerStateListener

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CircuitBreakerStateListener.java

target module:
  canvas-platform

target role:
  adapter.messaging

owning worker:
  DDD-W01

required tests:
  CircuitBreakerStateListenerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.DagEngine

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java

target module:
  canvas-context-execution

target role:
  domain service

owning worker:
  DDD-W08

required tests:
  DagEngineTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.ExecutionDlqWriter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/ExecutionDlqWriter.java

target module:
  canvas-context-execution

target role:
  domain model or service

coordinator decision:
  coordinator keeps ExecutionDlqWriter assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  ExecutionDlqWriterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.NodeGateCoordinator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeGateCoordinator.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  NodeGateCoordinatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.NodeResultRouter

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeResultRouter.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  NodeResultRouterTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.NodeStatePersistenceException

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeStatePersistenceException.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  NodeStatePersistenceExceptionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.NodeTimeoutCoordinator

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/NodeTimeoutCoordinator.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  NodeTimeoutCoordinatorTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.SpecialNodeTimeoutFailureException

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/SpecialNodeTimeoutFailureException.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  SpecialNodeTimeoutFailureExceptionTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.SpecialNodeTimeoutPoller

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/SpecialNodeTimeoutPoller.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  SpecialNodeTimeoutPollerTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.scheduler.TraceWriteBuffer

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  TraceWriteBufferTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.template.TemplateRenderService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/TemplateRenderService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  TemplateRenderServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.trace.ExecutionTraceContext

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trace/ExecutionTraceContext.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  ExecutionTraceContextTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.trigger.CanvasExecutionConfigLoader

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionConfigLoader.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  CanvasExecutionConfigLoaderTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.trigger.CanvasExecutionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  CanvasExecutionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.trigger.CanvasSchedulerService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  CanvasSchedulerServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.trigger.ExecutionLaneDispatcher

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ExecutionLaneDispatcher.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  ExecutionLaneDispatcherTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.trigger.ExecutionLifecycleGate

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ExecutionLifecycleGate.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  ExecutionLifecycleGateTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.trigger.ExecutionWatchdog

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ExecutionWatchdog.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  ExecutionWatchdogTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.trigger.InFlightExecutionRegistry

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java

target module:
  canvas-context-execution

target role:
  domain model or service

owning worker:
  DDD-W08

required tests:
  InFlightExecutionRegistryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.trigger.TriggerAdmissionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerAdmissionService.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  TriggerAdmissionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.trigger.TriggerPreCheckService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  TriggerPreCheckServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.trigger.TriggerPriorityConfig

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPriorityConfig.java

target module:
  canvas-context-execution

target role:
  config

owning worker:
  DDD-W08

required tests:
  TriggerPriorityConfigTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.wait.WaitResumeService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitResumeService.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  WaitResumeServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.engine.wait.WaitSubscriptionService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitSubscriptionService.java

target module:
  canvas-context-execution

target role:
  application service

owning worker:
  DDD-W08

required tests:
  WaitSubscriptionServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.platform.JdbcMarketingPlatformControlPlaneEvidenceProvider

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform/JdbcMarketingPlatformControlPlaneEvidenceProvider.java

target module:
  canvas-context-marketing

target role:
  adapter.persistence

owning worker:
  DDD-W03

required tests:
  JdbcMarketingPlatformControlPlaneEvidenceProviderTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.platform.JdbcPlatformWorkstreamRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform/JdbcPlatformWorkstreamRepository.java

target module:
  canvas-platform

target role:
  adapter.persistence

owning worker:
  DDD-W01

required tests:
  JdbcPlatformWorkstreamRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.platform.MarketingPlatformControlPlaneEvidenceProvider

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform/MarketingPlatformControlPlaneEvidenceProvider.java

target module:
  canvas-context-marketing

target role:
  adapter.external

owning worker:
  DDD-W03

required tests:
  MarketingPlatformControlPlaneEvidenceProviderTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.platform.MarketingPlatformControlPlaneService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform/MarketingPlatformControlPlaneService.java

target module:
  canvas-context-marketing

target role:
  application service

owning worker:
  DDD-W03

required tests:
  MarketingPlatformControlPlaneServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.platform.PlatformWorkstreamService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform/PlatformWorkstreamService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  PlatformWorkstreamServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.architecture.JdbcTechnicalMigrationCandidateRepository

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/JdbcTechnicalMigrationCandidateRepository.java

target module:
  canvas-platform

target role:
  adapter.persistence

owning worker:
  DDD-W01

required tests:
  JdbcTechnicalMigrationCandidateRepositoryTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.architecture.TechnicalMigrationCandidateEvidenceRecord

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateEvidenceRecord.java

target module:
  canvas-platform

target role:
  domain model or service

owning worker:
  DDD-W01

required tests:
  TechnicalMigrationCandidateEvidenceRecordTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.architecture.TechnicalMigrationCandidateService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  TechnicalMigrationCandidateServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts

old class:
  org.chovy.canvas.strategy.architecture.ArchitectureDeploymentEvidenceService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture/ArchitectureDeploymentEvidenceService.java

target module:
  canvas-platform

target role:
  application service

owning worker:
  DDD-W01

required tests:
  ArchitectureDeploymentEvidenceServiceTest

compatibility notes:
  preserve public method behavior, transaction boundaries, side effects, and existing integration contracts
