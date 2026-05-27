/**
 * 页面职责：系统字典管理页，提供分类筛选、关键字搜索和字典项编辑。
 *
 * 维护说明：前台多个配置控件依赖这些字典项，禁用或改值会影响下拉选项。
 */
import { useEffect, useState } from 'react'
import {
  Button, Form, Input, InputNumber, Modal, Select,
  Space, Switch, Table, Tag, Typography, message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { SystemOption } from '../../types'
import { SYSTEM_OPTION_CATEGORIES, systemOptionsApi } from '../../services/systemOptions'

/** 页面标题和说明文本组件别名。 */
const { Title, Text } = Typography

/** 系统字典管理页主组件，维护表格筛选、编辑弹窗和保存动作。 */
export default function SystemOptionsPage() {
  const [data, setData] = useState<SystemOption[]>([])
  const [loading, setLoading] = useState(false)
  const [category, setCategory] = useState<string>()
  const [enabled, setEnabled] = useState<number>()
  const [keyword, setKeyword] = useState('')
  const [editing, setEditing] = useState<SystemOption | null>(null)
  const [form] = Form.useForm()

  /** 统一列表加载入口；筛选条件直接来自页面状态。 */
  const fetchList = async () => {
    setLoading(true)
    try {
      const res = await systemOptionsApi.adminList({
        category,
        enabled,
        keyword: keyword.trim() || undefined,
      })
      setData(res.data.list)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchList() }, [category, enabled])

  /** 打开编辑弹窗并把只允许修改的字段回填到表单。 */
  const openEdit = (record: SystemOption) => {
    setEditing(record)
    form.setFieldsValue({
      label: record.label,
      description: record.description,
      sortOrder: record.sortOrder,
      enabled: record.enabled === 1,
    })
  }

  /** 保存字典项；布尔开关在提交时转换为后端使用的 1/0。 */
  const save = async () => {
    if (!editing) return
    const values = await form.validateFields()
    await systemOptionsApi.update(editing.id, {
      label: values.label,
      description: values.description,
      sortOrder: values.sortOrder,
      enabled: values.enabled ? 1 : 0,
    })
    message.success('保存成功')
    setEditing(null)
    fetchList()
  }

  /** 字典项表格列，内置项只做展示提示，是否允许修改由后端最终校验。 */
  const columns: ColumnsType<SystemOption> = [
    { title: '分类', dataIndex: 'category', width: 220 },
    { title: 'Key', dataIndex: 'optionKey', width: 180 },
    { title: '显示名', dataIndex: 'label', width: 220 },
    { title: '描述', dataIndex: 'description', ellipsis: true },
    { title: '排序', dataIndex: 'sortOrder', width: 88 },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 88,
      render: value => value === 1 ? <Tag color="green">启用</Tag> : <Tag>禁用</Tag>,
    },
    {
      title: '内置',
      dataIndex: 'systemBuiltin',
      width: 88,
      render: value => value === 1 ? <Tag color="blue">内置</Tag> : <Tag>自定义</Tag>,
    },
    { title: '更新时间', dataIndex: 'updatedAt', width: 180 },
    {
      title: '操作',
      width: 96,
      fixed: 'right',
      render: (_, record) => <Button size="small" onClick={() => openEdit(record)}>编辑</Button>,
    },
  ]

  return (
    <div style={{ minHeight: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>系统选项配置</Title>
          <Text type="secondary">禁用只影响新配置，历史画布仍会保留并展示已有值。</Text>
        </div>
        <Space wrap>
          <Select
            allowClear
            showSearch
            placeholder="筛选分类"
            style={{ width: 260 }}
            options={SYSTEM_OPTION_CATEGORIES}
            value={category}
            onChange={setCategory}
            optionFilterProp="label"
          />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 120 }}
            value={enabled}
            onChange={setEnabled}
            options={[
              { value: 1, label: '启用' },
              { value: 0, label: '禁用' },
            ]}
          />
          <Input.Search
            allowClear
            placeholder="搜索 Key/显示名/描述"
            value={keyword}
            onChange={event => setKeyword(event.target.value)}
            onSearch={fetchList}
            style={{ width: 240 }}
          />
        </Space>
      </div>

      <Table
        rowKey="id"
        dataSource={data}
        columns={columns}
        loading={loading}
        pagination={{ pageSize: 20, showSizeChanger: true }}
        scroll={{ x: 1180 }}
      />

      <Modal
        title="编辑系统选项"
        open={!!editing}
        onOk={save}
        onCancel={() => setEditing(null)}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="分类">
            <Input value={editing?.category} disabled />
          </Form.Item>
          <Form.Item label="Key">
            <Input value={editing?.optionKey} disabled />
          </Form.Item>
          <Form.Item name="label" label="显示名" rules={[{ required: true, message: '请输入显示名' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序" rules={[{ required: true, message: '请输入排序' }]}>
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
