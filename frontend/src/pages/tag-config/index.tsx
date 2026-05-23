import { useEffect, useState } from 'react'
import { Button, Table, Tag, Space, Modal, Form, Input, Select, Switch, message, Typography, Popconfirm } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { tagDefinitionApi } from '../../services/api'
import { normalizeTagDefinitionPayload } from './tagConfigPayload'

const { Title } = Typography

interface TagDef {
  id: number
  name: string
  tagCode: string
  tagType: string
  description?: string
  enabled: number
  valueType?: string
  manualEnabled?: number
  defaultTtlDays?: number
  category?: string
  owner?: string
  writePolicy?: string
}

export default function TagConfigPage() {
  const [data,     setData]     = useState<TagDef[]>([])
  const [total,    setTotal]    = useState(0)
  const [loading,  setLoading]  = useState(false)
  const [page,     setPage]     = useState(1)
  const [visible,  setVisible]  = useState(false)
  const [editing,  setEditing]  = useState<TagDef | null>(null)
  const [form] = Form.useForm()
  const [saving, setSaving] = useState(false)

  const fetchList = async (p = page) => {
    setLoading(true)
    try {
      const res = await tagDefinitionApi.list({ page: p, size: 20 })
      setData(res.data.list)
      setTotal(res.data.total)
    } finally { setLoading(false) }
  }

  useEffect(() => { fetchList(1) }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ tagType: 'offline', enabled: true, valueType: 'STRING', manualEnabled: true, writePolicy: 'UPSERT' })
    setVisible(true)
  }

  const openEdit = (r: TagDef) => {
    setEditing(r)
    form.setFieldsValue({ ...r, enabled: r.enabled === 1, manualEnabled: r.manualEnabled !== 0 })
    setVisible(true)
  }

  const handleOk = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      const body = normalizeTagDefinitionPayload(values)
      if (editing) {
        await tagDefinitionApi.update(editing.id, body)
        message.success('更新成功')
      } else {
        await tagDefinitionApi.create(body)
        message.success('创建成功')
      }
      setVisible(false)
      fetchList(editing ? page : 1)
    } finally { setSaving(false) }
  }

  const columns: ColumnsType<TagDef> = [
    { title: 'ID',       dataIndex: 'id',       width: 60 },
    { title: '名称',     dataIndex: 'name' },
    { title: '标签编码', dataIndex: 'tagCode',  ellipsis: true },
    {
      title: '类型', dataIndex: 'tagType', width: 90,
      render: (v: string) => <Tag color={v === 'offline' ? 'blue' : 'green'}>{v === 'offline' ? '离线' : '实时'}</Tag>,
    },
    { title: '值类型', dataIndex: 'valueType', width: 90, render: v => v || 'STRING' },
    {
      title: '人工打标', dataIndex: 'manualEnabled', width: 90,
      render: v => <Tag color={v === 0 ? 'default' : 'green'}>{v === 0 ? '关闭' : '允许'}</Tag>,
    },
    { title: '说明', dataIndex: 'description', ellipsis: true },
    {
      title: '状态', dataIndex: 'enabled', width: 72,
      render: (v: number) => <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag>,
    },
    {
      title: '操作', width: 100,
      render: (_, r) => (
        <Space size={4}>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(r)} />
          <Popconfirm title="确认删除？" onConfirm={() => tagDefinitionApi.delete(r.id).then(() => { message.success('已删除'); fetchList(page) })}
            okText="删除" okType="danger" cancelText="取消">
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0 }}>标签配置</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建标签</Button>
      </div>
      <Table rowKey="id" dataSource={data} columns={columns} loading={loading}
        pagination={{ total, pageSize: 20, current: page, onChange: p => { setPage(p); fetchList(p) } }} />

      <Modal title={editing ? '编辑标签' : '新建标签'} open={visible} onOk={handleOk}
        onCancel={() => setVisible(false)} confirmLoading={saving} okText={editing ? '保存' : '创建'} cancelText="取消">
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="标签名称" rules={[{ required: true }]}>
            <Input placeholder="如：新客标签" />
          </Form.Item>
          <Form.Item name="tagCode" label="标签编码" rules={[{ required: true }]}>
            <Input placeholder="如：new_user" />
          </Form.Item>
          <Form.Item name="tagType" label="类型" rules={[{ required: true }]}>
            <Select options={[{ value: 'offline', label: '离线标签' }, { value: 'realtime', label: '实时标签' }]} />
          </Form.Item>
          <Form.Item name="valueType" label="标签值类型" rules={[{ required: true }]}>
            <Select options={[
              { value: 'STRING', label: '字符串' },
              { value: 'NUMBER', label: '数字' },
              { value: 'BOOLEAN', label: '布尔' },
              { value: 'JSON', label: 'JSON' },
            ]} />
          </Form.Item>
          <Form.Item name="manualEnabled" label="允许人工打标" valuePropName="checked">
            <Switch checkedChildren="允许" unCheckedChildren="关闭" />
          </Form.Item>
          <Form.Item name="defaultTtlDays" label="默认有效期（天）">
            <Input type="number" min={1} placeholder="留空表示长期有效" />
          </Form.Item>
          <Form.Item name="category" label="分类">
            <Input placeholder="如：生命周期" />
          </Form.Item>
          <Form.Item name="owner" label="负责人">
            <Input placeholder="如：growth" />
          </Form.Item>
          <Form.Item name="writePolicy" label="写入策略">
            <Select disabled options={[{ value: 'UPSERT', label: '覆盖当前值' }]} />
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
