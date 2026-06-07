import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { PlusOutlined, ReloadOutlined, SaveOutlined, SearchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { messageTemplateApi } from '../../services/messageTemplateApi'
import {
  channelLabel,
  formatMissingVariables,
  localTemplatePreview,
  templatePreviewState,
  templateStatusView,
  variablesFromBody,
  type MessageTemplate,
  type MessageTemplateDraft,
  type TemplatePreviewResult,
} from './messageTemplateCenter'

const { Title, Text, Paragraph } = Typography
const CHANNEL_OPTIONS = ['SMS', 'EMAIL', 'PUSH', 'WECHAT', 'IN_APP', 'WEBHOOK']
  .map(value => ({ value, label: channelLabel(value) }))

interface SearchValues {
  keyword?: string
  channel?: string
}

interface EditorValues extends MessageTemplateDraft {
  previewContextJson?: string
}

function trimText(value?: string) {
  const text = value?.trim()
  return text || undefined
}

function parsePreviewContext(value?: string): Record<string, unknown> {
  const text = value?.trim()
  if (!text) return {}
  const parsed = JSON.parse(text) as unknown
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('预览上下文必须是 JSON 对象')
  }
  return parsed as Record<string, unknown>
}

export default function MessageTemplatesPage() {
  const [searchForm] = Form.useForm<SearchValues>()
  const [editorForm] = Form.useForm<EditorValues>()
  const [templates, setTemplates] = useState<MessageTemplate[]>([])
  const [selected, setSelected] = useState<MessageTemplate | null>(null)
  const [preview, setPreview] = useState<TemplatePreviewResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [previewing, setPreviewing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const watchedBody = Form.useWatch('body', editorForm) ?? ''
  const draftVariables = useMemo(() => variablesFromBody(watchedBody), [watchedBody])

  const loadTemplates = async (values: SearchValues = searchForm.getFieldsValue()) => {
    setLoading(true)
    setError(null)
    try {
      const response = await messageTemplateApi.search({
        keyword: trimText(values.keyword),
        channel: trimText(values.channel),
      })
      setTemplates(response.data)
      if (!selected && response.data.length > 0) {
        selectTemplate(response.data[0])
      }
    } catch (caught) {
      setError((caught as Error).message || '模板列表加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTemplates()
  }, [])

  const selectTemplate = (template: MessageTemplate) => {
    setSelected(template)
    setPreview(null)
    editorForm.setFieldsValue({
      templateCode: template.templateCode,
      displayName: template.displayName,
      channel: template.channel,
      body: template.body,
      previewContextJson: '{}',
    })
  }

  const handleNew = () => {
    setSelected(null)
    setPreview(null)
    editorForm.setFieldsValue({
      templateCode: '',
      displayName: '',
      channel: 'SMS',
      body: 'Hi {{firstName}}',
      previewContextJson: '{"firstName":"Alice"}',
    })
  }

  const handleSave = async (values: EditorValues) => {
    setSaving(true)
    setError(null)
    try {
      const payload: MessageTemplateDraft = {
        templateCode: values.templateCode.trim(),
        displayName: values.displayName.trim(),
        channel: values.channel,
        body: values.body,
      }
      const response = await messageTemplateApi.create(payload)
      setSelected(response.data)
      message.success('模板已保存')
      await loadTemplates(searchForm.getFieldsValue())
    } catch (caught) {
      setError((caught as Error).message || '模板保存失败')
    } finally {
      setSaving(false)
    }
  }

  const handlePreview = async () => {
    const values = editorForm.getFieldsValue()
    setPreviewing(true)
    setError(null)
    try {
      const context = parsePreviewContext(values.previewContextJson)
      const response = selected?.templateCode
        ? await messageTemplateApi.preview(selected.templateCode, context)
        : { data: localTemplatePreview(values.body ?? '', context) }
      setPreview(response.data)
    } catch (caught) {
      setError((caught as Error).message || '模板预览失败')
    } finally {
      setPreviewing(false)
    }
  }

  const columns: ColumnsType<MessageTemplate> = [
    {
      title: '模板',
      render: (_, row) => (
        <Space direction="vertical" size={0}>
          <Button type="link" style={{ padding: 0 }} onClick={() => selectTemplate(row)}>
            {row.displayName}
          </Button>
          <Text type="secondary">{row.templateCode}</Text>
        </Space>
      ),
    },
    {
      title: '渠道',
      dataIndex: 'channel',
      width: 120,
      render: value => channelLabel(value),
    },
    {
      title: '变量',
      dataIndex: 'variables',
      render: (variables: string[]) => (
        <Space size={[4, 4]} wrap>
          {variables.length === 0 ? <Text type="secondary">无变量</Text> : variables.map(variable => (
            <Tag key={variable}>{variable}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: value => {
        const view = templateStatusView(value)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
  ]

  const previewState = preview ? templatePreviewState(preview) : null

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
        <Title level={3} style={{ margin: 0 }}>模板中心</Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => loadTemplates()} loading={loading}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleNew}>
            新建模板
          </Button>
        </Space>
      </Space>

      {error && <Alert type="error" showIcon message={error} />}

      <Card size="small">
        <Form form={searchForm} layout="inline" onFinish={loadTemplates}>
          <Form.Item name="keyword" label="关键词">
            <Input placeholder="编码或名称" allowClear />
          </Form.Item>
          <Form.Item name="channel" label="渠道">
            <Select allowClear style={{ width: 140 }} options={CHANNEL_OPTIONS} />
          </Form.Item>
          <Button htmlType="submit" icon={<SearchOutlined />} loading={loading}>
            搜索
          </Button>
        </Form>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={13}>
          <Card size="small" title="模板列表">
            <Table
              rowKey="templateCode"
              columns={columns}
              dataSource={templates}
              loading={loading}
              pagination={false}
              locale={{ emptyText: <Empty description="暂无消息模板" /> }}
            />
          </Card>
        </Col>

        <Col xs={24} xl={11}>
          <Card size="small" title={selected ? '编辑模板' : '新建模板'}>
            <Form
              form={editorForm}
              layout="vertical"
              initialValues={{
                templateCode: '',
                displayName: '',
                channel: 'SMS',
                body: 'Hi {{firstName}}',
                previewContextJson: '{"firstName":"Alice"}',
              }}
              onFinish={handleSave}
            >
              <Row gutter={12}>
                <Col xs={24} md={12}>
                  <Form.Item name="templateCode" label="模板编码" rules={[{ required: true }]}>
                    <Input disabled={Boolean(selected)} placeholder="welcome_sms" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item name="channel" label="渠道" rules={[{ required: true }]}>
                    <Select options={CHANNEL_OPTIONS} />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item name="displayName" label="模板名称" rules={[{ required: true }]}>
                <Input placeholder="Welcome SMS" />
              </Form.Item>

              <Form.Item name="body" label="模板内容" rules={[{ required: true }]}>
                <Input.TextArea rows={5} placeholder="Hi {{firstName}}" />
              </Form.Item>

              <Space size={[4, 4]} wrap>
                {draftVariables.length === 0 ? <Text type="secondary">当前内容无变量</Text> : draftVariables.map(variable => (
                  <Tag key={variable}>{variable}</Tag>
                ))}
              </Space>

              <Form.Item name="previewContextJson" label="预览上下文 JSON" style={{ marginTop: 12 }}>
                <Input.TextArea rows={3} />
              </Form.Item>

              <Space>
                <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>
                  保存模板
                </Button>
                <Button onClick={handlePreview} loading={previewing}>
                  预览
                </Button>
              </Space>
            </Form>

            {preview && (
              <Alert
                style={{ marginTop: 16 }}
                type={previewState?.status === 'READY' ? 'success' : 'warning'}
                showIcon
                message={formatMissingVariables(preview.missingVariables)}
                description={<Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>{previewState?.text}</Paragraph>}
              />
            )}
          </Card>
        </Col>
      </Row>
    </Space>
  )
}
