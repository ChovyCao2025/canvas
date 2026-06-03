/**
 * 测试职责：验证节点业务配置到画布分支 handle 的推导规则。
 *
 * 维护说明：新增分支型节点或修改 handle ID 时，需要同步更新这些契约测试。
 */
import { describe, it, expect } from 'vitest'
import { getBranchHandles } from './branchHandles'

describe('getBranchHandles', () => {
  it('returns empty array for non-branching node', () => {
    expect(getBranchHandles('WAIT', {})).toEqual([])
  })

  it('API_CALL returns empty array (no branch handles for now)', () => {
    expect(getBranchHandles('API_CALL', {})).toEqual([])
  })

  it('DIRECT_CALL uses the default outlet and does not render branch handles', () => {
    expect(getBranchHandles('DIRECT_CALL', {
      branches: [{ label: '查询用户' }, { label: '查询订单' }],
    })).toEqual([])
    expect(getBranchHandles('DIRECT_CALL', {})).toEqual([])
  })

  it('IF_CONDITION returns fixed success+fail handles', () => {
    const handles = getBranchHandles('IF_CONDITION', {})
    expect(handles).toHaveLength(2)
    expect(handles[0]).toEqual({ id: 'success', label: '条件成立', color: '#52c41a' })
    expect(handles[1]).toEqual({ id: 'fail', label: '否则', color: '#8c8c8c' })
  })

  it('SPLIT handles use configured branches and weights', () => {
    const handles = getBranchHandles('SPLIT', {
      branches: [
        { branchId: 'a', label: 'A组', weight: 30 },
        { branchId: 'b', label: 'B组', weight: 70 },
      ],
    })
    expect(handles.map(h => h.id)).toEqual(['branch-a', 'branch-b'])
    expect(handles.map(h => h.label)).toEqual(['A组 30%', 'B组 70%'])
  })

  it('SPLIT with empty branches returns empty array', () => {
    expect(getBranchHandles('SPLIT', { branches: [] })).toEqual([])
  })
})
