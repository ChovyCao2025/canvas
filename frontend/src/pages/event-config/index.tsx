import { useEffect, useState } from 'react'
import { Button, Table, Tag, Space, Modal, Form, Input, Select, Switch, message, Typography, Popconfirm, Divider, Alert } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, CodeOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import http from '../../services/api'
import type { ApiParam } from '../api-config'
import { useSystemOptions } from '../../hooks/useSystemOptions'

const { Title, Text } = Typography

/**
 * 事件定义模型。
 */
interface EventDef {
  /** 事件定义 ID。 */
  id: number

  /** 事件名称。 */
  name: string

  /** 事件编码（上报时使用）。 */
  eventCode: string

  /** 事件属性 schema（JSON 字符串）。 */
  attributes?: string

  /** 事件说明。 */
  description?: string

  /** 启用状态：1 启用，0 禁用。 */
  enabled: number
}

/**
 * 事件属性 Schema 编辑器。
 *
 * 用于定义上报事件可携带的 attributes 结构，
 * 后续在 EVENT_TRIGGER 节点里可作为上下文字段引用。
 */
interface AttrSchemaEditorProps {
  /** 当前属性定义数组。 */
  value?: ApiParam[]

  /** 属性定义变更回调。 */
  onChange?: (v: ApiParam[]) => void

  /** 属性类型选项。 */
  attrTypeOptions: { value: string; label: string }[]
}

function AttrSchemaEditor({ value, onChange, attrTypeOptions }: AttrSchemaEditorProps) {
  const attrs: ApiParam[] = value ?? []
  const set = (next: ApiParam[]) => onChange?.(next)
  const add = () => set([...attrs, { name: '', displayName: '', type: 'STRING', required: false }])
  const remove = (i: number) => set(attrs.filter((_, idx) => idx !== i))
  const update = (i: number, patch: Partial<ApiParam>) =>
    set(attrs.map((p, idx) => idx === i ? { ...p, ...patch } : p))

  return (
    <div>
      {attrs.map((p, i) => (
        <Space key={i} style={{ display: 'flex', marginBottom: 6 }}>
          <Input size="small" style={{ width: 90 }} placeholder="属性名" value={p.name}
            onChange={e => update(i, { name: e.target.value })} />
          <Input size="small" style={{ width: 80 }} placeholder="显示名" value={p.displayName}
            onChange={e => update(i, { displayName: e.target.value })} />
          <Select
            size="small"
            style={{ width: 108 }}
            value={p.type}
            options={attrTypeOptions}
            onChange={type => update(i, { type })}
          />
          <label style={{ fontSize: 12, display: 'flex', alignItems: 'center', gap: 4 }}>
            <input type="checkbox" checked={p.required} onChange={e => update(i, { required: e.target.checked })} />
            必填
          </label>
          <Button size="small" type="text" danger icon={<DeleteOutlined />} onClick={() => remove(i)} />
        </Space>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add}>添加属性</Button>
    </div>
  )
}

/**
 * 事件配置页。
 */
export default function EventConfigPage() {
  const [data,    setData]    = useState<EventDef[]>([])
  const [total,   setTotal]   = useState(0)
  const [loading, setLoading] = useState(false)
  const [page,    setPage]    = useState(1)
  const [visible, setVisible] = useState(false)
  const [editing, setEditing] = useState<EventDef | null>(null)
  const [form] = Form.useForm()
  const [saving, setSaving] = useState(false)
  const { options: attrTypeOptions } = useSystemOptions('event_attr_type')

  const fetchList = async (p = page) => {
    setLoading(true)
    try {
      const res: any = await http.get('/canvas/event-definitions', { params: { page: p, size: 20 } })
      setData(res.data.list); setTotal(res.data.total)
    } finally { setLoading(false) }
  }

  useEffect(() => { fetchList(1) }, [])

  const openCreate = () => {
    setEditing(null); form.resetFields()
    form.setFieldsValue({ enabled: true, attributes: [] }); setVisible(true)
  }
  const openEdit = (r: EventDef) => {
    // 后端存储为 JSON 字符串，编辑时反序列化为数组
    setEditing(r)
    let attrs: ApiParam[] = []
    try { attrs = JSON.parse(r.attributes || '[]') } catch {}
    form.setFieldsValue({ ...r, enabled: r.enabled === 1, attributes: attrs })
    setVisible(true)
  }
  const handleOk = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      const body = {
        ...values,
        enabled: values.enabled ? 1 : 0,
        attributes: JSON.stringify(values.attributes ?? []),
      }
      if (editing) { await http.put(`/canvas/event-definitions/${editing.id}`, body); message.success('更新成功') }
      else { await http.post('/canvas/event-definitions', body); message.success('创建成功') }
      setVisible(false); fetchList(editing ? page : 1)
    } finally { setSaving(false) }
  }

  const columns: ColumnsType<EventDef> = [
    { title: 'ID',     dataIndex: 'id',        width: 60 },
    { title: '事件名称', dataIndex: 'name' },
    {
      title: '事件编码', dataIndex: 'eventCode',
      render: (v: string) => <Text code>{v}</Text>,
    },
    {
      title: '属性数', dataIndex: 'attributes', width: 70,
      render: (v: string) => { try { return JSON.parse(v || '[]').length } catch { return 0 } },
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
          <Popconfirm title="确认删除？"
            onConfirm={() => http.delete(`/canvas/event-definitions/${r.id}`).then(() => { message.success('已删除'); fetchList(page) })}
            okText="删除" okType="danger" cancelText="取消">
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>事件配置</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建事件</Button>
      </div>

      <Alert
        type="info"
        showIcon
        icon={<CodeOutlined />}
        style={{ marginBottom: 16 }}
        message="事件上报接口"
        description={
          <div style={{ fontSize: 12, fontFamily: 'monospace' }}>
            POST /canvas/events/report<br />
            {'{'} "eventCode": "ORDER_COMPLETE", "userId": "u001", "attributes": {'{'}"orderId":"12345"{'}'} {'}'}
          </div>
        }
      />

      <Table rowKey="id" dataSource={data} columns={columns} loading={loading}
        pagination={{ total, pageSize: 20, current: page, onChange: p => { setPage(p); fetchList(p) } }} />

      <Modal title={editing ? '编辑事件' : '新建事件'} open={visible} onOk={handleOk}
        onCancel={() => setVisible(false)} confirmLoading={saving} width={620}
        okText={editing ? '保存' : '创建'} cancelText="取消">
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="事件名称" rules={[{ required: true }]}>
            <Input placeholder="如：订单完成" />
          </Form.Item>
          <Form.Item name="eventCode" label="事件编码" rules={[{ required: true }]}
            extra="业务方调用上报接口时使用此编码，画布触发条件匹配此编码">
            <Input placeholder="如：ORDER_COMPLETE" style={{ fontFamily: 'monospace' }} />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
          <Divider style={{ margin: '8px 0 12px' }}>事件属性定义</Divider>
          <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 8 }}>
            属性类型：文本（STRING）/ 数值（NUMBER）/ 日期（DATE），上报时作为上下文变量传入画布
          </div>
          <Form.Item name="attributes" style={{ marginBottom: 0 }}>
            <AttrSchemaEditor attrTypeOptions={attrTypeOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
