import { useEffect, useState } from 'react'
import {
  Button, Table, Tag, Space, Modal, Form, Input,
  message, Typography, Tooltip, Dropdown,
} from 'antd'
import {
  PlusOutlined, EditOutlined, CloudUploadOutlined,
  StopOutlined, CopyOutlined, ThunderboltOutlined, BarChartOutlined, EyeOutlined,
  MoreOutlined,
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
  3: { label: '已归档', color: 'orange' },
  4: { label: '已停止', color: 'volcano' },
}

/**
 * 画布列表页：提供创建、发布、下线、克隆、归档等运营操作入口。
 *
 * 交互原则：
 * - 高风险动作（发布/下线/紧急停止/归档）都走确认弹窗；
 * - 列表页只负责“操作入口”，具体编排在编辑器页完成。
 */
export default function CanvasListPage() {
  const navigate = useNavigate()
  const { isAdmin } = useAuth()
  // 列表数据与分页态
  const [data, setData] = useState<Canvas[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  // 创建弹窗态
  const [createVisible, setCreateVisible] = useState(false)
  const [form] = Form.useForm()

  // 拉取画布分页列表：是所有列表变更后的统一刷新入口。
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

  // 创建新画布：成功后回到第一页刷新列表
  const handleCreate = async () => {
    const values = await form.validateFields()
    await canvasApi.create({ ...values })
    message.success('创建成功')
    setCreateVisible(false)
    form.resetFields()
    fetchList(1)
  }

  // 发布：把当前草稿版本设为线上生效版本。
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

  // 下线后不再接收新触发。
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

  // 紧急停止：用于事故处理，优先保证“快速止血”。
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

  // 克隆会复制草稿图结构，创建一份新的 DRAFT 画布。
  const handleClone = async (id: number) => {
    const res = await canvasApi.clone(id)
    message.success(`克隆成功，新画布 ID: ${res.data.id}`)
    fetchList()
  }

  const handleArchive = (id: number, name: string) => {
    Modal.confirm({
      title: '归档画布',
      content: (
        <div>
          <p>确认将「{name}」归档？</p>
          <ul style={{ color: '#8c8c8c', fontSize: 13, paddingLeft: 16, margin: '8px 0 0' }}>
            <li>画布将从列表中隐藏</li>
            <li>正在运行中的流程不受影响</li>
            <li>可联系管理员恢复</li>
          </ul>
        </div>
      ),
      okText: '确认归档',
      okType: 'danger',
      onOk: async () => {
        await canvasApi.archive(id)
        message.success('已归档')
        fetchList()
      },
    })
  }

  // 表格列定义：状态 + 操作按钮集中在列表页。
  // 按钮展示会根据状态和角色动态收敛，减少误操作。
  const columns: ColumnsType<Canvas> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    {
      title: '名称',
      dataIndex: 'name',
      render: (name, record) => (
        <Space size={6}>
          <Button type="link" onClick={() => navigate(`/canvas/${record.id}/edit`)}>
            {name}
          </Button>
          {record.isExample === 1 && <Tag color="blue">示例</Tag>}
        </Space>
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
          <Tooltip title="查看">
            <Button size="small" icon={<EyeOutlined />}
              onClick={() => navigate(`/canvas/${record.id}/edit?readonly=true`)} />
          </Tooltip>
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

          <Dropdown
            menu={{
              items: [
                {
                  key: 'archive',
                  label: <span style={{ color: '#ff4d4f' }}>归档画布</span>,
                  onClick: () => handleArchive(record.id, record.name),
                },
              ],
            }}
            trigger={['click']}
          >
            <Button size="small" icon={<MoreOutlined />} />
          </Dropdown>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0 }}>旅程管理</Title>
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
