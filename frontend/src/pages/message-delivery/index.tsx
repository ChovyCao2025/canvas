/**
 * Operator surface for delivery outbox, receipts, replay, and reconciliation.
 */
import { useEffect, useState } from 'react'
import {
  Button,
  Descriptions,
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
import { ReloadOutlined, RetweetOutlined, SearchOutlined, SyncOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  messageDeliveryApi,
  type DeliveryOutbox,
  type DeliveryReceiptLog,
  type DeliverySearchParams,
} from '../../services/messageDeliveryApi'
import {
  DELIVERY_CHANNEL_OPTIONS,
  DELIVERY_STATUS_OPTIONS,
  canReplayDelivery,
  deliveryDetailTitle,
  deliveryStatusColor,
  formatDateTime,
  normalizeDeliveryFilters,
  receiptLine,
} from './messageDeliveryPresentation'

const { Title, Text } = Typography

export default function MessageDeliveryPage() {
  const [form] = Form.useForm<DeliverySearchParams>()
  const [rows, setRows] = useState<DeliveryOutbox[]>([])
  const [total, setTotal] = useState(0)
  const [filters, setFilters] = useState<DeliverySearchParams>({ page: 1, size: 20 })
  const [loading, setLoading] = useState(false)
  const [selected, setSelected] = useState<DeliveryOutbox | null>(null)
  const [receipts, setReceipts] = useState<DeliveryReceiptLog[]>([])
  const [receiptLoading, setReceiptLoading] = useState(false)
  const [replaying, setReplaying] = useState<number>()
  const [reconciling, setReconciling] = useState(false)

  const load = async (nextFilters = filters) => {
    const params = normalizeDeliveryFilters(nextFilters)
    setLoading(true)
    try {
      const res = await messageDeliveryApi.list(params)
      setRows(res.data.list)
      setTotal(res.data.total)
      setFilters(params)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const openDetail = async (row: DeliveryOutbox) => {
    setSelected(row)
    setReceiptLoading(true)
    try {
      const [detailRes, receiptRes] = await Promise.all([
        messageDeliveryApi.detail(row.id),
        messageDeliveryApi.receipts(row.id),
      ])
      setSelected(detailRes.data)
      setReceipts(receiptRes.data)
    } finally {
      setReceiptLoading(false)
    }
  }

  const replay = async (row: DeliveryOutbox) => {
    setReplaying(row.id)
    try {
      await messageDeliveryApi.replay(row.id)
      message.success('已重新入队')
      await load(filters)
      if (selected?.id === row.id) {
        setSelected(null)
      }
    } finally {
      setReplaying(undefined)
    }
  }

  const reconcile = async () => {
    setReconciling(true)
    try {
      const res = await messageDeliveryApi.reconcile()
      message.success(`已重排 ${res.data.requeued} 条停滞投递`)
      await load(filters)
    } finally {
      setReconciling(false)
    }
  }

  const columns: ColumnsType<DeliveryOutbox> = [
    { title: 'ID', dataIndex: 'id', width: 90 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: status => <Tag color={deliveryStatusColor(status)}>{status}</Tag>,
    },
    { title: '渠道', dataIndex: 'channel', width: 110 },
    { title: 'Provider', dataIndex: 'provider', width: 120 },
    { title: '画布', dataIndex: 'canvasId', width: 100 },
    { title: '执行 ID', dataIndex: 'executionId', width: 180, ellipsis: true },
    { title: '用户', dataIndex: 'userId', width: 160, ellipsis: true },
    { title: 'Provider 消息 ID', dataIndex: 'providerMessageId', width: 180, ellipsis: true, render: value => value ?? '-' },
    { title: '尝试', dataIndex: 'attemptCount', width: 80 },
    { title: '下次重试', dataIndex: 'nextRetryAt', width: 170, render: formatDateTime },
    { title: '更新于', dataIndex: 'updatedAt', width: 170, render: formatDateTime },
    {
      title: '操作',
      width: 150,
      fixed: 'right',
      render: (_, row) => (
        <Space>
          <Button aria-label="详情" size="small" onClick={() => openDetail(row)}>详情</Button>
          <Button
            aria-label="重放"
            size="small"
            icon={<RetweetOutlined />}
            disabled={!canReplayDelivery(row)}
            loading={replaying === row.id}
            onClick={() => replay(row)}
          >
            重放
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ minHeight: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>投递监控</Title>
          <Text type="secondary">查询 outbox、回执、死信重放和停滞投递重排。</Text>
        </div>
        <Space wrap>
          <Button aria-label="刷新" icon={<ReloadOutlined />} onClick={() => load(filters)} loading={loading}>刷新</Button>
          <Button aria-label="重排停滞投递" icon={<SyncOutlined />} onClick={reconcile} loading={reconciling}>重排停滞投递</Button>
        </Space>
      </div>

      <Form
        form={form}
        layout="inline"
        onFinish={values => load({ ...values, page: 1, size: filters.size })}
        style={{ gap: 8 }}
      >
        <Form.Item name="canvasId">
          <InputNumber min={1} placeholder="画布 ID" style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="executionId">
          <Input allowClear placeholder="执行 ID" style={{ width: 180 }} />
        </Form.Item>
        <Form.Item name="userId">
          <Input allowClear placeholder="用户 ID" style={{ width: 160 }} />
        </Form.Item>
        <Form.Item name="channel">
          <Select
            allowClear
            placeholder="渠道"
            style={{ width: 130 }}
            options={DELIVERY_CHANNEL_OPTIONS.map(value => ({ value, label: value }))}
          />
        </Form.Item>
        <Form.Item name="status">
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 140 }}
            options={DELIVERY_STATUS_OPTIONS.map(value => ({ value, label: value }))}
          />
        </Form.Item>
        <Form.Item name="providerMessageId">
          <Input allowClear placeholder="Provider 消息 ID" style={{ width: 190 }} />
        </Form.Item>
        <Form.Item>
          <Button aria-label="查询" htmlType="submit" type="primary" icon={<SearchOutlined />}>查询</Button>
        </Form.Item>
      </Form>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={rows}
        loading={loading}
        scroll={{ x: 1480 }}
        pagination={{
          current: filters.page ?? 1,
          pageSize: filters.size ?? 20,
          total,
          showSizeChanger: true,
          onChange: (page, size) => load({ ...filters, page, size }),
        }}
        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无投递记录" /> }}
      />

      <Drawer
        width={680}
        open={!!selected}
        title={deliveryDetailTitle(selected)}
        onClose={() => setSelected(null)}
        extra={selected ? (
          <Button
            aria-label="重放死信"
            icon={<RetweetOutlined />}
            disabled={!canReplayDelivery(selected)}
            loading={replaying === selected.id}
            onClick={() => replay(selected)}
          >
            重放死信
          </Button>
        ) : null}
      >
        {selected ? (
          <Space direction="vertical" size={18} style={{ width: '100%' }}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="状态">
                <Tag color={deliveryStatusColor(selected.status)}>{selected.status}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="渠道">{selected.channel}</Descriptions.Item>
              <Descriptions.Item label="Provider">{selected.provider}</Descriptions.Item>
              <Descriptions.Item label="尝试次数">{selected.attemptCount}</Descriptions.Item>
              <Descriptions.Item label="画布">{selected.canvasId}</Descriptions.Item>
              <Descriptions.Item label="节点">{selected.nodeId}</Descriptions.Item>
              <Descriptions.Item label="执行 ID" span={2}>{selected.executionId}</Descriptions.Item>
              <Descriptions.Item label="用户" span={2}>{selected.userId}</Descriptions.Item>
              <Descriptions.Item label="Provider 消息 ID" span={2}>{selected.providerMessageId ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="幂等键" span={2}>{selected.idempotencyKey}</Descriptions.Item>
              <Descriptions.Item label="错误" span={2}>{selected.lastError ?? '-'}</Descriptions.Item>
            </Descriptions>

            <List
              header="回执历史"
              loading={receiptLoading}
              dataSource={receipts}
              locale={{ emptyText: '暂无回执' }}
              renderItem={receipt => (
                <List.Item>
                  <Space direction="vertical" size={2}>
                    <Text strong>{receiptLine(receipt)}</Text>
                    <Text type="secondary" style={{ wordBreak: 'break-all' }}>{receipt.rawPayloadJson ?? '{}'}</Text>
                  </Space>
                </List.Item>
              )}
            />
          </Space>
        ) : null}
      </Drawer>
    </div>
  )
}
