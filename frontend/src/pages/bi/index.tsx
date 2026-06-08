import { useEffect, useMemo, useRef, useState, type ChangeEvent, type CSSProperties, type DragEvent as ReactDragEvent, type KeyboardEvent as ReactKeyboardEvent, type PointerEvent as ReactPointerEvent, type ReactNode } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  Badge,
  Button,
  Checkbox,
  Col,
  Descriptions,
  Divider,
  Drawer,
  Empty,
  Input,
  List,
  Modal,
  Progress,
  Row,
  Segmented,
  Select,
  Space,
  Steps,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  ApiOutlined,
  AppstoreOutlined,
  AlignCenterOutlined,
  AlignLeftOutlined,
  AlignRightOutlined,
  ArrowDownOutlined,
  ArrowLeftOutlined,
  ArrowRightOutlined,
  ArrowUpOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CopyOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  DeploymentUnitOutlined,
  DownloadOutlined,
  CloudUploadOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  FileSearchOutlined,
  FilterOutlined,
  FolderOpenOutlined,
  LayoutOutlined,
  LinkOutlined,
  LockOutlined,
  MessageOutlined,
  NodeIndexOutlined,
  PlusOutlined,
  PlayCircleOutlined,
  RedoOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  SaveOutlined,
  SearchOutlined,
  SendOutlined,
  SettingOutlined,
  ShareAltOutlined,
  SlidersOutlined,
  StarOutlined,
  StopOutlined,
  SwapOutlined,
  SyncOutlined,
  UndoOutlined,
  VerticalAlignBottomOutlined,
  VerticalAlignMiddleOutlined,
  VerticalAlignTopOutlined,
} from '@ant-design/icons'
import {
  BI_WORKBENCH_SECTIONS,
  DEFAULT_MARKETING_DATASETS,
  DASHBOARD_GRID_ROW_HEIGHT,
  QUICKBI_CHART_PALETTE,
  QUICKBI_CONTROL_PALETTE,
  QUICKBI_DESIGNER_ACTIONS,
  BIG_SCREEN_COMPONENT_LIBRARY,
  addBigScreenLibraryComponent,
  alignDashboardWidgets,
  buildBiResourceTargets,
  buildBigScreenResourceOptions,
  buildBigScreenDraftResource,
  buildVisualEditorDiagnosticRows,
  alignBigScreenLayoutItems,
  bigScreenResourceSummaryRows,
  moveBigScreenLayoutItem,
  resizeBigScreenLayoutItem,
  snapBigScreenLayoutItem,
  updateBigScreenMobileLayout,
  updateBigScreenLayoutItem,
  biRuntimeRouteFromSearchParams,
  upsertBigScreenResource,
  selectBigScreenRuntimeResource,
  buildSpreadsheetResourceOptions,
  buildSpreadsheetDraftResource,
  buildSpreadsheetPivotTable,
  evaluateSpreadsheetCells,
  spreadsheetResourceSummaryRows,
  updateSpreadsheetCell,
  updateSpreadsheetCellStyle,
  updateSpreadsheetCellRange,
  upsertSpreadsheetResource,
  selectSpreadsheetRuntimeResource,
  movePortalMenuItem,
  updatePortalNavigationConfig,
  buildDashboardCloneCommand,
  buildDashboardImportCommand,
  buildDatasourceMultiTableDatasetCommand,
  buildDatasourceRelationshipDiagnosticRows,
  buildDatasourceOnboardingCommand,
  buildSqlDatasetDraftResource,
  buildSqlDatasetParameterDrafts,
  buildSqlDatasetImpactRows,
  buildSqlDatasetReadinessRows,
  buildSqlDatasetSampleProfileRows,
  exportHardeningDiagnosticRows,
  alertAnomalyDiagnosticRows,
  buildDatasourceTableDatasetCommand,
  buildResourceCommentCommand,
  buildResourceFavoriteCommand,
  buildResourceLockCommand,
  buildResourceMoveCommand,
  buildExportApprovalReviewCommand,
  exportAuditDetailRows,
  buildPublishApprovalRequestCommand,
  buildPublishApprovalReviewCommand,
  buildResourceTransferCommand,
  buildDashboardControlOptionQuery,
  buildDashboardInteractionTarget,
  buildDashboardRuntimeStateCommand,
  buildSelfServiceExtractionQuery,
  chartReferenceImpactSummary,
  chartReferenceImpactSummaryFromImpact,
  chartQueryPatchFromDesigner,
  chartQueryFieldsAfterDrop,
  dashboardWidgetGridPlacementForColumns,
  dashboardLayoutColumns,
  dashboardPackageFileName,
  dashboardDefaultRuntimeParameters,
  dashboardRuntimeParametersFromSearchParams,
  dashboardRuntimeSearchParamKeys,
  dashboardRuntimeStateRows,
  dashboardRuntimeControlValue,
  dashboardRuntimeFilterLocked,
  dashboardResponsiveWidgets,
  dashboardDesignerKeyboardActionFromEventLike,
  resolveDashboardRuntimeParameters,
  stripDashboardRuntimeSearchParam,
  stripDashboardRuntimeSearchParams,
  updateDashboardRuntimeParameters,
  dropSelfServiceExtractionField,
  buildEmbedTicketRequest,
  buildEmbedTicketPreviewRows,
  buildBiPermissionResourceTargets,
  buildWidgetQueryRequest,
  chartLabel,
  controlLabel,
  duplicateDashboardWidget,
  filterDesignerItems,
  getBiSection,
  getDashboardWidget,
  getDefaultDashboardPreset,
  interactionLabel,
  moveDashboardWidget,
  moveDashboardWidgetByPixels,
  parseDashboardPackageText,
  removeDashboardWidget,
  resizeDashboardWidgetByPixels,
  removeSelfServiceExtractionField,
  resourceCommentScopeLabel,
  resourceFavoriteLabel,
  resourceFolderLabel,
  resourceLocationIndexKey,
  resourceLockLabel,
  resourceLockTokenFor,
  resourceOwnerLabel,
  exportApprovalStatusLabel,
  isCancelableExportJob,
  publishApprovalStatusLabel,
  datasourceCapacityPolicyRows,
  datasourceAdvancedCapabilityRows,
  datasourceConnectorRows,
  datasourceConnectionTestRows,
  datasourceHealthHistoryRows,
  datasourceHealthSloRows,
  datasourceNextActionRows,
  datasourceOnboardingRows,
  datasourceSchemaPreviewRows,
  datasourceSchemaSnapshotHistoryRows,
  datasourceSchemaSnapshotRows,
  datasetAccelerationPolicyRows,
  datasetAccelerationSchedulerRows,
  buildQuickEngineCapacityAlertPolicyCommand,
  buildQuickEngineTenantPoolPolicyCommand,
  permissionAuditRows,
  quickEngineCapacityDetailRows,
  quickEngineConcurrencyQueueRows,
  quickEngineCapacitySummaryRows,
  quickEngineCapacityUserRows,
  buildQueryCachePolicyCommand,
  queryCacheInvalidationActionRows,
  queryCachePolicyRows,
  queryCacheStatsRows,
  queryExecutionPlanRows,
  queryCancellationStatusLabel,
  queryGovernanceAuditRows,
  queryGovernancePolicyRows,
  queryGovernanceSummaryRows,
  queryHistoryDetailRows,
  toResourceFavoriteIndex,
  toResourceLocationIndex,
  toResourceOwnershipIndex,
  toMarketingDatasetPreset,
  reorderPortalMenuTree,
  updatePortalMenuConfig,
  createDashboardPresetHistory,
  pushDashboardPresetHistory,
  redoDashboardPresetHistory,
  undoDashboardPresetHistory,
  type BiDashboardPresetLike,
  type BiDashboardFilterPreset,
  type BiDashboardRuntimeParameters,
  type BiDashboardRuntimeStateRow,
  type BiDashboardWidgetPreset,
  type DashboardWidgetAlignment,
  type DashboardLayoutMode,
  type DashboardWidgetMoveDirection,
  type BiRuntimeRouteLike,
  type BiSectionKey,
  type BiDatasourceOnboardingDraftInputLike,
  type BiSqlDatasetFieldDraftLike,
  type BiSqlDatasetMetricDraftLike,
  type BiSqlDatasetParameterDraftLike,
  type BiSqlDatasetReadinessRow,
  type BiDatasourceRelationshipDiagnosticRow,
  type BiVisualEditorDiagnosticRow,
  type BiExportHardeningDiagnosticRow,
  type BiAlertAnomalyDiagnosticRow,
  type BiSpreadsheetPivotAggregation,
  type MarketingDatasetPreset,
  type SelfServiceExtractionState,
} from './biWorkbench'
import {
  biApi,
  type BiCompiledQuery,
  type BiDeliveryAttachmentCleanupResult,
  type BiDeliveryAuditSummary,
  type BiAlertRuleView,
  type BiChartResource,
  type BiChartReferenceImpact,
  type BiChartVersionView,
  type BiDatasetFieldResource,
  type BiDatasetResource,
  type BiDatasetAccelerationPolicyView,
  type BiDatasetAccelerationSchedulerResult,
  type BiDatasetVersionView,
  type BiSqlDatasetPreviewResult,
  type BiDatasetView,
  type BiDatasourceConnectorCapability,
  type BiDatasourceApiPreview,
  type BiDatasourceConnectionTestResult,
  type BiDatasourceHealth,
  type BiDatasourceHealthSloSummary,
  type BiDatasourceHealthSnapshot,
  type BiDatasourceOnboardingView,
  type BiDatasourceSchemaPreview,
  type BiDatasourceSchemaSnapshotView,
  type BiDatasourceTablePreview,
  type BiDashboardExportPackage,
  type BiDashboardPreset,
  type BiDashboardResource,
  type BiDashboardRuntimeStateView,
  type BiDashboardVersionView,
  type BiDeliveryAttachmentView,
  type BiDeliveryLogView,
  type BiDeliveryRetryResult,
  type BiDeliverySchedulerResult,
  type BiEmbedTicket,
  type BiExportCleanupResult,
  type BiExportJobDetailView,
  type BiExportJobView,
  type BiExportRetryResult,
  type BiBigScreenResource,
  type BiBigScreenVersionView,
  type BiPortalResource,
  type BiPortalVersionView,
  type BiSpreadsheetResource,
  type BiSpreadsheetVersionView,
  type BiColumnPermissionView,
  type BiPermissionAuditEntry,
  type BiPermissionRequestView,
  type BiQueryCacheInvalidationResult,
  type BiQueryCacheInvalidationCommand,
  type BiQueryCachePolicyView,
  type BiQueryCacheStats,
  type BiQuickEngineCapacitySummary,
  type BiQueryColumn,
  type BiQueryCancellationResult,
  type BiQueryExplanation,
  type BiQueryGovernanceAuditEntry,
  type BiQueryGovernancePolicyView,
  type BiQueryGovernanceSummary,
  type BiQueryHistoryDetail,
  type BiQueryHistoryItem,
  type BiQueryResult,
  type BiResourceFavoriteView,
  type BiResourceCommentView,
  type BiResourceLocationView,
  type BiResourceLockView,
  type BiResourceOwnershipView,
  type BiPublishApprovalView,
  type BiResourcePermissionView,
  type BiRowPermissionView,
  type BiSubscriptionView,
} from '../../services/biApi'

const { Text, Title } = Typography

const sectionIcons: Record<BiSectionKey, JSX.Element> = {
  overview: <AppstoreOutlined />,
  data: <DatabaseOutlined />,
  dataset: <DeploymentUnitOutlined />,
  chart: <BarChartOutlined />,
  dashboard: <LayoutOutlined />,
  portal: <ApiOutlined />,
  'self-service': <FileSearchOutlined />,
  subscription: <SendOutlined />,
  embed: <LinkOutlined />,
  ai: <RobotOutlined />,
}

const actionIcons: Record<string, JSX.Element> = {
  save: <SaveOutlined />,
  undo: <UndoOutlined />,
  redo: <RedoOutlined />,
  preview: <PlayCircleOutlined />,
  publish: <ShareAltOutlined />,
  clone: <CopyOutlined />,
  export: <DownloadOutlined />,
  import: <CloudUploadOutlined />,
  subscribe: <SendOutlined />,
  embed: <LinkOutlined />,
  archive: <DeleteOutlined />,
}

const datasetColumns: ColumnsType<MarketingDatasetPreset> = [
  {
    title: '数据集',
    dataIndex: 'label',
    render: (label: string, row) => (
      <Space direction="vertical" size={2}>
        <Space size={8}>
          <Text strong>{label}</Text>
          <Tag color="blue">{row.key}</Tag>
        </Space>
        <Text type="secondary" style={{ fontSize: 12 }}>{row.source}</Text>
      </Space>
    ),
  },
  {
    title: '指标',
    dataIndex: 'metrics',
    render: (metrics: string[]) => (
      <Space size={[4, 4]} wrap>
        {metrics.map(metric => <Tag key={metric}>{metric}</Tag>)}
      </Space>
    ),
  },
  {
    title: '状态',
    dataIndex: 'preset',
    width: 110,
    render: (preset: boolean) => preset ? <Badge status="processing" text="预置" /> : <Badge status="default" text="自建" />,
  },
]

const queryHistoryColumns: ColumnsType<BiQueryHistoryItem> = [
  {
    title: '数据集',
    dataIndex: 'datasetKey',
    render: (datasetKey: string, row) => (
        <Space direction="vertical" size={0}>
          <Space size={6}>
          <Tag color={queryStatusColor(row.status)}>{row.status}</Tag>
          <Text strong style={{ fontSize: 12 }}>{datasetKey}</Text>
        </Space>
        <Text type="secondary" style={{ fontSize: 11 }}>{row.username} · {row.createdAt}</Text>
      </Space>
    ),
  },
  {
    title: '结果',
    width: 116,
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Text style={{ fontSize: 12 }}>{row.rowCount} 行</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>{row.durationMs} ms</Text>
      </Space>
    ),
  },
  {
    title: 'SQL Hash',
    dataIndex: 'sqlHash',
    width: 108,
    render: (sqlHash: string) => <Text code style={{ fontSize: 11 }}>{sqlHash?.slice(0, 8) || '-'}</Text>,
  },
]

function isSqlDatasetResource(row: BiDatasetResource): boolean {
  return row.datasetType?.toUpperCase() === 'SQL'
    || row.model?.sqlApprovalRequired === true
}

const datasetResourceColumns: ColumnsType<BiDatasetResource> = [
  {
    title: '资产',
    dataIndex: 'name',
    render: (name: string, row) => (
      <Space direction="vertical" size={2}>
        <Space size={6}>
          <Text strong>{name}</Text>
          <Tag color={row.status === 'PUBLISHED' ? 'green' : row.status === 'DRAFT' ? 'gold' : 'default'}>{row.status}</Tag>
          {isSqlDatasetResource(row) && <Tag color="volcano">SQL 审批</Tag>}
        </Space>
        <Text type="secondary" style={{ fontSize: 12 }}>{row.datasetKey}</Text>
      </Space>
    ),
  },
  {
    title: '来源',
    dataIndex: 'tableExpression',
    render: (tableExpression: string, row) => (
      <Space direction="vertical" size={0}>
        <Text code style={{ fontSize: 12 }}>{tableExpression}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>{row.datasetType} · tenant: {row.tenantColumn}</Text>
        {isSqlDatasetResource(row) && (
          <Text type="warning" style={{ fontSize: 11 }}>发布前必须审批</Text>
        )}
      </Space>
    ),
  },
  {
    title: '模型',
    width: 150,
    render: (_, row) => (
      <Space size={[4, 4]} wrap>
        <Tag>{row.fields.length} 字段</Tag>
        <Tag color="blue">{row.metrics.length} 指标</Tag>
      </Space>
    ),
  },
]

const dashboardVersionColumns: ColumnsType<BiDashboardVersionView> = [
  {
    title: '版本',
    width: 96,
    render: (_, row) => (
      <Space size={6}>
        <Tag color="blue">v{row.version}</Tag>
        <Tag color={row.status === 'PUBLISHED' ? 'green' : 'default'}>{row.status}</Tag>
      </Space>
    ),
  },
  {
    title: '快照',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Text strong style={{ fontSize: 12 }}>{row.preset.title}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>
          {row.preset.widgets.length} 组件 · {row.preset.filters.length} 查询控件 · {row.preset.interactions.length} 交互
        </Text>
      </Space>
    ),
  },
  {
    title: '发布人',
    width: 120,
    render: (_, row) => <Text style={{ fontSize: 12 }}>{row.publishedBy ?? '-'}</Text>,
  },
  {
    title: '发布时间',
    width: 150,
    render: (_, row) => <Text type="secondary" style={{ fontSize: 12 }}>{formatAttachmentTime(row.createdAt)}</Text>,
  },
]

const chartVersionColumns: ColumnsType<BiChartVersionView> = [
  {
    title: '版本',
    width: 96,
    render: (_, row) => (
      <Space size={6}>
        <Tag color="blue">v{row.version}</Tag>
        <Tag color={row.status === 'PUBLISHED' ? 'green' : 'default'}>{row.status}</Tag>
      </Space>
    ),
  },
  {
    title: '图表快照',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Text strong style={{ fontSize: 12 }}>{row.resource.name}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>
          {chartLabel(row.resource.chartType)} · {row.resource.datasetKey} · {row.resource.query.metrics.length} 指标
        </Text>
      </Space>
    ),
  },
  {
    title: '发布人',
    width: 120,
    render: (_, row) => <Text style={{ fontSize: 12 }}>{row.publishedBy ?? '-'}</Text>,
  },
  {
    title: '发布时间',
    width: 150,
    render: (_, row) => <Text type="secondary" style={{ fontSize: 12 }}>{formatAttachmentTime(row.createdAt)}</Text>,
  },
]

const datasetVersionColumns: ColumnsType<BiDatasetVersionView> = [
  {
    title: '版本',
    width: 96,
    render: (_, row) => (
      <Space size={6}>
        <Tag color="blue">v{row.version}</Tag>
        <Tag color={row.status === 'PUBLISHED' ? 'green' : 'default'}>{row.status}</Tag>
      </Space>
    ),
  },
  {
    title: '数据集快照',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Text strong style={{ fontSize: 12 }}>{row.resource.name}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>
          {row.resource.datasetType} · {row.resource.fields.length} 字段 · {row.resource.metrics.length} 指标
        </Text>
      </Space>
    ),
  },
  {
    title: '发布人',
    width: 120,
    render: (_, row) => <Text style={{ fontSize: 12 }}>{row.publishedBy ?? '-'}</Text>,
  },
  {
    title: '发布时间',
    width: 150,
    render: (_, row) => <Text type="secondary" style={{ fontSize: 12 }}>{formatAttachmentTime(row.createdAt)}</Text>,
  },
]

const portalVersionColumns: ColumnsType<BiPortalVersionView> = [
  {
    title: '版本',
    width: 96,
    render: (_, row) => (
      <Space size={6}>
        <Tag color="blue">v{row.version}</Tag>
        <Tag color={row.status === 'PUBLISHED' ? 'green' : 'default'}>{row.status}</Tag>
      </Space>
    ),
  },
  {
    title: '门户快照',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Text strong style={{ fontSize: 12 }}>{row.resource.name}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>
          {row.resource.menus.length} 菜单 · {String(row.resource.theme.theme ?? 'default')}
        </Text>
      </Space>
    ),
  },
  {
    title: '发布人',
    width: 120,
    render: (_, row) => <Text style={{ fontSize: 12 }}>{row.publishedBy ?? '-'}</Text>,
  },
  {
    title: '发布时间',
    width: 150,
    render: (_, row) => <Text type="secondary" style={{ fontSize: 12 }}>{formatAttachmentTime(row.createdAt)}</Text>,
  },
]

const bigScreenVersionColumns: ColumnsType<BiBigScreenVersionView> = [
  {
    title: '版本',
    width: 96,
    render: (_, row) => (
      <Space size={6}>
        <Tag color="blue">v{row.version}</Tag>
        <Tag color={row.status === 'PUBLISHED' ? 'green' : 'default'}>{row.status}</Tag>
      </Space>
    ),
  },
  {
    title: '大屏快照',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Text strong style={{ fontSize: 12 }}>{row.resource.name}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>
          {row.resource.screenKey} · {row.resource.layout.length} 组件 · {String(row.resource.size.width ?? '-')}x{String(row.resource.size.height ?? '-')}
        </Text>
      </Space>
    ),
  },
  {
    title: '发布人',
    width: 120,
    render: (_, row) => <Text style={{ fontSize: 12 }}>{row.publishedBy ?? '-'}</Text>,
  },
  {
    title: '发布时间',
    width: 150,
    render: (_, row) => <Text type="secondary" style={{ fontSize: 12 }}>{formatAttachmentTime(row.createdAt)}</Text>,
  },
]

const spreadsheetVersionColumns: ColumnsType<BiSpreadsheetVersionView> = [
  {
    title: '版本',
    width: 96,
    render: (_, row) => (
      <Space size={6}>
        <Tag color="blue">v{row.version}</Tag>
        <Tag color={row.status === 'PUBLISHED' ? 'green' : 'default'}>{row.status}</Tag>
      </Space>
    ),
  },
  {
    title: '电子表格快照',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Text strong style={{ fontSize: 12 }}>{row.resource.name}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>
          {row.resource.spreadsheetKey} · {row.resource.sheets.length} 工作表 · {String(row.resource.dataBinding.datasetKey ?? '-')}
        </Text>
      </Space>
    ),
  },
  {
    title: '发布人',
    width: 120,
    render: (_, row) => <Text style={{ fontSize: 12 }}>{row.publishedBy ?? '-'}</Text>,
  },
  {
    title: '发布时间',
    width: 150,
    render: (_, row) => <Text type="secondary" style={{ fontSize: 12 }}>{formatAttachmentTime(row.createdAt)}</Text>,
  },
]

const portalResourceColumns: ColumnsType<BiPortalResource> = [
  {
    title: '门户',
    dataIndex: 'name',
    render: (name: string, row) => (
      <Space direction="vertical" size={2}>
        <Space size={6}>
          <Text strong>{name}</Text>
          <Tag color={row.status === 'PUBLISHED' ? 'green' : row.status === 'DRAFT' ? 'gold' : 'default'}>{row.status}</Tag>
        </Space>
        <Text type="secondary" style={{ fontSize: 12 }}>{row.portalKey}</Text>
      </Space>
    ),
  },
  {
    title: '菜单',
    dataIndex: 'menus',
    render: (menus: BiPortalResource['menus']) => (
      <Space size={[4, 4]} wrap>
        {menus.slice(0, 4).map(menu => (
          <Tag key={menu.menuKey}>{menu.title} · {menu.resourceType}</Tag>
        ))}
        {menus.length > 4 && <Tag>+{menus.length - 4}</Tag>}
      </Space>
    ),
  },
  {
    title: '主题',
    width: 150,
    render: (_, row) => (
      <Space size={[4, 4]} wrap>
        <Tag>{String(row.theme.theme ?? 'default')}</Tag>
        <Tag color="blue">{row.menus.length} 菜单</Tag>
      </Space>
    ),
  },
]

const resourceLocationColumns: ColumnsType<BiResourceLocationView> = [
  {
    title: '资源',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Space size={6}>
          <Tag color={resourceTypeColor(row.resourceType)}>{row.resourceType}</Tag>
          <Text strong style={{ fontSize: 12 }}>{row.resourceKey}</Text>
        </Space>
        <Text type="secondary" style={{ fontSize: 11 }}>workspace #{row.workspaceId}</Text>
      </Space>
    ),
  },
  {
    title: '文件夹',
    width: 150,
    render: (_, row) => <Tag color={row.folderKey ? 'blue' : 'default'}>{resourceFolderLabel(row)}</Tag>,
  },
  {
    title: '排序',
    dataIndex: 'sortOrder',
    width: 72,
    render: (sortOrder: number) => <Text style={{ fontSize: 12 }}>{sortOrder}</Text>,
  },
  {
    title: '移动信息',
    width: 190,
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Text style={{ fontSize: 12 }}>{row.movedBy ?? '-'}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>{formatAttachmentTime(row.movedAt)}</Text>
      </Space>
    ),
  },
]

const resourceOwnershipColumns: ColumnsType<BiResourceOwnershipView> = [
  {
    title: '资源',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Space size={6}>
          <Tag color={resourceTypeColor(row.resourceType)}>{row.resourceType}</Tag>
          <Text strong style={{ fontSize: 12 }}>{row.resourceKey}</Text>
        </Space>
        <Text type="secondary" style={{ fontSize: 11 }}>workspace #{row.workspaceId}</Text>
      </Space>
    ),
  },
  {
    title: '负责人',
    width: 170,
    render: (_, row) => <Tag color="purple">{resourceOwnerLabel(row)}</Tag>,
  },
  {
    title: '转让信息',
    width: 190,
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Text style={{ fontSize: 12 }}>{row.transferredBy ?? '-'}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>{formatAttachmentTime(row.transferredAt)}</Text>
      </Space>
    ),
  },
]

const resourceFavoriteColumns: ColumnsType<BiResourceFavoriteView> = [
  {
    title: '资源',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Space size={6}>
          <Tag color={resourceTypeColor(row.resourceType)}>{row.resourceType}</Tag>
          <Text strong style={{ fontSize: 12 }}>{row.resourceKey}</Text>
        </Space>
        <Text type="secondary" style={{ fontSize: 11 }}>workspace #{row.workspaceId}</Text>
      </Space>
    ),
  },
  {
    title: '收藏人',
    width: 150,
    render: (_, row) => <Tag color="gold">{row.username}</Tag>,
  },
  {
    title: '状态',
    width: 112,
    render: (_, row) => <Tag color={row.favorite ? 'gold' : 'default'}>{resourceFavoriteLabel(row)}</Tag>,
  },
  {
    title: '收藏时间',
    width: 150,
    render: (_, row) => <Text type="secondary" style={{ fontSize: 12 }}>{formatAttachmentTime(row.createdAt)}</Text>,
  },
]

/** 创建资源权限表格列，注入删除状态以便单行展示 loading。 */
function createResourcePermissionColumns(
  deletingPermission: string | null,
  onDelete: (row: BiResourcePermissionView) => void,
): ColumnsType<BiResourcePermissionView> {
  return [
    {
      title: '资源',
      render: (_, row) => (
        <Space direction="vertical" size={0}>
          <Space size={6}>
            <Tag color="blue">{row.resourceType}</Tag>
            <Text strong style={{ fontSize: 12 }}>{row.resourceKey ?? row.resourceId}</Text>
          </Space>
          <Text type="secondary" style={{ fontSize: 11 }}>workspace #{row.workspaceId}</Text>
        </Space>
      ),
    },
    {
      title: '主体',
      width: 128,
      render: (_, row) => <Tag>{permissionSubjectLabel(row.subjectType, row.subjectId)}</Tag>,
    },
    {
      title: '动作',
      width: 108,
      render: (_, row) => <Tag color={row.effect === 'DENY' ? 'red' : 'green'}>{row.actionKey} · {row.effect}</Tag>,
    },
    {
      title: '操作',
      width: 72,
      render: (_, row) => (
        <Button
          size="small"
          danger
          aria-label={`删除资源权限 #${row.id}`}
          icon={<DeleteOutlined />}
          loading={deletingPermission === `resource-${row.id}`}
          onClick={() => onDelete(row)}
        />
      ),
    },
  ]
}

/** 创建行权限表格列，展示数据集行级过滤规则和删除入口。 */
function createRowPermissionColumns(
  deletingPermission: string | null,
  onDelete: (row: BiRowPermissionView) => void,
): ColumnsType<BiRowPermissionView> {
  return [
    {
      title: '规则',
      render: (_, row) => (
        <Space direction="vertical" size={0}>
          <Space size={6}>
            <Text strong style={{ fontSize: 12 }}>{row.ruleKey}</Text>
            <Tag color={row.enabled ? 'green' : 'default'}>{row.enabled ? '启用' : '停用'}</Tag>
          </Space>
          <Text type="secondary" style={{ fontSize: 11 }}>{row.datasetKey}</Text>
        </Space>
      ),
    },
    {
      title: '主体',
      width: 128,
      render: (_, row) => <Tag>{permissionSubjectLabel(row.subjectType, row.subjectId)}</Tag>,
    },
    {
      title: '过滤',
      width: 180,
      render: (_, row) => <Text code ellipsis style={{ maxWidth: 170, display: 'block', fontSize: 11 }}>{row.filterJson}</Text>,
    },
    {
      title: '操作',
      width: 72,
      render: (_, row) => (
        <Button
          size="small"
          danger
          aria-label={`删除行权限 #${row.id}`}
          icon={<DeleteOutlined />}
          loading={deletingPermission === `row-${row.id}`}
          onClick={() => onDelete(row)}
        />
      ),
    },
  ]
}

/** 创建列权限表格列，展示字段级允许、拒绝或脱敏策略。 */
function createColumnPermissionColumns(
  deletingPermission: string | null,
  onDelete: (row: BiColumnPermissionView) => void,
): ColumnsType<BiColumnPermissionView> {
  return [
    {
      title: '字段',
      render: (_, row) => (
        <Space direction="vertical" size={0}>
          <Space size={6}>
            <Text strong style={{ fontSize: 12 }}>{row.fieldKey}</Text>
            <Tag color={row.policy === 'DENY' ? 'red' : row.policy === 'MASK' ? 'gold' : 'green'}>{row.policy}</Tag>
          </Space>
          <Text type="secondary" style={{ fontSize: 11 }}>{row.datasetKey}</Text>
        </Space>
      ),
    },
    {
      title: '主体',
      width: 128,
      render: (_, row) => <Tag>{permissionSubjectLabel(row.subjectType, row.subjectId)}</Tag>,
    },
    {
      title: '脱敏',
      width: 130,
      render: (_, row) => row.maskJson ? <Text code ellipsis style={{ maxWidth: 120, display: 'block', fontSize: 11 }}>{row.maskJson}</Text> : <Text type="secondary">-</Text>,
    },
    {
      title: '操作',
      width: 72,
      render: (_, row) => (
        <Button
          size="small"
          danger
          aria-label={`删除列权限 #${row.id}`}
          icon={<DeleteOutlined />}
          loading={deletingPermission === `column-${row.id}`}
          onClick={() => onDelete(row)}
        />
      ),
    },
  ]
}

function createExportJobColumns(
  reviewingExport: string | null,
  cancellingExportId: number | null,
  onReview: (row: BiExportJobView, status: 'APPROVED' | 'REJECTED') => void,
  onInspect: (row: BiExportJobView) => void,
  onCancel: (row: BiExportJobView) => void,
): ColumnsType<BiExportJobView> {
  return [
  {
    title: '任务',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Space size={6}>
          <Text strong style={{ fontSize: 12 }}>#{row.id}</Text>
          <Tag color={exportStatusColor(row.status)}>{row.status}</Tag>
          {row.approvalStatus && (
            <Tag color={publishApprovalStatusColor(row.approvalStatus)}>
              {exportApprovalStatusLabel(row.approvalStatus)}
            </Tag>
          )}
        </Space>
        <Text type="secondary" style={{ fontSize: 11 }}>{row.resourceKey ?? row.resourceId} · {row.createdBy ?? '-'}</Text>
        {row.approvalReason && (
          <Text type="secondary" ellipsis style={{ maxWidth: 220, fontSize: 11 }}>审批：{row.approvalReason}</Text>
        )}
        <Text type={isExpiredAt(row.expiresAt) || row.status === 'EXPIRED' ? 'danger' : 'secondary'} style={{ fontSize: 11 }}>
          {(isExpiredAt(row.expiresAt) || row.status === 'EXPIRED') ? '已过期' : `过期 ${formatAttachmentTime(row.expiresAt)}`} · 下载 {row.downloadCount ?? 0}
        </Text>
        <Progress
          percent={exportProgressPercent(row)}
          size="small"
          status={exportProgressStatus(row)}
          showInfo={false}
          style={{ width: 160 }}
        />
        {row.status === 'FAILED' && (
          <Text type={row.retryExhaustedAt ? 'danger' : 'secondary'} style={{ fontSize: 11 }}>
            {row.retryExhaustedAt
              ? `重试耗尽 ${row.retryCount ?? 0}/${row.maxRetryCount ?? 0}`
              : `下次重试 ${formatAttachmentTime(row.nextRetryAt)}`}
          </Text>
        )}
      </Space>
    ),
  },
  {
    title: '格式',
    width: 88,
    render: (_, row) => <Tag>{row.exportFormat}</Tag>,
  },
  {
    title: '行数',
    width: 88,
    dataIndex: 'rowLimit',
  },
  {
    title: '操作',
    width: 132,
    render: (_, row) => {
      const expired = isExpiredAt(row.expiresAt) || row.status === 'EXPIRED'
      const disabled = !row.fileUrl || row.status !== 'COMPLETED' || expired
      return (
        <Space size={4}>
          <Tooltip title="审计详情">
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => onInspect(row)}
            />
          </Tooltip>
          <Tooltip title={exportJobTooltip(row)}>
            <span style={{ display: 'inline-block' }}>
              <Button
                size="small"
                icon={<DownloadOutlined />}
                disabled={disabled}
                onClick={() => row.fileUrl && window.open(row.fileUrl, '_blank')}
              />
            </span>
          </Tooltip>
          {isCancelableExportJob(row) && (
            <Tooltip title="取消导出">
              <Button
                size="small"
                danger
                aria-label={`取消导出 #${row.id}`}
                icon={<StopOutlined />}
                loading={cancellingExportId === row.id}
                onClick={() => onCancel(row)}
              />
            </Tooltip>
          )}
          {row.status === 'PENDING_APPROVAL' && row.approvalStatus === 'PENDING' && (
            <>
              <Tooltip title="批准导出">
                <Button
                  size="small"
                  aria-label={`批准导出 #${row.id}`}
                  icon={<CheckCircleOutlined />}
                  loading={reviewingExport === `${row.id}:APPROVED`}
                  onClick={() => onReview(row, 'APPROVED')}
                />
              </Tooltip>
              <Tooltip title="驳回导出">
                <Button
                  size="small"
                  danger
                  aria-label={`驳回导出 #${row.id}`}
                  icon={<ExclamationCircleOutlined />}
                  loading={reviewingExport === `${row.id}:REJECTED`}
                  onClick={() => onReview(row, 'REJECTED')}
                />
              </Tooltip>
            </>
          )}
        </Space>
      )
    },
  },
  ]
}

const subscriptionColumns: ColumnsType<BiSubscriptionView> = [
  {
    title: '订阅',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Space size={6}>
          <Text strong style={{ fontSize: 12 }}>{row.name}</Text>
          <Tag color={row.enabled ? 'green' : 'default'}>{row.enabled ? '启用' : '停用'}</Tag>
        </Space>
        <Text type="secondary" style={{ fontSize: 11 }}>{row.subscriptionKey}</Text>
      </Space>
    ),
  },
  {
    title: '资源',
    width: 170,
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Tag color="blue">{row.resourceType}</Tag>
        <Text type="secondary" ellipsis style={{ maxWidth: 150, fontSize: 11 }}>{row.resourceKey ?? row.resourceId}</Text>
      </Space>
    ),
  },
  {
    title: '周期',
    width: 140,
    render: (_, row) => <Text style={{ fontSize: 12 }}>{scheduleLabel(row.schedule)}</Text>,
  },
  {
    title: '渠道',
    width: 150,
    render: (_, row) => <ChannelTags receivers={row.receivers} />,
  },
]

const alertRuleColumns: ColumnsType<BiAlertRuleView> = [
  {
    title: '告警',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Space size={6}>
          <Text strong style={{ fontSize: 12 }}>{row.name}</Text>
          <Tag color={row.enabled ? 'green' : 'default'}>{row.enabled ? '启用' : '停用'}</Tag>
        </Space>
        <Text type="secondary" style={{ fontSize: 11 }}>{row.alertKey}</Text>
      </Space>
    ),
  },
  {
    title: '指标',
    width: 180,
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Text style={{ fontSize: 12 }}>{row.metricKey}</Text>
        <Text type="secondary" style={{ fontSize: 11 }}>{row.datasetKey}</Text>
      </Space>
    ),
  },
  {
    title: '条件',
    width: 150,
    render: (_, row) => <Text style={{ fontSize: 12 }}>{alertConditionLabel(row.condition)}</Text>,
  },
  {
    title: '渠道',
    width: 150,
    render: (_, row) => <ChannelTags receivers={row.receivers} />,
  },
]

const deliveryLogColumns: ColumnsType<BiDeliveryLogView> = [
  {
    title: '任务',
    render: (_, row) => (
      <Space direction="vertical" size={0}>
        <Space size={6}>
          <Tag color={row.jobType === 'ALERT' ? 'orange' : 'blue'}>{row.jobType}</Tag>
          <Text strong style={{ fontSize: 12 }}>{row.jobKey}</Text>
        </Space>
        <Text type="secondary" style={{ fontSize: 11 }}>{row.createdAt ?? '-'}</Text>
      </Space>
    ),
  },
  {
    title: '渠道',
    width: 110,
    render: (_, row) => <Tag>{row.channel}</Tag>,
  },
  {
    title: '状态',
    width: 136,
    render: (_, row) => <DeliveryStatusCell row={row} />,
  },
  {
    title: '指标值',
    width: 96,
    render: (_, row) => row.metricValue == null ? <Text type="secondary">-</Text> : <Text>{String(row.metricValue)}</Text>,
  },
  {
    title: '附件',
    width: 154,
    render: (_, row) => <AttachmentLinks attachments={deliveryAttachmentsFromPayload(row.payload)} />,
  },
  {
    title: '说明',
    width: 180,
    render: (_, row) => <Text ellipsis style={{ maxWidth: 170, display: 'block', fontSize: 12 }}>{row.errorMessage ?? row.message ?? '-'}</Text>,
  },
]

function defaultDatasourceDraft(connectorType = 'MYSQL'): BiDatasourceOnboardingDraftInputLike {
  return {
    connectorType,
    name: '',
    url: '',
    username: '',
    password: '',
    driverClassName: '',
    description: '',
    enabled: true,
    connectionMode: 'DIRECT_QUERY',
    apiRequestMethod: 'GET',
    apiAuthType: 'NONE',
    apiHeaderName: '',
    apiHeaderValue: '',
    apiParameterName: '',
    apiParameterValue: '',
    apiBodyTemplate: '',
    apiResponseRowsPath: '$',
    apiResponseFormat: 'JSON',
    fileName: '',
    fileType: 'CSV',
    fileSheetName: '',
    fileDelimiter: ',',
    fileHeaderRow: true,
    fileEncoding: 'UTF-8',
  }
}

type SqlDatasetMetricDraft = BiSqlDatasetMetricDraftLike & {
  allowedDimensionsText?: string | null
}

interface SpreadsheetPivotMetricDraft {
  id: string
  field: string
  aggregation: BiSpreadsheetPivotAggregation
  label: string
}

type SpreadsheetPivotDropRole = 'ROW' | 'COLUMN' | 'METRIC'

interface SpreadsheetPivotPreviewRow {
  rowKey: string
  cells: Array<{ cellKey: string; value: string }>
}

type ResourceLocationTableRow = BiResourceLocationView & { __tableRowKey: string }
type ResourceOwnershipTableRow = BiResourceOwnershipView & { __tableRowKey: string }
type ResourceFavoriteTableRow = BiResourceFavoriteView & { __tableRowKey: string }

const SPREADSHEET_PIVOT_AGGREGATION_OPTIONS: Array<{ label: string; value: BiSpreadsheetPivotAggregation }> = [
  { label: '求和', value: 'SUM' },
  { label: '计数', value: 'COUNT' },
  { label: '平均值', value: 'AVERAGE' },
  { label: '最小值', value: 'MIN' },
  { label: '最大值', value: 'MAX' },
]

function defaultSpreadsheetPivotMetrics(): SpreadsheetPivotMetricDraft[] {
  return [
    { id: 'metric-1', field: '消耗', aggregation: 'SUM', label: '消耗' },
    { id: 'metric-2', field: '转化', aggregation: 'COUNT', label: '转化次数' },
  ]
}

function spreadsheetPivotFieldsFromSheet(
  sheet: Record<string, unknown> | null | undefined,
  sourceRange: string,
): string[] {
  const cells = sheet?.cells
  if (!cells || typeof cells !== 'object') return []
  const [startCell, endCell] = sourceRange.split(':').map(item => item.trim()).filter(Boolean)
  const start = parseSpreadsheetCellAddress(startCell ?? sourceRange)
  const end = parseSpreadsheetCellAddress(endCell ?? startCell ?? sourceRange)
  if (!start || !end) return []
  const headerRow = Math.min(start.row, end.row)
  const fromColumn = Math.min(start.column, end.column)
  const toColumn = Math.max(start.column, end.column)
  const cellMap = cells as Record<string, unknown>
  const fields: string[] = []
  for (let column = fromColumn; column <= toColumn; column += 1) {
    const value = cellMap[`${spreadsheetColumnName(column)}${headerRow}`]
    const field = String(value ?? '').trim()
    if (field && !fields.includes(field)) fields.push(field)
  }
  return fields
}

function parseSpreadsheetCellAddress(value: string): { column: number; row: number } | null {
  const match = value.trim().toUpperCase().match(/^([A-Z]+)([0-9]+)$/)
  if (!match) return null
  return {
    column: match[1].split('').reduce((total, char) => total * 26 + char.charCodeAt(0) - 64, 0),
    row: Number(match[2]),
  }
}

function spreadsheetColumnName(index: number): string {
  let remaining = index
  let name = ''
  while (remaining > 0) {
    const offset = (remaining - 1) % 26
    name = String.fromCharCode(65 + offset) + name
    remaining = Math.floor((remaining - offset - 1) / 26)
  }
  return name
}

function spreadsheetPivotPreviewColumns(
  sheet: Record<string, unknown> | null | undefined,
  sourceRange: string,
  columnField: string,
  metrics: SpreadsheetPivotMetricDraft[],
): string[] {
  const cells = sheet?.cells
  if (!cells || typeof cells !== 'object') return []
  const [startCell, endCell] = sourceRange.split(':').map(item => item.trim()).filter(Boolean)
  const start = parseSpreadsheetCellAddress(startCell ?? sourceRange)
  const end = parseSpreadsheetCellAddress(endCell ?? startCell ?? sourceRange)
  if (!start || !end) return []
  const fromColumn = Math.min(start.column, end.column)
  const toColumn = Math.max(start.column, end.column)
  const fromRow = Math.min(start.row, end.row)
  const toRow = Math.max(start.row, end.row)
  const cellMap = cells as Record<string, unknown>
  const headerRow = fromRow
  let columnFieldIndex = -1
  for (let column = fromColumn; column <= toColumn; column += 1) {
    const header = String(cellMap[`${spreadsheetColumnName(column)}${headerRow}`] ?? '').trim()
    if (header === columnField.trim()) {
      columnFieldIndex = column
      break
    }
  }
  if (columnFieldIndex < 0) return []
  const labels: string[] = []
  for (let row = fromRow + 1; row <= toRow; row += 1) {
    const label = String(cellMap[`${spreadsheetColumnName(columnFieldIndex)}${row}`] ?? '').trim()
    if (label && !labels.includes(label)) labels.push(label)
  }
  const activeMetrics = metrics
    .map(metric => ({ field: metric.field.trim(), label: metric.label.trim() || metric.field.trim() }))
    .filter(metric => metric.field)
  if (activeMetrics.length === 0) return labels
  return labels.flatMap(label => activeMetrics.map(metric => `${label} ${metric.label}`))
}

function spreadsheetPivotPreviewRows(
  sheet: Record<string, unknown> | null | undefined,
  sourceRange: string,
  targetCell: string,
  rowField: string,
  columnField: string,
  metrics: SpreadsheetPivotMetricDraft[],
): SpreadsheetPivotPreviewRow[] {
  if (!sheet) return []
  const target = parseSpreadsheetCellAddress(targetCell)
  if (!target) return []
  const activeMetrics = metrics
    .map(metric => ({
      field: metric.field.trim(),
      aggregation: metric.aggregation,
      label: metric.label.trim() || metric.field.trim(),
    }))
    .filter(metric => metric.field)
  if (!activeMetrics.length) return []
  const sheetKey = String(sheet.sheetKey ?? 'summary')
  const preview = buildSpreadsheetPivotTable({
    spreadsheetKey: 'pivot-preview',
    name: 'Pivot Preview',
    description: null,
    sheets: [{ ...sheet }],
    dataBinding: {},
    style: {},
    status: 'DRAFT',
    version: 0,
    source: 'PREVIEW',
  }, sheetKey, {
    sourceRange,
    targetCell,
    rowField,
    columnField,
    valueField: activeMetrics[0].field,
    aggregation: activeMetrics[0].aggregation,
    valueFields: activeMetrics,
  })
  const previewSheet = preview.sheets[0] ?? {}
  const cells = (previewSheet.cells && typeof previewSheet.cells === 'object') ? previewSheet.cells as Record<string, unknown> : {}
  const pivotTable = Array.isArray(previewSheet.pivotTables) ? previewSheet.pivotTables[previewSheet.pivotTables.length - 1] as Record<string, unknown> : null
  const rowCount = 1 + (Array.isArray(pivotTable?.rowLabels) ? pivotTable.rowLabels.length : 0)
  const columnLabelCount = Array.isArray(pivotTable?.columnLabels) ? pivotTable.columnLabels.length : 0
  const columnCount = 1 + Math.max(1, columnLabelCount * activeMetrics.length)
  return Array.from({ length: Math.min(rowCount, 5) }, (_, rowOffset) => ({
    rowKey: `preview-row-${rowOffset}`,
    cells: Array.from({ length: Math.min(columnCount, 8) }, (_, columnOffset) => {
      const cellKey = `${spreadsheetColumnName(target.column + columnOffset)}${target.row + rowOffset}`
      const value = cells[cellKey]
      return { cellKey, value: value == null ? '' : String(value) }
    }),
  }))
}

function defaultSqlDatasetFields(): BiSqlDatasetFieldDraftLike[] {
  return [
    {
      fieldKey: 'tenant_id',
      displayName: 'Tenant',
      columnExpression: 'tenant_id',
      role: 'DIMENSION',
      dataType: 'NUMBER',
    },
    {
      fieldKey: 'stat_date',
      displayName: 'Date',
      columnExpression: 'stat_date',
      role: 'DIMENSION',
      dataType: 'DATE',
    },
    {
      fieldKey: 'channel',
      displayName: 'Channel',
      columnExpression: 'channel',
      role: 'DIMENSION',
      dataType: 'STRING',
    },
  ]
}

function defaultSqlDatasetMetrics(): SqlDatasetMetricDraft[] {
  return [
    {
      metricKey: 'total_cost',
      displayName: 'Cost',
      expression: 'SUM(total_cost)',
      aggregation: 'SUM',
      dataType: 'NUMBER',
      allowedDimensionsText: 'stat_date,channel',
    },
  ]
}

type DatasourceModelingJoinConditionOperator = '=' | '<>' | '>' | '>=' | '<' | '<='

const DATASOURCE_MODELING_JOIN_CONDITION_OPERATORS: DatasourceModelingJoinConditionOperator[] = ['=', '<>', '>', '>=', '<', '<=']
const DATASOURCE_MODELING_JOIN_CONDITION_CONNECTORS: Array<'AND' | 'OR'> = ['AND', 'OR']

type DatasourceModelingJoinConditionDraft = {
  leftColumn: string
  operator: DatasourceModelingJoinConditionOperator
  connector?: 'AND' | 'OR'
  rightColumn: string
  groupStart?: boolean
  groupEnd?: boolean
}

type DatasourceModelingJoinDraft = {
  joinType: string
  leftTableName: string
  leftColumn: string
  rightTableName: string
  rightColumn: string
  conditions: DatasourceModelingJoinConditionDraft[]
}

function normalizeDatasourceModelingJoinConditionOperator(operator?: string | null): DatasourceModelingJoinConditionOperator {
  const value = String(operator ?? '=').trim()
  return DATASOURCE_MODELING_JOIN_CONDITION_OPERATORS.includes(value as DatasourceModelingJoinConditionOperator)
    ? value as DatasourceModelingJoinConditionOperator
    : '='
}

function swapDatasourceModelingJoinConditionOperator(operator?: string | null): DatasourceModelingJoinConditionOperator {
  const normalized = normalizeDatasourceModelingJoinConditionOperator(operator)
  if (normalized === '>') return '<'
  if (normalized === '>=') return '<='
  if (normalized === '<') return '>'
  if (normalized === '<=') return '>='
  return normalized
}

function datasourceModelingJoinConditionSummary(conditions: DatasourceModelingJoinDraft['conditions']): string {
  if (conditions.length === 0) return '0 个条件'
  const pairs = conditions.map((condition, index) => {
    const expression = `${condition.groupStart ? '(' : ''}${condition.leftColumn} ${condition.operator} ${condition.rightColumn}${condition.groupEnd ? ')' : ''}`
    if (index === 0) return expression
    return `${condition.connector === 'OR' ? '或' : '且'} ${expression}`
  })
  if (pairs.length <= 2) return pairs.join(' ')
  return `${pairs.slice(0, 2).join(' ')} 等 ${pairs.length} 个条件`
}

function datasourceModelingJoinGroupsBalanced(conditions: DatasourceModelingJoinDraft['conditions']): boolean {
  let depth = 0
  for (const condition of conditions) {
    if (condition.groupStart) depth += 1
    if (condition.groupEnd) depth -= 1
    if (depth < 0) return false
  }
  return depth === 0
}

type ApiPreviewVariableDraft = {
  name: string
  value: string
}

const MAX_API_PREVIEW_VARIABLES = 20
const DATASOURCE_MODELING_GRAPH_WIDTH = 880
const DATASOURCE_MODELING_GRAPH_HEIGHT = 260
const DATASOURCE_MODELING_GRAPH_NODE_WIDTH = 156
const DATASOURCE_MODELING_GRAPH_NODE_HEIGHT = 56

type DatasourceModelingGraphNode = {
  tableName: string
  alias: string
  x: number
  y: number
}

type DatasourceModelingGraphDragState = {
  tableName: string
  alias: string
  startClientX: number
  startClientY: number
  originX: number
  originY: number
}

/** BI 工作台主页面，集中承载数据集、图表、仪表盘、门户、订阅、导出和权限配置。 */
export default function BiWorkbenchPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const dashboardPackageInputRef = useRef<HTMLInputElement | null>(null)
  const dashboardKey = searchParams.get('dashboard') || 'canvas-effect'
  const canvasId = searchParams.get('canvasId')
  const runtimeParameterKey = searchParams.toString()
  const runtimeRoute = useMemo(
    () => biRuntimeRouteFromSearchParams(searchParams),
    [runtimeParameterKey],
  )
  // 图表查询条件的 draftRef 用于保存按钮读取最新输入，避免受 React 异步状态批处理影响。
  const [sectionKey, setSectionKey] = useState<BiSectionKey>('dashboard')
  const [datasets, setDatasets] = useState<MarketingDatasetPreset[]>(DEFAULT_MARKETING_DATASETS)
  const [datasetMetadata, setDatasetMetadata] = useState<BiDatasetView | null>(null)
  const [datasetResources, setDatasetResources] = useState<BiDatasetResource[]>([])
  const [selectedDatasetKey, setSelectedDatasetKey] = useState<string | null>(null)
  const [selectedDatasetFieldKeys, setSelectedDatasetFieldKeys] = useState('')
  const [datasetFieldFolderKey, setDatasetFieldFolderKey] = useState('')
  const [selectedDatasetDetailFieldKey, setSelectedDatasetDetailFieldKey] = useState<string | null>(null)
  const [datasetBatchRole, setDatasetBatchRole] = useState<'DIMENSION' | 'MEASURE'>('DIMENSION')
  const [datasetBatchFormat, setDatasetBatchFormat] = useState('')
  const [datasetBatchVisible, setDatasetBatchVisible] = useState(true)
  const [datasetDragFieldKey, setDatasetDragFieldKey] = useState<string | null>(null)
  const [datasetDropTargetFieldKey, setDatasetDropTargetFieldKey] = useState<string | null>(null)
  const [datasetDropPosition, setDatasetDropPosition] = useState<'before' | 'after'>('before')
  const [datasetDropFolderKey, setDatasetDropFolderKey] = useState('')
  const [datasetVersions, setDatasetVersions] = useState<BiDatasetVersionView[]>([])
  const [chartResources, setChartResources] = useState<BiChartResource[]>([])
  const [selectedChartKey, setSelectedChartKey] = useState<string | null>(null)
  const [chartVersions, setChartVersions] = useState<BiChartVersionView[]>([])
  const [chartReferenceImpacts, setChartReferenceImpacts] = useState<Record<string, BiChartReferenceImpact>>({})
  const [chartFilterFieldDraft, setChartFilterFieldDraft] = useState('')
  const [chartFilterOperatorDraft, setChartFilterOperatorDraft] = useState('IN')
  const [chartFilterValueDraft, setChartFilterValueDraft] = useState('')
  const [chartSortFieldDraft, setChartSortFieldDraft] = useState('')
  const [chartSortDirectionDraft, setChartSortDirectionDraft] = useState('DESC')
  const chartFilterFieldDraftRef = useRef('')
  const chartFilterOperatorDraftRef = useRef('IN')
  const chartFilterValueDraftRef = useRef('')
  const chartSortFieldDraftRef = useRef('')
  const chartSortDirectionDraftRef = useRef('DESC')
  const [portalResources, setPortalResources] = useState<BiPortalResource[]>([])
  const [selectedPortalKey, setSelectedPortalKey] = useState<string | null>(null)
  const [selectedPortalMenuKey, setSelectedPortalMenuKey] = useState<string | null>(null)
  const [portalDragMenuKey, setPortalDragMenuKey] = useState<string | null>(null)
  const [portalDropTargetMenuKey, setPortalDropTargetMenuKey] = useState<string | null>(null)
  const [portalDropPosition, setPortalDropPosition] = useState<'before' | 'after' | 'inside'>('inside')
  const [portalVersions, setPortalVersions] = useState<BiPortalVersionView[]>([])
  const [portalRuntimeResources, setPortalRuntimeResources] = useState<BiPortalResource[]>([])
  const [bigScreenResources, setBigScreenResources] = useState<BiBigScreenResource[]>([])
  const [selectedBigScreenKey, setSelectedBigScreenKey] = useState<string | null>(null)
  const [bigScreenVersions, setBigScreenVersions] = useState<BiBigScreenVersionView[]>([])
  const [spreadsheetResources, setSpreadsheetResources] = useState<BiSpreadsheetResource[]>([])
  const [selectedSpreadsheetKey, setSelectedSpreadsheetKey] = useState<string | null>(null)
  const [selectedBigScreenWidgetKey, setSelectedBigScreenWidgetKey] = useState<string | null>(null)
  const [selectedBigScreenComponentKey, setSelectedBigScreenComponentKey] = useState(BIG_SCREEN_COMPONENT_LIBRARY[0]?.key ?? 'metric-card')
  const [selectedBigScreenAlignWidgetKeys, setSelectedBigScreenAlignWidgetKeys] = useState<string[]>([])
  const [bigScreenSnapGuideCount, setBigScreenSnapGuideCount] = useState(0)
  const [selectedSpreadsheetSheetKey, setSelectedSpreadsheetSheetKey] = useState<string | null>(null)
  const [selectedSpreadsheetCellKey, setSelectedSpreadsheetCellKey] = useState('A1')
  const [spreadsheetFillRange, setSpreadsheetFillRange] = useState('')
  const [spreadsheetPivotSourceRange, setSpreadsheetPivotSourceRange] = useState('A1:D5')
  const [spreadsheetPivotTargetCell, setSpreadsheetPivotTargetCell] = useState('F1')
  const [spreadsheetPivotRowField, setSpreadsheetPivotRowField] = useState('地区')
  const [spreadsheetPivotColumnField, setSpreadsheetPivotColumnField] = useState('渠道')
  const [spreadsheetPivotMetrics, setSpreadsheetPivotMetrics] = useState<SpreadsheetPivotMetricDraft[]>(() => defaultSpreadsheetPivotMetrics())
  const [spreadsheetVersions, setSpreadsheetVersions] = useState<BiSpreadsheetVersionView[]>([])
  const [dashboardPreset, setDashboardPreset] = useState<BiDashboardPresetLike>(() => getDefaultDashboardPreset(dashboardKey))
  const [dashboardHistory, setDashboardHistory] = useState(() => createDashboardPresetHistory(dashboardPreset))
  const [dashboardResource, setDashboardResource] = useState<BiDashboardResource | null>(null)
  const [dashboardVersions, setDashboardVersions] = useState<BiDashboardVersionView[]>([])
  const [dashboardExportPackage, setDashboardExportPackage] = useState<BiDashboardExportPackage | null>(null)
  const [dashboardRuntimeState, setDashboardRuntimeState] = useState<BiDashboardRuntimeStateView | null>(null)
  const [clearedDashboardRuntimeParameterKeys, setClearedDashboardRuntimeParameterKeys] = useState<string[]>([])
  const [selectedWidgetKey, setSelectedWidgetKey] = useState<string>(dashboardPreset.widgets[0]?.widgetKey)
  const [selectedWidgetKeys, setSelectedWidgetKeys] = useState<string[]>(() =>
    dashboardPreset.widgets[0]?.widgetKey ? [dashboardPreset.widgets[0].widgetKey] : [])
  const [dashboardLayoutMode, setDashboardLayoutMode] = useState<DashboardLayoutMode>('desktop')
  const [configTab, setConfigTab] = useState('data')
  const [leftTab, setLeftTab] = useState('data')
  const [designerSearch, setDesignerSearch] = useState('')
  const [loadingDatasets, setLoadingDatasets] = useState(false)
  const [loadingDatasetResources, setLoadingDatasetResources] = useState(false)
  const [loadingDatasetVersions, setLoadingDatasetVersions] = useState(false)
  const [loadingCharts, setLoadingCharts] = useState(false)
  const [loadingChartVersions, setLoadingChartVersions] = useState(false)
  const [loadingDashboardVersions, setLoadingDashboardVersions] = useState(false)
  const [loadingPortals, setLoadingPortals] = useState(false)
  const [loadingPortalVersions, setLoadingPortalVersions] = useState(false)
  const [loadingPortalRuntime, setLoadingPortalRuntime] = useState(false)
  const [loadingBigScreens, setLoadingBigScreens] = useState(false)
  const [loadingBigScreenVersions, setLoadingBigScreenVersions] = useState(false)
  const [loadingSpreadsheets, setLoadingSpreadsheets] = useState(false)
  const [loadingSpreadsheetVersions, setLoadingSpreadsheetVersions] = useState(false)
  const [loadingQueries, setLoadingQueries] = useState(false)
  const [loadingGovernance, setLoadingGovernance] = useState(false)
  const [savingDashboard, setSavingDashboard] = useState(false)
  const [publishingDashboard, setPublishingDashboard] = useState(false)
  const [cloningDashboard, setCloningDashboard] = useState(false)
  const [exportingDashboard, setExportingDashboard] = useState(false)
  const [importingDashboard, setImportingDashboard] = useState(false)
  const [archivingDashboard, setArchivingDashboard] = useState(false)
  const [restoringDashboardVersion, setRestoringDashboardVersion] = useState<number | null>(null)
  const [restoringChartVersion, setRestoringChartVersion] = useState<number | null>(null)
  const [restoringDatasetVersion, setRestoringDatasetVersion] = useState<number | null>(null)
  const [restoringPortalVersion, setRestoringPortalVersion] = useState<number | null>(null)
  const [savingDataset, setSavingDataset] = useState<string | null>(null)
  const [savingChart, setSavingChart] = useState<string | null>(null)
  const [savingPortal, setSavingPortal] = useState<string | null>(null)
  const [savingBigScreen, setSavingBigScreen] = useState<string | null>(null)
  const [publishingBigScreen, setPublishingBigScreen] = useState<string | null>(null)
  const [archivingBigScreen, setArchivingBigScreen] = useState<string | null>(null)
  const [restoringBigScreenVersion, setRestoringBigScreenVersion] = useState<number | null>(null)
  const [savingSpreadsheet, setSavingSpreadsheet] = useState<string | null>(null)
  const [publishingSpreadsheet, setPublishingSpreadsheet] = useState<string | null>(null)
  const [archivingSpreadsheet, setArchivingSpreadsheet] = useState<string | null>(null)
  const [restoringSpreadsheetVersion, setRestoringSpreadsheetVersion] = useState<number | null>(null)
  const [queryResults, setQueryResults] = useState<Record<string, BiQueryResult>>({})
  const [controlOptionResults, setControlOptionResults] = useState<Record<string, BiQueryResult>>({})
  const [compiledWidgetQuery, setCompiledWidgetQuery] = useState<BiCompiledQuery | null>(null)
  const [queryExecutionPlan, setQueryExecutionPlan] = useState<BiQueryExplanation | null>(null)
  const [compilingWidgetKey, setCompilingWidgetKey] = useState<string | null>(null)
  const [explainingWidgetKey, setExplainingWidgetKey] = useState<string | null>(null)
  const [queryHistory, setQueryHistory] = useState<BiQueryHistoryItem[]>([])
  const [loadingControlOptions, setLoadingControlOptions] = useState(false)
  const [queryHistoryDetailOpen, setQueryHistoryDetailOpen] = useState(false)
  const [queryHistoryDetail, setQueryHistoryDetail] = useState<BiQueryHistoryDetail | null>(null)
  const [loadingQueryHistoryDetail, setLoadingQueryHistoryDetail] = useState(false)
  const [cancellingQueryHash, setCancellingQueryHash] = useState<string | null>(null)
  const [queryCancellationResult, setQueryCancellationResult] = useState<BiQueryCancellationResult | null>(null)
  const [queryGovernanceSummary, setQueryGovernanceSummary] = useState<BiQueryGovernanceSummary | null>(null)
  const [queryGovernancePolicy, setQueryGovernancePolicy] = useState<BiQueryGovernancePolicyView | null>(null)
  const [queryGovernanceAudit, setQueryGovernanceAudit] = useState<BiQueryGovernanceAuditEntry[]>([])
  const [queryCachePolicy, setQueryCachePolicy] = useState<BiQueryCachePolicyView | null>(null)
  const [queryCacheStats, setQueryCacheStats] = useState<BiQueryCacheStats | null>(null)
  const [quickEngineCapacity, setQuickEngineCapacity] = useState<BiQuickEngineCapacitySummary | null>(null)
  const [queryPolicyTimeoutMs, setQueryPolicyTimeoutMs] = useState('30000')
  const [queryPolicyQuotaRows, setQueryPolicyQuotaRows] = useState('1000000')
  const [savingQueryGovernancePolicy, setSavingQueryGovernancePolicy] = useState(false)
  const [queryCacheDefaultEnabled, setQueryCacheDefaultEnabled] = useState(true)
  const [queryCacheTtlSeconds, setQueryCacheTtlSeconds] = useState('300')
  const [queryCacheResourceScope, setQueryCacheResourceScope] = useState<'DASHBOARD' | 'DATASET'>('DASHBOARD')
  const [queryCacheResourceEnabled, setQueryCacheResourceEnabled] = useState(true)
  const [queryCacheResourceCacheMode, setQueryCacheResourceCacheMode] = useState('CACHE')
  const [queryCacheResourceTtlSeconds, setQueryCacheResourceTtlSeconds] = useState('300')
  const [savingQueryCachePolicy, setSavingQueryCachePolicy] = useState(false)
  const [invalidatingQueryCache, setInvalidatingQueryCache] = useState(false)
  const [queryCacheInvalidationResult, setQueryCacheInvalidationResult] = useState<BiQueryCacheInvalidationResult | null>(null)
  const [quickEngineAlertEnabled, setQuickEngineAlertEnabled] = useState(true)
  const [quickEngineCapacityLimitRows, setQuickEngineCapacityLimitRows] = useState('1000000')
  const [quickEngineWarningThreshold, setQuickEngineWarningThreshold] = useState('80')
  const [quickEngineCriticalThreshold, setQuickEngineCriticalThreshold] = useState('95')
  const [quickEngineNotificationChannels, setQuickEngineNotificationChannels] = useState('')
  const [quickEngineNotificationReceivers, setQuickEngineNotificationReceivers] = useState('')
  const [savingQuickEngineCapacityPolicy, setSavingQuickEngineCapacityPolicy] = useState(false)
  const [quickEnginePoolKey, setQuickEnginePoolKey] = useState('STANDARD')
  const [quickEngineMaxConcurrentQueries, setQuickEngineMaxConcurrentQueries] = useState('8')
  const [quickEngineQueueLimit, setQuickEngineQueueLimit] = useState('50')
  const [quickEngineQueueTimeoutSeconds, setQuickEngineQueueTimeoutSeconds] = useState('120')
  const [quickEnginePoolWeight, setQuickEnginePoolWeight] = useState('100')
  const [savingQuickEngineTenantPoolPolicy, setSavingQuickEngineTenantPoolPolicy] = useState(false)
  const [datasetAccelerationPolicy, setDatasetAccelerationPolicy] = useState<BiDatasetAccelerationPolicyView | null>(null)
  const [datasetAccelerationEnabled, setDatasetAccelerationEnabled] = useState(true)
  const [datasetAccelerationMode, setDatasetAccelerationMode] = useState('EXTRACT')
  const [datasetAccelerationRefreshMode, setDatasetAccelerationRefreshMode] = useState('SCHEDULED')
  const [datasetAccelerationIntervalMinutes, setDatasetAccelerationIntervalMinutes] = useState('60')
  const [datasetAccelerationTtlSeconds, setDatasetAccelerationTtlSeconds] = useState('300')
  const [datasetAccelerationMaxRows, setDatasetAccelerationMaxRows] = useState('100000')
  const [datasetAccelerationCronExpression, setDatasetAccelerationCronExpression] = useState('')
  const [savingDatasetAccelerationPolicy, setSavingDatasetAccelerationPolicy] = useState(false)
  const [refreshingDatasetAcceleration, setRefreshingDatasetAcceleration] = useState(false)
  const [runningDatasetAccelerationScheduler, setRunningDatasetAccelerationScheduler] = useState(false)
  const [datasetAccelerationSchedulerResult, setDatasetAccelerationSchedulerResult] =
    useState<BiDatasetAccelerationSchedulerResult | null>(null)
  const [datasourceHealth, setDatasourceHealth] = useState<BiDatasourceHealth[]>([])
  const [datasourceHealthHistory, setDatasourceHealthHistory] = useState<BiDatasourceHealthSnapshot[]>([])
  const [datasourceHealthSlo, setDatasourceHealthSlo] = useState<BiDatasourceHealthSloSummary | null>(null)
  const [datasourceConnectors, setDatasourceConnectors] = useState<BiDatasourceConnectorCapability[]>([])
  const [datasourceOnboarding, setDatasourceOnboarding] = useState<BiDatasourceOnboardingView[]>([])
  const [datasourceConnectionTestResult, setDatasourceConnectionTestResult] = useState<BiDatasourceConnectionTestResult | null>(null)
  const [datasourceSchemaPreview, setDatasourceSchemaPreview] = useState<BiDatasourceSchemaPreview | null>(null)
  const [datasourceApiPreview, setDatasourceApiPreview] = useState<BiDatasourceApiPreview | null>(null)
  const [datasourceApiPreviewVariableDrafts, setDatasourceApiPreviewVariableDrafts] = useState<ApiPreviewVariableDraft[]>([
    { name: '', value: '' },
  ])
  const [datasourceApiPreviewLimit, setDatasourceApiPreviewLimit] = useState('50')
  const [datasourceSchemaSnapshot, setDatasourceSchemaSnapshot] = useState<BiDatasourceSchemaSnapshotView | null>(null)
  const [datasourceSchemaSnapshots, setDatasourceSchemaSnapshots] = useState<BiDatasourceSchemaSnapshotView[]>([])
  const [testingDatasourceId, setTestingDatasourceId] = useState<number | null>(null)
  const [previewingDatasourceId, setPreviewingDatasourceId] = useState<number | null>(null)
  const [previewingApiDatasourceId, setPreviewingApiDatasourceId] = useState<number | null>(null)
  const [syncingDatasourceId, setSyncingDatasourceId] = useState<number | null>(null)
  const [rotatingDatasourceId, setRotatingDatasourceId] = useState<number | null>(null)
  const [credentialRotationDatasourceId, setCredentialRotationDatasourceId] = useState<number | null>(null)
  const [credentialRotationPassword, setCredentialRotationPassword] = useState('')
  const [datasourceModelingTableNames, setDatasourceModelingTableNames] = useState<string[]>([])
  const [datasourceModelingBaseTableName, setDatasourceModelingBaseTableName] = useState('')
  const [datasourceModelingJoinDrafts, setDatasourceModelingJoinDrafts] = useState<DatasourceModelingJoinDraft[]>([])
  const [datasourceModelingGraphNodes, setDatasourceModelingGraphNodes] = useState<DatasourceModelingGraphNode[]>([])
  const [selectedDatasourceModelingGraphJoinIndex, setSelectedDatasourceModelingGraphJoinIndex] = useState(0)
  const datasourceModelingGraphDragRef = useRef<DatasourceModelingGraphDragState | null>(null)
  const [sqlDatasetKey, setSqlDatasetKey] = useState('campaign_sql')
  const [sqlDatasetName, setSqlDatasetName] = useState('Campaign SQL')
  const [sqlDatasetTemplate, setSqlDatasetTemplate] = useState('')
  const [sqlDatasetTenantColumn, setSqlDatasetTenantColumn] = useState('tenant_id')
  const [sqlDatasetParameters, setSqlDatasetParameters] = useState<BiSqlDatasetParameterDraftLike[]>([])
  const [sqlDatasetFields, setSqlDatasetFields] = useState<BiSqlDatasetFieldDraftLike[]>(() => defaultSqlDatasetFields())
  const [sqlDatasetMetrics, setSqlDatasetMetrics] = useState<SqlDatasetMetricDraft[]>(() => defaultSqlDatasetMetrics())
  const [savingSqlDatasetDraft, setSavingSqlDatasetDraft] = useState(false)
  const [previewingSqlDataset, setPreviewingSqlDataset] = useState(false)
  const [sqlDatasetPreview, setSqlDatasetPreview] = useState<BiSqlDatasetPreviewResult | null>(null)
  const [sqlDatasetPreviewError, setSqlDatasetPreviewError] = useState('')
  const [datasourceDraft, setDatasourceDraft] = useState<BiDatasourceOnboardingDraftInputLike>(() => defaultDatasourceDraft())
  const [datasourceUploadFile, setDatasourceUploadFile] = useState<File | null>(null)
  const [datasourceWizardStep, setDatasourceWizardStep] = useState(0)
  const [editingDatasourceId, setEditingDatasourceId] = useState<number | null>(null)
  const [savingDatasourceOnboarding, setSavingDatasourceOnboarding] = useState(false)
  const [creatingDatasourceDatasetTable, setCreatingDatasourceDatasetTable] = useState<string | null>(null)
  const [creatingDatasourceMultiTableDataset, setCreatingDatasourceMultiTableDataset] = useState(false)
  const [resourcePermissions, setResourcePermissions] = useState<BiResourcePermissionView[]>([])
  const [rowPermissions, setRowPermissions] = useState<BiRowPermissionView[]>([])
  const [columnPermissions, setColumnPermissions] = useState<BiColumnPermissionView[]>([])
  const [permissionAudit, setPermissionAudit] = useState<BiPermissionAuditEntry[]>([])
  const [permissionRequests, setPermissionRequests] = useState<BiPermissionRequestView[]>([])
  const [resourceLocations, setResourceLocations] = useState<BiResourceLocationView[]>([])
  const [resourceOwnerships, setResourceOwnerships] = useState<BiResourceOwnershipView[]>([])
  const [resourceFavorites, setResourceFavorites] = useState<BiResourceFavoriteView[]>([])
  const [resourceComments, setResourceComments] = useState<BiResourceCommentView[]>([])
  const [resourceLock, setResourceLock] = useState<BiResourceLockView | null>(null)
  const [publishApprovals, setPublishApprovals] = useState<BiPublishApprovalView[]>([])
  const [loadingPermissions, setLoadingPermissions] = useState(false)
  const [loadingResourceLocations, setLoadingResourceLocations] = useState(false)
  const [loadingResourceOwnerships, setLoadingResourceOwnerships] = useState(false)
  const [loadingResourceFavorites, setLoadingResourceFavorites] = useState(false)
  const [loadingResourceComments, setLoadingResourceComments] = useState(false)
  const [loadingResourceLock, setLoadingResourceLock] = useState(false)
  const [loadingPublishApprovals, setLoadingPublishApprovals] = useState(false)
  const [savingPermission, setSavingPermission] = useState<string | null>(null)
  const [deletingPermission, setDeletingPermission] = useState<string | null>(null)
  const [permissionResourceTarget, setPermissionResourceTarget] = useState('DATASET')
  const [resourcePermissionSubjectType, setResourcePermissionSubjectType] = useState('ROLE')
  const [resourcePermissionSubjectId, setResourcePermissionSubjectId] = useState('OPERATOR')
  const [resourcePermissionAction, setResourcePermissionAction] = useState('USE')
  const [resourcePermissionEffect, setResourcePermissionEffect] = useState('ALLOW')
  const [rowPermissionRuleKey, setRowPermissionRuleKey] = useState('operator-canvas-scope')
  const [rowPermissionSubjectType, setRowPermissionSubjectType] = useState('ROLE')
  const [rowPermissionSubjectId, setRowPermissionSubjectId] = useState('OPERATOR')
  const [rowPermissionFilterJson, setRowPermissionFilterJson] = useState('{"canvas_id":[12,13]}')
  const [rowPermissionEnabled, setRowPermissionEnabled] = useState(true)
  const [columnPermissionFieldKey, setColumnPermissionFieldKey] = useState('canvas_name')
  const [columnPermissionSubjectType, setColumnPermissionSubjectType] = useState('ROLE')
  const [columnPermissionSubjectId, setColumnPermissionSubjectId] = useState('OPERATOR')
  const [columnPermissionPolicy, setColumnPermissionPolicy] = useState('MASK')
  const [columnPermissionMaskJson, setColumnPermissionMaskJson] = useState('{"strategy":"FIXED","replacement":"MASKED"}')
  const [columnPermissionEnabled, setColumnPermissionEnabled] = useState(true)
  const [permissionRequestAction, setPermissionRequestAction] = useState('USE')
  const [permissionRequestReason, setPermissionRequestReason] = useState('')
  const [permissionReviewComment, setPermissionReviewComment] = useState('')
  const [reviewingPermissionRequest, setReviewingPermissionRequest] = useState<string | null>(null)
  const [moveResourceTarget, setMoveResourceTarget] = useState('DASHBOARD')
  const [moveFolderKey, setMoveFolderKey] = useState('marketing')
  const [movingResource, setMovingResource] = useState<string | null>(null)
  const [transferOwnerUser, setTransferOwnerUser] = useState('owner@example.com')
  const [transferringResource, setTransferringResource] = useState<string | null>(null)
  const [batchDatasetResourceKeys, setBatchDatasetResourceKeys] = useState('')
  const [batchDatasetFolderKey, setBatchDatasetFolderKey] = useState('')
  const [batchDatasetOwnerUser, setBatchDatasetOwnerUser] = useState('owner@example.com')
  const [batchDatasetResourceAction, setBatchDatasetResourceAction] = useState<'move' | 'transfer' | null>(null)
  const [favoritingResource, setFavoritingResource] = useState<string | null>(null)
  const [resourceCommentText, setResourceCommentText] = useState('')
  const [savingResourceComment, setSavingResourceComment] = useState(false)
  const [deletingResourceComment, setDeletingResourceComment] = useState<number | null>(null)
  const [resourceLockToken] = useState(() => `bi-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`)
  const [savingResourceLock, setSavingResourceLock] = useState<string | null>(null)
  const [publishApprovalReason, setPublishApprovalReason] = useState('')
  const [publishReviewComment, setPublishReviewComment] = useState('')
  const [savingPublishApproval, setSavingPublishApproval] = useState<string | null>(null)
  const [exportJobs, setExportJobs] = useState<BiExportJobView[]>([])
  const [loadingExports, setLoadingExports] = useState(false)
  const [previewingSelfService, setPreviewingSelfService] = useState(false)
  const [creatingExport, setCreatingExport] = useState(false)
  const [reviewingExport, setReviewingExport] = useState<string | null>(null)
  const [exportReviewComment, setExportReviewComment] = useState('')
  const [cancellingExportId, setCancellingExportId] = useState<number | null>(null)
  const [cleaningExports, setCleaningExports] = useState(false)
  const [retryingExports, setRetryingExports] = useState(false)
  const [selfServicePreview, setSelfServicePreview] = useState<BiQueryResult | null>(null)
  const [exportCleanupResult, setExportCleanupResult] = useState<BiExportCleanupResult | null>(null)
  const [exportRetryResult, setExportRetryResult] = useState<BiExportRetryResult | null>(null)
  const [exportDetailOpen, setExportDetailOpen] = useState(false)
  const [exportDetail, setExportDetail] = useState<BiExportJobDetailView | null>(null)
  const [loadingExportDetail, setLoadingExportDetail] = useState(false)
  const [selfServiceExtraction, setSelfServiceExtraction] = useState<SelfServiceExtractionState>({
    dimensions: [],
    metrics: [],
  })
  const [subscriptions, setSubscriptions] = useState<BiSubscriptionView[]>([])
  const [alertRules, setAlertRules] = useState<BiAlertRuleView[]>([])
  const [deliveryLogs, setDeliveryLogs] = useState<BiDeliveryLogView[]>([])
  const [deliveryAudit, setDeliveryAudit] = useState<BiDeliveryAuditSummary | null>(null)
  const [deliveryAttachments, setDeliveryAttachments] = useState<BiDeliveryAttachmentView[]>([])
  const [schedulerResult, setSchedulerResult] = useState<BiDeliverySchedulerResult | null>(null)
  const [retryResult, setRetryResult] = useState<BiDeliveryRetryResult | null>(null)
  const [cleanupResult, setCleanupResult] = useState<BiDeliveryAttachmentCleanupResult | null>(null)
  const [loadingSubscriptions, setLoadingSubscriptions] = useState(false)
  const [savingSubscription, setSavingSubscription] = useState<string | null>(null)
  const [runningDelivery, setRunningDelivery] = useState<string | null>(null)
  const [creatingEmbedTicket, setCreatingEmbedTicket] = useState(false)
  const [embedTicket, setEmbedTicket] = useState<BiEmbedTicket | null>(null)
  const selected = useMemo(() => getBiSection(sectionKey), [sectionKey])
  const selectedWidget = useMemo(
    () => getDashboardWidget(dashboardPreset, selectedWidgetKey),
    [dashboardPreset, selectedWidgetKey],
  )
  const selectedWidgetKeySet = useMemo(() => new Set(selectedWidgetKeys), [selectedWidgetKeys])
  const selectedLayoutWidgetKeys = useMemo(
    () => selectedWidgetKeys.filter(widgetKey =>
      dashboardPreset.widgets.some(widget => widget.widgetKey === widgetKey)),
    [dashboardPreset.widgets, selectedWidgetKeys],
  )
  const dashboardDisplayWidgets = useMemo(
    () => dashboardResponsiveWidgets(dashboardPreset, dashboardLayoutMode),
    [dashboardPreset, dashboardLayoutMode],
  )
  const explicitDashboardRuntimeParameters = useMemo(
    () => {
      // 用户主动清除的参数需要从显式 URL 参数中剔除，避免再次参与优先级解析。
      let effectiveSearchParams = new URLSearchParams(searchParams)
      for (const key of clearedDashboardRuntimeParameterKeys) {
        effectiveSearchParams = stripDashboardRuntimeSearchParam(dashboardPreset, effectiveSearchParams, key)
      }
      return dashboardRuntimeParametersFromSearchParams(dashboardPreset, effectiveSearchParams)
    },
    [dashboardPreset, runtimeParameterKey, clearedDashboardRuntimeParameterKeys],
  )
  const explicitDashboardRuntimeParameterKey = useMemo(
    () => JSON.stringify(explicitDashboardRuntimeParameters),
    [explicitDashboardRuntimeParameters],
  )
  const dashboardRuntimeParameters = useMemo(
    () => {
      // UI 最终使用的参数由 URL、已记住状态和默认值共同解析得出。
      let effectiveSearchParams = new URLSearchParams(searchParams)
      for (const key of clearedDashboardRuntimeParameterKeys) {
        effectiveSearchParams = stripDashboardRuntimeSearchParam(dashboardPreset, effectiveSearchParams, key)
      }
      return resolveDashboardRuntimeParameters(
        dashboardPreset,
        effectiveSearchParams,
        dashboardRuntimeState?.parameters as BiDashboardRuntimeParameters | null,
      )
    },
    [dashboardPreset, runtimeParameterKey, dashboardRuntimeState, clearedDashboardRuntimeParameterKeys],
  )
  const dashboardRuntimeParametersRef = useRef(dashboardRuntimeParameters)
  useEffect(() => {
    dashboardRuntimeParametersRef.current = dashboardRuntimeParameters
  }, [dashboardRuntimeParameters])
  const dashboardRuntimeRows = useMemo(
    () => dashboardRuntimeStateRows(
      dashboardPreset,
      searchParams,
      dashboardRuntimeState?.parameters as BiDashboardRuntimeParameters | null,
      clearedDashboardRuntimeParameterKeys,
    ),
    [dashboardPreset, runtimeParameterKey, dashboardRuntimeState, clearedDashboardRuntimeParameterKeys],
  )
  const dashboardDisplayColumns = dashboardLayoutColumns(dashboardLayoutMode)
  const canUndoDashboardEdit = dashboardHistory.past.length > 0
  const canRedoDashboardEdit = dashboardHistory.future.length > 0

  useEffect(() => {
    // 自助分析默认跟随当前组件字段，用户可在此基础上增删维度和指标。
    setSelfServiceExtraction({
      dimensions: selectedWidget.dimensions,
      metrics: selectedWidget.metrics,
    })
  }, [selectedWidget.widgetKey])

  const resetDashboardPreset = (preset: BiDashboardPresetLike) => {
    // 切换仪表盘时重置历史栈，避免撤销跨资源污染。
    setDashboardPreset(preset)
    setDashboardHistory(createDashboardPresetHistory(preset))
    const nextSelectedKey = preset.widgets.some(widget => widget.widgetKey === selectedWidgetKey)
      ? selectedWidgetKey
      : preset.widgets[0]?.widgetKey ?? selectedWidgetKey
    setSelectedWidgetKey(nextSelectedKey)
    setSelectedWidgetKeys(nextSelectedKey ? [nextSelectedKey] : [])
  }

  const applyDashboardEdit = (updater: (preset: BiDashboardPresetLike) => BiDashboardPresetLike) => {
    setDashboardPreset(current => {
      const next = updater(current)
      if (next === current) return current
      // 每次设计器编辑都推入历史栈，支撑撤销/重做。
      setDashboardHistory(history => pushDashboardPresetHistory({ ...history, present: current }, next))
      return next
    })
  }

  const undoDashboardEdit = () => {
    setDashboardHistory(history => {
      const next = undoDashboardPresetHistory(history)
      if (next === history) return history
      setDashboardPreset(next.present)
      setSelectedWidgetKey(current =>
        next.present.widgets.some(widget => widget.widgetKey === current)
          ? current
          : next.present.widgets[0]?.widgetKey ?? current)
      setSelectedWidgetKeys(current => {
        const kept = current.filter(widgetKey => next.present.widgets.some(widget => widget.widgetKey === widgetKey))
        return kept.length > 0 ? kept : [next.present.widgets[0]?.widgetKey ?? selectedWidgetKey]
      })
      setCompiledWidgetQuery(null)
      return next
    })
  }

  const redoDashboardEdit = () => {
    setDashboardHistory(history => {
      const next = redoDashboardPresetHistory(history)
      if (next === history) return history
      setDashboardPreset(next.present)
      setSelectedWidgetKey(current =>
        next.present.widgets.some(widget => widget.widgetKey === current)
          ? current
          : next.present.widgets[0]?.widgetKey ?? current)
      setSelectedWidgetKeys(current => {
        const kept = current.filter(widgetKey => next.present.widgets.some(widget => widget.widgetKey === widgetKey))
        return kept.length > 0 ? kept : [next.present.widgets[0]?.widgetKey ?? selectedWidgetKey]
      })
      setCompiledWidgetQuery(null)
      return next
    })
  }

  const selectedDatasetResource = useMemo(
    () => datasetResources.find(dataset => dataset.datasetKey === selectedDatasetKey) ?? datasetResources[0] ?? null,
    [datasetResources, selectedDatasetKey],
  )
  const selectedChartResource = useMemo(
    () => chartResources.find(chart => chart.chartKey === selectedChartKey) ?? chartResources[0] ?? null,
    [chartResources, selectedChartKey],
  )
  const selectedChartServerImpact = selectedChartResource ? chartReferenceImpacts[selectedChartResource.chartKey] : null
  const selectedChartReferenceImpact = useMemo(
    // 服务端影响分析优先；失败时使用当前页面内存资源做本地兜底摘要。
    () => selectedChartServerImpact
      ? chartReferenceImpactSummaryFromImpact(selectedChartResource, selectedChartServerImpact)
      : chartReferenceImpactSummary(selectedChartResource, {
        dashboards: [dashboardPreset],
        portals: portalResources,
        subscriptions,
      }),
    [dashboardPreset, portalResources, selectedChartResource, selectedChartServerImpact, subscriptions],
  )
  const selectedPortalResource = useMemo(
    () => portalResources.find(portal => portal.portalKey === selectedPortalKey) ?? portalResources[0] ?? null,
    [portalResources, selectedPortalKey],
  )
  const runtimeBigScreenResource = useMemo(
    () => selectBigScreenRuntimeResource(bigScreenResources, runtimeRoute),
    [bigScreenResources, runtimeRoute],
  )
  const runtimeSpreadsheetResource = useMemo(
    () => selectSpreadsheetRuntimeResource(spreadsheetResources, runtimeRoute),
    [spreadsheetResources, runtimeRoute],
  )
  const selectedBigScreenResource = useMemo(
    () => runtimeRoute.mode === 'big-screen'
      ? runtimeBigScreenResource
      : bigScreenResources.find(screen => screen.screenKey === selectedBigScreenKey) ?? bigScreenResources[0] ?? null,
    [bigScreenResources, runtimeBigScreenResource, runtimeRoute.mode, selectedBigScreenKey],
  )
  const selectedSpreadsheetResource = useMemo(
    () => runtimeRoute.mode === 'spreadsheet'
      ? runtimeSpreadsheetResource
      : spreadsheetResources.find(spreadsheet => spreadsheet.spreadsheetKey === selectedSpreadsheetKey) ?? spreadsheetResources[0] ?? null,
    [runtimeRoute.mode, runtimeSpreadsheetResource, selectedSpreadsheetKey, spreadsheetResources],
  )
  const bigScreenResourceOptions = useMemo(
    () => buildBigScreenResourceOptions(bigScreenResources),
    [bigScreenResources],
  )
  const spreadsheetResourceOptions = useMemo(
    () => buildSpreadsheetResourceOptions(spreadsheetResources),
    [spreadsheetResources],
  )
  const portalMenuOptions = useMemo(
    () => (selectedPortalResource?.menus ?? [])
      .slice()
      .sort((left, right) => Number(left.sortOrder ?? 0) - Number(right.sortOrder ?? 0))
      .map((menu, index) => {
        const menuKey = String(menu.menuKey ?? `menu-${index + 1}`)
        return {
          label: `${String(menu.title ?? menuKey)} · ${menuKey}`,
          value: menuKey,
        }
      }),
    [selectedPortalResource],
  )
  const selectedPortalMenu = useMemo(
    () => (selectedPortalResource?.menus ?? [])
      .find(menu => String(menu.menuKey ?? '') === selectedPortalMenuKey) ?? null,
    [selectedPortalResource?.menus, selectedPortalMenuKey],
  )
  const portalParentMenuOptions = useMemo(
    () => portalMenuOptions.filter(option => option.value !== selectedPortalMenuKey),
    [portalMenuOptions, selectedPortalMenuKey],
  )
  const selectedDatasetFolderSummary = useMemo(() => {
    const counts = new Map<string, number>()
    for (const field of selectedDatasetResource?.fields ?? []) {
      const folderKey = field.folderKey?.trim()
      if (!folderKey) continue
      counts.set(folderKey, (counts.get(folderKey) ?? 0) + 1)
    }
    return Array.from(counts.entries())
      .map(([folderKey, count]) => `字段文件夹：${folderKey} · ${count} 字段`)
      .join('；')
  }, [selectedDatasetResource])
  const selectedDatasetDetailField = useMemo(
    () => (selectedDatasetResource?.fields ?? [])
      .find(field => field.fieldKey === selectedDatasetDetailFieldKey)
      ?? selectedDatasetResource?.fields?.[0]
      ?? null,
    [selectedDatasetResource, selectedDatasetDetailFieldKey],
  )
  const datasetFieldTreeGroups = useMemo(() => {
    const groups = new Map<string, BiDatasetFieldResource[]>()
    for (const field of [...(selectedDatasetResource?.fields ?? [])].sort((left, right) => Number(left.sortOrder ?? 0) - Number(right.sortOrder ?? 0))) {
      const folderKey = field.folderKey?.trim() || '未分组'
      groups.set(folderKey, [...(groups.get(folderKey) ?? []), field])
    }
    return Array.from(groups.entries())
  }, [selectedDatasetResource])
  const bigScreenSummaryRows = useMemo(
    () => bigScreenResourceSummaryRows(selectedBigScreenResource),
    [selectedBigScreenResource],
  )
  const spreadsheetSummaryRows = useMemo(
    () => spreadsheetResourceSummaryRows(selectedSpreadsheetResource),
    [selectedSpreadsheetResource],
  )
  const visualEditorDiagnosticRows = useMemo(
    () => buildVisualEditorDiagnosticRows({
      bigScreen: selectedBigScreenResource,
      spreadsheet: selectedSpreadsheetResource,
    }),
    [selectedBigScreenResource, selectedSpreadsheetResource],
  )
  const selectedBigScreenLayoutItem = useMemo(() => {
    const layout = selectedBigScreenResource?.layout ?? []
    return layout.find(item => String(item.widgetKey ?? '') === selectedBigScreenWidgetKey) ?? layout[0] ?? null
  }, [selectedBigScreenResource, selectedBigScreenWidgetKey])
  const bigScreenLayoutOptions = useMemo(
    () => (selectedBigScreenResource?.layout ?? []).map((item, index) => {
      const widgetKey = String(item.widgetKey ?? `widget-${index + 1}`)
      return {
        label: `${String(item.title ?? widgetKey)} · ${widgetKey}`,
        value: widgetKey,
      }
    }),
    [selectedBigScreenResource],
  )
  const selectedSpreadsheetSheet = useMemo(() => {
    const sheets = selectedSpreadsheetResource?.sheets ?? []
    return sheets.find(sheet => String(sheet.sheetKey ?? '') === selectedSpreadsheetSheetKey) ?? sheets[0] ?? null
  }, [selectedSpreadsheetResource, selectedSpreadsheetSheetKey])
  const spreadsheetSheetOptions = useMemo(
    () => (selectedSpreadsheetResource?.sheets ?? []).map((sheet, index) => {
      const sheetKey = String(sheet.sheetKey ?? `sheet-${index + 1}`)
      return {
        label: `${String(sheet.name ?? sheetKey)} · ${sheetKey}`,
        value: sheetKey,
      }
    }),
    [selectedSpreadsheetResource],
  )
  const selectedSpreadsheetCellValue = useMemo(() => {
    const cells = selectedSpreadsheetSheet?.cells
    if (!cells || typeof cells !== 'object') return ''
    const value = (cells as Record<string, unknown>)[selectedSpreadsheetCellKey.toUpperCase()]
    return value == null ? '' : String(value)
  }, [selectedSpreadsheetCellKey, selectedSpreadsheetSheet])
  const selectedSpreadsheetCellStyle = useMemo(() => {
    const cellStyles = selectedSpreadsheetSheet?.cellStyles
    if (!cellStyles || typeof cellStyles !== 'object') return {}
    const style = (cellStyles as Record<string, unknown>)[selectedSpreadsheetCellKey.toUpperCase()]
    return style && typeof style === 'object' ? style as Record<string, unknown> : {}
  }, [selectedSpreadsheetCellKey, selectedSpreadsheetSheet])
  const spreadsheetPivotFields = useMemo(
    () => spreadsheetPivotFieldsFromSheet(selectedSpreadsheetSheet, spreadsheetPivotSourceRange),
    [selectedSpreadsheetSheet, spreadsheetPivotSourceRange],
  )
  const spreadsheetPivotPreviewColumnLabels = useMemo(
    () => spreadsheetPivotPreviewColumns(
      selectedSpreadsheetSheet,
      spreadsheetPivotSourceRange,
      spreadsheetPivotColumnField,
      spreadsheetPivotMetrics,
    ),
    [selectedSpreadsheetSheet, spreadsheetPivotColumnField, spreadsheetPivotMetrics, spreadsheetPivotSourceRange],
  )
  const spreadsheetPivotPreviewGridRows = useMemo(
    () => spreadsheetPivotPreviewRows(
      selectedSpreadsheetSheet,
      spreadsheetPivotSourceRange,
      spreadsheetPivotTargetCell,
      spreadsheetPivotRowField,
      spreadsheetPivotColumnField,
      spreadsheetPivotMetrics,
    ),
    [
      selectedSpreadsheetSheet,
      spreadsheetPivotColumnField,
      spreadsheetPivotMetrics,
      spreadsheetPivotRowField,
      spreadsheetPivotSourceRange,
      spreadsheetPivotTargetCell,
    ],
  )
  const resourceLocationIndex = useMemo(
    () => toResourceLocationIndex(resourceLocations),
    [resourceLocations],
  )
  const resourceLocationTableRows = useMemo(
    () => resourceLocations.map((row, index) => ({
      ...row,
      __tableRowKey: resourceLocationTableRowKey(row, index),
    })),
    [resourceLocations],
  )
  const resourceOwnershipIndex = useMemo(
    () => toResourceOwnershipIndex(resourceOwnerships),
    [resourceOwnerships],
  )
  const resourceOwnershipTableRows = useMemo(
    () => resourceOwnerships.map((row, index) => ({
      ...row,
      __tableRowKey: resourceOwnershipTableRowKey(row, index),
    })),
    [resourceOwnerships],
  )
  const resourceFavoriteIndex = useMemo(
    () => toResourceFavoriteIndex(resourceFavorites),
    [resourceFavorites],
  )
  const resourceFavoriteTableRows = useMemo(
    () => resourceFavorites.map((row, index) => ({
      ...row,
      __tableRowKey: resourceFavoriteTableRowKey(row, index),
    })),
    [resourceFavorites],
  )
  const resourceMoveTargets = useMemo(() => buildBiResourceTargets({
    dashboardKey: dashboardPreset.dashboardKey,
    chartKey: selectedChartResource?.chartKey,
    datasetKey: selectedDatasetResource?.datasetKey,
    dataSourceKey: datasourceOnboarding[0]?.sourceKey,
    portalKey: selectedPortalResource?.portalKey,
    bigScreenKey: selectedBigScreenResource?.screenKey,
    spreadsheetKey: selectedSpreadsheetResource?.spreadsheetKey,
  }), [
    dashboardPreset.dashboardKey,
    selectedChartResource,
    selectedDatasetResource,
    datasourceOnboarding,
    selectedPortalResource,
    selectedBigScreenResource,
    selectedSpreadsheetResource,
  ])
  const permissionResourceTargets = useMemo(() => buildBiPermissionResourceTargets({
    dashboardKey: dashboardPreset.dashboardKey,
    chartKey: selectedChartResource?.chartKey,
    datasetKey: selectedDatasetResource?.datasetKey,
    dataSourceKey: datasourceOnboarding[0]?.sourceKey,
    portalKey: selectedPortalResource?.portalKey,
    bigScreenKey: selectedBigScreenResource?.screenKey,
    spreadsheetKey: selectedSpreadsheetResource?.spreadsheetKey,
  }), [
    dashboardPreset.dashboardKey,
    selectedChartResource,
    selectedDatasetResource,
    datasourceOnboarding,
    selectedPortalResource,
    selectedBigScreenResource,
    selectedSpreadsheetResource,
  ])
  const selectedMoveTarget = useMemo(
    () => resourceMoveTargets.find(target => target.value === moveResourceTarget && !target.disabled) ?? resourceMoveTargets[0],
    [resourceMoveTargets, moveResourceTarget],
  )
  const selectedPermissionTarget = useMemo(
    () => permissionResourceTargets.find(target => target.value === permissionResourceTarget && !target.disabled)
      ?? permissionResourceTargets.find(target => target.value === 'DATASET' && !target.disabled)
      ?? permissionResourceTargets[0],
    [permissionResourceTargets, permissionResourceTarget],
  )
  const isDatasetPermissionTarget = selectedPermissionTarget.resourceType === 'DATASET'
  const selectedPermissionDatasetResource = useMemo(
    () => datasetResources.find(dataset => dataset.datasetKey === selectedPermissionTarget.resourceKey)
      ?? (isDatasetPermissionTarget ? selectedDatasetResource : null),
    [datasetResources, selectedPermissionTarget.resourceKey, isDatasetPermissionTarget, selectedDatasetResource],
  )
  const selectedPermissionSensitiveFields = useMemo(
    () => (selectedPermissionDatasetResource?.fields ?? []).filter(field =>
      String(field.sensitiveLevel ?? 'PUBLIC').toUpperCase() !== 'PUBLIC' || field.visible === false),
    [selectedPermissionDatasetResource],
  )
  const columnPermissionShortcutFieldKeys = useMemo(() => {
    const sensitiveFieldKeys = selectedPermissionSensitiveFields
      .map(field => field.fieldKey)
      .filter(fieldKey => fieldKey.trim())
    if (sensitiveFieldKeys.length > 0) {
      return sensitiveFieldKeys
    }
    const fallbackFieldKey = columnPermissionFieldKey.trim()
    return fallbackFieldKey ? [fallbackFieldKey] : []
  }, [selectedPermissionSensitiveFields, columnPermissionFieldKey])
  const selectedResourceIndexKey = selectedMoveTarget.resourceKey
    ? resourceLocationIndexKey(selectedMoveTarget.resourceType, selectedMoveTarget.resourceKey)
    : ''
  const selectedMoveLocation = resourceLocationIndex[
    selectedResourceIndexKey
  ]
  const selectedOwnership = resourceOwnershipIndex[
    selectedResourceIndexKey
  ]
  const selectedFavorite = resourceFavoriteIndex[
    selectedResourceIndexKey
  ]
  const selectedPublishApprovals = useMemo(
    () => publishApprovals.filter(approval =>
      resourceLocationIndexKey(approval.resourceType, approval.resourceKey) === selectedResourceIndexKey),
    [publishApprovals, selectedResourceIndexKey],
  )
  const latestPublishApproval = selectedPublishApprovals[0]
  const pendingPublishApproval = selectedPublishApprovals.find(approval =>
    approval.status?.toUpperCase() === 'PENDING')
  const loadingSharedResourceTargets = loadingResourceLocations || loadingBigScreens || loadingSpreadsheets
  const expiredAttachmentCount = useMemo(
    () => deliveryAttachments.filter(attachment => isExpiredAt(attachment.expiresAt) || attachment.status === 'EXPIRED').length,
    [deliveryAttachments],
  )
  const attachmentDownloadCount = useMemo(
    () => deliveryAttachments.reduce((total, attachment) => total + (attachment.downloadCount ?? 0), 0),
    [deliveryAttachments],
  )
  const expiredExportCount = useMemo(
    () => exportJobs.filter(job => isExpiredAt(job.expiresAt) || job.status === 'EXPIRED').length,
    [exportJobs],
  )
  const exportDownloadCount = useMemo(
    () => exportJobs.reduce((total, job) => total + (job.downloadCount ?? 0), 0),
    [exportJobs],
  )
  const retryableExportCount = useMemo(
    () => exportJobs.filter(isRetryableExportJob).length,
    [exportJobs],
  )
  const exportHardeningRows = useMemo(
    () => exportHardeningDiagnosticRows(exportJobs),
    [exportJobs],
  )
  const alertAnomalyDiagnosticRowsValue = useMemo(
    () => alertAnomalyDiagnosticRows(alertRules),
    [alertRules],
  )

  useEffect(() => {
    let cancelled = false
    setLoadingDatasets(true)
    biApi.listDatasets()
      .then(response => {
        if (!cancelled && response.data?.length) {
          setDatasets(response.data.map(toMarketingDatasetPreset))
          setDatasetMetadata(response.data.find(dataset => dataset.datasetKey === dashboardPreset.datasetKey) ?? response.data[0])
        }
      })
      .catch(() => {
        if (!cancelled) setDatasets(DEFAULT_MARKETING_DATASETS)
      })
      .finally(() => {
        if (!cancelled) setLoadingDatasets(false)
      })
    return () => {
      cancelled = true
    }
  }, [dashboardPreset.datasetKey])

  useEffect(() => {
    let cancelled = false
    setLoadingDatasetResources(true)
    biApi.listDatasetResources()
      .then(response => {
        if (!cancelled) {
          const resources = response.data ?? []
          setDatasetResources(resources)
          setSelectedDatasetKey(current => current ?? resources[0]?.datasetKey ?? null)
        }
      })
      .catch(() => {
        if (!cancelled) setDatasetResources([])
      })
      .finally(() => {
        if (!cancelled) setLoadingDatasetResources(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    biApi.getDashboardRuntimeState(dashboardPreset.dashboardKey)
      .then(response => {
        if (!cancelled) setDashboardRuntimeState(response.data ?? null)
      })
      .catch(() => {
        if (!cancelled) setDashboardRuntimeState(null)
      })
    return () => {
      cancelled = true
    }
  }, [dashboardPreset.dashboardKey])

  useEffect(() => {
    if (Object.keys(explicitDashboardRuntimeParameters).length === 0) return
    let cancelled = false
    biApi.saveDashboardRuntimeState(dashboardPreset.dashboardKey, buildDashboardRuntimeStateCommand(dashboardPreset, dashboardRuntimeParameters))
      .then(response => {
        if (!cancelled) setDashboardRuntimeState(response.data ?? null)
      })
      .catch(() => undefined)
    return () => {
      cancelled = true
    }
  }, [dashboardPreset.dashboardKey, explicitDashboardRuntimeParameterKey])

  useEffect(() => {
    let cancelled = false
    setLoadingPortals(true)
    biApi.listPortalResources()
      .then(response => {
        if (!cancelled) {
          const resources = response.data ?? []
          setPortalResources(resources)
          setSelectedPortalKey(current => current ?? resources[0]?.portalKey ?? null)
        }
      })
      .catch(() => {
        if (!cancelled) setPortalResources([])
      })
      .finally(() => {
        if (!cancelled) setLoadingPortals(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoadingPortalRuntime(true)
    biApi.listPortalRuntime()
      .then(response => {
        if (!cancelled) setPortalRuntimeResources(response.data ?? [])
      })
      .catch(() => {
        if (!cancelled) setPortalRuntimeResources([])
      })
      .finally(() => {
        if (!cancelled) setLoadingPortalRuntime(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoadingBigScreens(true)
    biApi.listBigScreenResources()
      .then(response => {
        if (!cancelled) {
          const screens = response.data ?? []
          setBigScreenResources(screens)
          setSelectedBigScreenKey(current => current ?? screens[0]?.screenKey ?? null)
        }
      })
      .catch(() => {
        if (!cancelled) setBigScreenResources([])
      })
      .finally(() => {
        if (!cancelled) setLoadingBigScreens(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoadingSpreadsheets(true)
    biApi.listSpreadsheetResources()
      .then(response => {
        if (!cancelled) {
          const spreadsheets = response.data ?? []
          setSpreadsheetResources(spreadsheets)
          setSelectedSpreadsheetKey(current => current ?? spreadsheets[0]?.spreadsheetKey ?? null)
        }
      })
      .catch(() => {
        if (!cancelled) setSpreadsheetResources([])
      })
      .finally(() => {
        if (!cancelled) setLoadingSpreadsheets(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (runtimeRoute.mode !== 'big-screen') return
    setSelectedBigScreenKey(runtimeBigScreenResource?.screenKey ?? null)
  }, [runtimeBigScreenResource?.screenKey, runtimeRoute.mode])

  useEffect(() => {
    if (runtimeRoute.mode !== 'spreadsheet') return
    setSelectedSpreadsheetKey(runtimeSpreadsheetResource?.spreadsheetKey ?? null)
  }, [runtimeRoute.mode, runtimeSpreadsheetResource?.spreadsheetKey])

  useEffect(() => {
    const layout = selectedBigScreenResource?.layout ?? []
    setSelectedBigScreenWidgetKey(current =>
      current && layout.some(item => String(item.widgetKey ?? '') === current)
        ? current
        : String(layout[0]?.widgetKey ?? '') || null)
  }, [selectedBigScreenResource?.screenKey, selectedBigScreenResource?.layout])

  useEffect(() => {
    const sheets = selectedSpreadsheetResource?.sheets ?? []
    setSelectedSpreadsheetSheetKey(current =>
      current && sheets.some(sheet => String(sheet.sheetKey ?? '') === current)
        ? current
        : String(sheets[0]?.sheetKey ?? '') || null)
  }, [selectedSpreadsheetResource?.spreadsheetKey, selectedSpreadsheetResource?.sheets])

  useEffect(() => {
    const menus = selectedPortalResource?.menus ?? []
    setSelectedPortalMenuKey(current =>
      current && menus.some(menu => String(menu.menuKey ?? '') === current)
        ? current
        : String(menus[0]?.menuKey ?? '') || null)
    setPortalDragMenuKey(current =>
      current && menus.some(menu => String(menu.menuKey ?? '') === current)
        ? current
        : String(menus[0]?.menuKey ?? '') || null)
    setPortalDropTargetMenuKey(current =>
      current && menus.some(menu => String(menu.menuKey ?? '') === current)
        ? current
        : String(menus[1]?.menuKey ?? menus[0]?.menuKey ?? '') || null)
  }, [selectedPortalResource?.portalKey, selectedPortalResource?.menus])

  useEffect(() => {
    const fields = selectedDatasetResource?.fields ?? []
    setSelectedDatasetFieldKeys(current => {
      const currentKeys = parseChartFieldList(current)
      if (currentKeys.length > 0 && currentKeys.every(fieldKey => fields.some(field => field.fieldKey === fieldKey))) {
        return current
      }
      return fields.slice(0, 2).map(field => field.fieldKey).join(',')
    })
    const firstFolderKey = fields.map(field => field.folderKey?.trim()).find(Boolean)
    setDatasetFieldFolderKey(current => current || firstFolderKey || '')
  }, [selectedDatasetResource?.datasetKey, selectedDatasetResource?.fields])

  useEffect(() => {
    let cancelled = false
    setLoadingCharts(true)
    biApi.listChartResources()
      .then(response => {
        if (!cancelled) {
          const charts = response.data ?? []
          setChartResources(charts)
          setSelectedChartKey(current => current ?? charts[0]?.chartKey ?? null)
        }
      })
      .catch(() => {
        if (!cancelled) setChartResources([])
      })
      .finally(() => {
        if (!cancelled) setLoadingCharts(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    const chartKey = selectedChartResource?.chartKey
    if (!chartKey) return
    let cancelled = false
    // 图表引用影响用于提示修改/发布图表会波及哪些仪表盘、门户和订阅。
    biApi.getChartReferenceImpact(chartKey)
      .then(response => {
        const impact = response.data
        if (!cancelled && impact && !Array.isArray(impact)) {
          setChartReferenceImpacts(current => ({ ...current, [chartKey]: impact }))
        }
      })
      .catch(() => {
        if (!cancelled) {
          // 服务端影响接口失败时清空缓存，让 UI 使用本地资源关系兜底。
          setChartReferenceImpacts(current => {
            const next = { ...current }
            delete next[chartKey]
            return next
          })
        }
      })
    return () => {
      cancelled = true
    }
  }, [selectedChartResource?.chartKey])

  useEffect(() => {
    // 切换图表后把已保存查询条件同步到表单草稿和 ref，保持保存动作读取一致。
    const filterField = String(selectedChartResource?.query.filters?.[0]?.field ?? '')
    const filterOperator = String(selectedChartResource?.query.filters?.[0]?.operator ?? 'IN')
    const filterValue = chartFilterValueText(selectedChartResource?.query.filters?.[0]?.value)
    const sortField = String(selectedChartResource?.query.sorts?.[0]?.field ?? '')
    const sortDirection = String(selectedChartResource?.query.sorts?.[0]?.direction ?? 'DESC')
    chartFilterFieldDraftRef.current = filterField
    chartFilterOperatorDraftRef.current = filterOperator
    chartFilterValueDraftRef.current = filterValue
    chartSortFieldDraftRef.current = sortField
    chartSortDirectionDraftRef.current = sortDirection
    setChartFilterFieldDraft(filterField)
    setChartFilterOperatorDraft(filterOperator)
    setChartFilterValueDraft(filterValue)
    setChartSortFieldDraft(sortField)
    setChartSortDirectionDraft(sortDirection)
  }, [selectedChartResource?.chartKey])

  const reloadResourceLocations = () => {
    setLoadingResourceLocations(true)
    return biApi.listResourceLocations()
      .then(response => setResourceLocations(response.data ?? []))
      .catch(() => setResourceLocations([]))
      .finally(() => setLoadingResourceLocations(false))
  }

  const reloadResourceOwnerships = () => {
    setLoadingResourceOwnerships(true)
    return biApi.listResourceOwnerships()
      .then(response => setResourceOwnerships(response.data ?? []))
      .catch(() => setResourceOwnerships([]))
      .finally(() => setLoadingResourceOwnerships(false))
  }

  const reloadResourceFavorites = () => {
    setLoadingResourceFavorites(true)
    return biApi.listResourceFavorites()
      .then(response => setResourceFavorites(response.data ?? []))
      .catch(() => setResourceFavorites([]))
      .finally(() => setLoadingResourceFavorites(false))
  }

  useEffect(() => {
    reloadResourceLocations()
    reloadResourceOwnerships()
    reloadResourceFavorites()
  }, [])

  useEffect(() => {
    if (!selectedMoveTarget.resourceKey) {
      setResourceComments([])
      setResourceLock(null)
      setPublishApprovals([])
      return undefined
    }
    let cancelled = false
    setLoadingResourceComments(true)
    setLoadingResourceLock(true)
    setLoadingPublishApprovals(true)
    biApi.listResourceComments(selectedMoveTarget.resourceType, selectedMoveTarget.resourceKey)
      .then(response => {
        if (!cancelled) setResourceComments(response.data ?? [])
      })
      .catch(() => {
        if (!cancelled) setResourceComments([])
      })
      .finally(() => {
        if (!cancelled) setLoadingResourceComments(false)
      })
    biApi.getResourceLock(selectedMoveTarget.resourceType, selectedMoveTarget.resourceKey)
      .then(response => {
        if (!cancelled) setResourceLock(response.data ?? null)
      })
      .catch(() => {
        if (!cancelled) setResourceLock(null)
      })
      .finally(() => {
        if (!cancelled) setLoadingResourceLock(false)
      })
    biApi.listPublishApprovals({
      resourceType: selectedMoveTarget.resourceType,
      resourceKey: selectedMoveTarget.resourceKey,
    })
      .then(response => {
        if (!cancelled) setPublishApprovals(response.data ?? [])
      })
      .catch(() => {
        if (!cancelled) setPublishApprovals([])
      })
      .finally(() => {
        if (!cancelled) setLoadingPublishApprovals(false)
      })
    return () => {
      cancelled = true
    }
  }, [selectedMoveTarget.resourceType, selectedMoveTarget.resourceKey])

  const reloadDashboardVersions = (targetDashboardKey = dashboardKey) => {
    setLoadingDashboardVersions(true)
    return biApi.listDashboardVersions(targetDashboardKey, 10)
      .then(response => setDashboardVersions(response.data ?? []))
      .catch(() => setDashboardVersions([]))
      .finally(() => setLoadingDashboardVersions(false))
  }

  const reloadChartVersions = (targetChartKey: string | null | undefined) => {
    if (!targetChartKey) {
      setChartVersions([])
      return Promise.resolve()
    }
    setLoadingChartVersions(true)
    return biApi.listChartVersions(targetChartKey, 10)
      .then(response => setChartVersions(response.data ?? []))
      .catch(() => setChartVersions([]))
      .finally(() => setLoadingChartVersions(false))
  }

  const reloadDatasetVersions = (targetDatasetKey: string | null | undefined) => {
    if (!targetDatasetKey) {
      setDatasetVersions([])
      return Promise.resolve()
    }
    setLoadingDatasetVersions(true)
    return biApi.listDatasetVersions(targetDatasetKey, 10)
      .then(response => setDatasetVersions(response.data ?? []))
      .catch(() => setDatasetVersions([]))
      .finally(() => setLoadingDatasetVersions(false))
  }

  const applyDatasetAccelerationPolicy = (policy: BiDatasetAccelerationPolicyView | null) => {
    setDatasetAccelerationPolicy(policy)
    if (!policy) return
    setDatasetAccelerationEnabled(policy.enabled)
    setDatasetAccelerationMode(policy.accelerationMode || 'DIRECT_QUERY')
    setDatasetAccelerationRefreshMode(policy.refreshMode || 'MANUAL')
    setDatasetAccelerationIntervalMinutes(String(policy.refreshIntervalMinutes ?? 60))
    setDatasetAccelerationTtlSeconds(String(policy.ttlSeconds ?? 300))
    setDatasetAccelerationMaxRows(String(policy.maxRows ?? 100000))
    setDatasetAccelerationCronExpression(policy.cronExpression ?? '')
  }

  const applyQuickEngineCapacity = (summary: BiQuickEngineCapacitySummary | null) => {
    setQuickEngineCapacity(summary)
    const policy = summary?.alertPolicy
    if (!policy) return
    setQuickEngineAlertEnabled(policy.enabled)
    setQuickEngineCapacityLimitRows(String(policy.capacityLimitRows ?? summary.capacityLimitRows ?? 1000000))
    setQuickEngineWarningThreshold(String(policy.warningThresholdPercent ?? 80))
    setQuickEngineCriticalThreshold(String(policy.criticalThresholdPercent ?? 95))
    setQuickEngineNotificationChannels((policy.notificationChannels ?? []).join(', '))
    setQuickEngineNotificationReceivers((policy.notificationReceivers ?? []).join(', '))
    const poolPolicy = summary?.tenantPoolPolicy
    if (!poolPolicy) return
    setQuickEnginePoolKey(poolPolicy.poolKey ?? 'STANDARD')
    setQuickEngineMaxConcurrentQueries(String(poolPolicy.maxConcurrentQueries ?? 8))
    setQuickEngineQueueLimit(String(poolPolicy.queueLimit ?? 50))
    setQuickEngineQueueTimeoutSeconds(String(poolPolicy.queueTimeoutSeconds ?? 120))
    setQuickEnginePoolWeight(String(poolPolicy.poolWeight ?? 100))
  }

  const reloadDatasetAccelerationPolicy = (targetDatasetKey: string | null | undefined) => {
    if (!targetDatasetKey) {
      applyDatasetAccelerationPolicy(null)
      return Promise.resolve()
    }
    return biApi.getDatasetAccelerationPolicy(targetDatasetKey)
      .then(response => applyDatasetAccelerationPolicy(response.data ?? null))
      .catch(() => applyDatasetAccelerationPolicy(null))
  }

  const reloadPortalVersions = (targetPortalKey: string | null | undefined) => {
    if (!targetPortalKey) {
      setPortalVersions([])
      return Promise.resolve()
    }
    setLoadingPortalVersions(true)
    return biApi.listPortalVersions(targetPortalKey, 10)
      .then(response => setPortalVersions(response.data ?? []))
      .catch(() => setPortalVersions([]))
      .finally(() => setLoadingPortalVersions(false))
  }

  const reloadBigScreenVersions = (targetScreenKey: string | null | undefined) => {
    if (!targetScreenKey) {
      setBigScreenVersions([])
      return Promise.resolve()
    }
    setLoadingBigScreenVersions(true)
    return biApi.listBigScreenVersions(targetScreenKey, 10)
      .then(response => setBigScreenVersions(response.data ?? []))
      .catch(() => setBigScreenVersions([]))
      .finally(() => setLoadingBigScreenVersions(false))
  }

  const reloadSpreadsheetVersions = (targetSpreadsheetKey: string | null | undefined) => {
    if (!targetSpreadsheetKey) {
      setSpreadsheetVersions([])
      return Promise.resolve()
    }
    setLoadingSpreadsheetVersions(true)
    return biApi.listSpreadsheetVersions(targetSpreadsheetKey, 10)
      .then(response => setSpreadsheetVersions(response.data ?? []))
      .catch(() => setSpreadsheetVersions([]))
      .finally(() => setLoadingSpreadsheetVersions(false))
  }

  useEffect(() => {
    let cancelled = false
    biApi.getDashboardResource(dashboardKey)
      .then(response => {
        if (!cancelled && response.data?.preset) {
          setDashboardResource(response.data)
          resetDashboardPreset(response.data.preset)
          setSelectedWidgetKey(response.data.preset.widgets[0]?.widgetKey)
        }
      })
      .catch(() => {
        biApi.getDashboardPreset(dashboardKey)
          .then(response => {
            if (!cancelled && response.data) {
              setDashboardResource(null)
              resetDashboardPreset(response.data)
              setSelectedWidgetKey(response.data.widgets[0]?.widgetKey)
            }
          })
          .catch(() => {
            const fallback = getDefaultDashboardPreset(dashboardKey)
            if (!cancelled) {
              setDashboardResource(null)
              resetDashboardPreset(fallback)
              setSelectedWidgetKey(fallback.widgets[0]?.widgetKey)
            }
          })
      })
    return () => {
      cancelled = true
    }
  }, [dashboardKey])

  useEffect(() => {
    reloadDashboardVersions(dashboardKey)
  }, [dashboardKey])

  useEffect(() => {
    reloadChartVersions(selectedChartResource?.chartKey)
  }, [selectedChartResource?.chartKey])

  useEffect(() => {
    reloadDatasetVersions(selectedDatasetResource?.datasetKey)
  }, [selectedDatasetResource?.datasetKey])

  useEffect(() => {
    reloadDatasetAccelerationPolicy(selectedDatasetResource?.datasetKey ?? dashboardPreset.datasetKey)
  }, [selectedDatasetResource?.datasetKey, dashboardPreset.datasetKey])

  useEffect(() => {
    reloadPortalVersions(selectedPortalResource?.portalKey)
  }, [selectedPortalResource?.portalKey])

  useEffect(() => {
    reloadBigScreenVersions(selectedBigScreenResource?.screenKey)
  }, [selectedBigScreenResource?.screenKey])

  useEffect(() => {
    reloadSpreadsheetVersions(selectedSpreadsheetResource?.spreadsheetKey)
  }, [selectedSpreadsheetResource?.spreadsheetKey])

  useEffect(() => {
    let cancelled = false
    setLoadingQueries(true)
    Promise.all(dashboardPreset.widgets.map(widget =>
      biApi.executeQuery(buildWidgetQueryRequest(dashboardPreset, widget, canvasId, dashboardRuntimeParameters))
        .then(response => [widget.widgetKey, response.data] as const)
        .catch(() => [widget.widgetKey, null] as const),
    ))
      .then(entries => {
        if (cancelled) return
        setQueryResults(Object.fromEntries(entries.filter((entry): entry is readonly [string, BiQueryResult] => Boolean(entry[1]))))
      })
      .finally(() => {
        if (!cancelled) setLoadingQueries(false)
      })
    return () => {
      cancelled = true
    }
  }, [dashboardPreset, canvasId, dashboardRuntimeParameters])

  useEffect(() => {
    let cancelled = false
    if (dashboardPreset.filters.length === 0) {
      setControlOptionResults({})
      return () => {
        cancelled = true
      }
    }
    setLoadingControlOptions(true)
    Promise.all(dashboardPreset.filters.map(filter =>
      biApi.executeQuery(buildDashboardControlOptionQuery(
        dashboardPreset,
        filter.filterKey,
        dashboardRuntimeParameters,
        canvasId,
      ))
        .then(response => [filter.filterKey, response.data] as const)
        .catch(() => [filter.filterKey, null] as const),
    ))
      .then(entries => {
        if (cancelled) return
        setControlOptionResults(Object.fromEntries(entries.filter((entry): entry is readonly [string, BiQueryResult] => Boolean(entry[1]))))
      })
      .finally(() => {
        if (!cancelled) setLoadingControlOptions(false)
      })
    return () => {
      cancelled = true
    }
  }, [dashboardPreset, canvasId, dashboardRuntimeParameters])

  useEffect(() => {
    if (loadingQueries) return
    let cancelled = false
    setLoadingGovernance(true)
    Promise.all([
      biApi.listQueryHistory(10).catch(() => ({ data: [] })),
      biApi.getQueryGovernanceSummary(100).catch(() => ({ data: null })),
      biApi.getQueryGovernancePolicy().catch(() => ({ data: null })),
      biApi.listQueryGovernanceAudit(8).catch(() => ({ data: [] })),
      biApi.getQueryCachePolicy().catch(() => ({ data: null })),
      biApi.getQueryCacheStats().catch(() => ({ data: null })),
      biApi.getQuickEngineCapacity(20).catch(() => ({ data: null })),
      biApi.listDatasourceHealth().catch(() => ({ data: [] })),
      biApi.listDatasourceHealthHistory(8).catch(() => ({ data: [] })),
      biApi.getDatasourceHealthSlo(100).catch(() => ({ data: null })),
      biApi.listDatasourceConnectors().catch(() => ({ data: [] })),
      biApi.listDatasourceOnboarding().catch(() => ({ data: [] })),
    ])
      .then(([
        historyResponse,
        summaryResponse,
        policyResponse,
        auditResponse,
        cachePolicyResponse,
        cacheStatsResponse,
        quickEngineCapacityResponse,
        healthResponse,
        healthHistoryResponse,
        healthSloResponse,
        connectorResponse,
        onboardingResponse,
      ]) => {
        if (cancelled) return
        setQueryHistory(historyResponse.data)
        setQueryGovernanceSummary(summaryResponse.data)
        setQueryGovernancePolicy(policyResponse.data)
        setQueryGovernanceAudit(auditResponse.data)
        setQueryCachePolicy(cachePolicyResponse.data)
        setQueryCacheStats(cacheStatsResponse.data)
        applyQuickEngineCapacity(quickEngineCapacityResponse.data)
        if (policyResponse.data) {
          setQueryPolicyTimeoutMs(String(policyResponse.data.defaultTimeoutMs))
          setQueryPolicyQuotaRows(String(policyResponse.data.defaultQuotaRows))
        }
        if (cachePolicyResponse.data) {
          setQueryCacheDefaultEnabled(cachePolicyResponse.data.defaultEnabled)
          setQueryCacheTtlSeconds(String(cachePolicyResponse.data.defaultTtlSeconds))
        }
        setDatasourceHealth(healthResponse.data)
        setDatasourceHealthHistory(healthHistoryResponse.data)
        setDatasourceHealthSlo(healthSloResponse.data)
        setDatasourceConnectors(connectorResponse.data)
        setDatasourceOnboarding(onboardingResponse.data)
      })
      .finally(() => {
        if (!cancelled) setLoadingGovernance(false)
      })
    return () => {
      cancelled = true
    }
  }, [loadingQueries])

  const reloadPermissions = () => {
    setLoadingPermissions(true)
    return Promise.all([
      selectedPermissionTarget.resourceKey
        ? biApi.listResourcePermissions({
          resourceType: selectedPermissionTarget.resourceType,
          resourceKey: selectedPermissionTarget.resourceKey,
        }).catch(() => ({ data: [] }))
        : Promise.resolve({ data: [] }),
      biApi.listRowPermissions(dashboardPreset.datasetKey).catch(() => ({ data: [] })),
      biApi.listColumnPermissions(dashboardPreset.datasetKey).catch(() => ({ data: [] })),
      biApi.listPermissionAudit(8).catch(() => ({ data: [] })),
      selectedPermissionTarget.resourceKey
        ? biApi.listPermissionRequests({
          resourceType: selectedPermissionTarget.resourceType,
          resourceKey: selectedPermissionTarget.resourceKey,
        }).catch(() => ({ data: [] }))
        : Promise.resolve({ data: [] }),
    ])
      .then(([resourceResponse, rowResponse, columnResponse, auditResponse, requestResponse]) => {
        setResourcePermissions(resourceResponse.data ?? [])
        setRowPermissions(rowResponse.data ?? [])
        setColumnPermissions(columnResponse.data ?? [])
        setPermissionAudit(auditResponse.data ?? [])
        setPermissionRequests(requestResponse.data ?? [])
      })
      .finally(() => setLoadingPermissions(false))
  }

  useEffect(() => {
    reloadPermissions()
  }, [dashboardPreset.datasetKey, selectedPermissionTarget.resourceType, selectedPermissionTarget.resourceKey])

  const reloadExports = () => {
    setLoadingExports(true)
    return biApi.listExports(10)
      .then(response => setExportJobs(response.data ?? []))
      .catch(() => setExportJobs([]))
      .finally(() => setLoadingExports(false))
  }

  useEffect(() => {
    reloadExports()
  }, [dashboardPreset.datasetKey])

  const reloadSubscriptions = () => {
    setLoadingSubscriptions(true)
    Promise.all([
      biApi.listSubscriptions(10).catch(() => ({ data: [] })),
      biApi.listAlerts(10).catch(() => ({ data: [] })),
      biApi.listDeliveryLogs(10).catch(() => ({ data: [] })),
      biApi.auditDeliveryLogs(50).catch(() => ({ data: null })),
      biApi.listDeliveryAttachments(20).catch(() => ({ data: [] })),
    ])
      .then(([subscriptionResponse, alertResponse, logResponse, auditResponse, attachmentResponse]) => {
        setSubscriptions(subscriptionResponse.data ?? [])
        setAlertRules(alertResponse.data ?? [])
        setDeliveryLogs(logResponse.data ?? [])
        setDeliveryAudit(auditResponse.data ?? null)
        setDeliveryAttachments(attachmentResponse.data ?? [])
      })
      .finally(() => setLoadingSubscriptions(false))
  }

  useEffect(() => {
    reloadSubscriptions()
  }, [dashboardPreset.datasetKey])

  const fields = datasetMetadata?.fields ?? []
  const dimensions = fields.filter(field => field.role === 'DIMENSION')
  const measures = fields.filter(field => field.role === 'MEASURE')
  const metrics = datasetMetadata?.metrics ?? dashboardPreset.widgets.flatMap(widget => widget.metrics).map(metricKey => ({ metricKey, dataType: 'NUMBER' }))
  const filteredDimensions = useMemo(
    () => filterDesignerItems(dimensions.map(field => ({ ...field, key: field.fieldKey, label: field.fieldKey })), designerSearch),
    [dimensions, designerSearch],
  )
  const filteredMeasures = useMemo(
    () => filterDesignerItems(measures.map(field => ({ ...field, key: field.fieldKey, label: field.fieldKey })), designerSearch),
    [measures, designerSearch],
  )
  const filteredMetrics = useMemo(
    () => filterDesignerItems(metrics.map(metric => ({ ...metric, key: metric.metricKey, label: metric.metricKey })), designerSearch),
    [metrics, designerSearch],
  )
  const filteredChartResources = useMemo(
    () => filterDesignerItems(chartResources.map(chart => ({
      ...chart,
      key: chart.chartKey,
      label: chart.name,
    })), designerSearch),
    [chartResources, designerSearch],
  )
  const filteredChartPalette = useMemo(
    () => filterDesignerItems(QUICKBI_CHART_PALETTE, designerSearch),
    [designerSearch],
  )
  const filteredControlPalette = useMemo(
    () => filterDesignerItems(QUICKBI_CONTROL_PALETTE, designerSearch),
    [designerSearch],
  )

  const createEmbedTicket = () => {
    setCreatingEmbedTicket(true)
    biApi.createEmbedTicket(buildEmbedTicketRequest(dashboardPreset, canvasId, 'INTERNAL_CANVAS', dashboardRuntimeParameters))
      .then(response => {
        setEmbedTicket(response.data)
        setConfigTab('interaction')
      })
      .finally(() => setCreatingEmbedTicket(false))
  }

  const createPortalEmbedTicket = () => {
    if (!selectedPortalResource) return
    setCreatingEmbedTicket(true)
    biApi.createEmbedTicket({
      resourceType: 'PORTAL',
      resourceKey: selectedPortalResource.portalKey,
      scope: 'INTERNAL_CANVAS',
      filters: {},
      ttlSeconds: 900,
      maxAccessCount: Math.max(3, selectedPortalResource.menus.length + 2),
    })
      .then(response => setEmbedTicket(response.data))
      .finally(() => setCreatingEmbedTicket(false))
  }

  const persistDashboardRuntimeParameters = (parameters: BiDashboardRuntimeParameters) => {
    dashboardRuntimeParametersRef.current = parameters
    setDashboardRuntimeState(current => ({
      dashboardKey: dashboardPreset.dashboardKey,
      username: current?.username ?? '',
      parameters,
      updatedAt: current?.updatedAt ?? null,
    }))
    biApi.saveDashboardRuntimeState(dashboardPreset.dashboardKey, buildDashboardRuntimeStateCommand(dashboardPreset, parameters))
      .then(response => setDashboardRuntimeState(response.data ?? null))
      .catch(() => undefined)
  }

  const changeDashboardRuntimeParameter = (filterKey: string, rawValue: string) => {
    if (rawValue) {
      setClearedDashboardRuntimeParameterKeys(keys => keys.filter(key => key !== filterKey))
    }
    const parameters = updateDashboardRuntimeParameters(dashboardPreset, dashboardRuntimeParametersRef.current, filterKey, rawValue)
    const nextSearchParams = stripDashboardRuntimeSearchParam(dashboardPreset, searchParams, filterKey)
    if (rawValue.trim()) {
      nextSearchParams.set(filterKey, rawValue.trim())
    }
    const nextQuery = nextSearchParams.toString()
    if (nextQuery !== searchParams.toString()) {
      navigate(nextQuery ? `/bi?${nextQuery}` : '/bi', { replace: true })
    }
    persistDashboardRuntimeParameters(parameters)
  }

  const clearDashboardRuntimeParameter = (filterKey: string) => {
    setClearedDashboardRuntimeParameterKeys(keys => keys.includes(filterKey) ? keys : [...keys, filterKey])
    const parameters = updateDashboardRuntimeParameters(dashboardPreset, dashboardRuntimeParameters, filterKey, '')
    const nextSearchParams = stripDashboardRuntimeSearchParam(dashboardPreset, searchParams, filterKey)
    const nextQuery = nextSearchParams.toString()
    if (nextQuery !== searchParams.toString()) {
      navigate(nextQuery ? `/bi?${nextQuery}` : '/bi', { replace: true })
    }
    persistDashboardRuntimeParameters(parameters)
  }

  const resetDashboardRuntimeParametersToDefaults = () => {
    setClearedDashboardRuntimeParameterKeys(dashboardRuntimeSearchParamKeys(dashboardPreset))
    const parameters = dashboardDefaultRuntimeParameters(dashboardPreset)
    const nextSearchParams = stripDashboardRuntimeSearchParams(dashboardPreset, searchParams)
    const nextQuery = nextSearchParams.toString()
    if (nextQuery !== searchParams.toString()) {
      navigate(nextQuery ? `/bi?${nextQuery}` : '/bi', { replace: true })
    }
    persistDashboardRuntimeParameters(parameters)
  }

  const saveDashboard = () => {
    setSavingDashboard(true)
    const lockToken = resourceLockTokenFor(resourceLock, resourceLockToken, 'DASHBOARD', dashboardPreset.dashboardKey)
    biApi.saveDashboardDraft(dashboardPreset.dashboardKey, dashboardPreset as BiDashboardPreset, lockToken)
      .then(response => {
        setDashboardResource(response.data)
        resetDashboardPreset(response.data.preset)
      })
      .catch(() => undefined)
      .finally(() => setSavingDashboard(false))
  }

  const publishDashboard = () => {
    setPublishingDashboard(true)
    biApi.publishDashboard(dashboardPreset.dashboardKey)
      .then(response => {
        setDashboardResource(response.data)
        resetDashboardPreset(response.data.preset)
        return reloadDashboardVersions(response.data.preset.dashboardKey)
      })
      .catch(() => undefined)
      .finally(() => setPublishingDashboard(false))
  }

  const cloneDashboard = () => {
    setCloningDashboard(true)
    const command = buildDashboardCloneCommand(dashboardPreset, `copy-${Date.now().toString(36)}`)
    biApi.cloneDashboard(dashboardPreset.dashboardKey, command)
      .then(response => {
        setDashboardResource(response.data)
        resetDashboardPreset(response.data.preset)
        setSelectedWidgetKey(response.data.preset.widgets[0]?.widgetKey)
        setDashboardVersions([])
        const params = new URLSearchParams()
        params.set('dashboard', response.data.preset.dashboardKey)
        if (canvasId) params.set('canvasId', canvasId)
        navigate(`/bi?${params.toString()}`)
      })
      .catch(() => undefined)
      .finally(() => setCloningDashboard(false))
  }

  const exportDashboardPackage = () => {
    setExportingDashboard(true)
    biApi.exportDashboard(dashboardPreset.dashboardKey)
      .then(response => {
        const packagePayload = response.data ?? null
        setDashboardExportPackage(packagePayload)
        if (packagePayload) {
          return biApi.exportDashboardFile(dashboardPreset.dashboardKey)
            .then(blob => downloadDashboardPackage(blob, packagePayload))
        }
        return undefined
      })
      .catch(() => setDashboardExportPackage(null))
      .finally(() => setExportingDashboard(false))
  }

  const downloadDashboardPackage = (blob: Blob, packagePayload: BiDashboardExportPackage) => {
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = dashboardPackageFileName(packagePayload)
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  }

  const importDashboardPackagePayload = (file: File, packagePayload: BiDashboardExportPackage) => {
    setImportingDashboard(true)
    const command = buildDashboardImportCommand(packagePayload, `import-${Date.now().toString(36)}`)
    biApi.importDashboardFile(file, command.dashboardKey, command.title, command.overwrite)
      .then(response => {
        setDashboardResource(response.data)
        resetDashboardPreset(response.data.preset)
        setSelectedWidgetKey(response.data.preset.widgets[0]?.widgetKey)
        setDashboardExportPackage(packagePayload)
        setDashboardVersions([])
        const params = new URLSearchParams()
        params.set('dashboard', response.data.preset.dashboardKey)
        if (canvasId) params.set('canvasId', canvasId)
        navigate(`/bi?${params.toString()}`)
      })
      .catch(() => undefined)
      .finally(() => setImportingDashboard(false))
  }

  const importDashboardPackage = () => {
    dashboardPackageInputRef.current?.click()
  }

  const importDashboardPackageFile = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setImportingDashboard(true)
    file.text()
      .then(text => parseDashboardPackageText<BiDashboardExportPackage>(text))
      .then(packagePayload => {
        setImportingDashboard(false)
        importDashboardPackagePayload(file, packagePayload)
      })
      .catch(() => {
        setDashboardExportPackage(null)
        setImportingDashboard(false)
      })
  }

  const archiveDashboard = () => {
    if (dashboardResource?.source !== 'PERSISTED') return
    setArchivingDashboard(true)
    biApi.archiveDashboard(dashboardPreset.dashboardKey)
      .then(response => {
        setDashboardResource(response.data)
        resetDashboardPreset(response.data.preset)
        setDashboardVersions([])
      })
      .catch(() => undefined)
      .finally(() => setArchivingDashboard(false))
  }

  const restoreDashboardVersion = (version: BiDashboardVersionView) => {
    setRestoringDashboardVersion(version.version)
    biApi.restoreDashboardVersion(
      version.dashboardKey,
      version.version,
      resourceLockTokenFor(resourceLock, resourceLockToken, 'DASHBOARD', version.dashboardKey),
    )
      .then(response => {
        setDashboardResource(response.data)
        resetDashboardPreset(response.data.preset)
        setSelectedWidgetKey(response.data.preset.widgets[0]?.widgetKey)
        return reloadDashboardVersions(response.data.preset.dashboardKey)
      })
      .catch(() => undefined)
      .finally(() => setRestoringDashboardVersion(null))
  }

  const restoreChartVersion = (version: BiChartVersionView) => {
    setRestoringChartVersion(version.version)
    biApi.restoreChartVersion(
      version.chartKey,
      version.version,
      resourceLockTokenFor(resourceLock, resourceLockToken, 'CHART', version.chartKey),
    )
      .then(response => {
        setChartResources(current => current.map(chart =>
          chart.chartKey === response.data.chartKey ? response.data : chart))
        setSelectedChartKey(response.data.chartKey)
        return reloadChartVersions(response.data.chartKey)
      })
      .catch(() => undefined)
      .finally(() => setRestoringChartVersion(null))
  }

  const upsertSelectedChartResource = (resource: BiChartResource) => {
    setChartResources(current => {
      const index = current.findIndex(chart => chart.chartKey === resource.chartKey)
      if (index < 0) return [...current, resource]
      return current.map(chart => chart.chartKey === resource.chartKey ? resource : chart)
    })
    setSelectedChartKey(resource.chartKey)
  }

  const parseChartFieldList = (value: string) => value
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)

  const updateSelectedChartResource = (patch: Omit<Partial<BiChartResource>, 'query'> & { query?: Partial<BiChartResource['query']> }) => {
    if (!selectedChartResource) return
    const nextDatasetKey = patch.datasetKey ?? patch.query?.datasetKey ?? selectedChartResource.datasetKey
    const updated: BiChartResource = {
      ...selectedChartResource,
      ...patch,
      datasetKey: nextDatasetKey,
      query: {
        ...selectedChartResource.query,
        ...patch.query,
        datasetKey: nextDatasetKey,
      },
    }
    upsertSelectedChartResource(updated)
  }

  const updateSelectedChartStyle = (patch: Record<string, unknown>) => {
    if (!selectedChartResource) return
    updateSelectedChartResource({
      style: {
        ...selectedChartResource.style,
        ...patch,
      },
    })
  }

  const updateSelectedChartAxisStyle = (patch: Record<string, unknown>) => {
    if (!selectedChartResource) return
    const currentAxis = selectedChartResource.style?.axis && typeof selectedChartResource.style.axis === 'object'
      ? selectedChartResource.style.axis as Record<string, unknown>
      : {}
    updateSelectedChartStyle({
      axis: {
        ...currentAxis,
        ...patch,
      },
    })
  }

  const updateSelectedChartLabelStyle = (patch: Record<string, unknown>) => {
    if (!selectedChartResource) return
    const currentLabels = selectedChartResource.style?.labels && typeof selectedChartResource.style.labels === 'object'
      ? selectedChartResource.style.labels as Record<string, unknown>
      : {}
    updateSelectedChartStyle({
      labels: {
        ...currentLabels,
        ...patch,
      },
    })
  }

  const updateSelectedChartConditionalFormat = (patch: Record<string, unknown>) => {
    if (!selectedChartResource) return
    const currentRule = Array.isArray(selectedChartResource.style?.conditionalFormats)
      && selectedChartResource.style.conditionalFormats[0]
      && typeof selectedChartResource.style.conditionalFormats[0] === 'object'
      ? selectedChartResource.style.conditionalFormats[0] as Record<string, unknown>
      : {}
    updateSelectedChartStyle({
      conditionalFormats: [{
        ...currentRule,
        ...patch,
      }],
    })
  }

  const updateSelectedChartInteraction = (patch: Record<string, unknown>) => {
    if (!selectedChartResource) return
    updateSelectedChartResource({
      interaction: {
        drillEnabled: Boolean(selectedChartResource.interaction?.drillEnabled ?? selectedChartResource.interaction?.drill),
        linkageTarget: String(selectedChartResource.interaction?.linkageTarget ?? ''),
        hyperlinkTemplate: String(selectedChartResource.interaction?.hyperlinkTemplate ?? ''),
        ...patch,
      },
    })
  }

  const applySelectedChartQueryDesigner = (patch: {
    selectedDimensions?: string[]
    selectedMetrics?: string[]
    filterField?: string
    filterOperator?: string
    filterValue?: string
    sortField?: string
    sortDirection?: string
    limit?: number | string
  }) => {
    if (!selectedChartResource) return
    // 合并局部表单变更和当前图表查询，再统一走查询 patch 归一化。
    updateSelectedChartResource({
      query: chartQueryPatchFromDesigner({
        datasetKey: selectedChartResource.datasetKey,
        selectedDimensions: patch.selectedDimensions ?? selectedChartResource.query.dimensions,
        selectedMetrics: patch.selectedMetrics ?? selectedChartResource.query.metrics,
        filterField: patch.filterField ?? chartFilterFieldDraftRef.current,
        filterOperator: patch.filterOperator ?? chartFilterOperatorDraftRef.current,
        filterValue: patch.filterValue ?? chartFilterValueDraftRef.current,
        sortField: patch.sortField ?? chartSortFieldDraftRef.current,
        sortDirection: patch.sortDirection ?? chartSortDirectionDraftRef.current,
        limit: patch.limit ?? selectedChartResource.query.limit,
      }),
    })
  }

  /** 将图表过滤值转换为表单展示文本，数组过滤使用逗号分隔。 */
  const chartFilterValueText = (value: unknown) => Array.isArray(value) ? value.join(',') : String(value ?? '')

  /** 处理图表字段拖放，按角色写入维度或指标槽。 */
  const dropSelectedChartField = (role: 'DIMENSION' | 'METRIC', fieldKey: string) => {
    if (!selectedChartResource) return
    const next = chartQueryFieldsAfterDrop({
      dimensions: selectedChartResource.query.dimensions,
      metrics: selectedChartResource.query.metrics,
    }, role, fieldKey)
    applySelectedChartQueryDesigner({
      selectedDimensions: next.dimensions,
      selectedMetrics: next.metrics,
    })
  }

  /** 复制当前图表草稿，并生成不冲突的新 chartKey。 */
  const copySelectedChartDraft = () => {
    if (!selectedChartResource) return
    const baseKey = `${selectedChartResource.chartKey}-copy`
    const existingKeys = new Set(chartResources.map(chart => chart.chartKey))
    let chartKey = baseKey
    let index = 2
    // 副本 key 需要避开已有图表，保证本地 upsert 不覆盖原资源。
    while (existingKeys.has(chartKey)) {
      chartKey = `${baseKey}-${index}`
      index += 1
    }
    upsertSelectedChartResource({
      ...selectedChartResource,
      chartKey,
      name: `${selectedChartResource.name} 副本`,
      status: 'DRAFT',
      source: 'PERSISTED',
    })
  }

  /** 保存当前图表草稿，保存前重新归一化查询条件。 */
  const saveChartDraft = () => {
    if (!selectedChartResource) return
    const resourceToSave: BiChartResource = {
      ...selectedChartResource,
      query: {
        ...selectedChartResource.query,
        // 使用 ref 中的最新表单值，避免 onChange 后立即保存读到旧 state。
        ...chartQueryPatchFromDesigner({
          datasetKey: selectedChartResource.datasetKey,
          selectedDimensions: selectedChartResource.query.dimensions,
          selectedMetrics: selectedChartResource.query.metrics,
          filterField: chartFilterFieldDraftRef.current,
          filterOperator: chartFilterOperatorDraftRef.current,
          filterValue: chartFilterValueDraftRef.current,
          sortField: chartSortFieldDraftRef.current,
          sortDirection: chartSortDirectionDraftRef.current,
          limit: selectedChartResource.query.limit,
        }),
      },
    }
    setSavingChart(selectedChartResource.chartKey)
    biApi.saveChartDraft(
      resourceToSave.chartKey,
      resourceToSave,
      resourceLockTokenFor(resourceLock, resourceLockToken, 'CHART', selectedChartResource.chartKey),
    )
      .then(response => {
        upsertSelectedChartResource(response.data)
        return reloadChartVersions(response.data.chartKey)
      })
      .catch(() => undefined)
      .finally(() => setSavingChart(null))
  }

  const upsertSelectedDatasetResource = (resource: BiDatasetResource) => {
    setDatasetResources(current => {
      const index = current.findIndex(dataset => dataset.datasetKey === resource.datasetKey)
      if (index < 0) return [...current, resource]
      return current.map(dataset => dataset.datasetKey === resource.datasetKey ? resource : dataset)
    })
    setSelectedDatasetKey(resource.datasetKey)
  }

  const moveSelectedDatasetFieldsToFolder = () => {
    if (!selectedDatasetResource) return
    const targetFieldKeys = new Set(parseChartFieldList(selectedDatasetFieldKeys))
    const folderKey = datasetFieldFolderKey.trim()
    if (targetFieldKeys.size === 0) return
    const updated: BiDatasetResource = {
      ...selectedDatasetResource,
      fields: selectedDatasetResource.fields.map(field =>
        targetFieldKeys.has(field.fieldKey)
          ? { ...field, folderKey: folderKey || null }
          : field),
    }
    upsertSelectedDatasetResource(updated)
  }

  const updateSelectedDatasetField = (patch: Partial<BiDatasetFieldResource>) => {
    if (!selectedDatasetResource || !selectedDatasetDetailField) return
    const updated: BiDatasetResource = {
      ...selectedDatasetResource,
      fields: selectedDatasetResource.fields.map(field =>
        field.fieldKey === selectedDatasetDetailField.fieldKey
          ? { ...field, ...patch }
          : field),
    }
    upsertSelectedDatasetResource(updated)
    setSelectedDatasetDetailFieldKey(selectedDatasetDetailField.fieldKey)
  }

  const applyDatasetBatchFieldConfig = () => {
    if (!selectedDatasetResource) return
    const targetFieldKeys = new Set(parseChartFieldList(selectedDatasetFieldKeys))
    if (targetFieldKeys.size === 0) return
    const formatPattern = datasetBatchFormat.trim()
    const updated: BiDatasetResource = {
      ...selectedDatasetResource,
      fields: selectedDatasetResource.fields.map(field =>
        targetFieldKeys.has(field.fieldKey)
          ? {
              ...field,
              role: datasetBatchRole,
              ...(formatPattern ? { formatPattern } : {}),
              visible: datasetBatchVisible,
            }
          : field),
    }
    upsertSelectedDatasetResource(updated)
  }

  const applyDatasetFieldDrop = (dragKey = datasetDragFieldKey, targetKey = datasetDropTargetFieldKey) => {
    if (!selectedDatasetResource || !dragKey || !targetKey || dragKey === targetKey) return
    const currentFields = [...selectedDatasetResource.fields]
      .sort((left, right) => Number(left.sortOrder ?? 0) - Number(right.sortOrder ?? 0))
    const dragged = currentFields.find(field => field.fieldKey === dragKey)
    const withoutDragged = currentFields.filter(field => field.fieldKey !== dragKey)
    const targetIndex = withoutDragged.findIndex(field => field.fieldKey === targetKey)
    if (!dragged || targetIndex < 0) return
    const insertIndex = datasetDropPosition === 'after' ? targetIndex + 1 : targetIndex
    const folderKey = datasetDropFolderKey.trim()
    const reordered = [
      ...withoutDragged.slice(0, insertIndex),
      { ...dragged, ...(folderKey ? { folderKey } : {}) },
      ...withoutDragged.slice(insertIndex),
    ].map((field, index) => ({ ...field, sortOrder: index + 1 }))
    upsertSelectedDatasetResource({
      ...selectedDatasetResource,
      fields: reordered,
    })
    setSelectedDatasetDetailFieldKey(dragKey)
  }

  const copySelectedDatasetDraft = () => {
    if (!selectedDatasetResource) return
    const baseKey = `${selectedDatasetResource.datasetKey}_copy`
    const existingKeys = new Set(datasetResources.map(dataset => dataset.datasetKey))
    let datasetKey = baseKey
    let index = 2
    while (existingKeys.has(datasetKey)) {
      datasetKey = `${baseKey}_${index}`
      index += 1
    }
    upsertSelectedDatasetResource({
      ...selectedDatasetResource,
      datasetKey,
      name: `${selectedDatasetResource.name} 副本`,
      status: 'DRAFT',
      source: 'PERSISTED',
    })
  }

  const saveDatasetDraft = () => {
    if (!selectedDatasetResource) return
    setSavingDataset(selectedDatasetResource.datasetKey)
    biApi.saveDatasetDraft(
      selectedDatasetResource.datasetKey,
      selectedDatasetResource,
      resourceLockTokenFor(resourceLock, resourceLockToken, 'DATASET', selectedDatasetResource.datasetKey),
    )
      .then(response => {
        upsertSelectedDatasetResource(response.data)
        return reloadDatasetVersions(response.data.datasetKey)
      })
      .catch(() => undefined)
      .finally(() => setSavingDataset(null))
  }

  const restoreDatasetVersion = (version: BiDatasetVersionView) => {
    setRestoringDatasetVersion(version.version)
    biApi.restoreDatasetVersion(
      version.datasetKey,
      version.version,
      resourceLockTokenFor(resourceLock, resourceLockToken, 'DATASET', version.datasetKey),
    )
      .then(response => {
        setDatasetResources(current => current.map(dataset =>
          dataset.datasetKey === response.data.datasetKey ? response.data : dataset))
        setSelectedDatasetKey(response.data.datasetKey)
        return reloadDatasetVersions(response.data.datasetKey)
      })
      .catch(() => undefined)
      .finally(() => setRestoringDatasetVersion(null))
  }

  const restorePortalVersion = (version: BiPortalVersionView) => {
    setRestoringPortalVersion(version.version)
    biApi.restorePortalVersion(
      version.portalKey,
      version.version,
      resourceLockTokenFor(resourceLock, resourceLockToken, 'PORTAL', version.portalKey),
    )
      .then(response => {
        setPortalResources(current => current.map(portal =>
          portal.portalKey === response.data.portalKey ? response.data : portal))
        setSelectedPortalKey(response.data.portalKey)
        return reloadPortalVersions(response.data.portalKey)
      })
      .catch(() => undefined)
      .finally(() => setRestoringPortalVersion(null))
  }

  const updateSelectedPortalNavigationConfig = (patch: Parameters<typeof updatePortalNavigationConfig>[1]) => {
    if (!selectedPortalResource) return
    const updated = updatePortalNavigationConfig(selectedPortalResource, patch) as BiPortalResource
    setPortalResources(current => current.map(portal =>
      portal.portalKey === updated.portalKey ? updated : portal))
    setSelectedPortalKey(updated.portalKey)
  }

  const updateSelectedPortalMenuConfig = (patch: Parameters<typeof updatePortalMenuConfig>[2]) => {
    if (!selectedPortalResource || !selectedPortalMenuKey) return
    const updated = updatePortalMenuConfig(selectedPortalResource, selectedPortalMenuKey, patch) as BiPortalResource
    setPortalResources(current => current.map(portal =>
      portal.portalKey === updated.portalKey ? updated : portal))
    setSelectedPortalKey(updated.portalKey)
    setSelectedPortalMenuKey(selectedPortalMenuKey)
  }

  const applyPortalMenuDrop = (
    draggedMenuKey = portalDragMenuKey,
    targetMenuKey = portalDropTargetMenuKey,
    position = portalDropPosition,
  ) => {
    if (!selectedPortalResource || !draggedMenuKey || !targetMenuKey) return
    const updated = reorderPortalMenuTree(selectedPortalResource, draggedMenuKey, targetMenuKey, position) as BiPortalResource
    setPortalResources(current => current.map(portal =>
      portal.portalKey === updated.portalKey ? updated : portal))
    setSelectedPortalKey(updated.portalKey)
    setSelectedPortalMenuKey(draggedMenuKey)
    setPortalDragMenuKey(draggedMenuKey)
    setPortalDropTargetMenuKey(targetMenuKey)
  }

  const moveSelectedPortalMenu = (direction: 'up' | 'down') => {
    if (!selectedPortalResource) return
    const targetMenuKey = selectedPortalMenuKey ?? String(selectedPortalResource.menus[0]?.menuKey ?? '')
    if (!targetMenuKey) return
    const updated = movePortalMenuItem(selectedPortalResource, targetMenuKey, direction) as BiPortalResource
    setPortalResources(current => current.map(portal =>
      portal.portalKey === updated.portalKey ? updated : portal))
    setSelectedPortalKey(updated.portalKey)
    setSelectedPortalMenuKey(targetMenuKey)
  }

  const savePortalDraft = () => {
    if (!selectedPortalResource) return
    setSavingPortal(selectedPortalResource.portalKey)
    biApi.savePortalDraft(
      selectedPortalResource.portalKey,
      selectedPortalResource,
      resourceLockTokenFor(resourceLock, resourceLockToken, 'PORTAL', selectedPortalResource.portalKey),
    )
      .then(response => {
        setPortalResources(current => current.map(portal =>
          portal.portalKey === response.data.portalKey ? response.data : portal))
        setSelectedPortalKey(response.data.portalKey)
        return reloadPortalVersions(response.data.portalKey)
      })
      .catch(() => undefined)
      .finally(() => setSavingPortal(null))
  }

  const defaultBigScreenDraft = () => buildBigScreenDraftResource({
    screenKey: `${dashboardPreset.dashboardKey}-big-screen`,
    name: `${dashboardPreset.title} 大屏`,
    description: dashboardPreset.description,
    dashboardKey: dashboardPreset.dashboardKey,
    datasetKey: dashboardPreset.datasetKey,
  }) as BiBigScreenResource

  const updateSelectedBigScreenLayout = (patch: Parameters<typeof updateBigScreenLayoutItem>[2]) => {
    const resource = selectedBigScreenResource ?? defaultBigScreenDraft()
    const targetWidgetKey = selectedBigScreenWidgetKey ?? String(resource.layout[0]?.widgetKey ?? '')
    const updated = updateBigScreenLayoutItem(resource, targetWidgetKey, patch) as BiBigScreenResource
    setBigScreenResources(current => upsertBigScreenResource(current, updated))
    setSelectedBigScreenKey(updated.screenKey)
    setSelectedBigScreenWidgetKey(String(patch.widgetKey ?? targetWidgetKey ?? updated.layout[0]?.widgetKey ?? ''))
  }

  const moveSelectedBigScreenLayout = (direction: 'left' | 'right' | 'up' | 'down') => {
    const resource = selectedBigScreenResource ?? defaultBigScreenDraft()
    const targetWidgetKey = selectedBigScreenWidgetKey ?? String(resource.layout[0]?.widgetKey ?? '')
    const updated = moveBigScreenLayoutItem(resource, targetWidgetKey, direction) as BiBigScreenResource
    setBigScreenResources(current => upsertBigScreenResource(current, updated))
    setSelectedBigScreenKey(updated.screenKey)
    setSelectedBigScreenWidgetKey(targetWidgetKey)
  }

  const resizeSelectedBigScreenLayout = (direction: 'left' | 'right' | 'up' | 'down') => {
    const resource = selectedBigScreenResource ?? defaultBigScreenDraft()
    const targetWidgetKey = selectedBigScreenWidgetKey ?? String(resource.layout[0]?.widgetKey ?? '')
    const updated = resizeBigScreenLayoutItem(resource, targetWidgetKey, direction) as BiBigScreenResource
    setBigScreenResources(current => upsertBigScreenResource(current, updated))
    setSelectedBigScreenKey(updated.screenKey)
    setSelectedBigScreenWidgetKey(targetWidgetKey)
  }

  const alignSelectedBigScreenLayouts = (alignment: DashboardWidgetAlignment) => {
    const resource = selectedBigScreenResource ?? defaultBigScreenDraft()
    const widgetKeys = selectedBigScreenAlignWidgetKeys.length > 1
      ? selectedBigScreenAlignWidgetKeys
      : (resource.layout ?? []).map(item => String(item.widgetKey ?? '')).filter(Boolean)
    const updated = alignBigScreenLayoutItems(resource, widgetKeys, alignment) as BiBigScreenResource
    setBigScreenResources(current => upsertBigScreenResource(current, updated))
    setSelectedBigScreenKey(updated.screenKey)
  }

  const snapSelectedBigScreenLayout = () => {
    const resource = selectedBigScreenResource ?? defaultBigScreenDraft()
    const targetWidgetKey = selectedBigScreenWidgetKey ?? String(resource.layout[0]?.widgetKey ?? '')
    const result = snapBigScreenLayoutItem(resource, targetWidgetKey, 1)
    setBigScreenResources(current => upsertBigScreenResource(current, result.resource as BiBigScreenResource))
    setSelectedBigScreenKey(result.resource.screenKey)
    setSelectedBigScreenWidgetKey(targetWidgetKey)
    setBigScreenSnapGuideCount(result.guides.length)
  }

  const updateSelectedBigScreenMobileLayout = (variant: 'single-column' | 'compact-grid') => {
    const resource = selectedBigScreenResource ?? defaultBigScreenDraft()
    const updated = updateBigScreenMobileLayout(resource, variant) as BiBigScreenResource
    setBigScreenResources(current => upsertBigScreenResource(current, updated))
    setSelectedBigScreenKey(updated.screenKey)
  }

  const addSelectedBigScreenComponent = () => {
    const resource = selectedBigScreenResource ?? defaultBigScreenDraft()
    const updated = addBigScreenLibraryComponent(resource, selectedBigScreenComponentKey) as BiBigScreenResource
    const addedWidgetKey = String(updated.layout[updated.layout.length - 1]?.widgetKey ?? '')
    setBigScreenResources(current => upsertBigScreenResource(current, updated))
    setSelectedBigScreenKey(updated.screenKey)
    setSelectedBigScreenWidgetKey(addedWidgetKey)
  }

  const saveBigScreenDraft = () => {
    const resource = selectedBigScreenResource ?? defaultBigScreenDraft()
    setSavingBigScreen(resource.screenKey)
    biApi.saveBigScreenDraft(
      resource.screenKey,
      resource,
      resourceLockTokenFor(resourceLock, resourceLockToken, 'BIG_SCREEN', resource.screenKey),
    )
      .then(response => {
        setBigScreenResources(current => upsertBigScreenResource(current, response.data))
        setSelectedBigScreenKey(response.data.screenKey)
        return reloadBigScreenVersions(response.data.screenKey)
      })
      .catch(() => undefined)
      .finally(() => setSavingBigScreen(null))
  }

  const publishBigScreen = () => {
    if (!selectedBigScreenResource) return
    setPublishingBigScreen(selectedBigScreenResource.screenKey)
    biApi.publishBigScreen(selectedBigScreenResource.screenKey)
      .then(response => {
        setBigScreenResources(current => upsertBigScreenResource(current, response.data))
        setSelectedBigScreenKey(response.data.screenKey)
        return reloadBigScreenVersions(response.data.screenKey)
      })
      .catch(() => undefined)
      .finally(() => setPublishingBigScreen(null))
  }

  const archiveBigScreen = () => {
    if (!selectedBigScreenResource) return
    const archivedKey = selectedBigScreenResource.screenKey
    setArchivingBigScreen(archivedKey)
    biApi.archiveBigScreen(archivedKey)
      .then(response => {
        setBigScreenResources(current => upsertBigScreenResource(current, response.data))
        setSelectedBigScreenKey(current => current === archivedKey ? null : current)
        setBigScreenVersions([])
      })
      .catch(() => undefined)
      .finally(() => setArchivingBigScreen(null))
  }

  const restoreBigScreenVersion = (version: BiBigScreenVersionView) => {
    setRestoringBigScreenVersion(version.version)
    biApi.restoreBigScreenVersion(
      version.screenKey,
      version.version,
      resourceLockTokenFor(resourceLock, resourceLockToken, 'BIG_SCREEN', version.screenKey),
    )
      .then(response => {
        setBigScreenResources(current => upsertBigScreenResource(current, response.data))
        setSelectedBigScreenKey(response.data.screenKey)
        return reloadBigScreenVersions(response.data.screenKey)
      })
      .catch(() => undefined)
      .finally(() => setRestoringBigScreenVersion(null))
  }

  const defaultSpreadsheetDraft = () => buildSpreadsheetDraftResource({
    spreadsheetKey: `${dashboardPreset.dashboardKey}-spreadsheet`,
    name: `${dashboardPreset.title} 电子表格`,
    description: dashboardPreset.description,
    datasetKey: selectedDatasetResource?.datasetKey ?? dashboardPreset.datasetKey,
  }) as BiSpreadsheetResource

  const updateSelectedSpreadsheetCell = (cellKey: string, value: string) => {
    const resource = selectedSpreadsheetResource ?? defaultSpreadsheetDraft()
    const sheetKey = selectedSpreadsheetSheetKey ?? String(resource.sheets[0]?.sheetKey ?? 'summary')
    const updated = updateSpreadsheetCell(resource, sheetKey, cellKey, value) as BiSpreadsheetResource
    setSpreadsheetResources(current => upsertSpreadsheetResource(current, updated))
    setSelectedSpreadsheetKey(updated.spreadsheetKey)
    setSelectedSpreadsheetSheetKey(sheetKey)
    setSelectedSpreadsheetCellKey(cellKey.toUpperCase())
  }

  const fillSelectedSpreadsheetRange = () => {
    const range = spreadsheetFillRange.trim()
    if (!range) return
    const resource = selectedSpreadsheetResource ?? defaultSpreadsheetDraft()
    const sheetKey = selectedSpreadsheetSheetKey ?? String(resource.sheets[0]?.sheetKey ?? 'summary')
    const updated = updateSpreadsheetCellRange(resource, sheetKey, range, selectedSpreadsheetCellValue) as BiSpreadsheetResource
    setSpreadsheetResources(current => upsertSpreadsheetResource(current, updated))
    setSelectedSpreadsheetKey(updated.spreadsheetKey)
    setSelectedSpreadsheetSheetKey(sheetKey)
  }

  const updateSelectedSpreadsheetCellStyle = (patch: { bold?: boolean | null; backgroundColor?: string | null; textColor?: string | null }) => {
    const resource = selectedSpreadsheetResource ?? defaultSpreadsheetDraft()
    const sheetKey = selectedSpreadsheetSheetKey ?? String(resource.sheets[0]?.sheetKey ?? 'summary')
    const updated = updateSpreadsheetCellStyle(resource, sheetKey, selectedSpreadsheetCellKey, patch) as BiSpreadsheetResource
    setSpreadsheetResources(current => upsertSpreadsheetResource(current, updated))
    setSelectedSpreadsheetKey(updated.spreadsheetKey)
    setSelectedSpreadsheetSheetKey(sheetKey)
  }

  const updateSpreadsheetPivotMetric = (
    metricId: string,
    patch: Partial<Pick<SpreadsheetPivotMetricDraft, 'field' | 'aggregation' | 'label'>>,
  ) => {
    setSpreadsheetPivotMetrics(current => current.map(metric => (
      metric.id === metricId ? { ...metric, ...patch } : metric
    )))
  }

  const addSpreadsheetPivotMetric = () => {
    setSpreadsheetPivotMetrics(current => [
      ...current,
      {
        id: `metric-${Date.now()}-${current.length + 1}`,
        field: '',
        aggregation: 'SUM',
        label: '',
      },
    ])
  }

  const removeSpreadsheetPivotMetric = (metricId: string) => {
    setSpreadsheetPivotMetrics(current => (
      current.length > 1 ? current.filter(metric => metric.id !== metricId) : current
    ))
  }

  const moveSpreadsheetPivotMetric = (metricId: string, direction: 'up' | 'down') => {
    setSpreadsheetPivotMetrics(current => {
      const index = current.findIndex(metric => metric.id === metricId)
      if (index < 0) return current
      const targetIndex = direction === 'up' ? index - 1 : index + 1
      if (targetIndex < 0 || targetIndex >= current.length) return current
      const next = [...current]
      const [metric] = next.splice(index, 1)
      next.splice(targetIndex, 0, metric)
      return next
    })
  }

  const assignSpreadsheetPivotField = (role: SpreadsheetPivotDropRole, field: string) => {
    const normalizedField = field.trim()
    if (!normalizedField) return
    if (role === 'ROW') {
      setSpreadsheetPivotRowField(normalizedField)
      return
    }
    if (role === 'COLUMN') {
      setSpreadsheetPivotColumnField(normalizedField)
      return
    }
    setSpreadsheetPivotMetrics(current => {
      if (current.some(metric => metric.field.trim() === normalizedField)) return current
      const sourceFieldSet = new Set(spreadsheetPivotFields)
      const replaceIndex = current.findIndex(metric => (
        !metric.field.trim() || (sourceFieldSet.size > 0 && !sourceFieldSet.has(metric.field.trim()))
      ))
      const nextMetric = {
        id: replaceIndex >= 0 ? current[replaceIndex].id : `metric-${Date.now()}-${current.length + 1}`,
        field: normalizedField,
        aggregation: 'SUM' as BiSpreadsheetPivotAggregation,
        label: normalizedField,
      }
      if (replaceIndex < 0) return [...current, nextMetric]
      return current.map((metric, index) => (index === replaceIndex ? nextMetric : metric))
    })
  }

  const dragSpreadsheetPivotField = (event: ReactDragEvent<HTMLElement>, field: string) => {
    event.dataTransfer.effectAllowed = 'copy'
    event.dataTransfer.setData('application/x-bi-spreadsheet-pivot-field', field)
    event.dataTransfer.setData('text/plain', field)
  }

  const dropSpreadsheetPivotField = (event: ReactDragEvent<HTMLElement>, role: SpreadsheetPivotDropRole) => {
    event.preventDefault()
    const field = event.dataTransfer.getData('application/x-bi-spreadsheet-pivot-field') || event.dataTransfer.getData('text/plain')
    assignSpreadsheetPivotField(role, field)
  }

  const normalizedSpreadsheetPivotMetrics = () => spreadsheetPivotMetrics
    .map(metric => ({
      field: metric.field.trim(),
      aggregation: metric.aggregation,
      label: metric.label.trim() || metric.field.trim(),
    }))
    .filter(metric => metric.field)

  const generateSelectedSpreadsheetPivotTable = () => {
    const resource = selectedSpreadsheetResource ?? defaultSpreadsheetDraft()
    const sheetKey = selectedSpreadsheetSheetKey ?? String(resource.sheets[0]?.sheetKey ?? 'summary')
    const metric = normalizedSpreadsheetPivotMetrics()[0] ?? { field: '', aggregation: 'SUM' as BiSpreadsheetPivotAggregation, label: '' }
    const updated = buildSpreadsheetPivotTable(resource, sheetKey, {
      sourceRange: spreadsheetPivotSourceRange,
      targetCell: spreadsheetPivotTargetCell,
      rowField: spreadsheetPivotRowField,
      columnField: spreadsheetPivotColumnField,
      valueField: metric.field,
      aggregation: metric.aggregation,
    }) as BiSpreadsheetResource
    setSpreadsheetResources(current => upsertSpreadsheetResource(current, updated))
    setSelectedSpreadsheetKey(updated.spreadsheetKey)
    setSelectedSpreadsheetSheetKey(sheetKey)
    setSelectedSpreadsheetCellKey(spreadsheetPivotTargetCell.toUpperCase())
  }

  const generateSelectedSpreadsheetMultiMetricPivotTable = () => {
    const resource = selectedSpreadsheetResource ?? defaultSpreadsheetDraft()
    const sheetKey = selectedSpreadsheetSheetKey ?? String(resource.sheets[0]?.sheetKey ?? 'summary')
    const metrics = normalizedSpreadsheetPivotMetrics()
    const firstMetric = metrics[0] ?? { field: '', aggregation: 'SUM' as BiSpreadsheetPivotAggregation, label: '' }
    const updated = buildSpreadsheetPivotTable(resource, sheetKey, {
      sourceRange: spreadsheetPivotSourceRange,
      targetCell: spreadsheetPivotTargetCell,
      rowField: spreadsheetPivotRowField,
      columnField: spreadsheetPivotColumnField,
      valueField: firstMetric.field,
      aggregation: firstMetric.aggregation,
      valueFields: metrics,
    }) as BiSpreadsheetResource
    setSpreadsheetResources(current => upsertSpreadsheetResource(current, updated))
    setSelectedSpreadsheetKey(updated.spreadsheetKey)
    setSelectedSpreadsheetSheetKey(sheetKey)
    setSelectedSpreadsheetCellKey(spreadsheetPivotTargetCell.toUpperCase())
  }

  const saveSpreadsheetDraft = () => {
    const resource = selectedSpreadsheetResource ?? defaultSpreadsheetDraft()
    setSavingSpreadsheet(resource.spreadsheetKey)
    biApi.saveSpreadsheetDraft(
      resource.spreadsheetKey,
      resource,
      resourceLockTokenFor(resourceLock, resourceLockToken, 'SPREADSHEET', resource.spreadsheetKey),
    )
      .then(response => {
        setSpreadsheetResources(current => upsertSpreadsheetResource(current, response.data))
        setSelectedSpreadsheetKey(response.data.spreadsheetKey)
        return reloadSpreadsheetVersions(response.data.spreadsheetKey)
      })
      .catch(() => undefined)
      .finally(() => setSavingSpreadsheet(null))
  }

  const publishSpreadsheet = () => {
    if (!selectedSpreadsheetResource) return
    setPublishingSpreadsheet(selectedSpreadsheetResource.spreadsheetKey)
    biApi.publishSpreadsheet(selectedSpreadsheetResource.spreadsheetKey)
      .then(response => {
        setSpreadsheetResources(current => upsertSpreadsheetResource(current, response.data))
        setSelectedSpreadsheetKey(response.data.spreadsheetKey)
        return reloadSpreadsheetVersions(response.data.spreadsheetKey)
      })
      .catch(() => undefined)
      .finally(() => setPublishingSpreadsheet(null))
  }

  const archiveSpreadsheet = () => {
    if (!selectedSpreadsheetResource) return
    const archivedKey = selectedSpreadsheetResource.spreadsheetKey
    setArchivingSpreadsheet(archivedKey)
    biApi.archiveSpreadsheet(archivedKey)
      .then(response => {
        setSpreadsheetResources(current => upsertSpreadsheetResource(current, response.data))
        setSelectedSpreadsheetKey(current => current === archivedKey ? null : current)
        setSpreadsheetVersions([])
      })
      .catch(() => undefined)
      .finally(() => setArchivingSpreadsheet(null))
  }

  const restoreSpreadsheetVersion = (version: BiSpreadsheetVersionView) => {
    setRestoringSpreadsheetVersion(version.version)
    biApi.restoreSpreadsheetVersion(
      version.spreadsheetKey,
      version.version,
      resourceLockTokenFor(resourceLock, resourceLockToken, 'SPREADSHEET', version.spreadsheetKey),
    )
      .then(response => {
        setSpreadsheetResources(current => upsertSpreadsheetResource(current, response.data))
        setSelectedSpreadsheetKey(response.data.spreadsheetKey)
        return reloadSpreadsheetVersions(response.data.spreadsheetKey)
      })
      .catch(() => undefined)
      .finally(() => setRestoringSpreadsheetVersion(null))
  }

  const selectWidget = (widgetKey: string, additive = false) => {
    setSelectedWidgetKey(widgetKey)
    setSelectedWidgetKeys(current => {
      if (!additive) return [widgetKey]
      if (current.includes(widgetKey)) {
        const next = current.filter(key => key !== widgetKey)
        return next.length > 0 ? next : [widgetKey]
      }
      return [...current, widgetKey]
    })
    setCompiledWidgetQuery(null)
    setConfigTab('data')
  }

  const duplicateWidget = (widgetKey: string) => {
    applyDashboardEdit(current => {
      const next = duplicateDashboardWidget(current, widgetKey)
      if (next !== current) {
        const copied = next.widgets[next.widgets.length - 1]
        setSelectedWidgetKey(copied.widgetKey)
        setSelectedWidgetKeys([copied.widgetKey])
      }
      return next
    })
    setCompiledWidgetQuery(null)
  }

  const removeWidget = (widgetKey: string) => {
    applyDashboardEdit(current => {
      const next = removeDashboardWidget(current, widgetKey)
      if (next !== current && selectedWidgetKey === widgetKey) {
        const fallbackKey = next.widgets[0]?.widgetKey ?? selectedWidgetKey
        setSelectedWidgetKey(fallbackKey)
        setSelectedWidgetKeys([fallbackKey])
      } else if (next !== current) {
        setSelectedWidgetKeys(keys => keys.filter(key => key !== widgetKey))
      }
      return next
    })
    setCompiledWidgetQuery(null)
  }

  const alignSelectedWidgets = (alignment: DashboardWidgetAlignment) => {
    if (selectedLayoutWidgetKeys.length < 2) return
    applyDashboardEdit(current => alignDashboardWidgets(current, selectedLayoutWidgetKeys, alignment))
  }

  const moveWidget = (widgetKey: string, direction: DashboardWidgetMoveDirection) => {
    applyDashboardEdit(current => moveDashboardWidget(current, widgetKey, direction))
  }

  const moveWidgetByPixels = (widgetKey: string, deltaX: number, deltaY: number) => {
    applyDashboardEdit(current => moveDashboardWidgetByPixels(current, widgetKey, deltaX, deltaY))
  }

  const resizeWidgetByPixels = (widgetKey: string, deltaX: number, deltaY: number) => {
    applyDashboardEdit(current => resizeDashboardWidgetByPixels(current, widgetKey, deltaX, deltaY))
  }

  const handleDashboardDesignerKeyDown = (event: ReactKeyboardEvent<HTMLElement>) => {
    const action = dashboardDesignerKeyboardActionFromEventLike(event)
    if (!action || !selectedWidgetKey) return
    event.preventDefault()
    event.stopPropagation()

    if (action.type === 'undo') {
      undoDashboardEdit()
      return
    }
    if (action.type === 'redo') {
      redoDashboardEdit()
      return
    }
    if (action.type === 'remove') {
      removeWidget(selectedWidgetKey)
      return
    }
    if (action.type === 'duplicate') {
      duplicateWidget(selectedWidgetKey)
      return
    }

    if (action.type === 'move') {
      moveWidget(selectedWidgetKey, action.direction)
      return
    }

    if (action.type === 'resize') {
      const deltaByDirection: Record<DashboardWidgetMoveDirection, [number, number]> = {
        left: [-DASHBOARD_GRID_ROW_HEIGHT, 0],
        right: [DASHBOARD_GRID_ROW_HEIGHT, 0],
        up: [0, -DASHBOARD_GRID_ROW_HEIGHT],
        down: [0, DASHBOARD_GRID_ROW_HEIGHT],
      }
      const [deltaX, deltaY] = deltaByDirection[action.direction]
      resizeWidgetByPixels(selectedWidgetKey, deltaX, deltaY)
    }
  }

  const moveSelectedResource = () => {
    if (!selectedMoveTarget.resourceKey) return
    const command = buildResourceMoveCommand(
      selectedMoveTarget.resourceType,
      selectedMoveTarget.resourceKey,
      moveFolderKey,
      selectedMoveLocation?.sortOrder ?? 0,
    )
    const key = resourceLocationIndexKey(command.resourceType, command.resourceKey)
    setMovingResource(key)
    biApi.moveResource(command)
      .then(response => {
        setResourceLocations(current => [
          ...current.filter(location =>
            resourceLocationIndexKey(location.resourceType, location.resourceKey) !== key),
          response.data,
        ].sort(compareResourceLocations))
      })
      .catch(() => undefined)
      .finally(() => setMovingResource(null))
  }

  const transferSelectedResource = () => {
    if (!selectedMoveTarget.resourceKey || !transferOwnerUser.trim()) return
    const command = buildResourceTransferCommand(
      selectedMoveTarget.resourceType,
      selectedMoveTarget.resourceKey,
      transferOwnerUser,
    )
    const key = resourceLocationIndexKey(command.resourceType, command.resourceKey)
    setTransferringResource(key)
    biApi.transferResource(command)
      .then(response => {
        setResourceOwnerships(current => [
          ...current.filter(ownership =>
            resourceLocationIndexKey(ownership.resourceType, ownership.resourceKey) !== key),
          response.data,
        ].sort(compareResourceOwnerships))
      })
      .catch(() => undefined)
      .finally(() => setTransferringResource(null))
  }

  const selectedBatchDatasetResourceKeys = () => parseChartFieldList(batchDatasetResourceKeys)
    .filter(key => datasetResources.some(dataset => dataset.datasetKey === key))

  const moveBatchDatasetResources = () => {
    const datasetKeys = selectedBatchDatasetResourceKeys()
    if (datasetKeys.length === 0) return
    setBatchDatasetResourceAction('move')
    Promise.all(datasetKeys.map((datasetKey, index) =>
      biApi.moveResource(buildResourceMoveCommand('DATASET', datasetKey, batchDatasetFolderKey, index))))
      .then(responses => {
        setResourceLocations(current => {
          const movedKeys = new Set(datasetKeys.map(key => resourceLocationIndexKey('DATASET', key)))
          return [
            ...current.filter(location => !movedKeys.has(resourceLocationIndexKey(location.resourceType, location.resourceKey))),
            ...responses.map(response => response.data),
          ].sort(compareResourceLocations)
        })
      })
      .catch(() => undefined)
      .finally(() => setBatchDatasetResourceAction(null))
  }

  const transferBatchDatasetResources = () => {
    const ownerUser = batchDatasetOwnerUser.trim()
    const datasetKeys = selectedBatchDatasetResourceKeys()
    if (datasetKeys.length === 0 || !ownerUser) return
    setBatchDatasetResourceAction('transfer')
    Promise.all(datasetKeys.map(datasetKey =>
      biApi.transferResource(buildResourceTransferCommand('DATASET', datasetKey, ownerUser))))
      .then(responses => {
        setResourceOwnerships(current => {
          const transferredKeys = new Set(datasetKeys.map(key => resourceLocationIndexKey('DATASET', key)))
          return [
            ...current.filter(ownership => !transferredKeys.has(resourceLocationIndexKey(ownership.resourceType, ownership.resourceKey))),
            ...responses.map(response => response.data),
          ].sort(compareResourceOwnerships)
        })
      })
      .catch(() => undefined)
      .finally(() => setBatchDatasetResourceAction(null))
  }

  const toggleSelectedFavorite = () => {
    if (!selectedMoveTarget.resourceKey) return
    const command = buildResourceFavoriteCommand(
      selectedMoveTarget.resourceType,
      selectedMoveTarget.resourceKey,
      !(selectedFavorite?.favorite === true),
    )
    const key = resourceLocationIndexKey(command.resourceType, command.resourceKey)
    setFavoritingResource(key)
    biApi.favoriteResource(command)
      .then(response => {
        setResourceFavorites(current => {
          const withoutTarget = current.filter(favorite =>
            resourceLocationIndexKey(favorite.resourceType, favorite.resourceKey) !== key)
          return response.data?.favorite
            ? [...withoutTarget, response.data].sort(compareResourceFavorites)
            : withoutTarget.sort(compareResourceFavorites)
        })
      })
      .catch(() => undefined)
      .finally(() => setFavoritingResource(null))
  }

  const addSelectedResourceComment = () => {
    if (!selectedMoveTarget.resourceKey || !resourceCommentText.trim()) return
    const command = buildResourceCommentCommand(
      selectedMoveTarget.resourceType,
      selectedMoveTarget.resourceKey,
      selectedMoveTarget.resourceType === 'DASHBOARD' ? selectedWidget.widgetKey : null,
      resourceCommentText,
    )
    setSavingResourceComment(true)
    biApi.addResourceComment(command)
      .then(response => {
        if (!response.data) return
        setResourceComments(current => [...current, response.data as BiResourceCommentView].sort(compareResourceComments))
        setResourceCommentText('')
      })
      .catch(() => undefined)
      .finally(() => setSavingResourceComment(false))
  }

  const deleteSelectedResourceComment = (commentId: number | null | undefined) => {
    if (!commentId) return
    setDeletingResourceComment(commentId)
    biApi.deleteResourceComment(commentId)
      .then(() => setResourceComments(current => current.filter(comment => comment.id !== commentId)))
      .catch(() => undefined)
      .finally(() => setDeletingResourceComment(null))
  }

  const acquireSelectedResourceLock = () => {
    if (!selectedMoveTarget.resourceKey) return
    const command = buildResourceLockCommand(
      selectedMoveTarget.resourceType,
      selectedMoveTarget.resourceKey,
      resourceLockToken,
      300,
    )
    setSavingResourceLock('acquire')
    biApi.acquireResourceLock(command)
      .then(response => setResourceLock(response.data ?? null))
      .catch(() => undefined)
      .finally(() => setSavingResourceLock(null))
  }

  const releaseSelectedResourceLock = () => {
    if (!selectedMoveTarget.resourceKey) return
    const command = buildResourceLockCommand(
      selectedMoveTarget.resourceType,
      selectedMoveTarget.resourceKey,
      resourceLock?.lockToken || resourceLockToken,
      null,
    )
    setSavingResourceLock('release')
    biApi.releaseResourceLock(command)
      .then(() => setResourceLock(null))
      .catch(() => undefined)
      .finally(() => setSavingResourceLock(null))
  }

  const requestSelectedPublishApproval = () => {
    if (!selectedMoveTarget.resourceKey) return
    const command = buildPublishApprovalRequestCommand(
      selectedMoveTarget.resourceType,
      selectedMoveTarget.resourceKey,
      publishApprovalReason || '发布前审批',
    )
    setSavingPublishApproval('request')
    biApi.requestPublishApproval(command)
      .then(response => {
        if (!response.data) return
        setPublishApprovals(current => [
          response.data as BiPublishApprovalView,
          ...current.filter(approval => approval.id !== response.data?.id),
        ].sort(comparePublishApprovals))
        setPublishApprovalReason('')
      })
      .catch(() => undefined)
      .finally(() => setSavingPublishApproval(null))
  }

  const reviewSelectedPublishApproval = (status: 'APPROVED' | 'REJECTED') => {
    if (!pendingPublishApproval?.id) return
    const command = buildPublishApprovalReviewCommand(status, publishReviewComment, pendingPublishApproval.id)
    setSavingPublishApproval(status)
    biApi.reviewPublishApproval(pendingPublishApproval.id, command)
      .then(response => {
        if (!response.data) return
        setPublishApprovals(current => [
          response.data as BiPublishApprovalView,
          ...current.filter(approval => approval.id !== response.data?.id),
        ].sort(comparePublishApprovals))
        setPublishReviewComment('')
      })
      .catch(() => undefined)
      .finally(() => setSavingPublishApproval(null))
  }

  const compileWidgetQuery = (widget: BiDashboardWidgetPreset) => {
    setCompilingWidgetKey(widget.widgetKey)
    biApi.compileQuery(buildWidgetQueryRequest(dashboardPreset, widget, canvasId, dashboardRuntimeParameters))
      .then(response => setCompiledWidgetQuery(response.data ?? null))
      .catch(() => setCompiledWidgetQuery(null))
      .finally(() => setCompilingWidgetKey(null))
  }

  const explainWidgetQuery = (widget: BiDashboardWidgetPreset) => {
    setExplainingWidgetKey(widget.widgetKey)
    const request = buildWidgetQueryRequest(dashboardPreset, widget, canvasId, dashboardRuntimeParameters)
    biApi.explainQuery(request)
      .then(response => setQueryExecutionPlan(response.data ?? null))
      .catch(() => setQueryExecutionPlan(null))
      .finally(() => setExplainingWidgetKey(null))
  }

  const handleDesignerAction = (actionKey: string) => {
    if (actionKey === 'save') {
      saveDashboard()
    } else if (actionKey === 'undo') {
      undoDashboardEdit()
    } else if (actionKey === 'redo') {
      redoDashboardEdit()
    } else if (actionKey === 'publish') {
      publishDashboard()
    } else if (actionKey === 'clone') {
      cloneDashboard()
    } else if (actionKey === 'export') {
      exportDashboardPackage()
    } else if (actionKey === 'import') {
      importDashboardPackage()
    } else if (actionKey === 'subscribe') {
      createDashboardSubscription()
    } else if (actionKey === 'embed') {
      createEmbedTicket()
    } else if (actionKey === 'archive') {
      archiveDashboard()
    }
  }

  const grantSelectedResourcePermission = (actionKey: string, savingKey: string) => {
    if (!selectedPermissionTarget.resourceKey) return
    setSavingPermission(savingKey)
    biApi.upsertResourcePermission({
      resourceType: selectedPermissionTarget.resourceType,
      resourceKey: selectedPermissionTarget.resourceKey,
      subjectType: 'ROLE',
      subjectId: 'OPERATOR',
      actionKey,
      effect: 'ALLOW',
    })
      .then(reloadPermissions)
      .finally(() => setSavingPermission(null))
  }

  const grantSelectedResourceUse = () => grantSelectedResourcePermission('USE', 'resource')

  const grantSelectedResourceEdit = () => grantSelectedResourcePermission('EDIT', 'edit')

  const grantSelectedResourceExport = () => grantSelectedResourcePermission('EXPORT', 'export')

  const saveResourcePermissionRule = () => {
    if (!selectedPermissionTarget.resourceKey) return
    const subjectId = resourcePermissionSubjectType === 'ALL'
      ? 'ALL'
      : resourcePermissionSubjectId.trim()
    if (!subjectId) return
    setSavingPermission('resource-editor')
    biApi.upsertResourcePermission({
      resourceType: selectedPermissionTarget.resourceType,
      resourceKey: selectedPermissionTarget.resourceKey,
      subjectType: resourcePermissionSubjectType,
      subjectId,
      actionKey: resourcePermissionAction,
      effect: resourcePermissionEffect,
    })
      .then(() => reloadPermissions())
      .finally(() => setSavingPermission(null))
  }

  const submitPermissionRequest = () => {
    if (!selectedPermissionTarget.resourceKey) return
    setSavingPermission('request')
    biApi.requestPermission({
      resourceType: selectedPermissionTarget.resourceType,
      resourceKey: selectedPermissionTarget.resourceKey,
      requestedAction: permissionRequestAction,
      reason: permissionRequestReason.trim() || null,
    })
      .then(response => {
        setPermissionRequests(current => [
          response.data,
          ...current.filter(request => request.id !== response.data.id),
        ])
        setPermissionRequestReason('')
        return reloadPermissions()
      })
      .finally(() => setSavingPermission(null))
  }

  const reviewPermissionRequest = (request: BiPermissionRequestView, status: 'APPROVED' | 'REJECTED') => {
    if (!request.id) return
    setReviewingPermissionRequest(`${status}-${request.id}`)
    biApi.reviewPermissionRequest(request.id, {
      requestId: request.id,
      status,
      reviewComment: permissionReviewComment.trim() || null,
    })
      .then(response => {
        setPermissionRequests(current => current.map(item => item.id === response.data.id ? response.data : item))
        return reloadPermissions()
      })
      .finally(() => setReviewingPermissionRequest(null))
  }

  const deleteResourcePermissionRule = (row: BiResourcePermissionView) => {
    if (!row.id) return
    setDeletingPermission(`resource-${row.id}`)
    biApi.deleteResourcePermission(row.id)
      .then(() => reloadPermissions())
      .finally(() => setDeletingPermission(null))
  }

  const deleteRowPermissionRule = (row: BiRowPermissionView) => {
    if (!row.id) return
    setDeletingPermission(`row-${row.id}`)
    biApi.deleteRowPermission(row.id)
      .then(() => reloadPermissions())
      .finally(() => setDeletingPermission(null))
  }

  const deleteColumnPermissionRule = (row: BiColumnPermissionView) => {
    if (!row.id) return
    setDeletingPermission(`column-${row.id}`)
    biApi.deleteColumnPermission(row.id)
      .then(() => reloadPermissions())
      .finally(() => setDeletingPermission(null))
  }

  const addCanvasRowPermission = () => {
    const scopedCanvasIds = canvasId ? [Number.isNaN(Number(canvasId)) ? canvasId : Number(canvasId)] : [12, 13]
    setSavingPermission('row')
    biApi.upsertRowPermission({
      datasetKey: dashboardPreset.datasetKey,
      ruleKey: canvasId ? `canvas-${canvasId}` : 'operator-canvas-scope',
      subjectType: 'ROLE',
      subjectId: 'OPERATOR',
      filters: [{ field: 'canvas_id', operator: 'IN', value: scopedCanvasIds }],
      filter: {},
      enabled: true,
    })
      .then(reloadPermissions)
      .finally(() => setSavingPermission(null))
  }

  const saveRowPermissionRule = () => {
    if (!isDatasetPermissionTarget || !selectedPermissionTarget.resourceKey) return
    const ruleKey = rowPermissionRuleKey.trim()
    const subjectId = rowPermissionSubjectType === 'ALL' ? 'ALL' : rowPermissionSubjectId.trim()
    if (!ruleKey || !subjectId) return
    let parsedFilter: Record<string, unknown>
    try {
      const parsed = JSON.parse(rowPermissionFilterJson || '{}')
      parsedFilter = parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
    } catch {
      parsedFilter = {}
    }
    setSavingPermission('row-editor')
    biApi.upsertRowPermission({
      datasetKey: selectedPermissionTarget.resourceKey,
      ruleKey,
      subjectType: rowPermissionSubjectType,
      subjectId,
      filters: [],
      filter: parsedFilter,
      enabled: rowPermissionEnabled,
    })
      .then(() => reloadPermissions())
      .finally(() => setSavingPermission(null))
  }

  const saveColumnPermissionRule = () => {
    if (!isDatasetPermissionTarget || !selectedPermissionTarget.resourceKey) return
    const fieldKey = columnPermissionFieldKey.trim()
    const subjectId = columnPermissionSubjectType === 'ALL' ? 'ALL' : columnPermissionSubjectId.trim()
    if (!fieldKey || !subjectId) return
    let parsedMask: Record<string, unknown>
    try {
      const parsed = JSON.parse(columnPermissionMaskJson || '{}')
      parsedMask = parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
    } catch {
      parsedMask = {}
    }
    setSavingPermission('column-editor')
    biApi.upsertColumnPermission({
      datasetKey: selectedPermissionTarget.resourceKey,
      fieldKey,
      subjectType: columnPermissionSubjectType,
      subjectId,
      policy: columnPermissionPolicy,
      mask: parsedMask,
      enabled: columnPermissionEnabled,
    })
      .then(() => reloadPermissions())
      .finally(() => setSavingPermission(null))
  }

  const applySensitiveFieldColumnPermissions = () => {
    if (!isDatasetPermissionTarget || !selectedPermissionTarget.resourceKey || columnPermissionShortcutFieldKeys.length === 0) return
    const subjectId = columnPermissionSubjectType === 'ALL' ? 'ALL' : columnPermissionSubjectId.trim()
    if (!subjectId) return
    setSavingPermission('sensitive-columns')
    Promise.all(columnPermissionShortcutFieldKeys.map(fieldKey =>
      biApi.upsertColumnPermission({
        datasetKey: selectedPermissionTarget.resourceKey!,
        fieldKey,
        subjectType: columnPermissionSubjectType,
        subjectId,
        policy: 'MASK',
        mask: { strategy: 'FIXED', replacement: 'MASKED' },
        enabled: true,
      })))
      .then(() => reloadPermissions())
      .finally(() => setSavingPermission(null))
  }

  const maskCanvasName = () => {
    setSavingPermission('column')
    biApi.upsertColumnPermission({
      datasetKey: dashboardPreset.datasetKey,
      fieldKey: 'canvas_name',
      subjectType: 'ROLE',
      subjectId: 'OPERATOR',
      policy: 'MASK',
      mask: { strategy: 'FIXED', replacement: 'MASKED' },
      enabled: true,
    })
      .then(reloadPermissions)
      .finally(() => setSavingPermission(null))
  }

  const selfServiceQuery = () => buildSelfServiceExtractionQuery(
    dashboardPreset,
    selfServiceExtraction,
    canvasId,
    dashboardRuntimeParameters,
    1000,
  )

  const dropSelfServiceField = (role: 'DIMENSION' | 'METRIC', fieldKey: string) => {
    setSelfServiceExtraction(current => dropSelfServiceExtractionField(current, role, fieldKey))
  }

  const removeSelfServiceField = (role: 'DIMENSION' | 'METRIC', fieldKey: string) => {
    setSelfServiceExtraction(current => removeSelfServiceExtractionField(current, role, fieldKey))
  }

  const previewSelfService = () => {
    setPreviewingSelfService(true)
    biApi.previewSelfService({ query: selfServiceQuery(), previewLimit: 50 })
      .then(response => setSelfServicePreview(response.data))
      .catch(() => setSelfServicePreview(null))
      .finally(() => setPreviewingSelfService(false))
  }

  const createSelfServiceExport = (approvalRequired = false) => {
    setCreatingExport(true)
    biApi.createExport({
      resourceType: 'DATASET',
      resourceKey: dashboardPreset.datasetKey,
      exportFormat: 'CSV',
      query: selfServiceQuery(),
      rowLimit: approvalRequired ? 6000 : 1000,
      approvalRequired,
      sensitive: approvalRequired,
      approvalReason: approvalRequired ? `${selectedWidget.title} 敏感取数导出` : null,
    })
      .then(() => reloadExports())
      .finally(() => setCreatingExport(false))
  }

  const reviewSelfServiceExport = (row: BiExportJobView, status: 'APPROVED' | 'REJECTED') => {
    setReviewingExport(`${row.id}:${status}`)
    biApi.reviewExport(
      row.id,
      buildExportApprovalReviewCommand(
        status,
        exportReviewComment.trim() || (status === 'APPROVED' ? '工作台通过' : '工作台驳回'),
      ),
    )
      .then(() => {
        setExportReviewComment('')
        return reloadExports()
      })
      .finally(() => setReviewingExport(null))
  }

  const inspectSelfServiceExport = (row: BiExportJobView) => {
    setExportDetailOpen(true)
    setExportDetail(null)
    setLoadingExportDetail(true)
    biApi.getExportDetail(row.id)
      .then(response => setExportDetail(response.data ?? null))
      .catch(() => setExportDetail({ job: row, request: {
        resourceType: row.resourceType,
        resourceKey: row.resourceKey,
        resourceId: row.resourceId,
        exportFormat: row.exportFormat,
        query: selfServiceQuery(),
        rowLimit: row.rowLimit,
        approvalRequired: row.approvalStatus === 'PENDING',
        sensitive: false,
        approvalReason: row.approvalReason ?? null,
      } }))
      .finally(() => setLoadingExportDetail(false))
  }

  const cancelSelfServiceExport = (row: BiExportJobView) => {
    setCancellingExportId(row.id)
    biApi.cancelExport(row.id)
      .then(() => reloadExports())
      .finally(() => setCancellingExportId(null))
  }

  const exportJobTableColumns = useMemo(
    () => createExportJobColumns(
      reviewingExport,
      cancellingExportId,
      reviewSelfServiceExport,
      inspectSelfServiceExport,
      cancelSelfServiceExport,
    ),
    [reviewingExport, cancellingExportId, exportReviewComment],
  )
  const resourcePermissionTableColumns = useMemo(
    () => createResourcePermissionColumns(deletingPermission, deleteResourcePermissionRule),
    [deletingPermission],
  )
  const rowPermissionTableColumns = useMemo(
    () => createRowPermissionColumns(deletingPermission, deleteRowPermissionRule),
    [deletingPermission],
  )
  const columnPermissionTableColumns = useMemo(
    () => createColumnPermissionColumns(deletingPermission, deleteColumnPermissionRule),
    [deletingPermission],
  )

  const exportAuditRows = useMemo(
    () => exportAuditDetailRows(exportDetail),
    [exportDetail],
  )

  const inspectQueryHistory = (row: BiQueryHistoryItem) => {
    setQueryHistoryDetailOpen(true)
    setQueryHistoryDetail(null)
    setLoadingQueryHistoryDetail(true)
    biApi.getQueryHistoryDetail(row.id)
      .then(response => setQueryHistoryDetail(response.data ?? null))
      .catch(() => setQueryHistoryDetail({ ...row, request: null }))
      .finally(() => setLoadingQueryHistoryDetail(false))
  }

  const queryHistoryRows = useMemo(
    () => queryHistoryDetailRows(queryHistoryDetail),
    [queryHistoryDetail],
  )

  const queryGovernanceRows = useMemo(
    () => queryGovernanceSummaryRows(queryGovernanceSummary),
    [queryGovernanceSummary],
  )

  const queryGovernancePolicyDisplayRows = useMemo(
    () => queryGovernancePolicyRows(queryGovernancePolicy),
    [queryGovernancePolicy],
  )

  const queryGovernanceAuditTableRows = useMemo(
    () => queryGovernanceAuditRows(queryGovernanceAudit),
    [queryGovernanceAudit],
  )

  const queryCachePolicyDisplayRows = useMemo(
    () => queryCachePolicyRows(queryCachePolicy),
    [queryCachePolicy],
  )

  const queryCacheStatsDisplayRows = useMemo(
    () => queryCacheStatsRows(queryCacheStats),
    [queryCacheStats],
  )

  const quickEngineCapacitySummaryDisplayRows = useMemo(
    () => quickEngineCapacitySummaryRows(quickEngineCapacity),
    [quickEngineCapacity],
  )

  const quickEngineConcurrencyQueueDisplayRows = useMemo(
    () => quickEngineConcurrencyQueueRows(quickEngineCapacity),
    [quickEngineCapacity],
  )

  const quickEngineCapacityDetailTableRows = useMemo(
    () => quickEngineCapacityDetailRows(quickEngineCapacity),
    [quickEngineCapacity],
  )

  const quickEngineCapacityUserTableRows = useMemo(
    () => quickEngineCapacityUserRows(quickEngineCapacity),
    [quickEngineCapacity],
  )

  const queryCacheResourceKey = useMemo(
    () => queryCacheResourceScope === 'DATASET'
      ? selectedDatasetResource?.datasetKey ?? dashboardPreset.datasetKey
      : dashboardPreset.dashboardKey,
    [queryCacheResourceScope, selectedDatasetResource?.datasetKey, dashboardPreset.datasetKey, dashboardPreset.dashboardKey],
  )

  useEffect(() => {
    const resourcePolicy = queryCachePolicy?.resources.find(resource =>
      resource.resourceType === queryCacheResourceScope && resource.resourceKey === queryCacheResourceKey)
    const enabled = resourcePolicy?.enabled ?? queryCachePolicy?.defaultEnabled ?? true
    const cacheMode = resourcePolicy?.cacheMode ?? queryCachePolicy?.defaultCacheMode ?? (enabled ? 'CACHE' : 'DIRECT_QUERY')
    setQueryCacheResourceEnabled(enabled)
    setQueryCacheResourceCacheMode(cacheMode)
    setQueryCacheResourceTtlSeconds(String(resourcePolicy?.ttlSeconds ?? queryCachePolicy?.defaultTtlSeconds ?? 300))
  }, [queryCachePolicy, queryCacheResourceScope, queryCacheResourceKey])

  const queryCacheInvalidationActions = useMemo(
    () => queryCacheInvalidationActionRows(selectedDatasetResource?.datasetKey ?? dashboardPreset.datasetKey),
    [selectedDatasetResource?.datasetKey, dashboardPreset.datasetKey],
  )

  const datasetAccelerationPolicyDisplayRows = useMemo(
    () => datasetAccelerationPolicyRows(datasetAccelerationPolicy),
    [datasetAccelerationPolicy],
  )

  const datasetAccelerationSchedulerTableRows = useMemo(
    () => datasetAccelerationSchedulerRows(datasetAccelerationSchedulerResult),
    [datasetAccelerationSchedulerResult],
  )

  const permissionAuditTableRows = useMemo(
    () => permissionAuditRows(permissionAudit),
    [permissionAudit],
  )

  const datasourceHealthHistoryTableRows = useMemo(
    () => datasourceHealthHistoryRows(datasourceHealthHistory),
    [datasourceHealthHistory],
  )

  const datasourceHealthSloDisplayRows = useMemo(
    () => datasourceHealthSloRows(datasourceHealthSlo),
    [datasourceHealthSlo],
  )

  const datasourceConnectorTableRows = useMemo(
    () => datasourceConnectorRows(datasourceConnectors),
    [datasourceConnectors],
  )

  const datasourceCapacityPolicyTableRows = useMemo(
    () => datasourceCapacityPolicyRows(datasourceConnectors),
    [datasourceConnectors],
  )

  const datasourceAdvancedCapabilityTableRows = useMemo(
    () => datasourceAdvancedCapabilityRows(datasourceConnectors),
    [datasourceConnectors],
  )

  const availableDatasourceConnectors = useMemo(
    () => datasourceConnectors.filter(connector => connector.supportStatus === 'AVAILABLE'),
    [datasourceConnectors],
  )

  const datasourceConnectorOptions = useMemo(() => {
    const connectors = availableDatasourceConnectors.length > 0 ? availableDatasourceConnectors : datasourceConnectors
    return connectors.map(connector => ({
      label: `${connector.label || connector.connectorType} / ${connector.connectorType}`,
      value: connector.connectorType,
    }))
  }, [availableDatasourceConnectors, datasourceConnectors])

  const selectedDatasourceConnector = useMemo(() => {
    const connectorType = datasourceDraft.connectorType || 'MYSQL'
    return datasourceConnectors.find(connector => connector.connectorType === connectorType)
      ?? availableDatasourceConnectors[0]
      ?? datasourceConnectors[0]
      ?? null
  }, [availableDatasourceConnectors, datasourceConnectors, datasourceDraft.connectorType])

  const datasourceConnectorValue = selectedDatasourceConnector?.connectorType
    ?? datasourceDraft.connectorType
    ?? 'MYSQL'
  const isApiDatasourceConnector = datasourceConnectorValue === 'API'
    || selectedDatasourceConnector?.sourceCategory === 'HTTP'
    || selectedDatasourceConnector?.sourceCategory === 'APP'
  const isFileDatasourceConnector = datasourceConnectorValue === 'CSV_EXCEL'
    || selectedDatasourceConnector?.sourceCategory === 'FILE'
  const datasourceRequiresCredentials = selectedDatasourceConnector?.supportsCredentials !== false

  const datasourceConnectionModeOptions = useMemo(() => {
    const supportedModes = selectedDatasourceConnector?.supportedModes?.length
      ? selectedDatasourceConnector.supportedModes
      : ['DIRECT_QUERY', 'CACHE']
    const labels: Record<string, string> = {
      DIRECT_QUERY: '直连查询',
      CACHE: '查询缓存',
      EXTRACT: '抽取加速',
    }
    return supportedModes.map(mode => ({
      label: labels[mode] ?? mode,
      value: mode,
    }))
  }, [selectedDatasourceConnector])

  const datasourceConnectionModeValue = datasourceConnectionModeOptions.some(option => option.value === datasourceDraft.connectionMode)
    ? datasourceDraft.connectionMode ?? 'DIRECT_QUERY'
    : datasourceConnectionModeOptions[0]?.value ?? 'DIRECT_QUERY'

  const datasourceWizardStepItems = useMemo(() => [
    { title: '连接器配置' },
    { title: '连接凭证' },
    { title: '接入复核' },
  ], [])

  const datasourceConnectorCapabilityTags = useMemo(() => {
    if (!selectedDatasourceConnector) return []
    return [
      selectedDatasourceConnector.sourceCategory,
      selectedDatasourceConnector.capacityCategory,
      selectedDatasourceConnector.supportsConnectionTest ? '连接测试' : '',
      selectedDatasourceConnector.supportsSchemaSync ? 'Schema 同步' : '',
      selectedDatasourceConnector.supportsTableDataset ? '表数据集' : '',
      selectedDatasourceConnector.supportsSqlDataset ? 'SQL 数据集' : '',
      selectedDatasourceConnector.supportsCredentials ? '凭证' : '',
    ].filter((item): item is string => Boolean(item))
  }, [selectedDatasourceConnector])

  const datasourceOnboardingTableRows = useMemo(
    () => datasourceOnboardingRows(datasourceOnboarding),
    [datasourceOnboarding],
  )

  const datasourceNextActionTableRows = useMemo(
    () => datasourceNextActionRows(datasourceOnboarding),
    [datasourceOnboarding],
  )

  const credentialRotationDatasource = useMemo(
    () => datasourceOnboarding.find(source => source.id === credentialRotationDatasourceId) ?? null,
    [credentialRotationDatasourceId, datasourceOnboarding],
  )

  const datasourceLocationProvided = isFileDatasourceConnector
    ? Boolean(datasourceUploadFile || datasourceDraft.url?.trim() || datasourceDraft.fileName?.trim())
    : Boolean(datasourceDraft.url?.trim())
  const datasourceDraftMissingRequiredFields = !datasourceDraft.name?.trim()
    || !datasourceLocationProvided
    || (datasourceRequiresCredentials && !datasourceDraft.username?.trim())
    || (datasourceRequiresCredentials && !editingDatasourceId && !datasourceDraft.password?.trim())

  const datasourceWizardReviewCommand = useMemo(
    () => buildDatasourceOnboardingCommand(
      { ...datasourceDraft, connectorType: datasourceConnectorValue, connectionMode: datasourceConnectionModeValue },
      datasourceConnectors,
    ),
    [datasourceConnectionModeValue, datasourceConnectorValue, datasourceConnectors, datasourceDraft],
  )

  const datasourceWizardReviewRows = useMemo(() => {
    const connectorConfig = datasourceWizardReviewCommand.connectorConfig as Record<string, unknown> | undefined
    const rows = [
      { label: '连接器', value: `${selectedDatasourceConnector?.label || datasourceWizardReviewCommand.connectorType} / ${datasourceWizardReviewCommand.connectorType}` },
      { label: '连接模式', value: datasourceConnectionModeOptions.find(option => option.value === datasourceWizardReviewCommand.connectionMode)?.label ?? datasourceWizardReviewCommand.connectionMode },
      { label: '数据源', value: datasourceWizardReviewCommand.name || '-' },
      { label: '地址', value: datasourceWizardReviewCommand.url || '-' },
      { label: '账号', value: datasourceWizardReviewCommand.username || '-' },
      { label: '驱动', value: datasourceWizardReviewCommand.driverClassName || '-' },
      { label: '状态', value: datasourceWizardReviewCommand.enabled ? '启用' : '停用' },
    ]
    if (connectorConfig) {
      if (isFileDatasourceConnector) {
        rows.push(
          { label: '文件名', value: String(connectorConfig.fileName ?? '-') },
          { label: '文件类型', value: String(connectorConfig.fileType ?? '-') },
          { label: '工作表', value: String(connectorConfig.sheetName ?? '-') },
          { label: '编码', value: String(connectorConfig.encoding ?? '-') },
        )
      } else {
        rows.push(
          { label: '请求方法', value: String(connectorConfig.requestMethod ?? '-') },
          { label: '认证类型', value: String(connectorConfig.authType ?? '-') },
          { label: '响应路径', value: String(connectorConfig.responseRowsPath ?? '-') },
        )
      }
    }
    return rows
  }, [datasourceConnectionModeOptions, datasourceWizardReviewCommand, isFileDatasourceConnector, selectedDatasourceConnector])

  const datasourceWizardNextDisabled = datasourceWizardStep === 0
    ? !selectedDatasourceConnector
    : datasourceDraftMissingRequiredFields

  const datasourceConnectionTestDisplayRows = useMemo(
    () => datasourceConnectionTestRows(datasourceConnectionTestResult),
    [datasourceConnectionTestResult],
  )

  const datasourceSchemaPreviewTableRows = useMemo(
    () => datasourceSchemaPreviewRows(datasourceSchemaPreview),
    [datasourceSchemaPreview],
  )

  const hasApiDatasourceOnboarding = useMemo(
    () => datasourceOnboarding.some(datasource =>
      datasource.connectorType === 'API' || datasource.type === 'API' || datasource.driverClassName === 'HTTP_JSON',
    ),
    [datasourceOnboarding],
  )

  const datasourceApiPreviewTableColumns = useMemo<ColumnsType<Record<string, unknown> & { __rowKey: number }>>(() => (
    (datasourceApiPreview?.columns ?? []).map(column => ({
      title: column.key,
      dataIndex: column.key,
      key: column.key,
      width: 140,
      ellipsis: true,
      render: (value: unknown) => value == null ? '-' : String(value),
    }))
  ), [datasourceApiPreview])

  const datasourceApiPreviewTableRows = useMemo(
    () => (datasourceApiPreview?.rows ?? []).map((row, index) => ({ ...row, __rowKey: index })),
    [datasourceApiPreview],
  )

  const datasourceApiPreviewDisplayRows = useMemo(() => {
    if (!datasourceApiPreview) return []
    return [
      { label: '数据源', value: `${datasourceApiPreview.name || '-'} · ${datasourceApiPreview.sourceKey || '-'}` },
      { label: '行数', value: `${datasourceApiPreview.rowCount} 行${datasourceApiPreview.truncated ? ' · 已截断' : ''}` },
      { label: '耗时', value: `${datasourceApiPreview.durationMs} ms` },
      { label: '检查时间', value: datasourceApiPreview.checkedAt || '-' },
    ]
  }, [datasourceApiPreview])

  const datasourceSchemaSnapshotDisplayRows = useMemo(
    () => datasourceSchemaSnapshotRows(datasourceSchemaSnapshot),
    [datasourceSchemaSnapshot],
  )

  const datasourceSchemaSnapshotHistoryTableRows = useMemo(
    () => datasourceSchemaSnapshotHistoryRows(datasourceSchemaSnapshots),
    [datasourceSchemaSnapshots],
  )

  const datasourceSchemaTableNames = useMemo(
    () => (datasourceSchemaSnapshot?.tables ?? []).map(table => table.name).filter(Boolean),
    [datasourceSchemaSnapshot],
  )

  const datasourceSchemaTableOptions = useMemo(
    () => datasourceSchemaTableNames.map(tableName => ({ label: tableName, value: tableName })),
    [datasourceSchemaTableNames],
  )

  const datasourceModelingTableNamesValue = useMemo(() => {
    const valid = datasourceModelingTableNames.filter(tableName => datasourceSchemaTableNames.includes(tableName))
    return valid.length >= 2 ? valid : datasourceSchemaTableNames.slice(0, 2)
  }, [datasourceModelingTableNames, datasourceSchemaTableNames])

  const datasourceModelingBaseTableValue = datasourceModelingTableNamesValue.includes(datasourceModelingBaseTableName)
    ? datasourceModelingBaseTableName
    : datasourceModelingTableNamesValue[0] ?? ''

  const datasourceModelingColumnOptions = (tableName: string) => {
    const table = datasourceSchemaSnapshot?.tables?.find(item => item.name === tableName)
    return (table?.columns ?? [])
      .filter(column => !!column.name)
      .sort((left, right) => (left.ordinalPosition ?? 0) - (right.ordinalPosition ?? 0))
      .map(column => ({ label: column.name, value: column.name }))
  }

  const datasourceModelingJoinWithDefaults = (
    draft: Partial<DatasourceModelingJoinDraft>,
    fallbackRightTableName = '',
  ): DatasourceModelingJoinDraft => {
    const leftTableName = datasourceModelingTableNamesValue.includes(draft.leftTableName ?? '')
      ? draft.leftTableName ?? ''
      : datasourceModelingBaseTableValue
    const rightFallback = fallbackRightTableName
      || datasourceModelingTableNamesValue.find(tableName => tableName !== leftTableName)
      || ''
    const rightTableName = datasourceModelingTableNamesValue.includes(draft.rightTableName ?? '')
      && draft.rightTableName !== leftTableName
      ? draft.rightTableName ?? ''
      : rightFallback
    const leftColumnOptions = datasourceModelingColumnOptions(leftTableName)
    const rightColumnOptions = datasourceModelingColumnOptions(rightTableName)
    const commonColumns = leftColumnOptions
      .map(option => option.value)
      .filter(column => rightColumnOptions.some(option => option.value === column))
    const defaultLeftColumn = leftColumnOptions.some(option => option.value === draft.leftColumn)
      ? draft.leftColumn ?? ''
      : commonColumns.find(column => column !== 'tenant_id')
        ?? commonColumns[0]
        ?? leftColumnOptions[0]?.value
        ?? ''
    const defaultRightColumn = rightColumnOptions.some(option => option.value === draft.rightColumn)
      ? draft.rightColumn ?? ''
      : rightColumnOptions.some(option => option.value === defaultLeftColumn)
        ? defaultLeftColumn
        : commonColumns[0] ?? rightColumnOptions[0]?.value ?? ''
    const rawConditions: Array<Partial<DatasourceModelingJoinConditionDraft>> = (draft.conditions ?? []).length > 0
      ? draft.conditions ?? []
      : [{ leftColumn: draft.leftColumn ?? defaultLeftColumn, operator: '=', rightColumn: draft.rightColumn ?? defaultRightColumn }]
    const seenConditions = new Set<string>()
    const conditions: DatasourceModelingJoinConditionDraft[] = []
    rawConditions.forEach(condition => {
      const leftColumn = leftColumnOptions.some(option => option.value === condition.leftColumn)
        ? condition.leftColumn ?? ''
        : ''
      const operator = normalizeDatasourceModelingJoinConditionOperator(condition.operator)
      const rightColumn = rightColumnOptions.some(option => option.value === condition.rightColumn)
        ? condition.rightColumn ?? ''
        : ''
      const connector = condition.connector === 'OR' ? 'OR' : 'AND'
      const key = `${leftColumn}=${rightColumn}`
      if (!leftColumn || !rightColumn || seenConditions.has(key)) return
      seenConditions.add(key)
      conditions.push({
        leftColumn,
        operator,
        connector,
        rightColumn,
        groupStart: condition.groupStart === true,
        groupEnd: condition.groupEnd === true,
      })
    })
    if (conditions.length === 0 && defaultLeftColumn && defaultRightColumn) {
      conditions.push({ leftColumn: defaultLeftColumn, operator: '=', connector: 'AND', rightColumn: defaultRightColumn })
    }
    const firstCondition = conditions[0] ?? { leftColumn: '', operator: '=', rightColumn: '' }
    return {
      joinType: (draft.joinType || 'LEFT').toUpperCase(),
      leftTableName,
      leftColumn: firstCondition.leftColumn,
      rightTableName,
      rightColumn: firstCondition.rightColumn,
      conditions,
    }
  }

  const explicitDatasourceModelingJoins = datasourceModelingJoinDrafts
    .filter(draft => datasourceModelingTableNamesValue.includes(draft.leftTableName)
      && datasourceModelingTableNamesValue.includes(draft.rightTableName)
      && draft.leftTableName !== draft.rightTableName)
    .map(draft => datasourceModelingJoinWithDefaults(draft, draft.rightTableName))
    .filter((join, index, joins) => index === joins.findIndex(item => item.rightTableName === join.rightTableName))
  const explicitDatasourceModelingCoveredTables = new Set<string>()
  explicitDatasourceModelingJoins.forEach(join => {
    explicitDatasourceModelingCoveredTables.add(join.leftTableName)
    explicitDatasourceModelingCoveredTables.add(join.rightTableName)
  })
  const defaultDatasourceModelingTargetTables = datasourceModelingTableNamesValue
    .filter(tableName => tableName !== datasourceModelingBaseTableValue)
    .filter(tableName => !explicitDatasourceModelingCoveredTables.has(tableName))
  const datasourceModelingBridgeTargetTable = explicitDatasourceModelingJoins.length > 0
    && !explicitDatasourceModelingCoveredTables.has(datasourceModelingBaseTableValue)
    ? datasourceModelingTableNamesValue.find(tableName => tableName !== datasourceModelingBaseTableValue)
    : ''
  const defaultDatasourceModelingJoins = Array.from(new Set([
    datasourceModelingBridgeTargetTable || '',
    ...defaultDatasourceModelingTargetTables,
  ]))
    .filter(Boolean)
    .map(tableName => datasourceModelingJoinWithDefaults({
      joinType: 'LEFT',
      leftTableName: datasourceModelingBaseTableValue,
      rightTableName: tableName,
    }, tableName))
  const effectiveDatasourceModelingJoins = [
    ...explicitDatasourceModelingJoins,
    ...defaultDatasourceModelingJoins,
  ]
  const datasourceRelationshipDiagnosticRows = useMemo(
    () => buildDatasourceRelationshipDiagnosticRows({
      baseTableName: datasourceModelingBaseTableValue,
      tableNames: datasourceModelingTableNamesValue,
      joins: effectiveDatasourceModelingJoins,
    }),
    [datasourceModelingBaseTableValue, datasourceModelingTableNamesValue, effectiveDatasourceModelingJoins],
  )
  const selectedDatasourceModelingGraphJoinIndexValue = effectiveDatasourceModelingJoins[selectedDatasourceModelingGraphJoinIndex]
    ? selectedDatasourceModelingGraphJoinIndex
    : effectiveDatasourceModelingJoins.length > 0 ? 0 : -1
  const selectedDatasourceModelingGraphJoin = selectedDatasourceModelingGraphJoinIndexValue >= 0
    ? effectiveDatasourceModelingJoins[selectedDatasourceModelingGraphJoinIndexValue]
    : null
  const datasourceModelingGraphNodesValue = datasourceModelingTableNamesValue.map((tableName, index) => {
    const override = datasourceModelingGraphNodes.find(node => node.tableName === tableName || node.alias === tableName)
    return {
      tableName,
      alias: override?.alias || tableName,
      x: override?.x ?? 80 + (index % 3) * 280,
      y: override?.y ?? 80 + Math.floor(index / 3) * 180,
    }
  })
  const datasourceModelingCoveredTables = new Set<string>([datasourceModelingBaseTableValue])
  effectiveDatasourceModelingJoins.forEach(join => {
    datasourceModelingCoveredTables.add(join.leftTableName)
    datasourceModelingCoveredTables.add(join.rightTableName)
  })
  const datasourceModelingRemainingTableNames = datasourceSchemaTableNames
    .filter(tableName => !datasourceModelingTableNamesValue.includes(tableName))
  const datasourceModelingReady = !!datasourceSchemaSnapshot
    && datasourceModelingTableNamesValue.length >= 2
    && !!datasourceModelingBaseTableValue
    && effectiveDatasourceModelingJoins.length >= datasourceModelingTableNamesValue.length - 1
    && effectiveDatasourceModelingJoins.every(join => join.leftTableName
      && join.rightTableName
      && join.leftTableName !== join.rightTableName
      && join.conditions.length > 0
      && join.conditions.every(condition => condition.leftColumn && condition.rightColumn)
      && datasourceModelingJoinGroupsBalanced(join.conditions))
    && datasourceModelingTableNamesValue.every(tableName => datasourceModelingCoveredTables.has(tableName))

  const sqlDatasetDataSourceConfigId = datasourceSchemaSnapshot?.dataSourceConfigId
    ?? datasourceOnboarding.find(source => source.capabilities?.includes('SQL_DATASET'))?.id
    ?? datasourceOnboarding[0]?.id
    ?? null
  const sqlDatasetParameterDrafts = useMemo(
    () => buildSqlDatasetParameterDrafts(sqlDatasetTemplate, sqlDatasetParameters),
    [sqlDatasetParameters, sqlDatasetTemplate],
  )
  const sqlDatasetDraftResource = useMemo(
    () => buildSqlDatasetDraftResource({
      dataSourceConfigId: sqlDatasetDataSourceConfigId,
      datasetKey: sqlDatasetKey,
      name: sqlDatasetName,
      sqlTemplate: sqlDatasetTemplate,
      tenantColumn: sqlDatasetTenantColumn,
      parameters: sqlDatasetParameterDrafts,
      fields: sqlDatasetFields,
      metrics: sqlDatasetMetrics.map(metric => ({
        ...metric,
        allowedDimensions: (metric.allowedDimensionsText ?? '')
          .split(',')
          .map(value => value.trim())
          .filter(Boolean),
      })),
    }),
    [
      sqlDatasetDataSourceConfigId,
      sqlDatasetFields,
      sqlDatasetKey,
      sqlDatasetMetrics,
      sqlDatasetName,
      sqlDatasetParameterDrafts,
      sqlDatasetTemplate,
      sqlDatasetTenantColumn,
    ],
  )
  const sqlDatasetReady = !!sqlDatasetKey.trim()
    && !!sqlDatasetName.trim()
    && !!sqlDatasetTemplate.trim()
    && sqlDatasetParameterDrafts.every(parameter => !!parameter.key)
    && sqlDatasetDraftResource.fields.length > 0
    && sqlDatasetDraftResource.metrics.length > 0
  const sqlDatasetReadinessRows = useMemo(
    () => buildSqlDatasetReadinessRows({
      draft: sqlDatasetDraftResource,
      parameters: sqlDatasetParameterDrafts,
      preview: sqlDatasetPreview,
    }),
    [sqlDatasetDraftResource, sqlDatasetParameterDrafts, sqlDatasetPreview],
  )

  const updateSqlDatasetParameter = (parameterKey: string, patch: Partial<BiSqlDatasetParameterDraftLike>) => {
    setSqlDatasetParameters(current => buildSqlDatasetParameterDrafts(sqlDatasetTemplate, current)
      .map(parameter => parameter.key === parameterKey ? { ...parameter, ...patch } : parameter))
  }

  const updateSqlDatasetField = (index: number, patch: Partial<BiSqlDatasetFieldDraftLike>) => {
    setSqlDatasetFields(current => current.map((field, fieldIndex) => (
      fieldIndex === index ? { ...field, ...patch } : field
    )))
  }

  const addSqlDatasetField = () => {
    setSqlDatasetFields(current => [
      ...current,
      {
        fieldKey: `field_${current.length + 1}`,
        displayName: `Field ${current.length + 1}`,
        columnExpression: `field_${current.length + 1}`,
        role: 'DIMENSION',
        dataType: 'STRING',
        visible: true,
        sensitiveLevel: 'NORMAL',
      },
    ])
  }

  const removeSqlDatasetField = (index: number) => {
    setSqlDatasetFields(current => current.length <= 1 ? current : current.filter((_, fieldIndex) => fieldIndex !== index))
  }

  const updateSqlDatasetMetric = (index: number, patch: Partial<SqlDatasetMetricDraft>) => {
    setSqlDatasetMetrics(current => current.map((metric, metricIndex) => (
      metricIndex === index ? { ...metric, ...patch } : metric
    )))
  }

  const addSqlDatasetMetric = () => {
    setSqlDatasetMetrics(current => [
      ...current,
      {
        metricKey: `metric_${current.length + 1}`,
        displayName: `Metric ${current.length + 1}`,
        expression: `SUM(metric_${current.length + 1})`,
        aggregation: 'SUM',
        dataType: 'NUMBER',
        allowedDimensionsText: sqlDatasetFields
          .filter(field => field.role !== 'MEASURE')
          .map(field => field.fieldKey || field.columnExpression)
          .filter(Boolean)
          .join(','),
        status: 'ACTIVE',
      },
    ])
  }

  const removeSqlDatasetMetric = (index: number) => {
    setSqlDatasetMetrics(current => current.length <= 1 ? current : current.filter((_, metricIndex) => metricIndex !== index))
  }

  const sqlDatasetPreviewParameters = () => sqlDatasetParameterDrafts.reduce<Record<string, string>>((parameters, parameter) => {
    if (parameter.defaultValue) {
      parameters[parameter.key] = parameter.defaultValue
    }
    return parameters
  }, {})

  const addDatasourceModelingJoin = () => {
    const nextTableName = datasourceModelingRemainingTableNames[0]
    if (!nextTableName) return
    setDatasourceModelingTableNames(Array.from(new Set([
      ...datasourceModelingTableNamesValue,
      nextTableName,
    ])))
  }

  const updateDatasourceModelingJoin = (index: number, patch: Partial<DatasourceModelingJoinDraft>) => {
    setDatasourceModelingJoinDrafts(effectiveDatasourceModelingJoins.map((join, itemIndex) => {
      if (itemIndex !== index) return join
      const nextJoin = { ...join, ...patch }
      if (patch.leftTableName && patch.leftTableName !== join.leftTableName) {
        nextJoin.leftColumn = ''
        nextJoin.conditions = []
      }
      if (patch.rightTableName && patch.rightTableName !== join.rightTableName) {
        nextJoin.rightColumn = ''
        nextJoin.conditions = []
      }
      if (nextJoin.leftTableName === nextJoin.rightTableName) {
        nextJoin.rightTableName = datasourceModelingTableNamesValue.find(tableName => tableName !== nextJoin.leftTableName) ?? ''
        nextJoin.rightColumn = ''
        nextJoin.conditions = []
      }
      return datasourceModelingJoinWithDefaults(nextJoin, nextJoin.rightTableName)
    }))
  }

  const addDatasourceModelingJoinCondition = (joinIndex: number) => {
    setDatasourceModelingJoinDrafts(effectiveDatasourceModelingJoins.map((join, itemIndex) => {
      if (itemIndex !== joinIndex) return join
      const leftColumnOptions = datasourceModelingColumnOptions(join.leftTableName)
      const rightColumnOptions = datasourceModelingColumnOptions(join.rightTableName)
      const used = new Set(join.conditions.map(condition => `${condition.leftColumn}=${condition.rightColumn}`))
      const commonColumn = leftColumnOptions
        .map(option => option.value)
        .find(column => rightColumnOptions.some(option => option.value === column) && !used.has(`${column}=${column}`))
      const leftColumn = commonColumn ?? leftColumnOptions.find(option => !join.conditions.some(condition => condition.leftColumn === option.value))?.value ?? leftColumnOptions[0]?.value ?? ''
      const rightColumn = rightColumnOptions.some(option => option.value === leftColumn)
        ? leftColumn
        : rightColumnOptions.find(option => !join.conditions.some(condition => condition.rightColumn === option.value))?.value ?? rightColumnOptions[0]?.value ?? ''
      return datasourceModelingJoinWithDefaults({
        ...join,
        conditions: [...join.conditions, { leftColumn, operator: '=' as const, connector: 'AND' as const, rightColumn }],
      }, join.rightTableName)
    }))
  }

  const addSelectedDatasourceModelingGraphJoinCondition = () => {
    if (selectedDatasourceModelingGraphJoinIndexValue < 0) return
    addDatasourceModelingJoinCondition(selectedDatasourceModelingGraphJoinIndexValue)
  }

  const removeSelectedDatasourceModelingGraphJoinCondition = (conditionIndex: number) => {
    if (selectedDatasourceModelingGraphJoinIndexValue < 0) return
    removeDatasourceModelingJoinCondition(selectedDatasourceModelingGraphJoinIndexValue, conditionIndex)
  }

  const addSelectedDatasourceModelingGraphJoinCommonConditions = () => {
    if (selectedDatasourceModelingGraphJoinIndexValue < 0) return
    setDatasourceModelingJoinDrafts(effectiveDatasourceModelingJoins.map((join, itemIndex) => {
      if (itemIndex !== selectedDatasourceModelingGraphJoinIndexValue) return join
      const leftColumnOptions = datasourceModelingColumnOptions(join.leftTableName)
      const rightColumnOptions = datasourceModelingColumnOptions(join.rightTableName)
      const rightColumns = new Set(rightColumnOptions.map(option => option.value))
      const used = new Set(join.conditions.map(condition => `${condition.leftColumn}=${condition.rightColumn}`))
      const commonConditions: DatasourceModelingJoinConditionDraft[] = leftColumnOptions
        .map(option => option.value)
        .filter(column => rightColumns.has(column))
        .map(column => ({ leftColumn: column, operator: '=' as const, connector: 'AND' as const, rightColumn: column }))
        .filter(condition => !used.has(`${condition.leftColumn}=${condition.rightColumn}`))
      if (commonConditions.length === 0) return join
      return datasourceModelingJoinWithDefaults({
        ...join,
        conditions: [...join.conditions, ...commonConditions],
      }, join.rightTableName)
    }))
  }

  const swapSelectedDatasourceModelingGraphJoinDirection = () => {
    if (selectedDatasourceModelingGraphJoinIndexValue < 0) return
    setDatasourceModelingJoinDrafts(effectiveDatasourceModelingJoins.map((join, itemIndex) => {
      if (itemIndex !== selectedDatasourceModelingGraphJoinIndexValue) return join
      return datasourceModelingJoinWithDefaults({
        ...join,
        leftTableName: join.rightTableName,
        leftColumn: join.rightColumn,
        rightTableName: join.leftTableName,
        rightColumn: join.leftColumn,
        conditions: join.conditions.map(condition => ({
          leftColumn: condition.rightColumn,
          operator: swapDatasourceModelingJoinConditionOperator(condition.operator),
          connector: condition.connector,
          rightColumn: condition.leftColumn,
          groupStart: condition.groupStart,
          groupEnd: condition.groupEnd,
        })),
      }, join.leftTableName)
    }))
  }

  const updateDatasourceModelingJoinCondition = (
    joinIndex: number,
    conditionIndex: number,
    patch: Partial<DatasourceModelingJoinConditionDraft>,
  ) => {
    setDatasourceModelingJoinDrafts(effectiveDatasourceModelingJoins.map((join, itemIndex) => {
      if (itemIndex !== joinIndex) return join
      return datasourceModelingJoinWithDefaults({
        ...join,
        conditions: join.conditions.map((condition, itemConditionIndex) => (
          itemConditionIndex === conditionIndex ? { ...condition, ...patch } : condition
        )),
      }, join.rightTableName)
    }))
  }

  const removeDatasourceModelingJoinCondition = (joinIndex: number, conditionIndex: number) => {
    setDatasourceModelingJoinDrafts(effectiveDatasourceModelingJoins.map((join, itemIndex) => {
      if (itemIndex !== joinIndex || join.conditions.length <= 1) return join
      return datasourceModelingJoinWithDefaults({
        ...join,
        conditions: join.conditions.filter((_, itemConditionIndex) => itemConditionIndex !== conditionIndex),
      }, join.rightTableName)
    }))
  }

  const removeDatasourceModelingJoin = (index: number) => {
    const removedJoin = effectiveDatasourceModelingJoins[index]
    if (!removedJoin || effectiveDatasourceModelingJoins.length <= 1) return
    setDatasourceModelingJoinDrafts(effectiveDatasourceModelingJoins.filter((_, itemIndex) => itemIndex !== index))
    if (removedJoin.rightTableName !== datasourceModelingBaseTableValue) {
      setDatasourceModelingTableNames(datasourceModelingTableNamesValue.filter(tableName => tableName !== removedJoin.rightTableName))
    }
  }

  const startDatasourceModelingGraphDrag = (
    event: ReactPointerEvent<HTMLButtonElement>,
    node: DatasourceModelingGraphNode,
  ) => {
    event.preventDefault()
    datasourceModelingGraphDragRef.current = {
      tableName: node.tableName,
      alias: node.alias,
      startClientX: event.clientX,
      startClientY: event.clientY,
      originX: node.x,
      originY: node.y,
    }
  }

  const moveDatasourceModelingGraphDrag = (event: ReactPointerEvent<HTMLDivElement>) => {
    const drag = datasourceModelingGraphDragRef.current
    if (!drag) return
    const nextNode: DatasourceModelingGraphNode = {
      tableName: drag.tableName,
      alias: drag.alias,
      x: clampDatasourceModelingGraphCoordinate(
        drag.originX + event.clientX - drag.startClientX,
        DATASOURCE_MODELING_GRAPH_WIDTH - DATASOURCE_MODELING_GRAPH_NODE_WIDTH,
      ),
      y: clampDatasourceModelingGraphCoordinate(
        drag.originY + event.clientY - drag.startClientY,
        DATASOURCE_MODELING_GRAPH_HEIGHT - DATASOURCE_MODELING_GRAPH_NODE_HEIGHT,
      ),
    }
    setDatasourceModelingGraphNodes(current => {
      let found = false
      const next = current.map(node => {
        if (node.tableName !== drag.tableName && node.alias !== drag.alias) return node
        found = true
        return nextNode
      })
      return found ? next : [...current, nextNode]
    })
  }

  const stopDatasourceModelingGraphDrag = () => {
    datasourceModelingGraphDragRef.current = null
  }

  const runDatasourceConnectionTest = (id?: number | null) => {
    if (!id) return
    setTestingDatasourceId(id)
    biApi.testDatasourceConnection(id)
      .then(response => setDatasourceConnectionTestResult(response.data ?? null))
      .catch(error => {
        setDatasourceConnectionTestResult({
          id,
          sourceKey: `jdbc-${id}`,
          connectorType: '-',
          success: false,
          message: error?.message || 'connection failed',
          databaseProductName: null,
          databaseProductVersion: null,
          checkedAt: new Date().toISOString(),
          durationMs: 0,
        })
      })
      .finally(() => setTestingDatasourceId(null))
  }

  const previewDatasourceSchema = (id?: number | null) => {
    if (!id) return
    setPreviewingDatasourceId(id)
    biApi.previewDatasourceSchema(id, 100)
      .then(response => setDatasourceSchemaPreview(response.data ?? null))
      .catch(() => setDatasourceSchemaPreview(null))
      .finally(() => setPreviewingDatasourceId(null))
  }

  const datasourceApiPreviewVariables = () => {
    return datasourceApiPreviewVariableDrafts.reduce<Record<string, string>>((variables, draft) => {
      const variableName = draft.name.trim()
      if (variableName) {
        variables[variableName] = draft.value.trim()
      }
      return variables
    }, {})
  }

  const datasourceApiPreviewRequest = () => {
    const requestedLimit = Number.parseInt(datasourceApiPreviewLimit, 10)
    const limit = Number.isFinite(requestedLimit) ? Math.min(1000, Math.max(1, requestedLimit)) : 50
    return {
      variables: datasourceApiPreviewVariables(),
      limit,
    }
  }

  const datasourceIsApi = (id?: number | null) => {
    const source = datasourceOnboarding.find(item => item.id === id)
    return (source?.connectorType || '').toUpperCase() === 'API'
      || (source?.sourceKey || '').toLowerCase().startsWith('api-')
  }

  const updateDatasourceApiPreviewVariable = (index: number, patch: Partial<ApiPreviewVariableDraft>) => {
    setDatasourceApiPreviewVariableDrafts(current => current.map((draft, draftIndex) => (
      draftIndex === index ? { ...draft, ...patch } : draft
    )))
  }

  const addDatasourceApiPreviewVariable = () => {
    setDatasourceApiPreviewVariableDrafts(current => (
      current.length >= MAX_API_PREVIEW_VARIABLES
        ? current
        : [...current, { name: '', value: '' }]
    ))
  }

  const removeDatasourceApiPreviewVariable = (index: number) => {
    setDatasourceApiPreviewVariableDrafts(current => {
      if (current.length <= 1) return [{ name: '', value: '' }]
      return current.filter((_, draftIndex) => draftIndex !== index)
    })
  }

  const previewApiDatasource = (id?: number | null) => {
    if (!id) return
    setPreviewingApiDatasourceId(id)
    biApi.previewApiDatasource(id, datasourceApiPreviewRequest())
      .then(response => setDatasourceApiPreview(response.data ?? null))
      .catch(() => setDatasourceApiPreview(null))
      .finally(() => setPreviewingApiDatasourceId(null))
  }

  const syncDatasourceSchema = (id?: number | null) => {
    if (!id) return
    setSyncingDatasourceId(id)
    biApi.syncDatasourceSchema(id, 100, datasourceIsApi(id) ? datasourceApiPreviewRequest() : undefined)
      .then(response => {
        const snapshot = response.data ?? null
        setDatasourceSchemaSnapshot(snapshot)
        return Promise.all([
          biApi.listDatasourceSchemaSnapshots(id, 20)
            .then(historyResponse => setDatasourceSchemaSnapshots(historyResponse.data ?? (snapshot ? [snapshot] : [])))
            .catch(() => setDatasourceSchemaSnapshots(snapshot ? [snapshot] : [])),
          biApi.listDatasourceOnboarding()
            .then(onboardingResponse => setDatasourceOnboarding(onboardingResponse.data ?? []))
            .catch(() => undefined),
        ])
      })
      .catch(error => {
        const failedSnapshot: BiDatasourceSchemaSnapshotView = {
          id: null,
          dataSourceConfigId: id,
          sourceKey: `jdbc-${id}`,
          name: `jdbc-${id}`,
          connectorType: '-',
          syncStatus: 'FAILED',
          errorMessage: error?.message || 'schema sync failed',
          tableCount: 0,
          columnCount: 0,
          tables: [],
          syncedAt: new Date().toISOString(),
          syncedBy: '-',
        }
        setDatasourceSchemaSnapshot(failedSnapshot)
        setDatasourceSchemaSnapshots([failedSnapshot])
      })
      .finally(() => setSyncingDatasourceId(null))
  }

  const updateDatasourceDraft = (patch: Partial<BiDatasourceOnboardingDraftInputLike>) => {
    setDatasourceDraft(current => ({ ...current, ...patch }))
  }

  const updateDatasourceUploadFile = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null
    setDatasourceUploadFile(file)
    if (!file) return
    const extension = file.name.split('.').pop()?.trim().toUpperCase()
    updateDatasourceDraft({
      fileName: file.name,
      fileType: extension === 'XLS' || extension === 'XLSX' ? extension : 'CSV',
      url: '',
    })
  }

  const saveDatasourceOnboarding = () => {
    if (datasourceDraftMissingRequiredFields) return
    const command = datasourceWizardReviewCommand
    setSavingDatasourceOnboarding(true)
    if (!editingDatasourceId && isFileDatasourceConnector && datasourceUploadFile) {
      biApi.uploadAndMaterializeDatasourceFile(datasourceUploadFile, {
        name: command.name,
        description: command.description,
        sheetName: datasourceDraft.fileSheetName,
        delimiter: datasourceDraft.fileDelimiter || ',',
        headerRow: datasourceDraft.fileHeaderRow !== false,
        encoding: datasourceDraft.fileEncoding || 'UTF-8',
        tenantColumn: 'tenant_id',
        schemaLimit: 200,
        maxRows: 100000,
      })
        .then(response => {
          const result = response.data ?? null
          if (result?.source) {
            setDatasourceOnboarding(current => [
              result.source,
              ...current.filter(source => source.id !== result.source.id),
            ])
          }
          if (result?.schemaSnapshot) {
            setDatasourceSchemaSnapshot(result.schemaSnapshot)
            setDatasourceSchemaSnapshots(current => [
              result.schemaSnapshot,
              ...current.filter(snapshot => snapshot.id !== result.schemaSnapshot.id),
            ])
          }
          if (result?.dataset) {
            setSelectedDatasetKey(result.dataset.datasetKey)
            setDatasetResources(current => [
              result.dataset,
              ...current.filter(item => item.datasetKey !== result.dataset.datasetKey),
            ])
          }
          if (result?.accelerationPolicy) {
            applyDatasetAccelerationPolicy(result.accelerationPolicy)
          }
          return biApi.listDatasourceOnboarding()
            .then(onboardingResponse => {
              const listed = onboardingResponse.data ?? []
              const source = result?.source ?? null
              setDatasourceOnboarding(source && !listed.some(item => item.id === source.id)
                ? [source, ...listed]
                : listed)
            })
            .catch(() => undefined)
        })
        .then(() => {
          setDatasourceDraft(defaultDatasourceDraft(command.connectorType))
          setDatasourceUploadFile(null)
          setEditingDatasourceId(null)
          setDatasourceWizardStep(0)
        })
        .catch(() => undefined)
        .finally(() => setSavingDatasourceOnboarding(false))
      return
    }
    const request = editingDatasourceId
      ? biApi.updateDatasourceOnboarding(editingDatasourceId, command)
      : biApi.createDatasourceOnboarding(command)
    request
      .then(response => biApi.listDatasourceOnboarding()
        .then(onboardingResponse => setDatasourceOnboarding(onboardingResponse.data ?? (response.data ? [response.data] : [])))
        .catch(() => {
          if (response.data) setDatasourceOnboarding(current => [response.data, ...current.filter(source => source.id !== response.data?.id)])
        }))
      .then(() => {
        setDatasourceDraft(defaultDatasourceDraft(command.connectorType))
        setDatasourceUploadFile(null)
        setEditingDatasourceId(null)
        setDatasourceWizardStep(0)
      })
      .catch(() => undefined)
      .finally(() => setSavingDatasourceOnboarding(false))
  }

  const editDatasourceOnboarding = (id?: number | null) => {
    if (!id) return
    const source = datasourceOnboarding.find(item => item.id === id)
    if (!source) return
    setEditingDatasourceId(id)
    setDatasourceWizardStep(1)
    setDatasourceDraft({
      connectorType: source.connectorType || 'MYSQL',
      name: source.name || '',
      url: '',
      username: '',
      password: '',
      driverClassName: source.driverClassName || '',
      description: '',
      enabled: source.enabled !== false,
      connectionMode: source.connectionMode || 'DIRECT_QUERY',
      fileName: '',
      fileType: 'CSV',
      fileSheetName: '',
      fileDelimiter: ',',
      fileHeaderRow: true,
      fileEncoding: 'UTF-8',
    })
  }

  const cancelDatasourceEdit = () => {
    setEditingDatasourceId(null)
    setDatasourceWizardStep(0)
    setDatasourceDraft(defaultDatasourceDraft(datasourceDraft.connectorType || 'MYSQL'))
    setDatasourceUploadFile(null)
  }

  const openDatasourceCredentialRotation = (id?: number | null) => {
    if (!id) return
    setCredentialRotationDatasourceId(id)
    setCredentialRotationPassword('')
  }

  const cancelDatasourceCredentialRotation = () => {
    if (rotatingDatasourceId !== null) return
    setCredentialRotationDatasourceId(null)
    setCredentialRotationPassword('')
  }

  const rotateDatasourceCredential = () => {
    const id = credentialRotationDatasourceId
    if (!id || !credentialRotationPassword.trim()) return
    setRotatingDatasourceId(id)
    biApi.rotateDatasourceCredential(id, { password: credentialRotationPassword })
      .then(() => biApi.listDatasourceOnboarding()
        .then(onboardingResponse => setDatasourceOnboarding(onboardingResponse.data ?? []))
        .catch(() => undefined))
      .then(() => {
        setCredentialRotationDatasourceId(null)
        setCredentialRotationPassword('')
      })
      .catch(() => undefined)
      .finally(() => setRotatingDatasourceId(null))
  }

  const createDatasetFromDatasourceTable = (tableName?: string | null) => {
    if (!tableName || !datasourceSchemaSnapshot) return
    setCreatingDatasourceDatasetTable(tableName)
    const apiSnapshot = (datasourceSchemaSnapshot.connectorType || '').toUpperCase() === 'API'
      || (datasourceSchemaSnapshot.sourceKey || '').toLowerCase().startsWith('api-')
    const command = buildDatasourceTableDatasetCommand(
      datasourceSchemaSnapshot,
      tableName,
      apiSnapshot ? datasourceApiPreviewVariables() : {},
    )
    biApi.createDatasetFromDatasourceSchema(command)
      .then(response => {
        const resource = response.data ?? null
        if (resource) {
          setSelectedDatasetKey(resource.datasetKey)
          setDatasetResources(current => [
            resource,
            ...current.filter(item => item.datasetKey !== resource.datasetKey),
          ])
        }
        setLoadingDatasetResources(true)
        return biApi.listDatasetResources()
          .then(resourcesResponse => {
            const listedResources = resourcesResponse.data ?? []
            const resources = resource && !listedResources.some(item => item.datasetKey === resource.datasetKey)
              ? [resource, ...listedResources]
              : listedResources
            setDatasetResources(resources)
            setSelectedDatasetKey(current => resource?.datasetKey ?? current ?? resources[0]?.datasetKey ?? null)
          })
          .catch(() => undefined)
          .finally(() => setLoadingDatasetResources(false))
      })
      .catch(() => undefined)
      .finally(() => setCreatingDatasourceDatasetTable(null))
  }

  const createMultiTableDatasetFromDatasourceSchema = () => {
    if (!datasourceSchemaSnapshot || datasourceSchemaSnapshot.syncStatus !== 'SUCCESS' || !datasourceModelingReady) return
    setCreatingDatasourceMultiTableDataset(true)
    const command = buildDatasourceMultiTableDatasetCommand(datasourceSchemaSnapshot, {
      baseTableName: datasourceModelingBaseTableValue,
      tableNames: datasourceModelingTableNamesValue,
      tenantColumn: 'tenant_id',
      joins: effectiveDatasourceModelingJoins,
      graphNodes: datasourceModelingGraphNodesValue,
    })
    biApi.createMultiTableDatasetFromDatasourceSchema(command)
      .then(response => {
        const resource = response.data ?? null
        if (resource) {
          setSelectedDatasetKey(resource.datasetKey)
          setDatasetResources(current => [
            resource,
            ...current.filter(item => item.datasetKey !== resource.datasetKey),
          ])
        }
        setLoadingDatasetResources(true)
        return biApi.listDatasetResources()
          .then(resourcesResponse => {
            const listedResources = resourcesResponse.data ?? []
            const resources = resource && !listedResources.some(item => item.datasetKey === resource.datasetKey)
              ? [resource, ...listedResources]
              : listedResources
            setDatasetResources(resources)
            setSelectedDatasetKey(current => resource?.datasetKey ?? current ?? resources[0]?.datasetKey ?? null)
          })
          .catch(() => undefined)
          .finally(() => setLoadingDatasetResources(false))
      })
      .catch(() => undefined)
      .finally(() => setCreatingDatasourceMultiTableDataset(false))
  }

  const saveSqlDatasetDraft = () => {
    if (!sqlDatasetReady) return
    const resource = sqlDatasetDraftResource as BiDatasetResource
    setSavingSqlDatasetDraft(true)
    biApi.saveDatasetDraft(resource.datasetKey, resource)
      .then(response => {
        const savedResource = response.data ?? resource
        setSelectedDatasetKey(savedResource.datasetKey)
        setDatasetResources(current => [
          savedResource,
          ...current.filter(item => item.datasetKey !== savedResource.datasetKey),
        ])
        setLoadingDatasetResources(true)
        return biApi.listDatasetResources()
          .then(resourcesResponse => {
            const listedResources = resourcesResponse.data ?? []
            const resources = listedResources.some(item => item.datasetKey === savedResource.datasetKey)
              ? listedResources
              : [savedResource, ...listedResources]
            setDatasetResources(resources)
            setSelectedDatasetKey(savedResource.datasetKey)
          })
          .catch(() => undefined)
          .finally(() => setLoadingDatasetResources(false))
      })
      .catch(() => undefined)
      .finally(() => setSavingSqlDatasetDraft(false))
  }

  const previewSqlDataset = () => {
    if (!sqlDatasetReady) return
    const resource = sqlDatasetDraftResource as BiDatasetResource
    setPreviewingSqlDataset(true)
    setSqlDatasetPreviewError('')
    biApi.previewSqlDataset({
      resource,
      sqlParameters: sqlDatasetPreviewParameters(),
      limit: 20,
      executeSample: true,
    })
      .then(response => setSqlDatasetPreview(response.data ?? null))
      .catch(error => {
        setSqlDatasetPreview(null)
        setSqlDatasetPreviewError(error?.message || 'SQL 预览失败')
      })
      .finally(() => setPreviewingSqlDataset(false))
  }

  const saveQueryGovernancePolicy = () => {
    setSavingQueryGovernancePolicy(true)
    biApi.updateQueryGovernancePolicy({
      defaultTimeoutMs: optionalNumber(queryPolicyTimeoutMs) ?? 30000,
      defaultQuotaRows: optionalNumber(queryPolicyQuotaRows) ?? 1000000,
      datasets: queryGovernancePolicy?.datasets ?? [],
    })
      .then(response => {
        setQueryGovernancePolicy(response.data ?? null)
        if (response.data) {
          setQueryPolicyTimeoutMs(String(response.data.defaultTimeoutMs))
          setQueryPolicyQuotaRows(String(response.data.defaultQuotaRows))
        }
      })
      .finally(() => setSavingQueryGovernancePolicy(false))
  }

  const saveQueryCachePolicy = () => {
    setSavingQueryCachePolicy(true)
    biApi.updateQueryCachePolicy(buildQueryCachePolicyCommand(
      queryCachePolicy,
      {
        enabled: queryCacheDefaultEnabled,
        ttlSeconds: optionalNumber(queryCacheTtlSeconds) ?? 300,
        cacheMode: queryCacheDefaultEnabled ? 'CACHE' : 'DIRECT_QUERY',
      },
      {
        resourceType: queryCacheResourceScope,
        resourceKey: queryCacheResourceKey,
        enabled: queryCacheResourceEnabled,
        ttlSeconds: optionalNumber(queryCacheResourceTtlSeconds) ?? optionalNumber(queryCacheTtlSeconds) ?? 300,
        cacheMode: queryCacheResourceCacheMode,
      },
    ))
      .then(response => {
        setQueryCachePolicy(response.data ?? null)
        if (response.data) {
          setQueryCacheDefaultEnabled(response.data.defaultEnabled)
          setQueryCacheTtlSeconds(String(response.data.defaultTtlSeconds))
        }
      })
      .finally(() => setSavingQueryCachePolicy(false))
  }

  const reloadQuickEngineCapacity = () =>
    biApi.getQuickEngineCapacity(20)
      .then(response => applyQuickEngineCapacity(response.data ?? null))
      .catch(() => applyQuickEngineCapacity(null))

  const saveQuickEngineCapacityPolicy = () => {
    setSavingQuickEngineCapacityPolicy(true)
    biApi.upsertQuickEngineCapacityAlertPolicy(buildQuickEngineCapacityAlertPolicyCommand({
      enabled: quickEngineAlertEnabled,
      capacityLimitRows: optionalNumber(quickEngineCapacityLimitRows) ?? 1000000,
      warningThresholdPercent: optionalNumber(quickEngineWarningThreshold) ?? 80,
      criticalThresholdPercent: optionalNumber(quickEngineCriticalThreshold) ?? 95,
      notificationChannels: quickEngineNotificationChannels,
      notificationReceivers: quickEngineNotificationReceivers,
    }))
      .then(() => reloadQuickEngineCapacity())
      .finally(() => setSavingQuickEngineCapacityPolicy(false))
  }

  const saveQuickEngineTenantPoolPolicy = () => {
    setSavingQuickEngineTenantPoolPolicy(true)
    biApi.upsertQuickEngineTenantPoolPolicy(buildQuickEngineTenantPoolPolicyCommand({
      poolKey: quickEnginePoolKey,
      maxConcurrentQueries: optionalNumber(quickEngineMaxConcurrentQueries) ?? 8,
      queueLimit: optionalNumber(quickEngineQueueLimit) ?? 50,
      queueTimeoutSeconds: optionalNumber(quickEngineQueueTimeoutSeconds) ?? 120,
      poolWeight: optionalNumber(quickEnginePoolWeight) ?? 100,
    }))
      .then(() => reloadQuickEngineCapacity())
      .finally(() => setSavingQuickEngineTenantPoolPolicy(false))
  }

  const invalidateQueryCache = (command: BiQueryCacheInvalidationCommand) => {
    setInvalidatingQueryCache(true)
    biApi.invalidateQueryCache(command)
      .then(response => setQueryCacheInvalidationResult(response.data ?? null))
      .finally(() => setInvalidatingQueryCache(false))
  }

  const saveDatasetAccelerationPolicy = () => {
    const datasetKey = selectedDatasetResource?.datasetKey ?? dashboardPreset.datasetKey
    if (!datasetKey) return
    setSavingDatasetAccelerationPolicy(true)
    biApi.updateDatasetAccelerationPolicy(datasetKey, {
      enabled: datasetAccelerationEnabled,
      accelerationMode: datasetAccelerationMode,
      refreshMode: datasetAccelerationRefreshMode,
      refreshIntervalMinutes: optionalNumber(datasetAccelerationIntervalMinutes) ?? 60,
      ttlSeconds: optionalNumber(datasetAccelerationTtlSeconds) ?? 300,
      maxRows: optionalNumber(datasetAccelerationMaxRows) ?? 100000,
      cronExpression: datasetAccelerationCronExpression.trim() || null,
    })
      .then(response => applyDatasetAccelerationPolicy(response.data ?? null))
      .finally(() => setSavingDatasetAccelerationPolicy(false))
  }

  const refreshSelectedDatasetAcceleration = () => {
    const datasetKey = selectedDatasetResource?.datasetKey ?? dashboardPreset.datasetKey
    if (!datasetKey) return
    setRefreshingDatasetAcceleration(true)
    biApi.refreshDatasetAcceleration(datasetKey)
      .then(response => {
        const run = response.data
        if (run) {
          setDatasetAccelerationPolicy(current => current
            ? {
              ...current,
              lastStatus: run.status,
              lastRunId: run.id ?? current.lastRunId,
              lastRefreshedAt: run.finishedAt ?? current.lastRefreshedAt,
              materializedTable: run.materializedTable ?? current.materializedTable,
              recentRuns: [run, ...(current.recentRuns ?? [])],
            }
            : current)
        }
        return reloadDatasetAccelerationPolicy(datasetKey)
      })
      .catch(() => undefined)
      .finally(() => setRefreshingDatasetAcceleration(false))
  }

  const runDatasetAccelerationScheduler = () => {
    const datasetKey = selectedDatasetResource?.datasetKey ?? dashboardPreset.datasetKey
    setRunningDatasetAccelerationScheduler(true)
    biApi.runDatasetAccelerationScheduler()
      .then(response => {
        setDatasetAccelerationSchedulerResult(response.data ?? null)
        return reloadDatasetAccelerationPolicy(datasetKey)
      })
      .catch(() => setDatasetAccelerationSchedulerResult(null))
      .finally(() => setRunningDatasetAccelerationScheduler(false))
  }

  const cancelQuery = (row: BiQueryHistoryItem) => {
    if (!row.sqlHash) return
    setCancellingQueryHash(row.sqlHash)
    biApi.cancelQuery(row.sqlHash)
      .then(response => setQueryCancellationResult(response.data ?? null))
      .catch(() => setQueryCancellationResult({
        sqlHash: row.sqlHash,
        cancelled: false,
        message: 'cancel request failed',
      }))
      .finally(() => setCancellingQueryHash(null))
  }

  const queryHistoryTableColumns = useMemo<ColumnsType<BiQueryHistoryItem>>(
    () => [
      ...queryHistoryColumns,
      {
        title: '操作',
        width: 116,
        render: (_, row) => (
          <Space size={4}>
            <Tooltip title="查看查询详情">
              <Button
                size="small"
                icon={<EyeOutlined />}
                onClick={() => inspectQueryHistory(row)}
              />
            </Tooltip>
            <Tooltip title="取消运行中查询">
              <Button
                size="small"
                icon={<StopOutlined />}
                loading={cancellingQueryHash === row.sqlHash}
                disabled={!row.sqlHash}
                onClick={() => cancelQuery(row)}
              />
            </Tooltip>
          </Space>
        ),
      },
    ],
    [cancellingQueryHash],
  )

  const cleanupExports = () => {
    setCleaningExports(true)
    biApi.cleanupExports(100)
      .then(response => {
        setExportCleanupResult(response.data ?? null)
        return reloadExports()
      })
      .finally(() => setCleaningExports(false))
  }

  const retryExports = () => {
    setRetryingExports(true)
    biApi.retryExports(10)
      .then(response => {
        setExportRetryResult(response.data ?? null)
        return reloadExports()
      })
      .finally(() => setRetryingExports(false))
  }

  const createDashboardSubscription = () => {
    setSavingSubscription('subscription')
    biApi.upsertSubscription({
      subscriptionKey: `${dashboardPreset.dashboardKey}-daily`,
      name: `${dashboardPreset.title} 日报`,
      resourceType: 'DASHBOARD',
      resourceKey: dashboardPreset.dashboardKey,
      schedule: { frequency: 'DAILY', time: '09:00', timezone: 'Asia/Shanghai' },
      receivers: { channels: ['EMAIL', 'LARK'], users: ['CURRENT_USER'] },
      delivery: { content: 'SNAPSHOT_LINK', attachments: ['PDF', 'CSV'], includeFilters: true },
      enabled: true,
    })
      .then(reloadSubscriptions)
      .finally(() => setSavingSubscription(null))
  }

  const createMetricAlert = () => {
    const metricKey = selectedWidget.metrics[0] ?? 'success_rate'
    setSavingSubscription('alert')
    biApi.upsertAlert({
      alertKey: `${dashboardPreset.dashboardKey}-${metricKey}-threshold`,
      name: `${selectedWidget.title} 阈值告警`,
      datasetKey: dashboardPreset.datasetKey,
      metricKey,
      condition: { operator: metricKey === 'success_rate' ? 'LT' : 'GT', threshold: metricKey === 'success_rate' ? 0.9 : 10000, window: 'LAST_1_DAY' },
      receivers: { channels: ['LARK', 'WEBHOOK'], users: ['CURRENT_USER'] },
      enabled: true,
    })
      .then(reloadSubscriptions)
      .finally(() => setSavingSubscription(null))
  }

  const createAnomalyAlert = () => {
    const metricKey = selectedWidget.metrics[0] ?? 'success_rate'
    const dropMetric = metricKey === 'success_rate' || metricKey.includes('rate')
    setSavingSubscription('anomaly')
    biApi.upsertAlert({
      alertKey: `${dashboardPreset.dashboardKey}-${metricKey}-anomaly`,
      name: `${selectedWidget.title} 异常检测`,
      datasetKey: dashboardPreset.datasetKey,
      metricKey,
      condition: {
        operator: dropMetric ? 'ANOMALY_DROP' : 'ANOMALY_RISE',
        baselineWindow: 7,
        minSamples: 3,
        sensitivity: 2,
        minDeltaPercent: 0.05,
      },
      receivers: { channels: ['LARK', 'WEBHOOK'], users: ['CURRENT_USER'] },
      enabled: true,
    })
      .then(reloadSubscriptions)
      .finally(() => setSavingSubscription(null))
  }

  const runLatestSubscription = () => {
    const subscription = subscriptions[0]
    if (!subscription) return
    setRunningDelivery('subscription')
    biApi.runSubscription(subscription.id)
      .then(reloadSubscriptions)
      .finally(() => setRunningDelivery(null))
  }

  const runLatestAlert = () => {
    const alert = alertRules[0]
    if (!alert) return
    setRunningDelivery('alert')
    biApi.runAlert(alert.id)
      .then(reloadSubscriptions)
      .finally(() => setRunningDelivery(null))
  }

  const runDeliveryScheduler = () => {
    setRunningDelivery('scheduler')
    biApi.runDeliveryScheduler()
      .then(response => {
        setSchedulerResult(response.data ?? null)
        return reloadSubscriptions()
      })
      .finally(() => setRunningDelivery(null))
  }

  const retryDeliveryLogs = () => {
    setRunningDelivery('retry')
    biApi.retryDeliveryLogs(10)
      .then(response => {
        setRetryResult(response.data ?? null)
        return reloadSubscriptions()
      })
      .finally(() => setRunningDelivery(null))
  }

  const cleanupDeliveryAttachments = () => {
    setRunningDelivery('cleanup')
    biApi.cleanupDeliveryAttachments(100)
      .then(response => {
        setCleanupResult(response.data ?? null)
        return reloadSubscriptions()
      })
      .finally(() => setRunningDelivery(null))
  }

  const dashboardVersionTableColumns: ColumnsType<BiDashboardVersionView> = [
    ...dashboardVersionColumns,
    {
      title: '操作',
      width: 112,
      render: (_, row) => (
        <Button
          size="small"
          icon={<SyncOutlined />}
          loading={restoringDashboardVersion === row.version}
          onClick={() => restoreDashboardVersion(row)}
        >
          恢复草稿
        </Button>
      ),
    },
  ]

  const chartVersionTableColumns: ColumnsType<BiChartVersionView> = [
    ...chartVersionColumns,
    {
      title: '操作',
      width: 112,
      render: (_, row) => (
        <Button
          size="small"
          icon={<SyncOutlined />}
          loading={restoringChartVersion === row.version}
          onClick={() => restoreChartVersion(row)}
        >
          恢复草稿
        </Button>
      ),
    },
  ]

  const datasetVersionTableColumns: ColumnsType<BiDatasetVersionView> = [
    ...datasetVersionColumns,
    {
      title: '操作',
      width: 112,
      render: (_, row) => (
        <Button
          size="small"
          icon={<SyncOutlined />}
          loading={restoringDatasetVersion === row.version}
          onClick={() => restoreDatasetVersion(row)}
        >
          恢复草稿
        </Button>
      ),
    },
  ]

  const portalVersionTableColumns: ColumnsType<BiPortalVersionView> = [
    ...portalVersionColumns,
    {
      title: '操作',
      width: 112,
      render: (_, row) => (
        <Button
          size="small"
          icon={<SyncOutlined />}
          loading={restoringPortalVersion === row.version}
          onClick={() => restorePortalVersion(row)}
        >
          恢复草稿
        </Button>
      ),
    },
  ]

  const bigScreenVersionTableColumns: ColumnsType<BiBigScreenVersionView> = [
    ...bigScreenVersionColumns,
    {
      title: '操作',
      width: 112,
      render: (_, row) => (
        <Button
          size="small"
          icon={<SyncOutlined />}
          loading={restoringBigScreenVersion === row.version}
          onClick={() => restoreBigScreenVersion(row)}
        >
          恢复草稿
        </Button>
      ),
    },
  ]

  const spreadsheetVersionTableColumns: ColumnsType<BiSpreadsheetVersionView> = [
    ...spreadsheetVersionColumns,
    {
      title: '操作',
      width: 112,
      render: (_, row) => (
        <Button
          size="small"
          icon={<SyncOutlined />}
          loading={restoringSpreadsheetVersion === row.version}
          onClick={() => restoreSpreadsheetVersion(row)}
        >
          恢复草稿
        </Button>
      ),
    },
  ]

  if (runtimeRoute.mode === 'big-screen') {
    return (
      <BiBigScreenRuntimeView
        loading={loadingBigScreens}
        resource={runtimeBigScreenResource}
        route={runtimeRoute}
      />
    )
  }

  if (runtimeRoute.mode === 'spreadsheet') {
    return (
      <BiSpreadsheetRuntimeView
        loading={loadingSpreadsheets}
        resource={runtimeSpreadsheetResource}
        route={runtimeRoute}
      />
    )
  }

  return (
    <div style={{ minHeight: '100vh', background: '#f4f6f9', color: '#111827' }}>
      <div style={topbarStyle}>
        <Space size={12} align="center">
          <div style={brandIconStyle}><BarChartOutlined /></div>
          <Space direction="vertical" size={0}>
            <Space size={8} align="center">
              <Title level={4} style={{ margin: 0 }}>数据分析工作台</Title>
              <Tag color="geekblue">{dashboardPreset.title}</Tag>
              <Tag color={dashboardResource?.status === 'PUBLISHED' ? 'green' : dashboardResource?.status === 'DRAFT' ? 'gold' : 'default'}>
                {dashboardResource?.source ?? 'PRESET'} · {dashboardResource?.status ?? 'PRESET'} v{dashboardResource?.version ?? 1}
              </Tag>
              {canvasId && <Tag color="cyan">Canvas #{canvasId}</Tag>}
            </Space>
            <Text type="secondary" style={{ fontSize: 12 }}>{selected.label} · {selected.capability}</Text>
          </Space>
        </Space>
        <Space size={8} wrap>
          {QUICKBI_DESIGNER_ACTIONS.map(action => (
            <Tooltip key={action.key} title={action.label}>
              <Button
                type={action.key === 'publish' ? 'primary' : 'default'}
                icon={actionIcons[action.key]}
                loading={
                  (action.key === 'embed' && creatingEmbedTicket)
                  || (action.key === 'save' && savingDashboard)
                  || (action.key === 'publish' && publishingDashboard)
                  || (action.key === 'clone' && cloningDashboard)
                  || (action.key === 'export' && exportingDashboard)
                  || (action.key === 'import' && importingDashboard)
                  || (action.key === 'archive' && archivingDashboard)
                  || (action.key === 'subscribe' && savingSubscription === 'subscription')
                }
                disabled={
                  (action.key === 'archive' && dashboardResource?.source !== 'PERSISTED')
                  || (action.key === 'export' && dashboardResource?.status !== 'PUBLISHED')
                  || (action.key === 'undo' && !canUndoDashboardEdit)
                  || (action.key === 'redo' && !canRedoDashboardEdit)
                }
                onClick={() => handleDesignerAction(action.key)}
              >
                {action.label}
              </Button>
            </Tooltip>
          ))}
        </Space>
      </div>

      <input
        ref={dashboardPackageInputRef}
        type="file"
        accept="application/json,.json,.bi-dashboard.json"
        style={{ display: 'none' }}
        onChange={importDashboardPackageFile}
      />

      <Drawer
        title="导出审计详情"
        width={520}
        open={exportDetailOpen}
        onClose={() => setExportDetailOpen(false)}
      >
        {loadingExportDetail ? (
          <Empty description="正在加载审计详情" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : exportDetail ? (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Descriptions size="small" column={1} bordered>
              {exportAuditRows.map(row => (
                <Descriptions.Item key={row.label} label={row.label}>
                  <Text code={row.label === '存储'}>{row.value}</Text>
                </Descriptions.Item>
              ))}
            </Descriptions>
            <Space size={[6, 6]} wrap>
              {(exportDetail.request.query?.dimensions ?? []).map(field => <Tag key={`dimension-${field}`} color="blue">{field}</Tag>)}
              {(exportDetail.request.query?.metrics ?? []).map(field => <Tag key={`metric-${field}`} color="purple">{field}</Tag>)}
            </Space>
          </Space>
        ) : (
          <Empty description="暂无审计详情" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        )}
      </Drawer>

      <Drawer
        title="查询历史详情"
        width={520}
        open={queryHistoryDetailOpen}
        onClose={() => setQueryHistoryDetailOpen(false)}
      >
        {loadingQueryHistoryDetail ? (
          <Empty description="正在加载查询详情" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : queryHistoryDetail ? (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Descriptions size="small" column={1} bordered>
              {queryHistoryRows.map(row => (
                <Descriptions.Item key={row.label} label={row.label}>
                  <Text code={row.label === 'SQL Hash'}>{row.value}</Text>
                </Descriptions.Item>
              ))}
            </Descriptions>
            <Space size={[6, 6]} wrap>
              {(queryHistoryDetail.request?.dimensions ?? []).map(field => <Tag key={`query-dimension-${field}`} color="blue">{field}</Tag>)}
              {(queryHistoryDetail.request?.metrics ?? []).map(field => <Tag key={`query-metric-${field}`} color="purple">{field}</Tag>)}
            </Space>
          </Space>
        ) : (
          <Empty description="暂无查询详情" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        )}
      </Drawer>

      <div style={moduleBarStyle}>
        <Segmented
          value={sectionKey}
          options={BI_WORKBENCH_SECTIONS.map(section => ({
            label: (
              <Space size={6}>
                {sectionIcons[section.key]}
                <span>{section.label}</span>
              </Space>
            ),
            value: section.key,
          }))}
          onChange={value => setSectionKey(value as BiSectionKey)}
        />
      </div>

      <div style={designerShellStyle}>
        <aside style={leftPanelStyle}>
          <Input
            size="small"
            allowClear
            prefix={<SearchOutlined />}
            placeholder="搜索字段、图表、控件"
            value={designerSearch}
            onChange={event => setDesignerSearch(event.target.value)}
            style={{ marginBottom: 10 }}
          />
          <Tabs
            activeKey={leftTab}
            onChange={setLeftTab}
            size="small"
            items={[
              {
                key: 'data',
                label: '数据',
                children: (
                  <Space direction="vertical" size={12} style={{ width: '100%' }}>
                    <PanelHeader icon={<DatabaseOutlined />} title={dashboardPreset.datasetKey} />
                    <ResourceGroup title="维度" items={filteredDimensions.map(field => field.fieldKey)} emptyText="暂无维度字段" />
                    <ResourceGroup title="度量字段" items={filteredMeasures.map(field => field.fieldKey)} emptyText="暂无度量字段" />
                    <ResourceGroup title="指标" items={filteredMetrics.map(metric => metric.metricKey)} emptyText="暂无指标" accent />
                  </Space>
                ),
              },
              {
                key: 'chart',
                label: '图表',
                children: (
                  <Space direction="vertical" size={12} style={{ width: '100%' }}>
                    <PanelHeader icon={<BarChartOutlined />} title="图表资产" />
                    <ChartResourceList
                      charts={filteredChartResources}
                      loading={loadingCharts}
                      selectedKey={selectedChartResource?.chartKey}
                      onSelect={setSelectedChartKey}
                    />
                    <Space direction="vertical" size={8} style={{ width: '100%', padding: 10, border: '1px solid #eef2f7', borderRadius: 6 }}>
                      <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                        <Space>
                          <BarChartOutlined />
                          <Text strong>图表编辑器</Text>
                          <Tag color={selectedChartResource ? 'blue' : 'default'}>{selectedChartResource?.chartKey ?? '未选择图表'}</Tag>
                        </Space>
                      </Space>
                      <select
                        aria-label="图表类型"
                        value={selectedChartResource?.chartType ?? 'BAR'}
                        style={{ width: '100%', height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                        disabled={!selectedChartResource}
                        onChange={event => updateSelectedChartResource({ chartType: event.target.value })}
                      >
                        {QUICKBI_CHART_PALETTE.map(chart => (
                          <option key={chart.key} value={chart.key}>{chart.label}</option>
                        ))}
                      </select>
                      <Input
                        size="small"
                        aria-label="图表数据集"
                        value={selectedChartResource?.datasetKey ?? ''}
                        placeholder="dataset_key"
                        disabled={!selectedChartResource}
                        onChange={event => updateSelectedChartResource({ datasetKey: event.target.value, query: { datasetKey: event.target.value } })}
                      />
                      <Input
                        size="small"
                        aria-label="图表维度字段"
                        value={(selectedChartResource?.query.dimensions ?? []).join(',')}
                        placeholder="dimension_a,dimension_b"
                        disabled={!selectedChartResource}
                        onChange={event => applySelectedChartQueryDesigner({ selectedDimensions: parseChartFieldList(event.target.value) })}
                      />
                      <Input
                        size="small"
                        aria-label="图表指标字段"
                        value={(selectedChartResource?.query.metrics ?? []).join(',')}
                        placeholder="metric_a,metric_b"
                        disabled={!selectedChartResource}
                        onChange={event => applySelectedChartQueryDesigner({ selectedMetrics: parseChartFieldList(event.target.value) })}
                      />
                      <ChartFieldDropBuilder
                        dimensionFields={filteredDimensions.map(field => field.fieldKey)}
                        metricFields={filteredMetrics.map(metric => metric.metricKey)}
                        selectedDimensions={selectedChartResource?.query.dimensions ?? []}
                        selectedMetrics={selectedChartResource?.query.metrics ?? []}
                        disabled={!selectedChartResource}
                        onDropField={dropSelectedChartField}
                      />
                      <Space size={8} wrap>
                        <Input
                          size="small"
                          aria-label="图表筛选字段"
                          value={chartFilterFieldDraft}
                          placeholder="filter_field"
                          style={{ width: 140 }}
                          disabled={!selectedChartResource}
                          onChange={event => {
                            chartFilterFieldDraftRef.current = event.target.value
                            setChartFilterFieldDraft(event.target.value)
                            applySelectedChartQueryDesigner({ filterField: event.target.value })
                          }}
                        />
                        <select
                          aria-label="图表筛选操作符"
                          value={chartFilterOperatorDraft}
                          style={{ width: 96, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                          disabled={!selectedChartResource}
                          onChange={event => {
                            chartFilterOperatorDraftRef.current = event.target.value
                            setChartFilterOperatorDraft(event.target.value)
                            applySelectedChartQueryDesigner({ filterOperator: event.target.value })
                          }}
                        >
                          <option value="IN">IN</option>
                          <option value="EQ">EQ</option>
                          <option value="CONTAINS">CONTAINS</option>
                          <option value="BETWEEN">BETWEEN</option>
                        </select>
                        <Input
                          size="small"
                          aria-label="图表筛选值"
                          value={chartFilterValueDraft}
                          placeholder="value 或 a,b"
                          style={{ width: 160 }}
                          disabled={!selectedChartResource}
                          onChange={event => {
                            chartFilterValueDraftRef.current = event.target.value
                            setChartFilterValueDraft(event.target.value)
                            applySelectedChartQueryDesigner({ filterValue: event.target.value })
                          }}
                        />
                      </Space>
                      <Space size={8} wrap>
                        <Input
                          size="small"
                          aria-label="图表排序字段"
                          value={chartSortFieldDraft}
                          placeholder="sort_field"
                          style={{ width: 140 }}
                          disabled={!selectedChartResource}
                          onChange={event => {
                            chartSortFieldDraftRef.current = event.target.value
                            setChartSortFieldDraft(event.target.value)
                            applySelectedChartQueryDesigner({ sortField: event.target.value })
                          }}
                        />
                        <select
                          aria-label="图表排序方向"
                          value={chartSortDirectionDraft}
                          style={{ width: 96, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                          disabled={!selectedChartResource}
                          onChange={event => {
                            chartSortDirectionDraftRef.current = event.target.value
                            setChartSortDirectionDraft(event.target.value)
                            applySelectedChartQueryDesigner({ sortDirection: event.target.value })
                          }}
                        >
                          <option value="DESC">DESC</option>
                          <option value="ASC">ASC</option>
                        </select>
                      </Space>
                      <Input
                        size="small"
                        type="number"
                        aria-label="图表取数上限"
                        value={String(selectedChartResource?.query.limit ?? 100)}
                        placeholder="100"
                        disabled={!selectedChartResource}
                        onChange={event => applySelectedChartQueryDesigner({ limit: Math.max(1, Number(event.target.value) || 1) })}
                      />
                      <Space size={8} wrap>
                        <select
                          aria-label="图表主题"
                          value={String(selectedChartResource?.style?.theme ?? 'default')}
                          style={{ width: 132, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartStyle({ theme: event.target.value })}
                        >
                          <option value="default">默认浅色</option>
                          <option value="screen-dark">大屏深色</option>
                          <option value="mobile-compact">移动紧凑</option>
                        </select>
                        <select
                          aria-label="图表密度"
                          value={String(selectedChartResource?.style?.density ?? 'compact')}
                          style={{ width: 116, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartStyle({ density: event.target.value })}
                        >
                          <option value="compact">紧凑</option>
                          <option value="comfortable">标准</option>
                          <option value="spacious">宽松</option>
                        </select>
                        <select
                          aria-label="图表调色板"
                          value={String(selectedChartResource?.style?.palette ?? 'blue')}
                          style={{ width: 116, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartStyle({ palette: event.target.value })}
                        >
                          <option value="blue">蓝色</option>
                          <option value="green">绿色</option>
                          <option value="warm">暖色</option>
                          <option value="mono">灰阶</option>
                        </select>
                      </Space>
                      <Space size={12} wrap>
                        <Switch
                          size="small"
                          aria-label="图表图例"
                          checked={Boolean(selectedChartResource?.style?.legendVisible ?? selectedChartResource?.chartType !== 'KPI_CARD')}
                          disabled={!selectedChartResource}
                          onChange={checked => updateSelectedChartStyle({ legendVisible: checked })}
                        />
                        <Text type="secondary" style={{ fontSize: 12 }}>图例</Text>
                        <Switch
                          size="small"
                          aria-label="图表数据标签"
                          checked={Boolean(selectedChartResource?.style?.dataLabelsVisible ?? false)}
                          disabled={!selectedChartResource}
                          onChange={checked => updateSelectedChartStyle({ dataLabelsVisible: checked })}
                        />
                        <Text type="secondary" style={{ fontSize: 12 }}>数据标签</Text>
                        <Switch
                          size="small"
                          aria-label="图表钻取"
                          checked={Boolean(selectedChartResource?.interaction?.drillEnabled ?? selectedChartResource?.interaction?.drill ?? false)}
                          disabled={!selectedChartResource}
                          onChange={checked => updateSelectedChartInteraction({ drillEnabled: checked })}
                        />
                        <Text type="secondary" style={{ fontSize: 12 }}>钻取</Text>
                      </Space>
                      <Space size={8} wrap>
                        <Input
                          size="small"
                          aria-label="图表X轴标题"
                          value={String((selectedChartResource?.style?.axis as Record<string, unknown> | undefined)?.xTitle ?? '')}
                          placeholder="X 轴标题"
                          style={{ width: 140 }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartAxisStyle({ xTitle: event.target.value })}
                        />
                        <Input
                          size="small"
                          aria-label="图表Y轴标题"
                          value={String((selectedChartResource?.style?.axis as Record<string, unknown> | undefined)?.yTitle ?? '')}
                          placeholder="Y 轴标题"
                          style={{ width: 140 }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartAxisStyle({ yTitle: event.target.value })}
                        />
                        <select
                          aria-label="图表标签位置"
                          value={String((selectedChartResource?.style?.labels as Record<string, unknown> | undefined)?.position ?? 'outside')}
                          style={{ width: 116, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartLabelStyle({ position: event.target.value })}
                        >
                          <option value="outside">外侧</option>
                          <option value="inside">内侧</option>
                          <option value="center">居中</option>
                        </select>
                        <Input
                          size="small"
                          aria-label="图表数字格式"
                          value={String((selectedChartResource?.style?.labels as Record<string, unknown> | undefined)?.numberFormat ?? '')}
                          placeholder="0,0"
                          style={{ width: 116 }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartLabelStyle({ numberFormat: event.target.value })}
                        />
                      </Space>
                      <Space size={8} wrap>
                        <Input
                          size="small"
                          aria-label="图表条件格式字段"
                          value={String(((selectedChartResource?.style?.conditionalFormats as Array<Record<string, unknown>> | undefined)?.[0])?.field ?? '')}
                          placeholder="metric"
                          style={{ width: 140 }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartConditionalFormat({ field: event.target.value })}
                        />
                        <select
                          aria-label="图表条件格式操作符"
                          value={String(((selectedChartResource?.style?.conditionalFormats as Array<Record<string, unknown>> | undefined)?.[0])?.operator ?? 'GTE')}
                          style={{ width: 96, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartConditionalFormat({ operator: event.target.value })}
                        >
                          <option value="GTE">&gt;=</option>
                          <option value="GT">&gt;</option>
                          <option value="LTE">&lt;=</option>
                          <option value="LT">&lt;</option>
                          <option value="EQ">=</option>
                        </select>
                        <Input
                          size="small"
                          aria-label="图表条件格式阈值"
                          value={String(((selectedChartResource?.style?.conditionalFormats as Array<Record<string, unknown>> | undefined)?.[0])?.value ?? '')}
                          placeholder="阈值"
                          style={{ width: 96 }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartConditionalFormat({ value: event.target.value })}
                        />
                        <Input
                          size="small"
                          aria-label="图表条件格式颜色"
                          value={String(((selectedChartResource?.style?.conditionalFormats as Array<Record<string, unknown>> | undefined)?.[0])?.color ?? '')}
                          placeholder="#16a34a"
                          style={{ width: 112 }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartConditionalFormat({ color: event.target.value })}
                        />
                      </Space>
                      <Space size={8} wrap>
                        <Input
                          size="small"
                          aria-label="图表联动目标"
                          value={String(selectedChartResource?.interaction?.linkageTarget ?? '')}
                          placeholder="target_widget"
                          style={{ width: 160 }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartInteraction({ linkageTarget: event.target.value })}
                        />
                        <Input
                          size="small"
                          aria-label="图表跳转模板"
                          value={String(selectedChartResource?.interaction?.hyperlinkTemplate ?? '')}
                          placeholder="/path/{field}"
                          style={{ width: 220 }}
                          disabled={!selectedChartResource}
                          onChange={event => updateSelectedChartInteraction({ hyperlinkTemplate: event.target.value })}
                        />
                      </Space>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {selectedChartReferenceImpact}
                      </Text>
                      <Space size={8} wrap>
                        <Button
                          size="small"
                          type="primary"
                          aria-label="保存图表草稿"
                          icon={<SaveOutlined />}
                          disabled={!selectedChartResource}
                          loading={savingChart === selectedChartResource?.chartKey}
                          onClick={saveChartDraft}
                        >
                          保存草稿
                        </Button>
                        <Button
                          size="small"
                          aria-label="复制图表草稿"
                          icon={<CopyOutlined />}
                          disabled={!selectedChartResource}
                          onClick={copySelectedChartDraft}
                        >
                          复制
                        </Button>
                      </Space>
                    </Space>
                    <Divider style={{ margin: '4px 0' }} />
                    <PaletteList items={filteredChartPalette} />
                  </Space>
                ),
              },
              {
                key: 'control',
                label: '控件',
                children: <PaletteList items={filteredControlPalette} />,
              },
            ]}
          />
        </aside>

        <main style={canvasPanelStyle}>
          <div style={canvasToolbarStyle}>
            <DashboardRuntimeToolbar
              preset={dashboardPreset}
              runtimeParameters={dashboardRuntimeParameters}
              runtimeRows={dashboardRuntimeRows}
              runtimeUpdatedAt={dashboardRuntimeState?.updatedAt ?? null}
              onRuntimeParameterChange={changeDashboardRuntimeParameter}
              onRuntimeParameterClear={clearDashboardRuntimeParameter}
              onRuntimeParametersReset={resetDashboardRuntimeParametersToDefaults}
            />
            <Space size={8}>
              <Segmented
                size="small"
                value={dashboardLayoutMode}
                onChange={value => setDashboardLayoutMode(value as DashboardLayoutMode)}
                options={[
                  { label: '桌面', value: 'desktop' },
                  { label: '平板', value: 'tablet' },
                  { label: '手机', value: 'mobile' },
                ]}
              />
              <Tag color={selectedLayoutWidgetKeys.length > 1 ? 'blue' : 'default'}>{selectedLayoutWidgetKeys.length} 选中</Tag>
              <Tooltip title="左对齐">
                <Button
                  size="small"
                  icon={<AlignLeftOutlined />}
                  disabled={selectedLayoutWidgetKeys.length < 2}
                  onClick={() => alignSelectedWidgets('left')}
                />
              </Tooltip>
              <Tooltip title="水平居中">
                <Button
                  size="small"
                  icon={<AlignCenterOutlined />}
                  disabled={selectedLayoutWidgetKeys.length < 2}
                  onClick={() => alignSelectedWidgets('center')}
                />
              </Tooltip>
              <Tooltip title="右对齐">
                <Button
                  size="small"
                  icon={<AlignRightOutlined />}
                  disabled={selectedLayoutWidgetKeys.length < 2}
                  onClick={() => alignSelectedWidgets('right')}
                />
              </Tooltip>
              <Tooltip title="顶对齐">
                <Button
                  size="small"
                  icon={<VerticalAlignTopOutlined />}
                  disabled={selectedLayoutWidgetKeys.length < 2}
                  onClick={() => alignSelectedWidgets('top')}
                />
              </Tooltip>
              <Tooltip title="垂直居中">
                <Button
                  size="small"
                  icon={<VerticalAlignMiddleOutlined />}
                  disabled={selectedLayoutWidgetKeys.length < 2}
                  onClick={() => alignSelectedWidgets('middle')}
                />
              </Tooltip>
              <Tooltip title="底对齐">
                <Button
                  size="small"
                  icon={<VerticalAlignBottomOutlined />}
                  disabled={selectedLayoutWidgetKeys.length < 2}
                  onClick={() => alignSelectedWidgets('bottom')}
                />
              </Tooltip>
              <Tag icon={<NodeIndexOutlined />} color="blue">{dashboardPreset.interactions.length} 交互</Tag>
              <Tag icon={<SendOutlined />} color="purple">{dashboardPreset.subscriptionChannels.length} 订阅</Tag>
              <Tag icon={<LinkOutlined />} color="cyan">{dashboardPreset.embedScopes.length} 嵌入</Tag>
            </Space>
          </div>

          <section
            aria-label="QuickBI 仪表板画布"
            tabIndex={0}
            onKeyDown={handleDashboardDesignerKeyDown}
            style={{
              ...dashboardCanvasStyle,
              gridTemplateColumns: `repeat(${dashboardDisplayColumns}, minmax(0, 1fr))`,
              maxWidth: dashboardLayoutMode === 'mobile' ? 420 : dashboardLayoutMode === 'tablet' ? 860 : undefined,
              margin: dashboardLayoutMode === 'desktop' ? undefined : '0 auto',
            }}
          >
            {dashboardDisplayWidgets.map(widget => (
              <DashboardWidgetCard
                key={widget.widgetKey}
                widget={widget}
                displayColumns={dashboardDisplayColumns}
                queryResult={queryResults[widget.widgetKey]}
                loading={loadingQueries}
                selected={selectedWidgetKeySet.has(widget.widgetKey)}
                onSelect={additive => selectWidget(widget.widgetKey, additive)}
                onDuplicate={() => duplicateWidget(widget.widgetKey)}
                onRemove={() => removeWidget(widget.widgetKey)}
                onMove={direction => moveWidget(widget.widgetKey, direction)}
                onMoveByPixels={(deltaX, deltaY) => moveWidgetByPixels(widget.widgetKey, deltaX, deltaY)}
                onResizeByPixels={(deltaX, deltaY) => resizeWidgetByPixels(widget.widgetKey, deltaX, deltaY)}
                onCompile={() => {
                  selectWidget(widget.widgetKey)
                  compileWidgetQuery(widget)
                }}
              />
            ))}
          </section>
        </main>

        <aside style={rightPanelStyle}>
          <Space direction="vertical" size={10} style={{ width: '100%' }}>
            <PanelHeader icon={<SettingOutlined />} title={selectedWidget.title} />
            <Tabs
              activeKey={configTab}
              onChange={setConfigTab}
              size="small"
              items={[
                {
                  key: 'data',
                  label: '数据',
                  children: (
                    <DataConfig
                      widget={selectedWidget}
                      queryResult={queryResults[selectedWidget.widgetKey]}
                      compiledQuery={compiledWidgetQuery}
                      executionPlan={queryExecutionPlan}
                      compiling={compilingWidgetKey === selectedWidget.widgetKey}
                      explaining={explainingWidgetKey === selectedWidget.widgetKey}
                      onCompile={() => compileWidgetQuery(selectedWidget)}
                      onExplain={() => explainWidgetQuery(selectedWidget)}
                    />
                  ),
                },
                {
                  key: 'style',
                  label: '样式',
                  children: <StyleConfig widget={selectedWidget} />,
                },
                {
                  key: 'interaction',
                  label: '交互',
                  children: (
                    <InteractionConfig
                      preset={dashboardPreset}
                      widget={selectedWidget}
                      canvasId={canvasId}
                      runtimeParameters={dashboardRuntimeParameters}
                      runtimeRows={dashboardRuntimeRows}
                      runtimeUpdatedAt={dashboardRuntimeState?.updatedAt ?? null}
                      queryResult={queryResults[selectedWidget.widgetKey]}
                      onRuntimeParameterChange={changeDashboardRuntimeParameter}
                      onRuntimeParameterClear={clearDashboardRuntimeParameter}
                      onRuntimeParametersReset={resetDashboardRuntimeParametersToDefaults}
                      embedTicket={embedTicket}
                      controlOptionResults={controlOptionResults}
                      loadingControlOptions={loadingControlOptions}
                    />
                  ),
                },
              ]}
            />
          </Space>
        </aside>
      </div>

      <div style={versionBandStyle}>
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <ShareAltOutlined />
              <Text strong>发布历史</Text>
              <Tag color={dashboardVersions.length > 0 ? 'green' : 'default'}>{dashboardVersions.length} 快照</Tag>
              {dashboardExportPackage && (
                <Tag color="cyan">资源包 v{dashboardExportPackage.sourceVersion}</Tag>
              )}
            </Space>
            <Badge status={loadingDashboardVersions ? 'processing' : 'success'} text={loadingDashboardVersions ? '同步中' : '已就绪'} />
          </Space>
          <Table
            rowKey="id"
            size="small"
            loading={loadingDashboardVersions}
            pagination={false}
            columns={dashboardVersionTableColumns}
            dataSource={dashboardVersions}
            locale={{ emptyText: '暂无发布历史，发布后会生成可追溯快照' }}
            scroll={{ x: 760 }}
          />
        </Space>
      </div>

      <div style={versionBandStyle}>
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <BarChartOutlined />
              <Text strong>图表发布历史</Text>
              <Tag color={selectedChartResource ? 'blue' : 'default'}>{selectedChartResource?.chartKey ?? '未选择图表'}</Tag>
              <Tag color={chartVersions.length > 0 ? 'green' : 'default'}>{chartVersions.length} 快照</Tag>
            </Space>
            <Badge status={loadingChartVersions ? 'processing' : 'success'} text={loadingChartVersions ? '同步中' : '已就绪'} />
          </Space>
          <Table
            rowKey="id"
            size="small"
            loading={loadingChartVersions}
            pagination={false}
            columns={chartVersionTableColumns}
            dataSource={chartVersions}
            locale={{ emptyText: selectedChartResource ? '暂无图表发布历史，发布图表后会生成快照' : '暂无可查看图表' }}
            scroll={{ x: 760 }}
          />
        </Space>
      </div>

      <div style={versionBandStyle}>
        <Row gutter={16}>
          <Col xs={24} xl={12}>
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                <Space>
                  <DeploymentUnitOutlined />
                  <Text strong>数据集发布历史</Text>
                  <Tag color={selectedDatasetResource ? 'blue' : 'default'}>{selectedDatasetResource?.datasetKey ?? '未选择数据集'}</Tag>
                  <Tag color={datasetVersions.length > 0 ? 'green' : 'default'}>{datasetVersions.length} 快照</Tag>
                </Space>
                <Badge status={loadingDatasetVersions ? 'processing' : 'success'} text={loadingDatasetVersions ? '同步中' : '已就绪'} />
              </Space>
              <Table
                rowKey="id"
                size="small"
                loading={loadingDatasetVersions}
                pagination={false}
                columns={datasetVersionTableColumns}
                dataSource={datasetVersions}
                locale={{ emptyText: selectedDatasetResource ? '暂无数据集发布历史，发布数据集后会生成快照' : '暂无可查看数据集' }}
                scroll={{ x: 720 }}
              />
            </Space>
          </Col>
          <Col xs={24} xl={12}>
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                <Space>
                  <ApiOutlined />
                  <Text strong>门户发布历史</Text>
                  <Tag color={selectedPortalResource ? 'blue' : 'default'}>{selectedPortalResource?.portalKey ?? '未选择门户'}</Tag>
                  <Tag color={portalVersions.length > 0 ? 'green' : 'default'}>{portalVersions.length} 快照</Tag>
                </Space>
                <Badge status={loadingPortalVersions ? 'processing' : 'success'} text={loadingPortalVersions ? '同步中' : '已就绪'} />
              </Space>
              <Table
                rowKey="id"
                size="small"
                loading={loadingPortalVersions}
                pagination={false}
                columns={portalVersionTableColumns}
                dataSource={portalVersions}
                locale={{ emptyText: selectedPortalResource ? '暂无门户发布历史，发布门户后会生成快照' : '暂无可查看门户' }}
                scroll={{ x: 720 }}
              />
            </Space>
          </Col>
        </Row>
      </div>

      <div style={governanceBandStyle}>
        <Row gutter={12}>
          <Col xs={24} lg={8}>
            <Space direction="vertical" size={10} style={{ width: '100%' }}>
              <Space>
                <DatabaseOutlined />
                <Text strong>数据源健康</Text>
                <Badge status={loadingGovernance ? 'processing' : 'success'} />
              </Space>
              <Space size={[6, 6]} wrap>
                {datasourceHealth.length === 0 ? (
                  <Text type="secondary" style={{ fontSize: 12 }}>暂无健康数据</Text>
                ) : datasourceHealth.map(source => (
                  <Tag
                    key={source.sourceKey}
                    color={source.available ? 'green' : 'orange'}
                    icon={source.available ? <CheckCircleOutlined /> : <ExclamationCircleOutlined />}
                  >
                    {source.sourceType} · {source.message}
                  </Tag>
                ))}
              </Space>
              <Descriptions
                size="small"
                column={1}
                items={datasourceHealthSloDisplayRows.map(row => ({
                  key: row.label,
                  label: row.label,
                  children: row.value,
                }))}
              />
              <Space>
                <CloudUploadOutlined />
                <Text strong>数据源接入</Text>
                <Tag color="blue">{datasourceOnboarding.length} 个</Tag>
              </Space>
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                <Steps
                  size="small"
                  direction="vertical"
                  current={datasourceWizardStep}
                  items={datasourceWizardStepItems}
                />
                <Space align="center" size={[6, 6]} wrap>
                  <Text strong>当前步骤：{datasourceWizardStepItems[datasourceWizardStep]?.title ?? '连接器配置'}</Text>
                  {datasourceWizardStep === 0 ? datasourceConnectorCapabilityTags.map(tag => (
                    <Tag key={`datasource-connector-capability-${tag}`}>{tag}</Tag>
                  )) : null}
                </Space>
                {datasourceWizardStep === 2 ? (
                  <Descriptions
                    size="small"
                    column={1}
                    items={datasourceWizardReviewRows.map(row => ({
                      key: row.label,
                      label: row.label,
                      children: row.value,
                    }))}
                  />
                ) : null}
                <Space size={8} wrap>
                  <Button
                    size="small"
                    disabled={datasourceWizardStep === 0}
                    onClick={() => setDatasourceWizardStep(step => Math.max(0, step - 1))}
                  >
                    上一步
                  </Button>
                  {datasourceWizardStep < datasourceWizardStepItems.length - 1 ? (
                    <Button
                      size="small"
                      type="primary"
                      disabled={datasourceWizardNextDisabled}
                      onClick={() => setDatasourceWizardStep(step => Math.min(datasourceWizardStepItems.length - 1, step + 1))}
                    >
                      下一步
                    </Button>
                  ) : (
                    <Button
                      size="small"
                      type="primary"
                      aria-label={editingDatasourceId ? '更新数据源' : '保存数据源'}
                      loading={savingDatasourceOnboarding}
                      disabled={datasourceDraftMissingRequiredFields}
                      onClick={saveDatasourceOnboarding}
                    >
                      {editingDatasourceId ? '更新数据源' : '保存数据源'}
                    </Button>
                  )}
                  {editingDatasourceId ? (
                    <Button size="small" onClick={cancelDatasourceEdit}>取消编辑</Button>
                  ) : null}
                </Space>
                <Space size={8} wrap>
                  {datasourceWizardStep === 0 ? (
                    <Select
                      size="small"
                      aria-label="BI数据源连接器"
                      value={datasourceConnectorValue}
                      style={{ width: 180 }}
                      options={datasourceConnectorOptions.length > 0 ? datasourceConnectorOptions : [{ label: 'MySQL / MYSQL', value: 'MYSQL' }]}
                      onChange={value => {
                        const connector = datasourceConnectors.find(item => item.connectorType === value)
                        const supportedModes = connector?.supportedModes?.length ? connector.supportedModes : ['DIRECT_QUERY', 'CACHE']
                        const isFileConnector = value === 'CSV_EXCEL' || connector?.sourceCategory === 'FILE'
                        const isHttpJsonConnector = value === 'API' || connector?.sourceCategory === 'HTTP' || connector?.sourceCategory === 'APP'
                        if (!isFileConnector) {
                          setDatasourceUploadFile(null)
                        }
                        updateDatasourceDraft({
                          connectorType: value,
                          connectionMode: supportedModes.includes('DIRECT_QUERY') ? 'DIRECT_QUERY' : supportedModes[0] ?? 'DIRECT_QUERY',
                          ...(isHttpJsonConnector ? {
                            apiRequestMethod: 'GET',
                            apiAuthType: 'NONE',
                            apiResponseRowsPath: '$',
                            apiResponseFormat: 'JSON',
                          } : {}),
                          ...(isFileConnector ? {
                            username: '',
                            password: '',
                            driverClassName: '',
                            fileType: 'CSV',
                            fileDelimiter: ',',
                            fileHeaderRow: true,
                            fileEncoding: 'UTF-8',
                          } : {}),
                        })
                      }}
                    />
                  ) : null}
                  {datasourceWizardStep === 1 ? (
                    <>
                      <Input
                        size="small"
                        aria-label="BI数据源名称"
                        value={datasourceDraft.name ?? ''}
                        placeholder="数据源名称"
                        style={{ width: 180 }}
                        onChange={event => updateDatasourceDraft({ name: event.target.value })}
                      />
                      <Input
                        size="small"
                        aria-label="BI数据源URL"
                        value={datasourceDraft.url ?? ''}
                        placeholder={isFileDatasourceConnector ? 'file://uploads/orders.csv' : isApiDatasourceConnector ? 'HTTP API URL' : 'JDBC URL'}
                        style={{ width: 260 }}
                        onChange={event => updateDatasourceDraft({ url: event.target.value })}
                      />
                      {datasourceRequiresCredentials ? (
                        <>
                          <Input
                            size="small"
                            aria-label="BI数据源账号"
                            value={datasourceDraft.username ?? ''}
                            placeholder="账号"
                            style={{ width: 140 }}
                            onChange={event => updateDatasourceDraft({ username: event.target.value })}
                          />
                          <Input.Password
                            size="small"
                            aria-label="BI数据源密码"
                            value={datasourceDraft.password ?? ''}
                            placeholder="密码"
                            autoComplete="new-password"
                            style={{ width: 150 }}
                            onChange={event => updateDatasourceDraft({ password: event.target.value })}
                          />
                        </>
                      ) : null}
                    </>
                  ) : null}
                </Space>
                <Space size={8} wrap>
                  {datasourceWizardStep === 1 && !isApiDatasourceConnector && !isFileDatasourceConnector ? (
                    <Input
                      size="small"
                      aria-label="BI数据源Driver"
                      value={datasourceDraft.driverClassName ?? ''}
                      placeholder={selectedDatasourceConnector?.driverClassNames?.[0] || '驱动类'}
                      style={{ width: 220 }}
                      onChange={event => updateDatasourceDraft({ driverClassName: event.target.value })}
                    />
                  ) : null}
                  {datasourceWizardStep === 0 ? (
                    <Select
                      size="small"
                      aria-label="BI数据源连接模式"
                      value={datasourceConnectionModeValue}
                      options={datasourceConnectionModeOptions}
                      style={{ width: 118 }}
                      onChange={value => updateDatasourceDraft({ connectionMode: value })}
                    />
                  ) : null}
                  {datasourceWizardStep === 0 && isApiDatasourceConnector ? (
                    <>
                      <Select
                        size="small"
                        aria-label="BI API请求方法"
                        value={datasourceDraft.apiRequestMethod || 'GET'}
                        options={[
                          { label: 'GET', value: 'GET' },
                          { label: 'POST', value: 'POST' },
                        ]}
                        style={{ width: 92 }}
                        onChange={value => updateDatasourceDraft({ apiRequestMethod: value })}
                      />
                      <Select
                        size="small"
                        aria-label="BI API认证类型"
                        value={datasourceDraft.apiAuthType || 'NONE'}
                        options={[
                          { label: '无认证', value: 'NONE' },
                          { label: 'Basic', value: 'BASIC' },
                          { label: 'Bearer Token', value: 'BEARER' },
                          { label: 'API Key', value: 'API_KEY' },
                        ]}
                        style={{ width: 132 }}
                        onChange={value => updateDatasourceDraft({ apiAuthType: value })}
                      />
                      <Input
                        size="small"
                        aria-label="BI API响应行路径"
                        value={datasourceDraft.apiResponseRowsPath ?? '$'}
                        placeholder="$.data.items"
                        style={{ width: 170 }}
                        onChange={event => updateDatasourceDraft({ apiResponseRowsPath: event.target.value })}
                      />
                    </>
                  ) : null}
                  {datasourceWizardStep === 1 && isFileDatasourceConnector ? (
                    <>
                      <input
                        aria-label="BI上传文件"
                        type="file"
                        accept=".csv,.xls,.xlsx"
                        style={{ width: 190, fontSize: 12 }}
                        onChange={updateDatasourceUploadFile}
                      />
                      <Input
                        size="small"
                        aria-label="BI文件名称"
                        value={datasourceDraft.fileName ?? ''}
                        placeholder="orders.csv"
                        style={{ width: 150 }}
                        onChange={event => updateDatasourceDraft({ fileName: event.target.value })}
                      />
                      <Select
                        size="small"
                        aria-label="BI文件类型"
                        value={datasourceDraft.fileType || 'CSV'}
                        options={[
                          { label: 'CSV', value: 'CSV' },
                          { label: 'Excel xls', value: 'XLS' },
                          { label: 'Excel xlsx', value: 'XLSX' },
                        ]}
                        style={{ width: 112 }}
                        onChange={value => updateDatasourceDraft({ fileType: value })}
                      />
                      <Input
                        size="small"
                        aria-label="BI文件工作表"
                        value={datasourceDraft.fileSheetName ?? ''}
                        placeholder="Sheet"
                        style={{ width: 110 }}
                        onChange={event => updateDatasourceDraft({ fileSheetName: event.target.value })}
                      />
                      <Input
                        size="small"
                        aria-label="BI文件分隔符"
                        value={datasourceDraft.fileDelimiter ?? ','}
                        placeholder=","
                        style={{ width: 72 }}
                        onChange={event => updateDatasourceDraft({ fileDelimiter: event.target.value })}
                      />
                      <Input
                        size="small"
                        aria-label="BI文件编码"
                        value={datasourceDraft.fileEncoding ?? 'UTF-8'}
                        placeholder="UTF-8"
                        style={{ width: 92 }}
                        onChange={event => updateDatasourceDraft({ fileEncoding: event.target.value })}
                      />
                      <Switch
                        size="small"
                        aria-label="BI文件首行为表头"
                        checked={datasourceDraft.fileHeaderRow !== false}
                        onChange={checked => updateDatasourceDraft({ fileHeaderRow: checked })}
                      />
                    </>
                  ) : null}
                  {datasourceWizardStep === 1 ? (
                    <>
                      <Input
                        size="small"
                        aria-label="BI数据源说明"
                        value={datasourceDraft.description ?? ''}
                        placeholder="说明"
                        style={{ width: 220 }}
                        onChange={event => updateDatasourceDraft({ description: event.target.value })}
                      />
                      <Switch
                        size="small"
                        aria-label="BI数据源启用"
                        checked={datasourceDraft.enabled !== false}
                        onChange={checked => updateDatasourceDraft({ enabled: checked })}
                      />
                    </>
                  ) : null}
                </Space>
                {datasourceWizardStep === 1 && isApiDatasourceConnector ? (
                  <Space size={8} wrap>
                    <Input
                      size="small"
                      aria-label="BI API请求头名称"
                      value={datasourceDraft.apiHeaderName ?? ''}
                      placeholder="Header"
                      style={{ width: 140 }}
                      onChange={event => updateDatasourceDraft({ apiHeaderName: event.target.value })}
                    />
                    <Input
                      size="small"
                      aria-label="BI API请求头值"
                      value={datasourceDraft.apiHeaderValue ?? ''}
                      placeholder="{{tenantId}}"
                      style={{ width: 160 }}
                      onChange={event => updateDatasourceDraft({ apiHeaderValue: event.target.value })}
                    />
                    <Input
                      size="small"
                      aria-label="BI API参数名称"
                      value={datasourceDraft.apiParameterName ?? ''}
                      placeholder="Query"
                      style={{ width: 120 }}
                      onChange={event => updateDatasourceDraft({ apiParameterName: event.target.value })}
                    />
                    <Input
                      size="small"
                      aria-label="BI API参数值"
                      value={datasourceDraft.apiParameterValue ?? ''}
                      placeholder="{{page}}"
                      style={{ width: 140 }}
                      onChange={event => updateDatasourceDraft({ apiParameterValue: event.target.value })}
                    />
                  </Space>
                ) : null}
                {datasourceWizardStep === 1 && isApiDatasourceConnector ? (
                  <Input.TextArea
                    size="small"
                    aria-label="BI API请求体模板"
                    value={datasourceDraft.apiBodyTemplate ?? ''}
                    placeholder='{"campaign":"{{campaignId}}"}'
                    autoSize={{ minRows: 2, maxRows: 4 }}
                    onChange={event => updateDatasourceDraft({ apiBodyTemplate: event.target.value })}
                  />
                ) : null}
                {hasApiDatasourceOnboarding ? (
                  <Space direction="vertical" size={4} style={{ width: '100%' }}>
                    {datasourceApiPreviewVariableDrafts.map((draft, index) => (
                      <Space.Compact key={`api-preview-variable-${index}`} style={{ width: '100%' }}>
                        <Input
                          size="small"
                          aria-label={index === 0 ? 'BI API预览变量名' : `BI API预览变量名 ${index + 1}`}
                          value={draft.name}
                          placeholder="变量名"
                          onChange={event => updateDatasourceApiPreviewVariable(index, { name: event.target.value })}
                        />
                        <Input
                          size="small"
                          aria-label={index === 0 ? 'BI API预览变量值' : `BI API预览变量值 ${index + 1}`}
                          value={draft.value}
                          placeholder="变量值"
                          onChange={event => updateDatasourceApiPreviewVariable(index, { value: event.target.value })}
                        />
                        {index === datasourceApiPreviewVariableDrafts.length - 1 ? (
                          <Tooltip title="添加变量">
                            <Button
                              size="small"
                              aria-label="新增BI API预览变量"
                              icon={<PlusOutlined />}
                              disabled={datasourceApiPreviewVariableDrafts.length >= MAX_API_PREVIEW_VARIABLES}
                              onClick={addDatasourceApiPreviewVariable}
                            />
                          </Tooltip>
                        ) : null}
                        <Tooltip title="删除变量">
                          <Button
                            size="small"
                            aria-label={`删除BI API预览变量 ${index + 1}`}
                            icon={<DeleteOutlined />}
                            disabled={datasourceApiPreviewVariableDrafts.length <= 1}
                            onClick={() => removeDatasourceApiPreviewVariable(index)}
                          />
                        </Tooltip>
                      </Space.Compact>
                    ))}
                    <Input
                      size="small"
                      aria-label="BI API预览行数"
                      value={datasourceApiPreviewLimit}
                      placeholder="行数"
                      style={{ maxWidth: 120 }}
                      onChange={event => setDatasourceApiPreviewLimit(event.target.value)}
                    />
                  </Space>
                ) : null}
              </Space>
              <Table
                rowKey="key"
                size="small"
                pagination={false}
                columns={[
                  { title: '数据源', dataIndex: 'source', key: 'source', width: 180, ellipsis: true },
                  { title: '连接器', dataIndex: 'connector', key: 'connector', width: 110 },
                  { title: '连接', dataIndex: 'connection', key: 'connection', width: 160, ellipsis: true },
                  { title: '凭证', dataIndex: 'credential', key: 'credential', ellipsis: true },
                  {
                    title: '操作',
                    key: 'actions',
                    width: 300,
                    render: (_: unknown, row: { id?: number | null; connector?: string }) => {
                      const isApiDatasource = String(row.connector ?? '').startsWith('API /')
                      return (
                        <Space.Compact>
                          <Tooltip title="连接测试">
                            <Button
                              size="small"
                              icon={<PlayCircleOutlined />}
                              loading={testingDatasourceId === row.id}
                              disabled={!row.id}
                              onClick={() => runDatasourceConnectionTest(row.id)}
                            />
                          </Tooltip>
                          {isApiDatasource ? (
                            <Tooltip title="预览数据">
                              <Button
                                size="small"
                                aria-label="预览数据"
                                icon={<EyeOutlined />}
                                loading={previewingApiDatasourceId === row.id}
                                disabled={!row.id}
                                onClick={() => previewApiDatasource(row.id)}
                              />
                            </Tooltip>
                          ) : null}
                          <Tooltip title="预览 schema">
                            <Button
                              size="small"
                              icon={<DatabaseOutlined />}
                              loading={previewingDatasourceId === row.id}
                              disabled={!row.id}
                              onClick={() => previewDatasourceSchema(row.id)}
                            />
                          </Tooltip>
                          <Tooltip title="同步 schema">
                            <Button
                              size="small"
                              aria-label="同步 schema"
                              title="同步 schema"
                              icon={<SyncOutlined />}
                              loading={syncingDatasourceId === row.id}
                              disabled={!row.id}
                              onClick={() => syncDatasourceSchema(row.id)}
                            >
                              同步 schema
                            </Button>
                          </Tooltip>
                          <Tooltip title="轮换凭证">
                            <Button
                              size="small"
                              aria-label="轮换凭证"
                              icon={<LockOutlined />}
                              loading={rotatingDatasourceId === row.id}
                              disabled={!row.id}
                              onClick={() => openDatasourceCredentialRotation(row.id)}
                            />
                          </Tooltip>
                          <Tooltip title="编辑数据源">
                            <Button
                              size="small"
                              aria-label="编辑数据源"
                              icon={<EditOutlined />}
                              disabled={!row.id || savingDatasourceOnboarding}
                              onClick={() => editDatasourceOnboarding(row.id)}
                            />
                          </Tooltip>
                        </Space.Compact>
                      )
                    },
                  },
                ]}
                dataSource={datasourceOnboardingTableRows}
                locale={{ emptyText: '暂无已接入数据源' }}
                scroll={{ x: 800 }}
              />
              <Table
                rowKey="key"
                size="small"
                pagination={false}
                columns={[
                  { title: '数据源', dataIndex: 'source', key: 'source', width: 180, ellipsis: true },
                  { title: '就绪状态', dataIndex: 'readiness', key: 'readiness', width: 130 },
                  { title: '下一步', dataIndex: 'nextAction', key: 'nextAction', width: 240, ellipsis: true },
                  { title: '限制', dataIndex: 'limitations', key: 'limitations', ellipsis: true },
                ]}
                dataSource={datasourceNextActionTableRows}
                locale={{ emptyText: '暂无数据源后续动作' }}
                scroll={{ x: 860 }}
              />
              <Modal
                title={`轮换凭证${credentialRotationDatasource ? ` · ${credentialRotationDatasource.name}` : ''}`}
                open={credentialRotationDatasourceId !== null}
                okText="轮换"
                cancelText="取消"
                confirmLoading={rotatingDatasourceId === credentialRotationDatasourceId}
                okButtonProps={{ disabled: !credentialRotationPassword.trim() }}
                onOk={rotateDatasourceCredential}
                onCancel={cancelDatasourceCredentialRotation}
              >
                <Space direction="vertical" size={8} style={{ width: '100%' }}>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {credentialRotationDatasource?.sourceKey ?? `jdbc-${credentialRotationDatasourceId ?? '-'}`}
                  </Text>
                  <Input.Password
                    aria-label="数据源新密码"
                    autoComplete="new-password"
                    value={credentialRotationPassword}
                    placeholder="输入新的数据源密码"
                    onChange={event => setCredentialRotationPassword(event.target.value)}
                    onPressEnter={rotateDatasourceCredential}
                  />
                </Space>
              </Modal>
              {datasourceConnectionTestResult ? (
                <Descriptions
                  size="small"
                  column={1}
                  items={datasourceConnectionTestDisplayRows.map(row => ({
                    key: row.label,
                    label: row.label,
                    children: row.value,
                  }))}
                />
              ) : null}
              {datasourceSchemaPreview ? (
                <Table
                  rowKey="key"
                  size="small"
                  pagination={false}
                  columns={[
                    { title: '表', dataIndex: 'table', key: 'table', width: 150 },
                    { title: '字段数', dataIndex: 'columnCount', key: 'columnCount', width: 80 },
                    { title: '字段', dataIndex: 'columns', key: 'columns', ellipsis: true },
                  ]}
                  dataSource={datasourceSchemaPreviewTableRows}
                  locale={{ emptyText: '暂无 schema 预览' }}
                  scroll={{ x: 620 }}
                />
              ) : null}
              {datasourceApiPreview ? (
                <Space direction="vertical" size={8} style={{ width: '100%' }}>
                  <Descriptions
                    size="small"
                    column={1}
                    items={datasourceApiPreviewDisplayRows.map(row => ({
                      key: row.label,
                      label: row.label,
                      children: row.value,
                    }))}
                  />
                  <Table
                    rowKey="__rowKey"
                    size="small"
                    pagination={false}
                    columns={datasourceApiPreviewTableColumns}
                    dataSource={datasourceApiPreviewTableRows}
                    locale={{ emptyText: '暂无 API 预览数据' }}
                    scroll={{ x: Math.max(620, datasourceApiPreviewTableColumns.length * 140) }}
                  />
                </Space>
              ) : null}
              {datasourceSchemaSnapshot ? (
                <Descriptions
                  size="small"
                  column={1}
                  items={datasourceSchemaSnapshotDisplayRows.map(row => ({
                    key: row.label,
                    label: row.label,
                    children: row.value,
                  }))}
                />
              ) : null}
              <Space
                direction="vertical"
                size={10}
                style={{
                  width: '100%',
                  padding: 12,
                  border: '1px solid #e5e7eb',
                  borderRadius: 8,
                  background: '#fff',
                }}
              >
                <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
                  <Space size={8} align="center">
                    <DatabaseOutlined />
                    <Text strong>SQL 数据集</Text>
                    <Tag color={sqlDatasetDataSourceConfigId ? 'blue' : 'default'}>
                      {sqlDatasetDataSourceConfigId ? `datasource #${sqlDatasetDataSourceConfigId}` : '未绑定数据源'}
                    </Tag>
                    <Tag color={sqlDatasetParameterDrafts.length > 0 ? 'volcano' : 'default'}>
                      {sqlDatasetParameterDrafts.length} 参数
                    </Tag>
                  </Space>
                  <Space size={8}>
                    <Button
                      size="small"
                      aria-label="预览 SQL 数据集"
                      icon={<PlayCircleOutlined />}
                      loading={previewingSqlDataset}
                      disabled={!sqlDatasetReady}
                      onClick={previewSqlDataset}
                    >
                      预览与血缘
                    </Button>
                    <Button
                      size="small"
                      type="primary"
                      aria-label="保存 SQL 数据集"
                      icon={<SaveOutlined />}
                      loading={savingSqlDatasetDraft}
                      disabled={!sqlDatasetReady}
                      onClick={saveSqlDatasetDraft}
                    >
                      保存 SQL 数据集
                    </Button>
                  </Space>
                </Space>
                <Row gutter={[8, 8]}>
                  <Col xs={24} md={8}>
                    <Input
                      size="small"
                      aria-label="BI SQL数据集Key"
                      value={sqlDatasetKey}
                      placeholder="dataset_key"
                      onChange={event => setSqlDatasetKey(event.target.value)}
                    />
                  </Col>
                  <Col xs={24} md={8}>
                    <Input
                      size="small"
                      aria-label="BI SQL数据集名称"
                      value={sqlDatasetName}
                      placeholder="数据集名称"
                      onChange={event => setSqlDatasetName(event.target.value)}
                    />
                  </Col>
                  <Col xs={24} md={8}>
                    <Input
                      size="small"
                      aria-label="BI SQL租户字段"
                      value={sqlDatasetTenantColumn}
                      placeholder="tenant_id"
                      onChange={event => setSqlDatasetTenantColumn(event.target.value)}
                    />
                  </Col>
                  <Col xs={24}>
                    <Input.TextArea
                      aria-label="BI SQL模板"
                      value={sqlDatasetTemplate}
                      autoSize={{ minRows: 4, maxRows: 8 }}
                      placeholder="SELECT tenant_id, stat_date, channel, total_cost FROM campaign_daily WHERE stat_date >= {{start_date}}"
                      onChange={event => setSqlDatasetTemplate(event.target.value)}
                    />
                  </Col>
                  {sqlDatasetParameterDrafts.map(parameter => (
                    <Col xs={24} key={parameter.key}>
                      <Space direction="vertical" size={6} style={{ width: '100%' }}>
                        <Space size={8} wrap>
                          <Tag color="geekblue">{parameter.key}</Tag>
                          <Select
                            size="small"
                            aria-label={`SQL参数${parameter.key}类型`}
                            value={parameter.dataType}
                            style={{ width: 120 }}
                            options={['STRING', 'NUMBER', 'DATE', 'DATETIME', 'BOOLEAN', 'PERCENT']
                              .map(dataType => ({ label: dataType, value: dataType }))}
                            onChange={value => updateSqlDatasetParameter(parameter.key, { dataType: value })}
                          />
                          <Switch
                            size="small"
                            aria-label={`SQL参数${parameter.key}必填`}
                            checked={parameter.required}
                            onChange={checked => updateSqlDatasetParameter(parameter.key, { required: checked })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL参数${parameter.key}默认值`}
                            value={parameter.defaultValue}
                            placeholder="默认值"
                            style={{ width: 150 }}
                            onChange={event => updateSqlDatasetParameter(parameter.key, { defaultValue: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL参数${parameter.key}允许值`}
                            value={parameter.allowedValuesText}
                            placeholder="允许值"
                            style={{ width: 180 }}
                            onChange={event => updateSqlDatasetParameter(parameter.key, { allowedValuesText: event.target.value })}
                          />
                        </Space>
                      </Space>
                    </Col>
                  ))}
                  <Col xs={24}>
                    <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
                      <Text strong>字段编辑</Text>
                      <Button size="small" icon={<PlusOutlined />} onClick={addSqlDatasetField}>添加字段</Button>
                    </Space>
                  </Col>
                  {sqlDatasetFields.map((field, index) => (
                    <Col xs={24} key={`sql-field-${index}`}>
                      <Space direction="vertical" size={6} style={{ width: '100%', padding: 8, border: '1px solid #eef2f7', borderRadius: 6 }}>
                        <Space size={8} wrap>
                          <Input
                            size="small"
                            aria-label={`SQL字段${index}Key`}
                            value={field.fieldKey ?? ''}
                            placeholder="field_key"
                            style={{ width: 140 }}
                            onChange={event => updateSqlDatasetField(index, { fieldKey: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL字段${index}显示名`}
                            value={field.displayName ?? ''}
                            placeholder="显示名"
                            style={{ width: 140 }}
                            onChange={event => updateSqlDatasetField(index, { displayName: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL字段${index}表达式`}
                            value={field.columnExpression ?? ''}
                            placeholder="column_expression"
                            style={{ width: 170 }}
                            onChange={event => updateSqlDatasetField(index, { columnExpression: event.target.value })}
                          />
                          <Select
                            size="small"
                            aria-label={`SQL字段${index}角色`}
                            value={(field.role ?? 'DIMENSION') as string}
                            style={{ width: 120 }}
                            options={[
                              { label: '维度', value: 'DIMENSION' },
                              { label: '度量', value: 'MEASURE' },
                            ]}
                            onChange={value => updateSqlDatasetField(index, { role: value })}
                          />
                          <Select
                            size="small"
                            aria-label={`SQL字段${index}类型`}
                            value={(field.dataType ?? 'STRING') as string}
                            style={{ width: 120 }}
                            options={['STRING', 'NUMBER', 'DATE', 'DATETIME', 'BOOLEAN', 'PERCENT']
                              .map(dataType => ({ label: dataType, value: dataType }))}
                            onChange={value => updateSqlDatasetField(index, { dataType: value })}
                          />
                          <Switch
                            size="small"
                            aria-label={`SQL字段${index}可见`}
                            checked={field.visible !== false}
                            onChange={checked => updateSqlDatasetField(index, { visible: checked })}
                          />
                          <Button
                            size="small"
                            aria-label={`删除SQL字段${index}`}
                            icon={<DeleteOutlined />}
                            disabled={sqlDatasetFields.length <= 1}
                            onClick={() => removeSqlDatasetField(index)}
                          />
                        </Space>
                        <Space size={8} wrap>
                          <Input
                            size="small"
                            aria-label={`SQL字段${index}语义类型`}
                            value={field.semanticType ?? ''}
                            placeholder="语义类型"
                            style={{ width: 130 }}
                            onChange={event => updateSqlDatasetField(index, { semanticType: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL字段${index}默认聚合`}
                            value={field.defaultAggregation ?? ''}
                            placeholder="默认聚合"
                            style={{ width: 120 }}
                            onChange={event => updateSqlDatasetField(index, { defaultAggregation: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL字段${index}格式`}
                            value={field.formatPattern ?? ''}
                            placeholder="格式"
                            style={{ width: 120 }}
                            onChange={event => updateSqlDatasetField(index, { formatPattern: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL字段${index}单位`}
                            value={field.unit ?? ''}
                            placeholder="单位"
                            style={{ width: 90 }}
                            onChange={event => updateSqlDatasetField(index, { unit: event.target.value })}
                          />
                          <Select
                            size="small"
                            aria-label={`SQL字段${index}敏感级别`}
                            value={(field.sensitiveLevel ?? 'NORMAL') as string}
                            style={{ width: 120 }}
                            options={['NORMAL', 'SENSITIVE', 'CONFIDENTIAL']
                              .map(level => ({ label: level, value: level }))}
                            onChange={value => updateSqlDatasetField(index, { sensitiveLevel: value })}
                          />
                        </Space>
                      </Space>
                    </Col>
                  ))}
                  <Col xs={24}>
                    <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
                      <Text strong>指标编辑</Text>
                      <Button size="small" icon={<PlusOutlined />} onClick={addSqlDatasetMetric}>添加指标</Button>
                    </Space>
                  </Col>
                  {sqlDatasetMetrics.map((metric, index) => (
                    <Col xs={24} key={`sql-metric-${index}`}>
                      <Space direction="vertical" size={6} style={{ width: '100%', padding: 8, border: '1px solid #eef2f7', borderRadius: 6 }}>
                        <Space size={8} wrap>
                          <Input
                            size="small"
                            aria-label={`SQL指标${index}Key`}
                            value={metric.metricKey ?? ''}
                            placeholder="metric_key"
                            style={{ width: 140 }}
                            onChange={event => updateSqlDatasetMetric(index, { metricKey: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL指标${index}显示名`}
                            value={metric.displayName ?? ''}
                            placeholder="显示名"
                            style={{ width: 140 }}
                            onChange={event => updateSqlDatasetMetric(index, { displayName: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL指标${index}表达式`}
                            value={metric.expression ?? ''}
                            placeholder="SUM(amount)"
                            style={{ width: 180 }}
                            onChange={event => updateSqlDatasetMetric(index, { expression: event.target.value })}
                          />
                          <Select
                            size="small"
                            aria-label={`SQL指标${index}聚合`}
                            value={(metric.aggregation ?? 'SUM') as string}
                            style={{ width: 110 }}
                            options={['SUM', 'COUNT', 'COUNT_DISTINCT', 'AVG', 'MIN', 'MAX', 'CUSTOM']
                              .map(aggregation => ({ label: aggregation, value: aggregation }))}
                            onChange={value => updateSqlDatasetMetric(index, { aggregation: value })}
                          />
                          <Select
                            size="small"
                            aria-label={`SQL指标${index}类型`}
                            value={(metric.dataType ?? 'NUMBER') as string}
                            style={{ width: 120 }}
                            options={['NUMBER', 'PERCENT', 'STRING']
                              .map(dataType => ({ label: dataType, value: dataType }))}
                            onChange={value => updateSqlDatasetMetric(index, { dataType: value })}
                          />
                          <Button
                            size="small"
                            aria-label={`删除SQL指标${index}`}
                            icon={<DeleteOutlined />}
                            disabled={sqlDatasetMetrics.length <= 1}
                            onClick={() => removeSqlDatasetMetric(index)}
                          />
                        </Space>
                        <Space size={8} wrap>
                          <Input
                            size="small"
                            aria-label={`SQL指标${index}允许维度`}
                            value={metric.allowedDimensionsText ?? ''}
                            placeholder="stat_date,channel"
                            style={{ width: 180 }}
                            onChange={event => updateSqlDatasetMetric(index, { allowedDimensionsText: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL指标${index}单位`}
                            value={metric.unit ?? ''}
                            placeholder="单位"
                            style={{ width: 90 }}
                            onChange={event => updateSqlDatasetMetric(index, { unit: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL指标${index}格式`}
                            value={metric.formatPattern ?? ''}
                            placeholder="格式"
                            style={{ width: 120 }}
                            onChange={event => updateSqlDatasetMetric(index, { formatPattern: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL指标${index}负责人`}
                            value={metric.owner ?? ''}
                            placeholder="负责人"
                            style={{ width: 110 }}
                            onChange={event => updateSqlDatasetMetric(index, { owner: event.target.value })}
                          />
                          <Input
                            size="small"
                            aria-label={`SQL指标${index}描述`}
                            value={metric.description ?? ''}
                            placeholder="描述"
                            style={{ width: 180 }}
                            onChange={event => updateSqlDatasetMetric(index, { description: event.target.value })}
                          />
                        </Space>
                      </Space>
                    </Col>
                  ))}
                  <Col xs={24}>
                    <Space size={[4, 4]} wrap>
                      {sqlDatasetDraftResource.fields.map(field => (
                        <Tag key={field.fieldKey}>{field.fieldKey} · {field.dataType}</Tag>
                      ))}
                      {sqlDatasetDraftResource.metrics.map(metric => (
                        <Tag color="blue" key={metric.metricKey}>{metric.metricKey} · {metric.aggregation}</Tag>
                      ))}
                    </Space>
                  </Col>
                  <Col xs={24}>
                    <SqlDatasetReadinessSummary rows={sqlDatasetReadinessRows} />
                  </Col>
                  {sqlDatasetPreviewError && (
                    <Col xs={24}>
                      <Text type="danger" style={{ fontSize: 12 }}>{sqlDatasetPreviewError}</Text>
                    </Col>
                  )}
                  {sqlDatasetPreview && (
                    <Col xs={24}>
                      <Space direction="vertical" size={8} style={{ width: '100%' }}>
                        <Space size={[4, 4]} wrap>
                          <Tag color={sqlDatasetPreview.sampleExecuted ? 'green' : 'gold'}>
                            SQL 预览 {sqlDatasetPreview.rowCount} 行
                          </Tag>
                          <Tag color="blue">参数 {sqlDatasetPreview.parameterCount}</Tag>
                          <Tag color={sqlDatasetPreview.lineage?.approvalRequired ? 'volcano' : 'default'}>
                            {sqlDatasetPreview.lineage?.approvalRequired ? '发布需审批' : '无审批门禁'}
                          </Tag>
                          {sqlDatasetPreview.lineage?.dataSourceConfigId && (
                            <Tag color="geekblue">datasource #{sqlDatasetPreview.lineage.dataSourceConfigId}</Tag>
                          )}
                        </Space>
                        <Text code style={{ display: 'block', whiteSpace: 'normal', fontSize: 12 }}>
                          {sqlDatasetPreview.compiledSql}
                        </Text>
                        <Space size={[4, 4]} wrap>
                          {(sqlDatasetPreview.lineage?.sourceTables ?? []).map(table => (
                            <Tag color="purple" key={table}>{table}</Tag>
                          ))}
                          {(sqlDatasetPreview.impact?.governanceGates ?? []).map(gate => (
                            <Tag key={gate}>{gate}</Tag>
                          ))}
                          {(sqlDatasetPreview.impact?.warnings ?? []).map(warning => (
                            <Tag color="orange" key={warning}>{warning}</Tag>
                          ))}
                        </Space>
                        <SqlDatasetImpactSummary result={sqlDatasetPreview} />
                        <SqlDatasetSampleProfileTable result={sqlDatasetPreview} />
                        <SqlDatasetPreviewTable result={sqlDatasetPreview} loading={previewingSqlDataset} />
                      </Space>
                    </Col>
                  )}
                </Row>
              </Space>
              {datasourceSchemaSnapshot?.tables?.length && datasourceSchemaSnapshot.tables.length >= 2 ? (
                <Space
                  direction="vertical"
                  size={10}
                  style={{
                    width: '100%',
                    padding: 12,
                    border: '1px solid #e5e7eb',
                    borderRadius: 8,
                    background: '#fff',
                  }}
                >
                  <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
                    <Space size={8} align="center">
                      <DeploymentUnitOutlined />
                      <Text strong>多表建模</Text>
                      <Tag color={datasourceSchemaSnapshot.syncStatus === 'SUCCESS' ? 'green' : 'default'}>
                        {datasourceSchemaSnapshot.syncStatus}
                      </Tag>
                    </Space>
                    <Space size={8}>
                      <Button
                        size="small"
                        aria-label="添加关联表"
                        icon={<NodeIndexOutlined />}
                        disabled={datasourceSchemaSnapshot.syncStatus !== 'SUCCESS' || datasourceModelingRemainingTableNames.length === 0}
                        onClick={addDatasourceModelingJoin}
                      >
                        添加关联表
                      </Button>
                      <Button
                        size="small"
                        type="primary"
                        aria-label="生成多表数据集"
                        icon={<DeploymentUnitOutlined />}
                        loading={creatingDatasourceMultiTableDataset}
                        disabled={datasourceSchemaSnapshot.syncStatus !== 'SUCCESS' || !datasourceModelingReady}
                        onClick={createMultiTableDatasetFromDatasourceSchema}
                      >
                        生成多表数据集
                      </Button>
                    </Space>
                  </Space>
                  <Row gutter={[8, 8]}>
                    <Col xs={24} md={12}>
                      <Space direction="vertical" size={4} style={{ width: '100%' }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>建模表</Text>
                        <Select
                          size="small"
                          mode="multiple"
                          aria-label="多表建模表"
                          options={datasourceSchemaTableOptions}
                          value={datasourceModelingTableNamesValue}
                          onChange={value => setDatasourceModelingTableNames(value)}
                          style={{ width: '100%' }}
                        />
                      </Space>
                    </Col>
                    <Col xs={24} md={12}>
                      <Space direction="vertical" size={4} style={{ width: '100%' }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>主表</Text>
                        <Select
                          size="small"
                          aria-label="多表建模主表"
                          options={datasourceModelingTableNamesValue.map(tableName => ({ label: tableName, value: tableName }))}
                          value={datasourceModelingBaseTableValue || undefined}
                          onChange={setDatasourceModelingBaseTableName}
                          style={{ width: '100%' }}
                        />
                      </Space>
                    </Col>
                    <Col xs={24}>
                      <Space direction="vertical" size={6} style={{ width: '100%' }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>关系画布</Text>
                        <div style={{ overflowX: 'auto', width: '100%' }}>
                          <div
                            aria-label="多表关系画布"
                            onPointerMove={moveDatasourceModelingGraphDrag}
                            onPointerUp={stopDatasourceModelingGraphDrag}
                            onPointerCancel={stopDatasourceModelingGraphDrag}
                            style={{
                              position: 'relative',
                              width: DATASOURCE_MODELING_GRAPH_WIDTH,
                              height: DATASOURCE_MODELING_GRAPH_HEIGHT,
                              border: '1px solid #dbe4f0',
                              borderRadius: 8,
                              background: '#f8fafc',
                              overflow: 'hidden',
                            }}
                          >
                            <svg
                              aria-hidden="true"
                              width={DATASOURCE_MODELING_GRAPH_WIDTH}
                              height={DATASOURCE_MODELING_GRAPH_HEIGHT}
                              style={{
                                position: 'absolute',
                                inset: 0,
                                pointerEvents: 'none',
                              }}
                            >
                              {effectiveDatasourceModelingJoins.map((join, index) => {
                                const leftNode = datasourceModelingGraphNodesValue.find(node => node.tableName === join.leftTableName)
                                const rightNode = datasourceModelingGraphNodesValue.find(node => node.tableName === join.rightTableName)
                                if (!leftNode || !rightNode) return null
                                return (
                                  <line
                                    key={`${join.leftTableName}-${join.rightTableName}-${index}`}
                                    x1={leftNode.x + DATASOURCE_MODELING_GRAPH_NODE_WIDTH / 2}
                                    y1={leftNode.y + DATASOURCE_MODELING_GRAPH_NODE_HEIGHT / 2}
                                    x2={rightNode.x + DATASOURCE_MODELING_GRAPH_NODE_WIDTH / 2}
                                    y2={rightNode.y + DATASOURCE_MODELING_GRAPH_NODE_HEIGHT / 2}
                                    stroke="#94a3b8"
                                    strokeWidth={2}
                                    strokeDasharray={join.joinType === 'INNER' ? undefined : '5 4'}
                                  />
                                )
                              })}
                            </svg>
                            {effectiveDatasourceModelingJoins.map((join, index) => {
                              const leftNode = datasourceModelingGraphNodesValue.find(node => node.tableName === join.leftTableName)
                              const rightNode = datasourceModelingGraphNodesValue.find(node => node.tableName === join.rightTableName)
                              if (!leftNode || !rightNode) return null
                              const selected = index === selectedDatasourceModelingGraphJoinIndexValue
                              const labelWidth = 128
                              const labelHeight = 28
                              const conditionSummary = datasourceModelingJoinConditionSummary(join.conditions)
                              const leftCenterX = leftNode.x + DATASOURCE_MODELING_GRAPH_NODE_WIDTH / 2
                              const leftCenterY = leftNode.y + DATASOURCE_MODELING_GRAPH_NODE_HEIGHT / 2
                              const rightCenterX = rightNode.x + DATASOURCE_MODELING_GRAPH_NODE_WIDTH / 2
                              const rightCenterY = rightNode.y + DATASOURCE_MODELING_GRAPH_NODE_HEIGHT / 2
                              const labelX = clampDatasourceModelingGraphCoordinate(
                                (leftCenterX + rightCenterX) / 2 - labelWidth / 2,
                                DATASOURCE_MODELING_GRAPH_WIDTH - labelWidth,
                              )
                              const labelY = clampDatasourceModelingGraphCoordinate(
                                (leftCenterY + rightCenterY) / 2 - labelHeight / 2,
                                DATASOURCE_MODELING_GRAPH_HEIGHT - labelHeight,
                              )
                              return (
                                <button
                                  key={`${join.leftTableName}-${join.rightTableName}-${index}-edge`}
                                  type="button"
                                  aria-label={`关系边 ${join.leftTableName} 到 ${join.rightTableName} ${join.joinType} ${conditionSummary}`}
                                  aria-pressed={selected}
                                  onClick={() => setSelectedDatasourceModelingGraphJoinIndex(index)}
                                  style={{
                                    position: 'absolute',
                                    left: labelX,
                                    top: labelY,
                                    width: labelWidth,
                                    height: labelHeight,
                                    padding: '0 8px',
                                    border: `1px solid ${selected ? '#2563eb' : '#cbd5e1'}`,
                                    borderRadius: 8,
                                    background: selected ? '#eff6ff' : '#fff',
                                    color: selected ? '#1d4ed8' : '#334155',
                                    cursor: 'pointer',
                                    fontSize: 11,
                                    fontWeight: 600,
                                    lineHeight: '26px',
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap',
                                    boxShadow: '0 1px 2px rgba(15, 23, 42, 0.10)',
                                  }}
                                >
                                  {join.joinType} · {conditionSummary}
                                </button>
                              )
                            })}
                            {datasourceModelingGraphNodesValue.map(node => (
                              <button
                                key={node.tableName}
                                type="button"
                                aria-label={`关系节点 ${node.tableName}`}
                                onPointerDown={event => startDatasourceModelingGraphDrag(event, node)}
                                style={{
                                  position: 'absolute',
                                  left: node.x,
                                  top: node.y,
                                  width: DATASOURCE_MODELING_GRAPH_NODE_WIDTH,
                                  height: DATASOURCE_MODELING_GRAPH_NODE_HEIGHT,
                                  padding: '6px 10px',
                                  border: `1px solid ${node.tableName === datasourceModelingBaseTableValue ? '#2563eb' : '#cbd5e1'}`,
                                  borderRadius: 8,
                                  background: '#fff',
                                  color: '#111827',
                                  cursor: 'grab',
                                  display: 'grid',
                                  gap: 2,
                                  textAlign: 'left',
                                  boxShadow: '0 1px 2px rgba(15, 23, 42, 0.08)',
                                  userSelect: 'none',
                                }}
                              >
                                <span style={{ fontSize: 12, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                  {node.alias}
                                </span>
                                <span style={{ fontSize: 11, color: '#64748b', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                  {node.tableName}
                                </span>
                              </button>
                            ))}
                          </div>
                        </div>
                        {selectedDatasourceModelingGraphJoin ? (
                          <Space
                            direction="vertical"
                            size={6}
                            style={{
                              width: '100%',
                              padding: '6px 8px',
                              border: '1px solid #dbe4f0',
                              borderRadius: 8,
                              background: '#fff',
                            }}
                          >
                            <Space align="center" size={8} wrap style={{ width: '100%', justifyContent: 'space-between' }}>
                              <Text type="secondary" style={{ fontSize: 12 }}>
                                已选关系 {selectedDatasourceModelingGraphJoin.leftTableName} → {selectedDatasourceModelingGraphJoin.rightTableName}
                                {' · '}
                                {selectedDatasourceModelingGraphJoin.joinType}
                                {' · '}
                                {selectedDatasourceModelingGraphJoin.conditions.length} 个条件
                              </Text>
                              <Space size={6} wrap>
                                <Select
                                  size="small"
                                  aria-label={`从画布设置左表 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName}`}
                                  options={datasourceModelingTableNamesValue
                                    .filter(tableName => tableName !== selectedDatasourceModelingGraphJoin.rightTableName)
                                    .map(tableName => ({ label: tableName, value: tableName }))}
                                  value={selectedDatasourceModelingGraphJoin.leftTableName || undefined}
                                  onChange={(value: string) => updateDatasourceModelingJoin(selectedDatasourceModelingGraphJoinIndexValue, { leftTableName: value })}
                                  style={{ width: 144 }}
                                />
                                <Select
                                  size="small"
                                  aria-label={`从画布设置右表 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName}`}
                                  options={datasourceModelingTableNamesValue
                                    .filter(tableName => tableName !== selectedDatasourceModelingGraphJoin.leftTableName)
                                    .map(tableName => ({ label: tableName, value: tableName }))}
                                  value={selectedDatasourceModelingGraphJoin.rightTableName || undefined}
                                  onChange={(value: string) => updateDatasourceModelingJoin(selectedDatasourceModelingGraphJoinIndexValue, { rightTableName: value })}
                                  style={{ width: 144 }}
                                />
                                <Select
                                  size="small"
                                  aria-label={`从画布设置 Join 类型 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName}`}
                                  options={['LEFT', 'INNER', 'RIGHT', 'FULL'].map(joinType => ({ label: joinType, value: joinType }))}
                                  value={selectedDatasourceModelingGraphJoin.joinType}
                                  onChange={(value: string) => updateDatasourceModelingJoin(selectedDatasourceModelingGraphJoinIndexValue, { joinType: value })}
                                  style={{ width: 96 }}
                                />
                                <Button
                                  size="small"
                                  icon={<PlusOutlined />}
                                  aria-label={`从画布添加关联条件 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName}`}
                                  onClick={addSelectedDatasourceModelingGraphJoinCondition}
                                >
                                  添加条件
                                </Button>
                                <Button
                                  size="small"
                                  icon={<PlusOutlined />}
                                  aria-label={`从画布添加全部同名字段条件 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName}`}
                                  onClick={addSelectedDatasourceModelingGraphJoinCommonConditions}
                                >
                                  同名字段
                                </Button>
                                <Button
                                  size="small"
                                  icon={<SwapOutlined />}
                                  aria-label={`从画布交换关联方向 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName}`}
                                  onClick={swapSelectedDatasourceModelingGraphJoinDirection}
                                >
                                  交换方向
                                </Button>
                              </Space>
                            </Space>
                            <Space size={6} wrap>
                              {selectedDatasourceModelingGraphJoin.conditions.map((condition, conditionIndex) => {
                                const leftColumnOptions = datasourceModelingColumnOptions(selectedDatasourceModelingGraphJoin.leftTableName)
                                const rightColumnOptions = datasourceModelingColumnOptions(selectedDatasourceModelingGraphJoin.rightTableName)
                                return (
                                  <Space
                                    key={`${condition.leftColumn}-${condition.operator}-${condition.rightColumn}-${conditionIndex}`}
                                    size={4}
                                    style={{
                                      padding: '2px 6px',
                                      border: '1px solid #e2e8f0',
                                      borderRadius: 6,
                                      background: '#f8fafc',
                                    }}
                                  >
                                    {conditionIndex > 0 ? (
                                      <select
                                        aria-label={`从画布设置连接符 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName} 条件 ${conditionIndex + 1}`}
                                        value={condition.connector ?? 'AND'}
                                        onChange={event => updateDatasourceModelingJoinCondition(
                                          selectedDatasourceModelingGraphJoinIndexValue,
                                          conditionIndex,
                                          { connector: event.target.value as 'AND' | 'OR' },
                                        )}
                                        style={{
                                          width: 70,
                                          height: 24,
                                          border: '1px solid #d9d9d9',
                                          borderRadius: 6,
                                          background: '#fff',
                                          fontSize: 12,
                                        }}
                                      >
                                        {DATASOURCE_MODELING_JOIN_CONDITION_CONNECTORS.map(connector => (
                                          <option key={connector} value={connector}>{connector}</option>
                                        ))}
                                      </select>
                                    ) : null}
                                    <Select
                                      size="small"
                                      aria-label={`从画布设置左字段 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName} 条件 ${conditionIndex + 1}`}
                                      options={leftColumnOptions}
                                      value={condition.leftColumn || undefined}
                                      onChange={(value: string) => updateDatasourceModelingJoinCondition(selectedDatasourceModelingGraphJoinIndexValue, conditionIndex, { leftColumn: value })}
                                      style={{ width: 132 }}
                                    />
                                    <Select
                                      size="small"
                                      aria-label={`从画布设置操作符 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName} 条件 ${conditionIndex + 1}`}
                                      options={DATASOURCE_MODELING_JOIN_CONDITION_OPERATORS.map(operator => ({ label: operator, value: operator }))}
                                      value={condition.operator}
                                      onChange={(value: DatasourceModelingJoinConditionOperator) => updateDatasourceModelingJoinCondition(selectedDatasourceModelingGraphJoinIndexValue, conditionIndex, { operator: value })}
                                      style={{ width: 72 }}
                                    />
                                    <Select
                                      size="small"
                                      aria-label={`从画布设置右字段 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName} 条件 ${conditionIndex + 1}`}
                                      options={rightColumnOptions}
                                      value={condition.rightColumn || undefined}
                                      onChange={(value: string) => updateDatasourceModelingJoinCondition(selectedDatasourceModelingGraphJoinIndexValue, conditionIndex, { rightColumn: value })}
                                      style={{ width: 132 }}
                                    />
                                    <Checkbox
                                      aria-label={`从画布设置左括号 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName} 条件 ${conditionIndex + 1}`}
                                      checked={condition.groupStart === true}
                                      onChange={event => updateDatasourceModelingJoinCondition(
                                        selectedDatasourceModelingGraphJoinIndexValue,
                                        conditionIndex,
                                        { groupStart: event.target.checked },
                                      )}
                                    >
                                      (
                                    </Checkbox>
                                    <Checkbox
                                      aria-label={`从画布设置右括号 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName} 条件 ${conditionIndex + 1}`}
                                      checked={condition.groupEnd === true}
                                      onChange={event => updateDatasourceModelingJoinCondition(
                                        selectedDatasourceModelingGraphJoinIndexValue,
                                        conditionIndex,
                                        { groupEnd: event.target.checked },
                                      )}
                                    >
                                      )
                                    </Checkbox>
                                    {selectedDatasourceModelingGraphJoin.conditions.length > 1 ? (
                                      <Button
                                        size="small"
                                        type="text"
                                        icon={<DeleteOutlined />}
                                        aria-label={`从画布移除关联条件 ${selectedDatasourceModelingGraphJoin.leftTableName} 到 ${selectedDatasourceModelingGraphJoin.rightTableName} 条件 ${conditionIndex + 1}`}
                                        onClick={() => removeSelectedDatasourceModelingGraphJoinCondition(conditionIndex)}
                                      />
                                    ) : null}
                                  </Space>
                                )
                              })}
                            </Space>
                          </Space>
                        ) : null}
                        <DatasourceRelationshipDiagnosticSummary rows={datasourceRelationshipDiagnosticRows} />
                      </Space>
                    </Col>
                    {effectiveDatasourceModelingJoins.map((join, index) => {
                      const leftColumnOptions = datasourceModelingColumnOptions(join.leftTableName)
                      const rightColumnOptions = datasourceModelingColumnOptions(join.rightTableName)
                      const labelSuffix = index === 0 ? '' : ` ${index + 1}`
                      return (
                        <Col xs={24} key={`${join.leftTableName}-${join.rightTableName}-${index}`}>
                          <Space direction="vertical" size={8} style={{ width: '100%' }}>
                            <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
                              <Text type="secondary" style={{ fontSize: 12 }}>关联 {index + 1}</Text>
                              {effectiveDatasourceModelingJoins.length > 1 ? (
                                <Tooltip title="移除关联">
                                  <Button
                                    size="small"
                                    aria-label={`移除关联 ${index + 1}`}
                                    icon={<DeleteOutlined />}
                                    onClick={() => removeDatasourceModelingJoin(index)}
                                  />
                                </Tooltip>
                              ) : null}
                            </Space>
                            <Row gutter={[8, 8]}>
                              <Col xs={24} md={8}>
                                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                                  <Text type="secondary" style={{ fontSize: 12 }}>Join</Text>
                                  <Select
                                    size="small"
                                    aria-label={`多表建模 Join 类型${labelSuffix}`}
                                    options={['LEFT', 'INNER', 'RIGHT', 'FULL'].map(joinType => ({ label: joinType, value: joinType }))}
                                    value={join.joinType}
                                    onChange={(value: string) => updateDatasourceModelingJoin(index, { joinType: value })}
                                    style={{ width: '100%' }}
                                  />
                                </Space>
                              </Col>
                              <Col xs={24} md={8}>
                                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                                  <Text type="secondary" style={{ fontSize: 12 }}>左表</Text>
                                  <Select
                                    size="small"
                                    aria-label={`多表建模左表${labelSuffix}`}
                                    options={datasourceModelingTableNamesValue.map(tableName => ({ label: tableName, value: tableName }))}
                                    value={join.leftTableName || undefined}
                                    onChange={(value: string) => updateDatasourceModelingJoin(index, { leftTableName: value })}
                                    style={{ width: '100%' }}
                                  />
                                </Space>
                              </Col>
                              <Col xs={24} md={8}>
                                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                                  <Text type="secondary" style={{ fontSize: 12 }}>右表</Text>
                                  <Select
                                    size="small"
                                    aria-label={`多表建模右表${labelSuffix}`}
                                    options={datasourceModelingTableNamesValue
                                      .filter(tableName => tableName !== join.leftTableName)
                                      .map(tableName => ({ label: tableName, value: tableName }))}
                                    value={join.rightTableName || undefined}
                                    onChange={(value: string) => updateDatasourceModelingJoin(index, { rightTableName: value })}
                                    style={{ width: '100%' }}
                                  />
                                </Space>
                              </Col>
                              <Col xs={24}>
                                <Space direction="vertical" size={6} style={{ width: '100%' }}>
                                  <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
                                    <Text type="secondary" style={{ fontSize: 12 }}>关联条件</Text>
                                    <Tooltip title="添加关联条件">
                                      <Button
                                        size="small"
                                        aria-label={`添加关联条件${labelSuffix}`}
                                        icon={<PlusOutlined />}
                                        onClick={() => addDatasourceModelingJoinCondition(index)}
                                      />
                                    </Tooltip>
                                  </Space>
                                  {join.conditions.map((condition, conditionIndex) => {
                                    const conditionSuffix = `${labelSuffix}${conditionIndex === 0 ? '' : ` 条件 ${conditionIndex + 1}`}`
                                    return (
                                      <Row gutter={[8, 8]} key={`${condition.leftColumn}-${condition.operator}-${condition.rightColumn}-${conditionIndex}`} align="middle">
                                        <Col xs={24} md={8}>
                                          <Select
                                            size="small"
                                            aria-label={`多表建模左字段${conditionSuffix}`}
                                            options={leftColumnOptions}
                                            value={condition.leftColumn || undefined}
                                            onChange={(value: string) => updateDatasourceModelingJoinCondition(index, conditionIndex, { leftColumn: value })}
                                            style={{ width: '100%' }}
                                          />
                                        </Col>
                                        <Col xs={24} md={4}>
                                          <Select
                                            size="small"
                                            aria-label={`多表建模操作符${conditionSuffix}`}
                                            options={DATASOURCE_MODELING_JOIN_CONDITION_OPERATORS.map(operator => ({ label: operator, value: operator }))}
                                            value={condition.operator}
                                            onChange={(value: DatasourceModelingJoinConditionOperator) => updateDatasourceModelingJoinCondition(index, conditionIndex, { operator: value })}
                                            style={{ width: '100%' }}
                                          />
                                        </Col>
                                        <Col xs={24} md={8}>
                                          <Select
                                            size="small"
                                            aria-label={`多表建模右字段${conditionSuffix}`}
                                            options={rightColumnOptions}
                                            value={condition.rightColumn || undefined}
                                            onChange={(value: string) => updateDatasourceModelingJoinCondition(index, conditionIndex, { rightColumn: value })}
                                            style={{ width: '100%' }}
                                          />
                                        </Col>
                                        <Col xs={12} md={1}>
                                          <Checkbox
                                            aria-label={`多表建模左括号${conditionSuffix}`}
                                            checked={condition.groupStart === true}
                                            onChange={event => updateDatasourceModelingJoinCondition(index, conditionIndex, { groupStart: event.target.checked })}
                                          >
                                            (
                                          </Checkbox>
                                        </Col>
                                        <Col xs={12} md={1}>
                                          <Checkbox
                                            aria-label={`多表建模右括号${conditionSuffix}`}
                                            checked={condition.groupEnd === true}
                                            onChange={event => updateDatasourceModelingJoinCondition(index, conditionIndex, { groupEnd: event.target.checked })}
                                          >
                                            )
                                          </Checkbox>
                                        </Col>
                                        <Col xs={24} md={2}>
                                          {join.conditions.length > 1 ? (
                                            <Tooltip title="移除关联条件">
                                              <Button
                                                size="small"
                                                aria-label={`移除关联条件${conditionSuffix}`}
                                                icon={<DeleteOutlined />}
                                                onClick={() => removeDatasourceModelingJoinCondition(index, conditionIndex)}
                                              />
                                            </Tooltip>
                                          ) : null}
                                        </Col>
                                      </Row>
                                    )
                                  })}
                                </Space>
                              </Col>
                            </Row>
                            {index < effectiveDatasourceModelingJoins.length - 1 ? (
                              <Divider style={{ margin: '2px 0' }} />
                            ) : null}
                          </Space>
                        </Col>
                      )
                    })}
                  </Row>
                </Space>
              ) : null}
              {datasourceSchemaSnapshot?.tables?.length ? (
                <Table
                  rowKey="name"
                  size="small"
                  pagination={false}
                  columns={[
                    { title: '表', dataIndex: 'name', key: 'name', width: 150, ellipsis: true },
                    { title: '类型', dataIndex: 'tableType', key: 'tableType', width: 90 },
                    {
                      title: '字段',
                      key: 'columns',
                      ellipsis: true,
                      render: (_: unknown, row: BiDatasourceTablePreview) =>
                        row.columns.map(column => `${column.name} ${column.typeName}`).join(' · ') || '-',
                    },
                    {
                      title: '建模',
                      key: 'modeling',
                      width: 72,
                      render: (_: unknown, row: BiDatasourceTablePreview) => (
                        <Tooltip title="生成数据集草稿">
                          <Button
                            size="small"
                            aria-label={`生成数据集草稿 ${row.name}`}
                            icon={<DeploymentUnitOutlined />}
                            loading={creatingDatasourceDatasetTable === row.name}
                            disabled={datasourceSchemaSnapshot.syncStatus !== 'SUCCESS'}
                            onClick={() => createDatasetFromDatasourceTable(row.name)}
                          />
                        </Tooltip>
                      ),
                    },
                  ]}
                  dataSource={datasourceSchemaSnapshot.tables}
                  locale={{ emptyText: '暂无可建模表' }}
                  scroll={{ x: 760 }}
                />
              ) : null}
              {datasourceSchemaSnapshots.length > 0 ? (
                <Table
                  rowKey="key"
                  size="small"
                  pagination={false}
                  columns={[
                    { title: '数据源', dataIndex: 'source', key: 'source', width: 160, ellipsis: true },
                    { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
                    { title: 'Schema', dataIndex: 'schema', key: 'schema', width: 120 },
                    { title: '同步人', dataIndex: 'syncedBy', key: 'syncedBy', width: 100 },
                    { title: '同步时间', dataIndex: 'syncedAt', key: 'syncedAt', width: 150 },
                    { title: '错误', dataIndex: 'error', key: 'error', ellipsis: true },
                  ]}
                  dataSource={datasourceSchemaSnapshotHistoryTableRows}
                  locale={{ emptyText: '暂无 schema 同步历史' }}
                  scroll={{ x: 760 }}
                />
              ) : null}
              <Table
                rowKey="key"
                size="small"
                pagination={false}
                columns={[
                  { title: '连接器', dataIndex: 'connector', key: 'connector', width: 130 },
                  { title: '模式', dataIndex: 'modes', key: 'modes', width: 150 },
                  { title: '容量', dataIndex: 'capacity', key: 'capacity', width: 190, ellipsis: true },
                  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
                  { title: '能力', dataIndex: 'capabilities', key: 'capabilities', ellipsis: true },
                ]}
                dataSource={datasourceConnectorTableRows}
                locale={{ emptyText: '暂无连接器目录' }}
                scroll={{ x: 760 }}
              />
              <Table
                rowKey="key"
                size="small"
                pagination={false}
                columns={[
                  { title: '连接器', dataIndex: 'connector', key: 'connector', width: 180, ellipsis: true },
                  { title: '容量池', dataIndex: 'capacityPool', key: 'capacityPool', width: 150 },
                  { title: '预算/约束', dataIndex: 'budget', key: 'budget', width: 300, ellipsis: true },
                  { title: '自助取数', dataIndex: 'eligibility', key: 'eligibility', width: 180 },
                  { title: '治理动作', dataIndex: 'guardrails', key: 'guardrails', ellipsis: true },
                ]}
                dataSource={datasourceCapacityPolicyTableRows}
                locale={{ emptyText: '暂无容量策略' }}
                scroll={{ x: 980 }}
              />
              <Table
                rowKey="key"
                size="small"
                pagination={false}
                columns={[
                  { title: '连接器', dataIndex: 'connector', key: 'connector', width: 180, ellipsis: true },
                  { title: 'Quick 引擎', dataIndex: 'quickEngine', key: 'quickEngine', width: 260, ellipsis: true },
                  { title: '跨源建模', dataIndex: 'crossSourceModeling', key: 'crossSourceModeling', width: 260, ellipsis: true },
                  { title: '自助取数', dataIndex: 'selfService', key: 'selfService', width: 220, ellipsis: true },
                  { title: '语义建模', dataIndex: 'semanticAuthoring', key: 'semanticAuthoring', width: 130 },
                  { title: '风险', dataIndex: 'risk', key: 'risk', ellipsis: true },
                ]}
                dataSource={datasourceAdvancedCapabilityTableRows}
                locale={{ emptyText: '暂无高阶功能支持' }}
                scroll={{ x: 1220 }}
              />
              <Table
                rowKey="key"
                size="small"
                pagination={false}
                columns={[
                  { title: '数据源', dataIndex: 'source', key: 'source', width: 110 },
                  { title: '状态', dataIndex: 'status', key: 'status', width: 70 },
                  { title: '信息', dataIndex: 'message', key: 'message', ellipsis: true },
                ]}
                dataSource={datasourceHealthHistoryTableRows}
                locale={{ emptyText: '暂无健康历史' }}
              />
              <Descriptions
                size="small"
                column={1}
                items={queryGovernanceRows.map(row => ({
                  key: row.label,
                  label: row.label,
                  children: row.value,
                }))}
              />
              <Descriptions
                size="small"
                column={1}
                items={queryGovernancePolicyDisplayRows.map(row => ({
                  key: row.label,
                  label: row.label,
                  children: row.value,
                }))}
              />
              <Descriptions
                size="small"
                column={1}
                items={queryCachePolicyDisplayRows.map(row => ({
                  key: row.label,
                  label: row.label,
                  children: row.value,
                }))}
              />
              <Descriptions
                size="small"
                column={1}
                items={queryCacheStatsDisplayRows.map(row => ({
                  key: row.label,
                  label: row.label,
                  children: row.value,
                }))}
              />
              <Divider plain style={{ margin: '4px 0' }}>数据集加速</Divider>
              <Descriptions
                size="small"
                column={1}
                items={datasetAccelerationPolicyDisplayRows.map(row => ({
                  key: row.label,
                  label: row.label,
                  children: row.value,
                }))}
              />
              <Space.Compact style={{ width: '100%' }}>
                <Select
                  aria-label="数据集加速模式"
                  value={datasetAccelerationMode}
                  onChange={setDatasetAccelerationMode}
                  options={[
                    { label: '直连', value: 'DIRECT_QUERY' },
                    { label: '查询缓存', value: 'CACHE' },
                    { label: '抽取加速', value: 'EXTRACT' },
                  ]}
                  style={{ width: 116 }}
                />
                <Select
                  aria-label="数据集刷新模式"
                  value={datasetAccelerationRefreshMode}
                  onChange={setDatasetAccelerationRefreshMode}
                  options={[
                    { label: '手动', value: 'MANUAL' },
                    { label: '定时', value: 'SCHEDULED' },
                  ]}
                  style={{ width: 96 }}
                />
                <Input
                  aria-label="数据集刷新间隔"
                  value={datasetAccelerationIntervalMinutes}
                  onChange={event => setDatasetAccelerationIntervalMinutes(event.target.value)}
                  placeholder="分钟"
                />
                <Input
                  aria-label="数据集加速TTL"
                  value={datasetAccelerationTtlSeconds}
                  onChange={event => setDatasetAccelerationTtlSeconds(event.target.value)}
                  placeholder="TTL"
                />
              </Space.Compact>
              <Space.Compact style={{ width: '100%' }}>
                <Input
                  aria-label="数据集抽取行数上限"
                  value={datasetAccelerationMaxRows}
                  onChange={event => setDatasetAccelerationMaxRows(event.target.value)}
                  placeholder="max rows"
                />
                <Input
                  aria-label="数据集抽取Cron"
                  value={datasetAccelerationCronExpression}
                  onChange={event => setDatasetAccelerationCronExpression(event.target.value)}
                  placeholder="cron"
                />
              </Space.Compact>
              <Space size={8} wrap>
                <Switch
                  size="small"
                  checked={datasetAccelerationEnabled}
                  onChange={setDatasetAccelerationEnabled}
                />
                <Button
                  loading={savingDatasetAccelerationPolicy}
                  onClick={saveDatasetAccelerationPolicy}
                >
                  保存加速
                </Button>
                <Button
                  icon={<SyncOutlined />}
                  loading={refreshingDatasetAcceleration}
                  disabled={datasetAccelerationMode !== 'EXTRACT' || !datasetAccelerationEnabled}
                  onClick={refreshSelectedDatasetAcceleration}
                >
                  刷新抽取
                </Button>
                <Button
                  icon={<PlayCircleOutlined />}
                  loading={runningDatasetAccelerationScheduler}
                  onClick={runDatasetAccelerationScheduler}
                >
                  运行抽取调度
                </Button>
                {datasetAccelerationSchedulerResult ? (
                  <Tag color={datasetAccelerationSchedulerResult.failed > 0 ? 'red' : 'green'}>
                    抽取调度 policies {datasetAccelerationSchedulerResult.policiesChecked} · refreshed {datasetAccelerationSchedulerResult.refreshed} · skipped {datasetAccelerationSchedulerResult.skipped} · failed {datasetAccelerationSchedulerResult.failed}
                  </Tag>
                ) : null}
              </Space>
              {datasetAccelerationSchedulerTableRows.length > 0 ? (
                <Table
                  rowKey="key"
                  size="small"
                  pagination={false}
                  columns={[
                    { title: '数据集', dataIndex: 'datasetKey', key: 'datasetKey', width: 160 },
                    { title: '状态', dataIndex: 'status', key: 'status', width: 96 },
                    { title: '原因', dataIndex: 'reason', key: 'reason', width: 160, ellipsis: true },
                    { title: '运行', dataIndex: 'run', key: 'run', width: 160 },
                    { title: '物化表', dataIndex: 'materializedTable', key: 'materializedTable', ellipsis: true },
                    { title: '窗口', dataIndex: 'window', key: 'window', width: 260 },
                  ]}
                  dataSource={datasetAccelerationSchedulerTableRows}
                />
              ) : null}
              <Table
                rowKey="key"
                size="small"
                pagination={false}
                columns={[
                  { title: '操作人', dataIndex: 'actor', key: 'actor', width: 80 },
                  { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 150 },
                  { title: '详情', dataIndex: 'detail', key: 'detail', ellipsis: true },
                ]}
                dataSource={queryGovernanceAuditTableRows}
                locale={{ emptyText: '暂无策略审计' }}
              />
              <Space.Compact style={{ width: '100%' }}>
                <Input
                  value={queryPolicyTimeoutMs}
                  onChange={event => setQueryPolicyTimeoutMs(event.target.value)}
                  placeholder="timeout ms"
                />
                <Input
                  value={queryPolicyQuotaRows}
                  onChange={event => setQueryPolicyQuotaRows(event.target.value)}
                  placeholder="quota rows"
                />
                <Button loading={savingQueryGovernancePolicy} onClick={saveQueryGovernancePolicy}>
                  保存
                </Button>
              </Space.Compact>
              <Space.Compact style={{ width: '100%' }}>
                {queryCacheInvalidationActions.map(action => (
                  <Button
                    key={action.key}
                    icon={<SyncOutlined />}
                    loading={invalidatingQueryCache}
                    onClick={() => invalidateQueryCache(action.command)}
                  >
                    {action.label}
                  </Button>
                ))}
                <Input
                  value={queryCacheTtlSeconds}
                  onChange={event => setQueryCacheTtlSeconds(event.target.value)}
                  placeholder="cache ttl seconds"
                />
                <Button
                  loading={savingQueryCachePolicy}
                  onClick={saveQueryCachePolicy}
                >
                  保存缓存
                </Button>
              </Space.Compact>
              <Space.Compact style={{ width: '100%' }}>
                <Select
                  aria-label="缓存策略资源"
                  value={queryCacheResourceScope}
                  onChange={setQueryCacheResourceScope}
                  options={[
                    { label: '当前仪表盘', value: 'DASHBOARD' },
                    { label: '当前数据集', value: 'DATASET' },
                  ]}
                  style={{ width: 120 }}
                />
                <Select
                  aria-label="缓存资源模式"
                  value={queryCacheResourceCacheMode}
                  onChange={value => {
                    setQueryCacheResourceCacheMode(value)
                    setQueryCacheResourceEnabled(value !== 'DIRECT_QUERY')
                  }}
                  options={[
                    { label: '查询缓存', value: 'CACHE' },
                    { label: '直连查询', value: 'DIRECT_QUERY' },
                  ]}
                  style={{ width: 116 }}
                />
                <Input
                  aria-label="缓存资源TTL"
                  value={queryCacheResourceTtlSeconds}
                  onChange={event => setQueryCacheResourceTtlSeconds(event.target.value)}
                  placeholder="resource ttl seconds"
                />
              </Space.Compact>
              <Space size={8} wrap>
                <Switch
                  size="small"
                  checked={queryCacheDefaultEnabled}
                  onChange={setQueryCacheDefaultEnabled}
                />
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {queryCacheDefaultEnabled ? '默认启用查询缓存' : '默认直连查询'}
                </Text>
                <Switch
                  size="small"
                  checked={queryCacheResourceEnabled}
                  onChange={checked => {
                    setQueryCacheResourceEnabled(checked)
                    setQueryCacheResourceCacheMode(checked ? 'CACHE' : 'DIRECT_QUERY')
                  }}
                />
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {queryCacheResourceScope === 'DASHBOARD' ? '当前仪表盘' : '当前数据集'} · {queryCacheResourceKey} · {queryCacheResourceEnabled ? '缓存' : '直连'}
                </Text>
                {queryCacheInvalidationResult ? (
                  <Tag color="gold">
                    {queryCacheInvalidationResult.scope} · {queryCacheInvalidationResult.deletedEntries} 条
                  </Tag>
                ) : null}
              </Space>
              <Divider style={{ margin: '4px 0' }} />
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                  <Space>
                    <DatabaseOutlined />
                    <Text strong>Quick 引擎容量</Text>
                    <Tag color={quickEngineCapacity?.alertLevel === 'CRITICAL'
                      ? 'red'
                      : quickEngineCapacity?.alertLevel === 'WARNING'
                        ? 'orange'
                        : 'green'}
                    >
                      {quickEngineCapacity?.alertLevel ?? 'NORMAL'}
                    </Tag>
                  </Space>
                  <Button size="small" icon={<SyncOutlined />} onClick={reloadQuickEngineCapacity}>
                    刷新容量
                  </Button>
                </Space>
                <List
                  size="small"
                  dataSource={quickEngineCapacitySummaryDisplayRows}
                  renderItem={row => (
                    <List.Item style={{ padding: '4px 0' }}>
                      <Text type="secondary">{row.label}</Text>
                      <Text strong>{row.value}</Text>
                    </List.Item>
                  )}
                />
                <List
                  size="small"
                  dataSource={quickEngineConcurrencyQueueDisplayRows}
                  renderItem={row => (
                    <List.Item style={{ padding: '4px 0' }}>
                      <Text type="secondary">{row.label}</Text>
                      <Text strong>{row.value}</Text>
                    </List.Item>
                  )}
                />
                <Progress
                  percent={Math.min(100, Math.max(0, quickEngineCapacity?.usagePercent ?? 0))}
                  size="small"
                  status={quickEngineCapacity?.alertLevel === 'CRITICAL' ? 'exception' : 'normal'}
                  showInfo={false}
                />
                <div role="region" aria-label="Quick引擎容量明细">
                  <Table
                    rowKey="key"
                    size="small"
                    pagination={false}
                    columns={[
                      { title: '资源', dataIndex: 'resourceKey', key: 'resourceKey', width: 130 },
                      { title: '行数', dataIndex: 'usedRows', key: 'usedRows', width: 86 },
                      { title: '活跃表', dataIndex: 'activeTables', key: 'activeTables', width: 72 },
                      { title: '最近刷新', dataIndex: 'latest', key: 'latest', ellipsis: true },
                      { title: 'Owner', dataIndex: 'owner', key: 'owner', width: 90 },
                    ]}
                    dataSource={quickEngineCapacityDetailTableRows}
                    locale={{ emptyText: '暂无容量明细' }}
                  />
                </div>
                <div role="region" aria-label="Quick引擎用户排行">
                  <Table
                    rowKey="key"
                    size="small"
                    pagination={false}
                    columns={[
                      { title: '用户', dataIndex: 'user', key: 'user', width: 120 },
                      { title: '行数', dataIndex: 'usedRows', key: 'usedRows', width: 86 },
                      { title: '活跃表', dataIndex: 'activeTables', key: 'activeTables', width: 72 },
                      { title: '资源数', dataIndex: 'resourceCount', key: 'resourceCount', width: 72 },
                    ]}
                    dataSource={quickEngineCapacityUserTableRows}
                    locale={{ emptyText: '暂无用户排行' }}
                  />
                </div>
                <Space.Compact style={{ width: '100%' }}>
                  <Input
                    aria-label="Quick引擎容量上限"
                    value={quickEngineCapacityLimitRows}
                    onChange={event => setQuickEngineCapacityLimitRows(event.target.value)}
                    placeholder="capacity rows"
                  />
                  <Input
                    aria-label="Quick引擎预警阈值"
                    value={quickEngineWarningThreshold}
                    onChange={event => setQuickEngineWarningThreshold(event.target.value)}
                    placeholder="warning %"
                  />
                  <Input
                    aria-label="Quick引擎严重阈值"
                    value={quickEngineCriticalThreshold}
                    onChange={event => setQuickEngineCriticalThreshold(event.target.value)}
                    placeholder="critical %"
                  />
                </Space.Compact>
                <Space.Compact style={{ width: '100%' }}>
                  <Input
                    aria-label="Quick引擎通知渠道"
                    value={quickEngineNotificationChannels}
                    onChange={event => setQuickEngineNotificationChannels(event.target.value)}
                    placeholder="EMAIL, LARK"
                  />
                  <Input
                    aria-label="Quick引擎通知接收人"
                    value={quickEngineNotificationReceivers}
                    onChange={event => setQuickEngineNotificationReceivers(event.target.value)}
                    placeholder="bi-ops"
                  />
                  <Button
                    loading={savingQuickEngineCapacityPolicy}
                    onClick={saveQuickEngineCapacityPolicy}
                  >
                    保存容量告警
                  </Button>
                </Space.Compact>
                <Space size={8} wrap>
                  <Switch
                    size="small"
                    checked={quickEngineAlertEnabled}
                    onChange={setQuickEngineAlertEnabled}
                  />
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {quickEngineAlertEnabled ? '容量告警启用' : '容量告警关闭'}
                  </Text>
                </Space>
                <Space.Compact style={{ width: '100%' }}>
                  <Input
                    aria-label="Quick引擎容量池"
                    value={quickEnginePoolKey}
                    onChange={event => setQuickEnginePoolKey(event.target.value)}
                    placeholder="STANDARD"
                  />
                  <Input
                    aria-label="Quick引擎最大并发"
                    value={quickEngineMaxConcurrentQueries}
                    onChange={event => setQuickEngineMaxConcurrentQueries(event.target.value)}
                    placeholder="max concurrency"
                  />
                  <Input
                    aria-label="Quick引擎队列上限"
                    value={quickEngineQueueLimit}
                    onChange={event => setQuickEngineQueueLimit(event.target.value)}
                    placeholder="queue limit"
                  />
                </Space.Compact>
                <Space.Compact style={{ width: '100%' }}>
                  <Input
                    aria-label="Quick引擎队列等待秒数"
                    value={quickEngineQueueTimeoutSeconds}
                    onChange={event => setQuickEngineQueueTimeoutSeconds(event.target.value)}
                    placeholder="queue wait seconds"
                  />
                  <Input
                    aria-label="Quick引擎容量池权重"
                    value={quickEnginePoolWeight}
                    onChange={event => setQuickEnginePoolWeight(event.target.value)}
                    placeholder="pool weight"
                  />
                  <Button
                    loading={savingQuickEngineTenantPoolPolicy}
                    onClick={saveQuickEngineTenantPoolPolicy}
                  >
                    保存容量池
                  </Button>
                </Space.Compact>
              </Space>
              {queryCancellationResult ? (
                <Tag color={queryCancellationResult.cancelled ? 'green' : 'orange'}>
                  {queryCancellationStatusLabel(queryCancellationResult)}
                </Tag>
              ) : null}
            </Space>
          </Col>
          <Col xs={24} lg={16}>
            <Space direction="vertical" size={10} style={{ width: '100%' }}>
              <Space>
                <FileSearchOutlined />
                <Text strong>最近查询</Text>
                <Text type="secondary" style={{ fontSize: 12 }}>用于慢查询和失败诊断</Text>
              </Space>
              <Table
                rowKey="id"
                size="small"
                loading={loadingGovernance}
                pagination={false}
                columns={queryHistoryTableColumns}
                dataSource={queryHistory}
                locale={{ emptyText: '暂无查询历史' }}
              />
            </Space>
          </Col>
        </Row>
      </div>

      <div style={permissionBandStyle}>
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <SafetyCertificateOutlined />
              <Text strong>权限治理</Text>
              <Tag color="blue">{selectedPermissionTarget.resourceKey || '未选择资源'}</Tag>
              <Tag color="geekblue">资源 {resourcePermissions.length}</Tag>
              <Tag color="cyan">行 {rowPermissions.length}</Tag>
              <Tag color="purple">列 {columnPermissions.length}</Tag>
              <Tag color={selectedPermissionSensitiveFields.length ? 'red' : 'default'}>敏感字段 {selectedPermissionSensitiveFields.length}</Tag>
            </Space>
            <Space size={8} wrap>
              <Select
                size="small"
                aria-label="权限治理资源"
                value={selectedPermissionTarget.value}
                style={{ width: 220 }}
                options={permissionResourceTargets.map(target => ({
                  label: `${target.label} · ${target.resourceKey || '无可用资源'}`,
                  value: target.value,
                  disabled: target.disabled,
                }))}
                onChange={setPermissionResourceTarget}
              />
              <Button
                size="small"
                icon={<LockOutlined />}
                disabled={!selectedPermissionTarget.resourceKey}
                loading={savingPermission === 'resource'}
                onClick={grantSelectedResourceUse}
              >
                授予使用
              </Button>
              <Button
                size="small"
                icon={<SettingOutlined />}
                disabled={!selectedPermissionTarget.resourceKey}
                loading={savingPermission === 'edit'}
                onClick={grantSelectedResourceEdit}
              >
                授予编辑
              </Button>
              <Button
                size="small"
                icon={<DownloadOutlined />}
                disabled={!selectedPermissionTarget.resourceKey || selectedPermissionTarget.resourceType === 'DATASOURCE'}
                loading={savingPermission === 'export'}
                onClick={grantSelectedResourceExport}
              >
                授予导出
              </Button>
              <Button size="small" icon={<FilterOutlined />} disabled={!isDatasetPermissionTarget} loading={savingPermission === 'row'} onClick={addCanvasRowPermission}>行权限</Button>
              <Button size="small" icon={<SafetyCertificateOutlined />} disabled={!isDatasetPermissionTarget} loading={savingPermission === 'column'} onClick={maskCanvasName}>字段脱敏</Button>
              <Badge status={loadingPermissions ? 'processing' : 'success'} text={loadingPermissions ? '同步中' : '已就绪'} />
            </Space>
          </Space>
          <Space size={8} wrap>
            <select
              aria-label="资源权限主体类型"
              value={resourcePermissionSubjectType}
              style={{ width: 104, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
              disabled={!selectedPermissionTarget.resourceKey}
              onChange={event => setResourcePermissionSubjectType(event.target.value)}
            >
              <option value="ROLE">ROLE</option>
              <option value="USER">USER</option>
              <option value="GROUP">GROUP</option>
              <option value="ALL">ALL</option>
            </select>
            <Input
              size="small"
              aria-label="资源权限主体"
              value={resourcePermissionSubjectId}
              placeholder={resourcePermissionSubjectType === 'ALL' ? 'ALL' : '主体 ID'}
              style={{ width: 160 }}
              disabled={!selectedPermissionTarget.resourceKey || resourcePermissionSubjectType === 'ALL'}
              onChange={event => setResourcePermissionSubjectId(event.target.value)}
            />
            <select
              aria-label="资源权限动作"
              value={resourcePermissionAction}
              style={{ width: 112, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
              disabled={!selectedPermissionTarget.resourceKey}
              onChange={event => setResourcePermissionAction(event.target.value)}
            >
              <option value="USE">USE</option>
              <option value="EDIT">EDIT</option>
              <option value="EXPORT">EXPORT</option>
              <option value="PUBLISH">PUBLISH</option>
              <option value="SUBSCRIBE">SUBSCRIBE</option>
            </select>
            <select
              aria-label="资源权限效果"
              value={resourcePermissionEffect}
              style={{ width: 104, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
              disabled={!selectedPermissionTarget.resourceKey}
              onChange={event => setResourcePermissionEffect(event.target.value)}
            >
              <option value="ALLOW">ALLOW</option>
              <option value="DENY">DENY</option>
            </select>
            <Button
              size="small"
              type="primary"
              aria-label="保存资源权限规则"
              icon={<SafetyCertificateOutlined />}
              disabled={!selectedPermissionTarget.resourceKey}
              loading={savingPermission === 'resource-editor'}
              onClick={saveResourcePermissionRule}
            >
              保存资源权限规则
            </Button>
          </Space>
          <Space size={8} wrap>
            <Input
              size="small"
              aria-label="行权限规则Key"
              value={rowPermissionRuleKey}
              placeholder="行权限规则 Key"
              style={{ width: 170 }}
              disabled={!isDatasetPermissionTarget}
              onChange={event => setRowPermissionRuleKey(event.target.value)}
            />
            <select
              aria-label="行权限主体类型"
              value={rowPermissionSubjectType}
              style={{ width: 104, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
              disabled={!isDatasetPermissionTarget}
              onChange={event => setRowPermissionSubjectType(event.target.value)}
            >
              <option value="ROLE">ROLE</option>
              <option value="USER">USER</option>
              <option value="GROUP">GROUP</option>
              <option value="ALL">ALL</option>
            </select>
            <Input
              size="small"
              aria-label="行权限主体"
              value={rowPermissionSubjectId}
              placeholder={rowPermissionSubjectType === 'ALL' ? 'ALL' : '主体 ID'}
              style={{ width: 140 }}
              disabled={!isDatasetPermissionTarget || rowPermissionSubjectType === 'ALL'}
              onChange={event => setRowPermissionSubjectId(event.target.value)}
            />
            <Input
              size="small"
              aria-label="行权限过滤JSON"
              value={rowPermissionFilterJson}
              placeholder='{"field":"value"}'
              style={{ width: 220 }}
              disabled={!isDatasetPermissionTarget}
              onChange={event => setRowPermissionFilterJson(event.target.value)}
            />
            <Checkbox
              checked={rowPermissionEnabled}
              disabled={!isDatasetPermissionTarget}
              onChange={event => setRowPermissionEnabled(event.target.checked)}
            >
              启用
            </Checkbox>
            <Button
              size="small"
              type="primary"
              aria-label="保存行权限规则"
              icon={<FilterOutlined />}
              disabled={!isDatasetPermissionTarget}
              loading={savingPermission === 'row-editor'}
              onClick={saveRowPermissionRule}
            >
              保存行权限规则
            </Button>
          </Space>
          <Space size={8} wrap>
            <Input
              size="small"
              aria-label="列权限字段"
              value={columnPermissionFieldKey}
              placeholder="字段 key"
              style={{ width: 140 }}
              disabled={!isDatasetPermissionTarget}
              onChange={event => setColumnPermissionFieldKey(event.target.value)}
            />
            <select
              aria-label="列权限主体类型"
              value={columnPermissionSubjectType}
              style={{ width: 104, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
              disabled={!isDatasetPermissionTarget}
              onChange={event => setColumnPermissionSubjectType(event.target.value)}
            >
              <option value="ROLE">ROLE</option>
              <option value="USER">USER</option>
              <option value="GROUP">GROUP</option>
              <option value="ALL">ALL</option>
            </select>
            <Input
              size="small"
              aria-label="列权限主体"
              value={columnPermissionSubjectId}
              placeholder={columnPermissionSubjectType === 'ALL' ? 'ALL' : '主体 ID'}
              style={{ width: 140 }}
              disabled={!isDatasetPermissionTarget || columnPermissionSubjectType === 'ALL'}
              onChange={event => setColumnPermissionSubjectId(event.target.value)}
            />
            <select
              aria-label="列权限策略"
              value={columnPermissionPolicy}
              style={{ width: 104, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
              disabled={!isDatasetPermissionTarget}
              onChange={event => setColumnPermissionPolicy(event.target.value)}
            >
              <option value="MASK">MASK</option>
              <option value="DENY">DENY</option>
              <option value="ALLOW">ALLOW</option>
            </select>
            <Input
              size="small"
              aria-label="列权限Mask JSON"
              value={columnPermissionMaskJson}
              placeholder='{"strategy":"FIXED"}'
              style={{ width: 240 }}
              disabled={!isDatasetPermissionTarget}
              onChange={event => setColumnPermissionMaskJson(event.target.value)}
            />
            <Checkbox
              checked={columnPermissionEnabled}
              disabled={!isDatasetPermissionTarget}
              onChange={event => setColumnPermissionEnabled(event.target.checked)}
            >
              启用
            </Checkbox>
            <Button
              size="small"
              type="primary"
              aria-label="保存列权限规则"
              icon={<SafetyCertificateOutlined />}
              disabled={!isDatasetPermissionTarget}
              loading={savingPermission === 'column-editor'}
              onClick={saveColumnPermissionRule}
            >
              保存列权限规则
            </Button>
            <Button
              size="small"
              aria-label="应用敏感字段列权限"
              icon={<SafetyCertificateOutlined />}
              disabled={!isDatasetPermissionTarget || columnPermissionShortcutFieldKeys.length === 0}
              loading={savingPermission === 'sensitive-columns'}
              onClick={applySensitiveFieldColumnPermissions}
            >
              应用敏感字段列权限
            </Button>
          </Space>
          <Space size={8} wrap>
            <select
              aria-label="权限申请动作"
              value={permissionRequestAction}
              style={{ width: 112, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
              disabled={!selectedPermissionTarget.resourceKey}
              onChange={event => setPermissionRequestAction(event.target.value)}
            >
              <option value="USE">USE</option>
              <option value="EDIT">EDIT</option>
              <option value="EXPORT">EXPORT</option>
              <option value="PUBLISH">PUBLISH</option>
            </select>
            <Input
              size="small"
              aria-label="权限申请理由"
              value={permissionRequestReason}
              placeholder="申请理由"
              style={{ width: 260 }}
              disabled={!selectedPermissionTarget.resourceKey}
              onChange={event => setPermissionRequestReason(event.target.value)}
            />
            <Button
              size="small"
              type="primary"
              aria-label="提交权限申请"
              icon={<SafetyCertificateOutlined />}
              disabled={!selectedPermissionTarget.resourceKey}
              loading={savingPermission === 'request'}
              onClick={submitPermissionRequest}
            >
              提交权限申请
            </Button>
            <Input
              size="small"
              aria-label="权限审核意见"
              value={permissionReviewComment}
              placeholder="审核意见"
              style={{ width: 240 }}
              onChange={event => setPermissionReviewComment(event.target.value)}
            />
          </Space>
          <Row gutter={12}>
            <Col xs={24} xl={8}>
              <Table
                rowKey="id"
                size="small"
                loading={loadingPermissions}
                pagination={false}
                columns={resourcePermissionTableColumns}
                dataSource={resourcePermissions}
                locale={{ emptyText: '暂无资源授权' }}
                scroll={{ x: 592 }}
              />
            </Col>
            <Col xs={24} xl={8}>
              <Table
                rowKey="id"
                size="small"
                loading={loadingPermissions}
                pagination={false}
                columns={rowPermissionTableColumns}
                dataSource={rowPermissions}
                locale={{ emptyText: '暂无行权限' }}
                scroll={{ x: 632 }}
              />
            </Col>
            <Col xs={24} xl={8}>
              <Table
                rowKey="id"
                size="small"
                loading={loadingPermissions}
                pagination={false}
                columns={columnPermissionTableColumns}
                dataSource={columnPermissions}
                locale={{ emptyText: '暂无列权限' }}
                scroll={{ x: 592 }}
              />
            </Col>
          </Row>
          <Table
            rowKey={row => String(row.id ?? `${row.resourceType}-${row.resourceKey}-${row.requestedAction}`)}
            size="small"
            loading={loadingPermissions}
            pagination={false}
            columns={[
              {
                title: '申请资源',
                render: (_, row: BiPermissionRequestView) => (
                  <Space direction="vertical" size={0}>
                    <Space size={6}>
                      <Tag color="blue">{row.resourceType}</Tag>
                      <Text strong style={{ fontSize: 12 }}>{row.resourceKey}</Text>
                    </Space>
                    <Text type="secondary" style={{ fontSize: 11 }}>{row.requestedBy ?? '-'} · {formatAttachmentTime(row.requestedAt)}</Text>
                  </Space>
                ),
              },
              {
                title: '动作',
                width: 96,
                render: (_, row: BiPermissionRequestView) => <Tag color="geekblue">{row.requestedAction}</Tag>,
              },
              {
                title: '状态',
                width: 112,
                render: (_, row: BiPermissionRequestView) => <Tag color={row.status === 'APPROVED' ? 'green' : row.status === 'REJECTED' ? 'red' : 'gold'}>{row.status}</Tag>,
              },
              {
                title: '理由',
                width: 220,
                render: (_, row: BiPermissionRequestView) => <Text type="secondary" ellipsis style={{ maxWidth: 210, display: 'block', fontSize: 12 }}>{row.reason ?? '-'}</Text>,
              },
              {
                title: '审核',
                width: 180,
                render: (_, row: BiPermissionRequestView) => row.status === 'PENDING' ? (
                  <Space size={4}>
                    <Button
                      size="small"
                      aria-label={`批准权限申请 #${row.id}`}
                      loading={reviewingPermissionRequest === `APPROVED-${row.id}`}
                      onClick={() => reviewPermissionRequest(row, 'APPROVED')}
                    >
                      批准
                    </Button>
                    <Button
                      size="small"
                      danger
                      aria-label={`拒绝权限申请 #${row.id}`}
                      loading={reviewingPermissionRequest === `REJECTED-${row.id}`}
                      onClick={() => reviewPermissionRequest(row, 'REJECTED')}
                    >
                      拒绝
                    </Button>
                  </Space>
                ) : <Text type="secondary" style={{ fontSize: 12 }}>{row.reviewedBy ?? '-'}</Text>,
              },
            ]}
            dataSource={permissionRequests}
            locale={{ emptyText: '暂无权限申请' }}
            scroll={{ x: 860 }}
          />
          <Table
            rowKey="key"
            size="small"
            loading={loadingPermissions}
            pagination={false}
            columns={[
              { title: '操作人', dataIndex: 'actor', key: 'actor', width: 100 },
              { title: '动作', dataIndex: 'action', key: 'action', width: 170 },
              { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
              { title: '详情', dataIndex: 'detail', key: 'detail', ellipsis: true },
            ]}
            dataSource={permissionAuditTableRows}
            locale={{ emptyText: '暂无权限审计' }}
          />
        </Space>
      </div>

      <div style={selfServiceBandStyle}>
        <Row gutter={12}>
          <Col xs={24} xl={14}>
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                <Space>
                  <FileSearchOutlined />
                  <Text strong>自助取数</Text>
                  <Tag color="blue">{selectedWidget.title}</Tag>
                </Space>
                <Space size={8}>
                  <Button size="small" icon={<PlayCircleOutlined />} loading={previewingSelfService} onClick={previewSelfService}>预览</Button>
                  <Button size="small" type="primary" icon={<DownloadOutlined />} loading={creatingExport} onClick={() => createSelfServiceExport(false)}>导出 CSV</Button>
                  <Button size="small" icon={<SafetyCertificateOutlined />} loading={creatingExport} onClick={() => createSelfServiceExport(true)}>敏感导出</Button>
                </Space>
              </Space>
              <SelfServiceExtractionBuilder
                dimensionFields={filteredDimensions.map(field => field.fieldKey)}
                metricFields={filteredMetrics.map(metric => metric.metricKey)}
                value={selfServiceExtraction}
                onDropField={dropSelfServiceField}
                onRemoveField={removeSelfServiceField}
              />
              <SelfServicePreviewTable result={selfServicePreview} loading={previewingSelfService} />
            </Space>
          </Col>
          <Col xs={24} xl={10}>
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                <Space>
                  <DownloadOutlined />
                  <Text strong>导出任务</Text>
                  <Tag>{exportJobs.length} 条</Tag>
                  {exportDownloadCount > 0 && <Tag color="geekblue">{exportDownloadCount} 下载</Tag>}
                  {expiredExportCount > 0 && <Tag color="gold">{expiredExportCount} 过期</Tag>}
                  {retryableExportCount > 0 && <Tag color="volcano">{retryableExportCount} 可重试</Tag>}
                  {exportRetryResult && (
                    <Tag color={exportRetryResult.failed > 0 ? 'red' : 'green'}>
                      重试 {exportRetryResult.completed}/{exportRetryResult.checked}
                    </Tag>
                  )}
                  {exportCleanupResult && (
                    <Tag color={exportCleanupResult.failed > 0 ? 'red' : 'purple'}>
                      清理 {exportCleanupResult.expired}/{exportCleanupResult.checked}
                    </Tag>
                  )}
                </Space>
                <Space size={8}>
                  <Badge status={loadingExports ? 'processing' : 'success'} text={loadingExports ? '同步中' : '已就绪'} />
                  <Button size="small" icon={<SyncOutlined />} loading={retryingExports} onClick={retryExports}>重试失败</Button>
                  <Button size="small" icon={<DeleteOutlined />} loading={cleaningExports} onClick={cleanupExports}>清理过期</Button>
                </Space>
              </Space>
              <Input
                size="small"
                aria-label="导出审批意见"
                value={exportReviewComment}
                placeholder="导出审批意见"
                onChange={event => setExportReviewComment(event.target.value)}
              />
              <ExportHardeningDiagnosticSummary rows={exportHardeningRows} />
              <Table
                rowKey="id"
                size="small"
                loading={loadingExports}
                pagination={false}
                columns={exportJobTableColumns}
                dataSource={exportJobs}
                locale={{ emptyText: '暂无导出任务' }}
                scroll={{ x: 520 }}
              />
            </Space>
          </Col>
        </Row>
      </div>

      <div style={subscriptionBandStyle}>
        <Row gutter={12}>
          <Col xs={24} xl={12}>
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                <Space>
                  <SendOutlined />
                  <Text strong>订阅推送</Text>
                  <Tag color="blue">{dashboardPreset.dashboardKey}</Tag>
                </Space>
                <Space size={8}>
                  <Button size="small" icon={<CalendarOutlined />} loading={savingSubscription === 'subscription'} onClick={createDashboardSubscription}>创建日报</Button>
                  <Button size="small" icon={<PlayCircleOutlined />} disabled={subscriptions.length === 0} loading={runningDelivery === 'subscription'} onClick={runLatestSubscription}>执行</Button>
                  <Badge status={loadingSubscriptions ? 'processing' : 'success'} text={loadingSubscriptions ? '同步中' : '已就绪'} />
                </Space>
              </Space>
              <Table
                rowKey="id"
                size="small"
                loading={loadingSubscriptions}
                pagination={false}
                columns={subscriptionColumns}
                dataSource={subscriptions}
                locale={{ emptyText: '暂无订阅任务' }}
                scroll={{ x: 640 }}
              />
            </Space>
          </Col>
          <Col xs={24} xl={12}>
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                <Space>
                  <ExclamationCircleOutlined />
                  <Text strong>指标告警</Text>
                  <Tag color="orange">{selectedWidget.metrics[0] ?? 'metric'}</Tag>
                </Space>
                <Space size={8}>
                  <Button size="small" icon={<ExclamationCircleOutlined />} loading={savingSubscription === 'alert'} onClick={createMetricAlert}>创建告警</Button>
                  <Button size="small" icon={<SlidersOutlined />} loading={savingSubscription === 'anomaly'} onClick={createAnomalyAlert}>创建异常</Button>
                  <Button size="small" icon={<PlayCircleOutlined />} disabled={alertRules.length === 0} loading={runningDelivery === 'alert'} onClick={runLatestAlert}>检测</Button>
                </Space>
              </Space>
              <AlertAnomalyDiagnosticSummary rows={alertAnomalyDiagnosticRowsValue} />
              <Table
                rowKey="id"
                size="small"
                loading={loadingSubscriptions}
                pagination={false}
                columns={alertRuleColumns}
                dataSource={alertRules}
                locale={{ emptyText: '暂无指标告警' }}
                scroll={{ x: 640 }}
              />
            </Space>
          </Col>
        </Row>
        <Divider style={{ margin: '12px 0' }} />
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <FileSearchOutlined />
              <Text strong>投递记录</Text>
              <Tag>{deliveryLogs.length} 条</Tag>
              {deliveryAudit && <Tag color="blue">审计 {deliveryAudit.total}</Tag>}
              {deliveryAudit && deliveryAudit.failed > 0 && <Tag color="red">失败 {deliveryAudit.failed}</Tag>}
              {deliveryAudit && deliveryAudit.retryable > 0 && <Tag color="gold">可重试 {deliveryAudit.retryable}</Tag>}
              {deliveryAudit && deliveryAudit.retryExhausted > 0 && <Tag color="volcano">耗尽 {deliveryAudit.retryExhausted}</Tag>}
              <Tag color={expiredAttachmentCount > 0 ? 'gold' : 'cyan'}>{deliveryAttachments.length} 附件</Tag>
              {attachmentDownloadCount > 0 && <Tag color="geekblue">{attachmentDownloadCount} 下载</Tag>}
              {expiredAttachmentCount > 0 && <Tag color="gold">{expiredAttachmentCount} 过期</Tag>}
              {schedulerResult && (
                <Tag color={schedulerResult.failed > 0 ? 'red' : 'green'}>
                  调度 {schedulerResult.subscriptionsTriggered + schedulerResult.alertsTriggered}/{schedulerResult.subscriptionsChecked + schedulerResult.alertsChecked}
                </Tag>
              )}
              {retryResult && (
                <Tag color={retryResult.failed > 0 ? 'red' : retryResult.pending > 0 ? 'gold' : 'green'}>
                  重试 {retryResult.delivered}/{retryResult.checked}
                </Tag>
              )}
              {cleanupResult && (
                <Tag color={cleanupResult.failed > 0 ? 'red' : 'purple'}>
                  清理 {cleanupResult.expired}/{cleanupResult.checked}
                </Tag>
              )}
            </Space>
            <Space size={8}>
              <Button size="small" icon={<PlayCircleOutlined />} loading={runningDelivery === 'scheduler'} onClick={runDeliveryScheduler}>调度</Button>
              <Button size="small" icon={<SyncOutlined />} loading={runningDelivery === 'retry'} onClick={retryDeliveryLogs}>重试</Button>
              <Button size="small" icon={<DeleteOutlined />} loading={runningDelivery === 'cleanup'} onClick={cleanupDeliveryAttachments}>清理</Button>
              <Button size="small" icon={<SyncOutlined />} loading={loadingSubscriptions} onClick={reloadSubscriptions}>刷新</Button>
            </Space>
          </Space>
          <Table
            rowKey="id"
            size="small"
            loading={loadingSubscriptions}
            pagination={false}
            columns={deliveryLogColumns}
            dataSource={deliveryLogs}
            locale={{ emptyText: '暂无投递记录' }}
            scroll={{ x: 920 }}
          />
        </Space>
      </div>

      <div style={datasetBandStyle}>
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <LayoutOutlined />
              <Text strong>大屏与电子表格</Text>
              <Tag color="blue">{bigScreenResources.length} 大屏</Tag>
              <Tag color="purple">{spreadsheetResources.length} 表格</Tag>
            </Space>
            <Badge
              status={loadingBigScreens || loadingSpreadsheets ? 'processing' : 'success'}
              text={loadingBigScreens || loadingSpreadsheets ? '同步中' : '已就绪'}
            />
          </Space>
          <VisualEditorDiagnosticSummary rows={visualEditorDiagnosticRows} />
          <Row gutter={[12, 12]}>
            <Col xs={24} lg={12}>
              <Space direction="vertical" size={10} style={{ width: '100%' }}>
                <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                  <Space>
                    <LayoutOutlined />
                    <Text strong>大屏工作台</Text>
                    <Tag color={selectedBigScreenResource?.status === 'PUBLISHED' ? 'green' : 'gold'}>
                      {selectedBigScreenResource?.status ?? '未创建'}
                    </Tag>
                  </Space>
                  <Select
                    size="small"
                    value={selectedBigScreenKey ?? undefined}
                    style={{ width: 220 }}
                    placeholder="选择大屏"
                    loading={loadingBigScreens}
                    disabled={bigScreenResourceOptions.length === 0}
                    options={bigScreenResourceOptions}
                    onChange={setSelectedBigScreenKey}
                  />
                </Space>
                <Descriptions
                  size="small"
                  column={1}
                  items={bigScreenSummaryRows.map(row => ({
                    key: row.label,
                    label: row.label,
                    children: row.value,
                  }))}
                />
                <Space direction="vertical" size={8} style={{ width: '100%', padding: 10, border: '1px solid #eef2f7', borderRadius: 6 }}>
                  <Space size={8} wrap>
                    <Select
                      size="small"
                      aria-label="大屏布局组件"
                      value={selectedBigScreenLayoutItem ? String(selectedBigScreenLayoutItem.widgetKey ?? '') : undefined}
                      style={{ width: 220 }}
                      placeholder="选择布局组件"
                      options={bigScreenLayoutOptions}
                      disabled={bigScreenLayoutOptions.length === 0}
                      onChange={setSelectedBigScreenWidgetKey}
                    />
                    <Select
                      mode="multiple"
                      size="small"
                      aria-label="大屏对齐组件"
                      value={selectedBigScreenAlignWidgetKeys}
                      style={{ minWidth: 220 }}
                      placeholder="选择对齐组件"
                      options={bigScreenLayoutOptions}
                      disabled={bigScreenLayoutOptions.length < 2}
                      onChange={setSelectedBigScreenAlignWidgetKeys}
                    />
                    <Input
                      size="small"
                      aria-label="大屏组件标题"
                      value={String(selectedBigScreenLayoutItem?.title ?? '')}
                      placeholder="组件标题"
                      style={{ width: 150 }}
                      onChange={event => updateSelectedBigScreenLayout({ title: event.target.value })}
                    />
                    <Select
                      size="small"
                      aria-label="大屏组件资源类型"
                      value={String(selectedBigScreenLayoutItem?.resourceType ?? 'DASHBOARD')}
                      style={{ width: 120 }}
                      options={['DASHBOARD', 'CHART', 'DATASET', 'URL', 'TEXT'].map(value => ({ label: value, value }))}
                      onChange={value => updateSelectedBigScreenLayout({ resourceType: value })}
                    />
                    <Input
                      size="small"
                      aria-label="大屏组件资源Key"
                      value={String(selectedBigScreenLayoutItem?.resourceKey ?? selectedBigScreenLayoutItem?.datasetKey ?? '')}
                      placeholder="resource key"
                      style={{ width: 150 }}
                      onChange={event => updateSelectedBigScreenLayout({ resourceKey: event.target.value })}
                    />
                    <select
                      aria-label="大屏移动端布局"
                      value={String(selectedBigScreenResource?.mobileLayout?.variant ?? 'single-column')}
                      style={{ height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px', fontSize: 12 }}
                      onChange={event => updateSelectedBigScreenMobileLayout(event.target.value as 'single-column' | 'compact-grid')}
                    >
                      <option value="single-column">移动单列</option>
                      <option value="compact-grid">移动紧凑</option>
                    </select>
                    <select
                      aria-label="大屏组件库"
                      value={selectedBigScreenComponentKey}
                      style={{ height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px', fontSize: 12 }}
                      onChange={event => setSelectedBigScreenComponentKey(event.target.value)}
                    >
                      {BIG_SCREEN_COMPONENT_LIBRARY.map(component => (
                        <option key={component.key} value={component.key}>{component.title}</option>
                      ))}
                    </select>
                    <Button
                      size="small"
                      aria-label="添加大屏组件"
                      icon={<PlusOutlined />}
                      onClick={addSelectedBigScreenComponent}
                    >
                      添加组件
                    </Button>
                  </Space>
                  <Space size={8} wrap>
                    <Tooltip title="左移">
                      <Button size="small" aria-label="大屏组件左移" icon={<ArrowLeftOutlined />} onClick={() => moveSelectedBigScreenLayout('left')} />
                    </Tooltip>
                    <Tooltip title="右移">
                      <Button size="small" aria-label="大屏组件右移" icon={<ArrowRightOutlined />} onClick={() => moveSelectedBigScreenLayout('right')} />
                    </Tooltip>
                    <Tooltip title="上移">
                      <Button size="small" aria-label="大屏组件上移" icon={<ArrowUpOutlined />} onClick={() => moveSelectedBigScreenLayout('up')} />
                    </Tooltip>
                    <Tooltip title="下移">
                      <Button size="small" aria-label="大屏组件下移" icon={<ArrowDownOutlined />} onClick={() => moveSelectedBigScreenLayout('down')} />
                    </Tooltip>
                    <Tooltip title="缩窄">
                      <Button size="small" aria-label="大屏组件缩窄" icon={<AlignLeftOutlined />} onClick={() => resizeSelectedBigScreenLayout('left')} />
                    </Tooltip>
                    <Tooltip title="放宽">
                      <Button size="small" aria-label="大屏组件放宽" icon={<AlignRightOutlined />} onClick={() => resizeSelectedBigScreenLayout('right')} />
                    </Tooltip>
                    <Tooltip title="压低">
                      <Button size="small" aria-label="大屏组件压低" icon={<VerticalAlignTopOutlined />} onClick={() => resizeSelectedBigScreenLayout('up')} />
                    </Tooltip>
                    <Tooltip title="增高">
                      <Button size="small" aria-label="大屏组件增高" icon={<VerticalAlignBottomOutlined />} onClick={() => resizeSelectedBigScreenLayout('down')} />
                    </Tooltip>
                    <Tooltip title="左对齐">
                      <Button size="small" aria-label="大屏组件左对齐" icon={<AlignLeftOutlined />} disabled={(selectedBigScreenResource?.layout.length ?? 0) < 2} onClick={() => alignSelectedBigScreenLayouts('left')} />
                    </Tooltip>
                    <Tooltip title="顶对齐">
                      <Button size="small" aria-label="大屏组件顶对齐" icon={<VerticalAlignTopOutlined />} disabled={(selectedBigScreenResource?.layout.length ?? 0) < 2} onClick={() => alignSelectedBigScreenLayouts('top')} />
                    </Tooltip>
                    <Tooltip title="吸附到邻近参考线">
                      <Button size="small" aria-label="吸附大屏组件" icon={<NodeIndexOutlined />} disabled={(selectedBigScreenResource?.layout.length ?? 0) < 2} onClick={snapSelectedBigScreenLayout} />
                    </Tooltip>
                    {(['x', 'y', 'w', 'h'] as const).map(axis => (
                      <Input
                        key={axis}
                        size="small"
                        type="number"
                        aria-label={`大屏组件${axis}`}
                        value={String(selectedBigScreenLayoutItem?.[axis] ?? '')}
                        placeholder={axis}
                        style={{ width: 80 }}
                        onChange={event => updateSelectedBigScreenLayout({ [axis]: event.target.value })}
                      />
                    ))}
                    <Tag color="blue">24 栅格</Tag>
                    <Tag color={bigScreenSnapGuideCount > 0 ? 'geekblue' : 'default'}>
                      参考线 {bigScreenSnapGuideCount}
                    </Tag>
                  </Space>
                </Space>
                <Space size={8} wrap>
                  <Button
                    size="small"
                    type="primary"
                    aria-label="保存大屏草稿"
                    icon={<SaveOutlined />}
                    loading={savingBigScreen === (selectedBigScreenResource?.screenKey ?? `${dashboardPreset.dashboardKey}-big-screen`)}
                    onClick={saveBigScreenDraft}
                  >
                    保存草稿
                  </Button>
                  <Button
                    size="small"
                    icon={<ShareAltOutlined />}
                    disabled={!selectedBigScreenResource}
                    loading={publishingBigScreen === selectedBigScreenResource?.screenKey}
                    onClick={publishBigScreen}
                  >
                    发布
                  </Button>
                  <Button
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    disabled={!selectedBigScreenResource}
                    loading={archivingBigScreen === selectedBigScreenResource?.screenKey}
                    onClick={archiveBigScreen}
                  >
                    归档
                  </Button>
                  <Button
                    size="small"
                    icon={<SyncOutlined />}
                    loading={loadingBigScreenVersions}
                    disabled={!selectedBigScreenResource}
                    onClick={() => reloadBigScreenVersions(selectedBigScreenResource?.screenKey)}
                  >
                    版本
                  </Button>
                </Space>
                <Table
                  rowKey={row => `${row.screenKey}-${row.version}`}
                  size="small"
                  loading={loadingBigScreenVersions}
                  pagination={false}
                  columns={bigScreenVersionTableColumns}
                  dataSource={bigScreenVersions}
                  locale={{ emptyText: '暂无大屏版本' }}
                  scroll={{ x: 720 }}
                />
              </Space>
            </Col>
            <Col xs={24} lg={12}>
              <Space direction="vertical" size={10} style={{ width: '100%' }}>
                <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                  <Space>
                    <DatabaseOutlined />
                    <Text strong>电子表格工作台</Text>
                    <Tag color={selectedSpreadsheetResource?.status === 'PUBLISHED' ? 'green' : 'gold'}>
                      {selectedSpreadsheetResource?.status ?? '未创建'}
                    </Tag>
                  </Space>
                  <Select
                    size="small"
                    value={selectedSpreadsheetKey ?? undefined}
                    style={{ width: 220 }}
                    placeholder="选择电子表格"
                    loading={loadingSpreadsheets}
                    disabled={spreadsheetResourceOptions.length === 0}
                    options={spreadsheetResourceOptions}
                    onChange={setSelectedSpreadsheetKey}
                  />
                </Space>
                <Descriptions
                  size="small"
                  column={1}
                  items={spreadsheetSummaryRows.map(row => ({
                    key: row.label,
                    label: row.label,
                    children: row.value,
                  }))}
                />
                <Space direction="vertical" size={8} style={{ width: '100%', padding: 10, border: '1px solid #eef2f7', borderRadius: 6 }}>
                  <Space size={8} wrap>
                    <Select
                      size="small"
                      aria-label="电子表格工作表"
                      value={selectedSpreadsheetSheet ? String(selectedSpreadsheetSheet.sheetKey ?? '') : undefined}
                      style={{ width: 200 }}
                      placeholder="选择工作表"
                      options={spreadsheetSheetOptions}
                      disabled={spreadsheetSheetOptions.length === 0}
                      onChange={setSelectedSpreadsheetSheetKey}
                    />
                    <Input
                      size="small"
                      aria-label="电子表格单元格"
                      value={selectedSpreadsheetCellKey}
                      placeholder="A1"
                      style={{ width: 90 }}
                      onChange={event => setSelectedSpreadsheetCellKey(event.target.value.toUpperCase())}
                    />
                    <Input
                      size="small"
                      aria-label="电子表格单元格内容"
                      value={selectedSpreadsheetCellValue}
                      placeholder="文本或 =SUM(B3:B8)"
                      style={{ width: 240 }}
                      onChange={event => updateSelectedSpreadsheetCell(selectedSpreadsheetCellKey, event.target.value)}
                    />
                    <Tag color={selectedSpreadsheetCellValue.startsWith('=') ? 'geekblue' : 'default'}>
                      {selectedSpreadsheetCellValue.startsWith('=') ? '公式' : '值'}
                    </Tag>
                    <Input
                      size="small"
                      aria-label="电子表格填充范围"
                      value={spreadsheetFillRange}
                      placeholder="B2:C3"
                      style={{ width: 110 }}
                      onChange={event => setSpreadsheetFillRange(event.target.value.toUpperCase())}
                    />
                    <Button
                      size="small"
                      aria-label="批量填充电子表格单元格"
                      icon={<DeploymentUnitOutlined />}
                      disabled={!spreadsheetFillRange.trim()}
                      onClick={fillSelectedSpreadsheetRange}
                    >
                      批量填充
                    </Button>
                    <Button
                      size="small"
                      aria-label="加粗单元格"
                      type={selectedSpreadsheetCellStyle.bold ? 'primary' : 'default'}
                      icon={<EditOutlined />}
                      onClick={() => updateSelectedSpreadsheetCellStyle({ bold: !selectedSpreadsheetCellStyle.bold })}
                    >
                      加粗
                    </Button>
                    <Input
                      size="small"
                      type="color"
                      aria-label="电子表格背景色"
                      value={String(selectedSpreadsheetCellStyle.backgroundColor ?? '#ffffff')}
                      style={{ width: 48, padding: 2 }}
                      onChange={event => updateSelectedSpreadsheetCellStyle({ backgroundColor: event.target.value.toUpperCase() })}
                    />
                    <Input
                      size="small"
                      type="color"
                      aria-label="电子表格文字色"
                      value={String(selectedSpreadsheetCellStyle.textColor ?? '#111827')}
                      style={{ width: 48, padding: 2 }}
                      onChange={event => updateSelectedSpreadsheetCellStyle({ textColor: event.target.value.toUpperCase() })}
                    />
                  </Space>
                  <Divider style={{ margin: '4px 0' }} />
                  <Space direction="vertical" size={6} style={{ width: '100%' }}>
                    <Space size={8} wrap>
                      <Input
                        size="small"
                        aria-label="透视源范围"
                        value={spreadsheetPivotSourceRange}
                        placeholder="A1:D5"
                        style={{ width: 110 }}
                        onChange={event => setSpreadsheetPivotSourceRange(event.target.value.toUpperCase())}
                      />
                      <Input
                        size="small"
                        aria-label="透视输出单元格"
                        value={spreadsheetPivotTargetCell}
                        placeholder="F1"
                        style={{ width: 92 }}
                        onChange={event => setSpreadsheetPivotTargetCell(event.target.value.toUpperCase())}
                      />
                      <Input
                        size="small"
                        aria-label="透视行字段"
                        value={spreadsheetPivotRowField}
                        placeholder="行字段"
                        style={{ width: 96 }}
                        onChange={event => setSpreadsheetPivotRowField(event.target.value)}
                      />
                      <Input
                        size="small"
                        aria-label="透视列字段"
                        value={spreadsheetPivotColumnField}
                        placeholder="列字段"
                        style={{ width: 96 }}
                        onChange={event => setSpreadsheetPivotColumnField(event.target.value)}
                      />
                      <Button
                        size="small"
                        aria-label="添加透视指标"
                        icon={<PlusOutlined />}
                        onClick={addSpreadsheetPivotMetric}
                      >
                        添加指标
                      </Button>
                    </Space>
                    <Space size={6} wrap>
                      <Tag color="geekblue">字段</Tag>
                      {spreadsheetPivotFields.length ? spreadsheetPivotFields.map(field => (
                        <Tag
                          key={`spreadsheet-pivot-field-${field}`}
                          aria-label={`透视字段 ${field}`}
                          draggable
                          color="blue"
                          style={{ cursor: 'grab' }}
                          onDragStart={event => dragSpreadsheetPivotField(event, field)}
                        >
                          {field}
                          <Button
                            size="small"
                            type="text"
                            aria-label={`将 ${field} 设为透视行字段`}
                            onClick={() => assignSpreadsheetPivotField('ROW', field)}
                          >
                            行
                          </Button>
                          <Button
                            size="small"
                            type="text"
                            aria-label={`将 ${field} 设为透视列字段`}
                            onClick={() => assignSpreadsheetPivotField('COLUMN', field)}
                          >
                            列
                          </Button>
                          <Button
                            size="small"
                            type="text"
                            aria-label={`将 ${field} 添加为透视指标`}
                            onClick={() => assignSpreadsheetPivotField('METRIC', field)}
                          >
                            指标
                          </Button>
                        </Tag>
                      )) : <Text type="secondary" style={{ fontSize: 12 }}>源范围首行暂无字段</Text>}
                    </Space>
                    <Space size={6} wrap>
                      <div
                        aria-label="透视行字段放置区"
                        onDragOver={event => event.preventDefault()}
                        onDrop={event => dropSpreadsheetPivotField(event, 'ROW')}
                        style={spreadsheetPivotDropZoneStyle}
                      >
                        <Tag color="purple">行</Tag>
                        <Text style={{ fontSize: 12 }}>{spreadsheetPivotRowField || '-'}</Text>
                      </div>
                      <div
                        aria-label="透视列字段放置区"
                        onDragOver={event => event.preventDefault()}
                        onDrop={event => dropSpreadsheetPivotField(event, 'COLUMN')}
                        style={spreadsheetPivotDropZoneStyle}
                      >
                        <Tag color="purple">列</Tag>
                        <Text style={{ fontSize: 12 }}>{spreadsheetPivotColumnField || '-'}</Text>
                      </div>
                      <div
                        aria-label="透视指标放置区"
                        onDragOver={event => event.preventDefault()}
                        onDrop={event => dropSpreadsheetPivotField(event, 'METRIC')}
                        style={{ ...spreadsheetPivotDropZoneStyle, minWidth: 220 }}
                      >
                        <Tag color="purple">指标</Tag>
                        <Text style={{ fontSize: 12 }}>
                          {spreadsheetPivotMetrics.map(metric => metric.field.trim()).filter(Boolean).join(' / ') || '-'}
                        </Text>
                      </div>
                    </Space>
                    {spreadsheetPivotMetrics.map((metric, index) => (
                      <Space key={metric.id} size={6} wrap>
                        <Tag color="blue">指标 {index + 1}</Tag>
                        <Input
                          size="small"
                          aria-label={`透视指标字段 ${index + 1}`}
                          value={metric.field}
                          placeholder="指标字段"
                          style={{ width: 104 }}
                          onChange={event => updateSpreadsheetPivotMetric(metric.id, { field: event.target.value })}
                        />
                        <Select
                          size="small"
                          aria-label={`透视指标聚合 ${index + 1}`}
                          value={metric.aggregation}
                          style={{ width: 96 }}
                          options={SPREADSHEET_PIVOT_AGGREGATION_OPTIONS}
                          onChange={aggregation => updateSpreadsheetPivotMetric(metric.id, { aggregation })}
                        />
                        <Input
                          size="small"
                          aria-label={`透视指标标签 ${index + 1}`}
                          value={metric.label}
                          placeholder={metric.field || '显示标签'}
                          style={{ width: 118 }}
                          onChange={event => updateSpreadsheetPivotMetric(metric.id, { label: event.target.value })}
                        />
                        <Button
                          size="small"
                          aria-label={`上移透视指标 ${index + 1}`}
                          icon={<ArrowUpOutlined />}
                          disabled={index === 0}
                          onClick={() => moveSpreadsheetPivotMetric(metric.id, 'up')}
                        />
                        <Button
                          size="small"
                          aria-label={`下移透视指标 ${index + 1}`}
                          icon={<ArrowDownOutlined />}
                          disabled={index === spreadsheetPivotMetrics.length - 1}
                          onClick={() => moveSpreadsheetPivotMetric(metric.id, 'down')}
                        />
                        <Button
                          size="small"
                          aria-label={`删除透视指标 ${index + 1}`}
                          icon={<DeleteOutlined />}
                          disabled={spreadsheetPivotMetrics.length <= 1}
                          onClick={() => removeSpreadsheetPivotMetric(metric.id)}
                        />
                      </Space>
                    ))}
                    <Space size={6} wrap>
                      <Tag color="cyan">预览</Tag>
                      <Text aria-label="透视输出列预览" style={{ fontSize: 12 }}>
                        {spreadsheetPivotPreviewColumnLabels.slice(0, 8).join(' / ') || '暂无输出列'}
                      </Text>
                    </Space>
                    {spreadsheetPivotPreviewGridRows.length > 0 && (
                      <div
                        aria-label="透视结果预览"
                        style={spreadsheetPivotPreviewGridStyle}
                      >
                        {spreadsheetPivotPreviewGridRows.flatMap(row => row.cells.map(cell => (
                          <div
                            key={`${row.rowKey}-${cell.cellKey}`}
                            aria-label={`透视预览单元格 ${cell.cellKey}`}
                            style={spreadsheetPivotPreviewCellStyle}
                          >
                            {cell.value}
                          </div>
                        )))}
                      </div>
                    )}
                    <Space size={8} wrap>
                    <Button
                      size="small"
                      aria-label="生成交叉表透视"
                      icon={<AppstoreOutlined />}
                      onClick={generateSelectedSpreadsheetPivotTable}
                    >
                      生成交叉表透视
                    </Button>
                    <Button
                      size="small"
                      aria-label="生成多指标透视"
                      icon={<AppstoreOutlined />}
                      onClick={generateSelectedSpreadsheetMultiMetricPivotTable}
                    >
                      多指标透视
                    </Button>
                    </Space>
                  </Space>
                </Space>
                <Space size={8} wrap>
                  <Button
                    size="small"
                    type="primary"
                    aria-label="保存电子表格草稿"
                    icon={<SaveOutlined />}
                    loading={savingSpreadsheet === (selectedSpreadsheetResource?.spreadsheetKey ?? `${dashboardPreset.dashboardKey}-spreadsheet`)}
                    onClick={saveSpreadsheetDraft}
                  >
                    保存草稿
                  </Button>
                  <Button
                    size="small"
                    icon={<ShareAltOutlined />}
                    disabled={!selectedSpreadsheetResource}
                    loading={publishingSpreadsheet === selectedSpreadsheetResource?.spreadsheetKey}
                    onClick={publishSpreadsheet}
                  >
                    发布
                  </Button>
                  <Button
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    disabled={!selectedSpreadsheetResource}
                    loading={archivingSpreadsheet === selectedSpreadsheetResource?.spreadsheetKey}
                    onClick={archiveSpreadsheet}
                  >
                    归档
                  </Button>
                  <Button
                    size="small"
                    icon={<SyncOutlined />}
                    loading={loadingSpreadsheetVersions}
                    disabled={!selectedSpreadsheetResource}
                    onClick={() => reloadSpreadsheetVersions(selectedSpreadsheetResource?.spreadsheetKey)}
                  >
                    版本
                  </Button>
                </Space>
                <Table
                  rowKey={row => `${row.spreadsheetKey}-${row.version}`}
                  size="small"
                  loading={loadingSpreadsheetVersions}
                  pagination={false}
                  columns={spreadsheetVersionTableColumns}
                  dataSource={spreadsheetVersions}
                  locale={{ emptyText: '暂无电子表格版本' }}
                  scroll={{ x: 720 }}
                />
              </Space>
            </Col>
          </Row>
        </Space>
      </div>

      <div style={datasetBandStyle}>
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <FolderOpenOutlined />
              <Text strong>资源位置</Text>
              <Tag>{resourceLocations.length} 条</Tag>
              <Tag color={selectedMoveLocation?.folderKey ? 'blue' : 'default'}>
                {selectedMoveTarget.resourceKey ? resourceFolderLabel(selectedMoveLocation) : '未选择资源'}
              </Tag>
            </Space>
            <Space size={8} wrap>
              <Select
                size="small"
                value={selectedMoveTarget.value}
                style={{ width: 180 }}
                options={resourceMoveTargets.map(target => ({
                  label: `${target.label} · ${target.resourceKey || '无可用资源'}`,
                  value: target.value,
                  disabled: target.disabled,
                }))}
                onChange={setMoveResourceTarget}
              />
              {selectedMoveTarget.value === 'BIG_SCREEN' ? (
                <Select
                  size="small"
                  value={selectedBigScreenKey ?? undefined}
                  style={{ width: 220 }}
                  placeholder="选择大屏"
                  loading={loadingBigScreens}
                  disabled={bigScreenResourceOptions.length === 0}
                  options={bigScreenResourceOptions}
                  onChange={setSelectedBigScreenKey}
                />
              ) : null}
              {selectedMoveTarget.value === 'SPREADSHEET' ? (
                <Select
                  size="small"
                  value={selectedSpreadsheetKey ?? undefined}
                  style={{ width: 220 }}
                  placeholder="选择电子表格"
                  loading={loadingSpreadsheets}
                  disabled={spreadsheetResourceOptions.length === 0}
                  options={spreadsheetResourceOptions}
                  onChange={setSelectedSpreadsheetKey}
                />
              ) : null}
              <Input
                size="small"
                allowClear
                prefix={<FolderOpenOutlined />}
                placeholder="文件夹 key，留空为根目录"
                value={moveFolderKey}
                onChange={event => setMoveFolderKey(event.target.value)}
                style={{ width: 220 }}
              />
              <Button
                size="small"
                type="primary"
                icon={<FolderOpenOutlined />}
                disabled={!selectedMoveTarget.resourceKey}
                loading={movingResource === resourceLocationIndexKey(selectedMoveTarget.resourceType, selectedMoveTarget.resourceKey)}
                onClick={moveSelectedResource}
              >
                移动
              </Button>
              <Badge
                status={loadingSharedResourceTargets ? 'processing' : 'success'}
                text={loadingSharedResourceTargets ? '同步中' : '已就绪'}
              />
            </Space>
          </Space>
          <Table
            rowKey="__tableRowKey"
            size="small"
            loading={loadingResourceLocations}
            pagination={false}
            columns={resourceLocationColumns as ColumnsType<ResourceLocationTableRow>}
            dataSource={resourceLocationTableRows}
            locale={{ emptyText: '暂无资源位置，移动资源后会记录文件夹和排序' }}
            scroll={{ x: 760 }}
          />
          <Divider style={{ margin: '4px 0' }} />
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <ShareAltOutlined />
              <Text strong>资源转让</Text>
              <Tag>{resourceOwnerships.length} 条</Tag>
              <Tag color={selectedOwnership?.ownerUser ? 'purple' : 'default'}>
                {selectedMoveTarget.resourceKey ? resourceOwnerLabel(selectedOwnership) : '未选择资源'}
              </Tag>
            </Space>
            <Space size={8} wrap>
              <Input
                size="small"
                allowClear
                prefix={<ShareAltOutlined />}
                placeholder="新负责人账号"
                value={transferOwnerUser}
                onChange={event => setTransferOwnerUser(event.target.value)}
                style={{ width: 220 }}
              />
              <Button
                size="small"
                type="primary"
                icon={<ShareAltOutlined />}
                disabled={!selectedMoveTarget.resourceKey || !transferOwnerUser.trim()}
                loading={transferringResource === resourceLocationIndexKey(selectedMoveTarget.resourceType, selectedMoveTarget.resourceKey)}
                onClick={transferSelectedResource}
              >
                转让
              </Button>
              <Badge status={loadingResourceOwnerships ? 'processing' : 'success'} text={loadingResourceOwnerships ? '同步中' : '已就绪'} />
            </Space>
          </Space>
          <Table
            rowKey="__tableRowKey"
            size="small"
            loading={loadingResourceOwnerships}
            pagination={false}
            columns={resourceOwnershipColumns as ColumnsType<ResourceOwnershipTableRow>}
            dataSource={resourceOwnershipTableRows}
            locale={{ emptyText: '暂无资源负责人记录，转让后会记录当前负责人' }}
            scroll={{ x: 700 }}
          />
          <Divider style={{ margin: '4px 0' }} />
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <StarOutlined />
              <Text strong>资源收藏</Text>
              <Tag>{resourceFavorites.length} 条</Tag>
              <Tag color={selectedFavorite?.favorite ? 'gold' : 'default'}>
                {selectedMoveTarget.resourceKey ? resourceFavoriteLabel(selectedFavorite) : '未选择资源'}
              </Tag>
            </Space>
            <Space size={8} wrap>
              <Button
                size="small"
                type={selectedFavorite?.favorite ? 'default' : 'primary'}
                icon={<StarOutlined />}
                disabled={!selectedMoveTarget.resourceKey}
                loading={favoritingResource === resourceLocationIndexKey(selectedMoveTarget.resourceType, selectedMoveTarget.resourceKey)}
                onClick={toggleSelectedFavorite}
              >
                {selectedFavorite?.favorite ? '取消收藏' : '收藏'}
              </Button>
              <Badge status={loadingResourceFavorites ? 'processing' : 'success'} text={loadingResourceFavorites ? '同步中' : '已就绪'} />
            </Space>
          </Space>
          <Table
            rowKey="__tableRowKey"
            size="small"
            loading={loadingResourceFavorites}
            pagination={false}
            columns={resourceFavoriteColumns as ColumnsType<ResourceFavoriteTableRow>}
            dataSource={resourceFavoriteTableRows}
            locale={{ emptyText: '暂无收藏资源，收藏后会出现在这里' }}
            scroll={{ x: 740 }}
          />
          <Divider style={{ margin: '4px 0' }} />
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <MessageOutlined />
              <Text strong>协作评论</Text>
              <Tag>{resourceComments.length} 条</Tag>
              <Tag color={resourceLock?.locked ? 'volcano' : 'default'}>
                {resourceLockLabel(resourceLock ?? undefined)}
              </Tag>
            </Space>
            <Space size={8} wrap>
              <Button
                size="small"
                icon={<LockOutlined />}
                disabled={!selectedMoveTarget.resourceKey || resourceLock?.locked === true}
                loading={savingResourceLock === 'acquire'}
                onClick={acquireSelectedResourceLock}
              >
                获取编辑锁
              </Button>
              <Button
                size="small"
                disabled={!selectedMoveTarget.resourceKey || !resourceLock?.locked}
                loading={savingResourceLock === 'release'}
                onClick={releaseSelectedResourceLock}
              >
                释放锁
              </Button>
              <Badge
                status={loadingResourceComments || loadingResourceLock ? 'processing' : 'success'}
                text={loadingResourceComments || loadingResourceLock ? '同步中' : '已就绪'}
              />
            </Space>
          </Space>
          <Row gutter={12}>
            <Col xs={24} lg={10}>
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                <Input.TextArea
                  rows={3}
                  value={resourceCommentText}
                  placeholder={selectedMoveTarget.resourceKey ? '写评论，仪表板评论会关联当前选中组件' : '请选择资源'}
                  onChange={event => setResourceCommentText(event.target.value)}
                  maxLength={4000}
                />
                <Button
                  size="small"
                  type="primary"
                  icon={<MessageOutlined />}
                  disabled={!selectedMoveTarget.resourceKey || !resourceCommentText.trim()}
                  loading={savingResourceComment}
                  onClick={addSelectedResourceComment}
                >
                  发送评论
                </Button>
              </Space>
            </Col>
            <Col xs={24} lg={14}>
              <List
                size="small"
                loading={loadingResourceComments}
                dataSource={resourceComments}
                locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无评论" /> }}
                renderItem={comment => (
                  <List.Item
                    key={comment.id ?? `${comment.createdBy}-${comment.createdAt}`}
                    actions={[
                      <Button
                        key="delete"
                        size="small"
                        type="link"
                        danger
                        icon={<DeleteOutlined />}
                        loading={deletingResourceComment === comment.id}
                        disabled={!comment.id}
                        onClick={() => deleteSelectedResourceComment(comment.id)}
                      >
                        删除
                      </Button>,
                    ]}
                  >
                    <List.Item.Meta
                      title={(
                        <Space size={8} wrap>
                          <Text strong>{comment.createdBy ?? 'system'}</Text>
                          <Tag>{resourceCommentScopeLabel(comment)}</Tag>
                          <Text type="secondary" style={{ fontSize: 12 }}>{comment.createdAt ?? '-'}</Text>
                        </Space>
                      )}
                      description={<Text>{comment.commentText}</Text>}
                    />
                  </List.Item>
                )}
              />
            </Col>
          </Row>
          <Divider style={{ margin: '4px 0' }} />
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <CheckCircleOutlined />
              <Text strong>发布审批</Text>
              <Tag>{selectedPublishApprovals.length} 条</Tag>
              <Tag color={publishApprovalStatusColor(latestPublishApproval?.status)}>
                {selectedMoveTarget.resourceKey ? publishApprovalStatusLabel(latestPublishApproval?.status) : '未选择资源'}
              </Tag>
            </Space>
            <Badge
              status={loadingPublishApprovals ? 'processing' : 'success'}
              text={loadingPublishApprovals ? '同步中' : '已就绪'}
            />
          </Space>
          <Row gutter={12}>
            <Col xs={24} lg={10}>
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                <Input.TextArea
                  rows={3}
                  value={publishApprovalReason}
                  placeholder={selectedMoveTarget.resourceKey ? '填写发布审批理由' : '请选择资源'}
                  onChange={event => setPublishApprovalReason(event.target.value)}
                  maxLength={512}
                />
                <Button
                  size="small"
                  type="primary"
                  icon={<CheckCircleOutlined />}
                  disabled={!selectedMoveTarget.resourceKey}
                  loading={savingPublishApproval === 'request'}
                  onClick={requestSelectedPublishApproval}
                >
                  申请审批
                </Button>
              </Space>
            </Col>
            <Col xs={24} lg={14}>
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                <Input
                  size="small"
                  allowClear
                  value={publishReviewComment}
                  placeholder={pendingPublishApproval ? '审批意见' : '暂无待审批请求'}
                  onChange={event => setPublishReviewComment(event.target.value)}
                  maxLength={512}
                />
                <Space size={8} wrap>
                  <Button
                    size="small"
                    icon={<CheckCircleOutlined />}
                    disabled={!pendingPublishApproval}
                    loading={savingPublishApproval === 'APPROVED'}
                    onClick={() => reviewSelectedPublishApproval('APPROVED')}
                  >
                    通过
                  </Button>
                  <Button
                    size="small"
                    danger
                    icon={<ExclamationCircleOutlined />}
                    disabled={!pendingPublishApproval}
                    loading={savingPublishApproval === 'REJECTED'}
                    onClick={() => reviewSelectedPublishApproval('REJECTED')}
                  >
                    驳回
                  </Button>
                </Space>
                <List
                  size="small"
                  loading={loadingPublishApprovals}
                  dataSource={selectedPublishApprovals}
                  locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无发布审批" /> }}
                  renderItem={approval => (
                    <List.Item key={approval.id ?? `${approval.resourceType}-${approval.resourceKey}-${approval.requestedAt}`}>
                      <List.Item.Meta
                        title={(
                          <Space size={8} wrap>
                            <Tag color={publishApprovalStatusColor(approval.status)}>
                              {publishApprovalStatusLabel(approval.status)}
                            </Tag>
                            <Text strong>{approval.requestedBy ?? 'system'}</Text>
                            <Text type="secondary" style={{ fontSize: 12 }}>{approval.requestedAt ?? '-'}</Text>
                          </Space>
                        )}
                        description={(
                          <Space direction="vertical" size={2}>
                            <Text>{approval.reason ?? '-'}</Text>
                            {approval.reviewedBy ? (
                              <Text type="secondary" style={{ fontSize: 12 }}>
                                {approval.reviewedBy}：{approval.reviewComment ?? '-'}
                              </Text>
                            ) : null}
                          </Space>
                        )}
                      />
                    </List.Item>
                  )}
                />
              </Space>
            </Col>
          </Row>
          <Divider style={{ margin: '4px 0' }} />
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <ApiOutlined />
              <Text strong>门户查看态</Text>
              <Tag color="green">
                {portalRuntimeResources.reduce((total, portal) => total + portal.menus.length, 0)} 可见菜单
              </Tag>
            </Space>
            <Badge
              status={loadingPortalRuntime ? 'processing' : 'success'}
              text={loadingPortalRuntime ? '同步中' : '已就绪'}
            />
          </Space>
          <Table
            rowKey="portalKey"
            size="small"
            loading={loadingPortalRuntime}
            pagination={false}
            columns={portalResourceColumns}
            dataSource={portalRuntimeResources}
            locale={{ emptyText: '暂无已发布或当前用户可见的数据门户' }}
            scroll={{ x: 760 }}
          />
          <Divider style={{ margin: '4px 0' }} />
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <ApiOutlined />
              <Text strong>数据门户资产</Text>
            </Space>
            <Badge status={loadingPortals ? 'processing' : 'success'} text={loadingPortals ? '同步中' : '已就绪'} />
          </Space>
          <Table
            rowKey="portalKey"
            size="small"
            loading={loadingPortals}
            pagination={false}
            columns={portalResourceColumns}
            dataSource={portalResources}
            rowSelection={{
              type: 'radio',
              selectedRowKeys: selectedPortalResource ? [selectedPortalResource.portalKey] : [],
              onChange: keys => setSelectedPortalKey(String(keys[0] ?? '')),
            }}
            onRow={row => ({
              onClick: () => setSelectedPortalKey(row.portalKey),
            })}
            locale={{ emptyText: '暂无持久化数据门户' }}
            scroll={{ x: 760 }}
          />
          <Space direction="vertical" size={8} style={{ width: '100%', padding: 10, border: '1px solid #eef2f7', borderRadius: 6 }}>
            <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
              <Space>
                <ApiOutlined />
                <Text strong>门户编辑器</Text>
                <Tag color={selectedPortalResource ? 'blue' : 'default'}>{selectedPortalResource?.portalKey ?? '未选择门户'}</Tag>
              </Space>
              <Space size={6}>
                <Button
                  size="small"
                  aria-label="生成门户嵌入"
                  icon={<LinkOutlined />}
                  disabled={!selectedPortalResource}
                  loading={creatingEmbedTicket}
                  onClick={createPortalEmbedTicket}
                >
                  生成门户嵌入
                </Button>
                <Button
                  size="small"
                  type="primary"
                  aria-label="保存门户草稿"
                  icon={<SaveOutlined />}
                  disabled={!selectedPortalResource}
                  loading={savingPortal === selectedPortalResource?.portalKey}
                  onClick={savePortalDraft}
                >
                  保存草稿
                </Button>
              </Space>
            </Space>
            <Space size={8} wrap>
              <select
                aria-label="门户导航布局"
                value={String(selectedPortalResource?.theme?.navigationLayout ?? 'top')}
                style={{ width: 132, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                disabled={!selectedPortalResource}
                onChange={event => updateSelectedPortalNavigationConfig({ navigationLayout: event.target.value })}
              >
                <option value="top">顶部导航</option>
                <option value="left">左侧导航</option>
                <option value="dual">双导航</option>
              </select>
              <select
                aria-label="门户默认主页"
                value={String(selectedPortalResource?.theme?.defaultMenuKey ?? selectedPortalResource?.menus[0]?.menuKey ?? '')}
                style={{ width: 220, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                disabled={!selectedPortalResource || portalMenuOptions.length === 0}
                onChange={event => updateSelectedPortalNavigationConfig({ defaultMenuKey: event.target.value })}
              >
                {portalMenuOptions.map(option => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
              <Switch
                size="small"
                aria-label="门户菜单搜索"
                checked={selectedPortalResource?.theme?.menuSearchEnabled === true}
                disabled={!selectedPortalResource}
                onChange={menuSearchEnabled => updateSelectedPortalNavigationConfig({ menuSearchEnabled })}
              />
              <Text type="secondary">菜单搜索</Text>
              <Switch
                size="small"
                aria-label="门户全屏"
                checked={selectedPortalResource?.theme?.fullScreenEnabled === true}
                disabled={!selectedPortalResource}
                onChange={fullScreenEnabled => updateSelectedPortalNavigationConfig({ fullScreenEnabled })}
              />
              <Text type="secondary">全屏</Text>
              <Switch
                size="small"
                aria-label="门户移动端"
                checked={selectedPortalResource?.theme?.mobileEnabled === true}
                disabled={!selectedPortalResource}
                onChange={mobileEnabled => updateSelectedPortalNavigationConfig({ mobileEnabled })}
              />
              <Text type="secondary">移动端</Text>
            </Space>
            <Space size={8} wrap>
              <Input
                size="small"
                aria-label="门户LOGO"
                value={String(selectedPortalResource?.theme?.logoUrl ?? '')}
                placeholder="logo url"
                style={{ width: 220 }}
                disabled={!selectedPortalResource}
                onChange={event => updateSelectedPortalNavigationConfig({ logoUrl: event.target.value })}
              />
              <Input
                size="small"
                aria-label="门户主标题"
                value={String(selectedPortalResource?.theme?.title ?? '')}
                placeholder="门户主标题"
                style={{ width: 160 }}
                disabled={!selectedPortalResource}
                onChange={event => updateSelectedPortalNavigationConfig({ title: event.target.value })}
              />
              <Input
                size="small"
                aria-label="门户副标题"
                value={String(selectedPortalResource?.theme?.subtitle ?? '')}
                placeholder="门户副标题"
                style={{ width: 180 }}
                disabled={!selectedPortalResource}
                onChange={event => updateSelectedPortalNavigationConfig({ subtitle: event.target.value })}
              />
              <Input
                size="small"
                aria-label="门户页脚"
                value={String(selectedPortalResource?.theme?.footerText ?? '')}
                placeholder="页脚"
                style={{ width: 160 }}
                disabled={!selectedPortalResource}
                onChange={event => updateSelectedPortalNavigationConfig({ footerText: event.target.value })}
              />
              <Input
                size="small"
                aria-label="门户别名"
                value={String(selectedPortalResource?.theme?.alias ?? '')}
                placeholder="portal-alias"
                style={{ width: 140 }}
                disabled={!selectedPortalResource}
                onChange={event => updateSelectedPortalNavigationConfig({ alias: event.target.value })}
              />
            </Space>
            <Space size={8} wrap>
              <Switch
                size="small"
                aria-label="门户面包屑"
                checked={selectedPortalResource?.theme?.breadcrumbEnabled === true}
                disabled={!selectedPortalResource}
                onChange={breadcrumbEnabled => updateSelectedPortalNavigationConfig({ breadcrumbEnabled })}
              />
              <Text type="secondary">面包屑</Text>
              <Switch
                size="small"
                aria-label="门户菜单缓存"
                checked={selectedPortalResource?.theme?.menuCacheEnabled === true}
                disabled={!selectedPortalResource}
                onChange={menuCacheEnabled => updateSelectedPortalNavigationConfig({ menuCacheEnabled })}
              />
              <Text type="secondary">菜单缓存</Text>
              <Input
                size="small"
                aria-label="门户缓存TTL"
                value={String(selectedPortalResource?.theme?.menuCacheTtlSeconds ?? '')}
                placeholder="TTL 秒"
                style={{ width: 96 }}
                disabled={!selectedPortalResource}
                onChange={event => updateSelectedPortalNavigationConfig({ menuCacheTtlSeconds: event.target.value })}
              />
            </Space>
            <Space size={8} wrap>
              <select
                aria-label="门户菜单"
                value={selectedPortalMenuKey ?? ''}
                style={{ width: 240, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                disabled={!selectedPortalResource || portalMenuOptions.length === 0}
                onChange={event => setSelectedPortalMenuKey(event.target.value)}
              >
                {portalMenuOptions.map(option => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
              <Input
                size="small"
                aria-label="门户菜单标题"
                value={String(selectedPortalMenu?.title ?? '')}
                placeholder="菜单标题"
                style={{ width: 160 }}
                disabled={!selectedPortalMenu}
                onChange={event => updateSelectedPortalMenuConfig({ title: event.target.value })}
              />
              <select
                aria-label="门户父级菜单"
                value={String(selectedPortalMenu?.parentMenuKey ?? '')}
                style={{ width: 180, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                disabled={!selectedPortalMenu}
                onChange={event => updateSelectedPortalMenuConfig({ parentMenuKey: event.target.value })}
              >
                <option value="">无父级菜单</option>
                {portalParentMenuOptions.map(option => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
              <Input
                size="small"
                aria-label="门户菜单图标"
                value={String(selectedPortalMenu?.visibility?.iconKey ?? '')}
                placeholder="icon key"
                style={{ width: 128 }}
                disabled={!selectedPortalMenu}
                onChange={event => updateSelectedPortalMenuConfig({ iconKey: event.target.value })}
              />
              <select
                aria-label="门户拖拽菜单"
                value={portalDragMenuKey ?? ''}
                style={{ width: 160, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                disabled={!selectedPortalResource || portalMenuOptions.length === 0}
                onChange={event => setPortalDragMenuKey(event.target.value)}
              >
                {portalMenuOptions.map(option => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
              <select
                aria-label="门户拖放目标菜单"
                value={portalDropTargetMenuKey ?? ''}
                style={{ width: 180, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                disabled={!selectedPortalResource || portalMenuOptions.length === 0}
                onChange={event => setPortalDropTargetMenuKey(event.target.value)}
              >
                {portalMenuOptions.map(option => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
              <select
                aria-label="门户拖放位置"
                value={portalDropPosition}
                style={{ width: 116, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                disabled={!selectedPortalResource}
                onChange={event => setPortalDropPosition(event.target.value as 'before' | 'after' | 'inside')}
              >
                <option value="before">前面</option>
                <option value="after">后面</option>
                <option value="inside">子菜单</option>
              </select>
              <Button
                size="small"
                aria-label="应用门户菜单拖放"
                disabled={!portalDragMenuKey || !portalDropTargetMenuKey || portalDragMenuKey === portalDropTargetMenuKey}
                onClick={() => applyPortalMenuDrop()}
              >
                应用拖放
              </Button>
              <Tooltip title="上移菜单">
                <Button
                  size="small"
                  aria-label="门户菜单上移"
                  icon={<ArrowUpOutlined />}
                  disabled={!selectedPortalMenuKey}
                  onClick={() => moveSelectedPortalMenu('up')}
                />
              </Tooltip>
              <Tooltip title="下移菜单">
                <Button
                  size="small"
                  aria-label="门户菜单下移"
                  icon={<ArrowDownOutlined />}
                  disabled={!selectedPortalMenuKey}
                  onClick={() => moveSelectedPortalMenu('down')}
                />
              </Tooltip>
              <Tag color="geekblue">{selectedPortalResource?.menus.length ?? 0} 菜单</Tag>
            </Space>
            <Space size={[6, 6]} wrap>
              {(selectedPortalResource?.menus ?? []).map(menu => {
                const menuKey = String(menu.menuKey ?? '')
                return (
                  <Tag
                    key={`portal-menu-drag-${menuKey}`}
                    draggable
                    aria-label={`门户菜单拖拽项 ${menuKey}`}
                    onDragStart={event => {
                      event.dataTransfer.effectAllowed = 'move'
                      event.dataTransfer.setData('application/x-bi-portal-menu', menuKey)
                      event.dataTransfer.setData('text/plain', menuKey)
                      setPortalDragMenuKey(menuKey)
                    }}
                    onDragOver={event => event.preventDefault()}
                    onDrop={event => {
                      event.preventDefault()
                      const draggedKey = event.dataTransfer.getData('application/x-bi-portal-menu') || event.dataTransfer.getData('text/plain')
                      setPortalDropTargetMenuKey(menuKey)
                      applyPortalMenuDrop(draggedKey, menuKey, portalDropPosition)
                    }}
                    style={{ cursor: 'grab' }}
                  >
                    {String(menu.title ?? menuKey)} · {menuKey}
                  </Tag>
                )
              })}
            </Space>
            {selectedPortalResource ? (
              <div style={{ padding: 10, border: '1px solid #e5e7eb', borderRadius: 6, background: '#fff' }}>
                <Space direction="vertical" size={6} style={{ width: '100%' }}>
                  <Space size={8} wrap>
                    <ApiOutlined />
                    <Text strong>门户预览</Text>
                    <Tag color="cyan">{selectedPortalResource.theme?.navigationLayout ? String(selectedPortalResource.theme.navigationLayout) : 'top'}</Tag>
                    {selectedPortalResource.theme?.breadcrumbEnabled === true && <Tag color="blue">面包屑</Tag>}
                  </Space>
                  <Space direction="vertical" size={0}>
                    <Text strong>{String(selectedPortalResource.theme?.title ?? selectedPortalResource.name)}</Text>
                    {selectedPortalResource.theme?.subtitle ? <Text type="secondary" style={{ fontSize: 12 }}>{String(selectedPortalResource.theme.subtitle)}</Text> : null}
                  </Space>
                  <Space size={[6, 6]} wrap>
                    {[...selectedPortalResource.menus].sort((left, right) => Number(left.sortOrder ?? 0) - Number(right.sortOrder ?? 0)).map(menu => (
                      <Tag key={`portal-preview-${menu.menuKey}`} color={menu.parentMenuKey ? 'purple' : 'geekblue'}>
                        {menu.title}{menu.parentMenuKey ? ` · ${menu.parentMenuKey}` : ''}
                      </Tag>
                    ))}
                  </Space>
                  {selectedPortalResource.theme?.footerText ? <Text type="secondary" style={{ fontSize: 12 }}>{String(selectedPortalResource.theme.footerText)}</Text> : null}
                </Space>
              </div>
            ) : null}
          </Space>
          <Divider style={{ margin: '4px 0' }} />
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <DeploymentUnitOutlined />
              <Text strong>数据集资产</Text>
            </Space>
            <Badge status={loadingDatasetResources ? 'processing' : 'success'} text={loadingDatasetResources ? '同步中' : '已就绪'} />
          </Space>
          <Table
            rowKey="datasetKey"
            size="small"
            loading={loadingDatasetResources}
            pagination={false}
            columns={datasetResourceColumns}
            dataSource={datasetResources}
            rowSelection={{
              type: 'radio',
              selectedRowKeys: selectedDatasetResource ? [selectedDatasetResource.datasetKey] : [],
              onChange: keys => setSelectedDatasetKey(String(keys[0] ?? '')),
            }}
            onRow={row => ({
              onClick: () => setSelectedDatasetKey(row.datasetKey),
            })}
            locale={{ emptyText: '暂无持久化数据集资产' }}
            scroll={{ x: 760 }}
          />
          <Space direction="vertical" size={8} style={{ width: '100%', padding: 10, border: '1px solid #eef2f7', borderRadius: 6 }}>
            <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
              <Space>
                <DeploymentUnitOutlined />
                <Text strong>数据集编辑器</Text>
                <Tag color={selectedDatasetResource ? 'blue' : 'default'}>{selectedDatasetResource?.datasetKey ?? '未选择数据集'}</Tag>
              </Space>
              <Button
                size="small"
                type="primary"
                aria-label="保存数据集草稿"
                icon={<SaveOutlined />}
                disabled={!selectedDatasetResource}
                loading={savingDataset === selectedDatasetResource?.datasetKey}
                onClick={saveDatasetDraft}
              >
                保存草稿
              </Button>
            </Space>
            <Space size={8} wrap>
              <Input
                size="small"
                aria-label="数据集字段选择"
                value={selectedDatasetFieldKeys}
                placeholder="field_a,field_b"
                style={{ width: 220 }}
                disabled={!selectedDatasetResource}
                onChange={event => setSelectedDatasetFieldKeys(event.target.value)}
              />
              <Input
                size="small"
                aria-label="数据集字段文件夹"
                value={datasetFieldFolderKey}
                placeholder="字段文件夹"
                style={{ width: 180 }}
                disabled={!selectedDatasetResource}
                onChange={event => setDatasetFieldFolderKey(event.target.value)}
              />
              <Button
                size="small"
                aria-label="批量移动字段到文件夹"
                icon={<DeploymentUnitOutlined />}
                disabled={!selectedDatasetResource || parseChartFieldList(selectedDatasetFieldKeys).length === 0}
                onClick={moveSelectedDatasetFieldsToFolder}
              >
                批量移动
              </Button>
              <Button
                size="small"
                aria-label="复制数据集草稿"
                icon={<CopyOutlined />}
                disabled={!selectedDatasetResource}
                onClick={copySelectedDatasetDraft}
              >
                复制
              </Button>
            </Space>
            <Space size={[4, 4]} wrap>
              <Tag color="blue">{selectedDatasetResource?.fields.length ?? 0} 字段</Tag>
              <Tag color="geekblue">{selectedDatasetResource?.metrics.length ?? 0} 指标</Tag>
              {selectedDatasetFolderSummary ? (
                <Text type="secondary" style={{ fontSize: 12 }}>{selectedDatasetFolderSummary}</Text>
              ) : (
                <Text type="secondary" style={{ fontSize: 12 }}>字段文件夹：未配置</Text>
              )}
            </Space>
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <Text strong style={{ fontSize: 12 }}>字段大纲</Text>
              <Space size={8} wrap>
                <select
                  aria-label="数据集拖拽字段"
                  value={datasetDragFieldKey ?? ''}
                  style={{ width: 150, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                  disabled={!selectedDatasetResource}
                  onChange={event => setDatasetDragFieldKey(event.target.value || null)}
                >
                  <option value="">选择字段</option>
                  {(selectedDatasetResource?.fields ?? []).map(field => (
                    <option key={`dataset-drag-${field.fieldKey}`} value={field.fieldKey}>{field.fieldKey}</option>
                  ))}
                </select>
                <select
                  aria-label="数据集拖放目标字段"
                  value={datasetDropTargetFieldKey ?? ''}
                  style={{ width: 170, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                  disabled={!selectedDatasetResource}
                  onChange={event => setDatasetDropTargetFieldKey(event.target.value || null)}
                >
                  <option value="">目标字段</option>
                  {(selectedDatasetResource?.fields ?? []).map(field => (
                    <option key={`dataset-drop-target-${field.fieldKey}`} value={field.fieldKey}>{field.fieldKey}</option>
                  ))}
                </select>
                <select
                  aria-label="数据集字段拖放位置"
                  value={datasetDropPosition}
                  style={{ width: 116, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                  disabled={!selectedDatasetResource}
                  onChange={event => setDatasetDropPosition(event.target.value as 'before' | 'after')}
                >
                  <option value="before">前面</option>
                  <option value="after">后面</option>
                </select>
                <Input
                  size="small"
                  aria-label="数据集字段拖放文件夹"
                  value={datasetDropFolderKey}
                  placeholder="目标文件夹"
                  style={{ width: 150 }}
                  disabled={!selectedDatasetResource}
                  onChange={event => setDatasetDropFolderKey(event.target.value)}
                />
                <Button
                  size="small"
                  aria-label="应用数据集字段拖放"
                  disabled={!datasetDragFieldKey || !datasetDropTargetFieldKey || datasetDragFieldKey === datasetDropTargetFieldKey}
                  onClick={() => applyDatasetFieldDrop()}
                >
                  应用字段拖放
                </Button>
              </Space>
              {datasetFieldTreeGroups.length === 0 ? (
                <Text type="secondary" style={{ fontSize: 12 }}>暂无字段</Text>
              ) : datasetFieldTreeGroups.map(([folderKey, fields]) => (
                <Space key={folderKey} direction="vertical" size={4} style={{ width: '100%' }}>
                  <Tag color={folderKey === '未分组' ? 'default' : 'cyan'}>{folderKey}</Tag>
                  <Space size={[4, 4]} wrap>
                    {fields.map(field => (
                      <Button
                        key={field.fieldKey}
                        size="small"
                        aria-label={`字段详情 ${field.fieldKey}`}
                        type={selectedDatasetDetailField?.fieldKey === field.fieldKey ? 'primary' : 'default'}
                        draggable
                        onDragStart={event => {
                          event.dataTransfer.effectAllowed = 'move'
                          event.dataTransfer.setData('application/x-bi-dataset-field', field.fieldKey)
                          event.dataTransfer.setData('text/plain', field.fieldKey)
                          setDatasetDragFieldKey(field.fieldKey)
                        }}
                        onDragOver={event => event.preventDefault()}
                        onDrop={event => {
                          event.preventDefault()
                          const draggedKey = event.dataTransfer.getData('application/x-bi-dataset-field') || event.dataTransfer.getData('text/plain')
                          setDatasetDropTargetFieldKey(field.fieldKey)
                          applyDatasetFieldDrop(draggedKey, field.fieldKey)
                        }}
                        onClick={() => setSelectedDatasetDetailFieldKey(field.fieldKey)}
                      >
                        {field.displayName || field.fieldKey}
                      </Button>
                    ))}
                  </Space>
                </Space>
              ))}
            </Space>
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <Text strong style={{ fontSize: 12 }}>字段详情</Text>
              <Space size={8} wrap>
                <Input
                  size="small"
                  aria-label="数据集字段显示名"
                  value={selectedDatasetDetailField?.displayName ?? ''}
                  placeholder="展示名"
                  style={{ width: 160 }}
                  disabled={!selectedDatasetDetailField}
                  onChange={event => updateSelectedDatasetField({ displayName: event.target.value })}
                />
                <select
                  aria-label="数据集字段角色"
                  value={selectedDatasetDetailField?.role ?? 'DIMENSION'}
                  style={{ width: 116, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                  disabled={!selectedDatasetDetailField}
                  onChange={event => updateSelectedDatasetField({ role: event.target.value as 'DIMENSION' | 'MEASURE' })}
                >
                  <option value="DIMENSION">维度</option>
                  <option value="MEASURE">度量</option>
                </select>
                <Input
                  size="small"
                  aria-label="数据集字段语义类型"
                  value={selectedDatasetDetailField?.semanticType ?? ''}
                  placeholder="semantic"
                  style={{ width: 130 }}
                  disabled={!selectedDatasetDetailField}
                  onChange={event => updateSelectedDatasetField({ semanticType: event.target.value || null })}
                />
                <select
                  aria-label="数据集字段默认聚合"
                  value={selectedDatasetDetailField?.defaultAggregation ?? 'NONE'}
                  style={{ width: 116, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                  disabled={!selectedDatasetDetailField}
                  onChange={event => updateSelectedDatasetField({ defaultAggregation: event.target.value || null })}
                >
                  <option value="NONE">NONE</option>
                  <option value="SUM">SUM</option>
                  <option value="COUNT">COUNT</option>
                  <option value="AVG">AVG</option>
                  <option value="MAX">MAX</option>
                  <option value="MIN">MIN</option>
                </select>
                <Input
                  size="small"
                  aria-label="数据集字段格式"
                  value={selectedDatasetDetailField?.formatPattern ?? ''}
                  placeholder="format"
                  style={{ width: 130 }}
                  disabled={!selectedDatasetDetailField}
                  onChange={event => updateSelectedDatasetField({ formatPattern: event.target.value || null })}
                />
                <Switch
                  size="small"
                  aria-label="数据集字段可见"
                  checked={Boolean(selectedDatasetDetailField?.visible)}
                  disabled={!selectedDatasetDetailField}
                  onChange={checked => updateSelectedDatasetField({ visible: checked })}
                />
                <Text type="secondary" style={{ fontSize: 12 }}>可见</Text>
              </Space>
            </Space>
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <Text strong style={{ fontSize: 12 }}>批量字段配置</Text>
              <Space size={8} wrap>
                <select
                  aria-label="数据集批量角色"
                  value={datasetBatchRole}
                  style={{ width: 116, height: 24, border: '1px solid #d9d9d9', borderRadius: 4, padding: '0 8px' }}
                  disabled={!selectedDatasetResource}
                  onChange={event => setDatasetBatchRole(event.target.value as 'DIMENSION' | 'MEASURE')}
                >
                  <option value="DIMENSION">维度</option>
                  <option value="MEASURE">度量</option>
                </select>
                <Input
                  size="small"
                  aria-label="数据集批量格式"
                  value={datasetBatchFormat}
                  placeholder="format"
                  style={{ width: 130 }}
                  disabled={!selectedDatasetResource}
                  onChange={event => setDatasetBatchFormat(event.target.value)}
                />
                <Switch
                  size="small"
                  aria-label="数据集批量可见"
                  checked={datasetBatchVisible}
                  disabled={!selectedDatasetResource}
                  onChange={setDatasetBatchVisible}
                />
                <Text type="secondary" style={{ fontSize: 12 }}>可见</Text>
                <Button
                  size="small"
                  aria-label="应用数据集字段批量配置"
                  icon={<SaveOutlined />}
                  disabled={!selectedDatasetResource || parseChartFieldList(selectedDatasetFieldKeys).length === 0}
                  onClick={applyDatasetBatchFieldConfig}
                >
                  应用批量配置
                </Button>
              </Space>
            </Space>
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <Text strong style={{ fontSize: 12 }}>批量资源治理</Text>
              <Space size={8} wrap>
                <Input
                  size="small"
                  aria-label="数据集批量资源选择"
                  value={batchDatasetResourceKeys}
                  placeholder="dataset_a,dataset_b"
                  style={{ width: 220 }}
                  disabled={datasetResources.length === 0}
                  onChange={event => setBatchDatasetResourceKeys(event.target.value)}
                />
                <Input
                  size="small"
                  aria-label="数据集批量移动文件夹"
                  value={batchDatasetFolderKey}
                  placeholder="目标文件夹"
                  style={{ width: 160 }}
                  disabled={datasetResources.length === 0}
                  onChange={event => setBatchDatasetFolderKey(event.target.value)}
                />
                <Button
                  size="small"
                  aria-label="批量移动数据集资源"
                  icon={<FolderOpenOutlined />}
                  disabled={selectedBatchDatasetResourceKeys().length === 0}
                  loading={batchDatasetResourceAction === 'move'}
                  onClick={moveBatchDatasetResources}
                >
                  批量移动
                </Button>
                <Input
                  size="small"
                  aria-label="数据集批量转让人"
                  value={batchDatasetOwnerUser}
                  placeholder="owner@example.com"
                  style={{ width: 190 }}
                  disabled={datasetResources.length === 0}
                  onChange={event => setBatchDatasetOwnerUser(event.target.value)}
                />
                <Button
                  size="small"
                  aria-label="批量转让数据集资源"
                  icon={<SwapOutlined />}
                  disabled={selectedBatchDatasetResourceKeys().length === 0 || !batchDatasetOwnerUser.trim()}
                  loading={batchDatasetResourceAction === 'transfer'}
                  onClick={transferBatchDatasetResources}
                >
                  批量转让
                </Button>
              </Space>
            </Space>
          </Space>
          <Divider style={{ margin: '4px 0' }} />
          <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
            <Space>
              <DatabaseOutlined />
              <Text strong>营销画布数据集</Text>
            </Space>
            <Badge status={loadingDatasets ? 'processing' : 'success'} text={loadingDatasets ? '同步中' : '已就绪'} />
          </Space>
          {datasets.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无预置数据集" />
          ) : (
            <Table
              rowKey="key"
              size="small"
              loading={loadingDatasets}
              pagination={false}
              columns={datasetColumns}
              dataSource={datasets}
              scroll={{ x: 760 }}
            />
          )}
        </Space>
      </div>
    </div>
  )
}

function BiBigScreenRuntimeView({
  resource,
  loading,
  route,
}: {
  resource: BiBigScreenResource | null
  loading: boolean
  route: BiRuntimeRouteLike
}) {
  const summaryRows = bigScreenResourceSummaryRows(resource)

  return (
    <div style={runtimePageStyle}>
      <div style={runtimeTopbarStyle}>
        <Space size={12} align="center">
          <div style={brandIconStyle}><LayoutOutlined /></div>
          <Space direction="vertical" size={0}>
            <Space size={8} align="center" wrap>
              <Title level={resource ? 3 : 4} style={{ margin: 0 }}>{resource?.name ?? '大屏快照'}</Title>
              {resource && <Tag color="blue">{resource.screenKey}</Tag>}
              {resource?.id != null && <Tag color="geekblue">ID #{resource.id}</Tag>}
              {resource && <Tag color={resourceStatusColor(resource.status)}>{resource.status} v{resource.version}</Tag>}
            </Space>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {resource?.description ?? `${route.resourceType ?? 'BIG_SCREEN'} · ${runtimeRouteLabel(route)}`}
            </Text>
          </Space>
        </Space>
        <Badge status={loading ? 'processing' : 'success'} text={loading ? '同步中' : '已就绪'} />
      </div>

      <main style={runtimeShellStyle}>
        {loading && !resource ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="正在加载大屏资源" />
        ) : !resource ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={`未找到大屏资源 ${runtimeRouteLabel(route)}`} />
        ) : (
          <Space direction="vertical" size={14} style={{ width: '100%' }}>
            <div
              style={{
                ...bigScreenRuntimeStageStyle,
                background: runtimeRecordString(resource.background, 'color', '#101820'),
              }}
            >
              {resource.layout.length === 0 ? (
                <div style={bigScreenRuntimeEmptyStyle}>暂无布局组件</div>
              ) : resource.layout.map((item, index) => (
                <BigScreenRuntimeWidget key={runtimeRecordString(item, 'widgetKey', `widget-${index}`)} item={item} index={index} />
              ))}
            </div>
            <Descriptions
              size="small"
              column={{ xs: 1, sm: 2, lg: 3 }}
              items={summaryRows.map(row => ({
                key: row.label,
                label: row.label,
                children: row.value,
              }))}
            />
          </Space>
        )}
      </main>
    </div>
  )
}

function BigScreenRuntimeWidget({ item, index }: { item: Record<string, unknown>; index: number }) {
  const widgetKey = runtimeRecordString(item, 'widgetKey', `widget-${index + 1}`)
  const title = runtimeRecordString(item, 'title', widgetKey)
  const resourceType = runtimeRecordString(item, 'resourceType', runtimeRecordString(item, 'type', 'WIDGET'))
  const gridX = runtimeRecordNumber(item, 'x', 0) + 1
  const gridY = runtimeRecordNumber(item, 'y', index * 4) + 1
  const gridW = Math.min(24, Math.max(3, runtimeRecordNumber(item, 'w', 8)))
  const gridH = Math.max(3, runtimeRecordNumber(item, 'h', 5))

  return (
    <div
      style={{
        ...bigScreenRuntimeWidgetStyle,
        gridColumn: `${Math.max(1, gridX)} / span ${gridW}`,
        gridRow: `${Math.max(1, gridY)} / span ${gridH}`,
      }}
    >
      <Space direction="vertical" size={8} style={{ width: '100%' }}>
        <Space size={8} align="center" wrap>
          <Text strong style={{ color: '#fff' }}>{title}</Text>
          <Tag color="cyan">{resourceType}</Tag>
        </Space>
        <Text style={{ color: '#cbd5e1', fontSize: 12 }}>{widgetKey}</Text>
        <Text style={{ color: '#94a3b8', fontSize: 12 }}>
          {runtimeRecordString(item, 'resourceKey', runtimeRecordString(item, 'datasetKey', '-'))}
        </Text>
      </Space>
    </div>
  )
}

function BiSpreadsheetRuntimeView({
  resource,
  loading,
  route,
}: {
  resource: BiSpreadsheetResource | null
  loading: boolean
  route: BiRuntimeRouteLike
}) {
  const summaryRows = spreadsheetResourceSummaryRows(resource)

  return (
    <div style={runtimePageStyle}>
      <div style={runtimeTopbarStyle}>
        <Space size={12} align="center">
          <div style={brandIconStyle}><DatabaseOutlined /></div>
          <Space direction="vertical" size={0}>
            <Space size={8} align="center" wrap>
              <Title level={resource ? 3 : 4} style={{ margin: 0 }}>{resource?.name ?? '电子表格快照'}</Title>
              {resource && <Tag color="purple">{resource.spreadsheetKey}</Tag>}
              {resource?.id != null && <Tag color="geekblue">ID #{resource.id}</Tag>}
              {resource && <Tag color={resourceStatusColor(resource.status)}>{resource.status} v{resource.version}</Tag>}
            </Space>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {resource?.description ?? `${route.resourceType ?? 'SPREADSHEET'} · ${runtimeRouteLabel(route)}`}
            </Text>
          </Space>
        </Space>
        <Badge status={loading ? 'processing' : 'success'} text={loading ? '同步中' : '已就绪'} />
      </div>

      <main style={runtimeShellStyle}>
        {loading && !resource ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="正在加载电子表格资源" />
        ) : !resource ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={`未找到电子表格资源 ${runtimeRouteLabel(route)}`} />
        ) : (
          <Space direction="vertical" size={14} style={{ width: '100%' }}>
            <Descriptions
              size="small"
              column={{ xs: 1, sm: 2, lg: 3 }}
              items={summaryRows.map(row => ({
                key: row.label,
                label: row.label,
                children: row.value,
              }))}
            />
            <div style={spreadsheetRuntimePanelStyle}>
              <Tabs
                size="small"
                items={resource.sheets.map((sheet, index) => ({
                  key: runtimeRecordString(sheet, 'sheetKey', `sheet-${index + 1}`),
                  label: runtimeRecordString(sheet, 'name', runtimeRecordString(sheet, 'sheetKey', `Sheet ${index + 1}`)),
                  children: <SpreadsheetRuntimeSheet sheet={sheet} />,
                }))}
              />
              {resource.sheets.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无工作表" />}
            </div>
          </Space>
        )}
      </main>
    </div>
  )
}

function SpreadsheetRuntimeSheet({ sheet }: { sheet: Record<string, unknown> }) {
  const cells = runtimeSheetCells(sheet)
  const cellStyles = runtimeSheetCellStyles(sheet)
  const mobileLayout = runtimeSheetMobileLayout(sheet)
  const evaluatedCells = evaluateSpreadsheetCells(cells)
  const columns = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'].slice(0, mobileLayout.enabled ? mobileLayout.columns : 8)
  const rows = Array.from({ length: 10 }, (_, index) => index + 1)
  const sheetKey = runtimeRecordString(sheet, 'sheetKey', 'summary')

  return (
    <div
      aria-label={mobileLayout.enabled ? `电子表格 ${sheetKey} 移动端视图` : `电子表格 ${sheetKey} 桌面视图`}
      style={{
        ...spreadsheetGridStyle,
        gridTemplateColumns: `42px repeat(${columns.length}, minmax(${mobileLayout.enabled ? 72 : 96}px, 1fr))`,
      }}
    >
      <div style={spreadsheetHeaderCellStyle} />
      {columns.map(column => (
        <div key={`header-${column}`} style={spreadsheetHeaderCellStyle}>{column}</div>
      ))}
      {rows.flatMap(row => [
        <div key={`row-${row}`} style={spreadsheetHeaderCellStyle}>{row}</div>,
        ...columns.map(column => {
          const cellKey = `${column}${row}`
          const value = runtimeCellText(evaluatedCells[cellKey] ?? cells[cellKey])
          const cellStyle = runtimeSpreadsheetCellStyle(cellStyles[cellKey])
          return (
            <div key={cellKey} aria-label={`电子表格单元格 ${cellKey}`} style={{ ...spreadsheetCellStyle, ...cellStyle }}>
              <Text code={value.startsWith('=')} ellipsis style={{ maxWidth: '100%', fontSize: 12 }}>
                {value || '\u00a0'}
              </Text>
            </div>
          )
        }),
      ])}
    </div>
  )
}

function PanelHeader({ icon, title }: { icon: JSX.Element; title: string }) {
  return (
    <Space size={8}>
      <span style={panelIconStyle}>{icon}</span>
      <Text strong ellipsis style={{ maxWidth: 220 }}>{title}</Text>
    </Space>
  )
}

function ResourceGroup({ title, items, emptyText, accent = false }: { title: string; items: string[]; emptyText: string; accent?: boolean }) {
  return (
    <div>
      <Text type="secondary" style={smallLabelStyle}>{title}</Text>
      <Space direction="vertical" size={6} style={{ width: '100%', marginTop: 6 }}>
        {items.length === 0 ? (
          <Text type="secondary" style={{ fontSize: 12 }}>{emptyText}</Text>
        ) : items.map((item, index) => (
          <div key={`${item}-${index}`} style={resourceRowStyle}>
            <span style={{ width: 7, height: 7, borderRadius: 2, background: accent ? '#2563eb' : '#64748b' }} />
            <Text ellipsis style={{ fontSize: 12 }}>{item}</Text>
          </div>
        ))}
      </Space>
    </div>
  )
}

function PaletteList({ items }: { items: Array<{ key: string; label: string; group: string }> }) {
  return (
    <List
      size="small"
      dataSource={items}
      renderItem={item => (
        <List.Item style={paletteItemStyle}>
          <Space size={8}>
            <span style={panelIconStyle}>{item.group === '查询控件' ? <FilterOutlined /> : <BarChartOutlined />}</span>
            <Space direction="vertical" size={0}>
              <Text style={{ fontSize: 12 }}>{item.label}</Text>
              <Text type="secondary" style={{ fontSize: 11 }}>{item.group}</Text>
            </Space>
          </Space>
        </List.Item>
      )}
    />
  )
}

function ChartResourceList({
  charts,
  loading,
  selectedKey,
  onSelect,
}: {
  charts: BiChartResource[]
  loading: boolean
  selectedKey?: string
  onSelect: (chartKey: string) => void
}) {
  if (!loading && charts.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无已保存图表" />
  }
  return (
    <List
      size="small"
      loading={loading}
      dataSource={charts}
      renderItem={chart => (
        <List.Item
          style={{
            ...paletteItemStyle,
            borderColor: chart.chartKey === selectedKey ? '#1677ff' : '#e5e7eb',
            background: chart.chartKey === selectedKey ? '#eff6ff' : '#fff',
            cursor: 'pointer',
          }}
          onClick={() => onSelect(chart.chartKey)}
        >
          <Space size={8} align="start">
            <span style={panelIconStyle}><BarChartOutlined /></span>
            <Space direction="vertical" size={2}>
              <Space size={6}>
                <Text style={{ fontSize: 12 }} ellipsis>{chart.name}</Text>
                <Tag color={chart.status === 'PUBLISHED' ? 'green' : 'gold'}>{chart.status}</Tag>
              </Space>
              <Text type="secondary" style={{ fontSize: 11 }}>
                {chartLabel(chart.chartType)} · {chart.datasetKey}
              </Text>
            </Space>
          </Space>
        </List.Item>
      )}
    />
  )
}

function DashboardWidgetCard({
  widget,
  displayColumns,
  queryResult,
  loading,
  selected,
  onSelect,
  onDuplicate,
  onRemove,
  onMove,
  onMoveByPixels,
  onResizeByPixels,
  onCompile,
}: {
  widget: BiDashboardWidgetPreset
  displayColumns: number
  queryResult?: BiQueryResult
  loading: boolean
  selected: boolean
  onSelect: (additive?: boolean) => void
  onDuplicate: () => void
  onRemove: () => void
  onMove: (direction: DashboardWidgetMoveDirection) => void
  onMoveByPixels: (deltaX: number, deltaY: number) => void
  onResizeByPixels: (deltaX: number, deltaY: number) => void
  onCompile: () => void
}) {
  const startMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    event.preventDefault()
    event.stopPropagation()
    onSelect(false)
    const startX = event.clientX
    const startY = event.clientY
    let latestX = startX
    let latestY = startY
    const handleMove = (moveEvent: PointerEvent) => {
      latestX = moveEvent.clientX
      latestY = moveEvent.clientY
    }
    const handleUp = () => {
      window.removeEventListener('pointermove', handleMove)
      window.removeEventListener('pointerup', handleUp)
      const deltaX = latestX - startX
      const deltaY = latestY - startY
      if (Math.abs(deltaX) >= 8 || Math.abs(deltaY) >= 8) {
        onMoveByPixels(deltaX, deltaY)
      }
    }
    window.addEventListener('pointermove', handleMove)
    window.addEventListener('pointerup', handleUp)
  }

  const startResize = (event: ReactPointerEvent<HTMLDivElement>) => {
    event.preventDefault()
    event.stopPropagation()
    onSelect(false)
    const startX = event.clientX
    const startY = event.clientY
    let latestX = startX
    let latestY = startY
    const handleMove = (moveEvent: PointerEvent) => {
      latestX = moveEvent.clientX
      latestY = moveEvent.clientY
    }
    const handleUp = () => {
      window.removeEventListener('pointermove', handleMove)
      window.removeEventListener('pointerup', handleUp)
      const deltaX = latestX - startX
      const deltaY = latestY - startY
      if (Math.abs(deltaX) >= 8 || Math.abs(deltaY) >= 8) {
        onResizeByPixels(deltaX, deltaY)
      }
    }
    window.addEventListener('pointermove', handleMove)
    window.addEventListener('pointerup', handleUp)
  }

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={event => onSelect(event.metaKey || event.ctrlKey || event.shiftKey)}
      onKeyDown={event => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          onSelect(false)
        }
      }}
      style={{
        ...widgetCardStyle,
        ...dashboardWidgetGridPlacementForColumns(widget, displayColumns),
        borderColor: selected ? '#2563eb' : '#d7dde8',
        boxShadow: selected ? '0 0 0 2px rgba(37,99,235,.12)' : '0 6px 18px rgba(15,23,42,.04)',
      }}
    >
      <div style={widgetCardHeaderStyle}>
        <div title="拖拽移动" onPointerDown={startMove} style={widgetDragHandleStyle}>
          <Space size={6}>
            <Text strong>{widget.title}</Text>
            <Tag color={selected ? 'blue' : 'default'}>{chartLabel(widget.chartType)}</Tag>
          </Space>
        </div>
        <Space size={6}>
          {queryResult ? (
            <>
              <Tag color={queryResult.cached ? 'gold' : 'green'}>{queryResult.cached ? '缓存' : '实时'}</Tag>
              <Tag color="blue">{queryResult.rowCount} 行 · {queryResult.durationMs} ms</Tag>
            </>
          ) : loading ? <Tag color="processing">查询中</Tag> : null}
          <WidgetActionButton title="查看 SQL" icon={<EyeOutlined />} onClick={onCompile} />
          <WidgetActionButton title="左移" icon={<ArrowLeftOutlined />} onClick={() => onMove('left')} />
          <WidgetActionButton title="上移" icon={<ArrowUpOutlined />} onClick={() => onMove('up')} />
          <WidgetActionButton title="下移" icon={<ArrowDownOutlined />} onClick={() => onMove('down')} />
          <WidgetActionButton title="右移" icon={<ArrowRightOutlined />} onClick={() => onMove('right')} />
          <WidgetActionButton title="复制" icon={<CopyOutlined />} onClick={onDuplicate} />
          <WidgetActionButton title="删除" icon={<DeleteOutlined />} danger onClick={onRemove} />
        </Space>
      </div>
      <WidgetPreview widget={widget} result={queryResult} />
      <div
        title="拖拽缩放"
        onPointerDown={startResize}
        style={resizeHandleStyle}
      />
    </div>
  )
}

function WidgetActionButton({
  title,
  icon,
  danger = false,
  onClick,
}: {
  title: string
  icon: JSX.Element
  danger?: boolean
  onClick: () => void
}) {
  return (
    <Tooltip title={title}>
      <Button
        type="text"
        size="small"
        danger={danger}
        icon={icon}
        onClick={event => {
          event.stopPropagation()
          onClick()
        }}
      />
    </Tooltip>
  )
}

function WidgetPreview({ widget, result }: { widget: BiDashboardWidgetPreset; result?: BiQueryResult }) {
  const rows = result?.rows ?? []
  if (widget.chartType === 'KPI_CARD') {
    const metric = widget.metrics[0]
    const value = rows.length ? toNumber(rows[0][metric]) : (widget.metrics.includes('success_rate') ? 0.968 : 128430)
    const percent = metric === 'success_rate' ? Math.round((value <= 1 ? value * 100 : value)) : Math.min(100, Math.round(value / 1800))
    return (
      <Space direction="vertical" size={4} style={{ width: '100%', alignItems: 'flex-start' }}>
        <Text style={{ fontSize: 28, fontWeight: 700 }}>{formatMetricValue(metric, value)}</Text>
        <Progress percent={percent} size="small" showInfo={false} />
        <Text type="secondary" style={{ fontSize: 12 }}>{widget.metrics.join(' / ')}</Text>
      </Space>
    )
  }
  if (widget.chartType === 'LINE') {
    if (rows.length) {
      const primaryMetric = widget.metrics[0]
      const secondaryMetric = widget.metrics[1]
      return (
        <svg width="100%" height="116" viewBox="0 0 360 116" role="img" aria-label={widget.title}>
          <polyline fill="none" stroke="#2563eb" strokeWidth="3" points={linePoints(rows.map(row => toNumber(row[primaryMetric])), 360, 116)} />
          {secondaryMetric && (
            <polyline fill="none" stroke="#ef4444" strokeWidth="2" points={linePoints(rows.map(row => toNumber(row[secondaryMetric])), 360, 116)} />
          )}
        </svg>
      )
    }
    return (
      <svg width="100%" height="116" viewBox="0 0 360 116" role="img" aria-label={widget.title}>
        <polyline fill="none" stroke="#2563eb" strokeWidth="3" points="10,88 62,72 114,76 166,42 218,51 270,26 350,34" />
        <polyline fill="none" stroke="#ef4444" strokeWidth="2" points="10,94 62,88 114,90 166,78 218,82 270,64 350,70" />
        {[10, 62, 114, 166, 218, 270, 350].map(x => <circle key={x} cx={x} cy={x === 270 ? 26 : 42} r="3" fill="#2563eb" />)}
      </svg>
    )
  }
  if (widget.chartType === 'BAR') {
    if (rows.length) {
      const metric = widget.metrics[0]
      const values = rows.slice(0, 5).map(row => toNumber(row[metric]))
      const max = Math.max(1, ...values)
      return (
        <div style={{ display: 'flex', alignItems: 'end', gap: 8, height: 120 }}>
          {values.map((value, index) => (
            <div key={`${metric}-${index}`} style={{ flex: 1, minWidth: 0 }}>
              <div style={{ height: Math.max(10, (value / max) * 112), background: index === 0 ? '#2563eb' : '#93c5fd', borderRadius: 4 }} />
            </div>
          ))}
        </div>
      )
    }
    return (
      <Row gutter={8} align="bottom" style={{ height: 120 }}>
        {[86, 64, 48, 36, 28].map((height, index) => (
          <Col key={height} span={4}>
            <div style={{ height, background: index === 0 ? '#2563eb' : '#93c5fd', borderRadius: 4 }} />
          </Col>
        ))}
      </Row>
    )
  }
  if (rows.length) {
    const columns = [...widget.dimensions, ...widget.metrics].slice(0, 3)
    return (
      <div style={{ width: '100%', overflow: 'hidden' }}>
        {rows.slice(0, 4).map((row, index) => (
          <div key={index} style={{ ...tablePreviewRowStyle, gridTemplateColumns: `repeat(${columns.length}, minmax(0, 1fr))` }}>
            {columns.map(column => (
              <Text key={column} ellipsis style={{ fontSize: 12 }}>{formatCellValue(column, row[column])}</Text>
            ))}
          </div>
        ))}
      </div>
    )
  }
  return (
    <div style={{ width: '100%', overflow: 'hidden' }}>
      {['欢迎旅程', '复购召回', '积分激活'].map((name, index) => (
        <div key={name} style={tablePreviewRowStyle}>
          <Text style={{ fontSize: 12 }}>{name}</Text>
          <Text style={{ fontSize: 12 }}>{[18240, 9632, 5840][index].toLocaleString()}</Text>
          <Tag color={index === 0 ? 'green' : 'blue'}>{index === 0 ? '96.8%' : '92.4%'}</Tag>
        </div>
      ))}
    </div>
  )
}

function linePoints(values: number[], width: number, height: number): string {
  if (values.length === 0) return ''
  const max = Math.max(...values)
  const min = Math.min(...values)
  const span = Math.max(1, max - min)
  const step = values.length === 1 ? 0 : (width - 20) / (values.length - 1)
  return values.map((value, index) => {
    const x = 10 + step * index
    const y = height - 12 - ((value - min) / span) * (height - 28)
    return `${Math.round(x)},${Math.round(y)}`
  }).join(' ')
}

function toNumber(value: unknown): number {
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : 0
  }
  return 0
}

function formatMetricValue(metric: string, value: number): string {
  if (metric === 'success_rate') {
    const percent = value <= 1 ? value * 100 : value
    return `${percent.toFixed(1)}%`
  }
  return Math.round(value).toLocaleString()
}

function formatCellValue(column: string, value: unknown): string {
  if (typeof value === 'number') {
    return column === 'success_rate' ? formatMetricValue(column, value) : Math.round(value).toLocaleString()
  }
  if (value == null) return '-'
  return String(value)
}

function queryStatusColor(status: string): string {
  if (status === 'SUCCESS') return 'green'
  if (status === 'CACHE_HIT') return 'gold'
  return 'red'
}

function runtimeStateSourceColor(source: BiDashboardRuntimeStateRow['source']): string {
  if (source === 'URL') return 'geekblue'
  if (source === 'REMEMBERED') return 'green'
  if (source === 'DEFAULT') return 'default'
  if (source === 'CLEARED') return 'orange'
  return 'default'
}

function resourceTypeColor(resourceType: string): string {
  if (resourceType === 'DASHBOARD') return 'geekblue'
  if (resourceType === 'DATASET') return 'blue'
  if (resourceType === 'CHART') return 'purple'
  if (resourceType === 'PORTAL') return 'cyan'
  return 'default'
}

function compareResourceLocations(a: BiResourceLocationView, b: BiResourceLocationView): number {
  const folderCompare = resourceFolderLabel(a).localeCompare(resourceFolderLabel(b))
  if (folderCompare !== 0) return folderCompare
  if (a.sortOrder !== b.sortOrder) return a.sortOrder - b.sortOrder
  return resourceLocationIndexKey(a.resourceType, a.resourceKey)
    .localeCompare(resourceLocationIndexKey(b.resourceType, b.resourceKey))
}

function compareResourceOwnerships(a: BiResourceOwnershipView, b: BiResourceOwnershipView): number {
  return resourceLocationIndexKey(a.resourceType, a.resourceKey)
    .localeCompare(resourceLocationIndexKey(b.resourceType, b.resourceKey))
}

function compareResourceFavorites(a: BiResourceFavoriteView, b: BiResourceFavoriteView): number {
  return resourceLocationIndexKey(a.resourceType, a.resourceKey)
    .localeCompare(resourceLocationIndexKey(b.resourceType, b.resourceKey))
}

function resourceLocationTableRowKey(row: BiResourceLocationView, index: number): string {
  return [
    resourceLocationIndexKey(row.resourceType, row.resourceKey),
    row.id ?? 'no-id',
    row.workspaceId ?? 'no-workspace',
    row.folderKey ?? 'root',
    row.sortOrder ?? 'no-sort',
    index,
  ].join(':')
}

function resourceOwnershipTableRowKey(row: BiResourceOwnershipView, index: number): string {
  return [
    resourceLocationIndexKey(row.resourceType, row.resourceKey),
    row.id ?? 'no-id',
    row.workspaceId ?? 'no-workspace',
    row.ownerUser ?? 'no-owner',
    index,
  ].join(':')
}

function resourceFavoriteTableRowKey(row: BiResourceFavoriteView, index: number): string {
  return [
    resourceLocationIndexKey(row.resourceType, row.resourceKey),
    row.id ?? 'no-id',
    row.workspaceId ?? 'no-workspace',
    row.username ?? 'no-user',
    row.favorite ? 'favorite' : 'not-favorite',
    index,
  ].join(':')
}

function compareResourceComments(a: BiResourceCommentView, b: BiResourceCommentView): number {
  return String(a.createdAt ?? '').localeCompare(String(b.createdAt ?? '')) || Number(a.id ?? 0) - Number(b.id ?? 0)
}

function comparePublishApprovals(a: BiPublishApprovalView, b: BiPublishApprovalView): number {
  return String(b.requestedAt ?? '').localeCompare(String(a.requestedAt ?? '')) || Number(b.id ?? 0) - Number(a.id ?? 0)
}

function publishApprovalStatusColor(status: string | null | undefined): string {
  if (status?.toUpperCase() === 'APPROVED') return 'green'
  if (status?.toUpperCase() === 'REJECTED') return 'red'
  if (status?.toUpperCase() === 'PENDING') return 'gold'
  return 'default'
}

function exportStatusColor(status: string): string {
  if (status === 'COMPLETED') return 'green'
  if (status === 'FAILED') return 'red'
  if (status === 'RUNNING') return 'processing'
  if (status === 'PENDING_APPROVAL') return 'gold'
  if (status === 'REJECTED') return 'red'
  if (status === 'EXPIRED') return 'default'
  return 'gold'
}

function exportProgressPercent(row: BiExportJobView): number {
  const value = row.progressPercent ?? (row.status === 'COMPLETED' ? 100 : row.status === 'RUNNING' ? 50 : 0)
  if (!Number.isFinite(value)) return 0
  return Math.max(0, Math.min(100, Math.round(value)))
}

function clampDatasourceModelingGraphCoordinate(value: number, max: number): number {
  if (!Number.isFinite(value)) return 0
  return Math.min(Math.max(0, max), Math.max(0, Math.round(value)))
}

function exportProgressStatus(row: BiExportJobView): 'exception' | 'active' | 'success' | 'normal' {
  if (row.status === 'COMPLETED') return 'success'
  if (row.status === 'FAILED' || row.status === 'REJECTED' || row.status === 'EXPIRED') return 'exception'
  if (row.status === 'RUNNING') return 'active'
  return 'normal'
}

function isRetryableExportJob(row: BiExportJobView): boolean {
  if (row.status !== 'FAILED' || row.retryExhaustedAt) return false
  const retryCount = row.retryCount ?? 0
  const maxRetryCount = row.maxRetryCount ?? 0
  if (maxRetryCount <= 0 || retryCount >= maxRetryCount) return false
  if (!row.nextRetryAt) return true
  const nextRetryTime = Date.parse(row.nextRetryAt)
  return Number.isFinite(nextRetryTime) && nextRetryTime <= Date.now()
}

function deliveryStatusColor(status: string): string {
  if (status === 'DELIVERED' || status === 'TRIGGERED') return 'green'
  if (status === 'FAILED') return 'red'
  if (status === 'PENDING_ADAPTER') return 'gold'
  if (status === 'SKIPPED') return 'default'
  return 'blue'
}

function permissionSubjectLabel(subjectType: string, subjectId: string): string {
  if (subjectType === 'ROLE') return `角色:${subjectId}`
  if (subjectType === 'USER') return `用户:${subjectId}`
  if (subjectType === 'ALL') return '全部成员'
  return `${subjectType}:${subjectId}`
}

function scheduleLabel(schedule: Record<string, unknown>): string {
  const frequency = String(schedule.frequency ?? '-')
  const time = schedule.time ? ` ${schedule.time}` : ''
  return `${frequency}${time}`
}

function alertConditionLabel(condition: Record<string, unknown>): string {
  const operator = String(condition.operator ?? '-')
  const mode = String(condition.mode ?? condition.type ?? '')
  if (operator.startsWith('ANOMALY') || mode === 'ANOMALY') {
    const direction = operator.includes('DROP') ? '下降' : operator.includes('RISE') || operator.includes('SPIKE') ? '上升' : '双向'
    const samples = condition.minSamples ?? 3
    const sensitivity = condition.sensitivity ?? 2
    return `${direction}异常 ${sensitivity}σ/${samples}样本`
  }
  const threshold = condition.threshold ?? '-'
  return `${operator} ${threshold}`
}

function ChannelTags({ receivers }: { receivers: Record<string, unknown> }) {
  const channels = Array.isArray(receivers.channels) ? receivers.channels.map(String) : []
  if (channels.length === 0) return <Text type="secondary">-</Text>
  return (
    <Space size={[4, 4]} wrap>
      {channels.slice(0, 3).map(channel => <Tag key={channel}>{channel}</Tag>)}
      {channels.length > 3 && <Tag>+{channels.length - 3}</Tag>}
    </Space>
  )
}

function DeliveryStatusCell({ row }: { row: BiDeliveryLogView }) {
  const retryable = row.status === 'FAILED' || row.status === 'PENDING_ADAPTER'
  const retryCount = row.retryCount ?? 0
  const maxRetryCount = row.maxRetryCount ?? 0
  const exhausted = Boolean(row.retryExhaustedAt)
  return (
    <Space direction="vertical" size={0}>
      <Tag color={deliveryStatusColor(row.status)}>{row.status}</Tag>
      {retryable && (
        <Text
          type={exhausted ? 'danger' : 'secondary'}
          ellipsis
          style={{ display: 'block', maxWidth: 128, fontSize: 11 }}
        >
          {exhausted
            ? `重试已耗尽 ${retryCount}/${maxRetryCount}`
            : `重试 ${retryCount}/${maxRetryCount} · ${formatRetryTime(row.nextRetryAt)}`}
        </Text>
      )}
    </Space>
  )
}

function formatRetryTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '待重试'
}

interface DeliveryPayloadAttachment {
  id?: number
  attachmentType?: string
  fileName?: string
  fileUrl?: string
  status?: string
  retentionDays?: number
  expiresAt?: string
  downloadCount?: number
  lastDownloadedAt?: string
}

function deliveryAttachmentsFromPayload(payload: Record<string, unknown>): DeliveryPayloadAttachment[] {
  const extra = payload.extra
  if (!extra || typeof extra !== 'object' || Array.isArray(extra)) return []
  const attachments = (extra as Record<string, unknown>).attachments
  if (!Array.isArray(attachments)) return []
  return attachments
    .filter((item): item is Record<string, unknown> => Boolean(item) && typeof item === 'object' && !Array.isArray(item))
    .map(item => ({
      id: typeof item.id === 'number' ? item.id : Number(item.id) || undefined,
      attachmentType: item.attachmentType == null ? undefined : String(item.attachmentType),
      fileName: item.fileName == null ? undefined : String(item.fileName),
      fileUrl: item.fileUrl == null ? undefined : String(item.fileUrl),
      status: item.status == null ? undefined : String(item.status),
      retentionDays: optionalNumber(item.retentionDays),
      expiresAt: item.expiresAt == null ? undefined : String(item.expiresAt),
      downloadCount: optionalNumber(item.downloadCount),
      lastDownloadedAt: item.lastDownloadedAt == null ? undefined : String(item.lastDownloadedAt),
    }))
}

function AttachmentLinks({ attachments }: { attachments: DeliveryPayloadAttachment[] }) {
  if (!attachments.length) return <Text type="secondary">-</Text>
  return (
    <Space size={[4, 4]} wrap>
      {attachments.slice(0, 2).map((attachment, index) => {
        const expired = isExpiredAt(attachment.expiresAt) || attachment.status === 'EXPIRED'
        return (
          <Tooltip key={attachment.id ?? `${attachment.attachmentType}-${index}`} title={attachmentTooltip(attachment)}>
            <Button
              size="small"
              icon={<DownloadOutlined />}
              disabled={!attachment.fileUrl || attachment.status === 'FAILED' || expired}
              onClick={() => attachment.fileUrl && window.open(attachment.fileUrl, '_blank')}
            >
              {attachment.attachmentType ?? 'FILE'}
            </Button>
          </Tooltip>
        )
      })}
      {attachments.length > 2 && <Tag>+{attachments.length - 2}</Tag>}
    </Space>
  )
}

interface SelfServiceExtractionBuilderProps {
  dimensionFields: string[]
  metricFields: string[]
  value: SelfServiceExtractionState
  onDropField: (role: 'DIMENSION' | 'METRIC', fieldKey: string) => void
  onRemoveField: (role: 'DIMENSION' | 'METRIC', fieldKey: string) => void
}

/** 图表字段拖放构建器，支持从数据集字段快速填充维度槽和指标槽。 */
function ChartFieldDropBuilder({
  dimensionFields,
  metricFields,
  selectedDimensions,
  selectedMetrics,
  disabled,
  onDropField,
}: {
  dimensionFields: string[]
  metricFields: string[]
  selectedDimensions: string[]
  selectedMetrics: string[]
  disabled: boolean
  onDropField: (role: 'DIMENSION' | 'METRIC', fieldKey: string) => void
}) {
  const uniqueDimensionFields = Array.from(new Set(dimensionFields))
  const uniqueMetricFields = Array.from(new Set(metricFields))
  const addField = (role: 'DIMENSION' | 'METRIC', fieldKey: string) => {
    if (!disabled) onDropField(role, fieldKey)
  }
  const startDrag = (event: ReactDragEvent<HTMLElement>, role: 'DIMENSION' | 'METRIC', fieldKey: string) => {
    // 写入自定义 MIME，避免和页面其它拖放能力互相误识别。
    event.dataTransfer.effectAllowed = 'copy'
    event.dataTransfer.setData('application/x-bi-chart-field-role', role)
    event.dataTransfer.setData('application/x-bi-chart-field', fieldKey)
    event.dataTransfer.setData('text/plain', fieldKey)
  }
  const dropField = (event: ReactDragEvent<HTMLDivElement>, role: 'DIMENSION' | 'METRIC') => {
    event.preventDefault()
    if (disabled) return
    // text/plain 作为兜底，兼容浏览器或测试环境丢失自定义 MIME 的情况。
    const fieldKey = event.dataTransfer.getData('application/x-bi-chart-field') || event.dataTransfer.getData('text/plain')
    if (fieldKey) onDropField(role, fieldKey)
  }
  return (
    <Row gutter={8}>
      <Col xs={24} md={8}>
        <Space direction="vertical" size={6} style={{ width: '100%' }}>
          <Text type="secondary" style={{ fontSize: 12 }}>拖放字段</Text>
          <Space size={[4, 4]} wrap>
            {uniqueDimensionFields.slice(0, 8).map(field => (
              <Tag
                key={`chart-dimension-source-${field}`}
                color="blue"
                role="button"
                tabIndex={disabled ? -1 : 0}
                aria-label={`添加图表维度字段 ${field}`}
                draggable={!disabled}
                onDragStart={event => startDrag(event, 'DIMENSION', field)}
                onClick={() => addField('DIMENSION', field)}
                onKeyDown={event => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault()
                    addField('DIMENSION', field)
                  }
                }}
                style={{ cursor: disabled ? 'not-allowed' : 'grab' }}
              >
                {field}
              </Tag>
            ))}
            {uniqueMetricFields.slice(0, 8).map(field => (
              <Tag
                key={`chart-metric-source-${field}`}
                color="purple"
                role="button"
                tabIndex={disabled ? -1 : 0}
                aria-label={`添加图表指标字段 ${field}`}
                draggable={!disabled}
                onDragStart={event => startDrag(event, 'METRIC', field)}
                onClick={() => addField('METRIC', field)}
                onKeyDown={event => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault()
                    addField('METRIC', field)
                  }
                }}
                style={{ cursor: disabled ? 'not-allowed' : 'grab' }}
              >
                {field}
              </Tag>
            ))}
          </Space>
        </Space>
      </Col>
      <Col xs={24} md={8}>
        <ChartFieldDropZone title="维度槽" role="DIMENSION" fields={selectedDimensions} onDrop={dropField} />
      </Col>
      <Col xs={24} md={8}>
        <ChartFieldDropZone title="指标槽" role="METRIC" fields={selectedMetrics} onDrop={dropField} />
      </Col>
    </Row>
  )
}

/** 图表字段槽位，负责接收拖入字段并展示当前已选字段。 */
function ChartFieldDropZone({
  title,
  role,
  fields,
  onDrop,
}: {
  title: string
  role: 'DIMENSION' | 'METRIC'
  fields: string[]
  onDrop: (event: ReactDragEvent<HTMLDivElement>, role: 'DIMENSION' | 'METRIC') => void
}) {
  return (
    <div
      style={extractionDropZoneStyle}
      onDragOver={event => event.preventDefault()}
      onDrop={event => onDrop(event, role)}
    >
      <Space direction="vertical" size={6} style={{ width: '100%' }}>
        <Text type="secondary" style={{ fontSize: 12 }}>{title}</Text>
        <Space size={[4, 4]} wrap>
          {fields.length === 0 ? <Text type="secondary" style={{ fontSize: 12 }}>未选择</Text> : fields.map(field => (
            <Tag key={`${role}-${field}`} color={role === 'DIMENSION' ? 'blue' : 'purple'}>{field}</Tag>
          ))}
        </Space>
      </Space>
    </div>
  )
}

function SelfServiceExtractionBuilder({
  dimensionFields,
  metricFields,
  value,
  onDropField,
  onRemoveField,
}: SelfServiceExtractionBuilderProps) {
  const startDrag = (event: ReactDragEvent<HTMLElement>, fieldKey: string) => {
    event.dataTransfer.effectAllowed = 'copy'
    event.dataTransfer.setData('application/x-bi-field', fieldKey)
    event.dataTransfer.setData('text/plain', fieldKey)
  }
  const dropField = (event: ReactDragEvent<HTMLDivElement>, role: 'DIMENSION' | 'METRIC') => {
    event.preventDefault()
    const fieldKey = event.dataTransfer.getData('application/x-bi-field') || event.dataTransfer.getData('text/plain')
    if (fieldKey) {
      onDropField(role, fieldKey)
    }
  }
  return (
    <Row gutter={10}>
      <Col xs={24} md={8}>
        <Space direction="vertical" size={6} style={{ width: '100%' }}>
          <Text type="secondary" style={{ fontSize: 12 }}>可选字段</Text>
          <Space size={[6, 6]} wrap>
            {dimensionFields.slice(0, 8).map((field, index) => (
              <Tag
                key={`dimension-source-${field}-${index}`}
                color="blue"
                draggable
                onDragStart={event => startDrag(event, field)}
                onClick={() => onDropField('DIMENSION', field)}
                style={{ cursor: 'grab' }}
              >
                {field}
              </Tag>
            ))}
            {metricFields.slice(0, 8).map((field, index) => (
              <Tag
                key={`metric-source-${field}-${index}`}
                color="purple"
                draggable
                onDragStart={event => startDrag(event, field)}
                onClick={() => onDropField('METRIC', field)}
                style={{ cursor: 'grab' }}
              >
                {field}
              </Tag>
            ))}
          </Space>
        </Space>
      </Col>
      <Col xs={24} md={8}>
        <ExtractionDropZone
          title="维度"
          role="DIMENSION"
          fields={value.dimensions}
          onDrop={dropField}
          onRemove={onRemoveField}
        />
      </Col>
      <Col xs={24} md={8}>
        <ExtractionDropZone
          title="指标"
          role="METRIC"
          fields={value.metrics}
          onDrop={dropField}
          onRemove={onRemoveField}
        />
      </Col>
    </Row>
  )
}

function ExtractionDropZone({
  title,
  role,
  fields,
  onDrop,
  onRemove,
}: {
  title: string
  role: 'DIMENSION' | 'METRIC'
  fields: string[]
  onDrop: (event: ReactDragEvent<HTMLDivElement>, role: 'DIMENSION' | 'METRIC') => void
  onRemove: (role: 'DIMENSION' | 'METRIC', fieldKey: string) => void
}) {
  return (
    <div
      style={extractionDropZoneStyle}
      onDragOver={event => event.preventDefault()}
      onDrop={event => onDrop(event, role)}
    >
      <Space direction="vertical" size={6} style={{ width: '100%' }}>
        <Text type="secondary" style={{ fontSize: 12 }}>{title}</Text>
        <Space size={[6, 6]} wrap>
          {fields.length === 0 ? (
            <Text type="secondary" style={{ fontSize: 12 }}>未选择</Text>
          ) : fields.map((field, index) => (
            <Tag
              key={`${role}-${field}-${index}`}
              color={role === 'DIMENSION' ? 'blue' : 'purple'}
              closable
              onClose={event => {
                event.preventDefault()
                onRemove(role, field)
              }}
            >
              {field}
            </Tag>
          ))}
        </Space>
      </Space>
    </div>
  )
}

function optionalNumber(value: unknown) {
  if (value == null || value === '') return undefined
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : undefined
}

function isExpiredAt(value?: string | null) {
  if (!value) return false
  const time = Date.parse(value)
  return Number.isFinite(time) && time <= Date.now()
}

function exportJobTooltip(row: BiExportJobView): ReactNode {
  const expired = isExpiredAt(row.expiresAt) || row.status === 'EXPIRED'
  return (
    <Space direction="vertical" size={0}>
      <Text style={{ color: 'inherit', fontSize: 12 }}>{row.resourceKey ?? row.resourceId ?? '导出任务'}</Text>
      <Text style={{ color: 'inherit', fontSize: 11 }}>
        {row.status ?? '-'} · {expired ? '已过期' : `过期 ${formatAttachmentTime(row.expiresAt)}`}
      </Text>
      <Text style={{ color: 'inherit', fontSize: 11 }}>
        进度 {exportProgressPercent(row)}% · 下载 {row.downloadCount ?? 0} · 留存 {row.retentionDays ?? '-'} 天
      </Text>
      {row.status === 'FAILED' && (
        <Text style={{ color: 'inherit', fontSize: 11 }}>
          重试 {row.retryCount ?? 0}/{row.maxRetryCount ?? 0} · {row.retryExhaustedAt ? '已耗尽' : formatRetryTime(row.nextRetryAt)}
        </Text>
      )}
      {row.lastDownloadedAt && (
        <Text style={{ color: 'inherit', fontSize: 11 }}>最近下载 {formatAttachmentTime(row.lastDownloadedAt)}</Text>
      )}
    </Space>
  )
}

function attachmentTooltip(attachment: DeliveryPayloadAttachment): ReactNode {
  const expired = isExpiredAt(attachment.expiresAt) || attachment.status === 'EXPIRED'
  return (
    <Space direction="vertical" size={0}>
      <Text style={{ color: 'inherit', fontSize: 12 }}>{attachment.fileName ?? attachment.attachmentType ?? '附件'}</Text>
      <Text style={{ color: 'inherit', fontSize: 11 }}>
        {attachment.status ?? '-'} · {expired ? '已过期' : `过期 ${formatAttachmentTime(attachment.expiresAt)}`}
      </Text>
      <Text style={{ color: 'inherit', fontSize: 11 }}>
        下载 {attachment.downloadCount ?? 0} · 留存 {attachment.retentionDays ?? '-'} 天
      </Text>
    </Space>
  )
}

function formatAttachmentTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function runtimeRouteLabel(route: BiRuntimeRouteLike): string {
  if (route.resourceId != null) return `#${route.resourceId}`
  return route.resourceKey ?? '-'
}

function runtimeRecordString(record: Record<string, unknown> | null | undefined, key: string, fallback = '-'): string {
  const value = record?.[key]
  if (value == null || value === '') return fallback
  return String(value)
}

function runtimeRecordNumber(record: Record<string, unknown> | null | undefined, key: string, fallback: number): number {
  const value = record?.[key]
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) return parsed
  }
  return fallback
}

function runtimeSheetCells(sheet: Record<string, unknown>): Record<string, unknown> {
  const cells = sheet.cells
  return isRuntimeRecord(cells) ? cells : {}
}

function runtimeSheetCellStyles(sheet: Record<string, unknown>): Record<string, unknown> {
  const cellStyles = sheet.cellStyles
  return isRuntimeRecord(cellStyles) ? cellStyles : {}
}

function runtimeSheetMobileLayout(sheet: Record<string, unknown>): { enabled: boolean; columns: number } {
  const mobileLayout = sheet.mobileLayout
  if (!isRuntimeRecord(mobileLayout)) return { enabled: false, columns: 8 }
  const parsedColumns = Number(mobileLayout.columns)
  const columns = Number.isFinite(parsedColumns) && parsedColumns > 0 ? parsedColumns : 4
  return {
    enabled: mobileLayout.enabled === true,
    columns: Math.max(1, Math.min(8, Math.round(columns))),
  }
}

function runtimeSpreadsheetCellStyle(style: unknown): CSSProperties {
  if (!isRuntimeRecord(style)) return {}
  return {
    ...(style.backgroundColor ? { backgroundColor: String(style.backgroundColor) } : {}),
    ...(style.textColor ? { color: String(style.textColor) } : {}),
    ...(style.bold ? { fontWeight: 600 } : {}),
  }
}

function runtimeCellText(value: unknown): string {
  if (value == null) return ''
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') return String(value)
  return JSON.stringify(value)
}

function isRuntimeRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function resourceStatusColor(status: string): string {
  if (status === 'PUBLISHED') return 'green'
  if (status === 'DRAFT') return 'gold'
  if (status === 'ARCHIVED') return 'default'
  return 'blue'
}

function SelfServicePreviewTable({ result, loading }: { result: BiQueryResult | null; loading: boolean }) {
  const columns: ColumnsType<Record<string, unknown>> = (result?.columns ?? []).map((column: BiQueryColumn) => ({
    title: column.key,
    dataIndex: column.key,
    render: (value: unknown) => <Text ellipsis style={{ maxWidth: 160, display: 'block', fontSize: 12 }}>{formatCellValue(column.key, value)}</Text>,
  }))
  const dataSource = (result?.rows ?? []).map((row, index) => ({ ...row, __rowKey: index }))
  return (
    <Table
      rowKey="__rowKey"
      size="small"
      loading={loading}
      pagination={false}
      columns={columns}
      dataSource={dataSource}
      locale={{ emptyText: '选择图表字段后预览取数结果' }}
      scroll={{ x: 720 }}
    />
  )
}

function SqlDatasetPreviewTable({ result, loading }: { result: BiSqlDatasetPreviewResult | null; loading: boolean }) {
  const columns: ColumnsType<Record<string, unknown>> = (result?.columns ?? []).map((column: BiQueryColumn) => ({
    title: column.key,
    dataIndex: column.key,
    render: (value: unknown) => <Text ellipsis style={{ maxWidth: 160, display: 'block', fontSize: 12 }}>{formatCellValue(column.key, value)}</Text>,
  }))
  const dataSource = (result?.rows ?? []).map((row, index) => ({ ...row, __rowKey: index }))
  return (
    <Table
      rowKey="__rowKey"
      size="small"
      loading={loading}
      pagination={false}
      columns={columns}
      dataSource={dataSource}
      locale={{ emptyText: 'SQL 样例预览暂无返回行' }}
      scroll={{ x: 720 }}
    />
  )
}

function SqlDatasetReadinessSummary({ rows }: { rows: BiSqlDatasetReadinessRow[] }) {
  const statusColor: Record<BiSqlDatasetReadinessRow['status'], string> = {
    pass: 'green',
    warn: 'gold',
    block: 'volcano',
  }
  return (
    <Space direction="vertical" size={6} style={{ width: '100%' }}>
      <Text strong style={{ fontSize: 12 }}>发布诊断</Text>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {rows.map(row => (
          <div
            key={row.key}
            style={{
              border: '1px solid #e5e7eb',
              borderRadius: 6,
              padding: '6px 8px',
              minWidth: 190,
              flex: '1 1 190px',
              background: '#fff',
            }}
          >
            <Space direction="vertical" size={4} style={{ width: '100%' }}>
              <Space size={6} wrap>
                <Text style={{ fontSize: 12 }}>{row.label}</Text>
                <Tag color={statusColor[row.status]}>{row.statusLabel}</Tag>
              </Space>
              <Text type="secondary" style={{ fontSize: 12 }}>{row.detail}</Text>
            </Space>
          </div>
        ))}
      </div>
    </Space>
  )
}

function DatasourceRelationshipDiagnosticSummary({ rows }: { rows: BiDatasourceRelationshipDiagnosticRow[] }) {
  const statusColor: Record<BiDatasourceRelationshipDiagnosticRow['status'], string> = {
    pass: 'green',
    warn: 'gold',
    block: 'volcano',
  }
  return (
    <Space direction="vertical" size={6} style={{ width: '100%' }}>
      <Text strong style={{ fontSize: 12 }}>关系诊断</Text>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {rows.map(row => (
          <div
            key={row.key}
            style={{
              border: '1px solid #e5e7eb',
              borderRadius: 6,
              padding: '6px 8px',
              minWidth: 190,
              flex: '1 1 190px',
              background: '#fff',
            }}
          >
            <Space direction="vertical" size={4} style={{ width: '100%' }}>
              <Space size={6} wrap>
                <Text style={{ fontSize: 12 }}>{row.label}</Text>
                <Tag color={statusColor[row.status]}>{row.statusLabel}</Tag>
              </Space>
              <Text type="secondary" style={{ fontSize: 12 }}>{row.detail}</Text>
            </Space>
          </div>
        ))}
      </div>
    </Space>
  )
}

function VisualEditorDiagnosticSummary({ rows }: { rows: BiVisualEditorDiagnosticRow[] }) {
  const statusColor: Record<BiVisualEditorDiagnosticRow['status'], string> = {
    pass: 'green',
    warn: 'gold',
    block: 'volcano',
  }
  return (
    <Space direction="vertical" size={6} style={{ width: '100%' }}>
      <Text strong style={{ fontSize: 12 }}>视觉诊断</Text>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {rows.map(row => (
          <div
            key={row.key}
            style={{
              border: '1px solid #e5e7eb',
              borderRadius: 6,
              padding: '6px 8px',
              minWidth: 190,
              flex: '1 1 190px',
              background: '#fff',
            }}
          >
            <Space direction="vertical" size={4} style={{ width: '100%' }}>
              <Space size={6} wrap>
                <Text style={{ fontSize: 12 }}>{row.label}</Text>
                <Tag color={statusColor[row.status]}>{row.statusLabel}</Tag>
              </Space>
              <Text type="secondary" style={{ fontSize: 12 }}>{row.detail}</Text>
            </Space>
          </div>
        ))}
      </div>
    </Space>
  )
}

function ExportHardeningDiagnosticSummary({ rows }: { rows: BiExportHardeningDiagnosticRow[] }) {
  const statusColor: Record<BiExportHardeningDiagnosticRow['status'], string> = {
    pass: 'green',
    warn: 'gold',
    block: 'volcano',
  }
  return (
    <Space direction="vertical" size={6} style={{ width: '100%' }}>
      <Text strong style={{ fontSize: 12 }}>导出硬化诊断</Text>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {rows.map(row => (
          <div
            key={row.key}
            style={{
              border: '1px solid #e5e7eb',
              borderRadius: 6,
              padding: '6px 8px',
              minWidth: 190,
              flex: '1 1 190px',
              background: '#fff',
            }}
          >
            <Space direction="vertical" size={4} style={{ width: '100%' }}>
              <Space size={6} wrap>
                <Text style={{ fontSize: 12 }}>{row.label}</Text>
                <Tag color={statusColor[row.status]}>{row.statusLabel}</Tag>
              </Space>
              <Text type="secondary" style={{ fontSize: 12 }}>{row.detail}</Text>
            </Space>
          </div>
        ))}
      </div>
    </Space>
  )
}

function AlertAnomalyDiagnosticSummary({ rows }: { rows: BiAlertAnomalyDiagnosticRow[] }) {
  const statusColor: Record<BiAlertAnomalyDiagnosticRow['status'], string> = {
    pass: 'green',
    warn: 'gold',
    block: 'volcano',
  }
  return (
    <Space direction="vertical" size={6} style={{ width: '100%' }}>
      <Text strong style={{ fontSize: 12 }}>异常诊断</Text>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {rows.map(row => (
          <div
            key={row.key}
            style={{
              border: '1px solid #e5e7eb',
              borderRadius: 6,
              padding: '6px 8px',
              minWidth: 190,
              flex: '1 1 190px',
              background: '#fff',
            }}
          >
            <Space direction="vertical" size={4} style={{ width: '100%' }}>
              <Space size={6} wrap>
                <Text style={{ fontSize: 12 }}>{row.label}</Text>
                <Tag color={statusColor[row.status]}>{row.statusLabel}</Tag>
              </Space>
              <Text type="secondary" style={{ fontSize: 12 }}>{row.detail}</Text>
            </Space>
          </div>
        ))}
      </div>
    </Space>
  )
}

function SqlDatasetSampleProfileTable({ result }: { result: BiSqlDatasetPreviewResult | null }) {
  const rows = buildSqlDatasetSampleProfileRows(result)
  if (!rows.length) return null
  return (
    <Space direction="vertical" size={6} style={{ width: '100%' }}>
      <Text strong style={{ fontSize: 12 }}>样本剖析</Text>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {rows.map(row => (
          <div
            key={row.key}
            style={{
              border: '1px solid #e5e7eb',
              borderRadius: 6,
              padding: '6px 8px',
              minWidth: 180,
              flex: '1 1 180px',
              background: '#fff',
            }}
          >
            <Space direction="vertical" size={2} style={{ width: '100%' }}>
              <Text style={{ fontSize: 12 }}>{row.field} · {row.role} · {row.dataType}</Text>
              <Space size={[4, 4]} wrap>
                <Tag>填充 {row.filled}</Tag>
                <Tag>唯一 {row.unique}</Tag>
              </Space>
              <Text type="secondary" ellipsis style={{ maxWidth: '100%', fontSize: 12 }}>{row.samples}</Text>
            </Space>
          </div>
        ))}
      </div>
    </Space>
  )
}

function SqlDatasetImpactSummary({ result }: { result: BiSqlDatasetPreviewResult | null }) {
  const rows = buildSqlDatasetImpactRows(result)
  if (!rows.length) return null
  return (
    <Space direction="vertical" size={6} style={{ width: '100%' }}>
      <Text strong style={{ fontSize: 12 }}>影响分析</Text>
      <Descriptions
        size="small"
        column={1}
        bordered
        items={rows.map(row => ({
          key: row.key,
          label: row.label,
          children: <Text style={{ fontSize: 12 }}>{row.value}</Text>,
        }))}
      />
    </Space>
  )
}

function DataConfig({
  widget,
  queryResult,
  compiledQuery,
  executionPlan,
  compiling,
  explaining,
  onCompile,
  onExplain,
}: {
  widget: BiDashboardWidgetPreset
  queryResult?: BiQueryResult
  compiledQuery: BiCompiledQuery | null
  executionPlan: BiQueryExplanation | null
  compiling: boolean
  explaining: boolean
  onCompile: () => void
  onExplain: () => void
}) {
  const executionPlanRows = queryExecutionPlanRows(executionPlan)
  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <ConfigBlock title="图表类型">
        <Tag color="blue">{chartLabel(widget.chartType)}</Tag>
      </ConfigBlock>
      <ConfigBlock title="维度">
        <Space size={[4, 4]} wrap>
          {widget.dimensions.length ? widget.dimensions.map(item => <Tag key={item}>{item}</Tag>) : <Text type="secondary">无</Text>}
        </Space>
      </ConfigBlock>
      <ConfigBlock title="指标">
        <Space size={[4, 4]} wrap>
          {widget.metrics.map(item => <Tag key={item} color="geekblue">{item}</Tag>)}
        </Space>
      </ConfigBlock>
      <ConfigBlock title="取数结果">
        {queryResult ? (
          <Space direction="vertical" size={6} style={{ width: '100%' }}>
            <Space size={[4, 4]} wrap>
              <Tag color={queryResult.cached ? 'gold' : 'green'}>{queryResult.cached ? '缓存' : '实时'}</Tag>
              <Tag color="blue">{queryResult.rowCount} 行</Tag>
              <Tag>{queryResult.durationMs} ms</Tag>
            </Space>
            <Text code ellipsis style={{ maxWidth: '100%', display: 'block', fontSize: 11 }}>hash: {queryResult.sqlHash}</Text>
            <WidgetDataRows result={queryResult} />
          </Space>
        ) : (
          <Text type="secondary">暂无查询结果</Text>
        )}
      </ConfigBlock>
      <ConfigBlock title="SQL">
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          <Space.Compact style={{ width: '100%' }}>
            <Button style={{ width: '50%' }} icon={<EyeOutlined />} loading={compiling} onClick={onCompile}>查看 SQL</Button>
            <Button style={{ width: '50%' }} icon={<FileSearchOutlined />} loading={explaining} onClick={onExplain}>执行计划</Button>
          </Space.Compact>
          {compiledQuery ? (
            <Space direction="vertical" size={6} style={{ width: '100%' }}>
              <Input.TextArea value={compiledQuery.sql} autoSize={{ minRows: 3, maxRows: 8 }} readOnly />
              <Text type="secondary" style={{ fontSize: 12 }}>
                参数 {compiledQuery.parameters.length}
              </Text>
            </Space>
          ) : (
            <Text type="secondary">点击后由后端语义层编译参数化 SQL</Text>
          )}
          {executionPlan ? (
            <Descriptions
              size="small"
              column={1}
              items={executionPlanRows.map(row => ({
                key: row.label,
                label: row.label,
                children: row.label === '执行计划'
                  ? <Input.TextArea value={row.value} autoSize={{ minRows: 2, maxRows: 6 }} readOnly />
                  : row.value,
              }))}
            />
          ) : null}
        </Space>
      </ConfigBlock>
    </Space>
  )
}

function WidgetDataRows({ result }: { result: BiQueryResult }) {
  const columns = result.columns.slice(0, 3).map(column => column.key)
  if (!columns.length || !result.rows.length) {
    return <Text type="secondary" style={{ fontSize: 12 }}>暂无明细预览</Text>
  }
  return (
    <Space direction="vertical" size={4} style={{ width: '100%' }}>
      {result.rows.slice(0, 3).map((row, index) => (
        <div key={index} style={{ ...tablePreviewRowStyle, gridTemplateColumns: `repeat(${columns.length}, minmax(0, 1fr))` }}>
          {columns.map(column => (
            <Text key={column} ellipsis style={{ fontSize: 11 }}>{formatCellValue(column, row[column])}</Text>
          ))}
        </div>
      ))}
    </Space>
  )
}

function StyleConfig({ widget }: { widget: BiDashboardWidgetPreset }) {
  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <ConfigBlock title="主题">
        <Select
          size="small"
          value={widget.stylePreset}
          style={{ width: '100%' }}
          options={[
            { label: '运营浅色', value: widget.stylePreset },
            { label: '大屏深色', value: 'screen-dark' },
            { label: '移动紧凑', value: 'mobile-compact' },
          ]}
        />
      </ConfigBlock>
      <ConfigBlock title="显示">
        <Space style={{ justifyContent: 'space-between', width: '100%' }}>
          <Text>标题</Text>
          <Switch size="small" checked />
        </Space>
        <Space style={{ justifyContent: 'space-between', width: '100%' }}>
          <Text>图例</Text>
          <Switch size="small" checked={widget.chartType !== 'KPI_CARD'} />
        </Space>
      </ConfigBlock>
      <ConfigBlock title="配色">
        <Space>
          {['#2563eb', '#10b981', '#f59e0b', '#ef4444'].map(color => <span key={color} style={{ width: 18, height: 18, borderRadius: 4, background: color }} />)}
        </Space>
      </ConfigBlock>
    </Space>
  )
}

function DashboardRuntimeControlEditor({
  filter,
  runtimeParameters,
  disabled,
  ariaPrefix,
  compact = false,
  optionResult,
  onChange,
}: {
  filter: BiDashboardFilterPreset
  runtimeParameters: BiDashboardRuntimeParameters
  disabled: boolean
  ariaPrefix: string
  compact?: boolean
  optionResult?: BiQueryResult
  onChange: (rawValue: string) => void
}) {
  const controlValue = dashboardRuntimeControlValue(runtimeParameters, filter)
  const [draftValue, setDraftValue] = useState(controlValue)
  const editedAtRef = useRef(0)
  useEffect(() => {
    if (Date.now() - editedAtRef.current < 500) return
    setDraftValue(controlValue)
  }, [controlValue])
  const values = dashboardRuntimeControlParts(draftValue)
  const updateDraftValue = (nextValue: string) => {
    editedAtRef.current = Date.now()
    setDraftValue(nextValue)
    onChange(nextValue)
  }
  if (filter.controlType === 'DATE_RANGE') {
    return (
      <Space.Compact>
        <Input
          size="small"
          type="date"
          aria-label={`${ariaPrefix}开始日期`}
          prefix={<CalendarOutlined />}
          value={values[0] ?? ''}
          disabled={disabled}
          onChange={event => updateDraftValue(dashboardRuntimeDateRangeValue(event.target.value, values[1] ?? ''))}
          style={{ width: compact ? 140 : 150 }}
        />
        <Input
          size="small"
          type="date"
          aria-label={`${ariaPrefix}结束日期`}
          value={values[1] ?? ''}
          disabled={disabled}
          onChange={event => updateDraftValue(dashboardRuntimeDateRangeValue(values[0] ?? '', event.target.value))}
          style={{ width: compact ? 140 : 150 }}
        />
      </Space.Compact>
    )
  }
  const candidateValues = filter.controlType === 'ENUM_MULTI_SELECT'
    ? dashboardRuntimeCandidateValues(filter, optionResult)
    : []
  return (
    <Space direction={compact ? 'horizontal' : 'vertical'} size={4} style={{ width: compact ? undefined : '100%' }}>
      <Input
        size="small"
        aria-label={`${ariaPrefix}${compact ? '' : '运行参数'}`}
        prefix={compact ? <FilterOutlined /> : undefined}
        placeholder={filter.controlType === 'ENUM_MULTI_SELECT' ? '逗号分隔多选值' : filter.label}
        value={draftValue}
        disabled={disabled}
        onChange={event => updateDraftValue(event.target.value)}
        style={{ width: compact ? 150 : '100%' }}
      />
      {candidateValues.length > 0 && (
        <Space size={[4, 4]} wrap>
          {candidateValues.map(value => {
            const selected = values.includes(value)
            return (
              <Button
                key={`${filter.filterKey}-${value}`}
                size="small"
                type={selected ? 'primary' : 'default'}
                aria-label={`选择${filter.label} ${value}`}
                disabled={disabled}
                onClick={() => updateDraftValue(dashboardRuntimeToggleCandidateValue(values, value))}
              >
                {value}
              </Button>
            )
          })}
        </Space>
      )}
    </Space>
  )
}

function dashboardRuntimeControlParts(value: string): string[] {
  return value
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

function dashboardRuntimeDateRangeValue(startDate: string, endDate: string): string {
  return [startDate, endDate].map(value => value.trim()).filter(Boolean).join(',')
}

function dashboardRuntimeToggleCandidateValue(currentValues: string[], candidate: string): string {
  const normalized = candidate.trim()
  if (!normalized) return currentValues.join(',')
  const nextValues = currentValues.includes(normalized)
    ? currentValues.filter(value => value !== normalized)
    : [...currentValues, normalized]
  return nextValues.join(',')
}

function dashboardRuntimeCandidateValues(filter: BiDashboardFilterPreset, result?: BiQueryResult): string[] {
  const optionFieldKey = filter.optionFieldKey ?? filter.fieldKey
  return [...new Set((result?.rows ?? [])
    .map(row => row[optionFieldKey])
    .filter(value => value !== undefined && value !== null && String(value).trim() !== '')
    .map(value => String(value).trim()))]
    .slice(0, 8)
}

function DashboardRuntimeToolbar({
  preset,
  runtimeParameters,
  runtimeRows,
  runtimeUpdatedAt,
  onRuntimeParameterChange,
  onRuntimeParameterClear,
  onRuntimeParametersReset,
}: {
  preset: BiDashboardPresetLike
  runtimeParameters: BiDashboardRuntimeParameters
  runtimeRows: BiDashboardRuntimeStateRow[]
  runtimeUpdatedAt?: string | null
  onRuntimeParameterChange: (filterKey: string, rawValue: string) => void
  onRuntimeParameterClear: (filterKey: string) => void
  onRuntimeParametersReset: () => void
}) {
  return (
    <Space size={8} wrap>
      {preset.filters.map(filter => (
        <Space.Compact key={filter.filterKey}>
          <Tooltip title={dashboardRuntimeFilterLocked(preset, filter) ? '全局参数锁定' : undefined}>
            <DashboardRuntimeControlEditor
              filter={filter}
              runtimeParameters={runtimeParameters}
              disabled={dashboardRuntimeFilterLocked(preset, filter)}
              ariaPrefix={`运行态${filter.label}`}
              compact
              onChange={rawValue => onRuntimeParameterChange(filter.filterKey, rawValue)}
            />
          </Tooltip>
          <Tooltip title={`清除运行态${filter.label}`}>
            <Button
              size="small"
              aria-label={`清除运行态${filter.label}`}
              icon={<CloseCircleOutlined />}
              disabled={dashboardRuntimeFilterLocked(preset, filter)}
              onClick={() => onRuntimeParameterClear(filter.filterKey)}
            />
          </Tooltip>
        </Space.Compact>
      ))}
      <Tag color="blue">{Object.keys(runtimeParameters).length} 参数</Tag>
      {runtimeRows.map(row => (
        <Tooltip key={`runtime-state-${row.key}`} title={row.valueText ? `${row.label} = ${row.valueText}` : row.label}>
          <Tag color={runtimeStateSourceColor(row.source)}>{row.label}：{row.sourceLabel}</Tag>
        </Tooltip>
      ))}
      <Tag color={runtimeUpdatedAt ? 'green' : 'default'}>{runtimeUpdatedAt ? '已保存' : '默认运行态'}</Tag>
      <Tooltip title="重置运行态参数">
        <Button
          size="small"
          aria-label="重置运行态参数"
          icon={<SyncOutlined />}
          onClick={onRuntimeParametersReset}
        />
      </Tooltip>
    </Space>
  )
}

function InteractionConfig({
  preset,
  widget,
  canvasId,
  runtimeParameters,
  runtimeRows,
  runtimeUpdatedAt,
  queryResult,
  onRuntimeParameterChange,
  onRuntimeParameterClear,
  onRuntimeParametersReset,
  embedTicket,
  controlOptionResults,
  loadingControlOptions,
}: {
  preset: BiDashboardPresetLike
  widget: BiDashboardWidgetPreset
  canvasId?: string | null
  runtimeParameters: BiDashboardRuntimeParameters
  runtimeRows: BiDashboardRuntimeStateRow[]
  runtimeUpdatedAt?: string | null
  queryResult?: BiQueryResult
  onRuntimeParameterChange: (filterKey: string, rawValue: string) => void
  onRuntimeParameterClear: (filterKey: string) => void
  onRuntimeParametersReset: () => void
  embedTicket: BiEmbedTicket | null
  controlOptionResults: Record<string, BiQueryResult>
  loadingControlOptions: boolean
}) {
  const interactions = preset.interactions.filter(interaction => interaction.sourceWidgetKey === widget.widgetKey)
  const sampleRow = queryResult?.rows[0]
  const runtimeEntryCount = Object.keys(runtimeParameters).length
  const embedPreviewRows = buildEmbedTicketPreviewRows(preset, canvasId, 'INTERNAL_CANVAS', runtimeParameters)
  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <ConfigBlock title="运行参数">
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          <Space size={6} wrap>
            <Tag color="blue">{runtimeEntryCount} 参数</Tag>
            <Tag color={runtimeUpdatedAt ? 'green' : 'default'}>{runtimeUpdatedAt ? `已保存 ${runtimeUpdatedAt}` : '默认运行态'}</Tag>
            <Button size="small" icon={<SyncOutlined />} onClick={onRuntimeParametersReset}>重置运行态</Button>
          </Space>
          <Space size={[4, 4]} wrap>
            {runtimeRows.map(row => (
              <Tooltip key={row.key} title={row.valueText ? `${row.label} = ${row.valueText}` : row.label}>
                <Tag color={runtimeStateSourceColor(row.source)}>{row.label}：{row.sourceLabel}</Tag>
              </Tooltip>
            ))}
          </Space>
          {preset.filters.map(filter => (
            <label key={filter.filterKey} style={runtimeControlRowStyle}>
              <Space size={4}>
                <Text style={{ fontSize: 12 }}>{filter.label}</Text>
                {dashboardRuntimeFilterLocked(preset, filter) && <Tag color="gold">锁定</Tag>}
              </Space>
          <Space.Compact style={{ width: '100%' }}>
                <DashboardRuntimeControlEditor
                  filter={filter}
                  runtimeParameters={runtimeParameters}
                  disabled={dashboardRuntimeFilterLocked(preset, filter)}
                  ariaPrefix={filter.label}
                  optionResult={controlOptionResults[filter.filterKey]}
                  onChange={rawValue => onRuntimeParameterChange(filter.filterKey, rawValue)}
                />
                <Button
                  size="small"
                  aria-label={`清除${filter.label}运行参数`}
                  icon={<CloseCircleOutlined />}
                  disabled={dashboardRuntimeFilterLocked(preset, filter)}
                  onClick={() => onRuntimeParameterClear(filter.filterKey)}
                />
              </Space.Compact>
            </label>
          ))}
        </Space>
      </ConfigBlock>
      <ConfigBlock title="查询控件">
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          {preset.filters.map(filter => (
            <div key={filter.filterKey} style={controlOptionRowStyle}>
              <Space size={[4, 4]} wrap>
                <Tag icon={<SlidersOutlined />}>{filter.label} · {controlLabel(filter.controlType)}</Tag>
                <Tag color={filter.cascade?.parentFilterKeys?.length ? 'blue' : 'default'}>
                  级联 {cascadeParentLabels(preset, filter)}
                </Tag>
              </Space>
              <DashboardControlOptionPreview
                filter={filter}
                result={controlOptionResults[filter.filterKey]}
                loading={loadingControlOptions}
              />
            </div>
          ))}
        </Space>
      </ConfigBlock>
      <ConfigBlock title="组件交互">
        {interactions.length === 0 ? (
          <Text type="secondary">未配置</Text>
        ) : interactions.map(interaction => {
          const targetUrl = buildDashboardInteractionTarget(preset, interaction, sampleRow, {
            canvasId,
            runtimeParameters,
          })
          return (
            <div key={interaction.interactionKey} style={interactionRowStyle}>
              <Text strong style={{ fontSize: 12 }}>{interactionLabel(interaction.interactionType)}</Text>
              <Text type="secondary" style={{ fontSize: 12 }}>{interaction.fieldKey}</Text>
              {interaction.target && <Text type="secondary" ellipsis style={{ maxWidth: 180, fontSize: 12 }}>{interaction.target}</Text>}
              {targetUrl && (
                <Button
                  type="link"
                  size="small"
                  href={targetUrl}
                  style={{ padding: 0, height: 'auto', maxWidth: 180 }}
                >
                  打开目标
                </Button>
              )}
            </div>
          )
        })}
      </ConfigBlock>
      <ConfigBlock title="分发">
        <Space size={[4, 4]} wrap>
          {preset.subscriptionChannels.map(channel => <Tag key={channel} icon={<SendOutlined />}>{channel}</Tag>)}
          {preset.embedScopes.map(scope => <Tag key={scope} icon={<LinkOutlined />}>{scope}</Tag>)}
        </Space>
      </ConfigBlock>
      <ConfigBlock title="嵌入 Ticket">
        <Space direction="vertical" size={6} style={{ width: '100%', marginBottom: 8 }}>
          <Text strong style={{ fontSize: 12 }}>嵌入参数预览</Text>
          <Space size={[4, 4]} wrap>
            {embedPreviewRows.map(row => (
              <Tag key={row.key} color={row.key.startsWith('filter:') ? 'blue' : row.key.startsWith('parameter:') ? 'purple' : 'default'}>
                {row.label}：{row.value}
              </Tag>
            ))}
          </Space>
        </Space>
        {embedTicket ? (
          <Space direction="vertical" size={6} style={{ width: '100%' }}>
            <Input.TextArea value={embedTicket.embedUrl} autoSize={{ minRows: 2, maxRows: 4 }} readOnly />
            <Text type="secondary" style={{ fontSize: 12 }}>过期时间：{embedTicket.expiresAt}</Text>
          </Space>
        ) : (
          <Text type="secondary">点击顶部嵌入生成短期链接</Text>
        )}
      </ConfigBlock>
    </Space>
  )
}

function DashboardControlOptionPreview({
  filter,
  result,
  loading,
}: {
  filter: BiDashboardFilterPreset
  result?: BiQueryResult
  loading: boolean
}) {
  if (loading) {
    return <Text type="secondary" style={{ fontSize: 12 }}>候选值加载中</Text>
  }
  const optionFieldKey = filter.optionFieldKey ?? filter.fieldKey
  const values = (result?.rows ?? [])
    .map(row => row[optionFieldKey])
    .filter(value => value !== undefined && value !== null && String(value).trim() !== '')
    .slice(0, 6)
  if (values.length === 0) {
    return <Text type="secondary" style={{ fontSize: 12 }}>暂无候选值</Text>
  }
  return (
    <Space size={[4, 4]} wrap>
      {values.map((value, index) => (
        <Tag key={`${optionFieldKey}-${index}`}>{formatCellValue(optionFieldKey, value)}</Tag>
      ))}
    </Space>
  )
}

function cascadeParentLabels(preset: BiDashboardPresetLike, filter: BiDashboardFilterPreset): string {
  const labels = (filter.cascade?.parentFilterKeys ?? [])
    .map(parentKey => preset.filters.find(item => item.filterKey === parentKey || item.fieldKey === parentKey)?.label ?? parentKey)
  return labels.length ? labels.join(' / ') : '无'
}

function ConfigBlock({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div>
      <Text type="secondary" style={smallLabelStyle}>{title}</Text>
      <div style={{ marginTop: 6 }}>{children}</div>
      <Divider style={{ margin: '12px 0 0' }} />
    </div>
  )
}

const topbarStyle: CSSProperties = {
  height: 64,
  padding: '0 20px',
  background: '#fff',
  borderBottom: '1px solid #e5e7eb',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 16,
}

const moduleBarStyle: CSSProperties = {
  padding: '10px 20px',
  background: '#fff',
  borderBottom: '1px solid #e5e7eb',
  overflowX: 'auto',
}

const designerShellStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: '280px minmax(640px, 1fr) 320px',
  gap: 12,
  padding: 12,
  alignItems: 'stretch',
}

const leftPanelStyle: CSSProperties = {
  minHeight: 620,
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
  padding: '10px 12px',
  overflow: 'hidden',
}

const rightPanelStyle: CSSProperties = {
  minHeight: 620,
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
  padding: 12,
  overflow: 'hidden',
}

const canvasPanelStyle: CSSProperties = {
  minHeight: 620,
  background: '#eef2f7',
  border: '1px solid #dbe2ec',
  borderRadius: 8,
  overflow: 'hidden',
}

const canvasToolbarStyle: CSSProperties = {
  height: 46,
  background: '#fff',
  borderBottom: '1px solid #dbe2ec',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  padding: '0 12px',
  gap: 12,
}

const dashboardCanvasStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(20, minmax(0, 1fr))',
  gridAutoRows: DASHBOARD_GRID_ROW_HEIGHT,
  gap: 10,
  padding: 12,
  alignItems: 'stretch',
}

const widgetCardStyle: CSSProperties = {
  position: 'relative',
  background: '#fff',
  border: '1px solid #d7dde8',
  borderRadius: 8,
  padding: 12,
  textAlign: 'left',
  cursor: 'pointer',
  overflow: 'hidden',
}

const widgetCardHeaderStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  width: '100%',
  gap: 8,
  marginBottom: 10,
}

const widgetDragHandleStyle: CSSProperties = {
  minWidth: 0,
  flex: 1,
  cursor: 'grab',
}

const resizeHandleStyle: CSSProperties = {
  position: 'absolute',
  right: 4,
  bottom: 4,
  width: 18,
  height: 18,
  cursor: 'nwse-resize',
  borderRight: '2px solid #94a3b8',
  borderBottom: '2px solid #94a3b8',
  borderRadius: 2,
}

const datasetBandStyle: CSSProperties = {
  margin: '0 12px 16px',
  padding: 14,
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
}

const versionBandStyle: CSSProperties = {
  margin: '0 12px 12px',
  padding: 14,
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
}

const governanceBandStyle: CSSProperties = {
  margin: '0 12px 12px',
  padding: 14,
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
}

const permissionBandStyle: CSSProperties = {
  margin: '0 12px 12px',
  padding: 14,
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
}

const selfServiceBandStyle: CSSProperties = {
  margin: '0 12px 12px',
  padding: 14,
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
}

const extractionDropZoneStyle: CSSProperties = {
  minHeight: 82,
  padding: 10,
  border: '1px dashed #cbd5e1',
  borderRadius: 8,
  background: '#f8fafc',
}

const spreadsheetPivotDropZoneStyle: CSSProperties = {
  minHeight: 30,
  minWidth: 120,
  padding: '4px 8px',
  border: '1px dashed #cbd5e1',
  borderRadius: 6,
  background: '#f8fafc',
  display: 'inline-flex',
  alignItems: 'center',
  gap: 6,
}

const spreadsheetPivotPreviewGridStyle: CSSProperties = {
  width: '100%',
  maxWidth: 760,
  display: 'grid',
  gridTemplateColumns: 'repeat(8, minmax(74px, 1fr))',
  borderTop: '1px solid #e5e7eb',
  borderLeft: '1px solid #e5e7eb',
  overflowX: 'auto',
}

const spreadsheetPivotPreviewCellStyle: CSSProperties = {
  minHeight: 28,
  minWidth: 0,
  padding: '5px 8px',
  borderRight: '1px solid #e5e7eb',
  borderBottom: '1px solid #e5e7eb',
  background: '#fff',
  fontSize: 12,
  whiteSpace: 'nowrap',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
}

const subscriptionBandStyle: CSSProperties = {
  margin: '0 12px 12px',
  padding: 14,
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
}

const runtimePageStyle: CSSProperties = {
  minHeight: '100vh',
  background: '#f4f6f9',
  color: '#111827',
}

const runtimeTopbarStyle: CSSProperties = {
  minHeight: 72,
  padding: '12px 20px',
  background: '#fff',
  borderBottom: '1px solid #e5e7eb',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 16,
}

const runtimeShellStyle: CSSProperties = {
  padding: 16,
}

const bigScreenRuntimeStageStyle: CSSProperties = {
  minHeight: 560,
  display: 'grid',
  gridTemplateColumns: 'repeat(24, minmax(0, 1fr))',
  gridAutoRows: 34,
  gap: 10,
  padding: 18,
  border: '1px solid #1f2937',
  borderRadius: 8,
  overflow: 'hidden',
}

const bigScreenRuntimeWidgetStyle: CSSProperties = {
  minWidth: 0,
  minHeight: 0,
  padding: 14,
  border: '1px solid rgba(148, 163, 184, .42)',
  borderRadius: 8,
  background: 'rgba(15, 23, 42, .72)',
  boxShadow: '0 14px 32px rgba(0, 0, 0, .18)',
  overflow: 'hidden',
}

const bigScreenRuntimeEmptyStyle: CSSProperties = {
  gridColumn: '1 / -1',
  minHeight: 180,
  border: '1px dashed rgba(203, 213, 225, .48)',
  borderRadius: 8,
  color: '#cbd5e1',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
}

const spreadsheetRuntimePanelStyle: CSSProperties = {
  padding: 14,
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
}

const spreadsheetGridStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: '42px repeat(8, minmax(90px, 1fr))',
  borderTop: '1px solid #e5e7eb',
  borderLeft: '1px solid #e5e7eb',
  overflowX: 'auto',
}

const spreadsheetHeaderCellStyle: CSSProperties = {
  minHeight: 32,
  padding: '6px 8px',
  borderRight: '1px solid #e5e7eb',
  borderBottom: '1px solid #e5e7eb',
  background: '#f8fafc',
  color: '#475569',
  fontSize: 12,
  fontWeight: 600,
}

const spreadsheetCellStyle: CSSProperties = {
  minHeight: 32,
  minWidth: 0,
  padding: '6px 8px',
  borderRight: '1px solid #e5e7eb',
  borderBottom: '1px solid #e5e7eb',
  backgroundColor: '#fff',
  overflow: 'hidden',
}

const brandIconStyle: CSSProperties = {
  width: 36,
  height: 36,
  borderRadius: 8,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: '#111827',
  color: '#fff',
}

const panelIconStyle: CSSProperties = {
  width: 24,
  height: 24,
  borderRadius: 6,
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: '#eff6ff',
  color: '#2563eb',
}

const resourceRowStyle: CSSProperties = {
  height: 30,
  display: 'flex',
  alignItems: 'center',
  gap: 8,
  padding: '0 8px',
  border: '1px solid #e5e7eb',
  borderRadius: 6,
  background: '#f8fafc',
}

const paletteItemStyle: CSSProperties = {
  padding: '8px 0',
  borderBlockEnd: '1px solid #edf2f7',
}

const tablePreviewRowStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: '1fr auto auto',
  gap: 10,
  alignItems: 'center',
  padding: '8px 0',
  borderBottom: '1px solid #edf2f7',
}

const interactionRowStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: '54px 1fr',
  gap: 6,
  padding: '8px 0',
  borderBottom: '1px solid #edf2f7',
}

const controlOptionRowStyle: CSSProperties = {
  display: 'grid',
  gap: 6,
  padding: '8px 0',
  borderBottom: '1px solid #edf2f7',
}

const runtimeControlRowStyle: CSSProperties = {
  display: 'grid',
  gap: 4,
}

const smallLabelStyle: CSSProperties = {
  display: 'block',
  fontSize: 12,
  letterSpacing: 0,
}
