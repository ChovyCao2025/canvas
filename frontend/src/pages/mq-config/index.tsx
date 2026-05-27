/**
 * 页面职责：MQ 触发器配置页，维护 topic、tag、消费组和消息参数 schema。
 *
 * 维护说明：这些定义会被 MQ 触发节点引用，参数 schema 影响配置面板的上下文字段。
 */
import { useEffect, useState } from 'react'
import { Button, Table, Tag, Space, Modal, Form, Input, Select, Switch, message, Typography, Popconfirm, Divider } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import http from '../../services/api'
import type { ApiParam } from '../api-config'
import { useSystemOptions } from '../../hooks/useSystemOptions'

/** 页面标题组件别名，统一主标题渲染。 */
const { Title } = Typography

/**
 * MQ 消息定义模型。
 */
interface MqDef {
  /** 消息定义 ID。 */
  id: number

  /** 消息名称。 */
  name: string

  /** 消息编码。 */
  messageCode: string

  /** 所属 Topic。 */
  topic: string

  /** 请求参数 schema（JSON 字符串）。 */
  requestSchema?: string

  /** 消息说明。 */
  description?: string

  /** 启用状态：1 启用，0 禁用。 */
  enabled: number
}

/**
 * MQ 参数 Schema 编辑器。
 *
 * 与 API 配置页复用同一种参数结构（ApiParam），
 * 最终写入 `requestSchema`（JSON 字符串）字段。
 */
interface ParamSchemaEditorProps {
  /** 当前参数定义数组。 */
  value?: ApiParam[]

  /** 参数变更回调。 */
  onChange?: (v: ApiParam[]) => void

  /** 参数类型选项。 */
  paramTypeOptions: { value: string; label: string }[]
}

/** 表单内联参数编辑器，负责增删改 requestSchema 的单行参数。 */
function ParamSchemaEditor({ value, onChange, paramTypeOptions }: ParamSchemaEditorProps) {
  const params: ApiParam[] = value ?? []
  /** 将参数数组回传给 Form.Item，保持受控字段同步。 */
  const set = (next: ApiParam[]) => onChange?.(next)
  /** 追加一个默认字符串参数。 */
  const add = () => set([...params, { name: '', displayName: '', type: 'STRING', required: false }])
  /** 删除指定下标的参数。 */
  const remove = (i: number) => set(params.filter((_, idx) => idx !== i))
  /** 更新指定参数的局部字段。 */
  const update = (i: number, patch: Partial<ApiParam>) =>
    set(params.map((p, idx) => idx === i ? { ...p, ...patch } : p))

  return (
    <div>
      {params.map((p, i) => (
        <Space key={i} style={{ display: 'flex', marginBottom: 6 }}>
          <Input size="small" style={{ width: 90 }} placeholder="参数名" value={p.name}
            onChange={e => update(i, { name: e.target.value })} />
          <Input size="small" style={{ width: 80 }} placeholder="显示名" value={p.displayName}
            onChange={e => update(i, { displayName: e.target.value })} />
          <Select
            size="small"
            style={{ width: 132 }}
            value={p.type}
            options={paramTypeOptions}
            onChange={type => update(i, { type })}
          />
          <Button size="small" type="text" danger icon={<DeleteOutlined />} onClick={() => remove(i)} />
        </Space>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add}>添加参数</Button>
    </div>
  )
}

/**
 * MQ 配置页。
 */
export default function MqConfigPage() {
  const [data, setData] = useState<MqDef[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [visible, setVisible] = useState(false)
  const [editing, setEditing] = useState<MqDef | null>(null)
  const [form] = Form.useForm()
  const [saving, setSaving] = useState(false)
  const { options: paramTypeOptions } = useSystemOptions('param_type')

  /** 分页加载 MQ 定义列表。 */
  const fetchList = async (p = page) => {
    setLoading(true)
    try {
      const res: any = await http.get('/canvas/mq-definitions', { params: { page: p, size: 20 } })
      setData(res.data.list); setTotal(res.data.total)
    } finally { setLoading(false) }
  }

  useEffect(() => { fetchList(1) }, [])

  /** 打开新建弹窗，并初始化启用状态和空参数 schema。 */
  const openCreate = () => {
    setEditing(null); form.resetFields()
    form.setFieldsValue({ enabled: true, requestSchema: [] }); setVisible(true)
  }
  /** 打开编辑弹窗，将后端 JSON schema 字符串转换为表单数组。 */
  const openEdit = (r: MqDef) => {
    // 字符串 schema -> 数组，供 Form 内部编辑器渲染
    setEditing(r)
    let schema: ApiParam[] = []
    try { schema = JSON.parse(r.requestSchema || '[]') } catch {}
    form.setFieldsValue({ ...r, enabled: r.enabled === 1, requestSchema: schema })
    setVisible(true)
  }
  /** 保存 MQ 定义；提交前把参数数组序列化为后端字段。 */
  const handleOk = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      const body = {
        ...values,
        enabled: values.enabled ? 1 : 0,
        requestSchema: JSON.stringify(values.requestSchema ?? []),
      }
      if (editing) { await http.put(`/canvas/mq-definitions/${editing.id}`, body); message.success('更新成功') }
      else { await http.post('/canvas/mq-definitions', body); message.success('创建成功') }
      setVisible(false); fetchList(editing ? page : 1)
    } finally { setSaving(false) }
  }

  const columns: ColumnsType<MqDef> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '名称', dataIndex: 'name' },
    { title: '消息编码', dataIndex: 'messageCode', ellipsis: true },
    { title: 'Topic', dataIndex: 'topic', ellipsis: true },
    {
      title: '参数', dataIndex: 'requestSchema', width: 60,
      render: (v: string) => { try { return JSON.parse(v || '[]').length } catch { return 0 } },
    },
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
            onConfirm={() => http.delete(`/canvas/mq-definitions/${r.id}`).then(() => { message.success('已删除'); fetchList(page) })}
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
        <Title level={4} style={{ margin: 0 }}>MQ 消息配置</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建消息</Button>
      </div>
      <Typography.Text type="secondary" style={{ fontSize: 13, display: 'block', marginBottom: 16 }}>
        在此配置好 MQ 消息类型后，可在画布中的「MQ 消息触发」节点和「发送 MQ」节点直接选用。
      </Typography.Text>
      <Table rowKey="id" dataSource={data} columns={columns} loading={loading}
        pagination={{ total, pageSize: 20, current: page, onChange: p => { setPage(p); fetchList(p) } }} />

      <Modal title={editing ? '编辑消息定义' : '新建消息定义'} open={visible} onOk={handleOk}
        onCancel={() => setVisible(false)} confirmLoading={saving} width={640}
        okText={editing ? '保存' : '创建'} cancelText="取消">
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input placeholder="如：用户行为通知" />
          </Form.Item>
          <Form.Item name="messageCode" label="消息编码" rules={[{ required: true }]}>
            <Input placeholder="如：user_behavior_notify" />
          </Form.Item>
          <Form.Item name="topic" label="MQ Topic" rules={[{ required: true }]}>
            <Input placeholder="如：canvas.user.behavior" />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
          <Divider style={{ margin: '8px 0 12px' }}>消息参数定义</Divider>
          <Form.Item name="requestSchema" style={{ marginBottom: 0 }}>
            <ParamSchemaEditor paramTypeOptions={paramTypeOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
