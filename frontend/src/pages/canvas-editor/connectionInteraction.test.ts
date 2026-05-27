/**
 * 测试职责：验证画布连线命中半径大于 React Flow 默认值。
 *
 * 维护说明：节点 handle 尺寸调整时，应重新评估该半径是否仍适合拖线交互。
 */
import { describe, expect, it } from 'vitest'
import { CANVAS_CONNECTION_RADIUS } from './connectionInteraction'

describe('canvas connection interaction', () => {
  it('uses a larger connection radius than the React Flow default', () => {
    expect(CANVAS_CONNECTION_RADIUS).toBeGreaterThan(20)
  })
})
