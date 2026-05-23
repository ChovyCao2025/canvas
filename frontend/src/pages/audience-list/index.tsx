import { useEffect, useState } from 'react'
import { Button, message, Popconfirm, Space, Table, Tag, Tooltip, Typography } from 'antd'
import { PlusOutlined, ThunderboltOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import { audienceApi, type AudienceDefinition, type AudienceStat } from '../../services/audienceApi'

const { Title } = Typography

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待计算', color: 'default' },
  COMPUTING: { label: '计算中', color: 'processing' },
  READY: { label: '就绪', color: 'success' },
  FAILED: { label: '失败', color: 'error' },
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
  // 列表与统计分开维护：列表来自 definition，状态/规模来自 stat
  const [data, setData] = useState<AudienceDefinition[]>([])
  const [stats, setStats] = useState<Record<number, AudienceStat>>({})
  const [loading, setLoading] = useState(false)

  // 拉取定义列表并并发查询每个人群的统计状态。
  // 这里用 Promise.allSettled，避免单个人群 stat 失败导致整表失败。
  const fetchList = async () => {
    setLoading(true)
    try {
      const res = await audienceApi.list()
      const list = res.data.list
      setData(list)
      const statResults = await Promise.allSettled(
        list.filter(item => item.id != null).map(item => audienceApi.stat(item.id!)),
      )
      // 将 stat 结果重组为 id -> stat，便于表格列 O(1) 读取
      const nextStats: Record<number, AudienceStat> = {}
      statResults.forEach((result, index) => {
        const id = list[index]?.id
        if (result.status === 'fulfilled' && id != null && result.value.data) {
          nextStats[id] = result.value.data
        }
      })
      setStats(nextStats)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchList()
  }, [])

  // 手动触发重算：只发“开始计算”指令，实际计算异步进行。
  const handleCompute = async (id: number) => {
    await audienceApi.compute(id)
    message.success('已触发计算，请稍后刷新查看结果')
    fetchList()
  }

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
      title: '状态',
      width: 120,
      render: (_, record) => {
        const stat = record.id != null ? stats[record.id] : undefined
        if (!stat) {
          return <Tag>-</Tag>
        }
        const meta = STATUS_MAP[stat.status] ?? { label: stat.status, color: 'default' }
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
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => navigate(`/audiences/${record.id}/edit`)}>编辑</Button>
          <Tooltip title="立即触发重新计算">
            <Button size="small" icon={<ThunderboltOutlined />} onClick={() => handleCompute(record.id!)}>
              计算
            </Button>
          </Tooltip>
          <Popconfirm title="确认删除？" onConfirm={() => handleDelete(record.id!)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0 }}>人群管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/audiences/new')}>
          新建人群
        </Button>
      </div>
      <Table rowKey="id" dataSource={data} columns={columns} loading={loading} />
    </div>
  )
}
