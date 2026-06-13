# Persistence Ownership Inventory

Exact data object and mapper ownership rows for DDD context workers.

Generated on 2026-06-09 from the current `backend/canvas-engine` source tree and `context-ownership-seed.md`.
old class:
  org.chovy.canvas.dal.dataobject.AbExperimentAllocationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AbExperimentAllocationDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentAllocationDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentAllocationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AbExperimentDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AbExperimentDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AbExperimentGovernanceDecisionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AbExperimentGovernanceDecisionDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentGovernanceDecisionDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentGovernanceDecisionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AbExperimentGroupDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AbExperimentGroupDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentGroupDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentGroupPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AbExperimentLayerDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AbExperimentLayerDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentLayerDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentLayerPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AbExperimentMetricDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AbExperimentMetricDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentMetricDO assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  AbExperimentMetricPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AbExperimentMetricSnapshotDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AbExperimentMetricSnapshotDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentMetricSnapshotDO assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  AbExperimentMetricSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AiDecisionFeedbackDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiDecisionFeedbackDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiDecisionFeedbackDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionFeedbackPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AiDecisionRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiDecisionRunDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiDecisionRunDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AiModelRegistryDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiModelRegistryDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiModelRegistryDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiModelRegistryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AiPredictionRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiPredictionRunDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiPredictionRunDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiPredictionRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AiPromptTemplateDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiPromptTemplateDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiPromptTemplateDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiPromptTemplatePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AiProviderDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiProviderDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiProviderDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiProviderPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AiUsageAuditDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiUsageAuditDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiUsageAuditDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiUsageAuditPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AiUserDecisionRecommendationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiUserDecisionRecommendationDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiUserDecisionRecommendationDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiUserDecisionRecommendationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AiUserPredictionSnapshotDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiUserPredictionSnapshotDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiUserPredictionSnapshotDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiUserPredictionSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AnalyticsAlertRuleDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsAlertRuleDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsAlertRuleDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsAlertRulePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AnalyticsEventDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsEventDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsEventDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AnalyticsEventTraceDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsEventTraceDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsEventTraceDO assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  AnalyticsEventTracePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AnalyticsExportJobDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsExportJobDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsExportJobDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsExportJobPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AnalyticsFunnelDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsFunnelDefinitionDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsFunnelDefinitionDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsFunnelDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AnalyticsRetentionPolicyDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsRetentionPolicyDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsRetentionPolicyDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsRetentionPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AnalyticsRetentionRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsRetentionRunDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsRetentionRunDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsRetentionRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ApiDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApiDefinitionDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApiDefinitionDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApiDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ApprovalAuditEventDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApprovalAuditEventDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApprovalAuditEventDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalAuditEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ApprovalDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApprovalDefinitionDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApprovalDefinitionDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ApprovalInstanceDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApprovalInstanceDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApprovalInstanceDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalInstancePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ApprovalLarkUserIdentityDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApprovalLarkUserIdentityDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApprovalLarkUserIdentityDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalLarkUserIdentityPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ApprovalTaskDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApprovalTaskDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApprovalTaskDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalTaskPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AsyncTaskDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AsyncTaskDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AsyncTaskDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AsyncTaskPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AsyncTaskSubscriptionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AsyncTaskSubscriptionDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AsyncTaskSubscriptionDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AsyncTaskSubscriptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AudienceBitmapRollbackDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceBitmapRollbackDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceBitmapRollbackPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AudienceBitmapVersionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceBitmapVersionDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceBitmapVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AudienceComputeRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceComputeRunDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceComputeRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AudienceDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceDefinitionDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AudienceMaterializationRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceMaterializationRunDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceMaterializationRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AudienceQualityCheckDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceQualityCheckDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceQualityCheckPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AudienceSnapshotDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceSnapshotDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.AudienceStatDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceStatDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceStatPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiAlertRuleDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiAlertRuleDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiAlertRulePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiAuditLogDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiAuditLogDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiAuditLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiBigScreenDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiBigScreenDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiBigScreenPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiBigScreenVersionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiBigScreenVersionDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiBigScreenVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiChartDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiChartDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiChartPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiChartVersionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiChartVersionDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiChartVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiColumnPermissionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiColumnPermissionDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiColumnPermissionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDashboardDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDashboardDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDashboardPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDashboardRuntimeStateDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDashboardRuntimeStateDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDashboardRuntimeStatePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDashboardVersionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDashboardVersionDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDashboardVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDashboardWidgetDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDashboardWidgetDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDashboardWidgetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDatasetAccelerationPolicyDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDatasetAccelerationPolicyDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasetAccelerationPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDatasetDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDatasetDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDatasetExtractRefreshRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDatasetExtractRefreshRunDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasetExtractRefreshRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDatasetFieldDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDatasetFieldDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasetFieldPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDatasetVersionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDatasetVersionDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasetVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDatasourceHealthSnapshotDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDatasourceHealthSnapshotDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasourceHealthSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDatasourceSchemaSnapshotDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDatasourceSchemaSnapshotDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasourceSchemaSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDeliveryAttachmentDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDeliveryAttachmentDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDeliveryAttachmentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDeliveryLogDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDeliveryLogDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDeliveryLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiDeliverySchedulerLeaseDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiDeliverySchedulerLeaseDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDeliverySchedulerLeasePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiEmbedTokenDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiEmbedTokenDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiEmbedTokenPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiExportJobDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiExportJobDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiExportJobPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiMetricDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiMetricDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiMetricPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiPermissionRequestDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiPermissionRequestDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiPermissionRequestPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiPortalDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiPortalDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiPortalPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiPortalMenuDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiPortalMenuDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiPortalMenuPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiPortalVersionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiPortalVersionDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiPortalVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiPublishApprovalDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiPublishApprovalDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

coordinator decision:
  coordinator keeps BiPublishApprovalDO assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiPublishApprovalPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiQueryCachePolicyDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiQueryCachePolicyDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiQueryCachePolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiQueryGovernancePolicyDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiQueryGovernancePolicyDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiQueryGovernancePolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiQuickEngineCapacityPolicyDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiQuickEngineCapacityPolicyDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiQuickEngineCapacityPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiQuickEngineQueueJobDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiQuickEngineQueueJobDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueJobPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiResourceCommentDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourceCommentDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourceCommentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiResourceFavoriteDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourceFavoriteDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourceFavoritePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiResourceLocationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourceLocationDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourceLocationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiResourceLockDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourceLockDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourceLockPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiResourceOwnershipDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourceOwnershipDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourceOwnershipPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiResourcePermissionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiResourcePermissionDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourcePermissionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiRowPermissionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiRowPermissionDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiRowPermissionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiSpreadsheetDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiSpreadsheetDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiSpreadsheetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiSpreadsheetVersionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiSpreadsheetVersionDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiSpreadsheetVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiSubscriptionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiSubscriptionDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiSubscriptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiWorkspaceDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiWorkspaceDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiWorkspacePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.BiWorkspaceMemberDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/BiWorkspaceMemberDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiWorkspaceMemberPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasAuditLogDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasAuditLogDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasAuditLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasControlGroupHoldoutDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasControlGroupHoldoutDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasControlGroupHoldoutPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasConversionAttributionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasConversionAttributionDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasConversionAttributionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasExecutionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasExecutionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionDlqDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

coordinator decision:
  coordinator keeps CanvasExecutionDlqDO assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  CanvasExecutionDlqPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionRequestDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasExecutionStatsDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionStatsDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasExecutionStatsPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionTraceDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasExecutionTracePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasManualApprovalDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

coordinator decision:
  coordinator keeps CanvasManualApprovalDO assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasManualApprovalPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasMqTriggerRejectedDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasMqTriggerRejectedDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasMqTriggerRejectedPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasProjectDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasProjectPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectFolderDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasProjectFolderPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasProjectMemberDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasProjectMemberDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasProjectMemberPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasTemplateDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasTemplateDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasTemplatePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasUserQuotaDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasUserQuotaDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasUserQuotaPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasVersionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasVersionDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CanvasWaitSubscriptionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasWaitSubscriptionDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasWaitSubscriptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpAudienceSnapshotDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpAudienceSnapshotDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpAudienceSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpComputedProfileAttributeDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpComputedProfileAttributeDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpComputedProfileAttributePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpComputedProfileRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpComputedProfileRunDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpComputedProfileRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpComputedTagDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpComputedTagDefinitionDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpComputedTagDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpComputedTagDependencyDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpComputedTagDependencyDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpComputedTagDependencyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpComputedTagRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpComputedTagRunDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpComputedTagRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpEventLogDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpEventLogDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpEventLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpProfileAttributeChangeLogDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpProfileAttributeChangeLogDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpProfileAttributeChangeLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpRealtimeAudienceEventLogDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpRealtimeAudienceEventLogDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpRealtimeAudienceEventLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpTagOperationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpTagOperationDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpTagOperationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpUserIdentityDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpUserIdentityDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpUserIdentityPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpUserIndexDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpUserIndexDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpUserIndexPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpUserProfileDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpUserProfileDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpUserProfilePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpUserTagDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpUserTagDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpUserTagPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpUserTagHistoryDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpUserTagHistoryDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpUserTagHistoryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseAssetAvailabilityDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseAssetAvailabilityDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAssetAvailabilityPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseConsumerAvailabilityContractDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseConsumerAvailabilityContractDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseConsumerAvailabilityContractPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseDatasetCatalogDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseDatasetCatalogDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  CdpWarehouseDatasetCatalogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseE2eCertificationRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseE2eCertificationRunDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseE2eCertificationRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapEvidenceCollectionRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseEnterpriseOlapEvidenceDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseEnterpriseOlapEvidenceDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapEvidencePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseExternalRealtimeJobProbeTargetDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseExternalRealtimeJobProbeTargetDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseExternalRealtimeJobProbeTargetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseFieldAccessAuditDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseFieldAccessAuditDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseFieldAccessAuditPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseFieldPolicyDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseFieldPolicyDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseFieldPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseIncidentDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseIncidentDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseIncidentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseJobLeaseDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseJobLeaseDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseJobLeasePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseLineageEdgeDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseLineageEdgeDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseLineageEdgePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseMetricChangeReviewDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseMetricChangeReviewDO.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  CdpWarehouseMetricChangeReviewPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyErasureAssetProofDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehousePrivacyErasureAssetProofDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyErasureAssetProofPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyErasureRequestDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehousePrivacyErasureRequestDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyErasureRequestPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehousePrivacySubjectTombstoneDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehousePrivacySubjectTombstoneDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacySubjectTombstonePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseQualityCheckDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseQualityCheckDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseQualityCheckPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeCheckpointDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseRealtimeCheckpointDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeCheckpointPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeRetryDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseRealtimeRetryDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeRetryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseSloPolicyDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseSloPolicyDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSloPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseStreamCheckpointDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseStreamCheckpointDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseStreamCheckpointPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseStreamJobActionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseStreamJobActionDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseStreamJobActionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseStreamJobInstanceDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseStreamJobInstanceDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseStreamJobInstancePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseStreamPipelineDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseStreamPipelineDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseStreamPipelinePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseStreamSchemaDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseStreamSchemaDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseStreamSchemaPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseSyncRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseSyncRunDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSyncRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseSyntheticDataPathProbeRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseSyntheticDataPathProbeRunDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSyntheticDataPathProbeRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseTableContractDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseTableContractDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseTableContractPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseTableInspectionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseTableInspectionDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseTableInspectionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWarehouseWatermarkDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseWatermarkDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseWatermarkPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CdpWriteKeyDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWriteKeyDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWriteKeyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ChannelConnectorDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ChannelConnectorDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ChannelConnectorDO assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ChannelConnectorPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ChannelDedupeRecordDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ChannelDedupeRecordDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelDedupeRecordPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ChannelFallbackDecisionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ChannelFallbackDecisionDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelFallbackDecisionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ChannelFallbackPolicyDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ChannelFallbackPolicyDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelFallbackPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ChannelProviderLimitDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ChannelProviderLimitDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelProviderLimitPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConnectedContentCacheDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConnectedContentCacheDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  ConnectedContentCachePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ContextFieldDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ContextFieldDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  ContextFieldPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationAiReplySuggestionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationAiReplySuggestionDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

coordinator decision:
  coordinator keeps ConversationAiReplySuggestionDO assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplySuggestionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationContactProfileDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationContactProfileDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationContactProfilePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationMessageDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationMessageDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationMessagePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationPrivateContactDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationPrivateContactDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationPrivateContactPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationPrivateContactOwnerDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationPrivateContactOwnerDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationPrivateContactOwnerPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationPrivateGroupDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationPrivateGroupDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationPrivateGroupPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationPrivateGroupMemberDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationPrivateGroupMemberDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationPrivateGroupMemberPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationPrivateSyncRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationPrivateSyncRunDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationPrivateSyncRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationRoutingAgentDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationRoutingAgentDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationRoutingAgentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationRoutingRuleDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationRoutingRuleDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationRoutingRulePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationSessionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationSessionDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationSessionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationSlaBreachDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationSlaBreachDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationSlaBreachPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationSopTaskDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationSopTaskDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationSopTaskPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationWorkItemAuditDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationWorkItemAuditDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationWorkItemAuditPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ConversationWorkItemDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ConversationWorkItemDO.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationWorkItemPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CreatorCampaignDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CreatorCampaignDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps CreatorCampaignDO assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  CreatorCampaignPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CreatorCollaborationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CreatorCollaborationDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps CreatorCollaborationDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorCollaborationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CreatorDeliverableDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CreatorDeliverableDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps CreatorDeliverableDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorDeliverablePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CreatorProfileDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CreatorProfileDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps CreatorProfileDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProfilePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CreatorProviderMutationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CreatorProviderMutationDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps CreatorProviderMutationDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CustomerChannelDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CustomerChannelDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  CustomerChannelPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CustomerPointsLedgerDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CustomerPointsLedgerDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  CustomerPointsLedgerPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CustomerProfileDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CustomerProfileDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CustomerProfilePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CustomerTagDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CustomerTagDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CustomerTagPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.CustomerTaskRecordDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CustomerTaskRecordDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  CustomerTaskRecordPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.DataSourceConfigDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps DataSourceConfigDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DataSourceConfigPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.EventAttrDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventAttrDefinitionDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  EventAttrDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.EventDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventDefinitionDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps EventDefinitionDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  EventDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.EventLogDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventLogDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  EventLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ExecutionRerunAuditDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ExecutionRerunAuditDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  ExecutionRerunAuditPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.GrowthActivityDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/GrowthActivityDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthActivityPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.GrowthActivityEventDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/GrowthActivityEventDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthActivityEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.GrowthActivityParticipantDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/GrowthActivityParticipantDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthActivityParticipantPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.GrowthActivityRuleSetDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/GrowthActivityRuleSetDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthActivityRuleSetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.GrowthReferralCodeDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/GrowthReferralCodeDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthReferralCodePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.GrowthReferralRelationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/GrowthReferralRelationDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthReferralRelationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/GrowthRewardGrantDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthRewardGrantPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/GrowthRewardPoolDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthRewardPoolPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.GrowthTaskDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/GrowthTaskDefinitionDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthTaskDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.GrowthTaskProgressDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/GrowthTaskProgressDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthTaskProgressPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.IdentityTypeDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/IdentityTypeDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  IdentityTypePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.LoyaltyMemberAccountDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/LoyaltyMemberAccountDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps LoyaltyMemberAccountDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LoyaltyMemberAccountPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.LoyaltyRedemptionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/LoyaltyRedemptionDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps LoyaltyRedemptionDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LoyaltyRedemptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.LoyaltyRuleDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/LoyaltyRuleDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps LoyaltyRuleDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LoyaltyRulePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.LoyaltyTransactionJournalDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/LoyaltyTransactionJournalDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps LoyaltyTransactionJournalDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LoyaltyTransactionJournalPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingAssetDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingAssetDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingAssetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingAssetFolderDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingAssetFolderDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  MarketingAssetFolderPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingAssetUploadIntentDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingAssetUploadIntentDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingAssetUploadIntentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingCampaignLinkDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingCampaignLinkDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingCampaignLinkPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingCampaignMasterDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingCampaignMasterDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingCampaignMasterPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingCompetitorMentionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingCompetitorMentionDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingCompetitorMentionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingConsentDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingConsentDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingConsentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingContentAuditEventDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentAuditEventDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingContentAuditEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingContentEntryDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentEntryDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingContentEntryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingContentEntryVersionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentEntryVersionDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  MarketingContentEntryVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingContentReleaseDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentReleaseDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingContentReleasePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingContentReleaseItemDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentReleaseItemDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingContentReleaseItemPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingContentTemplateDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentTemplateDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingContentTemplatePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingContentTemplateVersionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentTemplateVersionDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  MarketingContentTemplateVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingFormDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingFormDefinitionDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingFormDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingFormSubmissionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingFormSubmissionDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingFormSubmissionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingIntegrationContractAuditEventDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingIntegrationContractAuditEventDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractAuditEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingIntegrationContractDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeObservationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingIntegrationContractProbeObservationDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeObservationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingIntegrationContractProbeRunDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeWindowStatsDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingIntegrationContractProbeWindowStatsDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeWindowStatsPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorAlertChannelDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorAlertChannelDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertChannelPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorAlertDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDeliveryDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorAlertDeliveryDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertDeliveryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorAnomalyEventDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorAnomalyEventDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorAnomalyRuleDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorAnomalyRuleDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyRulePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorInferenceDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorInferenceDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorInferencePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorItemDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorItemDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorItemPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorPollRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorPollRunDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorProviderCredentialDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialEventDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorProviderCredentialEventDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorProviderOAuthAuthorizationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorProviderOAuthAuthorizationDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationDO assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorProviderOAuthAuthorizationEventDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorProviderOAuthAuthorizationEventDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationEventDO assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorSourceDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorSourcePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingMonitorTrendSnapshotDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorTrendSnapshotDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorTrendSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingSentimentAnalysisDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingSentimentAnalysisDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingSentimentAnalysisPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MarketingSuppressionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingSuppressionDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingSuppressionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MessageSendRecordDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MessageSendRecordDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MessageSendRecordPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MqMessageDefinitionDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  MqMessageDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.NodeTypeRegistryDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/NodeTypeRegistryDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  NodeTypeRegistryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.NotificationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/NotificationDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps NotificationDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  NotificationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.PaidMediaAudienceDestinationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/PaidMediaAudienceDestinationDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceDestinationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.PaidMediaAudienceMemberDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/PaidMediaAudienceMemberDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceMemberPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.PaidMediaAudienceSyncRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/PaidMediaAudienceSyncRunDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceSyncRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ProgrammaticDspCampaignDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProgrammaticDspCampaignDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspCampaignDO assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspCampaignPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ProgrammaticDspLineItemDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProgrammaticDspLineItemDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspLineItemDO assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspLineItemPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ProgrammaticDspMutationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProgrammaticDspMutationDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspMutationDO assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ProgrammaticDspPerformanceSnapshotDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProgrammaticDspPerformanceSnapshotDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspPerformanceSnapshotDO assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspPerformanceSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ProgrammaticDspSeatDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProgrammaticDspSeatDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspSeatDO assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSeatPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.ProgrammaticDspSupplyPathDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProgrammaticDspSupplyPathDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspSupplyPathDO assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSupplyPathPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.RiskDecisionRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/RiskDecisionRunDO.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskDecisionRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.RiskListDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/RiskListDO.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskListPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.RiskListEntryDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/RiskListEntryDO.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskListEntryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.RiskRuleHitDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/RiskRuleHitDO.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskRuleHitPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.RiskSceneDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/RiskSceneDO.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskScenePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.RiskSimulationRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/RiskSimulationRunDO.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskSimulationRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.RiskStrategyDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/RiskStrategyDO.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskStrategyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.RiskStrategyVersionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/RiskStrategyVersionDO.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskStrategyVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.SearchMarketingImpactWindowDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingImpactWindowDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingImpactWindowPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.SearchMarketingKeywordDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingKeywordDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingKeywordPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingMutationDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingMutationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.SearchMarketingOpportunityDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingOpportunityDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingOpportunityPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.SearchMarketingProviderChangeDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingProviderChangeDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderChangePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.SearchMarketingSnapshotDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingSnapshotDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingSourceDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingSourcePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.SearchMarketingSyncRunDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingSyncRunDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingSyncRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.SearchMarketingUrlInspectionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingUrlInspectionDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingUrlInspectionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.SysUserDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SysUserDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps SysUserDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  SysUserPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.SystemOptionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SystemOptionDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps SystemOptionDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  SystemOptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.TagDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TagDefinitionDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  TagDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.TagImportBatchDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TagImportBatchDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  TagImportBatchPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.TagImportErrorDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TagImportErrorDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  TagImportErrorPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.TagImportSourceDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TagImportSourceDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  TagImportSourcePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.TagValueDefinitionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TagValueDefinitionDO.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  TagValueDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.TenantDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TenantDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps TenantDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  TenantPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.TestUserDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TestUserDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  TestUserPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.TestUserSetDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TestUserSetDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  TestUserSetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.UserInputFormDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/UserInputFormDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  UserInputFormPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.UserInputResponseDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/UserInputResponseDO.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  UserInputResponsePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.UserInputResumeAuditDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/UserInputResumeAuditDO.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  UserInputResumeAuditPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.UserWorkspacePreferenceDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/UserWorkspacePreferenceDO.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  UserWorkspacePreferencePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.WebhookDeliveryLogDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookDeliveryLogDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  WebhookDeliveryLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.dataobject.WebhookSubscriptionDO

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookSubscriptionDO.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps WebhookSubscriptionDO assigned to canvas-platform until the owning task pack accepts the row

required tests:
  WebhookSubscriptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AbExperimentAllocationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AbExperimentAllocationMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentAllocationMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentAllocationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AbExperimentGovernanceDecisionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AbExperimentGovernanceDecisionMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentGovernanceDecisionMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentGovernanceDecisionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AbExperimentGroupMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AbExperimentGroupMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentGroupMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentGroupPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AbExperimentLayerMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AbExperimentLayerMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentLayerMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentLayerPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AbExperimentMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AbExperimentMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AbExperimentMetricMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AbExperimentMetricMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentMetricMapper assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  AbExperimentMetricPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AbExperimentMetricSnapshotMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AbExperimentMetricSnapshotMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

coordinator decision:
  coordinator keeps AbExperimentMetricSnapshotMapper assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  AbExperimentMetricSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AiDecisionFeedbackMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiDecisionFeedbackMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiDecisionFeedbackMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionFeedbackPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AiDecisionRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiDecisionRunMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiDecisionRunMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AiModelRegistryMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiModelRegistryMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiModelRegistryMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiModelRegistryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AiPredictionRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiPredictionRunMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiPredictionRunMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiPredictionRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AiPromptTemplateMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiPromptTemplateMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiPromptTemplateMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiPromptTemplatePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AiProviderMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiProviderMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiProviderMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiProviderPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AiUsageAuditMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiUsageAuditMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiUsageAuditMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiUsageAuditPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AiUserDecisionRecommendationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiUserDecisionRecommendationMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiUserDecisionRecommendationMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiUserDecisionRecommendationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AiUserPredictionSnapshotMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiUserPredictionSnapshotMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AiUserPredictionSnapshotMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiUserPredictionSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AnalyticsAlertRuleMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsAlertRuleMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsAlertRuleMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsAlertRulePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AnalyticsEventMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsEventMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsEventMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AnalyticsEventTraceMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsEventTraceMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsEventTraceMapper assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  AnalyticsEventTracePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AnalyticsExportJobMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsExportJobMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsExportJobMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsExportJobPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AnalyticsFunnelDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsFunnelDefinitionMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsFunnelDefinitionMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsFunnelDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AnalyticsRetentionPolicyMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsRetentionPolicyMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsRetentionPolicyMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsRetentionPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AnalyticsRetentionRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsRetentionRunMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AnalyticsRetentionRunMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsRetentionRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ApiDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApiDefinitionMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApiDefinitionMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApiDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ApprovalAuditEventMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApprovalAuditEventMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApprovalAuditEventMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalAuditEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ApprovalDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApprovalDefinitionMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApprovalDefinitionMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ApprovalInstanceMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApprovalInstanceMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApprovalInstanceMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalInstancePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ApprovalLarkUserIdentityMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApprovalLarkUserIdentityMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApprovalLarkUserIdentityMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalLarkUserIdentityPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ApprovalTaskMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApprovalTaskMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps ApprovalTaskMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalTaskPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AsyncTaskMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AsyncTaskMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AsyncTaskMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AsyncTaskPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AsyncTaskSubscriptionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AsyncTaskSubscriptionMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps AsyncTaskSubscriptionMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AsyncTaskSubscriptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AudienceBitmapRollbackMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceBitmapRollbackMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceBitmapRollbackPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AudienceBitmapVersionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceBitmapVersionMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceBitmapVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AudienceComputeRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceComputeRunMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceComputeRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AudienceDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceDefinitionMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AudienceMaterializationRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceMaterializationRunMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceMaterializationRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AudienceQualityCheckMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceQualityCheckMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceQualityCheckPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AudienceSnapshotMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceSnapshotMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.AudienceStatMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceStatMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  AudienceStatPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiAlertRuleMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiAlertRuleMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiAlertRulePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiAuditLogMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiAuditLogMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiAuditLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiBigScreenMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiBigScreenMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiBigScreenPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiBigScreenVersionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiBigScreenVersionMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiBigScreenVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiChartMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiChartMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiChartPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiChartVersionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiChartVersionMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiChartVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiColumnPermissionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiColumnPermissionMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiColumnPermissionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDashboardMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDashboardMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDashboardPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDashboardRuntimeStateMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDashboardRuntimeStateMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDashboardRuntimeStatePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDashboardVersionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDashboardVersionMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDashboardVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDashboardWidgetMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDashboardWidgetMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDashboardWidgetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDatasetAccelerationPolicyMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDatasetAccelerationPolicyMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasetAccelerationPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDatasetExtractRefreshRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDatasetExtractRefreshRunMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasetExtractRefreshRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDatasetFieldMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDatasetFieldMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasetFieldPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDatasetMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDatasetMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDatasetVersionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDatasetVersionMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasetVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDatasourceHealthSnapshotMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDatasourceHealthSnapshotMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasourceHealthSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDatasourceSchemaSnapshotMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDatasourceSchemaSnapshotMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDatasourceSchemaSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDeliveryAttachmentMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDeliveryAttachmentMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDeliveryAttachmentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDeliveryLogMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDeliveryLogMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDeliveryLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiDeliverySchedulerLeaseMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiDeliverySchedulerLeaseMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiDeliverySchedulerLeasePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiEmbedTokenMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiEmbedTokenMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiEmbedTokenPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiExportJobMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiExportJobMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiExportJobPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiMetricMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiMetricMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiMetricPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiPermissionRequestMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiPermissionRequestMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiPermissionRequestPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiPortalMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiPortalMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiPortalPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiPortalMenuMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiPortalMenuMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiPortalMenuPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiPortalVersionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiPortalVersionMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiPortalVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiPublishApprovalMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiPublishApprovalMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

coordinator decision:
  coordinator keeps BiPublishApprovalMapper assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiPublishApprovalPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiQueryCachePolicyMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiQueryCachePolicyMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiQueryCachePolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiQueryGovernancePolicyMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiQueryGovernancePolicyMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiQueryGovernancePolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiQuickEngineCapacityPolicyMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiQuickEngineCapacityPolicyMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiQuickEngineCapacityPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiQuickEngineQueueJobMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiQuickEngineQueueJobMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueJobPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiResourceCommentMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourceCommentMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourceCommentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiResourceFavoriteMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourceFavoriteMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourceFavoritePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiResourceLocationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourceLocationMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourceLocationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiResourceLockMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourceLockMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourceLockPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiResourceOwnershipMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourceOwnershipMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourceOwnershipPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiResourcePermissionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiResourcePermissionMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiResourcePermissionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiRowPermissionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiRowPermissionMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiRowPermissionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiSpreadsheetMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiSpreadsheetMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiSpreadsheetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiSpreadsheetVersionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiSpreadsheetVersionMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiSpreadsheetVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiSubscriptionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiSubscriptionMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiSubscriptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiWorkspaceMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiWorkspaceMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiWorkspacePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.BiWorkspaceMemberMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/BiWorkspaceMemberMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  BiWorkspaceMemberPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasAuditLogMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasAuditLogMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasAuditLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasControlGroupHoldoutMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasControlGroupHoldoutMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasControlGroupHoldoutPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasConversionAttributionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasConversionAttributionMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasConversionAttributionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasExecutionDlqMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

coordinator decision:
  coordinator keeps CanvasExecutionDlqMapper assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  CanvasExecutionDlqPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasExecutionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasExecutionMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasExecutionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasExecutionRequestMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasExecutionStatsMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasExecutionStatsPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasExecutionTraceMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasExecutionTracePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasManualApprovalMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

coordinator decision:
  coordinator keeps CanvasManualApprovalMapper assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasManualApprovalPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasMqTriggerRejectedMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasMqTriggerRejectedMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasMqTriggerRejectedPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasProjectFolderMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasProjectFolderPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasProjectMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasProjectMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasProjectPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasProjectMemberMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasProjectMemberMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasProjectMemberPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasTemplateMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasTemplateMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasTemplatePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasUserQuotaMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasUserQuotaMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasUserQuotaPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasVersionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasVersionMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  CanvasVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CanvasWaitSubscriptionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasWaitSubscriptionMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  CanvasWaitSubscriptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpAudienceSnapshotMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpAudienceSnapshotMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpAudienceSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpComputedProfileAttributeMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpComputedProfileAttributeMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpComputedProfileAttributePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpComputedProfileRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpComputedProfileRunMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpComputedProfileRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpComputedTagDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpComputedTagDefinitionMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpComputedTagDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpComputedTagDependencyMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpComputedTagDependencyMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpComputedTagDependencyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpComputedTagRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpComputedTagRunMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpComputedTagRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpEventLogMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpEventLogMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpEventLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpProfileAttributeChangeLogMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpProfileAttributeChangeLogMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpProfileAttributeChangeLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpRealtimeAudienceEventLogMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpRealtimeAudienceEventLogMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpRealtimeAudienceEventLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpTagOperationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpTagOperationMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpTagOperationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpUserIdentityMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpUserIdentityMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpUserIdentityPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpUserIndexMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpUserIndexMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpUserIndexPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpUserProfileMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpUserProfileMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpUserProfilePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpUserTagHistoryMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpUserTagHistoryMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpUserTagHistoryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpUserTagMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpUserTagMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpUserTagPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseAssetAvailabilityMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseAssetAvailabilityMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAssetAvailabilityPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseConsumerAvailabilityContractMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseConsumerAvailabilityContractMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseConsumerAvailabilityContractPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseDatasetCatalogMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseDatasetCatalogMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  CdpWarehouseDatasetCatalogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseE2eCertificationRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseE2eCertificationRunMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseE2eCertificationRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapEvidenceCollectionRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseEnterpriseOlapEvidenceMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseEnterpriseOlapEvidenceMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapEvidencePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseExternalRealtimeJobProbeTargetMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseExternalRealtimeJobProbeTargetMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseExternalRealtimeJobProbeTargetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseFieldAccessAuditMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseFieldAccessAuditMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseFieldAccessAuditPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseFieldPolicyMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseFieldPolicyMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseFieldPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseIncidentMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseIncidentMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseIncidentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseJobLeaseMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseJobLeaseMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseJobLeasePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseLineageEdgeMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseLineageEdgeMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseLineageEdgePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseMetricChangeReviewMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseMetricChangeReviewMapper.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.adapter.persistence

owning worker:
  DDD-W05

required tests:
  CdpWarehouseMetricChangeReviewPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehousePrivacyErasureAssetProofMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehousePrivacyErasureAssetProofMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyErasureAssetProofPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehousePrivacyErasureRequestMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehousePrivacyErasureRequestMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyErasureRequestPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehousePrivacySubjectTombstoneMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehousePrivacySubjectTombstoneMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacySubjectTombstonePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseQualityCheckMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseQualityCheckMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseQualityCheckPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeCheckpointMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseRealtimeCheckpointMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeCheckpointPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeRetryMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseRealtimeRetryMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeRetryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseSloPolicyMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseSloPolicyMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSloPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseStreamCheckpointMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseStreamCheckpointMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseStreamCheckpointPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseStreamJobActionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseStreamJobActionMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseStreamJobActionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseStreamJobInstanceMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseStreamJobInstanceMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseStreamJobInstancePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseStreamPipelineMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseStreamPipelineMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseStreamPipelinePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseStreamSchemaMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseStreamSchemaMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseStreamSchemaPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseSyncRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseSyncRunMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSyncRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseSyntheticDataPathProbeRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseSyntheticDataPathProbeRunMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSyntheticDataPathProbeRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseTableContractMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseTableContractMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseTableContractPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseTableInspectionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseTableInspectionMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseTableInspectionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWarehouseWatermarkMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseWatermarkMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWarehouseWatermarkPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CdpWriteKeyMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWriteKeyMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CdpWriteKeyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ChannelConnectorMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ChannelConnectorMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ChannelConnectorMapper assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ChannelConnectorPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ChannelDedupeRecordMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ChannelDedupeRecordMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelDedupeRecordPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ChannelFallbackDecisionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ChannelFallbackDecisionMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelFallbackDecisionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ChannelFallbackPolicyMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ChannelFallbackPolicyMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelFallbackPolicyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ChannelProviderLimitMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ChannelProviderLimitMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  ChannelProviderLimitPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConnectedContentCacheMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConnectedContentCacheMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  ConnectedContentCachePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ContextFieldMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ContextFieldMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  ContextFieldPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationAiReplySuggestionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationAiReplySuggestionMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

coordinator decision:
  coordinator keeps ConversationAiReplySuggestionMapper assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplySuggestionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationContactProfileMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationContactProfileMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationContactProfilePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationMessageMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationMessageMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationMessagePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationPrivateContactMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationPrivateContactMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationPrivateContactPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationPrivateContactOwnerMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationPrivateContactOwnerMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationPrivateContactOwnerPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationPrivateGroupMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationPrivateGroupMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationPrivateGroupPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationPrivateGroupMemberMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationPrivateGroupMemberMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationPrivateGroupMemberPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationPrivateSyncRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationPrivateSyncRunMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationPrivateSyncRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationRoutingAgentMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationRoutingAgentMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationRoutingAgentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationRoutingRuleMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationRoutingRuleMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationRoutingRulePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationSessionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationSessionMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationSessionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationSlaBreachMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationSlaBreachMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationSlaBreachPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationSopTaskMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationSopTaskMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationSopTaskPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationWorkItemAuditMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationWorkItemAuditMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationWorkItemAuditPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ConversationWorkItemMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ConversationWorkItemMapper.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.adapter.persistence

owning worker:
  DDD-W06

required tests:
  ConversationWorkItemPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CreatorCampaignMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CreatorCampaignMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps CreatorCampaignMapper assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  CreatorCampaignPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CreatorCollaborationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CreatorCollaborationMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps CreatorCollaborationMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorCollaborationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CreatorDeliverableMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CreatorDeliverableMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps CreatorDeliverableMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorDeliverablePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CreatorProfileMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CreatorProfileMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps CreatorProfileMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProfilePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CreatorProviderMutationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CreatorProviderMutationMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps CreatorProviderMutationMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CustomerChannelMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CustomerChannelMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  CustomerChannelPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CustomerPointsLedgerMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CustomerPointsLedgerMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  CustomerPointsLedgerPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CustomerProfileMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CustomerProfileMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CustomerProfilePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CustomerTagMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CustomerTagMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  CustomerTagPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.CustomerTaskRecordMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CustomerTaskRecordMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  CustomerTaskRecordPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.DataSourceConfigMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/DataSourceConfigMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps DataSourceConfigMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DataSourceConfigPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.EventAttrDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/EventAttrDefinitionMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  EventAttrDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.EventDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/EventDefinitionMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps EventDefinitionMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  EventDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.EventLogMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/EventLogMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  EventLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ExecutionRerunAuditMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ExecutionRerunAuditMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  ExecutionRerunAuditPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.GrowthActivityEventMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/GrowthActivityEventMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthActivityEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.GrowthActivityMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/GrowthActivityMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthActivityPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.GrowthActivityParticipantMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/GrowthActivityParticipantMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthActivityParticipantPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.GrowthActivityRuleSetMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/GrowthActivityRuleSetMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthActivityRuleSetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.GrowthReferralCodeMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/GrowthReferralCodeMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthReferralCodePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.GrowthReferralRelationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/GrowthReferralRelationMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthReferralRelationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.GrowthRewardGrantMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/GrowthRewardGrantMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthRewardGrantPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.GrowthRewardPoolMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/GrowthRewardPoolMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthRewardPoolPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.GrowthTaskDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/GrowthTaskDefinitionMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthTaskDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.GrowthTaskProgressMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/GrowthTaskProgressMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  GrowthTaskProgressPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.IdentityTypeMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/IdentityTypeMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  IdentityTypePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.LoyaltyMemberAccountMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/LoyaltyMemberAccountMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps LoyaltyMemberAccountMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LoyaltyMemberAccountPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.LoyaltyRedemptionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/LoyaltyRedemptionMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps LoyaltyRedemptionMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LoyaltyRedemptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.LoyaltyRuleMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/LoyaltyRuleMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps LoyaltyRuleMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LoyaltyRulePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.LoyaltyTransactionJournalMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/LoyaltyTransactionJournalMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps LoyaltyTransactionJournalMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LoyaltyTransactionJournalPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingAssetFolderMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingAssetFolderMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  MarketingAssetFolderPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingAssetMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingAssetMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingAssetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingAssetUploadIntentMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingAssetUploadIntentMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingAssetUploadIntentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingCampaignLinkMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingCampaignLinkMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingCampaignLinkPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingCampaignMasterMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingCampaignMasterMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingCampaignMasterPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingCompetitorMentionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingCompetitorMentionMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingCompetitorMentionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingConsentMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingConsentMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingConsentPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingContentAuditEventMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingContentAuditEventMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingContentAuditEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingContentEntryMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingContentEntryMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingContentEntryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingContentEntryVersionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingContentEntryVersionMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  MarketingContentEntryVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingContentReleaseItemMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingContentReleaseItemMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingContentReleaseItemPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingContentReleaseMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingContentReleaseMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingContentReleasePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingContentTemplateMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingContentTemplateMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingContentTemplatePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingContentTemplateVersionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingContentTemplateVersionMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  MarketingContentTemplateVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingFormDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingFormDefinitionMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingFormDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingFormSubmissionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingFormSubmissionMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingFormSubmissionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingIntegrationContractAuditEventMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingIntegrationContractAuditEventMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractAuditEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingIntegrationContractMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeObservationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingIntegrationContractProbeObservationMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeObservationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingIntegrationContractProbeRunMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorAlertChannelMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorAlertChannelMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertChannelPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorAlertDeliveryMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorAlertDeliveryMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertDeliveryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorAlertMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorAnomalyEventMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorAnomalyEventMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorAnomalyRuleMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorAnomalyRuleMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyRulePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorInferenceMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorInferenceMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorInferencePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorItemMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorItemMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorItemPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorPollRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorPollRunMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialEventMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorProviderCredentialEventMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorProviderCredentialMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorProviderOAuthAuthorizationEventMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorProviderOAuthAuthorizationEventMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationEventMapper assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationEventPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorProviderOAuthAuthorizationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorProviderOAuthAuthorizationMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationMapper assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorSourceMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorSourcePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingMonitorTrendSnapshotMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorTrendSnapshotMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingMonitorTrendSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingSentimentAnalysisMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingSentimentAnalysisMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingSentimentAnalysisPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MarketingSuppressionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingSuppressionMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MarketingSuppressionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MessageSendRecordMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MessageSendRecordMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  MessageSendRecordPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.MqMessageDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MqMessageDefinitionMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  MqMessageDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.NodeTypeRegistryMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/NodeTypeRegistryMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  NodeTypeRegistryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.NotificationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/NotificationMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps NotificationMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  NotificationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.PaidMediaAudienceDestinationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/PaidMediaAudienceDestinationMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceDestinationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.PaidMediaAudienceMemberMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/PaidMediaAudienceMemberMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceMemberPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.PaidMediaAudienceSyncRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/PaidMediaAudienceSyncRunMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceSyncRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ProgrammaticDspCampaignMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ProgrammaticDspCampaignMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspCampaignMapper assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspCampaignPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ProgrammaticDspLineItemMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ProgrammaticDspLineItemMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspLineItemMapper assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspLineItemPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ProgrammaticDspMutationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ProgrammaticDspMutationMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspMutationMapper assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ProgrammaticDspPerformanceSnapshotMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ProgrammaticDspPerformanceSnapshotMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspPerformanceSnapshotMapper assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspPerformanceSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ProgrammaticDspSeatMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ProgrammaticDspSeatMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspSeatMapper assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSeatPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.ProgrammaticDspSupplyPathMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ProgrammaticDspSupplyPathMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

coordinator decision:
  coordinator keeps ProgrammaticDspSupplyPathMapper assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSupplyPathPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.RiskDecisionRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/RiskDecisionRunMapper.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskDecisionRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.RiskListEntryMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/RiskListEntryMapper.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskListEntryPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.RiskListMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/RiskListMapper.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskListPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.RiskRuleHitMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/RiskRuleHitMapper.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskRuleHitPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.RiskSceneMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/RiskSceneMapper.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskScenePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.RiskSimulationRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/RiskSimulationRunMapper.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskSimulationRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.RiskStrategyMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/RiskStrategyMapper.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskStrategyPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.RiskStrategyVersionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/RiskStrategyVersionMapper.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.adapter.persistence

owning worker:
  DDD-W02

required tests:
  RiskStrategyVersionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.SearchMarketingImpactWindowMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingImpactWindowMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingImpactWindowPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.SearchMarketingKeywordMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingKeywordMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingKeywordPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingMutationMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingMutationPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.SearchMarketingOpportunityMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingOpportunityMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingOpportunityPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.SearchMarketingProviderChangeMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingProviderChangeMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderChangePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.SearchMarketingSnapshotMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingSnapshotMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingSnapshotPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingSourceMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingSourcePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.SearchMarketingSyncRunMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingSyncRunMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingSyncRunPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.SearchMarketingUrlInspectionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingUrlInspectionMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  SearchMarketingUrlInspectionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.SysUserMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SysUserMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps SysUserMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  SysUserPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.SystemOptionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SystemOptionMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps SystemOptionMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  SystemOptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.TagDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TagDefinitionMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  TagDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.TagImportBatchMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TagImportBatchMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  TagImportBatchPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.TagImportErrorMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TagImportErrorMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  TagImportErrorPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.TagImportSourceMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TagImportSourceMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  TagImportSourcePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.TagValueDefinitionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TagValueDefinitionMapper.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.adapter.persistence

owning worker:
  DDD-W04

required tests:
  TagValueDefinitionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.TenantMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TenantMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps TenantMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  TenantPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.TestUserMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TestUserMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  TestUserPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.TestUserSetMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TestUserSetMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  TestUserSetPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.UserInputFormMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/UserInputFormMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  UserInputFormPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.UserInputResponseMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/UserInputResponseMapper.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.adapter.persistence

owning worker:
  DDD-W07

required tests:
  UserInputResponsePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.UserInputResumeAuditMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/UserInputResumeAuditMapper.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.adapter.persistence

owning worker:
  DDD-W08

required tests:
  UserInputResumeAuditPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.UserWorkspacePreferenceMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/UserWorkspacePreferenceMapper.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.adapter.persistence

owning worker:
  DDD-W03

required tests:
  UserWorkspacePreferencePersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.WebhookDeliveryLogMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/WebhookDeliveryLogMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

owning worker:
  DDD-W01

required tests:
  WebhookDeliveryLogPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics

old class:
  org.chovy.canvas.dal.mapper.WebhookSubscriptionMapper

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/WebhookSubscriptionMapper.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.adapter.persistence

coordinator decision:
  coordinator keeps WebhookSubscriptionMapper assigned to canvas-platform until the owning task pack accepts the row

required tests:
  WebhookSubscriptionPersistenceMappingTest

compatibility notes:
  preserve table ownership, column mapping, tenant filtering, and mapper query semantics
