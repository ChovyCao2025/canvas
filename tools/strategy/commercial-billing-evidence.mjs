import { readFileSync } from 'node:fs'
import path from 'node:path'

export function validateCommercialBillingEvidence(payload) {
  const required = [
    'key',
    'owner',
    'status',
    'metricDefinition',
    'sourceEvidence',
    'financeGate',
    'legalGate',
    'supportGate',
    'proofCommand',
    'rollback',
  ]
  const allowed = new Set(['Accepted For Child Spec', 'Needs Evidence', 'Deferred', 'Rejected'])
  const errors = []

  if (payload?.package !== 'p3-004-commercial-billing') {
    errors.push('package must be p3-004-commercial-billing')
  }
  if (payload?.rollout?.migration !== 'none') {
    errors.push('rollout.migration must be none')
  }
  if (payload?.rollout?.runtimeChange !== false) {
    errors.push('rollout.runtimeChange must be false')
  }
  if (payload?.rollout?.customerCharging !== false) {
    errors.push('rollout.customerCharging must be false')
  }
  if (!Array.isArray(payload?.capabilities) || payload.capabilities.length === 0) {
    errors.push('capabilities must be a non-empty array')
  }

  for (const capability of payload?.capabilities || []) {
    const key = capability.key || 'unknown'
    for (const field of required) {
      const value = capability[field]
      if (value === undefined || value === '' || (Array.isArray(value) && value.length === 0)) {
        errors.push(`${key}: ${field} is required`)
      }
    }
    if (!allowed.has(capability.status)) {
      errors.push(`${key}: unsupported status ${capability.status}`)
    }
    if (capability.status === 'Accepted For Child Spec' && !capability.childSpecPath) {
      errors.push(`${key}: childSpecPath is required for Accepted For Child Spec`)
    }
  }

  return {
    ok: errors.length === 0,
    errors,
    package: payload?.package,
    capabilityKeys: Array.isArray(payload?.capabilities)
      ? payload.capabilities.map(capability => capability.key)
      : [],
  }
}

export function readEvidence(file) {
  return JSON.parse(readFileSync(path.resolve(file), 'utf8'))
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const file = process.argv[2] || 'docs/product-evolution/discovery/p3-004-commercial-billing/evidence.json'
  const result = validateCommercialBillingEvidence(readEvidence(file))
  if (!result.ok) {
    console.error(result.errors.join('\n'))
    process.exit(1)
  }
  console.log(JSON.stringify({
    ok: true,
    package: result.package,
    capabilityKeys: result.capabilityKeys,
  }, null, 2))
}
