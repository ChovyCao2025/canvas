import type { PageResult, R } from '../types'
import http from './api'

export interface AnalyticsDateRange {
  startDate: string
  endDate: string
}

export interface EventCountRow {
  eventCode: string
  count: number
}

export interface EventTotal {
  count: number
}

export interface UserTimelineRow {
  eventCode: string
  eventTime: string
}

export interface AttributeDistributionRow {
  value: string
  count: number
}

export interface EventTotalQuery extends AnalyticsDateRange {
  eventCode?: string
}

export interface UserTimelineQuery extends AnalyticsDateRange {
  page?: number
  size?: number
}

export function createAnalyticsApi(client = http) {
  return {
    eventCounts: (scope: AnalyticsDateRange) =>
      client.get<R<EventCountRow[]>, R<EventCountRow[]>>('/analytics/events/counts', { params: scope }),
    eventTotal: (query: EventTotalQuery) =>
      client.get<R<EventTotal>, R<EventTotal>>('/analytics/events/count', { params: cleanParams(query) }),
    userTimeline: (userId: string, query: UserTimelineQuery) =>
      client.get<R<PageResult<UserTimelineRow>>, R<PageResult<UserTimelineRow>>>(
        `/analytics/users/${encodeURIComponent(userId)}/timeline`,
        { params: cleanParams(query) },
      ),
    attributeDistribution: (attribute: string, scope: AnalyticsDateRange) =>
      client.get<R<AttributeDistributionRow[]>, R<AttributeDistributionRow[]>>(
        `/analytics/events/attributes/${encodeURIComponent(attribute)}/distribution`,
        { params: scope },
      ),
  }
}

function cleanParams(params: object): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
}

export const analyticsApi = createAnalyticsApi()
