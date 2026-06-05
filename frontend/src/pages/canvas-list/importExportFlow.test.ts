import { describe, expect, it } from 'vitest'
import { buildImportPayload, exportedFileName } from './canvasImportExport'

describe('canvas import export helpers', () => {
  it('builds stable export file names', () => {
    expect(exportedFileName({ id: 62, name: '新客旅程' })).toBe('canvas-62.json')
  })

  it('trims package text', () => {
    expect(buildImportPayload('  {"packageVersion":1}  ')).toEqual({
      packageJson: '{"packageVersion":1}',
    })
  })
})
