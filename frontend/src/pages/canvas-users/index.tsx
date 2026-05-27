/**
 * 页面职责：单个画布的命中用户页，展示进入该画布的用户、标签和执行统计。
 *
 * 维护说明：用户 ID 链接到 CDP 详情页，便于从画布结果追溯用户画像。
 */
import { useEffect, useState } from 'react'
import { Button, Drawer, Space, Table, Tag, Typography } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate, useParams } from 'react-router-dom'
import { cdpApi, type CanvasUserRow } from '../../services/cdpApi'
import { formatDateTime, formatExecutionStatus, tagColor } from '../cdp-users/cdpPresentation'

/** 画布用户页标题和文本组件别名。 */
const { Title, Text } = Typography

/** 单画布命中用户页面，展示用户、标签和该画布下的执行统计。 */
export default function CanvasUsersPage() {
  const { id = '' } = useParams()
  const canvasId = Number(id)
  const navigate = useNavigate()
  const [rows, setRows] = useState<CanvasUserRow[]>([])
  const [selected, setSelected] = useState<CanvasUserRow | null>(null)
  const [executions, setExecutions] = useState<any[]>([])
  const [loading, setLoading] = useState(false)

  /** 加载当前画布命中过的用户列表。 */
  const load = async () => {
    setLoading(true)
    try {
      const res = await cdpApi.listCanvasUsers(canvasId)
      setRows(res.data ?? [])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { if (canvasId) load() }, [canvasId])

  /** 打开用户执行明细抽屉，并加载该用户在当前画布下的执行记录。 */
  const openUser = async (row: CanvasUserRow) => {
    setSelected(row)
    const res = await cdpApi.listCanvasUserExecutions(canvasId, row.userId)
    setExecutions(res.data ?? [])
  }

  const columns: ColumnsType<CanvasUserRow> = [
    { title: '用户 ID', dataIndex: 'userId', render: (_, row) => <Button type="link" onClick={() => openUser(row)}>{row.userId}</Button> },
    { title: '执行次数', dataIndex: 'executionCount', width: 100, align: 'right' },
    { title: '成功', dataIndex: 'successCount', width: 80, align: 'right' },
    { title: '失败', dataIndex: 'failedCount', width: 80, align: 'right' },
    { title: '最近状态', dataIndex: 'latestStatus', width: 100, render: v => { const s = formatExecutionStatus(v); return <Tag color={s.color}>{s.label}</Tag> } },
    { title: '标签', dataIndex: 'tags', render: (tags?: CanvasUserRow['tags']) => tags?.map(tag => <Tag key={tag.tagCode} color={tagColor(tag.tagCode)}>{tag.tagName || tag.tagCode}</Tag>) },
    { title: '最近进入', dataIndex: 'lastEnteredAt', width: 180, render: formatDateTime },
  ]

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} />
        <Title level={4} style={{ margin: 0 }}>画布用户数据</Title>
      </Space>
      <Table rowKey="userId" columns={columns} dataSource={rows} loading={loading} />

      <Drawer title={selected?.userId} open={!!selected} width={640} onClose={() => setSelected(null)}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Button type="link" onClick={() => selected && navigate(`/cdp/users/${encodeURIComponent(selected.userId)}`)}>打开用户详情</Button>
          <Text strong>执行记录</Text>
          <Table rowKey="id" dataSource={executions} pagination={false} size="small"
            columns={[
              { title: '执行 ID', dataIndex: 'id', ellipsis: true },
              { title: '状态', dataIndex: 'status' },
              { title: '触发', dataIndex: 'triggerType' },
              { title: '时间', dataIndex: 'createdAt', render: formatDateTime },
            ]} />
        </Space>
      </Drawer>
    </div>
  )
}
