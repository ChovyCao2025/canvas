/**
 * 页面职责：人群列表页，展示人群定义、最新统计和手动重算入口。
 *
 * 维护说明：人群统计是异步补充数据，表格渲染必须兼容统计未返回的状态。
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Button, message, Popconfirm, Space, Table, Tag, Tooltip, Typography } from 'antd'
import { PlusOutlined, ThunderboltOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useLocation, useNavigate } from 'react-router-dom'
import { audienceApi, type AudienceDefinition, type AudienceStat } from '../../services/audienceApi'
import { taskApi, type AsyncTask } from '../../services/taskApi'
import {
  getAudienceDisplayStatus,
  getNextAudiencePollDelay,
  hasRunningAudienceTasks,
} from './audienceTaskPresentation'
import { snapshotModeLabel } from '../audience-edit/audienceSnapshotMode'

/** 页面标题组件别名。 */
const { Title } = Typography

/** 人群统计/异步任务状态到表格 Tag 展示的映射。 */
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待计算', color: 'default' },
  COMPUTING: { label: '计算中', color: 'processing' },
  QUEUED: { label: '排队中', color: 'processing' },
  RUNNING: { label: '计算中', color: 'processing' },
  READY: { label: '就绪', color: 'success' },
  SUCCEEDED: { label: '就绪', color: 'success' },
  FAILED: { label: '失败', color: 'error' },
  CANCELED: { label: '已取消', color: 'default' },
}

/**
 * 人群列表页：展示计算状态并提供重算/删除能力。
 *
 * 数据来源：
 * - `AudienceDefinition`：名称、规则等静态定义；
 * - `AudienceStat`：状态、规模、计算时间等运行态信息。
 */
export default function AudienceListPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [data, setData] = useState<AudienceDefinition[]>([])
  const [stats, setStats] = useState<Record<number, AudienceStat>>({})
  const [tasks, setTasks] = useState<Record<number, AsyncTask>>({})
  const [loading, setLoading] = useState(false)
  const [pollFailureCount, setPollFailureCount] = useState(0)
  const pollTimerRef = useRef<number | null>(null)
  const tasksRef = useRef<Record<number, AsyncTask>>({})
  /** URL 中指定的高亮人群 ID，用于从任务回跳时补齐当前页展示。 */
  const highlightedAudienceId = useMemo(() => {
    const value = new URLSearchParams(location.search).get('highlight')
    if (!value) return undefined
    const id = Number(value)
    return Number.isFinite(id) ? id : undefined
  }, [location.search])
  /** URL 中关联的异步任务 ID，用于进入列表后立即展示任务状态。 */
  const linkedTaskId = useMemo(() => {
    return new URLSearchParams(location.search).get('taskId') ?? undefined
  }, [location.search])

  useEffect(() => {
    tasksRef.current = tasks
  }, [tasks])

  /** 加载人群定义列表，并并行补充每个人群的最新统计。 */
  const fetchList = useCallback(async () => {
    setLoading(true)
    try {
      const res = await audienceApi.list()
      let list = res.data.list
      if (highlightedAudienceId != null && !list.some(item => item.id === highlightedAudienceId)) {
        try {
          const highlighted = await audienceApi.get(highlightedAudienceId)
          if (highlighted.data) {
            list = [highlighted.data, ...list]
          }
        } catch {
          // The target may have been deleted; keep the normal first page usable.
        }
      }
      setData(list)
      const statTargets = list.filter((item): item is AudienceDefinition & { id: number } => item.id != null)
      const statResults = await Promise.allSettled(
        statTargets.map(item => audienceApi.stat(item.id)),
      )
      // 将 stat 结果重组为 id -> stat，便于表格列 O(1) 读取
      const nextStats: Record<number, AudienceStat> = {}
      statResults.forEach((result, index) => {
        const id = statTargets[index]?.id
        if (result.status === 'fulfilled' && id != null && result.value.data) {
          nextStats[id] = result.value.data
        }
      })
      setStats(nextStats)
    } finally {
      setLoading(false)
    }
  }, [highlightedAudienceId])

  useEffect(() => {
    fetchList()
  }, [fetchList])

  /** 轮询人群计算任务，任务结束或列表无运行任务时回补统计数据。 */
  const pollAudienceTasks = useCallback(async (refreshWhenIdle = true) => {
    const ids = data.map(item => item.id).filter((id): id is number => id != null)
    if (ids.length === 0) {
      setTasks({})
      return
    }
    const res = await taskApi.list({
      taskType: 'AUDIENCE_COMPUTE',
      bizType: 'AUDIENCE',
      bizIds: ids.join(','),
      statuses: 'QUEUED,RUNNING',
    })
    const nextTasks: Record<number, AsyncTask> = {}
    for (const task of res.data) {
      const audienceId = Number(task.bizId)
      if (!Number.isNaN(audienceId) && nextTasks[audienceId] == null) {
        nextTasks[audienceId] = task
      }
    }
    const completedAudienceIds = Object.entries(tasksRef.current)
      .filter(([, task]) => task.status === 'QUEUED' || task.status === 'RUNNING')
      .map(([audienceId]) => Number(audienceId))
      .filter(audienceId => nextTasks[audienceId] == null)
    setTasks(nextTasks)
    setPollFailureCount(0)
    if (refreshWhenIdle && (completedAudienceIds.length > 0 || !hasRunningAudienceTasks(res.data))) {
      await fetchList()
    }
  }, [data, fetchList])

  useEffect(() => {
    if (!linkedTaskId) return
    let canceled = false
    taskApi.get(linkedTaskId)
      .then(res => {
        if (canceled || !res.data) return
        const audienceId = Number(res.data.bizId)
        if (Number.isNaN(audienceId)) return
        setTasks(prev => ({ ...prev, [audienceId]: res.data }))
        if (!data.some(item => item.id === audienceId)) {
          fetchList().catch(() => undefined)
        }
      })
      .catch(() => undefined)
    return () => {
      canceled = true
    }
  }, [data, fetchList, linkedTaskId])

  useEffect(() => {
    if (data.length === 0) return
    pollAudienceTasks(false).catch(() => setPollFailureCount(count => count + 1))
  }, [data, pollAudienceTasks])

  useEffect(() => {
    const activeTasks = Object.values(tasks).filter(task => task.status === 'QUEUED' || task.status === 'RUNNING')
    if (activeTasks.length === 0) {
      if (pollTimerRef.current != null) {
        window.clearTimeout(pollTimerRef.current)
        pollTimerRef.current = null
      }
      return
    }

    const delay = getNextAudiencePollDelay(pollFailureCount, document.hidden)
    pollTimerRef.current = window.setTimeout(async () => {
      try {
        await pollAudienceTasks()
      } catch {
        setPollFailureCount(count => count + 1)
      }
    }, delay)

    return () => {
      if (pollTimerRef.current != null) {
        window.clearTimeout(pollTimerRef.current)
        pollTimerRef.current = null
      }
    }
  }, [pollAudienceTasks, pollFailureCount, tasks])

  useEffect(() => {
    /** 页面从隐藏回到可见时立即刷新运行中的任务，避免等待下一轮定时器。 */
    const handleVisibilityChange = () => {
      if (document.hidden || !hasRunningAudienceTasks(Object.values(tasks))) return
      pollAudienceTasks().catch(() => setPollFailureCount(count => count + 1))
    }
    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange)
  }, [pollAudienceTasks, tasks])

  // 手动触发重算：只发“开始计算”指令，实际计算异步进行。
  const handleCompute = async (id: number) => {
    const res = await audienceApi.compute(id)
    setTasks(prev => ({
      ...prev,
      [id]: {
        taskId: res.data.taskId,
        taskType: 'AUDIENCE_COMPUTE',
        bizType: 'AUDIENCE',
        bizId: String(id),
        title: '人群计算',
        status: res.data.status,
        progress: res.data.status === 'RUNNING' ? 5 : 0,
      },
    }))
    message.success('已开始计算，完成后会自动更新结果')
  }

  /** 删除人群定义并刷新列表。 */
  const handleDelete = async (id: number) => {
    await audienceApi.delete(id)
    message.success('已删除')
    fetchList()
  }

  // 列表列定义：状态/规模/最后计算时间都来自 stat 表。
  // 因为 stat 是异步补充的，所以列渲染要能容忍 stat 未加载完成。
  const columns: ColumnsType<AudienceDefinition> = [
    { title: '名称', dataIndex: 'name' },
    { title: '计算策略', dataIndex: 'evaluationStrategy', width: 120 },
    {
      title: '默认发送人群',
      width: 140,
      render: (_, record) => <Tag>{snapshotModeLabel(record.defaultSnapshotMode)}</Tag>,
    },
    {
      title: '状态',
      width: 120,
      render: (_, record) => {
        const stat = record.id != null ? stats[record.id] : undefined
        if (!stat) {
          const task = record.id != null ? tasks[record.id] : undefined
          if (!task) return <Tag>-</Tag>
        }
        const task = record.id != null ? tasks[record.id] : undefined
        const status = getAudienceDisplayStatus(stat, task)
        const meta = STATUS_MAP[status] ?? { label: status, color: 'default' }
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: '人群规模',
      width: 120,
      render: (_, record) => {
        const stat = record.id != null ? stats[record.id] : undefined
        return stat?.estimatedSize != null ? stat.estimatedSize.toLocaleString() : '-'
      },
    },
    {
      title: '最后计算',
      width: 180,
      render: (_, record) => {
        const stat = record.id != null ? stats[record.id] : undefined
        return stat?.computedAt ? stat.computedAt.replace('T', ' ').slice(0, 19) : '-'
      },
    },
    {
      title: '操作',
      width: 220,
      render: (_, record) => {
        const task = record.id != null ? tasks[record.id] : undefined
        const computing = task?.status === 'QUEUED' || task?.status === 'RUNNING'
        return (
          <Space>
            <Button size="small" onClick={() => navigate(`/audiences/${record.id}/edit`)}>编辑</Button>
            <Tooltip title={computing ? '计算任务进行中' : '立即触发重新计算'}>
              <Button
                size="small"
                icon={<ThunderboltOutlined />}
                disabled={computing}
                onClick={() => handleCompute(record.id!)}
              >
                {computing ? '计算中' : '计算'}
              </Button>
            </Tooltip>
            <Popconfirm title="确认删除？" onConfirm={() => handleDelete(record.id!)}>
              <Button size="small" danger>删除</Button>
            </Popconfirm>
          </Space>
        )
      },
    },
  ]

  return (
    <div>
      <style>{`
        .audience-row-highlight > td {
          background: #fff7e6 !important;
          transition: background .2s;
        }
      `}</style>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0 }}>人群管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/audiences/new')}>
          新建人群
        </Button>
      </div>
      <Table
        rowKey="id"
        dataSource={data}
        columns={columns}
        loading={loading}
        rowClassName={record => record.id === highlightedAudienceId ? 'audience-row-highlight' : ''}
      />
    </div>
  )
}
