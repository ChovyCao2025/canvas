#!/usr/bin/env node
import { existsSync, readFileSync, statSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const repoRoot = resolve(__dirname, '../..')

export const REQUIRED_CANDIDATES = [
  'runtime-web-stack',
  'dag-engine-execution-model',
  'delivery-and-mq-topic-split',
  'script-engine-sandbox',
  'audience-bitmap-identity-mapping',
  'trace-olap-storage',
  'service-split-boundaries',
]

const evidenceFiles = {
  'runtime-web-stack': ['backend/canvas-engine/pom.xml'],
  'dag-engine-execution-model': ['backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java'],
  'delivery-and-mq-topic-split': ['backend/canvas-engine/pom.xml', 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java'],
  'script-engine-sandbox': ['backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java'],
  'audience-bitmap-identity-mapping': ['backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java'],
  'trace-olap-storage': ['backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java'],
  'service-split-boundaries': ['backend/canvas-engine/src/main/java/org/chovy/canvas/web', 'backend/canvas-engine/src/main/java/org/chovy/canvas/domain', 'backend/canvas-engine/src/main/java/org/chovy/canvas/engine'],
}

const candidateConfig = {
  'runtime-web-stack': {
    riskLevel: 'HIGH',
    dependencyStatus: 'blocked-by-production-safety',
    decisionStatus: 'Proof Required',
    proofCommand: 'mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest',
    rollbackNote: 'Restore WebFlux dependencies and route handlers.',
  },
  'dag-engine-execution-model': {
    riskLevel: 'VERY_HIGH',
    dependencyStatus: 'blocked-by-idempotency-and-context-gates',
    decisionStatus: 'Deferred',
    proofCommand: 'mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest',
    rollbackNote: 'Keep current DagEngine as production path behind feature flag.',
  },
  'delivery-and-mq-topic-split': {
    riskLevel: 'HIGH',
    dependencyStatus: 'merged-into-p0-003-before-topic-split',
    decisionStatus: 'Merged Into Existing Spec',
    proofCommand: 'node tools/perf/runtime-migration-baseline.mjs --format json',
    rollbackNote: 'Disable split route registration and replay from outbox.',
  },
  'script-engine-sandbox': {
    riskLevel: 'HIGH',
    dependencyStatus: 'requires-script-compatibility-corpus',
    decisionStatus: 'Proof Required',
    proofCommand: 'mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest',
    rollbackNote: 'Route script nodes back to GroovyHandler.',
  },
  'audience-bitmap-identity-mapping': {
    riskLevel: 'HIGH',
    dependencyStatus: 'requires-cdp-identity-evidence',
    decisionStatus: 'Proof Required',
    proofCommand: 'mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest',
    rollbackNote: 'Dual-read old bitmap keys and stop new backfill jobs.',
  },
  'trace-olap-storage': {
    riskLevel: 'MEDIUM_HIGH',
    dependencyStatus: 'blocked-by-p2-016-trace-sink',
    decisionStatus: 'Deferred',
    proofCommand: 'mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest',
    rollbackNote: 'Disable Doris load and query MySQL trace fallback.',
  },
  'service-split-boundaries': {
    riskLevel: 'VERY_HIGH',
    dependencyStatus: 'blocked-by-3000-4000-hardening',
    decisionStatus: 'Deferred',
    proofCommand: 'node tools/perf/runtime-migration-baseline.mjs --format json',
    rollbackNote: 'Route all traffic back to the single Spring Boot artifact.',
  },
}

export function buildReport({ root = repoRoot, generatedAt = new Date().toISOString() } = {}) {
  const candidates = REQUIRED_CANDIDATES.map(key => {
    const files = evidenceFiles[key] ?? []
    const missing = files.filter(file => !existsSync(resolve(root, file)))
    const config = candidateConfig[key]
    const sourceEvidence = files.map(file => ({
      path: file,
      available: !missing.includes(file),
      bytes: existsSync(resolve(root, file)) && statSync(resolve(root, file)).isFile()
        ? readFileSync(resolve(root, file)).length
        : null,
    }))
    return {
      key,
      sourceEvidence,
      proofCommands: [{ command: config.proofCommand, expected: 'PASS before child spec starts' }],
      riskLevel: config.riskLevel,
      dependencyStatus: config.dependencyStatus,
      decisionStatus: config.decisionStatus,
      rollbackNote: config.rollbackNote,
      valid: missing.length === 0 && Boolean(config.proofCommand) && Boolean(config.rollbackNote),
    }
  })
  return {
    generatedAt,
    candidates,
    summary: {
      total: candidates.length,
      valid: candidates.filter(candidate => candidate.valid).length,
      invalid: candidates.filter(candidate => !candidate.valid).length,
    },
  }
}

export function validateReport(report) {
  const keys = new Set(report.candidates?.map(candidate => candidate.key))
  for (const key of REQUIRED_CANDIDATES) {
    if (!keys.has(key)) {
      throw new Error(`missing candidate: ${key}`)
    }
  }
  for (const candidate of report.candidates ?? []) {
    if (!candidate.sourceEvidence?.length) throw new Error(`missing source evidence: ${candidate.key}`)
    if (!candidate.proofCommands?.length) throw new Error(`missing proof command: ${candidate.key}`)
    if (!candidate.rollbackNote) throw new Error(`missing rollback note: ${candidate.key}`)
    if (!candidate.sourceEvidence.every(item => item.available)) {
      throw new Error(`source evidence unavailable: ${candidate.key}`)
    }
  }
  return true
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const report = buildReport()
  try {
    validateReport(report)
  } catch (error) {
    console.error(JSON.stringify({ error: error.message, report }, null, 2))
    process.exit(1)
  }
  console.log(JSON.stringify(report, null, process.argv.includes('--format') ? 2 : 0))
}
