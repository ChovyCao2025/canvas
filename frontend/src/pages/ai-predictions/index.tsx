/**
 * Page for churn prediction and smart timing operator visibility.
 */
import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Checkbox,
  Col,
  Descriptions,
  Empty,
  InputNumber,
  Progress,
  Row,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  aiPredictionApi,
  type PredictionReadinessView,
  type PredictionRunView,
  type RiskDistributionItem,
  type TopRiskUser,
} from '../../services/aiPredictionApi'
import {
  distributionPercent,
  distributionTotal,
  formatDateTime,
  formatProbability,
  orderedDistribution,
  riskBandColor,
  riskBandLabel,
  runStatusColor,
} from './aiPredictions'

const { Title, Text } = Typography

export default function AiPredictionsPage() {
  const [latestRun, setLatestRun] = useState<PredictionRunView | null>(null)
  const [readiness, setReadiness] = useState<PredictionReadinessView | null>(null)
  const [distribution, setDistribution] = useState<RiskDistributionItem[]>([])
  const [topRiskUsers, setTopRiskUsers] = useState<TopRiskUser[]>([])
  const [loading, setLoading] = useState(false)
  const [recomputing, setRecomputing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [limit, setLimit] = useState(100)
  const [force, setForce] = useState(false)

  const load = async (nextLimit = limit) => {
    setLoading(true)
    try {
      setError(null)
      const [runRes, readinessRes, distributionRes, topRiskRes] = await Promise.all([
        aiPredictionApi.latestRun(),
        aiPredictionApi.readiness(),
        aiPredictionApi.churnDistribution(),
        aiPredictionApi.topRiskUsers(nextLimit),
      ])
      setLatestRun(runRes.data ?? null)
      setReadiness(readinessRes.data ?? null)
      setDistribution(distributionRes.data ?? [])
      setTopRiskUsers(topRiskRes.data ?? [])
    } catch (err) {
      setError('预测数据加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const runRecompute = async () => {
    setRecomputing(true)
    try {
      setError(null)
      const res = await aiPredictionApi.recompute({ force, limit })
      setLatestRun(res.data)
      message.success('预测已重新计算')
      await load(limit)
    } catch (err) {
      setError('预测重新计算失败')
    } finally {
      setRecomputing(false)
    }
  }

  const total = useMemo(() => distributionTotal(distribution), [distribution])
  const ordered = useMemo(() => orderedDistribution(distribution), [distribution])
  const readinessDisabled = readiness?.recomputeEnabled === false
  const recomputeDisabled = latestRun?.status === 'RUNNING' || loading || readinessDisabled

  const columns: ColumnsType<TopRiskUser> = [
    {
      title: '用户',
      dataIndex: 'userId',
      width: 180,
      render: value => <Text strong>{value}</Text>,
    },
    {
      title: '风险',
      dataIndex: 'churnRiskBand',
      width: 120,
      render: value => <Tag color={riskBandColor(value)}>{riskBandLabel(value)}</Tag>,
    },
    {
      title: '流失概率',
      dataIndex: 'churnProbability',
      width: 140,
      sorter: (a, b) => Number(a.churnProbability) - Number(b.churnProbability),
      render: value => <Text>{formatProbability(value)}</Text>,
    },
    {
      title: '最佳发送小时',
      dataIndex: 'bestSendHour',
      width: 140,
      render: value => value ?? '-',
    },
    {
      title: '置信度',
      dataIndex: 'confidence',
      width: 120,
      render: value => formatProbability(value),
    },
  ]

  return (
    <div style={{ display: 'grid', gap: 18 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <Title level={4} style={{ margin: 0 }}>流失预测</Title>
        <Space wrap>
          <Text type="secondary">样本上限</Text>
          <InputNumber
            min={1}
            max={500}
            value={limit}
            onChange={value => setLimit(Number(value || 100))}
            style={{ width: 110 }}
          />
          <Checkbox checked={force} onChange={event => setForce(event.target.checked)}>强制重算</Checkbox>
          <Button icon={<ReloadOutlined />} onClick={() => load(limit)} loading={loading}>刷新</Button>
          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            onClick={runRecompute}
            loading={recomputing}
            disabled={recomputeDisabled}
          >
            重新计算
          </Button>
        </Space>
      </div>

      {error ? <Alert type="error" showIcon message={error} /> : null}
      {readinessDisabled ? (
        <Alert
          type="info"
          showIcon
          message="预测重算暂未启用"
          description={readiness?.disabledReason ?? 'canvas.ai.prediction.enabled must be true to recompute predictions'}
        />
      ) : null}

      <Descriptions bordered size="small" column={{ xs: 1, sm: 2, lg: 4 }}>
        <Descriptions.Item label="状态">
          {latestRun ? <Tag color={runStatusColor(latestRun.status)}>{latestRun.status}</Tag> : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="模型版本">{latestRun?.modelVersion ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="运行日期">{latestRun?.runDate ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="处理用户">{latestRun?.processedCount ?? 0}</Descriptions.Item>
        <Descriptions.Item label="跳过">{latestRun?.skippedCount ?? 0}</Descriptions.Item>
        <Descriptions.Item label="失败">{latestRun?.failedCount ?? 0}</Descriptions.Item>
        <Descriptions.Item label="开始时间">{formatDateTime(latestRun?.startedAt)}</Descriptions.Item>
        <Descriptions.Item label="结束时间">{formatDateTime(latestRun?.finishedAt)}</Descriptions.Item>
      </Descriptions>

      <Row gutter={[12, 12]}>
        {ordered.map(item => (
          <Col xs={24} md={8} key={item.band}>
            <div style={{ background: '#fff', border: '1px solid #eef0f5', borderRadius: 8, padding: 16 }}>
              <Space direction="vertical" size={8} style={{ width: '100%' }}>
                <Space style={{ justifyContent: 'space-between', width: '100%' }}>
                  <Tag color={riskBandColor(item.band)}>{riskBandLabel(item.band)}</Tag>
                  <Text strong>{item.count}</Text>
                </Space>
                <Progress
                  percent={distributionPercent(item.count, total)}
                  showInfo
                  status={item.band === 'HIGH' ? 'exception' : 'normal'}
                />
              </Space>
            </div>
          </Col>
        ))}
      </Row>

      <Table
        rowKey="userId"
        dataSource={topRiskUsers}
        columns={columns}
        loading={loading}
        pagination={{ pageSize: 10 }}
        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无预测结果" /> }}
      />
    </div>
  )
}
