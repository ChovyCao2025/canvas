/**
 * Tenant-scoped analytics workbench backed by bounded P2-016C query APIs.
 */
import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, DatePicker, Empty, Input, Space, Statistic, Table, Tabs, Tag, Typography, message } from 'antd'
import { BarChartOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { useSearchParams } from 'react-router-dom'
import {
  analyticsApi,
  type AttributeDistributionRow,
  type EventCountRow,
  type UserTimelineRow,
} from '../../services/analyticsApi'
import {
  exportStateText,
  formatDateTime,
  requireDateRangeMessage,
  timelineRowText,
  toEventCountRows,
} from './analyticsPresentation'

const { RangePicker } = DatePicker
const { Title, Text } = Typography

type RangeValue = [Dayjs, Dayjs]

export default function AnalyticsPage() {
  const [searchParams] = useSearchParams()
  const [range, setRange] = useState<RangeValue>(() => initialRange(searchParams))
  const [eventCode, setEventCode] = useState(searchParams.get('eventCode') ?? '')
  const [userId, setUserId] = useState(searchParams.get('userId') ?? '')
  const [attribute, setAttribute] = useState(searchParams.get('attribute') ?? 'utm.source')
  const [eventCounts, setEventCounts] = useState<EventCountRow[]>([])
  const [eventTotal, setEventTotal] = useState<number | null>(null)
  const [timeline, setTimeline] = useState<UserTimelineRow[]>([])
  const [attributeRows, setAttributeRows] = useState<AttributeDistributionRow[]>([])
  const [loading, setLoading] = useState(false)
  const [timelineLoading, setTimelineLoading] = useState(false)
  const [attributeLoading, setAttributeLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const scope = useMemo(() => ({
    startDate: range[0].format('YYYY-MM-DD'),
    endDate: range[1].format('YYYY-MM-DD'),
  }), [range])

  const eventRows = useMemo(() => toEventCountRows(eventCounts), [eventCounts])

  const loadEvents = async () => {
    const messageText = requireDateRangeMessage(scope)
    if (messageText) {
      message.warning(messageText)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const [countsRes, totalRes] = await Promise.all([
        analyticsApi.eventCounts(scope),
        analyticsApi.eventTotal({ ...scope, eventCode: eventCode.trim() || undefined }),
      ])
      setEventCounts(countsRes.data ?? [])
      setEventTotal(totalRes.data?.count ?? 0)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  const loadTimeline = async () => {
    if (!userId.trim()) {
      message.warning('请输入用户 ID')
      return
    }
    setTimelineLoading(true)
    setError(null)
    try {
      const res = await analyticsApi.userTimeline(userId.trim(), { ...scope, page: 1, size: 50 })
      setTimeline(res.data?.list ?? [])
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setTimelineLoading(false)
    }
  }

  const loadAttribute = async () => {
    if (!attribute.trim()) {
      message.warning('请输入属性路径')
      return
    }
    setAttributeLoading(true)
    setError(null)
    try {
      const res = await analyticsApi.attributeDistribution(attribute.trim(), scope)
      setAttributeRows(res.data ?? [])
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setAttributeLoading(false)
    }
  }

  useEffect(() => {
    loadEvents()
    if (userId) loadTimeline()
  }, [])

  const eventColumns: ColumnsType<ReturnType<typeof toEventCountRows>[number]> = [
    { title: '事件', dataIndex: 'eventCode' },
    { title: '次数', dataIndex: 'count', width: 120, align: 'right', sorter: (a, b) => a.count - b.count },
    { title: '摘要', dataIndex: 'label', width: 220 },
  ]

  const timelineColumns: ColumnsType<UserTimelineRow> = [
    { title: '事件', dataIndex: 'eventCode' },
    { title: '时间', dataIndex: 'eventTime', width: 220, render: formatDateTime },
    { title: '摘要', width: 320, render: (_, row) => timelineRowText(row) },
  ]

  const attributeColumns: ColumnsType<AttributeDistributionRow> = [
    { title: '属性值', dataIndex: 'value' },
    { title: '次数', dataIndex: 'count', width: 120, align: 'right', sorter: (a, b) => a.count - b.count },
  ]

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
        <Title level={4} style={{ margin: 0 }}>事件分析</Title>
        <Space wrap>
          <RangePicker
            value={range}
            allowClear={false}
            onChange={value => {
              if (value?.[0] && value?.[1]) setRange([value[0], value[1]])
            }}
          />
          <Button icon={<ReloadOutlined />} onClick={loadEvents} loading={loading}>刷新</Button>
        </Space>
      </div>

      {error && <Alert type="error" showIcon message={error} />}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
        <Statistic title="事件总数" value={eventTotal ?? 0} prefix={<BarChartOutlined />} />
        <Statistic title="事件类型" value={eventRows.length} />
        <Statistic title="导出状态" value={exportStateText('UNAVAILABLE')} />
      </div>

      <Tabs
        items={[
          {
            key: 'events',
            label: '事件总览',
            children: (
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <Space wrap>
                  <Input
                    allowClear
                    value={eventCode}
                    onChange={event => setEventCode(event.target.value)}
                    placeholder="事件编码筛选"
                    style={{ width: 220 }}
                  />
                  <Button icon={<SearchOutlined />} onClick={loadEvents} loading={loading}>查询</Button>
                </Space>
                <Table
                  rowKey="key"
                  dataSource={eventRows}
                  columns={eventColumns}
                  loading={loading}
                  pagination={{ pageSize: 10 }}
                  locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无事件数据" /> }}
                />
              </Space>
            ),
          },
          {
            key: 'timeline',
            label: '用户时间线',
            children: (
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <Space wrap>
                  <Input
                    allowClear
                    value={userId}
                    onChange={event => setUserId(event.target.value)}
                    placeholder="用户 ID"
                    style={{ width: 260 }}
                  />
                  <Button icon={<SearchOutlined />} onClick={loadTimeline} loading={timelineLoading}>查询</Button>
                </Space>
                <Table
                  rowKey={(_, index) => String(index)}
                  dataSource={timeline}
                  columns={timelineColumns}
                  loading={timelineLoading}
                  pagination={{ pageSize: 10 }}
                  locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无时间线数据" /> }}
                />
              </Space>
            ),
          },
          {
            key: 'attributes',
            label: '属性分布',
            children: (
              <Space direction="vertical" size={12} style={{ width: '100%' }}>
                <Space wrap>
                  <Input
                    allowClear
                    value={attribute}
                    onChange={event => setAttribute(event.target.value)}
                    placeholder="属性路径，例如 utm.source"
                    style={{ width: 260 }}
                  />
                  <Button icon={<SearchOutlined />} onClick={loadAttribute} loading={attributeLoading}>查询</Button>
                </Space>
                <Table
                  rowKey="value"
                  dataSource={attributeRows}
                  columns={attributeColumns}
                  loading={attributeLoading}
                  pagination={{ pageSize: 10 }}
                  locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无属性分布" /> }}
                />
              </Space>
            ),
          },
          {
            key: 'exports',
            label: '导出',
            children: (
              <Space direction="vertical" size={12}>
                <Tag color="default">{exportStateText('UNAVAILABLE')}</Tag>
                <Text type="secondary">后端导出任务接口尚未在当前 P2-016C 切片暴露。</Text>
              </Space>
            ),
          },
        ]}
      />
    </div>
  )
}

function initialRange(searchParams: URLSearchParams): RangeValue {
  const end = searchParams.get('endDate') ? dayjs(searchParams.get('endDate')) : dayjs()
  const start = searchParams.get('startDate') ? dayjs(searchParams.get('startDate')) : end.subtract(6, 'day')
  return [start, end]
}

function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  return '分析数据加载失败'
}
