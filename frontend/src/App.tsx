/**
 * 模块职责：应用路由入口，集中声明登录页、主布局页、画布编辑页和后台管理页的访问路径。
 *
 * 维护说明：本文件只负责编排 Provider、懒加载页面和权限路由，不承载具体业务状态。
 */
import { Suspense, lazy, useEffect, type ReactNode } from 'react'
import { BrowserRouter, Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Spin } from 'antd'
import { AuthProvider } from './context/AuthContext'
import { NotificationProvider } from './context/NotificationContext'
import { RequireAuth, RequireAdmin, RequireSuperAdmin } from './auth/guards'
import AppErrorBoundary from './components/errors/AppErrorBoundary'
import NotFoundPage from './components/errors/NotFoundPage'
import RouteA11y from './components/accessibility/RouteA11y'
import ErrorBoundary from './components/layout/ErrorBoundary'

// 页面组件统一 lazy，首屏只加载登录态、路由和当前访问页面所需的代码块。
const AppLayout = lazy(() => import('./components/layout/AppLayout'))
/** 登录页懒加载组件，匿名访问时才下载对应页面代码。 */
const LoginPage = lazy(() => import('./pages/login'))
/** 首页懒加载组件，承载登录后的运营概览。 */
const HomePage = lazy(() => import('./pages/home'))
/** 画布列表页懒加载组件，展示可管理的营销旅程。 */
const CanvasListPage = lazy(() => import('./pages/canvas-list'))
/** 画布编辑器懒加载组件，体积较大所以独立成路由 chunk。 */
const CanvasEditorPage = lazy(() => import('./pages/canvas-editor'))
/** 画布统计页懒加载组件，包含图表依赖，按需加载。 */
const CanvasStatsPage = lazy(() => import('./pages/canvas-stats'))
/** 管理员用户页懒加载组件，仅管理员路由会访问。 */
const AdminUsersPage = lazy(() => import('./pages/admin'))
/** 租户管理页懒加载组件，仅超级管理员路由会访问。 */
const TenantAdminPage = lazy(() => import('./pages/tenant-admin'))
/** API 配置页懒加载组件，维护外部接口定义。 */
const ApiConfigPage = lazy(() => import('./pages/api-config'))
/** 数据源配置页懒加载组件，维护下拉和远程选项来源。 */
const DataSourceConfigPage = lazy(() => import('./pages/data-source-config'))
/** AB 实验配置页懒加载组件。 */
const AbExperimentPage = lazy(() => import('./pages/ab-experiment'))
/** 标签配置页懒加载组件。 */
const TagConfigPage = lazy(() => import('./pages/tag-config'))
/** 身份类型配置页懒加载组件。 */
const IdentityTypesPage = lazy(() => import('./pages/identity-types'))
/** 标签导入页懒加载组件。 */
const TagImportPage = lazy(() => import('./pages/tag-import'))
/** MQ 定义配置页懒加载组件。 */
const MqConfigPage = lazy(() => import('./pages/mq-config'))
/** 事件定义配置页懒加载组件。 */
const EventConfigPage = lazy(() => import('./pages/event-config'))
/** 人群列表页懒加载组件。 */
const AudienceListPage = lazy(() => import('./pages/audience-list'))
/** 人群编辑页懒加载组件。 */
const AudienceEditPage = lazy(() => import('./pages/audience-edit'))
/** API 文档页懒加载组件。 */
const ApiDocsPage = lazy(() => import('./pages/api-docs'))
/** 系统选项配置页懒加载组件。 */
const SystemOptionsPage = lazy(() => import('./pages/system-options'))
/** CDP 用户列表页懒加载组件。 */
const CdpUsersPage = lazy(() => import('./pages/cdp-users'))
/** CDP 用户详情页懒加载组件。 */
const CdpUserDetailPage = lazy(() => import('./pages/cdp-user-detail'))
/** 画布触达用户页懒加载组件。 */
const CanvasUsersPage = lazy(() => import('./pages/canvas-users'))
/** 测试用户和单用户复跑页懒加载组件。 */
const TestUsersPage = lazy(() => import('./pages/test-users'))
/** AI 流失预测运营页懒加载组件。 */
const AiPredictionsPage = lazy(() => import('./pages/ai-predictions'))
/** 投递 outbox 和回执监控页懒加载组件。 */
const MessageDeliveryPage = lazy(() => import('./pages/message-delivery'))
/** 生产运维控制台懒加载组件，运营角色可只读访问。 */
const OpsDashboardPage = lazy(() => import('./pages/ops-dashboard'))
/** Mautic 启发解释台懒加载组件。 */
const MauticInsightsPage = lazy(() => import('./pages/mautic-insights'))
/** 营销偏好中心懒加载组件。 */
const MarketingPreferencesPage = lazy(() => import('./pages/marketing-preferences'))
/** 营销表单中心懒加载组件。 */
const MarketingFormsPage = lazy(() => import('./pages/marketing-forms'))
/** 公开营销表单页懒加载组件；无需登录，用于线索收集。 */
const PublicMarketingFormPage = lazy(() => import('./pages/public-marketing-form'))
/** 通用 BI 工作台懒加载组件。 */
const BiWorkbenchPage = lazy(() => import('./pages/bi'))
/** BI 外部嵌入页懒加载组件；ticket 自带短期访问权限，不包登录布局。 */
const BiEmbedPage = lazy(() => import('./pages/bi/embed'))

/** 全屏加载态，配合 Suspense 覆盖页面 chunk 下载时间。 */
function RouteFallback() {
  return <Spin fullscreen />
}

function UnauthorizedRedirect() {
  const navigate = useNavigate()

  useEffect(() => {
    const onUnauthorized = (event: Event) => {
      const intendedPath = (event as CustomEvent<{ intendedPath?: string }>).detail?.intendedPath
      navigate('/login', { replace: true, state: { from: intendedPath } })
    }
    globalThis.addEventListener?.('canvas:unauthorized', onUnauthorized)
    return () => globalThis.removeEventListener?.('canvas:unauthorized', onUnauthorized)
  }, [navigate])

  return null
}

function RouteBoundary({ routeName, children }: { routeName: string; children: ReactNode }) {
  const location = useLocation()
  return (
    <ErrorBoundary routeName={routeName} resetKey={location.pathname}>
      {children}
    </ErrorBoundary>
  )
}

/** 应用根组件：Provider 注入在外，路由权限和布局分层在内。 */
export default function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <BrowserRouter>
          <UnauthorizedRedirect />
          <RouteA11y />
          <AppErrorBoundary>
            <Suspense fallback={<RouteFallback />}>
              <Routes>
                {/* 登录页不包权限守卫，避免未登录访问时出现重定向循环。 */}
                <Route path="/login" element={<LoginPage />} />
                <Route path="/bi/embed/:resourceType/:resourceKey" element={<BiEmbedPage />} />
                <Route path="/public/forms/:publicKey" element={<PublicMarketingFormPage />} />

              {/* 普通登录用户可访问首页、画布列表、CDP 用户和单画布详情页。 */}
                <Route element={<RouteBoundary routeName="登录用户路由"><RequireAuth /></RouteBoundary>}>
                  <Route element={<RouteBoundary routeName="主应用布局"><AppLayout /></RouteBoundary>}>
                    <Route path="/" element={<Navigate to="/home" replace />} />
                    <Route path="/home" element={<HomePage />} />
                    <Route path="/canvas" element={<CanvasListPage />} />
                    <Route path="/ops" element={<OpsDashboardPage />} />
                    <Route path="/bi" element={<BiWorkbenchPage />} />
                    <Route path="/mautic-insights" element={<MauticInsightsPage />} />
                    <Route path="/marketing-preferences" element={<MarketingPreferencesPage />} />
                    <Route path="/marketing-forms" element={<MarketingFormsPage />} />
                    <Route path="/cdp/users" element={<CdpUsersPage />} />
                    <Route path="/cdp/users/:userId" element={<CdpUserDetailPage />} />
                  </Route>

                  <Route path="/canvas/:id/edit" element={<RouteBoundary routeName="画布编辑器"><CanvasEditorPage /></RouteBoundary>} />
                  <Route path="/canvas/:id/stats" element={<RouteBoundary routeName="画布统计"><CanvasStatsPage /></RouteBoundary>} />
                  <Route path="/canvas/:id/users" element={<RouteBoundary routeName="画布用户"><CanvasUsersPage /></RouteBoundary>} />
                </Route>

                {/* 管理配置类页面统一放在管理员守卫下，菜单展示也由 AppLayout 内部按角色收敛。 */}
                <Route element={<RouteBoundary routeName="管理员路由"><RequireAdmin /></RouteBoundary>}>
                  <Route element={<RouteBoundary routeName="管理员布局"><AppLayout /></RouteBoundary>}>
                    <Route path="/admin/users" element={<AdminUsersPage />} />
                    <Route path="/api-config" element={<ApiConfigPage />} />
                    <Route path="/data-source-config" element={<DataSourceConfigPage />} />
                    <Route path="/ab-experiments" element={<AbExperimentPage />} />
                    <Route path="/tag-config" element={<TagConfigPage />} />
                    <Route path="/identity-types" element={<IdentityTypesPage />} />
                    <Route path="/tag-import" element={<TagImportPage />} />
                    <Route path="/audiences" element={<AudienceListPage />} />
                    <Route path="/audiences/new" element={<AudienceEditPage />} />
                    <Route path="/audiences/:id/edit" element={<AudienceEditPage />} />
                    <Route path="/mq-config" element={<MqConfigPage />} />
                    <Route path="/event-config" element={<EventConfigPage />} />
                    <Route path="/api-docs" element={<ApiDocsPage />} />
                    <Route path="/system-options" element={<SystemOptionsPage />} />
                    <Route path="/test-users" element={<TestUsersPage />} />
                    <Route path="/ai-predictions" element={<AiPredictionsPage />} />
                    <Route path="/message-deliveries" element={<MessageDeliveryPage />} />
                  </Route>
                </Route>

                <Route element={<RouteBoundary routeName="超级管理员路由"><RequireSuperAdmin /></RouteBoundary>}>
                  <Route element={<RouteBoundary routeName="超级管理员布局"><AppLayout /></RouteBoundary>}>
                    <Route path="/admin/tenants" element={<TenantAdminPage />} />
                  </Route>
                </Route>

                <Route path="*" element={<NotFoundPage />} />
              </Routes>
            </Suspense>
          </AppErrorBoundary>
        </BrowserRouter>
      </NotificationProvider>
    </AuthProvider>
  )
}
