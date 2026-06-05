import type { Canvas } from '../../types'

export function exportedFileName(canvas: Pick<Canvas, 'id' | 'name'>) {
  return `canvas-${canvas.id}.json`
}

export function buildImportPayload(packageText: string) {
  return { packageJson: packageText.trim() }
}
