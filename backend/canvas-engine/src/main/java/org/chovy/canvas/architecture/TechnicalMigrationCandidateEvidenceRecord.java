package org.chovy.canvas.architecture;

/**
 * TechnicalMigrationCandidateEvidenceRecord 承载 architecture 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param candidateKey candidateKey 字段。
 * @param proofCommand proofCommand 字段。
 * @param baselineResultJson baselineResultJson 字段。
 * @param rollbackCommand rollbackCommand 字段。
 * @param decisionStatus decisionStatus 字段。
 * @param submittedBy submittedBy 字段。
 */
public record TechnicalMigrationCandidateEvidenceRecord(
        Long tenantId,
        String candidateKey,
        String proofCommand,
        String baselineResultJson,
        String rollbackCommand,
        String decisionStatus,
        String submittedBy) {
}
