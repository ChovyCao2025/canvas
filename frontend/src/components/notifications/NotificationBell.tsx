import { useCallback, useEffect, useState } from 'react'
import { Badge, Button, Drawer, Empty, List, message, Space, Tag, Tooltip, Typography } from 'antd'
import { BellOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { notificationApi, type UserNotification } from '../../services/notificationApi'
import { getNotificationStatusColor, shouldShowUnreadBadge } from './notificationPresentation'

const { Text } = Typography

export default function NotificationBell() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)
  const [items, setItems] = useState<UserNotification[]>([])
  const [loading, setLoading] = useState(false)

  const fetchUnreadCount = useCallback(async () => {
    const res = await notificationApi.unreadCount()
    setUnreadCount(res.data.count)
  }, [])

  const fetchNotifications = useCallback(async () => {
    setLoading(true)
    try {
      const res = await notificationApi.list({ page: 1, size: 20 })
      setItems(res.data)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchUnreadCount().catch(() => undefined)
    const timer = window.setInterval(() => {
      fetchUnreadCount().catch(() => undefined)
    }, 30000)
    return () => window.clearInterval(timer)
  }, [fetchUnreadCount])

  const handleOpen = async () => {
    setOpen(true)
    try {
      await fetchNotifications()
    } catch {
      message.error('通知加载失败')
    }
  }

  const handleClick = async (item: UserNotification) => {
    try {
      if (!item.readAt) {
        await notificationApi.markRead(item.notificationId)
        await fetchUnreadCount()
      }
      setOpen(false)
      if (item.targetUrl) {
        navigate(item.targetUrl)
      }
    } catch {
      message.error('通知状态更新失败')
    }
  }

  const handleReadAll = async () => {
    try {
      await notificationApi.markAllRead()
      await fetchUnreadCount()
      await fetchNotifications()
    } catch {
      message.error('通知状态更新失败')
    }
  }

  return (
    <>
      <Tooltip title="通知" placement="right">
        <Badge count={shouldShowUnreadBadge(unreadCount) ? unreadCount : 0} size="small">
          <Button
            type="text"
            aria-label="通知"
            icon={<BellOutlined />}
            onClick={handleOpen}
            style={{ color: 'rgba(255,255,255,.72)' }}
          />
        </Badge>
      </Tooltip>
      <Drawer
        title="通知"
        width={360}
        placement="right"
        open={open}
        onClose={() => setOpen(false)}
        extra={<Button size="small" onClick={handleReadAll}>全部已读</Button>}
      >
        <List
          loading={loading}
          dataSource={items}
          locale={{ emptyText: <Empty description="暂无通知" /> }}
          renderItem={item => (
            <List.Item style={{ cursor: 'pointer' }} onClick={() => handleClick(item)}>
              <List.Item.Meta
                title={
                  <Space size={8} wrap>
                    <span>{item.title}</span>
                    <Tag color={getNotificationStatusColor(item.type)}>{item.readAt ? '已读' : '未读'}</Tag>
                  </Space>
                }
                description={
                  <Space direction="vertical" size={2}>
                    <Text type="secondary">{item.content}</Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {item.createdAt?.replace('T', ' ').slice(0, 19)}
                    </Text>
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Drawer>
    </>
  )
}
