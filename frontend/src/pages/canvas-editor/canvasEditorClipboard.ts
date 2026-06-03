/**
 * 工具职责：画布编辑器复制粘贴时的节点配置克隆和引用清理。
 *
 * 维护说明：粘贴节点必须得到独立 bizConfig，避免修改副本时污染源节点。
 */
import type { BizConfig } from '../../types/canvas'

/** 清理指向指定节点集合的后继引用，删除节点和粘贴节点都复用这套规则。 */
export function cleanCanvasBizConfigRefs(cfg: Record<string, unknown>, deletedIds: Set<string>): BizConfig {
  /** 如果引用目标已被删除或不应复制，则把该引用字段清空。 */
  const clean = (v: unknown) => (typeof v === 'string' && deletedIds.has(v) ? undefined : v)
  const biz = cfg as BizConfig
  return {
    ...cfg,
    nextNodeId: clean(biz.nextNodeId) as string | undefined,
    successNodeId: clean(biz.successNodeId) as string | undefined,
    failNodeId: clean(biz.failNodeId) as string | undefined,
    elseNodeId: clean(biz.elseNodeId) as string | undefined,
    approveNodeId: clean(biz.approveNodeId) as string | undefined,
    rejectNodeId: clean(biz.rejectNodeId) as string | undefined,
    hitNextNodeId: clean(biz.hitNextNodeId) as string | undefined,
    missNextNodeId: clean(biz.missNextNodeId) as string | undefined,
    timeoutNodeId: clean(biz.timeoutNodeId) as string | undefined,
    suppressedNodeId: clean(biz.suppressedNodeId) as string | undefined,
    skippedNodeId: clean(biz.skippedNodeId) as string | undefined,
    allowedNodeId: clean(biz.allowedNodeId) as string | undefined,
    quietNodeId: clean(biz.quietNodeId) as string | undefined,
    availableNodeId: clean(biz.availableNodeId) as string | undefined,
    unavailableNodeId: clean(biz.unavailableNodeId) as string | undefined,
    passNodeId: clean(biz.passNodeId) as string | undefined,
    cappedNodeId: clean(biz.cappedNodeId) as string | undefined,
    fallbackNodeId: clean(biz.fallbackNodeId) as string | undefined,
    exitNodeId: clean(biz.exitNodeId) as string | undefined,
    loopStartNodeId: clean(biz.loopStartNodeId) as string | undefined,
    targetNodeId: clean(biz.targetNodeId) as string | undefined,
    maxExceededNodeId: clean(biz.maxExceededNodeId) as string | undefined,
    goalMetNodeId: clean(biz.goalMetNodeId) as string | undefined,
    goalNotMetNodeId: clean(biz.goalNotMetNodeId) as string | undefined,
    branches: biz.branches?.map(branch => ({
      ...branch,
      nextNodeId: clean(branch.nextNodeId) as string | undefined,
    })),
    priorities: biz.priorities?.map(priority => ({
      ...priority,
      nextNodeId: clean(priority.nextNodeId) as string | undefined,
    })),
    groups: biz.groups?.map(group => ({
      ...group,
      nextNodeId: clean(group.nextNodeId) as string | undefined,
    })),
    paths: biz.paths?.map(path => ({
      ...path,
      nextNodeId: clean(path.nextNodeId) as string | undefined,
    })),
    variants: biz.variants?.map(variant => ({
      ...variant,
      nextNodeId: clean(variant.nextNodeId) as string | undefined,
    })),
    bands: biz.bands?.map(band => ({
      ...band,
      nextNodeId: clean(band.nextNodeId) as string | undefined,
    })),
  }
}

/** 深拷贝粘贴节点配置，并清理复制时不应保留的旧画布连线目标。 */
export function cloneCanvasNodeBizConfigForPaste(
  cfg: Record<string, unknown> | undefined,
  existingIds: Set<string>,
): BizConfig {
  const source = cfg ?? {}
  const cloned = typeof structuredClone === 'function'
    ? structuredClone(source) as Record<string, unknown>
    : JSON.parse(JSON.stringify(source)) as Record<string, unknown>
  return cleanCanvasBizConfigRefs(cloned, existingIds)
}
