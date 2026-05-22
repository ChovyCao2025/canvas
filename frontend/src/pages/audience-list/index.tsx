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

export default function AudienceListPage() {
  const navigate = useNavigate()
  const [data, setData] = useState<AudienceDefinition[]>([])
  const [stats, setStats] = useState<Record<number, AudienceStat>>({})
  const [loading, setLoading] = useState(false)

  const fetchList = async () => {
    setLoading(true)
    try {
      const res = await audienceApi.list()
      const list = res.data.list
      setData(list)
      const statResults = await Promise.allSettled(
        list.filter(item => item.id != null).map(item => audienceApi.stat(item.id!)),
      )
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
