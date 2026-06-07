import { afterEach, describe, expect, it, vi } from 'vitest'

import { canStartMigrationText, evidencePayload, migrationCandidateLabel } from './technicalMigrationCandidates'
import http from '../../services/api'
import { technicalMigrationApi } from '../../services/technicalMigrationApi'

describe('technicalMigrationCandidates', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('builds evidence payloads with proof and rollback commands', () => {
    expect(evidencePayload({
      candidateKey: 'rocketmq-topic-split',
      proofCommand: 'mvn test',
      baselineResultJson: '{"baseline":"PASS"}',
      rollbackCommand: 'restore topic config',
    })).toEqual({
      candidateKey: 'rocketmq-topic-split',
      proofCommand: 'mvn test',
      baselineResultJson: '{"baseline":"PASS"}',
      rollbackCommand: 'restore topic config',
      submittedBy: 'frontend',
    })
  })

  it('formats migration candidate labels and gate copy', () => {
    expect(migrationCandidateLabel('spring-mvc-command-dag')).toBe('Spring MVC Command DAG')
    expect(canStartMigrationText(false)).toBe('Blocked until reviewed evidence is approved')
    expect(canStartMigrationText(true)).toBe('Approved for child spec')
  })

  it('registers evidence through the migration candidate API endpoint', async () => {
    const payload = evidencePayload({
      candidateKey: 'powerjob-dynamic-scheduling',
      proofCommand: 'mvn test',
      baselineResultJson: '{"baseline":"PASS"}',
      rollbackCommand: 'disable PowerJob adapter',
    })
    const response = {
      code: 0,
      message: 'success',
      data: { ...payload, decisionStatus: 'BLOCKED_PENDING_REVIEW' as const },
    }
    const post = vi.spyOn(http, 'post').mockResolvedValue(response)

    await expect(technicalMigrationApi.registerEvidence(payload)).resolves.toBe(response)

    expect(post).toHaveBeenCalledWith('/architecture/migration-candidates/evidence', payload)
  })
})
