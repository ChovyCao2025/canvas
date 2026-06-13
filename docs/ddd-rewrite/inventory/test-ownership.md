# Test Ownership Inventory

Exact old test ownership rows for porting or replacement planning.

Generated on 2026-06-09 from the current `backend/canvas-engine` source tree and `context-ownership-seed.md`.
old class:
  org.chovy.canvas.architecture.RuntimeMigrationEvidenceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/RuntimeMigrationEvidenceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  RuntimeMigrationEvidenceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.architecture.TechnicalMigrationCandidateTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  TechnicalMigrationCandidateTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.auth.domain.SysUserServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/auth/domain/SysUserServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps SysUserServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  SysUserServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.common.OutboundUrlValidatorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/common/OutboundUrlValidatorTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  OutboundUrlValidatorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.common.enums.NodeTypeGovernanceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  NodeTypeGovernanceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.common.tenant.TenantContextResolverTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/common/tenant/TenantContextResolverTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps TenantContextResolverTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  TenantContextResolverTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.common.tenant.TenantScopeSupportTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/common/tenant/TenantScopeSupportTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps TenantScopeSupportTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  TenantScopeSupportTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.ApplicationShutdownConfigTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/ApplicationShutdownConfigTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ApplicationShutdownConfigTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.ApplicationYamlTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/ApplicationYamlTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ApplicationYamlTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.CacheConfigTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/CacheConfigTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  CacheConfigTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.CanvasInfrastructureHealthIndicatorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/CanvasInfrastructureHealthIndicatorTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasInfrastructureHealthIndicatorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.CanvasRuntimeMetricsTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/CanvasRuntimeMetricsTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  CanvasRuntimeMetricsTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.CorrelationIdWebFilterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/CorrelationIdWebFilterTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  CorrelationIdWebFilterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.FlywayConfigTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/FlywayConfigTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  FlywayConfigTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.GlobalExceptionHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/GlobalExceptionHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  GlobalExceptionHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.GlobalExceptionHandlerTraceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/GlobalExceptionHandlerTraceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  GlobalExceptionHandlerTraceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.InternalApiAuthFilterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/InternalApiAuthFilterTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps InternalApiAuthFilterTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  InternalApiAuthFilterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.JwtAuthFilterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/JwtAuthFilterTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps JwtAuthFilterTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  JwtAuthFilterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.ProductionConfigGuardTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionConfigGuardTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ProductionConfigGuardTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.ProductionProfileValidationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionProfileValidationTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ProductionProfileValidationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.ProductionSecurityValidatorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionSecurityValidatorTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ProductionSecurityValidatorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.RedisRoleConfigurationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/RedisRoleConfigurationTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  RedisRoleConfigurationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.RiskControlConfigurationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/RiskControlConfigurationTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskControlConfigurationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.SecurityConfigRoleTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRoleTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  SecurityConfigRoleTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.SecurityConfigRouteTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  SecurityConfigRouteTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.config.WebConfigTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/WebConfigTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  WebConfigTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.ApiDefinitionControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/ApiDefinitionControllerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps ApiDefinitionControllerTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApiDefinitionControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.AsyncTaskControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AsyncTaskControllerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AsyncTaskControllerTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AsyncTaskControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.AudienceControllerTaskTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerTaskTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceControllerTaskTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.AudienceControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.CanvasControllerCollaborationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasControllerCollaborationTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasControllerCollaborationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.CanvasExecutionManagementControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasExecutionManagementControllerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasExecutionManagementControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.CanvasExecutionRequestManagementControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasExecutionRequestManagementControllerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestManagementControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.CanvasMqTriggerRejectedControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasMqTriggerRejectedControllerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasMqTriggerRejectedControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.CanvasStatsControllerEffectClosureTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasStatsControllerEffectClosureTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasStatsControllerEffectClosureTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.CanvasStatsControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasStatsControllerTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasStatsControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.CanvasUserControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasUserControllerTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasUserControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.CdpTagOperationControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CdpTagOperationControllerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpTagOperationControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.CdpUserControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CdpUserControllerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpUserControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.DataSourceConfigControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DataSourceConfigControllerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps DataSourceConfigControllerTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DataSourceConfigControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.DemoSandboxControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DemoSandboxControllerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps DemoSandboxControllerTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DemoSandboxControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.EventDefinitionControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/EventDefinitionControllerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps EventDefinitionControllerTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  EventDefinitionControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.MarketingContentUploadControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MarketingContentUploadControllerTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingContentUploadControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.MarketingPolicyAdminControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MarketingPolicyAdminControllerTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

coordinator decision:
  coordinator keeps MarketingPolicyAdminControllerTest assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingPolicyAdminControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.MessageSendRecordControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MessageSendRecordControllerTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MessageSendRecordControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.MessageTemplateControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MessageTemplateControllerTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MessageTemplateControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.NotificationControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/NotificationControllerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps NotificationControllerTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  NotificationControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.OpsControllerRecoveryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsControllerRecoveryTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps OpsControllerRecoveryTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  OpsControllerRecoveryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.OpsControllerTemplateTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsControllerTemplateTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps OpsControllerTemplateTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  OpsControllerTemplateTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.PluginRegistryControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/PluginRegistryControllerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps PluginRegistryControllerTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  PluginRegistryControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.controller.SystemOptionControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/SystemOptionControllerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps SystemOptionControllerTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  SystemOptionControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.dal.dataobject.CoreTenantFieldMappingTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/dal/dataobject/CoreTenantFieldMappingTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps CoreTenantFieldMappingTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CoreTenantFieldMappingTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.dal.dataobject.DataSourceConfigDOTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDOTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps DataSourceConfigDOTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DataSourceConfigDOTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.ai.AiBackendFoundationSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/AiBackendFoundationSchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AiBackendFoundationSchemaTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiBackendFoundationSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.ai.AiDecisionModelSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/AiDecisionModelSchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AiDecisionModelSchemaTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionModelSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.ai.AiDecisionModelServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/AiDecisionModelServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AiDecisionModelServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiDecisionModelServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.ai.AiLlmNodeProductionizationSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/AiLlmNodeProductionizationSchemaTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

coordinator decision:
  coordinator keeps AiLlmNodeProductionizationSchemaTest assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  AiLlmNodeProductionizationSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.ai.AiPromptEvaluationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/AiPromptEvaluationServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AiPromptEvaluationServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiPromptEvaluationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.ai.AiProviderModelRegistryServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/AiProviderModelRegistryServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AiProviderModelRegistryServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiProviderModelRegistryServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.ai.ChurnFeatureSnapshotServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/ChurnFeatureSnapshotServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ChurnFeatureSnapshotServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.ai.ChurnPredictionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/ChurnPredictionServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ChurnPredictionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.ai.ChurnPredictionSmartTimingSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/ChurnPredictionSmartTimingSchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ChurnPredictionSmartTimingSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.ai.PredictionProfileWriterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/PredictionProfileWriterTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  PredictionProfileWriterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.ai.SmartTimingServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/SmartTimingServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  SmartTimingServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.AnalyticsQueryGuardTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQueryGuardTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AnalyticsQueryGuardTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsQueryGuardTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.AnalyticsQuerySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQuerySchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AnalyticsQuerySchemaTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsQuerySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.AnalyticsQueryServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AnalyticsQueryServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AnalyticsQueryServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AnalyticsQueryServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.AudienceMaterializationOperationsServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AudienceMaterializationOperationsServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceMaterializationOperationsServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.AudienceMaterializationScheduleServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AudienceMaterializationScheduleServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceMaterializationScheduleServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.AudienceMaterializationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AudienceMaterializationServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceMaterializationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.AudienceQualityServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AudienceQualityServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceQualityServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.BehaviorAudienceRuleCompilerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/BehaviorAudienceRuleCompilerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  BehaviorAudienceRuleCompilerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.CdpAudienceMaterializationRollbackSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/CdpAudienceMaterializationRollbackSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpAudienceMaterializationRollbackSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.CdpOlapAudienceSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/CdpOlapAudienceSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpOlapAudienceSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.CdpWarehouseAudienceMaterializationSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/CdpWarehouseAudienceMaterializationSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAudienceMaterializationSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.MyBatisAudienceDefinitionRepositoryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/MyBatisAudienceDefinitionRepositoryTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  MyBatisAudienceDefinitionRepositoryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.analytics.RetentionPolicyServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/RetentionPolicyServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  RetentionPolicyServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.approval.ApprovalLarkUserIdentityResolverTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/approval/ApprovalLarkUserIdentityResolverTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps ApprovalLarkUserIdentityResolverTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalLarkUserIdentityResolverTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.approval.ApprovalWorkflowServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/approval/ApprovalWorkflowServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps ApprovalWorkflowServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  ApprovalWorkflowServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.approval.CanvasPublishApprovalServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/approval/CanvasPublishApprovalServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

coordinator decision:
  coordinator keeps CanvasPublishApprovalServiceTest assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasPublishApprovalServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.approval.HttpLarkApprovalClientTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/approval/HttpLarkApprovalClientTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps HttpLarkApprovalClientTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  HttpLarkApprovalClientTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.approval.LarkApprovalProviderTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/approval/LarkApprovalProviderTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps LarkApprovalProviderTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LarkApprovalProviderTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.approval.LarkApprovalSyncSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/approval/LarkApprovalSyncSchedulerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

coordinator decision:
  coordinator keeps LarkApprovalSyncSchedulerTest assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  LarkApprovalSyncSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.audience.AudienceComputeRunTrackingSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceComputeRunTrackingSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceComputeRunTrackingSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.audience.AudienceSnapshotModeMigrationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceSnapshotModeMigrationTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceSnapshotModeMigrationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.audience.AudienceSnapshotServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceSnapshotServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceSnapshotServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.ai.BiAskDataAgentServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/ai/BiAskDataAgentServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiAskDataAgentServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.ai.BiRemainingAiAgentsTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/ai/BiRemainingAiAgentsTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

coordinator decision:
  coordinator keeps BiRemainingAiAgentsTest assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiRemainingAiAgentsTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.bigscreen.BiBigScreenResourceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/bigscreen/BiBigScreenResourceServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiBigScreenResourceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.chart.BiChartReferenceImpactServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/chart/BiChartReferenceImpactServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiChartReferenceImpactServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.chart.BiChartResourceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/chart/BiChartResourceServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiChartResourceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardResourceServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDashboardResourceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/BiDashboardRuntimeStateServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDashboardRuntimeStateServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dashboard.MarketingBiDashboardPresetRegistryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dashboard/MarketingBiDashboardPresetRegistryTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  MarketingBiDashboardPresetRegistryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationSchedulerServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetAccelerationSchedulerServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasetAccelerationSchedulerServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetAccelerationSchemaTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasetAccelerationSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetAccelerationServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasetAccelerationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasetFromDatasourceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dataset.BiDatasetResourceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasetResourceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineCapacityServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineCapacityServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiQuickEngineCapacityServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueSchedulerServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueSchedulerServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueSchedulerServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueueServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.dataset.BiSqlDatasetPreviewServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/dataset/BiSqlDatasetPreviewServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiSqlDatasetPreviewServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceApiPreviewRuntimeServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceApiPreviewRuntimeServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasourceApiPreviewRuntimeServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceFileMaterializationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceFileMaterializationServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasourceFileMaterializationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceFileRuntimeServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceFileRuntimeServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasourceFileRuntimeServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceFileUploadControllerContractTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceFileUploadControllerContractTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasourceFileUploadControllerContractTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceFileUploadServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceFileUploadServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasourceFileUploadServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.datasource.BiDatasourceOnboardingServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceOnboardingServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasourceOnboardingServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.embed.BiEmbedTicketSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketSchemaTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiEmbedTicketSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.embed.BiEmbedTicketServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/embed/BiEmbedTicketServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiEmbedTicketServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.export.BiSelfServiceExportServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/export/BiSelfServiceExportServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiSelfServiceExportServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.permission.BiPermissionAdminServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionAdminServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

coordinator decision:
  coordinator keeps BiPermissionAdminServiceTest assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiPermissionAdminServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.permission.BiPermissionRequestServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionRequestServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiPermissionRequestServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.permission.BiPermissionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiPermissionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.portal.BiPortalResourceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/portal/BiPortalResourceServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiPortalResourceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.portal.BiPortalRuntimeServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/portal/BiPortalRuntimeServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiPortalRuntimeServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.query.BiDatasourceHealthSloSummaryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiDatasourceHealthSloSummaryTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDatasourceHealthSloSummaryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.query.BiQueryCachePolicyServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryCachePolicyServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiQueryCachePolicyServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.query.BiQueryCompilerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryCompilerTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiQueryCompilerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.query.BiQueryExecutionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryExecutionServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiQueryExecutionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.query.BiQueryGovernancePolicyServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryGovernancePolicyServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiQueryGovernancePolicyServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.query.BiQueryHistoryReaderTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQueryHistoryReaderTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiQueryHistoryReaderTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.query.BiQuickEngineQueryAdmissionQueueWiringTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/BiQuickEngineQueryAdmissionQueueWiringTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiQuickEngineQueryAdmissionQueueWiringTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.query.MarketingBiDatasetRegistryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/MarketingBiDatasetRegistryTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  MarketingBiDatasetRegistryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.resource.BiPublishApprovalServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

coordinator decision:
  coordinator keeps BiPublishApprovalServiceTest assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiPublishApprovalServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceCollaborationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceCollaborationServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiResourceCollaborationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceFavoriteServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceFavoriteServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiResourceFavoriteServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceMovementServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceMovementServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiResourceMovementServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.resource.BiResourceTransferServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiResourceTransferServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiResourceTransferServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.spreadsheet.BiSpreadsheetResourceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/spreadsheet/BiSpreadsheetResourceServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiSpreadsheetResourceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.storage.BiFileStorageConfigurationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/storage/BiFileStorageConfigurationTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiFileStorageConfigurationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.storage.HttpS3ObjectClientTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/storage/HttpS3ObjectClientTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  HttpS3ObjectClientTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.storage.S3CompatibleBiFileStorageTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/storage/S3CompatibleBiFileStorageTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  S3CompatibleBiFileStorageTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryAdapterServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAdapterServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDeliveryAdapterServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryAttachmentServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDeliveryAttachmentServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliveryRuntimeServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliveryRuntimeServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDeliveryRuntimeServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerLeaseServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerLeaseServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDeliverySchedulerLeaseServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiDeliverySchedulerServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiDeliverySchedulerServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.subscription.BiSmtpEmailDeliveryClientTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiSmtpEmailDeliveryClientTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  BiSmtpEmailDeliveryClientTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.subscription.BiSubscriptionAdminServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/BiSubscriptionAdminServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

coordinator decision:
  coordinator keeps BiSubscriptionAdminServiceTest assigned to canvas-context-bi until the owning task pack accepts the row

required tests:
  BiSubscriptionAdminServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.bi.subscription.HttpBiSnapshotRendererTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/subscription/HttpBiSnapshotRendererTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  HttpBiSnapshotRendererTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasAttributionSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasAttributionSchemaTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasAttributionSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasAttributionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasAttributionServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasAttributionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasControlGroupServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasControlGroupServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasControlGroupServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasExampleLibrarySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleLibrarySchemaTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasExampleLibrarySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasExampleSeederTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleSeederTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasExampleSeederTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasExampleTemplateMigrationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleTemplateMigrationTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasExampleTemplateMigrationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasExampleTemplateSqlTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExampleTemplateSqlTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasExampleTemplateSqlTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasExamplesPropertiesTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasExamplesPropertiesTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasExamplesPropertiesTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasImportExportServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasImportExportServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasImportExportServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasMessagePreviewServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasMessagePreviewServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasMessagePreviewServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasOpsServiceStateTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasOpsServiceStateTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

coordinator decision:
  coordinator keeps CanvasOpsServiceStateTest assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasOpsServiceStateTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasOpsServiceTenantTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasOpsServiceTenantTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

coordinator decision:
  coordinator keeps CanvasOpsServiceTenantTest assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasOpsServiceTenantTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasPrePublishCheckServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPrePublishCheckServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasPrePublishCheckServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasProjectFilterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasProjectFilterTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasProjectFilterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasProjectFolderMetadataServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasProjectFolderMetadataServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

coordinator decision:
  coordinator keeps CanvasProjectFolderMetadataServiceTest assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasProjectFolderMetadataServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasProjectMetadataMigrationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasProjectMetadataMigrationTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

coordinator decision:
  coordinator keeps CanvasProjectMetadataMigrationTest assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasProjectMetadataMigrationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasPublishAudienceSnapshotTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPublishAudienceSnapshotTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CanvasPublishAudienceSnapshotTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasServiceDraftUpdateStateTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceDraftUpdateStateTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasServiceDraftUpdateStateTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasServiceTenantIsolationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceTenantIsolationTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

coordinator decision:
  coordinator keeps CanvasServiceTenantIsolationTest assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasServiceTenantIsolationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasServiceTenantScopeTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceTenantScopeTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

coordinator decision:
  coordinator keeps CanvasServiceTenantScopeTest assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasServiceTenantScopeTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasStateTransitionPolicyTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasStateTransitionPolicyTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasStateTransitionPolicyTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasTenantIsolationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTenantIsolationTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

coordinator decision:
  coordinator keeps CanvasTenantIsolationTest assigned to canvas-context-canvas until the owning task pack accepts the row

required tests:
  CanvasTenantIsolationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasTransactionAnnotationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionAnnotationTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasTransactionAnnotationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasTransactionBoundaryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionBoundaryTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasTransactionBoundaryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasTransactionServiceStateTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionServiceStateTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasTransactionServiceStateTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasTransactionSideEffectTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionSideEffectTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasTransactionSideEffectTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasValidationRuntimeGuardTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasValidationRuntimeGuardTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasValidationRuntimeGuardTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CanvasVersionCleanupJobTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJobTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasVersionCleanupJobTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.ConnectedContentGatewayServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/ConnectedContentGatewayServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  ConnectedContentGatewayServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.CoreTenantNotNullMigrationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CoreTenantNotNullMigrationTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps CoreTenantNotNullMigrationTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CoreTenantNotNullMigrationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.DirectCallNodeDefinitionMigrationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/DirectCallNodeDefinitionMigrationTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DirectCallNodeDefinitionMigrationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.canvas.UserInputServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/UserInputServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  UserInputServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CanvasUserQueryServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CanvasUserQueryServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasUserQueryServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpEntityMappingTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEntityMappingTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpEntityMappingTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpEventIngestionPrivacyTombstoneGuardTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionPrivacyTombstoneGuardTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpEventIngestionPrivacyTombstoneGuardTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpEventIngestionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpEventIngestionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpEventIngestionWarehouseCheckpointTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionWarehouseCheckpointTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpEventIngestionWarehouseCheckpointTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpEventIngestionWarehouseRetryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionWarehouseRetryTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpEventIngestionWarehouseRetryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpEventIngestionWarehouseSinkTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionWarehouseSinkTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpEventIngestionWarehouseSinkTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpEventLogSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventLogSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpEventLogSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpEventPublisherTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventPublisherTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpEventPublisherTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpLineageServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpLineageServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpLineageServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpTagOperationServiceRetryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpTagOperationServiceRetryTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpTagOperationServiceRetryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpTagServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpTagServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpTagServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpUserDirectoryServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpUserDirectoryServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpUserDirectoryServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpUserInsightServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpUserInsightServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpUserInsightServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpUserServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpUserServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpUserServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpWriteKeyAuthServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

coordinator decision:
  coordinator keeps CdpWriteKeyAuthServiceTest assigned to canvas-context-cdp until the owning task pack accepts the row

required tests:
  CdpWriteKeyAuthServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.CdpWriteKeySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpWriteKeySchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWriteKeySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.ComputedProfileAttributeSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeSchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ComputedProfileAttributeSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.ComputedProfileAttributeServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ComputedProfileAttributeServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.ComputedTagSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedTagSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  ComputedTagSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.ComputedTagServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedTagServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  ComputedTagServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.DemoDatasourceCredentialMigrationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/DemoDatasourceCredentialMigrationTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  DemoDatasourceCredentialMigrationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.EventAttributeDiscoverySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoverySchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps EventAttributeDiscoverySchemaTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  EventAttributeDiscoverySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.EventAttributeDiscoveryServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoveryServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps EventAttributeDiscoveryServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  EventAttributeDiscoveryServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.RealtimeAudienceSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  RealtimeAudienceSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.RealtimeAudienceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/RealtimeAudienceServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  RealtimeAudienceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.WebhookDispatcherServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/WebhookDispatcherServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  WebhookDispatcherServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.WebhookRetryPolicyTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/WebhookRetryPolicyTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  WebhookRetryPolicyTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.WebhookSignatureServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/WebhookSignatureServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  WebhookSignatureServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.WebhookSubscriptionSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/WebhookSubscriptionSchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps WebhookSubscriptionSchemaTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  WebhookSubscriptionSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.cdp.WebhookSubscriptionValidatorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/WebhookSubscriptionValidatorTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps WebhookSubscriptionValidatorTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  WebhookSubscriptionValidatorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.collaboration.UserWorkspacePreferenceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/collaboration/UserWorkspacePreferenceServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  UserWorkspacePreferenceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.compliance.AuditEventServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/AuditEventServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  AuditEventServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.compliance.DataDeletionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/DataDeletionServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  DataDeletionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.compliance.PiiMaskingServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/PiiMaskingServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  PiiMaskingServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.content.ContentEntryServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/ContentEntryServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  ContentEntryServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.content.ContentTemplateServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/ContentTemplateServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  ContentTemplateServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.content.MarketingAssetServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingAssetServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingAssetServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.content.MarketingAssetUploadIntentCleanupSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingAssetUploadIntentCleanupSchedulerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  MarketingAssetUploadIntentCleanupSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.content.MarketingAssetUploadIntentSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingAssetUploadIntentSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingAssetUploadIntentSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.content.MarketingAssetUploadServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingAssetUploadServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingAssetUploadServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.content.MarketingAssetUploadWebhookSignatureServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingAssetUploadWebhookSignatureServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingAssetUploadWebhookSignatureServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.content.MarketingContentHubSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingContentHubSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingContentHubSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.content.MarketingContentReleaseSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingContentReleaseSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingContentReleaseSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.content.MarketingContentReleaseServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingContentReleaseServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingContentReleaseServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.AbstractProviderConversationReplyAdapterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/AbstractProviderConversationReplyAdapterTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  AbstractProviderConversationReplyAdapterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationAdapterCatalogTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterCatalogTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationAdapterCatalogTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationAdapterContractMatrixTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterContractMatrixTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationAdapterContractMatrixTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationAdapterHarnessTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAdapterHarnessTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationAdapterHarnessTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationAiReplySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAiReplySchemaTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

coordinator decision:
  coordinator keeps ConversationAiReplySchemaTest assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationAiReplyServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationAiReplyServiceTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

coordinator decision:
  coordinator keeps ConversationAiReplyServiceTest assigned to canvas-context-conversation until the owning task pack accepts the row

required tests:
  ConversationAiReplyServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationIngressServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationIngressServiceTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationIngressServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationPrivateDomainSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationPrivateDomainSchemaTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationPrivateDomainSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationPrivateDomainSyncServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationPrivateDomainSyncServiceTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationPrivateDomainSyncServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationReplyAdapterSupportTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationReplyAdapterSupportTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationReplyAdapterSupportTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationRoutingSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationRoutingSchemaTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationRoutingSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationRoutingServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationRoutingServiceTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationRoutingServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationSessionSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationSessionSchemaTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationSessionSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationWorkspaceSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationWorkspaceSchemaTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationWorkspaceSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.ConversationWorkspaceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationWorkspaceServiceTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  ConversationWorkspaceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.RcsConversationReplyAdapterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/RcsConversationReplyAdapterTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  RcsConversationReplyAdapterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.SocialDmConversationReplyAdapterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/SocialDmConversationReplyAdapterTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  SocialDmConversationReplyAdapterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.WebChatConversationReplyAdapterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WebChatConversationReplyAdapterTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  WebChatConversationReplyAdapterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.WhatsAppConversationReplyAdapterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppConversationReplyAdapterTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  WhatsAppConversationReplyAdapterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.WhatsAppWebhookPayloadMapperTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookPayloadMapperTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  WhatsAppWebhookPayloadMapperTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.conversation.WhatsAppWebhookSecurityServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/WhatsAppWebhookSecurityServiceTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  WhatsAppWebhookSecurityServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.creator.CreatorCollaborationSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/creator/CreatorCollaborationSchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps CreatorCollaborationSchemaTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorCollaborationSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.creator.CreatorCollaborationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/creator/CreatorCollaborationServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps CreatorCollaborationServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorCollaborationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.creator.CreatorProviderMutationSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/creator/CreatorProviderMutationSchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps CreatorProviderMutationSchemaTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.creator.CreatorProviderMutationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/creator/CreatorProviderMutationServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps CreatorProviderMutationServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  CreatorProviderMutationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.datasource.DataSecurityMigrationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/datasource/DataSecurityMigrationTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  DataSecurityMigrationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.datasource.DataSourceConfigServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/datasource/DataSourceConfigServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps DataSourceConfigServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DataSourceConfigServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.datasource.DataSourceCredentialCipherTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/datasource/DataSourceCredentialCipherTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  DataSourceCredentialCipherTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.demo.DemoSandboxServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/demo/DemoSandboxServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps DemoSandboxServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  DemoSandboxServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.execution.CanvasExecutionDlqSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/CanvasExecutionDlqSchemaTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

coordinator decision:
  coordinator keeps CanvasExecutionDlqSchemaTest assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  CanvasExecutionDlqSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.execution.ExecutionContextColdBackupSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/ExecutionContextColdBackupSchemaTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ExecutionContextColdBackupSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.execution.PerfRunEntityMappingTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunEntityMappingTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  PerfRunEntityMappingTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.execution.PerfRunTrackingSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunTrackingSchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  PerfRunTrackingSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.loyalty.LoyaltySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/loyalty/LoyaltySchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps LoyaltySchemaTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LoyaltySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.loyalty.LoyaltyServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/loyalty/LoyaltyServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps LoyaltyServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  LoyaltyServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityEventServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/GrowthActivityEventServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  GrowthActivityEventServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityReadinessServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/GrowthActivityReadinessServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  GrowthActivityReadinessServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityReportServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/GrowthActivityReportServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  GrowthActivityReportServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.GrowthActivitySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/GrowthActivitySchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  GrowthActivitySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.GrowthActivityServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/GrowthActivityServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  GrowthActivityServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.GrowthBenefitPromotionGrantAdapterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/GrowthBenefitPromotionGrantAdapterTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  GrowthBenefitPromotionGrantAdapterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.GrowthLoyaltyAdapterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/GrowthLoyaltyAdapterTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

coordinator decision:
  coordinator keeps GrowthLoyaltyAdapterTest assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  GrowthLoyaltyAdapterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.GrowthReferralServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/GrowthReferralServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  GrowthReferralServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.GrowthRewardGrantServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/GrowthRewardGrantServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  GrowthRewardGrantServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.GrowthRewardPoolServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/GrowthRewardPoolServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  GrowthRewardPoolServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.GrowthTaskServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/GrowthTaskServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  GrowthTaskServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.HttpMarketingIntegrationContractProbeClientTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/HttpMarketingIntegrationContractProbeClientTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  HttpMarketingIntegrationContractProbeClientTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.MarketingCampaignMasterSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/MarketingCampaignMasterSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingCampaignMasterSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.MarketingCampaignServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/MarketingCampaignServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingCampaignServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.MarketingFormServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/MarketingFormServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingFormServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeAlertServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeAlertServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeAlertServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeAutomationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeAutomationServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeAutomationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeSchedulerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  MarketingIntegrationContractProbeSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractProbeServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractProbeServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.marketing.MarketingIntegrationContractSloServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/MarketingIntegrationContractSloServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingIntegrationContractSloServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.meta.AbExperimentGovernanceSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/AbExperimentGovernanceSchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AbExperimentGovernanceSchemaTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentGovernanceSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.meta.AbExperimentGovernanceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/AbExperimentGovernanceServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AbExperimentGovernanceServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentGovernanceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.meta.AbExperimentGroupServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/AbExperimentGroupServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AbExperimentGroupServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AbExperimentGroupServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.meta.SystemOptionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/SystemOptionServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps SystemOptionServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  SystemOptionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.meta.TagDefinitionCdpFieldsTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagDefinitionCdpFieldsTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  TagDefinitionCdpFieldsTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.meta.TagImportSourceServiceReactiveTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/TagImportSourceServiceReactiveTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  TagImportSourceServiceReactiveTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertFanoutSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertFanoutSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertFanoutServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAlertFanoutServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyDetectionSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyDetectionSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyDetectionServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorAnomalyDetectionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorInferenceSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorInferenceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollingScheduleServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingScheduleServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollingScheduleServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollingSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingSchedulerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  MarketingMonitorPollingSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollingSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollingSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorPollingServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorPollingServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorPollingServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialRefreshSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialRefreshSchedulerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  MarketingMonitorProviderCredentialRefreshSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderCredentialServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderOAuthAuthorizationSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationSchemaTest assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderOAuthAuthorizationServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

coordinator decision:
  coordinator keeps MarketingMonitorProviderOAuthAuthorizationServiceTest assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  MarketingMonitorProviderOAuthAuthorizationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorProviderPollClientTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderPollClientTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorProviderPollClientTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestionSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookIngestionSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorWebhookIngestionSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookIngestionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookIngestionServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorWebhookIngestionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookPayloadMapperTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookPayloadMapperTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorWebhookPayloadMapperTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitorWebhookSignatureServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookSignatureServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitorWebhookSignatureServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitoringSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitoringSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitoringSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.monitoring.MarketingMonitoringServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitoringServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingMonitoringServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.notification.NotificationEventServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationEventServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps NotificationEventServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  NotificationEventServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.notification.NotificationRealtimeServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationRealtimeServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

coordinator decision:
  coordinator keeps NotificationRealtimeServiceTest assigned to canvas-context-cdp until the owning task pack accepts the row

required tests:
  NotificationRealtimeServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.notification.NotificationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps NotificationServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  NotificationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.notification.NotificationWebSocketHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationWebSocketHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

coordinator decision:
  coordinator keeps NotificationWebSocketHandlerTest assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  NotificationWebSocketHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.notification.NotificationWebSocketTicketServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationWebSocketTicketServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps NotificationWebSocketTicketServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  NotificationWebSocketTicketServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.notification.RuntimeAlertNotificationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/RuntimeAlertNotificationTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

coordinator decision:
  coordinator keeps RuntimeAlertNotificationTest assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  RuntimeAlertNotificationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.paidmedia.PaidMediaAudienceSyncSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceSyncSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceSyncSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.paidmedia.PaidMediaAudienceSyncServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceSyncServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  PaidMediaAudienceSyncServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.policy.MarketingPreferenceCenterServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/policy/MarketingPreferenceCenterServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingPreferenceCenterServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

coordinator decision:
  coordinator keeps ProgrammaticDspMutationSchemaTest assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspMutationServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

coordinator decision:
  coordinator keeps ProgrammaticDspMutationServiceTest assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspMutationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

coordinator decision:
  coordinator keeps ProgrammaticDspSchemaTest assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.programmatic.ProgrammaticDspServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

coordinator decision:
  coordinator keeps ProgrammaticDspServiceTest assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ProgrammaticDspServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.project.CanvasProjectPermissionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/project/CanvasProjectPermissionServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasProjectPermissionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.project.CanvasProjectServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/project/CanvasProjectServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasProjectServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/providerwrite/ProviderWriteEvidenceSanitizerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ProviderWriteEvidenceSanitizerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.providerwrite.SandboxProviderWriteClientTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/providerwrite/SandboxProviderWriteClientTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  SandboxProviderWriteClientTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.RiskControlSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/RiskControlSchemaTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskControlSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.RiskMetricsTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/RiskMetricsTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskMetricsTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.RiskPersistenceMappingTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/RiskPersistenceMappingTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskPersistenceMappingTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.RiskProductionReadinessAuditTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/RiskProductionReadinessAuditTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskProductionReadinessAuditTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleParserTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/dsl/RiskRuleParserTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskRuleParserTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.dsl.RiskRuleValidatorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/dsl/RiskRuleValidatorTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskRuleValidatorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.feature.RedisRiskFeatureStoreTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/feature/RedisRiskFeatureStoreTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RedisRiskFeatureStoreTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.feature.RiskFeatureResolverIntegrationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/feature/RiskFeatureResolverIntegrationTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskFeatureResolverIntegrationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.governance.JdbcRiskListStoreTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/governance/JdbcRiskListStoreTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  JdbcRiskListStoreTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.governance.JdbcRiskSceneStoreTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/governance/JdbcRiskSceneStoreTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  JdbcRiskSceneStoreTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.governance.JdbcRiskStrategyStateStoreTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/governance/JdbcRiskStrategyStateStoreTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  JdbcRiskStrategyStateStoreTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.governance.RiskListServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/governance/RiskListServiceTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskListServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.governance.RiskStrategyRuntimeReaderTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/governance/RiskStrategyRuntimeReaderTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskStrategyRuntimeReaderTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.graph.RiskGraphServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/graph/RiskGraphServiceTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskGraphServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.lab.JdbcRiskSimulationRunRepositoryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/lab/JdbcRiskSimulationRunRepositoryTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  JdbcRiskSimulationRunRepositoryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.lab.JdbcRiskSimulationSampleRepositoryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/lab/JdbcRiskSimulationSampleRepositoryTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  JdbcRiskSimulationSampleRepositoryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.lab.RiskSimulationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/lab/RiskSimulationServiceTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskSimulationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.modeling.RiskModelGatewayTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/modeling/RiskModelGatewayTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskModelGatewayTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.runtime.JdbcRiskDecisionLedgerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/JdbcRiskDecisionLedgerTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  JdbcRiskDecisionLedgerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.runtime.JdbcRiskDecisionTraceReaderTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/JdbcRiskDecisionTraceReaderTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  JdbcRiskDecisionTraceReaderTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionMergerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionMergerTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskDecisionMergerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionPerformanceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionPerformanceTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskDecisionPerformanceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionServiceTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskDecisionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.runtime.RiskDecisionShadowModeTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskDecisionShadowModeTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskDecisionShadowModeTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.runtime.RiskListMatcherTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskListMatcherTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskListMatcherTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.runtime.RiskRuleEvaluatorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskRuleEvaluatorTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskRuleEvaluatorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.risk.runtime.RiskStrategyCompilerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/runtime/RiskStrategyCompilerTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskStrategyCompilerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingCredentialResolverTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingCredentialResolverTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingCredentialResolverTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingImpactWindowServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingImpactWindowServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingImpactWindowServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingMutationSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingMutationSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingMutationSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingMutationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingMutationServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingMutationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingProductionClosedLoopSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingProductionClosedLoopSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingProductionClosedLoopSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderAdapterContractTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingProviderAdapterContractTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderAdapterContractTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderReadGatewayTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingProviderReadGatewayTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderReadGatewayTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingProviderWriteGatewayTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingProviderWriteGatewayTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingProviderWriteGatewayTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingReadinessServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingReadinessServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingReadinessServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingReconciliationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingReconciliationServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingReconciliationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.search.SearchMarketingSyncRunServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingSyncRunServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  SearchMarketingSyncRunServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.task.AsyncTaskServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/task/AsyncTaskServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AsyncTaskServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AsyncTaskServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.template.MessageTemplateServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/template/MessageTemplateServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MessageTemplateServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.tenant.TenantServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/tenant/TenantServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps TenantServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  TenantServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseAggregationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAggregationServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAggregationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityIncidentSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAvailabilityIncidentSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAvailabilityIncidentSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityIncidentServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAvailabilityIncidentServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAvailabilityIncidentServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAvailabilityServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseAvailabilityServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseBackfillServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseBackfillServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseBackfillServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseCatalogSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseCatalogSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseCatalogSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseCatalogServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseCatalogServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseCatalogServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityIncidentSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseConsumerAvailabilityIncidentSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseConsumerAvailabilityIncidentSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityIncidentServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseConsumerAvailabilityIncidentServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseConsumerAvailabilityIncidentServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilitySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseConsumerAvailabilitySchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseConsumerAvailabilitySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseConsumerAvailabilityServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseConsumerAvailabilityServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseDorisPrometheusMetricsParserTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseDorisPrometheusMetricsParserTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  CdpWarehouseDorisPrometheusMetricsParserTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationGateServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseE2eCertificationGateServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationGateSourceModeTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateSourceModeTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseE2eCertificationGateSourceModeTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationRunSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationRunSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseE2eCertificationRunSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationRunServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationRunServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseE2eCertificationRunServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseE2eCertificationSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseE2eDataPathProofSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eDataPathProofSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseE2eDataPathProofSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapEvidenceSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapEvidenceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapReadinessServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapReadinessServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseEnterpriseOlapReadinessServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseExternalRealtimeJobProbeSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseExternalRealtimeJobProbeSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseExternalRealtimeJobProbeSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseExternalRealtimeJobProbeSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseExternalRealtimeJobProbeSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseExternalRealtimeJobProbeSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseExternalRealtimeJobProbeServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseExternalRealtimeJobProbeServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseExternalRealtimeJobProbeServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseFieldGovernanceSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseFieldGovernanceSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseFieldGovernanceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseFieldGovernanceServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseFieldGovernanceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseIncidentSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseIncidentSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseIncidentServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseIncidentServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseJobLeaseSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseJobLeaseSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseJobLeaseServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseJobLeaseServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseJobLeaseServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseMetricChangeReviewSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseMetricChangeReviewSchemaTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  CdpWarehouseMetricChangeReviewSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseMetricChangeReviewServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseMetricChangeReviewServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  CdpWarehouseMetricChangeReviewServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseMetricLineageServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseMetricLineageServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  CdpWarehouseMetricLineageServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseOperationsServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseOperationsServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseOperationsServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePhysicalE2eCertificationDataPathSourceModeTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationDataPathSourceModeTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePhysicalE2eCertificationDataPathSourceModeTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePhysicalE2eCertificationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePhysicalE2eCertificationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyAudienceBitmapRebuildServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyErasureExecutionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyErasureExecutionServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyErasureExecutionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyErasureSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyErasureSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyErasureSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyErasureServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyErasureServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyErasureServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyTombstoneSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyTombstoneSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyTombstoneSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyTombstoneServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyTombstoneServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehousePrivacyTombstoneServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseProductionReadinessProofServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseProductionReadinessProofServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseQualitySchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseQualitySchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseQualitySchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseQualitySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseQualitySchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseQualitySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseQualityServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseQualityServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseQualityServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseReadinessIncidentSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessIncidentSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseReadinessIncidentSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseReadinessIncidentServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessIncidentServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseReadinessIncidentServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseReadinessServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseReadinessServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCheckpointSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeCheckpointSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeCheckpointSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCheckpointServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeCheckpointServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeCheckpointServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCutoverReadinessServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeCutoverReadinessServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeCutoverReadinessServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobControlSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeJobControlSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeJobControlSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobControlServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeJobControlServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeJobControlServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobIncidentSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeJobIncidentSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeJobIncidentSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobIncidentServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeJobIncidentServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeJobIncidentServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePhysicalE2eCertificationSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePhysicalE2eCertificationSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimePhysicalE2eCertificationSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePipelineIncidentServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePipelineIncidentServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimePipelineIncidentServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePipelineSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePipelineSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimePipelineSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePipelineServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePipelineServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimePipelineServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeRetrySchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeRetrySchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeRetrySchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeRetrySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeRetrySchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeRetrySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeRetryServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeRetryServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeRetryServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeSchemaEvolutionSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeSchemaEvolutionSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeSchemaEvolutionSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeSchemaServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeSchemaServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRealtimeSchemaServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRetentionSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRetentionSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRetentionSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseRetentionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRetentionServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseRetentionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseSemanticMetricServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSemanticMetricServiceTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  CdpWarehouseSemanticMetricServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseSloPolicySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSloPolicySchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSloPolicySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseSloPolicyServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSloPolicyServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSloPolicyServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseSyntheticDataPathProbeSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSyntheticDataPathProbeSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSyntheticDataPathProbeSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseSyntheticDataPathProbeServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSyntheticDataPathProbeServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSyntheticDataPathProbeServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseSyntheticDataPathProbeSourceModeTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSyntheticDataPathProbeSourceModeTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseSyntheticDataPathProbeSourceModeTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseTableDriftIncidentSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableDriftIncidentSchedulerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseTableDriftIncidentSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseTableDriftIncidentServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableDriftIncidentServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseTableDriftIncidentServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseTableGovernanceSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableGovernanceSchemaTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseTableGovernanceSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.CdpWarehouseTableGovernanceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableGovernanceServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpWarehouseTableGovernanceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.HttpCdpWarehouseExternalRealtimeJobProbeClientTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/HttpCdpWarehouseExternalRealtimeJobProbeClientTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  HttpCdpWarehouseExternalRealtimeJobProbeClientTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.domain.warehouse.HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClientTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClientTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClientTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.AudienceBatchComputeReactiveBoundaryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBatchComputeReactiveBoundaryTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceBatchComputeReactiveBoundaryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.AudienceBitmapStoreSetOpsTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBitmapStoreSetOpsTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

coordinator decision:
  coordinator keeps AudienceBitmapStoreSetOpsTest assigned to canvas-context-cdp until the owning task pack accepts the row

required tests:
  AudienceBitmapStoreSetOpsTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.AudienceComputeTaskRunnerSpringTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunnerSpringTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceComputeTaskRunnerSpringTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.AudienceComputeTaskRunnerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceComputeTaskRunnerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceComputeTaskRunnerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.AudienceEvaluationContextFetcherTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceEvaluationContextFetcherTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceEvaluationContextFetcherTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.AudienceUserResolverStreamingTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceUserResolverStreamingTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceUserResolverStreamingTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.CdpAudienceSourceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/CdpAudienceSourceServiceTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CdpAudienceSourceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.JdbcConfigResolverTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/JdbcConfigResolverTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  JdbcConfigResolverTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.RuleEvaluatorFailClosedTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/RuleEvaluatorFailClosedTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  RuleEvaluatorFailClosedTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.SqlWhereGeneratorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/SqlWhereGeneratorTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  SqlWhereGeneratorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.StableUserIndexServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/StableUserIndexServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  StableUserIndexServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.audience.VersionedAudienceBitmapStoreTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/VersionedAudienceBitmapStoreTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  VersionedAudienceBitmapStoreTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.channel.ChannelConnectorRegistryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelConnectorRegistryTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

coordinator decision:
  coordinator keeps ChannelConnectorRegistryTest assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ChannelConnectorRegistryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.channel.ChannelConnectorSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelConnectorSchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

coordinator decision:
  coordinator keeps ChannelConnectorSchemaTest assigned to canvas-context-marketing until the owning task pack accepts the row

required tests:
  ChannelConnectorSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.channel.ChannelDedupeServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelDedupeServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  ChannelDedupeServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.channel.ChannelFallbackServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelFallbackServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  ChannelFallbackServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.channel.ChannelProviderPolicySchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ChannelProviderPolicySchemaTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  ChannelProviderPolicySchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.channel.DownstreamBulkheadRegistryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/DownstreamBulkheadRegistryTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  DownstreamBulkheadRegistryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.channel.ProviderBackpressureServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/ProviderBackpressureServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ProviderBackpressureServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.channel.WhatsAppCloudApiConnectorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/channel/WhatsAppCloudApiConnectorTest.java

target module:
  canvas-context-conversation

target package:
  org.chovy.canvas.conversation.test

owning worker:
  DDD-W06

required tests:
  WhatsAppCloudApiConnectorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.concurrent.BackgroundTaskExecutorGovernanceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/concurrent/BackgroundTaskExecutorGovernanceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  BackgroundTaskExecutorGovernanceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.concurrent.BackgroundTaskExecutorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/concurrent/BackgroundTaskExecutorTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  BackgroundTaskExecutorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.context.ExecutionContextBoundsTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextBoundsTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ExecutionContextBoundsTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.context.ExecutionContextConcurrencyTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextConcurrencyTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ExecutionContextConcurrencyTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.context.ExecutionContextMemoryLimitTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextMemoryLimitTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ExecutionContextMemoryLimitTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.context.ExecutionContextNamespaceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextNamespaceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ExecutionContextNamespaceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.context.ExecutionContextPerfRunTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextPerfRunTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ExecutionContextPerfRunTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.delivery.DeliveryOutboxSchemaTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryOutboxSchemaTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  DeliveryOutboxSchemaTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.delivery.DeliveryOutboxServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryOutboxServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  DeliveryOutboxServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.delivery.ReachDeliveryServiceConnectorDispatchTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/ReachDeliveryServiceConnectorDispatchTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ReachDeliveryServiceConnectorDispatchTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.delivery.ReachDeliveryServicePolicyTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/ReachDeliveryServicePolicyTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ReachDeliveryServicePolicyTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.disruptor.CanvasDisruptorRequestLaneIsolationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorRequestLaneIsolationTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasDisruptorRequestLaneIsolationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.disruptor.CanvasDisruptorServiceLifecycleTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorServiceLifecycleTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasDisruptorServiceLifecycleTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.disruptor.CanvasDisruptorServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorServiceTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasDisruptorServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handler.NodeResultV2Test

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handler/NodeResultV2Test.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  NodeResultV2Test

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handler.NodeRouteResolverTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handler/NodeRouteResolverTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  NodeRouteResolverTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.AiLlmHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/AiLlmHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

coordinator decision:
  coordinator keeps AiLlmHandlerTest assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  AiLlmHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.ApiCallHandlerRateLimitTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ApiCallHandlerRateLimitTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ApiCallHandlerRateLimitTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.ApiCallPayloadBuilderTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ApiCallPayloadBuilderTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ApiCallPayloadBuilderTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.BlockingHandlerAssemblyTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/BlockingHandlerAssemblyTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  BlockingHandlerAssemblyTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.CommitActionHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/CommitActionHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CommitActionHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.ConnectedContentHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ConnectedContentHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ConnectedContentHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.CouponHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/CouponHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CouponHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.DirectCallHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/DirectCallHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DirectCallHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.DirectReturnHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/DirectReturnHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DirectReturnHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.EventTriggerHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/EventTriggerHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  EventTriggerHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.GroovyHandlerValidationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/GroovyHandlerValidationTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  GroovyHandlerValidationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.IfConditionHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/IfConditionHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  IfConditionHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.MqTriggerHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/MqTriggerHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  MqTriggerHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.PointsOperationHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/PointsOperationHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  PointsOperationHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.RiskDecisionHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/RiskDecisionHandlerTest.java

target module:
  canvas-context-risk

target package:
  org.chovy.canvas.risk.test

owning worker:
  DDD-W02

required tests:
  RiskDecisionHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.SendMessageHandlerContentReleaseTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMessageHandlerContentReleaseTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  SendMessageHandlerContentReleaseTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.SendMessageHandlerOutboxRoutingTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMessageHandlerOutboxRoutingTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  SendMessageHandlerOutboxRoutingTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.SendMessageHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMessageHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  SendMessageHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.SendMqHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMqHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  SendMqHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.SplitHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SplitHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  SplitHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.StartHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/StartHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  StartHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.SubFlowRefHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SubFlowRefHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  SubFlowRefHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.TaggerHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/TaggerHandlerTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  TaggerHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.TransferJourneyHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/TransferJourneyHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  TransferJourneyHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.UserInputHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/UserInputHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  UserInputHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.handlers.WaitHandlerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/WaitHandlerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  WaitHandlerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.idempotency.NodeSideEffectIdempotencyServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/idempotency/NodeSideEffectIdempotencyServiceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  NodeSideEffectIdempotencyServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.insights.MauticInspiredInsightServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/insights/MauticInspiredInsightServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MauticInspiredInsightServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.lane.DagCostProfilerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/DagCostProfilerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DagCostProfilerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.lane.ExecutionLaneResolverTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ExecutionLaneResolverTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.lane.LaneWorkerIsolationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/LaneWorkerIsolationTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  LaneWorkerIsolationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.lifecycle.ExecutionLifecycleGateTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lifecycle/ExecutionLifecycleGateTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ExecutionLifecycleGateTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.llm.AiLlmGatewayTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/AiLlmGatewayTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AiLlmGatewayTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiLlmGatewayTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.llm.AiUsageAuditServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/AiUsageAuditServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps AiUsageAuditServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  AiUsageAuditServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.llm.OpenAiCompatibleLlmClientTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/OpenAiCompatibleLlmClientTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps OpenAiCompatibleLlmClientTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  OpenAiCompatibleLlmClientTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.plugin.PluginRegistryServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/plugin/PluginRegistryServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

coordinator decision:
  coordinator keeps PluginRegistryServiceTest assigned to canvas-platform until the owning task pack accepts the row

required tests:
  PluginRegistryServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.policy.ContactabilityExplainerServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/ContactabilityExplainerServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  ContactabilityExplainerServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.policy.MarketingPolicyServiceFrequencyPreviewTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceFrequencyPreviewTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingPolicyServiceFrequencyPreviewTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.policy.MarketingPolicyServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingPolicyServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.reactive.BackgroundSubscriptionRegistryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/reactive/BackgroundSubscriptionRegistryTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  BackgroundSubscriptionRegistryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.reactive.BusinessSubscriptionGovernanceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/reactive/BusinessSubscriptionGovernanceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  BusinessSubscriptionGovernanceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.request.AdaptiveRetryBackoffPolicyTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/AdaptiveRetryBackoffPolicyTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  AdaptiveRetryBackoffPolicyTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.request.CanvasExecutionReplayRateLimiterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionReplayRateLimiterTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasExecutionReplayRateLimiterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.request.CanvasExecutionRequestBacklogMetricsTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  CanvasExecutionRequestBacklogMetricsTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.request.CanvasExecutionRequestExecutorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutorTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestExecutorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.request.CanvasExecutionRequestServiceIdempotencyIntegrationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceIdempotencyIntegrationTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestServiceIdempotencyIntegrationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.request.CanvasExecutionRequestServiceIdempotencyTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceIdempotencyTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestServiceIdempotencyTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.request.CanvasExecutionRequestServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasExecutionRequestServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.rule.AudienceDefinitionRuleValidatorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/rule/AudienceDefinitionRuleValidatorTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  AudienceDefinitionRuleValidatorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.rule.CanvasRuleGraphValidatorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/rule/CanvasRuleGraphValidatorTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasRuleGraphValidatorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.rule.RuleEngineAstTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/rule/RuleEngineAstTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  RuleEngineAstTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.rule.RuleSqlCompilerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/rule/RuleSqlCompilerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  RuleSqlCompilerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.CanvasMetricsTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/CanvasMetricsTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  CanvasMetricsTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.CircuitBreakerRegistryConcurrencyTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistryConcurrencyTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  CircuitBreakerRegistryConcurrencyTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.CircuitBreakerRegistryRedisTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistryRedisTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  CircuitBreakerRegistryRedisTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.CircuitBreakerRegistryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistryTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  CircuitBreakerRegistryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.CircuitBreakerStateListenerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/CircuitBreakerStateListenerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  CircuitBreakerStateListenerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.DagEngineCircuitBreakerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineCircuitBreakerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DagEngineCircuitBreakerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.DagEngineCommitActionTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineCommitActionTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DagEngineCommitActionTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.DagEngineContextCommitTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineContextCommitTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DagEngineContextCommitTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.DagEngineDepthTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineDepthTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DagEngineDepthTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.DagEngineFanOutBatchTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineFanOutBatchTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DagEngineFanOutBatchTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.DagEngineLifecycleTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineLifecycleTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DagEngineLifecycleTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.DagEngineManagedSubscriptionTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineManagedSubscriptionTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DagEngineManagedSubscriptionTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.DagEnginePendingTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEnginePendingTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DagEnginePendingTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.DagParserTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagParserTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  DagParserTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.ExecutionDlqWriterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/ExecutionDlqWriterTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

coordinator decision:
  coordinator keeps ExecutionDlqWriterTest assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  ExecutionDlqWriterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.NodeGateCoordinatorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/NodeGateCoordinatorTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  NodeGateCoordinatorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.NodeResultRouterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/NodeResultRouterTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  NodeResultRouterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.NodeTimeoutCoordinatorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/NodeTimeoutCoordinatorTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  NodeTimeoutCoordinatorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.SpecialNodeTimeoutPollerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/SpecialNodeTimeoutPollerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  SpecialNodeTimeoutPollerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.SpecialNodeTimeoutQueueTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/SpecialNodeTimeoutQueueTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  SpecialNodeTimeoutQueueTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.SpecialNodeTimerRaceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/SpecialNodeTimerRaceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  SpecialNodeTimerRaceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.SpecialNodeTraceDurationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/SpecialNodeTraceDurationTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  SpecialNodeTraceDurationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.TraceSinkTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceSinkTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  TraceSinkTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.scheduler.TraceWriteBufferTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceWriteBufferTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  TraceWriteBufferTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.template.TemplateRenderServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/template/TemplateRenderServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  TemplateRenderServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trace.ExecutionTraceContextTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trace/ExecutionTraceContextTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ExecutionTraceContextTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.CanvasEntityCacheTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasEntityCacheTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasEntityCacheTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.CanvasExecutionConfigLoaderTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionConfigLoaderTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasExecutionConfigLoaderTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.CanvasExecutionServiceCdpTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceCdpTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  CanvasExecutionServiceCdpTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.CanvasExecutionServiceResumeTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceResumeTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasExecutionServiceResumeTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.CanvasSchedulerServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasSchedulerServiceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  CanvasSchedulerServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.ExecutionLaneDispatcherTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/ExecutionLaneDispatcherTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ExecutionLaneDispatcherTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.ExecutionLifecycleGateTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/ExecutionLifecycleGateTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ExecutionLifecycleGateTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.InFlightExecutionRegistryConcurrencyTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryConcurrencyTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  InFlightExecutionRegistryConcurrencyTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.InFlightExecutionRegistryLaneTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  InFlightExecutionRegistryLaneTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.TriggerAdmissionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TriggerAdmissionServiceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  TriggerAdmissionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.TriggerPreCheckServiceControlGroupTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TriggerPreCheckServiceControlGroupTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  TriggerPreCheckServiceControlGroupTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.TriggerPreCheckServiceQuotaReconciliationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TriggerPreCheckServiceQuotaReconciliationTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  TriggerPreCheckServiceQuotaReconciliationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.TriggerPriorityConfigTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TriggerPriorityConfigTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  TriggerPriorityConfigTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.trigger.WaitResumeQuotaBypassTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/WaitResumeQuotaBypassTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  WaitResumeQuotaBypassTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.wait.WaitEventFilterTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitEventFilterTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  WaitEventFilterTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.wait.WaitResumeServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitResumeServiceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  WaitResumeServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.engine.wait.WaitSubscriptionServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitSubscriptionServiceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  WaitSubscriptionServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.health.CanvasEngineHealthIndicatorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/health/CanvasEngineHealthIndicatorTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasEngineHealthIndicatorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.cache.CanvasConfigCacheTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/cache/CanvasConfigCacheTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasConfigCacheTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.cache.RocketMqCacheInvalidationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/cache/RocketMqCacheInvalidationTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  RocketMqCacheInvalidationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.mq.MqTriggerConsumerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/mq/MqTriggerConsumerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  MqTriggerConsumerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.mq.OverflowRetryConsumerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/mq/OverflowRetryConsumerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  OverflowRetryConsumerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.redis.CanvasRouteInitializerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/CanvasRouteInitializerTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  CanvasRouteInitializerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.redis.ContextPersistenceIncrementalTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/ContextPersistenceIncrementalTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ContextPersistenceIncrementalTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.redis.ContextPersistenceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/ContextPersistenceServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ContextPersistenceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.redis.KillSwitchSubscriberTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/KillSwitchSubscriberTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  KillSwitchSubscriberTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.redis.MqRouteRefreshServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/MqRouteRefreshServiceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  MqRouteRefreshServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.redis.NodeGateRedisTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/NodeGateRedisTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  NodeGateRedisTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.redis.RedisDelayQueueTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/RedisDelayQueueTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  RedisDelayQueueTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.redis.TriggerRouteRecoveryServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/TriggerRouteRecoveryServiceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  TriggerRouteRecoveryServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infra.redis.TriggerRouteServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/TriggerRouteServiceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  TriggerRouteServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.bi.InMemoryBiQueryResultCacheTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/bi/InMemoryBiQueryResultCacheTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  InMemoryBiQueryResultCacheTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.bi.JdbcBiDatasetExtractMaterializerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/bi/JdbcBiDatasetExtractMaterializerTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  JdbcBiDatasetExtractMaterializerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.bi.JdbcBiQueryExecutorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/bi/JdbcBiQueryExecutorTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  JdbcBiQueryExecutorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.bi.RedisBiQueryResultCacheTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/bi/RedisBiQueryResultCacheTest.java

target module:
  canvas-context-bi

target package:
  org.chovy.canvas.bi.test

owning worker:
  DDD-W05

required tests:
  RedisBiQueryResultCacheTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.concurrent.ManagedVirtualThreadExecutorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/concurrent/ManagedVirtualThreadExecutorTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  ManagedVirtualThreadExecutorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.doris.DorisBehaviorAudienceOlapRepositoryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/DorisBehaviorAudienceOlapRepositoryTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  DorisBehaviorAudienceOlapRepositoryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.doris.DorisCdpEventStreamLoaderTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/DorisCdpEventStreamLoaderTest.java

target module:
  canvas-context-cdp

target package:
  org.chovy.canvas.cdp.test

owning worker:
  DDD-W04

required tests:
  DorisCdpEventStreamLoaderTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.doris.DorisConnectionTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/DorisConnectionTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  DorisConnectionTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.doris.DorisStreamLoaderTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/DorisStreamLoaderTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  DorisStreamLoaderTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.mq.DeliveryOutboxConsumerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/mq/DeliveryOutboxConsumerTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  DeliveryOutboxConsumerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.observability.MdcTaskDecoratorTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/observability/MdcTaskDecoratorTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  MdcTaskDecoratorTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.reactor.BlockingWorkSchedulerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/reactor/BlockingWorkSchedulerTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

owning worker:
  DDD-W08

required tests:
  BlockingWorkSchedulerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.reactor.TrackedReactiveTaskRegistryTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/reactor/TrackedReactiveTaskRegistryTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  TrackedReactiveTaskRegistryTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.infrastructure.redis.KillSwitchSubscriberTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/redis/KillSwitchSubscriberTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  KillSwitchSubscriberTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.migration.FlywayMigrationPolicyTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/migration/FlywayMigrationPolicyTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  FlywayMigrationPolicyTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.migration.ProjectGovernanceMigrationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/migration/ProjectGovernanceMigrationTest.java

target module:
  canvas-context-canvas

target package:
  org.chovy.canvas.canvas.test

owning worker:
  DDD-W07

required tests:
  ProjectGovernanceMigrationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.perf.PerfRunContextTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/perf/PerfRunContextTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  PerfRunContextTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.platform.JdbcMarketingPlatformControlPlaneEvidenceProviderTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/platform/JdbcMarketingPlatformControlPlaneEvidenceProviderTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  JdbcMarketingPlatformControlPlaneEvidenceProviderTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.platform.MarketingPlatformControlPlaneServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/platform/MarketingPlatformControlPlaneServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  MarketingPlatformControlPlaneServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.platform.PlatformWorkstreamContractTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/platform/PlatformWorkstreamContractTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  PlatformWorkstreamContractTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.security.PublicTriggerAuthServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/security/PublicTriggerAuthServiceTest.java

target module:
  canvas-context-execution

target package:
  org.chovy.canvas.execution.test

coordinator decision:
  coordinator keeps PublicTriggerAuthServiceTest assigned to canvas-context-execution until the owning task pack accepts the row

required tests:
  PublicTriggerAuthServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.security.SecretCipherTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/security/SecretCipherTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  SecretCipherTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.strategy.architecture.ArchitectureDeploymentEvidenceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/architecture/ArchitectureDeploymentEvidenceServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  ArchitectureDeploymentEvidenceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.strategy.globalization.RegionalExpansionEvidenceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/globalization/RegionalExpansionEvidenceServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  RegionalExpansionEvidenceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.strategy.growth.ProductLedGrowthEvidenceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceServiceTest.java

target module:
  canvas-context-marketing

target package:
  org.chovy.canvas.marketing.test

owning worker:
  DDD-W03

required tests:
  ProductLedGrowthEvidenceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.strategy.privacy.PrivacyComplianceEvidenceServiceTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/privacy/PrivacyComplianceEvidenceServiceTest.java

target module:
  canvas-platform

target package:
  org.chovy.canvas.platform.test

owning worker:
  DDD-W01

required tests:
  PrivacyComplianceEvidenceServiceTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.AbExperimentGovernanceControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/AbExperimentGovernanceControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  AbExperimentGovernanceControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.AiDecisionControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/AiDecisionControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  AiDecisionControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.AiPredictionControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/AiPredictionControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  AiPredictionControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.AiPromptTemplateControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/AiPromptTemplateControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  AiPromptTemplateControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.AiProviderControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/AiProviderControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  AiProviderControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.AnalyticsControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/AnalyticsControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  AnalyticsControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ApiRequestValidationTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ApiRequestValidationTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ApiRequestValidationTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ApprovalControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ApprovalControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ApprovalControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CanvasBatchOperationControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasBatchOperationControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CanvasBatchOperationControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CanvasControllerApprovalTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasControllerApprovalTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CanvasControllerApprovalTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CanvasControllerOperatorLoopTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasControllerOperatorLoopTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CanvasControllerOperatorLoopTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CanvasProjectControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasProjectControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CanvasProjectControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpComputedProfileControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpComputedProfileControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpComputedProfileControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpComputedTagControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpComputedTagControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpComputedTagControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpEventIngestionControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpEventIngestionControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpEventIngestionControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseAudienceMaterializationControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseAudienceMaterializationControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseAudienceMaterializationControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseAvailabilityControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseAvailabilityControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseAvailabilityControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseAvailabilityIncidentControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseAvailabilityIncidentControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseAvailabilityIncidentControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseCatalogControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseCatalogControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseCatalogControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseConsumerAvailabilityIncidentControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseConsumerAvailabilityIncidentControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseConsumerAvailabilityIncidentControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseE2eCertificationGateControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationGateControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseE2eCertificationGateControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseE2eCertificationRunControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseE2eCertificationRunControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseEnterpriseOlapEvidenceControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseEnterpriseOlapEvidenceControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseEnterpriseOlapEvidenceControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseExternalRealtimeJobProbeControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseExternalRealtimeJobProbeControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseExternalRealtimeJobProbeControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseFieldGovernanceControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseFieldGovernanceControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseFieldGovernanceControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseIncidentControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseIncidentControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseIncidentControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseMetricChangeReviewControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseMetricChangeReviewControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseMetricChangeReviewControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseMetricLineageControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseMetricLineageControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseMetricLineageControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehousePhysicalE2eCertificationControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehousePhysicalE2eCertificationControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehousePhysicalE2eCertificationControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehousePrivacyErasureControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehousePrivacyErasureControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehousePrivacyTombstoneControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehousePrivacyTombstoneControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehousePrivacyTombstoneControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseProductionReadinessControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseProductionReadinessControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseProductionReadinessControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseQualityControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseQualityControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseQualityControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseReadinessControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseReadinessControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseReadinessControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseReadinessIncidentControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseReadinessIncidentControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseReadinessIncidentControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimeControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseRealtimeControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseRealtimeControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimeCutoverReadinessControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseRealtimeCutoverReadinessControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseRealtimeCutoverReadinessControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimeJobControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseRealtimeJobControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseRealtimeJobControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimeJobIncidentControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseRealtimeJobIncidentControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseRealtimeJobIncidentControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimePipelineControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseRealtimePipelineControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseRealtimePipelineControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimePipelineIncidentControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseRealtimePipelineIncidentControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseRealtimePipelineIncidentControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseRealtimeSchemaControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseRealtimeSchemaControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseRealtimeSchemaControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseSemanticMetricControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseSemanticMetricControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseSemanticMetricControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseSloPolicyControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseSloPolicyControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseSloPolicyControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseSyntheticDataPathProbeControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseSyntheticDataPathProbeControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseSyntheticDataPathProbeControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseSyntheticDataPathProbeSourceModeControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseSyntheticDataPathProbeSourceModeControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseSyntheticDataPathProbeSourceModeControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseTableDriftIncidentControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseTableDriftIncidentControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseTableDriftIncidentControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWarehouseTableGovernanceControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseTableGovernanceControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWarehouseTableGovernanceControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CdpWriteKeyControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWriteKeyControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CdpWriteKeyControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ChannelConnectorControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ChannelConnectorControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ChannelConnectorControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ContactabilityControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ContactabilityControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ContactabilityControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ConversationControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ConversationControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ConversationPrivateDomainControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationPrivateDomainControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ConversationPrivateDomainControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ConversationProviderWebhookControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationProviderWebhookControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ConversationProviderWebhookControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ConversationWorkspaceControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationWorkspaceControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ConversationWorkspaceControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.CreatorCollaborationControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CreatorCollaborationControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  CreatorCollaborationControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.DeliveryReceiptControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/DeliveryReceiptControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  DeliveryReceiptControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.EventAttributeDiscoveryControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/EventAttributeDiscoveryControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  EventAttributeDiscoveryControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ExecutionControllerMachineAuthTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerMachineAuthTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ExecutionControllerMachineAuthTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ExecutionControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ExecutionControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ExecutionRerunControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionRerunControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ExecutionRerunControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.GrowthActivityControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/GrowthActivityControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  GrowthActivityControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.HomeOverviewControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/HomeOverviewControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  HomeOverviewControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.LoyaltyControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/LoyaltyControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  LoyaltyControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.MarketingCampaignControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingCampaignControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  MarketingCampaignControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.MarketingFormControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingFormControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  MarketingFormControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.MarketingIntegrationContractControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingIntegrationContractControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  MarketingIntegrationContractControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.MarketingIntegrationContractProbeControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingIntegrationContractProbeControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  MarketingIntegrationContractProbeControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.MarketingMonitorAnomalyControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingMonitorAnomalyControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  MarketingMonitorAnomalyControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.MarketingMonitoringControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingMonitoringControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  MarketingMonitoringControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.MarketingMonitoringWebhookAdminControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingMonitoringWebhookAdminControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  MarketingMonitoringWebhookAdminControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.MarketingPreferenceCenterControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingPreferenceCenterControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  MarketingPreferenceCenterControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.MauticInspiredInsightControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/MauticInspiredInsightControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  MauticInspiredInsightControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.OpsControllerApprovalTaskTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/OpsControllerApprovalTaskTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  OpsControllerApprovalTaskTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.OpsControllerSecurityTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/OpsControllerSecurityTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  OpsControllerSecurityTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.PaidMediaAudienceSyncControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/PaidMediaAudienceSyncControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  PaidMediaAudienceSyncControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.ProgrammaticDspControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ProgrammaticDspControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  ProgrammaticDspControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.PublicConversationWebhookControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/PublicConversationWebhookControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  PublicConversationWebhookControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.PublicMarketingContentUploadWebhookControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/PublicMarketingContentUploadWebhookControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  PublicMarketingContentUploadWebhookControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.PublicMarketingMonitoringWebhookControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/PublicMarketingMonitoringWebhookControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  PublicMarketingMonitoringWebhookControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.RealtimeAudienceControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/RealtimeAudienceControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  RealtimeAudienceControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.SearchMarketingControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/SearchMarketingControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  SearchMarketingControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.UserInputControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/UserInputControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  UserInputControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.WebhookSubscriptionControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/WebhookSubscriptionControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  WebhookSubscriptionControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiAiControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiAiControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiAiControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiBigScreenControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiBigScreenControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiBigScreenControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiCapacityControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiCapacityControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiCapacityControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiChartControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiChartControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiChartControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiDashboardControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDashboardControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiDashboardControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiDatasetControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDatasetControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiDatasetControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiDatasourceControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiDatasourceControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiDatasourceControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiEmbedResourceControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiEmbedResourceControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiEmbedResourceControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiPermissionControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPermissionControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiPermissionControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiPortalControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPortalControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiPortalControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiPortalRuntimeControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPortalRuntimeControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiPortalRuntimeControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiPublishApprovalControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiPublishApprovalControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiPublishApprovalControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiQueryControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiQueryControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiQueryControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiResourceCollaborationControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceCollaborationControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiResourceCollaborationControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiResourceFavoriteControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceFavoriteControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiResourceFavoriteControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiResourceMovementControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceMovementControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiResourceMovementControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiResourceTransferControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiResourceTransferControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiResourceTransferControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiSelfServiceControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiSelfServiceControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiSpreadsheetControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSpreadsheetControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiSpreadsheetControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.bi.BiSubscriptionControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/BiSubscriptionControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  BiSubscriptionControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.risk.RiskDecisionControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/RiskDecisionControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  RiskDecisionControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.risk.RiskLabControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/RiskLabControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  RiskLabControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.risk.RiskListControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/RiskListControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  RiskListControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.risk.RiskSceneControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/RiskSceneControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  RiskSceneControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior

old class:
  org.chovy.canvas.web.risk.RiskStrategyControllerTest

current path:
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/RiskStrategyControllerTest.java

target module:
  canvas-web

target package:
  org.chovy.canvas.web.test

coordinator decision:
  DDD-C09 ports or replaces HTTP compatibility coverage

required tests:
  RiskStrategyControllerTest

compatibility notes:
  port as-is when possible; otherwise replace with contract coverage preserving observed behavior
