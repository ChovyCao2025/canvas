import { useEffect, useState } from 'react'
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Typography, message } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { dataSourceConfigApi, type DataSourceConfig } from '../../services/dataSourceConfigApi'

const { Title } = Typography

type DataSourceFormValues = Omit<DataSourceConfig, 'enabled'> & {
  enabled?: boolean
}

export default function DataSourceConfigPage() {
  const [data, setData] = useState<DataSourceConfig[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(false)
  const [visible, setVisible] = useState(false)
  const [saving, setSaving] = useState(false)
  const [editing, setEditing] = useState<DataSourceConfig | null>(null)
  const [form] = Form.useForm<DataSourceFormValues>()

  const fetchList = async (p = page) => {
    setLoading(true)
    try {
      const res = await dataSourceConfigApi.list({ page: p, size: 20 })
      setData(res.data.list)
      setTotal(res.data.total)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchList(1) }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({
      type: 'JDBC',
      enabled: true,
      driverClassName: 'com.mysql.cj.jdbc.Driver',
    })
    setVisible(true)
  }

  const openEdit = (record: DataSourceConfig) => {
    setEditing(record)
    form.setFieldsValue({
      ...record,
      enabled: record.enabled === 1,
    })
    setVisible(true)
  }

  const handleOk = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      const body: DataSourceConfig = {
        ...values,
        type: 'JDBC',
        enabled: values.enabled ? 1 : 0,
      }
      if (editing?.id) {
        await dataSourceConfigApi.update(editing.id, body)
        message.success('更新成功')
      } else {
        await dataSourceConfigApi.create(body)
        message.success('创建成功')
      }
      setVisible(false)
      fetchList(editing ? page : 1)
    } finally {
      setSaving(false)
    }
  }

  const columns: ColumnsType<DataSourceConfig> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '名称', dataIndex: 'name', width: 180 },
    { title: '类型', dataIndex: 'type', width: 90, render: value => <Tag>{value}</Tag> },
    { title: 'JDBC URL', dataIndex: 'url', ellipsis: true },
    { title: '用户名', dataIndex: 'username', width: 140 },
    { title: 'Driver Class', dataIndex: 'driverClassName', width: 220, ellipsis: true },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 90,
      render: value => <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '禁用'}</Tag>,
    },
    {
      title: '操作',
      width: 120,
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm
            title="确认删除？"
            okText="删除"
            okType="danger"
            cancelText="取消"
            onConfirm={() => dataSourceConfigApi.delete(record.id!).then(() => {
              message.success('已删除')
              fetchList(page)
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
        <Title level={4} style={{ margin: 0 }}>数据源配置</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建数据源</Button>
      </div>

      <Table
        rowKey="id"
        dataSource={data}
        columns={columns}
        loading={loading}
        pagination={{ total, pageSize: 20, current: page, onChange: p => { setPage(p); fetchList(p) } }}
      />

      <Modal
        title={editing ? '编辑数据源' : '新建数据源'}
        open={visible}
        onOk={handleOk}
        onCancel={() => setVisible(false)}
        confirmLoading={saving}
        width={760}
        okText={editing ? '保存' : '创建'}
        cancelText="取消"
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="例：用户画像库" />
          </Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true }]}>
            <Select options={[{ value: 'JDBC', label: 'JDBC' }]} disabled />
          </Form.Item>
          <Form.Item name="url" label="JDBC URL" rules={[{ required: true, message: '请输入 JDBC URL' }]}>
            <Input placeholder="jdbc:mysql://host:3306/db" />
          </Form.Item>
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="driverClassName" label="Driver Class">
            <Input placeholder="com.mysql.cj.jdbc.Driver" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="enabled" label="启用状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
