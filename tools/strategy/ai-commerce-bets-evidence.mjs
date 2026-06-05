import { readFileSync } from 'node:fs'
import path from 'node:path'

export function validateAiCommerceBetsEvidence(payload) {
  const required = [
    'key',
    'owner',
    'status',
    'customerEvidence',
    'dependencyStatus',
    'modelRiskStatus',
    'approvalBoundary',
    'proofCommand',
    'rollback',
  ]
  const allowed = new Set(['Accepted For Child Spec', 'Needs Evidence', 'Deferred', 'Rejected'])
  const errors = []

  if (payload?.package !== 'p3-002-ai-commerce-bets') {
    errors.push('package must be p3-002-ai-commerce-bets')
  }
  if (payload?.rollout?.migration !== 'none') {
    errors.push('rollout.migration must be none')
  }
  if (payload?.rollout?.runtimeChange !== false) {
    errors.push('rollout.runtimeChange must be false')
  }
  if (!Array.isArray(payload?.bets) || payload.bets.length === 0) {
    errors.push('bets must be a non-empty array')
  }

  for (const bet of payload?.bets || []) {
    const key = bet.key || 'unknown'
    for (const field of required) {
      const value = bet[field]
      if (value === undefined || value === '' || (Array.isArray(value) && value.length === 0)) {
        errors.push(`${key}: ${field} is required`)
      }
    }
    if (!allowed.has(bet.status)) {
      errors.push(`${key}: unsupported status ${bet.status}`)
    }
    if (bet.status === 'Accepted For Child Spec' && !bet.childSpecPath) {
      errors.push(`${key}: childSpecPath is required for Accepted For Child Spec`)
    }
  }

  return {
    ok: errors.length === 0,
    errors,
    package: payload?.package,
    betKeys: Array.isArray(payload?.bets) ? payload.bets.map(bet => bet.key) : [],
  }
}

export function readEvidence(file) {
  return JSON.parse(readFileSync(path.resolve(file), 'utf8'))
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const file = process.argv[2] || 'docs/product-evolution/discovery/p3-002-ai-commerce-bets/evidence.json'
  const result = validateAiCommerceBetsEvidence(readEvidence(file))
  if (!result.ok) {
    console.error(result.errors.join('\n'))
    process.exit(1)
  }
  console.log(JSON.stringify({
    ok: true,
    package: result.package,
    betKeys: result.betKeys,
  }, null, 2))
}
