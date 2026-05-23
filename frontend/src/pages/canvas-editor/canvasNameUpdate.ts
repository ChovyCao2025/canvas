export type CanvasNameUpdate =
  | {
      /** 可提交的画布名称。 */
      name: string
    }
  | {
      /** 名称未变化。 */
      unchanged: true
    }
  | {
      /** 校验失败信息。 */
      error: string
    }

/**
 * 构造画布名称更新 payload。
 * 返回联合类型而非抛错，方便调用方在 UI 层显式分支处理。
 *
 * 分支语义：
 * - `{ name }`：可提交；
 * - `{ unchanged: true }`：输入合法但无需请求后端；
 * - `{ error }`：输入非法，前端直接提示。
 */
export function buildCanvasNameUpdate(inputName: string, savedName: string): CanvasNameUpdate {
  // 统一 trim，避免“仅空格变化”触发无意义更新
  const name = inputName.trim()
  if (!name) return { error: '画布名称不能为空' }
  if (name === savedName) return { unchanged: true }
  return { name }
}

/** 名称进入编辑态时才展示“保存/取消”操作。 */
export function shouldShowCanvasNameActions(isEditing: boolean): boolean {
  // 只在编辑态渲染动作按钮，降低非编辑态视觉噪音
  return isEditing
}

/** 编辑态给状态区域留出操作按钮间距；只读态不占位。 */
export function getCanvasNameStatusGap(isEditing: boolean): number {
  // 编辑态预留按钮空间，防止状态文案跳动
  return isEditing ? 16 : 0
}
