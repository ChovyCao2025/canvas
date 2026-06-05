import { useState } from 'react'
import {
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Form,
  Input,
  Row,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { ReloadOutlined, SaveOutlined, SearchOutlined, StopOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  marketingPreferencesApi,
  type ChannelRow,
  type ConsentRow,
  type PreferenceReport,
  type SuppressionRow,
} from '../../services/marketingPreferencesApi'
import {
  booleanStatusView,
  channelReachabilityView,
  consentStatusView,
  formatPreferenceDateTime,
  formatPreferenceSummary,
  suppressionStateView,
} from './marketingPreferencesPresentation'

const { Title, Text } = Typography

const CHANNEL_OPTIONS = ['EMAIL', 'SMS', 'PUSH', 'WECHAT', 'IN_APP', 'ALL'].map(value => ({ value, label: value }))

interface QueryForm {
  userId: string
}

interface ConsentForm {
  channel: string
  consentStatus: string
  source?: string
}

interface ChannelForm {
  channel: string
  address?: string
  enabled?: boolean
  verified?: boolean
  metadata?: string
}

interface SuppressionForm {
  channel: string
  reason?: string
  active?: boolean
  expiresAt?: string
}

function trimText(value?: string) {
  const text = value?.trim()
  return text || undefined
}

export default function MarketingPreferencesPage() {
  const [queryForm] = Form.useForm<QueryForm>()
  const [consentForm] = Form.useForm<ConsentForm>()
  const [channelForm] = Form.useForm<ChannelForm>()
  const [suppressionForm] = Form.useForm<SuppressionForm>()
  const [report, setReport] = useState<PreferenceReport | null>(null)
  const [currentUserId, setCurrentUserId] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [deactivatingId, setDeactivatingId] = useState<number | null>(null)

  const loadReport = async (userId: string = currentUserId) => {
    const normalizedUserId = trimText(userId)
    if (!normalizedUserId) return
    setLoading(true)
    try {
      const response = await marketingPreferencesApi.report(normalizedUserId)
      setCurrentUserId(normalizedUserId)
      setReport(response.data)
    } catch (error) {
      message.error((error as Error).message || '偏好数据加载失败')
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = async (values: QueryForm) => {
    await loadReport(values.userId)
  }

  const reloadAfterSave = async () => {
    if (currentUserId) {
      await loadReport(currentUserId)
    }
  }

  const handleConsentSave = async (values: ConsentForm) => {
    if (!currentUserId) return
    setSaving(true)
    try {
      await marketingPreferencesApi.updateConsent(currentUserId, values.channel, {
        consentStatus: values.consentStatus,
        source: trimText(values.source),
      })
      message.success('同意状态已更新')
      await reloadAfterSave()
    } finally {
      setSaving(false)
    }
  }

  const handleChannelSave = async (values: ChannelForm) => {
    if (!currentUserId) return
    setSaving(true)
    try {
      await marketingPreferencesApi.updateChannel(currentUserId, values.channel, {
        address: trimText(values.address),
        enabled: values.enabled,
        verified: values.verified,
        metadata: trimText(values.metadata),
      })
      message.success('渠道信息已更新')
      await reloadAfterSave()
    } finally {
      setSaving(false)
    }
  }

  const handleSuppressionSave = async (values: SuppressionForm) => {
    if (!currentUserId) return
    setSaving(true)
    try {
      await marketingPreferencesApi.addSuppression(currentUserId, {
        channel: values.channel,
        reason: trimText(values.reason),
        active: values.active,
        expiresAt: trimText(values.expiresAt),
      })
      message.success('抑制记录已新增')
      suppressionForm.resetFields()
      suppressionForm.setFieldsValue({ channel: 'ALL', active: true })
      await reloadAfterSave()
    } finally {
      setSaving(false)
    }
  }

  const handleDeactivate = async (row: SuppressionRow) => {
    setDeactivatingId(row.id)
    try {
      await marketingPreferencesApi.deactivateSuppression(row.id)
      message.success('抑制记录已停用')
      await reloadAfterSave()
    } finally {
      setDeactivatingId(null)
    }
  }

  const consentColumns: ColumnsType<ConsentRow> = [
    { title: '渠道', dataIndex: 'channel', width: 110 },
    {
      title: '状态',
      dataIndex: 'consentStatus',
      width: 120,
      render: value => {
        const view = consentStatusView(value)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    { title: '来源', dataIndex: 'source', render: value => value || '-' },
    { title: '更新时间', dataIndex: 'updatedAt', width: 180, render: formatPreferenceDateTime },
  ]

  const channelColumns: ColumnsType<ChannelRow> = [
    { title: '渠道', dataIndex: 'channel', width: 110 },
    { title: '地址', dataIndex: 'address', ellipsis: true, render: value => value || '-' },
    {
      title: '启用',
      dataIndex: 'enabled',
      width: 90,
      render: value => {
        const view = booleanStatusView(value, '启用', '关闭')
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    {
      title: '验证',
      dataIndex: 'verified',
      width: 90,
      render: value => {
        const view = booleanStatusView(value, '已验证', '未验证')
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    {
      title: '可达',
      width: 100,
      render: (_, row) => {
        const view = channelReachabilityView(row)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    { title: '扩展信息', dataIndex: 'metadata', ellipsis: true, render: value => value || '-' },
    { title: '更新时间', dataIndex: 'updatedAt', width: 180, render: formatPreferenceDateTime },
  ]

  const suppressionColumns: ColumnsType<SuppressionRow> = [
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
    { title: '过期时间', dataIndex: 'expiresAt', width: 180, render: formatPreferenceDateTime },
    { title: '创建时间', dataIndex: 'createdAt', width: 180, render: formatPreferenceDateTime },
    {
      title: '操作',
      width: 110,
      render: (_, row) => (
        <Button
          size="small"
          icon={<StopOutlined />}
          disabled={row.state !== 'ACTIVE'}
          loading={deactivatingId === row.id}
          onClick={() => handleDeactivate(row)}
        >
          停用
        </Button>
      ),
    },
  ]

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>偏好中心</Title>
          <Text type="secondary">{formatPreferenceSummary(report?.summary)}</Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={() => loadReport()} loading={loading} disabled={!currentUserId}>
          刷新
        </Button>
      </div>

      <Card>
        <Form form={queryForm} layout="inline" onFinish={handleSearch} style={{ gap: 8 }}>
          <Form.Item name="userId" rules={[{ required: true, message: '请输入用户 ID' }]}>
            <Input allowClear placeholder="用户 ID" style={{ width: 260 }} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>查询</Button>
          </Form.Item>
        </Form>
      </Card>

      {report ? (
        <>
          <Card title="汇总">
            <Descriptions size="small" column={{ xs: 1, sm: 2, lg: 5 }}>
              <Descriptions.Item label="用户">{report.userId}</Descriptions.Item>
              <Descriptions.Item label="渠道数">{report.summary.totalChannels}</Descriptions.Item>
              <Descriptions.Item label="可达渠道">{report.summary.reachableChannelCount}</Descriptions.Item>
              <Descriptions.Item label="同意">{report.summary.optInCount}</Descriptions.Item>
              <Descriptions.Item label="退订">{report.summary.optOutCount}</Descriptions.Item>
              <Descriptions.Item label="生效抑制">{report.summary.activeSuppressionCount}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={8}>
              <Card title="同意状态">
                <Form
                  form={consentForm}
                  layout="vertical"
                  initialValues={{ channel: 'EMAIL', consentStatus: 'OPT_IN', source: 'operator' }}
                  onFinish={handleConsentSave}
                >
                  <Form.Item name="channel" label="渠道" rules={[{ required: true, message: '请选择渠道' }]}>
                    <Select options={CHANNEL_OPTIONS.filter(option => option.value !== 'ALL')} />
                  </Form.Item>
                  <Form.Item name="consentStatus" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
                    <Select options={[
                      { value: 'OPT_IN', label: '已同意' },
                      { value: 'OPT_OUT', label: '已退订' },
                    ]} />
                  </Form.Item>
                  <Form.Item name="source" label="来源">
                    <Input placeholder="operator" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>保存同意</Button>
                </Form>
              </Card>
            </Col>
            <Col xs={24} xl={8}>
              <Card title="渠道地址">
                <Form
                  form={channelForm}
                  layout="vertical"
                  initialValues={{ channel: 'EMAIL', enabled: true, verified: false }}
                  onFinish={handleChannelSave}
                >
                  <Form.Item name="channel" label="渠道" rules={[{ required: true, message: '请选择渠道' }]}>
                    <Select options={CHANNEL_OPTIONS.filter(option => option.value !== 'ALL')} />
                  </Form.Item>
                  <Form.Item name="address" label="地址">
                    <Input allowClear />
                  </Form.Item>
                  <Space wrap>
                    <Form.Item name="enabled" label="启用" valuePropName="checked">
                      <Switch />
                    </Form.Item>
                    <Form.Item name="verified" label="已验证" valuePropName="checked">
                      <Switch />
                    </Form.Item>
                  </Space>
                  <Form.Item name="metadata" label="扩展信息">
                    <Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>保存渠道</Button>
                </Form>
              </Card>
            </Col>
            <Col xs={24} xl={8}>
              <Card title="抑制名单">
                <Form
                  form={suppressionForm}
                  layout="vertical"
                  initialValues={{ channel: 'ALL', active: true }}
                  onFinish={handleSuppressionSave}
                >
                  <Form.Item name="channel" label="渠道" rules={[{ required: true, message: '请选择渠道' }]}>
                    <Select options={CHANNEL_OPTIONS} />
                  </Form.Item>
                  <Form.Item name="reason" label="原因">
                    <Input allowClear />
                  </Form.Item>
                  <Form.Item name="active" label="生效" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                  <Form.Item name="expiresAt" label="过期时间">
                    <Input placeholder="2026-06-30T23:59:59" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>新增抑制</Button>
                </Form>
              </Card>
            </Col>
          </Row>

          <Table
            rowKey={row => `${row.channel}:${row.updatedAt ?? ''}`}
            title={() => '同意记录'}
            dataSource={report.consents}
            columns={consentColumns}
            loading={loading}
            pagination={false}
            size="small"
          />
          <Table
            rowKey={row => `${row.channel}:${row.address ?? ''}`}
            title={() => '渠道记录'}
            dataSource={report.channels}
            columns={channelColumns}
            loading={loading}
            pagination={false}
            size="small"
          />
          <Table
            rowKey="id"
            title={() => '抑制记录'}
            dataSource={report.suppressions}
            columns={suppressionColumns}
            loading={loading}
            pagination={{ pageSize: 8 }}
            size="small"
          />
        </>
      ) : (
        <Card>
          <Empty description="暂无用户偏好数据" />
        </Card>
      )}
    </div>
  )
}
