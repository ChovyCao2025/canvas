/**
 * 上下文职责：通知中心上下文，封装通知列表、未读数、轮询和 WebSocket 实时同步。
 *
 * 维护说明：页面组件只消费统一状态和动作，避免各处重复维护通知刷新逻辑。
 */
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

/** 通知上下文对外暴露的状态和动作集合。 */
interface NotificationState {
  items: UserNotification[]
  unreadCount: number
  connected: boolean
  refresh: () => Promise<void>
  markRead: (notificationId: string) => Promise<void>
  markAllRead: () => Promise<void>
  archive: (notificationId: string) => Promise<void>
}

/** 通知上下文默认值；真实实现由 NotificationProvider 注入。 */
const NotificationContext = createContext<NotificationState>({
  items: [],
  unreadCount: 0,
  connected: false,
  refresh: async () => {},
  markRead: async () => {},
  markAllRead: async () => {},
  archive: async () => {},
})

/** WebSocket 不可用时的兜底轮询间隔。 */
const FALLBACK_POLL_MS = 30000

/** WebSocket 连续重试上限，超过后停留在 HTTP 轮询模式。 */
export const MAX_RECONNECT_ATTEMPTS = 5

interface ReconnectPlan {
  mode: 'reconnect' | 'polling'
  nextAttempt: number
  delayMs?: number
}

/** 判断某个 WebSocket 生命周期事件是否仍属于当前活动连接。 */
export function isActiveNotificationSocket(activeSocketId: string | null, socketId: string) {
  return activeSocketId === socketId
}

/** 计算下一步重连动作；达到上限后保持轮询，不继续创建新 WebSocket。 */
export function nextNotificationReconnectPlan(attempt: number): ReconnectPlan {
  if (attempt >= MAX_RECONNECT_ATTEMPTS) {
    return { mode: 'polling', nextAttempt: attempt }
  }
  return {
    mode: 'reconnect',
    nextAttempt: attempt + 1,
    delayMs: nextNotificationReconnectDelay(attempt),
  }
}

/** 创建客户端本地 socket 标识，用于隔离旧连接的延迟事件。 */
function createSocketId() {
  return globalThis.crypto?.randomUUID?.() ?? `socket-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

/** 通知状态 Provider，负责在 HTTP 轮询和 WebSocket 实时通道之间切换。 */
export function NotificationProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [items, setItems] = useState<UserNotification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [connected, setConnected] = useState(false)
  const itemsRef = useRef<UserNotification[]>([])
  const wsRef = useRef<WebSocket | null>(null)
  const activeSocketIdRef = useRef<string | null>(null)
  const reconnectAttemptRef = useRef(0)
  const reconnectTimerRef = useRef<number | null>(null)
  const fallbackTimerRef = useRef<number | null>(null)
  const activeUserRef = useRef<string | null>(null)
  const stoppedRef = useRef(false)

  /** 同时刷新通知列表和未读数，作为 WebSocket 不可用时的兜底同步入口。 */
  async function refresh() {
    const [listRes, countRes] = await Promise.all([
      notificationApi.list({ page: 1, size: 20 }),
      notificationApi.unreadCount(),
    ])
    setItems(listRes.data)
    itemsRef.current = listRes.data
    setUnreadCount(countRes.data.count)
  }

  /** 乐观标记单条通知已读；后端成功后本地列表和未读数同步更新。 */
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

  /** 将当前用户所有未读通知置为已读。 */
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

  /** 归档通知并从当前列表移除；未读归档同时递减未读数。 */
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

  /** 清理待执行的 WebSocket 重连计时器。 */
  function clearReconnectTimer() {
    if (reconnectTimerRef.current != null) {
      window.clearTimeout(reconnectTimerRef.current)
      reconnectTimerRef.current = null
    }
  }

  /** 清理 HTTP 轮询兜底计时器。 */
  function clearFallbackTimer() {
    if (fallbackTimerRef.current != null) {
      window.clearInterval(fallbackTimerRef.current)
      fallbackTimerRef.current = null
    }
  }

  /** 主动关闭当前 WebSocket，切换用户或卸载时调用。 */
  function closeSocket() {
    const socket = wsRef.current
    if (socket) {
      wsRef.current = null
      activeSocketIdRef.current = null
      socket.close()
    }
  }

  /** 开启兜底轮询；WebSocket 断开或创建失败时使用。 */
  function scheduleFallbackPolling() {
    clearFallbackTimer()
    fallbackTimerRef.current = window.setInterval(() => {
      refresh().catch(() => undefined)
    }, FALLBACK_POLL_MS)
  }

  /** 按指数退避安排下一次 WebSocket 重连。 */
  function scheduleReconnect() {
    if (stoppedRef.current || !activeUserRef.current) {
      return
    }
    clearReconnectTimer()
    const plan = nextNotificationReconnectPlan(reconnectAttemptRef.current)
    if (plan.mode === 'polling') {
      scheduleFallbackPolling()
      return
    }
    reconnectTimerRef.current = window.setTimeout(() => {
      connectRealtime().catch(() => undefined)
    }, plan.delayMs)
    reconnectAttemptRef.current = plan.nextAttempt
  }

  /** 处理服务端实时推送，合并通知列表并覆盖未读数。 */
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

  /** 建立实时通知 WebSocket；失败时自动切到轮询并安排重连。 */
  async function connectRealtime() {
    if (!activeUserRef.current || stoppedRef.current) {
      return
    }
    try {
      const ticketRes = await notificationApi.createWsTicket()
      const socketId = createSocketId()
      const socket = new WebSocket(buildNotificationWebSocketUrl(ticketRes.data.ticket, window.location))
      activeSocketIdRef.current = socketId
      wsRef.current = socket
      socket.onopen = () => {
        if (!isActiveNotificationSocket(activeSocketIdRef.current, socketId)) return
        reconnectAttemptRef.current = 0
        setConnected(true)
        clearFallbackTimer()
      }
      socket.onmessage = (event) => {
        if (!isActiveNotificationSocket(activeSocketIdRef.current, socketId)) return
        try {
          const payload = JSON.parse(event.data) as NotificationRealtimePayload
          handleRealtimePayload(payload)
        } catch {
          // ignore malformed payload
        }
      }
      socket.onerror = () => {
        if (!isActiveNotificationSocket(activeSocketIdRef.current, socketId)) return
        setConnected(false)
        scheduleFallbackPolling()
      }
      socket.onclose = () => {
        if (!isActiveNotificationSocket(activeSocketIdRef.current, socketId)) return
        setConnected(false)
        wsRef.current = null
        activeSocketIdRef.current = null
        scheduleFallbackPolling()
        scheduleReconnect()
      }
    } catch {
      setConnected(false)
      scheduleFallbackPolling()
      scheduleReconnect()
    }
  }

  // 登录用户变化时重建连接；退出登录时清空通知并停止所有后台同步。
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

/** 读取通知上下文，页面和通知铃铛都通过这个 Hook 访问统一状态。 */
export function useNotifications() {
  return useContext(NotificationContext)
}
