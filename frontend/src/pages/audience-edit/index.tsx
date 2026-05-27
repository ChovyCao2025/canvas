/**
 * 页面职责：人群新建/编辑页，维护规则树、数据源配置、种子标签和计算策略。
 *
 * 维护说明：保存时会把前端表单拆分的配置重新序列化为后端 DTO。
 */
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, Form, Input, InputNumber, Select, Space, Switch, Typography, message } from 'antd'
import { QueryBuilder, type RuleGroupType } from 'react-querybuilder'
import { QueryBuilderAntD } from '@react-querybuilder/antd'
import { audienceApi, type AudienceDefinition } from '../../services/audienceApi'
import { dataSourceConfigApi, type DataSourceConfig, type DataSourceTableMeta } from '../../services/dataSourceConfigApi'
import { tagDefinitionApi } from '../../services/api'
import { useSystemOptions } from '../../hooks/useSystemOptions'
import 'react-querybuilder/dist/query-builder.css'

/** 页面标题组件别名。 */
const { Title } = Typography

/**
 * 人群编辑页表单模型。
 * 说明：
 * 1) 后端 `enabled` 使用 0/1，这里改为 boolean 便于 Switch 组件绑定。
 * 2) `dataSourceConfig` 在后端是 JSON 字符串，这里拆成两种子配置分别编辑。
 */
type AudienceEditFormValues = Omit<AudienceDefinition, 'enabled'> & {
  /** UI 层启用状态（保存时转换成 0/1）。 */
  enabled?: boolean

  /** TAGGER_API 数据源配置。 */
  taggerConfig?: {
    /** 初始圈选标签编码。 */
    seedTagCode?: string
  }

  /** JDBC 数据源配置。 */
  jdbcConfig?: {
    /** 平台数据源配置 ID。 */
    dataSourceId?: number

    /** 人群筛选的基础表名。 */
    baseTable?: string

    /** 用户主键列名。 */
    userIdColumn?: string
    /** 最大扫描行数，防止全表无上限扫描。 */
    maxRows?: number
  }
}

/**
 * 后端规则 JSON -> QueryBuilder 结构。
 * 解析失败时返回空规则，避免页面因历史脏数据崩溃。
 */
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

/**
 * 递归归一化后端规则组：
 * - `logic` 统一映射为 and/or
 * - `IN` 运算符在编辑器里用 `in`
 * - 数组值转成逗号分隔字符串，便于文本输入
 */
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

/**
 * QueryBuilder 结构 -> 后端规则 JSON 对象。
 * 这里不直接 stringify，便于预览和后续复用。
 */
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

/** 将文本输入值还原成基础类型，减少后端二次类型推断。 */
function normalizeScalarValue(value: unknown) {
  if (typeof value !== 'string') {
    return value
  }
  if (value === 'true') return true
  if (value === 'false') return false
  if (value !== '' && !Number.isNaN(Number(value))) return Number(value)
  return value
}

/** 解析数据源配置 JSON，容错历史空值/非法值。 */
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

/** 人群新建/编辑页面主组件。 */
export default function AudienceEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [form] = Form.useForm<AudienceEditFormValues>()

  /** QueryBuilder 当前编辑态规则。 */
  const [query, setQuery] = useState<RuleGroupType>({ combinator: 'and', rules: [] })

  /** QueryBuilder 字段（来自标签定义）。 */
  const [tagFields, setTagFields] = useState<{ name: string; label: string; value: string }[]>([])

  /** Select 组件的标签选项。 */
  const [tagOptions, setTagOptions] = useState<{ label: string; value: string }[]>([])

  /** 已启用 JDBC 数据源配置。 */
  const [jdbcDataSources, setJdbcDataSources] = useState<DataSourceConfig[]>([])

  /** 当前 JDBC 数据源下的表结构。 */
  const [jdbcTables, setJdbcTables] = useState<DataSourceTableMeta[]>([])
  const [tableLoading, setTableLoading] = useState(false)

  /** 保存按钮 loading。 */
  const [loading, setLoading] = useState(false)
  const isEdit = Boolean(id)
  const { options: dataSourceOptions } = useSystemOptions('audience_data_source_type')
  const { options: evaluationOptions } = useSystemOptions('audience_evaluation_strategy')
  const { options: engineOptions } = useSystemOptions('audience_engine_type')
  const { options: combinatorOptions } = useSystemOptions('query_combinator')
  const { options: audienceOperatorOptions } = useSystemOptions('audience_condition_operator')

  const dataSourceType = Form.useWatch('dataSourceType', form) ?? 'TAGGER_API'
  const jdbcDataSourceId = Form.useWatch(['jdbcConfig', 'dataSourceId'], form)
  const jdbcBaseTable = Form.useWatch(['jdbcConfig', 'baseTable'], form)
  const enabled = Form.useWatch('enabled', form) ?? true
  /** 当前选中的 JDBC 基表元数据，用于派生规则字段和用户 ID 列候选。 */
  const selectedJdbcTable = useMemo(
    () => jdbcTables.find(item => item.name === jdbcBaseTable),
    [jdbcBaseTable, jdbcTables],
  )
  /** QueryBuilder 字段来源：JDBC 模式取表字段，标签模式取标签定义。 */
  const fields = useMemo(() => {
    if (dataSourceType === 'JDBC' && selectedJdbcTable) {
      return selectedJdbcTable.columns.map(column => ({ name: column, label: column, value: column }))
    }
    return tagFields
  }, [dataSourceType, selectedJdbcTable, tagFields])
  /** QueryBuilder 操作符选项，由系统字典转换成库需要的 name/label。 */
  const queryOperators = useMemo(
    () => audienceOperatorOptions.map(option => ({ name: option.value, label: option.label })),
    [audienceOperatorOptions],
  )
  /** QueryBuilder 逻辑关系选项，由系统字典转换成库需要的 name/label。 */
  const queryCombinators = useMemo(
    () => combinatorOptions.map(option => ({ name: option.value, label: option.label })),
    [combinatorOptions],
  )

  // 加载标签定义：同时用于规则字段和种子标签下拉选项。
  useEffect(() => {
    tagDefinitionApi.list({ page: 1, size: 100, enabled: 1 }).then(res => {
      const tags = res.data.list.map((item: any) => ({ name: item.tagCode, label: item.name, value: item.tagCode }))
      setTagFields(tags)
      setTagOptions(tags.map(item => ({ label: item.label, value: item.name })))
    })
  }, [])

  // 加载平台级 JDBC 数据源，下拉选择后只在人群配置里保存 dataSourceId。
  useEffect(() => {
    dataSourceConfigApi.list({ page: 1, size: 100, type: 'JDBC', enabled: 1 }).then(res => {
      setJdbcDataSources(res.data.list)
    })
  }, [])

  // 选择 JDBC 数据源后读取表和列，后续表名/用户 ID 列都从元数据下拉选择。
  useEffect(() => {
    if (dataSourceType !== 'JDBC' || !jdbcDataSourceId) {
      setJdbcTables([])
      return
    }
    setTableLoading(true)
    dataSourceConfigApi.listTables(Number(jdbcDataSourceId))
      .then(res => setJdbcTables(res.data))
      .catch(error => {
        setJdbcTables([])
        message.error(error?.message || '读取数据源表结构失败')
      })
      .finally(() => setTableLoading(false))
  }, [dataSourceType, jdbcDataSourceId])

  /** 切换 JDBC 数据源后清空表选择，避免沿用上一个数据源的表名。 */
  const handleJdbcDataSourceChange = (dataSourceId: number) => {
    form.setFieldsValue({
      jdbcConfig: {
        ...form.getFieldValue('jdbcConfig'),
        dataSourceId,
        baseTable: undefined,
        userIdColumn: 'user_id',
      },
    })
    setJdbcTables([])
  }

  /** 切换基表后根据字段自动选择 user_id 或第一列作为用户 ID 列。 */
  const handleBaseTableChange = (tableName: string) => {
    const table = jdbcTables.find(item => item.name === tableName)
    const columns = table?.columns ?? []
    form.setFieldsValue({
      jdbcConfig: {
        ...form.getFieldValue('jdbcConfig'),
        baseTable: tableName,
        userIdColumn: columns.includes('user_id') ? 'user_id' : columns[0],
      },
    })
  }

  // 编辑态回填：包含基本字段、数据源配置拆分以及规则 JSON 反序列化。
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
    })
  }, [form, id, isEdit])

  // 规则实时预览：方便排查复杂条件最终落库格式。
  const querySummary = useMemo(() => JSON.stringify(serializeGroup(query), null, 2), [query])

  // 保存链路：校验 -> 组装后端 DTO -> 创建/更新。
  const handleSave = async () => {
    const values = await form.validateFields()
    setLoading(true)
    try {
      // dataSourceConfig 在后端为 JSON 字符串，按当前数据源类型序列化。
      const config = values.dataSourceType === 'JDBC'
        ? values.jdbcConfig
        : values.taggerConfig

      const body: AudienceDefinition = {
        name: values.name,
        description: values.description,
        ruleJson: JSON.stringify(serializeGroup(query)),
        engineType: values.engineType,
        dataSourceType: values.dataSourceType,
        dataSourceConfig: JSON.stringify(config ?? {}),
        evaluationStrategy: values.evaluationStrategy,
        cronExpression: values.cronExpression,
        enabled: values.enabled ? 1 : 0,
      }

      // 编辑成功后保持当前页；新建成功后跳转到编辑页继续细化配置。
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
            <Select options={dataSourceOptions} />
          </Form.Item>

          {dataSourceType === 'TAGGER_API' && (
            <Form.Item name={['taggerConfig', 'seedTagCode']} label="种子标签" rules={[{ required: true, message: '请选择种子标签' }]}>
              <Select showSearch options={tagOptions} placeholder="选择一个种子标签" />
            </Form.Item>
          )}

          {dataSourceType === 'JDBC' && (
            <>
              <Form.Item name={['jdbcConfig', 'dataSourceId']} label="数据源" rules={[{ required: true, message: '请选择数据源' }]}>
                <Select
                  showSearch
                  optionFilterProp="label"
                  placeholder={jdbcDataSources.length ? '选择已配置的数据源' : '暂无可用数据源，请先到数据源配置中创建'}
                  onChange={handleJdbcDataSourceChange}
                  options={jdbcDataSources.map(item => ({
                    value: item.id,
                    label: item.name,
                  }))}
                />
              </Form.Item>
              <Form.Item name={['jdbcConfig', 'baseTable']} label="基础表名" rules={[{ required: true, message: '请输入基础表名' }]}>
                <Select
                  showSearch
                  loading={tableLoading}
                  optionFilterProp="label"
                  placeholder={jdbcDataSourceId ? '选择基础表' : '请先选择数据源'}
                  onChange={handleBaseTableChange}
                  options={jdbcTables.map(table => ({ value: table.name, label: table.name }))}
                />
              </Form.Item>
              <Form.Item name={['jdbcConfig', 'userIdColumn']} label="用户 ID 列名" rules={[{ required: true, message: '请输入用户 ID 列名' }]}>
                <Select
                  showSearch
                  optionFilterProp="label"
                  placeholder={selectedJdbcTable ? '选择用户 ID 列' : '请先选择基础表'}
                  options={(selectedJdbcTable?.columns ?? []).map(column => ({ value: column, label: column }))}
                />
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
              operators={queryOperators}
              query={query}
              onQueryChange={next => setQuery(next as RuleGroupType)}
              combinators={queryCombinators}
            />
          </QueryBuilderAntD>
          <div style={{ marginTop: 12 }}>
            <div style={{ fontSize: 12, color: '#666', marginBottom: 6 }}>规则 JSON 预览</div>
            <Input.TextArea value={querySummary} rows={8} readOnly />
          </div>
        </Card>

        <Card title="计算配置">
          <Form.Item name="evaluationStrategy" label="计算策略">
            <Select options={evaluationOptions} />
          </Form.Item>
          <Form.Item name="engineType" label="规则引擎">
            <Select options={engineOptions} />
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
