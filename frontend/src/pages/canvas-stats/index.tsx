/**
 * 页面职责：画布统计页，展示执行 KPI、趋势图和节点漏斗。
 *
 * 维护说明：统计数据从多个接口并行加载，页面负责聚合展示和空态降级。
 */
import { useEffect, useState, useCallback } from 'react'
import { Card, Col, Row, Table, Typography, Spin, Button, DatePicker, Space, Tag, Modal, message } from 'antd'
import { ArrowLeftOutlined, DownOutlined, UpOutlined, CalendarOutlined, TeamOutlined, CheckCircleOutlined, CloseCircleOutlined, ThunderboltOutlined, PauseCircleOutlined, BarChartOutlined, DownloadOutlined, ReloadOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'
import type { ColumnsType } from 'antd/es/table'
import dayjs, { type Dayjs } from 'dayjs'
import http, { canvasApi } from '../../services/api'
import type { R } from '../../types'
import { operatorApi, type ExecutionRequestRow, type MessageSendRecordRow } from '../../services/operatorApi'
import { canvasBiEntrypoint } from '../bi/biWorkbench'
import {
  buildAttributionKpis,
  buildReceiptStatusRows,
  type AttributionKpi,
  type ReceiptStatusRow,
} from './effectClosure'
import { buildOperatorTableQuery, canExportSynchronously, OPERATION_COLUMN } from './operatorTables'

/** 统计页标题和辅助文本组件别名。 */
const { Title, Text } = Typography
/** 日期范围选择器别名，用于切换统计窗口。 */
const { RangePicker } = DatePicker

/** 统计总览数据。 */
interface StatsData {
  /** 触发总次数。 */
  total: number

  /** 成功次数。 */
  success: number

  /** 失败次数。 */
  failed: number

  /** 挂起次数。 */
  paused: number

  /** 成功率（服务端已格式化）。 */
  successRate: string

  /** 去重用户数。 */
  uniqueUsers: number
}

/** 节点漏斗行数据。 */
interface FunnelRow {
  /** 节点 ID。 */
  nodeId: string

  /** 节点类型编码。 */
  nodeType: string

  /** 节点展示名。 */
  nodeName: string

  /** 进入次数。 */
  totalEntered: number

  /** 成功次数。 */
  totalSuccess: number

  /** 失败次数。 */
  totalFailed: number

  /** 跳过次数。 */
  totalSkipped: number

  /** 平均耗时（秒）。 */
  avgDurationSec: number
}

/** 趋势图单日数据点。 */
interface TrendPoint {
  /** 日期（YYYY-MM-DD）。 */
  date: string

  /** 当日执行次数。 */
  count: number
}

/**
 * Dayjs -> API 日期字符串（YYYY-MM-DD）。
 */
function formatDate(d: Dayjs) { return d.format('YYYY-MM-DD') }

/**
 * KPI 环比徽章：
 * curr 当前值，prev 对比区间值。
 */
function CompBadge({ curr, prev }: { curr: number; prev: number | null }) {
  if (prev === null) return <span style={{ display: 'inline-block', height: 21 }} />
  const delta = curr - prev
  const pct = Math.round(Math.abs(delta) / Math.max(prev, 1) * 100)
  if (delta === 0) return <span style={BS.neutral}>↑ 0% vs 上期</span>
  const up = delta > 0
  return <span style={up ? BS.up : BS.down}>{up ? '↑' : '↓'} {pct}% vs 上期</span>
}

/** 环比徽章样式集合，按上升、下降和持平分别着色。 */
const BS = {
  up:      { display:'inline-flex', gap:3, background:'#dcfce7', borderRadius:20, padding:'2px 10px', fontSize:11, color:'#16a34a', fontWeight:600 },
  down:    { display:'inline-flex', gap:3, background:'#fee2e2', borderRadius:20, padding:'2px 10px', fontSize:11, color:'#dc2626', fontWeight:600 },
  neutral: { display:'inline-flex', gap:3, padding:'2px 0', fontSize:11, color:'#94a3b8', fontWeight:500 },
}

/**
 * KPI 卡片定义：
 * key 对应 StatsData 字段，format 负责值展示。
 */
const KPI_DEFS = [
  { key:'uniqueUsers', label:'触达用户数', bg:'#eff6ff', iconBg:'#dbeafe', icon:<TeamOutlined style={{ fontSize:16, color:'#3b82f6' }} />, format:(v:number|string) => Number(v).toLocaleString() },
  { key:'successRate', label:'执行成功率', bg:'#f0fdf4', iconBg:'#dcfce7', icon:<CheckCircleOutlined style={{ fontSize:16, color:'#22c55e' }} />, format:(v:number|string) => String(v) },
  { key:'failed',      label:'执行失败',   bg:'#fff1f2', iconBg:'#fee2e2', icon:<CloseCircleOutlined style={{ fontSize:16, color:'#ef4444' }} />, format:(v:number|string) => Number(v).toLocaleString() },
  { key:'total',       label:'总触发次数', bg:'#faf5ff', iconBg:'#ede9fe', icon:<ThunderboltOutlined style={{ fontSize:16, color:'#8b5cf6' }} />, format:(v:number|string) => Number(v).toLocaleString() },
  { key:'paused',      label:'挂起中',     bg:'#fffbeb', iconBg:'#fef3c7', icon:<PauseCircleOutlined style={{ fontSize:16, color:'#f59e0b' }} />, format:(v:number|string) => Number(v).toLocaleString() },
]

// 默认展示的关键节点类型（去掉 END）
const KEY_NODE_TYPES = new Set(['TRIGGER','CRON_TRIGGER','MQ_TRIGGER','SEND_MQ','API_CALL'])

/** 常用统计时间范围快捷项。 */
const PRESETS: { label: string; value: [Dayjs, Dayjs] }[] = [
  { label:'最近7天',   value:[dayjs().subtract(6,'day'), dayjs()] },
  { label:'最近30天',  value:[dayjs().subtract(29,'day'), dayjs()] },
  { label:'最近90天',  value:[dayjs().subtract(89,'day'), dayjs()] },
  { label:'最近180天', value:[dayjs().subtract(179,'day'), dayjs()] },
  { label:'最近1年',   value:[dayjs().subtract(364,'day'), dayjs()] },
]

function escapeCsvValue(value: unknown) {
  const text = value == null ? '' : String(value)
  return `"${text.replace(/"/g, '""')}"`
}

function downloadRowsCsv(fileName: string, rows: Record<string, unknown>[]) {
  if (rows.length === 0) {
    message.info('暂无可导出数据')
    return
  }

  const headers = Object.keys(rows[0])
  const csv = [
    headers.map(escapeCsvValue).join(','),
    ...rows.map(row => headers.map(header => escapeCsvValue(row[header])).join(',')),
  ].join('\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

/**
 * 画布统计页：
 * 1) 查询统计总览；
 * 2) 查询上期对比；
 * 3) 查询节点漏斗；
 * 4) 查询按天趋势。
 */
export default function CanvasStatsPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [range, setRange] = useState<[Dayjs, Dayjs]>([dayjs().subtract(6, 'day'), dayjs()])
  const [stats, setStats] = useState<StatsData | null>(null)
  const [prevStats, setPrev] = useState<StatsData | null>(null)
  const [funnel, setFunnel] = useState<FunnelRow[]>([])
  const [trend, setTrend] = useState<TrendPoint[]>([])
  const [receiptRows, setReceiptRows] = useState<ReceiptStatusRow[]>([])
  const [attributionKpis, setAttributionKpis] = useState<AttributionKpi[]>([])
  const [executionRequestRows, setExecutionRequestRows] = useState<ExecutionRequestRow[]>([])
  const [executionRequestTotal, setExecutionRequestTotal] = useState(0)
  const [sendRecordRows, setSendRecordRows] = useState<MessageSendRecordRow[]>([])
  const [sendRecordTotal, setSendRecordTotal] = useState(0)
  const [operatorLoading, setOperatorLoading] = useState(false)
  const [loading, setLoading] = useState(true)
  const [expanded, setExpanded] = useState(false)

  // 主加载流程：并行拉取 4 份统计数据
  const load = useCallback(() => {
    if (!id) return
    const [start, end] = range
    const since     = formatDate(start)
    const until     = formatDate(end)
    const days      = end.diff(start,'day') + 1
    const prevSince = formatDate(start.subtract(days,'day'))
    const prevUntil = formatDate(start.subtract(1,'day'))
    setLoading(true)
    Promise.all([
      http.get<R<StatsData>,R<StatsData>>(`/canvas/${id}/stats?since=${since}&until=${until}`),
      http.get<R<StatsData>,R<StatsData>>(`/canvas/${id}/stats?since=${prevSince}&until=${prevUntil}`),
      http.get<R<FunnelRow[]>,R<FunnelRow[]>>(`/canvas/${id}/funnel`),
      http.get<R<TrendPoint[]>,R<TrendPoint[]>>(`/canvas/${id}/trend?since=${since}&until=${until}`),
      canvasApi.receipts(Number(id)),
      canvasApi.attributionSummary(Number(id)),
    ]).then(([s,ps,f,t,receipts,attribution]) => {
      setStats(s.data); setPrev(ps.data); setFunnel(f.data); setTrend(t.data)
      setReceiptRows(buildReceiptStatusRows(receipts.data ?? {}))
      setAttributionKpis(buildAttributionKpis(attribution.data))
    }).finally(() => setLoading(false))
  }, [id, range])

  useEffect(() => { load() }, [load])

  const loadOperatorTables = useCallback(() => {
    if (!id) return
    setOperatorLoading(true)
    const params = buildOperatorTableQuery({ canvasId: Number(id), page: 1, size: 20 })
    Promise.all([
      operatorApi.executionRequests(params),
      operatorApi.messageSendRecords(params),
    ]).then(([requests, records]) => {
      setExecutionRequestRows(requests.data?.list ?? [])
      setExecutionRequestTotal(requests.data?.total ?? 0)
      setSendRecordRows(records.data?.list ?? [])
      setSendRecordTotal(records.data?.total ?? 0)
    }).catch(() => {
      message.error('运营排查数据加载失败')
    }).finally(() => setOperatorLoading(false))
  }, [id])

  useEffect(() => { loadOperatorTables() }, [loadOperatorTables])

  const replayExecutionRequest = useCallback((row: ExecutionRequestRow) => {
    operatorApi.replayExecutionRequest(row.id, 'operator-table-replay')
      .then(() => {
        message.success('已提交重放')
        loadOperatorTables()
      })
      .catch(() => message.error('重放失败'))
  }, [loadOperatorTables])

  const exportOperatorRows = useCallback((fileName: string, total: number, rows: Record<string, unknown>[]) => {
    if (!canExportSynchronously(total)) {
      message.warning('当前结果超过 5000 行，请缩小筛选范围后导出')
      return
    }
    downloadRowsCsv(fileName, rows)
  }, [])

  // 默认只展示关键节点，展开后展示完整漏斗
  const displayFunnel = expanded ? funnel : funnel.filter(r => KEY_NODE_TYPES.has(r.nodeType))

  const funnelColumns: ColumnsType<FunnelRow> = [
    { title:'节点名称', dataIndex:'nodeName', width:180, ellipsis:true },
    { title:'类型', dataIndex:'nodeType', width:130,
      render:(v:string) => <Tag style={{ fontSize:11 }}>{v}</Tag> },
    { title:'进入次数', dataIndex:'totalEntered', width:90, align:'right',
      sorter:(a,b) => a.totalEntered - b.totalEntered },
    { title:'成功率', width:90, align:'right',
      render:(_,r) => r.totalEntered > 0
        ? <Text style={{ color:'#16a34a', fontWeight:600 }}>{(r.totalSuccess/r.totalEntered*100).toFixed(1)}%</Text>
        : <Text type="secondary">-</Text> },
    { title:'成功', dataIndex:'totalSuccess', width:70, align:'right' },
    { title:'失败', dataIndex:'totalFailed', width:70, align:'right',
      render:(v:number) => v > 0 ? <Text style={{ color:'#ef4444' }}>{v}</Text> : <Text type="secondary">0</Text> },
    { title:'跳过', dataIndex:'totalSkipped', width:70, align:'right' },
    { title:'均耗时(s)', dataIndex:'avgDurationSec', width:90, align:'right',
      render:(v:number) => v != null ? v.toFixed(2) : '-' },
  ]

  const receiptColumns: ColumnsType<ReceiptStatusRow> = [
    { title:'状态', dataIndex:'label' },
    { title:'数量', dataIndex:'count', width:96, align:'right',
      render:(value:number) => value.toLocaleString() },
  ]

  const executionRequestColumns: ColumnsType<ExecutionRequestRow> = [
    { title:'请求 ID', dataIndex:'id', width:160, ellipsis:true },
    { title:'用户', dataIndex:'userId', width:140, ellipsis:true },
    { title:'状态', dataIndex:'status', width:96,
      render:(value:string) => <Tag color={value === 'FAILED' ? 'red' : value === 'RETRY' ? 'orange' : 'blue'}>{value}</Tag> },
    { title:'尝试', dataIndex:'attemptCount', width:72, align:'right',
      render:(value?:number) => value ?? '-' },
    { title:'错误', dataIndex:'lastError', ellipsis:true,
      render:(value?:string) => value || '-' },
    { title:'更新时间', dataIndex:'updatedAt', width:168,
      render:(value?:string) => value || '-' },
    { ...OPERATION_COLUMN, title:'操作',
      render:(_:unknown, row) => (
        <Button
          type="link"
          size="small"
          disabled={!['FAILED', 'RETRY'].includes(row.status)}
          onClick={() => replayExecutionRequest(row)}
        >
          重放
        </Button>
      ) },
  ]

  const sendRecordColumns: ColumnsType<MessageSendRecordRow> = [
    { title:'记录 ID', dataIndex:'id', width:96 },
    { title:'用户', dataIndex:'userId', width:140, ellipsis:true },
    { title:'渠道', dataIndex:'channel', width:88 },
    { title:'状态', dataIndex:'status', width:96,
      render:(value:string) => <Tag color={value === 'FAILED' ? 'red' : value === 'SENT' ? 'green' : 'default'}>{value}</Tag> },
    { title:'外部消息 ID', dataIndex:'externalMessageId', width:160, ellipsis:true,
      render:(value?:string) => value || '-' },
    { title:'错误', dataIndex:'errorMessage', ellipsis:true,
      render:(value?:string) => value || '-' },
    { title:'创建时间', dataIndex:'createdAt', width:168,
      render:(value?:string) => value || '-' },
    { ...OPERATION_COLUMN, title:'操作',
      render:(_:unknown, row) => (
        <Button
          type="link"
          size="small"
          onClick={() => operatorApi.messageSendRecord(row.id).then(res => {
            const record = res.data
            Modal.info({
              title: `发送记录 ${record.id}`,
              width: 720,
              content: (
                <pre style={{ maxHeight: 420, overflow: 'auto', whiteSpace: 'pre-wrap', margin: 0 }}>
                  {JSON.stringify(record, null, 2)}
                </pre>
              ),
            })
          }).catch(() => message.error('加载发送记录失败'))}
        >
          详情
        </Button>
      ) },
  ]

  if (loading) return <Spin size="large" style={{ display:'block', margin:'80px auto' }} />

  return (
    <div style={{ padding:'24px 28px', background:'#f8fafc', minHeight:'100vh' }}>

      {/* ── 页头 ── */}
      <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:24, flexWrap:'wrap', gap:12 }}>
        <Space align="center">
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} />
          <Title level={4} style={{ margin:0 }}>活动效果看板</Title>
        </Space>
        <Space>
          <Button icon={<BarChartOutlined />} type="primary" onClick={() => id && navigate(canvasBiEntrypoint(id))}>
            BI 分析
          </Button>
          <Button onClick={() => navigate(`/canvas/${id}/users`)}>用户明细</Button>
          <RangePicker
            value={range}
            presets={PRESETS}
            onChange={v => { if (v?.[0] && v?.[1]) setRange([v[0], v[1]]) }}
            allowClear={false}
            format="YYYY.MM.DD"
            suffixIcon={<CalendarOutlined />}
            style={{ borderRadius:8 }}
          />
        </Space>
      </div>

      {/* ── KPI 卡片 ── */}
      {stats && (
        <Row gutter={[14,14]} style={{ marginBottom:20 }}>
          {KPI_DEFS.map(def => {
            const curr = stats[def.key as keyof StatsData]
            const prev = prevStats?.[def.key as keyof StatsData] ?? null
            const currNum = typeof curr === 'string' ? parseFloat(curr) : (curr as number)
            const prevNum = typeof prev === 'string' ? parseFloat(prev as string) : (prev as number|null)
            return (
              <Col key={def.key} flex={1} style={{ minWidth:0, display:'flex' }}>
                <div style={{
                  flex:1, background:def.bg, borderRadius:12, padding:'18px 20px',
                  display:'flex', flexDirection:'column', justifyContent:'space-between',
                  minHeight:130,
                }}>
                  {/* 顶部：图标 + 标签 */}
                  <div style={{ display:'flex', alignItems:'center', gap:10 }}>
                    <div style={{
                      width:36, height:36, borderRadius:10, background:def.iconBg,
                      display:'flex', alignItems:'center', justifyContent:'center', fontSize:17, flexShrink:0,
                    }}>
                      {def.icon}
                    </div>
                    <span style={{ fontSize:12, color:'#64748b', fontWeight:500, lineHeight:1.3 }}>
                      {def.label}
                    </span>
                  </div>
                  {/* 数值 */}
                  <div style={{ fontSize:28, fontWeight:700, color:'#0f172a', lineHeight:1, margin:'12px 0 8px' }}>
                    {def.format(curr)}
                  </div>
                  {/* 环比 */}
                  <CompBadge curr={currNum} prev={prevNum} />
                </div>
              </Col>
            )
          })}
        </Row>
      )}

      {/* ── 趋势图 ── */}
      <Card
        title={<span style={{ fontWeight:600 }}>每日执行量趋势</span>}
        style={{ marginBottom:20, borderRadius:12, border:'1px solid #e2e8f0', boxShadow:'0 1px 6px rgba(0,0,0,.05)' }}
        styles={{ body:{ paddingTop:8 } }}
      >
        {trend.length === 0 ? (
          <div style={{ textAlign:'center', padding:'32px 0', color:'#94a3b8' }}>暂无数据</div>
        ) : (
          <ResponsiveContainer width="100%" height={220}>
            <AreaChart data={trend} margin={{ top:8, right:16, bottom:0, left:0 }}>
              <defs>
                <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#3b82f6" stopOpacity={0.15} />
                  <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f4f8" />
              <XAxis dataKey="date" tick={{ fontSize:11, fill:'#94a3b8' }}
                tickFormatter={v => v.slice(5)} interval="preserveStartEnd"
                axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize:11, fill:'#94a3b8' }} width={36} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ borderRadius:8, fontSize:12, border:'1px solid #e2e8f0', boxShadow:'0 4px 12px rgba(0,0,0,.08)' }}
                formatter={v => [v ?? 0, '执行次数']}
              />
              <Area type="monotone" dataKey="count" name="执行次数"
                stroke="#3b82f6" strokeWidth={2.5} fill="url(#areaGrad)"
                dot={false} activeDot={{ r:4, fill:'#3b82f6' }} />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </Card>

      <Row gutter={[14,14]} style={{ marginBottom:20 }}>
        <Col xs={24} lg={10}>
          <Card
            title={<span style={{ fontWeight:600 }}>投递状态</span>}
            style={{ height:'100%', borderRadius:12, border:'1px solid #e2e8f0', boxShadow:'0 1px 6px rgba(0,0,0,.05)' }}
          >
            {receiptRows.length === 0 ? (
              <div style={{ textAlign:'center', padding:'24px 0', color:'#94a3b8' }}>暂无投递数据</div>
            ) : (
              <Table
                rowKey="status"
                dataSource={receiptRows}
                columns={receiptColumns}
                size="small"
                pagination={false}
              />
            )}
          </Card>
        </Col>
        <Col xs={24} lg={14}>
          <Card
            title={<span style={{ fontWeight:600 }}>转化归因</span>}
            style={{ height:'100%', borderRadius:12, border:'1px solid #e2e8f0', boxShadow:'0 1px 6px rgba(0,0,0,.05)' }}
          >
            <Row gutter={[12,12]}>
              {attributionKpis.map(item => (
                <Col xs={12} md={6} key={item.label}>
                  <div style={{ padding:'12px 14px', border:'1px solid #e2e8f0', borderRadius:8, background:'#f8fafc' }}>
                    <div style={{ fontSize:12, color:'#64748b', marginBottom:8 }}>{item.label}</div>
                    <div style={{ fontSize:20, fontWeight:700, color:'#0f172a', lineHeight:1.2 }}>{item.value}</div>
                  </div>
                </Col>
              ))}
            </Row>
          </Card>
        </Col>
      </Row>

      <Card
        title={<span style={{ fontWeight:600 }}>运营排查</span>}
        extra={<Button size="small" icon={<ReloadOutlined />} loading={operatorLoading} onClick={loadOperatorTables}>刷新</Button>}
        style={{ marginBottom:20, borderRadius:12, border:'1px solid #e2e8f0', boxShadow:'0 1px 6px rgba(0,0,0,.05)' }}
      >
        <Row gutter={[16,16]}>
          <Col xs={24} xl={12}>
            <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:10 }}>
              <Text strong>执行请求</Text>
              <Space size={8}>
                <Text type="secondary" style={{ fontSize:12 }}>{executionRequestTotal.toLocaleString()} 条</Text>
                <Button
                  size="small"
                  icon={<DownloadOutlined />}
                  onClick={() => exportOperatorRows(
                    `canvas-${id}-execution-requests.csv`,
                    executionRequestTotal,
                    executionRequestRows as unknown as Record<string, unknown>[],
                  )}
                >
                  导出
                </Button>
              </Space>
            </div>
            <Table
              rowKey="id"
              dataSource={executionRequestRows}
              columns={executionRequestColumns}
              size="small"
              pagination={false}
              loading={operatorLoading}
              scroll={{ x: 860 }}
            />
          </Col>
          <Col xs={24} xl={12}>
            <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:10 }}>
              <Text strong>发送记录</Text>
              <Space size={8}>
                <Text type="secondary" style={{ fontSize:12 }}>{sendRecordTotal.toLocaleString()} 条</Text>
                <Button
                  size="small"
                  icon={<DownloadOutlined />}
                  onClick={() => exportOperatorRows(
                    `canvas-${id}-message-send-records.csv`,
                    sendRecordTotal,
                    sendRecordRows as unknown as Record<string, unknown>[],
                  )}
                >
                  导出
                </Button>
              </Space>
            </div>
            <Table
              rowKey="id"
              dataSource={sendRecordRows}
              columns={sendRecordColumns}
              size="small"
              pagination={false}
              loading={operatorLoading}
              scroll={{ x: 920 }}
            />
          </Col>
        </Row>
      </Card>

      {/* ── 节点漏斗 ── */}
      <Card
        title={<span style={{ fontWeight:600 }}>节点转化漏斗</span>}
        extra={
          <Button type="link" size="small"
            icon={expanded ? <UpOutlined /> : <DownOutlined />}
            onClick={() => setExpanded(v => !v)}
          >
            {expanded ? '收起' : '展开完整节点'}
          </Button>
        }
        style={{ borderRadius:12, border:'1px solid #e2e8f0', boxShadow:'0 1px 6px rgba(0,0,0,.05)' }}
      >
        {displayFunnel.length === 0 ? (
          <div style={{ textAlign:'center', padding:'32px 0', color:'#94a3b8' }}>暂无节点数据</div>
        ) : (
          <Table rowKey="nodeId" dataSource={displayFunnel} columns={funnelColumns}
            size="small" pagination={false} scroll={{ x:700 }} />
        )}
      </Card>
    </div>
  )
}
