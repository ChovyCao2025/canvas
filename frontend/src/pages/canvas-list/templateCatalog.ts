import type { Canvas } from '../../types'
import type { CanvasTemplate } from '../../services/api'

export function buildTemplateCategoryOptions(templates: CanvasTemplate[]) {
  return Array.from(new Set(templates.map(template => template.category).filter(Boolean) as string[]))
    .sort()
    .map(value => ({ label: value, value }))
}

export function buildTemplateCloneSuccessMessage(canvas: Pick<Canvas, 'id' | 'name'>) {
  return `已从模板创建「${canvas.name}」(ID: ${canvas.id})`
}
