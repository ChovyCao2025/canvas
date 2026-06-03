import { describe, expect, it } from 'vitest'

import { CATEGORY_COLORS, DEFAULT_NAMES, PUBLISH_TRIGGER_NODE_TYPES } from './constants'

describe('canvas node display constants', () => {
  it('labels DIRECT_CALL as the API entry node shown to users', () => {
    expect(DEFAULT_NAMES.DIRECT_CALL).toBe('API入口')
  })

  it('uses only governed publishable entry nodes', () => {
    expect([...PUBLISH_TRIGGER_NODE_TYPES].sort()).toEqual([
      'DIRECT_CALL',
      'EVENT_TRIGGER',
      'MQ_TRIGGER',
      'SCHEDULED_TRIGGER',
    ].sort())
  })

  it('exposes only governed default node names', () => {
    expect(Object.keys(DEFAULT_NAMES).sort()).toEqual([
      'AGGREGATE',
      'API_CALL',
      'COMMIT_ACTION',
      'DIRECT_CALL',
      'DIRECT_RETURN',
      'END',
      'EVENT_TRIGGER',
      'GROOVY',
      'HUB',
      'IF_CONDITION',
      'MQ_TRIGGER',
      'SCHEDULED_TRIGGER',
      'SEND_MESSAGE',
      'SEND_MQ',
      'SPLIT',
      'START',
      'SUB_FLOW_REF',
      'TAGGER',
      'THRESHOLD',
      'WAIT',
    ].sort())
  })

  it('uses governed product categories', () => {
    expect(Object.keys(CATEGORY_COLORS).sort()).toEqual([
      '入口触发',
      '动作执行',
      '基础控制',
      '数据与权益',
      '流程复用',
      '条件与分流',
      '消息触达',
      '等待与汇聚',
    ].sort())
  })
})
