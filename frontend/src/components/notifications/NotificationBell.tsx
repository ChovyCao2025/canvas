import { useEffect, type ReactNode, useState } from 'react'
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

const { Text } = Typography

const FILTER_OPTIONS = [
  { label: '全部', value: 'ALL' },
  { label: '任务', value: 'TASK' },
  { label: '审批', value: 'APPROVAL' },
  { label: '告警', value: 'ALERT' },
  { label: '变更', value: 'CHANGE' },
] as const

const TOOL_BUTTON_SIZE = 40
const TOOL_ICON_SIZE = 19

type FilterValue = typeof FILTER_OPTIONS[number]['value']

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

export default function NotificationBell() {
  const navigate = useNavigate()
  const { items, unreadCount, connected, refresh, markRead, markAllRead, archive } = useNotifications()
  const [open, setOpen] = useState(false)
  const [filter, setFilter] = useState<FilterValue>('ALL')
  const [showArchived, setShowArchived] = useState(false)
  const [archivedItems, setArchivedItems] = useState<UserNotification[]>([])
  const [loadingArchived, setLoadingArchived] = useState(false)

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

  async function handleOpen() {
    setOpen(true)
    try {
      await refresh()
    } catch {
      message.error('通知加载失败')
    }
  }

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

  async function handleMarkAllRead() {
    try {
      await markAllRead()
    } catch {
      message.error('通知状态更新失败')
    }
  }

  async function handleArchive(item: UserNotification) {
    try {
      await archive(item.notificationId)
    } catch {
      message.error('通知归档失败')
    }
  }

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
            aria-label="通知"
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
                onClick={() => handleClick(item)}
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
