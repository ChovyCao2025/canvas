import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Empty, Form, Input, Modal, Space, Spin, Table, Tabs, Tag, Typography, message } from 'antd'
import { searchMarketingApi } from '../../services/searchMarketingApi'
import {
  calculateSearchMarketingKpis,
  canApplyMutation,
  canDryRunMutation,
  readinessStatusView,
  redactSearchMarketingSecrets,
  syncRunStatusView,
  type SearchMarketingImpactWindow,
  type SearchMarketingKeyword,
  type SearchMarketingMutation,
  type SearchMarketingOpportunity,
  type SearchMarketingProviderChange,
  type SearchMarketingReadiness,
  type SearchMarketingSnapshot,
  type SearchMarketingSource,
  type SearchMarketingSyncRun,
  type SearchMarketingUrlInspection,
} from './searchMarketingWorkbench'

const { Text, Title } = Typography
const compactActionButtonStyle = { minWidth: 52 }

interface ProposalFormValues {
  mutationKey: string
}

export default function SearchMarketingPage() {
  const [proposalForm] = Form.useForm<ProposalFormValues>()
  const [readiness, setReadiness] = useState<SearchMarketingReadiness | null>(null)
  const [sources, setSources] = useState<SearchMarketingSource[]>([])
  const [keywords, setKeywords] = useState<SearchMarketingKeyword[]>([])
  const [snapshots, setSnapshots] = useState<SearchMarketingSnapshot[]>([])
  const [opportunities, setOpportunities] = useState<SearchMarketingOpportunity[]>([])
  const [mutations, setMutations] = useState<SearchMarketingMutation[]>([])
  const [syncRuns, setSyncRuns] = useState<SearchMarketingSyncRun[]>([])
  const [urlInspections, setUrlInspections] = useState<SearchMarketingUrlInspection[]>([])
  const [providerChanges, setProviderChanges] = useState<SearchMarketingProviderChange[]>([])
  const [impactWindows, setImpactWindows] = useState<SearchMarketingImpactWindow[]>([])
  const [proposalOpportunity, setProposalOpportunity] = useState<SearchMarketingOpportunity | null>(null)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      const [
        readinessResponse,
        sourceResponse,
        keywordResponse,
        snapshotResponse,
        opportunityResponse,
        mutationResponse,
        syncRunResponse,
        urlInspectionResponse,
        providerChangeResponse,
        impactWindowResponse,
      ] =
        await Promise.all([
          searchMarketingApi.readiness(),
          searchMarketingApi.listSources({ limit: 50 }),
          searchMarketingApi.listKeywords({ limit: 50 }),
          searchMarketingApi.listSnapshots({ limit: 50 }),
          searchMarketingApi.listOpportunities({ limit: 50 }),
          searchMarketingApi.listMutations({ limit: 50 }),
          searchMarketingApi.listSyncRuns({ limit: 50 }),
          searchMarketingApi.listUrlInspections({ limit: 50 }),
          searchMarketingApi.listProviderChanges({ limit: 50 }),
          searchMarketingApi.listImpactWindows({ limit: 50 }),
        ])
      setReadiness(readinessResponse.data)
      setSources(sourceResponse.data ?? [])
      setKeywords(keywordResponse.data ?? [])
      setSnapshots(snapshotResponse.data ?? [])
      setOpportunities(opportunityResponse.data ?? [])
      setMutations(mutationResponse.data ?? [])
      setSyncRuns(syncRunResponse.data ?? [])
      setUrlInspections(urlInspectionResponse.data ?? [])
      setProviderChanges(providerChangeResponse.data ?? [])
      setImpactWindows(impactWindowResponse.data ?? [])
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '请稍后重试')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const kpis = useMemo(
    () => calculateSearchMarketingKpis({ snapshots, opportunities, mutations }),
    [snapshots, opportunities, mutations],
  )

  const syncSource = useCallback(async (source: SearchMarketingSource) => {
    await searchMarketingApi.syncSource(source.id, { runType: 'PERFORMANCE' })
    message.success('同步任务已提交')
    await load()
  }, [load])

  const openProposal = useCallback((opportunity: SearchMarketingOpportunity) => {
    proposalForm.setFieldsValue({ mutationKey: `${opportunity.opportunityType.toLowerCase()}-${opportunity.id}` })
    setProposalOpportunity(opportunity)
  }, [proposalForm])

  const submitProposal = useCallback(async () => {
    if (!proposalOpportunity) return
    const values = await proposalForm.validateFields()
    await searchMarketingApi.proposeOpportunityMutation(proposalOpportunity.id, {
      mutationKey: values.mutationKey,
      mutationType: 'UPDATE_KEYWORD_BID',
      entityType: 'KEYWORD',
      externalEntityId: 'customers/1/adGroupCriteria/2~3',
      dryRunRequired: true,
      payload: { bidMicros: 1500000 },
    })
    setProposalOpportunity(null)
    await load()
  }, [load, proposalForm, proposalOpportunity])

  const executeMutation = useCallback(async (row: SearchMarketingMutation, dryRun: boolean) => {
    await searchMarketingApi.executeMutation(row.id, { dryRun, partialFailure: true })
    message.success(dryRun ? '干跑已提交' : '应用已提交')
    await load()
  }, [load])

  const reconcileMutation = useCallback(async (row: SearchMarketingMutation) => {
    await searchMarketingApi.reconcileMutation(row.id)
    message.success('对账已提交')
    await load()
  }, [load])

  const evaluateDueImpactWindows = useCallback(async () => {
    await searchMarketingApi.evaluateDueImpactWindows({ limit: 50 })
    message.success('影响窗口评估已提交')
    await load()
  }, [load])

  const readinessView = loading && !readiness
    ? { text: '加载中', color: 'blue' }
    : readinessStatusView(readiness?.status ?? 'BLOCKED')

  return (
    <div style={{ padding: 24 }}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
          <Title level={2} style={{ margin: 0 }}>SEO / SEM 管理</Title>
          <Tag color={readinessView.color}>{readinessView.text}</Tag>
        </Space>

        {loading && (
          <Space size={8} align="center">
            <Spin size="small" />
            <Text type="secondary">加载搜索营销数据</Text>
          </Space>
        )}

        {loadError && (
          <Alert
            showIcon
            type="error"
            message="搜索营销数据加载失败"
            description={loadError}
            action={(
              <Button size="small" aria-label="重试" onClick={() => void load()}>
                重试
              </Button>
            )}
          />
        )}

        <Space size={8} wrap>
          <Tag>SEO Clicks {kpis.seoClicks}</Tag>
          <Tag>SEM Spend {kpis.semSpend}</Tag>
          <Tag>Conversions {kpis.conversions}</Tag>
          <Tag>ROAS {kpis.roas}</Tag>
          <Tag>Failed Writes {kpis.failedWrites}</Tag>
        </Space>

        <Tabs
          items={[
            {
              key: 'overview',
              label: 'Overview',
              children: (
                <Card size="small">
                  <Space direction="vertical" size={8} style={{ width: '100%' }}>
                    {sources.length === 0 ? (
                      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无搜索营销来源" />
                    ) : sources.map(source => (
                      <Space key={source.id} align="center">
                        <Text strong>{source.displayName}</Text>
                        <Tag>{source.provider}</Tag>
                        <Button size="small" onClick={() => void syncSource(source)}>
                          同步 {source.displayName}
                        </Button>
                      </Space>
                    ))}
                    {syncRuns.length === 0 && (
                      <Text type="secondary">暂无同步记录</Text>
                    )}
                    {syncRuns.map(run => {
                      const view = syncRunStatusView(run.status)
                      return <Tag key={run.id} color={view.color}>{run.runType} {view.text}</Tag>
                    })}
                  </Space>
                </Card>
              ),
            },
            {
              key: 'sources',
              label: 'Sources',
              children: <SimpleList rows={sources.map(source => source.displayName)} emptyText="暂无 Sources 数据" />,
            },
            {
              key: 'keywords',
              label: 'Keyword Portfolio',
              children: <SimpleList rows={keywords.map(keyword => keyword.keywordText)} emptyText="暂无 Keyword Portfolio 数据" />,
            },
            {
              key: 'snapshots',
              label: 'Performance Evidence',
              children: <SimpleList rows={snapshots.map(snapshot => `${snapshot.channel} ${snapshot.snapshotDate}`)} emptyText="暂无 Performance Evidence 数据" />,
            },
            {
              key: 'seo',
              label: 'SEO Technical Evidence',
              children: (
                <Table<SearchMarketingUrlInspection>
                  size="small"
                  rowKey="id"
                  dataSource={urlInspections.map(redactSearchMarketingSecrets)}
                  loading={loading}
                  locale={tableLocale('暂无 SEO 技术证据')}
                  pagination={false}
                  scroll={{ x: 720 }}
                  columns={[
                    {
                      title: 'Page',
                      dataIndex: 'pageUrl',
                    },
                    {
                      title: 'Indexed',
                      dataIndex: 'indexedState',
                    },
                    {
                      title: 'Crawl',
                      dataIndex: 'crawlState',
                    },
                    {
                      title: 'Inspection Date',
                      dataIndex: 'inspectionDate',
                    },
                  ]}
                />
              ),
            },
            {
              key: 'opportunities',
              label: 'Opportunities',
              children: (
                <Table<SearchMarketingOpportunity>
                  size="small"
                  rowKey="id"
                  dataSource={opportunities}
                  loading={loading}
                  locale={tableLocale('暂无机会建议')}
                  pagination={false}
                  scroll={{ x: 640 }}
                  columns={[
                    {
                      title: 'Recommendation',
                      dataIndex: 'recommendation',
                    },
                    {
                      title: 'Action',
                      key: 'action',
                      render: (_, row) => (
                        <Button size="small" onClick={() => openProposal(row)}>
                          创建提案 {row.opportunityType}
                        </Button>
                      ),
                    },
                  ]}
                />
              ),
            },
            {
              key: 'writes',
              label: 'Provider Writes and Impact',
              children: (
                <Space direction="vertical" size={12} style={{ width: '100%' }}>
                  <Button size="small" onClick={() => void evaluateDueImpactWindows()}>
                    评估到期影响窗口
                  </Button>
                  <Table<SearchMarketingMutation>
                    size="small"
                    rowKey="id"
                    dataSource={mutations.map(redactSearchMarketingSecrets)}
                    loading={loading}
                    locale={tableLocale('暂无供应商写入')}
                    pagination={false}
                    scroll={{ x: 760 }}
                    columns={[
                      {
                        title: 'Mutation',
                        dataIndex: 'mutationKey',
                      },
                      {
                        title: 'Status',
                        dataIndex: 'status',
                      },
                      {
                        title: 'Action',
                        key: 'action',
                        render: (_, row) => (
                          <Space size={8} wrap>
                            <Button
                              size="small"
                              aria-label={`干跑 ${row.mutationKey}`}
                              disabled={!canDryRunMutation(row)}
                              style={compactActionButtonStyle}
                              onClick={() => void executeMutation(row, true)}
                            >
                              干跑
                            </Button>
                            <Button
                              size="small"
                              aria-label={`应用 ${row.mutationKey}`}
                              disabled={!canApplyMutation(row, readiness?.status ?? 'BLOCKED')}
                              style={compactActionButtonStyle}
                              onClick={() => void executeMutation(row, false)}
                            >
                              应用
                            </Button>
                            <Button
                              size="small"
                              aria-label={`对账 ${row.mutationKey}`}
                              disabled={row.status !== 'APPLIED' && row.status !== 'RECONCILE_FAILED'}
                              style={compactActionButtonStyle}
                              onClick={() => void reconcileMutation(row)}
                            >
                              对账
                            </Button>
                          </Space>
                        ),
                      },
                    ]}
                  />
                  <Table<SearchMarketingProviderChange>
                    size="small"
                    rowKey="id"
                    dataSource={providerChanges.map(redactSearchMarketingSecrets)}
                    loading={loading}
                    locale={tableLocale('暂无供应商变更')}
                    pagination={false}
                    scroll={{ x: 760 }}
                    columns={[
                      {
                        title: 'Provider Change',
                        dataIndex: 'externalResourceId',
                      },
                      {
                        title: 'Change Type',
                        dataIndex: 'changeType',
                      },
                      {
                        title: 'Reconciliation',
                        dataIndex: 'reconciliationStatus',
                      },
                      {
                        title: 'Changed At',
                        dataIndex: 'providerChangedAt',
                      },
                    ]}
                  />
                  <Table<SearchMarketingImpactWindow>
                    size="small"
                    rowKey="id"
                    dataSource={impactWindows.map(redactSearchMarketingSecrets)}
                    loading={loading}
                    locale={tableLocale('暂无影响窗口')}
                    pagination={false}
                    scroll={{ x: 720 }}
                    columns={[
                      {
                        title: 'Impact Mutation',
                        dataIndex: 'mutationId',
                      },
                      {
                        title: 'Window Status',
                        dataIndex: 'status',
                      },
                      {
                        title: 'Decision',
                        dataIndex: 'decision',
                        render: value => value ?? '-',
                      },
                      {
                        title: 'Due At',
                        dataIndex: 'dueAt',
                      },
                    ]}
                  />
                </Space>
              ),
            },
          ]}
        />
      </Space>

      <Modal
        title="创建提案"
        open={Boolean(proposalOpportunity)}
        okText="提交提案"
        cancelText="取消"
        onOk={() => void submitProposal()}
        onCancel={() => setProposalOpportunity(null)}
      >
        <Form<ProposalFormValues> form={proposalForm} layout="vertical">
          <Form.Item name="mutationKey" label="Mutation Key" rules={[{ required: true, message: '请输入 Mutation Key' }]}>
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

function SimpleList({ rows, emptyText }: { rows: string[]; emptyText: string }) {
  return (
    <Card size="small">
      <Space direction="vertical" size={4} style={{ width: '100%' }}>
        {rows.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={emptyText} />
        ) : rows.map(row => <Text key={row}>{row}</Text>)}
      </Space>
    </Card>
  )
}

function tableLocale(description: string) {
  return {
    emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={description} />,
  }
}
