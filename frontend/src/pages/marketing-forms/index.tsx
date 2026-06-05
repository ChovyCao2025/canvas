import { useEffect, useMemo, useState } from 'react'
import {
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  Row,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  CopyOutlined,
  EyeOutlined,
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  StopOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  marketingFormsApi,
  type MarketingFormDefinition,
  type MarketingFormPayload,
  type MarketingFormSubmission,
} from '../../services/marketingFormsApi'
import {
  DEFAULT_FORM_SCHEMA,
  formStatusView,
  formatFormDateTime,
  publicFormPath,
  responsePreview,
  summarizeFormSchema,
} from './marketingFormsPresentation'

const { Title, Text } = Typography

interface FormEditorValues {
  publicKey?: string
  name: string
  description?: string
  fieldSchemaJson?: string
  submitActionJson?: string
  successMessage?: string
  active?: boolean
}

function trimText(value?: string) {
  const text = value?.trim()
  return text || undefined
}

function toPayload(values: FormEditorValues): MarketingFormPayload {
  return {
    publicKey: trimText(values.publicKey),
    name: values.name.trim(),
    description: trimText(values.description),
    fieldSchemaJson: trimText(values.fieldSchemaJson),
    submitActionJson: trimText(values.submitActionJson),
    successMessage: trimText(values.successMessage),
    active: values.active,
    createdBy: 'operator',
  }
}

export default function MarketingFormsPage() {
  const [form] = Form.useForm<FormEditorValues>()
  const [forms, setForms] = useState<MarketingFormDefinition[]>([])
  const [submissions, setSubmissions] = useState<MarketingFormSubmission[]>([])
  const [selected, setSelected] = useState<MarketingFormDefinition | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [loadingSubmissions, setLoadingSubmissions] = useState(false)

  const selectedPublicPath = useMemo(() => publicFormPath(selected?.publicKey), [selected?.publicKey])

  const loadForms = async () => {
    setLoading(true)
    try {
      const response = await marketingFormsApi.list()
      setForms(response.data)
      if (!selected && response.data.length > 0) {
        selectForm(response.data[0])
      }
    } finally {
      setLoading(false)
    }
  }

  const loadSubmissions = async (formId = selected?.id) => {
    if (!formId) return
    setLoadingSubmissions(true)
    try {
      const response = await marketingFormsApi.submissions(formId, 50)
      setSubmissions(response.data)
    } finally {
      setLoadingSubmissions(false)
    }
  }

  useEffect(() => {
    loadForms()
  }, [])

  const selectForm = (row: MarketingFormDefinition) => {
    setSelected(row)
    form.setFieldsValue({
      publicKey: row.publicKey,
      name: row.name,
      description: row.description ?? undefined,
      fieldSchemaJson: row.fieldSchemaJson,
      submitActionJson: row.submitActionJson ?? '{}',
      successMessage: row.successMessage ?? '提交成功',
      active: row.status === 'ACTIVE',
    })
    loadSubmissions(row.id)
  }

  const handleNew = () => {
    setSelected(null)
    setSubmissions([])
    form.setFieldsValue({
      publicKey: undefined,
      name: '',
      description: '',
      fieldSchemaJson: DEFAULT_FORM_SCHEMA,
      submitActionJson: '{}',
      successMessage: '提交成功',
      active: true,
    })
  }

  const handleSave = async (values: FormEditorValues) => {
    setSaving(true)
    try {
      const payload = toPayload(values)
      const response = selected
        ? await marketingFormsApi.update(selected.id, payload)
        : await marketingFormsApi.create(payload)
      message.success(selected ? '表单已更新' : '表单已创建')
      setSelected(response.data)
      await loadForms()
      await loadSubmissions(response.data.id)
    } finally {
      setSaving(false)
    }
  }

  const handleSetStatus = async (row: MarketingFormDefinition, active: boolean) => {
    setSaving(true)
    try {
      const response = await marketingFormsApi.setStatus(row.id, active)
      message.success(active ? '表单已启用' : '表单已停用')
      setSelected(response.data)
      await loadForms()
    } finally {
      setSaving(false)
    }
  }

  const handleCopy = async (row: MarketingFormDefinition) => {
    const path = publicFormPath(row.publicKey)
    const url = `${window.location.origin}${path}`
    await navigator.clipboard?.writeText(url)
    message.success('公开地址已复制')
  }

  const formColumns: ColumnsType<MarketingFormDefinition> = [
    {
      title: '表单',
      render: (_, row) => (
        <Space direction="vertical" size={0}>
          <Button type="link" style={{ padding: 0 }} onClick={() => selectForm(row)}>
            {row.name}
          </Button>
          <Text type="secondary">{row.publicKey}</Text>
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: value => {
        const view = formStatusView(value)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    { title: '字段', width: 150, render: (_, row) => summarizeFormSchema(row.fieldSchemaJson) },
    { title: '更新时间', dataIndex: 'updatedAt', width: 180, render: formatFormDateTime },
    {
      title: '操作',
      width: 190,
      render: (_, row) => (
        <Space>
          <Button size="small" icon={<CopyOutlined />} onClick={() => handleCopy(row)} />
          <Button
            size="small"
            icon={row.status === 'ACTIVE' ? <StopOutlined /> : <EyeOutlined />}
            loading={saving && selected?.id === row.id}
            onClick={() => handleSetStatus(row, row.status !== 'ACTIVE')}
          >
            {row.status === 'ACTIVE' ? '停用' : '启用'}
          </Button>
        </Space>
      ),
    },
  ]

  const submissionColumns: ColumnsType<MarketingFormSubmission> = [
    { title: '提交 ID', dataIndex: 'id', width: 90 },
    { title: '用户', dataIndex: 'userId', width: 180, ellipsis: true, render: value => value || '-' },
    { title: '响应摘要', dataIndex: 'responseJson', ellipsis: true, render: responsePreview },
    { title: '授权', width: 140, render: (_, row) => row.consentStatus ? `${row.consentChannel}: ${row.consentStatus}` : '-' },
    { title: '触发事件', dataIndex: 'triggerEventCode', width: 130, render: value => value || '-' },
    { title: '时间', dataIndex: 'createdAt', width: 180, render: formatFormDateTime },
  ]

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>表单中心</Title>
          <Text type="secondary">
            {forms.length > 0 ? `共 ${forms.length} 个表单` : '暂无表单'}
            {selected ? `，当前 ${summarizeFormSchema(selected.fieldSchemaJson)}` : ''}
          </Text>
        </div>
        <Space wrap>
          {selectedPublicPath && (
            <Button icon={<EyeOutlined />} href={selectedPublicPath} target="_blank" rel="noreferrer">
              预览
            </Button>
          )}
          <Button icon={<PlusOutlined />} onClick={handleNew}>新建</Button>
          <Button icon={<ReloadOutlined />} onClick={loadForms} loading={loading}>刷新</Button>
        </Space>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={14}>
          <Table
            rowKey="id"
            dataSource={forms}
            columns={formColumns}
            loading={loading}
            pagination={{ pageSize: 8 }}
            size="small"
          />
        </Col>
        <Col xs={24} xl={10}>
          <Card title={selected ? '编辑表单' : '新建表单'}>
            <Form
              form={form}
              layout="vertical"
              initialValues={{
                fieldSchemaJson: DEFAULT_FORM_SCHEMA,
                submitActionJson: '{}',
                successMessage: '提交成功',
                active: true,
              }}
              onFinish={handleSave}
            >
              <Row gutter={12}>
                <Col xs={24} sm={12}>
                  <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
                    <Input allowClear />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item name="publicKey" label="公开 Key">
                    <Input allowClear placeholder="自动生成" />
                  </Form.Item>
                </Col>
              </Row>
              <Form.Item name="description" label="描述">
                <Input.TextArea autoSize={{ minRows: 2, maxRows: 3 }} />
              </Form.Item>
              <Form.Item name="fieldSchemaJson" label="字段 JSON" rules={[{ required: true, message: '请输入字段 JSON' }]}>
                <Input.TextArea autoSize={{ minRows: 6, maxRows: 10 }} spellCheck={false} />
              </Form.Item>
              <Form.Item name="submitActionJson" label="提交动作 JSON">
                <Input.TextArea autoSize={{ minRows: 3, maxRows: 6 }} spellCheck={false} placeholder='{"canvasId":1,"triggerEventCode":"form_signup"}' />
              </Form.Item>
              <Row gutter={12} align="middle">
                <Col xs={24} sm={16}>
                  <Form.Item name="successMessage" label="成功文案">
                    <Input allowClear />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item name="active" label="启用" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                </Col>
              </Row>
              <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>
                {selected ? '保存' : '创建'}
              </Button>
            </Form>
          </Card>
        </Col>
      </Row>

      {selected ? (
        <Table
          rowKey="id"
          title={() => (
            <Space>
              <span>最近提交</span>
              <Button size="small" icon={<ReloadOutlined />} onClick={() => loadSubmissions()} loading={loadingSubmissions} />
            </Space>
          )}
          dataSource={submissions}
          columns={submissionColumns}
          loading={loadingSubmissions}
          pagination={{ pageSize: 8 }}
          size="small"
        />
      ) : (
        <Card>
          <Empty description="选择或新建一个表单" />
        </Card>
      )}
    </div>
  )
}
