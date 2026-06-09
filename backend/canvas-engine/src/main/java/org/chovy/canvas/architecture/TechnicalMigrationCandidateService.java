package org.chovy.canvas.architecture;

import org.chovy.canvas.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

/**
 * TechnicalMigrationCandidateService 支撑 architecture 场景的后端处理。
 */
@Service
public class TechnicalMigrationCandidateService {

    /** 候选迁移已提交证据但仍需人工复核，禁止生成或执行子规格。 */
    public static final String BLOCKED_PENDING_REVIEW = "BLOCKED_PENDING_REVIEW";
    /** 候选迁移证据已通过复核，可以进入子规格拆分和执行阶段。 */
    public static final String APPROVED_FOR_CHILD_SPEC = "APPROVED_FOR_CHILD_SPEC";

    /** 迁移证据仓储，隔离数据库实现并便于在测试中替换。 */
    private final EvidenceRepository repository;

    /**
     * 创建 TechnicalMigrationCandidateService 实例并注入 architecture 场景依赖。
     * @param repository 依赖组件，用于完成数据访问或外部能力调用。
     */
    public TechnicalMigrationCandidateService(EvidenceRepository repository) {
        this.repository = repository;
    }

    /**
     * 登记技术迁移候选项的可回滚证据。
     *
     * <p>提交时强制要求证明命令、基线结果和回滚命令齐全，并将状态固定为待复核，
     * 防止未经人工审查的迁移直接进入执行规格。
     *
     * @param tenant 当前租户上下文，用于隔离候选项证据
     * @param request 候选项 key、证明命令、基线结果和回滚命令
     * @return 已写入仓储的证据记录
     */
    public TechnicalMigrationCandidateEvidenceRecord register(TenantContext tenant, EvidenceRequest request) {
        Long tenantId = requireTenantId(tenant);
        String candidateKey = normalizeCandidateKey(request.candidateKey());
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.baselineResultJson(), "baseline result is required");
        requireText(request.rollbackCommand(), "rollback command is required");

        TechnicalMigrationCandidateEvidenceRecord record = new TechnicalMigrationCandidateEvidenceRecord(
                tenantId,
                candidateKey,
                request.proofCommand().trim(),
                request.baselineResultJson().trim(),
                request.rollbackCommand().trim(),
                BLOCKED_PENDING_REVIEW,
                submittedBy(tenant));
        repository.insert(record);
        return record;
    }

    /**
     * 判断指定候选迁移是否已经通过复核并允许启动。
     *
     * <p>只读取同租户下最新证据记录，且必须处于 {@link #APPROVED_FOR_CHILD_SPEC}
     * 状态才返回 true；缺失记录或仍待复核时均视为不可启动。
     *
     * @param tenant 当前租户上下文
     * @param candidateKey 技术迁移候选项稳定 key
     * @return true 表示可以继续创建子规格或触发迁移执行
     */
    public boolean canStartMigration(TenantContext tenant, String candidateKey) {
        Long tenantId = requireTenantId(tenant);
        TechnicalMigrationCandidateEvidenceRecord latest = repository.latest(tenantId, normalizeCandidateKey(candidateKey));
        return latest != null && APPROVED_FOR_CHILD_SPEC.equals(latest.decisionStatus());
    }

    /**
     * 校验租户上下文中必须存在租户 ID。
     *
     * @param tenant 当前租户上下文
     * @return 已确认非空的租户 ID
     */
    private static Long requireTenantId(TenantContext tenant) {
        if (tenant == null || tenant.tenantId() == null) {
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return tenant.tenantId();
    }

    /**
     * 从租户上下文提取提交人名称。
     *
     * @param tenant 当前租户上下文
     * @return 去除首尾空白后的用户名，缺失时返回 unknown
     */
    private static String submittedBy(TenantContext tenant) {
        if (tenant.username() == null || tenant.username().isBlank()) {
            return "unknown";
        }
        return tenant.username().trim();
    }

    /**
     * 规范化技术迁移候选项 key。
     *
     * @param candidateKey 原始候选项 key
     * @return 小写并通过格式校验的候选项 key
     */
    private static String normalizeCandidateKey(String candidateKey) {
        requireText(candidateKey, "candidate key is required");
        String normalized = Objects.requireNonNull(candidateKey).trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9-]{0,127}")) {
            throw new IllegalArgumentException("invalid candidate key: " + candidateKey);
        }
        return normalized;
    }

    /**
     * 校验文本字段不能为空。
     *
     * @param value 待校验的文本
     * @param message 校验失败时抛出的错误信息
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 技术迁移证据登记请求。
     *
     * @param candidateKey 候选迁移稳定 key，使用小写字母、数字和连字符
     * @param proofCommand 证明迁移必要性或安全性的可复现命令
     * @param baselineResultJson 执行证明命令得到的基线结果 JSON
     * @param rollbackCommand 迁移失败时可执行的回滚命令
     * @param submittedBy 提交人标识，当前由租户上下文兜底
     */
    public record EvidenceRequest(
            String candidateKey,
            String proofCommand,
            String baselineResultJson,
            String rollbackCommand,
            String submittedBy) {
    }

    /**
     * 技术迁移证据仓储端口。
     *
     * <p>服务层只依赖插入和读取最新记录两个能力，具体持久化可由 JDBC、MyBatis
     * 或测试内存实现提供。
     */
    public interface EvidenceRepository {
        /**
         * 写入一条候选迁移证据记录。
         *
         * @param record 已完成租户、候选 key 和复核状态归一化的证据记录
         */
        void insert(TechnicalMigrationCandidateEvidenceRecord record);

        /**
         * 查询指定租户和候选 key 的最新证据。
         *
         * @param tenantId 租户 ID
         * @param candidateKey 已归一化的候选迁移 key
         * @return 最新证据记录；不存在时返回 null
         */
        TechnicalMigrationCandidateEvidenceRecord latest(Long tenantId, String candidateKey);
    }
}
