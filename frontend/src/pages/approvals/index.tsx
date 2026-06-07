import { useEffect, useMemo, useState } from 'react'
import { Button, Popconfirm, Space, Table, Tag, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { CheckOutlined, CloseOutlined, ReloadOutlined } from '@ant-design/icons'
import { approvalApi, type ApprovalTaskView } from '../../services/api'
import {
  approvalStatusColor,
  approvalStatusLabel,
  canDecideApprovalTask,
  sortApprovalTasks,
} from './approvalPresentation'

const { Title, Text } = Typography

export default function ApprovalsPage() {
  const [tasks, setTasks] = useState<ApprovalTaskView[]>([])
  const [loading, setLoading] = useState(false)
  const [actingTaskId, setActingTaskId] = useState<number | null>(null)

  const load = async () => {
    setLoading(true)
    try {
      const response = await approvalApi.tasks('PENDING')
      setTasks(response.data ?? [])
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载审批任务失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const decide = async (task: ApprovalTaskView, action: 'approve' | 'reject') => {
    if (!canDecideApprovalTask(task) || task.id == null) return
    setActingTaskId(task.id)
    try {
      if (action === 'approve') {
        await approvalApi.approve(task.id, { comment: 'approved from inbox' })
        message.success('审批已通过')
      } else {
        await approvalApi.reject(task.id, { comment: 'rejected from inbox' })
        message.success('审批已拒绝')
      }
      await load()
    } catch (error) {
      message.error(error instanceof Error ? error.message : '审批操作失败')
    } finally {
      setActingTaskId(null)
    }
  }

  const rows = useMemo(() => sortApprovalTasks(tasks), [tasks])

  const columns: ColumnsType<ApprovalTaskView> = [
    {
      title: '任务',
      dataIndex: 'id',
      width: 120,
      render: (_, row) => <Text strong>#{row.id ?? row.instanceId ?? '-'}</Text>,
    },
    {
      title: '审批人',
      dataIndex: 'approver',
      width: 180,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: status => <Tag color={approvalStatusColor(status)}>{approvalStatusLabel(status)}</Tag>,
    },
    {
      title: '到期时间',
      dataIndex: 'dueAt',
      width: 220,
      render: dueAt => dueAt ?? '-',
    },
    {
      title: '操作',
      key: 'actions',
      width: 220,
      render: (_, row) => (
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<CheckOutlined />}
            disabled={!canDecideApprovalTask(row)}
            loading={actingTaskId === row.id}
            onClick={() => void decide(row, 'approve')}
          >
            通过
          </Button>
          <Popconfirm
            title="拒绝该审批任务？"
            okText="拒绝"
            cancelText="取消"
            onConfirm={() => void decide(row, 'reject')}
            disabled={!canDecideApprovalTask(row)}
          >
            <Button
              danger
              size="small"
              icon={<CloseOutlined />}
              disabled={!canDecideApprovalTask(row)}
              loading={actingTaskId === row.id}
            >
              拒绝
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <Title level={3} style={{ margin: 0 }}>审批任务</Title>
          <Button icon={<ReloadOutlined />} onClick={() => void load()} loading={loading}>
            刷新
          </Button>
        </Space>
        <Table
          rowKey={row => String(row.id ?? `${row.instanceId}-${row.stepNo}-${row.approver}`)}
          columns={columns}
          dataSource={rows}
          loading={loading}
          pagination={{ pageSize: 20 }}
        />
      </Space>
    </div>
  )
}
