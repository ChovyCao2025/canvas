import type { ProjectRole, ProjectStats, ProjectStatus } from '../../services/api'

const ROLE_LABELS: Record<ProjectRole, string> = {
  PROJECT_ADMIN: '项目管理员',
  EDITOR: '编辑者',
  EXECUTOR: '执行者',
  VIEWER: '查看者',
}

export function projectRoleLabel(role?: string | null) {
  if (!role) return '-'
  return ROLE_LABELS[role as ProjectRole] ?? role
}

export function projectStatusLabel(status?: ProjectStatus | null) {
  if (status === 'ACTIVE') return '启用'
  if (status === 'DISABLED') return '停用'
  return status || '-'
}

export function projectStatusColor(status?: ProjectStatus | null) {
  if (status === 'ACTIVE') return 'green'
  return 'default'
}

export interface ProjectStatsCard {
  key: string
  label: string
  value: number | string
}

export function projectStatsCards(stats: ProjectStats): ProjectStatsCard[] {
  return [
    { key: 'canvasCount', label: '画布数', value: stats.canvasCount },
    { key: 'publishedCanvasCount', label: '已发布', value: stats.publishedCanvasCount },
    { key: 'executionCount7d', label: '7日执行', value: stats.executionCount7d },
    { key: 'failedExecutionCount7d', label: '7日失败', value: stats.failedExecutionCount7d },
    { key: 'avgDurationMs7d', label: '平均耗时', value: `${stats.avgDurationMs7d} ms` },
  ]
}
