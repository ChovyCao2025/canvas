import { useEffect, useState, type CSSProperties } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { Alert, Button, Card, Col, Empty, List, Progress, Row, Space, Spin, Tag, Typography } from 'antd'
import { ApiOutlined, BarChartOutlined, LinkOutlined, SafetyCertificateOutlined } from '@ant-design/icons'
import { biApi, type BiEmbedTicketPayload, type BiPortalMenuResource, type BiPortalResource, type BiQueryResult } from '../../services/biApi'
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

/** BI 嵌入页，通过 ticket 校验后加载仪表盘预设并执行组件查询。 */
export default function BiEmbedPage() {
  const { resourceType = 'DASHBOARD', resourceKey = 'canvas-effect' } = useParams<{
    resourceType: string
    resourceKey: string
  }>()
  const [searchParams] = useSearchParams()
  const ticket = searchParams.get('ticket') || ''
  const [payload, setPayload] = useState<BiEmbedTicketPayload | null>(null)
  const [preset, setPreset] = useState<BiDashboardPresetLike | null>(null)
  const [portal, setPortal] = useState<BiPortalResource | null>(null)
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
      // 每次 ticket 或资源变化都清空旧状态，避免短暂展示上一个嵌入报表的数据。
      setError(null)
      setPayload(null)
      setPreset(null)
      setPortal(null)
      setRuntimeParameters({})
      setQueryResults({})
      setQueryErrors({})
      try {
        const verified = await biApi.verifyEmbedTicket(ticket)
        if (cancelled) return
        // ticket 只能打开签发时绑定的资源，防止 URL 手工替换 resourceKey 越权访问。
        if (verified.data.resourceType !== resourceType || verified.data.resourceKey !== resourceKey) {
          setError('嵌入 ticket 与当前资源不匹配')
          return
        }
        if (verified.data.resourceType === 'PORTAL') {
          const loadedPortal = await biApi.getEmbedPortalResource({
            ticket,
            resourceType,
            resourceKey,
          })
          if (cancelled) return
          setPayload(verified.data)
          setPortal(loadedPortal.data)
          return
        }
        if (verified.data.resourceType !== 'DASHBOARD') {
          setError('当前嵌入页仅支持仪表板和门户资源')
          return
        }
        const loadedDashboard = await loadEmbeddedDashboard(ticket, resourceType, resourceKey, verified.data)
        if (cancelled) return
        setPayload(verified.data)
        setPreset(loadedDashboard.preset)
        setRuntimeParameters(loadedDashboard.runtimeParameters)
        setQueryResults(loadedDashboard.queryResults)
        setQueryErrors(loadedDashboard.queryErrors)
      } catch {
        // 不暴露 ticket 校验细节，统一返回无效或过期提示。
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
            <Title level={4} style={{ margin: 0 }}>{portalTitle(portal) ?? preset?.title ?? resourceKey}</Title>
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

      {portal ? (
        <EmbedPortal portal={portal} ticket={ticket} payload={payload} />
      ) : (
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
      )}
      <footer style={footerStyle}>
        <Text type="secondary" style={{ fontSize: 12 }}>{portalFooter(portal) ?? `Ticket expires at ${payload.expiresAt}`}</Text>
      </footer>
    </div>
  )
}

/** 加载嵌入仪表盘资源；资源缺失时回退到本地默认预设，保证演示页可打开。 */
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

async function loadEmbeddedDashboard(
  ticket: string,
  resourceType: string,
  resourceKey: string,
  payload: BiEmbedTicketPayload,
): Promise<{
  preset: BiDashboardPresetLike
  runtimeParameters: BiDashboardRuntimeParameters
  queryResults: Record<string, BiQueryResult>
  queryErrors: Record<string, string>
}> {
  // 先取仪表盘资源，再取嵌入运行态参数，保证控件默认值能和服务端记忆状态合并。
  const loadedPreset = await loadDashboardPreset(ticket, resourceType, resourceKey)
  const runtimeState = await biApi.getEmbedDashboardRuntimeState({
    ticket,
    resourceType,
    resourceKey,
  })
  const resolvedRuntimeParameters = dashboardRuntimeParametersFromEmbedPayload(
    loadedPreset,
    payload,
    (runtimeState.data?.parameters ?? null) as BiDashboardRuntimeParameters | null,
  )
  const canvasId = embedPayloadCanvasId(payload)
  // 单组件查询互不阻塞，失败的组件记录错误并继续展示其它组件。
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
  return {
    preset: loadedPreset,
    runtimeParameters: resolvedRuntimeParameters,
    queryResults: Object.fromEntries(queryEntries
      .filter((entry): entry is { widgetKey: string; result: BiQueryResult } => 'result' in entry)
      .map(entry => [entry.widgetKey, entry.result])),
    queryErrors: Object.fromEntries(queryEntries
      .filter((entry): entry is { widgetKey: string; error: string } => 'error' in entry)
      .map(entry => [entry.widgetKey, entry.error])),
  }
}

/** 从 ticket 过滤条件中兼容读取画布 ID。 */
function embedPayloadCanvasId(payload: BiEmbedTicketPayload): string | null {
  return payload.filters?.canvasId ?? payload.filters?.canvas_id ?? null
}

/** 将运行态参数格式化为 header 标签文案。 */
function formatRuntimeParameterValue(value: unknown): string {
  if (Array.isArray(value)) return value.map(item => formatRuntimeParameterValue(item)).filter(Boolean).join(',')
  if (value == null) return ''
  return String(value)
}

function portalTitle(portal: BiPortalResource | null): string | null {
  if (!portal) return null
  return typeof portal.theme?.title === 'string' && portal.theme.title.trim()
    ? portal.theme.title.trim()
    : portal.name
}

function portalSubtitle(portal: BiPortalResource): string | null {
  return typeof portal.theme?.subtitle === 'string' && portal.theme.subtitle.trim()
    ? portal.theme.subtitle.trim()
    : null
}

function portalFooter(portal: BiPortalResource | null): string | null {
  if (!portal) return null
  return typeof portal.theme?.footerText === 'string' && portal.theme.footerText.trim()
    ? portal.theme.footerText.trim()
    : null
}

function EmbedPortal({
  portal,
  ticket,
  payload,
}: {
  portal: BiPortalResource
  ticket: string
  payload: BiEmbedTicketPayload
}) {
  const sortedMenus = [...(portal.menus ?? [])].sort((left, right) => Number(left.sortOrder ?? 0) - Number(right.sortOrder ?? 0))
  const defaultMenuKey = typeof portal.theme?.defaultMenuKey === 'string' ? portal.theme.defaultMenuKey : null
  const defaultMenu = sortedMenus.find(menu => menu.menuKey === defaultMenuKey) ?? sortedMenus[0] ?? null
  const [selectedMenuKey, setSelectedMenuKey] = useState<string | null>(defaultMenu?.menuKey ?? null)
  const selectedMenu = sortedMenus.find(menu => menu.menuKey === selectedMenuKey) ?? defaultMenu
  return (
    <main style={portalMainStyle}>
      <div style={portalLayoutStyle}>
        <Card
          title={(
            <Space direction="vertical" size={0}>
              <Space size={8}>
                <ApiOutlined />
                <Text strong>{portal.name}</Text>
                <Tag color="cyan">{portal.status}</Tag>
              </Space>
              {portalSubtitle(portal) && <Text type="secondary" style={{ fontSize: 12 }}>{portalSubtitle(portal)}</Text>}
            </Space>
          )}
          bordered
          style={{ borderRadius: 8 }}
        >
          {sortedMenus.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无门户菜单" />
          ) : (
            <List
              dataSource={sortedMenus}
              renderItem={menu => (
                <PortalMenuItem
                  menu={menu}
                  selected={menu.menuKey === selectedMenu?.menuKey}
                  onOpen={() => setSelectedMenuKey(menu.menuKey)}
                />
              )}
            />
          )}
        </Card>
        <PortalResourcePanel menu={selectedMenu} ticket={ticket} payload={payload} />
      </div>
    </main>
  )
}

function PortalMenuItem({
  menu,
  selected,
  onOpen,
}: {
  menu: BiPortalMenuResource
  selected: boolean
  onOpen: () => void
}) {
  const iconKey = typeof menu.visibility?.iconKey === 'string' ? menu.visibility.iconKey : null
  return (
    <List.Item>
      <Space direction="vertical" size={4} style={{ width: '100%' }}>
        <Space size={8} wrap>
          <Button
            type={selected ? 'primary' : 'text'}
            size="small"
            aria-label={`打开门户菜单 ${menu.title}`}
            onClick={onOpen}
          >
            {menu.title}
          </Button>
          {iconKey && <Tag color="blue">{iconKey}</Tag>}
          {menu.parentMenuKey && <Tag color="purple">父级 {menu.parentMenuKey}</Tag>}
          <Tag>{menu.resourceType}</Tag>
          {menu.resourceKey && <Text type="secondary" style={{ fontSize: 12 }}>{menu.resourceKey}</Text>}
        </Space>
        {menu.externalUrl && <Text type="secondary" style={{ fontSize: 12 }}>{menu.externalUrl}</Text>}
      </Space>
    </List.Item>
  )
}

function PortalResourcePanel({
  menu,
  ticket,
  payload,
}: {
  menu: BiPortalMenuResource | null
  ticket: string
  payload: BiEmbedTicketPayload
}) {
  if (!menu) {
    return (
      <Card title="当前打开资源" bordered style={{ borderRadius: 8 }}>
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="请选择门户菜单" />
      </Card>
    )
  }
  const resourceSummary = menu.resourceKey
    ? `${menu.resourceType} / ${menu.resourceKey}`
    : menu.resourceType
  if (menu.resourceType === 'DASHBOARD' && menu.resourceKey) {
    return (
      <PortalDashboardResource
        menu={menu}
        ticket={ticket}
        payload={payload}
        resourceSummary={resourceSummary}
      />
    )
  }
  return (
    <Card
      title={(
        <Space size={8} wrap>
          <Text strong>当前打开资源</Text>
          <Tag color="blue">{menu.menuKey}</Tag>
        </Space>
      )}
      bordered
      style={{ borderRadius: 8 }}
    >
      <Space direction="vertical" size={10} style={{ width: '100%' }}>
        <Space direction="vertical" size={2}>
          <Title level={5} style={{ margin: 0 }}>资源：{menu.title}</Title>
          <Text>{resourceSummary}</Text>
          {menu.parentMenuKey && <Text type="secondary">层级 {menu.parentMenuKey} / {menu.menuKey}</Text>}
        </Space>
        {menu.externalUrl ? (
          <a href={menu.externalUrl} target="_blank" rel="noreferrer">打开外部链接</a>
        ) : (
          <Alert
            type="info"
            showIcon
            message="已在门户内选中资源"
            description="门户 ticket 保持在当前资源边界内，资源内容由宿主侧按授权策略加载。"
          />
        )}
      </Space>
    </Card>
  )
}

function PortalDashboardResource({
  menu,
  ticket,
  payload,
  resourceSummary,
}: {
  menu: BiPortalMenuResource
  ticket: string
  payload: BiEmbedTicketPayload
  resourceSummary: string
}) {
  const [preset, setPreset] = useState<BiDashboardPresetLike | null>(null)
  const [queryResults, setQueryResults] = useState<Record<string, BiQueryResult>>({})
  const [queryErrors, setQueryErrors] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    const loadDashboard = async () => {
      if (!menu.resourceKey) return
      setLoading(true)
      setError(null)
      setPreset(null)
      setQueryResults({})
      setQueryErrors({})
      try {
        const loadedDashboard = await loadEmbeddedDashboard(ticket, 'DASHBOARD', menu.resourceKey, payload)
        if (cancelled) return
        setPreset(loadedDashboard.preset)
        setQueryResults(loadedDashboard.queryResults)
        setQueryErrors(loadedDashboard.queryErrors)
      } catch {
        if (!cancelled) setError('门户仪表盘资源加载失败')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    loadDashboard()
    return () => {
      cancelled = true
    }
  }, [menu.resourceKey, payload, ticket])

  return (
    <Space direction="vertical" size={10} style={{ width: '100%' }}>
      <Card
        title={(
          <Space size={8} wrap>
            <Text strong>当前打开资源</Text>
            <Tag color="blue">{menu.menuKey}</Tag>
          </Space>
        )}
        bordered
        style={{ borderRadius: 8 }}
      >
        <Space direction="vertical" size={2}>
          <Title level={5} style={{ margin: 0 }}>资源：{menu.title}</Title>
          <Text>{resourceSummary}</Text>
          {menu.parentMenuKey && <Text type="secondary">层级 {menu.parentMenuKey} / {menu.menuKey}</Text>}
        </Space>
      </Card>
      {error ? (
        <Alert type="warning" showIcon message={error} />
      ) : loading ? (
        <Card bordered style={{ borderRadius: 8 }}>
          <Spin />
        </Card>
      ) : (
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
      )}
    </Space>
  )
}

/** 嵌入页单个仪表盘组件容器，负责布局定位和查询状态标签。 */
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

/** 根据组件类型和查询结果渲染简化版图表预览。 */
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
    // 单组件查询失败只降级当前卡片，不影响整张嵌入仪表盘。
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
    // KPI 卡片以首行首指标作为主值，成功率类指标按百分比展示。
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
    // 折线图只渲染前两个指标，保持嵌入预览轻量。
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
    // 柱状图取前 6 行，按当前结果最大值归一化高度。
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
  // 其它图表类型兜底为小表格，便于确认查询结果仍然可见。
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

/** 将数值数组转换为 SVG polyline points。 */
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

/** 将未知查询值安全转换为有限数字。 */
function toNumber(value: unknown): number {
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : 0
  }
  return 0
}

/** 按指标命名约定格式化数值或百分比。 */
function formatMetricValue(metric: string, value: number): string {
  if (metric.includes('rate') || metric.includes('ratio') || metric.includes('percent')) {
    return `${(value <= 1 ? value * 100 : value).toFixed(1)}%`
  }
  return Math.round(value).toLocaleString()
}

/** 格式化表格单元格，空值展示为占位符。 */
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

const portalMainStyle: CSSProperties = {
  display: 'grid',
  gap: 10,
}

const portalLayoutStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'minmax(260px, 360px) minmax(0, 1fr)',
  gap: 12,
  alignItems: 'start',
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
