import { useEffect, useState } from 'react'
import {
  Button, Form, Input, InputNumber, Modal, Select,
  Space, Switch, Table, Tag, Typography, message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { SystemOption } from '../../types'
import { SYSTEM_OPTION_CATEGORIES, systemOptionsApi } from '../../services/systemOptions'
import { tenantApi, type Tenant } from '../../services/api'
import { useAuth } from '../../context/AuthContext'

const { Title, Text } = Typography

export default function SystemOptionsPage() {
  const [data, setData] = useState<SystemOption[]>([])
  const [loading, setLoading] = useState(false)
  const [category, setCategory] = useState<string>()
  const [enabled, setEnabled] = useState<number>()
  const [tenantId, setTenantId] = useState<number>()
  const [tenants, setTenants] = useState<Tenant[]>([])
  const [keyword, setKeyword] = useState('')
  const [editing, setEditing] = useState<SystemOption | null>(null)
  const [form] = Form.useForm()
  const { isSuperAdmin } = useAuth()

  const fetchList = async () => {
    setLoading(true)
    try {
      const res = await systemOptionsApi.adminList({
        category,
        enabled,
        tenantId,
        keyword: keyword.trim() || undefined,
      })
      setData(res.data.list)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchList() }, [category, enabled, tenantId])

  useEffect(() => {
    if (!isSuperAdmin) return
    tenantApi.list().then(res => setTenants(res.data))
  }, [isSuperAdmin])

  const openEdit = (record: SystemOption) => {
    setEditing(record)
    form.setFieldsValue({
      label: record.label,
      description: record.description,
      sortOrder: record.sortOrder,
      enabled: record.enabled === 1,
    })
  }

  const save = async () => {
    if (!editing) return
    const values = await form.validateFields()
    await systemOptionsApi.update(editing.id, {
      label: values.label,
      description: values.description,
      sortOrder: values.sortOrder,
      enabled: values.enabled ? 1 : 0,
    })
    message.success('保存成功')
    setEditing(null)
    fetchList()
  }

  const columns: ColumnsType<SystemOption> = [
    { title: '分类', dataIndex: 'category', width: 220 },
    {
      title: '作用域',
      dataIndex: 'tenantId',
      width: 120,
      render: value => value ? <Tag color="purple">租户 {value}</Tag> : <Tag color="blue">全局</Tag>,
    },
    { title: 'Key', dataIndex: 'optionKey', width: 180 },
    { title: '显示名', dataIndex: 'label', width: 220 },
    { title: '描述', dataIndex: 'description', ellipsis: true },
    { title: '排序', dataIndex: 'sortOrder', width: 88 },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 88,
      render: value => value === 1 ? <Tag color="green">启用</Tag> : <Tag>禁用</Tag>,
    },
    {
      title: '内置',
      dataIndex: 'systemBuiltin',
      width: 88,
      render: value => value === 1 ? <Tag color="blue">内置</Tag> : <Tag>自定义</Tag>,
    },
    { title: '更新时间', dataIndex: 'updatedAt', width: 180 },
    {
      title: '操作',
      width: 96,
      fixed: 'right',
      render: (_, record) => <Button size="small" onClick={() => openEdit(record)}>编辑</Button>,
    },
  ]

  return (
    <div style={{ minHeight: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>系统选项配置</Title>
          <Text type="secondary">禁用只影响新配置，历史画布仍会保留并展示已有值。</Text>
        </div>
        <Space wrap>
          <Select
            allowClear
            showSearch
            placeholder="筛选分类"
            style={{ width: 260 }}
            options={SYSTEM_OPTION_CATEGORIES}
            value={category}
            onChange={setCategory}
            optionFilterProp="label"
          />
          {isSuperAdmin ? (
            <Select
              allowClear
              showSearch
              placeholder="筛选租户"
              style={{ width: 220 }}
              value={tenantId}
              onChange={setTenantId}
              options={tenants.map(tenant => ({
                value: tenant.id,
                label: `${tenant.name} (${tenant.tenantKey})`,
              }))}
              optionFilterProp="label"
            />
          ) : null}
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 120 }}
            value={enabled}
            onChange={setEnabled}
            options={[
              { value: 1, label: '启用' },
              { value: 0, label: '禁用' },
            ]}
          />
          <Input.Search
            allowClear
            placeholder="搜索 Key/显示名/描述"
            value={keyword}
            onChange={event => setKeyword(event.target.value)}
            onSearch={fetchList}
            style={{ width: 240 }}
          />
        </Space>
      </div>

      <Table
        rowKey="id"
        dataSource={data}
        columns={columns}
        loading={loading}
        pagination={{ pageSize: 20, showSizeChanger: true }}
        scroll={{ x: 1180 }}
      />

      <Modal
        title="编辑系统选项"
        open={!!editing}
        onOk={save}
        onCancel={() => setEditing(null)}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="分类">
            <Input value={editing?.category} disabled />
          </Form.Item>
          <Form.Item label="Key">
            <Input value={editing?.optionKey} disabled />
          </Form.Item>
          <Form.Item name="label" label="显示名" rules={[{ required: true, message: '请输入显示名' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序" rules={[{ required: true, message: '请输入排序' }]}>
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
