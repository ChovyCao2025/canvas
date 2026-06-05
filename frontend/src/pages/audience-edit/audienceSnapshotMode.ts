/**
 * 工具职责：人群默认发送快照模式的前端默认值与展示文案。
 *
 * 维护说明：后端历史数据可能没有 defaultSnapshotMode，页面统一按发布时锁定展示和提交。
 */
import type { AudienceSnapshotMode } from '../../services/audienceApi'

export const DEFAULT_AUDIENCE_SNAPSHOT_MODE: AudienceSnapshotMode = 'STATIC_LOCKED'

const SNAPSHOT_MODE_LABELS: Record<AudienceSnapshotMode, string> = {
  STATIC_LOCKED: '发布时锁定',
  DYNAMIC_REFRESH: '每次刷新',
}

/** 空值和历史缺省值都按发布时锁定处理。 */
export function normalizeAudienceSnapshotMode(value?: AudienceSnapshotMode | null | ''): AudienceSnapshotMode {
  return value === 'DYNAMIC_REFRESH' ? 'DYNAMIC_REFRESH' : DEFAULT_AUDIENCE_SNAPSHOT_MODE
}

/** 人群快照模式展示文案。 */
export function snapshotModeLabel(value?: AudienceSnapshotMode | null | '') {
  return SNAPSHOT_MODE_LABELS[normalizeAudienceSnapshotMode(value)]
}
