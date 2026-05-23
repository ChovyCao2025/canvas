import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { useAuth } from './AuthContext'
import {
  notificationApi,
  type NotificationRealtimePayload,
  type UserNotification,
} from '../services/notificationApi'
import {
  buildNotificationWebSocketUrl,
  mergeRealtimeNotifications,
  nextNotificationReconnectDelay,
} from '../components/notifications/notificationRealtime'

interface NotificationState {
  items: UserNotification[]
  unreadCount: number
  connected: boolean
  refresh: () => Promise<void>
  markRead: (notificationId: string) => Promise<void>
  markAllRead: () => Promise<void>
  archive: (notificationId: string) => Promise<void>
}

const NotificationContext = createContext<NotificationState>({
  items: [],
  unreadCount: 0,
  connected: false,
  refresh: async () => {},
  markRead: async () => {},
  markAllRead: async () => {},
  archive: async () => {},
})

const FALLBACK_POLL_MS = 30000

export function NotificationProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [items, setItems] = useState<UserNotification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [connected, setConnected] = useState(false)
  const itemsRef = useRef<UserNotification[]>([])
  const wsRef = useRef<WebSocket | null>(null)
  const reconnectAttemptRef = useRef(0)
  const reconnectTimerRef = useRef<number | null>(null)
  const fallbackTimerRef = useRef<number | null>(null)
  const activeUserRef = useRef<string | null>(null)
  const stoppedRef = useRef(false)

  async function refresh() {
    const [listRes, countRes] = await Promise.all([
      notificationApi.list({ page: 1, size: 20 }),
      notificationApi.unreadCount(),
    ])
    setItems(listRes.data)
    itemsRef.current = listRes.data
    setUnreadCount(countRes.data.count)
  }

  async function markRead(notificationId: string) {
    const target = itemsRef.current.find(item => item.notificationId === notificationId)
    await notificationApi.markRead(notificationId)
    setItems(current => {
      const next = current.map(item => item.notificationId === notificationId
        ? { ...item, status: 'READ', readAt: item.readAt || new Date().toISOString() }
        : item)
      itemsRef.current = next
      return next
    })
    if (target && !target.readAt) {
      setUnreadCount(current => Math.max(0, current - 1))
    }
  }

  async function markAllRead() {
    await notificationApi.markAllRead()
    setItems(current => {
      const next = current.map(item => item.readAt ? item : {
        ...item,
        status: 'READ',
        readAt: new Date().toISOString(),
      })
      itemsRef.current = next
      return next
    })
    setUnreadCount(0)
  }

  async function archive(notificationId: string) {
    const target = itemsRef.current.find(item => item.notificationId === notificationId)
    await notificationApi.archive(notificationId)
    setItems(current => {
      const next = current.filter(item => item.notificationId !== notificationId)
      itemsRef.current = next
      return next
    })
    if (target && !target.readAt) {
      setUnreadCount(current => Math.max(0, current - 1))
    }
  }

  function clearReconnectTimer() {
    if (reconnectTimerRef.current != null) {
      window.clearTimeout(reconnectTimerRef.current)
      reconnectTimerRef.current = null
    }
  }

  function clearFallbackTimer() {
    if (fallbackTimerRef.current != null) {
      window.clearInterval(fallbackTimerRef.current)
      fallbackTimerRef.current = null
    }
  }

  function closeSocket() {
    if (wsRef.current) {
      wsRef.current.close()
      wsRef.current = null
    }
  }

  function scheduleFallbackPolling() {
    clearFallbackTimer()
    fallbackTimerRef.current = window.setInterval(() => {
      refresh().catch(() => undefined)
    }, FALLBACK_POLL_MS)
  }

  function scheduleReconnect() {
    if (stoppedRef.current || !activeUserRef.current) {
      return
    }
    clearReconnectTimer()
    const delay = nextNotificationReconnectDelay(reconnectAttemptRef.current)
    reconnectTimerRef.current = window.setTimeout(() => {
      connectRealtime().catch(() => undefined)
    }, delay)
    reconnectAttemptRef.current += 1
  }

  function handleRealtimePayload(payload: NotificationRealtimePayload) {
    setItems(current => {
      const next = mergeRealtimeNotifications(current, payload)
      itemsRef.current = next
      return next
    })
    if (typeof payload.unreadCount === 'number') {
      setUnreadCount(payload.unreadCount)
    }
  }

  async function connectRealtime() {
    if (!activeUserRef.current || stoppedRef.current) {
      return
    }
    try {
      const ticketRes = await notificationApi.createWsTicket()
      const socket = new WebSocket(buildNotificationWebSocketUrl(ticketRes.data.ticket, window.location))
      wsRef.current = socket
      socket.onopen = () => {
        reconnectAttemptRef.current = 0
        setConnected(true)
        clearFallbackTimer()
      }
      socket.onmessage = (event) => {
        try {
          const payload = JSON.parse(event.data) as NotificationRealtimePayload
          handleRealtimePayload(payload)
        } catch {
          // ignore malformed payload
        }
      }
      socket.onerror = () => {
        setConnected(false)
        scheduleFallbackPolling()
      }
      socket.onclose = () => {
        setConnected(false)
        wsRef.current = null
        scheduleFallbackPolling()
        scheduleReconnect()
      }
    } catch {
      setConnected(false)
      scheduleFallbackPolling()
      scheduleReconnect()
    }
  }

  useEffect(() => {
    activeUserRef.current = user?.username ?? null
    stoppedRef.current = !user
    clearReconnectTimer()
    clearFallbackTimer()
    closeSocket()

    if (!user) {
      setItems([])
      setUnreadCount(0)
      setConnected(false)
      return
    }

    refresh().catch(() => undefined)
    connectRealtime().catch(() => undefined)

    return () => {
      stoppedRef.current = true
      clearReconnectTimer()
      clearFallbackTimer()
      closeSocket()
    }
  }, [user])

  return (
    <NotificationContext.Provider value={{
      items,
      unreadCount,
      connected,
      refresh,
      markRead,
      markAllRead,
      archive,
    }}
    >
      {children}
    </NotificationContext.Provider>
  )
}

export function useNotifications() {
  return useContext(NotificationContext)
}
