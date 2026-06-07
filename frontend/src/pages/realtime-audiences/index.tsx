import { useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  BranchesOutlined,
  MergeCellsOutlined,
  ReloadOutlined,
  SaveOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import {
  cdpApi,
  type AudienceOverlapResult,
  type AudienceSetOperationResult,
  type AudienceSnapshotRow,
  type RealtimeAudienceEventResult,
} from '../../services/cdpApi'
import {
  formatOverlapPercent,
  formatSetOperation,
  formatSnapshotRow,
  realtimeStatusText,
} from './realtimeAudiencePresentation'

const { Text, Title } = Typography

interface EventFormValues {
  audienceId: string
  sourceEventId: string
  userId: string
  eventTime?: string
  attributesText?: string
  removeOnNoMatch?: boolean
}

interface PairFormValues {
  leftId: string
  rightId: string
}

interface SnapshotFormValues {
  audienceId: string
}

export default function RealtimeAudiencesPage() {
  const [eventForm] = Form.useForm<EventFormValues>()
  const [overlapForm] = Form.useForm<PairFormValues>()
  const [operationForm] = Form.useForm<PairFormValues>()
  const [snapshotForm] = Form.useForm<SnapshotFormValues>()
  const [eventResult, setEventResult] = useState<RealtimeAudienceEventResult | null>(null)
  const [overlapResult, setOverlapResult] = useState<AudienceOverlapResult | null>(null)
  const [operationResult, setOperationResult] = useState<AudienceSetOperationResult | null>(null)
  const [snapshots, setSnapshots] = useState<AudienceSnapshotRow[]>([])
  const [loading, setLoading] = useState<string | null>(null)

  const submitEvent = async () => {
    const values = await eventForm.validateFields()
    let attributes: Record<string, unknown>
    try {
      attributes = parseAttributes(values.attributesText)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '事件属性 JSON 不合法')
      return
    }
    setLoading('event')
    try {
      const res = await cdpApi.realtimeAudiences.processEvent(values.audienceId.trim(), {
        sourceEventId: values.sourceEventId.trim(),
        userId: values.userId.trim(),
        eventTime: values.eventTime?.trim() || undefined,
        properties: attributes,
        removeOnNoMatch: values.removeOnNoMatch,
      })
      setEventResult(res.data)
      message.success(realtimeStatusText(res.data.status))
    } finally {
      setLoading(null)
    }
  }

  const queryOverlap = async () => {
    const values = await overlapForm.validateFields()
    setLoading('overlap')
    try {
      const res = await cdpApi.realtimeAudiences.overlap(values.leftId.trim(), values.rightId.trim())
      setOverlapResult(res.data)
    } finally {
      setLoading(null)
    }
  }

  const runSetOperation = async (operation: 'merge' | 'exclude') => {
    const values = await operationForm.validateFields()
    setLoading(operation)
    try {
      const res = operation === 'merge'
        ? await cdpApi.realtimeAudiences.merge(values.leftId.trim(), values.rightId.trim())
        : await cdpApi.realtimeAudiences.exclude(values.leftId.trim(), values.rightId.trim())
      setOperationResult(res.data)
      message.success(formatSetOperation(res.data))
    } finally {
      setLoading(null)
    }
  }

  const loadSnapshots = async (audienceId?: string) => {
    const id = audienceId ?? snapshotForm.getFieldValue('audienceId')
    if (!id?.trim()) return
    setLoading('snapshots')
    try {
      const res = await cdpApi.realtimeAudiences.snapshots(id.trim())
      setSnapshots(res.data)
    } finally {
      setLoading(null)
    }
  }

  const createSnapshot = async () => {
    const values = await snapshotForm.validateFields()
    setLoading('snapshot')
    try {
      await cdpApi.realtimeAudiences.createSnapshot(values.audienceId.trim())
      message.success('快照已创建')
      await loadSnapshots(values.audienceId)
    } finally {
      setLoading(null)
    }
  }

  const snapshotColumns: ColumnsType<AudienceSnapshotRow> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '估算规模', dataIndex: 'estimatedSize', width: 110 },
    { title: '来源', dataIndex: 'snapshotSource', width: 110, render: value => <Tag>{value}</Tag> },
    { title: 'Bitmap Key', dataIndex: 'bitmapKey', ellipsis: true, render: value => value || '-' },
    { title: '创建时间', dataIndex: 'createdAt', width: 190, render: value => value || '-' },
    { title: '摘要', width: 320, render: (_, row) => <Text>{formatSnapshotRow(row)}</Text> },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>实时人群</Title>
        <Text type="secondary">处理事件、查询重叠、执行受保护集合操作，并查看人群快照趋势。</Text>
      </div>

      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Card size="small" title="事件处理">
          <Form
            form={eventForm}
            layout="vertical"
            initialValues={{
              attributesText: JSON.stringify({ event: 'Paid' }, null, 2),
              removeOnNoMatch: true,
            }}
          >
            <Space size={12} wrap align="start">
              <Form.Item name="audienceId" label="人群 ID" rules={[{ required: true, message: '请输入人群 ID' }]}>
                <Input style={{ width: 140 }} placeholder="10" />
              </Form.Item>
              <Form.Item name="sourceEventId" label="事件 ID" rules={[{ required: true, message: '请输入事件 ID' }]}>
                <Input style={{ width: 220 }} placeholder="evt-1" />
              </Form.Item>
              <Form.Item name="userId" label="用户 ID" rules={[{ required: true, message: '请输入用户 ID' }]}>
                <Input style={{ width: 180 }} placeholder="u1" />
              </Form.Item>
              <Form.Item name="eventTime" label="事件时间">
                <Input style={{ width: 240 }} placeholder="2026-06-03T00:00:00Z" />
              </Form.Item>
              <Form.Item name="removeOnNoMatch" label="未命中移除" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Space>
            <Form.Item name="attributesText" label="事件属性 JSON" rules={[{ required: true, message: '请输入事件属性 JSON' }]}>
              <Input.TextArea autoSize={{ minRows: 4 }} style={{ fontFamily: 'monospace' }} />
            </Form.Item>
            <Button
              type="primary"
              icon={<ThunderboltOutlined />}
              loading={loading === 'event'}
              onClick={submitEvent}
            >
              处理事件
            </Button>
          </Form>
          {eventResult && (
            <Alert
              style={{ marginTop: 12 }}
              type={eventResult.status === 'DUPLICATED' ? 'warning' : 'success'}
              showIcon
              message={`状态：${realtimeStatusText(eventResult.status)}`}
              description={`operation ${eventResult.operation ?? '-'}，user ${eventResult.userId ?? '-'}`}
            />
          )}
        </Card>

        <Card size="small" title="Overlap 查询">
          <Form form={overlapForm} layout="inline">
            <Form.Item name="leftId" label="左侧人群" rules={[{ required: true, message: '请输入左侧人群 ID' }]}>
              <Input style={{ width: 160 }} placeholder="10" />
            </Form.Item>
            <Form.Item name="rightId" label="右侧人群" rules={[{ required: true, message: '请输入右侧人群 ID' }]}>
              <Input style={{ width: 160 }} placeholder="11" />
            </Form.Item>
            <Form.Item>
              <Button icon={<BranchesOutlined />} loading={loading === 'overlap'} onClick={queryOverlap}>查询</Button>
            </Form.Item>
          </Form>
          {overlapResult && (
            <Descriptions size="small" bordered column={{ xs: 1, sm: 2, md: 4 }} style={{ marginTop: 12 }}>
              <Descriptions.Item label="交集">{overlapResult.intersectionCount}</Descriptions.Item>
              <Descriptions.Item label="左侧占比">{formatOverlapPercent(overlapResult.leftPercentage)}</Descriptions.Item>
              <Descriptions.Item label="右侧占比">{formatOverlapPercent(overlapResult.rightPercentage)}</Descriptions.Item>
              <Descriptions.Item label="规模">{overlapResult.leftCount} / {overlapResult.rightCount}</Descriptions.Item>
            </Descriptions>
          )}
        </Card>

        <Card size="small" title="守卫集合操作">
          <Form form={operationForm} layout="inline">
            <Form.Item name="leftId" label="基础/左侧人群" rules={[{ required: true, message: '请输入人群 ID' }]}>
              <Input style={{ width: 160 }} placeholder="10" />
            </Form.Item>
            <Form.Item name="rightId" label="合并/排除人群" rules={[{ required: true, message: '请输入人群 ID' }]}>
              <Input style={{ width: 160 }} placeholder="11" />
            </Form.Item>
            <Form.Item>
              <Space>
                <Button icon={<MergeCellsOutlined />} loading={loading === 'merge'} onClick={() => runSetOperation('merge')}>Merge</Button>
                <Button danger loading={loading === 'exclude'} onClick={() => runSetOperation('exclude')}>Exclude</Button>
              </Space>
            </Form.Item>
          </Form>
          {operationResult && (
            <Alert
              style={{ marginTop: 12 }}
              type={operationResult.status === 'BLOCKED' ? 'error' : 'success'}
              showIcon
              message={formatSetOperation(operationResult)}
            />
          )}
        </Card>

        <Card size="small" title="快照趋势">
          <Form form={snapshotForm} layout="inline">
            <Form.Item name="audienceId" label="人群 ID" rules={[{ required: true, message: '请输入人群 ID' }]}>
              <Input style={{ width: 160 }} placeholder="10" />
            </Form.Item>
            <Form.Item>
              <Space>
                <Button icon={<ReloadOutlined />} loading={loading === 'snapshots'} onClick={() => loadSnapshots()}>刷新列表</Button>
                <Button type="primary" icon={<SaveOutlined />} loading={loading === 'snapshot'} onClick={createSnapshot}>创建快照</Button>
              </Space>
            </Form.Item>
          </Form>
          <Table
            rowKey="id"
            size="small"
            dataSource={snapshots}
            columns={snapshotColumns}
            loading={loading === 'snapshots' || loading === 'snapshot'}
            pagination={{ pageSize: 8, hideOnSinglePage: true }}
            scroll={{ x: 980 }}
            style={{ marginTop: 12 }}
          />
        </Card>
      </Space>
    </div>
  )
}

function parseAttributes(value?: string): Record<string, unknown> {
  if (!value?.trim()) return {}
  const parsed = JSON.parse(value)
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('事件属性必须是 JSON object')
  }
  return parsed as Record<string, unknown>
}
