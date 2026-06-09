import { describe, expect, it } from 'vitest'
import type { Node } from '@xyflow/react'
import type { CanvasNodeData } from '../canvas/constants'
import {
  buildNodeTargetOptions,
  createAiLlmPatch,
  isAiLlmConfigured,
  normalizeAiSelectOptions,
} from './AiLlmConfigPanel'

describe('AiLlmConfigPanel helpers', () => {
  it('builds node patch while preserving existing biz config', () => {
    const nodeData: CanvasNodeData = {
      nodeType: 'AI_LLM',
      name: 'Old AI',
      category: 'AI智能',
      bizConfig: {
        outputPrefix: 'ai',
        timeoutMs: 1000,
        failNodeId: 'old-fail',
      },
    }

    expect(createAiLlmPatch(nodeData, {
      name: 'New AI',
      templateId: '1',
      modelKey: 'gpt-test',
      maxTokens: 800,
      schemaOverride: '{"type":"object"}',
    })).toEqual({
      name: 'New AI',
      bizConfig: {
        outputPrefix: 'ai',
        timeoutMs: 1000,
        templateId: '1',
        modelKey: 'gpt-test',
        maxTokens: 800,
        schemaOverride: '{"type":"object"}',
      },
    })
  })

  it('normalizes backend options for antd select controls', () => {
    expect(normalizeAiSelectOptions([
      { key: '1', label: 'Mock AI' },
      { key: '2', label: 'OpenAI' },
    ])).toEqual([
      { value: '1', label: 'Mock AI' },
      { value: '2', label: 'OpenAI' },
    ])
  })

  it('filters current and start nodes from route target options', () => {
    const nodes = [
      node('start', 'START', '开始'),
      node('ai-1', 'AI_LLM', 'AI'),
      node('next-1', 'SEND_MESSAGE', '发送消息'),
    ]

    expect(buildNodeTargetOptions(nodes, 'ai-1')).toEqual([
      { value: 'next-1', label: '发送消息 (SEND_MESSAGE)' },
    ])
  })

  it('treats template id as the required configured signal', () => {
    expect(isAiLlmConfigured({ templateId: '1' })).toBe(true)
    expect(isAiLlmConfigured({})).toBe(false)
  })
})

function node(id: string, nodeType: string, name: string): Node<CanvasNodeData> {
  return {
    id,
    type: nodeType,
    position: { x: 0, y: 0 },
    data: {
      nodeType,
      name,
      category: nodeType === 'AI_LLM' ? 'AI智能' : '基础控制',
      bizConfig: {},
    },
  }
}
