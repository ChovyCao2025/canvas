import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Progress,
  Row,
  Select,
  Space,
  Spin,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  CheckCircleOutlined,
  DeleteOutlined,
  HistoryOutlined,
  LinkOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  RightOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { marketingPlatformApi } from '../../services/marketingPlatformApi'
import { marketingMonitoringApi } from '../../services/marketingMonitoringApi'
import type { MarketingMonitorAlert } from '../marketing-monitoring/monitoringWorkbench'
import {
  buildProviderWriteQueue,
  calculateControlPlaneKpis,
  calculateProviderWriteKpis,
  evidenceStatusColor,
  evidenceStatusText,
  laneStatusColor,
  priorityColor,
  providerWriteActionState,
  providerWriteApprovalColor,
  providerWriteApprovalText,
  providerWriteStatusColor,
  providerWriteStatusText,
  statusColor,
  statusText,
  type MarketingCampaign,
  type MarketingCampaignCommand,
  type MarketingCampaignLink,
  type MarketingCampaignLinkCommand,
  type MarketingCampaignReadiness,
  type MarketingIntegrationContractAuditEvent,
  type MarketingIntegrationContract,
  type MarketingIntegrationContractCommand,
  type MarketingIntegrationContractProbe,
  type MarketingIntegrationContractProbeCommand,
  type MarketingIntegrationContractSloEvaluation,
  type MarketingPlatformActionItem,
  type MarketingPlatformCapability,
  type MarketingPlatformControlPlaneSummary,
  type MarketingPlatformIntegrationAsset,
  type MarketingPlatformIntegrationLane,
  type MarketingPlatformReadinessFinding,
  type ProviderWriteGateway,
  type ProviderWriteQueueItem,
} from './marketingPlatformControlPlane'

const { Text, Title } = Typography
const integrationAlertTypes = new Set([
  'INTEGRATION_CONTRACT_PROBE_FAILURE',
  'INTEGRATION_CONTRACT_SLO_BURN_RATE',
])

interface CampaignFormValues {
  campaignKey: string
  campaignName: string
  objective?: string
  status?: string
  primaryChannel?: string
  ownerTeam?: string
  startAt?: string
  endAt?: string
  budgetAmount?: number
  currency?: string
  briefJson?: string
}

interface CampaignLinkFormValues {
  resourceType: string
  resourceId?: number
  resourceKey: string
  resourceName?: string
  resourceRoute?: string
  dependencyRole?: string
  linkStatus?: string
  requiredForLaunch?: boolean
  metadataJson?: string
}

interface IntegrationContractFormValues {
  contractKey: string
  displayName: string
  providerFamily: string
  sourceCapabilityKey: string
  targetCapabilityKey: string
  assetKey: string
  direction?: string
  environment?: string
  authMode?: string
  credentialDependency?: string
  apiRoot: string
  ownerTeam?: string
  status?: string
  slaTier?: string
  timeoutMs?: number
  retryPolicyJson?: string
  schemaContractJson?: string
  metadataJson?: string
}

interface IntegrationContractProbeFormValues {
  probeKey: string
  environment?: string
  status?: string
  httpStatusCode?: number
  latencyMs?: number
  errorType?: string
  problemTypeUri?: string
  problemTitle?: string
  problemDetail?: string
  observedAt?: string
  evidenceJson?: string
}

export default function MarketingPlatformPage() {
  const navigate = useNavigate()
  const [integrationContractForm] = Form.useForm<IntegrationContractFormValues>()
  const [integrationContractProbeForm] = Form.useForm<IntegrationContractProbeFormValues>()
  const [campaignForm] = Form.useForm<CampaignFormValues>()
  const [campaignLinkForm] = Form.useForm<CampaignLinkFormValues>()
  const [summary, setSummary] = useState<MarketingPlatformControlPlaneSummary | null>(null)
  const [integrationContracts, setIntegrationContracts] = useState<MarketingIntegrationContract[]>([])
  const [integrationContractProbes, setIntegrationContractProbes] = useState<MarketingIntegrationContractProbe[]>([])
  const [integrationSloEvaluations, setIntegrationSloEvaluations] = useState<MarketingIntegrationContractSloEvaluation[]>([])
  const [integrationProbeAlerts, setIntegrationProbeAlerts] = useState<MarketingMonitorAlert[]>([])
  const [integrationContractAuditEvents, setIntegrationContractAuditEvents] = useState<MarketingIntegrationContractAuditEvent[]>([])
  const [campaigns, setCampaigns] = useState<MarketingCampaign[]>([])
  const [campaignLinks, setCampaignLinks] = useState<Record<number, MarketingCampaignLink[]>>({})
  const [campaignReadiness, setCampaignReadiness] = useState<Record<number, MarketingCampaignReadiness>>({})
  const [providerWrites, setProviderWrites] = useState<ProviderWriteQueueItem[]>([])
  const [loading, setLoading] = useState(true)
  const [writeActionKey, setWriteActionKey] = useState<string | null>(null)
  const [readinessActionKey, setReadinessActionKey] = useState<number | null>(null)
  const [integrationContractModalOpen, setIntegrationContractModalOpen] = useState(false)
  const [integrationContractActionLoading, setIntegrationContractActionLoading] = useState(false)
  const [probeModalContract, setProbeModalContract] = useState<MarketingIntegrationContract | null>(null)
  const [probeActionLoading, setProbeActionLoading] = useState(false)
  const [probeScanLoading, setProbeScanLoading] = useState(false)
  const [auditModalContract, setAuditModalContract] = useState<MarketingIntegrationContract | null>(null)
  const [auditLoading, setAuditLoading] = useState(false)
  const [campaignModalOpen, setCampaignModalOpen] = useState(false)
  const [campaignActionLoading, setCampaignActionLoading] = useState(false)
  const [linkModalCampaign, setLinkModalCampaign] = useState<MarketingCampaign | null>(null)
  const [linkActionLoading, setLinkActionLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [
        controlPlane,
        integrationContractList,
        integrationProbeList,
        integrationSloEvaluationList,
        integrationAlertList,
        campaignList,
        search,
        creator,
        dsp,
      ] =
        await Promise.all([
        marketingPlatformApi.controlPlane(),
        marketingPlatformApi.listMarketingIntegrationContracts({ limit: 50 }),
        marketingPlatformApi.listRecentMarketingIntegrationContractProbes({ limit: 50 }),
        marketingPlatformApi.listMarketingIntegrationContractSloEvaluations({ limit: 50 }),
        marketingMonitoringApi.listAlerts({ status: 'OPEN', limit: 100 }),
        marketingPlatformApi.listMarketingCampaigns({ limit: 50 }),
        marketingPlatformApi.listSearchMarketingMutations({ limit: 50 }),
        marketingPlatformApi.listCreatorProviderMutations({ limit: 50 }),
        marketingPlatformApi.listProgrammaticDspMutations({ limit: 50 }),
      ])
      setSummary(controlPlane.data)
      setIntegrationContracts(integrationContractList.data ?? [])
      setIntegrationContractProbes(integrationProbeList.data ?? [])
      setIntegrationSloEvaluations(integrationSloEvaluationList.data ?? [])
      setIntegrationProbeAlerts((integrationAlertList.data ?? [])
        .filter(alert => integrationAlertTypes.has(alert.alertType)))
      setCampaigns(campaignList.data ?? [])
      setProviderWrites(buildProviderWriteQueue({
        search: search.data ?? [],
        creator: creator.data ?? [],
        dsp: dsp.data ?? [],
      }))
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '营销中台控制面加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const kpis = useMemo(() => summary ? calculateControlPlaneKpis(summary) : null, [summary])
  const writeKpis = useMemo(() => calculateProviderWriteKpis(providerWrites), [providerWrites])
  const integrationContractKpis = useMemo(
    () => calculateIntegrationContractKpis(integrationContracts),
    [integrationContracts],
  )
  const integrationProbeKpis = useMemo(
    () => calculateIntegrationProbeKpis(integrationContractProbes),
    [integrationContractProbes],
  )
  const campaignKpis = useMemo(() => calculateCampaignKpis(campaigns, campaignLinks), [campaigns, campaignLinks])

  const openIntegrationContractModal = useCallback(() => {
    integrationContractForm.resetFields()
    integrationContractForm.setFieldsValue({
      providerFamily: 'SEM',
      sourceCapabilityKey: 'search-marketing-governance',
      targetCapabilityKey: 'provider-credential-governance',
      assetKey: 'search-provider-write-gateway',
      direction: 'OUTBOUND',
      environment: 'PRODUCTION',
      authMode: 'OAUTH',
      credentialDependency: 'active provider credential',
      apiRoot: '/canvas/search-marketing/mutations',
      status: 'ACTIVE',
      slaTier: 'STANDARD',
      timeoutMs: 30000,
      retryPolicyJson: '{"maxAttempts":3}',
      schemaContractJson: '{}',
      metadataJson: '{}',
    })
    setIntegrationContractModalOpen(true)
  }, [integrationContractForm])

  const submitIntegrationContract = useCallback(async () => {
    setIntegrationContractActionLoading(true)
    try {
      const values = await integrationContractForm.validateFields()
      const payload: MarketingIntegrationContractCommand = {
        contractKey: values.contractKey,
        displayName: values.displayName,
        providerFamily: values.providerFamily,
        sourceCapabilityKey: values.sourceCapabilityKey,
        targetCapabilityKey: values.targetCapabilityKey,
        assetKey: values.assetKey,
        direction: values.direction,
        environment: values.environment,
        authMode: values.authMode,
        credentialDependency: values.credentialDependency,
        apiRoot: values.apiRoot,
        ownerTeam: values.ownerTeam,
        status: values.status,
        slaTier: values.slaTier,
        timeoutMs: values.timeoutMs,
        retryPolicy: parseJsonObject(values.retryPolicyJson, 'Retry policy'),
        schemaContract: parseJsonObject(values.schemaContractJson, 'Schema contract'),
        metadata: parseJsonObject(values.metadataJson, 'Integration metadata'),
      }
      await marketingPlatformApi.upsertMarketingIntegrationContract(payload)
      message.success('集成契约已保存')
      setIntegrationContractModalOpen(false)
      await load()
    } catch (caught) {
      message.error(caught instanceof Error ? caught.message : '集成契约保存失败')
    } finally {
      setIntegrationContractActionLoading(false)
    }
  }, [integrationContractForm, load])

  const archiveIntegrationContract = useCallback(async (contractId: number) => {
    try {
      await marketingPlatformApi.archiveMarketingIntegrationContract(contractId)
      message.success('集成契约已归档')
      await load()
    } catch (caught) {
      message.error(caught instanceof Error ? caught.message : '集成契约归档失败')
    }
  }, [load])

  const openIntegrationProbeModal = useCallback((contract: MarketingIntegrationContract) => {
    integrationContractProbeForm.resetFields()
    integrationContractProbeForm.setFieldsValue({
      probeKey: 'prod-readiness-probe',
      environment: contract.environment || 'PRODUCTION',
      status: 'PASS',
      httpStatusCode: 204,
      latencyMs: 180,
      evidenceJson: '{}',
    })
    setProbeModalContract(contract)
  }, [integrationContractProbeForm])

  const submitIntegrationProbe = useCallback(async () => {
    if (!probeModalContract) return
    setProbeActionLoading(true)
    try {
      const values = await integrationContractProbeForm.validateFields()
      const payload: MarketingIntegrationContractProbeCommand = {
        probeKey: values.probeKey,
        environment: values.environment,
        status: values.status,
        httpStatusCode: values.httpStatusCode,
        latencyMs: values.latencyMs,
        errorType: emptyToUndefined(values.errorType),
        problemTypeUri: emptyToUndefined(values.problemTypeUri),
        problemTitle: emptyToUndefined(values.problemTitle),
        problemDetail: emptyToUndefined(values.problemDetail),
        observedAt: emptyToUndefined(values.observedAt),
        evidence: parseJsonObject(values.evidenceJson, 'Probe evidence'),
      }
      await marketingPlatformApi.recordMarketingIntegrationContractProbe(probeModalContract.id, payload)
      message.success('集成契约探针已记录')
      setProbeModalContract(null)
      await load()
    } catch (caught) {
      message.error(caught instanceof Error ? caught.message : '集成契约探针记录失败')
    } finally {
      setProbeActionLoading(false)
    }
  }, [integrationContractProbeForm, load, probeModalContract])

  const scanIntegrationProbes = useCallback(async () => {
    setProbeScanLoading(true)
    try {
      const response = await marketingPlatformApi.scanMarketingIntegrationContractProbes({ limit: 50 })
      const result = response.data
      message.success(
        `自动探针完成：检测 ${result?.probedCount ?? 0}，PASS ${result?.passedCount ?? 0}，FAIL ${result?.failedCount ?? 0}`,
      )
      await load()
    } catch (caught) {
      message.error(caught instanceof Error ? caught.message : '自动探针扫描失败')
    } finally {
      setProbeScanLoading(false)
    }
  }, [load])

  const openIntegrationAuditModal = useCallback(async (contract: MarketingIntegrationContract) => {
    setAuditModalContract(contract)
    setAuditLoading(true)
    try {
      const response = await marketingPlatformApi.listMarketingIntegrationContractAuditEvents(contract.id, { limit: 50 })
      setIntegrationContractAuditEvents(response.data ?? [])
    } catch (caught) {
      message.error(caught instanceof Error ? caught.message : '集成契约审计加载失败')
      setIntegrationContractAuditEvents([])
    } finally {
      setAuditLoading(false)
    }
  }, [])

  const loadCampaignLinks = useCallback(async (campaignId: number) => {
    try {
      const response = await marketingPlatformApi.listMarketingCampaignLinks(campaignId)
      setCampaignLinks(previous => ({ ...previous, [campaignId]: response.data ?? [] }))
    } catch (caught) {
      message.error(caught instanceof Error ? caught.message : 'Campaign 资源链接加载失败')
    }
  }, [])

  const loadCampaignReadiness = useCallback(async (campaignId: number) => {
    setReadinessActionKey(campaignId)
    try {
      const response = await marketingPlatformApi.getMarketingCampaignReadiness(campaignId)
      setCampaignReadiness(previous => ({ ...previous, [campaignId]: response.data }))
    } catch (caught) {
      message.error(caught instanceof Error ? caught.message : 'Campaign 上线闸口评估失败')
    } finally {
      setReadinessActionKey(null)
    }
  }, [])

  const openCampaignModal = useCallback(() => {
    campaignForm.resetFields()
    campaignForm.setFieldsValue({
      objective: 'ACQUISITION',
      status: 'ACTIVE',
      primaryChannel: 'PAID_MEDIA',
      currency: 'CNY',
      briefJson: '{}',
    })
    setCampaignModalOpen(true)
  }, [campaignForm])

  const submitCampaign = useCallback(async () => {
    setCampaignActionLoading(true)
    try {
      const values = await campaignForm.validateFields()
      const payload: MarketingCampaignCommand = {
        campaignKey: values.campaignKey,
        campaignName: values.campaignName,
        objective: values.objective,
        status: values.status,
        primaryChannel: values.primaryChannel,
        ownerTeam: values.ownerTeam,
        startAt: emptyToUndefined(values.startAt),
        endAt: emptyToUndefined(values.endAt),
        budgetAmount: values.budgetAmount,
        currency: values.currency,
        brief: parseJsonObject(values.briefJson, 'Campaign brief'),
      }
      await marketingPlatformApi.upsertMarketingCampaign(payload)
      message.success('Campaign 主记录已保存')
      setCampaignModalOpen(false)
      await load()
    } catch (caught) {
      message.error(caught instanceof Error ? caught.message : 'Campaign 主记录保存失败')
    } finally {
      setCampaignActionLoading(false)
    }
  }, [campaignForm, load])

  const openLinkModal = useCallback((campaign: MarketingCampaign) => {
    campaignLinkForm.resetFields()
    campaignLinkForm.setFieldsValue({
      resourceType: 'JOURNEY',
      dependencyRole: 'PRIMARY',
      linkStatus: 'ACTIVE',
      requiredForLaunch: true,
      metadataJson: '{}',
    })
    setLinkModalCampaign(campaign)
  }, [campaignLinkForm])

  const submitCampaignLink = useCallback(async () => {
    if (!linkModalCampaign) return
    setLinkActionLoading(true)
    try {
      const values = await campaignLinkForm.validateFields()
      const payload: MarketingCampaignLinkCommand = {
        campaignId: linkModalCampaign.id,
        resourceType: values.resourceType,
        resourceId: values.resourceId,
        resourceKey: values.resourceKey,
        resourceName: values.resourceName,
        resourceRoute: values.resourceRoute,
        dependencyRole: values.dependencyRole,
        linkStatus: values.linkStatus,
        requiredForLaunch: Boolean(values.requiredForLaunch),
        metadata: parseJsonObject(values.metadataJson, 'Resource metadata'),
      }
      await marketingPlatformApi.linkMarketingCampaignResource(payload)
      message.success('Campaign 资源链接已保存')
      setLinkModalCampaign(null)
      await loadCampaignLinks(linkModalCampaign.id)
      await load()
    } catch (caught) {
      message.error(caught instanceof Error ? caught.message : 'Campaign 资源链接保存失败')
    } finally {
      setLinkActionLoading(false)
    }
  }, [campaignLinkForm, linkModalCampaign, load, loadCampaignLinks])

  const deleteCampaignLink = useCallback(async (campaignId: number, linkId: number) => {
    try {
      await marketingPlatformApi.unlinkMarketingCampaignResource(linkId)
      message.success('Campaign 资源链接已移除')
      await loadCampaignLinks(campaignId)
      await load()
    } catch (caught) {
      message.error(caught instanceof Error ? caught.message : 'Campaign 资源链接移除失败')
    }
  }, [load, loadCampaignLinks])

  const runProviderWriteAction = useCallback(async (
    item: ProviderWriteQueueItem,
    action: 'approve' | 'dry-run' | 'apply',
  ) => {
    const key = `${item.gateway}-${item.id}-${action}`
    setWriteActionKey(key)
    try {
      if (action === 'approve') {
        await approveProviderWrite(item.gateway, item.id)
        message.success('已审批 provider 写入')
      } else {
        await executeProviderWrite(item.gateway, item.id, action === 'dry-run')
        message.success(action === 'dry-run' ? 'Dry-run 已完成' : 'Provider 写入已提交')
      }
      await load()
    } catch (caught) {
      message.error(caught instanceof Error ? caught.message : 'Provider 写入操作失败')
    } finally {
      setWriteActionKey(null)
    }
  }, [load])

  return (
    <div style={{ padding: 24, maxWidth: 1440, margin: '0 auto' }}>
      <Space direction="vertical" size={20} style={{ width: '100%' }}>
        <Row gutter={[16, 12]} align="middle">
          <Col flex="auto">
            <Title level={2} style={{ margin: 0 }}>营销中台</Title>
            <Text type="secondary">能力地图、集成胶水层、生产就绪和配置队列</Text>
          </Col>
          <Col>
            <Button icon={<ReloadOutlined />} onClick={load} loading={loading}>刷新</Button>
          </Col>
        </Row>

        {error && (
          <Alert
            type="error"
            showIcon
            message="控制面加载失败"
            description={error}
            action={<Button size="small" onClick={load}>重试</Button>}
          />
        )}

        <Spin spinning={loading && !summary}>
          {summary && kpis ? (
            <Space direction="vertical" size={20} style={{ width: '100%' }}>
              <Row gutter={[16, 16]}>
                <Col xs={24} md={6}>
                  <Card size="small" title="生产就绪">
                    <Space align="center" size={16}>
                      <Progress type="circle" percent={kpis.readinessPercent} size={86} />
                      <Space direction="vertical" size={4}>
                        <Tag color={statusColor(summary.overallStatus)}>
                          {statusText(summary.overallStatus)}
                        </Tag>
                        <Text strong>{kpis.liveCapabilities} / {kpis.capabilityCount} 能力已上线</Text>
                        <Text type="secondary">租户 {summary.tenantId} · {summary.generatedAt}</Text>
                      </Space>
                    </Space>
                  </Card>
                </Col>
                <Col xs={24} md={6}>
                  <Card size="small" title="上线闸口">
                    <Space direction="vertical" size={6}>
                      <Tag color={statusColor(kpis.readinessGateStatus)}>
                        {statusText(kpis.readinessGateStatus)}
                      </Tag>
                      <Text strong>阻断 {kpis.blockerCount} · 警告 {kpis.warningCount}</Text>
                      <Text type="secondary">
                        {summary.readinessGate.productionReady ? '机器闸口允许生产上线' : '必须清理阻断项后上线'}
                      </Text>
                    </Space>
                  </Card>
                </Col>
                <Col xs={24} md={6}>
                  <Card size="small" title="配置队列">
                    <Space direction="vertical" size={6}>
                      <Text strong>{kpis.actionCount} 项待处理</Text>
                      <Text type="secondary">{kpis.configurationRequired} 个能力依赖外部提供方配置或运营工作台补齐</Text>
                    </Space>
                  </Card>
                </Col>
                <Col xs={24} md={6}>
                  <Card size="small" title="集成胶水层">
                    <Space direction="vertical" size={6}>
                      <Text strong>{summary.integrationLanes.length} 条集成链路</Text>
                      <Text type="secondary">{summary.integrationAssets.length} 个集成资产归入统一治理</Text>
                    </Space>
                  </Card>
                </Col>
              </Row>

              {(summary.readinessGate.blockers.length > 0 || summary.readinessGate.warnings.length > 0) && (
                <Row gutter={[16, 16]}>
                  <Col xs={24} lg={12}>
                    <ReadinessFindingPanel
                      title="上线阻断"
                      emptyText="暂无阻断项"
                      items={summary.readinessGate.blockers}
                      onOpen={route => navigate(route)}
                    />
                  </Col>
                  <Col xs={24} lg={12}>
                    <ReadinessFindingPanel
                      title="上线警告"
                      emptyText="暂无警告项"
                      items={summary.readinessGate.warnings}
                      onOpen={route => navigate(route)}
                    />
                  </Col>
                </Row>
              )}

              <Card
                size="small"
                title="集成契约注册表"
                extra={(
                  <Space size={8} wrap>
                    <Tag>总数 {integrationContractKpis.total}</Tag>
                    <Tag color="green">生产 ACTIVE {integrationContractKpis.productionActive}</Tag>
                    <Tag color={integrationContractKpis.blocked > 0 ? 'red' : 'default'}>
                      BLOCKED {integrationContractKpis.blocked}
                    </Tag>
                    <Tag color={integrationContractKpis.degraded > 0 ? 'gold' : 'default'}>
                      DEGRADED {integrationContractKpis.degraded}
                    </Tag>
                    <Tag color="green">健康 PASS {integrationProbeKpis.pass}</Tag>
                    <Tag color={integrationProbeKpis.fail > 0 ? 'red' : 'default'}>
                      FAIL {integrationProbeKpis.fail}
                    </Tag>
                    <Tag color={integrationProbeAlerts.length > 0 ? 'red' : 'default'}>
                      OPEN 告警 {integrationProbeAlerts.length}
                    </Tag>
                    <Button
                      size="small"
                      icon={<ThunderboltOutlined />}
                      loading={probeScanLoading}
                      onClick={() => void scanIntegrationProbes()}
                    >
                      运行自动探针
                    </Button>
                    <Button
                      size="small"
                      type="primary"
                      icon={<PlusOutlined />}
                      onClick={openIntegrationContractModal}
                    >
                      新建契约
                    </Button>
                  </Space>
                )}
              >
                <Table<MarketingIntegrationContract>
                  rowKey={row => String(row.id ?? row.contractKey)}
                  dataSource={integrationContracts}
                  pagination={{ pageSize: 6, showSizeChanger: false }}
                  scroll={{ x: 1280 }}
                  columns={[
                    {
                      title: '契约',
                      dataIndex: 'displayName',
                      render: (_, row) => (
                        <Space direction="vertical" size={0}>
                          <Text strong>{row.displayName}</Text>
                          <Text type="secondary">{row.contractKey}</Text>
                        </Space>
                      ),
                    },
                    {
                      title: 'Provider',
                      dataIndex: 'providerFamily',
                      width: 140,
                      render: value => <Tag>{String(value)}</Tag>,
                    },
                    {
                      title: '方向 / 环境',
                      key: 'direction',
                      width: 170,
                      render: (_, row) => (
                        <Space direction="vertical" size={2}>
                          <Tag color={row.environment === 'PRODUCTION' ? 'green' : 'blue'}>{row.environment}</Tag>
                          <Text type="secondary">{row.direction}</Text>
                        </Space>
                      ),
                    },
                    {
                      title: '状态',
                      dataIndex: 'status',
                      width: 130,
                      render: status => <Tag color={statusColor(String(status))}>{statusText(String(status))}</Tag>,
                    },
                    {
                      title: '能力链路',
                      key: 'capabilities',
                      render: (_, row) => (
                        <Space direction="vertical" size={0}>
                          <Text>{row.sourceCapabilityKey}{' -> '}{row.targetCapabilityKey}</Text>
                          <Text type="secondary">{row.assetKey}</Text>
                        </Space>
                      ),
                    },
                    {
                      title: '认证 / SLA',
                      key: 'auth',
                      width: 240,
                      render: (_, row) => (
                        <Space direction="vertical" size={0}>
                          <Text>{row.authMode} · {row.slaTier} · {row.timeoutMs}ms</Text>
                          <Text type="secondary">{row.credentialDependency ?? '-'}</Text>
                        </Space>
                      ),
                    },
                    {
                      title: 'API Root',
                      dataIndex: 'apiRoot',
                      width: 260,
                      render: value => <Text type="secondary">{String(value)}</Text>,
                    },
                    {
                      title: '操作',
                      key: 'actions',
                      fixed: 'right',
                      width: 210,
                      render: (_, row) => (
                        <Space size={6} wrap>
                          <Button
                            size="small"
                            icon={<PlayCircleOutlined />}
                            disabled={row.status === 'ARCHIVED'}
                            onClick={() => openIntegrationProbeModal(row)}
                          >
                            记录探针
                          </Button>
                          <Button
                            size="small"
                            icon={<HistoryOutlined />}
                            onClick={() => void openIntegrationAuditModal(row)}
                          >
                            审计
                          </Button>
                          <Popconfirm
                            title="归档集成契约"
                            okText="归档"
                            cancelText="取消"
                            onConfirm={() => void archiveIntegrationContract(row.id)}
                          >
                            <Button
                              size="small"
                              danger
                              icon={<DeleteOutlined />}
                              disabled={row.status === 'ARCHIVED'}
                            />
                          </Popconfirm>
                        </Space>
                      ),
                    },
                  ]}
                />
                <div style={{ marginTop: 16 }}>
                  <Space direction="vertical" size={8} style={{ width: '100%' }}>
                    <Text strong>最近探针证据</Text>
                    <Table<MarketingIntegrationContractProbe>
                      rowKey={row => String(row.id ?? `${row.contractId}-${row.probeKey}-${row.observedAt}`)}
                      dataSource={integrationContractProbes}
                      pagination={{ pageSize: 5, showSizeChanger: false }}
                      scroll={{ x: 980 }}
                      columns={[
                        {
                          title: '契约',
                          dataIndex: 'contractKey',
                          render: value => <Text>{String(value)}</Text>,
                        },
                        {
                          title: '探针',
                          dataIndex: 'probeKey',
                          render: (_, row) => (
                            <Space direction="vertical" size={0}>
                              <Text strong>{row.probeKey}</Text>
                              <Text type="secondary">{row.environment}</Text>
                            </Space>
                          ),
                        },
                        {
                          title: '状态',
                          dataIndex: 'status',
                          width: 120,
                          render: status => <Tag color={probeStatusColor(String(status))}>{String(status)}</Tag>,
                        },
                        {
                          title: 'HTTP / Latency',
                          key: 'runtime',
                          width: 160,
                          render: (_, row) => <Text>{formatHttpLatency(row)}</Text>,
                        },
                        {
                          title: '问题',
                          key: 'problem',
                          render: (_, row) => row.problemTitle || row.errorType
                            ? (
                              <Space direction="vertical" size={0}>
                                <Text type={row.status === 'FAIL' ? 'danger' : undefined}>
                                  {row.problemTitle ?? row.errorType}
                                </Text>
                                <Text type="secondary">{row.problemDetail ?? row.problemTypeUri ?? '-'}</Text>
                              </Space>
                            )
                            : <Text type="secondary">无</Text>,
                        },
                        {
                          title: '观测时间',
                          dataIndex: 'observedAt',
                          width: 190,
                          render: value => <Text type="secondary">{formatDateTime(String(value ?? ''))}</Text>,
                        },
                      ]}
                    />
                  </Space>
                </div>
                <div style={{ marginTop: 16 }}>
                  <Space direction="vertical" size={8} style={{ width: '100%' }}>
                    <Text strong>集成探针告警</Text>
                    <Table<MarketingMonitorAlert>
                      rowKey={row => String(row.id)}
                      dataSource={integrationProbeAlerts}
                      pagination={{ pageSize: 5, showSizeChanger: false }}
                      scroll={{ x: 980 }}
                      locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无 OPEN 集成探针告警" /> }}
                      columns={[
                        {
                          title: '告警',
                          dataIndex: 'title',
                          render: (_, row) => (
                            <Space direction="vertical" size={0}>
                              <Text strong>{row.title}</Text>
                              <Text type="secondary">{row.reason ?? '-'}</Text>
                            </Space>
                          ),
                        },
                        {
                          title: '契约',
                          dataIndex: 'scopeKey',
                          width: 260,
                          render: (_, row) => (
                            <Space direction="vertical" size={0}>
                              <Text>{row.scopeKey ?? '-'}</Text>
                              <Text type="secondary">{metadataText(row.metadata, 'providerFamily')}</Text>
                            </Space>
                          ),
                        },
                        {
                          title: '严重度',
                          dataIndex: 'severity',
                          width: 110,
                          render: severity => (
                            <Tag color={alertSeverityColor(String(severity))}>{String(severity)}</Tag>
                          ),
                        },
                        {
                          title: '次数',
                          dataIndex: 'itemCount',
                          width: 90,
                          render: value => <Text>{Number(value ?? 0)}</Text>,
                        },
                        {
                          title: 'HTTP / Latency',
                          key: 'runtime',
                          width: 170,
                          render: (_, row) => (
                            <Text>{formatAlertRuntime(row.metadata)}</Text>
                          ),
                        },
                        {
                          title: '窗口',
                          key: 'window',
                          width: 270,
                          render: (_, row) => (
                            <Text type="secondary">
                              {formatDateTime(row.windowStart)}{' -> '}{formatDateTime(row.windowEnd)}
                            </Text>
                          ),
                        },
                      ]}
                    />
                  </Space>
                </div>
                <div style={{ marginTop: 16 }}>
                  <Space direction="vertical" size={8} style={{ width: '100%' }}>
                    <Text strong>SLO Burn-rate</Text>
                    <Table<MarketingIntegrationContractSloEvaluation>
                      rowKey={row => `${row.contractId}-${row.probeKey}-${row.generatedAt}`}
                      dataSource={integrationSloEvaluations}
                      pagination={{ pageSize: 5, showSizeChanger: false }}
                      scroll={{ x: 980 }}
                      locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无集成 SLO 评估" /> }}
                      columns={[
                        {
                          title: '契约',
                          dataIndex: 'contractKey',
                          render: (_, row) => (
                            <Space direction="vertical" size={0}>
                              <Text>{row.contractKey}</Text>
                              <Text type="secondary">{row.probeKey}</Text>
                            </Space>
                          ),
                        },
                        {
                          title: '状态',
                          dataIndex: 'status',
                          width: 150,
                          render: (_, row) => (
                            <Space size={6} wrap>
                              <Tag color={alertSeverityColor(row.severity)}>{row.severity}</Tag>
                              <Text>{row.status}</Text>
                            </Space>
                          ),
                        },
                        {
                          title: 'SLO Burn-rate',
                          key: 'burnRate',
                          width: 190,
                          render: (_, row) => (
                            <Space direction="vertical" size={0}>
                              <Text>{row.triggeredRuleKey ?? '-'}</Text>
                              <Text type="secondary">{formatSloBurnRate(row)}</Text>
                            </Space>
                          ),
                        },
                        {
                          title: '原因',
                          dataIndex: 'reason',
                          render: reason => <Text>{String(reason ?? '-')}</Text>,
                        },
                        {
                          title: '生成时间',
                          dataIndex: 'generatedAt',
                          width: 190,
                          render: value => <Text type="secondary">{formatDateTime(String(value ?? ''))}</Text>,
                        },
                      ]}
                    />
                  </Space>
                </div>
              </Card>

              <Card
                size="small"
                title="Campaign 主账本"
                extra={(
                  <Space size={8} wrap>
                    <Tag>总数 {campaignKpis.total}</Tag>
                    <Tag color="green">活跃 {campaignKpis.active}</Tag>
                    <Tag color={campaignKpis.requiredLinks > 0 ? 'green' : 'gold'}>
                      必需资源 {campaignKpis.requiredLinks}
                    </Tag>
                    <Tag color={campaignKpis.blockedLinks > 0 ? 'red' : 'default'}>
                      阻断资源 {campaignKpis.blockedLinks}
                    </Tag>
                    <Button size="small" type="primary" icon={<PlusOutlined />} onClick={openCampaignModal}>
                      新建 Campaign
                    </Button>
                  </Space>
                )}
              >
                <Table<MarketingCampaign>
                  rowKey={row => String(row.id ?? row.campaignKey)}
                  dataSource={campaigns}
                  pagination={{ pageSize: 6, showSizeChanger: false }}
                  scroll={{ x: 1180 }}
                  expandable={{
                    expandedRowRender: campaign => (
                      <CampaignResourceLinks
                        campaign={campaign}
                        links={campaignLinks[campaign.id]}
                        readiness={campaignReadiness[campaign.id]}
                        readinessLoading={readinessActionKey === campaign.id}
                        onLoad={() => void loadCampaignLinks(campaign.id)}
                        onEvaluate={() => void loadCampaignReadiness(campaign.id)}
                        onCreate={() => openLinkModal(campaign)}
                        onDelete={(linkId) => void deleteCampaignLink(campaign.id, linkId)}
                      />
                    ),
                    onExpand: (expanded, campaign) => {
                      if (expanded && campaign.id && !campaignLinks[campaign.id]) {
                        void loadCampaignLinks(campaign.id)
                      }
                      if (expanded && campaign.id && !campaignReadiness[campaign.id]) {
                        void loadCampaignReadiness(campaign.id)
                      }
                    },
                  }}
                  columns={[
                    {
                      title: 'Campaign',
                      dataIndex: 'campaignName',
                      render: (_, row) => (
                        <Space direction="vertical" size={0}>
                          <Text strong>{row.campaignName}</Text>
                          <Text type="secondary">{row.campaignKey}</Text>
                        </Space>
                      ),
                    },
                    {
                      title: '状态',
                      dataIndex: 'status',
                      width: 120,
                      render: status => <Tag color={statusColor(String(status))}>{statusText(String(status))}</Tag>,
                    },
                    {
                      title: '目标',
                      dataIndex: 'objective',
                      width: 150,
                      render: value => <Text>{String(value ?? '-')}</Text>,
                    },
                    {
                      title: '渠道 / 团队',
                      key: 'owner',
                      width: 220,
                      render: (_, row) => (
                        <Space direction="vertical" size={0}>
                          <Text>{row.primaryChannel ?? '-'}</Text>
                          <Text type="secondary">{row.ownerTeam ?? '-'}</Text>
                        </Space>
                      ),
                    },
                    {
                      title: '预算',
                      key: 'budget',
                      width: 140,
                      render: (_, row) => <Text>{row.currency} {formatAmount(row.budgetAmount)}</Text>,
                    },
                    {
                      title: '上线闸口',
                      key: 'readiness',
                      width: 180,
                      render: (_, row) => {
                        const readiness = campaignReadiness[row.id]
                        return (
                          <Space direction="vertical" size={4}>
                            <Tag color={statusColor(readiness?.status ?? 'CONFIGURATION_REQUIRED')}>
                              {readiness ? statusText(readiness.status) : '未评估'}
                            </Tag>
                            {readiness && (
                              <Text type="secondary">阻断 {readiness.blockerCount} · 警告 {readiness.warningCount}</Text>
                            )}
                          </Space>
                        )
                      },
                    },
                    {
                      title: '周期',
                      key: 'window',
                      width: 260,
                      render: (_, row) => (
                        <Text type="secondary">
                          {formatDateTime(row.startAt)}{' -> '}{formatDateTime(row.endAt)}
                        </Text>
                      ),
                    },
                    {
                      title: '操作',
                      key: 'actions',
                      fixed: 'right',
                      width: 210,
                      render: (_, row) => (
                        <Space size={6} wrap>
                          <Button size="small" icon={<LinkOutlined />} onClick={() => openLinkModal(row)}>
                            绑定资源
                          </Button>
                          <Button
                            size="small"
                            loading={readinessActionKey === row.id}
                            onClick={() => void loadCampaignReadiness(row.id)}
                          >
                            评估闸口
                          </Button>
                          <Button size="small" onClick={() => void loadCampaignLinks(row.id)}>
                            刷新资源
                          </Button>
                        </Space>
                      ),
                    },
                  ]}
                />
              </Card>

              <Card
                size="small"
                title="Provider 写入操作"
                extra={(
                  <Space size={8} wrap>
                    <Tag>总数 {writeKpis.total}</Tag>
                    <Tag color="gold">待审批 {writeKpis.pendingApproval}</Tag>
                    <Tag color="blue">待执行 {writeKpis.ready}</Tag>
                    <Tag color="green">Dry-run 通过 {writeKpis.dryRunOk}</Tag>
                    <Tag color="red">失败 {writeKpis.failed}</Tag>
                  </Space>
                )}
              >
                <Table<ProviderWriteQueueItem>
                  rowKey={row => `${row.gateway}-${row.id}`}
                  dataSource={providerWrites}
                  pagination={{ pageSize: 8, showSizeChanger: false }}
                  scroll={{ x: 1180 }}
                  columns={[
                    {
                      title: '网关',
                      dataIndex: 'gatewayLabel',
                      width: 110,
                      render: (_, row) => (
                        <Space direction="vertical" size={2}>
                          <Tag color={row.gateway === 'PROGRAMMATIC_DSP' ? 'purple' : 'blue'}>{row.gatewayLabel}</Tag>
                          <Text type="secondary">{row.provider}</Text>
                        </Space>
                      ),
                    },
                    {
                      title: 'Mutation',
                      dataIndex: 'mutationType',
                      render: (_, row) => (
                        <Space direction="vertical" size={0}>
                          <Text strong>{row.mutationType}</Text>
                          <Text type="secondary">{row.mutationKey}</Text>
                        </Space>
                      ),
                    },
                    {
                      title: '对象',
                      dataIndex: 'entityType',
                      width: 220,
                      render: (_, row) => (
                        <Space direction="vertical" size={0}>
                          <Text>{row.entityType}</Text>
                          <Text type="secondary">{row.scopeLabel}</Text>
                        </Space>
                      ),
                    },
                    {
                      title: '状态',
                      dataIndex: 'status',
                      width: 190,
                      render: (_, row) => (
                        <Space direction="vertical" size={4}>
                          <Tag color={providerWriteApprovalColor(row.approvalStatus)}>
                            {providerWriteApprovalText(row.approvalStatus)}
                          </Tag>
                          <Tag color={providerWriteStatusColor(row.status)}>
                            {providerWriteStatusText(row.status)}
                          </Tag>
                        </Space>
                      ),
                    },
                    {
                      title: '错误',
                      dataIndex: 'errorCode',
                      width: 220,
                      render: (_, row) => row.errorCode
                        ? (
                          <Space direction="vertical" size={0}>
                            <Text type="danger">{row.errorCode}</Text>
                            <Text type="secondary">{row.errorMessage}</Text>
                          </Space>
                        )
                        : <Text type="secondary">无</Text>,
                    },
                    {
                      title: '更新时间',
                      dataIndex: 'updatedAt',
                      width: 180,
                      render: value => <Text type="secondary">{String(value ?? '-')}</Text>,
                    },
                    {
                      title: '操作',
                      key: 'actions',
                      fixed: 'right',
                      width: 260,
                      render: (_, row) => {
                        const state = providerWriteActionState(row)
                        return (
                          <Space size={6} wrap>
                            <Button
                              size="small"
                              icon={<CheckCircleOutlined />}
                              disabled={!state.canApprove}
                              loading={writeActionKey === `${row.gateway}-${row.id}-approve`}
                              onClick={() => void runProviderWriteAction(row, 'approve')}
                            >
                              审批
                            </Button>
                            <Button
                              size="small"
                              icon={<PlayCircleOutlined />}
                              disabled={!state.canDryRun}
                              loading={writeActionKey === `${row.gateway}-${row.id}-dry-run`}
                              onClick={() => void runProviderWriteAction(row, 'dry-run')}
                            >
                              Dry-run
                            </Button>
                            <Popconfirm
                              title="执行 live apply"
                              okText="执行"
                              cancelText="取消"
                              disabled={!state.canApply}
                              onConfirm={() => void runProviderWriteAction(row, 'apply')}
                            >
                              <Button
                                size="small"
                                type="primary"
                                icon={<ThunderboltOutlined />}
                                disabled={!state.canApply}
                                loading={writeActionKey === `${row.gateway}-${row.id}-apply`}
                              >
                                Apply
                              </Button>
                            </Popconfirm>
                          </Space>
                        )
                      },
                    },
                  ]}
                />
              </Card>

              <Card size="small" title="集成资产目录">
                <Table<MarketingPlatformIntegrationAsset>
                  rowKey="assetKey"
                  dataSource={summary.integrationAssets}
                  pagination={false}
                  scroll={{ x: 1180 }}
                  columns={[
                    {
                      title: '资产',
                      dataIndex: 'displayName',
                      render: (_, row) => (
                        <Space direction="vertical" size={0}>
                          <Text strong>{row.displayName}</Text>
                          <Text type="secondary">{row.assetType} · {row.providerFamily}</Text>
                        </Space>
                      ),
                    },
                    {
                      title: '状态',
                      dataIndex: 'status',
                      width: 120,
                      render: status => <Tag color={laneStatusColor(String(status))}>{statusText(String(status))}</Tag>,
                    },
                    {
                      title: '归属能力',
                      dataIndex: 'ownerCapabilityKey',
                      width: 220,
                      render: value => <Text>{String(value)}</Text>,
                    },
                    {
                      title: '写入队列',
                      key: 'writes',
                      width: 160,
                      render: (_, row) => (
                        <Space size={4} wrap>
                          <Tag color={row.pendingWrites > 0 ? 'gold' : 'default'}>待审批 {row.pendingWrites}</Tag>
                          <Tag color={row.failedWrites > 0 ? 'red' : 'default'}>失败 {row.failedWrites}</Tag>
                        </Space>
                      ),
                    },
                    {
                      title: '凭据依赖',
                      dataIndex: 'credentialDependency',
                      width: 180,
                      render: value => <Text type="secondary">{String(value)}</Text>,
                    },
                    {
                      title: '控制点',
                      dataIndex: 'controls',
                      render: controls => (
                        <Space size={[4, 4]} wrap>
                          {(controls as string[]).map(control => <Tag key={control}>{control}</Tag>)}
                        </Space>
                      ),
                    },
                    {
                      title: '运行证据',
                      dataIndex: 'evidence',
                      width: 260,
                      render: evidence => renderEvidence(evidence as MarketingPlatformIntegrationAsset['evidence']),
                    },
                    {
                      title: '缺口',
                      dataIndex: 'gaps',
                      width: 260,
                      render: gaps => {
                        const rows = gaps as string[]
                        return rows.length === 0
                          ? <Text type="secondary">无</Text>
                          : <Space direction="vertical" size={0}>{rows.map(gap => <Text key={gap}>{gap}</Text>)}</Space>
                      },
                    },
                  ]}
                />
              </Card>

              <Card size="small" title="能力总览">
                <Table<MarketingPlatformCapability>
                  rowKey="capabilityKey"
                  dataSource={summary.capabilities}
                  pagination={false}
                  scroll={{ x: 1160 }}
                  columns={[
                    {
                      title: '能力',
                      dataIndex: 'displayName',
                      render: (_, row) => (
                        <Space direction="vertical" size={0}>
                          <Text strong>{row.displayName}</Text>
                          <Text type="secondary">{row.domain} · {row.surface}</Text>
                        </Space>
                      ),
                    },
                    {
                      title: '状态',
                      dataIndex: 'status',
                      width: 120,
                      render: status => <Tag color={statusColor(String(status))}>{statusText(String(status))}</Tag>,
                    },
                    {
                      title: '入口',
                      dataIndex: 'route',
                      width: 160,
                      render: route => (
                        <Button type="link" size="small" onClick={() => navigate(String(route))}>
                          打开 <RightOutlined />
                        </Button>
                      ),
                    },
                    {
                      title: '生产信号',
                      dataIndex: 'productionSignals',
                      render: signals => (
                        <Space size={[4, 4]} wrap>
                          {(signals as string[]).map(signal => <Tag key={signal}>{signal}</Tag>)}
                        </Space>
                      ),
                    },
                    {
                      title: '运行证据',
                      dataIndex: 'evidence',
                      width: 260,
                      render: evidence => renderEvidence(evidence as MarketingPlatformCapability['evidence']),
                    },
                    {
                      title: '缺口',
                      dataIndex: 'gaps',
                      render: gaps => {
                        const rows = gaps as string[]
                        return rows.length === 0
                          ? <Text type="secondary">无</Text>
                          : <Space direction="vertical" size={0}>{rows.map(gap => <Text key={gap}>{gap}</Text>)}</Space>
                      },
                    },
                  ]}
                />
              </Card>

              <Row gutter={[16, 16]}>
                <Col xs={24} lg={14}>
                  <Card size="small" title="集成链路">
                    <Table<MarketingPlatformIntegrationLane>
                      rowKey="laneKey"
                      dataSource={summary.integrationLanes}
                      pagination={false}
                      scroll={{ x: 820 }}
                      columns={[
                        {
                          title: '链路',
                          dataIndex: 'displayName',
                          render: (_, row) => (
                            <Space direction="vertical" size={0}>
                              <Text strong>{row.displayName}</Text>
                              <Text type="secondary">{row.sourceCapabilityKey}{' -> '}{row.targetCapabilityKey}</Text>
                            </Space>
                          ),
                        },
                        {
                          title: '状态',
                          dataIndex: 'status',
                          width: 120,
                          render: status => <Tag color={laneStatusColor(String(status))}>{statusText(String(status))}</Tag>,
                        },
                        {
                          title: '控制点',
                          dataIndex: 'controls',
                          render: controls => (
                            <Space size={[4, 4]} wrap>
                              {(controls as string[]).map(control => <Tag key={control}>{control}</Tag>)}
                            </Space>
                          ),
                        },
                      ]}
                    />
                  </Card>
                </Col>
                <Col xs={24} lg={10}>
                  <Card size="small" title="行动队列">
                    {summary.actionItems.length === 0 ? (
                      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无待处理项" />
                    ) : (
                      <Space direction="vertical" size={10} style={{ width: '100%' }}>
                        {summary.actionItems.map(item => (
                          <ActionRow key={`${item.capabilityKey}-${item.title}`} item={item} onOpen={() => navigate(item.route)} />
                        ))}
                      </Space>
                    )}
                  </Card>
                </Col>
              </Row>
            </Space>
          ) : (
            !loading && <Empty description="暂无营销中台控制面数据" />
          )}
        </Spin>
      </Space>
      <Modal
        title="新建或更新集成契约"
        open={integrationContractModalOpen}
        width={840}
        confirmLoading={integrationContractActionLoading}
        onOk={() => void submitIntegrationContract()}
        onCancel={() => setIntegrationContractModalOpen(false)}
      >
        <Form<IntegrationContractFormValues> form={integrationContractForm} layout="vertical">
          <Row gutter={12}>
            <Col xs={24} md={12}>
              <Form.Item name="contractKey" label="Contract Key" rules={[{ required: true, message: '请输入 Contract Key' }]}>
                <Input placeholder="google-ads-keyword-write" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="displayName" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="providerFamily" label="Provider" rules={[{ required: true, message: '请选择 Provider' }]}>
                <Select options={providerFamilyOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="direction" label="方向">
                <Select options={integrationDirectionOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="environment" label="环境">
                <Select options={integrationEnvironmentOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="authMode" label="认证模式">
                <Select options={integrationAuthModeOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="status" label="状态">
                <Select options={integrationContractStatusOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="slaTier" label="SLA">
                <Select options={slaTierOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="timeoutMs" label="超时 ms">
                <InputNumber style={{ width: '100%' }} min={1000} max={300000} step={1000} precision={0} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="ownerTeam" label="负责团队">
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="credentialDependency" label="凭据依赖">
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                name="sourceCapabilityKey"
                label="Source Capability"
                rules={[{ required: true, message: '请输入 Source Capability' }]}
              >
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                name="targetCapabilityKey"
                label="Target Capability"
                rules={[{ required: true, message: '请输入 Target Capability' }]}
              >
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="assetKey" label="Asset Key" rules={[{ required: true, message: '请输入 Asset Key' }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="apiRoot" label="API Root" rules={[{ required: true, message: '请输入 API Root' }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="retryPolicyJson" label="Retry Policy JSON">
                <Input.TextArea rows={3} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="schemaContractJson" label="Schema Contract JSON">
                <Input.TextArea rows={3} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="metadataJson" label="Metadata JSON">
                <Input.TextArea rows={3} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
      <Modal
        title={probeModalContract ? `记录探针 - ${probeModalContract.displayName}` : '记录探针'}
        open={Boolean(probeModalContract)}
        width={760}
        confirmLoading={probeActionLoading}
        onOk={() => void submitIntegrationProbe()}
        onCancel={() => setProbeModalContract(null)}
      >
        <Form<IntegrationContractProbeFormValues> form={integrationContractProbeForm} layout="vertical">
          <Row gutter={12}>
            <Col xs={24} md={12}>
              <Form.Item name="probeKey" label="Probe Key" rules={[{ required: true, message: '请输入 Probe Key' }]}>
                <Input placeholder="prod-readiness-probe" />
              </Form.Item>
            </Col>
            <Col xs={24} md={6}>
              <Form.Item name="environment" label="环境">
                <Select options={integrationEnvironmentOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={6}>
              <Form.Item name="status" label="状态">
                <Select options={integrationProbeStatusOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="httpStatusCode" label="HTTP 状态">
                <InputNumber style={{ width: '100%' }} min={100} max={599} precision={0} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="latencyMs" label="延迟 ms">
                <InputNumber style={{ width: '100%' }} min={0} max={600000} precision={0} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="observedAt" label="观测时间">
                <Input placeholder="2026-06-06T10:00:00" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="errorType" label="错误类型">
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="problemTypeUri" label="Problem Type URI">
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="problemTitle" label="问题标题">
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="problemDetail" label="问题详情">
                <Input />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="evidenceJson" label="Evidence JSON">
                <Input.TextArea rows={4} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
      <Modal
        title={auditModalContract ? `审计 - ${auditModalContract.displayName}` : '审计'}
        open={Boolean(auditModalContract)}
        width={920}
        footer={null}
        onCancel={() => {
          setAuditModalContract(null)
          setIntegrationContractAuditEvents([])
        }}
      >
        <Table<MarketingIntegrationContractAuditEvent>
          rowKey={row => String(row.id ?? `${row.contractId}-${row.revision}`)}
          loading={auditLoading}
          dataSource={integrationContractAuditEvents}
          pagination={{ pageSize: 6, showSizeChanger: false }}
          columns={[
            {
              title: 'Revision',
              dataIndex: 'revision',
              width: 110,
              render: value => <Text>{String(value)}</Text>,
            },
            {
              title: '事件',
              dataIndex: 'eventType',
              width: 130,
              render: value => <Tag>{String(value)}</Tag>,
            },
            {
              title: '状态变化',
              key: 'status',
              render: (_, row) => <Text>{row.previousStatus ?? '-'}{' -> '}{row.newStatus ?? '-'}</Text>,
            },
            {
              title: '变更字段',
              key: 'changedFields',
              render: (_, row) => <Text type="secondary">{formatJsonBrief(row.changedFields)}</Text>,
            },
            {
              title: '操作人',
              dataIndex: 'changedBy',
              width: 150,
              render: value => <Text type="secondary">{String(value ?? '-')}</Text>,
            },
            {
              title: '时间',
              dataIndex: 'createdAt',
              width: 190,
              render: value => <Text type="secondary">{formatDateTime(String(value ?? ''))}</Text>,
            },
          ]}
        />
      </Modal>
      <Modal
        title="新建或更新 Campaign"
        open={campaignModalOpen}
        width={760}
        confirmLoading={campaignActionLoading}
        onOk={() => void submitCampaign()}
        onCancel={() => setCampaignModalOpen(false)}
      >
        <Form<CampaignFormValues> form={campaignForm} layout="vertical">
          <Row gutter={12}>
            <Col xs={24} md={12}>
              <Form.Item name="campaignKey" label="Campaign Key" rules={[{ required: true, message: '请输入 Campaign Key' }]}>
                <Input placeholder="spring-launch-2026" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="campaignName" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="status" label="状态">
                <Select options={campaignStatusOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="objective" label="目标">
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="primaryChannel" label="主渠道">
                <Select options={channelOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="ownerTeam" label="负责团队">
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="budgetAmount" label="预算">
                <InputNumber style={{ width: '100%' }} min={0} precision={2} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="currency" label="币种">
                <Select options={currencyOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="startAt" label="开始时间">
                <Input placeholder="2026-06-01T00:00:00" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="endAt" label="结束时间">
                <Input placeholder="2026-06-30T23:59:00" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="briefJson" label="Brief JSON">
                <Input.TextArea rows={4} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
      <Modal
        title={linkModalCampaign ? `绑定资源 - ${linkModalCampaign.campaignName}` : '绑定资源'}
        open={Boolean(linkModalCampaign)}
        width={760}
        confirmLoading={linkActionLoading}
        onOk={() => void submitCampaignLink()}
        onCancel={() => setLinkModalCampaign(null)}
      >
        <Form<CampaignLinkFormValues> form={campaignLinkForm} layout="vertical">
          <Row gutter={12}>
            <Col xs={24} md={12}>
              <Form.Item name="resourceType" label="资源类型" rules={[{ required: true, message: '请选择资源类型' }]}>
                <Select options={resourceTypeOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="resourceKey" label="资源 Key" rules={[{ required: true, message: '请输入资源 Key' }]}>
                <Input placeholder="launch-journey" />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="resourceId" label="资源 ID">
                <InputNumber style={{ width: '100%' }} min={1} precision={0} />
              </Form.Item>
            </Col>
            <Col xs={24} md={16}>
              <Form.Item name="resourceName" label="资源名称">
                <Input />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="resourceRoute" label="资源入口">
                <Input placeholder="/canvas/300" />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="dependencyRole" label="依赖角色">
                <Select options={dependencyRoleOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="linkStatus" label="链接状态">
                <Select options={linkStatusOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="requiredForLaunch" label="上线必需" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="metadataJson" label="Metadata JSON">
                <Input.TextArea rows={4} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  )
}

function renderEvidence(evidence: MarketingPlatformCapability['evidence']) {
  return evidence.length === 0
    ? <Text type="secondary">未上报</Text>
    : (
      <Space size={[4, 4]} wrap>
        {evidence.map(signal => (
          <Tag
            key={signal.signalKey}
            color={evidenceStatusColor(signal.status)}
            title={evidenceStatusText(signal.status)}
          >
            {signal.label} {signal.value}
          </Tag>
        ))}
      </Space>
    )
}

const providerFamilyOptions = [
  { value: 'SEM', label: 'SEM' },
  { value: 'SOCIAL_MONITORING', label: 'SOCIAL_MONITORING' },
  { value: 'PAID_MEDIA', label: 'PAID_MEDIA' },
  { value: 'PROGRAMMATIC_DSP', label: 'PROGRAMMATIC_DSP' },
  { value: 'CREATOR', label: 'CREATOR' },
  { value: 'CONTENT_JOURNEY', label: 'CONTENT_JOURNEY' },
  { value: 'CAMPAIGN', label: 'CAMPAIGN' },
]

const integrationDirectionOptions = [
  { value: 'INBOUND', label: 'INBOUND' },
  { value: 'OUTBOUND', label: 'OUTBOUND' },
  { value: 'BIDIRECTIONAL', label: 'BIDIRECTIONAL' },
  { value: 'INTERNAL', label: 'INTERNAL' },
]

const integrationEnvironmentOptions = [
  { value: 'PRODUCTION', label: 'PRODUCTION' },
  { value: 'STAGING', label: 'STAGING' },
  { value: 'SANDBOX', label: 'SANDBOX' },
]

const integrationAuthModeOptions = [
  { value: 'OAUTH', label: 'OAUTH' },
  { value: 'API_KEY', label: 'API_KEY' },
  { value: 'HMAC', label: 'HMAC' },
  { value: 'INTERNAL', label: 'INTERNAL' },
  { value: 'NONE', label: 'NONE' },
]

const integrationContractStatusOptions = [
  { value: 'DRAFT', label: 'DRAFT' },
  { value: 'ACTIVE', label: 'ACTIVE' },
  { value: 'DEGRADED', label: 'DEGRADED' },
  { value: 'BLOCKED', label: 'BLOCKED' },
  { value: 'ARCHIVED', label: 'ARCHIVED' },
]

const integrationProbeStatusOptions = [
  { value: 'PASS', label: 'PASS' },
  { value: 'WARN', label: 'WARN' },
  { value: 'FAIL', label: 'FAIL' },
]

const slaTierOptions = [
  { value: 'STANDARD', label: 'STANDARD' },
  { value: 'BUSINESS_CRITICAL', label: 'BUSINESS_CRITICAL' },
  { value: 'BEST_EFFORT', label: 'BEST_EFFORT' },
]

const campaignStatusOptions = [
  { value: 'DRAFT', label: 'DRAFT' },
  { value: 'ACTIVE', label: 'ACTIVE' },
  { value: 'PAUSED', label: 'PAUSED' },
  { value: 'COMPLETED', label: 'COMPLETED' },
  { value: 'ARCHIVED', label: 'ARCHIVED' },
]

const channelOptions = [
  { value: 'PAID_MEDIA', label: 'PAID_MEDIA' },
  { value: 'SCRM', label: 'SCRM' },
  { value: 'CONTENT', label: 'CONTENT' },
  { value: 'SEARCH', label: 'SEARCH' },
  { value: 'PROGRAMMATIC_DSP', label: 'PROGRAMMATIC_DSP' },
  { value: 'CREATOR', label: 'CREATOR' },
]

const currencyOptions = [
  { value: 'CNY', label: 'CNY' },
  { value: 'USD', label: 'USD' },
  { value: 'EUR', label: 'EUR' },
  { value: 'HKD', label: 'HKD' },
]

const resourceTypeOptions = [
  { value: 'JOURNEY', label: 'JOURNEY' },
  { value: 'CONTENT_RELEASE', label: 'CONTENT_RELEASE' },
  { value: 'PAID_MEDIA_DESTINATION', label: 'PAID_MEDIA_DESTINATION' },
  { value: 'BI_DASHBOARD', label: 'BI_DASHBOARD' },
  { value: 'PROVIDER_WRITE_GATEWAY', label: 'PROVIDER_WRITE_GATEWAY' },
]

const dependencyRoleOptions = [
  { value: 'PRIMARY', label: 'PRIMARY' },
  { value: 'SUPPORTING', label: 'SUPPORTING' },
  { value: 'MEASUREMENT', label: 'MEASUREMENT' },
  { value: 'APPROVAL', label: 'APPROVAL' },
]

const linkStatusOptions = [
  { value: 'ACTIVE', label: 'ACTIVE' },
  { value: 'MISSING', label: 'MISSING' },
  { value: 'BLOCKED', label: 'BLOCKED' },
  { value: 'ARCHIVED', label: 'ARCHIVED' },
]

function calculateIntegrationContractKpis(contracts: MarketingIntegrationContract[]) {
  return {
    total: contracts.length,
    productionActive: contracts.filter(contract =>
      contract.environment === 'PRODUCTION' && contract.status === 'ACTIVE').length,
    blocked: contracts.filter(contract => contract.status === 'BLOCKED').length,
    degraded: contracts.filter(contract => contract.status === 'DEGRADED').length,
  }
}

function calculateIntegrationProbeKpis(probes: MarketingIntegrationContractProbe[]) {
  return {
    pass: probes.filter(probe => probe.status === 'PASS').length,
    warn: probes.filter(probe => probe.status === 'WARN').length,
    fail: probes.filter(probe => probe.status === 'FAIL').length,
  }
}

function calculateCampaignKpis(
  campaigns: MarketingCampaign[],
  campaignLinks: Record<number, MarketingCampaignLink[]>,
) {
  const links = Object.values(campaignLinks).flat()
  return {
    total: campaigns.length,
    active: campaigns.filter(campaign => campaign.status === 'ACTIVE').length,
    requiredLinks: links.filter(link => link.requiredForLaunch).length,
    blockedLinks: links.filter(link => link.linkStatus === 'BLOCKED').length,
  }
}

function emptyToUndefined(value?: string) {
  if (!value || value.trim().length === 0) return undefined
  return value.trim()
}

function parseJsonObject(value: string | undefined, label: string): Record<string, unknown> {
  if (!value || value.trim().length === 0) return {}
  const parsed = JSON.parse(value) as unknown
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error(`${label} must be a JSON object`)
  }
  return parsed as Record<string, unknown>
}

function formatAmount(value: unknown) {
  if (typeof value === 'number') return value.toLocaleString(undefined, { maximumFractionDigits: 2 })
  if (typeof value === 'string' && value.length > 0) return value
  return '0'
}

function formatJsonBrief(value: Record<string, unknown>) {
  try {
    const text = JSON.stringify(value ?? {})
    return text.length > 120 ? `${text.slice(0, 117)}...` : text
  } catch {
    return '{}'
  }
}

function probeStatusColor(status: string) {
  switch (status) {
    case 'PASS':
      return 'green'
    case 'WARN':
      return 'gold'
    case 'FAIL':
      return 'red'
    default:
      return 'default'
  }
}

function alertSeverityColor(severity: string) {
  switch (severity) {
    case 'CRITICAL':
      return 'red'
    case 'HIGH':
      return 'volcano'
    case 'MEDIUM':
      return 'gold'
    case 'LOW':
      return 'blue'
    default:
      return 'default'
  }
}

function formatHttpLatency(probe: MarketingIntegrationContractProbe) {
  const status = probe.httpStatusCode == null ? '-' : String(probe.httpStatusCode)
  const latency = probe.latencyMs == null ? '-' : `${probe.latencyMs}ms`
  return `${status} · ${latency}`
}

function formatAlertRuntime(metadata: Record<string, unknown>) {
  const status = metadataText(metadata, 'lastHttpStatusCode') !== '-'
    ? metadataText(metadata, 'lastHttpStatusCode')
    : metadataText(metadata, 'httpStatusCode')
  const latency = metadataText(metadata, 'lastLatencyMs') !== '-'
    ? metadataText(metadata, 'lastLatencyMs')
    : metadataText(metadata, 'latencyMs')
  return `${status} · ${latency === '-' ? '-' : `${latency}ms`}`
}

function formatSloBurnRate(evaluation: MarketingIntegrationContractSloEvaluation) {
  const window = evaluation.windows.find(item => item.breached) ?? evaluation.windows[0]
  if (!window) return '-'
  return `${formatNumber(window.burnRate)}x / ${formatNumber(window.thresholdBurnRate)}x`
}

function formatNumber(value?: number | null) {
  if (value == null || Number.isNaN(value)) return '-'
  return Number.isInteger(value) ? String(value) : String(Number(value.toFixed(2)))
}

function metadataText(metadata: Record<string, unknown>, key: string) {
  const value = metadata?.[key]
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  if (typeof value === 'string' && value.length > 0) return value
  return '-'
}

function formatDateTime(value?: string | null) {
  return value && value.length > 0 ? value : '-'
}

function CampaignResourceLinks({
  campaign,
  links,
  readiness,
  readinessLoading,
  onLoad,
  onEvaluate,
  onCreate,
  onDelete,
}: {
  campaign: MarketingCampaign
  links?: MarketingCampaignLink[]
  readiness?: MarketingCampaignReadiness
  readinessLoading: boolean
  onLoad: () => void
  onEvaluate: () => void
  onCreate: () => void
  onDelete: (linkId: number) => void
}) {
  if (!links) {
    return (
      <Space>
        <Button size="small" onClick={onLoad}>加载资源链接</Button>
        <Button size="small" loading={readinessLoading} onClick={onEvaluate}>评估闸口</Button>
        <Button size="small" icon={<LinkOutlined />} onClick={onCreate}>绑定资源</Button>
      </Space>
    )
  }

  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <CampaignReadinessPanel
        readiness={readiness}
        loading={readinessLoading}
        onEvaluate={onEvaluate}
      />
      <Table<MarketingCampaignLink>
        size="small"
        rowKey="id"
        dataSource={links}
        pagination={false}
        locale={{ emptyText: `暂无 ${campaign.campaignName} 的资源链接` }}
        scroll={{ x: 980 }}
        columns={[
          {
            title: '资源',
            dataIndex: 'resourceName',
            render: (_, row) => (
              <Space direction="vertical" size={0}>
                <Text strong>{row.resourceName ?? row.resourceKey}</Text>
                <Text type="secondary">{row.resourceType} · {row.resourceKey}</Text>
              </Space>
            ),
          },
          {
            title: '状态',
            dataIndex: 'linkStatus',
            width: 130,
            render: status => <Tag color={statusColor(String(status))}>{statusText(String(status))}</Tag>,
          },
          {
            title: '角色',
            dataIndex: 'dependencyRole',
            width: 140,
            render: role => <Tag>{String(role)}</Tag>,
          },
          {
            title: '上线必需',
            dataIndex: 'requiredForLaunch',
            width: 110,
            render: required => <Tag color={required ? 'green' : 'default'}>{required ? '是' : '否'}</Tag>,
          },
          {
            title: '入口',
            dataIndex: 'resourceRoute',
            render: route => <Text type="secondary">{String(route ?? '-')}</Text>,
          },
          {
            title: '操作',
            key: 'actions',
            fixed: 'right',
            width: 150,
            render: (_, row) => (
              <Space size={6}>
                <Button size="small" icon={<LinkOutlined />} onClick={onCreate}>绑定</Button>
                <Popconfirm title="移除资源链接" okText="移除" cancelText="取消" onConfirm={() => onDelete(row.id)}>
                  <Button size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
    </Space>
  )
}

function CampaignReadinessPanel({
  readiness,
  loading,
  onEvaluate,
}: {
  readiness?: MarketingCampaignReadiness
  loading: boolean
  onEvaluate: () => void
}) {
  if (!readiness) {
    return (
      <Alert
        type="info"
        showIcon
        message="Campaign 上线闸口未评估"
        action={<Button size="small" loading={loading} onClick={onEvaluate}>评估闸口</Button>}
      />
    )
  }
  return (
    <Alert
      type={readiness.status === 'READY' ? 'success' : readiness.status === 'DEGRADED' ? 'warning' : 'error'}
      showIcon
      message={(
        <Space size={8} wrap>
          <Text strong>上线闸口：{statusText(readiness.status)}</Text>
          <Tag color={readiness.productionReady ? 'green' : 'red'}>
            {readiness.productionReady ? '允许上线' : '阻断上线'}
          </Tag>
          <Tag>必需资源 {readiness.activeRequiredLinkCount}/{readiness.requiredLinkCount}</Tag>
          <Tag color={readiness.blockerCount > 0 ? 'red' : 'default'}>阻断 {readiness.blockerCount}</Tag>
          <Tag color={readiness.warningCount > 0 ? 'gold' : 'default'}>警告 {readiness.warningCount}</Tag>
        </Space>
      )}
      description={(
        <Space direction="vertical" size={4} style={{ width: '100%' }}>
          {[...readiness.blockers, ...readiness.warnings].length === 0 ? (
            <Text type="secondary">所有上线必需资源均已通过闸口。</Text>
          ) : (
            [...readiness.blockers, ...readiness.warnings].map(finding => (
              <Text key={`${finding.severity}-${finding.itemType}-${finding.itemKey}`}>
                {finding.title}: {finding.reason}
              </Text>
            ))
          )}
          <Text type="secondary">评估时间 {readiness.generatedAt}</Text>
        </Space>
      )}
      action={<Button size="small" loading={loading} onClick={onEvaluate}>重新评估</Button>}
    />
  )
}

function ReadinessFindingPanel({
  title,
  emptyText,
  items,
  onOpen,
}: {
  title: string
  emptyText: string
  items: MarketingPlatformReadinessFinding[]
  onOpen: (route: string) => void
}) {
  return (
    <Card size="small" title={title}>
      {items.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={emptyText} />
      ) : (
        <Space direction="vertical" size={10} style={{ width: '100%' }}>
          {items.slice(0, 6).map(item => (
            <div key={`${item.itemType}-${item.itemKey}-${item.title}`} style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: 12 }}>
              <Space direction="vertical" size={6} style={{ width: '100%' }}>
                <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
                  <Text strong>{item.title}</Text>
                  <Tag color={item.severity === 'BLOCKER' ? 'red' : 'gold'}>{item.itemType}</Tag>
                </Space>
                <Text type="secondary">{item.reason}</Text>
                <Button size="small" type="link" onClick={() => onOpen(item.route)}>打开相关入口</Button>
              </Space>
            </div>
          ))}
          {items.length > 6 && <Text type="secondary">还有 {items.length - 6} 项未展示</Text>}
        </Space>
      )}
    </Card>
  )
}

function approveProviderWrite(gateway: ProviderWriteGateway, id: number) {
  const payload = { decision: 'APPROVED' as const, reason: 'approved from marketing platform operations' }
  switch (gateway) {
    case 'SEARCH_MARKETING':
      return marketingPlatformApi.approveSearchMarketingMutation(id, payload)
    case 'CREATOR':
      return marketingPlatformApi.approveCreatorProviderMutation(id, payload)
    case 'PROGRAMMATIC_DSP':
      return marketingPlatformApi.approveProgrammaticDspMutation(id, payload)
  }
}

function executeProviderWrite(gateway: ProviderWriteGateway, id: number, dryRun: boolean) {
  const payload = {
    dryRun,
    partialFailure: true,
    metadata: { source: 'marketing-platform-ui' },
  }
  switch (gateway) {
    case 'SEARCH_MARKETING':
      return marketingPlatformApi.executeSearchMarketingMutation(id, payload)
    case 'CREATOR':
      return marketingPlatformApi.executeCreatorProviderMutation(id, payload)
    case 'PROGRAMMATIC_DSP':
      return marketingPlatformApi.executeProgrammaticDspMutation(id, payload)
  }
}

function ActionRow({ item, onOpen }: { item: MarketingPlatformActionItem; onOpen: () => void }) {
  return (
    <div style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: 12 }}>
      <Space direction="vertical" size={6} style={{ width: '100%' }}>
        <Space align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
          <Text strong>{item.title}</Text>
          <Tag color={priorityColor(item.priority)}>{item.priority}</Tag>
        </Space>
        <Text type="secondary">{item.reason}</Text>
        <Button size="small" type="link" onClick={onOpen}>打开相关入口</Button>
      </Space>
    </div>
  )
}
