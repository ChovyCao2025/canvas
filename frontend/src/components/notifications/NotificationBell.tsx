/**
 * 组件职责：通知铃铛组件，展示未读数、通知列表、过滤、全部已读和归档操作。
 *
 * 维护说明：组件消费 NotificationContext，不直接维护 WebSocket 或轮询。
 */
import { useEffect, type KeyboardEvent, type ReactNode, useState } from 'react'
import {
  Badge,
  Button,
  Drawer,
  Empty,
  List,
  Segmented,
  Space,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import { BellOutlined, InboxOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useNotifications } from '../../context/NotificationContext'
import { notificationApi, type UserNotification } from '../../services/notificationApi'
import {
  getNotificationActionLabel,
  getNotificationCategoryLabel,
  getNotificationStatusColor,
  shouldShowUnreadBadge,
} from './notificationPresentation'

/** 通知铃铛列表中的文本组件别名。 */
const { Text } = Typography

/** 抽屉内分类筛选项；value 与后端通知 category 保持一致。 */
const FILTER_OPTIONS = [
  { label: '全部', value: 'ALL' },
  { label: '任务', value: 'TASK' },
  { label: '审批', value: 'APPROVAL' },
  { label: '告警', value: 'ALERT' },
  { label: '变更', value: 'CHANGE' },
] as const

/** 通知抽屉顶部工具按钮的固定尺寸。 */
const TOOL_BUTTON_SIZE = 40
/** 自定义工具图标的固定尺寸。 */
const TOOL_ICON_SIZE = 19

/** 通知分类筛选项的 value 类型，来源于 FILTER_OPTIONS。 */
type FilterValue = typeof FILTER_OPTIONS[number]['value']

/** 时钟图标：表示查看最新通知。 */
function ClockIcon() {
  return (
    <svg viewBox="0 0 20 20" width={TOOL_ICON_SIZE} height={TOOL_ICON_SIZE} aria-hidden="true">
      <circle cx="10" cy="10" r="7" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path
        d="M10 6.6v3.8l2.7 1.8"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

/** 归档箱图标：表示查看已归档通知。 */
function ArchiveIcon() {
  return (
    <svg viewBox="0 0 20 20" width={TOOL_ICON_SIZE} height={TOOL_ICON_SIZE} aria-hidden="true">
      <path
        d="M4.3 5.5h11.4l-1 9.2a1 1 0 0 1-1 .8H6.3a1 1 0 0 1-1-.8l-1-9.2Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <path d="M3.8 5.5h12.4" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      <path d="M8 9.5h4" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

/** 已读图标：表示全部标记为已读。 */
function MailOpenIcon() {
  return (
    <svg viewBox="0 0 20 20" width={TOOL_ICON_SIZE} height={TOOL_ICON_SIZE} aria-hidden="true">
      <path
        d="M3.5 8.4 10 4.8l6.5 3.6"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M4.4 8.2h11.2a1 1 0 0 1 1 1v6.1a1 1 0 0 1-1 1H4.4a1 1 0 0 1-1-1V9.2a1 1 0 0 1 1-1Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <path
        d="m4.2 9 5.1 3.8a1.2 1.2 0 0 0 1.4 0L15.8 9"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

/** 工具栏图标按钮，统一尺寸、激活态和 tooltip。 */
function ToolButton({
  active,
  disabled,
  label,
  icon,
  onClick,
}: {
  active?: boolean
  disabled?: boolean
  label: string
  icon: ReactNode
  onClick: () => void
}) {
  return (
    <Tooltip title={label}>
      <Button
        type="text"
        size="small"
        aria-label={label}
        icon={icon}
        disabled={disabled}
        onClick={onClick}
        style={{
          width: TOOL_BUTTON_SIZE,
          height: TOOL_BUTTON_SIZE,
          borderRadius: 12,
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: active ? '#ffffff' : 'transparent',
          color: active ? '#111827' : '#6b7280',
          boxShadow: active ? '0 2px 6px rgba(15, 23, 42, 0.08)' : 'none',
        }}
      />
    </Tooltip>
  )
}

/** 通知铃铛入口和右侧消息抽屉。 */
export default function NotificationBell() {
  const navigate = useNavigate()
  const { items, unreadCount, connected, refresh, markRead, markAllRead, archive } = useNotifications()
  const [open, setOpen] = useState(false)
  const [filter, setFilter] = useState<FilterValue>('ALL')
  const [showArchived, setShowArchived] = useState(false)
  const [archivedItems, setArchivedItems] = useState<UserNotification[]>([])
  const [loadingArchived, setLoadingArchived] = useState(false)

  // 归档列表只在用户切到归档视图并打开抽屉时加载，避免主界面常驻请求。
  useEffect(() => {
    if (!open || !showArchived) {
      return
    }
    setLoadingArchived(true)
    notificationApi.list({ archived: true, page: 1, size: 20 })
      .then(res => setArchivedItems(res.data))
      .catch(() => message.error('归档通知加载失败'))
      .finally(() => setLoadingArchived(false))
  }, [open, showArchived])

  /** 打开抽屉时主动刷新一次，弥补 WebSocket/轮询间隔带来的短暂延迟。 */
  async function handleOpen() {
    setOpen(true)
    try {
      await refresh()
    } catch {
      message.error('通知加载失败')
    }
  }

  /** 点击通知：未读先标记为已读，再跳到 actionUrl/targetUrl。 */
  async function handleClick(item: UserNotification) {
    try {
      if (!item.readAt && !showArchived) {
        await markRead(item.notificationId)
      }
      setOpen(false)
      navigate(item.actionUrl || item.targetUrl || '/home')
    } catch {
      message.error('通知状态更新失败')
    }
  }

  function handleNotificationKey(event: KeyboardEvent<HTMLElement>, item: UserNotification) {
    if (event.key !== 'Enter' && event.key !== ' ') {
      return
    }
    event.preventDefault()
    handleClick(item).catch(() => undefined)
  }

  /** 批量已读只作用于当前用户的未归档通知。 */
  async function handleMarkAllRead() {
    try {
      await markAllRead()
    } catch {
      message.error('通知状态更新失败')
    }
  }

  /** 归档单条通知，归档后从主列表移除。 */
  async function handleArchive(item: UserNotification) {
    try {
      await archive(item.notificationId)
    } catch {
      message.error('通知归档失败')
    }
  }

  // 主列表使用实时上下文，归档列表单独维护；两者再套同一分类筛选。
  const activeItems = items.filter(item => filter === 'ALL' || item.category === filter)
  const displayedItems = showArchived
    ? archivedItems.filter(item => filter === 'ALL' || item.category === filter)
    : activeItems

  return (
    <>
      <Tooltip title={connected ? '通知实时同步中' : '通知轮询同步中'} placement="right">
        <Badge count={shouldShowUnreadBadge(unreadCount) ? unreadCount : 0} size="small">
          <Button
            type="text"
            aria-label="打开消息中心"
            icon={<BellOutlined />}
            onClick={handleOpen}
            style={{ color: connected ? '#9ad17b' : 'rgba(255,255,255,.72)' }}
          />
        </Badge>
      </Tooltip>
      <Drawer
        title="消息中心"
        width={420}
        placement="right"
        open={open}
        onClose={() => setOpen(false)}
        extra={(
          <Space size={8}>
            <div
              style={{
                background: '#f3f4f7',
                padding: 4,
                borderRadius: 14,
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4,
              }}
            >
              <ToolButton
                label="最新"
                icon={<ClockIcon />}
                active={!showArchived}
                onClick={() => setShowArchived(false)}
              />
              <ToolButton
                label="归档"
                icon={<ArchiveIcon />}
                active={showArchived}
                onClick={() => setShowArchived(true)}
              />
            </div>
            <ToolButton
              label={showArchived ? '归档视图无需全部已读' : '全部已读'}
              icon={<MailOpenIcon />}
              disabled={showArchived}
              onClick={handleMarkAllRead}
            />
          </Space>
        )}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Segmented
            block
            aria-label="通知分类筛选"
            value={filter}
            options={[...FILTER_OPTIONS]}
            onChange={value => setFilter(value as FilterValue)}
          />
          <List
            loading={loadingArchived}
            dataSource={displayedItems}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无消息" /> }}
            renderItem={item => (
              <List.Item
                key={item.notificationId}
                actions={showArchived ? [] : [
                  <Button
                    key="archive"
                    type="text"
                    size="small"
                    aria-label={`归档通知 ${item.title}`}
                    icon={<InboxOutlined />}
                    onClick={(event) => {
                      event.stopPropagation()
                      handleArchive(item).catch(() => undefined)
                    }}
                  >
                    归档
                  </Button>,
                ]}
                style={{
                  cursor: 'pointer',
                  paddingInline: 8,
                  borderRadius: 8,
                  background: item.readAt || showArchived ? '#fff' : '#f4f8ff',
                }}
                role="button"
                tabIndex={0}
                aria-label={`查看通知 ${item.title}`}
                onClick={() => handleClick(item)}
                onKeyDown={(event) => handleNotificationKey(event, item)}
              >
                <List.Item.Meta
                  title={(
                    <Space size={8} wrap>
                      <span>{item.title}</span>
                      <Tag color={getNotificationStatusColor(item.type, item.severity)}>
                        {getNotificationCategoryLabel(item.category)}
                      </Tag>
                      {!item.readAt && !showArchived && <Tag color="blue">未读</Tag>}
                    </Space>
                  )}
                  description={(
                    <Space direction="vertical" size={4}>
                      <Text type="secondary">{item.content}</Text>
                      <Space size={8} wrap>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {item.createdAt?.replace('T', ' ').slice(0, 19)}
                        </Text>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {getNotificationActionLabel(item)}
                        </Text>
                      </Space>
                    </Space>
                  )}
                />
              </List.Item>
            )}
          />
        </Space>
      </Drawer>
    </>
  )
}
