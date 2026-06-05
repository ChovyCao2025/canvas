/**
 * 页面职责：首页运营仪表盘，展示画布、任务、通知、导入和人群计算等关键指标。
 *
 * 维护说明：页面聚合多个业务域的概览信息，用于登录后的快速巡检。
 */
import { useCallback, useEffect, useMemo, useState, type CSSProperties, type ReactNode } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Input,
  List,
  Row,
  Segmented,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd'
import {
  ApiOutlined,
  ApartmentOutlined,
  BarChartOutlined,
  LineChartOutlined,
  NotificationOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SettingOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { homeApi } from '../../services/api'
import {
  buildKpiCards,
  buildRiskSummary,
  filterHomeOverview,
  formatHomeRangeLabel,
  getAttentionAction,
  getAttentionPresentation,
  HOME_RANGE_OPTIONS,
  sortAttentionItems,
  type HomeAttentionItem,
  type HomeOverview,
  type HomeTopCanvas,
  type KpiCard,
  type RiskSummary,
} from './homeOverview'

/** 首页常用文本组件别名。 */
const { Text, Title } = Typography

/** 首页仪表盘主组件，按时间范围聚合展示运营健康度。 */
export default function HomePage() {
  const navigate = useNavigate()
  // days 控制所有概览指标的统计窗口，切换后统一重新拉取后端聚合数据。
  const [days, setDays] = useState<number>(7)
  const [overview, setOverview] = useState<HomeOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [keyword, setKeyword] = useState('')

  /** 拉取首页聚合数据；失败时保留旧 overview，仅展示错误提示。 */
  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await homeApi.overview(days)
      setOverview(res.data)
    } catch (err) {
      setError(err instanceof Error ? err.message : '首页数据加载失败')
    } finally {
      setLoading(false)
    }
  }, [days])

  useEffect(() => { load() }, [load])

  // KPI 卡片展示模型由纯函数生成，避免 JSX 中混入单位、图标和颜色规则。
  const kpiCards = useMemo(() => overview ? buildKpiCards(overview) : [], [overview])
  const visibleOverview = useMemo(
    () => overview ? filterHomeOverview(overview, keyword) : null,
    [overview, keyword],
  )
  const riskSummary = useMemo(() => overview ? buildRiskSummary(overview) : null, [overview])
  const attentionItems = useMemo(
    () => visibleOverview ? sortAttentionItems(visibleOverview.attentionItems) : [],
    [visibleOverview],
  )

  const openAttentionItem = useCallback((item: HomeAttentionItem) => {
    if (item.canvasId <= 0) {
      navigate('/canvas')
      return
    }
    navigate(attentionUrl(item))
  }, [navigate])

  return (
    <div className="home-dashboard" style={pageStyle}>
      <style>{homeResponsiveStyle}</style>
      {/* 页头：时间范围和刷新动作影响下方全部模块。 */}
      <div style={headerStyle}>
        <div>
          <Space size={10} align="center">
            <div style={brandIconStyle}>
              <ApartmentOutlined />
            </div>
            <Title level={3} style={{ margin: 0, color: '#0f172a' }}>运营驾驶舱</Title>
          </Space>
          <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
            当前聚焦{formatHomeRangeLabel(days)}活动表现，快速定位有效旅程和待处理异常
          </Text>
        </div>
        <Space className="home-header-actions" wrap style={{ justifyContent: 'flex-end' }}>
          <Input
            allowClear
            prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
            placeholder="搜索当前首页旅程"
            value={keyword}
            onChange={event => setKeyword(event.target.value)}
            style={{ width: 240 }}
          />
          <Segmented
            className="home-range-control"
            value={days}
            options={HOME_RANGE_OPTIONS.map(item => ({ label: item.label, value: item.value }))}
            onChange={value => setDays(Number(value))}
          />
          <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/canvas')}>新建画布</Button>
        </Space>
      </div>

      {/* 错误提示不替代已有数据，便于用户在短暂失败时仍可查看上一次结果。 */}
      {error && (
        <Alert
          type="error"
          showIcon
          message="首页数据加载失败"
          description={error}
          action={<Button size="small" onClick={load}>重试</Button>}
          style={{ marginBottom: 18 }}
        />
      )}

      {loading && !overview ? (
        <Spin size="large" style={{ display: 'block', margin: '96px auto' }} />
      ) : overview && visibleOverview && riskSummary ? (
        <>
          <RiskSummaryBanner summary={riskSummary} onOpen={() => navigate(riskSummary.targetCanvasId ? `/canvas/${riskSummary.targetCanvasId}/stats` : '/canvas')} />

          {/* KPI 区：快速展示画布、任务、人群、导入等核心指标。 */}
          <Row gutter={[14, 14]} style={{ marginBottom: 18 }}>
            {kpiCards.map(card => (
              <Col key={card.key} xs={24} sm={12} lg={8} xl={4} flex="1 1 180px" style={{ minWidth: 0 }}>
                <KpiSurface card={card} />
              </Col>
            ))}
          </Row>

          {/* 趋势 + 待关注事项：左侧看整体变化，右侧提示需要处理的问题。 */}
          <Row gutter={[16, 16]} style={{ marginBottom: 18 }}>
            <Col xs={24} xl={16}>
              <Card title="执行健康趋势" variant="borderless" style={cardStyle} styles={{ body: { paddingTop: 6 } }}>
                {overview.trend.length === 0 ? (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无趋势数据" />
                ) : (
                  <ResponsiveContainer width="100%" height={260}>
                    <AreaChart data={overview.trend} margin={{ top: 10, right: 16, bottom: 0, left: 0 }}>
                      <defs>
                        <linearGradient id="homeTotal" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#2563eb" stopOpacity={0.22} />
                          <stop offset="95%" stopColor="#2563eb" stopOpacity={0} />
                        </linearGradient>
                        <linearGradient id="homeFailed" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#e11d48" stopOpacity={0.18} />
                          <stop offset="95%" stopColor="#e11d48" stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                      <XAxis dataKey="date" tick={{ fontSize: 11, fill: '#64748b' }}
                        tickFormatter={value => String(value).slice(5)} axisLine={false} tickLine={false} />
                      <YAxis tick={{ fontSize: 11, fill: '#64748b' }} width={42} axisLine={false} tickLine={false} />
                      <Tooltip />
                      <Legend />
                      <Area type="monotone" dataKey="total" name="触发次数"
                        stroke="#2563eb" strokeWidth={2.4} fill="url(#homeTotal)" isAnimationActive={false} />
                      <Area type="monotone" dataKey="failed" name="失败次数"
                        stroke="#e11d48" strokeWidth={2.2} fill="url(#homeFailed)" isAnimationActive={false} />
                    </AreaChart>
                  </ResponsiveContainer>
                )}
              </Card>
            </Col>
            <Col xs={24} xl={8}>
              <Card title="异常队列" variant="borderless" style={cardStyle}>
                {attentionItems.length === 0 ? (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无需要关注的旅程" />
                ) : (
                  <List
                    dataSource={attentionItems}
                    renderItem={item => <AttentionItem item={item} onOpen={() => openAttentionItem(item)} />}
                  />
                )}
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={14}>
              <Card title="Top 旅程表现" variant="borderless" style={cardStyle}
                extra={<Button type="link" onClick={() => navigate('/canvas')}>查看全部</Button>}>
                {visibleOverview.topCanvases.length === 0 ? (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前范围暂无触发数据" />
                ) : (
                  <List
                    dataSource={visibleOverview.topCanvases}
                    renderItem={(item, index) => <TopCanvasItem item={item} rank={index + 1} onOpen={() => navigate(`/canvas/${item.canvasId}/stats`)} />}
                  />
                )}
              </Card>
            </Col>
            <Col xs={24} xl={10}>
              <Card title="常用动作" variant="borderless" style={cardStyle}>
                <div className="home-quick-actions" style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
                  <QuickAction icon={<PlusOutlined />} label="新建画布" color="#2563eb" onClick={() => navigate('/canvas')} />
                  <QuickAction icon={<BarChartOutlined />} label="旅程管理" color="#7c3aed" onClick={() => navigate('/canvas')} />
                  <QuickAction icon={<TeamOutlined />} label="人群管理" color="#059669" onClick={() => navigate('/audiences')} />
                  <QuickAction icon={<ApiOutlined />} label="API 配置" color="#dc2626" onClick={() => navigate('/api-config')} />
                  <QuickAction icon={<NotificationOutlined />} label="MQ 配置" color="#d97706" onClick={() => navigate('/mq-config')} />
                  <QuickAction icon={<SettingOutlined />} label="事件配置" color="#0891b2" onClick={() => navigate('/event-config')} />
                </div>
              </Card>
            </Col>
          </Row>
        </>
      ) : null}
    </div>
  )
}

/** 风险摘要横幅，使用原始 overview 聚合结果，不受本地搜索影响。 */
function RiskSummaryBanner({ summary, onOpen }: { summary: RiskSummary; onOpen: () => void }) {
  const presentation = getAttentionPresentation(summary.severity)
  const tone = getRiskBannerTone(summary)
  const title = summary.healthy ? summary.title : `最高风险：${summary.title}`
  const message = summary.healthy ? summary.message : `建议优先处理：${summary.message}`
  return (
    <div className="home-risk-banner" style={{
      ...riskBannerStyle,
      borderColor: tone.borderColor,
      background: tone.background,
    }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, minWidth: 0 }}>
        <div style={{
          ...riskIconStyle,
          color: tone.iconColor,
          background: tone.iconBackground,
        }}>
          <LineChartOutlined />
        </div>
        <div style={{ minWidth: 0 }}>
          <Space size={8} wrap>
            <Text style={{ color: '#0f172a', fontSize: 16, fontWeight: 700 }}>{title}</Text>
            <Tag color={presentation.color}>{presentation.label}</Tag>
          </Space>
          <Text type="secondary" style={{ display: 'block', marginTop: 6 }}>{message}</Text>
        </div>
      </div>
      <Space className="home-risk-metrics" size={18} wrap style={{ justifyContent: 'flex-end' }}>
        <Metric label="待处理" value={summary.pendingCount.toLocaleString()} />
        <Metric label="失败次数" value={summary.failedExecutions} />
        <Metric label="成功率" value={summary.successRate} />
        <Button onClick={onOpen}>{summary.actionLabel}</Button>
      </Space>
    </div>
  )
}

function getRiskBannerTone(summary: RiskSummary) {
  if (summary.healthy) {
    return {
      borderColor: '#bbf7d0',
      background: '#f0fdf4',
      iconColor: '#16a34a',
      iconBackground: '#dcfce7',
    }
  }
  if (summary.severity === 'error') {
    return {
      borderColor: '#fecaca',
      background: '#fef2f2',
      iconColor: '#dc2626',
      iconBackground: '#fee2e2',
    }
  }
  return {
    borderColor: '#fed7aa',
    background: '#fff7ed',
    iconColor: '#ea580c',
    iconBackground: '#ffedd5',
  }
}

/** KPI 白色材料卡片，颜色仅作为图标和状态强调。 */
function KpiSurface({ card }: { card: KpiCard }) {
  return (
    <div style={kpiSurfaceStyle}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{
          width: 36,
          height: 36,
          borderRadius: 8,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: card.iconBg,
          color: card.color,
          fontSize: 17,
          flexShrink: 0,
        }}>
          {card.icon}
        </div>
        <div style={{ minWidth: 0 }}>
          <Text style={{ color: '#64748b', fontWeight: 500, fontSize: 12, lineHeight: 1.3 }}>
            {card.label}
          </Text>
          <Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>{card.hint}</Text>
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'end', justifyContent: 'space-between', gap: 10 }}>
        <div style={{ color: '#0f172a', fontSize: 28, lineHeight: 1, fontWeight: 700, whiteSpace: 'nowrap' }}>
          {card.value}
        </div>
        <div style={{ width: 36, height: 3, borderRadius: 999, background: card.color }} />
      </div>
    </div>
  )
}

/** 快捷入口按钮，封装图标、文字和主题色，保持首页操作区布局一致。 */
function QuickAction({ icon, label, color, onClick }: {
  icon: ReactNode
  label: string
  color: string
  onClick: () => void
}) {
  return (
    <Button
      onClick={onClick}
      style={{
        height: 74,
        borderRadius: 8,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
        color,
        borderColor: 'rgba(15,23,42,.08)',
        background: '#fff',
      }}
      icon={<span style={{ fontSize: 18 }}>{icon}</span>}
    >
      <span style={{ color: '#334155', fontWeight: 600 }}>{label}</span>
    </Button>
  )
}

/** 需要关注列表项，按严重程度展示标签并提供跳转入口。 */
function AttentionItem({ item, onOpen }: { item: HomeAttentionItem; onOpen: () => void }) {
  const presentation = getAttentionPresentation(item.severity)
  const action = getAttentionAction(item)
  return (
    <List.Item
      actions={[<Button key="open" type="link" onClick={onOpen}>{action.label}</Button>]}
      style={{ padding: '12px 0' }}
    >
      <List.Item.Meta
        title={<Space size={8}><span>{item.name}</span><Tag color={presentation.color}>{presentation.label}</Tag></Space>}
        description={<Text type="secondary">{item.message}</Text>}
      />
    </List.Item>
  )
}

/** Top 旅程列表项，替代旧表格并保留关键统计。 */
function TopCanvasItem({ item, rank, onOpen }: { item: HomeTopCanvas; rank: number; onOpen: () => void }) {
  return (
    <List.Item
      actions={[<Button key="open" type="link" onClick={onOpen}>查看</Button>]}
      style={{ padding: '13px 0' }}
    >
      <List.Item.Meta
        avatar={<div style={rankStyle}>{rank}</div>}
        title={<Button type="link" style={{ padding: 0, height: 'auto', fontWeight: 700 }} onClick={onOpen}>{item.name}</Button>}
        description={(
          <Space size={18} wrap>
            <Metric label="触发" value={item.total.toLocaleString()} compact />
            <Metric label="用户" value={item.uniqueUsers.toLocaleString()} compact />
            <Metric label="成功率" value={item.successRate} compact />
            <Metric label="失败" value={item.failed.toLocaleString()} compact danger={item.failed > 0} />
          </Space>
        )}
      />
    </List.Item>
  )
}

function Metric({ label, value, compact = false, danger = false }: {
  label: string
  value: string
  compact?: boolean
  danger?: boolean
}) {
  return (
    <span style={{ display: 'inline-flex', flexDirection: compact ? 'row' : 'column', gap: compact ? 5 : 2 }}>
      <Text type="secondary" style={{ fontSize: 12 }}>{label}</Text>
      <Text style={{ color: danger ? '#dc2626' : '#0f172a', fontWeight: 700 }}>{value}</Text>
    </span>
  )
}

/** 根据关注项类型返回目标页：无执行记录去编辑页，其余去统计页。 */
function attentionUrl(item: HomeAttentionItem) {
  if (item.canvasId <= 0) return '/canvas'
  if (item.type === 'NO_RECENT_EXECUTIONS') return `/canvas/${item.canvasId}/edit`
  return `/canvas/${item.canvasId}/stats`
}

const homeResponsiveStyle = `
  @media (max-width: 767px) {
    .home-dashboard {
      padding: 18px 0 28px !important;
    }

    .home-dashboard .home-header-actions {
      width: 100%;
      justify-content: flex-start !important;
      row-gap: 8px;
    }

    .home-dashboard .home-header-actions .ant-space-item {
      max-width: 100%;
    }

    .home-dashboard .home-header-actions .ant-input-affix-wrapper,
    .home-dashboard .home-range-control {
      width: 100% !important;
    }

    .home-dashboard .home-range-control .ant-segmented-group {
      width: 100%;
    }

    .home-dashboard .home-range-control .ant-segmented-item {
      flex: 1;
      text-align: center;
    }

    .home-dashboard .home-risk-banner {
      align-items: flex-start !important;
      padding: 14px !important;
    }

    .home-dashboard .home-risk-metrics {
      width: 100%;
      justify-content: space-between !important;
      gap: 12px !important;
    }

    .home-dashboard .home-risk-metrics .ant-space-item:last-child,
    .home-dashboard .home-risk-metrics .ant-btn {
      width: 100%;
    }

    .home-dashboard .home-quick-actions {
      grid-template-columns: 1fr !important;
    }

    .home-dashboard .ant-card-head {
      min-height: 48px;
      padding: 0 16px;
    }

    .home-dashboard .ant-card-body {
      padding: 16px !important;
    }
  }
`

const pageStyle: CSSProperties = {
  minHeight: '100vh',
  background: '#f6f8fb',
  padding: '24px 28px 32px',
}

const headerStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'flex-start',
  justifyContent: 'space-between',
  gap: 16,
  marginBottom: 18,
  flexWrap: 'wrap',
}

const brandIconStyle: CSSProperties = {
  width: 38,
  height: 38,
  borderRadius: 8,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: 'linear-gradient(135deg, #2563eb 0%, #14b8a6 100%)',
  color: '#fff',
  boxShadow: '0 8px 22px rgba(37,99,235,.24)',
}

const riskBannerStyle: CSSProperties = {
  border: '1px solid',
  borderRadius: 8,
  padding: '16px 18px',
  marginBottom: 18,
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  gap: 16,
  flexWrap: 'wrap',
}

const riskIconStyle: CSSProperties = {
  width: 36,
  height: 36,
  borderRadius: 8,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontSize: 18,
  flexShrink: 0,
}

const kpiSurfaceStyle: CSSProperties = {
  minHeight: 130,
  height: '100%',
  borderRadius: 8,
  padding: '18px 20px',
  background: '#fff',
  border: '1px solid #e5e7eb',
  boxShadow: '0 8px 22px rgba(15,23,42,.05)',
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'space-between',
}

const rankStyle: CSSProperties = {
  width: 28,
  height: 28,
  borderRadius: 8,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  color: '#2563eb',
  background: '#eff6ff',
  fontWeight: 700,
}

/** 首页业务卡片通用样式，保持各模块边框和阴影一致。 */
const cardStyle: CSSProperties = {
  height: '100%',
  borderRadius: 8,
  border: '1px solid #e5e7eb',
  boxShadow: '0 8px 22px rgba(15,23,42,.05)',
}
