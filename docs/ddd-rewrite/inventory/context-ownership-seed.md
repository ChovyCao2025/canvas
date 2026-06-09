# Context Ownership Seed Inventory

This is the initial ownership seed for the DDD rewrite. It is not a substitute
for the full generated inventory, but it gives workers a deterministic starting
point.

---

## Controller Ownership

### Canvas

```text
CanvasController
CanvasBatchOperationController
CanvasCollaborationController
CanvasExecutionManagementController
CanvasExecutionRequestManagementController
CanvasMqTriggerRejectedController
CanvasProjectController
CanvasStatsController
CanvasUserController
ExecutionController
ExecutionRerunController
UserInputController
TestUserController
```

Decision:

- HTTP classes move to `canvas-web`.
- Use cases split between `canvas-context-canvas` and
  `canvas-context-execution`.
- `CanvasExecution*`, `Execution*`, `CanvasMqTriggerRejected*`, and runtime
  trigger endpoints should call execution API.

### Marketing

```text
GrowthActivityController
MarketingCampaignController
MarketingContentController
MarketingFormController
MarketingIntegrationContractController
MarketingIntegrationContractProbeController
MarketingMonitorAnomalyController
MarketingMonitoringController
MarketingMonitoringWebhookAdminController
MarketingPolicyAdminController
MarketingPreferenceCenterController
MauticInspiredInsightController
MessageDeliveryController
MessageSendRecordController
MessageTemplateController
PaidMediaAudienceSyncController
PublicMarketingContentUploadWebhookController
PublicMarketingMonitoringWebhookController
SearchMarketingController
ChannelConnectorController
ContactabilityController
```

Decision:

- HTTP classes move to `canvas-web`.
- Campaign/growth/content/preference/integration contract behavior moves to
  `canvas-context-marketing`.
- Message delivery may stay in marketing when it is marketing-channel behavior;
  if it is pure execution node delivery, coordinator assigns it to execution.

### CDP

```text
AudienceController
CdpComputedProfileController
CdpComputedTagController
CdpEventIngestionController
CdpTagOperationController
CdpUserController
CdpWarehouseAudienceMaterializationController
CdpWarehouseAvailabilityController
CdpWarehouseAvailabilityIncidentController
CdpWarehouseCatalogController
CdpWarehouseConsumerAvailabilityIncidentController
CdpWarehouseController
CdpWarehouseE2eCertificationGateController
CdpWarehouseE2eCertificationRunController
CdpWarehouseEnterpriseOlapEvidenceController
CdpWarehouseExternalRealtimeJobProbeController
CdpWarehouseFieldGovernanceController
CdpWarehouseIncidentController
CdpWarehouseMetricChangeReviewController
CdpWarehouseMetricLineageController
CdpWarehousePhysicalE2eCertificationController
CdpWarehousePrivacyErasureController
CdpWarehousePrivacyTombstoneController
CdpWarehouseProductionReadinessController
CdpWarehouseQualityController
CdpWarehouseReadinessController
CdpWarehouseReadinessIncidentController
CdpWarehouseRealtimeController
CdpWarehouseRealtimeCutoverReadinessController
CdpWarehouseRealtimeJobController
CdpWarehouseRealtimeJobIncidentController
CdpWarehouseRealtimePipelineController
CdpWarehouseRealtimePipelineIncidentController
CdpWarehouseRealtimeSchemaController
CdpWarehouseSemanticMetricController
CdpWarehouseSloPolicyController
CdpWarehouseSyntheticDataPathProbeController
CdpWarehouseTableDriftIncidentController
CdpWarehouseTableGovernanceController
CdpWriteKeyController
RealtimeAudienceController
TagDefinitionController
TagImportController
TagImportSourceController
IdentityTypeController
```

Decision:

- HTTP classes move to `canvas-web`.
- CDP profile/tag/audience/warehouse behavior moves to `canvas-context-cdp`.
- Doris and warehouse clients belong to CDP unless the inventory proves BI owns
  the use case.

### BI

```text
BiAiController
BiBigScreenController
BiCapacityController
BiChartController
BiDashboardController
BiDatasetController
BiDatasourceController
BiEmbedResourceController
BiPermissionController
BiPortalController
BiPortalRuntimeController
BiPublishApprovalController
BiQueryController
BiResourceCollaborationController
BiResourceFavoriteController
BiResourceMovementController
BiResourceTransferController
BiSelfServiceController
BiSpreadsheetController
BiSubscriptionController
```

Decision:

- HTTP classes move to `canvas-web`.
- BI behavior moves to `canvas-context-bi`.

### Risk

```text
RiskDecisionController
RiskLabController
RiskListController
RiskStrategyController
```

Decision:

- HTTP classes move to `canvas-web`.
- Risk behavior moves to `canvas-context-risk`.
- Risk node runtime remains in execution and calls risk API.

### Conversation

```text
ConversationController
ConversationPrivateDomainController
ConversationProviderWebhookController
ConversationWorkspaceController
PublicConversationWebhookController
```

Decision:

- HTTP classes move to `canvas-web`.
- Conversation behavior moves to `canvas-context-conversation`.

### Platform and Administration

```text
AdminController
AnalyticsController
ApiDefinitionController
ApprovalController
AsyncTaskController
AuthController
DataSourceConfigController
DeliveryReceiptController
DemoSandboxController
DlqController
EventAttributeDiscoveryController
EventDefinitionController
HomeOverviewController
LoyaltyController
MarketingPlatformControlPlaneController
MetaController
MqDefinitionController
NotificationController
OpsController
PlatformWorkstreamController
PluginRegistryController
ProgrammaticDspController
SystemOptionController
TechnicalMigrationCandidateController
TenantController
WebhookSubscriptionController
AbExperimentController
AbExperimentGovernanceController
AiDecisionController
AiPredictionController
AiPromptTemplateController
AiProviderController
ApprovalController
CreatorCollaborationController
```

Decision:

- HTTP classes move to `canvas-web`.
- `PlatformWorkstreamController`, `MarketingPlatformControlPlaneController`,
  and `TechnicalMigrationCandidateController` move behavior to
  `canvas-platform`.
- Auth/tenant/system/notification/approval/plugin/async/task ownership requires
  coordinator decision during full inventory.

---

## Persistence Ownership by Prefix

| Prefix pattern | Owner |
| --- | --- |
| `Canvas*` | `canvas-context-canvas`, unless execution-specific |
| `*Execution*`, `CanvasExecution*`, `CanvasWait*`, `CanvasMqTrigger*` | `canvas-context-execution` |
| `Marketing*`, `Growth*`, `Message*`, `Channel*`, `PaidMedia*`, `Programmatic*` | `canvas-context-marketing` by default |
| `Cdp*`, `Audience*`, `Tag*`, `CustomerProfile*`, `IdentityType*` | `canvas-context-cdp` |
| `Bi*` | `canvas-context-bi` |
| `Risk*` | `canvas-context-risk` |
| `Conversation*` | `canvas-context-conversation` |
| `TechnicalMigrationCandidate*`, `PlatformWorkstream*` | `canvas-platform` |
| `Approval*`, `AsyncTask*`, `Notification*`, `Tenant*`, `Sys*` | coordinator decision |

---

## Service Ownership by Current Package

| Current package | Default target |
| --- | --- |
| `org.chovy.canvas.domain.canvas` | `canvas-context-canvas` |
| `org.chovy.canvas.engine` | `canvas-context-execution` |
| `org.chovy.canvas.domain.marketing` | `canvas-context-marketing` |
| `org.chovy.canvas.domain.cdp` | `canvas-context-cdp` |
| `org.chovy.canvas.domain.warehouse` | `canvas-context-cdp` |
| `org.chovy.canvas.domain.bi` | `canvas-context-bi` |
| `org.chovy.canvas.domain.risk` | `canvas-context-risk` |
| `org.chovy.canvas.domain.conversation` | `canvas-context-conversation` |
| `org.chovy.canvas.platform` | `canvas-platform` |
| `org.chovy.canvas.architecture` | `canvas-platform` |
| `org.chovy.canvas.strategy.architecture` | `canvas-platform` |

---

## Ambiguity List

The coordinator must decide these before affected workers code:

```text
approval
async task
notification
auth and tenant administration
plugin registry
AI provider and AI decision ownership
loyalty ownership
message delivery ownership
channel connector ownership
analytics ownership
ab experiment ownership
creator collaboration ownership
ops and admin ownership
```

Rule:

- Ambiguous classes must not be moved into `canvas-common`.
- Ambiguous behavior may get its own context in a later spec if it is large
  enough.
