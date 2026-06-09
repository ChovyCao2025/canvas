import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  CheckCircleOutlined,
  HistoryOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SendOutlined,
} from '@ant-design/icons'
import { riskApi, type RiskDecisionAction, type RiskSimulationRequest } from '../../services/riskApi'
import {
  applyRiskRemoteSnapshot,
  activateStrategyVersion,
  buildRiskStrategyDraftCommand,
  editDraftRule,
  openRuleEditor,
  rollbackStrategyVersion,
  saveRuleGroup,
  selectRiskTrace,
  selectRiskWorkbenchTab,
  startLocalSimulation,
  submitStrategyApproval,
  validateDraftRule,
  type RiskDraftRule,
  type RiskRuleGroupRow,
  type RiskSimulationDraft,
  type RiskStudioState,
  type RiskStudioVersion,
  type RiskWorkbenchTab,
} from './riskWorkbench'

const { Text, Title } = Typography

interface RuleFormValues {
  ruleKey: string
  displayName: string
  action: RiskDecisionAction
  expression: string
}

interface RuleGroupFormValues {
  groupKey: string
  displayName: string
  priority: number
  ruleCount: number
  status: string
}

interface RiskListFormValues {
  listKey: string
  subjectType: string
  subjectKey: string
  action: RiskDecisionAction
  reason: string
}

const initialDraftRule: RiskDraftRule = {
  sceneKey: '',
  ruleKey: '',
  displayName: '',
  action: 'REVIEW',
  expression: '',
}

function createInitialState(): RiskStudioState {
  return {
    scenes: [],
    strategies: [],
    selectedSceneKey: null,
    selectedStrategyKey: null,
    activeVersion: null,
    draftVersion: null,
    versions: [],
    activeTab: 'STUDIO',
    ruleEditor: {
      open: false,
      groupKey: null,
    },
    ruleGroups: [],
    listEntries: [],
    simulations: [],
    traces: [],
    selectedTraceId: null,
    draftRule: initialDraftRule,
    validation: { status: 'IDLE', errors: [] },
    approval: { status: 'DRAFT' },
    rollback: null,
  }
}

/** 风控策略工作台，覆盖场景选择、规则草稿、校验审批、版本激活和回滚入口。 */
export default function RiskStudioPage() {
  const [form] = Form.useForm<RuleFormValues>()
  const [ruleGroupForm] = Form.useForm<RuleGroupFormValues>()
  const [listForm] = Form.useForm<RiskListFormValues>()
  const [state, setState] = useState<RiskStudioState>(() => createInitialState())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [simulating, setSimulating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const selectedScene = useMemo(
    () => state.scenes.find(scene => scene.sceneKey === state.selectedSceneKey) ?? null,
    [state.scenes, state.selectedSceneKey],
  )

  const selectedStrategy = useMemo(
    () => state.strategies.find(strategy => strategy.strategyKey === state.selectedStrategyKey) ?? null,
    [state.strategies, state.selectedStrategyKey],
  )

  const canActivateVersion = useCallback((record: RiskStudioVersion) => {
    if (record.status === 'ACTIVE' || record.status === 'DRAFT' || record.status === 'FAILED') {
      return false
    }
    if (selectedStrategy?.riskLevel === 'HIGH') {
      return record.status === 'APPROVAL_PENDING' && record.approvedBy != null
    }
    return ['VALIDATED', 'SIMULATED', 'APPROVAL_PENDING', 'ROLLED_BACK', 'PAUSED'].includes(record.status)
  }, [selectedStrategy])

  const loadWorkbench = useCallback(async (preferredSceneKey?: string, preferredStrategyKey?: string) => {
    setLoading(true)
    setError(null)
    try {
      const sceneResponse = await riskApi.listScenes()
      const scenes = sceneResponse.data ?? []
      const selectedSceneKey = preferredSceneKey ?? scenes[0]?.sceneKey ?? null
      const strategies = selectedSceneKey == null
        ? []
        : (await riskApi.listStrategies(selectedSceneKey)).data ?? []
      const selectedStrategyKey = preferredStrategyKey != null && strategies.some(strategy => strategy.strategyKey === preferredStrategyKey)
        ? preferredStrategyKey
        : strategies[0]?.strategyKey ?? null
      const versions = selectedStrategyKey == null
        ? []
        : (await riskApi.listStrategyVersions(selectedStrategyKey)).data ?? []
      const lists = (await riskApi.listLists()).data ?? []
      const entryResponses = await Promise.all(lists.map(list => riskApi.listListEntries(list.listKey)))
      const listEntries = entryResponses.flatMap(response => response.data ?? [])
      const traces = selectedSceneKey == null
        ? []
        : (await riskApi.listDecisionTraces(selectedSceneKey, 50)).data ?? []
      const simulations = selectedSceneKey == null
        ? []
        : (await riskApi.listSimulations(selectedSceneKey, 50)).data ?? []
      setState(current => applyRiskRemoteSnapshot({
        ...current,
        selectedSceneKey,
        selectedStrategyKey,
      }, {
        scenes,
        strategies,
        versions,
        lists,
        listEntries,
        simulations,
        traces,
      }))
    } catch (err) {
      setError(err instanceof Error ? err.message : '风控工作台数据加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadWorkbench()
  }, [loadWorkbench])

  useEffect(() => {
    form.setFieldsValue({
      ruleKey: state.draftRule.ruleKey,
      displayName: state.draftRule.displayName,
      action: state.draftRule.action,
      expression: state.draftRule.expression,
    })
  }, [form, state.draftRule])

  const handleSceneChange = (sceneKey: string) => {
    void loadWorkbench(sceneKey)
  }

  const handleRuleValuesChange = (_: Partial<RuleFormValues>, values: RuleFormValues) => {
    setState(current => editDraftRule(current, values))
  }

  const handleTabChange = (key: string) => {
    setState(current => selectRiskWorkbenchTab(current, key as RiskWorkbenchTab))
  }

  const handleOpenRuleGroup = (groupKey: string | null = null) => {
    const group = groupKey == null ? null : state.ruleGroups.find(item => item.groupKey === groupKey)
    ruleGroupForm.setFieldsValue(group ?? {
      groupKey: '',
      displayName: '',
      priority: 100,
      ruleCount: 1,
      status: 'ENABLED',
    })
    setState(current => openRuleEditor(current, groupKey))
  }

  const handleSaveRuleGroup = async () => {
    const values = await ruleGroupForm.validateFields()
    setState(current => saveRuleGroup(current, values))
  }

  const handleAddListEntry = async () => {
    const values = await listForm.validateFields()
    setSaving(true)
    try {
      await riskApi.createList({
        listKey: values.listKey,
        listType: listTypeForAction(values.action),
        subjectType: values.subjectType,
        requiresApproval: false,
      })
      await riskApi.addListEntry(values.listKey, {
        subjectType: values.subjectType,
        rawSubject: values.subjectKey,
        reason: values.reason || 'manual',
        source: 'strategy-studio',
      })
      listForm.resetFields()
      await loadWorkbench(state.selectedSceneKey ?? undefined)
      message.success('名单条目已写入')
    } catch {
      message.error('名单条目写入失败')
    } finally {
      setSaving(false)
    }
  }

  const handleValidate = async () => {
    const next = validateDraftRule(state)
    setState(next)
    if (next.validation.status !== 'VALID') return
    const command = buildRiskStrategyDraftCommand(next)
    if (command == null) {
      message.error('请先选择场景并填写规则')
      return
    }
    setSaving(true)
    try {
      const draft = await riskApi.createStrategyDraft(command)
      const version = draft.data.draftVersion
      if (version == null) {
        throw new Error('draft version is missing')
      }
      await riskApi.validateStrategyVersion(command.strategyKey, version)
      await loadWorkbench(command.sceneKey, command.strategyKey)
      message.success(`草稿版本 ${version} 已通过后端校验`)
    } catch {
      message.error('策略草稿后端校验失败')
    } finally {
      setSaving(false)
    }
  }

  const handleSubmit = async () => {
    const validated = validateDraftRule(state)
    const next = submitStrategyApproval(validated, '策略草稿已完成校验，提交审批')
    setState(validated)
    if (next.approval.status === 'BLOCKED') {
      setState(next)
      return
    }
    const command = buildRiskStrategyDraftCommand(validated)
    if (command == null) {
      message.error('请先选择场景并填写规则')
      return
    }
    setSaving(true)
    try {
      const draft = await riskApi.createStrategyDraft(command)
      const version = draft.data.draftVersion
      if (version == null) {
        throw new Error('draft version is missing')
      }
      await riskApi.validateStrategyVersion(command.strategyKey, version)
      await riskApi.simulateStrategyVersion(command.strategyKey, version, {
        reason: '从 Strategy Studio 完成上线前仿真标记',
      })
      await riskApi.submitStrategyVersion(command.strategyKey, version, {
        reason: next.approval.reason,
      })
      setState(next)
      await loadWorkbench(command.sceneKey, command.strategyKey)
      message.success(`版本 ${version} 已提交审批`)
    } catch {
      message.error('提交审批失败')
    } finally {
      setSaving(false)
    }
  }

  const handleApprove = async (version: number) => {
    if (state.selectedStrategyKey == null) {
      message.error('请先选择策略')
      return
    }
    setSaving(true)
    try {
      await riskApi.approveStrategyVersion(state.selectedStrategyKey, version, {
        reason: '从 Strategy Studio 审批',
      })
      await loadWorkbench(state.selectedSceneKey ?? undefined)
      message.success(`版本 ${version} 已审批`)
    } catch {
      message.error('策略审批失败')
    } finally {
      setSaving(false)
    }
  }

  const handleActivate = async (version: number) => {
    if (state.selectedStrategyKey == null) {
      message.error('请先选择策略')
      return
    }
    setSaving(true)
    try {
      const response = await riskApi.activateStrategyVersion(state.selectedStrategyKey, version, {
        reason: '从 Strategy Studio 激活',
      })
      setState(current => activateStrategyVersion(current, response.data.activeVersion ?? version))
      await loadWorkbench(state.selectedSceneKey ?? undefined)
      message.success(`已激活版本 ${version}`)
    } catch {
      message.error('策略激活失败')
    } finally {
      setSaving(false)
    }
  }

  const handleRollback = async (version: number) => {
    if (state.selectedStrategyKey == null) {
      message.error('请先选择策略')
      return
    }
    setSaving(true)
    try {
      const response = await riskApi.rollbackStrategy(state.selectedStrategyKey, {
        targetVersion: version,
        reason: '从 Strategy Studio 回滚',
      })
      setState(current => rollbackStrategyVersion(
        current,
        response.data.activeVersion ?? version,
        '从 Strategy Studio 回滚',
      ))
      await loadWorkbench(state.selectedSceneKey ?? undefined)
      message.success(`已回滚到版本 ${version}`)
    } catch {
      message.error('策略回滚失败')
    } finally {
      setSaving(false)
    }
  }

  const handleSimulation = async () => {
    const candidateVersion = state.draftVersion ?? state.activeVersion
    if (state.selectedSceneKey == null || state.selectedStrategyKey == null || candidateVersion == null) {
      message.error('请先选择可仿真的策略版本')
      return
    }
    const payload: RiskSimulationRequest = {
      sceneKey: state.selectedSceneKey,
      strategyKey: state.selectedStrategyKey,
      version: candidateVersion,
      sampleWindow: {
        startTime: '2026-06-01T00:00:00Z',
        endTime: '2026-06-08T00:00:00Z',
      },
      sampleLimit: 1000,
    }
    setSimulating(true)
    try {
      const response = await riskApi.startSimulation(payload)
      setState(current => startLocalSimulation(current, {
        simulationId: response.data.simulationId,
        baselineVersion: Math.max(0, candidateVersion - 1),
        candidateVersion,
        sampleSize: response.data.sampleSize ?? payload.sampleLimit,
        actionDistribution: response.data.actionDistribution ?? {},
      } satisfies RiskSimulationDraft))
      await loadWorkbench(state.selectedSceneKey)
      message.success('已启动仿真')
    } catch {
      message.error('仿真启动失败')
    } finally {
      setSimulating(false)
    }
  }

  const selectedTrace = state.traces.find(trace => trace.traceId === state.selectedTraceId) ?? null

  return (
    <div>
      <Space align="center" style={{ marginBottom: 16 }}>
        <SafetyCertificateOutlined style={{ fontSize: 24, color: '#1677ff' }} />
        <div>
          <Title level={3} style={{ margin: 0 }}>风控策略工作台</Title>
          <Text type="secondary">管理场景策略、规则草稿、审批激活和回滚验证。</Text>
        </div>
      </Space>

      {error && <Alert type="warning" message={error} showIcon style={{ marginBottom: 16 }} />}

      <Spin spinning={loading}>
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={7}>
            <Card title="场景" extra={<Button icon={<ReloadOutlined />} onClick={() => loadWorkbench(state.selectedSceneKey ?? undefined)} />}>
              <Select
                style={{ width: '100%', marginBottom: 16 }}
                value={state.selectedSceneKey ?? undefined}
                options={state.scenes.map(scene => ({ value: scene.sceneKey, label: scene.displayName }))}
                onChange={handleSceneChange}
              />
              {selectedScene ? (
                <Space direction="vertical" size={8}>
                  <Text strong>{selectedScene.sceneKey}</Text>
                  <Text type="secondary">延迟预算：{selectedScene.latencyBudgetMs ?? '-'} ms</Text>
                  <Tag color={selectedScene.status === 'ACTIVE' ? 'green' : 'gold'}>{selectedScene.status ?? 'DRAFT'}</Tag>
                </Space>
              ) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />}
            </Card>
          </Col>

          <Col xs={24} lg={17}>
            <Card title={selectedStrategy?.displayName ?? '策略草稿'} extra={<Tag color="blue">{state.selectedStrategyKey ?? '未选择策略'}</Tag>}>
              <Tabs
                activeKey={state.activeTab}
                onChange={handleTabChange}
                items={[
                  {
                    key: 'STUDIO',
                    label: '策略',
                    children: (
                      <Space direction="vertical" size={16} style={{ width: '100%' }}>
                        <Table
                          rowKey="groupKey"
                          dataSource={state.ruleGroups}
                          pagination={false}
                          columns={[
                            { title: '规则组', dataIndex: 'displayName' },
                            { title: '优先级', dataIndex: 'priority', width: 90 },
                            { title: '规则数', dataIndex: 'ruleCount', width: 90 },
                            {
                              title: '状态',
                              dataIndex: 'status',
                              width: 110,
                              render: status => <Tag color={status === 'ENABLED' ? 'green' : 'default'}>{status}</Tag>,
                            },
                            {
                              title: '操作',
                              width: 120,
                              render: (_, record: RiskRuleGroupRow) => (
                                <Button size="small" onClick={() => handleOpenRuleGroup(record.groupKey)}>编辑</Button>
                              ),
                            },
                          ]}
                        />
                        <Button onClick={() => handleOpenRuleGroup(null)}>新增规则组</Button>

                        <Form form={form} layout="vertical" onValuesChange={handleRuleValuesChange}>
                          <Row gutter={12}>
                            <Col xs={24} md={8}>
                              <Form.Item label="规则 Key" name="ruleKey">
                                <Input />
                              </Form.Item>
                            </Col>
                            <Col xs={24} md={8}>
                              <Form.Item label="规则名称" name="displayName">
                                <Input />
                              </Form.Item>
                            </Col>
                            <Col xs={24} md={8}>
                              <Form.Item label="动作" name="action">
                                <Select
                                  options={['ALLOW', 'REVIEW', 'VERIFY', 'BLOCK', 'LIMIT', 'DELAY'].map(action => ({
                                    value: action,
                                    label: action,
                                  }))}
                                />
                              </Form.Item>
                            </Col>
                          </Row>
                          <Form.Item label="规则条件 JSON" name="expression">
                            <Input.TextArea rows={5} />
                          </Form.Item>
                        </Form>

                        {state.validation.errors.length > 0 && (
                          <Alert type="error" showIcon message={state.validation.errors.join(' / ')} />
                        )}

                        <Space wrap>
                          <Button icon={<CheckCircleOutlined />} onClick={handleValidate}>校验</Button>
                          <Button icon={<SendOutlined />} type="primary" loading={saving} onClick={handleSubmit}>
                            提交审批
                          </Button>
                          <Tag color={state.validation.status === 'VALID' ? 'green' : 'default'}>
                            {state.validation.status}
                          </Tag>
                          <Tag color={state.approval.status === 'SUBMITTED' ? 'blue' : 'default'}>
                            {state.approval.status}
                          </Tag>
                        </Space>
                      </Space>
                    ),
                  },
                  {
                    key: 'LISTS',
                    label: '名单',
                    children: (
                      <Space direction="vertical" size={16} style={{ width: '100%' }}>
                        <Form form={listForm} layout="inline" initialValues={{ subjectType: 'DEVICE_ID', action: 'BLOCK' }}>
                          <Form.Item name="listKey" rules={[{ required: true }]}><Input placeholder="名单 Key" /></Form.Item>
                          <Form.Item name="subjectType"><Select style={{ width: 140 }} options={['USER_ID', 'DEVICE_ID', 'IP', 'EMAIL', 'PHONE', 'CARD', 'GENERIC'].map(value => ({ value, label: value }))} /></Form.Item>
                          <Form.Item name="subjectKey" rules={[{ required: true }]}><Input placeholder="主体标识" /></Form.Item>
                          <Form.Item name="action"><Select style={{ width: 120 }} options={['BLOCK', 'REVIEW', 'ALLOW'].map(value => ({ value, label: value }))} /></Form.Item>
                          <Form.Item name="reason"><Input placeholder="原因" /></Form.Item>
                          <Button onClick={handleAddListEntry}>加入名单</Button>
                        </Form>
                        <Table
                          rowKey={record => `${record.listKey}:${record.maskedSubject}`}
                          dataSource={state.listEntries}
                          pagination={false}
                          columns={[
                            { title: '名单', dataIndex: 'listKey' },
                            { title: '主体类型', dataIndex: 'subjectType' },
                            { title: '主体', dataIndex: 'maskedSubject' },
                            { title: '动作', dataIndex: 'action', render: action => <Tag color={action === 'BLOCK' ? 'red' : 'gold'}>{action}</Tag> },
                            { title: '原因', dataIndex: 'reason' },
                          ]}
                        />
                      </Space>
                    ),
                  },
                  {
                    key: 'SIMULATION',
                    label: '仿真',
                    children: (
                      <Space direction="vertical" size={16} style={{ width: '100%' }}>
                        <Button icon={<PlayCircleOutlined />} loading={simulating} onClick={handleSimulation}>启动仿真</Button>
                        <Table
                          rowKey="simulationId"
                          dataSource={state.simulations}
                          pagination={false}
                          columns={[
                            { title: '仿真 ID', dataIndex: 'simulationId' },
                            { title: '基线', dataIndex: 'baselineVersion', width: 90 },
                            { title: '候选', dataIndex: 'candidateVersion', width: 90 },
                            { title: '样本量', dataIndex: 'sampleSize', width: 100 },
                            { title: '状态', dataIndex: 'status', width: 110, render: status => <Tag color={status === 'COMPLETED' ? 'green' : 'blue'}>{status}</Tag> },
                            {
                              title: '动作分布',
                              render: (_, record) => Object.entries(record.actionDistribution)
                                .map(([action, count]) => `${action}:${count}`)
                                .join(' / '),
                            },
                          ]}
                        />
                      </Space>
                    ),
                  },
                  {
                    key: 'TRACE',
                    label: 'Trace',
                    children: (
                      <Row gutter={16}>
                        <Col xs={24} md={14}>
                          <Table
                            rowKey="traceId"
                            dataSource={state.traces}
                            pagination={false}
                            onRow={record => ({ onClick: () => setState(current => selectRiskTrace(current, record.traceId)) })}
                            columns={[
                              { title: 'Trace', dataIndex: 'traceId' },
                              { title: '请求', dataIndex: 'requestId' },
                              { title: '动作', dataIndex: 'action', render: action => <Tag>{action}</Tag> },
                              { title: '分数', dataIndex: 'score', width: 80 },
                            ]}
                          />
                        </Col>
                        <Col xs={24} md={10}>
                          {selectedTrace ? (
                            <Descriptions bordered column={1} size="small">
                              <Descriptions.Item label="Trace ID">{selectedTrace.traceId}</Descriptions.Item>
                              <Descriptions.Item label="Request ID">{selectedTrace.requestId}</Descriptions.Item>
                              <Descriptions.Item label="Action">{selectedTrace.action}</Descriptions.Item>
                              <Descriptions.Item label="Score">{selectedTrace.score}</Descriptions.Item>
                              <Descriptions.Item label="Latency">{selectedTrace.latencyMs} ms</Descriptions.Item>
                            </Descriptions>
                          ) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />}
                        </Col>
                      </Row>
                    ),
                  },
                ]}
              />
            </Card>
          </Col>

          <Col xs={24}>
            <Card title="版本">
              <Table
                rowKey="version"
                dataSource={state.versions}
                pagination={false}
                columns={[
                  { title: '版本', dataIndex: 'version' },
                  {
                    title: '状态',
                    dataIndex: 'status',
                    render: status => <Tag color={status === 'ACTIVE' ? 'green' : 'default'}>{status}</Tag>,
                  },
                  {
                    title: '操作',
                    render: (_, record: RiskStudioVersion) => (
                      <Space>
                        <Button
                          size="small"
                          disabled={record.status !== 'APPROVAL_PENDING' || record.approvedBy != null}
                          loading={saving}
                          onClick={() => handleApprove(record.version)}
                        >
                          审批
                        </Button>
                        <Button
                          size="small"
                          disabled={!canActivateVersion(record)}
                          loading={saving}
                          onClick={() => handleActivate(record.version)}
                        >
                          激活
                        </Button>
                        <Button
                          size="small"
                          icon={<HistoryOutlined />}
                          disabled={record.version === state.activeVersion}
                          loading={saving}
                          onClick={() => handleRollback(record.version)}
                        >
                          回滚
                        </Button>
                      </Space>
                    ),
                  },
                ]}
              />
            </Card>
          </Col>
        </Row>
      </Spin>

      <Drawer
        title="规则组"
        open={state.ruleEditor.open}
        width={420}
        onClose={() => setState(current => ({ ...current, ruleEditor: { open: false, groupKey: null } }))}
        extra={<Button type="primary" onClick={handleSaveRuleGroup}>保存</Button>}
      >
        <Form form={ruleGroupForm} layout="vertical">
          <Form.Item label="规则组 Key" name="groupKey" rules={[{ required: true }]}>
            <Input disabled={state.ruleEditor.groupKey != null} />
          </Form.Item>
          <Form.Item label="名称" name="displayName" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="优先级" name="priority" rules={[{ required: true }]}>
            <InputNumber min={1} max={9999} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="规则数" name="ruleCount" rules={[{ required: true }]}>
            <InputNumber min={0} max={500} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="状态" name="status" rules={[{ required: true }]}>
            <Select options={['ENABLED', 'DISABLED'].map(value => ({ value, label: value }))} />
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  )
}

function listTypeForAction(action: RiskDecisionAction): string {
  if (action === 'ALLOW') return 'WHITE'
  if (action === 'REVIEW') return 'GRAY'
  return 'BLACK'
}
