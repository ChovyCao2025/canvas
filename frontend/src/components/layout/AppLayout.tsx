import { useState } from 'react'
import { Layout, Menu, Avatar, Dropdown, Tooltip, type MenuProps } from 'antd'
import {
  ApartmentOutlined, SettingOutlined, ApiOutlined,
  ExperimentOutlined, TeamOutlined, LogoutOutlined,
  UserOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { authApi } from '../../services/api'

const { Sider, Content } = Layout

const SIDER_DARK  = '#0d1117'
const SIDER_HOVER = '#1f2d45'
const ACCENT      = '#4f8ef7'

export default function AppLayout() {
  const navigate   = useNavigate()
  const location   = useLocation()
  const { user, isAdmin, logout } = useAuth()
  const [collapsed, setCollapsed] = useState(false)

  // 当前选中 key
  const selectedKey = (() => {
    if (location.pathname.startsWith('/api-config'))     return 'api-config'
    if (location.pathname.startsWith('/ab-experiments')) return 'ab-experiments'
    if (location.pathname.startsWith('/admin/users'))    return 'admin-users'
    return 'canvas'
  })()

  // 展开哪个 submenu
  const openKey = ['api-config', 'ab-experiments', 'admin-users'].includes(selectedKey)
    ? 'settings' : undefined

  const menuItems: MenuProps['items'] = [
    {
      key: 'canvas',
      icon: <ApartmentOutlined />,
      label: '旅程管理',
      onClick: () => navigate('/canvas'),
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
      <Sider
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
        {/* Logo */}
        <div style={{
          height: 56,
          display: 'flex', alignItems: 'center',
          padding: collapsed ? '0 20px' : '0 20px',
          gap: 10,
          borderBottom: '1px solid rgba(255,255,255,.06)',
          flexShrink: 0,
          cursor: 'pointer',
          overflow: 'hidden',
        }} onClick={() => navigate('/canvas')}>
          <div style={{
            width: 28, height: 28, borderRadius: 8,
            background: `linear-gradient(135deg, ${ACCENT} 0%, #7c3aed 100%)`,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            flexShrink: 0,
          }}>
            <ApartmentOutlined style={{ color: '#fff', fontSize: 14 }} />
          </div>
          {!collapsed && (
            <span style={{
              color: '#fff', fontSize: 15, fontWeight: 700,
              letterSpacing: '.5px', whiteSpace: 'nowrap',
            }}>
              营销画布
            </span>
          )}
        </div>

        {/* Menu */}
        <div style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', paddingTop: 8 }}>
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            defaultOpenKeys={openKey ? [openKey] : []}
            items={menuItems}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'rgba(255,255,255,.65)',
            }}
            theme="dark"
          />
        </div>

        {/* Bottom: collapse toggle + user */}
        <div style={{
          borderTop: '1px solid rgba(255,255,255,.06)',
          padding: '10px 0',
          flexShrink: 0,
        }}>
          {/* User avatar */}
          <Dropdown menu={{ items: userMenu }} placement="topLeft" trigger={['click']}>
            <div style={{
              display: 'flex', alignItems: 'center', gap: 10,
              padding: collapsed ? '8px 18px' : '8px 16px',
              cursor: 'pointer', borderRadius: 6, margin: '0 8px 4px',
              transition: 'background .15s',
            }}
              onMouseEnter={e => (e.currentTarget.style.background = SIDER_HOVER)}
              onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
            >
              <Avatar
                size={28}
                style={{ background: ACCENT, flexShrink: 0, fontSize: 12, fontWeight: 600 }}
                icon={<UserOutlined />}
              >
                {user?.displayName?.[0]}
              </Avatar>
              {!collapsed && (
                <div style={{ overflow: 'hidden' }}>
                  <div style={{ color: 'rgba(255,255,255,.9)', fontSize: 13, fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {user?.displayName}
                  </div>
                  <div style={{ color: 'rgba(255,255,255,.4)', fontSize: 11, marginTop: 1 }}>
                    {user?.role === 'ADMIN' ? '管理员' : '操作员'}
                  </div>
                </div>
              )}
            </div>
          </Dropdown>

          {/* Collapse toggle */}
          <Tooltip title={collapsed ? '展开菜单' : '收起菜单'} placement="right">
            <div
              style={{
                display: 'flex', alignItems: 'center', justifyContent: collapsed ? 'center' : 'flex-end',
                padding: '6px 16px',
                color: 'rgba(255,255,255,.3)',
                cursor: 'pointer',
                fontSize: 16,
                transition: 'color .15s',
              }}
              onClick={() => setCollapsed(c => !c)}
              onMouseEnter={e => (e.currentTarget.style.color = 'rgba(255,255,255,.7)')}
              onMouseLeave={e => (e.currentTarget.style.color = 'rgba(255,255,255,.3)')}
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </div>
          </Tooltip>
        </div>
      </Sider>

      {/* Main */}
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
