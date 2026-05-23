import { useEffect, useState } from 'react'
import { Button, Form, Input, Modal, Popconfirm, Space, Switch, Table, Tag, Typography, message } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { audienceDataSourceApi, type AudienceDataSource } from '../../services/audienceDataSourceApi'

const { Title } = Typography

type AudienceDataSourceFormValues = Omit<AudienceDataSource, 'enabled'> & {
  enabled?: boolean
}

export default function AudienceDataSourcePage() {
  const [data, setData] = useState<AudienceDataSource[]>([])
  const [loading, setLoading] = useState(false)
  const [visible, setVisible] = useState(false)
  const [editing, setEditing] = useState<AudienceDataSource | null>(null)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm<AudienceDataSourceFormValues>()

  const fetchList = async () => {
    setLoading(true)
    try {
      const res = await audienceDataSourceApi.list()
      setData(res.data)
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
      enabled: true,
      driverClassName: 'com.mysql.cj.jdbc.Driver',
    })
    setVisible(true)
  }

  const openEdit = (record: AudienceDataSource) => {
    setEditing(record)
    form.resetFields()
    form.setFieldsValue({
      ...record,
      password: undefined,
      enabled: record.enabled === 1,
    })
    setVisible(true)
  }

  const handleOk = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      const body: AudienceDataSource = {
        name: values.name,
        description: values.description,
        url: values.url,
        username: values.username,
        password: values.password,
        driverClassName: values.driverClassName,
        enabled: values.enabled ? 1 : 0,
      }
      if (editing?.id != null) {
        await audienceDataSourceApi.update(editing.id, body)
        message.success('更新成功')
      } else {
        await audienceDataSourceApi.create(body)
        message.success('创建成功')
      }
      setVisible(false)
      fetchList()
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (record: AudienceDataSource) => {
    await audienceDataSourceApi.delete(record.id!)
    message.success('已删除')
    fetchList()
  }

  const columns: ColumnsType<AudienceDataSource> = [
    { title: '名称', dataIndex: 'name' },
    { title: 'JDBC URL', dataIndex: 'url', ellipsis: true },
    { title: '驱动', dataIndex: 'driverClassName', width: 220 },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 90,
      render: (value: number) => (
        <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '禁用'}</Tag>
      ),
    },
    { title: '引用数', dataIndex: 'referenceCount', width: 90 },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 180,
      render: (value?: string) => (value ? value.replace('T', ' ').slice(0, 19) : '-'),
    },
    {
      title: '操作',
      width: 100,
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm
            title={record.referenceCount ? '该数据源已被引用，无法删除' : '确认删除？'}
            onConfirm={() => handleDelete(record)}
            okText="删除"
            okType="danger"
            cancelText="取消"
            disabled={(record.referenceCount ?? 0) > 0}
          >
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              disabled={(record.referenceCount ?? 0) > 0}
            />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0 }}>人群数据源</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建数据源</Button>
      </div>

      <Table
        rowKey="id"
        dataSource={data}
        columns={columns}
        loading={loading}
        locale={{ emptyText: '暂无数据源' }}
      />

      <Modal
        title={editing ? '编辑数据源' : '新建数据源'}
        open={visible}
        onOk={handleOk}
        onCancel={() => setVisible(false)}
        confirmLoading={saving}
        okText={editing ? '保存' : '创建'}
        cancelText="取消"
        width={720}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入数据源名称' }]}>
            <Input placeholder="如：会员中心 MySQL" />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={2} placeholder="可选，说明用途或所属系统" />
          </Form.Item>
          <Form.Item name="url" label="JDBC URL" rules={[{ required: true, message: '请输入 JDBC URL' }]}>
            <Input placeholder="jdbc:mysql://host:3306/db" />
          </Form.Item>
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
            rules={editing ? [] : [{ required: true, message: '请输入密码' }]}
            extra={editing ? '留空则保持现有密码不变' : undefined}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item name="driverClassName" label="驱动类名">
            <Input placeholder="com.mysql.cj.jdbc.Driver" />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
