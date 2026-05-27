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
  List,
  Row,
  Segmented,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd'
import {
  ApiOutlined,
  ApartmentOutlined,
  BarChartOutlined,
  DatabaseOutlined,
  NotificationOutlined,
  PlusOutlined,
  ReloadOutlined,
  TeamOutlined,
  ThunderboltOutlined,
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
import type { ColumnsType } from 'antd/es/table'
import { homeApi } from '../../services/api'
import {
  buildKpiCards,
  getAttentionPresentation,
  HOME_RANGE_OPTIONS,
  type HomeAttentionItem,
  type HomeOverview,
  type HomeTopCanvas,
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

  /** TOP 旅程表格列；点击旅程名进入对应画布统计页。 */
  const topColumns: ColumnsType<HomeTopCanvas> = [
    {
      title: '旅程',
      dataIndex: 'name',
      ellipsis: true,
      render: (name: string, row) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => navigate(`/canvas/${row.canvasId}/stats`)}>
          {name}
        </Button>
      ),
    },
    { title: '触发', dataIndex: 'total', width: 90, align: 'right', render: (v: number) => v.toLocaleString() },
    { title: '用户', dataIndex: 'uniqueUsers', width: 90, align: 'right', render: (v: number) => v.toLocaleString() },
    { title: '成功率', dataIndex: 'successRate', width: 90, align: 'right',
      render: (v: string) => <Text style={{ color: '#16a34a', fontWeight: 600 }}>{v}</Text> },
    { title: '失败', dataIndex: 'failed', width: 80, align: 'right',
      render: (v: number) => v > 0 ? <Text type="danger">{v}</Text> : <Text type="secondary">0</Text> },
  ]

  return (
    <div style={{ minHeight: '100vh', background: '#f6f8fb', padding: '24px 28px 32px' }}>
      {/* 页头：时间范围和刷新动作影响下方全部模块。 */}
      <div style={{
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        gap: 16,
        marginBottom: 22,
        flexWrap: 'wrap',
      }}>
        <div>
          <Space size={10} align="center">
            <div style={{
              width: 38,
              height: 38,
              borderRadius: 10,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              background: 'linear-gradient(135deg, #2563eb 0%, #14b8a6 100%)',
              color: '#fff',
              boxShadow: '0 8px 22px rgba(37,99,235,.24)',
            }}>
              <ApartmentOutlined />
            </div>
            <Title level={3} style={{ margin: 0, color: '#0f172a' }}>运营驾驶舱</Title>
          </Space>
          <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
            默认聚焦近 7 天活动表现，快速定位有效旅程和待处理异常
          </Text>
        </div>
        <Space wrap>
          <Segmented
            value={days}
            options={HOME_RANGE_OPTIONS.map(item => ({ label: item.label, value: item.value }))}
            onChange={value => setDays(Number(value))}
          />
          <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
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
      ) : overview ? (
        <>
          {/* KPI 区：快速展示画布、任务、人群、导入等核心指标。 */}
          <Row gutter={[14, 14]} style={{ marginBottom: 18 }}>
            {kpiCards.map(card => (
              <Col key={card.key} flex={1} style={{ minWidth: 0, display: 'flex' }}>
                <div style={{
                  flex: 1,
                  minHeight: 130,
                  borderRadius: 12,
                  padding: '18px 20px',
                  background: card.bg,
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div style={{
                      width: 36,
                      height: 36,
                      borderRadius: 10,
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
              </Col>
            ))}
          </Row>

          {/* 趋势 + 待关注事项：左侧看整体变化，右侧提示需要处理的问题。 */}
          <Row gutter={[16, 16]} style={{ marginBottom: 18 }}>
            <Col xs={24} xl={16}>
              <Card title="每日触发趋势" bordered={false} style={cardStyle} styles={{ body: { paddingTop: 6 } }}>
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
                        stroke="#2563eb" strokeWidth={2.4} fill="url(#homeTotal)" />
                      <Area type="monotone" dataKey="failed" name="失败次数"
                        stroke="#e11d48" strokeWidth={2.2} fill="url(#homeFailed)" />
                    </AreaChart>
                  </ResponsiveContainer>
                )}
              </Card>
            </Col>
            <Col xs={24} xl={8}>
              <Card title="快捷入口" bordered={false} style={cardStyle}>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
                  <QuickAction icon={<PlusOutlined />} label="新建画布" color="#2563eb" onClick={() => navigate('/canvas')} />
                  <QuickAction icon={<BarChartOutlined />} label="旅程管理" color="#7c3aed" onClick={() => navigate('/canvas')} />
                  <QuickAction icon={<TeamOutlined />} label="人群管理" color="#059669" onClick={() => navigate('/audiences')} />
                  <QuickAction icon={<ApiOutlined />} label="API 配置" color="#dc2626" onClick={() => navigate('/api-config')} />
                  <QuickAction icon={<NotificationOutlined />} label="MQ 配置" color="#d97706" onClick={() => navigate('/mq-config')} />
                  <QuickAction icon={<ThunderboltOutlined />} label="事件配置" color="#0891b2" onClick={() => navigate('/event-config')} />
                </div>
                <Button block icon={<DatabaseOutlined />} style={{ marginTop: 12 }} onClick={() => navigate('/data-source-config')}>
                  数据源配置
                </Button>
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={14}>
              <Card title="Top 旅程" bordered={false} style={cardStyle}
                extra={<Button type="link" onClick={() => navigate('/canvas')}>查看全部</Button>}>
                {overview.topCanvases.length === 0 ? (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前范围暂无触发数据" />
                ) : (
                  <Table
                    rowKey="canvasId"
                    size="small"
                    pagination={false}
                    columns={topColumns}
                    dataSource={overview.topCanvases}
                    scroll={{ x: 640 }}
                  />
                )}
              </Card>
            </Col>
            <Col xs={24} xl={10}>
              <Card title="需要关注" bordered={false} style={cardStyle}>
                {overview.attentionItems.length === 0 ? (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无需要关注的旅程" />
                ) : (
                  <List
                    dataSource={overview.attentionItems}
                    renderItem={item => <AttentionItem item={item} onOpen={() => navigate(attentionUrl(item))} />}
                  />
                )}
              </Card>
            </Col>
          </Row>
        </>
      ) : null}
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
  return (
    <List.Item
      actions={[<Button key="open" type="link" onClick={onOpen}>查看</Button>]}
      style={{ padding: '12px 0' }}
    >
      <List.Item.Meta
        title={<Space size={8}><span>{item.name}</span><Tag color={presentation.color}>{presentation.label}</Tag></Space>}
        description={<Text type="secondary">{item.message}</Text>}
      />
    </List.Item>
  )
}

/** 根据关注项类型返回目标页：无执行记录去编辑页，其余去统计页。 */
function attentionUrl(item: HomeAttentionItem) {
  if (item.type === 'NO_RECENT_EXECUTIONS') return `/canvas/${item.canvasId}/edit`
  return `/canvas/${item.canvasId}/stats`
}

/** 首页业务卡片通用样式，保持各模块边框和阴影一致。 */
const cardStyle: CSSProperties = {
  height: '100%',
  borderRadius: 8,
  border: '1px solid #e5e7eb',
  boxShadow: '0 8px 22px rgba(15,23,42,.05)',
}
