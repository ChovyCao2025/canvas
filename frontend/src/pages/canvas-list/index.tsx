import { useEffect, useState } from 'react'
import {
  Button, Table, Tag, Space, Modal, Form, Input,
  message, Typography, Tooltip, Popconfirm,
} from 'antd'
import {
  PlusOutlined, EditOutlined, CloudUploadOutlined,
  StopOutlined, CopyOutlined, ThunderboltOutlined, BarChartOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import type { ColumnsType } from 'antd/es/table'
import { canvasApi } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import type { Canvas } from '../../types'

const { Title } = Typography

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿',   color: 'default' },
  1: { label: '已发布', color: 'green' },
  2: { label: '已下线', color: 'red' },
  4: { label: '已停止', color: 'volcano' },
}

export default function CanvasListPage() {
  const navigate = useNavigate()
  const { isAdmin } = useAuth()
  const [data, setData] = useState<Canvas[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [createVisible, setCreateVisible] = useState(false)
  const [form] = Form.useForm()

  const fetchList = async (p = page) => {
    setLoading(true)
    try {
      const res = await canvasApi.list({ page: p, size: 20 })
      setData(res.data.list)
      setTotal(res.data.total)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchList(1) }, [])

  const handleCreate = async () => {
    const values = await form.validateFields()
    await canvasApi.create({ ...values })
    message.success('创建成功')
    setCreateVisible(false)
    form.resetFields()
    fetchList(1)
  }

  const handlePublish = async (id: number) => {
    Modal.confirm({
      title: '确认发布？',
      content: '发布后该画布将对执行引擎生效',
      onOk: async () => {
        await canvasApi.publish(id)
        message.success('发布成功')
        fetchList()
      },
    })
  }

  const handleOffline = async (id: number) => {
    Modal.confirm({
      title: '确认下线？',
      okType: 'danger',
      onOk: async () => {
        await canvasApi.offline(id)
        message.success('已下线')
        fetchList()
      },
    })
  }

  const handleKill = async (id: number) => {
    Modal.confirm({
      title: '紧急停止',
      content: '将立即停止所有新触发，正在执行的实例将被标记为 KILLED',
      okType: 'danger',
      okText: '确认停止',
      onOk: async () => {
        await canvasApi.kill(id, 'GRACEFUL')
        message.warning('已紧急停止')
        fetchList()
      },
    })
  }

  const handleClone = async (id: number) => {
    const res = await canvasApi.clone(id)
    message.success(`克隆成功，新画布 ID: ${res.data.id}`)
    fetchList()
  }

  const columns: ColumnsType<Canvas> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    {
      title: '名称',
      dataIndex: 'name',
      render: (name, record) => (
        <Button type="link" onClick={() => navigate(`/canvas/${record.id}/edit`)}>
          {name}
        </Button>
      ),
    },
    { title: '描述', dataIndex: 'description', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: number) => {
        const { label, color } = STATUS_MAP[status] ?? { label: '未知', color: 'default' }
        return <Tag color={color}>{label}</Tag>
      },
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 180,
      render: (v: string) => v?.replace('T', ' ').slice(0, 19),
    },
    {
      title: '操作',
      width: 240,
      render: (_, record) => (
        <Space size={4}>
          <Tooltip title="编辑">
            <Button size="small" icon={<EditOutlined />}
              onClick={() => navigate(`/canvas/${record.id}/edit`)} />
          </Tooltip>

          {record.status !== 1 && isAdmin && (
            <Tooltip title="发布">
              <Button size="small" type="primary" icon={<CloudUploadOutlined />}
                onClick={() => handlePublish(record.id)} />
            </Tooltip>
          )}

          {record.status === 1 && isAdmin && (
            <Tooltip title="下线">
              <Button size="small" danger icon={<StopOutlined />}
                onClick={() => handleOffline(record.id)} />
            </Tooltip>
          )}

          {record.status === 1 && isAdmin && (
            <Tooltip title="紧急停止">
              <Button size="small" danger ghost icon={<ThunderboltOutlined />}
                onClick={() => handleKill(record.id)} />
            </Tooltip>
          )}

          <Tooltip title="克隆">
            <Button size="small" icon={<CopyOutlined />}
              onClick={() => handleClone(record.id)} />
          </Tooltip>

          <Tooltip title="效果看板">
            <Button size="small" icon={<BarChartOutlined />}
              onClick={() => navigate(`/canvas/${record.id}/stats`)} />
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>营销画布</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>
          新建画布
        </Button>
      </div>

      <Table
        rowKey="id"
        dataSource={data}
        columns={columns}
        loading={loading}
        pagination={{
          total,
          pageSize: 20,
          current: page,
          onChange: (p) => { setPage(p); fetchList(p) },
        }}
      />

      <Modal
        title="新建画布"
        open={createVisible}
        onOk={handleCreate}
        onCancel={() => { setCreateVisible(false); form.resetFields() }}
        okText="创建"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="画布名称" rules={[{ required: true }]}>
            <Input placeholder="例：新用户机票发券流程" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
