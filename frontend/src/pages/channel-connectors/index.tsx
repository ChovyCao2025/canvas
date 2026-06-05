import { useEffect, useState } from 'react'
import { Button, Form, Input, Select, Space, Table, Tag, Typography, message } from 'antd'
import { ReloadOutlined, SafetyCertificateOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  channelConnectorApi,
  type ConnectorRow,
  type DedupeRecordRow,
  type FallbackDecisionRow,
  type FallbackPolicyPayload,
  type LimitRow,
} from '../../services/channelConnectorApi'
import {
  connectorHealthBadge,
  formatDecisionRow,
  formatLimitRow,
} from './channelConnectorPresentation'

const { Title, Text } = Typography

export default function ChannelConnectorsPage() {
  const [connectors, setConnectors] = useState<ConnectorRow[]>([])
  const [limits, setLimits] = useState<LimitRow[]>([])
  const [decisions, setDecisions] = useState<FallbackDecisionRow[]>([])
  const [dedupeRecords, setDedupeRecords] = useState<DedupeRecordRow[]>([])
  const [loading, setLoading] = useState(false)
  const [savingId, setSavingId] = useState<number | null>(null)
  const [validating, setValidating] = useState(false)
  const [form] = Form.useForm<FallbackPolicyPayload>()

  const fetchAll = async () => {
    setLoading(true)
    try {
      const [connectorRes, limitRes, decisionRes, dedupeRes] = await Promise.all([
        channelConnectorApi.list(),
        channelConnectorApi.limits(),
        channelConnectorApi.decisions(),
        channelConnectorApi.dedupeRecords(),
      ])
      setConnectors(connectorRes.data)
      setLimits(limitRes.data)
      setDecisions(decisionRes.data)
      setDedupeRecords(dedupeRes.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchAll()
  }, [])

  const handleModeChange = async (record: ConnectorRow, mode: string) => {
    setSavingId(record.id)
    try {
      await channelConnectorApi.updateMode(record.id, mode, mode === 'DISABLED' ? 'operator disabled' : '')
      message.success('Mode updated')
      fetchAll()
    } finally {
      setSavingId(null)
    }
  }

  const handleHealthTest = async (record: ConnectorRow) => {
    setSavingId(record.id)
    try {
      const result = await channelConnectorApi.testHealth(record.id)
      message.success(`Health check: ${result.data.status}`)
      fetchAll()
    } finally {
      setSavingId(null)
    }
  }

  const handleValidateFallback = async () => {
    const values = await form.validateFields()
    setValidating(true)
    try {
      const result = await channelConnectorApi.validateFallback(values)
      if (result.data.valid) {
        message.success('Fallback policy is valid')
      } else {
        message.error(result.data.message)
      }
    } finally {
      setValidating(false)
    }
  }

  const connectorColumns: ColumnsType<ConnectorRow> = [
    { title: 'Connector', dataIndex: 'connectorKey', width: 180 },
    { title: 'Channel', dataIndex: 'channel', width: 110 },
    { title: 'Provider', dataIndex: 'provider', width: 130 },
    {
      title: 'Mode',
      dataIndex: 'mode',
      width: 150,
      render: (_, record) => (
        <Select
          size="small"
          value={record.mode}
          style={{ width: 120 }}
          loading={savingId === record.id}
          options={[
            { value: 'REAL', label: 'Real' },
            { value: 'SANDBOX', label: 'Sandbox' },
            { value: 'DISABLED', label: 'Disabled' },
          ]}
          onChange={value => handleModeChange(record, value)}
        />
      ),
    },
    {
      title: 'Health',
      dataIndex: 'healthStatus',
      width: 120,
      render: value => {
        const badge = connectorHealthBadge(value)
        return <Tag color={badge.color}>{badge.text}</Tag>
      },
    },
    { title: 'Message', dataIndex: 'healthMessage', ellipsis: true },
    {
      title: 'Actions',
      width: 90,
      render: (_, record) => (
        <Button
          size="small"
          icon={<SafetyCertificateOutlined />}
          loading={savingId === record.id}
          onClick={() => handleHealthTest(record)}
        />
      ),
    },
  ]

  const limitColumns: ColumnsType<LimitRow> = [
    { title: 'Route', render: (_, record) => formatLimitRow(record) },
    { title: 'Fail Closed', dataIndex: 'failClosed', width: 120, render: value => <Tag color={value ? 'red' : 'default'}>{value ? 'Yes' : 'No'}</Tag> },
    { title: 'Updated', dataIndex: 'updatedAt', width: 190 },
  ]

  const decisionColumns: ColumnsType<FallbackDecisionRow> = [
    { title: 'Decision', render: (_, record) => formatDecisionRow(record) },
    { title: 'Provider', render: (_, record) => `${record.originalProvider ?? '-'} -> ${record.finalProvider ?? '-'}`, width: 180 },
    { title: 'Created', dataIndex: 'createdAt', width: 190 },
  ]

  const dedupeColumns: ColumnsType<DedupeRecordRow> = [
    { title: 'Group', dataIndex: 'dedupeGroup', width: 160 },
    { title: 'Channel', dataIndex: 'channel', width: 100 },
    { title: 'User', dataIndex: 'userId', width: 160 },
    { title: 'Content Hash', dataIndex: 'contentHash', ellipsis: true },
    { title: 'Expires', dataIndex: 'expiresAt', width: 190 },
  ]

  return (
    <div style={{ display: 'grid', gap: 18 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
        <Title level={4} style={{ margin: 0 }}>Channel Connectors</Title>
        <Button icon={<ReloadOutlined />} onClick={fetchAll} loading={loading}>Refresh</Button>
      </div>

      <Table
        rowKey="id"
        dataSource={connectors}
        columns={connectorColumns}
        loading={loading}
        pagination={false}
      />

      <div>
        <Title level={5}>Fallback Validation</Title>
        <Form form={form} layout="inline" onFinish={handleValidateFallback}>
          <Form.Item name="channel" rules={[{ required: true, message: 'Required' }]}>
            <Input placeholder="Channel" />
          </Form.Item>
          <Form.Item name="provider" rules={[{ required: true, message: 'Required' }]}>
            <Input placeholder="Provider" />
          </Form.Item>
          <Text style={{ lineHeight: '32px' }}>to</Text>
          <Form.Item name="fallbackChannel" rules={[{ required: true, message: 'Required' }]}>
            <Input placeholder="Fallback channel" />
          </Form.Item>
          <Form.Item name="fallbackProvider" rules={[{ required: true, message: 'Required' }]}>
            <Input placeholder="Fallback provider" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={validating}>Validate</Button>
          </Form.Item>
        </Form>
      </div>

      <Space direction="vertical" size={18} style={{ width: '100%' }}>
        <Table rowKey={record => `${record.channel}:${record.provider}:${record.operation}`} dataSource={limits} columns={limitColumns} pagination={false} size="small" />
        <Table rowKey={record => `${record.originalChannel}:${record.finalChannel}:${record.createdAt}`} dataSource={decisions} columns={decisionColumns} pagination={{ pageSize: 8 }} size="small" />
        <Table rowKey={record => `${record.dedupeGroup}:${record.contentHash}:${record.userId}`} dataSource={dedupeRecords} columns={dedupeColumns} pagination={{ pageSize: 8 }} size="small" />
      </Space>
    </div>
  )
}
