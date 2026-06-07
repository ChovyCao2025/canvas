import http from './api'
import type { R } from '../types'
import type { PlatformWorkstreamStatus } from '../pages/home/platformCommandCenter'

export const platformWorkstreamApi = {
  list: () =>
    http.get<R<PlatformWorkstreamStatus[]>, R<PlatformWorkstreamStatus[]>>('/platform/workstreams'),
}
