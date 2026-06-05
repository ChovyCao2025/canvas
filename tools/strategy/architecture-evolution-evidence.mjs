import { readFileSync } from 'node:fs'
import path from 'node:path'

export function validateArchitectureEvolutionEvidence(payload) {
  const required = [
    'key',
    'owner',
    'status',
    'currentCodeEvidence',
    'scaleTrigger',
    'proofCommand',
    'compatibility',
    'rollback',
    'dependencyStatus',
  ]
  const allowed = new Set(['Accepted For Child Spec', 'Needs Evidence', 'Deferred', 'Rejected'])
  const errors = []

  if (payload?.package !== 'p3-003-architecture-evolution') {
    errors.push('package must be p3-003-architecture-evolution')
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
    if (candidate.status === 'Accepted For Child Spec' && !candidate.childSpecPath) {
      errors.push(`${key}: childSpecPath is required for Accepted For Child Spec`)
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
  const file = process.argv[2] || 'docs/product-evolution/discovery/p3-003-architecture-evolution/evidence.json'
  const result = validateArchitectureEvolutionEvidence(readEvidence(file))
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
