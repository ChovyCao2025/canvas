import type { R } from '../types'
import type {
  TechnicalMigrationEvidencePayload,
  TechnicalMigrationEvidenceRecord,
} from '../pages/technical-migration-candidates/technicalMigrationCandidates'
import http from './api'

export const technicalMigrationApi = {
  registerEvidence: (payload: TechnicalMigrationEvidencePayload) =>
    http.post<R<TechnicalMigrationEvidenceRecord>, R<TechnicalMigrationEvidenceRecord>>(
      '/architecture/migration-candidates/evidence',
      payload,
    ),
}
