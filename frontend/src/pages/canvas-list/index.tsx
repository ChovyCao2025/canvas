/**
 * 页面职责：画布列表页，提供创建、发布、下线、克隆、灰度、回滚和归档等操作入口。
 *
 * 维护说明：列表页是运营控制台的主要入口，所有状态变更后都通过统一刷新保持表格一致。
 */
import { useEffect, useState } from 'react'
import type { Key, ReactNode } from 'react'
import {
  Button, Table, Tag, Space, Modal, Form, Input,
  message, Typography, Tooltip, Dropdown, Drawer, Descriptions, Select, InputNumber,
} from 'antd'
import {
  PlusOutlined, EditOutlined, CloudUploadOutlined,
  StopOutlined, CopyOutlined, ThunderboltOutlined, BarChartOutlined, EyeOutlined,
  MoreOutlined, TeamOutlined, PauseCircleOutlined, PlayCircleOutlined, InboxOutlined,
  DeleteOutlined, DownloadOutlined, UploadOutlined, SearchOutlined,
} from '@ant-design/icons'
import type { MenuProps } from 'antd'
import { useNavigate } from 'react-router-dom'
import type { ColumnsType } from 'antd/es/table'
import { canvasApi, type CanvasTemplate } from '../../services/api'
import { canvasBiEntrypoint } from '../bi/biWorkbench'
import { buildTemplateCategoryOptions, buildTemplateCloneSuccessMessage } from './templateCatalog'
import { buildImportPayload, exportedFileName } from './canvasImportExport'
import { buildCanvasListParams, projectFolderLabel } from './canvasProjectFilters'
import {
  canvasBatchApi,
  type CanvasBatchItem,
  type CanvasBatchOperation,
  type CanvasBatchResult,
} from '../../services/canvasBatchApi'
import { useAuth } from '../../context/AuthContext'
import type { Canvas } from '../../types'

/** 页面标题组件别名。 */
const { Title } = Typography

/** 画布状态到表格 Tag 展示的映射，和后端状态码保持一致。 */
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿',   color: 'default' },
  1: { label: '已发布', color: 'green' },
  2: { label: '已下线', color: 'red' },
  3: { label: '已归档', color: 'orange' },
  4: { label: '已停止', color: 'volcano' },
}

const BATCH_OPERATION_META: Record<CanvasBatchOperation, { label: string; icon: ReactNode; okType?: 'danger' }> = {
  pause: { label: '批量暂停', icon: <PauseCircleOutlined /> },
  resume: { label: '批量恢复', icon: <PlayCircleOutlined /> },
  archive: { label: '批量归档', icon: <InboxOutlined />, okType: 'danger' },
  clone: { label: '批量克隆', icon: <CopyOutlined /> },
}

const BATCH_STATUS_MAP: Record<string, { label: string; color: string }> = {
  SUCCESS: { label: '成功', color: 'green' },
  SKIPPED: { label: '跳过', color: 'gold' },
  FAILED: { label: '失败', color: 'red' },
}

interface CloneReplacementForm {
  reason?: string
  name?: string
  description?: string
  tokens?: Array<{ key?: string; value?: string }>
}

interface FilterBatchForm {
  operation?: Exclude<CanvasBatchOperation, 'clone'>
  reason?: string
  status?: number
  name?: string
  triggerType?: string
  limit?: number
}

/**
 * 画布列表页：提供创建、发布、下线、克隆、归档等运营操作入口。
 *
 * 交互原则：
 * - 高风险动作（发布/下线/紧急停止/归档）都走确认弹窗；
 * - 列表页只负责“操作入口”，具体编排在编辑器页完成。
 */
export default function CanvasListPage() {
  const navigate = useNavigate()
  const { isAdmin } = useAuth()
  // 列表数据与分页态
  const [data, setData] = useState<Canvas[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [projectKey, setProjectKey] = useState('')
  const [folderKey, setFolderKey] = useState('')
  // 创建弹窗态
  const [createVisible, setCreateVisible] = useState(false)
  const [form] = Form.useForm()
  const [templateVisible, setTemplateVisible] = useState(false)
  const [templates, setTemplates] = useState<CanvasTemplate[]>([])
  const [templateCategoryOptions, setTemplateCategoryOptions] = useState<Array<{ label: string; value: string }>>([])
  const [templateLoading, setTemplateLoading] = useState(false)
  const [templateSubmitting, setTemplateSubmitting] = useState(false)
  const [templateForm] = Form.useForm<{ templateId?: number; name?: string }>()
  const [importVisible, setImportVisible] = useState(false)
  const [importText, setImportText] = useState('')
  const [importSubmitting, setImportSubmitting] = useState(false)
  // 批量操作态
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])
  const [batchAction, setBatchAction] = useState<Exclude<CanvasBatchOperation, 'clone'> | null>(null)
  const [batchModalOpen, setBatchModalOpen] = useState(false)
  const [cloneDrawerOpen, setCloneDrawerOpen] = useState(false)
  const [filterDrawerOpen, setFilterDrawerOpen] = useState(false)
  const [batchSubmitting, setBatchSubmitting] = useState(false)
  const [batchResult, setBatchResult] = useState<CanvasBatchResult | null>(null)
  const [resultDrawerOpen, setResultDrawerOpen] = useState(false)
  const [batchForm] = Form.useForm<{ reason?: string }>()
  const [cloneForm] = Form.useForm<CloneReplacementForm>()
  const [filterForm] = Form.useForm<FilterBatchForm>()
  const selectedCanvasIds = selectedRowKeys.map(key => Number(key)).filter(id => Number.isFinite(id))

  // 拉取画布分页列表：是所有列表变更后的统一刷新入口。
  const fetchList = async (p = page) => {
    setLoading(true)
    try {
      const res = await canvasApi.list(buildCanvasListParams({ page: p, projectKey, folderKey }))
      setData(res.data.list)
      setTotal(res.data.total)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchList(1) }, [])

  // 创建新画布：成功后回到第一页刷新列表
  const handleCreate = async () => {
    const values = await form.validateFields()
    await canvasApi.create({ ...values })
    message.success('创建成功')
    setCreateVisible(false)
    form.resetFields()
    fetchList(1)
  }

  const handleProjectFilter = () => {
    setPage(1)
    fetchList(1)
  }

  const loadTemplates = async (category?: string) => {
    setTemplateLoading(true)
    try {
      const res = await canvasApi.listTemplates(category)
      const list = res.data ?? []
      setTemplates(list)
      if (!category) {
        setTemplateCategoryOptions(buildTemplateCategoryOptions(list))
      }
    } finally {
      setTemplateLoading(false)
    }
  }

  const openTemplateModal = () => {
    templateForm.resetFields()
    setTemplates([])
    setTemplateCategoryOptions([])
    setTemplateVisible(true)
    loadTemplates()
  }

  const handleTemplateCategoryChange = (category?: string) => {
    templateForm.setFieldValue('templateId', undefined)
    loadTemplates(category)
  }

  const handleCreateFromTemplate = async () => {
    const values = await templateForm.validateFields()
    if (!values.templateId) return
    setTemplateSubmitting(true)
    try {
      const name = values.name?.trim() || undefined
      const res = await canvasApi.createFromTemplate(values.templateId, name)
      message.success(buildTemplateCloneSuccessMessage(res.data))
      setTemplateVisible(false)
      templateForm.resetFields()
      fetchList(1)
    } finally {
      setTemplateSubmitting(false)
    }
  }

  const handleExport = async (record: Canvas) => {
    const versionId = record.publishedVersionId
    if (!versionId) {
      message.warning('请先发布或选择可导出的版本')
      return
    }
    const res = await canvasApi.exportCanvas(record.id, versionId)
    const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = exportedFileName(record)
    link.click()
    URL.revokeObjectURL(url)
  }

  const handleImport = async () => {
    if (!importText.trim()) {
      message.warning('请粘贴画布导出 JSON')
      return
    }
    setImportSubmitting(true)
    try {
      await canvasApi.importCanvas(buildImportPayload(importText))
      message.success('导入成功')
      setImportVisible(false)
      setImportText('')
      fetchList(1)
    } finally {
      setImportSubmitting(false)
    }
  }

  // 发布：把当前草稿版本设为线上生效版本。
  const handlePublish = async (id: number) => {
    Modal.confirm({
      title: '确认发布？',
      content: '发布后该画布将对执行引擎生效',
      onOk: async () => {
        await canvasApi.publish(id)
        message.success('发布成功')
        fetchList()
      },
    })
  }

  // 下线后不再接收新触发。
  const handleOffline = async (id: number) => {
    Modal.confirm({
      title: '确认下线？',
      okType: 'danger',
      onOk: async () => {
        await canvasApi.offline(id)
        message.success('已下线')
        fetchList()
      },
    })
  }

  // 紧急停止：用于事故处理，优先保证“快速止血”。
  const handleKill = async (id: number) => {
    Modal.confirm({
      title: '紧急停止',
      content: '将立即停止所有新触发，正在执行的实例将被标记为 KILLED',
      okType: 'danger',
      okText: '确认停止',
      onOk: async () => {
        await canvasApi.kill(id, 'GRACEFUL')
        message.warning('已紧急停止')
        fetchList()
      },
    })
  }

  // 克隆会复制草稿图结构，创建一份新的 DRAFT 画布。
  const handleClone = async (id: number) => {
    const res = await canvasApi.clone(id)
    message.success(`克隆成功，新画布 ID: ${res.data.id}`)
    fetchList()
  }

  /** 归档画布前展示影响范围确认，归档后刷新当前列表。 */
  const handleArchive = (id: number, name: string) => {
    Modal.confirm({
      title: '归档画布',
      content: (
        <div>
          <p>确认将「{name}」归档？</p>
          <ul style={{ color: '#8c8c8c', fontSize: 13, paddingLeft: 16, margin: '8px 0 0' }}>
            <li>画布将从列表中隐藏</li>
            <li>正在运行中的流程不受影响</li>
            <li>可联系管理员恢复</li>
          </ul>
        </div>
      ),
      okText: '确认归档',
      okType: 'danger',
      onOk: async () => {
        await canvasApi.archive(id)
        message.success('已归档')
        fetchList()
      },
    })
  }

  const ensureBatchSelection = () => {
    if (selectedCanvasIds.length === 0) {
      message.warning('请先选择画布')
      return false
    }
    return true
  }

  const openBatchModal = (operation: Exclude<CanvasBatchOperation, 'clone'>) => {
    if (!ensureBatchSelection()) return
    setBatchAction(operation)
    batchForm.resetFields()
    setBatchModalOpen(true)
  }

  const openCloneDrawer = () => {
    if (!ensureBatchSelection()) return
    cloneForm.resetFields()
    setCloneDrawerOpen(true)
  }

  const openFilterDrawer = () => {
    filterForm.resetFields()
    filterForm.setFieldsValue({ operation: 'pause', limit: 100 })
    setFilterDrawerOpen(true)
  }

  const applyBatchResult = (result: CanvasBatchResult) => {
    setBatchResult(result)
    setResultDrawerOpen(true)
    setSelectedRowKeys([])
    message.success(`批量操作完成：成功 ${result.successCount}，跳过 ${result.skippedCount}，失败 ${result.failedCount}`)
    fetchList()
  }

  const submitBatchAction = async () => {
    if (!batchAction || !ensureBatchSelection()) return
    const values = await batchForm.validateFields()
    setBatchSubmitting(true)
    try {
      const res = await canvasBatchApi.run(batchAction, {
        canvasIds: selectedCanvasIds,
        reason: values.reason?.trim(),
      })
      setBatchModalOpen(false)
      applyBatchResult(res.data)
    } finally {
      setBatchSubmitting(false)
    }
  }

  const buildCloneReplacements = (values: CloneReplacementForm) => {
    const replacements: Record<string, string> = {}
    if (values.name?.trim()) {
      replacements.name = values.name.trim()
    }
    if (values.description?.trim()) {
      replacements.description = values.description.trim()
    }
    values.tokens?.forEach(token => {
      const key = token.key?.trim()
      if (key) {
        replacements[key] = token.value ?? ''
      }
    })
    return replacements
  }

  const submitCloneBatch = async () => {
    if (!ensureBatchSelection()) return
    const values = await cloneForm.validateFields()
    setBatchSubmitting(true)
    try {
      const res = await canvasBatchApi.run('clone', {
        canvasIds: selectedCanvasIds,
        replacements: buildCloneReplacements(values),
        reason: values.reason?.trim(),
      })
      setCloneDrawerOpen(false)
      applyBatchResult(res.data)
    } finally {
      setBatchSubmitting(false)
    }
  }

  const submitFilterBatch = async () => {
    const values = await filterForm.validateFields()
    if (values.status == null && !values.name?.trim() && !values.triggerType?.trim()) {
      message.warning('请至少设置一个过滤条件')
      return
    }
    setBatchSubmitting(true)
    try {
      const res = await canvasBatchApi.run(values.operation ?? 'pause', {
        filters: {
          status: values.status,
          name: values.name?.trim() || undefined,
          triggerType: values.triggerType?.trim() || undefined,
          limit: values.limit,
        },
        reason: values.reason?.trim(),
      })
      setFilterDrawerOpen(false)
      applyBatchResult(res.data)
    } finally {
      setBatchSubmitting(false)
    }
  }

  // 表格列定义：状态 + 操作按钮集中在列表页。
  // 按钮展示会根据状态和角色动态收敛，减少误操作。
  const columns: ColumnsType<Canvas> = [
    { title: 'ID', dataIndex: 'id', width: 80, align: 'center' },
    {
      title: '名称',
      dataIndex: 'name',
      align: 'center',
      render: (name, record) => (
        <Space size={6}>
          <Button type="link" onClick={() => navigate(`/canvas/${record.id}/edit`)}>
            {name}
          </Button>
          {record.isExample === 1 && <Tag color="blue">示例</Tag>}
        </Space>
      ),
    },
    { title: '描述', dataIndex: 'description', ellipsis: true, align: 'center' },
    {
      title: '项目/文件夹',
      width: 180,
      align: 'center',
      render: (_, record) => projectFolderLabel(record),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      align: 'center',
      render: (status: number) => {
        const { label, color } = STATUS_MAP[status] ?? { label: '未知', color: 'default' }
        return <Tag color={color}>{label}</Tag>
      },
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 180,
      align: 'center',
      render: (v: string) => v?.replace('T', ' ').slice(0, 19),
    },
    {
      title: '操作',
      width: 200,
      align: 'center',
      render: (_, record) => {
        const moreItems: MenuProps['items'] = [
          {
            key: 'clone',
            icon: <CopyOutlined />,
            label: '克隆',
            onClick: () => handleClone(record.id),
          },
          {
            key: 'stats',
            icon: <BarChartOutlined />,
            label: '效果看板',
            onClick: () => navigate(`/canvas/${record.id}/stats`),
          },
          {
            key: 'bi',
            icon: <BarChartOutlined />,
            label: 'BI 分析',
            onClick: () => navigate(canvasBiEntrypoint(record.id)),
          },
          {
            key: 'users',
            icon: <TeamOutlined />,
            label: '用户数据',
            onClick: () => navigate(`/canvas/${record.id}/users`),
          },
          {
            key: 'export',
            icon: <DownloadOutlined />,
            label: '导出',
            onClick: () => handleExport(record),
          },
          { type: 'divider' },
          {
            key: 'archive',
            label: <span style={{ color: '#ff4d4f' }}>归档画布</span>,
            onClick: () => handleArchive(record.id, record.name),
          },
        ]
        return (
          <Space size={4}>
            <Tooltip title="查看">
              <Button size="small" icon={<EyeOutlined />}
                onClick={() => navigate(`/canvas/${record.id}/edit?readonly=true`)} />
            </Tooltip>
            <Tooltip title="编辑">
              <Button size="small" icon={<EditOutlined />}
                onClick={() => navigate(`/canvas/${record.id}/edit`)} />
            </Tooltip>

            {record.status !== 1 && isAdmin && (
              <Tooltip title="发布">
                <Button size="small" type="primary" icon={<CloudUploadOutlined />}
                  onClick={() => handlePublish(record.id)} />
              </Tooltip>
            )}

            {record.status === 1 && isAdmin && (
              <Tooltip title="下线">
                <Button size="small" danger icon={<StopOutlined />}
                  onClick={() => handleOffline(record.id)} />
              </Tooltip>
            )}

            {record.status === 1 && isAdmin && (
              <Tooltip title="紧急停止">
                <Button size="small" danger ghost icon={<ThunderboltOutlined />}
                  onClick={() => handleKill(record.id)} />
              </Tooltip>
            )}

            <Dropdown menu={{ items: moreItems }} trigger={['click']}>
              <Button size="small" icon={<MoreOutlined />} />
            </Dropdown>
          </Space>
        )
      },
    },
  ]

  const batchResultColumns: ColumnsType<CanvasBatchItem> = [
    { title: '源画布 ID', dataIndex: 'canvasId', width: 110, align: 'center' },
    {
      title: '目标画布 ID',
      dataIndex: 'targetCanvasId',
      width: 120,
      align: 'center',
      render: (targetCanvasId?: number | null) => targetCanvasId ? (
        <Button type="link" onClick={() => navigate(`/canvas/${targetCanvasId}/edit`)}>
          {targetCanvasId}
        </Button>
      ) : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      align: 'center',
      render: (status: string) => {
        const { label, color } = BATCH_STATUS_MAP[status] ?? { label: status, color: 'default' }
        return <Tag color={color}>{label}</Tag>
      },
    },
    { title: '消息', dataIndex: 'message', ellipsis: true },
  ]

  const rowSelection = isAdmin ? {
    selectedRowKeys,
    preserveSelectedRowKeys: true,
    onChange: (keys: Key[]) => setSelectedRowKeys(keys),
  } : undefined

  const currentBatchMeta = batchAction ? BATCH_OPERATION_META[batchAction] : undefined

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0 }}>旅程管理</Title>
        <Space>
          <Button icon={<UploadOutlined />} onClick={() => setImportVisible(true)}>
            导入
          </Button>
          <Button icon={<CopyOutlined />} onClick={openTemplateModal}>
            从模板创建
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>
            新建画布
          </Button>
        </Space>
      </div>

      <Space wrap style={{ marginBottom: 12 }}>
        <Input
          allowClear
          style={{ width: 180 }}
          placeholder="项目 key"
          value={projectKey}
          onChange={event => setProjectKey(event.target.value)}
          onPressEnter={handleProjectFilter}
        />
        <Input
          allowClear
          style={{ width: 180 }}
          placeholder="文件夹 key"
          value={folderKey}
          onChange={event => setFolderKey(event.target.value)}
          onPressEnter={handleProjectFilter}
        />
        <Button icon={<SearchOutlined />} onClick={handleProjectFilter}>
          筛选
        </Button>
      </Space>

      {isAdmin && (
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: 12,
            padding: '12px 16px',
            marginBottom: 12,
            border: '1px solid #f0f0f0',
            borderRadius: 6,
            background: '#fafafa',
          }}
        >
          <Space wrap>
            <Typography.Text type={selectedCanvasIds.length > 0 ? undefined : 'secondary'}>
              已选 {selectedCanvasIds.length} 项
            </Typography.Text>
            <Button
              icon={BATCH_OPERATION_META.pause.icon}
              disabled={selectedCanvasIds.length === 0}
              onClick={() => openBatchModal('pause')}
            >
              暂停
            </Button>
            <Button
              icon={BATCH_OPERATION_META.resume.icon}
              disabled={selectedCanvasIds.length === 0}
              onClick={() => openBatchModal('resume')}
            >
              恢复
            </Button>
            <Button
              danger
              icon={BATCH_OPERATION_META.archive.icon}
              disabled={selectedCanvasIds.length === 0}
              onClick={() => openBatchModal('archive')}
            >
              归档
            </Button>
            <Button
              type="primary"
              ghost
              icon={BATCH_OPERATION_META.clone.icon}
              disabled={selectedCanvasIds.length === 0}
              onClick={openCloneDrawer}
            >
              克隆
            </Button>
            <Button icon={<MoreOutlined />} onClick={openFilterDrawer}>
              按条件批量
            </Button>
          </Space>
          <Button disabled={!batchResult} onClick={() => setResultDrawerOpen(true)}>
            查看批量结果
          </Button>
        </div>
      )}

      <Table
        rowKey="id"
        rowSelection={rowSelection}
        dataSource={data}
        columns={columns}
        loading={loading}
        pagination={{
          total,
          pageSize: 20,
          current: page,
          onChange: (p) => { setPage(p); fetchList(p) },
        }}
      />

      <Modal
        title="新建画布"
        open={createVisible}
        onOk={handleCreate}
        onCancel={() => { setCreateVisible(false); form.resetFields() }}
        okText="创建"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="画布名称" rules={[{ required: true }]}>
            <Input placeholder="例：新用户机票发券流程" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="projectKey" label="项目 Key">
            <Input placeholder="例：growth" />
          </Form.Item>
          <Form.Item name="projectName" label="项目名称">
            <Input placeholder="例：增长运营" />
          </Form.Item>
          <Form.Item name="folderKey" label="文件夹 Key">
            <Input placeholder="例：new-user" />
          </Form.Item>
          <Form.Item name="folderName" label="文件夹名称">
            <Input placeholder="例：新客旅程" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="从模板创建"
        open={templateVisible}
        onOk={handleCreateFromTemplate}
        onCancel={() => { setTemplateVisible(false); templateForm.resetFields() }}
        okText="创建"
        cancelText="取消"
        confirmLoading={templateSubmitting}
      >
        <Form form={templateForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="模板分类">
            <Select
              allowClear
              placeholder="全部分类"
              options={templateCategoryOptions}
              onChange={handleTemplateCategoryChange}
            />
          </Form.Item>
          <Form.Item name="templateId" label="模板" rules={[{ required: true, message: '请选择模板' }]}>
            <Select
              showSearch
              loading={templateLoading}
              optionFilterProp="label"
              placeholder="选择一个启用模板"
              options={templates.map(template => ({
                value: template.id,
                label: template.category
                  ? `${template.name} · ${template.category} · 使用 ${template.useCount}`
                  : `${template.name} · 使用 ${template.useCount}`,
              }))}
            />
          </Form.Item>
          <Form.Item name="name" label="画布名称">
            <Input placeholder="留空时使用模板默认名称" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="导入画布"
        open={importVisible}
        onOk={handleImport}
        onCancel={() => { setImportVisible(false); setImportText('') }}
        okText="导入"
        cancelText="取消"
        confirmLoading={importSubmitting}
      >
        <Input.TextArea
          rows={10}
          value={importText}
          onChange={event => setImportText(event.target.value)}
          placeholder="粘贴画布导出 JSON"
          style={{ marginTop: 16 }}
        />
      </Modal>

      <Modal
        title={currentBatchMeta?.label}
        open={batchModalOpen}
        onOk={submitBatchAction}
        onCancel={() => setBatchModalOpen(false)}
        okText="确认执行"
        cancelText="取消"
        okType={currentBatchMeta?.okType}
        confirmLoading={batchSubmitting}
      >
        <Form form={batchForm} layout="vertical" style={{ marginTop: 16 }}>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
            将处理已选择的 {selectedCanvasIds.length} 个画布。
          </Typography.Paragraph>
          <Form.Item name="reason" label="操作原因" rules={[{ required: true, message: '请输入操作原因' }]}>
            <Input.TextArea rows={3} placeholder="例：活动结束后统一归档" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="批量克隆"
        width={560}
        open={cloneDrawerOpen}
        onClose={() => setCloneDrawerOpen(false)}
        extra={(
          <Space>
            <Button onClick={() => setCloneDrawerOpen(false)}>取消</Button>
            <Button type="primary" loading={batchSubmitting} onClick={submitCloneBatch}>
              执行克隆
            </Button>
          </Space>
        )}
      >
        <Form form={cloneForm} layout="vertical">
          <Typography.Paragraph type="secondary">
            将克隆已选择的 {selectedCanvasIds.length} 个画布。
          </Typography.Paragraph>
          <Form.Item name="reason" label="操作原因" rules={[{ required: true, message: '请输入操作原因' }]}>
            <Input.TextArea rows={3} placeholder="例：复制一套用于新市场投放" />
          </Form.Item>
          <Form.Item name="name" label="名称覆盖">
            <Input placeholder="留空时仅按 token 替换原名称中的 ${key}" />
          </Form.Item>
          <Form.Item name="description" label="描述覆盖">
            <Input.TextArea rows={3} placeholder="留空时仅按 token 替换原描述中的 ${key}" />
          </Form.Item>
          <Form.Item label="Token 替换">
            <Form.List name="tokens">
              {(fields, { add, remove }) => (
                <Space direction="vertical" style={{ width: '100%' }} size={8}>
                  {fields.map(field => (
                    <div
                      key={field.key}
                      style={{
                        display: 'grid',
                        gridTemplateColumns: '1fr 1fr 32px',
                        gap: 8,
                        alignItems: 'start',
                      }}
                    >
                      <Form.Item
                        {...field}
                        name={[field.name, 'key']}
                        style={{ marginBottom: 0 }}
                        rules={[
                          { required: true, message: '请输入 Token' },
                          {
                            validator: (_, value) => {
                              const key = String(value ?? '').trim()
                              if (!key) return Promise.resolve()
                              if (key === 'name' || key === 'description') {
                                return Promise.reject(new Error('name/description 使用上方覆盖字段'))
                              }
                              return Promise.resolve()
                            },
                          },
                        ]}
                      >
                        <Input placeholder="market" />
                      </Form.Item>
                      <Form.Item
                        {...field}
                        name={[field.name, 'value']}
                        style={{ marginBottom: 0 }}
                      >
                        <Input placeholder="EU" />
                      </Form.Item>
                      <Tooltip title="删除">
                        <Button icon={<DeleteOutlined />} onClick={() => remove(field.name)} />
                      </Tooltip>
                    </div>
                  ))}
                  <Button type="dashed" icon={<PlusOutlined />} onClick={() => add()}>
                    添加 Token
                  </Button>
                </Space>
              )}
            </Form.List>
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title="按条件批量操作"
        width={520}
        open={filterDrawerOpen}
        onClose={() => setFilterDrawerOpen(false)}
        extra={(
          <Space>
            <Button onClick={() => setFilterDrawerOpen(false)}>取消</Button>
            <Button type="primary" loading={batchSubmitting} onClick={submitFilterBatch}>
              执行
            </Button>
          </Space>
        )}
      >
        <Form form={filterForm} layout="vertical">
          <Typography.Paragraph type="secondary">
            条件批量会处理匹配条件的画布，最多处理指定 limit 条。请至少填写一个过滤条件。
          </Typography.Paragraph>
          <Form.Item name="operation" label="操作" rules={[{ required: true, message: '请选择操作' }]}>
            <Select
              options={[
                { value: 'pause', label: '暂停已发布画布' },
                { value: 'resume', label: '恢复非归档画布' },
                { value: 'archive', label: '归档画布' },
              ]}
            />
          </Form.Item>
          <Form.Item name="reason" label="操作原因" rules={[{ required: true, message: '请输入操作原因' }]}>
            <Input.TextArea rows={3} placeholder="例：活动结束后统一处理" />
          </Form.Item>
          <Form.Item name="status" label="状态过滤">
            <Select
              allowClear
              options={Object.entries(STATUS_MAP).map(([value, meta]) => ({
                value: Number(value),
                label: meta.label,
              }))}
            />
          </Form.Item>
          <Form.Item name="name" label="名称包含">
            <Input placeholder="例：增长活动" />
          </Form.Item>
          <Form.Item name="triggerType" label="触发类型">
            <Input placeholder="例：REALTIME / SCHEDULED" />
          </Form.Item>
          <Form.Item name="limit" label="最大处理条数" rules={[{ required: true, message: '请输入最大处理条数' }]}>
            <InputNumber min={1} max={200} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title="批量操作结果"
        width={760}
        open={resultDrawerOpen}
        onClose={() => setResultDrawerOpen(false)}
      >
        {batchResult && (
          <>
            <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="操作">{batchResult.operation}</Descriptions.Item>
              <Descriptions.Item label="总数">{batchResult.totalCount}</Descriptions.Item>
              <Descriptions.Item label="成功">{batchResult.successCount}</Descriptions.Item>
              <Descriptions.Item label="跳过">{batchResult.skippedCount}</Descriptions.Item>
              <Descriptions.Item label="失败">{batchResult.failedCount}</Descriptions.Item>
              <Descriptions.Item label="状态汇总">
                <Space wrap>
                  {Object.entries(batchResult.countsByStatus ?? {}).map(([status, count]) => {
                    const { label, color } = BATCH_STATUS_MAP[status] ?? { label: status, color: 'default' }
                    return <Tag key={status} color={color}>{label}: {count}</Tag>
                  })}
                </Space>
              </Descriptions.Item>
            </Descriptions>
            <Table
              rowKey={(record) => `${record.canvasId}-${record.targetCanvasId ?? 'source'}`}
              size="small"
              columns={batchResultColumns}
              dataSource={batchResult.items}
              pagination={batchResult.items.length > 10 ? { pageSize: 10 } : false}
            />
          </>
        )}
      </Drawer>
    </div>
  )
}
