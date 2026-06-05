import { renderToString } from 'react-dom/server'
import type { MouseEvent } from 'react'
import { describe, expect, it, vi } from 'vitest'

import type { NodeTrace } from './ExecutionTracePanel'
import {
  createTraceRowProps,
  renderTraceError,
} from './ExecutionTracePanel'
import {
  downloadErrorText,
  formatTraceStatus,
  isLongError,
  tracePathClass,
} from './executionTimelinePresentation'

const longError = [
  'Validation failed while resolving personalization variables.',
  'Expected customer.profile.email to be present.',
  'Stack frame 1: TemplateRenderer.render',
  'Stack frame 2: ChannelMessageBuilder.build',
  'Stack frame 3: CampaignExecutionService.dispatch',
  'final stack frame',
].join('\n')

const trace: NodeTrace = {
  nodeId: 'node-email',
  nodeName: 'Send Email',
  nodeType: 'EMAIL',
  status: 2,
  durationMs: 42,
  errorMsg: longError,
}

describe('execution timeline presentation', () => {
  it('renders the full long error in the expandable error display', () => {
    const html = renderToString(<>{renderTraceError(trace, 'exec-001')}</>)

    expect(html).toContain('Validation failed while resolving personalization variables')
    expect(html).toContain('final stack frame')
    expect(html).toContain('下载错误')
  })

  it('builds downloadable long error text with trace metadata', () => {
    expect(isLongError(longError)).toBe(true)

    const text = downloadErrorText(trace, 'exec-001')

    expect(text).toContain('Execution ID: exec-001')
    expect(text).toContain('Node ID: node-email')
    expect(text).toContain('Node Name: Send Email')
    expect(text).toContain('Status: 失败')
    expect(text).toContain(longError)
  })

  it('formats path highlight classes by trace status', () => {
    expect(tracePathClass({ status: 0 })).toBe('execution-trace-path execution-trace-path--running')
    expect(tracePathClass({ status: 1 })).toBe('execution-trace-path execution-trace-path--success')
    expect(tracePathClass({ status: 2 })).toBe('execution-trace-path execution-trace-path--error')
    expect(tracePathClass({ status: 3 })).toBe('execution-trace-path execution-trace-path--skipped')
  })

  it('calls the click-to-node callback with node id and trace payload', () => {
    const onTraceClick = vi.fn()
    const rowProps = createTraceRowProps(trace, onTraceClick)

    rowProps.onClick?.({} as MouseEvent<HTMLTableRowElement>)

    expect(onTraceClick).toHaveBeenCalledWith('node-email', trace)
  })

  it('formats status labels for known and unknown backend values', () => {
    expect(formatTraceStatus(0)).toMatchObject({ color: 'processing', label: '执行中' })
    expect(formatTraceStatus(1)).toMatchObject({ color: 'success', label: '成功' })
    expect(formatTraceStatus(2)).toMatchObject({ color: 'error', label: '失败' })
    expect(formatTraceStatus(3)).toMatchObject({ color: 'default', label: '跳过' })
    expect(formatTraceStatus(99)).toMatchObject({ color: 'default', label: '未知' })
  })
})
