/**
 * 测试职责：验证人群异步任务状态展示、轮询退避和运行中状态优先级。
 *
 * 维护说明：后端任务状态机扩展时，应同步终态判断和显示状态合并规则。
 */
import { describe, expect, it } from 'vitest'
import {
  getAudienceDisplayStatus,
  getNextAudiencePollDelay,
  hasRunningAudienceTasks,
  isTerminalTaskStatus,
} from './audienceTaskPresentation'

describe('audienceTaskPresentation', () => {
  it('treats queued and running tasks as non-terminal', () => {
    expect(isTerminalTaskStatus('QUEUED')).toBe(false)
    expect(isTerminalTaskStatus('RUNNING')).toBe(false)
  })

  it('treats success, failure, and canceled tasks as terminal', () => {
    expect(isTerminalTaskStatus('SUCCEEDED')).toBe(true)
    expect(isTerminalTaskStatus('FAILED')).toBe(true)
    expect(isTerminalTaskStatus('CANCELED')).toBe(true)
  })

  it('detects active audience tasks', () => {
    expect(hasRunningAudienceTasks([{ taskId: 'task_1', status: 'RUNNING', bizId: '7' }])).toBe(true)
    expect(hasRunningAudienceTasks([{ taskId: 'task_2', status: 'SUCCEEDED', bizId: '7' }])).toBe(false)
  })

  it('backs off polling after repeated failures', () => {
    expect(getNextAudiencePollDelay(0, false)).toBe(3000)
    expect(getNextAudiencePollDelay(2, false)).toBe(5000)
    expect(getNextAudiencePollDelay(4, false)).toBe(10000)
    expect(getNextAudiencePollDelay(0, true)).toBe(15000)
  })

  it('prefers running task state over stale stat state', () => {
    expect(getAudienceDisplayStatus({ status: 'READY' }, { taskId: 'task_1', status: 'RUNNING', bizId: '7' }))
      .toBe('RUNNING')
    expect(getAudienceDisplayStatus({ status: 'READY' }, undefined)).toBe('READY')
  })
})
