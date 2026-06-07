/**
 * 组件职责：登录后的主应用布局，包含侧边导航、折叠菜单、用户信息和通知入口。
 *
 * 维护说明：菜单 key 与路由路径强绑定，新增页面时需要同步这里的导航配置。
 */
import { useEffect, useState, type CSSProperties } from 'react'
import { Layout, Menu, Avatar, Dropdown, Tooltip, Button, type MenuProps } from 'antd'
import {
  ApartmentOutlined, SettingOutlined, ApiOutlined,
  ExperimentOutlined, TeamOutlined, LogoutOutlined,
  UserOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
  RocketOutlined, HomeOutlined, TagsOutlined, NotificationOutlined, ThunderboltOutlined,
  BookOutlined,
  BarChartOutlined,
  DatabaseOutlined,
  IdcardOutlined,
  BankOutlined,
  EyeOutlined,
  SendOutlined,
  DashboardOutlined,
  SafetyCertificateOutlined,
  FormOutlined,
  FolderOpenOutlined,
  MessageOutlined,
  AlertOutlined,
  SearchOutlined,
  GiftOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { authApi } from '../../services/api'
import NotificationBell from '../notifications/NotificationBell'
import { roleLabel } from '../../auth/roles'

/** 主布局的侧栏和内容区组件别名。 */
const { Sider, Content } = Layout

/**
 * 侧边栏 children 需要强制变成纵向 flex：
 * 这样菜单区可滚动、底部用户区可固定。
 */
// Ant Design Sider 内部包了一层 .ant-layout-sider-children，需要让它也是 flex column
const siderChildrenStyle = `
  .app-sider > .ant-layout-sider-children {
    display: flex !important;
    flex-direction: column !important;
    height: 100% !important;
    overflow: hidden !important;
  }
`

/** 侧栏主背景色。 */
const SIDER_DARK  = '#0d1117'
/** 侧栏菜单悬浮背景色。 */
const SIDER_HOVER = '#1f2d45'
/** 侧栏高亮强调色。 */
const ACCENT      = '#4f8ef7'

/**
 * 主框架布局：
 * - 左侧固定导航栏
 * - 右侧内容区根据 collapsed 自适应左边距
 */
export default function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, isAdmin, canManageTenants, logout } = useAuth()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const isMobile = useMediaQuery('(max-width: 767px)')
  const effectiveCollapsed = isMobile ? false : collapsed

  useEffect(() => {
    if (!isMobile) setMobileOpen(false)
  }, [isMobile])

  // 根据当前 URL 计算菜单高亮 key
  const selectedKey = (() => {
    if (location.pathname === '/' || location.pathname === '/home') return 'home'
    if (location.pathname.startsWith('/api-config'))     return 'api-config'
    if (location.pathname.startsWith('/data-source-config')) return 'data-source-config'
    if (location.pathname.startsWith('/ab-experiments')) return 'ab-experiments'
    if (location.pathname.startsWith('/tag-config'))     return 'tag-config'
    if (location.pathname.startsWith('/cdp/computed-profile')) return 'cdp-computed-profile'
    if (location.pathname.startsWith('/cdp/computed-tags')) return 'cdp-computed-tags'
    if (location.pathname.startsWith('/cdp/realtime-audiences')) return 'cdp-realtime-audiences'
    if (location.pathname.startsWith('/identity-types')) return 'identity-types'
    if (location.pathname.startsWith('/tag-import'))     return 'tag-import'
    if (location.pathname.startsWith('/audiences'))      return 'audiences'
    if (location.pathname.startsWith('/mq-config'))      return 'mq-config'
    if (location.pathname.startsWith('/event-config'))   return 'event-config'
    if (location.pathname.startsWith('/webhook-subscriptions')) return 'webhook-subscriptions'
    if (location.pathname.startsWith('/channel-connectors')) return 'channel-connectors'
    if (location.pathname.startsWith('/api-docs'))       return 'api-docs'
    if (location.pathname.startsWith('/system-options')) return 'system-options'
    if (location.pathname.startsWith('/admin/tenants'))  return 'admin-tenants'
    if (location.pathname.startsWith('/admin/users'))    return 'admin-users'
    if (location.pathname.startsWith('/admin/projects')) return 'admin-projects'
    if (location.pathname.startsWith('/test-users'))     return 'test-users'
    if (location.pathname.startsWith('/message-deliveries')) return 'message-deliveries'
    if (location.pathname.startsWith('/mautic-insights')) return 'mautic-insights'
    if (location.pathname.startsWith('/marketing-platform')) return 'marketing-platform'
    if (location.pathname.startsWith('/growth-activities')) return 'growth-activities'
    if (location.pathname.startsWith('/search-marketing')) return 'search-marketing'
    if (location.pathname.startsWith('/marketing-preferences')) return 'marketing-preferences'
    if (location.pathname.startsWith('/marketing-forms')) return 'marketing-forms'
    if (location.pathname.startsWith('/content-hub')) return 'content-hub'
    if (location.pathname.startsWith('/message-templates')) return 'message-templates'
    if (location.pathname.startsWith('/demo-sandbox')) return 'demo-sandbox'
    if (location.pathname.startsWith('/approvals')) return 'approvals'
    if (location.pathname.startsWith('/ops')) return 'ops'
    if (location.pathname.startsWith('/conversations')) return 'conversations'
    if (location.pathname.startsWith('/marketing-monitoring')) return 'marketing-monitoring'
    if (location.pathname.startsWith('/bi')) return 'bi'
    if (location.pathname.startsWith('/ai-predictions')) return 'ai-predictions'
    if (location.pathname.startsWith('/cdp/users'))      return 'cdp-users'
    return 'canvas'
  })()

  /** 根据当前菜单高亮项计算需要展开的父级菜单。 */
  const getDesiredOpenKeys = () => {
    if (selectedKey === 'home') return []
    if (['ops', 'approvals', 'conversations', 'marketing-monitoring'].includes(selectedKey)) return ['operations']
    if (selectedKey === 'bi') return ['analytics']
    if (['cdp-users', 'ai-predictions'].includes(selectedKey)) return ['insight']
    if (['test-users', 'message-deliveries', 'mautic-insights', 'marketing-platform', 'growth-activities', 'search-marketing', 'marketing-preferences', 'marketing-forms', 'content-hub', 'message-templates', 'demo-sandbox'].includes(selectedKey)) return ['marketing']
    if (selectedKey === 'api-docs') return ['developer']
    if (['audiences', 'tag-config', 'cdp-computed-profile', 'cdp-computed-tags', 'cdp-realtime-audiences', 'identity-types', 'tag-import', 'ab-experiments'].includes(selectedKey)) return ['data']
    if (['api-config', 'data-source-config', 'mq-config', 'event-config', 'webhook-subscriptions', 'channel-connectors'].includes(selectedKey)) return ['integration']
    if (['system-options', 'admin-tenants', 'admin-users', 'admin-projects'].includes(selectedKey)) return ['settings']
    return ['marketing']
  }

  const [openKeys, setOpenKeys] = useState<string[]>(getDesiredOpenKeys)

  useEffect(() => {
    if (!effectiveCollapsed) setOpenKeys(getDesiredOpenKeys())
  }, [effectiveCollapsed, selectedKey])

  const go = (path: string) => {
    navigate(path)
    if (isMobile) setMobileOpen(false)
  }

  const menuItems: MenuProps['items'] = [
    {
      key: 'home',
      icon: <HomeOutlined />,
      label: '首页',
      onClick: () => go('/home'),
    },
    {
      key: 'marketing',
      icon: <RocketOutlined />,
      label: '自动化营销',
      children: [
        {
          key: 'canvas',
          icon: <ApartmentOutlined />,
          label: '旅程管理',
          onClick: () => go('/canvas'),
        },
        {
          key: 'marketing-platform',
          icon: <DashboardOutlined />,
          label: '营销中台',
          onClick: () => navigate('/marketing-platform'),
        },
        {
          key: 'search-marketing',
          icon: <SearchOutlined />,
          label: 'SEO / SEM 管理',
          onClick: () => go('/search-marketing'),
        },
        {
          key: 'growth-activities',
          icon: <GiftOutlined />,
          label: '增长活动',
          onClick: () => go('/growth-activities'),
        },
        {
          key: 'mautic-insights',
          icon: <EyeOutlined />,
          label: '解释台',
          onClick: () => navigate('/mautic-insights'),
        },
        {
          key: 'marketing-preferences',
          icon: <SafetyCertificateOutlined />,
          label: '偏好中心',
          onClick: () => navigate('/marketing-preferences'),
        },
        {
          key: 'marketing-forms',
          icon: <FormOutlined />,
          label: '表单中心',
          onClick: () => navigate('/marketing-forms'),
        },
        {
          key: 'content-hub',
          icon: <FolderOpenOutlined />,
          label: '内容中心',
          onClick: () => navigate('/content-hub'),
        },
        {
          key: 'message-templates',
          icon: <BookOutlined />,
          label: '模板中心',
          onClick: () => navigate('/message-templates'),
        },
        {
          key: 'demo-sandbox',
          icon: <ExperimentOutlined />,
          label: '演示沙箱',
          onClick: () => navigate('/demo-sandbox'),
        },
        ...(isAdmin ? [{
          key: 'test-users',
          icon: <TeamOutlined />,
          label: '测试用户',
          onClick: () => navigate('/test-users'),
        }, {
          key: 'message-deliveries',
          icon: <SendOutlined />,
          label: '投递监控',
          onClick: () => navigate('/message-deliveries'),
        }] : []),
      ],
    },
    {
      key: 'analytics',
      icon: <BarChartOutlined />,
      label: '数据分析',
      children: [
        {
          key: 'bi',
          icon: <BarChartOutlined />,
          label: 'BI 工作台',
          onClick: () => navigate('/bi'),
        },
      ],
    },
    {
      key: 'operations',
      icon: <DashboardOutlined />,
      label: '运营值班',
      children: [
        {
          key: 'ops',
          icon: <DashboardOutlined />,
          label: '运维控制台',
          onClick: () => navigate('/ops'),
        },
        {
          key: 'approvals',
          icon: <SafetyCertificateOutlined />,
          label: '审批任务',
          onClick: () => navigate('/approvals'),
        },
        {
          key: 'conversations',
          icon: <MessageOutlined />,
          label: '会话工作台',
          onClick: () => navigate('/conversations'),
        },
        {
          key: 'marketing-monitoring',
          icon: <AlertOutlined />,
          label: '监测工作台',
          onClick: () => navigate('/marketing-monitoring'),
        },
      ],
    },
    {
      key: 'insight',
      icon: <EyeOutlined />,
      label: '用户洞察',
      children: [
        {
          key: 'cdp-users',
          icon: <IdcardOutlined />,
          label: '用户中心',
          onClick: () => go('/cdp/users'),
        },
        ...(isAdmin ? [{
          key: 'ai-predictions',
          icon: <ThunderboltOutlined />,
          label: '流失预测',
          onClick: () => navigate('/ai-predictions'),
        }] : []),
      ],
    },
    ...(isAdmin ? [{
      key: 'data',
      icon: <DatabaseOutlined />,
      label: '数据管理',
      children: [
        {
          key: 'audiences',
          icon: <TeamOutlined />,
          label: '人群管理',
          onClick: () => go('/audiences'),
        },
        {
          key: 'cdp-realtime-audiences',
          icon: <ThunderboltOutlined />,
          label: '实时人群',
          onClick: () => go('/cdp/realtime-audiences'),
        },
        {
          key: 'tag-config',
          icon: <TagsOutlined />,
          label: '标签配置',
          onClick: () => go('/tag-config'),
        },
        {
          key: 'cdp-computed-profile',
          icon: <ThunderboltOutlined />,
          label: '计算画像属性',
          onClick: () => go('/cdp/computed-profile'),
        },
        {
          key: 'cdp-computed-tags',
          icon: <TagsOutlined />,
          label: '计算标签',
          onClick: () => go('/cdp/computed-tags'),
        },
        {
          key: 'identity-types',
          icon: <UserOutlined />,
          label: 'ID 类型配置',
          onClick: () => go('/identity-types'),
        },
        {
          key: 'tag-import',
          icon: <TagsOutlined />,
          label: '标签导入',
          onClick: () => go('/tag-import'),
        },
        {
          key: 'ab-experiments',
          icon: <ExperimentOutlined />,
          label: 'AB 实验管理',
          onClick: () => go('/ab-experiments'),
        },
      ],
    }] : []),
    ...(isAdmin ? [{
      key: 'integration',
      icon: <ApiOutlined />,
      label: '集成配置',
      children: [
        {
          key: 'api-config',
          icon: <ApiOutlined />,
          label: 'API 接口配置',
          onClick: () => go('/api-config'),
        },
        {
          key: 'data-source-config',
          icon: <DatabaseOutlined />,
          label: '数据源配置',
          onClick: () => go('/data-source-config'),
        },
        {
          key: 'mq-config',
          icon: <NotificationOutlined />,
          label: 'MQ 消息配置',
          onClick: () => go('/mq-config'),
        },
        {
          key: 'event-config',
          icon: <ThunderboltOutlined />,
          label: '事件配置',
          onClick: () => go('/event-config'),
        },
        {
          key: 'webhook-subscriptions',
          icon: <ApiOutlined />,
          label: 'Webhook 订阅',
          onClick: () => go('/webhook-subscriptions'),
        },
        {
          key: 'channel-connectors',
          icon: <SafetyCertificateOutlined />,
          label: '渠道连接器',
          onClick: () => go('/channel-connectors'),
        },
      ],
    }] : []),
    ...(isAdmin ? [{
      key: 'developer',
      icon: <BookOutlined />,
      label: '开发者文档',
      children: [
        {
          key: 'api-docs',
          icon: <ApiOutlined />,
          label: 'API 说明',
          onClick: () => go('/api-docs'),
        },
      ],
    }] : []),
    ...(isAdmin ? [{
      key: 'settings',
      icon: <SettingOutlined />,
      label: '系统设置',
      children: [
        {
          key: 'system-options',
          icon: <SettingOutlined />,
          label: '系统选项配置',
          onClick: () => go('/system-options'),
        },
        ...(canManageTenants ? [{
          key: 'admin-tenants',
          icon: <BankOutlined />,
          label: '租户管理',
          onClick: () => go('/admin/tenants'),
        }] : []),
        ...(isAdmin ? [{
          key: 'admin-projects',
          icon: <ApartmentOutlined />,
          label: '项目管理',
          onClick: () => go('/admin/projects'),
        }, {
          key: 'admin-users',
          icon: <TeamOutlined />,
          label: '用户管理',
          onClick: () => go('/admin/users'),
        }] : []),
      ],
    }] : []),
  ]

  /** 后端登出失败也继续清理本地会话，保证用户能退出当前浏览器。 */
  const handleLogout = async () => {
    // 先尝试通知后端登出，再清本地认证态，接口失败也不阻断退出
    try { await authApi.logout() } catch { /* ignore */ }
    logout()
    navigate('/login')
  }

  const userMenu: MenuProps['items'] = [
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true, onClick: handleLogout },
  ]

  return (
    <Layout style={{ minHeight: '100vh', width: '100%', maxWidth: '100vw', overflowX: 'hidden' }}>
      <style>{siderChildrenStyle}</style>
      <a
        href="#main-content"
        className="skip-link"
        style={{
          position: 'fixed',
          top: 8,
          left: 8,
          zIndex: 1000,
          padding: '8px 12px',
          borderRadius: 6,
          background: '#ffffff',
          color: '#111827',
          transform: 'translateY(-150%)',
        }}
        onFocus={event => { event.currentTarget.style.transform = 'translateY(0)' }}
        onBlur={event => { event.currentTarget.style.transform = 'translateY(-150%)' }}
      >
        跳到主要内容
      </a>
      {isMobile && (
        <>
          <div style={mobileHeaderStyle}>
            <Button
              type="text"
              aria-label="打开导航"
              icon={<MenuUnfoldOutlined />}
              onClick={() => setMobileOpen(true)}
              style={{ color: '#fff' }}
            />
            <div
              style={{
                width: 28,
                height: 28,
                borderRadius: 8,
                flexShrink: 0,
                background: `linear-gradient(135deg, ${ACCENT} 0%, #7c3aed 100%)`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
              }}
              onClick={() => go('/home')}
            >
              <ApartmentOutlined style={{ color: '#fff', fontSize: 14 }} />
            </div>
            <span style={{ color: '#fff', fontSize: 15, fontWeight: 700, whiteSpace: 'nowrap' }}>营销画布</span>
            <div style={{ marginLeft: 'auto' }}>
              <NotificationBell />
            </div>
          </div>
          {mobileOpen && <div style={mobileBackdropStyle} onClick={() => setMobileOpen(false)} />}
        </>
      )}
      <Sider
        className="app-sider"
        collapsible={!isMobile}
        collapsed={effectiveCollapsed}
        trigger={null}
        width={220}
        collapsedWidth={64}
        style={{
          background: SIDER_DARK,
          display: 'flex',
          flexDirection: 'column',
          position: 'fixed',
          left: 0, top: 0, bottom: 0,
          zIndex: isMobile ? 1000 : 100,
          boxShadow: '2px 0 12px rgba(0,0,0,.35)',
          overflow: 'hidden',
          transform: isMobile && !mobileOpen ? 'translateX(-100%)' : 'translateX(0)',
          transition: 'transform .2s ease, width .2s',
          pointerEvents: isMobile && !mobileOpen ? 'none' : 'auto',
        }}
      >
        {/* Logo 行 + 收起按钮 */}
        <div style={{
          height: 56,
          display: 'flex', alignItems: 'center',
          padding: '0 16px',
          gap: 10,
          borderBottom: '1px solid rgba(255,255,255,.06)',
          flexShrink: 0,
          overflow: 'hidden',
        }}>
          {/* Logo icon — 点击回首页 */}
          <div
            style={{
              width: 28, height: 28, borderRadius: 8, flexShrink: 0,
              background: `linear-gradient(135deg, ${ACCENT} 0%, #7c3aed 100%)`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer',
            }}
            onClick={() => go('/canvas')}
          >
            <ApartmentOutlined style={{ color: '#fff', fontSize: 14 }} />
          </div>

          {!effectiveCollapsed && (
            <span
              style={{ color: '#fff', fontSize: 15, fontWeight: 700, letterSpacing: '.5px', whiteSpace: 'nowrap', flex: 1, cursor: 'pointer' }}
              onClick={() => go('/canvas')}
            >
              营销画布
            </span>
          )}

          {/* 收起/展开按钮放在 logo 行右侧 */}
          <Tooltip title={isMobile ? '关闭导航' : collapsed ? '展开' : '收起'} placement="right">
            <div
              onClick={() => isMobile ? setMobileOpen(false) : setCollapsed(c => !c)}
              style={{
                color: 'rgba(255,255,255,.35)', fontSize: 15, cursor: 'pointer',
                flexShrink: 0, transition: 'color .15s',
                marginLeft: effectiveCollapsed ? 0 : 'auto',
              }}
              onMouseEnter={e => (e.currentTarget.style.color = 'rgba(255,255,255,.8)')}
              onMouseLeave={e => (e.currentTarget.style.color = 'rgba(255,255,255,.35)')}
            >
              {isMobile || !effectiveCollapsed ? <MenuFoldOutlined /> : <MenuUnfoldOutlined />}
            </div>
          </Tooltip>
        </div>

        {/* 菜单 */}
        <div role="navigation" aria-label="主导航" style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', paddingTop: 8 }}>
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            openKeys={effectiveCollapsed ? [] : openKeys}
            onOpenChange={keys => setOpenKeys(keys as string[])}
            items={menuItems}
            style={{ background: 'transparent', border: 'none' }}
            theme="dark"
          />
        </div>

        {/* 用户信息——固定在最底部 */}
        <div style={{ borderTop: '1px solid rgba(255,255,255,.06)', padding: '12px 8px', flexShrink: 0 }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: effectiveCollapsed ? 6 : 8,
            flexDirection: effectiveCollapsed ? 'column' : 'row',
          }}>
            {!isMobile && <NotificationBell />}
            <Dropdown menu={{ items: userMenu }} placement="topLeft" trigger={['click']}>
              <div
                style={{
                  display: 'flex', alignItems: 'center', gap: 10,
                  padding: effectiveCollapsed ? '8px 10px' : '8px 12px',
                  borderRadius: 8, cursor: 'pointer', transition: 'background .15s',
                  flex: effectiveCollapsed ? 'none' : 1,
                  minWidth: 0,
                }}
                onMouseEnter={e => (e.currentTarget.style.background = SIDER_HOVER)}
                onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
              >
                <Avatar
                  size={32}
                  style={{ background: `linear-gradient(135deg, ${ACCENT}, #7c3aed)`, flexShrink: 0, fontSize: 13, fontWeight: 700 }}
                >
                  {user?.displayName?.[0] ?? <UserOutlined />}
                </Avatar>
                {!effectiveCollapsed && (
                  <div style={{ overflow: 'hidden', flex: 1 }}>
                    <div style={{ color: 'rgba(255,255,255,.9)', fontSize: 13, fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {user?.displayName}
                    </div>
                    <div style={{ color: 'rgba(255,255,255,.38)', fontSize: 11, marginTop: 1 }}>
                      {roleLabel(user?.role)}
                    </div>
                  </div>
                )}
              </div>
            </Dropdown>
          </div>
        </div>
      </Sider>

      {/* 内容区 */}
      <Layout style={{
        marginLeft: isMobile ? 0 : effectiveCollapsed ? 64 : 220,
        paddingTop: isMobile ? 56 : 0,
        transition: 'margin-left .2s',
        background: '#f5f6fa',
        minHeight: '100vh',
        minWidth: 0,
        width: '100%',
        maxWidth: '100vw',
        overflowX: 'hidden',
      }}>
        <Content
          id="main-content"
          role="main"
          tabIndex={-1}
          style={{ padding: isMobile ? 12 : 24, minHeight: isMobile ? 'calc(100vh - 56px)' : '100vh', minWidth: 0, maxWidth: '100%' }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

/** 读取媒体查询状态，供主布局在移动端切换为抽屉导航。 */
function useMediaQuery(query: string) {
  const [matches, setMatches] = useState(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false
    return window.matchMedia(query).matches
  })

  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return undefined
    const media = window.matchMedia(query)
    const update = () => setMatches(media.matches)
    update()
    if (media.addEventListener) {
      media.addEventListener('change', update)
      return () => media.removeEventListener('change', update)
    }
    media.addListener(update)
    return () => media.removeListener(update)
  }, [query])

  return matches
}

const mobileHeaderStyle: CSSProperties = {
  position: 'fixed',
  left: 0,
  right: 0,
  top: 0,
  height: 56,
  zIndex: 900,
  display: 'flex',
  alignItems: 'center',
  gap: 10,
  padding: '0 12px',
  background: SIDER_DARK,
  borderBottom: '1px solid rgba(255,255,255,.08)',
  boxShadow: '0 6px 18px rgba(15,23,42,.2)',
}

const mobileBackdropStyle: CSSProperties = {
  position: 'fixed',
  inset: 0,
  zIndex: 999,
  background: 'rgba(15,23,42,.42)',
}
