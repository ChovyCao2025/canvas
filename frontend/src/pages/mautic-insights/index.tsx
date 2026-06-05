import { useEffect, useState } from 'react'
import {
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Form,
  Input,
  InputNumber,
  List,
  Progress,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  mauticInsightsApi,
  type AudienceMembershipReport,
  type ChannelCandidate,
  type ChannelPreferenceReport,
  type FrequencyTemplate,
  type HealthCheck,
  type JourneyPathReport,
  type JourneyStep,
  type PublishHealthReport,
  type SuppressionRecord,
  type SuppressionTimeline,
} from '../../services/mauticInsightsApi'
import {
  audienceMembershipStatusView,
  channelCandidateView,
  formatDateTime,
  formatWindowSeconds,
  healthCheckView,
  healthScoreView,
  journeyStepStatusView,
  suppressionStateView,
} from './mauticInsightsPresentation'

const { Title, Text } = Typography

interface InsightQuery {
  userId?: string
  audienceId?: number
  executionId?: string
  canvasId?: number
  preferredChannel?: string
}

const CHANNEL_OPTIONS = ['SMS', 'EMAIL', 'PUSH', 'WECHAT', 'IN_APP'].map(value => ({ value, label: value }))

function trimText(value?: string) {
  const text = value?.trim()
  return text || undefined
}

export default function MauticInsightsPage() {
  const [form] = Form.useForm<InsightQuery>()
  const [audience, setAudience] = useState<AudienceMembershipReport | null>(null)
  const [journey, setJourney] = useState<JourneyPathReport | null>(null)
  const [channelPreference, setChannelPreference] = useState<ChannelPreferenceReport | null>(null)
  const [suppressionTimeline, setSuppressionTimeline] = useState<SuppressionTimeline | null>(null)
  const [publishHealth, setPublishHealth] = useState<PublishHealthReport | null>(null)
  const [templates, setTemplates] = useState<FrequencyTemplate[]>([])
  const [loading, setLoading] = useState(false)
  const [templatesLoading, setTemplatesLoading] = useState(false)

  const loadTemplates = async () => {
    setTemplatesLoading(true)
    try {
      const res = await mauticInsightsApi.frequencyTemplates()
      setTemplates(res.data ?? [])
    } finally {
      setTemplatesLoading(false)
    }
  }

  useEffect(() => {
    loadTemplates()
  }, [])

  const loadInsights = async (values: InsightQuery = form.getFieldsValue()) => {
    const userId = trimText(values.userId)
    const executionId = trimText(values.executionId)
    const preferredChannel = trimText(values.preferredChannel)
    const audienceId = values.audienceId
    const canvasId = values.canvasId
    const tasks: Array<Promise<unknown>> = []

    setLoading(true)
    try {
      if (userId && audienceId) {
        tasks.push(mauticInsightsApi.explainAudienceMembership({ userId, audienceId }).then(res => setAudience(res.data)))
      } else {
        setAudience(null)
      }

      if (executionId) {
        tasks.push(mauticInsightsApi.explainJourneyPath(executionId).then(res => setJourney(res.data)))
      } else {
        setJourney(null)
      }

      if (userId) {
        tasks.push(mauticInsightsApi.resolveChannelPreference({ userId, preferredChannel }).then(res => setChannelPreference(res.data)))
        tasks.push(mauticInsightsApi.suppressionTimeline(userId).then(res => setSuppressionTimeline(res.data)))
      } else {
        setChannelPreference(null)
        setSuppressionTimeline(null)
      }

      if (canvasId) {
        tasks.push(mauticInsightsApi.publishHealth(canvasId).then(res => setPublishHealth(res.data)))
      } else {
        setPublishHealth(null)
      }

      await Promise.all(tasks)
    } catch (error) {
      message.error((error as Error).message || '解释数据加载失败')
    } finally {
      setLoading(false)
    }
  }

  const audienceStatus = audienceMembershipStatusView(audience?.status)
  const healthScore = healthScoreView(publishHealth?.score ?? 0)

  const journeyColumns: ColumnsType<JourneyStep> = [
    {
      title: '节点',
      render: (_, row) => (
        <Space direction="vertical" size={0}>
          <Text>{row.nodeName || row.nodeId || '-'}</Text>
          <Text type="secondary">{row.nodeId || '-'}</Text>
        </Space>
      ),
    },
    { title: '类型', dataIndex: 'nodeType', width: 130, render: value => value || '-' },
    {
      title: '状态',
      dataIndex: 'statusLabel',
      width: 110,
      render: value => {
        const view = journeyStepStatusView(value)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    { title: '原因', dataIndex: 'reason', ellipsis: true },
    { title: '错误', dataIndex: 'errorMessage', ellipsis: true, render: value => value || '-' },
    { title: '耗时', dataIndex: 'durationMs', width: 100, render: value => value == null ? '-' : `${value} ms` },
  ]

  const channelColumns: ColumnsType<ChannelCandidate> = [
    { title: '渠道', dataIndex: 'channel', width: 110 },
    {
      title: '状态',
      dataIndex: 'state',
      width: 110,
      render: value => {
        const view = channelCandidateView(value)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    { title: '原因', dataIndex: 'reason' },
  ]

  const suppressionColumns: ColumnsType<SuppressionRecord> = [
    { title: '渠道', dataIndex: 'channel', width: 100 },
    {
      title: '状态',
      dataIndex: 'state',
      width: 110,
      render: value => {
        const view = suppressionStateView(value)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    { title: '原因', dataIndex: 'reason', render: value => value || '-' },
    { title: '创建时间', dataIndex: 'createdAt', width: 170, render: formatDateTime },
    { title: '过期时间', dataIndex: 'expiresAt', width: 170, render: formatDateTime },
  ]

  const templateColumns: ColumnsType<FrequencyTemplate> = [
    { title: '模板', dataIndex: 'templateKey', width: 210 },
    { title: '范围', dataIndex: 'scope', width: 110 },
    { title: '上限', dataIndex: 'maxCount', width: 80 },
    { title: '窗口', dataIndex: 'windowSeconds', width: 110, render: formatWindowSeconds },
    { title: '说明', dataIndex: 'description' },
  ]

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>营销解释台</Title>
        </div>
        <Space wrap>
          <Button icon={<ReloadOutlined />} onClick={() => loadInsights()} loading={loading}>刷新</Button>
          <Button icon={<ReloadOutlined />} onClick={loadTemplates} loading={templatesLoading}>刷新模板</Button>
        </Space>
      </div>

      <Card>
        <Form
          form={form}
          layout="inline"
          initialValues={{ preferredChannel: 'SMS' }}
          onFinish={loadInsights}
          style={{ gap: 8 }}
        >
          <Form.Item name="userId">
            <Input allowClear placeholder="用户 ID" style={{ width: 190 }} />
          </Form.Item>
          <Form.Item name="audienceId">
            <InputNumber min={1} placeholder="人群 ID" style={{ width: 120 }} />
          </Form.Item>
          <Form.Item name="executionId">
            <Input allowClear placeholder="执行 ID" style={{ width: 190 }} />
          </Form.Item>
          <Form.Item name="canvasId">
            <InputNumber min={1} placeholder="画布 ID" style={{ width: 120 }} />
          </Form.Item>
          <Form.Item name="preferredChannel">
            <Select allowClear placeholder="偏好渠道" style={{ width: 130 }} options={CHANNEL_OPTIONS} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>查询</Button>
          </Form.Item>
        </Form>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={12}>
          <Card title="人群成员解释" loading={loading}>
            {audience ? (
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <Space wrap>
                  <Tag color={audienceStatus.color}>{audienceStatus.text}</Tag>
                  <Text strong>{audience.audienceName || `Audience #${audience.audienceId}`}</Text>
                </Space>
                <Descriptions size="small" column={2}>
                  <Descriptions.Item label="统计状态">{audience.statStatus || '-'}</Descriptions.Item>
                  <Descriptions.Item label="估算人数">{audience.estimatedSize ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="最近运行">{audience.latestRunStatus || '-'}</Descriptions.Item>
                  <Descriptions.Item label="用户">{audience.userId}</Descriptions.Item>
                </Descriptions>
                <List
                  size="small"
                  dataSource={audience.evidence ?? []}
                  locale={{ emptyText: '暂无证据' }}
                  renderItem={item => <List.Item>{item}</List.Item>}
                />
              </Space>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无人群解释" />
            )}
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card title="渠道偏好解析" loading={loading}>
            {channelPreference ? (
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <Space wrap>
                  <Text>推荐</Text>
                  <Tag color={channelPreference.recommendedChannel ? 'green' : 'default'}>
                    {channelPreference.recommendedChannel || '无可用渠道'}
                  </Tag>
                  <Text>备选</Text>
                  <Tag color={channelPreference.fallbackChannel ? 'blue' : 'default'}>
                    {channelPreference.fallbackChannel || '-'}
                  </Tag>
                </Space>
                <Table
                  rowKey="channel"
                  size="small"
                  pagination={false}
                  columns={channelColumns}
                  dataSource={channelPreference.channels ?? []}
                />
              </Space>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无渠道解析" />
            )}
          </Card>
        </Col>

        <Col xs={24}>
          <Card title="旅程路径解释" loading={loading}>
            {journey ? (
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <Space wrap>
                  <Tag color="green">成功 {journey.successCount}</Tag>
                  <Tag color="red">失败 {journey.failedCount}</Tag>
                  <Tag color="orange">跳过 {journey.skippedCount}</Tag>
                </Space>
                <Table
                  rowKey={(row, index) => `${row.nodeId || 'node'}:${index}`}
                  size="small"
                  columns={journeyColumns}
                  dataSource={journey.steps ?? []}
                  pagination={{ pageSize: 8 }}
                  scroll={{ x: 960 }}
                />
              </Space>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无旅程路径" />
            )}
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card title="抑制时间线" loading={loading}>
            {suppressionTimeline ? (
              <Table
                rowKey="id"
                size="small"
                columns={suppressionColumns}
                dataSource={suppressionTimeline.records ?? []}
                pagination={{ pageSize: 5 }}
                scroll={{ x: 740 }}
              />
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无抑制记录" />
            )}
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card title="发布健康检查" loading={loading}>
            {publishHealth ? (
              <Space direction="vertical" size={14} style={{ width: '100%' }}>
                <Space align="center" size={16} wrap>
                  <Progress
                    type="circle"
                    width={72}
                    percent={publishHealth.score}
                    strokeColor={healthScore.color}
                  />
                  <Space direction="vertical" size={4}>
                    <Text strong>{publishHealth.canvasName || `Canvas #${publishHealth.canvasId}`}</Text>
                    <Tag color={healthScore.color}>{healthScore.text}</Tag>
                  </Space>
                </Space>
                <List
                  size="small"
                  dataSource={publishHealth.checks ?? []}
                  renderItem={(check: HealthCheck) => {
                    const view = healthCheckView(check.passed)
                    return (
                      <List.Item>
                        <Space>
                          <Tag color={view.color}>{view.text}</Tag>
                          <Text>{check.message}</Text>
                          <Text type="secondary">{check.checkKey}</Text>
                        </Space>
                      </List.Item>
                    )
                  }}
                />
              </Space>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无健康检查" />
            )}
          </Card>
        </Col>

        <Col xs={24}>
          <Card title="频控模板">
            <Table
              rowKey="templateKey"
              size="small"
              loading={templatesLoading}
              columns={templateColumns}
              dataSource={templates}
              pagination={false}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
