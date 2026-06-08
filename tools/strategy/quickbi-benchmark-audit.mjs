import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'

const STATUS_CREDIT = {
  implemented: 1,
  partial: 0.5,
  planned: 0,
}

const REMAINING_STATUS_ORDER = ['partial', 'planned']

export function validateQuickBiBenchmark(payload) {
  const errors = []
  const capabilities = Array.isArray(payload?.capabilities) ? payload.capabilities : []
  const threshold = Number(payload?.coverageThresholdPercent ?? 90)

  if (payload?.package !== 'quickbi-platform-benchmark') {
    errors.push('package must be quickbi-platform-benchmark')
  }
  if (!Number.isFinite(threshold) || threshold < 0 || threshold > 100) {
    errors.push('coverageThresholdPercent must be between 0 and 100')
  }
  if (!Array.isArray(payload?.officialSources) || payload.officialSources.length < 3) {
    errors.push('officialSources must include at least three official references')
  }
  if (capabilities.length < 20) {
    errors.push('capabilities must include at least 20 benchmark rows')
  }

  let totalWeight = 0
  let coveredWeight = 0
  const capabilityKeys = []
  const remainingStatuses = new Set()

  for (const capability of capabilities) {
    const key = capability?.key || 'unknown'
    capabilityKeys.push(key)
    const weight = Number(capability?.weight)
    const status = capability?.status
    const evidence = Array.isArray(capability?.canvasEvidence) ? capability.canvasEvidence : []

    if (!capability?.key) errors.push('capability key is required')
    if (!capability?.officialCapability) errors.push(`${key}: officialCapability is required`)
    if (!Number.isFinite(weight) || weight <= 0) errors.push(`${key}: weight must be positive`)
    if (!Object.hasOwn(STATUS_CREDIT, status)) errors.push(`${key}: unsupported status ${status}`)
    if (evidence.length === 0) errors.push(`${key}: canvasEvidence is required`)

    for (const item of evidence) {
      if (typeof item !== 'string' || item.trim() === '') {
        errors.push(`${key}: canvasEvidence contains an empty item`)
        continue
      }
      if (!item.startsWith('http') && !existsSync(path.resolve(item))) {
        errors.push(`${key}: evidence path is missing: ${item}`)
      }
    }

    if (Number.isFinite(weight) && weight > 0 && Object.hasOwn(STATUS_CREDIT, status)) {
      totalWeight += weight
      coveredWeight += weight * STATUS_CREDIT[status]
    }
    if (status !== 'implemented') remainingStatuses.add(status)
  }

  const coveragePercent = totalWeight > 0
    ? Number(((coveredWeight / totalWeight) * 100).toFixed(1))
    : 0
  const passesThreshold = coveragePercent >= threshold
  if (!passesThreshold) {
    errors.push(`coverage ${coveragePercent}% is below threshold ${threshold}`)
  }

  return {
    ok: errors.length === 0,
    errors,
    package: payload?.package,
    thresholdPercent: threshold,
    coveragePercent,
    passesThreshold,
    capabilityCount: capabilities.length,
    capabilityKeys,
    remainingStatuses: REMAINING_STATUS_ORDER.filter(status => remainingStatuses.has(status)),
  }
}

export function readBenchmark(file) {
  return JSON.parse(readFileSync(path.resolve(file), 'utf8'))
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const file = process.argv[2] || 'docs/superpowers/evidence/quickbi-capability-benchmark.json'
  const result = validateQuickBiBenchmark(readBenchmark(file))
  if (!result.ok) {
    console.error(result.errors.join('\n'))
    process.exit(1)
  }
  console.log(JSON.stringify({
    ok: true,
    package: result.package,
    thresholdPercent: result.thresholdPercent,
    coveragePercent: result.coveragePercent,
    passesThreshold: result.passesThreshold,
    capabilityCount: result.capabilityCount,
    capabilityKeys: result.capabilityKeys,
    remainingStatuses: result.remainingStatuses,
  }, null, 2))
}
