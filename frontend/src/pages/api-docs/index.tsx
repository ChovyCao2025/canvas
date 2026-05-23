import { useMemo, useState } from 'react'
import { Button, Card, Empty, Input, Space, Switch, Table, Tag, Tooltip, Typography, message } from 'antd'
import { CopyOutlined, SearchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  API_DOCS,
  filterApiDocEndpoints,
  formatJsonExample,
  getApiDocCategorySummaries,
} from './apiDocs'
import type { ApiDocEndpoint, ApiDocParam } from './apiDocs'

const { Paragraph, Text, Title } = Typography

const methodColors: Record<ApiDocEndpoint['method'], string> = {
  GET: 'blue',
  POST: 'green',
  PUT: 'orange',
  DELETE: 'red',
}

const paramColumns: ColumnsType<ApiDocParam> = [
  {
    title: '参数',
    dataIndex: 'name',
    width: 180,
    render: (name: string, param) => (
      <Space size={6} wrap>
        <Text code style={{ wordBreak: 'break-all' }}>{name}</Text>
        {param.required ? <Tag color="red">必填</Tag> : null}
      </Space>
    ),
  },
  {
    title: '位置',
    dataIndex: 'in',
    width: 90,
    render: (value: ApiDocParam['in']) => <Tag>{value}</Tag>,
  },
  {
    title: '说明',
    dataIndex: 'desc',
    render: (desc: string) => <Text style={{ wordBreak: 'break-word' }}>{desc}</Text>,
  },
]

function CodeExample({ title, value }: { title: string; value: unknown }) {
  const code = formatJsonExample(value)

  const copyCode = async () => {
    await navigator.clipboard.writeText(code)
    message.success('已复制')
  }

  return (
    <div style={{ minWidth: 0 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, marginBottom: 8 }}>
        <Text strong>{title}</Text>
        <Tooltip title="复制代码">
          <Button size="small" type="text" icon={<CopyOutlined />} onClick={copyCode} />
        </Tooltip>
      </div>
      <pre style={{
        margin: 0,
        padding: 12,
        overflowX: 'auto',
        whiteSpace: 'pre',
        border: '1px solid #d9e0ea',
        borderRadius: 6,
        background: '#0f172a',
        color: '#dbeafe',
        fontSize: 12,
        lineHeight: 1.6,
        maxWidth: '100%',
      }}>
        <code>{code}</code>
      </pre>
    </div>
  )
}

function EndpointCard({ endpoint }: { endpoint: ApiDocEndpoint }) {
  return (
    <Card
      size="small"
      style={{ borderRadius: 8 }}
      bodyStyle={{ padding: 18 }}
    >
      <Space direction="vertical" size={14} style={{ width: '100%', minWidth: 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12, flexWrap: 'wrap' }}>
          <Space size={8} wrap style={{ minWidth: 0, flex: 1 }}>
            <Tag color={methodColors[endpoint.method]} style={{ marginInlineEnd: 0 }}>{endpoint.method}</Tag>
            <Text code style={{ fontSize: 13, whiteSpace: 'normal', wordBreak: 'break-all' }}>
              {endpoint.path}
            </Text>
          </Space>
          <Space size={8} wrap>
            <Tag color={endpoint.auth === 'bearer' ? 'geekblue' : 'default'}>
              {endpoint.auth === 'bearer' ? 'Bearer Auth' : 'No Auth'}
            </Tag>
            {endpoint.internal ? <Tag color="gold">内部管理</Tag> : null}
          </Space>
        </div>

        <div>
          <Title level={5} style={{ margin: 0 }}>{endpoint.title}</Title>
          <Paragraph type="secondary" style={{ margin: '6px 0 0', lineHeight: 1.7 }}>
            {endpoint.summary}
          </Paragraph>
        </div>

        {endpoint.params && endpoint.params.length > 0 ? (
          <Table
            size="small"
            rowKey={param => `${param.in}-${param.name}-${param.desc}`}
            columns={paramColumns}
            dataSource={endpoint.params}
            pagination={false}
            scroll={{ x: 520 }}
          />
        ) : (
          <Text type="secondary">无参数</Text>
        )}

        {endpoint.requestExample !== undefined ? (
          <CodeExample title="Request Example" value={endpoint.requestExample} />
        ) : null}
        <CodeExample title="Response Example" value={endpoint.responseExample} />
      </Space>
    </Card>
  )
}

export default function ApiDocsPage() {
  const [keyword, setKeyword] = useState('')
  const [showInternal, setShowInternal] = useState(false)
  const [category, setCategory] = useState<string | undefined>()

  const categorySourceEndpoints = useMemo(() => filterApiDocEndpoints({
    showInternal,
    keyword,
  }), [keyword, showInternal])

  const categorySummaries = useMemo(
    () => getApiDocCategorySummaries(categorySourceEndpoints),
    [categorySourceEndpoints],
  )

  const selectedCategory = categorySummaries.some(summary => summary.key === category) ? category : undefined
  const endpoints = useMemo(() => filterApiDocEndpoints({
    showInternal,
    keyword,
    category: selectedCategory,
  }), [keyword, selectedCategory, showInternal])

  const totalVisibleCount = categorySourceEndpoints.length
  const publicCount = API_DOCS.filter(endpoint => !endpoint.internal).length
  const internalCount = API_DOCS.length - publicCount

  return (
    <div style={{ padding: 24, minWidth: 0 }}>
      <Space direction="vertical" size={20} style={{ width: '100%' }}>
        <div style={{ maxWidth: 960 }}>
          <Title level={3} style={{ margin: 0 }}>API 说明</Title>
          <Paragraph type="secondary" style={{ margin: '8px 0 0', lineHeight: 1.8 }}>
            默认展示供外部业务系统调用的 API，包括登录、事件上报、画布触发和审批回调。
            打开“显示内部管理 API”后，可查看画布、配置、观测、运维和用户管理等后台接口。
          </Paragraph>
        </div>

        <div style={{
          display: 'flex',
          gap: 20,
          alignItems: 'flex-start',
          flexWrap: 'wrap',
        }}>
          <Card
            size="small"
            style={{ width: 280, maxWidth: '100%', borderRadius: 8 }}
            bodyStyle={{ padding: 16 }}
          >
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Input
                allowClear
                prefix={<SearchOutlined />}
                placeholder="搜索标题、路径、方法或分类"
                value={keyword}
                onChange={event => setKeyword(event.target.value)}
              />

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
                <div>
                  <Text strong>显示内部管理 API</Text>
                  <div>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      外部 {publicCount} / 内部 {internalCount}
                    </Text>
                  </div>
                </div>
                <Switch checked={showInternal} onChange={setShowInternal} />
              </div>

              <div>
                <Text strong>分类</Text>
                <Space direction="vertical" size={8} style={{ width: '100%', marginTop: 10 }}>
                  <Button
                    block
                    type={!selectedCategory ? 'primary' : 'default'}
                    onClick={() => setCategory(undefined)}
                    style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                  >
                    <span>全部 API</span>
                    <Tag style={{ marginInlineEnd: 0 }}>{totalVisibleCount}</Tag>
                  </Button>
                  {categorySummaries.map(summary => (
                    <Button
                      key={summary.key}
                      block
                      type={selectedCategory === summary.key ? 'primary' : 'default'}
                      onClick={() => setCategory(summary.key)}
                      style={{
                        height: 'auto',
                        minHeight: 42,
                        padding: '8px 11px',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        textAlign: 'left',
                      }}
                    >
                      <span style={{ minWidth: 0 }}>
                        <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {summary.title}
                        </span>
                        <span style={{ display: 'block', color: selectedCategory === summary.key ? 'rgba(255,255,255,0.75)' : '#8c8c8c', fontSize: 12, whiteSpace: 'normal' }}>
                          {summary.description}
                        </span>
                      </span>
                      <Tag style={{ marginInlineEnd: 0 }}>{summary.count}</Tag>
                    </Button>
                  ))}
                </Space>
              </div>
            </Space>
          </Card>

          <div style={{ flex: '1 1 560px', minWidth: 0 }}>
            <Space direction="vertical" size={14} style={{ width: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
                <Text type="secondary">
                  当前展示 <Text strong>{endpoints.length}</Text> 个接口
                </Text>
                {selectedCategory ? (
                  <Button size="small" onClick={() => setCategory(undefined)}>清除分类</Button>
                ) : null}
              </div>

              {endpoints.length > 0 ? (
                endpoints.map(endpoint => <EndpointCard key={endpoint.id} endpoint={endpoint} />)
              ) : (
                <Card size="small" style={{ borderRadius: 8 }}>
                  <Empty description="没有匹配的 API" />
                </Card>
              )}
            </Space>
          </div>
        </div>
      </Space>
    </div>
  )
}
