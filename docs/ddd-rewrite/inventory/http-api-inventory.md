# HTTP API Inventory

Exact controller inventory and DDD target behavior ownership for future HTTP adapter migration.

Generated on 2026-06-09 from the current `backend/canvas-engine` source tree and `context-ownership-seed.md`.
old class:
  org.chovy.canvas.web.AbExperimentController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AbExperimentController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  AbExperimentControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.AbExperimentGovernanceController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AbExperimentGovernanceController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  AbExperimentGovernanceControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.AdminController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AdminController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  AdminControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.AiDecisionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiDecisionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  AiDecisionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.AiPredictionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPredictionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  AiPredictionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.AiPromptTemplateController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  AiPromptTemplateControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.AiProviderController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiProviderController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  AiProviderControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.AnalyticsController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  AnalyticsControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.ApiDefinitionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ApiDefinitionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  ApiDefinitionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.ApprovalController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ApprovalController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  ApprovalControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.AsyncTaskController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AsyncTaskController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  AsyncTaskControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.AudienceController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  AudienceControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.AuthController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AuthController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  AuthControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CanvasBatchOperationController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasBatchOperationController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.canvas

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-canvas

required tests:
  CanvasBatchOperationControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CanvasCollaborationController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasCollaborationController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.canvas

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-canvas

required tests:
  CanvasCollaborationControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CanvasController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.canvas

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-canvas

required tests:
  CanvasControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CanvasExecutionManagementController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionManagementController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.execution

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-execution

required tests:
  CanvasExecutionManagementControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CanvasExecutionRequestManagementController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.execution

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-execution

required tests:
  CanvasExecutionRequestManagementControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CanvasMqTriggerRejectedController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasMqTriggerRejectedController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.execution

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-execution

required tests:
  CanvasMqTriggerRejectedControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CanvasProjectController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasProjectController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.canvas

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-canvas

required tests:
  CanvasProjectControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CanvasStatsController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.canvas

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-canvas

required tests:
  CanvasStatsControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CanvasUserController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasUserController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.canvas

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-canvas

required tests:
  CanvasUserControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpComputedProfileController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpComputedProfileControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpComputedTagController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedTagController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpComputedTagControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpEventIngestionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpEventIngestionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpEventIngestionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpTagOperationController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpTagOperationController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpTagOperationControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpUserController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpUserController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpUserControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseAudienceMaterializationController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAudienceMaterializationController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseAudienceMaterializationControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseAvailabilityController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAvailabilityController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseAvailabilityControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseAvailabilityIncidentController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAvailabilityIncidentController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseAvailabilityIncidentControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseCatalogController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseCatalogController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseCatalogControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseConsumerAvailabilityIncidentController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseConsumerAvailabilityIncidentController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseConsumerAvailabilityIncidentControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseE2eCertificationGateController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationGateController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseE2eCertificationGateControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseE2eCertificationRunController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseE2eCertificationRunControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseEnterpriseOlapEvidenceController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseEnterpriseOlapEvidenceController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseEnterpriseOlapEvidenceControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseExternalRealtimeJobProbeController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseExternalRealtimeJobProbeController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseExternalRealtimeJobProbeControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseFieldGovernanceController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseFieldGovernanceController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseFieldGovernanceControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseIncidentController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseIncidentController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseIncidentControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseMetricChangeReviewController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseMetricChangeReviewController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  CdpWarehouseMetricChangeReviewControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseMetricLineageController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseMetricLineageController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  CdpWarehouseMetricLineageControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehousePhysicalE2eCertificationController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePhysicalE2eCertificationController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehousePhysicalE2eCertificationControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehousePrivacyErasureController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehousePrivacyErasureControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehousePrivacyTombstoneController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyTombstoneController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehousePrivacyTombstoneControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseProductionReadinessController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseProductionReadinessController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseProductionReadinessControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseQualityController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseQualityController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseQualityControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseReadinessController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseReadinessController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseReadinessControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseReadinessIncidentController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseReadinessIncidentController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseReadinessIncidentControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimeController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseRealtimeControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimeCutoverReadinessController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeCutoverReadinessController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseRealtimeCutoverReadinessControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimeJobController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeJobController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseRealtimeJobControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimeJobIncidentController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeJobIncidentController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseRealtimeJobIncidentControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimePipelineController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimePipelineController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseRealtimePipelineControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimePipelineIncidentController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimePipelineIncidentController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseRealtimePipelineIncidentControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimeSchemaController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeSchemaController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseRealtimeSchemaControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseSemanticMetricController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseSemanticMetricController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  CdpWarehouseSemanticMetricControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseSloPolicyController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseSloPolicyController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseSloPolicyControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseSyntheticDataPathProbeController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseSyntheticDataPathProbeController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseSyntheticDataPathProbeControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseTableDriftIncidentController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableDriftIncidentController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseTableDriftIncidentControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWarehouseTableGovernanceController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableGovernanceController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWarehouseTableGovernanceControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CdpWriteKeyController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWriteKeyController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  CdpWriteKeyControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.ChannelConnectorController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ChannelConnectorController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  ChannelConnectorControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.ContactabilityController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ContactabilityController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  ContactabilityControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.ConversationController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.conversation

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-conversation

required tests:
  ConversationControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.ConversationPrivateDomainController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationPrivateDomainController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.conversation

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-conversation

required tests:
  ConversationPrivateDomainControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.ConversationProviderWebhookController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationProviderWebhookController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.conversation

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-conversation

required tests:
  ConversationProviderWebhookControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.ConversationWorkspaceController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationWorkspaceController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.conversation

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-conversation

required tests:
  ConversationWorkspaceControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.CreatorCollaborationController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CreatorCollaborationController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  CreatorCollaborationControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.DataSourceConfigController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/DataSourceConfigController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  DataSourceConfigControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.DeliveryReceiptController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/DeliveryReceiptController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  DeliveryReceiptControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.DemoSandboxController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/DemoSandboxController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  DemoSandboxControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.DlqController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  DlqControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.EventAttributeDiscoveryController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventAttributeDiscoveryController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  EventAttributeDiscoveryControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.EventDefinitionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventDefinitionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  EventDefinitionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.ExecutionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.execution

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-execution

required tests:
  ExecutionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.ExecutionRerunController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionRerunController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.execution

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-execution

required tests:
  ExecutionRerunControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.GrowthActivityController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/GrowthActivityController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  GrowthActivityControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.HomeOverviewController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/HomeOverviewController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  HomeOverviewControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.IdentityTypeController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/IdentityTypeController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  IdentityTypeControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.LoyaltyController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/LoyaltyController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  LoyaltyControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MarketingCampaignController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingCampaignController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MarketingCampaignControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MarketingContentController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingContentController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MarketingContentControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MarketingFormController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingFormController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MarketingFormControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MarketingIntegrationContractController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingIntegrationContractController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MarketingIntegrationContractControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MarketingIntegrationContractProbeController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingIntegrationContractProbeController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MarketingIntegrationContractProbeControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MarketingMonitorAnomalyController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitorAnomalyController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MarketingMonitorAnomalyControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MarketingMonitoringController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MarketingMonitoringControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MarketingMonitoringWebhookAdminController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringWebhookAdminController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MarketingMonitoringWebhookAdminControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MarketingPlatformControlPlaneController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingPlatformControlPlaneController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MarketingPlatformControlPlaneControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MarketingPolicyAdminController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingPolicyAdminController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MarketingPolicyAdminControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MarketingPreferenceCenterController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingPreferenceCenterController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MarketingPreferenceCenterControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MauticInspiredInsightController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MauticInspiredInsightController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MauticInspiredInsightControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MessageDeliveryController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageDeliveryController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MessageDeliveryControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MessageSendRecordController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageSendRecordController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MessageSendRecordControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MessageTemplateController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageTemplateController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  MessageTemplateControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MetaController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MetaController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  MetaControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.MqDefinitionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MqDefinitionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.execution

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-execution

required tests:
  MqDefinitionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.NotificationController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/NotificationController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  NotificationControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.OpsController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  OpsControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.PaidMediaAudienceSyncController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PaidMediaAudienceSyncController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  PaidMediaAudienceSyncControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.PlatformWorkstreamController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PlatformWorkstreamController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  PlatformWorkstreamControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.PluginRegistryController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PluginRegistryController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  PluginRegistryControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.ProgrammaticDspController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ProgrammaticDspController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  ProgrammaticDspControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.PublicConversationWebhookController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicConversationWebhookController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.conversation

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-conversation

required tests:
  PublicConversationWebhookControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.PublicMarketingContentUploadWebhookController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicMarketingContentUploadWebhookController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  PublicMarketingContentUploadWebhookControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.PublicMarketingMonitoringWebhookController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicMarketingMonitoringWebhookController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  PublicMarketingMonitoringWebhookControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.RealtimeAudienceController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/RealtimeAudienceController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  RealtimeAudienceControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.SearchMarketingController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/SearchMarketingController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.marketing

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-marketing

required tests:
  SearchMarketingControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.SystemOptionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/SystemOptionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  SystemOptionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.TagDefinitionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/TagDefinitionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  TagDefinitionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.TagImportController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/TagImportController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  TagImportControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.TagImportSourceController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/TagImportSourceController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.cdp

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-cdp

required tests:
  TagImportSourceControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.TechnicalMigrationCandidateController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/TechnicalMigrationCandidateController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  TechnicalMigrationCandidateControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.TenantController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/TenantController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  TenantControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.TestUserController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/TestUserController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  TestUserControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.UserInputController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/UserInputController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.canvas

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-canvas

required tests:
  UserInputControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.WebhookSubscriptionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.platform

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-platform

required tests:
  WebhookSubscriptionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiAiController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiAiController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiAiControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiBigScreenController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiBigScreenController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiBigScreenControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiCapacityController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiCapacityController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiCapacityControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiChartController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiChartController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiChartControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiDashboardController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiDashboardControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiDatasetController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasetController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiDatasetControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiDatasourceController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasourceController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiDatasourceControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiEmbedResourceController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiEmbedResourceController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiEmbedResourceControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiPermissionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPermissionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiPermissionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiPortalController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPortalController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiPortalControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiPortalRuntimeController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPortalRuntimeController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiPortalRuntimeControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiPublishApprovalController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPublishApprovalController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiPublishApprovalControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiQueryController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiQueryControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiResourceCollaborationController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceCollaborationController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiResourceCollaborationControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiResourceFavoriteController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceFavoriteController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiResourceFavoriteControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiResourceMovementController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceMovementController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiResourceMovementControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiResourceTransferController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceTransferController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiResourceTransferControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiSelfServiceController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSelfServiceController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiSelfServiceControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiSpreadsheetController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSpreadsheetController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiSpreadsheetControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.bi.BiSubscriptionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSubscriptionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.bi

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-bi

required tests:
  BiSubscriptionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.risk.RiskDecisionController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskDecisionController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.risk

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-risk

required tests:
  RiskDecisionControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.risk.RiskLabController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskLabController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.risk

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-risk

required tests:
  RiskLabControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.risk.RiskListController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskListController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.risk

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-risk

required tests:
  RiskListControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.risk.RiskSceneController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskSceneController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.risk

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-risk

required tests:
  RiskSceneControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior

old class:
  org.chovy.canvas.web.risk.RiskStrategyController

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskStrategyController.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.risk

coordinator decision:
  DDD-C09 migrates HTTP adapter; behavior delegates to canvas-context-risk

required tests:
  RiskStrategyControllerCompatibilityTest

compatibility notes:
  preserve route, request, response, auth, tenant, pagination, and error envelope behavior
