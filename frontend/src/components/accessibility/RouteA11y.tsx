import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import LiveRegion from './LiveRegion'

export function buildRouteAnnouncement(pathname: string): string {
  if (pathname.startsWith('/admin/users')) return '已进入用户管理'
  if (pathname.startsWith('/admin/tenants')) return '已进入租户管理'
  if (pathname === '/' || pathname === '/home') return '已进入首页'
  if (pathname.startsWith('/mautic-insights')) return '已进入营销解释台'
  if (pathname.startsWith('/marketing-preferences')) return '已进入偏好中心'
  if (pathname.startsWith('/marketing-forms')) return '已进入表单中心'
  if (pathname.startsWith('/public/forms')) return '已进入公开表单'
  if (pathname.startsWith('/canvas')) return '已进入旅程管理'
  if (pathname.startsWith('/cdp/users')) return '已进入用户中心'
  if (pathname.startsWith('/audiences')) return '已进入人群管理'
  return '页面已更新'
}

function pageTitle(message: string): string {
  return `${message.replace(/^已进入/, '').replace(/^页面已更新$/, '页面')} - 营销画布`
}

export default function RouteA11y() {
  const location = useLocation()
  const [message, setMessage] = useState('')

  useEffect(() => {
    const nextMessage = buildRouteAnnouncement(location.pathname)
    document.title = pageTitle(nextMessage)
    setMessage(nextMessage)
    const focusMain = () => document.getElementById('main-content')?.focus()
    if (typeof window.requestAnimationFrame === 'function') {
      window.requestAnimationFrame(focusMain)
    } else {
      focusMain()
    }
  }, [location.pathname])

  return <LiveRegion message={message} />
}
