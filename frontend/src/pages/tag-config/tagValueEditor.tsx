import { useEffect, useState } from 'react'
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Space, Switch, Table, Tag, message } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { tagValueApi } from '../../services/api'
import type { TagValueDefinition } from '../../types'
import type { TagValueFormValues, TagValueRecord } from './tagTypes'

export function toTagValueBody(values: TagValueFormValues): Partial<TagValueDefinition> {
  return {
    value: values.value,
    label: values.label,
    enabled: values.enabled ? 1 : 0,
    sortOrder: values.sortOrder ?? 0,
    source: values.source?.trim() || 'MANUAL',
  }
}

interface TagValueEditorProps {
  tagCode: string | null
}

export function TagValueEditor({ tagCode }: TagValueEditorProps) {
  const [data, setData] = useState<TagValueRecord[]>([])
  const [loading, setLoading] = useState(false)
  const [visible, setVisible] = useState(false)
  const [saving, setSaving] = useState(false)
  const [editing, setEditing] = useState<TagValueRecord | null>(null)
  const [form] = Form.useForm<TagValueFormValues>()

  const fetchList = async () => {
    if (!tagCode) {
      setData([])
      return
    }

    setLoading(true)
    try {
      const res = await tagValueApi.list(tagCode)
      setData(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void fetchList()
  }, [tagCode])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ enabled: true, sortOrder: 0, source: 'MANUAL' })
    setVisible(true)
  }

  const openEdit = (record: TagValueRecord) => {
    setEditing(record)
    form.setFieldsValue({
      value: record.value,
      label: record.label,
      sortOrder: record.sortOrder,
      source: record.source,
      enabled: record.enabled === 1,
    })
    setVisible(true)
  }

  const handleOk = async () => {
    if (!tagCode) return

    const values = await form.validateFields()
    setSaving(true)
    try {
      const body = toTagValueBody(values)
      if (editing) {
        await tagValueApi.update(editing.id, body)
        message.success('标签值更新成功')
      } else {
        await tagValueApi.create(tagCode, body)
        message.success('标签值创建成功')
      }
      setVisible(false)
      await fetchList()
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    await tagValueApi.delete(id)
    message.success('标签值已删除')
    await fetchList()
  }

  const columns: ColumnsType<TagValueRecord> = [
    { title: '值', dataIndex: 'value' },
    { title: '显示名', dataIndex: 'label' },
    { title: '排序', dataIndex: 'sortOrder', width: 90 },
    {
      title: '来源',
      dataIndex: 'source',
      width: 110,
      render: (value: string) => <Tag>{value}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 90,
      render: (value: number) => <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '禁用'}</Tag>,
    },
    {
      title: '操作',
      width: 110,
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm
            title="确认删除标签值？"
            okText="删除"
            okType="danger"
            cancelText="取消"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <span style={{ fontSize: 14, fontWeight: 500 }}>标签值列表</span>
        <Button type="dashed" icon={<PlusOutlined />} onClick={openCreate} disabled={!tagCode}>
          新建标签值
        </Button>
      </div>

      <Table
        rowKey="id"
        size="small"
        loading={loading}
        dataSource={data}
        columns={columns}
        pagination={false}
        locale={{ emptyText: tagCode ? '暂无标签值' : '保存标签后可维护标签值' }}
      />

      <Modal
        title={editing ? '编辑标签值' : '新建标签值'}
        open={visible}
        onOk={handleOk}
        onCancel={() => setVisible(false)}
        confirmLoading={saving}
        okText={editing ? '保存' : '创建'}
        cancelText="取消"
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="value" label="标签值" rules={[{ required: true, message: '请输入标签值' }]}>
            <Input placeholder="如：VIP" />
          </Form.Item>
          <Form.Item name="label" label="显示名" rules={[{ required: true, message: '请输入显示名' }]}>
            <Input placeholder="如：高价值用户" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} placeholder="默认 0" />
          </Form.Item>
          <Form.Item name="source" label="来源">
            <Input placeholder="默认 MANUAL" />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default TagValueEditor
