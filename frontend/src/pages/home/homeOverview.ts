import { createElement, type ReactNode } from 'react'
import {
  ApartmentOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  TeamOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'

export interface HomeOverview {
  range: HomeRange
  summary: HomeSummary
  trend: HomeTrendPoint[]
  topCanvases: HomeTopCanvas[]
  attentionItems: HomeAttentionItem[]
}

export interface HomeRange {
  days: number
  since: string
  until: string
}

export interface HomeSummary {
  publishedCanvasCount: number
  totalExecutions: number
  uniqueUsers: number
  successRate: string
  failedExecutions: number
}

export interface HomeTrendPoint {
  date: string
  total: number
  failed: number
}

export interface HomeTopCanvas {
  canvasId: number
  name: string
  total: number
  uniqueUsers: number
  successRate: string
  failed: number
}

export interface HomeAttentionItem {
  canvasId: number
  name: string
  type: 'HIGH_FAILURE_RATE' | 'HAS_FAILURES' | 'NO_RECENT_EXECUTIONS' | string
  message: string
  severity: 'warning' | 'info' | string
}

export interface KpiCard {
  key: string
  label: string
  value: string
  hint: string
  icon: ReactNode
  bg: string
  iconBg: string
  color: string
}

export const HOME_RANGE_OPTIONS = [
  { label: '今日', value: 1 },
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
] as const

export function buildKpiCards(overview: HomeOverview): KpiCard[] {
  const { summary } = overview
  return [
    {
      key: 'published',
      label: '已发布旅程',
      value: formatNumber(summary.publishedCanvasCount),
      hint: '线上生效活动',
      icon: createElement(ApartmentOutlined),
      bg: '#fffbeb',
      iconBg: '#fef3c7',
      color: '#f59e0b',
    },
    {
      key: 'users',
      label: '触达用户数',
      value: formatNumber(summary.uniqueUsers),
      hint: '去重用户规模',
      icon: createElement(TeamOutlined),
      bg: '#eff6ff',
      iconBg: '#dbeafe',
      color: '#3b82f6',
    },
    {
      key: 'successRate',
      label: '执行成功率',
      value: summary.successRate || '0%',
      hint: '成功执行占比',
      icon: createElement(CheckCircleOutlined),
      bg: '#f0fdf4',
      iconBg: '#dcfce7',
      color: '#22c55e',
    },
    {
      key: 'executions',
      label: '触发次数',
      value: formatNumber(summary.totalExecutions),
      hint: '当前范围总量',
      icon: createElement(ThunderboltOutlined),
      bg: '#faf5ff',
      iconBg: '#ede9fe',
      color: '#8b5cf6',
    },
    {
      key: 'failed',
      label: '执行失败',
      value: formatNumber(summary.failedExecutions),
      hint: '需要排查次数',
      icon: createElement(CloseCircleOutlined),
      bg: '#fff1f2',
      iconBg: '#fee2e2',
      color: '#ef4444',
    },
  ]
}

export function getAttentionPresentation(severity: string) {
  if (severity === 'warning') return { color: 'orange', label: '关注' }
  if (severity === 'error') return { color: 'red', label: '异常' }
  return { color: 'blue', label: '提示' }
}

function formatNumber(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString()
}
