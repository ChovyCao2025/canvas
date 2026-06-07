import { useEffect, useState } from 'react'
import { Button, Drawer, Empty, Form, Input, InputNumber, List, Select, Space, Table, Tag, Typography } from 'antd'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'

import {
  listConversationMessages,
  listConversationSessions,
  type ConversationMessage,
  type ConversationSession,
  type ConversationSessionParams,
} from '../../services/conversationCoreApi'
import {
  conversationMessageLine,
  formatConversationStatus,
  formatDateTime,
} from './conversationCorePresentation'

const { Title, Text } = Typography

const CHANNEL_OPTIONS = ['WEB_CHAT', 'WHATSAPP', 'SOCIAL_DM', 'RCS', 'SANDBOX']
const INITIAL_FILTERS: ConversationSessionParams = { limit: 25 }

export default function ConversationCoreInspectionPanel() {
  const [form] = Form.useForm<ConversationSessionParams>()
  const [sessions, setSessions] = useState<ConversationSession[]>([])
  const [messages, setMessages] = useState<ConversationMessage[]>([])
  const [filters, setFilters] = useState<ConversationSessionParams>(INITIAL_FILTERS)
  const [selected, setSelected] = useState<ConversationSession | null>(null)
  const [loading, setLoading] = useState(false)
  const [messageLoading, setMessageLoading] = useState(false)

  const loadSessions = async (nextFilters = filters) => {
    const params = normalizeSessionFilters(nextFilters)
    setLoading(true)
    try {
      const res = await listConversationSessions(params)
      setSessions(res.data)
      setFilters(params)
      form.setFieldsValue(params)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadSessions(INITIAL_FILTERS)
  }, [])

  const openMessages = async (session: ConversationSession) => {
    setSelected(session)
    setMessages([])
    setMessageLoading(true)
    try {
      const res = await listConversationMessages(session.id, { limit: 50 })
      setMessages(res.data)
    } finally {
      setMessageLoading(false)
    }
  }

  const columns: ColumnsType<ConversationSession> = [
    {
      title: '用户',
      dataIndex: 'userId',
      width: 220,
      render: (_, row) => (
        <Space direction="vertical" size={2}>
          <Text strong>{row.userId}</Text>
          <Text type="secondary">{row.channel} / {row.provider}</Text>
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: status => {
        const view = formatConversationStatus(status)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    {
      title: '轮次',
      dataIndex: 'turnCount',
      width: 90,
      render: value => value ?? 0,
    },
    {
      title: '最近消息',
      dataIndex: 'lastMessageAt',
      width: 180,
      render: formatDateTime,
    },
    {
      title: '操作',
      width: 110,
      render: (_, row) => (
        <Button size="small" aria-label="查看消息" onClick={() => openMessages(row)}>
          查看消息
        </Button>
      ),
    },
  ]

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <Title level={4} style={{ margin: 0 }}>会话检查</Title>
        <Button aria-label="刷新会话" icon={<ReloadOutlined />} loading={loading} onClick={() => loadSessions(filters)}>
          刷新
        </Button>
      </div>

      <Form form={form} layout="inline" initialValues={INITIAL_FILTERS} onFinish={values => loadSessions(values)} style={{ gap: 8 }}>
        <Form.Item name="userId">
          <Input allowClear placeholder="用户" style={{ width: 180 }} />
        </Form.Item>
        <Form.Item name="channel">
          <Select
            allowClear
            placeholder="渠道"
            style={{ width: 150 }}
            options={CHANNEL_OPTIONS.map(value => ({ value, label: value }))}
          />
        </Form.Item>
        <Form.Item name="limit">
          <InputNumber min={1} max={100} placeholder="数量" style={{ width: 110 }} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" aria-label="查询会话" icon={<SearchOutlined />}>
            查询
          </Button>
        </Form.Item>
      </Form>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={sessions}
        loading={loading}
        pagination={false}
        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无会话" /> }}
      />

      <Drawer
        width={640}
        open={!!selected}
        title={selected ? `${selected.userId} · ${selected.channel}` : '消息'}
        onClose={() => {
          setSelected(null)
          setMessages([])
        }}
      >
        <List
          loading={messageLoading}
          dataSource={messages}
          locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无消息" /> }}
          renderItem={item => (
            <List.Item>
              <Space direction="vertical" size={2}>
                <Text>{conversationMessageLine(item)}</Text>
                <Text type="secondary">{formatDateTime(item.createdAt)}</Text>
              </Space>
            </List.Item>
          )}
        />
      </Drawer>
    </div>
  )
}

function normalizeSessionFilters(filters: ConversationSessionParams = {}): ConversationSessionParams {
  const normalized: ConversationSessionParams = {
    limit: Math.max(1, Math.min(filters.limit ?? 25, 100)),
  }
  const userId = filters.userId?.trim()
  if (userId) normalized.userId = userId
  const channel = filters.channel?.trim()
  if (channel) normalized.channel = channel.toUpperCase()
  return normalized
}
