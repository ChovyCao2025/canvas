/**
 * 页面职责：标签导入源管理面板，维护第三方推送标签数据的鉴权和启用状态。
 *
 * 维护说明：导入源 key/secret 与外部系统集成相关，页面只展示 secret 的脱敏信息。
 */
import { useEffect, useState } from 'react'
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Typography, message } from 'antd'
import { DeleteOutlined, EditOutlined, PlayCircleOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { tagImportApi } from '../../services/api'
import { DEFAULT_SOURCE_FIELD_MAPPING } from './tagImportTypes'
import type { TagImportResult, TagImportSource, TagImportSourceFormValues } from './tagImportTypes'

/** 导入源表格中的文本组件别名。 */
const { Text } = Typography

/** 标签导入源配置面板，负责远程拉取来源的 CRUD 和手动运行。 */
export default function TagImportSourcePanel() {
  const [data, setData] = useState<TagImportSource[]>([])
  const [loading, setLoading] = useState(false)
  const [visible, setVisible] = useState(false)
  const [editing, setEditing] = useState<TagImportSource | null>(null)
  const [saving, setSaving] = useState(false)
  const [runningId, setRunningId] = useState<number>()
  const [form] = Form.useForm<TagImportSourceFormValues>()

  /** 查询所有导入源；来源数量较少，当前页不做分页。 */
  const fetchSources = async () => {
    setLoading(true)
    try {
      const res = await tagImportApi.sources()
      setData(res.data.list)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void fetchSources()
  }, [])

  /** 新建来源默认使用 GET 和标准字段映射模板。 */
  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({
      method: 'GET',
      fieldMapping: DEFAULT_SOURCE_FIELD_MAPPING,
      enabled: true,
    })
    setVisible(true)
  }

  /** 编辑来源时回填脱敏后的配置；secret 等敏感字段不在前端明文展示。 */
  const openEdit = (record: TagImportSource) => {
    setEditing(record)
    form.setFieldsValue({
      name: record.name,
      url: record.url,
      method: record.method,
      recordsPath: record.recordsPath,
      fieldMapping: record.fieldMapping || DEFAULT_SOURCE_FIELD_MAPPING,
      enabled: record.enabled === 1,
    })
    setVisible(true)
  }

  /** 校验字段映射 JSON 后提交来源配置。 */
  const handleSubmit = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      JSON.parse(values.fieldMapping)
      const payload: Partial<TagImportSource> = {
        name: values.name,
        url: values.url,
        method: values.method,
        recordsPath: values.recordsPath,
        fieldMapping: values.fieldMapping,
        enabled: values.enabled ? 1 : 0,
      }
      if (editing) {
        await tagImportApi.updateSource(editing.id, payload)
        message.success('来源已更新')
      } else {
        await tagImportApi.createSource(payload)
        message.success('来源已创建')
      }
      setVisible(false)
      void fetchSources()
    } catch (error) {
      if (error instanceof SyntaxError) {
        message.error('字段映射必须是合法 JSON')
        return
      }
      throw error
    } finally {
      setSaving(false)
    }
  }

  /** 手动触发一次来源拉取，结果以成功/失败行数提示。 */
  const handleRun = async (record: TagImportSource) => {
    setRunningId(record.id)
    try {
      const res = await tagImportApi.runSource(record.id)
      const result: TagImportResult = res.data
      message.success(`运行完成：成功 ${result.successRows}，失败 ${result.failedRows}`)
    } catch {
      message.error('执行拉取失败')
    } finally {
      setRunningId(undefined)
    }
  }

  /** 导入源列表列定义，URL 和映射 JSON 以 code/ellipsis 展示避免撑宽表格。 */
  const columns: ColumnsType<TagImportSource> = [
    { title: '名称', dataIndex: 'name', width: 160 },
    {
      title: 'URL',
      dataIndex: 'url',
      render: (value: string) => <Text code ellipsis={{ tooltip: value }}>{value}</Text>,
    },
    { title: '方法', dataIndex: 'method', width: 90 },
    {
      title: 'recordsPath',
      dataIndex: 'recordsPath',
      width: 140,
      render: (value?: string) => value || '-',
    },
    {
      title: 'fieldMapping',
      dataIndex: 'fieldMapping',
      width: 220,
      render: (value?: string) => value ? <Text code ellipsis={{ tooltip: value }}>{value}</Text> : '-',
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      render: (value: number) => <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '禁用'}</Tag>,
    },
    {
      title: '操作',
      width: 140,
      render: (_, record) => (
        <Space size={4}>
          <Button
            size="small"
            icon={<PlayCircleOutlined />}
            loading={runningId === record.id}
            onClick={() => void handleRun(record)}
          >
            运行
          </Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm
            title="确认删除该来源？"
            okText="删除"
            okType="danger"
            cancelText="取消"
            onConfirm={() => tagImportApi.deleteSource(record.id).then(() => {
              message.success('来源已删除')
              void fetchSources()
            })}
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建来源</Button>
        <Button icon={<ReloadOutlined />} onClick={() => void fetchSources()} loading={loading}>刷新</Button>
      </Space>
      <Table rowKey="id" dataSource={data} columns={columns} loading={loading} pagination={false} />

      <Modal
        title={editing ? '编辑拉取来源' : '新建拉取来源'}
        open={visible}
        onOk={() => void handleSubmit()}
        onCancel={() => setVisible(false)}
        confirmLoading={saving}
        okText={editing ? '保存' : '创建'}
        cancelText="取消"
        width={720}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="如：会员中心标签接口" />
          </Form.Item>
          <Form.Item name="url" label="URL" rules={[{ required: true, message: '请输入 URL' }]}>
            <Input placeholder="https://example.com/api/tag-import" />
          </Form.Item>
          <Form.Item name="method" label="方法" rules={[{ required: true, message: '请选择方法' }]}>
            <Select options={[{ value: 'GET', label: 'GET' }, { value: 'POST', label: 'POST' }]} />
          </Form.Item>
          <Form.Item name="recordsPath" label="recordsPath">
            <Input placeholder="如：data.records" />
          </Form.Item>
          <Form.Item
            name="fieldMapping"
            label="fieldMapping"
            rules={[{ required: true, message: '请输入字段映射 JSON' }]}
          >
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
