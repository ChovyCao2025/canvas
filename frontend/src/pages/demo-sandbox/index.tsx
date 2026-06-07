import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  InputNumber,
  Row,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { ReloadOutlined, SaveOutlined, SendOutlined, SyncOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { demoSandboxApi } from '../../services/demoSandboxApi'
import {
  demoExpiryText,
  demoMarkerWarning,
  resetStateText,
  sandboxReplyEventId,
  sandboxReplyStateText,
  sandboxStatusView,
  type DemoSandbox,
  type ResetState,
  type SandboxReplyState,
} from './demoSandbox'

const { Title, Text } = Typography

interface InstallFormValues {
  tenantId: number
  demoName: string
  ttlDays: number
}

interface ResetFormValues {
  tenantId: number
}

interface ConversationReplyFormValues {
  tenantId: number
  canvasId?: number
  versionId?: number
  executionId?: string
  userId: string
  text: string
  intent?: string
}

export default function DemoSandboxPage() {
  const [installForm] = Form.useForm<InstallFormValues>()
  const [resetForm] = Form.useForm<ResetFormValues>()
  const [replyForm] = Form.useForm<ConversationReplyFormValues>()
  const [expiredSandboxes, setExpiredSandboxes] = useState<DemoSandbox[]>([])
  const [currentSandbox, setCurrentSandbox] = useState<DemoSandbox | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [resetState, setResetState] = useState<ResetState>({ status: 'IDLE' })
  const [replyState, setReplyState] = useState<SandboxReplyState>({ status: 'IDLE' })
  const [error, setError] = useState<string | null>(null)

  const loadExpired = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await demoSandboxApi.expired()
      setExpiredSandboxes(response.data)
    } catch (caught) {
      setError((caught as Error).message || '演示沙箱加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadExpired()
  }, [])

  const handleInstall = async (values: InstallFormValues) => {
    setSaving(true)
    setError(null)
    try {
      const response = await demoSandboxApi.install(values)
      setCurrentSandbox(response.data)
      resetForm.setFieldsValue({ tenantId: response.data.tenantId })
      replyForm.setFieldsValue({ tenantId: response.data.tenantId })
      message.success('演示沙箱已安装')
      await loadExpired()
    } catch (caught) {
      setError((caught as Error).message || '演示沙箱安装失败')
    } finally {
      setSaving(false)
    }
  }

  const handleReset = async (values: ResetFormValues) => {
    setResetState({ status: 'RUNNING' })
    setError(null)
    try {
      const response = await demoSandboxApi.reset(values.tenantId)
      setResetState({ status: 'IDLE' })
      message.success(`沙箱已重置：${response.data.demoMarker}`)
      await loadExpired()
    } catch (caught) {
      const messageText = (caught as Error).message || '沙箱重置失败'
      setResetState({ status: 'FAILED', message: messageText })
      setError(messageText)
    }
  }

  const handleConversationReply = async (values: ConversationReplyFormValues) => {
    setReplyState({ status: 'RUNNING' })
    setError(null)
    const eventId = sandboxReplyEventId(values.tenantId)
    try {
      const response = await demoSandboxApi.reply(values.tenantId, {
        canvasId: values.canvasId,
        versionId: values.versionId,
        executionId: values.executionId,
        userId: values.userId,
        externalMessageId: eventId,
        eventId,
        text: values.text,
        intent: values.intent,
        attributes: { source: 'demo-sandbox' },
      })
      setReplyState({ status: 'RECORDED', result: response.data })
      message.success('模拟回复已发送')
    } catch (caught) {
      const messageText = (caught as Error).message || '模拟回复失败'
      setReplyState({ status: 'FAILED', message: messageText })
      setError(messageText)
    }
  }

  const columns: ColumnsType<DemoSandbox> = [
    { title: '租户', dataIndex: 'tenantId', width: 100 },
    { title: '演示名称', dataIndex: 'demoName' },
    {
      title: '标记',
      dataIndex: 'demoMarker',
      render: value => <Tag color="blue">{value}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: value => {
        const view = sandboxStatusView(value)
        return <Tag color={view.color}>{view.text}</Tag>
      },
    },
    { title: '过期时间', dataIndex: 'expiresAt', render: value => value || '-' },
    { title: '上次重置', dataIndex: 'lastResetAt', render: value => value || '-' },
  ]

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
        <Title level={3} style={{ margin: 0 }}>演示沙箱</Title>
        <Button icon={<ReloadOutlined />} loading={loading} onClick={loadExpired}>
          刷新
        </Button>
      </Space>

      {error && <Alert type="error" showIcon message={error} />}

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={11}>
          <Card size="small" title="安装演示沙箱">
            <Form
              form={installForm}
              layout="vertical"
              initialValues={{ demoName: 'Retail Lifecycle Demo', ttlDays: 14 }}
              onFinish={handleInstall}
            >
              <Form.Item name="tenantId" label="租户 ID" rules={[{ required: true }]}>
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item name="demoName" label="演示名称" rules={[{ required: true }]}>
                <Input />
              </Form.Item>
              <Form.Item name="ttlDays" label="有效天数" rules={[{ required: true }]}>
                <InputNumber min={1} max={90} style={{ width: '100%' }} />
              </Form.Item>
              <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>
                安装演示沙箱
              </Button>
            </Form>

            {currentSandbox && (
              <Alert
                style={{ marginTop: 16 }}
                type="success"
                showIcon
                message={demoMarkerWarning(currentSandbox)}
                description={demoExpiryText(currentSandbox)}
              />
            )}
          </Card>
        </Col>

        <Col xs={24} xl={13}>
          <Card size="small" title="重置沙箱">
            <Form
              form={resetForm}
              layout="inline"
              onFinish={handleReset}
              initialValues={{ tenantId: currentSandbox?.tenantId }}
            >
              <Form.Item name="tenantId" label="租户 ID" rules={[{ required: true }]}>
                <InputNumber min={1} style={{ width: 180 }} />
              </Form.Item>
              <Button
                htmlType="submit"
                icon={<SyncOutlined />}
                loading={resetState.status === 'RUNNING'}
              >
                重置沙箱
              </Button>
            </Form>
            <Text type="secondary" style={{ display: 'block', marginTop: 12 }}>
              {resetStateText(resetState)}
            </Text>
          </Card>
        </Col>
      </Row>

      <Card size="small" title="模拟会话回复">
        <Form
          form={replyForm}
          layout="vertical"
          initialValues={{
            tenantId: currentSandbox?.tenantId,
            userId: 'demo-user-1',
            text: 'yes please',
            intent: 'PRODUCT_A',
          }}
          onFinish={handleConversationReply}
        >
          <Row gutter={[16, 0]}>
            <Col xs={24} md={8} xl={4}>
              <Form.Item name="tenantId" label="目标租户 ID" rules={[{ required: true }]}>
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8} xl={4}>
              <Form.Item name="canvasId" label="Canvas ID">
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8} xl={4}>
              <Form.Item name="versionId" label="版本 ID">
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12} xl={4}>
              <Form.Item name="executionId" label="执行 ID">
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={12} xl={4}>
              <Form.Item name="userId" label="用户 ID" rules={[{ required: true }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={12} xl={4}>
              <Form.Item name="intent" label="意图">
                <Input />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="text" label="回复内容" rules={[{ required: true }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Space align="center">
            <Button
              htmlType="submit"
              icon={<SendOutlined />}
              loading={replyState.status === 'RUNNING'}
            >
              发送模拟回复
            </Button>
            <Text type="secondary">{sandboxReplyStateText(replyState)}</Text>
          </Space>
        </Form>
      </Card>

      <Card size="small" title="过期清理候选">
        <Table
          rowKey="tenantId"
          columns={columns}
          dataSource={expiredSandboxes}
          loading={loading}
          pagination={false}
          locale={{ emptyText: <Empty description="暂无过期沙箱" /> }}
        />
      </Card>
    </Space>
  )
}
