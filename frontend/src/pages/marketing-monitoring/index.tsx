import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  BarChartOutlined,
  CheckCircleOutlined,
  CopyOutlined,
  DisconnectOutlined,
  KeyOutlined,
  LinkOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined,
  SoundOutlined,
  StopOutlined,
  SyncOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import { marketingMonitoringApi } from '../../services/marketingMonitoringApi'
import {
  alertStatusView,
  calculateMonitoringKpis,
  credentialStatusView,
  defaultOAuthRedirectUri,
  formatMonitorDateTime,
  normalizeAlertQuery,
  normalizeCredentialEventQuery,
  normalizeCredentialQuery,
  normalizeItemQuery,
  normalizeOAuthAuthorizationQuery,
  normalizeTrendQuery,
  oauthAuthorizationStatusView,
  parseCompetitorMap,
  parseJsonObject,
  parseScopes,
  sentimentView,
  severityColor,
  type MarketingMonitorAlert,
  type MarketingMonitorAlertQuery,
  type MarketingMonitorItem,
  type MarketingMonitorItemIngestPayload,
  type MarketingMonitorItemQuery,
  type MarketingMonitorProviderCredential,
  type MarketingMonitorProviderCredentialEvent,
  type MarketingMonitorProviderCredentialEventQuery,
  type MarketingMonitorProviderCredentialQuery,
  type MarketingMonitorProviderOAuthAuthorization,
  type MarketingMonitorProviderOAuthAuthorizationCommand,
  type MarketingMonitorProviderOAuthAuthorizationQuery,
  type MarketingMonitorSourcePayload,
  type MarketingMonitorTrendSnapshot,
  type MarketingMonitorTrendSnapshotPayload,
  type MarketingMonitorTrendSnapshotQuery,
} from './monitoringWorkbench'

const { Title, Text } = Typography

interface SourceFormValues {
  sourceKey: string
  sourceType: string
  displayName: string
  enabled?: boolean
  metadataJson?: string
}

interface IngestFormValues {
  sourceId: number
  externalItemId: string
  sourceUrl?: string
  authorKey?: string
  brandKey?: string
  text: string
  language?: string
  publishedAt?: string
  competitorsJson?: string
  rawPayloadJson?: string
}

interface TrendBuildFormValues {
  sourceId: number
  bucketGrain: string
  bucketStart: string
  bucketEnd: string
  brandKey?: string
  competitorKey?: string
  metadataJson?: string
}

interface OAuthStartFormValues {
  credentialKey: string
  providerType: string
  authType: string
  displayName: string
  authorizeEndpoint: string
  tokenEndpoint: string
  revokeEndpoint?: string
  redirectUri: string
  clientId: string
  clientSecret?: string
  scopesText?: string
  authorizeParamsJson?: string
  expiresInMinutes?: number
  metadataJson?: string
}

interface OAuthCallbackFormValues {
  state: string
  code?: string
  error?: string
  errorDescription?: string
  metadataJson?: string
}

const SOURCE_TYPE_OPTIONS = ['MANUAL', 'SOCIAL', 'WEB', 'REVIEW', 'COMMUNITY', 'SANDBOX']
  .map(value => ({ value, label: value }))
const SENTIMENT_OPTIONS = ['NEGATIVE', 'NEUTRAL', 'POSITIVE'].map(value => ({
  value,
  label: sentimentView(value).text,
}))
const ALERT_STATUS_OPTIONS = ['OPEN', 'RESOLVED'].map(value => ({
  value,
  label: alertStatusView(value).text,
}))
const BUCKET_GRAIN_OPTIONS = ['HOUR', 'DAY', 'WEEK', 'MONTH'].map(value => ({ value, label: value }))
const CREDENTIAL_STATUS_OPTIONS = ['ACTIVE', 'DISABLED'].map(value => ({
  value,
  label: credentialStatusView(value).text,
}))
const OAUTH_STATUS_OPTIONS = ['PENDING', 'EXCHANGED', 'FAILED', 'EXPIRED'].map(value => ({
  value,
  label: oauthAuthorizationStatusView(value).text,
}))

export default function MarketingMonitoringPage() {
  const [sourceForm] = Form.useForm<SourceFormValues>()
  const [ingestForm] = Form.useForm<IngestFormValues>()
  const [itemFilterForm] = Form.useForm<MarketingMonitorItemQuery>()
  const [alertFilterForm] = Form.useForm<MarketingMonitorAlertQuery>()
  const [trendFilterForm] = Form.useForm<MarketingMonitorTrendSnapshotQuery>()
  const [trendBuildForm] = Form.useForm<TrendBuildFormValues>()
  const [credentialFilterForm] = Form.useForm<MarketingMonitorProviderCredentialQuery>()
  const [credentialEventFilterForm] = Form.useForm<MarketingMonitorProviderCredentialEventQuery>()
  const [oauthAuthorizationFilterForm] = Form.useForm<MarketingMonitorProviderOAuthAuthorizationQuery>()
  const [oauthStartForm] = Form.useForm<OAuthStartFormValues>()
  const [oauthCallbackForm] = Form.useForm<OAuthCallbackFormValues>()
  const [items, setItems] = useState<MarketingMonitorItem[]>([])
  const [alerts, setAlerts] = useState<MarketingMonitorAlert[]>([])
  const [trends, setTrends] = useState<MarketingMonitorTrendSnapshot[]>([])
  const [credentials, setCredentials] = useState<MarketingMonitorProviderCredential[]>([])
  const [credentialEvents, setCredentialEvents] = useState<MarketingMonitorProviderCredentialEvent[]>([])
  const [oauthAuthorizations, setOAuthAuthorizations] = useState<MarketingMonitorProviderOAuthAuthorization[]>([])
  const [itemFilters, setItemFilters] = useState<MarketingMonitorItemQuery>({ limit: 50 })
  const [alertFilters, setAlertFilters] = useState<MarketingMonitorAlertQuery>({ status: 'OPEN', limit: 50 })
  const [trendFilters, setTrendFilters] = useState<MarketingMonitorTrendSnapshotQuery>({ limit: 50 })
  const [credentialFilters, setCredentialFilters] = useState<MarketingMonitorProviderCredentialQuery>({
    status: 'ACTIVE',
    limit: 50,
  })
  const [credentialEventFilters, setCredentialEventFilters] = useState<MarketingMonitorProviderCredentialEventQuery>({
    limit: 50,
  })
  const [oauthAuthorizationFilters, setOAuthAuthorizationFilters] =
    useState<MarketingMonitorProviderOAuthAuthorizationQuery>({ limit: 50 })
  const [loading, setLoading] = useState(false)
  const [sourceSaving, setSourceSaving] = useState(false)
  const [ingesting, setIngesting] = useState(false)
  const [trendLoading, setTrendLoading] = useState(false)
  const [trendBuilding, setTrendBuilding] = useState(false)
  const [credentialLoading, setCredentialLoading] = useState(false)
  const [oauthStarting, setOAuthStarting] = useState(false)
  const [oauthCompleting, setOAuthCompleting] = useState(false)
  const [dueRefreshing, setDueRefreshing] = useState(false)
  const [refreshingCredentialKey, setRefreshingCredentialKey] = useState<string>()
  const [revokingCredentialKey, setRevokingCredentialKey] = useState<string>()
  const [disablingCredentialKey, setDisablingCredentialKey] = useState<string>()
  const [authorizationUrl, setAuthorizationUrl] = useState<string>()
  const [resolvingId, setResolvingId] = useState<number>()
  const [error, setError] = useState<string | null>(null)

  const kpis = useMemo(() => calculateMonitoringKpis(items, alerts), [items, alerts])

  const loadItems = async (nextFilters = itemFilters) => {
    const params = normalizeItemQuery(nextFilters)
    const response = await marketingMonitoringApi.listItems(params)
    setItems(response.data)
    setItemFilters(params)
    itemFilterForm.setFieldsValue(params)
  }

  const loadAlerts = async (nextFilters = alertFilters) => {
    const params = normalizeAlertQuery(nextFilters)
    const response = await marketingMonitoringApi.listAlerts(params)
    setAlerts(response.data)
    setAlertFilters(params)
    alertFilterForm.setFieldsValue(params)
  }

  const loadTrends = async (nextFilters = trendFilters) => {
    const params = normalizeTrendQuery(nextFilters)
    const response = await marketingMonitoringApi.listTrendSnapshots(params)
    setTrends(response.data)
    setTrendFilters(params)
    trendFilterForm.setFieldsValue(params)
  }

  const loadCredentials = async (nextFilters = credentialFilters) => {
    const params = normalizeCredentialQuery(nextFilters)
    const response = await marketingMonitoringApi.listProviderCredentials(params)
    setCredentials(response.data)
    setCredentialFilters(params)
    credentialFilterForm.setFieldsValue(params)
  }

  const loadCredentialEvents = async (nextFilters = credentialEventFilters) => {
    const params = normalizeCredentialEventQuery(nextFilters)
    const response = await marketingMonitoringApi.listProviderCredentialEvents(params)
    setCredentialEvents(response.data)
    setCredentialEventFilters(params)
    credentialEventFilterForm.setFieldsValue(params)
  }

  const loadOAuthAuthorizations = async (nextFilters = oauthAuthorizationFilters) => {
    const params = normalizeOAuthAuthorizationQuery(nextFilters)
    const response = await marketingMonitoringApi.listProviderOAuthAuthorizations(params)
    setOAuthAuthorizations(response.data)
    setOAuthAuthorizationFilters(params)
    oauthAuthorizationFilterForm.setFieldsValue(params)
  }

  const loadCredentialWorkspace = async () => {
    setCredentialLoading(true)
    try {
      await Promise.all([
        loadCredentials(),
        loadCredentialEvents(),
        loadOAuthAuthorizations(),
      ])
    } catch (err) {
      message.error(err instanceof Error ? err.message : '加载 Provider 凭据失败')
    } finally {
      setCredentialLoading(false)
    }
  }

  const loadAll = async () => {
    setLoading(true)
    setError(null)
    try {
      await Promise.all([loadItems(), loadAlerts(), loadTrends(), loadCredentials(), loadCredentialEvents(), loadOAuthAuthorizations()])
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载监测数据失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  const submitSource = async (values: SourceFormValues) => {
    setSourceSaving(true)
    try {
      const payload: MarketingMonitorSourcePayload = {
        sourceKey: values.sourceKey.trim(),
        sourceType: values.sourceType,
        displayName: values.displayName.trim(),
        enabled: values.enabled !== false,
        metadata: parseJsonObject(values.metadataJson),
      }
      await marketingMonitoringApi.upsertSource(payload)
      message.success('监测来源已保存')
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存监测来源失败')
    } finally {
      setSourceSaving(false)
    }
  }

  const submitIngest = async (values: IngestFormValues) => {
    setIngesting(true)
    try {
      const payload: MarketingMonitorItemIngestPayload = {
        sourceId: values.sourceId,
        externalItemId: values.externalItemId.trim(),
        sourceUrl: cleanText(values.sourceUrl),
        authorKey: cleanText(values.authorKey),
        brandKey: cleanText(values.brandKey),
        text: values.text.trim(),
        language: cleanText(values.language),
        publishedAt: cleanText(values.publishedAt),
        competitors: parseCompetitorMap(values.competitorsJson),
        rawPayload: parseJsonObject(values.rawPayloadJson),
      }
      await marketingMonitoringApi.ingestItem(payload)
      message.success('监测内容已入库')
      await Promise.all([loadItems(itemFilters), loadAlerts(alertFilters)])
    } catch (err) {
      message.error(err instanceof Error ? err.message : '监测内容入库失败')
    } finally {
      setIngesting(false)
    }
  }

  const resolveAlert = async (alert: MarketingMonitorAlert) => {
    setResolvingId(alert.id)
    try {
      await marketingMonitoringApi.resolveAlert(alert.id)
      message.success('告警已处理')
      await loadAlerts(alertFilters)
    } finally {
      setResolvingId(undefined)
    }
  }

  const searchTrends = async (values: MarketingMonitorTrendSnapshotQuery) => {
    setTrendLoading(true)
    try {
      await loadTrends(values)
    } catch (err) {
      message.error(err instanceof Error ? err.message : '加载趋势快照失败')
    } finally {
      setTrendLoading(false)
    }
  }

  const submitTrendBuild = async (values: TrendBuildFormValues) => {
    setTrendBuilding(true)
    try {
      const payload: MarketingMonitorTrendSnapshotPayload = {
        sourceId: values.sourceId,
        bucketGrain: values.bucketGrain,
        bucketStart: values.bucketStart.trim(),
        bucketEnd: values.bucketEnd.trim(),
        brandKey: cleanText(values.brandKey),
        competitorKey: cleanText(values.competitorKey),
        metadata: parseJsonObject(values.metadataJson),
      }
      await marketingMonitoringApi.buildTrendSnapshot(payload)
      message.success('趋势快照已生成')
      await loadTrends(trendFilters)
    } catch (err) {
      message.error(err instanceof Error ? err.message : '生成趋势快照失败')
    } finally {
      setTrendBuilding(false)
    }
  }

  const submitOAuthStart = async (values: OAuthStartFormValues) => {
    setOAuthStarting(true)
    try {
      const payload: MarketingMonitorProviderOAuthAuthorizationCommand = {
        credentialKey: values.credentialKey.trim(),
        providerType: values.providerType.trim(),
        authType: values.authType.trim(),
        displayName: values.displayName.trim(),
        authorizeEndpoint: values.authorizeEndpoint.trim(),
        tokenEndpoint: values.tokenEndpoint.trim(),
        revokeEndpoint: cleanText(values.revokeEndpoint),
        redirectUri: values.redirectUri.trim(),
        clientId: values.clientId.trim(),
        clientSecret: cleanText(values.clientSecret),
        scopes: parseScopes(values.scopesText),
        authorizeParams: parseJsonObject(values.authorizeParamsJson),
        expiresInMinutes: values.expiresInMinutes,
        metadata: parseJsonObject(values.metadataJson),
      }
      const response = await marketingMonitoringApi.startProviderOAuthAuthorization(payload)
      setAuthorizationUrl(response.data.authorizationUrl)
      oauthCallbackForm.setFieldsValue({
        state: response.data.authState,
        metadataJson: '{"source":"monitoring-workbench"}',
      })
      if (response.data.authorizationUrl) {
        window.open(response.data.authorizationUrl, '_blank', 'noopener,noreferrer')
      }
      message.success('OAuth 授权已创建')
      await loadOAuthAuthorizations(oauthAuthorizationFilters)
    } catch (err) {
      message.error(err instanceof Error ? err.message : '创建 OAuth 授权失败')
    } finally {
      setOAuthStarting(false)
    }
  }

  const submitOAuthCallback = async (values: OAuthCallbackFormValues) => {
    if (!values.code?.trim() && !values.error?.trim()) {
      message.error('授权 Code 或 Provider Error 必填其一')
      return
    }
    setOAuthCompleting(true)
    try {
      await marketingMonitoringApi.completeProviderOAuthAuthorization({
        state: values.state.trim(),
        code: cleanText(values.code),
        error: cleanText(values.error),
        errorDescription: cleanText(values.errorDescription),
        metadata: parseJsonObject(values.metadataJson),
      })
      message.success('OAuth 回调已完成')
      setAuthorizationUrl(undefined)
      await Promise.all([
        loadCredentials(credentialFilters),
        loadCredentialEvents(credentialEventFilters),
        loadOAuthAuthorizations(oauthAuthorizationFilters),
      ])
    } catch (err) {
      message.error(err instanceof Error ? err.message : '完成 OAuth 回调失败')
    } finally {
      setOAuthCompleting(false)
    }
  }

  const refreshCredential = async (credential: MarketingMonitorProviderCredential) => {
    setRefreshingCredentialKey(credential.credentialKey)
    try {
      await marketingMonitoringApi.refreshProviderCredential(credential.credentialKey)
      message.success('Provider 凭据已刷新')
      await Promise.all([loadCredentials(credentialFilters), loadCredentialEvents(credentialEventFilters)])
    } catch (err) {
      message.error(err instanceof Error ? err.message : '刷新 Provider 凭据失败')
    } finally {
      setRefreshingCredentialKey(undefined)
    }
  }

  const refreshDueCredentials = async () => {
    setDueRefreshing(true)
    try {
      const response = await marketingMonitoringApi.refreshDueProviderCredentials({
        windowMinutes: 30,
        limit: 50,
      })
      message.success(`到期刷新完成：成功 ${response.data.refreshedCount}，失败 ${response.data.failedCount}`)
      await Promise.all([loadCredentials(credentialFilters), loadCredentialEvents(credentialEventFilters)])
    } catch (err) {
      message.error(err instanceof Error ? err.message : '刷新到期 Provider 凭据失败')
    } finally {
      setDueRefreshing(false)
    }
  }

  const revokeCredential = async (credential: MarketingMonitorProviderCredential) => {
    setRevokingCredentialKey(credential.credentialKey)
    try {
      await marketingMonitoringApi.revokeProviderCredential(credential.credentialKey, {
        revokeRefreshToken: true,
        tokenTypeHint: 'refresh_token',
        disableAfterRevoke: true,
        metadata: { source: 'monitoring-workbench' },
      })
      message.success('Provider 凭据已撤销')
      await Promise.all([loadCredentials(credentialFilters), loadCredentialEvents(credentialEventFilters)])
    } catch (err) {
      message.error(err instanceof Error ? err.message : '撤销 Provider 凭据失败')
    } finally {
      setRevokingCredentialKey(undefined)
    }
  }

  const disableCredential = async (credential: MarketingMonitorProviderCredential) => {
    setDisablingCredentialKey(credential.credentialKey)
    try {
      await marketingMonitoringApi.disableProviderCredential(credential.credentialKey)
      message.success('Provider 凭据已停用')
      await Promise.all([loadCredentials(credentialFilters), loadCredentialEvents(credentialEventFilters)])
    } catch (err) {
      message.error(err instanceof Error ? err.message : '停用 Provider 凭据失败')
    } finally {
      setDisablingCredentialKey(undefined)
    }
  }

  const copyAuthorizationUrl = async () => {
    if (!authorizationUrl) return
    try {
      await navigator.clipboard?.writeText(authorizationUrl)
      message.success('授权链接已复制')
    } catch {
      message.warning('授权链接复制失败')
    }
  }

  const itemColumns: ColumnsType<MarketingMonitorItem> = [
    {
      title: '内容',
      dataIndex: 'text',
      ellipsis: true,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Text strong>{row.text}</Text>
          <Text type="secondary">#{row.id} · {row.externalItemId}</Text>
        </Space>
      ),
    },
    {
      title: '情绪',
      dataIndex: 'sentimentLabel',
      width: 110,
      render: value => {
        const view = sentimentView(value)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    {
      title: '竞品',
      dataIndex: 'competitorKeys',
      width: 180,
      render: keys => keys?.length
        ? <Space size={[0, 4]} wrap>{keys.map((key: string) => <Tag key={key}>{key}</Tag>)}</Space>
        : '-',
    },
    {
      title: '来源',
      dataIndex: 'sourceType',
      width: 160,
      render: (_, row) => `${row.sourceType || '-'} / ${row.sourceId}`,
    },
    {
      title: '品牌',
      dataIndex: 'brandKey',
      width: 140,
      render: value => value || '-',
    },
    {
      title: '发布时间',
      dataIndex: 'publishedAt',
      width: 180,
      render: formatMonitorDateTime,
    },
  ]

  const alertColumns: ColumnsType<MarketingMonitorAlert> = [
    {
      title: '告警',
      dataIndex: 'title',
      ellipsis: true,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Text strong>{row.title}</Text>
          <Text type="secondary">{row.reason || '-'}</Text>
        </Space>
      ),
    },
    {
      title: '类型',
      dataIndex: 'alertType',
      width: 180,
      render: value => <Tag>{value}</Tag>,
    },
    {
      title: '级别',
      dataIndex: 'severity',
      width: 90,
      render: value => <Tag color={severityColor(value)}>{value || '-'}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: value => {
        const view = alertStatusView(value)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    {
      title: '范围',
      dataIndex: 'scopeKey',
      width: 140,
      render: value => value || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      render: formatMonitorDateTime,
    },
    {
      title: '操作',
      width: 100,
      render: (_, row) => (row.status === 'OPEN'
        ? (
            <Button
              type="link"
              aria-label="处理"
              icon={<CheckCircleOutlined />}
              loading={resolvingId === row.id}
              onClick={() => resolveAlert(row)}
            >
              处理
            </Button>
          )
        : '-'),
    },
  ]

  const trendColumns: ColumnsType<MarketingMonitorTrendSnapshot> = [
    {
      title: '窗口',
      dataIndex: 'bucketStart',
      width: 240,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Tag>{row.bucketGrain}</Tag>
          <Text type="secondary">
            {formatMonitorDateTime(row.bucketStart)} - {formatMonitorDateTime(row.bucketEnd)}
          </Text>
        </Space>
      ),
    },
    {
      title: '来源',
      dataIndex: 'sourceKey',
      width: 220,
      render: (_, row) => `${row.sourceKey || '-'} / ${row.sourceId}`,
    },
    {
      title: '范围',
      width: 220,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Text>{row.brandKey || '-'}</Text>
          <Text type="secondary">{row.competitorKey || '-'}</Text>
        </Space>
      ),
    },
    {
      title: '提及',
      dataIndex: 'mentionCount',
      width: 90,
    },
    {
      title: '情绪分布',
      width: 220,
      render: (_, row) => (
        <Space size={[0, 4]} wrap>
          <Tag color="green">正 {row.positiveCount}</Tag>
          <Tag color="blue">中 {row.neutralCount}</Tag>
          <Tag color="red">负 {row.negativeCount}</Tag>
        </Space>
      ),
    },
    {
      title: '竞品',
      dataIndex: 'competitorCount',
      width: 90,
    },
    {
      title: '告警',
      dataIndex: 'alertCount',
      width: 90,
    },
    {
      title: '平均情绪',
      dataIndex: 'avgSentimentScore',
      width: 110,
      render: value => value ?? '-',
    },
    {
      title: '生成时间',
      dataIndex: 'createdAt',
      width: 180,
      render: formatMonitorDateTime,
    },
  ]

  const credentialColumns: ColumnsType<MarketingMonitorProviderCredential> = [
    {
      title: '凭据',
      dataIndex: 'displayName',
      width: 240,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Text strong>{row.displayName}</Text>
          <Text type="secondary">{row.credentialKey}</Text>
        </Space>
      ),
    },
    {
      title: 'Provider',
      width: 220,
      render: (_, row) => (
        <Space size={[0, 4]} wrap>
          <Tag>{row.providerType}</Tag>
          <Tag>{row.authType}</Tag>
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: value => {
        const view = credentialStatusView(value)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    {
      title: 'Token',
      width: 200,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Text type="secondary">access {row.accessTokenPrefix || '-'}</Text>
          <Text type="secondary">refresh {row.refreshTokenPrefix || '-'}</Text>
        </Space>
      ),
    },
    {
      title: '刷新',
      width: 220,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Text>{formatMonitorDateTime(row.expiresAt)}</Text>
          <Text type={row.lastRefreshStatus === 'FAILED' ? 'danger' : 'secondary'}>
            {row.lastRefreshStatus || '-'} · {row.refreshAttemptCount}
          </Text>
        </Space>
      ),
    },
    {
      title: '撤销',
      width: 180,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Text>{formatMonitorDateTime(row.revokedAt)}</Text>
          <Text type={row.lastRevokeStatus === 'FAILED' ? 'danger' : 'secondary'}>
            {row.lastRevokeStatus || '-'}
          </Text>
        </Space>
      ),
    },
    {
      title: '操作',
      fixed: 'right',
      width: 260,
      render: (_, row) => (
        <Space size={4} wrap>
          <Button
            size="small"
            icon={<SyncOutlined />}
            loading={refreshingCredentialKey === row.credentialKey}
            onClick={() => refreshCredential(row)}
          >
            刷新
          </Button>
          <Popconfirm
            title="撤销 Provider 授权"
            description={row.credentialKey}
            okText="撤销"
            cancelText="取消"
            onConfirm={() => revokeCredential(row)}
          >
            <Button
              size="small"
              danger
              icon={<DisconnectOutlined />}
              loading={revokingCredentialKey === row.credentialKey}
            >
              撤销
            </Button>
          </Popconfirm>
          <Popconfirm
            title="停用本地凭据"
            description={row.credentialKey}
            okText="停用"
            cancelText="取消"
            onConfirm={() => disableCredential(row)}
          >
            <Button
              size="small"
              icon={<StopOutlined />}
              loading={disablingCredentialKey === row.credentialKey}
            >
              停用
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const oauthAuthorizationColumns: ColumnsType<MarketingMonitorProviderOAuthAuthorization> = [
    {
      title: '授权',
      dataIndex: 'credentialKey',
      width: 240,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Text strong>{row.credentialKey}</Text>
          <Text type="secondary">{row.authState}</Text>
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: value => {
        const view = oauthAuthorizationStatusView(value)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    {
      title: 'Provider',
      width: 180,
      render: (_, row) => <Tag>{row.providerType}</Tag>,
    },
    {
      title: 'HTTP',
      dataIndex: 'lastHttpStatus',
      width: 90,
      render: value => value ?? '-',
    },
    {
      title: '到期/完成',
      width: 220,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Text>{formatMonitorDateTime(row.expiresAt)}</Text>
          <Text type="secondary">{formatMonitorDateTime(row.completedAt)}</Text>
        </Space>
      ),
    },
    {
      title: '错误',
      dataIndex: 'lastErrorMessage',
      ellipsis: true,
      render: (_, row) => row.lastErrorMessage || row.providerErrorDescription || row.providerError || '-',
    },
  ]

  const credentialEventColumns: ColumnsType<MarketingMonitorProviderCredentialEvent> = [
    {
      title: '事件',
      dataIndex: 'eventType',
      width: 180,
      render: value => <Tag>{value}</Tag>,
    },
    {
      title: '凭据',
      dataIndex: 'credentialKey',
      width: 180,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: value => <Tag color={value === 'FAILED' ? 'red' : 'green'}>{value}</Tag>,
    },
    {
      title: '错误',
      dataIndex: 'errorMessage',
      ellipsis: true,
      render: value => value || '-',
    },
    {
      title: '时间',
      dataIndex: 'createdAt',
      width: 180,
      render: formatMonitorDateTime,
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
          <div>
            <Title level={2} style={{ marginBottom: 4 }}>监测工作台</Title>
            <Text type="secondary">统一处理品牌提及、竞品命中、情绪识别和告警状态。</Text>
          </div>
          <Button icon={<ReloadOutlined />} loading={loading} onClick={loadAll}>刷新</Button>
        </Space>

        {error && <Alert type="error" showIcon message={error} />}

        <Row gutter={[12, 12]}>
          <Col xs={12} md={6}>
            <Card size="small"><Statistic title="可见提及" value={kpis.visibleMentions} /></Card>
          </Col>
          <Col xs={12} md={6}>
            <Card size="small"><Statistic title="负面提及" value={kpis.negativeMentions} prefix={<WarningOutlined />} /></Card>
          </Col>
          <Col xs={12} md={6}>
            <Card size="small"><Statistic title="竞品提及" value={kpis.competitorMentions} prefix={<SoundOutlined />} /></Card>
          </Col>
          <Col xs={12} md={6}>
            <Card size="small"><Statistic title="待处理告警" value={kpis.openAlerts} /></Card>
          </Col>
        </Row>

        <Card
          title="Provider 凭据"
          size="small"
          extra={(
            <Space>
              <Button icon={<ReloadOutlined />} loading={credentialLoading} onClick={loadCredentialWorkspace}>
                刷新凭据
              </Button>
              <Button icon={<SyncOutlined />} loading={dueRefreshing} onClick={refreshDueCredentials}>
                刷新到期
              </Button>
            </Space>
          )}
        >
          <Row gutter={[12, 12]}>
            <Col xs={24} xl={14}>
              <Form
                name="monitorProviderOAuthStart"
                form={oauthStartForm}
                layout="vertical"
                initialValues={{
                  providerType: 'X_RECENT_SEARCH',
                  authType: 'OAUTH2_BEARER',
                  redirectUri: defaultOAuthRedirectUri(),
                  scopesText: 'tweet.read users.read offline.access',
                  authorizeParamsJson: '{"access_type":"offline","prompt":"consent"}',
                  expiresInMinutes: 20,
                  metadataJson: '{"owner":"brand-team"}',
                }}
                onFinish={submitOAuthStart}
              >
                <Row gutter={12}>
                  <Col xs={24} md={8}>
                    <Form.Item name="credentialKey" label="凭据 Key" rules={[{ required: true }]}>
                      <Input placeholder="x-prod" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="providerType" label="Provider" rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="authType" label="认证类型" rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={12}>
                  <Col xs={24} md={8}>
                    <Form.Item name="displayName" label="显示名称" rules={[{ required: true }]}>
                      <Input placeholder="X Production" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="clientId" label="Client ID" rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="clientSecret" label="Client Secret">
                      <Input.Password autoComplete="new-password" />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <Form.Item name="authorizeEndpoint" label="Authorize Endpoint" rules={[{ required: true }]}>
                      <Input placeholder="https://provider.example.com/oauth/authorize" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="tokenEndpoint" label="Token Endpoint" rules={[{ required: true }]}>
                      <Input placeholder="https://provider.example.com/oauth/token" />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <Form.Item name="revokeEndpoint" label="Revoke Endpoint">
                      <Input placeholder="https://provider.example.com/oauth/revoke" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="redirectUri" label="Redirect URI" rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={12}>
                  <Col xs={24} md={8}>
                    <Form.Item name="scopesText" label="Scopes">
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="expiresInMinutes" label="State 有效分钟">
                      <InputNumber min={5} max={60} style={{ width: '100%' }} />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="metadataJson" label="元数据 JSON">
                      <Input />
                    </Form.Item>
                  </Col>
                </Row>
                <Form.Item name="authorizeParamsJson" label="授权参数 JSON">
                  <Input.TextArea rows={2} />
                </Form.Item>
                <Space wrap>
                  <Button
                    type="primary"
                    htmlType="submit"
                    aria-label="创建授权"
                    icon={<KeyOutlined />}
                    loading={oauthStarting}
                  >
                    创建授权
                  </Button>
                  {authorizationUrl && (
                    <>
                      <Button icon={<LinkOutlined />} href={authorizationUrl} target="_blank" rel="noreferrer">
                        打开授权
                      </Button>
                      <Button icon={<CopyOutlined />} onClick={copyAuthorizationUrl}>
                        复制链接
                      </Button>
                    </>
                  )}
                </Space>
              </Form>
            </Col>

            <Col xs={24} xl={10}>
              <Form
                name="monitorProviderOAuthCallback"
                form={oauthCallbackForm}
                layout="vertical"
                initialValues={{
                  metadataJson: '{"source":"monitoring-workbench"}',
                }}
                onFinish={submitOAuthCallback}
              >
                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <Form.Item name="state" label="State" rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="code" label="授权 Code">
                      <Input />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <Form.Item name="error" label="Provider Error">
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="errorDescription" label="Error Description">
                      <Input />
                    </Form.Item>
                  </Col>
                </Row>
                <Form.Item name="metadataJson" label="回调元数据 JSON">
                  <Input.TextArea rows={2} />
                </Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  aria-label="完成授权"
                  icon={<CheckCircleOutlined />}
                  loading={oauthCompleting}
                >
                  完成授权
                </Button>
              </Form>
            </Col>
          </Row>

          <Form
            name="monitorCredentialFilter"
            form={credentialFilterForm}
            layout="inline"
            initialValues={credentialFilters}
            onFinish={values => loadCredentials(values)}
            style={{ marginTop: 16, marginBottom: 12 }}
          >
            <Form.Item name="providerType" label="Provider">
              <Input allowClear style={{ width: 180 }} />
            </Form.Item>
            <Form.Item name="authType" label="认证类型">
              <Input allowClear style={{ width: 160 }} />
            </Form.Item>
            <Form.Item name="status" label="状态">
              <Select allowClear style={{ width: 130 }} options={CREDENTIAL_STATUS_OPTIONS} />
            </Form.Item>
            <Form.Item name="limit" label="数量">
              <InputNumber min={1} max={100} />
            </Form.Item>
            <Button htmlType="submit" icon={<SearchOutlined />}>查询</Button>
          </Form>
          <Table
            rowKey="id"
            size="small"
            loading={loading || credentialLoading}
            columns={credentialColumns}
            dataSource={credentials}
            pagination={{ pageSize: 5 }}
            scroll={{ x: 1300 }}
          />

          <Row gutter={[12, 12]} style={{ marginTop: 12 }}>
            <Col xs={24} xl={12}>
              <Form
                name="monitorOAuthAuthorizationFilter"
                form={oauthAuthorizationFilterForm}
                layout="inline"
                initialValues={oauthAuthorizationFilters}
                onFinish={values => loadOAuthAuthorizations(values)}
                style={{ marginBottom: 12 }}
              >
                <Form.Item name="credentialKey" label="凭据">
                  <Input allowClear style={{ width: 150 }} />
                </Form.Item>
                <Form.Item name="status" label="状态">
                  <Select allowClear style={{ width: 130 }} options={OAUTH_STATUS_OPTIONS} />
                </Form.Item>
                <Form.Item name="limit" label="数量">
                  <InputNumber min={1} max={100} />
                </Form.Item>
                <Button htmlType="submit" icon={<SearchOutlined />}>查询</Button>
              </Form>
              <Table
                rowKey="id"
                size="small"
                loading={loading || credentialLoading}
                columns={oauthAuthorizationColumns}
                dataSource={oauthAuthorizations}
                pagination={{ pageSize: 5 }}
                scroll={{ x: 900 }}
              />
            </Col>
            <Col xs={24} xl={12}>
              <Form
                name="monitorCredentialEventFilter"
                form={credentialEventFilterForm}
                layout="inline"
                initialValues={credentialEventFilters}
                onFinish={values => loadCredentialEvents(values)}
                style={{ marginBottom: 12 }}
              >
                <Form.Item name="credentialKey" label="凭据">
                  <Input allowClear style={{ width: 150 }} />
                </Form.Item>
                <Form.Item name="eventType" label="事件">
                  <Input allowClear style={{ width: 150 }} />
                </Form.Item>
                <Form.Item name="limit" label="数量">
                  <InputNumber min={1} max={100} />
                </Form.Item>
                <Button htmlType="submit" icon={<SearchOutlined />}>查询</Button>
              </Form>
              <Table
                rowKey="id"
                size="small"
                loading={loading || credentialLoading}
                columns={credentialEventColumns}
                dataSource={credentialEvents}
                pagination={{ pageSize: 5 }}
                scroll={{ x: 700 }}
              />
            </Col>
          </Row>
        </Card>

        <Row gutter={[12, 12]}>
          <Col xs={24} lg={10}>
            <Card title="监测来源" size="small">
              <Form
                name="monitorSource"
                form={sourceForm}
                layout="vertical"
                initialValues={{
                  sourceType: 'MANUAL',
                  enabled: true,
                  metadataJson: '{"owner":"brand-team"}',
                }}
                onFinish={submitSource}
              >
                <Row gutter={12}>
                  <Col span={12}>
                    <Form.Item name="sourceKey" label="来源 Key" rules={[{ required: true }]}>
                      <Input placeholder="manual-social-listening" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name="sourceType" label="类型" rules={[{ required: true }]}>
                      <Select options={SOURCE_TYPE_OPTIONS} />
                    </Form.Item>
                  </Col>
                </Row>
                <Form.Item name="displayName" label="名称" rules={[{ required: true }]}>
                  <Input placeholder="Manual Social Listening" />
                </Form.Item>
                <Form.Item name="metadataJson" label="元数据 JSON">
                  <Input.TextArea rows={3} />
                </Form.Item>
                <Form.Item name="enabled" valuePropName="checked">
                  <Checkbox>启用来源</Checkbox>
                </Form.Item>
                <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={sourceSaving}>
                  保存来源
                </Button>
              </Form>
            </Card>
          </Col>

          <Col xs={24} lg={14}>
            <Card title="手动入库" size="small">
              <Form
                name="monitorIngest"
                form={ingestForm}
                layout="vertical"
                initialValues={{
                  language: 'en',
                  competitorsJson: '{"competitorx":["CompetitorX","CX"]}',
                  rawPayloadJson: '{"provider":"manual"}',
                }}
                onFinish={submitIngest}
              >
                <Row gutter={12}>
                  <Col xs={24} md={8}>
                    <Form.Item name="sourceId" label="来源 ID" rules={[{ required: true }]}>
                      <InputNumber min={1} style={{ width: '100%' }} />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="externalItemId" label="外部内容 ID" rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="brandKey" label="品牌 Key">
                      <Input />
                    </Form.Item>
                  </Col>
                </Row>
                <Form.Item name="text" label="内容文本" rules={[{ required: true }]}>
                  <Input.TextArea rows={3} />
                </Form.Item>
                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <Form.Item name="competitorsJson" label="竞品词表 JSON">
                      <Input.TextArea rows={3} />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="rawPayloadJson" label="原始载荷 JSON">
                      <Input.TextArea rows={3} />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <Form.Item name="sourceUrl" label="来源 URL">
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="publishedAt" label="发布时间">
                      <Input placeholder="2026-06-06T10:00:00" />
                    </Form.Item>
                  </Col>
                </Row>
                <Button type="primary" htmlType="submit" loading={ingesting}>入库并分析</Button>
              </Form>
            </Card>
          </Col>
        </Row>

        <Card title="趋势快照" size="small">
          <Form
            name="monitorTrendFilter"
            form={trendFilterForm}
            layout="inline"
            initialValues={trendFilters}
            onFinish={searchTrends}
            style={{ marginBottom: 12 }}
          >
            <Form.Item name="sourceId" label="来源 ID">
              <InputNumber min={1} style={{ width: 120 }} />
            </Form.Item>
            <Form.Item name="brandKey" label="品牌">
              <Input allowClear style={{ width: 150 }} placeholder="our-brand" />
            </Form.Item>
            <Form.Item name="competitorKey" label="竞品">
              <Input allowClear style={{ width: 150 }} placeholder="competitorx" />
            </Form.Item>
            <Form.Item name="limit" label="数量">
              <InputNumber min={1} max={100} />
            </Form.Item>
            <Button htmlType="submit" icon={<SearchOutlined />} loading={trendLoading}>查询</Button>
          </Form>

          <Form
            name="monitorTrendBuild"
            form={trendBuildForm}
            layout="vertical"
            initialValues={{
              bucketGrain: 'DAY',
              metadataJson: '{}',
            }}
            onFinish={submitTrendBuild}
            style={{ marginBottom: 12 }}
          >
            <Row gutter={12}>
              <Col xs={24} md={6}>
                <Form.Item name="sourceId" label="快照来源 ID" rules={[{ required: true }]}>
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col xs={24} md={6}>
                <Form.Item name="bucketGrain" label="粒度" rules={[{ required: true }]}>
                  <Select options={BUCKET_GRAIN_OPTIONS} />
                </Form.Item>
              </Col>
              <Col xs={24} md={6}>
                <Form.Item name="bucketStart" label="开始时间" rules={[{ required: true }]}>
                  <Input placeholder="2026-06-05T00:00:00" />
                </Form.Item>
              </Col>
              <Col xs={24} md={6}>
                <Form.Item name="bucketEnd" label="结束时间" rules={[{ required: true }]}>
                  <Input placeholder="2026-06-06T00:00:00" />
                </Form.Item>
              </Col>
            </Row>
            <Row gutter={12}>
              <Col xs={24} md={8}>
                <Form.Item name="brandKey" label="快照品牌 Key">
                  <Input placeholder="our-brand" />
                </Form.Item>
              </Col>
              <Col xs={24} md={8}>
                <Form.Item name="competitorKey" label="快照竞品 Key">
                  <Input placeholder="competitorx" />
                </Form.Item>
              </Col>
              <Col xs={24} md={8}>
                <Form.Item name="metadataJson" label="快照元数据 JSON">
                  <Input.TextArea rows={1} />
                </Form.Item>
              </Col>
            </Row>
            <Button
              type="primary"
              htmlType="submit"
              aria-label="生成快照"
              icon={<BarChartOutlined />}
              loading={trendBuilding}
            >
              生成快照
            </Button>
          </Form>

          <Table
            rowKey="id"
            size="small"
            loading={loading || trendLoading}
            columns={trendColumns}
            dataSource={trends}
            pagination={{ pageSize: 10 }}
          />
        </Card>

        <Card title="提及列表" size="small">
          <Form
            name="monitorItemFilter"
            form={itemFilterForm}
            layout="inline"
            initialValues={itemFilters}
            onFinish={values => loadItems(values)}
            style={{ marginBottom: 12 }}
          >
            <Form.Item name="sentimentLabel" label="情绪">
              <Select allowClear style={{ width: 130 }} options={SENTIMENT_OPTIONS} />
            </Form.Item>
            <Form.Item name="competitorKey" label="竞品">
              <Input allowClear style={{ width: 160 }} placeholder="competitorx" />
            </Form.Item>
            <Form.Item name="limit" label="数量">
              <InputNumber min={1} max={100} />
            </Form.Item>
            <Button htmlType="submit" icon={<SearchOutlined />}>查询</Button>
          </Form>
          <Table
            rowKey="id"
            size="small"
            loading={loading}
            columns={itemColumns}
            dataSource={items}
            pagination={{ pageSize: 10 }}
          />
        </Card>

        <Card title="告警处理" size="small">
          <Form
            name="monitorAlertFilter"
            form={alertFilterForm}
            layout="inline"
            initialValues={alertFilters}
            onFinish={values => loadAlerts(values)}
            style={{ marginBottom: 12 }}
          >
            <Form.Item name="status" label="状态">
              <Select allowClear style={{ width: 130 }} options={ALERT_STATUS_OPTIONS} />
            </Form.Item>
            <Form.Item name="limit" label="数量">
              <InputNumber min={1} max={100} />
            </Form.Item>
            <Button htmlType="submit" icon={<SearchOutlined />}>查询</Button>
          </Form>
          <Table
            rowKey="id"
            size="small"
            loading={loading}
            columns={alertColumns}
            dataSource={alerts}
            pagination={{ pageSize: 10 }}
          />
        </Card>
      </Space>
    </div>
  )
}

function cleanText(value?: string) {
  const text = value?.trim()
  return text || undefined
}
