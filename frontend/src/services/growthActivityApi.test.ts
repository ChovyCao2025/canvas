import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import { growthActivityApi } from './growthActivityApi'

describe('growthActivityApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('calls growth activity activity, readiness, and report endpoints', async () => {
    const response = { code: 0, message: 'success', data: [] }
    const get = vi.spyOn(http, 'get').mockResolvedValue(response)
    const post = vi.spyOn(http, 'post').mockResolvedValue(response)
    const activity = {
      activityKey: 'invite-spring',
      activityName: 'Invite spring',
      activityType: 'REFERRAL_INVITE',
      status: 'DRAFT',
      campaignId: 10,
      metadata: {},
    }

    await expect(growthActivityApi.listActivities({
      activityType: 'REFERRAL_INVITE',
      status: 'ACTIVE',
      campaignId: 10,
      ownerTeam: 'Growth',
      readinessStatus: 'READY',
      scheduleStatus: 'RUNNING',
      grantHealth: 'HEALTHY',
      limit: 50,
    }))
      .resolves.toBe(response)
    await expect(growthActivityApi.upsertActivity(activity)).resolves.toBe(response)
    await expect(growthActivityApi.getActivity(10)).resolves.toBe(response)
    await expect(growthActivityApi.getReadiness(10)).resolves.toBe(response)
    await expect(growthActivityApi.getReport(10)).resolves.toBe(response)
    await expect(growthActivityApi.publishActivity(10)).resolves.toBe(response)
    await expect(growthActivityApi.pauseActivity(10)).resolves.toBe(response)
    await expect(growthActivityApi.closeActivity(10)).resolves.toBe(response)

    expect(get).toHaveBeenCalledWith('/canvas/growth-activities', {
      params: {
        activityType: 'REFERRAL_INVITE',
        status: 'ACTIVE',
        campaignId: 10,
        ownerTeam: 'Growth',
        readinessStatus: 'READY',
        scheduleStatus: 'RUNNING',
        grantHealth: 'HEALTHY',
        limit: 50,
      },
    })
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities', activity)
    expect(get).toHaveBeenCalledWith('/canvas/growth-activities/10')
    expect(get).toHaveBeenCalledWith('/canvas/growth-activities/10/readiness')
    expect(get).toHaveBeenCalledWith('/canvas/growth-activities/10/report')
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/publish', {})
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/pause', {})
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/close', {})
  })

  it('calls growth activity sub-resource endpoints', async () => {
    const response = { code: 0, message: 'success', data: [] }
    const get = vi.spyOn(http, 'get').mockResolvedValue(response)
    const post = vi.spyOn(http, 'post').mockResolvedValue(response)
    const pool = { poolKey: 'coupon-pool', rewardType: 'COUPON', grantChannel: 'COMMIT_ACTION' }
    const grant = { poolId: 20, grantReason: 'QUALIFIED_REFERRAL', idempotencyKey: 'idem-1' }
    const event = { eventType: 'CONVERSION_EVIDENCE', eventKey: 'conversion:order-1', payload: { amount: 99 } }
    const task = { taskKey: 'daily-login', taskType: 'EVENT', targetValue: 1 }
    const progress = { participantId: 30, taskId: 40, eventKey: 'event-1', incrementValue: 1 }
    const referral = { referralCode: 'G10P200', inviteeUserId: 'u-2' }

    await expect(growthActivityApi.listRewardPools(10)).resolves.toBe(response)
    await expect(growthActivityApi.upsertRewardPool(10, pool)).resolves.toBe(response)
    await expect(growthActivityApi.listGrants(10)).resolves.toBe(response)
    await expect(growthActivityApi.createGrant(10, grant)).resolves.toBe(response)
    await expect(growthActivityApi.retryGrant(10, 70)).resolves.toBe(response)
    await expect(growthActivityApi.reconcileGrant(10, 70, { providerStatus: 'SUCCESS' })).resolves.toBe(response)
    await expect(growthActivityApi.cancelGrant(10, 70)).resolves.toBe(response)
    await expect(growthActivityApi.listEvents(10, { eventType: 'TASK_PROGRESS', limit: 20 })).resolves.toBe(response)
    await expect(growthActivityApi.recordEvent(10, event)).resolves.toBe(response)
    await expect(growthActivityApi.listReferralCodes(10)).resolves.toBe(response)
    await expect(growthActivityApi.listReferralRelations(10)).resolves.toBe(response)
    await expect(growthActivityApi.generateReferralCode(10, 30)).resolves.toBe(response)
    await expect(growthActivityApi.upsertReferralRelation(10, referral)).resolves.toBe(response)
    await expect(growthActivityApi.qualifyReferral(10, 60, { qualified: true })).resolves.toBe(response)
    await expect(growthActivityApi.listTaskDefinitions(10)).resolves.toBe(response)
    await expect(growthActivityApi.upsertTaskDefinition(10, task)).resolves.toBe(response)
    await expect(growthActivityApi.listTaskProgress(10)).resolves.toBe(response)
    await expect(growthActivityApi.recordTaskProgress(10, progress)).resolves.toBe(response)

    expect(get).toHaveBeenCalledWith('/canvas/growth-activities/10/reward-pools')
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/reward-pools', pool)
    expect(get).toHaveBeenCalledWith('/canvas/growth-activities/10/grants')
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/grants', grant)
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/grants/70/retry', {})
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/grants/70/reconcile', { providerStatus: 'SUCCESS' })
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/grants/70/cancel', {})
    expect(get).toHaveBeenCalledWith('/canvas/growth-activities/10/events', {
      params: { eventType: 'TASK_PROGRESS', limit: 20 },
    })
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/events', event)
    expect(get).toHaveBeenCalledWith('/canvas/growth-activities/10/referral-codes')
    expect(get).toHaveBeenCalledWith('/canvas/growth-activities/10/referrals')
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/referral-codes', { participantId: 30 })
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/referrals', referral)
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/referrals/60/qualify', { qualified: true })
    expect(get).toHaveBeenCalledWith('/canvas/growth-activities/10/tasks')
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/tasks', task)
    expect(get).toHaveBeenCalledWith('/canvas/growth-activities/10/task-progress')
    expect(post).toHaveBeenCalledWith('/canvas/growth-activities/10/task-progress', progress)
  })
})
