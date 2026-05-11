import { useEffect, useState } from 'react'
import { Card, Col, Row, Statistic, Table, Typography, Spin, Button } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import type { ColumnsType } from 'antd/es/table'
import http from '../../services/api'
import type { R } from '../../types'

const { Title, Text } = Typography

interface StatsData {
  total: number
  success: number
  failed: number
  paused: number
  successRate: string
  uniqueUsers: number
}

interface FunnelRow {
  nodeId: string
  nodeType: string
  nodeName: string
  totalEntered: number
  totalSuccess: number
  totalFailed: number
  totalSkipped: number
  avgDurationSec: number
}

interface TrendPoint { date: string; count: number }

export default function CanvasStatsPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [stats,  setStats]  = useState<StatsData | null>(null)
  const [funnel, setFunnel] = useState<FunnelRow[]>([])
  const [trend,  setTrend]  = useState<TrendPoint[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      http.get<R<StatsData>, R<StatsData>>(`/canvas/${id}/stats?days=7`),
      http.get<R<FunnelRow[]>, R<FunnelRow[]>>(`/canvas/${id}/funnel`),
      http.get<R<TrendPoint[]>, R<TrendPoint[]>>(`/canvas/${id}/trend?days=30`),
    ]).then(([s, f, t]) => {
      setStats(s.data)
      setFunnel(f.data)
      setTrend(t.data)
    }).finally(() => setLoading(false))
  }, [id])

  const funnelColumns: ColumnsType<FunnelRow> = [
    { title: '节点名称', dataIndex: 'nodeName', ellipsis: true },
    { title: '类型', dataIndex: 'nodeType', width: 140 },
    { title: '进入次数', dataIndex: 'totalEntered', width: 100, sorter: (a, b) => a.totalEntered - b.totalEntered },
    {
      title: '成功率',
      width: 100,
      render: (_, r) => r.totalEntered > 0
        ? `${(r.totalSuccess / r.totalEntered * 100).toFixed(1)}%` : '-',
    },
    { title: '成功', dataIndex: 'totalSuccess', width: 80 },
    { title: '失败', dataIndex: 'totalFailed',  width: 80 },
    { title: '跳过', dataIndex: 'totalSkipped', width: 80 },
    { title: '平均耗时(s)', dataIndex: 'avgDurationSec', width: 110,
      render: (v: number) => v != null ? v.toFixed(2) : '-' },
  ]

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '80px auto' }} />

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} />
        <Title level={4} style={{ margin: 0 }}>活动效果看板（近7天）</Title>
      </div>

      {/* KPI 指标 */}
      {stats && (
        <Row gutter={16} style={{ marginBottom: 24 }}>
          {[
            { title: '触发次数', value: stats.total },
            { title: '成功次数', value: stats.success, valueStyle: { color: '#52c41a' } },
            { title: '失败次数', value: stats.failed,  valueStyle: { color: '#f5222d' } },
            { title: '挂起中',   value: stats.paused,  valueStyle: { color: '#faad14' } },
            { title: '成功率',   value: stats.successRate, suffix: '' },
            { title: '触达用户', value: stats.uniqueUsers },
          ].map((item, i) => (
            <Col key={i} xs={12} sm={8} md={4}>
              <Card>
                <Statistic {...item} />
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {/* 节点漏斗 */}
      <Card title="节点漏斗（所有执行汇总）" style={{ marginBottom: 24 }}>
        <Table
          rowKey="nodeId"
          dataSource={funnel}
          columns={funnelColumns}
          size="small"
          pagination={false}
        />
      </Card>

      {/* 每日趋势 */}
      <Card title="每日执行量（近30天）">
        {trend.length === 0
          ? <Text type="secondary">暂无数据</Text>
          : (
            <div style={{ display: 'flex', alignItems: 'flex-end', gap: 4, height: 120 }}>
              {trend.map(p => {
                const max = Math.max(...trend.map(t => t.count), 1)
                const h = Math.max(4, (p.count / max) * 100)
                return (
                  <div key={p.date} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <Text style={{ fontSize: 10, color: '#888' }}>{p.count}</Text>
                    <div style={{ width: '80%', height: h, background: '#1677ff', borderRadius: 2 }} title={`${p.date}: ${p.count}`} />
                    <Text style={{ fontSize: 9, color: '#bbb', writingMode: 'vertical-rl', marginTop: 2 }}>
                      {p.date.slice(5)}
                    </Text>
                  </div>
                )
              })}
            </div>
          )
        }
      </Card>
    </div>
  )
}
