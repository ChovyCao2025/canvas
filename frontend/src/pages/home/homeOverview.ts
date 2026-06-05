/**
 * 页面职责：首页运营概览展示模型和计算工具。
 *
 * 维护说明：把后端聚合数据转换为 KPI、趋势和异常提示所需的前端结构。
 */
import { createElement, type ReactNode } from 'react'
import {
  ApartmentOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  TeamOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'

/** 首页概览接口返回的完整数据。 */
export interface HomeOverview {
  range: HomeRange
  summary: HomeSummary
  trend: HomeTrendPoint[]
  topCanvases: HomeTopCanvas[]
  attentionItems: HomeAttentionItem[]
}

/** 当前统计窗口。 */
export interface HomeRange {
  days: number
  since: string
  until: string
}

/** 首页顶部 KPI 原始汇总。 */
export interface HomeSummary {
  publishedCanvasCount: number
  totalExecutions: number
  uniqueUsers: number
  successRate: string
  failedExecutions: number
}

/** 趋势图单日数据点。 */
export interface HomeTrendPoint {
  date: string
  total: number
  failed: number
}

/** TOP 旅程排行行数据。 */
export interface HomeTopCanvas {
  canvasId: number
  name: string
  total: number
  uniqueUsers: number
  successRate: string
  failed: number
}

/** 首页关注项和风险摘要支持的展示等级。 */
export type AttentionSeverity = 'error' | 'warning' | 'info' | 'success' | string

/** 需要运营关注的异常或提示项。 */
export interface HomeAttentionItem {
  canvasId: number
  name: string
  type: 'HIGH_FAILURE_RATE' | 'HAS_FAILURES' | 'NO_RECENT_EXECUTIONS' | string
  message: string
  severity: AttentionSeverity
}

/** 首页风险摘要展示模型。 */
export interface RiskSummary {
  healthy: boolean
  title: string
  message: string
  severity: AttentionSeverity
  actionLabel: string
  targetCanvasId: number | null
  failedExecutions: string
  successRate: string
  pendingCount: number
}

/** 关注项主操作展示模型。 */
export interface AttentionAction {
  label: string
  destination: 'stats' | 'edit'
}

/** KPI 卡片展示模型，页面直接消费。 */
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

/** 首页支持的统计范围选项。 */
export const HOME_RANGE_OPTIONS = [
  { label: '今日', value: 1 },
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
] as const

/** 将后端 summary 转换为首页 KPI 卡片展示模型。 */
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

/** 根据异常等级返回 Tag 展示配置。 */
export function getAttentionPresentation(severity: string) {
  if (severity === 'warning') return { color: 'orange', label: '关注' }
  if (severity === 'error') return { color: 'red', label: '异常' }
  return { color: 'blue', label: '提示' }
}

/** 按运营处理优先级排序关注项，同等级保持后端原始顺序。 */
export function sortAttentionItems(items: HomeAttentionItem[]): HomeAttentionItem[] {
  return items
    .map((item, index) => ({ item, index }))
    .sort((left, right) => {
      const priorityDiff = severityPriority(left.item.severity) - severityPriority(right.item.severity)
      if (priorityDiff !== 0) return priorityDiff
      return left.index - right.index
    })
    .map(({ item }) => item)
}

/** 根据关注项类型给出页面主操作。 */
export function getAttentionAction(item: Pick<HomeAttentionItem, 'type' | 'canvasId'>): AttentionAction {
  if (item.canvasId <= 0) return { label: '查看', destination: 'stats' }
  if (item.type === 'NO_RECENT_EXECUTIONS') return { label: '编辑', destination: 'edit' }
  if (item.type === 'HIGH_FAILURE_RATE') return { label: '处理', destination: 'stats' }
  return { label: '查看', destination: 'stats' }
}

/** 汇总首页当前最高优先级风险和关键运行指标。 */
export function buildRiskSummary(overview: HomeOverview): RiskSummary {
  const failedExecutions = formatNumber(overview.summary.failedExecutions)
  const successRate = overview.summary.successRate || '0%'
  const pendingCount = overview.attentionItems.length

  if (pendingCount === 0) {
    return {
      healthy: true,
      title: '当前暂无高优先级异常',
      message: '近 7 天旅程运行稳定，可继续关注触达趋势和 Top 旅程表现',
      severity: 'success',
      actionLabel: '查看趋势',
      targetCanvasId: null,
      failedExecutions,
      successRate,
      pendingCount,
    }
  }

  const [selectedItem] = sortAttentionItems(overview.attentionItems)
  const action = getAttentionAction(selectedItem)

  return {
    healthy: false,
    title: selectedItem.name,
    message: selectedItem.message,
    severity: selectedItem.severity,
    actionLabel: action.label,
    targetCanvasId: selectedItem.canvasId > 0 ? selectedItem.canvasId : null,
    failedExecutions,
    successRate,
    pendingCount,
  }
}

/** 基于本地关键词筛选 Top 旅程和关注项，不改变后端原始概览对象。 */
export function filterHomeOverview(overview: HomeOverview, keyword: string): HomeOverview {
  const normalizedKeyword = keyword.trim().toLowerCase()
  if (!normalizedKeyword) return overview

  return {
    ...overview,
    topCanvases: overview.topCanvases.filter(canvas => canvas.name.toLowerCase().includes(normalizedKeyword)),
    attentionItems: overview.attentionItems.filter(item => item.name.toLowerCase().includes(normalizedKeyword)),
  }
}

function severityPriority(severity: AttentionSeverity) {
  if (severity === 'error') return 0
  if (severity === 'warning') return 1
  if (severity === 'info') return 2
  return 3
}

/** 数字统一格式化为本地千分位，空值按 0 处理。 */
function formatNumber(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString()
}
