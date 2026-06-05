import { useEffect, useMemo, useState, type CSSProperties } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { Alert, Card, Col, Empty, Progress, Row, Space, Spin, Tag, Typography } from 'antd'
import { BarChartOutlined, LinkOutlined, SafetyCertificateOutlined } from '@ant-design/icons'
import { biApi, type BiEmbedTicketPayload } from '../../services/biApi'
import {
  chartLabel,
  dashboardWidgetGridPlacement,
  getDefaultDashboardPreset,
  type BiDashboardWidgetPreset,
} from './biWorkbench'

const { Text, Title } = Typography

export default function BiEmbedPage() {
  const { resourceType = 'DASHBOARD', resourceKey = 'canvas-effect' } = useParams<{
    resourceType: string
    resourceKey: string
  }>()
  const [searchParams] = useSearchParams()
  const ticket = searchParams.get('ticket') || ''
  const [payload, setPayload] = useState<BiEmbedTicketPayload | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const preset = useMemo(() => getDefaultDashboardPreset(resourceKey), [resourceKey])

  useEffect(() => {
    if (!ticket) {
      setError('缺少嵌入 ticket')
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    biApi.verifyEmbedTicket(ticket)
      .then(response => {
        if (cancelled) return
        if (response.data.resourceType !== resourceType || response.data.resourceKey !== resourceKey) {
          setError('嵌入 ticket 与当前资源不匹配')
          return
        }
        setPayload(response.data)
      })
      .catch(() => {
        if (!cancelled) setError('嵌入 ticket 无效或已过期')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [ticket, resourceType, resourceKey])

  if (loading) {
    return <Spin fullscreen />
  }

  if (error || !payload) {
    return (
      <div style={shellStyle}>
        <Alert type="error" showIcon message={error || '无法打开嵌入报表'} />
      </div>
    )
  }

  return (
    <div style={shellStyle}>
      <header style={headerStyle}>
        <Space size={10}>
          <span style={brandIconStyle}><BarChartOutlined /></span>
          <Space direction="vertical" size={0}>
            <Title level={4} style={{ margin: 0 }}>{preset.title}</Title>
            <Text type="secondary" style={{ fontSize: 12 }}>嵌入资源 · {resourceType}/{resourceKey}</Text>
          </Space>
        </Space>
        <Space size={8} wrap>
          <Tag icon={<SafetyCertificateOutlined />} color="green">{payload.scope}</Tag>
          <Tag icon={<LinkOutlined />} color="blue">Tenant {payload.tenantId}</Tag>
          {Object.entries(payload.filters).map(([key, value]) => (
            <Tag key={key} color="cyan">{key}: {value}</Tag>
          ))}
        </Space>
      </header>

      <main style={canvasStyle}>
        {preset.widgets.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无报表组件" />
        ) : (
          preset.widgets.map(widget => <EmbedWidget key={widget.widgetKey} widget={widget} />)
        )}
      </main>
      <footer style={footerStyle}>
        <Text type="secondary" style={{ fontSize: 12 }}>Ticket expires at {payload.expiresAt}</Text>
      </footer>
    </div>
  )
}

function EmbedWidget({ widget }: { widget: BiDashboardWidgetPreset }) {
  return (
    <Card
      title={<Space><Text strong>{widget.title}</Text><Tag>{chartLabel(widget.chartType)}</Tag></Space>}
      bordered
      style={{
        ...widgetStyle,
        ...dashboardWidgetGridPlacement(widget),
      }}
      styles={{ body: { padding: 12 } }}
    >
      <WidgetBody widget={widget} />
    </Card>
  )
}

function WidgetBody({ widget }: { widget: BiDashboardWidgetPreset }) {
  if (widget.chartType === 'KPI_CARD') {
    const successRate = widget.metrics.includes('success_rate')
    return (
      <Space direction="vertical" size={6} style={{ width: '100%' }}>
        <Text style={{ fontSize: 30, fontWeight: 700 }}>{successRate ? '96.8%' : '128,430'}</Text>
        <Progress percent={successRate ? 96 : 72} size="small" showInfo={false} />
        <Text type="secondary" style={{ fontSize: 12 }}>{widget.metrics.join(' / ')}</Text>
      </Space>
    )
  }
  if (widget.chartType === 'LINE') {
    return (
      <svg width="100%" height="140" viewBox="0 0 420 140" role="img" aria-label={widget.title}>
        <polyline fill="none" stroke="#2563eb" strokeWidth="3" points="12,102 72,88 132,92 192,50 252,61 312,30 408,40" />
        <polyline fill="none" stroke="#ef4444" strokeWidth="2" points="12,112 72,106 132,104 192,90 252,94 312,76 408,82" />
      </svg>
    )
  }
  if (widget.chartType === 'BAR') {
    return (
      <Row gutter={8} align="bottom" style={{ height: 140 }}>
        {[96, 72, 54, 42, 34].map((height, index) => (
          <Col key={height} span={4}>
            <div style={{ height, background: index === 0 ? '#2563eb' : '#93c5fd', borderRadius: 4 }} />
          </Col>
        ))}
      </Row>
    )
  }
  return (
    <Space direction="vertical" size={0} style={{ width: '100%' }}>
      {['欢迎旅程', '复购召回', '积分激活'].map((name, index) => (
        <div key={name} style={tableRowStyle}>
          <Text>{name}</Text>
          <Text>{[18240, 9632, 5840][index].toLocaleString()}</Text>
          <Tag color={index === 0 ? 'green' : 'blue'}>{index === 0 ? '96.8%' : '92.4%'}</Tag>
        </div>
      ))}
    </Space>
  )
}

const shellStyle: CSSProperties = {
  minHeight: '100vh',
  background: '#f4f6f9',
  padding: 16,
}

const headerStyle: CSSProperties = {
  minHeight: 62,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 12,
  padding: '12px 14px',
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
  marginBottom: 12,
}

const canvasStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(20, minmax(0, 1fr))',
  gridAutoRows: 42,
  gap: 10,
}

const widgetStyle: CSSProperties = {
  borderRadius: 8,
  overflow: 'hidden',
}

const brandIconStyle: CSSProperties = {
  width: 36,
  height: 36,
  borderRadius: 8,
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  color: '#fff',
  background: '#111827',
}

const footerStyle: CSSProperties = {
  padding: '12px 4px 0',
}

const tableRowStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: '1fr auto auto',
  gap: 10,
  alignItems: 'center',
  padding: '8px 0',
  borderBottom: '1px solid #edf2f7',
}
