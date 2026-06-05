import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Checkbox, Form, Input, Result, Select, Spin, Typography, message } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import { useLocation, useParams } from 'react-router-dom'
import { marketingFormsApi, type PublicMarketingForm } from '../../services/marketingFormsApi'
import { parseFormFields, type MarketingFormField } from '../marketing-forms/marketingFormsPresentation'

const { Title, Text } = Typography

function randomId(prefix: string) {
  const cryptoApi = globalThis.crypto
  if (cryptoApi && 'randomUUID' in cryptoApi) {
    return `${prefix}:${cryptoApi.randomUUID()}`
  }
  return `${prefix}:${Date.now()}:${Math.random().toString(16).slice(2)}`
}

function anonymousId() {
  const key = 'canvas_marketing_form_anonymous_id'
  const existing = localStorage.getItem(key)
  if (existing) return existing
  const next = randomId('anon')
  localStorage.setItem(key, next)
  return next
}

function utmFromSearch(search: string) {
  const params = new URLSearchParams(search)
  const values: Record<string, string> = {}
  params.forEach((value, key) => {
    if (key.startsWith('utm_')) {
      values[key] = value
    }
  })
  return values
}

function fieldInput(field: MarketingFormField) {
  const type = (field.type ?? 'text').toLowerCase()
  if (type === 'textarea') {
    return <Input.TextArea autoSize={{ minRows: 3, maxRows: 6 }} placeholder={field.placeholder} />
  }
  if (type === 'select') {
    return <Select options={(field.options ?? []).map(option => ({ label: option.label ?? option.value, value: option.value }))} />
  }
  if (type === 'checkbox') {
    return <Checkbox>{field.label ?? field.key}</Checkbox>
  }
  return <Input type={type === 'number' ? 'number' : type === 'email' ? 'email' : 'text'} placeholder={field.placeholder} />
}

export default function PublicMarketingFormPage() {
  const { publicKey = '' } = useParams()
  const location = useLocation()
  const [form] = Form.useForm()
  const [definition, setDefinition] = useState<PublicMarketingForm | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [submitted, setSubmitted] = useState(false)

  const fields = useMemo(() => parseFormFields(definition?.fieldSchemaJson), [definition?.fieldSchemaJson])

  useEffect(() => {
    setLoading(true)
    marketingFormsApi.publicForm(publicKey)
      .then(response => setDefinition(response.data))
      .catch(error => message.error((error as Error).message || '表单加载失败'))
      .finally(() => setLoading(false))
  }, [publicKey])

  const handleSubmit = async (values: Record<string, unknown>) => {
    setSubmitting(true)
    try {
      const response = await marketingFormsApi.publicSubmit(publicKey, {
        response: values,
        utm: utmFromSearch(location.search),
        anonymousId: anonymousId(),
        idempotencyKey: randomId('submit'),
      })
      setSubmitted(true)
      message.success(response.data.successMessage || definition?.successMessage || '提交成功')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <Spin fullscreen />
  }

  if (!definition) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 24, background: '#f5f6fa' }}>
        <Alert type="error" message="表单不可用" />
      </div>
    )
  }

  if (submitted) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 24, background: '#f5f6fa' }}>
        <Result status="success" title={definition.successMessage || '提交成功'} />
      </div>
    )
  }

  return (
    <div style={{ minHeight: '100vh', padding: '48px 16px', background: '#f5f6fa' }}>
      <main style={{ maxWidth: 680, margin: '0 auto' }}>
        <Card>
          <div style={{ marginBottom: 24 }}>
            <Title level={3} style={{ marginTop: 0 }}>{definition.name}</Title>
            {definition.description && <Text type="secondary">{definition.description}</Text>}
          </div>
          <Form form={form} layout="vertical" onFinish={handleSubmit}>
            {fields.map(field => (
              <Form.Item
                key={field.key}
                name={field.key}
                label={(field.type ?? '').toLowerCase() === 'checkbox' ? undefined : field.label ?? field.key}
                valuePropName={(field.type ?? '').toLowerCase() === 'checkbox' ? 'checked' : 'value'}
                rules={field.required ? [{ required: true, message: `请填写${field.label ?? field.key}` }] : undefined}
              >
                {fieldInput(field)}
              </Form.Item>
            ))}
            <Button type="primary" htmlType="submit" icon={<SendOutlined />} loading={submitting}>
              提交
            </Button>
          </Form>
        </Card>
      </main>
    </div>
  )
}
