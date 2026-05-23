import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, Form, Input, InputNumber, Select, Space, Switch, Typography, message } from 'antd'
import { QueryBuilder, type RuleGroupType } from 'react-querybuilder'
import { QueryBuilderAntD } from '@react-querybuilder/antd'
import { audienceApi, type AudienceDefinition } from '../../services/audienceApi'
import { audienceDataSourceApi, type AudienceDataSource } from '../../services/audienceDataSourceApi'
import { tagDefinitionApi } from '../../services/api'
import 'react-querybuilder/dist/query-builder.css'

const { Title } = Typography

const operators = [
  { name: '=', label: '等于' },
  { name: '!=', label: '不等于' },
  { name: '>', label: '大于' },
  { name: '>=', label: '大于等于' },
  { name: '<', label: '小于' },
  { name: '<=', label: '小于等于' },
  { name: 'in', label: '包含于' },
]

type AudienceEditFormValues = Omit<AudienceDefinition, 'enabled'> & {
  enabled?: boolean
  taggerConfig?: {
    seedTagCode?: string
  }
  jdbcConfig?: {
    baseTable?: string
    userIdColumn?: string
    maxRows?: number
  }
}

function toRuleGroup(ruleJson?: string): RuleGroupType {
  if (!ruleJson) {
    return { combinator: 'and', rules: [] }
  }
  try {
    const parsed = JSON.parse(ruleJson)
    return normalizeGroup(parsed)
  } catch {
    return { combinator: 'and', rules: [] }
  }
}

function normalizeGroup(group: any): RuleGroupType {
  const rules = [
    ...((group.conditions ?? []).map((condition: any) => ({
      field: condition.field,
      operator: condition.op === 'IN' ? 'in' : condition.op,
      value: Array.isArray(condition.value) ? condition.value.join(',') : condition.value,
    }))),
    ...((group.groups ?? []).map((child: any) => normalizeGroup(child))),
  ]
  return {
    combinator: String(group.logic ?? 'AND').toLowerCase() === 'or' ? 'or' : 'and',
    rules,
  }
}

function serializeGroup(group: RuleGroupType): any {
  const conditions: any[] = []
  const groups: any[] = []
  for (const rule of group.rules ?? []) {
    if ('rules' in rule) {
      groups.push(serializeGroup(rule))
      continue
    }
    conditions.push({
      field: rule.field,
      op: rule.operator === 'in' ? 'IN' : rule.operator,
      value: rule.operator === 'in'
        ? String(rule.value ?? '').split(',').map(item => item.trim()).filter(Boolean)
        : normalizeScalarValue(rule.value),
    })
  }
  return {
    logic: group.combinator === 'or' ? 'OR' : 'AND',
    conditions,
    groups,
  }
}

function normalizeScalarValue(value: unknown) {
  if (typeof value !== 'string') {
    return value
  }
  if (value === 'true') return true
  if (value === 'false') return false
  if (value !== '' && !Number.isNaN(Number(value))) return Number(value)
  return value
}

function parseDataSourceConfig(value?: string) {
  if (!value) {
    return {}
  }
  try {
    return JSON.parse(value)
  } catch {
    return {}
  }
}

export default function AudienceEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [form] = Form.useForm<AudienceEditFormValues>()
  const [query, setQuery] = useState<RuleGroupType>({ combinator: 'and', rules: [] })
  const [fields, setFields] = useState<{ name: string; label: string; value: string }[]>([])
  const [tagOptions, setTagOptions] = useState<{ label: string; value: string }[]>([])
  const [jdbcSources, setJdbcSources] = useState<AudienceDataSource[]>([])
  const [loading, setLoading] = useState(false)
  const isEdit = Boolean(id)

  const dataSourceType = Form.useWatch('dataSourceType', form) ?? 'TAGGER_API'
  const selectedDataSourceId = Form.useWatch('dataSourceId', form)
  const enabled = Form.useWatch('enabled', form) ?? true
  const selectedJdbcSource = jdbcSources.find(item => item.id === selectedDataSourceId)
  const jdbcSourceOptions = jdbcSources
    .filter(item => item.enabled === 1 || item.id === selectedDataSourceId)
    .map(item => ({
      value: item.id,
      label: item.enabled === 1 ? item.name : `${item.name}（已停用）`,
    }))

  useEffect(() => {
    tagDefinitionApi.list({ page: 1, size: 100, enabled: 1 }).then(res => {
      const tags = res.data.list.map((item: any) => ({ name: item.tagCode, label: item.name, value: item.tagCode }))
      setFields(tags)
      setTagOptions(tags.map(item => ({ label: item.label, value: item.name })))
    })
    audienceDataSourceApi.list().then(res => {
      setJdbcSources(res.data)
    })
  }, [])

  useEffect(() => {
    if (!isEdit || !id) {
      return
    }
    audienceApi.get(Number(id)).then(res => {
      const current = res.data
      const config = parseDataSourceConfig(current.dataSourceConfig)
      form.setFieldsValue({
        ...current,
        enabled: current.enabled === 1,
        taggerConfig: { seedTagCode: config.seedTagCode },
        jdbcConfig: config,
      } as any)
      setQuery(toRuleGroup(current.ruleJson))
      if (current.dataSourceType === 'JDBC' && current.dataSourceId != null) {
        audienceDataSourceApi.get(current.dataSourceId).then(sourceRes => {
          setJdbcSources(prev => prev.some(item => item.id === sourceRes.data.id) ? prev : [...prev, sourceRes.data])
        })
      }
    })
  }, [form, id, isEdit])

  const querySummary = useMemo(() => JSON.stringify(serializeGroup(query), null, 2), [query])

  const handleSave = async () => {
    const values = await form.validateFields()
    setLoading(true)
    try {
      const config = values.dataSourceType === 'JDBC'
        ? values.jdbcConfig
        : values.taggerConfig
      const body: AudienceDefinition = {
        name: values.name,
        description: values.description,
        ruleJson: JSON.stringify(serializeGroup(query)),
        engineType: values.engineType,
        dataSourceType: values.dataSourceType,
        dataSourceId: values.dataSourceType === 'JDBC' ? values.dataSourceId : undefined,
        dataSourceConfig: JSON.stringify(config ?? {}),
        evaluationStrategy: values.evaluationStrategy,
        cronExpression: values.cronExpression,
        enabled: values.enabled ? 1 : 0,
      }
      if (isEdit && id) {
        await audienceApi.update(Number(id), body)
        message.success('保存成功，已自动重新计算')
      } else {
        const res = await audienceApi.create(body)
        message.success('创建成功，已自动开始计算')
        navigate(`/audiences/${res.data.id}/edit`)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: 24 }}>
      <Title level={4}>{isEdit ? '编辑人群' : '新建人群'}</Title>
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          engineType: 'AVIATOR',
          dataSourceType: 'TAGGER_API',
          evaluationStrategy: 'OFFLINE_BATCH',
          enabled: true,
          taggerConfig: { seedTagCode: undefined },
          jdbcConfig: { userIdColumn: 'user_id' },
        }}
      >
        <Card title="基本信息" style={{ marginBottom: 16 }}>
          <Form.Item name="name" label="人群名称" rules={[{ required: true, message: '请输入人群名称' }]}>
            <Input placeholder="例：近30天消费-一线城市VIP用户" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="enabled" label="启用状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Card>

        <Card title="数据源配置" style={{ marginBottom: 16 }}>
          <Form.Item name="dataSourceType" label="数据源类型">
            <Select options={[{ value: 'TAGGER_API', label: 'Tagger API' }, { value: 'JDBC', label: 'JDBC' }]} />
          </Form.Item>

          {dataSourceType === 'TAGGER_API' && (
            <Form.Item name={['taggerConfig', 'seedTagCode']} label="种子标签" rules={[{ required: true, message: '请选择种子标签' }]}>
              <Select showSearch options={tagOptions} placeholder="选择一个种子标签" />
            </Form.Item>
          )}

          {dataSourceType === 'JDBC' && (
            <>
              <Form.Item name="dataSourceId" label="人群数据源" rules={[{ required: true, message: '请选择人群数据源' }]}>
                <Select
                  options={jdbcSourceOptions}
                  placeholder={jdbcSourceOptions.length ? '选择数据源' : '暂无可用数据源，请先创建'}
                />
              </Form.Item>
              {selectedJdbcSource?.enabled === 0 && (
                <div style={{ marginBottom: 12, color: '#d46b08', fontSize: 12 }}>
                  当前人群引用的数据源已停用，可继续查看和保存，但新建人群不应再选择该数据源。
                </div>
              )}
              <Form.Item name={['jdbcConfig', 'baseTable']} label="基础表名" rules={[{ required: true, message: '请输入基础表名' }]}>
                <Input placeholder="audience_demo_user" />
              </Form.Item>
              <Form.Item name={['jdbcConfig', 'userIdColumn']} label="用户 ID 列名" rules={[{ required: true, message: '请输入用户 ID 列名' }]}>
                <Input placeholder="user_id" />
              </Form.Item>
              <Form.Item name={['jdbcConfig', 'maxRows']} label="最大扫描行数">
                <InputNumber style={{ width: '100%' }} min={1} placeholder="可选，如 500000" />
              </Form.Item>
            </>
          )}
        </Card>

        <Card title="圈选规则" style={{ marginBottom: 16 }}>
          <QueryBuilderAntD>
            <QueryBuilder
              fields={fields}
              operators={operators}
              query={query}
              onQueryChange={next => setQuery(next as RuleGroupType)}
              combinators={[{ name: 'and', label: '且（AND）' }, { name: 'or', label: '或（OR）' }]}
            />
          </QueryBuilderAntD>
          <div style={{ marginTop: 12 }}>
            <div style={{ fontSize: 12, color: '#666', marginBottom: 6 }}>规则 JSON 预览</div>
            <Input.TextArea value={querySummary} rows={8} readOnly />
          </div>
        </Card>

        <Card title="计算配置">
          <Form.Item name="evaluationStrategy" label="计算策略">
            <Select options={[
              { value: 'OFFLINE_BATCH', label: '离线批量' },
              { value: 'ONLINE', label: '实时计算' },
              { value: 'HYBRID', label: '混合' },
            ]} />
          </Form.Item>
          <Form.Item name="engineType" label="规则引擎">
            <Select options={[
              { value: 'AVIATOR', label: 'AviatorScript' },
              { value: 'QL', label: 'QLExpress' },
            ]} />
          </Form.Item>
          <Form.Item name="cronExpression" label="定时计算 Cron">
            <Input placeholder="例：0 2 * * *" />
          </Form.Item>
          <div style={{ fontSize: 12, color: '#666' }}>
            当前状态：{enabled ? '启用后保存会自动重新计算并刷新调度' : '禁用后不会参与调度'}
          </div>
        </Card>
      </Form>

      <div style={{ marginTop: 16, textAlign: 'right' }}>
        <Space>
          <Button onClick={() => navigate('/audiences')}>取消</Button>
          <Button type="primary" loading={loading} onClick={handleSave}>
            {isEdit ? '保存' : '创建'}
          </Button>
        </Space>
      </div>
    </div>
  )
}
