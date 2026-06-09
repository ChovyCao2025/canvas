package org.chovy.canvas.strategy.privacy;

/**
 * PrivacyComplianceEvidenceService 沉淀战略演进证据的登记、校验和读取规则。
 */
public class PrivacyComplianceEvidenceService {

    private final EvidenceRepository repository;

    /**
     * 初始化 PrivacyComplianceEvidenceService 实例。
     *
     * @param repository 依赖组件，用于完成数据访问或外部能力调用。
     */
    public PrivacyComplianceEvidenceService(EvidenceRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 register 流程生成的业务结果。
     */
    public EvidenceRecord register(EvidenceRequest request) {
        requireText(request.capabilityKey(), "capability key is required");
        requireText(request.ownerId(), "owner is required");
        requireText(request.regulationProfile(), "regulation profile is required");
        requireText(request.affectedDataClasses(), "affected data classes are required");
        requireText(request.auditArtifactNotes(), "audit artifact notes are required");
        requireText(request.residencyImpactNotes(), "residency impact notes are required");
        requireText(request.threatModelNotes(), "threat model notes are required");
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.rollbackNote(), "rollback note is required");

        EvidenceRecord record = new EvidenceRecord(
                request.capabilityKey(), request.ownerId(), request.regulationProfile(),
                request.affectedDataClasses(), request.auditArtifactNotes(),
                request.residencyImpactNotes(), request.threatModelNotes(),
                request.proofCommand(), request.rollbackNote(), "BLOCKED_PENDING_REVIEW");
        repository.insert(record);
        return record;
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param capabilityKey 业务键，用于在同一租户下定位资源。
     * @param reviewerId 业务对象 ID，用于定位具体记录。
     * @param childSpec child spec 参数，用于 approve 流程中的校验、计算或对象转换。
     */
    public void approve(String capabilityKey, String reviewerId, String childSpec) {
        requireText(capabilityKey, "capability key is required");
        requireText(reviewerId, "reviewer is required");
        requireText(childSpec, "child spec is required");
        repository.approve(capabilityKey, reviewerId, childSpec);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * EvidenceRequest 沉淀战略演进证据的登记、校验和读取规则。
     */
    public record EvidenceRequest(
            String capabilityKey,
            String ownerId,
            String regulationProfile,
            String affectedDataClasses,
            String auditArtifactNotes,
            String residencyImpactNotes,
            String threatModelNotes,
            String proofCommand,
            String rollbackNote) {
    }

    /**
     * EvidenceRecord 沉淀战略演进证据的登记、校验和读取规则。
     */
    public record EvidenceRecord(
            String capabilityKey,
            String ownerId,
            String regulationProfile,
            String affectedDataClasses,
            String auditArtifactNotes,
            String residencyImpactNotes,
            String threatModelNotes,
            String proofCommand,
            String rollbackNote,
            String decisionStatus) {
    }

    /**
     * EvidenceRepository 沉淀战略演进证据的登记、校验和读取规则。
     */
    public interface EvidenceRepository {
        void insert(EvidenceRecord record);

        /**
         * 执行业务决策动作，并同步后续状态。
         *
         * @param capabilityKey 业务键，用于在同一租户下定位资源。
         * @param reviewerId 业务对象 ID，用于定位具体记录。
         * @param childSpec child spec 参数，用于 approve 流程中的校验、计算或对象转换。
         */
        void approve(String capabilityKey, String reviewerId, String childSpec);
    }
}
