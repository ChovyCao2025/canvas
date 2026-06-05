# Homepage Material Ops Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework `/home` into an operations-first dashboard with the confirmed B information architecture and C lightweight Material visual language.

**Architecture:** Keep the existing frontend-only `homeApi.overview(days)` flow. Add focused presentation helpers in `homeOverview.ts`, then render the new page layout in `index.tsx` using Ant Design, Recharts, and local inline style constants.

**Tech Stack:** React 18, TypeScript, Ant Design 5, Recharts, Vitest, Testing Library.

---

## File Structure

- Modify: `frontend/src/pages/home/homeOverview.ts` — add derived view models for risk summary, attention sorting, local homepage search, chip presentation, and action metadata.
- Modify: `frontend/src/pages/home/homeOverview.test.ts` — extend pure helper coverage for risk summary, sorting, search filtering, and chip labels.
- Create: `frontend/src/pages/home/index.test.tsx` — page-level rendering coverage for the Material ops layout, local search, healthy empty state, and error retry.
- Modify: `frontend/src/pages/home/index.tsx` — replace current layout with the approved information architecture and lightweight Material-inspired styling.

## Task 1: Add Homepage Presentation Helpers

**Files:**
- Modify: `frontend/src/pages/home/homeOverview.ts`
- Modify: `frontend/src/pages/home/homeOverview.test.ts`

- [ ] **Step 1: Write failing helper tests**

Append these tests to `frontend/src/pages/home/homeOverview.test.ts` and update the import list to include the new helpers:

```ts
import {
  buildKpiCards,
  buildRiskSummary,
  filterHomeOverview,
  getAttentionAction,
  getAttentionPresentation,
  HOME_RANGE_OPTIONS,
  sortAttentionItems,
  type HomeOverview,
} from './homeOverview'

it('builds the highest-priority risk summary from attention items', () => {
  const summary = buildRiskSummary({
    ...overview(),
    attentionItems: [
      { canvasId: 1, name: '一般提醒', type: 'HAS_FAILURES', message: '有失败执行', severity: 'info' },
      { canvasId: 2, name: '高失败旅程', type: 'HIGH_FAILURE_RATE', message: '失败率 4.8%', severity: 'error' },
      { canvasId: 3, name: '关注旅程', type: 'HAS_FAILURES', message: '存在失败执行', severity: 'warning' },
    ],
  })

  expect(summary).toEqual({
    healthy: false,
    title: '高失败旅程',
    message: '失败率 4.8%',
    severity: 'error',
    actionLabel: '处理',
    targetCanvasId: 2,
    failedExecutions: '26',
    successRate: '97.8%',
    pendingCount: 3,
  })
})

it('builds a healthy summary when no attention items exist', () => {
  expect(buildRiskSummary(overview())).toEqual({
    healthy: true,
    title: '当前暂无高优先级异常',
    message: '近 7 天旅程运行稳定，可继续关注触达趋势和 Top 旅程表现',
    severity: 'success',
    actionLabel: '查看趋势',
    targetCanvasId: null,
    failedExecutions: '26',
    successRate: '97.8%',
    pendingCount: 0,
  })
})

it('sorts attention items by severity while preserving backend order within each severity', () => {
  const items = [
    { canvasId: 1, name: 'info', type: 'HAS_FAILURES', message: 'i', severity: 'info' },
    { canvasId: 2, name: 'warning one', type: 'HAS_FAILURES', message: 'w1', severity: 'warning' },
    { canvasId: 3, name: 'error', type: 'HAS_FAILURES', message: 'e', severity: 'error' },
    { canvasId: 4, name: 'warning two', type: 'HAS_FAILURES', message: 'w2', severity: 'warning' },
  ]

  expect(sortAttentionItems(items).map(item => item.canvasId)).toEqual([3, 2, 4, 1])
})

it('filters homepage overview by journey name without mutating the original overview', () => {
  const source = {
    ...overview(),
    topCanvases: [
      { canvasId: 1, name: '新人激活 7 日链路', total: 100, uniqueUsers: 80, successRate: '99%', failed: 1 },
      { canvasId: 2, name: '沉睡用户召回', total: 50, uniqueUsers: 30, successRate: '95%', failed: 8 },
    ],
    attentionItems: [
      { canvasId: 2, name: '沉睡用户召回', type: 'HIGH_FAILURE_RATE', message: '失败率偏高', severity: 'warning' },
      { canvasId: 3, name: '生日礼遇活动', type: 'NO_RECENT_EXECUTIONS', message: '无执行记录', severity: 'info' },
    ],
  }

  const filtered = filterHomeOverview(source, '召回')

  expect(filtered.topCanvases.map(item => item.name)).toEqual(['沉睡用户召回'])
  expect(filtered.attentionItems.map(item => item.name)).toEqual(['沉睡用户召回'])
  expect(source.topCanvases).toHaveLength(2)
})

it('maps attention item actions to operator-facing labels', () => {
  expect(getAttentionAction({ type: 'NO_RECENT_EXECUTIONS' })).toEqual({ label: '编辑', destination: 'edit' })
  expect(getAttentionAction({ type: 'HIGH_FAILURE_RATE' })).toEqual({ label: '处理', destination: 'stats' })
  expect(getAttentionAction({ type: 'HAS_FAILURES' })).toEqual({ label: '查看', destination: 'stats' })
})
```

- [ ] **Step 2: Run helper tests and verify RED**

Run:

```bash
cd frontend
npm run test -- src/pages/home/homeOverview.test.ts
```

Expected: FAIL because `buildRiskSummary`, `sortAttentionItems`, `filterHomeOverview`, and `getAttentionAction` are not exported yet.

- [ ] **Step 3: Implement helper view models**

Add these exports to `frontend/src/pages/home/homeOverview.ts`:

```ts
export type AttentionSeverity = 'error' | 'warning' | 'info' | 'success' | string

export interface RiskSummary {
  healthy: boolean
  title: string
  message: string
  severity: AttentionSeverity
  actionLabel: string
  targetCanvasId: number | null
  failedExecutions: string
  successRate: string
  pendingCount: number
}

export interface AttentionAction {
  label: string
  destination: 'stats' | 'edit'
}

const ATTENTION_SEVERITY_ORDER: Record<string, number> = {
  error: 0,
  warning: 1,
  info: 2,
}

export function sortAttentionItems(items: HomeAttentionItem[]) {
  return [...items].sort((left, right) => (
    (ATTENTION_SEVERITY_ORDER[left.severity] ?? 3) - (ATTENTION_SEVERITY_ORDER[right.severity] ?? 3)
  ))
}

export function getAttentionAction(item: Pick<HomeAttentionItem, 'type'>): AttentionAction {
  if (item.type === 'NO_RECENT_EXECUTIONS') return { label: '编辑', destination: 'edit' }
  if (item.type === 'HIGH_FAILURE_RATE') return { label: '处理', destination: 'stats' }
  return { label: '查看', destination: 'stats' }
}

export function buildRiskSummary(overview: HomeOverview): RiskSummary {
  const primary = sortAttentionItems(overview.attentionItems)[0]
  if (!primary) {
    return {
      healthy: true,
      title: '当前暂无高优先级异常',
      message: `近 ${overview.range.days} 天旅程运行稳定，可继续关注触达趋势和 Top 旅程表现`,
      severity: 'success',
      actionLabel: '查看趋势',
      targetCanvasId: null,
      failedExecutions: formatNumber(overview.summary.failedExecutions),
      successRate: overview.summary.successRate || '0%',
      pendingCount: 0,
    }
  }

  const action = getAttentionAction(primary)
  return {
    healthy: false,
    title: primary.name,
    message: primary.message,
    severity: primary.severity,
    actionLabel: action.label,
    targetCanvasId: primary.canvasId,
    failedExecutions: formatNumber(overview.summary.failedExecutions),
    successRate: overview.summary.successRate || '0%',
    pendingCount: overview.attentionItems.length,
  }
}

export function filterHomeOverview(overview: HomeOverview, keyword: string): HomeOverview {
  const normalized = keyword.trim().toLocaleLowerCase()
  if (!normalized) return overview
  const includesKeyword = (value: string) => value.toLocaleLowerCase().includes(normalized)
  return {
    ...overview,
    topCanvases: overview.topCanvases.filter(item => includesKeyword(item.name)),
    attentionItems: overview.attentionItems.filter(item => includesKeyword(item.name)),
  }
}
```

- [ ] **Step 4: Run helper tests and verify GREEN**

Run:

```bash
cd frontend
npm run test -- src/pages/home/homeOverview.test.ts
```

Expected: PASS for the expanded `homeOverview helpers` suite.

- [ ] **Step 5: Commit helper changes**

Run:

```bash
git add frontend/src/pages/home/homeOverview.ts frontend/src/pages/home/homeOverview.test.ts
git commit -m "feat: add homepage ops presentation helpers"
```

Expected: commit contains only the helper file and its test.

## Task 2: Add Page-Level Rendering Tests

**Files:**
- Create: `frontend/src/pages/home/index.test.tsx`

- [ ] **Step 1: Write failing page tests**

Create `frontend/src/pages/home/index.test.tsx`:

```tsx
/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import HomePage from './index'
import type { HomeOverview } from './homeOverview'

const api = vi.hoisted(() => ({
  overview: vi.fn(),
}))

const navigate = vi.hoisted(() => vi.fn())

vi.mock('../../services/api', () => ({
  homeApi: api,
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

describe('HomePage material ops dashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.overview.mockResolvedValue({ data: overview() })
  })

  it('renders risk summary, KPI cards, anomaly queue, top journeys, and common actions', async () => {
    render(<MemoryRouter><HomePage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: '运营驾驶舱' })).toBeInTheDocument()
    expect(screen.getByText('沉睡用户召回')).toBeInTheDocument()
    expect(screen.getByText('失败率 4.8%')).toBeInTheDocument()
    expect(screen.getByText('异常队列')).toBeInTheDocument()
    expect(screen.getByText('Top 旅程表现')).toBeInTheDocument()
    expect(screen.getByText('常用动作')).toBeInTheDocument()
    expect(screen.getByText('128,430')).toBeInTheDocument()
  })

  it('filters current homepage journeys locally from the search field', async () => {
    render(<MemoryRouter><HomePage /></MemoryRouter>)
    await screen.findByText('新人激活 7 日链路')

    await userEvent.type(screen.getByPlaceholderText('搜索当前首页旅程'), '召回')

    expect(screen.queryByText('新人激活 7 日链路')).not.toBeInTheDocument()
    expect(screen.getAllByText('沉睡用户召回').length).toBeGreaterThan(0)
    expect(api.overview).toHaveBeenCalledTimes(1)
  })

  it('shows healthy summary when there are no attention items', async () => {
    api.overview.mockResolvedValue({ data: { ...overview(), attentionItems: [] } })

    render(<MemoryRouter><HomePage /></MemoryRouter>)

    expect(await screen.findByText('当前暂无高优先级异常')).toBeInTheDocument()
    expect(screen.getByText('近 7 天旅程运行稳定，可继续关注触达趋势和 Top 旅程表现')).toBeInTheDocument()
  })

  it('keeps retry behavior for overview failures', async () => {
    api.overview.mockRejectedValueOnce(new Error('network down'))
    api.overview.mockResolvedValueOnce({ data: overview() })

    render(<MemoryRouter><HomePage /></MemoryRouter>)

    expect(await screen.findByText('首页数据加载失败')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '重试' }))

    await waitFor(() => expect(api.overview).toHaveBeenCalledTimes(2))
    expect(await screen.findByText('沉睡用户召回')).toBeInTheDocument()
  })
})

function overview(): HomeOverview {
  return {
    range: { days: 7, since: '2026-05-29', until: '2026-06-05' },
    summary: {
      publishedCanvasCount: 42,
      totalExecutions: 246880,
      uniqueUsers: 128430,
      successRate: '98.6%',
      failedExecutions: 318,
    },
    trend: [
      { date: '2026-06-01', total: 12000, failed: 12 },
      { date: '2026-06-02', total: 18000, failed: 20 },
    ],
    topCanvases: [
      { canvasId: 1, name: '新人激活 7 日链路', total: 84000, uniqueUsers: 43000, successRate: '99.2%', failed: 28 },
      { canvasId: 2, name: '沉睡用户召回', total: 52000, uniqueUsers: 31000, successRate: '95.2%', failed: 141 },
    ],
    attentionItems: [
      { canvasId: 2, name: '沉睡用户召回', type: 'HIGH_FAILURE_RATE', message: '失败率 4.8%', severity: 'warning' },
      { canvasId: 3, name: '生日礼遇活动', type: 'NO_RECENT_EXECUTIONS', message: '近 7 天无执行记录', severity: 'info' },
    ],
  }
}
```

- [ ] **Step 2: Run page test and verify RED**

Run:

```bash
cd frontend
npm run test -- src/pages/home/index.test.tsx
```

Expected: FAIL because the current page has no search placeholder `搜索当前首页旅程`, no `异常队列` title, and no `Top 旅程表现` title.

- [ ] **Step 3: Commit failing test**

Run:

```bash
git add frontend/src/pages/home/index.test.tsx
git commit -m "test: cover homepage material ops layout"
```

Expected: commit contains only `frontend/src/pages/home/index.test.tsx`.

## Task 3: Implement Material Ops Layout

**Files:**
- Modify: `frontend/src/pages/home/index.tsx`

- [ ] **Step 1: Import the new helpers and search icon**

In `frontend/src/pages/home/index.tsx`, update imports:

```tsx
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Input,
  List,
  Row,
  Segmented,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd'
import {
  ApiOutlined,
  BarChartOutlined,
  CheckCircleOutlined,
  CloudOutlined,
  DatabaseOutlined,
  ExclamationCircleOutlined,
  NotificationOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  TeamOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
```

Replace the `homeOverview` import with:

```tsx
import {
  buildKpiCards,
  buildRiskSummary,
  filterHomeOverview,
  getAttentionAction,
  getAttentionPresentation,
  HOME_RANGE_OPTIONS,
  sortAttentionItems,
  type HomeAttentionItem,
  type HomeOverview,
  type HomeTopCanvas,
  type KpiCard,
  type RiskSummary,
} from './homeOverview'
```

- [ ] **Step 2: Add search and derived overview state**

Inside `HomePage`, add search state and derived models after existing state declarations:

```tsx
const [keyword, setKeyword] = useState('')
const visibleOverview = useMemo(
  () => overview ? filterHomeOverview(overview, keyword) : null,
  [overview, keyword],
)
const riskSummary = useMemo(
  () => overview ? buildRiskSummary(overview) : null,
  [overview],
)
const attentionItems = useMemo(
  () => visibleOverview ? sortAttentionItems(visibleOverview.attentionItems) : [],
  [visibleOverview],
)
const kpiCards = useMemo(() => overview ? buildKpiCards(overview) : [], [overview])
```

Remove the old `kpiCards` declaration so it is not declared twice.

- [ ] **Step 3: Replace the current dashboard JSX with the new structure**

In the `overview ? (` branch, render this section order:

```tsx
<>
  <DashboardHeader
    days={days}
    keyword={keyword}
    loading={loading}
    onDaysChange={setDays}
    onKeywordChange={setKeyword}
    onRefresh={load}
    onCreate={() => navigate('/canvas')}
  />
  {riskSummary && (
    <RiskSummaryBanner
      summary={riskSummary}
      onOpen={() => {
        if (riskSummary.targetCanvasId) navigate(`/canvas/${riskSummary.targetCanvasId}/stats`)
      }}
    />
  )}
  <KpiGrid cards={kpiCards} />
  <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
    <Col xs={24} xl={15}>
      <TrendCard trend={visibleOverview?.trend ?? []} />
    </Col>
    <Col xs={24} xl={9}>
      <AttentionQueue items={attentionItems} onOpen={item => navigate(attentionUrl(item))} />
    </Col>
  </Row>
  <Row gutter={[16, 16]}>
    <Col xs={24} xl={15}>
      <TopJourneyList journeys={visibleOverview?.topCanvases ?? []} onOpen={canvasId => navigate(`/canvas/${canvasId}/stats`)} />
    </Col>
    <Col xs={24} xl={9}>
      <CommonActions onNavigate={navigate} />
    </Col>
  </Row>
</>
```

Keep the existing `Alert` block above this branch and keep the initial `Spin` behavior.

- [ ] **Step 4: Add focused local components below `HomePage`**

Add these component signatures and render responsibilities below `HomePage`:

```tsx
function DashboardHeader(props: {
  days: number
  keyword: string
  loading: boolean
  onDaysChange: (days: number) => void
  onKeywordChange: (keyword: string) => void
  onRefresh: () => void
  onCreate: () => void
}) {
  return (
    <div style={headerStyle}>
      <div>
        <Title level={3} style={pageTitleStyle}>运营驾驶舱</Title>
        <Text type="secondary">近 {props.days} 天：优先关注异常队列，再查看核心指标和旅程表现</Text>
      </div>
      <Space wrap>
        <Input
          allowClear
          prefix={<SearchOutlined />}
          placeholder="搜索当前首页旅程"
          value={props.keyword}
          onChange={event => props.onKeywordChange(event.target.value)}
          style={searchInputStyle}
        />
        <Segmented
          value={props.days}
          options={HOME_RANGE_OPTIONS.map(item => ({ label: item.label, value: item.value }))}
          onChange={value => props.onDaysChange(Number(value))}
        />
        <Button icon={<ReloadOutlined />} loading={props.loading} onClick={props.onRefresh}>刷新</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={props.onCreate}>新建画布</Button>
      </Space>
    </div>
  )
}

function RiskSummaryBanner({ summary, onOpen }: { summary: RiskSummary; onOpen: () => void }) {
  return (
    <div style={{
      ...riskBannerStyle,
      background: summary.healthy ? '#e6f4ea' : '#fce8e6',
      borderColor: summary.healthy ? '#ceead6' : '#fad2cf',
    }}>
      <Space size={12}>
        {summary.healthy ? <CheckCircleOutlined style={riskIconStyle} /> : <ExclamationCircleOutlined style={{ ...riskIconStyle, color: '#d93025' }} />}
        <div>
          <Text strong style={{ color: '#202124' }}>{summary.title}</Text>
          <Text type="secondary" style={{ display: 'block', marginTop: 4 }}>{summary.message}</Text>
        </div>
      </Space>
      <Space size={18} wrap>
        <MiniMetric label="失败执行" value={summary.failedExecutions} danger={!summary.healthy} />
        <MiniMetric label="成功率" value={summary.successRate} />
        <MiniMetric label="待处理" value={String(summary.pendingCount)} />
        <Button onClick={onOpen} disabled={!summary.targetCanvasId && !summary.healthy}>{summary.actionLabel}</Button>
      </Space>
    </div>
  )
}
```

Also add `MiniMetric`, `KpiGrid`, `TrendCard`, `AttentionQueue`, `TopJourneyList`, and `CommonActions` in the same file. Use existing Ant Design components and the existing `attentionUrl(item)` function for navigation.

Use this component code:

```tsx
function MiniMetric({ label, value, danger = false }: { label: string; value: string; danger?: boolean }) {
  return (
    <div style={{ borderLeft: '1px solid rgba(95,99,104,.18)', paddingLeft: 12 }}>
      <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>{label}</Text>
      <Text strong style={{ color: danger ? '#a50e0e' : '#202124', fontSize: 20 }}>{value}</Text>
    </div>
  )
}

function MaterialChip({ text, color }: { text: string; color: 'green' | 'red' | 'blue' | 'orange' }) {
  const palette = {
    green: { background: '#e6f4ea', color: '#137333' },
    red: { background: '#fce8e6', color: '#a50e0e' },
    blue: { background: '#e8f0fe', color: '#174ea6' },
    orange: { background: '#fef7e0', color: '#b06000' },
  }[color]
  return <span style={{ ...chipStyle, ...palette }}>{text}</span>
}

function KpiGrid({ cards }: { cards: KpiCard[] }) {
  return (
    <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
      {cards.map(card => (
        <Col key={card.key} xs={24} sm={12} lg={8} xl={4}>
          <Card bordered={false} style={materialCardStyle} styles={{ body: { padding: 16 } }}>
            <Space align="start" style={{ width: '100%', justifyContent: 'space-between' }}>
              <Text type="secondary">{card.label}</Text>
              <span style={{ ...kpiIconStyle, background: card.iconBg, color: card.color }}>{card.icon}</span>
            </Space>
            <div style={{ marginTop: 14, color: '#202124', fontSize: 26, fontWeight: 500 }}>{card.value}</div>
            <MaterialChip text={card.hint} color={card.key === 'failed' ? 'red' : card.key === 'published' ? 'blue' : 'green'} />
          </Card>
        </Col>
      ))}
    </Row>
  )
}

function TrendCard({ trend }: { trend: HomeOverview['trend'] }) {
  return (
    <Card title="执行健康趋势" bordered={false} style={materialCardStyle}>
      {trend.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无趋势数据" />
      ) : (
        <ResponsiveContainer width="100%" height={280}>
          <AreaChart data={trend} margin={{ top: 10, right: 16, bottom: 0, left: 0 }}>
            <defs>
              <linearGradient id="homeMaterialTotal" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#1a73e8" stopOpacity={0.18} />
                <stop offset="95%" stopColor="#1a73e8" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="homeMaterialFailed" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#d93025" stopOpacity={0.14} />
                <stop offset="95%" stopColor="#d93025" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#edf2f7" />
            <XAxis dataKey="date" tick={{ fontSize: 11, fill: '#5f6368' }} tickFormatter={value => String(value).slice(5)} axisLine={false} tickLine={false} />
            <YAxis tick={{ fontSize: 11, fill: '#5f6368' }} width={42} axisLine={false} tickLine={false} />
            <Tooltip />
            <Legend />
            <Area type="monotone" dataKey="total" name="触发次数" stroke="#1a73e8" strokeWidth={2.4} fill="url(#homeMaterialTotal)" />
            <Area type="monotone" dataKey="failed" name="失败次数" stroke="#d93025" strokeWidth={2.2} fill="url(#homeMaterialFailed)" />
          </AreaChart>
        </ResponsiveContainer>
      )}
    </Card>
  )
}

function AttentionQueue({ items, onOpen }: { items: HomeAttentionItem[]; onOpen: (item: HomeAttentionItem) => void }) {
  return (
    <Card title="异常队列" bordered={false} style={materialCardStyle}
      extra={<Button type="link" size="small" onClick={() => items[0] && onOpen(items[0])} disabled={items.length === 0}>全部处理</Button>}>
      {items.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无需要关注的旅程" />
      ) : (
        <List
          dataSource={items}
          renderItem={item => {
            const presentation = getAttentionPresentation(item.severity)
            const action = getAttentionAction(item)
            return (
              <List.Item actions={[<Button key="open" type="link" onClick={() => onOpen(item)}>{action.label}</Button>]}>
                <List.Item.Meta
                  avatar={<span style={{ ...queueAvatarStyle, background: item.severity === 'error' ? '#fce8e6' : item.severity === 'warning' ? '#fef7e0' : '#e8f0fe' }} />}
                  title={<Space size={8}><span>{item.name}</span><Tag color={presentation.color}>{presentation.label}</Tag></Space>}
                  description={item.message}
                />
              </List.Item>
            )
          }}
        />
      )}
    </Card>
  )
}

function TopJourneyList({ journeys, onOpen }: { journeys: HomeTopCanvas[]; onOpen: (canvasId: number) => void }) {
  return (
    <Card title="Top 旅程表现" bordered={false} style={materialCardStyle}
      extra={<Button type="link" onClick={() => onOpen(journeys[0]?.canvasId ?? 0)} disabled={journeys.length === 0}>查看全部</Button>}>
      {journeys.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前范围暂无触发数据" />
      ) : (
        <List
          dataSource={journeys}
          renderItem={(item, index) => (
            <List.Item actions={[<Button key="stats" type="link" onClick={() => onOpen(item.canvasId)}>统计</Button>]}>
              <List.Item.Meta
                avatar={<span style={rankStyle}>{index + 1}</span>}
                title={item.name}
                description={`触发 ${item.total.toLocaleString()} · 用户 ${item.uniqueUsers.toLocaleString()} · 失败 ${item.failed.toLocaleString()}`}
              />
              <MaterialChip text={item.successRate} color={item.failed > 0 ? 'orange' : 'green'} />
            </List.Item>
          )}
        />
      )}
    </Card>
  )
}

function CommonActions({ onNavigate }: { onNavigate: (path: string) => void }) {
  const actions = [
    { label: '旅程管理', path: '/canvas', icon: <BarChartOutlined />, color: '#e8f0fe' },
    { label: '人群管理', path: '/audiences', icon: <TeamOutlined />, color: '#e6f4ea' },
    { label: '投递监控', path: '/message-deliveries', icon: <NotificationOutlined />, color: '#fce8e6' },
    { label: 'BI 工作台', path: '/bi', icon: <BarChartOutlined />, color: '#fef7e0' },
    { label: '事件配置', path: '/event-config', icon: <ThunderboltOutlined />, color: '#e8f0fe' },
    { label: '数据源', path: '/data-source-config', icon: <DatabaseOutlined />, color: '#edf2fa' },
    { label: 'API 配置', path: '/api-config', icon: <ApiOutlined />, color: '#e8f0fe' },
    { label: 'MQ 配置', path: '/mq-config', icon: <CloudOutlined />, color: '#edf2fa' },
  ]
  return (
    <Card title="常用动作" bordered={false} style={materialCardStyle}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 10 }}>
        {actions.map(action => (
          <Button key={action.path} onClick={() => onNavigate(action.path)} style={commonActionStyle}>
            <Space><span style={{ ...commonActionIconStyle, background: action.color }}>{action.icon}</span>{action.label}</Space>
          </Button>
        ))}
      </div>
    </Card>
  )
}
```

- [ ] **Step 5: Add local style constants**

At the bottom of `index.tsx`, replace `cardStyle` and add these constants:

```tsx
const pageStyle: CSSProperties = {
  minHeight: '100vh',
  background: '#f8fafd',
  padding: 0,
}

const headerStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'flex-start',
  justifyContent: 'space-between',
  gap: 16,
  marginBottom: 16,
  flexWrap: 'wrap',
}

const pageTitleStyle: CSSProperties = {
  margin: 0,
  color: '#202124',
  fontWeight: 500,
  letterSpacing: 0,
}

const searchInputStyle: CSSProperties = {
  width: 240,
  borderRadius: 999,
  background: '#edf2fa',
}

const materialCardStyle: CSSProperties = {
  height: '100%',
  borderRadius: 8,
  border: '1px solid #e1e7ef',
  boxShadow: '0 1px 2px rgba(60,64,67,.12)',
}

const riskBannerStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 16,
  marginBottom: 16,
  padding: '14px 16px',
  border: '1px solid #fad2cf',
  borderRadius: 8,
  flexWrap: 'wrap',
}

const riskIconStyle: CSSProperties = {
  fontSize: 24,
  color: '#188038',
}

const chipStyle: CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  height: 22,
  borderRadius: 999,
  padding: '0 9px',
  fontSize: 11,
  fontWeight: 700,
  whiteSpace: 'nowrap',
}

const kpiIconStyle: CSSProperties = {
  width: 28,
  height: 28,
  borderRadius: '50%',
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
}

const queueAvatarStyle: CSSProperties = {
  display: 'inline-block',
  width: 32,
  height: 32,
  borderRadius: '50%',
}

const rankStyle: CSSProperties = {
  display: 'inline-grid',
  placeItems: 'center',
  width: 28,
  height: 28,
  borderRadius: '50%',
  background: '#e8f0fe',
  color: '#174ea6',
  fontWeight: 700,
}

const commonActionStyle: CSSProperties = {
  height: 48,
  borderRadius: 8,
  justifyContent: 'flex-start',
}

const commonActionIconStyle: CSSProperties = {
  width: 24,
  height: 24,
  borderRadius: '50%',
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
}
```

Set the page root to `style={pageStyle}` and use `materialCardStyle` for cards.

- [ ] **Step 6: Run page and helper tests**

Run:

```bash
cd frontend
npm run test -- src/pages/home/homeOverview.test.ts src/pages/home/index.test.tsx
```

Expected: both test files PASS.

- [ ] **Step 7: Commit layout implementation**

Run:

```bash
git add frontend/src/pages/home/index.tsx
git commit -m "feat: redesign homepage ops dashboard"
```

Expected: commit contains only `frontend/src/pages/home/index.tsx`.

## Task 4: Final Verification

**Files:**
- Modify only if verification exposes a concrete defect in `frontend/src/pages/home/index.tsx` or `frontend/src/pages/home/homeOverview.ts`.

- [ ] **Step 1: Run focused frontend tests**

Run:

```bash
cd frontend
npm run test -- src/pages/home/homeOverview.test.ts src/pages/home/index.test.tsx
```

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run:

```bash
cd frontend
npm run build
```

Expected: TypeScript and Vite build PASS.

- [ ] **Step 3: Start or reuse Vite dev server**

Run:

```bash
cd frontend
npm run dev -- --host 127.0.0.1
```

Expected: Vite starts on `http://127.0.0.1:3000/` or reports the next available port.

- [ ] **Step 4: Browser-check the homepage**

Open `/home` in the browser. If authentication redirects to `/login`, verify the compiled page visually through an authenticated local session when credentials are available; otherwise rely on the page test and build results.

Expected visual checks:
- Risk summary appears above KPI cards.
- KPI cards use white surfaces instead of large pastel backgrounds.
- Trend card and anomaly queue are side by side on desktop width.
- Top journeys and common actions appear below.
- Search field filters visible journey names without calling the API again.

- [ ] **Step 5: Commit any verification fixes**

If Step 4 required code changes, run:

```bash
git add frontend/src/pages/home/index.tsx frontend/src/pages/home/homeOverview.ts frontend/src/pages/home/homeOverview.test.ts frontend/src/pages/home/index.test.tsx
git commit -m "fix: polish homepage material ops dashboard"
```

Expected: commit contains only files changed to fix concrete verification findings.

## Self-Review

Spec coverage:
- Risk-first information architecture is covered by Tasks 1, 2, and 3.
- Local homepage search is covered by Tasks 1 and 2.
- Material-light visual language is covered by Task 3 style constants and components.
- Empty/error states are covered by Tasks 1, 2, and 3.
- No backend API changes are included.

Placeholder scan:
- This plan contains no deferred implementation placeholders.

Type consistency:
- `RiskSummary`, `AttentionAction`, `sortAttentionItems`, `filterHomeOverview`, and `getAttentionAction` are introduced in Task 1 before `index.tsx` consumes them in Task 3.
