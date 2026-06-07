import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Col, Empty, Row, Space, Spin, Statistic, Tabs, Tag, Typography } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import {
  growthActivityApi,
  type GrowthActivity,
  type GrowthActivityCommand,
  type GrowthActivityQuery,
  type GrowthActivityReadiness,
  type GrowthActivityReport,
  type GrowthReferralCode,
  type GrowthReferralRelation,
  type GrowthRewardGrant,
  type GrowthRewardPool,
  type GrowthTaskDefinition,
  type GrowthTaskProgress,
} from '../../services/growthActivityApi'

const { Text, Title } = Typography

const ACTIVITY_TYPES = [
  'BENEFIT_PROMOTION',
  'REFERRAL_INVITE',
  'TASK_INCENTIVE',
  'LOYALTY_MEMBER_ACTIVITY',
  'RETENTION_WINBACK',
  'CONTENT_PRIVATE_DOMAIN_ACTIVITY',
]
const STATUSES = ['DRAFT', 'ACTIVE', 'PAUSED', 'CLOSED', 'ARCHIVED']
const READINESS_STATUSES = ['READY', 'WARNING', 'BLOCKED']
const SCHEDULE_STATUSES = ['NOT_STARTED', 'RUNNING', 'ENDING_SOON', 'ENDED', 'NO_SCHEDULE']
const GRANT_HEALTH_STATUSES = ['HEALTHY', 'LOW_INVENTORY', 'BUDGET_RISK', 'FAILED_GRANTS', 'NO_REWARD_POOL']
const ACTIVITY_PRESETS: Record<string, Pick<GrowthActivityCommand, 'activityType' | 'objective' | 'channelScope'>> = {
  REFERRAL_INVITE: { activityType: 'REFERRAL_INVITE', objective: 'ACQUISITION', channelScope: 'PRIVATE_DOMAIN' },
  TASK_INCENTIVE: { activityType: 'TASK_INCENTIVE', objective: 'ENGAGEMENT', channelScope: 'APP' },
  BENEFIT_PROMOTION: { activityType: 'BENEFIT_PROMOTION', objective: 'PROMOTION', channelScope: 'ALL' },
}

interface ActivityWizardState {
  activityKey: string
  activityName: string
  activityType: string
  status: string
  campaignId: string
  objective: string
  ownerTeam: string
  channelScope: string
  dashboardRef: string
}

export default function GrowthActivitiesPage() {
  const [activities, setActivities] = useState<GrowthActivity[]>([])
  const [selectedActivityId, setSelectedActivityId] = useState<number | null>(null)
  const [readiness, setReadiness] = useState<GrowthActivityReadiness | null>(null)
  const [report, setReport] = useState<GrowthActivityReport | null>(null)
  const [rewardPools, setRewardPools] = useState<GrowthRewardPool[]>([])
  const [grants, setGrants] = useState<GrowthRewardGrant[]>([])
  const [referralCodes, setReferralCodes] = useState<GrowthReferralCode[]>([])
  const [referralRelations, setReferralRelations] = useState<GrowthReferralRelation[]>([])
  const [taskDefinitions, setTaskDefinitions] = useState<GrowthTaskDefinition[]>([])
  const [taskProgress, setTaskProgress] = useState<GrowthTaskProgress[]>([])
  const [activityType, setActivityType] = useState('')
  const [status, setStatus] = useState('')
  const [campaignId, setCampaignId] = useState('')
  const [ownerTeam, setOwnerTeam] = useState('')
  const [readinessStatus, setReadinessStatus] = useState('')
  const [scheduleStatus, setScheduleStatus] = useState('')
  const [grantHealth, setGrantHealth] = useState('')
  const [activeDetailTab, setActiveDetailTab] = useState('overview')
  const [loading, setLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [rewardPoolLoading, setRewardPoolLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [wizardOpen, setWizardOpen] = useState(false)
  const [wizardMode, setWizardMode] = useState<'create' | 'edit'>('create')
  const [wizard, setWizard] = useState<ActivityWizardState>(() => emptyWizard('REFERRAL_INVITE'))

  const selectedActivity = useMemo(
    () => activities.find(activity => activity.id === selectedActivityId) ?? activities[0] ?? null,
    [activities, selectedActivityId],
  )
  const campaignOptions = useMemo(() => {
    const seen = new Set<number>()
    return activities
      .filter(activity => typeof activity.campaignId === 'number' && !seen.has(activity.campaignId))
      .map(activity => {
        seen.add(activity.campaignId as number)
        return { campaignId: activity.campaignId as number, label: `${activity.campaignId} · ${activity.activityName}` }
      })
  }, [activities])

  const loadActivities = useCallback(async (query: GrowthActivityQuery = { limit: 50 }) => {
    setLoading(true)
    setLoadError(null)
    try {
      const response = await growthActivityApi.listActivities(query)
      const rows = response.data ?? []
      setActivities(rows)
      setSelectedActivityId(current => {
        if (current && rows.some(row => row.id === current)) return current
        return rows[0]?.id ?? null
      })
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '请稍后重试')
    } finally {
      setLoading(false)
    }
  }, [])

  const loadSelectedDetails = useCallback(async (activityId: number | null) => {
    if (!activityId) {
      setReadiness(null)
      setReport(null)
      setRewardPools([])
      setGrants([])
      setReferralCodes([])
      setReferralRelations([])
      setTaskDefinitions([])
      setTaskProgress([])
      return
    }
    setDetailLoading(true)
    setRewardPoolLoading(true)
    try {
      const [
        readinessResponse,
        reportResponse,
        rewardPoolResponse,
        grantResponse,
        referralCodeResponse,
        referralRelationResponse,
        taskDefinitionResponse,
        taskProgressResponse,
      ] = await Promise.all([
        growthActivityApi.getReadiness(activityId),
        growthActivityApi.getReport(activityId),
        growthActivityApi.listRewardPools(activityId),
        growthActivityApi.listGrants(activityId),
        growthActivityApi.listReferralCodes(activityId),
        growthActivityApi.listReferralRelations(activityId),
        growthActivityApi.listTaskDefinitions(activityId),
        growthActivityApi.listTaskProgress(activityId),
      ])
      setReadiness(readinessResponse.data)
      setReport(reportResponse.data)
      setRewardPools(rewardPoolResponse.data ?? [])
      setGrants(grantResponse.data ?? [])
      setReferralCodes(referralCodeResponse.data ?? [])
      setReferralRelations(referralRelationResponse.data ?? [])
      setTaskDefinitions(taskDefinitionResponse.data ?? [])
      setTaskProgress(taskProgressResponse.data ?? [])
    } finally {
      setDetailLoading(false)
      setRewardPoolLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadActivities()
  }, [loadActivities])

  useEffect(() => {
    void loadSelectedDetails(selectedActivity?.id ?? null)
  }, [loadSelectedDetails, selectedActivity?.id])

  const applyFilters = () => {
    const normalizedCampaignId = Number(campaignId)
    void loadActivities({
      ...(activityType ? { activityType } : {}),
      ...(status ? { status } : {}),
      ...(campaignId.trim() && Number.isFinite(normalizedCampaignId) ? { campaignId: normalizedCampaignId } : {}),
      ...(ownerTeam.trim() ? { ownerTeam: ownerTeam.trim() } : {}),
      ...(readinessStatus ? { readinessStatus } : {}),
      ...(scheduleStatus ? { scheduleStatus } : {}),
      ...(grantHealth ? { grantHealth } : {}),
      limit: 50,
    })
  }

  const openCreateWizard = () => {
    setWizardMode('create')
    setWizard(emptyWizard('REFERRAL_INVITE'))
    setWizardOpen(true)
  }

  const openEditWizard = () => {
    if (!selectedActivity) return
    setWizardMode('edit')
    setWizard({
      activityKey: selectedActivity.activityKey,
      activityName: selectedActivity.activityName,
      activityType: selectedActivity.activityType,
      status: selectedActivity.status,
      campaignId: selectedActivity.campaignId == null ? '' : String(selectedActivity.campaignId),
      objective: selectedActivity.objective ?? '',
      ownerTeam: selectedActivity.ownerTeam ?? '',
      channelScope: selectedActivity.channelScope ?? '',
      dashboardRef: selectedActivity.dashboardRef ?? '',
    })
    setWizardOpen(true)
  }

  const applyActivityPreset = (presetKey: string) => {
    const preset = ACTIVITY_PRESETS[presetKey] ?? ACTIVITY_PRESETS.REFERRAL_INVITE
    setWizard(current => ({
      ...current,
      activityType: preset.activityType,
      objective: preset.objective ?? '',
      channelScope: preset.channelScope ?? '',
    }))
  }

  const saveActivity = async () => {
    const normalizedCampaignId = Number(wizard.campaignId)
    await growthActivityApi.upsertActivity({
      activityKey: wizard.activityKey.trim(),
      activityName: wizard.activityName.trim(),
      activityType: wizard.activityType,
      status: wizard.status,
      campaignId: wizard.campaignId.trim() && Number.isFinite(normalizedCampaignId) ? normalizedCampaignId : null,
      objective: wizard.objective.trim() || null,
      ownerTeam: wizard.ownerTeam.trim() || null,
      channelScope: wizard.channelScope.trim() || null,
      dashboardRef: wizard.dashboardRef.trim() || null,
      audienceRefs: {},
      metadata: { source: wizardMode },
    })
    setWizardOpen(false)
    await loadActivities({ limit: 50 })
  }

  const participationTotal = report?.participation.totalParticipants ?? 0
  const grantTotal = report?.grants.totalGrants ?? 0
  const roi = formatNumber(report?.conversion.roi)
  const readinessItems = readiness?.blockers.length ? readiness.blockers : readiness?.checks ?? []
  const rewardPoolCostCurrency = rewardPools[0]?.costCurrency ?? 'CNY'
  const rewardPoolBudgetTotal = rewardPools.reduce((total, pool) => total + numberValue(pool.budgetAmount), 0)
  const rewardPoolReservedAmount = rewardPools.reduce((total, pool) => total + numberValue(pool.reservedAmount), 0)
  const rewardPoolGrantedAmount = rewardPools.reduce((total, pool) => total + numberValue(pool.grantedAmount), 0)
  const rewardPoolCostTotal = rewardPoolReservedAmount + rewardPoolGrantedAmount
  const rewardPoolInventoryTotal = rewardPools.reduce((total, pool) => total + (pool.totalInventory ?? 0), 0)
  const rewardPoolReservedInventory = rewardPools.reduce((total, pool) => total + (pool.reservedInventory ?? 0), 0)
  const rewardPoolGrantedInventory = rewardPools.reduce((total, pool) => total + (pool.grantedInventory ?? 0), 0)
  const detailTabs = [
    {
      key: 'overview',
      label: '概览',
      children: (
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Space wrap>
            <Tag color={statusColor(selectedActivity?.status)}>{selectedActivity?.status ?? '-'}</Tag>
            <Tag>{selectedActivity?.activityType ?? '-'}</Tag>
            <Tag color={readiness?.productionReady ? 'green' : 'red'}>{readiness?.status ?? '-'}</Tag>
          </Space>
          <Row gutter={12}>
            <Col span={8} role="group" aria-label={`参与 ${participationTotal}`}>
              <Statistic title="参与" value={participationTotal} prefix="参与" />
            </Col>
            <Col span={8} role="group" aria-label={`发放 ${grantTotal}`}>
              <Statistic title="发放" value={grantTotal} prefix="发放" />
            </Col>
            <Col span={8} role="group" aria-label={`ROI ${roi}`}>
              <Statistic title="ROI" value={roi} prefix="ROI" />
            </Col>
          </Row>
          {readinessItems.slice(0, 1).map(check => (
            <Alert
              key={`${check.itemType}:${check.itemKey}:overview`}
              type={check.severity === 'BLOCKER' ? 'error' : check.severity === 'WARNING' ? 'warning' : 'success'}
              message={check.title}
              description={check.reason}
              showIcon
            />
          ))}
        </Space>
      ),
    },
    {
      key: 'readiness',
      label: '上线闸口',
      children: (
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          <Space>
            <Tag color={readiness?.productionReady ? 'green' : 'red'}>{readiness?.status ?? '-'}</Tag>
            <Text type="secondary">
              阻断 {readiness?.blockerCount ?? 0} · 警告 {readiness?.warningCount ?? 0}
            </Text>
          </Space>
          {readinessItems.slice(0, 4).map(check => (
            <Alert
              key={`${check.itemType}:${check.itemKey}:readiness`}
              type={check.severity === 'BLOCKER' ? 'error' : check.severity === 'WARNING' ? 'warning' : 'success'}
              message={check.title}
              description={check.reason}
              showIcon
            />
          ))}
        </Space>
      ),
    },
    {
      key: 'resources',
      label: '关联资源',
      children: (
        <Space direction="vertical" size={8}>
          <Text>{`Campaign ${selectedActivity?.campaignId ?? '-'}`}</Text>
          <Text>{`渠道 ${selectedActivity?.channelScope ?? 'ALL'}`}</Text>
          <Text>{`风险策略 ${selectedActivity?.riskPolicyRef ?? '-'}`}</Text>
          <Text>{`实验 ${selectedActivity?.experimentRef ?? '-'}`}</Text>
          <Text>{`仪表盘 ${selectedActivity?.dashboardRef ?? '-'}`}</Text>
        </Space>
      ),
    },
    {
      key: 'reward-pools',
      label: '奖励池',
      children: (
        <Spin spinning={rewardPoolLoading}>
          {rewardPools.length === 0 ? (
            <Empty description="暂无奖励池" />
          ) : (
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Row gutter={12}>
                <Col span={12}>
                  <Statistic title="预算" value={formatMoney(rewardPoolBudgetTotal)} suffix={rewardPoolCostCurrency} />
                </Col>
                <Col span={12}>
                  <Statistic title="成本" value={formatMoney(rewardPoolCostTotal)} suffix={rewardPoolCostCurrency} />
                </Col>
              </Row>
              <Text>{`预算 ${formatMoney(rewardPoolBudgetTotal)} ${rewardPoolCostCurrency}`}</Text>
              <Text>{`库存 ${rewardPoolInventoryTotal} · 预留 ${rewardPoolReservedInventory} · 已发 ${rewardPoolGrantedInventory}`}</Text>
              <Text>{`失败 ${report?.grants.failedGrants ?? 0} · 取消 ${report?.grants.canceledGrants ?? 0} · 兑换 ${report?.grants.redeemedGrants ?? 0} · 过期 ${report?.grants.expiredGrants ?? 0}`}</Text>
              <Text>{`成本 ${formatMoney(rewardPoolCostTotal)} ${rewardPoolCostCurrency}`}</Text>
              {rewardPools.map(pool => (
                <Card key={pool.id} size="small">
                  <Space direction="vertical" size={6} style={{ width: '100%' }}>
                    <Space wrap>
                      <Text strong>{pool.poolKey}</Text>
                      <Tag>{pool.rewardType}</Tag>
                      <Tag>{pool.grantChannel}</Tag>
                      <Tag color={statusColor(pool.status)}>{pool.status}</Tag>
                      {pool.inventoryLow && <Tag color="orange">低库存</Tag>}
                    </Space>
                    <Text type="secondary">
                      {`${pool.inventoryMode ?? '-'} · 单用户 ${pool.perUserLimit ?? '-'} · 单推荐 ${pool.perReferralLimit ?? '-'}`}
                    </Text>
                    <Text type="secondary">
                      {`预算 ${formatMoney(pool.budgetAmount)} ${pool.costCurrency ?? rewardPoolCostCurrency} · 预留 ${formatMoney(pool.reservedAmount)} · 已发 ${formatMoney(pool.grantedAmount)}`}
                    </Text>
                  </Space>
                </Card>
              ))}
            </Space>
          )}
        </Spin>
      ),
    },
    {
      key: 'participants',
      label: '参与者',
      children: <Text>{`参与者 ${participationTotal} · 活跃 ${report?.participation.activeParticipants ?? 0}`}</Text>,
    },
    {
      key: 'grants',
      label: '发放流水',
      children: (
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Text>{`发放 ${grantTotal} · 成功 ${report?.grants.successGrants ?? 0} · 失败 ${report?.grants.failedGrants ?? 0}`}</Text>
          {grants.length === 0 ? (
            <Empty description="暂无发放流水" />
          ) : (
            grants.map(grant => (
              <Card key={grant.id} size="small">
                <Space direction="vertical" size={6} style={{ width: '100%' }}>
                  <Space wrap>
                    <Text strong>{`流水 ${grant.id}`}</Text>
                    <Tag color={statusColor(grant.status)}>{grant.status}</Tag>
                    <Text>{grant.grantReason}</Text>
                  </Space>
                  <Text type="secondary">
                    {`幂等键 ${grant.idempotencyKey} · 渠道池 ${grant.poolId} · 成本 ${formatMoney(grant.costAmount)}`}
                  </Text>
                  <Text type="secondary">
                    {`参与者 ${grant.participantId ?? '-'} · 推荐 ${grant.referralRelationId ?? '-'} · 任务 ${grant.taskProgressId ?? '-'}`}
                  </Text>
                  <Text type="secondary">{`供应商证据 ${providerEvidenceText(grant.providerResponse)}`}</Text>
                  {grant.status === 'FAILED' ? (
                    <Text type="danger">{`错误 ${providerErrorText(grant.providerResponse)}`}</Text>
                  ) : null}
                  <Space wrap>
                    <Button size="small" onClick={() => void growthActivityApi.retryGrant(grant.activityId, grant.id)}>重试</Button>
                    <Button
                      size="small"
                      onClick={() => void growthActivityApi.reconcileGrant(grant.activityId, grant.id, {
                        providerStatus: grant.status,
                        providerResponse: grant.providerResponse ?? {},
                      })}
                    >
                      对账
                    </Button>
                    <Button size="small" danger onClick={() => void growthActivityApi.cancelGrant(grant.activityId, grant.id)}>取消</Button>
                  </Space>
                </Space>
              </Card>
            ))
          )}
        </Space>
      ),
    },
    {
      key: 'referrals',
      label: '推荐关系',
      children: (
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Text>{`推荐 ${report?.referral.totalRelations ?? 0} · 达标 ${report?.referral.qualifiedRelations ?? 0}`}</Text>
          {referralCodes.length === 0 && referralRelations.length === 0 ? (
            <Empty description="暂无推荐关系" />
          ) : (
            <>
              {referralCodes.map(code => (
                <Card key={code.id} size="small">
                  <Space direction="vertical" size={6}>
                    <Space wrap>
                      <Text strong>{`推荐码 ${code.code}`}</Text>
                      <Tag color={statusColor(code.status)}>{code.status}</Tag>
                    </Space>
                    <Text type="secondary">{`推荐人 ${code.participantId}`}</Text>
                  </Space>
                </Card>
              ))}
              {referralRelations.map(relation => (
                <Card key={relation.id} size="small">
                  <Space direction="vertical" size={6}>
                    <Space wrap>
                      <Text strong>{relation.inviteeUserId}</Text>
                      <Tag color={statusColor(relation.status)}>{relation.status}</Tag>
                    </Space>
                    <Text type="secondary">{`风险 ${riskEvidenceText(relation.riskEvidence)}`}</Text>
                    <Text type="secondary">
                      {`邀请人奖励 ${relation.inviterRewardGrantId ?? '-'} · 被邀请人奖励 ${relation.inviteeRewardGrantId ?? '-'}`}
                    </Text>
                  </Space>
                </Card>
              ))}
            </>
          )}
        </Space>
      ),
    },
    {
      key: 'tasks',
      label: '任务进度',
      children: (
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Text>
            {`任务定义 ${taskDefinitions.length} · 任务进度 ${report?.task.totalProgress ?? 0} · 完成 ${report?.task.completedProgress ?? 0}`}
          </Text>
          {taskDefinitions.length === 0 && taskProgress.length === 0 ? (
            <Empty description="暂无任务进度" />
          ) : (
            <>
              {taskDefinitions.map(task => (
                <Card key={task.id} size="small">
                  <Space direction="vertical" size={6}>
                    <Space wrap>
                      <Text strong>{task.taskKey}</Text>
                      <Tag color={statusColor(task.status)}>{task.status}</Tag>
                      <Text>{task.taskType}</Text>
                    </Space>
                    <Text type="secondary">
                      {`完成策略 ${task.completionPolicy} · 重置 ${task.resetPolicy} · 目标 ${formatNumber(task.targetValue)}`}
                    </Text>
                    <Text type="secondary">{`奖励池 ${task.rewardPoolId ?? '-'}`}</Text>
                  </Space>
                </Card>
              ))}
              {taskProgress.map(progress => (
                <Card key={progress.id} size="small">
                  <Space direction="vertical" size={6}>
                    <Space wrap>
                      <Text strong>{`参与者 ${progress.participantId}`}</Text>
                      <Tag color={statusColor(progress.status)}>{progress.status}</Tag>
                      <Text>{`任务 ${progress.taskId}`}</Text>
                    </Space>
                    <Text type="secondary">
                      {`进度 ${formatNumber(progress.progressValue)} / ${formatNumber(progress.targetValue)} · 事件 ${progress.lastEventKey ?? '-'}`}
                    </Text>
                    <Text type="secondary">{`证据 ${taskEvidenceText(progress.evidence)}`}</Text>
                    <Text type="secondary">{`奖励发放 ${progress.rewardGrantId ?? '-'}`}</Text>
                  </Space>
                </Card>
              ))}
            </>
          )}
        </Space>
      ),
    },
    {
      key: 'events',
      label: '事件时间线',
      children: <Text>生命周期、参与、推荐、任务、发放和转化事件将在此串联。</Text>,
    },
    {
      key: 'report',
      label: '报表',
      children: (
        <Space direction="vertical" size={10}>
          <Text>
            {`活动漏斗 参与 ${participationTotal} · 活跃 ${report?.participation.activeParticipants ?? 0} · 转化 ${report?.conversion.conversionCount ?? 0}`}
          </Text>
          <Text>
            {`转化金额 ${formatMoney(report?.conversion.conversionAmount)} · 成本 ${formatMoney(report?.grants.totalCost)} · ROI ${roi}`}
          </Text>
          <Text>
            {`推荐漏斗 关系 ${report?.referral.totalRelations ?? 0} · 达标 ${report?.referral.qualifiedRelations ?? 0} · 待定 ${report?.referral.pendingRelations ?? 0} · 拒绝 ${report?.referral.rejectedRelations ?? 0}`}
          </Text>
          <Text>
            {`任务完成 ${report?.task.completedProgress ?? 0} / ${report?.task.totalProgress ?? 0} · 完成率 ${formatPercent(report?.task.completionRate)}`}
          </Text>
          <Text>
            {`兑换 ${report?.grants.redeemedGrants ?? 0} · 过期 ${report?.grants.expiredGrants ?? 0} · 已发 ${report?.grants.successGrants ?? 0}`}
          </Text>
          <Text>{`仪表盘 ${selectedActivity?.dashboardRef ?? '-'}`}</Text>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Space align="start" style={{ justifyContent: 'space-between', width: '100%' }}>
          <div>
            <Title level={2} style={{ marginBottom: 4 }}>增长活动中心</Title>
            <Text type="secondary">活动、奖励池、发放、任务和转化闭环</Text>
          </div>
          <Space>
            <Button aria-label="新建活动" type="primary" onClick={openCreateWizard}>新建活动</Button>
            <Button aria-label="刷新" icon={<ReloadOutlined />} onClick={() => loadActivities({ limit: 50 })}>
              刷新
            </Button>
          </Space>
        </Space>

        {wizardOpen && (
          <Card size="small" title={wizardMode === 'create' ? '活动向导 · 新建' : '活动向导 · 编辑'}>
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space wrap>
                <label>
                  <Text style={{ marginRight: 8 }}>活动预设</Text>
                  <select
                    aria-label="活动预设"
                    value={wizard.activityType}
                    onChange={event => applyActivityPreset(event.target.value)}
                    style={{ minWidth: 220 }}
                  >
                    {Object.keys(ACTIVITY_PRESETS).map(preset => <option key={preset} value={preset}>{preset}</option>)}
                  </select>
                </label>
                <label>
                  <Text style={{ marginRight: 8 }}>活动 Key</Text>
                  <input
                    aria-label="活动 Key"
                    value={wizard.activityKey}
                    onChange={event => setWizard(current => ({ ...current, activityKey: event.target.value }))}
                    style={{ minWidth: 180 }}
                  />
                </label>
                <label>
                  <Text style={{ marginRight: 8 }}>活动名称</Text>
                  <input
                    aria-label="活动名称"
                    value={wizard.activityName}
                    onChange={event => setWizard(current => ({ ...current, activityName: event.target.value }))}
                    style={{ minWidth: 180 }}
                  />
                </label>
                <label>
                  <Text style={{ marginRight: 8 }}>Campaign master</Text>
                  <select
                    aria-label="Campaign master"
                    value={wizard.campaignId}
                    onChange={event => setWizard(current => ({ ...current, campaignId: event.target.value }))}
                    style={{ minWidth: 220 }}
                  >
                    <option value="">未关联</option>
                    {campaignOptions.map(option => (
                      <option key={option.campaignId} value={option.campaignId}>{option.label}</option>
                    ))}
                  </select>
                </label>
                <label>
                  <Text style={{ marginRight: 8 }}>负责人团队</Text>
                  <input
                    aria-label="负责人团队"
                    value={wizard.ownerTeam}
                    onChange={event => setWizard(current => ({ ...current, ownerTeam: event.target.value }))}
                    style={{ minWidth: 160 }}
                  />
                </label>
              </Space>
              <Space wrap>
                <Tag>{wizard.activityType}</Tag>
                <Tag color="blue">{wizard.objective || '-'}</Tag>
                <Tag>{wizard.channelScope || '-'}</Tag>
              </Space>
              <Space>
                <Button aria-label="保存活动" type="primary" onClick={() => void saveActivity()}>保存活动</Button>
                <Button aria-label="取消向导" onClick={() => setWizardOpen(false)}>取消</Button>
              </Space>
            </Space>
          </Card>
        )}

        <Card size="small">
          <Space wrap>
            <label>
              <Text style={{ marginRight: 8 }}>活动类型</Text>
              <select
                aria-label="活动类型"
                value={activityType}
                onChange={event => setActivityType(event.target.value)}
                style={{ minWidth: 220 }}
              >
                <option value="">全部类型</option>
                {ACTIVITY_TYPES.map(type => <option key={type} value={type}>{type}</option>)}
              </select>
            </label>
            <label>
              <Text style={{ marginRight: 8 }}>活动状态</Text>
              <select
                aria-label="活动状态"
                value={status}
                onChange={event => setStatus(event.target.value)}
                style={{ minWidth: 140 }}
              >
                <option value="">全部状态</option>
                {STATUSES.map(item => <option key={item} value={item}>{item}</option>)}
              </select>
            </label>
            <label>
              <Text style={{ marginRight: 8 }}>活动 ID</Text>
              <input
                aria-label="活动 ID"
                inputMode="numeric"
                value={campaignId}
                onChange={event => setCampaignId(event.target.value)}
                placeholder="Campaign ID"
                style={{ minWidth: 120 }}
              />
            </label>
            <label>
              <Text style={{ marginRight: 8 }}>负责人</Text>
              <input
                aria-label="负责人"
                value={ownerTeam}
                onChange={event => setOwnerTeam(event.target.value)}
                placeholder="Owner team"
                style={{ minWidth: 140 }}
              />
            </label>
            <label>
              <Text style={{ marginRight: 8 }}>上线状态</Text>
              <select
                aria-label="上线状态"
                value={readinessStatus}
                onChange={event => setReadinessStatus(event.target.value)}
                style={{ minWidth: 140 }}
              >
                <option value="">全部上线状态</option>
                {READINESS_STATUSES.map(item => <option key={item} value={item}>{item}</option>)}
              </select>
            </label>
            <label>
              <Text style={{ marginRight: 8 }}>排期</Text>
              <select
                aria-label="排期"
                value={scheduleStatus}
                onChange={event => setScheduleStatus(event.target.value)}
                style={{ minWidth: 150 }}
              >
                <option value="">全部排期</option>
                {SCHEDULE_STATUSES.map(item => <option key={item} value={item}>{item}</option>)}
              </select>
            </label>
            <label>
              <Text style={{ marginRight: 8 }}>发放健康</Text>
              <select
                aria-label="发放健康"
                value={grantHealth}
                onChange={event => setGrantHealth(event.target.value)}
                style={{ minWidth: 170 }}
              >
                <option value="">全部发放健康</option>
                {GRANT_HEALTH_STATUSES.map(item => <option key={item} value={item}>{item}</option>)}
              </select>
            </label>
            <Button aria-label="筛选" type="primary" onClick={applyFilters}>筛选</Button>
          </Space>
        </Card>

        {loadError && (
          <Alert
            type="error"
            message="增长活动加载失败"
            description={loadError}
            action={<Button aria-label="重试" size="small" onClick={() => loadActivities({ limit: 50 })}>重试</Button>}
          />
        )}

        <Spin spinning={loading}>
          {activities.length === 0 && !loading ? (
            <Empty description="暂无增长活动" />
          ) : (
            <Row gutter={[16, 16]}>
              <Col xs={24} lg={14}>
                <Space direction="vertical" size={12} style={{ width: '100%' }}>
                  {activities.map(activity => (
                    <Card
                      key={activity.id}
                      size="small"
                      onClick={() => setSelectedActivityId(activity.id)}
                      style={{
                        cursor: 'pointer',
                        borderColor: selectedActivity?.id === activity.id ? '#1677ff' : undefined,
                      }}
                    >
                      <Space direction="vertical" size={8} style={{ width: '100%' }}>
                        <Space style={{ justifyContent: 'space-between', width: '100%' }}>
                          <Text strong>{activity.activityName}</Text>
                          <Tag color={statusColor(activity.status)}>{activity.status}</Tag>
                        </Space>
                        <Space wrap>
                          <Tag>{activity.activityType}</Tag>
                          {activity.objective && <Tag color="blue">{activity.objective}</Tag>}
                          {activity.ownerTeam && <Text type="secondary">{activity.ownerTeam}</Text>}
                        </Space>
                        <Text type="secondary">
                          Campaign {activity.campaignId ?? '-'} · {activity.channelScope ?? 'ALL'}
                        </Text>
                      </Space>
                    </Card>
                  ))}
                </Space>
              </Col>

              <Col xs={24} lg={10}>
                <Spin spinning={detailLoading}>
                  <Card size="small" title={selectedActivity?.activityName ?? '活动详情'}>
                    <Button aria-label="编辑活动" size="small" onClick={openEditWizard} style={{ marginBottom: 12 }}>
                      编辑活动
                    </Button>
                    <div role="tablist" aria-label="增长活动详情" style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 12 }}>
                      {detailTabs.map(tab => (
                        <Button
                          key={tab.key}
                          role="tab"
                          size="small"
                          type={activeDetailTab === tab.key ? 'primary' : 'default'}
                          aria-label={tab.label}
                          aria-selected={activeDetailTab === tab.key}
                          onClick={() => setActiveDetailTab(tab.key)}
                        >
                          {tab.label}
                        </Button>
                      ))}
                    </div>
                    <Tabs
                      activeKey={activeDetailTab}
                      onChange={setActiveDetailTab}
                      items={detailTabs}
                      tabBarStyle={{ display: 'none' }}
                    />
                  </Card>
                </Spin>
              </Col>
            </Row>
          )}
        </Spin>
      </Space>
    </div>
  )
}

function statusColor(status?: string | null) {
  if (status === 'ACTIVE') return 'green'
  if (status === 'DRAFT') return 'default'
  if (status === 'PAUSED') return 'orange'
  if (status === 'CLOSED' || status === 'ARCHIVED') return 'red'
  return 'blue'
}

function formatNumber(value: number | string | undefined) {
  if (value === undefined || value === null) return 0
  const numberValue = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(numberValue)) return String(value)
  return Number.isInteger(numberValue) ? numberValue : numberValue.toFixed(2)
}

function numberValue(value: number | string | null | undefined) {
  if (value === undefined || value === null) return 0
  const number = typeof value === 'number' ? value : Number(value)
  return Number.isNaN(number) ? 0 : number
}

function formatMoney(value: number | string | null | undefined) {
  const number = numberValue(value)
  return number.toFixed(2)
}

function formatPercent(value: number | string | null | undefined) {
  return `${(numberValue(value) * 100).toFixed(2)}%`
}

function riskEvidenceText(riskEvidence: Record<string, unknown> | undefined) {
  const riskDecision = typeof riskEvidence?.riskDecision === 'string' ? riskEvidence.riskDecision : '-'
  const ipRisk = typeof riskEvidence?.ipRisk === 'string' ? riskEvidence.ipRisk : '-'
  return `${riskDecision} · IP ${ipRisk}`
}

function taskEvidenceText(evidence: Record<string, unknown> | undefined) {
  const eventName = typeof evidence?.eventName === 'string' ? evidence.eventName : '-'
  const source = typeof evidence?.source === 'string' ? evidence.source : '-'
  return `${eventName} · 来源 ${source}`
}

function providerEvidenceText(providerResponse: Record<string, unknown> | undefined) {
  const provider = typeof providerResponse?.provider === 'string' ? providerResponse.provider : '-'
  const referenceId = typeof providerResponse?.referenceId === 'string' ? providerResponse.referenceId : '-'
  return `${provider} · 单号 ${referenceId}`
}

function providerErrorText(providerResponse: Record<string, unknown> | undefined) {
  return typeof providerResponse?.errorCode === 'string' ? providerResponse.errorCode : '-'
}

function emptyWizard(presetKey: string): ActivityWizardState {
  const preset = ACTIVITY_PRESETS[presetKey] ?? ACTIVITY_PRESETS.REFERRAL_INVITE
  return {
    activityKey: '',
    activityName: '',
    activityType: preset.activityType,
    status: 'DRAFT',
    campaignId: '',
    objective: preset.objective ?? '',
    ownerTeam: '',
    channelScope: preset.channelScope ?? '',
    dashboardRef: '',
  }
}
