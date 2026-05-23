import { useEffect, useState } from 'react'
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Space, Switch, Table, Tag, Typography, message } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { identityTypeApi } from '../../services/api'
import type { IdentityType } from '../../types'

const { Title } = Typography

type IdentityTypeFormValues = {
  code: string
  name: string
  priority: number
  description?: string
  enabled: boolean
  allowImport: boolean
  multiValue: boolean
  participateMapping: boolean
}

export default function IdentityTypesPage() {
  const [data, setData] = useState<IdentityType[]>([])
  const [loading, setLoading] = useState(false)
  const [visible, setVisible] = useState(false)
  const [editing, setEditing] = useState<IdentityType | null>(null)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm<IdentityTypeFormValues>()

  const fetchList = async () => {
    setLoading(true)
    try {
      const res = await identityTypeApi.list()
      setData(res.data.list)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchList()
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({
      priority: 100,
      enabled: true,
      allowImport: true,
      multiValue: false,
      participateMapping: true,
    })
    setVisible(true)
  }

  const openEdit = (record: IdentityType) => {
    setEditing(record)
    form.setFieldsValue({
      code: record.code,
      name: record.name,
      priority: record.priority,
      description: record.description,
      enabled: record.enabled === 1,
      allowImport: record.allowImport === 1,
      multiValue: record.multiValue === 1,
      participateMapping: record.participateMapping === 1,
    })
    setVisible(true)
  }

  const handleOk = async () => {
    const values = await form.validateFields()
    const body: Partial<IdentityType> = {
      ...values,
      enabled: values.enabled ? 1 : 0,
      allowImport: values.allowImport ? 1 : 0,
      multiValue: values.multiValue ? 1 : 0,
      participateMapping: values.participateMapping ? 1 : 0,
    }

    setSaving(true)
    try {
      if (editing) {
        await identityTypeApi.update(editing.id, body)
        message.success('更新成功')
      } else {
        await identityTypeApi.create(body)
        message.success('创建成功')
      }
      setVisible(false)
      fetchList()
    } finally {
      setSaving(false)
    }
  }

  const columns: ColumnsType<IdentityType> = [
    { title: '编码', dataIndex: 'code', render: (value: string) => <Typography.Text code>{value}</Typography.Text> },
    { title: '名称', dataIndex: 'name' },
    { title: '优先级', dataIndex: 'priority', width: 90 },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      render: (value: number) => <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '禁用'}</Tag>,
    },
    {
      title: '允许导入',
      dataIndex: 'allowImport',
      width: 96,
      render: (value: number) => <Tag color={value === 1 ? 'blue' : 'default'}>{value === 1 ? '允许' : '禁止'}</Tag>,
    },
    {
      title: '取值',
      dataIndex: 'multiValue',
      width: 80,
      render: (value: number) => (value === 1 ? '多值' : '单值'),
    },
    {
      title: '参与映射',
      dataIndex: 'participateMapping',
      width: 88,
      render: (value: number) => (value === 1 ? '是' : '否'),
    },
    {
      title: '操作',
      width: 100,
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm
            title="确认删除？"
            okText="删除"
            okType="danger"
            cancelText="取消"
            onConfirm={() => identityTypeApi.delete(record.id).then(() => {
              message.success('已删除')
              fetchList()
            })}
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0 }}>ID 类型配置</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建类型</Button>
      </div>

      <Table rowKey="id" dataSource={data} columns={columns} loading={loading} pagination={false} />

      <Modal
        title={editing ? '编辑 ID 类型' : '新建 ID 类型'}
        open={visible}
        onOk={handleOk}
        onCancel={() => setVisible(false)}
        confirmLoading={saving}
        okText={editing ? '保存' : '创建'}
        cancelText="取消"
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="code" label="编码" rules={[{ required: true, message: '请输入编码' }]}>
            <Input placeholder="如：mobile" disabled={!!editing} />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="如：手机号" />
          </Form.Item>
          <Form.Item name="priority" label="优先级" rules={[{ required: true, message: '请输入优先级' }]}>
            <InputNumber min={0} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
          <Form.Item name="allowImport" label="允许导入" valuePropName="checked">
            <Switch checkedChildren="允许" unCheckedChildren="禁止" />
          </Form.Item>
          <Form.Item name="multiValue" label="取值方式" valuePropName="checked">
            <Switch checkedChildren="多值" unCheckedChildren="单值" />
          </Form.Item>
          <Form.Item name="participateMapping" label="参与映射" valuePropName="checked">
            <Switch checkedChildren="是" unCheckedChildren="否" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
