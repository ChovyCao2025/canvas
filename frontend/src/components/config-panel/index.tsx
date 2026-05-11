import { useEffect, useState, useCallback } from 'react'
import { Form, Input, InputNumber, Select, Switch, Button, Typography, Spin, Divider } from 'antd'
import { metaApi } from '../../services/api'
import type { NodeTypeRegistry, SchemaField, StubOption } from '../../types'
import type { CanvasNodeData } from '../canvas/constants'

const { Text } = Typography

interface Props {
  nodeId: string | null
  nodeData: CanvasNodeData | null
  onChange: (nodeId: string, patch: Partial<CanvasNodeData>) => void
}

export default function ConfigPanel({ nodeId, nodeData, onChange }: Props) {
  const [schema, setSchema] = useState<NodeTypeRegistry | null>(null)
  const [options, setOptions] = useState<Record<string, StubOption[]>>({})
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm()

  useEffect(() => {
    if (!nodeData?.nodeType) { setSchema(null); return }
    setLoading(true)
    metaApi.getNodeTypeSchema(nodeData.nodeType)
      .then((res) => setSchema(res.data))
      .finally(() => setLoading(false))
  }, [nodeData?.nodeType])

  // 设置表单初始值
  useEffect(() => {
    if (nodeData) {
      form.setFieldsValue({ name: nodeData.name, ...nodeData.bizConfig })
    }
  }, [nodeId, nodeData, form])

  // 加载 select 下拉选项
  useEffect(() => {
    if (!schema) return
    const fields: SchemaField[] = JSON.parse(schema.configSchema || '[]')
    fields.filter(f => f.type === 'select' && f.dataSource).forEach(f => {
      if (!f.dataSource) return
      // 根据 dataSource 路径决定调哪个 API
      const src = f.dataSource
      const fetcher: Promise<{ data: StubOption[] }> | null =
        src.includes('mq-topics')     ? metaApi.getMqTopics() :
        src.includes('coupon-types')  ? metaApi.getCouponTypes() :
        src.includes('reach-scenes')  ? metaApi.getReachScenes() :
        src.includes('ab-experiments') ? metaApi.getAbExperiments() :
        src.includes('tagger-tags') && src.includes('realtime') ? metaApi.getTaggerTags('realtime') :
        src.includes('tagger-tags') && src.includes('offline')  ? metaApi.getTaggerTags('offline') :
        src.includes('biz-lines')    ? metaApi.getBizLines() :
        null

      if (fetcher) {
        fetcher.then(res => setOptions(prev => ({ ...prev, [f.key]: res.data })))
      }
    })
  }, [schema])

  const handleValuesChange = useCallback(
    (changed: Record<string, unknown>) => {
      if (!nodeId || !nodeData) return
      const { name, ...rest } = changed
      onChange(nodeId, {
        ...(name !== undefined ? { name: name as string } : {}),
        bizConfig: { ...nodeData.bizConfig, ...rest },
      })
    },
    [nodeId, nodeData, onChange],
  )

  if (!nodeId || !nodeData) {
    return (
      <div style={{ padding: 16, color: '#bbb', fontSize: 12 }}>
        点击画布中的节点查看配置
      </div>
    )
  }

  const fields: SchemaField[] = schema ? JSON.parse(schema.configSchema || '[]') : []

  return (
    <div style={{ padding: '12px 12px 0', overflowY: 'auto', height: '100%' }}>
      {loading && <Spin size="small" style={{ display: 'block', marginBottom: 8 }} />}
      <Text type="secondary" style={{ fontSize: 11 }}>
        {nodeData.nodeType}
      </Text>
      <Divider style={{ margin: '8px 0' }} />

      <Form form={form} layout="vertical" size="small" onValuesChange={handleValuesChange}>
        <Form.Item name="name" label="节点名称" rules={[{ required: true }]}>
          <Input />
        </Form.Item>

        {fields.map(field => renderField(field, options))}
      </Form>
    </div>
  )
}

function renderField(field: SchemaField, options: Record<string, StubOption[]>) {
  const label = field.label + (field.required ? ' *' : '')

  const control = (() => {
    switch (field.type) {
      case 'select':
        return (
          <Select
            options={(options[field.key] ?? []).map(o => ({ label: o.label, value: o.key }))}
            placeholder={`请选择${field.label}`}
          />
        )
      case 'number':
        return <InputNumber style={{ width: '100%' }} />
      case 'toggle':
        return <Switch />
      case 'code-editor':
        return <Input.TextArea rows={6} style={{ fontFamily: 'monospace', fontSize: 12 }} />
      default:
        return <Input placeholder={field.label} />
    }
  })()

  return (
    <Form.Item key={field.key} name={field.key} label={label}
      valuePropName={field.type === 'toggle' ? 'checked' : 'value'}>
      {control}
    </Form.Item>
  )
}
