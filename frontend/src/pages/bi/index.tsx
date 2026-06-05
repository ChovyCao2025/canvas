import { useEffect, useMemo, useRef, useState, type ChangeEvent, type CSSProperties, type PointerEvent as ReactPointerEvent, type ReactNode } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  Badge,
  Button,
  Col,
  DatePicker,
  Divider,
  Empty,
  Input,
  List,
  Progress,
  Row,
  Segmented,
  Select,
  Space,
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
  CopyOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  DeploymentUnitOutlined,
  DownloadOutlined,
  CloudUploadOutlined,
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
  alignDashboardWidgets,
  buildDashboardCloneCommand,
  buildDashboardImportCommand,
  buildResourceCommentCommand,
  buildResourceFavoriteCommand,
  buildResourceLockCommand,
  buildResourceMoveCommand,
  buildExportApprovalReviewCommand,
  buildPublishApprovalRequestCommand,
  buildPublishApprovalReviewCommand,
  buildResourceTransferCommand,
  dashboardWidgetGridPlacementForColumns,
  dashboardLayoutColumns,
  dashboardPackageFileName,
  dashboardResponsiveWidgets,
  buildEmbedTicketRequest,
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
  resourceCommentScopeLabel,
  resourceFavoriteLabel,
  resourceFolderLabel,
  resourceLocationIndexKey,
  resourceLockLabel,
  resourceLockTokenFor,
  resourceOwnerLabel,
  exportApprovalStatusLabel,
  publishApprovalStatusLabel,
  toResourceFavoriteIndex,
  toResourceLocationIndex,
  toResourceOwnershipIndex,
  toMarketingDatasetPreset,
  createDashboardPresetHistory,
  pushDashboardPresetHistory,
  redoDashboardPresetHistory,
  undoDashboardPresetHistory,
  type BiDashboardPresetLike,
  type BiDashboardWidgetPreset,
  type DashboardWidgetAlignment,
  type DashboardLayoutMode,
  type DashboardWidgetMoveDirection,
  type BiSectionKey,
  type MarketingDatasetPreset,
} from './biWorkbench'
import {
  biApi,
  type BiCompiledQuery,
  type BiDeliveryAttachmentCleanupResult,
  type BiDeliveryAuditSummary,
  type BiAlertRuleView,
  type BiChartResource,
  type BiChartVersionView,
  type BiDatasetResource,
  type BiDatasetVersionView,
  type BiDatasetView,
  type BiDatasourceHealth,
  type BiDashboardExportPackage,
  type BiDashboardPreset,
  type BiDashboardResource,
  type BiDashboardVersionView,
  type BiDeliveryAttachmentView,
  type BiDeliveryLogView,
  type BiDeliveryRetryResult,
  type BiDeliverySchedulerResult,
  type BiEmbedTicket,
  type BiExportCleanupResult,
  type BiExportJobView,
  type BiExportRetryResult,
  type BiPortalResource,
  type BiPortalVersionView,
  type BiColumnPermissionView,
  type BiQueryColumn,
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

const { RangePicker } = DatePicker
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

const datasetResourceColumns: ColumnsType<BiDatasetResource> = [
  {
    title: '资产',
    dataIndex: 'name',
    render: (name: string, row) => (
      <Space direction="vertical" size={2}>
        <Space size={6}>
          <Text strong>{name}</Text>
          <Tag color={row.status === 'PUBLISHED' ? 'green' : row.status === 'DRAFT' ? 'gold' : 'default'}>{row.status}</Tag>
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

const resourcePermissionColumns: ColumnsType<BiResourcePermissionView> = [
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
]

const rowPermissionColumns: ColumnsType<BiRowPermissionView> = [
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
]

const columnPermissionColumns: ColumnsType<BiColumnPermissionView> = [
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
]

function createExportJobColumns(
  reviewingExport: string | null,
  onReview: (row: BiExportJobView, status: 'APPROVED' | 'REJECTED') => void,
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
          {row.status === 'PENDING_APPROVAL' && row.approvalStatus === 'PENDING' && (
            <>
              <Tooltip title="批准导出">
                <Button
                  size="small"
                  icon={<CheckCircleOutlined />}
                  loading={reviewingExport === `${row.id}:APPROVED`}
                  onClick={() => onReview(row, 'APPROVED')}
                />
              </Tooltip>
              <Tooltip title="驳回导出">
                <Button
                  size="small"
                  danger
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

export default function BiWorkbenchPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const dashboardPackageInputRef = useRef<HTMLInputElement | null>(null)
  const dashboardKey = searchParams.get('dashboard') || 'canvas-effect'
  const canvasId = searchParams.get('canvasId')
  const [sectionKey, setSectionKey] = useState<BiSectionKey>('dashboard')
  const [datasets, setDatasets] = useState<MarketingDatasetPreset[]>(DEFAULT_MARKETING_DATASETS)
  const [datasetMetadata, setDatasetMetadata] = useState<BiDatasetView | null>(null)
  const [datasetResources, setDatasetResources] = useState<BiDatasetResource[]>([])
  const [selectedDatasetKey, setSelectedDatasetKey] = useState<string | null>(null)
  const [datasetVersions, setDatasetVersions] = useState<BiDatasetVersionView[]>([])
  const [chartResources, setChartResources] = useState<BiChartResource[]>([])
  const [selectedChartKey, setSelectedChartKey] = useState<string | null>(null)
  const [chartVersions, setChartVersions] = useState<BiChartVersionView[]>([])
  const [portalResources, setPortalResources] = useState<BiPortalResource[]>([])
  const [selectedPortalKey, setSelectedPortalKey] = useState<string | null>(null)
  const [portalVersions, setPortalVersions] = useState<BiPortalVersionView[]>([])
  const [portalRuntimeResources, setPortalRuntimeResources] = useState<BiPortalResource[]>([])
  const [dashboardPreset, setDashboardPreset] = useState<BiDashboardPresetLike>(() => getDefaultDashboardPreset(dashboardKey))
  const [dashboardHistory, setDashboardHistory] = useState(() => createDashboardPresetHistory(dashboardPreset))
  const [dashboardResource, setDashboardResource] = useState<BiDashboardResource | null>(null)
  const [dashboardVersions, setDashboardVersions] = useState<BiDashboardVersionView[]>([])
  const [dashboardExportPackage, setDashboardExportPackage] = useState<BiDashboardExportPackage | null>(null)
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
  const [queryResults, setQueryResults] = useState<Record<string, BiQueryResult>>({})
  const [compiledWidgetQuery, setCompiledWidgetQuery] = useState<BiCompiledQuery | null>(null)
  const [compilingWidgetKey, setCompilingWidgetKey] = useState<string | null>(null)
  const [queryHistory, setQueryHistory] = useState<BiQueryHistoryItem[]>([])
  const [datasourceHealth, setDatasourceHealth] = useState<BiDatasourceHealth[]>([])
  const [resourcePermissions, setResourcePermissions] = useState<BiResourcePermissionView[]>([])
  const [rowPermissions, setRowPermissions] = useState<BiRowPermissionView[]>([])
  const [columnPermissions, setColumnPermissions] = useState<BiColumnPermissionView[]>([])
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
  const [moveResourceTarget, setMoveResourceTarget] = useState('DASHBOARD')
  const [moveFolderKey, setMoveFolderKey] = useState('marketing')
  const [movingResource, setMovingResource] = useState<string | null>(null)
  const [transferOwnerUser, setTransferOwnerUser] = useState('owner@example.com')
  const [transferringResource, setTransferringResource] = useState<string | null>(null)
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
  const [cleaningExports, setCleaningExports] = useState(false)
  const [retryingExports, setRetryingExports] = useState(false)
  const [selfServicePreview, setSelfServicePreview] = useState<BiQueryResult | null>(null)
  const [exportCleanupResult, setExportCleanupResult] = useState<BiExportCleanupResult | null>(null)
  const [exportRetryResult, setExportRetryResult] = useState<BiExportRetryResult | null>(null)
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
  const dashboardDisplayColumns = dashboardLayoutColumns(dashboardLayoutMode)
  const canUndoDashboardEdit = dashboardHistory.past.length > 0
  const canRedoDashboardEdit = dashboardHistory.future.length > 0

  const resetDashboardPreset = (preset: BiDashboardPresetLike) => {
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
  const selectedPortalResource = useMemo(
    () => portalResources.find(portal => portal.portalKey === selectedPortalKey) ?? portalResources[0] ?? null,
    [portalResources, selectedPortalKey],
  )
  const resourceLocationIndex = useMemo(
    () => toResourceLocationIndex(resourceLocations),
    [resourceLocations],
  )
  const resourceOwnershipIndex = useMemo(
    () => toResourceOwnershipIndex(resourceOwnerships),
    [resourceOwnerships],
  )
  const resourceFavoriteIndex = useMemo(
    () => toResourceFavoriteIndex(resourceFavorites),
    [resourceFavorites],
  )
  const resourceMoveTargets = useMemo(() => [
    {
      label: '当前仪表板',
      value: 'DASHBOARD',
      resourceType: 'DASHBOARD',
      resourceKey: dashboardPreset.dashboardKey,
    },
    {
      label: '选中图表',
      value: 'CHART',
      resourceType: 'CHART',
      resourceKey: selectedChartResource?.chartKey ?? '',
      disabled: !selectedChartResource,
    },
    {
      label: '选中数据集',
      value: 'DATASET',
      resourceType: 'DATASET',
      resourceKey: selectedDatasetResource?.datasetKey ?? '',
      disabled: !selectedDatasetResource,
    },
    {
      label: '选中门户',
      value: 'PORTAL',
      resourceType: 'PORTAL',
      resourceKey: selectedPortalResource?.portalKey ?? '',
      disabled: !selectedPortalResource,
    },
  ], [dashboardPreset.dashboardKey, selectedChartResource, selectedDatasetResource, selectedPortalResource])
  const selectedMoveTarget = useMemo(
    () => resourceMoveTargets.find(target => target.value === moveResourceTarget && !target.disabled) ?? resourceMoveTargets[0],
    [resourceMoveTargets, moveResourceTarget],
  )
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
    reloadPortalVersions(selectedPortalResource?.portalKey)
  }, [selectedPortalResource?.portalKey])

  useEffect(() => {
    let cancelled = false
    setLoadingQueries(true)
    Promise.all(dashboardPreset.widgets.map(widget =>
      biApi.executeQuery(buildWidgetQueryRequest(dashboardPreset, widget, canvasId))
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
  }, [dashboardPreset, canvasId])

  useEffect(() => {
    if (loadingQueries) return
    let cancelled = false
    setLoadingGovernance(true)
    Promise.all([
      biApi.listQueryHistory(10).catch(() => ({ data: [] })),
      biApi.listDatasourceHealth().catch(() => ({ data: [] })),
    ])
      .then(([historyResponse, healthResponse]) => {
        if (cancelled) return
        setQueryHistory(historyResponse.data)
        setDatasourceHealth(healthResponse.data)
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
    Promise.all([
      biApi.listResourcePermissions({ resourceType: 'DATASET', resourceKey: dashboardPreset.datasetKey }).catch(() => ({ data: [] })),
      biApi.listRowPermissions(dashboardPreset.datasetKey).catch(() => ({ data: [] })),
      biApi.listColumnPermissions(dashboardPreset.datasetKey).catch(() => ({ data: [] })),
    ])
      .then(([resourceResponse, rowResponse, columnResponse]) => {
        setResourcePermissions(resourceResponse.data ?? [])
        setRowPermissions(rowResponse.data ?? [])
        setColumnPermissions(columnResponse.data ?? [])
      })
      .finally(() => setLoadingPermissions(false))
  }

  useEffect(() => {
    reloadPermissions()
  }, [dashboardPreset.datasetKey])

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
    biApi.createEmbedTicket(buildEmbedTicketRequest(dashboardPreset, canvasId))
      .then(response => {
        setEmbedTicket(response.data)
        setConfigTab('interaction')
      })
      .finally(() => setCreatingEmbedTicket(false))
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
    biApi.compileQuery(buildWidgetQueryRequest(dashboardPreset, widget, canvasId))
      .then(response => setCompiledWidgetQuery(response.data ?? null))
      .catch(() => setCompiledWidgetQuery(null))
      .finally(() => setCompilingWidgetKey(null))
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

  const grantDatasetUse = () => {
    setSavingPermission('resource')
    biApi.upsertResourcePermission({
      resourceType: 'DATASET',
      resourceKey: dashboardPreset.datasetKey,
      subjectType: 'ROLE',
      subjectId: 'OPERATOR',
      actionKey: 'USE',
      effect: 'ALLOW',
    })
      .then(reloadPermissions)
      .finally(() => setSavingPermission(null))
  }

  const grantDatasetExport = () => {
    setSavingPermission('export')
    biApi.upsertResourcePermission({
      resourceType: 'DATASET',
      resourceKey: dashboardPreset.datasetKey,
      subjectType: 'ROLE',
      subjectId: 'OPERATOR',
      actionKey: 'EXPORT',
      effect: 'ALLOW',
    })
      .then(reloadPermissions)
      .finally(() => setSavingPermission(null))
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

  const selfServiceQuery = () => buildWidgetQueryRequest(dashboardPreset, selectedWidget, canvasId, 1000)

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
      buildExportApprovalReviewCommand(status, status === 'APPROVED' ? '工作台通过' : '工作台驳回'),
    )
      .then(() => reloadExports())
      .finally(() => setReviewingExport(null))
  }

  const exportJobTableColumns = useMemo(
    () => createExportJobColumns(reviewingExport, reviewSelfServiceExport),
    [reviewingExport],
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
            <Space size={8} wrap>
              <RangePicker size="small" suffixIcon={<CalendarOutlined />} />
              <Select
                size="small"
                placeholder="画布名称"
                style={{ width: 150 }}
                options={[{ label: '全部画布', value: 'all' }, { label: '欢迎旅程', value: 'welcome' }]}
              />
              <Input size="small" prefix={<FilterOutlined />} placeholder="触发方式" style={{ width: 140 }} />
            </Space>
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
                      compiling={compilingWidgetKey === selectedWidget.widgetKey}
                      onCompile={() => compileWidgetQuery(selectedWidget)}
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
                  children: <InteractionConfig preset={dashboardPreset} widget={selectedWidget} embedTicket={embedTicket} />,
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
                columns={queryHistoryColumns}
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
              <Tag color="blue">{dashboardPreset.datasetKey}</Tag>
            </Space>
            <Space size={8} wrap>
              <Button size="small" icon={<LockOutlined />} loading={savingPermission === 'resource'} onClick={grantDatasetUse}>授予使用</Button>
              <Button size="small" icon={<DownloadOutlined />} loading={savingPermission === 'export'} onClick={grantDatasetExport}>授予导出</Button>
              <Button size="small" icon={<FilterOutlined />} loading={savingPermission === 'row'} onClick={addCanvasRowPermission}>行权限</Button>
              <Button size="small" icon={<SafetyCertificateOutlined />} loading={savingPermission === 'column'} onClick={maskCanvasName}>字段脱敏</Button>
              <Badge status={loadingPermissions ? 'processing' : 'success'} text={loadingPermissions ? '同步中' : '已就绪'} />
            </Space>
          </Space>
          <Row gutter={12}>
            <Col xs={24} xl={8}>
              <Table
                rowKey="id"
                size="small"
                loading={loadingPermissions}
                pagination={false}
                columns={resourcePermissionColumns}
                dataSource={resourcePermissions}
                locale={{ emptyText: '暂无资源授权' }}
                scroll={{ x: 520 }}
              />
            </Col>
            <Col xs={24} xl={8}>
              <Table
                rowKey="id"
                size="small"
                loading={loadingPermissions}
                pagination={false}
                columns={rowPermissionColumns}
                dataSource={rowPermissions}
                locale={{ emptyText: '暂无行权限' }}
                scroll={{ x: 560 }}
              />
            </Col>
            <Col xs={24} xl={8}>
              <Table
                rowKey="id"
                size="small"
                loading={loadingPermissions}
                pagination={false}
                columns={columnPermissionColumns}
                dataSource={columnPermissions}
                locale={{ emptyText: '暂无列权限' }}
                scroll={{ x: 520 }}
              />
            </Col>
          </Row>
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
              <Badge status={loadingResourceLocations ? 'processing' : 'success'} text={loadingResourceLocations ? '同步中' : '已就绪'} />
            </Space>
          </Space>
          <Table
            rowKey={row => resourceLocationIndexKey(row.resourceType, row.resourceKey)}
            size="small"
            loading={loadingResourceLocations}
            pagination={false}
            columns={resourceLocationColumns}
            dataSource={resourceLocations}
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
            rowKey={row => resourceLocationIndexKey(row.resourceType, row.resourceKey)}
            size="small"
            loading={loadingResourceOwnerships}
            pagination={false}
            columns={resourceOwnershipColumns}
            dataSource={resourceOwnerships}
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
            rowKey={row => resourceLocationIndexKey(row.resourceType, row.resourceKey)}
            size="small"
            loading={loadingResourceFavorites}
            pagination={false}
            columns={resourceFavoriteColumns}
            dataSource={resourceFavorites}
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
        ) : items.map(item => (
          <div key={item} style={resourceRowStyle}>
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

function DataConfig({
  widget,
  queryResult,
  compiledQuery,
  compiling,
  onCompile,
}: {
  widget: BiDashboardWidgetPreset
  queryResult?: BiQueryResult
  compiledQuery: BiCompiledQuery | null
  compiling: boolean
  onCompile: () => void
}) {
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
          <Button block icon={<EyeOutlined />} loading={compiling} onClick={onCompile}>查看 SQL</Button>
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

function InteractionConfig({
  preset,
  widget,
  embedTicket,
}: {
  preset: BiDashboardPresetLike
  widget: BiDashboardWidgetPreset
  embedTicket: BiEmbedTicket | null
}) {
  const interactions = preset.interactions.filter(interaction => interaction.sourceWidgetKey === widget.widgetKey)
  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <ConfigBlock title="查询控件">
        <Space size={[4, 4]} wrap>
          {preset.filters.map(filter => <Tag key={filter.filterKey} icon={<SlidersOutlined />}>{filter.label} · {controlLabel(filter.controlType)}</Tag>)}
        </Space>
      </ConfigBlock>
      <ConfigBlock title="组件交互">
        {interactions.length === 0 ? (
          <Text type="secondary">未配置</Text>
        ) : interactions.map(interaction => (
          <div key={interaction.interactionKey} style={interactionRowStyle}>
            <Text strong style={{ fontSize: 12 }}>{interactionLabel(interaction.interactionType)}</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>{interaction.fieldKey}</Text>
            {interaction.target && <Text type="secondary" ellipsis style={{ maxWidth: 180, fontSize: 12 }}>{interaction.target}</Text>}
          </div>
        ))}
      </ConfigBlock>
      <ConfigBlock title="分发">
        <Space size={[4, 4]} wrap>
          {preset.subscriptionChannels.map(channel => <Tag key={channel} icon={<SendOutlined />}>{channel}</Tag>)}
          {preset.embedScopes.map(scope => <Tag key={scope} icon={<LinkOutlined />}>{scope}</Tag>)}
        </Space>
      </ConfigBlock>
      <ConfigBlock title="嵌入 Ticket">
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

const subscriptionBandStyle: CSSProperties = {
  margin: '0 12px 12px',
  padding: 14,
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
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

const smallLabelStyle: CSSProperties = {
  display: 'block',
  fontSize: 12,
  letterSpacing: 0,
}
