import { useEffect, useState } from 'react'
import {
  Button, Table, Tag, Space, Modal, Form, Input, InputNumber,
  Select, Switch, message, Typography, Popconfirm,
  Divider, Tabs,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { apiDefinitionApi } from '../../services/api'
import {
  buildApiReceiptPreview,
  buildApiRequestPreview,
  formatApiRequestPreview,
  normalizeApiDefinitionPayload,
} from './requestPreview'
import type { ApiReceiptStatus } from './requestPreview'

const { Title } = Typography
const DEFAULT_RECEIPT_EXPIRE_MINUTES = 1440
const DEFAULT_RECEIPT_STATUSES: ApiReceiptStatus[] = [{ code: '200', label: '成功' }]

export const PARAM_TYPES = [
  { value: 'STRING',       label: '字符型' },
  { value: 'NUMBER',       label: '数值型' },
  { value: 'TEXT',         label: '文本型' },
  { value: 'DATE',         label: '日期型' },
  { value: 'STRING_PARAM', label: '字符型（参数调用）' },
]

export interface ApiParam {
  name:        string
  displayName: string
  type:        string
  required:    boolean
}

interface ApiDefinition {
  id: number
  name: string
  apiKey: string
  url: string
  method: string
  bizLine?: string
  description?: string
  requestSchema?: string
  includeContextPayload?: number
  receiptEnabled?: number
  receiptExpireMinutes?: number
  receiptStatuses?: string
  enabled: number
}

function parseArrayField<T>(raw: string | undefined, fallback: T[]): T[] {
  try {
    const parsed = JSON.parse(raw || '[]')
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : fallback
  } catch {
    return fallback
  }
}

// ── 参数定义子表格 ───────────────────────────────────────────────
function ParamSchemaEditor({ value, onChange }: {
  value?: ApiParam[]; onChange?: (v: ApiParam[]) => void
}) {
  const params: ApiParam[] = value ?? []

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

function ReceiptStatusEditor({ value, onChange }: {
  value?: ApiReceiptStatus[]; onChange?: (v: ApiReceiptStatus[]) => void
}) {
  const statuses: ApiReceiptStatus[] = value ?? []

  const set = (next: ApiReceiptStatus[]) => onChange?.(next)
  const add = () => set([...statuses, { code: '', label: '' }])
  const remove = (i: number) => set(statuses.filter((_, idx) => idx !== i))
  const update = (i: number, patch: Partial<ApiReceiptStatus>) =>
    set(statuses.map((item, idx) => idx === i ? { ...item, ...patch } : item))

  const cols: ColumnsType<ApiReceiptStatus> = [
    {
      title: '状态 code', dataIndex: 'code', width: 140,
      render: (v, _, i) => (
        <Input size="small" value={v} placeholder="200"
          onChange={e => update(i, { code: e.target.value })} />
      ),
    },
    {
      title: '状态值', dataIndex: 'label',
      render: (v, _, i) => (
        <Input size="small" value={v} placeholder="成功"
          onChange={e => update(i, { label: e.target.value })} />
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
        dataSource={statuses}
        columns={cols}
        pagination={false}
        style={{ marginBottom: 8 }}
        locale={{ emptyText: '暂无状态' }}
      />
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add}>
        添加状态
      </Button>
    </div>
  )
}

// ── 主页面 ────────────────────────────────────────────────────────
export default function ApiConfigPage() {
  const [data,          setData]          = useState<ApiDefinition[]>([])
  const [total,         setTotal]         = useState(0)
  const [loading,       setLoading]       = useState(false)
  const [page,          setPage]          = useState(1)
  const [modalVisible,  setModalVisible]  = useState(false)
  const [editingRecord, setEditingRecord] = useState<ApiDefinition | null>(null)
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const requestSchemaPreview = Form.useWatch('requestSchema', form) as ApiParam[] | undefined
  const includeContextPayloadPreview = Form.useWatch('includeContextPayload', form) as boolean | undefined
  const receiptEnabledPreview = Form.useWatch('receiptEnabled', form) as boolean | undefined
  const receiptStatusesPreview = Form.useWatch('receiptStatuses', form) as ApiReceiptStatus[] | undefined
  const requestPreviewJson = formatApiRequestPreview(buildApiRequestPreview({
    requestSchema: requestSchemaPreview,
    includeContextPayload: !!includeContextPayloadPreview,
  }))
  const receiptPreviewJson = formatApiRequestPreview(buildApiReceiptPreview({
    receiptEnabled: !!receiptEnabledPreview,
    receiptStatuses: receiptStatusesPreview,
  }))

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
    form.setFieldsValue({
      method: 'POST',
      enabled: true,
      includeContextPayload: false,
      receiptEnabled: false,
      receiptExpireMinutes: DEFAULT_RECEIPT_EXPIRE_MINUTES,
      receiptStatuses: DEFAULT_RECEIPT_STATUSES,
      requestSchema: [],
    })
    setModalVisible(true)
  }

  const openEdit = (record: ApiDefinition) => {
    setEditingRecord(record)
    const schema = parseArrayField<ApiParam>(record.requestSchema, [])
    const receiptStatuses = parseArrayField<ApiReceiptStatus>(
      record.receiptStatuses,
      DEFAULT_RECEIPT_STATUSES,
    )
    form.setFieldsValue({
      ...record,
      enabled: record.enabled === 1,
      includeContextPayload: record.includeContextPayload === 1,
      receiptEnabled: record.receiptEnabled === 1,
      receiptExpireMinutes: record.receiptExpireMinutes ?? DEFAULT_RECEIPT_EXPIRE_MINUTES,
      receiptStatuses,
      requestSchema: schema,
    })
    setModalVisible(true)
  }

  const handleOk = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      const body = normalizeApiDefinitionPayload(values)
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
      title: '环境信息', dataIndex: 'includeContextPayload', width: 92,
      render: (v: number) => <Tag color={v === 1 ? 'blue' : 'default'}>{v === 1 ? '携带' : '不携带'}</Tag>,
    },
    {
      title: '回执', dataIndex: 'receiptEnabled', width: 72,
      render: (v: number) => <Tag color={v === 1 ? 'purple' : 'default'}>{v === 1 ? '开启' : '关闭'}</Tag>,
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
        cancelText="取消" width={1080}>
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 380px', gap: 20 }}>
            <div>
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
              <Space size={24} align="start">
                <Form.Item name="enabled" label="状态" valuePropName="checked">
                  <Switch checkedChildren="启用" unCheckedChildren="禁用" />
                </Form.Item>
                <Form.Item name="includeContextPayload" label="环境信息" valuePropName="checked">
                  <Switch checkedChildren="携带" unCheckedChildren="不携带" />
                </Form.Item>
                <Form.Item name="receiptEnabled" label="回执设置" valuePropName="checked">
                  <Switch checkedChildren="开启" unCheckedChildren="关闭" />
                </Form.Item>
              </Space>

              {receiptEnabledPreview && (
                <>
                  <Space style={{ width: '100%' }} align="start">
                    <Form.Item
                      name="receiptExpireMinutes"
                      label="数据回收周期（分钟）"
                      style={{ width: 180 }}
                      rules={[{ required: true, message: '请填写数据回收周期' }]}
                    >
                      <InputNumber min={1} max={43200} style={{ width: '100%' }} />
                    </Form.Item>
                  </Space>
                  <Form.Item name="receiptStatuses" label="回执状态映射" style={{ marginBottom: 0 }}>
                    <ReceiptStatusEditor />
                  </Form.Item>
                </>
              )}

              <Divider style={{ margin: '16px 0 12px' }}>请求参数定义</Divider>
              <Form.Item name="requestSchema" style={{ marginBottom: 0 }}>
                <ParamSchemaEditor />
              </Form.Item>
            </div>
            <div style={{ position: 'sticky', top: 0, alignSelf: 'start', border: '1px solid #e5e7eb', borderRadius: 8, background: '#fafafa', padding: 12 }}>
              <Tabs
                size="small"
                items={[
                  {
                    key: 'request',
                    label: '请求 Body',
                    children: (
                      <Input.TextArea
                        value={requestPreviewJson}
                        readOnly
                        autoSize={{ minRows: 18, maxRows: 24 }}
                        style={{ fontFamily: 'SFMono-Regular, Consolas, monospace', fontSize: 12 }}
                      />
                    ),
                  },
                  {
                    key: 'receipt',
                    label: '回执上报',
                    children: (
                      <Input.TextArea
                        value={receiptPreviewJson}
                        readOnly
                        autoSize={{ minRows: 18, maxRows: 24 }}
                        style={{ fontFamily: 'SFMono-Regular, Consolas, monospace', fontSize: 12 }}
                      />
                    ),
                  },
                ]}
              />
            </div>
          </div>
        </Form>
      </Modal>
    </div>
  )
}
