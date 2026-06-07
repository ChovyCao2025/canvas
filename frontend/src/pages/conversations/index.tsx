/**
 * SCRM operator workspace for shared conversation inbox triage and follow-up.
 */
import { useEffect, useState } from 'react'
import {
  Button,
  Descriptions,
  Divider,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  List,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  CheckCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  assignConversationWorkItem,
  completeConversationSopTask,
  createConversationSopTask,
  generateConversationAiReplySuggestion,
  getConversationWorkspaceTimeline,
  listConversationAiReplySuggestions,
  listConversationInbox,
  reviewConversationAiReplySuggestion,
  updateConversationWorkItemStatus,
} from '../../services/conversationApi'
import {
  WORK_ITEM_CHANNEL_OPTIONS,
  WORK_ITEM_PRIORITY_OPTIONS,
  WORK_ITEM_STATUS_OPTIONS,
  contactProfileTitle,
  conversationMessageLine,
  formatAiReplySuggestionStatus,
  formatConversationStatus,
  formatDateTime,
  formatWorkItemStatus,
  normalizeAiReplySuggestionParams,
  normalizeInboxFilters,
  priorityColor,
  taskStatusColor,
  timelineAuditLine,
  workItemTitle,
  type ConversationAiReplySuggestion,
  type ConversationAssignmentPayload,
  type ConversationInboxParams,
  type ConversationSopTask,
  type ConversationSopTaskPayload,
  type ConversationWorkItem,
  type ConversationWorkItemStatusPayload,
  type ConversationWorkspaceTimeline,
} from './conversationPresentation'

const { Title, Text } = Typography

const INITIAL_FILTERS: ConversationInboxParams = { status: 'OPEN', limit: 50 }
const INITIAL_AI_SUGGESTION_STATUS = 'DRAFT'

export default function ConversationsPage() {
  const [filterForm] = Form.useForm<ConversationInboxParams>()
  const [assignForm] = Form.useForm<ConversationAssignmentPayload>()
  const [statusForm] = Form.useForm<ConversationWorkItemStatusPayload>()
  const [taskForm] = Form.useForm<ConversationSopTaskPayload>()
  const [rows, setRows] = useState<ConversationWorkItem[]>([])
  const [filters, setFilters] = useState<ConversationInboxParams>(INITIAL_FILTERS)
  const [loading, setLoading] = useState(false)
  const [selected, setSelected] = useState<ConversationWorkItem | null>(null)
  const [timeline, setTimeline] = useState<ConversationWorkspaceTimeline | null>(null)
  const [aiSuggestions, setAiSuggestions] = useState<ConversationAiReplySuggestion[]>([])
  const [aiSuggestionStatus, setAiSuggestionStatus] = useState(INITIAL_AI_SUGGESTION_STATUS)
  const [timelineLoading, setTimelineLoading] = useState(false)
  const [mutating, setMutating] = useState<string>()

  const load = async (nextFilters = filters) => {
    const params = normalizeInboxFilters(nextFilters)
    setLoading(true)
    try {
      const res = await listConversationInbox(params)
      setRows(res.data)
      setFilters(params)
      filterForm.setFieldsValue(params)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load(INITIAL_FILTERS)
  }, [])

  const hydrateForms = (row: ConversationWorkItem) => {
    assignForm.setFieldsValue({
      assignedTo: row.assignedTo,
      assignedTeam: row.assignedTeam,
    })
    statusForm.setFieldsValue({
      status: row.status,
      priority: row.priority,
      nextFollowUpAt: row.nextFollowUpAt,
    })
    taskForm.setFieldsValue({
      assignee: row.assignedTo,
    })
  }

  const refreshTimeline = async (workItemId: number, suggestionStatus = aiSuggestionStatus) => {
    setTimelineLoading(true)
    try {
      const [res, suggestionRes] = await Promise.all([
        getConversationWorkspaceTimeline(workItemId, {
          messageLimit: 50,
          auditLimit: 50,
        }),
        listConversationAiReplySuggestions(workItemId, normalizeAiReplySuggestionParams({
          status: suggestionStatus,
          limit: 20,
        })),
      ])
      setTimeline(res.data)
      setSelected(res.data.workItem)
      setAiSuggestions(suggestionRes.data)
      hydrateForms(res.data.workItem)
    } finally {
      setTimelineLoading(false)
    }
  }

  const openTimeline = async (row: ConversationWorkItem) => {
    setSelected(row)
    setTimeline(null)
    setAiSuggestions([])
    setAiSuggestionStatus(INITIAL_AI_SUGGESTION_STATUS)
    hydrateForms(row)
    await refreshTimeline(row.id, INITIAL_AI_SUGGESTION_STATUS)
  }

  const refreshAiSuggestions = async (workItemId: number, suggestionStatus = aiSuggestionStatus) => {
    const res = await listConversationAiReplySuggestions(workItemId, normalizeAiReplySuggestionParams({
      status: suggestionStatus,
      limit: 20,
    }))
    setAiSuggestions(res.data)
  }

  const changeAiSuggestionStatus = async (status: string) => {
    if (!selected) return
    setAiSuggestionStatus(status)
    await refreshAiSuggestions(selected.id, status)
  }

  const submitAssign = async (values: ConversationAssignmentPayload) => {
    if (!selected) return
    setMutating('assign')
    try {
      await assignConversationWorkItem(selected.id, {
        assignedTo: cleanText(values.assignedTo),
        assignedTeam: cleanText(values.assignedTeam),
        note: cleanText(values.note),
      })
      message.success('已指派')
      await Promise.all([load(filters), refreshTimeline(selected.id)])
      assignForm.setFieldValue('note', undefined)
    } finally {
      setMutating(undefined)
    }
  }

  const submitStatus = async (values: ConversationWorkItemStatusPayload) => {
    if (!selected) return
    setMutating('status')
    try {
      await updateConversationWorkItemStatus(selected.id, {
        status: values.status,
        priority: values.priority,
        nextFollowUpAt: cleanText(values.nextFollowUpAt),
        note: cleanText(values.note),
      })
      message.success('状态已更新')
      await Promise.all([load(filters), refreshTimeline(selected.id)])
      statusForm.setFieldValue('note', undefined)
    } finally {
      setMutating(undefined)
    }
  }

  const submitTask = async (values: ConversationSopTaskPayload) => {
    if (!selected) return
    setMutating('task')
    try {
      await createConversationSopTask(selected.id, {
        taskKey: values.taskKey.trim(),
        title: values.title.trim(),
        assignee: cleanText(values.assignee),
        dueAt: cleanText(values.dueAt),
        metadata: { source: 'operator_workspace' },
      })
      message.success('任务已新增')
      taskForm.resetFields(['taskKey', 'title', 'dueAt'])
      await refreshTimeline(selected.id)
    } finally {
      setMutating(undefined)
    }
  }

  const completeTask = async (task: ConversationSopTask) => {
    if (!selected) return
    setMutating(`complete-${task.id}`)
    try {
      await completeConversationSopTask(task.id, { note: `完成 ${task.title}` })
      message.success('任务已完成')
      await refreshTimeline(selected.id)
    } finally {
      setMutating(undefined)
    }
  }

  const generateAiSuggestion = async () => {
    if (!selected) return
    setMutating('ai-generate')
    try {
      await generateConversationAiReplySuggestion(selected.id, {
        tone: 'HELPFUL',
        intent: timeline?.messages?.[0]?.intent,
        modelKey: 'gpt-4.1-mini',
        params: { source: 'operator_workspace' },
      })
      message.success('已生成建议')
      await refreshAiSuggestions(selected.id)
    } finally {
      setMutating(undefined)
    }
  }

  const reviewAiSuggestion = async (suggestion: ConversationAiReplySuggestion, decision: 'ACCEPTED' | 'REJECTED') => {
    if (!selected) return
    setMutating(`ai-review-${suggestion.id}-${decision}`)
    try {
      await reviewConversationAiReplySuggestion(selected.id, suggestion.id, {
        decision,
        note: decision === 'ACCEPTED' ? 'operator accepted suggestion' : 'operator rejected suggestion',
      })
      message.success(decision === 'ACCEPTED' ? '已采纳建议' : '已拒绝建议')
      await refreshAiSuggestions(selected.id)
      await refreshTimeline(selected.id)
    } finally {
      setMutating(undefined)
    }
  }

  const columns: ColumnsType<ConversationWorkItem> = [
    {
      title: '工单',
      dataIndex: 'subject',
      width: 240,
      ellipsis: true,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Text strong>{workItemTitle(row)}</Text>
          <Text type="secondary">{row.userId}</Text>
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: status => {
        const view = formatWorkItemStatus(status)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      width: 100,
      render: priority => <Tag color={priorityColor(priority)}>{priority || '-'}</Tag>,
    },
    {
      title: '渠道',
      dataIndex: 'channel',
      width: 140,
      render: (_, row) => `${row.channel || '-'} / ${row.provider || '-'}`,
    },
    {
      title: '负责人',
      dataIndex: 'assignedTo',
      width: 150,
      render: (_, row) => row.assignedTo
        ? `${row.assignedTo}${row.assignedTeam ? ` / ${row.assignedTeam}` : ''}`
        : '-',
    },
    {
      title: '客户消息',
      dataIndex: 'lastCustomerMessageAt',
      width: 170,
      render: formatDateTime,
    },
    {
      title: '下次跟进',
      dataIndex: 'nextFollowUpAt',
      width: 170,
      render: formatDateTime,
    },
    {
      title: '标签',
      dataIndex: 'tags',
      width: 180,
      render: tags => tags?.length
        ? <Space size={[0, 4]} wrap>{tags.map((tag: string) => <Tag key={tag}>{tag}</Tag>)}</Space>
        : '-',
    },
    {
      title: '操作',
      width: 96,
      fixed: 'right',
      render: (_, row) => (
        <Button aria-label="详情" size="small" onClick={() => openTimeline(row)}>
          详情
        </Button>
      ),
    },
  ]

  return (
    <div style={{ minHeight: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <Title level={4} style={{ margin: 0 }}>会话工作台</Title>
        <Button aria-label="刷新" icon={<ReloadOutlined />} onClick={() => load(filters)} loading={loading}>
          刷新
        </Button>
      </div>

      <Form
        form={filterForm}
        layout="inline"
        initialValues={INITIAL_FILTERS}
        onFinish={values => load(values)}
        style={{ gap: 8 }}
      >
        <Form.Item name="status">
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 150 }}
            options={WORK_ITEM_STATUS_OPTIONS.map(value => ({
              value,
              label: formatWorkItemStatus(value).text,
            }))}
          />
        </Form.Item>
        <Form.Item name="assignedTo">
          <Input allowClear placeholder="负责人" style={{ width: 150 }} />
        </Form.Item>
        <Form.Item name="channel">
          <Select
            allowClear
            placeholder="渠道"
            style={{ width: 150 }}
            options={WORK_ITEM_CHANNEL_OPTIONS.map(value => ({ value, label: value }))}
          />
        </Form.Item>
        <Form.Item name="limit">
          <InputNumber min={1} max={100} placeholder="数量" style={{ width: 110 }} />
        </Form.Item>
        <Form.Item>
          <Button aria-label="查询" type="primary" htmlType="submit" icon={<SearchOutlined />}>
            查询
          </Button>
        </Form.Item>
      </Form>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={rows}
        loading={loading}
        pagination={false}
        scroll={{ x: 1360 }}
        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无会话工单" /> }}
      />

      <Drawer
        width={760}
        open={!!selected}
        title={selected ? workItemTitle(selected) : '客户时间线'}
        onClose={() => {
          setSelected(null)
          setTimeline(null)
        }}
        extra={selected ? (
          <Button aria-label="刷新时间线" icon={<ReloadOutlined />} onClick={() => refreshTimeline(selected.id)} loading={timelineLoading}>
            刷新
          </Button>
        ) : null}
      >
        {selected ? (
          <Space direction="vertical" size={18} style={{ width: '100%' }}>
            <Title level={5} style={{ margin: 0 }}>客户时间线</Title>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="客户" span={2}>
                {contactProfileTitle(timeline?.contactProfile)} / {selected.userId}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={formatWorkItemStatus(selected.status).color}>
                  {formatWorkItemStatus(selected.status).text}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="优先级">
                <Tag color={priorityColor(selected.priority)}>{selected.priority}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="渠道">{selected.channel} / {selected.provider}</Descriptions.Item>
              <Descriptions.Item label="会话">
                <Tag color={formatConversationStatus(timeline?.session.status ?? '').color}>
                  {timeline?.session.status ?? '-'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="负责人">{selected.assignedTo ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="团队">{selected.assignedTeam ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="最近客户消息">{formatDateTime(selected.lastCustomerMessageAt)}</Descriptions.Item>
              <Descriptions.Item label="下次跟进">{formatDateTime(selected.nextFollowUpAt)}</Descriptions.Item>
            </Descriptions>

            <Form form={assignForm} layout="vertical" onFinish={submitAssign}>
              <Space align="end" wrap>
                <Form.Item label="负责人" name="assignedTo" style={{ marginBottom: 0 }}>
                  <Input allowClear placeholder="alice" style={{ width: 160 }} />
                </Form.Item>
                <Form.Item label="团队" name="assignedTeam" style={{ marginBottom: 0 }}>
                  <Input allowClear placeholder="sales" style={{ width: 160 }} />
                </Form.Item>
                <Form.Item label="备注" name="note" style={{ marginBottom: 0 }}>
                  <Input allowClear placeholder="备注" style={{ width: 220 }} />
                </Form.Item>
                <Button
                  aria-label="指派"
                  htmlType="submit"
                  icon={<TeamOutlined />}
                  loading={mutating === 'assign'}
                >
                  指派
                </Button>
              </Space>
            </Form>

            <Form form={statusForm} layout="vertical" onFinish={submitStatus}>
              <Space align="end" wrap>
                <Form.Item label="状态" name="status" style={{ marginBottom: 0 }}>
                  <Select
                    style={{ width: 150 }}
                    options={WORK_ITEM_STATUS_OPTIONS.map(value => ({
                      value,
                      label: formatWorkItemStatus(value).text,
                    }))}
                  />
                </Form.Item>
                <Form.Item label="优先级" name="priority" style={{ marginBottom: 0 }}>
                  <Select
                    style={{ width: 140 }}
                    options={WORK_ITEM_PRIORITY_OPTIONS.map(value => ({ value, label: value }))}
                  />
                </Form.Item>
                <Form.Item label="下次跟进" name="nextFollowUpAt" style={{ marginBottom: 0 }}>
                  <Input allowClear placeholder="2026-06-07T09:30:00" style={{ width: 210 }} />
                </Form.Item>
                <Form.Item label="备注" name="note" style={{ marginBottom: 0 }}>
                  <Input allowClear placeholder="备注" style={{ width: 180 }} />
                </Form.Item>
                <Button
                  aria-label="更新状态"
                  htmlType="submit"
                  icon={<CheckCircleOutlined />}
                  loading={mutating === 'status'}
                >
                  更新状态
                </Button>
              </Space>
            </Form>

            <Form form={taskForm} layout="vertical" onFinish={submitTask}>
              <Space align="end" wrap>
                <Form.Item
                  label="任务键"
                  name="taskKey"
                  rules={[{ required: true, message: '请输入任务键' }]}
                  style={{ marginBottom: 0 }}
                >
                  <Input placeholder="book_demo" style={{ width: 150 }} />
                </Form.Item>
                <Form.Item
                  label="任务"
                  name="title"
                  rules={[{ required: true, message: '请输入任务标题' }]}
                  style={{ marginBottom: 0 }}
                >
                  <Input placeholder="Book a product demo" style={{ width: 220 }} />
                </Form.Item>
                <Form.Item label="负责人" name="assignee" style={{ marginBottom: 0 }}>
                  <Input allowClear placeholder="alice" style={{ width: 140 }} />
                </Form.Item>
                <Form.Item label="到期" name="dueAt" style={{ marginBottom: 0 }}>
                  <Input allowClear placeholder="2026-06-07T10:00:00" style={{ width: 190 }} />
                </Form.Item>
                <Button
                  aria-label="新增任务"
                  htmlType="submit"
                  icon={<PlusOutlined />}
                  loading={mutating === 'task'}
                >
                  新增任务
                </Button>
              </Space>
            </Form>

            <Divider style={{ margin: 0 }} />

            <List
              header={(
                <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                  <Text strong>AI 回复建议</Text>
                  <Space>
                    <Select
                      aria-label="建议状态"
                      size="small"
                      value={aiSuggestionStatus}
                      style={{ width: 120 }}
                      onChange={changeAiSuggestionStatus}
                      options={[
                        { value: 'DRAFT', label: '待审核' },
                        { value: 'ACCEPTED', label: '已采纳' },
                        { value: 'REJECTED', label: '已拒绝' },
                      ]}
                    />
                    <Button
                      aria-label="生成建议"
                      size="small"
                      icon={<PlusOutlined />}
                      loading={mutating === 'ai-generate'}
                      onClick={generateAiSuggestion}
                    >
                      生成建议
                    </Button>
                  </Space>
                </Space>
              )}
              loading={timelineLoading}
              dataSource={aiSuggestions}
              locale={{ emptyText: '暂无 AI 建议' }}
              renderItem={suggestion => {
                const view = formatAiReplySuggestionStatus(suggestion.status)
                return (
                  <List.Item
                    actions={[
                      <Button
                        key="accept"
                        aria-label="采纳建议"
                        size="small"
                        disabled={suggestion.status !== 'DRAFT'}
                        loading={mutating === `ai-review-${suggestion.id}-ACCEPTED`}
                        onClick={() => reviewAiSuggestion(suggestion, 'ACCEPTED')}
                      >
                        采纳建议
                      </Button>,
                      <Button
                        key="reject"
                        aria-label="拒绝建议"
                        size="small"
                        disabled={suggestion.status !== 'DRAFT'}
                        loading={mutating === `ai-review-${suggestion.id}-REJECTED`}
                        onClick={() => reviewAiSuggestion(suggestion, 'REJECTED')}
                      >
                        拒绝建议
                      </Button>,
                    ]}
                  >
                    <Space direction="vertical" size={4}>
                      <Space wrap>
                        <Tag color={view.color}>{view.text}</Tag>
                        <Text type="secondary">
                          {suggestion.modelKey ?? '-'} · 置信度 {Math.round((suggestion.confidence ?? 0) * 100)}%
                        </Text>
                      </Space>
                      <Text>{suggestion.suggestedReplyText}</Text>
                      {suggestion.riskFlags?.length ? (
                        <Space size={[0, 4]} wrap>
                          {suggestion.riskFlags.map(flag => <Tag key={flag} color="orange">{flag}</Tag>)}
                        </Space>
                      ) : null}
                    </Space>
                  </List.Item>
                )
              }}
            />

            <List
              header="消息记录"
              loading={timelineLoading}
              dataSource={timeline?.messages ?? []}
              locale={{ emptyText: '暂无消息' }}
              renderItem={item => (
                <List.Item>
                  <Space direction="vertical" size={2}>
                    <Text strong>{conversationMessageLine(item)}</Text>
                    <Text type="secondary">{formatDateTime(item.createdAt)}</Text>
                  </Space>
                </List.Item>
              )}
            />

            <List
              header="SOP 任务"
              loading={timelineLoading}
              dataSource={timeline?.tasks ?? []}
              locale={{ emptyText: '暂无任务' }}
              renderItem={task => (
                <List.Item
                  actions={[
                    <Button
                      key="complete"
                      aria-label={`完成 ${task.title}`}
                      size="small"
                      disabled={task.status === 'DONE'}
                      loading={mutating === `complete-${task.id}`}
                      onClick={() => completeTask(task)}
                    >
                      完成
                    </Button>,
                  ]}
                >
                  <Space direction="vertical" size={2}>
                    <Space wrap>
                      <Text strong>{task.title}</Text>
                      <Tag color={taskStatusColor(task.status)}>{task.status}</Tag>
                    </Space>
                    <Text type="secondary">
                      {task.taskKey} · {task.assignee ?? '-'} · {formatDateTime(task.dueAt)}
                    </Text>
                  </Space>
                </List.Item>
              )}
            />

            <List
              header="审计历史"
              loading={timelineLoading}
              dataSource={timeline?.audits ?? []}
              locale={{ emptyText: '暂无审计记录' }}
              renderItem={audit => (
                <List.Item>
                  <Text>{timelineAuditLine(audit)}</Text>
                </List.Item>
              )}
            />
          </Space>
        ) : null}
      </Drawer>
    </div>
  )
}

function cleanText(value?: string) {
  const text = value?.trim()
  return text || undefined
}
