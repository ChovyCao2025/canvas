/**
 * 组件职责：登录后的主应用布局，包含侧边导航、折叠菜单、用户信息和通知入口。
 *
 * 维护说明：菜单 key 与路由路径强绑定，新增页面时需要同步这里的导航配置。
 */
import { useEffect, useState } from 'react'
import { Layout, Menu, Avatar, Dropdown, Tooltip, type MenuProps } from 'antd'
import {
  ApartmentOutlined, SettingOutlined, ApiOutlined,
  ExperimentOutlined, TeamOutlined, LogoutOutlined,
  UserOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
  RocketOutlined, HomeOutlined, TagsOutlined, NotificationOutlined, ThunderboltOutlined,
  BookOutlined,
  DatabaseOutlined,
  IdcardOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { authApi } from '../../services/api'
import NotificationBell from '../notifications/NotificationBell'

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
  const { user, isAdmin, logout } = useAuth()
  const [collapsed, setCollapsed] = useState(false)

  // 根据当前 URL 计算菜单高亮 key
  const selectedKey = (() => {
    if (location.pathname === '/' || location.pathname === '/home') return 'home'
    if (location.pathname.startsWith('/api-config'))     return 'api-config'
    if (location.pathname.startsWith('/data-source-config')) return 'data-source-config'
    if (location.pathname.startsWith('/ab-experiments')) return 'ab-experiments'
    if (location.pathname.startsWith('/tag-config'))     return 'tag-config'
    if (location.pathname.startsWith('/identity-types')) return 'identity-types'
    if (location.pathname.startsWith('/tag-import'))     return 'tag-import'
    if (location.pathname.startsWith('/audiences'))      return 'audiences'
    if (location.pathname.startsWith('/mq-config'))      return 'mq-config'
    if (location.pathname.startsWith('/event-config'))   return 'event-config'
    if (location.pathname.startsWith('/api-docs'))       return 'api-docs'
    if (location.pathname.startsWith('/system-options')) return 'system-options'
    if (location.pathname.startsWith('/admin/users'))    return 'admin-users'
    if (location.pathname.startsWith('/cdp/users'))      return 'cdp-users'
    return 'canvas'
  })()

  /** 根据当前菜单高亮项计算需要展开的父级菜单。 */
  const getDesiredOpenKeys = () => {
    // 保证进入子页面时，父菜单分组同步展开，减少额外点击
    if (selectedKey === 'api-docs') return ['developer']
    if ([
      'api-config',
      'data-source-config',
      'ab-experiments',
      'tag-config',
      'identity-types',
      'tag-import',
      'audiences',
      'mq-config',
      'event-config',
      'system-options',
      'admin-users',
    ].includes(selectedKey)) return ['settings']
    if (selectedKey === 'home') return []
    return ['marketing']
  }

  const [openKeys, setOpenKeys] = useState<string[]>(getDesiredOpenKeys)

  useEffect(() => {
    if (!collapsed) setOpenKeys(getDesiredOpenKeys())
  }, [collapsed, selectedKey])

  const menuItems: MenuProps['items'] = [
    {
      key: 'home',
      icon: <HomeOutlined />,
      label: '首页',
      onClick: () => navigate('/home'),
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
          onClick: () => navigate('/canvas'),
        },
        {
          key: 'cdp-users',
          icon: <IdcardOutlined />,
          label: 'CDP 用户中心',
          onClick: () => navigate('/cdp/users'),
        },
      ],
    },
    ...(isAdmin ? [{
      key: 'developer',
      icon: <BookOutlined />,
      label: '开发者文档',
      children: [
        {
          key: 'api-docs',
          icon: <ApiOutlined />,
          label: 'API 说明',
          onClick: () => navigate('/api-docs'),
        },
      ],
    }] : []),
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '系统设置',
      children: [
        {
          key: 'api-config',
          icon: <ApiOutlined />,
          label: 'API 接口配置',
          onClick: () => navigate('/api-config'),
        },
        {
          key: 'data-source-config',
          icon: <DatabaseOutlined />,
          label: '数据源配置',
          onClick: () => navigate('/data-source-config'),
        },
        {
          key: 'ab-experiments',
          icon: <ExperimentOutlined />,
          label: 'AB 实验管理',
          onClick: () => navigate('/ab-experiments'),
        },
        {
          key: 'tag-config',
          icon: <TagsOutlined />,
          label: '标签配置',
          onClick: () => navigate('/tag-config'),
        },
        {
          key: 'identity-types',
          icon: <UserOutlined />,
          label: 'ID 类型配置',
          onClick: () => navigate('/identity-types'),
        },
        {
          key: 'tag-import',
          icon: <TagsOutlined />,
          label: '标签导入',
          onClick: () => navigate('/tag-import'),
        },
        {
          key: 'audiences',
          icon: <TeamOutlined />,
          label: '人群管理',
          onClick: () => navigate('/audiences'),
        },
        {
          key: 'mq-config',
          icon: <NotificationOutlined />,
          label: 'MQ 消息配置',
          onClick: () => navigate('/mq-config'),
        },
        {
          key: 'event-config',
          icon: <ThunderboltOutlined />,
          label: '事件配置',
          onClick: () => navigate('/event-config'),
        },
        {
          key: 'system-options',
          icon: <SettingOutlined />,
          label: '系统选项配置',
          onClick: () => navigate('/system-options'),
        },
        ...(isAdmin ? [{
          // 管理员才显示用户管理入口
          key: 'admin-users',
          icon: <TeamOutlined />,
          label: '用户管理',
          onClick: () => navigate('/admin/users'),
        }] : []),
      ],
    },
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
    <Layout style={{ minHeight: '100vh' }}>
      <style>{siderChildrenStyle}</style>
      <Sider
        className="app-sider"
        collapsible
        collapsed={collapsed}
        trigger={null}
        width={220}
        collapsedWidth={64}
        style={{
          background: SIDER_DARK,
          display: 'flex',
          flexDirection: 'column',
          position: 'fixed',
          left: 0, top: 0, bottom: 0,
          zIndex: 100,
          boxShadow: '2px 0 12px rgba(0,0,0,.35)',
          overflow: 'hidden',
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
            onClick={() => navigate('/canvas')}
          >
            <ApartmentOutlined style={{ color: '#fff', fontSize: 14 }} />
          </div>

          {!collapsed && (
            <span
              style={{ color: '#fff', fontSize: 15, fontWeight: 700, letterSpacing: '.5px', whiteSpace: 'nowrap', flex: 1, cursor: 'pointer' }}
              onClick={() => navigate('/canvas')}
            >
              营销画布
            </span>
          )}

          {/* 收起/展开按钮放在 logo 行右侧 */}
          <Tooltip title={collapsed ? '展开' : '收起'} placement="right">
            <div
              onClick={() => setCollapsed(c => !c)}
              style={{
                color: 'rgba(255,255,255,.35)', fontSize: 15, cursor: 'pointer',
                flexShrink: 0, transition: 'color .15s',
                marginLeft: collapsed ? 0 : 'auto',
              }}
              onMouseEnter={e => (e.currentTarget.style.color = 'rgba(255,255,255,.8)')}
              onMouseLeave={e => (e.currentTarget.style.color = 'rgba(255,255,255,.35)')}
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </div>
          </Tooltip>
        </div>

        {/* 菜单 */}
        <div style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', paddingTop: 8 }}>
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            openKeys={collapsed ? [] : openKeys}
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
            gap: collapsed ? 6 : 8,
            flexDirection: collapsed ? 'column' : 'row',
          }}>
            <NotificationBell />
            <Dropdown menu={{ items: userMenu }} placement="topLeft" trigger={['click']}>
              <div
                style={{
                  display: 'flex', alignItems: 'center', gap: 10,
                  padding: collapsed ? '8px 10px' : '8px 12px',
                  borderRadius: 8, cursor: 'pointer', transition: 'background .15s',
                  flex: collapsed ? 'none' : 1,
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
                {!collapsed && (
                  <div style={{ overflow: 'hidden', flex: 1 }}>
                    <div style={{ color: 'rgba(255,255,255,.9)', fontSize: 13, fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {user?.displayName}
                    </div>
                    <div style={{ color: 'rgba(255,255,255,.38)', fontSize: 11, marginTop: 1 }}>
                      {user?.role === 'ADMIN' ? '管理员' : '操作员'}
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
        marginLeft: collapsed ? 64 : 220,
        transition: 'margin-left .2s',
        background: '#f5f6fa',
        minHeight: '100vh',
      }}>
        <Content style={{ padding: 24, minHeight: '100vh' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
