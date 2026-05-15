import { useState } from 'react'
import { Layout, Menu, Avatar, Dropdown, Tooltip, type MenuProps } from 'antd'
import {
  ApartmentOutlined, SettingOutlined, ApiOutlined,
  ExperimentOutlined, TeamOutlined, LogoutOutlined,
  UserOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
  RocketOutlined, HomeOutlined, TagsOutlined, NotificationOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { authApi } from '../../services/api'

const { Sider, Content } = Layout

// Ant Design Sider 内部包了一层 .ant-layout-sider-children，需要让它也是 flex column
const siderChildrenStyle = `
  .app-sider > .ant-layout-sider-children {
    display: flex !important;
    flex-direction: column !important;
    height: 100% !important;
    overflow: hidden !important;
  }
`

const SIDER_DARK  = '#0d1117'
const SIDER_HOVER = '#1f2d45'
const ACCENT      = '#4f8ef7'

export default function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, isAdmin, logout } = useAuth()
  const [collapsed, setCollapsed] = useState(false)

  const selectedKey = (() => {
    if (location.pathname === '/' || location.pathname === '/home') return 'home'
    if (location.pathname.startsWith('/api-config'))     return 'api-config'
    if (location.pathname.startsWith('/ab-experiments')) return 'ab-experiments'
    if (location.pathname.startsWith('/tag-config'))     return 'tag-config'
    if (location.pathname.startsWith('/mq-config'))      return 'mq-config'
    if (location.pathname.startsWith('/admin/users'))    return 'admin-users'
    return 'canvas'
  })()

  const defaultOpenKeys = (() => {
    if (['api-config', 'ab-experiments', 'admin-users'].includes(selectedKey)) return ['marketing', 'settings']
    return ['marketing']
  })()

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
      ],
    },
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
          key: 'mq-config',
          icon: <NotificationOutlined />,
          label: 'MQ 消息配置',
          onClick: () => navigate('/mq-config'),
        },
        ...(isAdmin ? [{
          key: 'admin-users',
          icon: <TeamOutlined />,
          label: '用户管理',
          onClick: () => navigate('/admin/users'),
        }] : []),
      ],
    },
  ]

  const handleLogout = async () => {
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
            defaultOpenKeys={collapsed ? [] : defaultOpenKeys}
            items={menuItems}
            style={{ background: 'transparent', border: 'none' }}
            theme="dark"
          />
        </div>

        {/* 用户信息——固定在最底部 */}
        <div style={{ borderTop: '1px solid rgba(255,255,255,.06)', padding: '12px 8px', flexShrink: 0 }}>
          <Dropdown menu={{ items: userMenu }} placement="topLeft" trigger={['click']}>
            <div
              style={{
                display: 'flex', alignItems: 'center', gap: 10,
                padding: collapsed ? '8px 10px' : '8px 12px',
                borderRadius: 8, cursor: 'pointer', transition: 'background .15s',
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
