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

export const marketingMonitoringApi = {
  upsertSource: (payload: MarketingMonitorSourcePayload) =>
    http.post<R<MarketingMonitorSource>, R<MarketingMonitorSource>>(
      '/canvas/marketing-monitoring/sources',
      payload,
    ),

  ingestItem: (payload: MarketingMonitorItemIngestPayload) =>
    http.post<R<MarketingMonitorIngestResult>, R<MarketingMonitorIngestResult>>(
      '/canvas/marketing-monitoring/items',
      payload,
    ),

  listItems: (params?: MarketingMonitorItemQuery) =>
    http.get<R<MarketingMonitorItem[]>, R<MarketingMonitorItem[]>>(
      '/canvas/marketing-monitoring/items',
      { params },
    ),

  listAlerts: (params?: MarketingMonitorAlertQuery) =>
    http.get<R<MarketingMonitorAlert[]>, R<MarketingMonitorAlert[]>>(
      '/canvas/marketing-monitoring/alerts',
      { params },
    ),

  resolveAlert: (alertId: number) =>
    http.post<R<MarketingMonitorAlert>, R<MarketingMonitorAlert>>(
      `/canvas/marketing-monitoring/alerts/${alertId}/resolve`,
    ),

  configureSourcePolling: (sourceId: number, payload: MarketingMonitorSourcePollingPayload) =>
    http.post<R<MarketingMonitorSourcePolling>, R<MarketingMonitorSourcePolling>>(
      `/canvas/marketing-monitoring/sources/${sourceId}/polling`,
      payload,
    ),

  pollSource: (sourceId: number, payload: MarketingMonitorPollPayload) =>
    http.post<R<MarketingMonitorPollRun>, R<MarketingMonitorPollRun>>(
      `/canvas/marketing-monitoring/sources/${sourceId}/poll`,
      payload,
    ),

  buildTrendSnapshot: (payload: MarketingMonitorTrendSnapshotPayload) =>
    http.post<R<MarketingMonitorTrendSnapshot>, R<MarketingMonitorTrendSnapshot>>(
      '/canvas/marketing-monitoring/trends/snapshots/build',
      payload,
    ),

  listTrendSnapshots: (params?: MarketingMonitorTrendSnapshotQuery) =>
    http.get<R<MarketingMonitorTrendSnapshot[]>, R<MarketingMonitorTrendSnapshot[]>>(
      '/canvas/marketing-monitoring/trends/snapshots',
      { params },
    ),

  listProviderCredentials: (params?: MarketingMonitorProviderCredentialQuery) =>
    http.get<R<MarketingMonitorProviderCredential[]>, R<MarketingMonitorProviderCredential[]>>(
      '/canvas/marketing-monitoring/provider-credentials',
      { params },
    ),

  refreshProviderCredential: (credentialKey: string) =>
    http.post<R<MarketingMonitorProviderCredential>, R<MarketingMonitorProviderCredential>>(
      `/canvas/marketing-monitoring/provider-credentials/${encodeURIComponent(credentialKey)}/refresh`,
      {},
    ),

  refreshDueProviderCredentials: (payload?: MarketingMonitorProviderCredentialDueRefreshCommand) =>
    http.post<
      R<MarketingMonitorProviderCredentialDueRefreshResult>,
      R<MarketingMonitorProviderCredentialDueRefreshResult>
    >(
      '/canvas/marketing-monitoring/provider-credentials/refresh-due',
      payload ?? {},
    ),

  revokeProviderCredential: (credentialKey: string, payload?: MarketingMonitorProviderCredentialRevokeCommand) =>
    http.post<R<MarketingMonitorProviderCredential>, R<MarketingMonitorProviderCredential>>(
      `/canvas/marketing-monitoring/provider-credentials/${encodeURIComponent(credentialKey)}/revoke`,
      payload ?? {},
    ),

  disableProviderCredential: (credentialKey: string) =>
    http.post<R<MarketingMonitorProviderCredential>, R<MarketingMonitorProviderCredential>>(
      `/canvas/marketing-monitoring/provider-credentials/${encodeURIComponent(credentialKey)}/disable`,
    ),

  listProviderCredentialEvents: (params?: MarketingMonitorProviderCredentialEventQuery) =>
    http.get<R<MarketingMonitorProviderCredentialEvent[]>, R<MarketingMonitorProviderCredentialEvent[]>>(
      '/canvas/marketing-monitoring/provider-credentials/events',
      { params },
    ),

  startProviderOAuthAuthorization: (payload: MarketingMonitorProviderOAuthAuthorizationCommand) =>
    http.post<R<MarketingMonitorProviderOAuthAuthorization>, R<MarketingMonitorProviderOAuthAuthorization>>(
      '/canvas/marketing-monitoring/provider-credentials/oauth/authorizations',
      payload,
    ),

  completeProviderOAuthAuthorization: (payload: MarketingMonitorProviderOAuthCallbackCommand) =>
    http.post<R<MarketingMonitorProviderOAuthAuthorization>, R<MarketingMonitorProviderOAuthAuthorization>>(
      '/canvas/marketing-monitoring/provider-credentials/oauth/callback',
      payload,
    ),

  listProviderOAuthAuthorizations: (params?: MarketingMonitorProviderOAuthAuthorizationQuery) =>
    http.get<R<MarketingMonitorProviderOAuthAuthorization[]>, R<MarketingMonitorProviderOAuthAuthorization[]>>(
      '/canvas/marketing-monitoring/provider-credentials/oauth/authorizations',
      { params },
    ),
}
