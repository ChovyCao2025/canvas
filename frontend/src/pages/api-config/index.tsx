import { useEffect, useState } from 'react'
import {
  Button, Table, Tag, Space, Modal, Form, Input,
  Select, Switch, message, Typography, Popconfirm,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { apiDefinitionApi } from '../../services/api'

const { Title } = Typography

interface ApiDefinition {
  id: number
  name: string
  apiKey: string
  url: string
  method: string
  bizLine?: string
  description?: string
  enabled: number
}

export default function ApiConfigPage() {
  const [data, setData] = useState<ApiDefinition[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRecord, setEditingRecord] = useState<ApiDefinition | null>(null)
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)

  const fetchList = async (p = page) => {
    setLoading(true)
    try {
      const res = await apiDefinitionApi.list({ page: p, size: 20 })
      setData(res.data.list)
      setTotal(res.data.total)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchList(1) }, [])

  const openCreate = () => {
    setEditingRecord(null)
    form.resetFields()
    form.setFieldsValue({ method: 'POST', enabled: true })
    setModalVisible(true)
  }

  const openEdit = (record: ApiDefinition) => {
    setEditingRecord(record)
    form.setFieldsValue({
      ...record,
      enabled: record.enabled === 1,
    })
    setModalVisible(true)
  }

  const handleOk = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      const body = { ...values, enabled: values.enabled ? 1 : 0 }
      if (editingRecord) {
        await apiDefinitionApi.update(editingRecord.id, body)
        message.success('更新成功')
      } else {
        await apiDefinitionApi.create(body)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchList(editingRecord ? page : 1)
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: number) => {
    await apiDefinitionApi.delete(id)
    message.success('已删除')
    fetchList(page)
  }

  const columns: ColumnsType<ApiDefinition> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '名称', dataIndex: 'name' },
    { title: 'apiKey', dataIndex: 'apiKey', ellipsis: true },
    { title: 'URL', dataIndex: 'url', ellipsis: true },
    {
      title: '方法',
      dataIndex: 'method',
      width: 80,
      render: (m: string) => (
        <Tag color={m === 'GET' ? 'blue' : 'green'}>{m}</Tag>
      ),
    },
    { title: '业务线', dataIndex: 'bizLine', width: 100 },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      render: (v: number) => (
        <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '操作',
      width: 120,
      render: (_, record) => (
        <Space size={4}>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEdit(record)}
          />
          <Popconfirm
            title="确认删除？"
            onConfirm={() => handleDelete(record.id)}
            okText="删除"
            okType="danger"
            cancelText="取消"
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>API 管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新建 API
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
        title={editingRecord ? '编辑 API' : '新建 API'}
        open={modalVisible}
        onOk={handleOk}
        onCancel={() => setModalVisible(false)}
        confirmLoading={submitting}
        okText={editingRecord ? '保存' : '创建'}
        cancelText="取消"
        width={560}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="如：查询用户信息" />
          </Form.Item>
          <Form.Item name="apiKey" label="apiKey" rules={[{ required: true, message: '请输入 apiKey' }]}>
            <Input placeholder="如：query_user_info" />
          </Form.Item>
          <Form.Item name="url" label="URL" rules={[{ required: true, message: '请输入 URL' }]}>
            <Input placeholder="如：/api/v1/user/info" />
          </Form.Item>
          <Form.Item name="method" label="HTTP 方法" rules={[{ required: true }]}>
            <Select options={[{ value: 'GET', label: 'GET' }, { value: 'POST', label: 'POST' }]} />
          </Form.Item>
          <Form.Item name="bizLine" label="业务线">
            <Input placeholder="如：FLIGHT" />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
