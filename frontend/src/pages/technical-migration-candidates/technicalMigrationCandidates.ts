export interface TechnicalMigrationEvidencePayload {
  candidateKey: string
  proofCommand: string
  baselineResultJson: string
  rollbackCommand: string
  submittedBy: string
}

export interface TechnicalMigrationEvidenceRecord extends TechnicalMigrationEvidencePayload {
  decisionStatus: 'BLOCKED_PENDING_REVIEW' | 'APPROVED_FOR_CHILD_SPEC' | 'REJECTED'
}

export function evidencePayload(input: Omit<TechnicalMigrationEvidencePayload, 'submittedBy'>): TechnicalMigrationEvidencePayload {
  return { ...input, submittedBy: 'frontend' }
}

export function migrationCandidateLabel(candidateKey: string) {
  const acronyms = new Set(['DAG', 'MVC', 'MQ'])

  return candidateKey
    .split('-')
    .map((part) => {
      const upper = part.toUpperCase()
      return acronyms.has(upper) ? upper : part.charAt(0).toUpperCase() + part.slice(1)
    })
    .join(' ')
}

export function canStartMigrationText(canStart: boolean) {
  return canStart ? 'Approved for child spec' : 'Blocked until reviewed evidence is approved'
}
