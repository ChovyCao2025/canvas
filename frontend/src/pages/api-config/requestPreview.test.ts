import { describe, expect, it } from 'vitest'
import {
  buildApiReceiptPreview,
  buildApiRequestPreview,
  formatApiRequestPreview,
  normalizeApiDefinitionPayload,
} from './requestPreview'

describe('api request preview', () => {
  it('wraps configured parameters under params when environment info is disabled', () => {
    const preview = buildApiRequestPreview({
      requestSchema: [
        { name: 'define_item1', displayName: '优惠券ID', type: 'STRING', required: true },
        { name: 'define_item2', displayName: '优惠券内容示例', type: 'TEXT', required: false },
      ],
      includeContextPayload: false,
    })

    expect(preview).toEqual([
      {
        params: {
          define_item1: '优惠券ID',
          define_item2: '优惠券内容示例',
        },
      },
    ])
  })

  it('ignores blank parameter names and falls back to the name as display value', () => {
    const preview = buildApiRequestPreview({
      requestSchema: [
        { name: 'couponId', displayName: '', type: 'STRING', required: true },
        { name: '  ', displayName: '空参数', type: 'STRING', required: false },
      ],
      includeContextPayload: false,
    })

    expect(preview).toEqual([
      {
        params: {
          couponId: 'couponId',
        },
      },
    ])
  })

  it('adds user_profile callback_params and process_info when environment info is enabled', () => {
    const preview = buildApiRequestPreview({
      requestSchema: [
        { name: 'define_item1', displayName: '优惠券ID', type: 'STRING', required: true },
      ],
      includeContextPayload: true,
    })

    expect(preview).toEqual([
      {
        user_profile: {
          target_type: 'OPEN_ID',
          target_id: '1917810',
          customer_id: '1917810',
        },
        params: {
          define_item1: '优惠券ID',
        },
        callback_params: {
          webhook_id: '',
          send_time: '1625037472000',
          nodeId: '节点Id',
          instanceId: '实例Id',
          batchId: '执行动作批次Id，可做批次幂等ID',
          actionId: '执行动作实例Id，可做单条幂等ID',
          customerId: '用户Id，customerId',
        },
        process_info: {
          processInstanceId: '新版：旅程周期中，每个用户的旅程实例ID',
          processInstanceStartTime: '新版：旅程周期中，每个用户的旅程实例开始时间，时间戳格式',
          processNodeInstanceId: '新版：旅程节点实例ID（每次不同）',
          processNodeInstanceStartTime: '新版：旅程周期中，每个用户的旅程的节点实例开始时间，时间戳格式',
          groupName: 'nodeId:nodeName:groupResult(node.result),groupName(node.resultExt)',
        },
      },
    ])
  })

  it('formats preview as pretty JSON', () => {
    expect(formatApiRequestPreview([{ params: { couponId: '优惠券ID' } }])).toBe(
      '[\n  {\n    "params": {\n      "couponId": "优惠券ID"\n    }\n  }\n]',
    )
  })

  it('normalizes API definition form values for submission', () => {
    const body = normalizeApiDefinitionPayload({
      name: '发券接口',
      enabled: true,
      includeContextPayload: true,
      receiptEnabled: true,
      receiptExpireMinutes: undefined,
      receiptStatuses: [{ code: '200', label: '成功' }],
      requestSchema: [{ name: 'couponId', displayName: '优惠券ID', type: 'STRING', required: true }],
    })

    expect(body).toEqual({
      name: '发券接口',
      enabled: 1,
      includeContextPayload: 1,
      receiptEnabled: 1,
      receiptExpireMinutes: 1440,
      receiptStatuses: '[{"code":"200","label":"成功"}]',
      requestSchema: '[{"name":"couponId","displayName":"优惠券ID","type":"STRING","required":true}]',
    })
  })

  it('returns an empty receipt preview when receipt is disabled', () => {
    expect(buildApiReceiptPreview({
      receiptEnabled: false,
      receiptStatuses: [{ code: '200', label: '成功' }],
    })).toEqual([])
  })

  it('builds a receipt report preview from the first configured status', () => {
    expect(buildApiReceiptPreview({
      receiptEnabled: true,
      receiptStatuses: [
        { code: '', label: '空状态' },
        { code: '500', label: '发送失败' },
      ],
    })).toEqual([
      {
        msg_id: '业务侧唯一ID',
        status: '500',
        cst_id: '用户Id，customerId',
        send_time: '回执发送时间，时间戳格式',
        callback_params: {
          webhook_id: '',
          send_time: '1625037472000',
          nodeId: '节点Id',
          instanceId: '实例Id',
          batchId: '执行动作批次Id，可做批次幂等ID',
          actionId: '执行动作实例Id，可做单条幂等ID',
          customerId: '用户Id，customerId',
        },
      },
    ])
  })
})
