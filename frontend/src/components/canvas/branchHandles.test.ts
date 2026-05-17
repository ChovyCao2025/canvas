import { describe, it, expect } from 'vitest'
import { getBranchHandles } from './branchHandles'

describe('getBranchHandles', () => {
  it('returns empty array for non-branching node', () => {
    expect(getBranchHandles('DELAY', {})).toEqual([])
  })

  it('API_CALL returns empty array (no branch handles for now)', () => {
    expect(getBranchHandles('API_CALL', {})).toEqual([])
  })

  it('IF_CONDITION returns fixed success+else handles', () => {
    const handles = getBranchHandles('IF_CONDITION', {})
    expect(handles).toHaveLength(2)
    expect(handles[0].id).toBe('success')
    expect(handles[1].id).toBe('else')
  })

  it('MANUAL_APPROVAL returns approve+reject', () => {
    const handles = getBranchHandles('MANUAL_APPROVAL', {})
    expect(handles.map(h => h.id)).toEqual(['approve', 'reject'])
  })

  it('SELECTOR handles = branches.length + 1 else', () => {
    const branches = [{ label: '分支A' }, { label: '分支B' }, { label: '分支C' }]
    const handles = getBranchHandles('SELECTOR', { branches })
    expect(handles).toHaveLength(4)
    expect(handles[0]).toEqual({ id: 'branch-0', label: '分支A', color: '#1677ff' })
    expect(handles[3]).toEqual({ id: 'else', label: '否则', color: '#8c8c8c' })
  })

  it('AB_SPLIT handles = groups.length, ids are group-KEY', () => {
    const groups = [{ groupKey: 'A' }, { groupKey: 'B' }, { groupKey: 'C' }]
    const handles = getBranchHandles('AB_SPLIT', { groups })
    expect(handles).toHaveLength(3)
    expect(handles[0].id).toBe('group-A')
    expect(handles[1].id).toBe('group-B')
    expect(handles[2].id).toBe('group-C')
  })

  it('PRIORITY handles = priorities.length + 1 default', () => {
    const priorities = [{ order: 1 }, { order: 2 }]
    const handles = getBranchHandles('PRIORITY', { priorities })
    expect(handles).toHaveLength(3)
    expect(handles[0]).toEqual({ id: 'priority-0', label: '优先 1', color: '#eb2f96' })
    expect(handles[2]).toEqual({ id: 'default', label: '其余', color: '#8c8c8c' })
  })

  it('SELECTOR with empty branches returns only else', () => {
    const handles = getBranchHandles('SELECTOR', { branches: [] })
    expect(handles).toHaveLength(1)
    expect(handles[0].id).toBe('else')
  })

  it('AB_SPLIT with empty groups returns empty array', () => {
    expect(getBranchHandles('AB_SPLIT', { groups: [] })).toEqual([])
  })
})
