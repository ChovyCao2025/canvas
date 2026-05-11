import { useEffect, useState } from 'react'
import {
  Button, Table, Tag, Space, Modal, Form, Input,
  message, Typography, Tooltip,
} from 'antd'
import { PlusOutlined, EditOutlined, CloudUploadOutlined, StopOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import type { ColumnsType } from 'antd/es/table'
import { canvasApi } from '../../services/api'
import type { Canvas } from '../../types'

const { Title } = Typography

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '已发布', color: 'green' },
  2: { label: '已下线', color: 'red' },
}

export default function CanvasListPage() {
  const navigate = useNavigate()
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
    await canvasApi.create({ ...values, createdBy: 'admin' })
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
      content: '下线后画布将停止触发新的执行',
      okType: 'danger',
      onOk: async () => {
        await canvasApi.offline(id)
        message.success('已下线')
        fetchList()
      },
    })
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
    { title: '创建人', dataIndex: 'createdBy', width: 120 },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 180,
      render: (v: string) => v?.replace('T', ' ').slice(0, 19),
    },
    {
      title: '操作',
      width: 200,
      render: (_, record) => (
        <Space>
          <Tooltip title="编辑画布">
            <Button
              size="small"
              icon={<EditOutlined />}
              onClick={() => navigate(`/canvas/${record.id}/edit`)}
            />
          </Tooltip>
          {record.status !== 1 && (
            <Tooltip title="发布">
              <Button
                size="small"
                type="primary"
                icon={<CloudUploadOutlined />}
                onClick={() => handlePublish(record.id)}
              />
            </Tooltip>
          )}
          {record.status === 1 && (
            <Tooltip title="下线">
              <Button
                size="small"
                danger
                icon={<StopOutlined />}
                onClick={() => handleOffline(record.id)}
              />
            </Tooltip>
          )}
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
          <Form.Item name="name" label="画布名称" rules={[{ required: true, message: '请输入画布名称' }]}>
            <Input placeholder="例：新用户机票发券流程" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="（可选）描述这个画布的业务目标" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
