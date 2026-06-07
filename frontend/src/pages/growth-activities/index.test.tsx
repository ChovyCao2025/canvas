/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import GrowthActivitiesPage from './index'
import { growthActivityApi } from '../../services/growthActivityApi'

vi.mock('../../services/growthActivityApi', () => ({
  growthActivityApi: {
    listActivities: vi.fn(),
    upsertActivity: vi.fn(),
    getReadiness: vi.fn(),
    getReport: vi.fn(),
    listRewardPools: vi.fn(),
    listGrants: vi.fn(),
    retryGrant: vi.fn(),
    reconcileGrant: vi.fn(),
    cancelGrant: vi.fn(),
    listReferralCodes: vi.fn(),
    listReferralRelations: vi.fn(),
    listTaskDefinitions: vi.fn(),
    listTaskProgress: vi.fn(),
  },
}))

describe('GrowthActivitiesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(growthActivityApi.listActivities).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [referralActivity, taskActivity],
    })
    vi.mocked(growthActivityApi.upsertActivity).mockResolvedValue({
      code: 0,
      message: 'success',
      data: referralActivity,
    })
    vi.mocked(growthActivityApi.getReadiness).mockResolvedValue({
      code: 0,
      message: 'success',
      data: readiness,
    })
    vi.mocked(growthActivityApi.getReport).mockResolvedValue({
      code: 0,
      message: 'success',
      data: report,
    })
    vi.mocked(growthActivityApi.listRewardPools).mockResolvedValue({
      code: 0,
      message: 'success',
      data: rewardPools,
    })
    vi.mocked(growthActivityApi.listGrants).mockResolvedValue({
      code: 0,
      message: 'success',
      data: grants,
    })
    vi.mocked(growthActivityApi.retryGrant).mockResolvedValue({ code: 0, message: 'success', data: {} })
    vi.mocked(growthActivityApi.reconcileGrant).mockResolvedValue({ code: 0, message: 'success', data: {} })
    vi.mocked(growthActivityApi.cancelGrant).mockResolvedValue({ code: 0, message: 'success', data: {} })
    vi.mocked(growthActivityApi.listReferralCodes).mockResolvedValue({
      code: 0,
      message: 'success',
      data: referralCodes,
    })
    vi.mocked(growthActivityApi.listReferralRelations).mockResolvedValue({
      code: 0,
      message: 'success',
      data: referralRelations,
    })
    vi.mocked(growthActivityApi.listTaskDefinitions).mockResolvedValue({
      code: 0,
      message: 'success',
      data: taskDefinitions,
    })
    vi.mocked(growthActivityApi.listTaskProgress).mockResolvedValue({
      code: 0,
      message: 'success',
      data: taskProgress,
    })
  })

  it('renders activities with readiness and report summaries', async () => {
    render(<GrowthActivitiesPage />)

    expect(screen.getByRole('heading', { name: '增长活动中心' })).toBeInTheDocument()

    await waitFor(() => expect(growthActivityApi.listActivities).toHaveBeenCalledWith({ limit: 50 }))
    expect(screen.getAllByText('Invite spring').length).toBeGreaterThan(0)
    expect(screen.getByText('Daily task incentive')).toBeInTheDocument()
    expect(screen.getAllByText('REFERRAL_INVITE').length).toBeGreaterThan(0)
    expect(screen.getAllByText('TASK_INCENTIVE').length).toBeGreaterThan(0)
    expect(await screen.findByText('READY')).toBeInTheDocument()
    expect(await screen.findByRole('group', { name: '参与 120' })).toBeInTheDocument()
    expect(screen.getByRole('group', { name: '发放 50' })).toBeInTheDocument()
    expect(screen.getByRole('group', { name: 'ROI 4.20' })).toBeInTheDocument()
    expect(screen.getByText('Campaign master is linked')).toBeInTheDocument()
  })

  it('filters activities by type and status', async () => {
    render(<GrowthActivitiesPage />)

    await screen.findAllByText('Invite spring')
    fireEvent.change(screen.getByLabelText('活动类型'), { target: { value: 'REFERRAL_INVITE' } })
    fireEvent.change(screen.getByLabelText('活动状态'), { target: { value: 'ACTIVE' } })
    fireEvent.change(screen.getByLabelText('活动 ID'), { target: { value: '100' } })
    fireEvent.change(screen.getByLabelText('负责人'), { target: { value: 'Growth' } })
    fireEvent.change(screen.getByLabelText('上线状态'), { target: { value: 'READY' } })
    fireEvent.change(screen.getByLabelText('排期'), { target: { value: 'RUNNING' } })
    fireEvent.change(screen.getByLabelText('发放健康'), { target: { value: 'HEALTHY' } })
    fireEvent.click(screen.getByRole('button', { name: '筛选' }))

    await waitFor(() => expect(growthActivityApi.listActivities).toHaveBeenLastCalledWith({
      activityType: 'REFERRAL_INVITE',
      status: 'ACTIVE',
      campaignId: 100,
      ownerTeam: 'Growth',
      readinessStatus: 'READY',
      scheduleStatus: 'RUNNING',
      grantHealth: 'HEALTHY',
      limit: 50,
    }))
  })

  it('creates and edits activities with type presets and campaign master selection', async () => {
    render(<GrowthActivitiesPage />)

    await screen.findAllByText('Invite spring')

    fireEvent.click(screen.getByRole('button', { name: '新建活动' }))
    fireEvent.change(screen.getByLabelText('活动预设'), { target: { value: 'TASK_INCENTIVE' } })
    fireEvent.change(screen.getByLabelText('活动 Key'), { target: { value: 'daily-checkin' } })
    fireEvent.change(screen.getByLabelText('活动名称'), { target: { value: 'Daily checkin' } })
    fireEvent.change(screen.getByLabelText('Campaign master'), { target: { value: '100' } })
    fireEvent.change(screen.getByLabelText('负责人团队'), { target: { value: 'Growth Ops' } })
    fireEvent.click(screen.getByRole('button', { name: '保存活动' }))

    await waitFor(() => expect(growthActivityApi.upsertActivity).toHaveBeenCalledWith(expect.objectContaining({
      activityKey: 'daily-checkin',
      activityName: 'Daily checkin',
      activityType: 'TASK_INCENTIVE',
      status: 'DRAFT',
      campaignId: 100,
      objective: 'ENGAGEMENT',
      ownerTeam: 'Growth Ops',
      channelScope: 'APP',
    })))

    fireEvent.click(screen.getByRole('button', { name: '编辑活动' }))
    fireEvent.change(screen.getByLabelText('活动名称'), { target: { value: 'Invite spring updated' } })
    fireEvent.click(screen.getByRole('button', { name: '保存活动' }))

    await waitFor(() => expect(growthActivityApi.upsertActivity).toHaveBeenLastCalledWith(expect.objectContaining({
      activityKey: 'invite-spring',
      activityName: 'Invite spring updated',
      activityType: 'REFERRAL_INVITE',
      campaignId: 100,
    })))
  })

  it('renders activity detail tabs for linked resources and closed-loop panels', async () => {
    render(<GrowthActivitiesPage />)

    await screen.findAllByText('Invite spring')

    expect(screen.getByRole('tab', { name: '概览' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '上线闸口' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '关联资源' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '奖励池' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '参与者' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '发放流水' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '推荐关系' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '任务进度' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '事件时间线' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '报表' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '关联资源' }))
    expect(screen.getByText('Campaign 100')).toBeInTheDocument()
    expect(screen.getByText('风险策略 -')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '报表' }))
    expect(await screen.findByText('活动漏斗 参与 120 · 活跃 100 · 转化 20')).toBeInTheDocument()
    expect(screen.getByText('转化金额 420.00 · 成本 100.00 · ROI 4.20')).toBeInTheDocument()
  })

  it('loads and renders reward pool budget, inventory, status, and cost counters', async () => {
    render(<GrowthActivitiesPage />)

    await waitFor(() => expect(growthActivityApi.listRewardPools).toHaveBeenCalledWith(10))

    fireEvent.click(screen.getByRole('tab', { name: '奖励池' }))

    expect(screen.getByText('coupon-pool')).toBeInTheDocument()
    expect(screen.getByText('COUPON')).toBeInTheDocument()
    expect(screen.getAllByText('ACTIVE').length).toBeGreaterThan(0)
    expect(screen.getByText('低库存')).toBeInTheDocument()
    expect(screen.getByText('预算 1000.00 CNY')).toBeInTheDocument()
    expect(screen.getByText('库存 100 · 预留 10 · 已发 40')).toBeInTheDocument()
    expect(screen.getByText('失败 1 · 取消 0 · 兑换 14 · 过期 0')).toBeInTheDocument()
    expect(screen.getByText('成本 550.00 CNY')).toBeInTheDocument()
  })

  it('loads and renders grant ledger idempotency, provider evidence, actions, and error state', async () => {
    render(<GrowthActivitiesPage />)

    await waitFor(() => expect(growthActivityApi.listGrants).toHaveBeenCalledWith(10))

    fireEvent.click(screen.getByRole('tab', { name: '发放流水' }))

    expect(screen.getByText('流水 300')).toBeInTheDocument()
    expect(screen.getByText('TASK_COMPLETION')).toBeInTheDocument()
    expect(screen.getByText('幂等键 task:900:completion · 渠道池 20 · 成本 5.00')).toBeInTheDocument()
    expect(screen.getByText('参与者 200 · 推荐 - · 任务 900')).toBeInTheDocument()
    expect(screen.getByText('供应商证据 coupon-provider · 单号 grant-300')).toBeInTheDocument()
    expect(screen.getByText('错误 PROVIDER_TIMEOUT')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /重\s*试/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /对\s*账/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /取\s*消/ })).toBeInTheDocument()
  })

  it('loads and renders referral codes, qualification state, risk evidence, and both-side rewards', async () => {
    render(<GrowthActivitiesPage />)

    await waitFor(() => expect(growthActivityApi.listReferralCodes).toHaveBeenCalledWith(10))
    await waitFor(() => expect(growthActivityApi.listReferralRelations).toHaveBeenCalledWith(10))

    fireEvent.click(screen.getByRole('tab', { name: '推荐关系' }))

    expect(screen.getByText('推荐码 G10P200')).toBeInTheDocument()
    expect(screen.getByText('推荐人 200')).toBeInTheDocument()
    expect(screen.getByText('invitee-1')).toBeInTheDocument()
    expect(screen.getByText('QUALIFIED')).toBeInTheDocument()
    expect(screen.getByText('风险 PASS · IP LOW')).toBeInTheDocument()
    expect(screen.getByText('邀请人奖励 900 · 被邀请人奖励 901')).toBeInTheDocument()
  })

  it('loads and renders task definitions, progress evidence, and reward grant links', async () => {
    render(<GrowthActivitiesPage />)

    await waitFor(() => expect(growthActivityApi.listTaskDefinitions).toHaveBeenCalledWith(10))
    await waitFor(() => expect(growthActivityApi.listTaskProgress).toHaveBeenCalledWith(10))

    fireEvent.click(screen.getByRole('tab', { name: '任务进度' }))

    expect(screen.getByText('daily-login')).toBeInTheDocument()
    expect(screen.getByText('EVENT_COUNT')).toBeInTheDocument()
    expect(screen.getByText('完成策略 EVENT · 重置 DAILY · 目标 3')).toBeInTheDocument()
    expect(screen.getByText('奖励池 20')).toBeInTheDocument()
    expect(screen.getByText('参与者 200')).toBeInTheDocument()
    expect(screen.getByText('COMPLETED')).toBeInTheDocument()
    expect(screen.getByText('进度 3 / 3 · 事件 login:2026-06-07')).toBeInTheDocument()
    expect(screen.getByText('证据 login · 来源 app')).toBeInTheDocument()
    expect(screen.getByText('奖励发放 902')).toBeInTheDocument()
  })

  it('renders report funnel, conversion, cost, referral, task, redemption, and dashboard link', async () => {
    render(<GrowthActivitiesPage />)

    await waitFor(() => expect(growthActivityApi.getReport).toHaveBeenCalledWith(10))

    fireEvent.click(screen.getByRole('tab', { name: '报表' }))

    expect(screen.getByText('活动漏斗 参与 120 · 活跃 100 · 转化 20')).toBeInTheDocument()
    expect(screen.getByText('转化金额 420.00 · 成本 100.00 · ROI 4.20')).toBeInTheDocument()
    expect(screen.getByText('推荐漏斗 关系 80 · 达标 40 · 待定 20 · 拒绝 2')).toBeInTheDocument()
    expect(screen.getByText('任务完成 8 / 12 · 完成率 66.67%')).toBeInTheDocument()
    expect(screen.getByText('兑换 14 · 过期 0 · 已发 30')).toBeInTheDocument()
    expect(screen.getByText('仪表盘 bi-dashboard-1')).toBeInTheDocument()
  })

  it('shows retry state when loading fails', async () => {
    vi.mocked(growthActivityApi.listActivities)
      .mockRejectedValueOnce(new Error('network down'))
      .mockResolvedValueOnce({ code: 0, message: 'success', data: [] })

    render(<GrowthActivitiesPage />)

    await screen.findByText('增长活动加载失败')
    fireEvent.click(screen.getByRole('button', { name: '重试' }))

    await waitFor(() => expect(growthActivityApi.listActivities).toHaveBeenCalledTimes(2))
    expect(screen.getByText('暂无增长活动')).toBeInTheDocument()
  })
})

const referralActivity = {
  id: 10,
  tenantId: 7,
  activityKey: 'invite-spring',
  activityName: 'Invite spring',
  activityType: 'REFERRAL_INVITE',
  status: 'ACTIVE',
  campaignId: 100,
  objective: 'ACQUISITION',
  ownerTeam: 'Growth',
  channelScope: 'PRIVATE_DOMAIN',
  audienceRefs: { segmentIds: ['seg-1'] },
  dashboardRef: 'bi-dashboard-1',
  metadata: {},
}

const taskActivity = {
  id: 11,
  tenantId: 7,
  activityKey: 'daily-task',
  activityName: 'Daily task incentive',
  activityType: 'TASK_INCENTIVE',
  status: 'DRAFT',
  campaignId: 101,
  objective: 'ENGAGEMENT',
  ownerTeam: 'Growth',
  channelScope: 'APP',
  audienceRefs: {},
  metadata: {},
}

const readiness = {
  tenantId: 7,
  activityId: 10,
  activityKey: 'invite-spring',
  activityType: 'REFERRAL_INVITE',
  generatedAt: '2026-06-07T15:00:00',
  status: 'READY',
  productionReady: true,
  blockerCount: 0,
  warningCount: 0,
  blockers: [],
  warnings: [],
  checks: [{
    severity: 'PASS',
    itemType: 'CAMPAIGN_MASTER',
    itemKey: 'campaign-master',
    title: 'Campaign master is linked',
    reason: 'ok',
  }],
}

const report = {
  tenantId: 7,
  activityId: 10,
  participation: { totalParticipants: 120, activeParticipants: 100 },
  referral: { totalRelations: 80, qualifiedRelations: 40, pendingRelations: 20, rejectedRelations: 2 },
  grants: {
    totalGrants: 50,
    reservedGrants: 5,
    successGrants: 30,
    failedGrants: 1,
    canceledGrants: 0,
    redeemedGrants: 14,
    expiredGrants: 0,
    totalCost: '100.00',
  },
  conversion: { conversionCount: 20, conversionAmount: '420.00', roi: '4.20' },
  task: { totalProgress: 12, completedProgress: 8, completionRate: '0.6667' },
}

const rewardPools = [{
  id: 20,
  tenantId: 7,
  activityId: 10,
  poolKey: 'coupon-pool',
  rewardType: 'COUPON',
  grantChannel: 'COMMIT_ACTION',
  couponTypeKey: 'spring-coupon',
  loyaltyRewardKey: null,
  pointsType: null,
  externalContractKey: 'coupon-contract',
  inventoryMode: 'FINITE',
  totalInventory: 100,
  reservedInventory: 10,
  grantedInventory: 40,
  perUserLimit: 1,
  perReferralLimit: 1,
  budgetAmount: '1000.00',
  reservedAmount: '150.00',
  grantedAmount: '400.00',
  costCurrency: 'CNY',
  status: 'ACTIVE',
  inventoryLow: true,
  metadata: {},
}]

const grants = [{
  id: 300,
  tenantId: 7,
  activityId: 10,
  poolId: 20,
  participantId: 200,
  referralRelationId: null,
  taskProgressId: 900,
  grantReason: 'TASK_COMPLETION',
  status: 'FAILED',
  idempotencyKey: 'task:900:completion',
  providerRequest: { channel: 'coupon' },
  providerResponse: { provider: 'coupon-provider', referenceId: 'grant-300', errorCode: 'PROVIDER_TIMEOUT' },
  costAmount: '5.00',
  createdBy: 'operator-1',
  updatedBy: 'operator-1',
}]

const referralCodes = [{
  id: 500,
  tenantId: 7,
  activityId: 10,
  participantId: 200,
  code: 'G10P200',
  status: 'ACTIVE',
  createdBy: 'operator-1',
}]

const referralRelations = [{
  id: 700,
  tenantId: 7,
  activityId: 10,
  referralCodeId: 500,
  referrerParticipantId: 200,
  inviteeUserId: 'invitee-1',
  status: 'QUALIFIED',
  riskEvidence: { riskDecision: 'PASS', ipRisk: 'LOW' },
  inviterRewardGrantId: 900,
  inviteeRewardGrantId: 901,
  createdBy: 'operator-1',
  updatedBy: 'operator-1',
}]

const taskDefinitions = [{
  id: 800,
  tenantId: 7,
  activityId: 10,
  taskKey: 'daily-login',
  taskType: 'EVENT_COUNT',
  completionPolicy: 'EVENT',
  resetPolicy: 'DAILY',
  rewardPoolId: 20,
  targetValue: '3',
  status: 'ACTIVE',
  rule: { eventName: 'login' },
  createdBy: 'operator-1',
  updatedBy: 'operator-1',
}]

const taskProgress = [{
  id: 900,
  tenantId: 7,
  activityId: 10,
  participantId: 200,
  taskId: 800,
  progressValue: '3',
  targetValue: '3',
  status: 'COMPLETED',
  lastEventKey: 'login:2026-06-07',
  evidence: { eventName: 'login', source: 'app' },
  rewardGrantId: 902,
  updatedBy: 'operator-1',
}]
