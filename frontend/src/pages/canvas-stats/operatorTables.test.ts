import { describe, expect, it } from 'vitest'
import { buildOperatorTableQuery, canExportSynchronously, OPERATION_COLUMN } from './operatorTables'

describe('operator table helpers', () => {
  it('drops empty filters and keeps concrete values', () => {
    expect(buildOperatorTableQuery({ canvasId: 10, status: 'FAILED', userId: '', page: 1, size: 20 }))
      .toEqual({ canvasId: 10, status: 'FAILED', page: 1, size: 20 })
  })

  it('blocks synchronous export above limit', () => {
    expect(canExportSynchronously(5000)).toBe(true)
    expect(canExportSynchronously(5001)).toBe(false)
  })

  it('defines a fixed operation column', () => {
    expect(OPERATION_COLUMN).toEqual({ key: 'operation', fixed: 'right', width: 160 })
  })
})
