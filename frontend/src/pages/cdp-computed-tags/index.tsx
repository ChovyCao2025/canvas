import { useEffect, useState } from 'react'
import { Button, Drawer, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  HistoryOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import {
  cdpApi,
  type ComputedTagPreviewResult,
  type ComputedTagRow,
  type ComputedTagRunRow,
  type LineageImpact,
} from '../../services/cdpApi'
import {
  buildComputedTagPayload,
  formatComputedTagRunSummary,
  formatLineageImpact,
  statusColor,
  statusText,
  type ComputedTagFormValues,
} from './computedTagPresentation'

const { Text, Title } = Typography

export default function CdpComputedTagsPage() {
  const [rows, setRows] = useState<ComputedTagRow[]>([])
  const [runs, setRuns] = useState<ComputedTagRunRow[]>([])
  const [impacts, setImpacts] = useState<LineageImpact[]>([])
  const [preview, setPreview] = useState<ComputedTagPreviewResult | null>(null)
  const [selected, setSelected] = useState<ComputedTagRow | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [form] = Form.useForm<ComputedTagFormValues>()

  const loadRows = async () => {
    setLoading(true)
    try {
      const res = await cdpApi.computedTags.list()
      setRows(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadRows()
  }, [])

  const openCreate = () => {
    form.setFieldsValue({
      tagCode: '',
      displayName: '',
      valueType: 'BOOLEAN',
      computeType: 'RULE',
      refreshMode: 'MANUAL',
      expressionText: JSON.stringify({ field: 'paidCount', op: '>=', value: 2 }, null, 2),
      dependenciesText: '',
    })
    setModalOpen(true)
  }

  const create = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      await cdpApi.computedTags.create(buildComputedTagPayload(values))
      message.success('计算标签已创建')
      setModalOpen(false)
      await loadRows()
    } finally {
      setSaving(false)
    }
  }

  const activate = async (row: ComputedTagRow) => {
    await cdpApi.computedTags.activate(row.tagCode)
    message.success('已启用')
    await loadRows()
  }

  const pause = async (row: ComputedTagRow) => {
    await cdpApi.computedTags.pause(row.tagCode)
    message.success('已暂停')
    await loadRows()
  }

  const runRow = async (row: ComputedTagRow) => {
    const res = await cdpApi.computedTags.run(row.tagCode)
    message.success(formatComputedTagRunSummary(res.data))
    await loadAudit(row)
  }

  const previewRow = async (row: ComputedTagRow) => {
    const res = await cdpApi.computedTags.preview(row.tagCode)
    setPreview(res.data)
    await loadAudit(row)
  }

  const loadAudit = async (row: ComputedTagRow) => {
    setSelected(row)
    setDrawerOpen(true)
    const [runRes, lineageRes] = await Promise.all([
      cdpApi.computedTags.runs(row.tagCode),
      cdpApi.computedTags.lineage(row.tagCode),
    ])
    setRuns(runRes.data)
    setImpacts(lineageRes.data)
  }

  const columns: ColumnsType<ComputedTagRow> = [
    { title: '标签编码', dataIndex: 'tagCode', width: 180, render: value => <Text code>{value}</Text> },
    { title: '名称', dataIndex: 'displayName', width: 160 },
    { title: '值类型', dataIndex: 'valueType', width: 90, render: value => <Tag>{value}</Tag> },
    { title: '计算类型', dataIndex: 'computeType', width: 90, render: value => <Tag>{value}</Tag> },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: value => <Tag color={statusColor(value)}>{statusText(value)}</Tag>,
    },
    {
      title: '表达式',
      dataIndex: 'expressionJson',
      ellipsis: true,
      render: value => <Text code style={{ whiteSpace: 'normal', wordBreak: 'break-all' }}>{value}</Text>,
    },
    {
      title: '操作',
      width: 300,
      fixed: 'right',
      render: (_, row) => (
        <Space size="small" wrap>
          <Button size="small" onClick={() => previewRow(row)}>预览</Button>
          {row.status === 'ACTIVE' ? (
            <Button size="small" icon={<PauseCircleOutlined />} onClick={() => pause(row)}>暂停</Button>
          ) : (
            <Button size="small" icon={<PlayCircleOutlined />} onClick={() => activate(row)}>启用</Button>
          )}
          <Button size="small" icon={<ThunderboltOutlined />} disabled={row.status !== 'ACTIVE'} onClick={() => runRow(row)}>运行</Button>
          <Button size="small" icon={<HistoryOutlined />} onClick={() => loadAudit(row)}>血缘</Button>
        </Space>
      ),
    },
  ]

  const runColumns: ColumnsType<ComputedTagRunRow> = [
    { title: 'Run ID', dataIndex: 'id', width: 90 },
    { title: '状态', dataIndex: 'status', width: 90, render: value => <Tag>{statusText(value)}</Tag> },
    { title: '扫描', dataIndex: 'scannedCount', width: 80 },
    { title: '命中', dataIndex: 'matchedCount', width: 80 },
    { title: '更新', dataIndex: 'updatedCount', width: 80 },
    { title: '跳过', dataIndex: 'skippedCount', width: 80 },
    { title: '失败', dataIndex: 'failedCount', width: 80 },
    { title: '开始时间', dataIndex: 'startedAt', width: 170, render: value => value || '-' },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>计算标签</Title>
          <Text type="secondary">管理行为规则标签、依赖校验、运行写回和下游影响。</Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={loadRows} loading={loading}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建标签</Button>
        </Space>
      </div>

      <Table
        rowKey="id"
        dataSource={rows}
        columns={columns}
        loading={loading}
        pagination={{ pageSize: 10, hideOnSinglePage: true }}
        scroll={{ x: 1100 }}
      />

      <Modal
        title="新建计算标签"
        open={modalOpen}
        onOk={create}
        confirmLoading={saving}
        onCancel={() => setModalOpen(false)}
        width={760}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="tagCode" label="标签编码" rules={[{ required: true, message: '请输入标签编码' }]}>
            <Input placeholder="vip_likely" />
          </Form.Item>
          <Form.Item name="displayName" label="显示名称" rules={[{ required: true, message: '请输入显示名称' }]}>
            <Input placeholder="高价值倾向" />
          </Form.Item>
          <Space size={12} style={{ width: '100%' }} align="start">
            <Form.Item name="valueType" label="值类型" rules={[{ required: true }]}>
              <Select style={{ width: 140 }} options={[
                { value: 'BOOLEAN', label: 'BOOLEAN' },
                { value: 'STRING', label: 'STRING' },
                { value: 'NUMBER', label: 'NUMBER' },
                { value: 'JSON', label: 'JSON' },
              ]} />
            </Form.Item>
            <Form.Item name="computeType" label="计算类型" rules={[{ required: true }]}>
              <Select style={{ width: 140 }} options={[
                { value: 'RULE', label: 'RULE' },
                { value: 'EXPR', label: 'EXPR' },
                { value: 'SQL', label: 'SQL' },
              ]} />
            </Form.Item>
            <Form.Item name="refreshMode" label="刷新模式" rules={[{ required: true }]}>
              <Select style={{ width: 140 }} options={[
                { value: 'MANUAL', label: 'MANUAL' },
                { value: 'EVENT', label: 'EVENT' },
              ]} />
            </Form.Item>
          </Space>
          <Form.Item name="expressionText" label="表达式 JSON" rules={[{ required: true, message: '请输入表达式 JSON' }]}>
            <Input.TextArea autoSize={{ minRows: 8 }} style={{ fontFamily: 'monospace' }} />
          </Form.Item>
          <Form.Item name="dependenciesText" label="依赖标签">
            <Input.TextArea autoSize={{ minRows: 3 }} placeholder={'paid_user\nhigh_value'} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={selected ? `${selected.displayName} 血缘与运行` : '血缘与运行'}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={760}
      >
        {preview && (
          <div style={{ marginBottom: 18 }}>
            <Text strong>预览</Text>
            <div style={{ marginTop: 8 }}>扫描 {preview.scannedCount}，命中 {preview.matchedCount}</div>
            <Table
              rowKey="userId"
              size="small"
              dataSource={preview.samples}
              pagination={false}
              style={{ marginTop: 8 }}
              columns={[
                { title: '用户 ID', dataIndex: 'userId' },
                { title: '标签值', dataIndex: 'tagValue' },
              ]}
            />
          </div>
        )}

        <Title level={5}>运行历史</Title>
        <Table rowKey="id" size="small" dataSource={runs} columns={runColumns} pagination={false} scroll={{ x: 800 }} />

        <Title level={5} style={{ marginTop: 24 }}>下游影响</Title>
        <Space direction="vertical" style={{ width: '100%' }}>
          {impacts.length === 0 ? <Text type="secondary">暂无下游引用</Text> : impacts.map(impact => (
            <Text key={`${impact.objectType}-${impact.objectId}-${impact.referencePath}`} code>
              {formatLineageImpact(impact)}
            </Text>
          ))}
        </Space>
      </Drawer>
    </div>
  )
}
