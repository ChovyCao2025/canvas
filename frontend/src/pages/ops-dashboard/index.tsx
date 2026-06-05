/**
 * Production runtime operations dashboard.
 */
import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  InputNumber,
  List,
  Result,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { ReloadOutlined, SafetyCertificateOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { opsApi, type OpsAuditEvent, type OpsEmergencyAction, type OpsRuntimeStatus } from '../../services/opsApi'
import {
  OPS_ALERT_SUMMARIES,
  canRunEmergencyAction,
  formatOpsDateTime,
  isOpsPermissionError,
  normalizeOpsAuditEvents,
  opsAlertSeverityColor,
  opsRuntimeStatusColor,
  summarizeRuntimeStatus,
} from './opsDashboardPresentation'

const { Title, Text } = Typography

interface EmergencyActionForm {
  canvasId: number
  action: OpsEmergencyAction
  reason: string
  mode?: string
}

const ACTION_OPTIONS: Array<{ value: OpsEmergencyAction; label: string }> = [
  { value: 'pause', label: '暂停' },
  { value: 'offline', label: '下线' },
  { value: 'resume', label: '恢复' },
  { value: 'kill', label: '终止执行' },
  { value: 'rollback', label: '回滚' },
]

export default function OpsDashboardPage() {
  const [form] = Form.useForm<EmergencyActionForm>()
  const [runtimeStatus, setRuntimeStatus] = useState<OpsRuntimeStatus | null>(null)
  const [auditEvents, setAuditEvents] = useState<OpsAuditEvent[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [permissionDenied, setPermissionDenied] = useState(false)
  const [error, setError] = useState<string>()

  const latestAuditEvents = useMemo(() => normalizeOpsAuditEvents(auditEvents, 20), [auditEvents])
  const emergencyAllowed = canRunEmergencyAction(runtimeStatus?.role)

  const load = async () => {
    setLoading(true)
    setError(undefined)
    setPermissionDenied(false)
    try {
      const [runtimeRes, auditRes] = await Promise.all([
        opsApi.runtimeStatus(),
        opsApi.auditEvents(50),
      ])
      setRuntimeStatus(runtimeRes.data)
      setAuditEvents(auditRes.data)
    } catch (err) {
      if (isOpsPermissionError(err)) {
        setPermissionDenied(true)
      } else {
        setError('运行状态加载失败，请稍后重试或查看网关日志。')
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const submitEmergencyAction = async (values: EmergencyActionForm) => {
    setSubmitting(true)
    try {
      const reason = values.reason.trim()
      await opsApi.emergencyCanvasAction(values.canvasId, values.action, {
        reason,
        mode: values.mode,
      })
      message.success('应急动作已提交并写入审计')
      form.resetFields(['reason'])
      await load()
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<OpsAuditEvent> = [
    { title: '时间', dataIndex: 'createdAt', width: 180, render: formatOpsDateTime },
    { title: '动作', dataIndex: 'action', width: 120, render: value => <Tag color="blue">{value}</Tag> },
    { title: '画布', dataIndex: 'canvasId', width: 100 },
    { title: '租户', dataIndex: 'tenantId', width: 100, render: value => value ?? 'global' },
    { title: '操作人', dataIndex: 'operator', width: 140 },
    { title: '角色', dataIndex: 'role', width: 140 },
    { title: '原因', dataIndex: 'reason', ellipsis: true },
  ]

  if (permissionDenied) {
    return (
      <Result
        status="403"
        title="无权访问运维控制台"
        subTitle="当前账号没有 Ops 读取权限，或登录态已过期。"
      />
    )
  }

  return (
    <div style={{ minHeight: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>运维控制台</Title>
          <Text type="secondary">生产运行状态、告警摘要、应急动作和审计轨迹。</Text>
        </div>
        <Button aria-label="刷新运维状态" icon={<ReloadOutlined />} onClick={load} loading={loading}>刷新</Button>
      </div>

      {error ? <Alert type="error" showIcon message={error} /> : null}

      <Spin spinning={loading}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}>
          <Card size="small">
            <Statistic
              title="运行状态"
              value={runtimeStatus?.status ?? 'UNKNOWN'}
              prefix={<SafetyCertificateOutlined />}
              valueStyle={{ color: runtimeStatus?.status === 'UP' ? '#237804' : '#ad4e00' }}
            />
            <Tag color={opsRuntimeStatusColor(runtimeStatus?.status)} style={{ marginTop: 8 }}>
              {summarizeRuntimeStatus(runtimeStatus)}
            </Tag>
          </Card>
          <Card size="small">
            <Statistic title="活跃告警规则" value={OPS_ALERT_SUMMARIES.length} suffix="条" />
            <Text type="secondary">Grafana / Prometheus 规则已按 P0 基线定义。</Text>
          </Card>
          <Card size="small">
            <Statistic title="最近审计事件" value={latestAuditEvents.length} suffix="条" />
            <Text type="secondary">高影响动作必须保留原因和操作人。</Text>
          </Card>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(280px, 1fr) minmax(320px, 1.2fr)', gap: 16, alignItems: 'start', marginTop: 16 }}>
          <Card title="告警摘要" size="small">
            <List
              dataSource={OPS_ALERT_SUMMARIES}
              renderItem={item => (
                <List.Item>
                  <List.Item.Meta
                    title={(
                      <Space wrap>
                        <span>{item.title}</span>
                        <Tag color={opsAlertSeverityColor(item.severity)}>{item.severity}</Tag>
                        <Tag>{item.scope}</Tag>
                      </Space>
                    )}
                    description={item.metric}
                  />
                </List.Item>
              )}
            />
          </Card>

          <Card title="应急动作" size="small">
            {!emergencyAllowed ? (
              <Alert type="info" showIcon message="当前角色为只读运维权限，不能执行暂停、下线、终止或回滚。" />
            ) : null}
            <Form
              form={form}
              layout="vertical"
              initialValues={{ action: 'pause' }}
              onFinish={submitEmergencyAction}
              disabled={!emergencyAllowed}
              style={{ marginTop: emergencyAllowed ? 0 : 12 }}
            >
              <div style={{ display: 'grid', gridTemplateColumns: 'minmax(120px, 1fr) minmax(140px, 1fr)', gap: 12 }}>
                <Form.Item name="canvasId" label="画布 ID" rules={[{ required: true, message: '请输入画布 ID' }]}>
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item name="action" label="动作" rules={[{ required: true, message: '请选择动作' }]}>
                  <Select options={ACTION_OPTIONS} />
                </Form.Item>
              </div>
              <Form.Item name="mode" label="终止模式">
                <Select
                  allowClear
                  options={[
                    { value: 'GRACEFUL', label: 'GRACEFUL' },
                    { value: 'FORCE', label: 'FORCE' },
                  ]}
                />
              </Form.Item>
              <Form.Item name="reason" label="原因" rules={[{ required: true, whitespace: true, message: '请输入应急原因' }]}>
                <Input.TextArea rows={3} maxLength={300} showCount />
              </Form.Item>
              <Button
                type="primary"
                danger
                htmlType="submit"
                icon={<ThunderboltOutlined />}
                loading={submitting}
                disabled={!emergencyAllowed}
              >
                提交应急动作
              </Button>
            </Form>
          </Card>
        </div>

        <Card title="最新审计事件" size="small" style={{ marginTop: 16 }}>
          <Table
            rowKey="id"
            size="small"
            columns={columns}
            dataSource={latestAuditEvents}
            pagination={false}
            scroll={{ x: 980 }}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无运维审计事件" /> }}
          />
        </Card>
      </Spin>
    </div>
  )
}
