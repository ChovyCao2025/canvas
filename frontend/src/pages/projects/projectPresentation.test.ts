import { describe, expect, it } from 'vitest'
import {
  projectRoleLabel,
  projectStatusLabel,
  projectStatusColor,
  projectStatsCards,
} from './projectPresentation'

describe('projectPresentation', () => {
  it('formats role labels', () => {
    expect(projectRoleLabel('PROJECT_ADMIN')).toBe('项目管理员')
    expect(projectRoleLabel('EDITOR')).toBe('编辑者')
    expect(projectRoleLabel('EXECUTOR')).toBe('执行者')
    expect(projectRoleLabel('VIEWER')).toBe('查看者')
    expect(projectRoleLabel('CUSTOM')).toBe('CUSTOM')
  })

  it('formats status labels and colors', () => {
    expect(projectStatusLabel('ACTIVE')).toBe('启用')
    expect(projectStatusLabel('DISABLED')).toBe('停用')
    expect(projectStatusColor('ACTIVE')).toBe('green')
    expect(projectStatusColor('DISABLED')).toBe('default')
  })

  it('builds compact stats cards', () => {
    expect(projectStatsCards({
      projectId: 3,
      canvasCount: 8,
      publishedCanvasCount: 5,
      executionCount7d: 100,
      failedExecutionCount7d: 4,
      avgDurationMs7d: 250,
    })).toEqual([
      { key: 'canvasCount', label: '画布数', value: 8 },
      { key: 'publishedCanvasCount', label: '已发布', value: 5 },
      { key: 'executionCount7d', label: '7日执行', value: 100 },
      { key: 'failedExecutionCount7d', label: '7日失败', value: 4 },
      { key: 'avgDurationMs7d', label: '平均耗时', value: '250 ms' },
    ])
  })
})
