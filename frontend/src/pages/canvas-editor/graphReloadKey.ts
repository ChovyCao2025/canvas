/**
 * 页面职责：画布图重载 key 生成工具。
 *
 * 维护说明：编辑器用它判断服务端版本或 graphJson 内容是否变化。
 */
import type { CanvasDetail } from '../../types'

/**
 * 构造画布图重载 key：
 * 只要 draftVersionId 或 graphJson 变化，依赖该 key 的 effect 会重新执行。
 *
 * <p>主要用于编辑器页的“拉取详情后重建节点/边”流程。
 * 避免仅比较对象引用导致的漏刷新。
 *
 * 为什么不用 `detail` 对象本身做依赖：
 * - 请求层/状态层可能复用对象引用；
 * - 但版本号或图 JSON 已变化，仍然必须强制重建画布。
 */
export function getCanvasGraphReloadKey(detail: CanvasDetail): string {
  // `draftVersionId` 为空时用固定占位符，保证 key 字符串结构稳定
  // 用 "version:content" 组合，保证版本变化和内容变化都能触发重载
  // 调用方一般把该值放到 useEffect/useMemo 依赖数组里
  // 注意：graphJson 可能较长，这里只用于比较，不做展示。
  // 若后续担心 key 过长，可改成 hash(detail.graphJson)。
  return `${detail.draftVersionId ?? 'none'}:${detail.graphJson}`
}
