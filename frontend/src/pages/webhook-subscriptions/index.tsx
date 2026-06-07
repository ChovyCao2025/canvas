import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  EditOutlined,
  HistoryOutlined,
  KeyOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SendOutlined,
  StopOutlined,
} from '@ant-design/icons'
import {
  cdpEventApi,
  type WebhookDeliveryRow,
  type WebhookSubscriptionForm,
  type WebhookSubscriptionRow,
} from '../../services/cdpEventApi'
import {
  deliveryStatusColor,
  deliveryStatusLabel,
  maskWebhookSecret,
  subscriptionStatusColor,
  subscriptionStatusLabel,
} from './webhookSubscriptionPresentation'

const { Text, Title } = Typography

export default function WebhookSubscriptionsPage() {
  const [rows, setRows] = useState<WebhookSubscriptionRow[]>([])
  const [deliveries, setDeliveries] = useState<WebhookDeliveryRow[]>([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [deliveryLoading, setDeliveryLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [deliveryDrawerOpen, setDeliveryDrawerOpen] = useState(false)
  const [editing, setEditing] = useState<WebhookSubscriptionRow | null>(null)
  const [selected, setSelected] = useState<WebhookSubscriptionRow | null>(null)
  const [rotatedSecret, setRotatedSecret] = useState<string | null>(null)
  const [form] = Form.useForm<WebhookSubscriptionForm>()

  const loadRows = async () => {
    setLoading(true)
    try {
      const res = await cdpEventApi.listWebhookSubscriptions()
      setRows(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadRows()
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ eventTypesText: 'cdp.event.ingested', maxAttempts: 3 })
    setModalOpen(true)
  }

  const openEdit = (row: WebhookSubscriptionRow) => {
    setEditing(row)
    form.setFieldsValue({
      name: row.name,
      callbackUrl: row.callbackUrl,
      eventTypesText: row.eventTypes.join('\n'),
      maxAttempts: row.maxAttempts,
    })
    setModalOpen(true)
  }

  const save = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      if (editing) {
        await cdpEventApi.updateWebhookSubscription(editing.id, values)
        message.success('Webhook 订阅已更新')
      } else {
        await cdpEventApi.createWebhookSubscription(values)
        message.success('Webhook 订阅已创建')
      }
      setModalOpen(false)
      await loadRows()
    } finally {
      setSaving(false)
    }
  }

  const updateStatus = async (row: WebhookSubscriptionRow, action: 'pause' | 'resume' | 'disable') => {
    if (action === 'pause') await cdpEventApi.pauseWebhookSubscription(row.id)
    if (action === 'resume') await cdpEventApi.resumeWebhookSubscription(row.id)
    if (action === 'disable') await cdpEventApi.disableWebhookSubscription(row.id)
    message.success('状态已更新')
    await loadRows()
  }

  const rotateSecret = async (row: WebhookSubscriptionRow) => {
    const res = await cdpEventApi.rotateWebhookSecret(row.id)
    setRotatedSecret(res.data.secret)
    message.success('Secret 已轮换')
    await loadRows()
  }

  const testDelivery = async (row: WebhookSubscriptionRow) => {
    await cdpEventApi.testWebhookDelivery(row.id)
    message.success('测试投递已发送')
  }

  const openDeliveries = async (row: WebhookSubscriptionRow) => {
    setSelected(row)
    setDeliveryDrawerOpen(true)
    setDeliveryLoading(true)
    try {
      const res = await cdpEventApi.listWebhookDeliveries(row.id)
      setDeliveries(res.data)
    } finally {
      setDeliveryLoading(false)
    }
  }

  const columns: ColumnsType<WebhookSubscriptionRow> = [
    { title: '名称', dataIndex: 'name', width: 160 },
    {
      title: 'Callback URL',
      dataIndex: 'callbackUrl',
      render: value => <Text code style={{ whiteSpace: 'normal', wordBreak: 'break-all' }}>{value}</Text>,
    },
    {
      title: '事件类型',
      dataIndex: 'eventTypes',
      width: 220,
      render: values => (
        <Space size={[4, 4]} wrap>
          {(values as string[]).map(value => <Tag key={value}>{value}</Tag>)}
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: status => <Tag color={subscriptionStatusColor(status)}>{subscriptionStatusLabel(status)}</Tag>,
    },
    {
      title: 'Secret',
      dataIndex: 'secretPrefix',
      width: 130,
      render: value => <Text code>{maskWebhookSecret(value)}</Text>,
    },
    { title: '最大尝试', dataIndex: 'maxAttempts', width: 90 },
    {
      title: '操作',
      width: 310,
      fixed: 'right',
      render: (_, row) => (
        <Space size="small" wrap>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(row)} />
          {row.status === 'ACTIVE' ? (
            <Button size="small" icon={<PauseCircleOutlined />} onClick={() => updateStatus(row, 'pause')}>暂停</Button>
          ) : (
            <Button size="small" icon={<PlayCircleOutlined />} onClick={() => updateStatus(row, 'resume')}>恢复</Button>
          )}
          <Button size="small" icon={<KeyOutlined />} onClick={() => rotateSecret(row)}>轮换</Button>
          <Button size="small" icon={<SendOutlined />} onClick={() => testDelivery(row)}>测试</Button>
          <Button size="small" icon={<HistoryOutlined />} onClick={() => openDeliveries(row)}>日志</Button>
          <Popconfirm title="确认禁用该订阅？" onConfirm={() => updateStatus(row, 'disable')}>
            <Button size="small" danger icon={<StopOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const deliveryColumns: ColumnsType<WebhookDeliveryRow> = [
    { title: 'Delivery ID', dataIndex: 'deliveryId', width: 220, render: value => <Text code>{value}</Text> },
    { title: '事件', dataIndex: 'eventType', width: 160 },
    { title: '尝试', dataIndex: 'attempt', width: 70 },
    { title: 'HTTP', dataIndex: 'httpStatus', width: 80, render: value => value ?? '-' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: status => <Tag color={deliveryStatusColor(status)}>{deliveryStatusLabel(status)}</Tag>,
    },
    { title: '下次重试', dataIndex: 'nextRetryAt', width: 170, render: value => value || '-' },
    { title: '终态原因', dataIndex: 'terminalReason', width: 180, render: value => value || '-' },
    { title: '创建时间', dataIndex: 'createdAt', width: 170, render: value => value || '-' },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>Webhook 订阅</Title>
          <Text type="secondary">管理出站事件订阅、Secret 轮换、测试投递和投递日志。</Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={loadRows} loading={loading}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建订阅</Button>
        </Space>
      </div>

      {rotatedSecret && (
        <Alert
          type="success"
          showIcon
          closable
          style={{ marginBottom: 16 }}
          message="新 Secret 仅显示一次"
          onClose={() => setRotatedSecret(null)}
          description={<Input.TextArea value={rotatedSecret} readOnly autoSize style={{ marginTop: 8, fontFamily: 'monospace' }} />}
        />
      )}

      <Table
        rowKey="id"
        dataSource={rows}
        columns={columns}
        loading={loading}
        pagination={{ pageSize: 10, hideOnSinglePage: true }}
        scroll={{ x: 1180 }}
      />

      <Modal
        title={editing ? '编辑订阅' : '新建订阅'}
        open={modalOpen}
        onOk={save}
        confirmLoading={saving}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="CRM / Data warehouse" />
          </Form.Item>
          <Form.Item name="callbackUrl" label="Callback URL" rules={[{ required: true, message: '请输入回调 URL' }]}>
            <Input placeholder="https://example.com/webhook" />
          </Form.Item>
          <Form.Item name="eventTypesText" label="事件类型" rules={[{ required: true, message: '请输入事件类型' }]}>
            <Input.TextArea autoSize={{ minRows: 3 }} placeholder={'cdp.event.ingested\nprofile.updated'} />
          </Form.Item>
          <Form.Item name="maxAttempts" label="最大尝试次数">
            <InputNumber min={1} max={10} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={selected ? `${selected.name} 投递日志` : '投递日志'}
        width={960}
        open={deliveryDrawerOpen}
        onClose={() => setDeliveryDrawerOpen(false)}
      >
        <Table
          rowKey="id"
          size="small"
          dataSource={deliveries}
          columns={deliveryColumns}
          loading={deliveryLoading}
          pagination={{ pageSize: 10, hideOnSinglePage: true }}
          scroll={{ x: 1160 }}
        />
      </Drawer>
    </div>
  )
}
