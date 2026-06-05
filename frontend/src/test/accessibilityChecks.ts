export interface AccessibilityControlContract {
  id: string
  role: string
  name?: string
  labelledBy?: string
  focusable?: boolean
}

export function findAccessibilityIssues(controls: AccessibilityControlContract[]): string[] {
  const issues: string[] = []
  const ids = new Set<string>()

  for (const control of controls) {
    if (!control.id.trim()) {
      issues.push('control id is required')
      continue
    }
    if (ids.has(control.id)) {
      issues.push(`${control.id}: duplicate control id`)
    }
    ids.add(control.id)

    if (!control.role.trim()) {
      issues.push(`${control.id}: role is required`)
    }

    const hasName = Boolean(control.name?.trim())
    const hasLabelledBy = Boolean(control.labelledBy?.trim())
    if (!hasName && !hasLabelledBy) {
      issues.push(`${control.id}: accessible name is required`)
    }

    if (control.focusable !== false && control.focusable !== true) {
      issues.push(`${control.id}: focusable metadata is required`)
    }
  }

  return issues
}

export function findMissingAccessibleNames(html: string, names: string[]): string[] {
  return names.filter(name => {
    const escaped = escapeAttributeValue(name)
    return !html.includes(`aria-label="${escaped}"`) &&
      !html.includes(`aria-label='${escaped}'`) &&
      !html.includes(`>${name}<`)
  })
}

function escapeAttributeValue(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}
