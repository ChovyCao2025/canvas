import type { R } from '../types'
import type {
  MarketingMonitorAlert,
  MarketingMonitorAlertQuery,
  MarketingMonitorIngestResult,
  MarketingMonitorItem,
  MarketingMonitorItemIngestPayload,
  MarketingMonitorItemQuery,
  MarketingMonitorPollPayload,
  MarketingMonitorPollRun,
  MarketingMonitorProviderCredential,
  MarketingMonitorProviderCredentialDueRefreshCommand,
  MarketingMonitorProviderCredentialDueRefreshResult,
  MarketingMonitorProviderCredentialEvent,
  MarketingMonitorProviderCredentialEventQuery,
  MarketingMonitorProviderCredentialQuery,
  MarketingMonitorProviderCredentialRevokeCommand,
  MarketingMonitorProviderOAuthAuthorization,
  MarketingMonitorProviderOAuthAuthorizationCommand,
  MarketingMonitorProviderOAuthAuthorizationQuery,
  MarketingMonitorProviderOAuthCallbackCommand,
  MarketingMonitorSource,
  MarketingMonitorSourcePayload,
  MarketingMonitorSourcePolling,
  MarketingMonitorSourcePollingPayload,
  MarketingMonitorTrendSnapshot,
  MarketingMonitorTrendSnapshotPayload,
  MarketingMonitorTrendSnapshotQuery,
} from '../pages/marketing-monitoring/monitoringWorkbench'
import http from './api'

/** 营销监控 API，覆盖来源、入库、告警、趋势和 Provider 凭据/OAuth。 */
export const marketingMonitoringApi = {
  /** 保存监控来源。 */
  upsertSource: (payload: MarketingMonitorSourcePayload) =>
    http.post<R<MarketingMonitorSource>, R<MarketingMonitorSource>>(
      '/canvas/marketing-monitoring/sources',
      payload,
    ),

  /** 入库监控内容并触发情绪、竞品和告警计算。 */
  ingestItem: (payload: MarketingMonitorItemIngestPayload) =>
    http.post<R<MarketingMonitorIngestResult>, R<MarketingMonitorIngestResult>>(
      '/canvas/marketing-monitoring/items',
      payload,
    ),

  /** 查询监控内容项。 */
  listItems: (params?: MarketingMonitorItemQuery) =>
    http.get<R<MarketingMonitorItem[]>, R<MarketingMonitorItem[]>>(
      '/canvas/marketing-monitoring/items',
      { params },
    ),

  /** 查询监控告警。 */
  listAlerts: (params?: MarketingMonitorAlertQuery) =>
    http.get<R<MarketingMonitorAlert[]>, R<MarketingMonitorAlert[]>>(
      '/canvas/marketing-monitoring/alerts',
      { params },
    ),

  /** 将告警标记为已处理。 */
  resolveAlert: (alertId: number) =>
    http.post<R<MarketingMonitorAlert>, R<MarketingMonitorAlert>>(
      `/canvas/marketing-monitoring/alerts/${alertId}/resolve`,
    ),

  /** 配置监控来源轮询。 */
  configureSourcePolling: (sourceId: number, payload: MarketingMonitorSourcePollingPayload) =>
    http.post<R<MarketingMonitorSourcePolling>, R<MarketingMonitorSourcePolling>>(
      `/canvas/marketing-monitoring/sources/${sourceId}/polling`,
      payload,
    ),

  /** 手动触发来源轮询。 */
  pollSource: (sourceId: number, payload: MarketingMonitorPollPayload) =>
    http.post<R<MarketingMonitorPollRun>, R<MarketingMonitorPollRun>>(
      `/canvas/marketing-monitoring/sources/${sourceId}/poll`,
      payload,
    ),

  /** 生成品牌/竞品趋势快照。 */
  buildTrendSnapshot: (payload: MarketingMonitorTrendSnapshotPayload) =>
    http.post<R<MarketingMonitorTrendSnapshot>, R<MarketingMonitorTrendSnapshot>>(
      '/canvas/marketing-monitoring/trends/snapshots/build',
      payload,
    ),

  /** 查询趋势快照。 */
  listTrendSnapshots: (params?: MarketingMonitorTrendSnapshotQuery) =>
    http.get<R<MarketingMonitorTrendSnapshot[]>, R<MarketingMonitorTrendSnapshot[]>>(
      '/canvas/marketing-monitoring/trends/snapshots',
      { params },
    ),

  /** 查询 Provider 凭据。 */
  listProviderCredentials: (params?: MarketingMonitorProviderCredentialQuery) =>
    http.get<R<MarketingMonitorProviderCredential[]>, R<MarketingMonitorProviderCredential[]>>(
      '/canvas/marketing-monitoring/provider-credentials',
      { params },
    ),

  /** 刷新单个 Provider 凭据。 */
  refreshProviderCredential: (credentialKey: string) =>
    http.post<R<MarketingMonitorProviderCredential>, R<MarketingMonitorProviderCredential>>(
      `/canvas/marketing-monitoring/provider-credentials/${encodeURIComponent(credentialKey)}/refresh`,
      {},
    ),

  /** 批量刷新即将到期的 Provider 凭据。 */
  refreshDueProviderCredentials: (payload?: MarketingMonitorProviderCredentialDueRefreshCommand) =>
    http.post<
      R<MarketingMonitorProviderCredentialDueRefreshResult>,
      R<MarketingMonitorProviderCredentialDueRefreshResult>
    >(
      '/canvas/marketing-monitoring/provider-credentials/refresh-due',
      payload ?? {},
    ),

  /** 撤销 Provider 凭据。 */
  revokeProviderCredential: (credentialKey: string, payload?: MarketingMonitorProviderCredentialRevokeCommand) =>
    http.post<R<MarketingMonitorProviderCredential>, R<MarketingMonitorProviderCredential>>(
      `/canvas/marketing-monitoring/provider-credentials/${encodeURIComponent(credentialKey)}/revoke`,
      payload ?? {},
    ),

  /** 停用本地 Provider 凭据。 */
  disableProviderCredential: (credentialKey: string) =>
    http.post<R<MarketingMonitorProviderCredential>, R<MarketingMonitorProviderCredential>>(
      `/canvas/marketing-monitoring/provider-credentials/${encodeURIComponent(credentialKey)}/disable`,
    ),

  /** 查询 Provider 凭据事件。 */
  listProviderCredentialEvents: (params?: MarketingMonitorProviderCredentialEventQuery) =>
    http.get<R<MarketingMonitorProviderCredentialEvent[]>, R<MarketingMonitorProviderCredentialEvent[]>>(
      '/canvas/marketing-monitoring/provider-credentials/events',
      { params },
    ),

  /** 发起 Provider OAuth 授权。 */
  startProviderOAuthAuthorization: (payload: MarketingMonitorProviderOAuthAuthorizationCommand) =>
    http.post<R<MarketingMonitorProviderOAuthAuthorization>, R<MarketingMonitorProviderOAuthAuthorization>>(
      '/canvas/marketing-monitoring/provider-credentials/oauth/authorizations',
      payload,
    ),

  /** 完成 Provider OAuth 回调。 */
  completeProviderOAuthAuthorization: (payload: MarketingMonitorProviderOAuthCallbackCommand) =>
    http.post<R<MarketingMonitorProviderOAuthAuthorization>, R<MarketingMonitorProviderOAuthAuthorization>>(
      '/canvas/marketing-monitoring/provider-credentials/oauth/callback',
      payload,
    ),

  /** 查询 OAuth 授权会话。 */
  listProviderOAuthAuthorizations: (params?: MarketingMonitorProviderOAuthAuthorizationQuery) =>
    http.get<R<MarketingMonitorProviderOAuthAuthorization[]>, R<MarketingMonitorProviderOAuthAuthorization[]>>(
      '/canvas/marketing-monitoring/provider-credentials/oauth/authorizations',
      { params },
    ),
}
