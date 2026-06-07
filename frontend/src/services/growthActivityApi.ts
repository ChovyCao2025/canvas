import http from './api'
import type { R } from '../types'

export interface GrowthActivityQuery {
  activityType?: string
  status?: string
  campaignId?: number
  ownerTeam?: string
  readinessStatus?: string
  scheduleStatus?: string
  grantHealth?: string
  limit?: number
}

export interface GrowthActivityCommand {
  activityKey: string
  activityName: string
  activityType: string
  status?: string
  campaignId?: number | null
  objective?: string | null
  ownerTeam?: string | null
  startAt?: string | null
  endAt?: string | null
  channelScope?: string | null
  audienceRefs?: Record<string, unknown>
  riskPolicyRef?: string | null
  experimentRef?: string | null
  dashboardRef?: string | null
  metadata?: Record<string, unknown>
}

export interface GrowthActivity {
  id: number
  tenantId: number
  activityKey: string
  activityName: string
  activityType: string
  status: string
  campaignId?: number | null
  objective?: string | null
  ownerTeam?: string | null
  startAt?: string | null
  endAt?: string | null
  channelScope?: string | null
  audienceRefs?: Record<string, unknown>
  riskPolicyRef?: string | null
  experimentRef?: string | null
  dashboardRef?: string | null
  metadata?: Record<string, unknown>
  createdBy?: string | null
  updatedBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface GrowthActivityReadinessCheck {
  severity: string
  itemType: string
  itemKey: string
  title: string
  reason: string
  route?: string | null
}

export interface GrowthActivityReadiness {
  tenantId: number
  activityId: number
  activityKey: string
  activityType: string
  generatedAt: string
  status: string
  productionReady: boolean
  blockerCount: number
  warningCount: number
  blockers: GrowthActivityReadinessCheck[]
  warnings: GrowthActivityReadinessCheck[]
  checks: GrowthActivityReadinessCheck[]
}

export interface GrowthActivityReport {
  tenantId: number
  activityId: number
  participation: { totalParticipants: number; activeParticipants: number }
  referral: { totalRelations: number; qualifiedRelations: number; pendingRelations: number; rejectedRelations: number }
  grants: {
    totalGrants: number
    reservedGrants: number
    successGrants: number
    failedGrants: number
    canceledGrants: number
    redeemedGrants: number
    expiredGrants: number
    totalCost: number | string
  }
  conversion: { conversionCount: number; conversionAmount: number | string; roi: number | string }
  task: { totalProgress: number; completedProgress: number; completionRate: number | string }
}

export interface GrowthRewardPool {
  id: number
  tenantId: number
  activityId: number
  poolKey: string
  rewardType: string
  grantChannel: string
  couponTypeKey?: string | null
  loyaltyRewardKey?: string | null
  pointsType?: string | null
  externalContractKey?: string | null
  inventoryMode?: string | null
  totalInventory?: number | null
  reservedInventory?: number | null
  grantedInventory?: number | null
  perUserLimit?: number | null
  perReferralLimit?: number | null
  budgetAmount?: number | string | null
  reservedAmount?: number | string | null
  grantedAmount?: number | string | null
  costCurrency?: string | null
  status: string
  inventoryLow?: boolean
  metadata?: Record<string, unknown>
  createdBy?: string | null
  updatedBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface GrowthRewardGrant {
  id: number
  tenantId: number
  activityId: number
  poolId: number
  participantId?: number | null
  referralRelationId?: number | null
  taskProgressId?: number | null
  grantReason: string
  status: string
  idempotencyKey: string
  providerRequest?: Record<string, unknown>
  providerResponse?: Record<string, unknown>
  costAmount: number | string
  createdBy?: string | null
  updatedBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface GrowthReferralCode {
  id: number
  tenantId: number
  activityId: number
  participantId: number
  code: string
  status: string
  createdBy?: string | null
  createdAt?: string | null
}

export interface GrowthReferralRelation {
  id: number
  tenantId: number
  activityId: number
  referralCodeId: number
  referrerParticipantId: number
  inviteeUserId: string
  status: string
  riskEvidence?: Record<string, unknown>
  inviterRewardGrantId?: number | null
  inviteeRewardGrantId?: number | null
  createdBy?: string | null
  updatedBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface GrowthTaskDefinition {
  id: number
  tenantId: number
  activityId: number
  taskKey: string
  taskType: string
  completionPolicy: string
  resetPolicy: string
  rewardPoolId?: number | null
  targetValue: number | string
  status: string
  rule?: Record<string, unknown>
  createdBy?: string | null
  updatedBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface GrowthTaskProgress {
  id: number
  tenantId: number
  activityId: number
  participantId: number
  taskId: number
  progressValue: number | string
  targetValue: number | string
  status: string
  lastEventKey?: string | null
  evidence?: Record<string, unknown>
  rewardGrantId?: number | null
  updatedBy?: string | null
  completedAt?: string | null
  updatedAt?: string | null
}

export type GrowthRewardPoolCommand = Record<string, unknown>
export type GrowthRewardGrantCommand = Record<string, unknown>
export type GrowthActivityEventCommand = Record<string, unknown>
export type GrowthReferralRelationCommand = Record<string, unknown>
export type GrowthReferralQualificationCommand = Record<string, unknown>
export type GrowthTaskDefinitionCommand = Record<string, unknown>
export type GrowthTaskProgressCommand = Record<string, unknown>

const base = '/canvas/growth-activities'

export const growthActivityApi = {
  listActivities: (params?: GrowthActivityQuery) =>
    http.get<R<GrowthActivity[]>, R<GrowthActivity[]>>(base, { params }),

  upsertActivity: (payload: GrowthActivityCommand) =>
    http.post<R<GrowthActivity>, R<GrowthActivity>>(base, payload),

  getActivity: (activityId: number) =>
    http.get<R<GrowthActivity>, R<GrowthActivity>>(`${base}/${activityId}`),

  publishActivity: (activityId: number) =>
    http.post<R<GrowthActivity>, R<GrowthActivity>>(`${base}/${activityId}/publish`, {}),

  pauseActivity: (activityId: number) =>
    http.post<R<GrowthActivity>, R<GrowthActivity>>(`${base}/${activityId}/pause`, {}),

  closeActivity: (activityId: number) =>
    http.post<R<GrowthActivity>, R<GrowthActivity>>(`${base}/${activityId}/close`, {}),

  getReadiness: (activityId: number) =>
    http.get<R<GrowthActivityReadiness>, R<GrowthActivityReadiness>>(`${base}/${activityId}/readiness`),

  getReport: (activityId: number) =>
    http.get<R<GrowthActivityReport>, R<GrowthActivityReport>>(`${base}/${activityId}/report`),

  listRewardPools: (activityId: number) =>
    http.get<R<GrowthRewardPool[]>, R<GrowthRewardPool[]>>(`${base}/${activityId}/reward-pools`),

  upsertRewardPool: (activityId: number, payload: GrowthRewardPoolCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/reward-pools`, payload),

  listGrants: (activityId: number) =>
    http.get<R<GrowthRewardGrant[]>, R<GrowthRewardGrant[]>>(`${base}/${activityId}/grants`),

  createGrant: (activityId: number, payload: GrowthRewardGrantCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/grants`, payload),

  retryGrant: (activityId: number, grantId: number) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/grants/${grantId}/retry`, {}),

  reconcileGrant: (activityId: number, grantId: number, payload: Record<string, unknown>) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/grants/${grantId}/reconcile`, payload),

  cancelGrant: (activityId: number, grantId: number) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/grants/${grantId}/cancel`, {}),

  listEvents: (activityId: number, params?: { eventType?: string; limit?: number }) =>
    http.get<R<unknown[]>, R<unknown[]>>(`${base}/${activityId}/events`, { params }),

  recordEvent: (activityId: number, payload: GrowthActivityEventCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/events`, payload),

  listReferralCodes: (activityId: number) =>
    http.get<R<GrowthReferralCode[]>, R<GrowthReferralCode[]>>(`${base}/${activityId}/referral-codes`),

  generateReferralCode: (activityId: number, participantId: number) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/referral-codes`, { participantId }),

  listReferralRelations: (activityId: number) =>
    http.get<R<GrowthReferralRelation[]>, R<GrowthReferralRelation[]>>(`${base}/${activityId}/referrals`),

  upsertReferralRelation: (activityId: number, payload: GrowthReferralRelationCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/referrals`, payload),

  qualifyReferral: (activityId: number, relationId: number, payload: GrowthReferralQualificationCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/referrals/${relationId}/qualify`, payload),

  listTaskDefinitions: (activityId: number) =>
    http.get<R<GrowthTaskDefinition[]>, R<GrowthTaskDefinition[]>>(`${base}/${activityId}/tasks`),

  upsertTaskDefinition: (activityId: number, payload: GrowthTaskDefinitionCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/tasks`, payload),

  listTaskProgress: (activityId: number) =>
    http.get<R<GrowthTaskProgress[]>, R<GrowthTaskProgress[]>>(`${base}/${activityId}/task-progress`),

  recordTaskProgress: (activityId: number, payload: GrowthTaskProgressCommand) =>
    http.post<R<unknown>, R<unknown>>(`${base}/${activityId}/task-progress`, payload),
}
