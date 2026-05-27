import { useEffect, useState } from 'react'
import {
  Button, Form, Input, Modal, Space, Table, Tag, Typography, message,
  type TableColumnsType,
} from 'antd'
import { PlusOutlined, PlayCircleOutlined, StopOutlined } from '@ant-design/icons'
import { tenantApi, type Tenant, type TenantUsage } from '../../services/api'

const { Title, Text } = Typography

export default function TenantAdminPage() {
  const [tenants, setTenants] = useState<Tenant[]>([])
  const [usage, setUsage] = useState<Record<number, TenantUsage>>({})
  const [loading, setLoading] = useState(false)
  const [createVisible, setCreateVisible] = useState(false)
  const [form] = Form.useForm()

  const fetchTenants = async () => {
    setLoading(true)
    try {
      const res = await tenantApi.list()
      setTenants(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchTenants() }, [])

  const handleCreate = async () => {
    const values = await form.validateFields()
    await tenantApi.create(values)
    message.success('租户已创建')
    setCreateVisible(false)
    form.resetFields()
    fetchTenants()
  }

  const changeStatus = (tenant: Tenant) => {
    const active = tenant.status === 'ACTIVE'
    Modal.confirm({
      title: active ? '确认禁用该租户？' : '确认启用该租户？',
      okType: active ? 'danger' : 'primary',
      onOk: async () => {
        if (active) await tenantApi.disable(tenant.id)
        else await tenantApi.activate(tenant.id)
        message.success(active ? '已禁用' : '已启用')
        fetchTenants()
      },
    })
  }

  const loadUsage = async (tenantId: number) => {
    const res = await tenantApi.usage(tenantId)
    setUsage(prev => ({ ...prev, [tenantId]: res.data }))
  }

  const columns: TableColumnsType<Tenant> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '租户名', dataIndex: 'name', width: 180 },
    { title: 'Key', dataIndex: 'tenantKey', width: 180 },
    { title: '套餐', dataIndex: 'planCode', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 96,
      render: value => value === 'ACTIVE' ? <Tag color="green">启用</Tag> : <Tag>禁用</Tag>,
    },
    {
      title: '用量',
      width: 260,
      render: (_, record) => {
        const item = usage[record.id]
        if (!item) return <Button size="small" onClick={() => loadUsage(record.id)}>查看用量</Button>
        return (
          <Text type="secondary">
            画布 {item.canvasCount} / 发布 {item.publishedCanvasCount} / 执行 {item.executionCount} / DLQ {item.dlqCount}
          </Text>
        )
      },
    },
    { title: '创建人', dataIndex: 'createdBy', width: 140 },
    { title: '更新时间', dataIndex: 'updatedAt', width: 180 },
    {
      title: '操作',
      width: 96,
      fixed: 'right',
      render: (_, record) => (
        <Button
          size="small"
          danger={record.status === 'ACTIVE'}
          icon={record.status === 'ACTIVE' ? <StopOutlined /> : <PlayCircleOutlined />}
          onClick={() => changeStatus(record)}
        />
      ),
    },
  ]

  return (
    <div style={{ minHeight: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>租户管理</Title>
          <Text type="secondary">管理 SaaS 租户、启停状态和基础用量。</Text>
        </div>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>
            新建租户
          </Button>
        </Space>
      </div>

      <Table
        rowKey="id"
        dataSource={tenants}
        columns={columns}
        loading={loading}
        pagination={false}
        scroll={{ x: 1180 }}
      />

      <Modal
        title="新建租户"
        open={createVisible}
        onOk={handleCreate}
        onCancel={() => { setCreateVisible(false); form.resetFields() }}
        okText="创建"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="租户名" rules={[{ required: true, message: '请输入租户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="tenantKey" label="租户 Key" rules={[{ required: true, message: '请输入租户 Key' }]}>
            <Input placeholder="例如 acme" />
          </Form.Item>
          <Form.Item name="planCode" label="套餐" initialValue="standard">
            <Input />
          </Form.Item>
          <Form.Item name="quotaJson" label="配额 JSON">
            <Input.TextArea rows={4} placeholder='{"maxCanvas":100}' />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
