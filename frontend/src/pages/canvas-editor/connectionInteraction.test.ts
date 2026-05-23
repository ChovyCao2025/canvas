import { describe, expect, it } from 'vitest'
import { CANVAS_CONNECTION_RADIUS } from './connectionInteraction'

describe('canvas connection interaction', () => {
  it('uses a larger connection radius than the React Flow default', () => {
    expect(CANVAS_CONNECTION_RADIUS).toBeGreaterThan(20)
  })
})
