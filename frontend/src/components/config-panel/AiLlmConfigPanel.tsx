import { useEffect, useMemo, useState } from 'react'
import type { Node } from '@xyflow/react'
import { Form, Input, InputNumber, Select, Spin } from 'antd'
import type { CanvasNodeData } from '../canvas/constants'
import { CATEGORY_SOLID } from '../canvas/constants'
import type { StubOption } from '../../types'
import { aiApi } from '../../services/aiApi'
import { ConfigSectionCard, NodeHeaderCard } from './InspectorCards'
import { CONTROL_CHROME_SELECTOR_CSS, getControlChrome, getControlLabelStyle } from './controlChrome'

interface AiLlmConfigPanelProps {
  nodeId: string
  nodeData: CanvasNodeData
  onChange: (nodeId: string, patch: Partial<CanvasNodeData>) => void
  nodes?: Node<CanvasNodeData>[]
  readonly?: boolean
}

export interface AiLlmFormValues {
  name?: string
  providerId?: string | number
  templateId?: string | number
  modelKey?: string
  temperature?: number
  maxTokens?: number
  timeoutMs?: number
  promptOverride?: string
  schemaOverride?: string
  outputPrefix?: string
  nextNodeId?: string
}

export function createAiLlmPatch(
  nodeData: CanvasNodeData,
  values: AiLlmFormValues,
): Partial<CanvasNodeData> {
  const { name, ...bizValues } = values
  const existingBizConfig = { ...(nodeData.bizConfig ?? {}) }
  delete existingBizConfig.failNodeId
  return {
    ...(name !== undefined ? { name: String(name) } : {}),
    bizConfig: {
      ...existingBizConfig,
      ...bizValues,
    },
  }
}

export function normalizeAiSelectOptions(items: StubOption[] | undefined) {
  return (items ?? [])
    .filter(item => item.key != null && item.label != null)
    .map(item => ({ value: String(item.key), label: item.label }))
}

export function buildNodeTargetOptions(nodes: Node<CanvasNodeData>[] | undefined, currentNodeId: string) {
  return (nodes ?? [])
    .filter(node => node.id !== currentNodeId && node.data?.nodeType !== 'START')
    .map(node => ({
      value: node.id,
      label: node.data?.name ? `${node.data.name} (${node.data.nodeType})` : node.id,
    }))
}

export function isAiLlmConfigured(values: AiLlmFormValues) {
  return Boolean(values.templateId)
}

export default function AiLlmConfigPanel({
  nodeId,
  nodeData,
  onChange,
  nodes,
  readonly,
}: AiLlmConfigPanelProps) {
  const [form] = Form.useForm<AiLlmFormValues>()
  const [providers, setProviders] = useState<StubOption[]>([])
  const [templates, setTemplates] = useState<StubOption[]>([])
  const [models, setModels] = useState<StubOption[]>([])
  const [loading, setLoading] = useState(false)
  const controlChrome = getControlChrome()
  const accentColor = CATEGORY_SOLID[nodeData.category] ?? '#2563eb'

  const values = useMemo<AiLlmFormValues>(() => ({
    name: nodeData.name,
    ...(nodeData.bizConfig ?? {}),
  }), [nodeData])

  useEffect(() => {
    form.setFieldsValue(values)
  }, [form, nodeId, values])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    Promise.all([
      aiApi.listAiProvidersForMeta(),
      aiApi.listAiTemplatesForMeta(),
    ])
      .then(([providerRes, templateRes]) => {
        if (cancelled) return
        setProviders(providerRes.data ?? [])
        setTemplates(templateRes.data ?? [])
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    let cancelled = false
    aiApi.listAiModelsForMeta(values.providerId)
      .then(res => {
        if (!cancelled) setModels(res.data ?? [])
      })
    return () => { cancelled = true }
  }, [values.providerId])

  const providerOptions = normalizeAiSelectOptions(providers)
  const templateOptions = normalizeAiSelectOptions(templates)
  const modelOptions = normalizeAiSelectOptions(models)
  const nodeOptions = buildNodeTargetOptions(nodes, nodeId)

  const applyValues = (_changed: Partial<AiLlmFormValues>, all: AiLlmFormValues) => {
    onChange(nodeId, createAiLlmPatch(nodeData, all))
  }

  const label = (text: string) => (
    <div style={{ ...getControlLabelStyle(), margin: '0 4px 5px' }}>{text}</div>
  )

  return (
    <div style={{ padding: 12, overflowY: 'auto', height: '100%', background: `${accentColor}0d` }}>
      <style>{CONTROL_CHROME_SELECTOR_CSS}</style>
      {loading && <Spin size="small" style={{ display: 'block', marginBottom: 8 }} />}

      <NodeHeaderCard
        tone="default"
        typeBadge="AI_LLM"
        title={nodeData.name}
        statusLabel={isAiLlmConfigured(values) ? '已配置' : '待配置'}
        categoryLabel={nodeData.category}
        categoryColor={accentColor}
        metaBadges={[
          values.templateId ? '模板已选' : '缺少模板',
          values.modelKey ? String(values.modelKey) : '默认模型',
        ]}
      />

      <Form
        className="config-panel-form"
        form={form}
        layout="vertical"
        onValuesChange={applyValues}
        disabled={readonly}
      >
        <ConfigSectionCard title="基础配置" accentColor={accentColor}>
          <Form.Item name="name" label={label('节点名称')} rules={[{ required: true }]}>
            <Input className="config-panel-ios-input" style={controlChrome} />
          </Form.Item>
          <Form.Item name="providerId" label={label('AI 服务商')}>
            <Select
              className="config-panel-ios-select"
              style={{ width: '100%' }}
              options={providerOptions}
              allowClear
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="templateId" label={label('提示词模板')} rules={[{ required: true, message: '请选择提示词模板' }]}>
            <Select
              className="config-panel-ios-select"
              style={{ width: '100%' }}
              options={templateOptions}
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="modelKey" label={label('模型')}>
            <Select
              className="config-panel-ios-select"
              style={{ width: '100%' }}
              options={modelOptions}
              allowClear
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
        </ConfigSectionCard>

        <ConfigSectionCard title="生成参数" accentColor={accentColor}>
          <Form.Item name="temperature" label={label('温度')}>
            <InputNumber min={0} max={2} step={0.1} style={{ width: '100%', ...controlChrome }} />
          </Form.Item>
          <Form.Item name="maxTokens" label={label('最大 Token')}>
            <InputNumber min={1} max={8000} step={50} style={{ width: '100%', ...controlChrome }} />
          </Form.Item>
          <Form.Item name="timeoutMs" label={label('超时毫秒')}>
            <InputNumber min={100} max={30000} step={100} style={{ width: '100%', ...controlChrome }} />
          </Form.Item>
          <Form.Item name="promptOverride" label={label('提示词覆盖')}>
            <Input.TextArea rows={5} className="config-panel-ios-input" style={controlChrome} />
          </Form.Item>
          <Form.Item name="schemaOverride" label={label('输出 Schema 覆盖')}>
            <Input.TextArea rows={5} className="config-panel-ios-input" style={controlChrome} />
          </Form.Item>
          <Form.Item name="outputPrefix" label={label('输出前缀')}>
            <Input className="config-panel-ios-input" style={controlChrome} />
          </Form.Item>
        </ConfigSectionCard>

        <ConfigSectionCard title="出口路由" accentColor={accentColor}>
          <Form.Item name="nextNodeId" label={label('成功后')}>
            <Select
              className="config-panel-ios-select"
              style={{ width: '100%' }}
              options={nodeOptions}
              allowClear
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
        </ConfigSectionCard>
      </Form>
    </div>
  )
}
