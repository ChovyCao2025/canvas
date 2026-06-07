import { describe, expect, it, vi } from 'vitest'
import { createApprovalApi, canvasApi, http } from './api'
import type { ApprovalInstanceView } from './api'

describe('approvalApi', () => {
  it('calls approval task and decision endpoints', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createApprovalApi(client as any)

    await api.tasks('PENDING')
    await api.instances({ targetType: 'CANVAS', targetId: '62', status: 'PENDING' })
    await api.approve(201, { comment: 'ok' })
    await api.reject(202, { comment: 'risk unclear' })

    expect(client.get).toHaveBeenCalledWith('/approvals/tasks', { params: { status: 'PENDING' } })
    expect(client.get).toHaveBeenCalledWith('/approvals/instances', {
      params: { targetType: 'CANVAS', targetId: '62', status: 'PENDING' },
    })
    expect(client.post).toHaveBeenCalledWith('/approvals/tasks/201/approve', { comment: 'ok' })
    expect(client.post).toHaveBeenCalledWith('/approvals/tasks/202/reject', { comment: 'risk unclear' })
  })

  it('calls canvas publish approval endpoints', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ code: 0, data: {} })
    const post = vi.spyOn(http, 'post').mockResolvedValue({ code: 0, data: {} })

    await canvasApi.approvalStatus(62)
    await canvasApi.submitReview(62, { reason: '准备发布' })

    expect(get).toHaveBeenCalledWith('/canvas/62/approval-status')
    expect(post).toHaveBeenCalledWith('/canvas/62/submit-review', { reason: '准备发布' })
  })

  it('types Lark external approval bindings on approval views', () => {
    const view: ApprovalInstanceView = {
      id: 101,
      tenantId: 7,
      definitionKey: 'CANVAS_PUBLISH_DEFAULT',
      domain: 'CANVAS',
      targetType: 'CANVAS',
      targetId: '62',
      status: 'PENDING',
      submitter: 'alice',
      externalInstanceId: 'lark-instance-101',
      pendingTasks: [
        {
          id: 201,
          tenantId: 7,
          instanceId: 101,
          stepNo: 1,
          approver: 'bob',
          status: 'PENDING',
          externalTaskId: 'lark-task-201',
        },
      ],
    }

    expect(view.externalInstanceId).toBe('lark-instance-101')
    expect(view.pendingTasks[0].externalTaskId).toBe('lark-task-201')
  })
})
