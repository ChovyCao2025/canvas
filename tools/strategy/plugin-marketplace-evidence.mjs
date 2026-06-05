import { readFileSync } from 'node:fs'
import path from 'node:path'

export function validatePluginMarketplaceEvidence(payload) {
  const required = ['key', 'owner', 'status', 'evidence', 'proofCommand', 'launchGate', 'rollback', 'dependencies']
  const allowed = new Set(['Accepted For Child Spec', 'Needs Evidence', 'Deferred', 'Rejected'])
  const errors = []

  if (payload?.package !== 'p3-001-plugin-marketplace') {
    errors.push('package must be p3-001-plugin-marketplace')
  }
  if (payload?.rollout?.migration !== 'none') {
    errors.push('rollout.migration must be none')
  }
  if (payload?.rollout?.runtimeChange !== false) {
    errors.push('rollout.runtimeChange must be false')
  }
  if (!Array.isArray(payload?.candidates) || payload.candidates.length === 0) {
    errors.push('candidates must be a non-empty array')
  }

  for (const candidate of payload?.candidates || []) {
    const key = candidate.key || 'unknown'
    for (const field of required) {
      const value = candidate[field]
      if (value === undefined || value === '' || (Array.isArray(value) && value.length === 0)) {
        errors.push(`${key}: ${field} is required`)
      }
    }
    if (!allowed.has(candidate.status)) {
      errors.push(`${key}: unsupported status ${candidate.status}`)
    }
    if (candidate.status === 'Accepted For Child Spec') {
      if (!candidate.childSpecPath) {
        errors.push(`${key}: childSpecPath is required for Accepted For Child Spec`)
      }
      const dependencies = Array.isArray(candidate.dependencies) ? candidate.dependencies : []
      if (!dependencies.some(dependency => String(dependency).includes('P2-002'))) {
        errors.push(`${key}: P2-002 plugin foundations dependency is required for Accepted For Child Spec`)
      }
    }
  }

  return {
    ok: errors.length === 0,
    errors,
    package: payload?.package,
    candidateKeys: Array.isArray(payload?.candidates)
      ? payload.candidates.map(candidate => candidate.key)
      : [],
  }
}

export function readEvidence(file) {
  return JSON.parse(readFileSync(path.resolve(file), 'utf8'))
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const file = process.argv[2] || 'docs/product-evolution/discovery/p3-001-plugin-marketplace/evidence.json'
  const result = validatePluginMarketplaceEvidence(readEvidence(file))
  if (!result.ok) {
    console.error(result.errors.join('\n'))
    process.exit(1)
  }
  console.log(JSON.stringify({
    ok: true,
    package: result.package,
    candidateKeys: result.candidateKeys,
  }, null, 2))
}
