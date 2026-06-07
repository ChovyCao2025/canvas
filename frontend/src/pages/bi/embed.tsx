import { useEffect, useState, type CSSProperties } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { Alert, Card, Col, Empty, Progress, Row, Space, Spin, Tag, Typography } from 'antd'
import { BarChartOutlined, LinkOutlined, SafetyCertificateOutlined } from '@ant-design/icons'
import { biApi, type BiEmbedTicketPayload, type BiQueryResult } from '../../services/biApi'
import {
  buildWidgetQueryRequest,
  chartLabel,
  dashboardRuntimeParametersFromEmbedPayload,
  dashboardWidgetGridPlacement,
  getDefaultDashboardPreset,
  type BiDashboardRuntimeParameters,
  type BiDashboardPresetLike,
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
  const [preset, setPreset] = useState<BiDashboardPresetLike | null>(null)
  const [runtimeParameters, setRuntimeParameters] = useState<BiDashboardRuntimeParameters>({})
  const [queryResults, setQueryResults] = useState<Record<string, BiQueryResult>>({})
  const [queryErrors, setQueryErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!ticket) {
      setError('缺少嵌入 ticket')
      setLoading(false)
      return
    }
    let cancelled = false
    const loadEmbedReport = async () => {
      setLoading(true)
      setError(null)
      setPayload(null)
      setPreset(null)
      setRuntimeParameters({})
      setQueryResults({})
      setQueryErrors({})
      try {
        const verified = await biApi.verifyEmbedTicket(ticket)
        if (cancelled) return
        if (verified.data.resourceType !== resourceType || verified.data.resourceKey !== resourceKey) {
          setError('嵌入 ticket 与当前资源不匹配')
          return
        }
        if (verified.data.resourceType !== 'DASHBOARD') {
          setError('当前嵌入页仅支持仪表板资源')
          return
        }
        const loadedPreset = await loadDashboardPreset(ticket, resourceType, resourceKey)
        if (cancelled) return
        const runtimeState = await biApi.getEmbedDashboardRuntimeState({
          ticket,
          resourceType,
          resourceKey,
        })
        if (cancelled) return
        const resolvedRuntimeParameters = dashboardRuntimeParametersFromEmbedPayload(
          loadedPreset,
          verified.data,
          (runtimeState.data?.parameters ?? null) as BiDashboardRuntimeParameters | null,
        )
        const canvasId = embedPayloadCanvasId(verified.data)
        const queryEntries = await Promise.all(loadedPreset.widgets.map(widget =>
          biApi.executeEmbedQuery({
            ticket,
            resourceType,
            resourceKey,
            widgetKey: widget.widgetKey,
            query: buildWidgetQueryRequest(loadedPreset, widget, canvasId, resolvedRuntimeParameters),
          })
            .then(response => ({ widgetKey: widget.widgetKey, result: response.data }))
            .catch(error => ({
              widgetKey: widget.widgetKey,
              error: error instanceof Error ? error.message : '查询失败',
            })),
        ))
        if (cancelled) return
        setPayload(verified.data)
        setPreset(loadedPreset)
        setRuntimeParameters(resolvedRuntimeParameters)
        setQueryResults(Object.fromEntries(queryEntries
          .filter((entry): entry is { widgetKey: string; result: BiQueryResult } => 'result' in entry)
          .map(entry => [entry.widgetKey, entry.result])))
        setQueryErrors(Object.fromEntries(queryEntries
          .filter((entry): entry is { widgetKey: string; error: string } => 'error' in entry)
          .map(entry => [entry.widgetKey, entry.error])))
      } catch {
        if (!cancelled) setError('嵌入 ticket 无效或已过期')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    loadEmbedReport()
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
            <Title level={4} style={{ margin: 0 }}>{preset?.title ?? resourceKey}</Title>
            <Text type="secondary" style={{ fontSize: 12 }}>嵌入资源 · {resourceType}/{resourceKey}</Text>
          </Space>
        </Space>
        <Space size={8} wrap>
          <Tag icon={<SafetyCertificateOutlined />} color="green">{payload.scope}</Tag>
          <Tag icon={<LinkOutlined />} color="blue">Tenant {payload.tenantId}</Tag>
          {embedPayloadCanvasId(payload) && <Tag color="cyan">canvasId: {embedPayloadCanvasId(payload)}</Tag>}
          {Object.entries(runtimeParameters).map(([key, value]) => (
            <Tag key={key} color="cyan">{key}: {formatRuntimeParameterValue(value)}</Tag>
          ))}
        </Space>
      </header>

      <main style={canvasStyle}>
        {!preset || preset.widgets.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无报表组件" />
        ) : (
          preset.widgets.map(widget => (
            <EmbedWidget
              key={widget.widgetKey}
              widget={widget}
              result={queryResults[widget.widgetKey]}
              error={queryErrors[widget.widgetKey]}
            />
          ))
        )}
      </main>
      <footer style={footerStyle}>
        <Text type="secondary" style={{ fontSize: 12 }}>Ticket expires at {payload.expiresAt}</Text>
      </footer>
    </div>
  )
}

async function loadDashboardPreset(
  ticket: string,
  resourceType: string,
  dashboardKey: string,
): Promise<BiDashboardPresetLike> {
  const resource = await biApi.getEmbedDashboardResource({
    ticket,
    resourceType,
    resourceKey: dashboardKey,
  })
  return resource.data?.preset ?? getDefaultDashboardPreset(dashboardKey)
}

function embedPayloadCanvasId(payload: BiEmbedTicketPayload): string | null {
  return payload.filters?.canvasId ?? payload.filters?.canvas_id ?? null
}

function formatRuntimeParameterValue(value: unknown): string {
  if (Array.isArray(value)) return value.map(item => formatRuntimeParameterValue(item)).filter(Boolean).join(',')
  if (value == null) return ''
  return String(value)
}

function EmbedWidget({
  widget,
  result,
  error,
}: {
  widget: BiDashboardWidgetPreset
  result?: BiQueryResult
  error?: string
}) {
  return (
    <Card
      title={(
        <Space size={6} wrap>
          <Text strong>{widget.title}</Text>
          <Tag>{chartLabel(widget.chartType)}</Tag>
          {result && <Tag color={result.cached ? 'green' : 'blue'}>{result.cached ? '缓存命中' : '实时查询'}</Tag>}
          {result && <Tag color="default">{result.rowCount} 行 · {result.durationMs}ms</Tag>}
        </Space>
      )}
      bordered
      style={{
        ...widgetStyle,
        ...dashboardWidgetGridPlacement(widget),
      }}
      styles={{ body: { padding: 12 } }}
    >
      <WidgetBody widget={widget} result={result} error={error} />
    </Card>
  )
}

function WidgetBody({
  widget,
  result,
  error,
}: {
  widget: BiDashboardWidgetPreset
  result?: BiQueryResult
  error?: string
}) {
  if (error) {
    return <Alert type="warning" showIcon message="组件查询失败" description={error} />
  }
  if (!result) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无查询结果" />
  }
  const rows = result.rows ?? []
  if (rows.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无数据" />
  }
  if (widget.chartType === 'KPI_CARD') {
    const metric = widget.metrics[0]
    const value = toNumber(rows[0][metric])
    const percent = metric === 'success_rate'
      ? Math.round(value <= 1 ? value * 100 : value)
      : Math.min(100, Math.round(value / 1800))
    return (
      <Space direction="vertical" size={6} style={{ width: '100%' }}>
        <Text style={{ fontSize: 30, fontWeight: 700 }}>{formatMetricValue(metric, value)}</Text>
        <Progress percent={percent} size="small" showInfo={false} />
        <Text type="secondary" style={{ fontSize: 12 }}>{widget.metrics.join(' / ')}</Text>
      </Space>
    )
  }
  if (widget.chartType === 'LINE') {
    const primaryMetric = widget.metrics[0]
    const secondaryMetric = widget.metrics[1]
    return (
      <svg width="100%" height="140" viewBox="0 0 420 140" role="img" aria-label={widget.title}>
        <polyline fill="none" stroke="#2563eb" strokeWidth="3" points={linePoints(rows.map(row => toNumber(row[primaryMetric])), 420, 140)} />
        {secondaryMetric && (
          <polyline fill="none" stroke="#ef4444" strokeWidth="2" points={linePoints(rows.map(row => toNumber(row[secondaryMetric])), 420, 140)} />
        )}
      </svg>
    )
  }
  if (widget.chartType === 'BAR') {
    const metric = widget.metrics[0]
    const values = rows.slice(0, 6).map(row => toNumber(row[metric]))
    const max = Math.max(1, ...values)
    return (
      <Row gutter={8} align="bottom" style={{ height: 140 }}>
        {values.map((value, index) => (
          <Col key={`${metric}-${index}`} span={4}>
            <div style={{ height: Math.max(10, (value / max) * 132), background: index === 0 ? '#2563eb' : '#93c5fd', borderRadius: 4 }} />
          </Col>
        ))}
      </Row>
    )
  }
  const columns = [...widget.dimensions, ...widget.metrics].slice(0, 3)
  return (
    <Space direction="vertical" size={0} style={{ width: '100%' }}>
      {rows.slice(0, 5).map((row, index) => (
        <div key={index} style={{ ...tableRowStyle, gridTemplateColumns: `repeat(${columns.length}, minmax(0, 1fr))` }}>
          {columns.map(column => (
            <Text key={column} ellipsis style={{ fontSize: 12 }}>{formatCellValue(column, row[column])}</Text>
          ))}
        </div>
      ))}
    </Space>
  )
}

function linePoints(values: number[], width: number, height: number): string {
  if (values.length === 0) return ''
  const max = Math.max(...values, 1)
  const min = Math.min(...values, 0)
  const range = Math.max(1, max - min)
  return values.map((value, index) => {
    const x = values.length === 1 ? width / 2 : 12 + (index * (width - 24)) / (values.length - 1)
    const y = height - 12 - ((value - min) / range) * (height - 24)
    return `${Math.round(x)},${Math.round(y)}`
  }).join(' ')
}

function toNumber(value: unknown): number {
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : 0
  }
  return 0
}

function formatMetricValue(metric: string, value: number): string {
  if (metric.includes('rate') || metric.includes('ratio') || metric.includes('percent')) {
    return `${(value <= 1 ? value * 100 : value).toFixed(1)}%`
  }
  return Math.round(value).toLocaleString()
}

function formatCellValue(column: string, value: unknown): string {
  if (typeof value === 'number') {
    return column.includes('rate') || column.includes('ratio') || column.includes('percent')
      ? formatMetricValue(column, value)
      : Math.round(value).toLocaleString()
  }
  if (value == null) return '-'
  return String(value)
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
