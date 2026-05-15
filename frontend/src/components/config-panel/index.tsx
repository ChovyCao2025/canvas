import { useEffect, useState, useCallback } from 'react'
import {
  Form, Input, InputNumber, Select, Switch, Button,
  Typography, Spin, Divider, Space, Collapse, Tag,
} from 'antd'
import { PlusOutlined, DeleteOutlined, DownOutlined } from '@ant-design/icons'
import { metaApi, canvasApi } from '../../services/api'
import type { NodeTypeRegistry, ContextField, StubOption, Canvas } from '../../types'
import type { CanvasNodeData } from '../canvas/constants'

const { Text } = Typography

interface Props {
  nodeId:   string | null
  nodeData: CanvasNodeData | null
  onChange: (nodeId: string, patch: Partial<CanvasNodeData>) => void
}

// ── 模块级缓存（会话内不重复请求）────────────────────────────────
const schemaCache   = new Map<string, NodeTypeRegistry>()
const optionsCache  = new Map<string, StubOption[]>()
let contextFieldsCache: ContextField[] | null = null

export default function ConfigPanel({ nodeId, nodeData, onChange }: Props) {
  const [schema,   setSchema]   = useState<NodeTypeRegistry | null>(null)
  const [options,  setOptions]  = useState<Record<string, StubOption[]>>({})
  const [ctxFields, setCtxFields] = useState<ContextField[]>([])
  const [loading,  setLoading]  = useState(false)
  const [formValues, setFormValues] = useState<Record<string, unknown>>({})
  const [form] = Form.useForm()

  useEffect(() => {
    if (!nodeData?.nodeType) { setSchema(null); return }
    setLoading(true)

    // 上下文字段（全局，只请求一次）
    if (!contextFieldsCache) {
      metaApi.getContextFields().then(res => {
        contextFieldsCache = res.data
        setCtxFields(res.data)
      })
    } else {
      setCtxFields(contextFieldsCache)
    }

    // Schema（按 nodeType 缓存）
    const cached = schemaCache.get(nodeData.nodeType)
    if (cached) {
      setSchema(cached); setLoading(false)
      return
    }
    metaApi.getNodeTypeSchema(nodeData.nodeType)
      .then(res => { schemaCache.set(nodeData.nodeType, res.data); setSchema(res.data) })
      .finally(() => setLoading(false))
  }, [nodeData?.nodeType])

  // 加载 select 下拉选项（按 dataSource 缓存）
  useEffect(() => {
    if (!schema) return
    const fields = parseSchema(schema.configSchema)
    const loaders: Array<[string, () => Promise<{data: StubOption[]}>]> = []
    fields.filter(f => f.type === 'select' && f.dataSource).forEach(f => {
      const src = f.dataSource!
      if (optionsCache.has(src)) {
        setOptions(prev => ({ ...prev, [f.key]: optionsCache.get(src)! }))
        return
      }
      const fetcher = dataSourceFetcher(src)
      if (fetcher) loaders.push([f.key, fetcher])
    })
    loaders.forEach(([key, fetch]) =>
      fetch().then(res => {
        optionsCache.set(key, res.data)
        setOptions(prev => ({ ...prev, [key]: res.data }))
      })
    )
  }, [schema])

  // 同步表单初始值
  useEffect(() => {
    if (nodeData) {
      const vals = { name: nodeData.name, ...nodeData.bizConfig }
      form.setFieldsValue(vals)
      setFormValues(vals as Record<string, unknown>)
    }
  }, [nodeId, nodeData, form])

  const handleValuesChange = useCallback((changed: Record<string, unknown>, all: Record<string, unknown>) => {
    if (!nodeId || !nodeData) return
    setFormValues(all)
    const { name, ...rest } = changed
    onChange(nodeId, {
      ...(name !== undefined ? { name: name as string } : {}),
      bizConfig: { ...nodeData.bizConfig, ...rest },
    })
  }, [nodeId, nodeData, onChange])

  if (!nodeId || !nodeData) {
    return (
      <div style={{ padding: 16, color: '#bbb', fontSize: 12 }}>
        点击画布中的节点查看配置
      </div>
    )
  }

  const fields = parseSchema(schema?.configSchema)

  return (
    <div style={{ padding: '12px 12px 0', overflowY: 'auto', height: '100%' }}>
      {loading && <Spin size="small" style={{ display: 'block', marginBottom: 8 }} />}
      <Text type="secondary" style={{ fontSize: 11 }}>{nodeData.nodeType}</Text>
      <Divider style={{ margin: '8px 0' }} />

      <Form form={form} layout="vertical" size="small" onValuesChange={handleValuesChange}>
        <Form.Item name="name" label="节点名称" rules={[{ required: true }]}>
          <Input />
        </Form.Item>

        {fields
          .filter(f => evaluateVisible(f.visible, formValues))
          .map(field => (
            <Form.Item key={field.key} name={field.key} label={field.label}
              rules={field.required ? [{ required: true, message: `请填写${field.label}` }] : []}>
              {renderControl(field, options, ctxFields, form)}
            </Form.Item>
          ))
        }
      </Form>
    </div>
  )
}

// ── visible 条件评估（设计文档 config_schema 规范）─────────────────
function evaluateVisible(visible: string | undefined, values: Record<string, unknown>): boolean {
  if (!visible) return true
  // 支持：fieldName==value、fieldName!=value、fieldName==true
  const eq  = visible.match(/^(\w+)\s*==\s*(.+)$/)
  const neq = visible.match(/^(\w+)\s*!=\s*(.+)$/)
  if (eq) {
    const [, fname, expected] = eq
    const actual = values[fname]
    return String(actual ?? '') === expected.trim() ||
           (expected.trim() === 'true'  && actual === true) ||
           (expected.trim() === 'false' && actual === false)
  }
  if (neq) {
    const [, fname, expected] = neq
    return String(values[fname] ?? '') !== expected.trim()
  }
  return true
}

// ── 控件映射（含自定义复杂控件）─────────────────────────────────
function renderControl(
  field: SchemaField,
  options: Record<string, StubOption[]>,
  ctxFields: ContextField[],
  form: ReturnType<typeof Form.useForm>[0],
): React.ReactNode {
  switch (field.type) {
    case 'select':
      return (
        <Select
          options={(options[field.key] ?? field.options ?? []).map((o: any) =>
            ({ label: o.label ?? o.option_name, value: o.key ?? o.value }))}
          placeholder={`请选择${field.label}`}
          showSearch filterOption={(v, opt) =>
            String(opt?.label ?? '').toLowerCase().includes(v.toLowerCase())}
        />
      )
    case 'number':
      return <InputNumber style={{ width: '100%' }} defaultValue={field.defaultValue as number} />
    case 'toggle':
      return <Switch />
    case 'radio':
      return (
        <Select options={(field.options ?? []).map((o: any) => ({ label: o.label, value: o.value }))}
          placeholder={`请选择${field.label}`} />
      )
    case 'code-editor':
      return (
        <Input.TextArea rows={8}
          style={{ fontFamily: 'SFMono-Regular, Consolas, monospace', fontSize: 12 }}
          placeholder={`// Groovy 脚本`} />
      )
    case 'condition-rule-list':
      return <ConditionRuleList ctxFields={ctxFields} />
    case 'context-value-list':
      return <ContextValueList ctxFields={ctxFields} />
    case 'param-define-list':
      return <ParamDefineList />
    case 'branch-list':
      return <BranchList ctxFields={ctxFields} />
    case 'ab-group-list':
      return <AbGroupList />
    case 'priority-list':
      return <PriorityList />
    case 'key-value':
      return <KeyValueMapping fieldKey={field.key} ctxFields={ctxFields} />
    case 'canvas-select':
      return <CanvasSelector />
    case 'node-select':
      return <Input placeholder="节点 ID（连线后自动填入）" />
    default:
      return <Input placeholder={field.label} />
  }
}

// ── 条件规则列表控件（IF判断 / SELECTOR / MQ_TRIGGER 等）─────────
interface ConditionRule {
  field: string; operator: string; value: string; isCustom: boolean
}
function ConditionRuleList({ ctxFields }: { ctxFields: ContextField[] }) {
  const form = Form.useFormInstance()
  const fieldKey = 'rules'
  const rules: ConditionRule[] = Form.useWatch(fieldKey, form) ?? []
  const ops = ['EQ', 'NEQ', 'CONTAINS', 'GT', 'LT', 'GTE', 'LTE']

  const add = () => form.setFieldValue(fieldKey,
    [...rules, { field: '', operator: 'EQ', value: '', isCustom: true }])
  const remove = (i: number) => {
    const next = [...rules]; next.splice(i, 1); form.setFieldValue(fieldKey, next)
  }
  const update = (i: number, k: keyof ConditionRule, v: string | boolean) => {
    const next = [...rules]; (next[i] as any)[k] = v; form.setFieldValue(fieldKey, next)
  }

  return (
    <div>
      {rules.map((rule, i) => (
        <Space key={i} style={{ display: 'flex', marginBottom: 4 }} align="center">
          <Select size="small" style={{ width: 100 }} placeholder="字段"
            value={rule.field || undefined}
            options={ctxFields.map(f => ({ label: f.fieldName, value: f.fieldKey }))}
            onChange={v => update(i, 'field', v)} showSearch />
          <Select size="small" style={{ width: 80 }} value={rule.operator}
            options={ops.map(o => ({ label: o, value: o }))}
            onChange={v => update(i, 'operator', v)} />
          <Input size="small" style={{ width: 100 }} placeholder="值"
            value={rule.value} onChange={e => update(i, 'value', e.target.value)} />
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(i)} />
        </Space>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add}>
        添加条件
      </Button>
    </div>
  )
}

// ── 上下文引用值列表控件（IN_APP_NOTIFY / GROOVY inputParams 等）─
interface ContextValueItem { name: string; valueType: 'CUSTOM' | 'CONTEXT'; value: string }
function ContextValueList({ ctxFields }: { ctxFields: ContextField[] }) {
  const form = Form.useFormInstance()
  const fieldKey = 'bizData'
  const items: ContextValueItem[] = Form.useWatch(fieldKey, form) ?? []

  const add = () => form.setFieldValue(fieldKey,
    [...items, { name: '', valueType: 'CUSTOM', value: '' }])
  const remove = (i: number) => {
    const next = [...items]; next.splice(i, 1); form.setFieldValue(fieldKey, next)
  }
  const update = (i: number, k: keyof ContextValueItem, v: string) => {
    const next = [...items]; (next[i] as any)[k] = v; form.setFieldValue(fieldKey, next)
  }

  return (
    <div>
      {items.map((item, i) => (
        <Space key={i} style={{ display: 'flex', marginBottom: 4 }} align="center">
          <Input size="small" style={{ width: 80 }} placeholder="字段名"
            value={item.name} onChange={e => update(i, 'name', e.target.value)} />
          <Select size="small" style={{ width: 80 }} value={item.valueType}
            options={[{ label: '自定义', value: 'CUSTOM' }, { label: '上下文', value: 'CONTEXT' }]}
            onChange={v => update(i, 'valueType', v)} />
          {item.valueType === 'CONTEXT'
            ? <Select size="small" style={{ width: 110 }} placeholder="选择字段"
                value={item.value || undefined}
                options={ctxFields.map(f => ({ label: f.fieldName, value: f.fieldKey }))}
                onChange={v => update(i, 'value', v)} showSearch />
            : <Input size="small" style={{ width: 110 }} placeholder="值"
                value={item.value} onChange={e => update(i, 'value', e.target.value)} />
          }
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(i)} />
        </Space>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add}>添加</Button>
    </div>
  )
}

// ── 参数定义列表控件（DIRECT_CALL inputParams / GROOVY outputParams）
interface ParamDef { name: string; description?: string; dataType: string; required?: boolean }
function ParamDefineList() {
  const form = Form.useFormInstance()
  const fieldKey = 'inputParams'
  const items: ParamDef[] = Form.useWatch(fieldKey, form) ?? []

  const add = () => form.setFieldValue(fieldKey,
    [...items, { name: '', dataType: 'STRING', required: false }])
  const remove = (i: number) => {
    const next = [...items]; next.splice(i, 1); form.setFieldValue(fieldKey, next)
  }
  const update = (i: number, k: keyof ParamDef, v: string | boolean) => {
    const next = [...items]; (next[i] as any)[k] = v; form.setFieldValue(fieldKey, next)
  }

  return (
    <div>
      {items.map((p, i) => (
        <Space key={i} style={{ display: 'flex', marginBottom: 4 }} align="center">
          <Input size="small" style={{ width: 90 }} placeholder="参数名"
            value={p.name} onChange={e => update(i, 'name', e.target.value)} />
          <Select size="small" style={{ width: 80 }} value={p.dataType}
            options={['STRING','NUMBER','BOOLEAN','LIST'].map(t => ({ label: t, value: t }))}
            onChange={v => update(i, 'dataType', v)} />
          <Switch size="small" checked={!!p.required}
            onChange={v => update(i, 'required', v)} />
          <Tag style={{ marginLeft: -4, cursor: 'default' }}>必填</Tag>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(i)} />
        </Space>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add}>添加参数</Button>
    </div>
  )
}

// ── SELECTOR 分支列表控件（branch-list）────────────────────────────
interface BranchItem { label: string; strategyRelation: string; conditions: ConditionRule[]; nextNodeId?: string }
function BranchList({ ctxFields }: { ctxFields: ContextField[] }) {
  const form = Form.useFormInstance()
  const branches: BranchItem[] = Form.useWatch('branches', form) ?? []
  const ops = ['EQ', 'NEQ', 'CONTAINS', 'GT', 'LT', 'GTE', 'LTE']

  const addBranch = () => form.setFieldValue('branches', [
    ...branches,
    { label: branches.length === 0 ? '如果' : '否则如果', strategyRelation: 'AND', conditions: [], nextNodeId: undefined }
  ])
  const removeBranch = (i: number) => {
    const next = [...branches]; next.splice(i, 1); form.setFieldValue('branches', next)
  }
  const updateBranch = (i: number, k: keyof BranchItem, v: unknown) => {
    const next = [...branches]; (next[i] as any)[k] = v; form.setFieldValue('branches', next)
  }
  const addCondition = (branchIdx: number) => {
    const next = [...branches]
    next[branchIdx] = { ...next[branchIdx], conditions: [...(next[branchIdx].conditions ?? []), { field: '', operator: 'EQ', value: '', isCustom: true }] }
    form.setFieldValue('branches', next)
  }
  const removeCondition = (bi: number, ci: number) => {
    const next = [...branches]; next[bi].conditions.splice(ci, 1); form.setFieldValue('branches', next)
  }
  const updateCondition = (bi: number, ci: number, k: string, v: string) => {
    const next = [...branches]; (next[bi].conditions[ci] as any)[k] = v; form.setFieldValue('branches', next)
  }

  const items = branches.map((b, i) => ({
    key: String(i),
    label: (
      <Space>
        <span style={{ fontSize: 12, fontWeight: 500, minWidth: 52 }}>{b.label}</span>
        <Select size="small" style={{ width: 60 }} value={b.strategyRelation}
          options={[{ label: 'AND', value: 'AND' }, { label: 'OR', value: 'OR' }]}
          onChange={v => updateBranch(i, 'strategyRelation', v)}
          onClick={e => e.stopPropagation()} />
        <Button size="small" danger type="text" icon={<DeleteOutlined />}
          onClick={e => { e.stopPropagation(); removeBranch(i) }} />
      </Space>
    ),
    children: (
      <div>
        {b.conditions?.map((c, ci) => (
          <Space key={ci} style={{ display: 'flex', marginBottom: 4 }}>
            <Select size="small" style={{ width: 90 }} placeholder="字段"
              value={c.field || undefined}
              options={ctxFields.map(f => ({ label: f.fieldName, value: f.fieldKey }))}
              onChange={v => updateCondition(i, ci, 'field', v)} showSearch />
            <Select size="small" style={{ width: 72 }} value={c.operator}
              options={ops.map(o => ({ label: o, value: o }))}
              onChange={v => updateCondition(i, ci, 'operator', v)} />
            {c.isContext ? (
              <Select size="small" style={{ width: 90 }} placeholder="上下文字段"
                value={c.value || undefined}
                options={ctxFields.map(f => ({ label: f.fieldName, value: '${' + f.fieldKey + '}' }))}
                onChange={v => updateCondition(i, ci, 'value', v)} showSearch />
            ) : (
              <Input size="small" style={{ width: 90 }} placeholder="静态值"
                value={c.value} onChange={e => updateCondition(i, ci, 'value', e.target.value)} />
            )}
            <Button size="small" type={c.isContext ? 'primary' : 'default'} style={{ fontSize: 10, padding: '0 4px' }}
              title={c.isContext ? '切换为静态值' : '切换为上下文字段'}
              onClick={() => updateCondition(i, ci, 'isContext', String(!c.isContext))}>
              {c.isContext ? '变量' : '值'}
            </Button>
            <Button size="small" danger icon={<DeleteOutlined />} onClick={() => removeCondition(i, ci)} />
          </Space>
        ))}
        <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={() => addCondition(i)}>添加条件</Button>
      </div>
    ),
  }))

  return (
    <div>
      {branches.length > 0 && (
        <Collapse size="small" items={items} style={{ marginBottom: 8 }} />
      )}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={addBranch}>
        {branches.length === 0 ? '添加第一个分支' : '添加分支'}
      </Button>
    </div>
  )
}

// ── AB 分流分组路由控件（ab-group-list）────────────────────────────
interface AbGroup { groupKey: string; nextNodeId?: string }
function AbGroupList() {
  const form = Form.useFormInstance()
  const groups: AbGroup[] = Form.useWatch('groups', form) ?? []
  const add = () => form.setFieldValue('groups', [...groups, { groupKey: `G${groups.length + 1}`, nextNodeId: undefined }])
  const remove = (i: number) => { const n = [...groups]; n.splice(i, 1); form.setFieldValue('groups', n) }
  const update = (i: number, k: keyof AbGroup, v: string) => {
    const n = [...groups]; (n[i] as any)[k] = v; form.setFieldValue('groups', n)
  }

  return (
    <div>
      {groups.map((g, i) => (
        <Space key={i} style={{ display: 'flex', marginBottom: 4 }}>
          <Input size="small" style={{ width: 80 }} placeholder="分组Key"
            value={g.groupKey} onChange={e => update(i, 'groupKey', e.target.value)} />
          <span style={{ fontSize: 11, color: '#999' }}>→ 通过连线设置后继节点</span>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(i)} />
        </Space>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add}>添加分组</Button>
      <div style={{ fontSize: 11, color: '#999', marginTop: 4 }}>
        Hash(userId:experimentKey) % 100 等比分配，分组顺序决定 bucket 范围
      </div>
    </div>
  )
}

// ── 优先级列表控件（priority-list）────────────────────────────────
interface PriorityItem { order: number; nextNodeId?: string }
function PriorityList() {
  const form = Form.useFormInstance()
  const priorities: PriorityItem[] = Form.useWatch('priorities', form) ?? []
  const add = () => form.setFieldValue('priorities', [...priorities, { order: priorities.length + 1, nextNodeId: undefined }])
  const remove = (i: number) => { const n = [...priorities]; n.splice(i, 1); form.setFieldValue('priorities', n) }
  const update = (i: number, k: keyof PriorityItem, v: string | number) => {
    const n = [...priorities]; (n[i] as any)[k] = v; form.setFieldValue('priorities', n)
  }

  return (
    <div>
      {priorities.map((p, i) => (
        <Space key={i} style={{ display: 'flex', marginBottom: 4 }}>
          <InputNumber size="small" style={{ width: 60 }} placeholder="优先级"
            value={p.order} onChange={v => update(i, 'order', v ?? i + 1)} min={1} />
          <Input size="small" style={{ width: 110 }} placeholder="后继节点ID"
            value={p.nextNodeId ?? ''} onChange={e => update(i, 'nextNodeId', e.target.value)} />
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(i)} />
        </Space>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add}>添加优先级</Button>
      <div style={{ fontSize: 11, color: '#999', marginTop: 4 }}>
        按 order 从小到大依次尝试，第一个成功则停止
      </div>
    </div>
  )
}

// ── 键值映射控件（CANVAS_TRIGGER paramMapping / SUB_FLOW_REF inputMapping）
function KeyValueMapping({ fieldKey, ctxFields }: { fieldKey: string; ctxFields: ContextField[] }) {
  const form = Form.useFormInstance()
  const mapping: Record<string, string> = Form.useWatch(fieldKey, form) ?? {}
  const entries = Object.entries(mapping)

  const add = () => form.setFieldValue(fieldKey, { ...mapping, '': '' })
  const remove = (k: string) => {
    const n = { ...mapping }; delete n[k]; form.setFieldValue(fieldKey, n)
  }
  const updateKey = (oldKey: string, newKey: string) => {
    const n: Record<string, string> = {}
    Object.entries(mapping).forEach(([k, v]) => n[k === oldKey ? newKey : k] = v)
    form.setFieldValue(fieldKey, n)
  }
  const updateVal = (k: string, v: string) => form.setFieldValue(fieldKey, { ...mapping, [k]: v })

  return (
    <div>
      {entries.map(([k, v], i) => (
        <Space key={i} style={{ display: 'flex', marginBottom: 4 }}>
          <Input size="small" style={{ width: 90 }} placeholder="子流程字段名"
            value={k} onChange={e => updateKey(k, e.target.value)} />
          <span style={{ fontSize: 12 }}>←</span>
          <Select size="small" style={{ width: 110 }} placeholder="父上下文字段"
            value={v || undefined}
            options={ctxFields.map(f => ({ label: f.fieldName, value: `ctx.${f.fieldKey}` }))}
            onChange={nv => updateVal(k, nv)} showSearch />
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(k)} />
        </Space>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add}>添加映射</Button>
    </div>
  )
}

// ── 画布选择控件（CANVAS_TRIGGER / SUB_FLOW_REF）─────────────────
let canvasListCache: Canvas[] | null = null

function CanvasSelector() {
  const [canvases, setCanvases] = useState<Canvas[]>(canvasListCache ?? [])
  const [loading, setLoading] = useState(!canvasListCache)

  useEffect(() => {
    if (canvasListCache) return
    setLoading(true)
    canvasApi.list({ status: 1, size: 100 })
      .then(r => { canvasListCache = r.data.list; setCanvases(r.data.list) })
      .finally(() => setLoading(false))
  }, [])

  return (
    <Select
      loading={loading}
      showSearch
      placeholder="搜索并选择已发布画布"
      filterOption={(input, opt) =>
        String(opt?.label ?? '').toLowerCase().includes(input.toLowerCase())}
      options={canvases.map(c => ({ label: `${c.name} (ID:${c.id})`, value: c.id }))}
    />
  )
}

interface SchemaField {
  key: string; label: string; type: string
  required?: boolean; options?: any[]; dataSource?: string
  visible?: string; defaultValue?: unknown
}

function parseSchema(raw: string | undefined): SchemaField[] {
  try { return raw ? JSON.parse(raw) : [] } catch { return [] }
}

function dataSourceFetcher(src: string): (() => Promise<{data: StubOption[]}>) | null {
  if (src.includes('api-definitions'))  return metaApi.getApiDefinitions
  if (src.includes('mq-topics'))        return metaApi.getMqTopics
  if (src.includes('coupon-types'))     return metaApi.getCouponTypes
  if (src.includes('reach-scenes'))     return metaApi.getReachScenes
  if (src.includes('ab-experiments'))   return metaApi.getAbExperiments
  if (src.includes('tagger-tags') && src.includes('realtime')) return () => metaApi.getTaggerTags('realtime')
  if (src.includes('tagger-tags') && src.includes('offline'))  return () => metaApi.getTaggerTags('offline')
  if (src.includes('biz-lines'))        return metaApi.getBizLines
  return null
}
