/**
 * 页面职责：标签定义管理页，维护标签基本信息和枚举值。
 *
 * 维护说明：标签定义供 CDP、打标节点和人群规则共用。
 */
import { useEffect, useState } from 'react'
import { Button, Divider, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Typography, message } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { tagDefinitionApi } from '../../services/api'
import { useSystemOptions } from '../../hooks/useSystemOptions'
import { normalizeTagDefinitionPayload } from './tagConfigPayload'
import type { TagConfigRecord, TagFormValues } from './tagTypes'
import TagValueEditor from './tagValueEditor'

/** 页面标题组件别名。 */
const { Title } = Typography

/** 标签定义管理页面主组件。 */
export default function TagConfigPage() {
  const [data, setData] = useState<TagConfigRecord[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [visible, setVisible] = useState(false)
  const [editing, setEditing] = useState<TagConfigRecord | null>(null)
  const [form] = Form.useForm<TagFormValues>()
  const [saving, setSaving] = useState(false)
  const { options: tagTypeOptions } = useSystemOptions('tag_type')

  /** 分页拉取标签定义列表。 */
  const fetchList = async (p = page) => {
    setLoading(true)
    try {
      const res = await tagDefinitionApi.list({ page: p, size: 20 })
      setData(res.data.list)
      setTotal(res.data.total)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchList(1) }, [])

  /** 新建标签时设置常用默认值，和后端默认写入策略保持一致。 */
  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({
      tagType: 'offline',
      enabled: true,
      valueType: 'STRING',
      manualEnabled: true,
      writePolicy: 'UPSERT',
    })
    setVisible(true)
  }

  /** 编辑标签时把后端 1/0 状态转换为表单 Switch 所需的 boolean。 */
  const openEdit = (record: TagConfigRecord) => {
    setEditing(record)
    form.setFieldsValue({
      ...record,
      enabled: record.enabled === 1,
      manualEnabled: record.manualEnabled !== 0,
    })
    setVisible(true)
  }

  /** 保存标签定义；新建后回到第一页，编辑后保留当前页。 */
  const handleOk = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      const body = normalizeTagDefinitionPayload(values)
      if (editing) {
        await tagDefinitionApi.update(editing.id, body)
        message.success('更新成功')
      } else {
        await tagDefinitionApi.create(body)
        message.success('创建成功')
      }
      setVisible(false)
      fetchList(editing ? page : 1)
    } finally {
      setSaving(false)
    }
  }

  /** 标签表格列；删除后重新加载当前页。 */
  const columns: ColumnsType<TagConfigRecord> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '名称', dataIndex: 'name' },
    { title: '标签编码', dataIndex: 'tagCode', ellipsis: true },
    {
      title: '类型',
      dataIndex: 'tagType',
      width: 90,
      render: (value: string) => <Tag color={value === 'offline' ? 'blue' : 'green'}>{value === 'offline' ? '离线' : '实时'}</Tag>,
    },
    { title: '值类型', dataIndex: 'valueType', width: 90, render: value => <Tag>{value || 'STRING'}</Tag> },
    {
      title: '人工打标',
      dataIndex: 'manualEnabled',
      width: 90,
      render: value => <Tag color={value === 0 ? 'default' : 'green'}>{value === 0 ? '关闭' : '允许'}</Tag>,
    },
    { title: '分类', dataIndex: 'category', width: 110, ellipsis: true },
    { title: '负责人', dataIndex: 'owner', width: 100, ellipsis: true },
    { title: '说明', dataIndex: 'description', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 72,
      render: (value: number) => <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '禁用'}</Tag>,
    },
    {
      title: '操作',
      width: 100,
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm
            title="确认删除？"
            onConfirm={() => tagDefinitionApi.delete(record.id).then(() => { message.success('已删除'); fetchList(page) })}
            okText="删除"
            okType="danger"
            cancelText="取消"
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0 }}>标签配置</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建标签</Button>
      </div>
      <Table
        rowKey="id"
        dataSource={data}
        columns={columns}
        loading={loading}
        pagination={{ total, pageSize: 20, current: page, onChange: p => { setPage(p); fetchList(p) } }}
      />

      <Modal
        title={editing ? '编辑标签' : '新建标签'}
        open={visible}
        onOk={handleOk}
        onCancel={() => setVisible(false)}
        confirmLoading={saving}
        width={760}
        okText={editing ? '保存' : '创建'}
        cancelText="取消"
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="标签名称" rules={[{ required: true }]}>
            <Input placeholder="如：新客标签" />
          </Form.Item>
          <Form.Item name="tagCode" label="标签编码" rules={[{ required: true }]}>
            <Input placeholder="如：new_user" />
          </Form.Item>
          <Form.Item name="tagType" label="类型" rules={[{ required: true }]}>
            <Select options={tagTypeOptions} />
          </Form.Item>
          <Form.Item name="valueType" label="标签值类型" rules={[{ required: true }]}>
            <Select options={[
              { value: 'STRING', label: '字符串' },
              { value: 'NUMBER', label: '数字' },
              { value: 'BOOLEAN', label: '布尔' },
              { value: 'JSON', label: 'JSON' },
            ]} />
          </Form.Item>
          <Form.Item name="manualEnabled" label="允许人工打标" valuePropName="checked">
            <Switch checkedChildren="允许" unCheckedChildren="关闭" />
          </Form.Item>
          <Form.Item name="defaultTtlDays" label="默认有效期（天）">
            <InputNumber style={{ width: '100%' }} min={1} placeholder="留空表示长期有效" />
          </Form.Item>
          <Form.Item name="category" label="分类">
            <Input placeholder="如：生命周期" />
          </Form.Item>
          <Form.Item name="owner" label="负责人">
            <Input placeholder="如：growth" />
          </Form.Item>
          <Form.Item name="writePolicy" label="写入策略">
            <Select disabled options={[{ value: 'UPSERT', label: '覆盖当前值' }]} />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
          {editing?.tagCode ? (
            <>
              <Divider style={{ margin: '8px 0 16px' }}>标签值管理</Divider>
              <TagValueEditor tagCode={editing.tagCode} />
            </>
          ) : null}
        </Form>
      </Modal>
    </div>
  )
}
