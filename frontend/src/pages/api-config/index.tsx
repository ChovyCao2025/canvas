import { useEffect, useState } from 'react'
import {
  Button, Table, Tag, Space, Modal, Form, Input,
  Select, Switch, message, Typography, Popconfirm,
  Divider,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { apiDefinitionApi } from '../../services/api'

const { Title } = Typography

/**
 * 参数类型下拉选项：
 * 与后端元数据约定保持一致，最终落库到 requestSchema 字段中。
 */
export const PARAM_TYPES = [
  { value: 'STRING',       label: '字符型' },
  { value: 'NUMBER',       label: '数值型' },
  { value: 'TEXT',         label: '文本型' },
  { value: 'DATE',         label: '日期型' },
  { value: 'STRING_PARAM', label: '字符型（参数调用）' },
]

/**
 * API 参数 schema 的单项结构。
 */
export interface ApiParam {
  /** 参数名（调用方传参 key）。 */
  name: string

  /** 参数展示名（给运营同学看的文案）。 */
  displayName: string

  /** 参数类型（受 PARAM_TYPES 约束）。 */
  type: string

  /** 是否必填。 */
  required: boolean
}

/**
 * API 定义列表项模型。
 */
interface ApiDefinition {
  /** 主键 ID。 */
  id: number

  /** 接口名称。 */
  name: string

  /** 接口唯一业务编码。 */
  apiKey: string

  /** 请求地址。 */
  url: string

  /** 请求方法。 */
  method: string

  /** 业务线。 */
  bizLine?: string

  /** 描述。 */
  description?: string

  /** 请求参数 schema（JSON 字符串）。 */
  requestSchema?: string

  /** 启用状态：1 启用，0 禁用。 */
  enabled: number
}

// ── 参数定义子表格 ───────────────────────────────────────────────
/**
 * 参数 schema 编辑器。
 *
 * 这是一个“受控组件”：
 * - value 来自 Form.Item
 * - onChange 把更新后的数组回传给 Form
 */
interface ParamSchemaEditorProps {
  /** 当前参数数组。 */
  value?: ApiParam[]

  /** 参数变更回调。 */
  onChange?: (v: ApiParam[]) => void
}

function ParamSchemaEditor({ value, onChange }: ParamSchemaEditorProps) {
  const params: ApiParam[] = value ?? []

  // 小工具函数：所有改动都走 set(next) 回传，保持单向数据流
  const set = (next: ApiParam[]) => onChange?.(next)
  const add = () => set([...params, { name: '', displayName: '', type: 'STRING', required: false }])
  const remove = (i: number) => set(params.filter((_, idx) => idx !== i))
  const update = (i: number, patch: Partial<ApiParam>) =>
    set(params.map((p, idx) => idx === i ? { ...p, ...patch } : p))

  const cols: ColumnsType<ApiParam> = [
    {
      title: '参数名称', dataIndex: 'name', width: 130,
      render: (v, _, i) => (
        <Input size="small" value={v} placeholder="paramName"
          onChange={e => update(i, { name: e.target.value })} />
      ),
    },
    {
      title: '显示名称', dataIndex: 'displayName', width: 120,
      render: (v, _, i) => (
        <Input size="small" value={v} placeholder="展示给配置者"
          onChange={e => update(i, { displayName: e.target.value })} />
      ),
    },
    {
      title: '类型', dataIndex: 'type', width: 160,
      render: (v, _, i) => (
        <Select size="small" style={{ width: '100%' }} value={v}
          options={PARAM_TYPES}
          onChange={t => update(i, { type: t })} />
      ),
    },
    {
      title: '必填', dataIndex: 'required', width: 60,
      render: (v, _, i) => (
        <Switch size="small" checked={v} onChange={c => update(i, { required: c })} />
      ),
    },
    {
      title: '', width: 40,
      render: (_, __, i) => (
        <Button size="small" type="text" danger icon={<DeleteOutlined />} onClick={() => remove(i)} />
      ),
    },
  ]

  return (
    <div>
      <Table
        size="small"
        rowKey={(_, i) => String(i)}
        dataSource={params}
        columns={cols}
        pagination={false}
        style={{ marginBottom: 8 }}
        locale={{ emptyText: '暂无参数' }}
      />
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add}>
        添加参数
      </Button>
    </div>
  )
}

// ── 主页面 ────────────────────────────────────────────────────────
/**
 * API 配置页。
 */
export default function ApiConfigPage() {
  const [data,          setData]          = useState<ApiDefinition[]>([])
  const [total,         setTotal]         = useState(0)
  const [loading,       setLoading]       = useState(false)
  const [page,          setPage]          = useState(1)
  const [modalVisible,  setModalVisible]  = useState(false)
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
    form.setFieldsValue({ method: 'POST', enabled: true, requestSchema: [] })
    setModalVisible(true)
  }

  const openEdit = (record: ApiDefinition) => {
    // requestSchema 存储为 JSON 字符串，编辑时需要反序列化回数组
    setEditingRecord(record)
    let schema: ApiParam[] = []
    try { schema = JSON.parse(record.requestSchema || '[]') } catch {}
    form.setFieldsValue({ ...record, enabled: record.enabled === 1, requestSchema: schema })
    setModalVisible(true)
  }

  const handleOk = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      const body = {
        ...values,
        enabled: values.enabled ? 1 : 0,
        // 后端字段是字符串，因此这里序列化数组
        requestSchema: JSON.stringify(values.requestSchema ?? []),
      }
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
    { title: 'ID',   dataIndex: 'id',     width: 60 },
    { title: '名称', dataIndex: 'name' },
    { title: 'apiKey', dataIndex: 'apiKey', ellipsis: true },
    { title: 'URL',  dataIndex: 'url',    ellipsis: true },
    {
      title: '方法', dataIndex: 'method', width: 72,
      render: (m: string) => <Tag color={m === 'GET' ? 'blue' : 'green'}>{m}</Tag>,
    },
    {
      title: '参数', dataIndex: 'requestSchema', width: 60,
      render: (v: string) => {
        try { return JSON.parse(v || '[]').length } catch { return 0 }
      },
    },
    {
      title: '状态', dataIndex: 'enabled', width: 72,
      render: (v: number) => <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag>,
    },
    {
      title: '操作', width: 100,
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm title="确认删除？" onConfirm={() => handleDelete(record.id)}
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
        <Title level={4} style={{ margin: 0 }}>API 接口配置</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建 API</Button>
      </div>

      <Table rowKey="id" dataSource={data} columns={columns} loading={loading}
        pagination={{ total, pageSize: 20, current: page, onChange: (p) => { setPage(p); fetchList(p) } }} />

      <Modal title={editingRecord ? '编辑 API' : '新建 API'}
        open={modalVisible} onOk={handleOk} onCancel={() => setModalVisible(false)}
        confirmLoading={submitting} okText={editingRecord ? '保存' : '创建'}
        cancelText="取消" width={700}>
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Space style={{ width: '100%' }} direction="vertical" size={0}>
            <Form.Item name="name" label="名称" rules={[{ required: true }]}>
              <Input placeholder="如：查询用户信息" />
            </Form.Item>
            <Form.Item name="apiKey" label="apiKey（唯一标识）" rules={[{ required: true }]}>
              <Input placeholder="如：query_user_info" />
            </Form.Item>
            <Space style={{ width: '100%' }}>
              <Form.Item name="url" label="URL" style={{ flex: 1 }} rules={[{ required: true }]}>
                <Input placeholder="https://api.example.com/v1/user" />
              </Form.Item>
              <Form.Item name="method" label="方法" style={{ width: 100 }} rules={[{ required: true }]}>
                <Select options={[{ value: 'GET', label: 'GET' }, { value: 'POST', label: 'POST' }]} />
              </Form.Item>
            </Space>
            <Form.Item name="description" label="说明">
              <Input.TextArea rows={2} />
            </Form.Item>
            <Form.Item name="enabled" label="状态" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="禁用" />
            </Form.Item>

            <Divider style={{ margin: '8px 0 12px' }}>请求参数定义</Divider>
            <Form.Item name="requestSchema" style={{ marginBottom: 0 }}>
              <ParamSchemaEditor />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  )
}
