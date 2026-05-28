/**
 * 组件职责：节点配置面板主组件，根据节点 schema 渲染表单、远程选项和特定节点的复杂控件。
 *
 * 维护说明：这里是画布编辑器右侧属性面板的核心，负责把表单变更归一化后回传节点 data。
 */
import { useEffect, useState, useCallback, useRef } from 'react'
import type { Node } from '@xyflow/react'
import {
  Form, Input, InputNumber, Select, Switch, Button,
  Typography, Spin, Space, Tag, Tooltip, AutoComplete, DatePicker,
} from 'antd'
import { PlusOutlined, DeleteOutlined, QuestionCircleOutlined } from '@ant-design/icons'
import http, { metaApi, canvasApi } from '../../services/api'
import type { NodeTypeRegistry, ContextField, StubOption, Canvas } from '../../types'
import type { CanvasNodeData } from '../canvas/constants'
import { CATEGORY_SOLID } from '../canvas/constants'
import { systemOptionsApi } from '../../services/systemOptions'
import { useSystemOptions } from '../../hooks/useSystemOptions'
import CronBuilder from './CronBuilder'
import {
  BranchRouteCard,
  ConfigSectionCard,
  NodeHeaderCard,
} from './InspectorCards'
import {
  CONTROL_CHROME_SELECTOR_CSS,
  CONTROL_SELECT_CLASS_NAMES,
  getControlChrome,
  getInlineControlChrome,
  getControlLabelStyle,
} from './controlChrome'
import { normalizeFieldOptions, resolveDisplayValue } from './displayValues'
import { buildConfigPanelPresentation } from './presentation'

/** API 参数定义的最小形态，用于事件属性预览等只读控件。 */
interface ApiParamDef { name: string; displayName: string; type: string; required: boolean }

/** 配置面板内常用文本组件别名。 */
const { Text } = Typography

/** 配置面板组件入参，承接编辑器选中节点和回写回调。 */
interface Props {
  /** 当前选中的节点 ID。 */
  nodeId:    string | null

  /** 当前选中节点的数据。 */
  nodeData:  CanvasNodeData | null

  /** 配置变更回调，主编辑器负责把 patch 写回节点。 */
  onChange:  (nodeId: string, patch: Partial<CanvasNodeData>) => void

  /** 画布全部节点，用于解析后继节点名称和动态上下文字段。 */
  nodes?:    Node<CanvasNodeData>[]

  /** 只读态禁用表单编辑。 */
  readonly?: boolean
}

// ── 模块级缓存 ────────────────────────────────────────────────────
// rawCache: URL → 原始响应数组（包含 value/label/requestSchema 等全量字段）
// 统一缓存，彻底替代 apiDefsCache + optionsCache + dataSourceFetcher 分散逻辑
const schemaCache   = new Map<string, NodeTypeRegistry>()
/** 远程 dataSource 原始响应缓存，key 为最终请求 URL。 */
const rawCache      = new Map<string, any[]>()         // key = dataSource URL
/** 系统字典分类缓存，避免同一页面重复请求相同分类。 */
const systemOptionCache = new Map<string, StubOption[]>()
/** 上下文字段缓存，供配置项引用运行时字段。 */
let contextFieldsCache: ContextField[] | null = null

/** 从 dataSource 模板中提取依赖字段，例如 /meta/foo/{apiKey} 依赖 apiKey。 */
export function getDataSourceDependencies(src?: string): string[] {
  if (!src) return []
  return [...src.matchAll(/\{(\w+)\}/g)].map((match) => match[1])
}

/** 判断 dataSource 依赖值是否缺失；缺失时不发起远程请求。 */
function isBlankDependencyValue(value: unknown): boolean {
  return value == null || (typeof value === 'string' && value.trim() === '')
}

/** 用当前表单值填充 dataSource URL 模板；有依赖缺失时返回 null。 */
export function resolveDataSourceTemplate(src: string, values: Record<string, unknown>): string | null {
  let unresolved = false
  const resolved = src.replace(/\{(\w+)\}/g, (_match, key: string) => {
    const value = values[key]
    if (isBlankDependencyValue(value)) {
      unresolved = true
      return ''
    }
    return encodeURIComponent(String(value))
  })

  return unresolved ? null : resolved
}

/** 统一渲染配置项标题，避免每个 Form.Item 重复写样式。 */
function renderControlLabel(label: string): React.ReactNode {
  return <div style={{ ...getControlLabelStyle(), margin: '0 4px 5px' }}>{label}</div>
}

/** 通用数据源加载：任意 dataSource URL，自动缓存，无需逐个注册 */
async function loadDataSource(src: string): Promise<any[]> {
  if (rawCache.has(src)) return rawCache.get(src)!
  const res: any = await http.get(src)
  const data: any[] = res.data ?? []
  rawCache.set(src, data)
  return data
}

/** 将原始数组归一化为 Select options（兼容多种后端字段命名）
 *  StubOption = { key, label }，select 渲染时 value 取 o.key ?? o.value
 */
function toSelectOptions(data: any[]): StubOption[] {
  return data.map(item => ({
    key:   String(item.value ?? item.key ?? item.code ?? item.id ?? ''),
    label: String(item.label ?? item.name ?? item.displayName ?? item.value ?? ''),
  }))
}

/** 加载系统字典分类并缓存，减少同一页面内重复请求。 */
async function loadSystemOptionCategory(category: string): Promise<StubOption[]> {
  if (systemOptionCache.has(category)) return systemOptionCache.get(category)!
  const res = await systemOptionsApi.meta(category)
  systemOptionCache.set(category, res.data)
  return res.data
}

/** 把 StubOption 转为 antd Select 的 options。 */
function selectOptionsFromStubs(options: StubOption[]) {
  return options.map(option => ({ label: option.label, value: option.key }))
}

/** 节点配置面板主组件。 */
export default function ConfigPanel({ nodeId, nodeData, onChange, nodes, readonly }: Props) {
  const [schema,   setSchema]   = useState<NodeTypeRegistry | null>(null)
  const [options,  setOptions]  = useState<Record<string, StubOption[]>>({})
  const [ctxFields, setCtxFields] = useState<ContextField[]>([])
  const [loading,  setLoading]  = useState(false)
  const [formValues, setFormValues] = useState<Record<string, unknown>>({})
  const [form] = Form.useForm()
  const controlChrome = getControlChrome()
  const { raw: conditionOps } = useSystemOptions('condition_operator')
  const { raw: logicRelations } = useSystemOptions('logic_relation')
  const { raw: contextValueTypes } = useSystemOptions('context_value_type')
  const { raw: paramTypes } = useSystemOptions('param_type')
  const { raw: delayUnits } = useSystemOptions('delay_unit')
  const { raw: cronFrequencies } = useSystemOptions('cron_frequency')
  const { raw: weekdays } = useSystemOptions('weekday')

  // 复杂控件共用的字典选项在这里集中转换，避免子控件各自请求。
  const sharedOptions = {
    conditionOps: selectOptionsFromStubs(conditionOps),
    logicRelations: selectOptionsFromStubs(logicRelations),
    contextValueTypes: selectOptionsFromStubs(contextValueTypes),
    paramTypes: selectOptionsFromStubs(paramTypes),
    delayUnits: selectOptionsFromStubs(delayUnits),
    cronFrequencies: selectOptionsFromStubs(cronFrequencies),
    weekdays: weekdays.map(option => ({ label: option.label, value: Number(option.key) })),
  }

  /** 根据节点 ID 反查画布中的节点名称，用于分支/连线摘要。 */
  const getNodeName = (id: string | undefined): string | null => {
    if (!id || !nodes) return null
    return nodes.find(n => n.id === id)?.data.name ?? null
  }

  // 用画布中影响上下文字段的节点数据生成稳定 key，避免每次渲染都触发 effect
  const ctxSignature = nodes
    ?.filter(n => n.data?.nodeType === 'EVENT_TRIGGER' || n.data?.nodeType === 'API_CALL')
    .map(n => `${n.data?.nodeType}:${(n.data?.bizConfig as any)?.eventCode ?? ''}:${(n.data?.bizConfig as any)?.apiKey ?? ''}:${(n.data?.bizConfig as any)?.outputPrefix ?? ''}`)
    .sort()
    .join('|') ?? ''

  useEffect(() => {
    if (!nodeData?.nodeType) { setSchema(null); return }
    setLoading(true)

    // 动态上下文字段：从 nodes 列表提取 EVENT_TRIGGER / API_CALL 节点信息，按画布实际内容推导
    if (nodes && nodes.length > 0) {
      const eventCodes: string[] = []
      const apiKeys: string[] = []
      const outputPrefixes: string[] = []

      for (const n of nodes) {
        const cfg = n.data?.bizConfig ?? {}
        if (n.data?.nodeType === 'EVENT_TRIGGER' && cfg.eventCode) {
          eventCodes.push(cfg.eventCode as string)
        }
        if (n.data?.nodeType === 'API_CALL' && cfg.apiKey) {
          apiKeys.push(cfg.apiKey as string)
          outputPrefixes.push((cfg.outputPrefix as string) ?? '')
        }
      }

      metaApi.getCanvasContextFields({ eventCodes, apiKeys, outputPrefixes })
        .then(res => setCtxFields(res.data))
    } else if (!contextFieldsCache) {
      // 无 nodes 信息时退回静态表（兜底）
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
  }, [nodeData?.nodeType, ctxSignature])

  // 加载 select 下拉选项：dataSource 优先，其次 optionCategory，最后保留 field.options 兜底
  useEffect(() => {
    if (!schema) return
    let cancelled = false
    const fields = parseSchema(schema.configSchema)
    fields.filter(f => f.type === 'select' && f.dataSource).forEach(f => {
      const src = resolveDataSourceTemplate(f.dataSource!, formValues)
      if (!src) {
        // 依赖字段还没填完时清空该下拉，避免展示上一组依赖下的旧选项。
        setOptions(prev => ({ ...prev, [f.key]: [] }))
        return
      }
      if (rawCache.has(src)) {
        setOptions(prev => ({ ...prev, [f.key]: toSelectOptions(rawCache.get(src)!) }))
        return
      }
      loadDataSource(src).then(data => {
        if (cancelled) return
        setOptions(prev => ({ ...prev, [f.key]: toSelectOptions(data) }))
      })
    })
    fields.filter(f => (f.type === 'select' || f.type === 'radio') && f.optionCategory && !f.dataSource)
      .forEach(f => {
        const category = f.optionCategory!
        if (systemOptionCache.has(category)) {
          setOptions(prev => ({ ...prev, [f.key]: systemOptionCache.get(category)! }))
          return
        }
        loadSystemOptionCategory(category).then(data =>
          setOptions(prev => ({ ...prev, [f.key]: data }))
        )
      })
    return () => { cancelled = true }
  }, [formValues, schema])

  // 同步表单初始值
  useEffect(() => {
    if (nodeData) {
      const vals = { name: nodeData.name, ...nodeData.bizConfig }
      form.setFieldsValue(vals)
      setFormValues(vals as Record<string, unknown>)
    }
  }, [nodeId, nodeData, form])

  /** 表单变化后把 name 和 bizConfig 拆开回传给画布编辑器。 */
  const handleValuesChange = useCallback((changed: Record<string, unknown>, all: Record<string, unknown>) => {
    if (!nodeId || !nodeData) return
    const schemaFields = parseSchema(schema?.configSchema)
    const changedKeys = new Set(Object.keys(changed))
    const clearedFields = schemaFields
      .filter((field) =>
        !!field.dataSource &&
        getDataSourceDependencies(field.dataSource).some((dependency) => changedKeys.has(dependency)),
      )
      .map((field) => field.key)
      .filter((fieldKey) => !changedKeys.has(fieldKey) && !isBlankDependencyValue(all[fieldKey]))

    const nextValues = { ...all } as Record<string, unknown>
    if (clearedFields.length > 0) {
      const clearedPatch = Object.fromEntries(clearedFields.map((fieldKey) => [fieldKey, undefined]))
      Object.assign(nextValues, clearedPatch)
      form.setFieldsValue(clearedPatch)
    }

    setFormValues(nextValues)
    // 用 all（全量）而非 changed（增量）：nested Form.Item（如 inputParams.xxx）每次只在 changed
    // 里携带单个子字段，用 changed 会导致其他子字段被丢弃；all 是表单当前完整状态
    const { name, ...rest } = nextValues
    onChange(nodeId, {
      ...(name !== undefined ? { name: name as string } : {}),
      bizConfig: rest,
    })
  }, [form, nodeData, nodeId, onChange, schema])

  /** 子控件需要一次性写多个字段时使用，比如延迟预设同时写 duration 和 unit。 */
  const applyFormPatch = useCallback((patch: Record<string, unknown>) => {
    if (!nodeId || !nodeData) return
    const all = { ...form.getFieldsValue(true), ...patch }
    setFormValues(all)
    const { name, ...rest } = all
    onChange(nodeId, {
      ...(name !== undefined ? { name: name as string } : {}),
      bizConfig: rest,
    })
  }, [form, nodeData, nodeId, onChange])

  if (!nodeId || !nodeData) {
    return (
      <div style={{ padding: 16, color: '#bbb', fontSize: 12 }}>
        点击画布中的节点查看配置
      </div>
    )
  }

  const fields = parseSchema(schema?.configSchema)
  // visible/showWhen 都由 schema 控制，前端只实现轻量表达式判断。
  const visibleFields = fields.filter((f) =>
    evaluateVisible(f.visible, formValues) &&
    evaluateVisible(f.showWhen, formValues)
  )
  const displayValues = Object.fromEntries(
    visibleFields.map((field) => [
      field.key,
      resolveDisplayValue(field, formValues[field.key], options),
    ]),
  )
  // Header/摘要/分支卡片先计算成 presentation model，再交给 InspectorCards 渲染。
  const presentation = buildConfigPanelPresentation({
    nodeData,
    formValues,
    displayValues,
    fields: visibleFields.map(({ key, label, type }) => ({ key, label, type })),
    getNodeName,
  })
  const visibleFieldByKey = new Map(visibleFields.map(field => [field.key, field]))
  const groupedSections = presentation.fieldGroups.some(group => group.key === 'basic')
    ? presentation.fieldGroups
    : [{ key: 'basic' as const, title: '基础配置', fields: [] }, ...presentation.fieldGroups]

  /** 根据 schema 字段渲染具体控件，分组只改变布局，不改变字段名和回写路径。 */
  const renderConfigField = (field: SchemaField) => {
    // api-input-params / event-attr-preview 自己管理渲染，不能被外层 Form.Item 包住
    if (field.type === 'api-input-params') {
      return (
        <div key={field.key} style={{ marginBottom: 10 }}>
          <ApiCallInputParams
            label={field.label}
            apiKeyField={field.apiKeyField ?? 'apiKey'}
            defsSource={field.defsSource ?? '/meta/api-definitions'}
          />
        </div>
      )
    }
    if (field.type === 'event-attr-preview') {
      return (
        <div key={field.key} style={{ marginBottom: 10 }}>
          {renderControlLabel(field.label)}
          <EventAttrPreview />
        </div>
      )
    }
    if (field.type === 'delay-input') {
      return (
        <div key={field.key} style={{ marginBottom: 10 }}>
          {renderControlLabel(field.label)}
          <DelayInput
            valueFieldKey={field.key}
            unitFieldKey="unit"
            unitOptions={sharedOptions.delayUnits}
            applyFormPatch={applyFormPatch}
          />
        </div>
      )
    }
    return (
      <Form.Item key={field.key} name={field.key} label={renderControlLabel(field.label)}
        rules={field.required ? [{ required: true, message: `请填写${field.label}` }] : []}>
        {renderControl(field, options, ctxFields, form, sharedOptions, applyFormPatch, nodeId, getNodeName, nodeData)}
      </Form.Item>
    )
  }

  const accentColor = CATEGORY_SOLID[nodeData.category] ?? '#475569'

  return (
    <div style={{ padding: 12, overflowY: 'auto', height: '100%', background: `${accentColor}0d` }}>
      <style>{CONTROL_CHROME_SELECTOR_CSS}</style>
      {loading && <Spin size="small" style={{ display: 'block', marginBottom: 8 }} />}

      <NodeHeaderCard
        {...presentation.header}
        categoryColor={accentColor}
      />

      <Form
        className="config-panel-form"
        form={form}
        layout="vertical"
        onValuesChange={handleValuesChange}
        disabled={readonly}
      >
        {groupedSections.map(group => (
          <ConfigSectionCard key={group.key} title={group.title} summary={group.summary} accentColor={accentColor}>
            {group.key === 'basic' && (
              <Form.Item name="name" label={renderControlLabel('节点名称')} rules={[{ required: true }]}>
                <Input className="config-panel-ios-input" style={controlChrome} />
              </Form.Item>
            )}
            {group.fields.map((groupField) => {
              const field = visibleFieldByKey.get(groupField.key)
              return field ? renderConfigField(field) : null
            })}
          </ConfigSectionCard>
        ))}

        {presentation.branchRoutes.length > 0 && (
          <ConfigSectionCard title="出口路由" accentColor={accentColor}>
            <div style={{ display: 'grid', gap: 7 }}>
              {presentation.branchRoutes.map((route) => (
                <BranchRouteCard key={route.label} {...route} />
              ))}
            </div>
          </ConfigSectionCard>
        )}
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
/** 根据 schema field.type 渲染对应控件；新增控件类型优先在这里扩展。 */
function renderControl(
  field: SchemaField,
  options: Record<string, StubOption[]>,
  ctxFields: ContextField[],
  _form: ReturnType<typeof Form.useForm>[0],
  sharedOptions: SharedConfigOptions,
  applyFormPatch: (patch: Record<string, unknown>) => void,
  nodeId?: string | null,
  getNodeName?: (id: string | undefined) => string | null,
  nodeData?: CanvasNodeData | null,
): React.ReactNode {
  const controlChrome = getControlChrome()

  switch (field.type) {
    case 'select':
      return (
        <Select
          className="config-panel-ios-select"
          classNames={CONTROL_SELECT_CLASS_NAMES}
          style={{ height: controlChrome.height, width: '100%' }}
          options={normalizeFieldOptions(field, options)}
          placeholder={`请选择${field.label}`}
          showSearch filterOption={(v, opt) =>
            String(opt?.label ?? '').toLowerCase().includes(v.toLowerCase())}
        />
      )
    case 'number':
      return <InputNumber className="config-panel-ios-input-number" style={{ ...controlChrome, width: '100%' }} defaultValue={field.defaultValue as number} />
    case 'toggle':
      return <Switch />
    case 'radio':
      return (
        <Select options={normalizeFieldOptions(field, options)}
          className="config-panel-ios-select"
          classNames={CONTROL_SELECT_CLASS_NAMES}
          style={{ height: controlChrome.height, width: '100%' }}
          placeholder={`请选择${field.label}`} />
      )
    case 'code-editor':
      return (
        <Input.TextArea rows={8}
          className="config-panel-ios-textarea"
          style={{ fontFamily: 'SFMono-Regular, Consolas, monospace', fontSize: 12 }}
          placeholder={`// Groovy 脚本`} />
      )
    case 'datetime':
      return (
        <DatePicker
          showTime
          format="YYYY-MM-DD HH:mm:ss"
          style={{ ...controlChrome, width: '100%' }}
          placeholder={`选择${field.label}`}
        />
      )
    case 'cron':
      return <CronBuilder frequencyOptions={sharedOptions.cronFrequencies} weekdayOptions={sharedOptions.weekdays} />
    case 'event-attr-preview':
      return <EventAttrPreview />
    case 'edge-hint': {
      const connectedId = nodeData?.bizConfig?.[field.key] as string | undefined
      const name = getNodeName?.(connectedId) ?? null
      return (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 14 }}>{field.icon === 'check' ? '✓' : '✕'}</span>
          {name
            ? <Tag color="blue">→ {name}</Tag>
            : <Tag color="warning">⚠ 未连线</Tag>
          }
        </div>
      )
    }
    case 'condition-rule-list':
      return <ConditionRuleList
        ctxFields={ctxFields}
        operatorOptions={sharedOptions.conditionOps}
        fieldKey={getConditionRuleListFieldKey(field.key)}
      />
    case 'context-value-list':
      return <ContextValueList ctxFields={ctxFields} valueTypeOptions={sharedOptions.contextValueTypes} />
    case 'param-define-list':
      return <ParamDefineList paramTypeOptions={sharedOptions.paramTypes} />
    case 'branch-list':
      return <BranchList
        ctxFields={ctxFields}
        operatorOptions={sharedOptions.conditionOps}
        relationOptions={sharedOptions.logicRelations}
      />
    case 'ab-group-list':
      return (
        <AbGroupList
          nodeId={nodeId}
          getNodeName={getNodeName ?? (() => null)}
          onGroupsChange={groups => applyFormPatch({ groups })}
        />
      )
    case 'priority-list':
      return <PriorityList />
    case 'key-value':
      return <KeyValueMapping fieldKey={field.key} ctxFields={ctxFields} />
    case 'canvas-select':
      return <CanvasSelector />
    case 'node-select':
      return <Input className="config-panel-ios-input" style={controlChrome} placeholder="节点 ID（连线后自动填入）" />
    default:
      return <Input className="config-panel-ios-input" style={controlChrome} placeholder={field.label} />
  }
}

// ── 条件规则列表控件（IF判断 / SELECTOR / MQ_TRIGGER 等）─────────
interface SharedConfigOptions {
  conditionOps: { label: string; value: string }[]
  logicRelations: { label: string; value: string }[]
  contextValueTypes: { label: string; value: string }[]
  paramTypes: { label: string; value: string }[]
  delayUnits: { label: string; value: string }[]
  cronFrequencies: { label: string; value: string }[]
  weekdays: { label: string; value: number }[]
}

/** 条件规则配置项，表示单条字段比较表达式。 */
interface ConditionRule {
  field: string; operator: string; value: string; isCustom: boolean
}

/** 条件规则列表字段名兜底，兼容旧 schema 未显式指定 key 的情况。 */
export function getConditionRuleListFieldKey(schemaFieldKey?: string) {
  return schemaFieldKey && schemaFieldKey.trim() ? schemaFieldKey : 'rules'
}

/** 条件规则列表控件，用于 IF、SELECTOR、MQ_TRIGGER 等节点。 */
function ConditionRuleList({ ctxFields, operatorOptions, fieldKey }: {
  ctxFields: ContextField[]
  operatorOptions: { label: string; value: string }[]
  fieldKey: string
}) {
  const form = Form.useFormInstance()
  const rules: ConditionRule[] = Form.useWatch(fieldKey, form) ?? []

  // 每条规则由字段、操作符、比较值组成，写回同一个数组字段。
  const add = () => form.setFieldValue(fieldKey,
    [...rules, { field: '', operator: 'EQ', value: '', isCustom: true }])
  /** 删除指定下标的条件规则。 */
  const remove = (i: number) => {
    const next = [...rules]; next.splice(i, 1); form.setFieldValue(fieldKey, next)
  }
  /** 更新指定规则的单个字段，避免重置整组条件。 */
  const update = (i: number, k: keyof ConditionRule, v: string | boolean) => {
    const next = [...rules]; (next[i] as any)[k] = v; form.setFieldValue(fieldKey, next)
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      {rules.map((rule, i) => (
        <div key={i} style={{
          background: '#f0f7ff', border: '1px solid #dbeafe',
          borderRadius: 8, padding: '8px 10px',
        }}>
          {/* 顶栏：编号 + 删除 */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
            <span style={{ fontSize: 11, fontWeight: 700, color: '#1677ff' }}>#{i + 1}</span>
            <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(i)}
              style={{ width: 24, height: 24, minWidth: 24, padding: 0, borderRadius: 6 }} />
          </div>
          {/* 行1：字段（全宽） */}
          <Select className="config-panel-ios-select" classNames={CONTROL_SELECT_CLASS_NAMES}
            size="small" style={{ width: '100%', marginBottom: 6 }} placeholder="字段"
            value={rule.field || undefined}
            options={ctxFields.map(f => ({ label: f.fieldName, value: f.fieldKey }))}
            onChange={v => update(i, 'field', v)} showSearch
            dropdownStyle={{ minWidth: 200 }} />
          {/* 行2：操作符 + 值 */}
          <div style={{ display: 'flex', gap: 6 }}>
            <Select className="config-panel-ios-select" classNames={CONTROL_SELECT_CLASS_NAMES}
              size="small" style={{ width: 90 }} value={rule.operator}
              options={operatorOptions}
              onChange={v => update(i, 'operator', v)} />
            <AutoComplete className="config-panel-ios-auto-complete" classNames={CONTROL_SELECT_CLASS_NAMES}
              size="small" style={{ flex: 1, minWidth: 0 }}
              placeholder="值或 ${key}"
              value={rule.value}
              options={ctxFields.map(f => ({ value: '${' + f.fieldKey + '}', label: f.fieldName }))}
              onChange={v => update(i, 'value', v)}
              filterOption={(input, opt) => String(opt?.label ?? '').toLowerCase().includes(input.toLowerCase())}
            />
          </div>
        </div>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add}
        style={{ width: '100%', borderColor: '#93c5fd', color: '#1677ff' }}>
        添加条件
      </Button>
    </div>
  )
}

// ── 上下文引用值列表控件（IN_APP_NOTIFY / GROOVY inputParams 等）─
interface ContextValueItem { name: string; valueType: 'CUSTOM' | 'CONTEXT'; value: string }

/** 上下文引用值列表，支持固定值和 ${contextKey} 两种来源。 */
function ContextValueList({ ctxFields, valueTypeOptions }: {
  ctxFields: ContextField[]
  valueTypeOptions: { label: string; value: string }[]
}) {
  const form = Form.useFormInstance()
  const fieldKey = 'bizData'
  const items: ContextValueItem[] = Form.useWatch(fieldKey, form) ?? []
  const inlineChrome = getInlineControlChrome()

  /** 新增一个固定值类型的上下文字段映射。 */
  const add = () => form.setFieldValue(fieldKey,
    [...items, { name: '', valueType: 'CUSTOM', value: '' }])
  /** 删除指定下标的上下文字段映射。 */
  const remove = (i: number) => {
    const next = [...items]; next.splice(i, 1); form.setFieldValue(fieldKey, next)
  }
  /** 更新上下文字段映射的名称、取值类型或取值。 */
  const update = (i: number, k: keyof ContextValueItem, v: string) => {
    const next = [...items]; (next[i] as any)[k] = v; form.setFieldValue(fieldKey, next)
  }

  return (
    <div>
      {items.map((item, i) => (
        <div key={i} style={{ display: 'grid', gridTemplateColumns: '68px 72px minmax(0, 1fr) 28px', gap: 6, marginBottom: 6, alignItems: 'center' }}>
          <Input size="small" style={{ ...inlineChrome, width: '100%' }} placeholder="字段名"
            value={item.name} onChange={e => update(i, 'name', e.target.value)} />
          <Select className="config-panel-ios-select" classNames={CONTROL_SELECT_CLASS_NAMES} size="small" style={{ width: '100%' }} value={item.valueType}
            options={valueTypeOptions}
            onChange={v => update(i, 'valueType', v)} />
          {item.valueType === 'CONTEXT'
            ? <AutoComplete className="config-panel-ios-auto-complete" classNames={CONTROL_SELECT_CLASS_NAMES} size="small" style={{ width: '100%' }}
                placeholder="${key} 或字段名"
                value={item.value || undefined}
                options={ctxFields.map(f => ({ value: '${' + f.fieldKey + '}', label: f.fieldName }))}
                onChange={v => update(i, 'value', v)}
                filterOption={(input, opt) => String(opt?.label ?? '').toLowerCase().includes(input.toLowerCase())} />
            : <Input size="small" style={{ ...inlineChrome, width: '100%' }} placeholder="值"
                value={item.value} onChange={e => update(i, 'value', e.target.value)} />
          }
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(i)} />
        </div>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add} style={{ width: '100%' }}>添加</Button>
    </div>
  )
}

// ── 参数定义列表控件（DIRECT_CALL inputParams / GROOVY outputParams）
interface ParamDef { name: string; description?: string; dataType: string; required?: boolean }

/** 参数定义列表控件，用于直调和脚本类节点声明输入/输出参数。 */
function ParamDefineList({ paramTypeOptions }: { paramTypeOptions: { label: string; value: string }[] }) {
  const form = Form.useFormInstance()
  const fieldKey = 'inputParams'
  const items: ParamDef[] = Form.useWatch(fieldKey, form) ?? []
  const inlineChrome = getInlineControlChrome()

  /** 追加一个默认字符串参数定义。 */
  const add = () => form.setFieldValue(fieldKey,
    [...items, { name: '', dataType: 'STRING', required: false }])
  /** 删除指定参数定义。 */
  const remove = (i: number) => {
    const next = [...items]; next.splice(i, 1); form.setFieldValue(fieldKey, next)
  }
  /** 更新参数名、类型或必填状态。 */
  const update = (i: number, k: keyof ParamDef, v: string | boolean) => {
    const next = [...items]; (next[i] as any)[k] = v; form.setFieldValue(fieldKey, next)
  }

  return (
    <div>
      {items.map((p, i) => (
        <div key={i} style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 72px 46px 28px', gap: 6, marginBottom: 6, alignItems: 'center' }}>
          <Input size="small" style={{ ...inlineChrome, width: '100%' }} placeholder="参数名"
            value={p.name} onChange={e => update(i, 'name', e.target.value)} />
          <Select className="config-panel-ios-select" classNames={CONTROL_SELECT_CLASS_NAMES} size="small" style={{ width: '100%' }} value={p.dataType}
            options={paramTypeOptions}
            onChange={v => update(i, 'dataType', v)} />
          <Switch size="small" checked={!!p.required} checkedChildren="必" unCheckedChildren="选"
            onChange={v => update(i, 'required', v)} />
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(i)} />
        </div>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add} style={{ width: '100%' }}>添加参数</Button>
    </div>
  )
}

// ── SELECTOR 分支列表控件（branch-list）────────────────────────────
interface BranchItem { label: string; strategyRelation: string; conditions: ConditionRule[]; nextNodeId?: string }

/** SELECTOR 分支配置控件；实际后继节点由画布连线写回 nextNodeId。 */
function BranchList({ ctxFields, operatorOptions, relationOptions }: {
  ctxFields: ContextField[]
  operatorOptions: { label: string; value: string }[]
  relationOptions: { label: string; value: string }[]
}) {
  const form = Form.useFormInstance()
  const branches: BranchItem[] = Form.useWatch('branches', form) ?? []

  const LABELS = ['如果', '否则如果', '否则如果', '否则如果', '否则如果']
  /** 追加一个 SELECTOR 分支，默认使用 AND 关系。 */
  const addBranch = () => form.setFieldValue('branches', [
    ...branches,
    { label: LABELS[Math.min(branches.length, LABELS.length - 1)], strategyRelation: 'AND', conditions: [], nextNodeId: undefined }
  ])
  /** 删除指定分支，同时保留其他分支顺序。 */
  const removeBranch = (i: number) => {
    const next = [...branches]; next.splice(i, 1); form.setFieldValue('branches', next)
  }
  /** 更新指定分支的标题、关系或后继节点字段。 */
  const updateBranch = (i: number, k: keyof BranchItem, v: unknown) => {
    const next = [...branches]; (next[i] as any)[k] = v; form.setFieldValue('branches', next)
  }
  /** 在指定分支下追加一条条件。 */
  const addCondition = (branchIdx: number) => {
    const next = [...branches]
    next[branchIdx] = { ...next[branchIdx], conditions: [...(next[branchIdx].conditions ?? []), { field: '', operator: 'EQ', value: '', isCustom: true }] }
    form.setFieldValue('branches', next)
  }
  /** 删除指定分支下的指定条件。 */
  const removeCondition = (bi: number, ci: number) => {
    const next = [...branches]; next[bi].conditions.splice(ci, 1); form.setFieldValue('branches', next)
  }
  /** 更新指定分支条件中的字段、操作符或比较值。 */
  const updateCondition = (bi: number, ci: number, k: string, v: string) => {
    const next = [...branches]; (next[bi].conditions[ci] as any)[k] = v; form.setFieldValue('branches', next)
  }

  const LABEL_COLORS: Record<string, string> = { '如果': '#1677ff', '否则如果': '#fa8c16', '否则': '#8c8c8c' }

  return (
    <div>
      {branches.map((b, i) => (
        <div key={i} style={{ marginBottom: 8, border: '1px solid #e8e8e8', borderRadius: 6, overflow: 'hidden' }}>
          {/* 分支标题行 */}
          <div style={{ background: '#fafafa', padding: '6px 10px', display: 'flex', alignItems: 'center', gap: 8, borderBottom: b.conditions?.length ? '1px solid #f0f0f0' : 'none' }}>
            <Tag color={LABEL_COLORS[b.label] ?? 'default'} style={{ margin: 0 }}>{b.label}</Tag>
            <Select className="config-panel-ios-select" classNames={CONTROL_SELECT_CLASS_NAMES} size="small" style={{ width: 92 }} value={b.strategyRelation}
              options={relationOptions}
              onChange={v => updateBranch(i, 'strategyRelation', v)} />
            <span style={{ flex: 1, fontSize: 11, color: '#999' }}>
              {b.conditions?.length
                ? `${b.conditions.length} 个条件`
                : <span style={{ color: '#f5222d' }}>无条件 = 必走此分支</span>}
            </span>
            <Button size="small" type="text" danger icon={<DeleteOutlined />} onClick={() => removeBranch(i)} />
          </div>
          {/* 条件列表 */}
          <div style={{ padding: '6px 10px' }}>
            {b.conditions?.map((c, ci) => (
              <div key={ci} style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 56px minmax(0, 1fr) 28px', gap: 6, marginBottom: 6, alignItems: 'center' }}>
                <Select className="config-panel-ios-select" classNames={CONTROL_SELECT_CLASS_NAMES} size="small" style={{ width: '100%' }} placeholder="字段"
                  value={c.field || undefined}
                  options={ctxFields.map(f => ({ label: f.fieldName, value: f.fieldKey }))}
                  onChange={v => updateCondition(i, ci, 'field', v)} showSearch
                  dropdownStyle={{ minWidth: 200 }} />
                <Select className="config-panel-ios-select" classNames={CONTROL_SELECT_CLASS_NAMES} size="small" style={{ width: '100%' }} value={c.operator}
                  options={operatorOptions}
                  onChange={v => updateCondition(i, ci, 'operator', v)} />
                <AutoComplete className="config-panel-ios-auto-complete" classNames={CONTROL_SELECT_CLASS_NAMES} size="small" style={{ width: '100%' }}
                  placeholder="值或 ${key}"
                  value={c.value}
                  options={ctxFields.map(f => ({ value: '${' + f.fieldKey + '}', label: f.fieldName }))}
                  onChange={v => updateCondition(i, ci, 'value', v)}
                  filterOption={(input, opt) => String(opt?.label ?? '').toLowerCase().includes(input.toLowerCase())} />
                <Button size="small" danger icon={<DeleteOutlined />} onClick={() => removeCondition(i, ci)} />
              </div>
            ))}
            <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={() => addCondition(i)}>
              添加条件
            </Button>
          </div>
        </div>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={addBranch}
        style={{ width: '100%' }}>
        {branches.length === 0 ? '添加第一个分支（如果）' : '添加分支（否则如果）'}
      </Button>
      {branches.length > 0 && (
        <div style={{ fontSize: 11, color: '#8c8c8c', marginTop: 4 }}>
          最后一个分支若无条件，则作为「否则」兜底；每个分支的后继节点通过画布连线自动设置
        </div>
      )}
    </div>
  )
}

// ── AB 分流分组路由控件（ab-group-list）────────────────────────────
interface AbGroup { groupKey: string; label?: string; nextNodeId?: string }

/** AB 分流分组展示控件，分组来自实验配置，后继节点来自画布连线。 */
function AbGroupList({ nodeId, getNodeName, onGroupsChange }: {
  nodeId?: string | null
  getNodeName: (id: string | undefined) => string | null
  onGroupsChange: (groups: AbGroup[]) => void
}) {
  const form = Form.useFormInstance()
  const groups: AbGroup[] = Form.useWatch('groups', form) ?? []
  const experimentKey = Form.useWatch('experimentKey', form)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const previousExperimentKeyRef = useRef<string>()

  // 切换节点时重置实验 key 记忆，避免把上一个节点的禁用分组带到当前节点。
  useEffect(() => {
    previousExperimentKeyRef.current = undefined
  }, [nodeId])

  useEffect(() => {
    if (!experimentKey) return
    const currentExperimentKey = String(experimentKey)
    const previousExperimentKey = previousExperimentKeyRef.current
    const experimentChanged = previousExperimentKey !== undefined && previousExperimentKey !== currentExperimentKey
    setLoading(true)
    setLoadError(false)
    metaApi.getAbExperimentGroups(currentExperimentKey)
      .then(res => {
        const existing: AbGroup[] = form.getFieldValue('groups') ?? []
        const existingByKey = new Map(existing.map(group => [group.groupKey, group]))
        const loaded = res.data.map(option => ({
          groupKey: option.key,
          label: option.label,
          nextNodeId: existingByKey.get(option.key)?.nextNodeId,
        }))
        const loadedKeys = new Set(loaded.map(group => group.groupKey))
        // 同一实验下保留历史禁用分组，切换实验时丢弃旧分组防止误连线。
        const disabledHistory = experimentChanged
          ? []
          : existing
              .filter(group => group.groupKey && !loadedKeys.has(group.groupKey))
              .map(group => ({
                ...group,
                label: group.label ?? `已禁用：${group.groupKey}`,
              }))
        const next = [...loaded, ...disabledHistory]
        previousExperimentKeyRef.current = currentExperimentKey
        if (JSON.stringify(existing) !== JSON.stringify(next)) {
          form.setFieldValue('groups', next)
          onGroupsChange(next)
        }
      })
      .catch(() => setLoadError(true))
      .finally(() => setLoading(false))
  }, [experimentKey, form, onGroupsChange])

  const bucketSize = groups.length > 0 ? Math.floor(100 / groups.length) : 0

  if (!experimentKey) {
    return <Text type="secondary" style={{ fontSize: 12 }}>请先选择实验</Text>
  }
  if (loading) {
    return <Spin size="small" />
  }
  if (loadError) {
    return <Text type="danger" style={{ fontSize: 12 }}>分组加载失败</Text>
  }
  if (!groups.length) {
    return <Text type="secondary" style={{ fontSize: 12 }}>该实验未配置启用分组</Text>
  }

  return (
    <div>
      {groups.map((g, i) => {
        const start = i * bucketSize
        const end   = i === groups.length - 1 ? 100 : start + bucketSize
        const successorName = getNodeName(g.nextNodeId)
        return (
          <div key={i} style={{ marginBottom: 8, padding: '6px 8px', background: '#fafafa', borderRadius: 6, border: '1px solid #f0f0f0' }}>
            <Space style={{ width: '100%', justifyContent: 'space-between' }}>
              <Space size={4}>
                <Tag color={g.label?.startsWith('已禁用') ? 'default' : 'blue'} style={{ fontSize: 11 }}>
                  {g.label ?? g.groupKey}
                </Tag>
                <Text code style={{ fontSize: 11 }}>{g.groupKey}</Text>
                <Text style={{ fontSize: 11, color: '#8c8c8c' }}>bucket {start}–{end}%</Text>
              </Space>
            </Space>
            <Space style={{ marginTop: 4 }} size={4}>
              {successorName
                ? <Tag color="blue" style={{ fontSize: 11 }}>→ {successorName}</Tag>
                : <Tag color="warning" style={{ fontSize: 11 }}>⚠ 未连线</Tag>
              }
            </Space>
          </div>
        )
      })}
      <div style={{ fontSize: 11, color: '#8c8c8c', marginTop: 6 }}>
        分组来自 AB 实验管理；连线时边上会显示分组 Key
      </div>
    </div>
  )
}

// ── 优先级列表控件（priority-list）────────────────────────────────
interface PriorityItem { order: number; nextNodeId?: string }

/** 优先级路由列表，按 order 从小到大尝试。 */
function PriorityList() {
  const form = Form.useFormInstance()
  const priorities: PriorityItem[] = Form.useWatch('priorities', form) ?? []
  const inlineChrome = getInlineControlChrome()
  /** 追加一个优先级路由项，默认排在当前列表末尾。 */
  const add = () => form.setFieldValue('priorities', [...priorities, { order: priorities.length + 1, nextNodeId: undefined }])
  /** 删除指定优先级路由项。 */
  const remove = (i: number) => { const n = [...priorities]; n.splice(i, 1); form.setFieldValue('priorities', n) }
  /** 更新优先级顺序或后继节点 ID。 */
  const update = (i: number, k: keyof PriorityItem, v: string | number) => {
    const n = [...priorities]; (n[i] as any)[k] = v; form.setFieldValue('priorities', n)
  }

  return (
    <div>
      {priorities.map((p, i) => (
        <div key={i} style={{ display: 'grid', gridTemplateColumns: '60px minmax(0, 1fr) 28px', gap: 6, marginBottom: 6 }}>
          <InputNumber size="small" style={{ ...inlineChrome, width: '100%' }} placeholder="优先级"
            value={p.order} onChange={v => update(i, 'order', v ?? i + 1)} min={1} />
          <Input size="small" style={{ ...inlineChrome, width: '100%' }} placeholder="后继节点ID"
            value={p.nextNodeId ?? ''} onChange={e => update(i, 'nextNodeId', e.target.value)} />
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(i)} />
        </div>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add} style={{ width: '100%' }}>添加优先级</Button>
      <div style={{ fontSize: 11, color: '#999', marginTop: 4 }}>
        按 order 从小到大依次尝试，第一个成功则停止
      </div>
    </div>
  )
}

// ── 键值映射控件（CANVAS_TRIGGER paramMapping / SUB_FLOW_REF inputMapping）
/** 子流程/画布调用参数映射控件，左侧是目标字段，右侧是上下文或固定值。 */
function KeyValueMapping({ fieldKey, ctxFields }: { fieldKey: string; ctxFields: ContextField[] }) {
  const form = Form.useFormInstance()
  const mapping: Record<string, string> = Form.useWatch(fieldKey, form) ?? {}
  const entries = Object.entries(mapping)
  const inlineChrome = getInlineControlChrome()

  /** 新增一个空键值映射行。 */
  const add = () => form.setFieldValue(fieldKey, { ...mapping, '': '' })
  /** 删除指定目标字段的映射。 */
  const remove = (k: string) => {
    const n = { ...mapping }; delete n[k]; form.setFieldValue(fieldKey, n)
  }
  /** 修改目标字段名时保留原值并重建映射对象。 */
  const updateKey = (oldKey: string, newKey: string) => {
    const n: Record<string, string> = {}
    Object.entries(mapping).forEach(([k, v]) => n[k === oldKey ? newKey : k] = v)
    form.setFieldValue(fieldKey, n)
  }
  /** 修改目标字段对应的来源值。 */
  const updateVal = (k: string, v: string) => form.setFieldValue(fieldKey, { ...mapping, [k]: v })

  return (
    <div>
      {entries.map(([k, v], i) => (
        <div key={i} style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 14px minmax(0, 1fr) 28px', gap: 6, marginBottom: 6, alignItems: 'center' }}>
          <Input size="small" style={{ ...inlineChrome, width: '100%' }} placeholder="子流程字段名"
            value={k} onChange={e => updateKey(k, e.target.value)} />
          <span style={{ fontSize: 12 }}>←</span>
          <AutoComplete className="config-panel-ios-auto-complete" classNames={CONTROL_SELECT_CLASS_NAMES} size="small" style={{ width: '100%' }}
            placeholder="${key} 或固定值"
            value={v || undefined}
            options={ctxFields.map(f => ({ value: '${' + f.fieldKey + '}', label: f.fieldName }))}
            onChange={nv => updateVal(k, nv)}
            filterOption={(input, opt) => String(opt?.label ?? '').toLowerCase().includes(input.toLowerCase())} />
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(k)} />
        </div>
      ))}
      <Button size="small" type="dashed" icon={<PlusOutlined />} onClick={add} style={{ width: '100%' }}>添加映射</Button>
    </div>
  )
}

// ── 画布选择控件（CANVAS_TRIGGER / SUB_FLOW_REF）─────────────────
let canvasListCache: Canvas[] | null = null

/** 已发布画布选择器，用于配置调用其他画布的节点。 */
function CanvasSelector() {
  const [canvases, setCanvases] = useState<Canvas[]>(canvasListCache ?? [])
  const [loading, setLoading] = useState(!canvasListCache)
  const controlChrome = getControlChrome()

  useEffect(() => {
    if (canvasListCache) return
    setLoading(true)
    canvasApi.list({ status: 1, size: 100 })
      .then(r => { canvasListCache = r.data.list; setCanvases(r.data.list) })
      .finally(() => setLoading(false))
  }, [])

  return (
    <Select
      className="config-panel-ios-select"
      classNames={CONTROL_SELECT_CLASS_NAMES}
      style={{ height: controlChrome.height, width: '100%' }}
      loading={loading}
      showSearch
      placeholder="搜索并选择已发布画布"
      filterOption={(input, opt) =>
        String(opt?.label ?? '').toLowerCase().includes(input.toLowerCase())}
      options={canvases.map(c => ({ label: `${c.name} (ID:${c.id})`, value: c.id }))}
    />
  )
}

/** 后端 configSchema 中单个表单字段的前端可用子集。 */
interface SchemaField {
  key: string; label: string; type: string
  required?: boolean; options?: any[]; dataSource?: string; optionCategory?: string
  visible?: string; showWhen?: string; defaultValue?: unknown
  hint?: string; icon?: string          // edge-hint 使用
  apiKeyField?: string                   // api-input-params 使用，默认 apiKey
  defsSource?: string                    // api-input-params 使用，默认 /meta/api-definitions
}

/** 安全解析后端 configSchema，解析失败时按空 schema 处理。 */
function parseSchema(raw: string | undefined): SchemaField[] {
  try { return raw ? JSON.parse(raw) : [] } catch { return [] }
}

// ── 事件属性只读预览控件 ──────────────────────────────────────────
// 根据选中的 eventCode，显示该事件定义的属性列表（只读，运行时由上报内容决定）
function EventAttrPreview() {
  const form   = Form.useFormInstance()
  const evCode = Form.useWatch('eventCode', form)
  const [attrs, setAttrs] = useState<ApiParamDef[]>([])
  const { options: eventAttrTypes } = useSystemOptions('event_attr_type')

  useEffect(() => {
    if (!evCode) { setAttrs([]); return }
    const src = '/meta/event-definitions'
    /** 从事件定义列表中选中当前事件并解析属性 schema。 */
    const pick = (list: any[]) => {
      const def = list.find(d => d.value === evCode)
      try { setAttrs(def ? JSON.parse(def.requestSchema || '[]') : []) } catch { setAttrs([]) }
    }
    // 复用全局 dataSource 缓存，避免事件选择后重复加载事件定义列表。
    if (rawCache.has(src)) { pick(rawCache.get(src)!); return }
    loadDataSource(src).then(pick)
  }, [evCode])

  if (!evCode) return <Text type="secondary" style={{ fontSize: 12 }}>请先选择触发事件</Text>
  if (!attrs.length) return <Text type="secondary" style={{ fontSize: 12 }}>该事件未定义属性</Text>

  /** 把事件属性类型编码转换成系统字典展示名。 */
  const typeLabel = (type: string) => eventAttrTypes.find(option => option.value === type)?.label ?? type

  return (
    <div style={{ background: '#f0f7ff', border: '1px solid #bae0ff', borderRadius: 6, padding: '8px 12px' }}>
      <div style={{ fontSize: 11, color: '#0958d9', marginBottom: 6, fontWeight: 500 }}>
        事件上报后，以下属性自动注入执行上下文，可在后续节点中直接引用：
      </div>
      {attrs.map(a => (
        <div
          key={a.name}
          style={{
            padding: '6px 0',
            marginBottom: 6,
            borderBottom: '1px solid rgba(9, 88, 217, 0.08)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0, flexWrap: 'wrap' }}>
            <code style={{ fontSize: 12, background: '#e6f4ff', padding: '1px 6px', borderRadius: 4, color: '#0958d9' }}>
              ${'{' + a.name + '}'}
            </code>
            <Text style={{ fontSize: 12, minWidth: 0, wordBreak: 'break-word' }}>{a.displayName || a.name}</Text>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 6, flexWrap: 'wrap' }}>
            <Tag style={{ fontSize: 10, margin: 0 }}>{typeLabel(a.type)}</Tag>
            {a.required && <Text type="danger" style={{ fontSize: 10 }}>必填</Text>}
          </div>
        </div>
      ))}
      <div style={{ fontSize: 11, color: '#8c8c8c', marginTop: 6 }}>
        在 API_CALL 入参中填 <code>${'{orderId}'}</code> 即可引用上报的对应属性值
      </div>
    </div>
  )
}

// ── 延迟时长复合控件 ─────────────────────────────────────────────
// 存储格式：{ duration: number, unit: 'SECOND'|'MINUTE'|'HOUR' }
function DelayInput({
  valueFieldKey,
  unitFieldKey,
  unitOptions,
  applyFormPatch,
}: {
  valueFieldKey: string
  unitFieldKey: string
  unitOptions: { label: string; value: string }[]
  applyFormPatch: (patch: Record<string, unknown>) => void
}) {
  const form = Form.useFormInstance()

  // 单位默认值写回表单和节点配置，保证只填时长时也能保存完整结构。
  useEffect(() => {
    if (form.getFieldValue(unitFieldKey) == null) {
      form.setFieldValue(unitFieldKey, 'MINUTE')
      applyFormPatch({ [unitFieldKey]: 'MINUTE' })
    }
  }, [applyFormPatch, form, unitFieldKey])

  const PRESETS = [
    { label: '30秒', d: 30, u: 'SECOND' },
    { label: '5分', d: 5, u: 'MINUTE' },
    { label: '30分', d: 30, u: 'MINUTE' },
    { label: '1小时', d: 1, u: 'HOUR' },
  ]
  const controlChrome = getControlChrome()
  const displayUnitOptions = unitOptions.map((option) => {
    const value = String(option.value)
    if (value === 'SECOND') return { ...option, label: '秒' }
    if (value === 'MINUTE') return { ...option, label: '分钟' }
    if (value === 'HOUR') return { ...option, label: '小时' }
    return option
  })

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'stretch', gap: 12, width: '100%', flexWrap: 'nowrap' }}>
        <Form.Item name={valueFieldKey} noStyle>
          <InputNumber
            className="config-panel-ios-input-number"
            style={{ ...controlChrome, width: 88, minWidth: 88, flex: '0 0 88px' }}
            min={1}
            controls={false}
            placeholder="时长"
          />
        </Form.Item>
        <Form.Item name={unitFieldKey} noStyle>
          <Select
            className="config-panel-ios-select"
            classNames={CONTROL_SELECT_CLASS_NAMES}
            style={{ height: controlChrome.height, minWidth: 0, flex: '1 1 0' }}
            options={displayUnitOptions}
          />
        </Form.Item>
      </div>
      <Space style={{ marginTop: 6 }} size={4} wrap>
        {PRESETS.map(p => (
          <Button key={p.label} size="small"
            onClick={() => {
              form.setFieldValue(valueFieldKey, p.d)
              form.setFieldValue(unitFieldKey, p.u)
              applyFormPatch({ [valueFieldKey]: p.d, [unitFieldKey]: p.u })
            }}>
            {p.label}
          </Button>
        ))}
      </Space>
    </div>
  )
}

// ── API_CALL 动态入参编辑器 ─────────────────────────────────────────
// 每个参数渲染独立的 Form.Item name={['inputParams', paramName]}
// 这样 onChange 会正常触发，值会回写到 canvas 节点 config
function ApiCallInputParams({ label, apiKeyField = 'apiKey', defsSource = '/meta/api-definitions' }: {
  label: string; apiKeyField?: string; defsSource?: string
}) {
  const form   = Form.useFormInstance()
  const apiKey = Form.useWatch(apiKeyField, form)
  const [params, setParams] = useState<ApiParamDef[]>([])
  const controlChrome = getControlChrome()
  const { options: paramTypeOptions } = useSystemOptions('param_type')

  // 根据当前 apiKey 找到 API 定义中的 requestSchema，并同步清理旧接口遗留入参。
  useEffect(() => {
    if (!apiKey) { setParams([]); return }
    /** 从 API 定义列表中选中当前接口并同步请求参数 schema。 */
    const pick = (list: any[]) => {
      const def = list.find(d => d.value === apiKey)
      let schema: ApiParamDef[] = []
      try { schema = def ? JSON.parse(def.requestSchema || '[]') : [] } catch {}
      setParams(schema)
      // 切换接口时清理不属于新 schema 的旧 inputParams 键
      const schemaKeys = new Set(schema.map(p => p.name))
      const cur: Record<string, unknown> = form.getFieldValue('inputParams') ?? {}
      if (Object.keys(cur).some(k => !schemaKeys.has(k))) {
        const next: Record<string, unknown> = {}
        schema.forEach(p => { if (cur[p.name] !== undefined) next[p.name] = cur[p.name] })
        form.setFieldValue('inputParams', next)
      }
    }
    if (rawCache.has(defsSource)) { pick(rawCache.get(defsSource)!); return }
    loadDataSource(defsSource).then(pick)
  }, [apiKey, defsSource]) // eslint-disable-line react-hooks/exhaustive-deps

  /** 把 API 参数类型编码转换成系统字典展示名。 */
  const typeLabel = (t: string) => paramTypeOptions.find(p => p.value === t)?.label ?? t

  return (
    <div>
      {renderControlLabel(label)}
      {!apiKey && <Text type="secondary" style={{ fontSize: 12 }}>请先选择接口</Text>}
      {apiKey && !params.length && <Text type="secondary" style={{ fontSize: 12 }}>该接口未定义请求参数</Text>}
      {params.map(p => (
        <Form.Item
          key={p.name}
          name={['inputParams', p.name]}
          label={
            <span style={getControlLabelStyle()}>
              {p.displayName || p.name}
              {p.required && <span style={{ color: '#f5222d', marginLeft: 2 }}>*</span>}
              <Tag style={{ marginLeft: 6, fontSize: 10 }}>{typeLabel(p.type)}</Tag>
              {p.type === 'STRING_PARAM' && (
                <Tooltip title="支持 ${contextKey} 占位符，运行时自动替换为上下文值">
                  <QuestionCircleOutlined style={{ marginLeft: 4, color: '#8c8c8c' }} />
                </Tooltip>
              )}
            </span>
          }
          style={{ marginBottom: 8 }}
        >
          {p.type === 'NUMBER' ? (
            <InputNumber className="config-panel-ios-input-number" style={{ ...controlChrome, width: '100%' }} />
          ) : p.type === 'TEXT' ? (
            <Input.TextArea className="config-panel-ios-textarea" rows={2} />
          ) : (
            <Input
              className="config-panel-ios-input"
              style={controlChrome}
              placeholder={p.type === 'STRING_PARAM' ? '如：${userId} 或固定值' : ''}
            />
          )}
        </Form.Item>
      ))}
    </div>
  )
}
