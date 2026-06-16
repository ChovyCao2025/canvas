package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.PlatformActor;
import org.chovy.canvas.platform.api.TechnicalMigrationCandidateFacade;
import org.chovy.canvas.platform.api.TechnicalMigrationEvidenceRequest;
import org.chovy.canvas.platform.api.TechnicalMigrationEvidenceView;
import org.chovy.canvas.platform.domain.TechnicalMigrationCandidateEvidence;
import org.chovy.canvas.platform.domain.TechnicalMigrationCandidateEvidenceRepository;
import org.chovy.canvas.platform.domain.TechnicalMigrationDecisionStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

/**
 * 处理技术迁移候选项证据登记和迁移准入判断的应用服务。
 */
@Service
public class TechnicalMigrationCandidateApplicationService implements TechnicalMigrationCandidateFacade {

    /**
     * 持久化技术迁移候选项证据的仓储。
     */
    private final TechnicalMigrationCandidateEvidenceRepository repository;

    /**
     * 使用迁移证据仓储创建应用服务。
     *
     * @param repository 持久化技术迁移候选项证据的仓储
     */
    public TechnicalMigrationCandidateApplicationService(TechnicalMigrationCandidateEvidenceRepository repository) {
        this.repository = repository;
    }

    /**
     * 登记技术迁移候选项证据。
     *
     * {@inheritDoc}
     */
    @Override
    public TechnicalMigrationEvidenceView register(
            PlatformActor actor,
            TechnicalMigrationEvidenceRequest request) {
        Long tenantId = requireTenantId(actor);
        String candidateKey = normalizeCandidateKey(request.candidateKey());
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.baselineResultJson(), "baseline result is required");
        requireText(request.rollbackCommand(), "rollback command is required");

        // 新提交的迁移证据必须先进入人工复核状态，防止未经评审直接执行迁移。
        TechnicalMigrationCandidateEvidence record = new TechnicalMigrationCandidateEvidence(
                tenantId,
                candidateKey,
                request.proofCommand().trim(),
                request.baselineResultJson().trim(),
                request.rollbackCommand().trim(),
                TechnicalMigrationDecisionStatus.BLOCKED_PENDING_REVIEW,
                submittedBy(actor));
        repository.insert(record);
        return toView(record);
    }

    /**
     * 判断迁移候选项是否满足启动条件。
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canStartMigration(PlatformActor actor, String candidateKey) {
        Long tenantId = requireTenantId(actor);
        TechnicalMigrationCandidateEvidence latest =
                repository.latest(tenantId, normalizeCandidateKey(candidateKey));
        return latest != null
                && latest.decisionStatus() == TechnicalMigrationDecisionStatus.APPROVED_FOR_CHILD_SPEC;
    }

    /**
     * 将领域迁移证据转换为公开 API 视图。
     *
     * @param record 领域迁移证据
     * @return 公开 API 视图
     */
    private static TechnicalMigrationEvidenceView toView(TechnicalMigrationCandidateEvidence record) {
        return new TechnicalMigrationEvidenceView(
                record.tenantId(),
                record.candidateKey(),
                record.proofCommand(),
                record.baselineResultJson(),
                record.rollbackCommand(),
                record.decisionStatus().name(),
                record.submittedBy());
    }

    /**
     * 从操作者上下文中读取租户标识。
     *
     * @param actor 操作者上下文
     * @return 租户标识
     * @throws SecurityException 当操作者或租户上下文缺失时抛出
     */
    private static Long requireTenantId(PlatformActor actor) {
        if (actor == null || actor.tenantId() == null) {
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return actor.tenantId();
    }

    /**
     * 计算提交人展示值。
     *
     * @param actor 操作者上下文
     * @return 提交人名称；用户名缺失时返回 unknown
     */
    private static String submittedBy(PlatformActor actor) {
        if (actor.username() == null || actor.username().isBlank()) {
            return "unknown";
        }
        return actor.username().trim();
    }

    /**
     * 规范化迁移候选项键。
     *
     * @param candidateKey 原始迁移候选项键
     * @return 小写且通过格式校验的迁移候选项键
     * @throws IllegalArgumentException 当候选项键为空或格式非法时抛出
     */
    private static String normalizeCandidateKey(String candidateKey) {
        requireText(candidateKey, "candidate key is required");
        String normalized = Objects.requireNonNull(candidateKey).trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9-]{0,127}")) {
            // 与工作流键保持同一套 URL 友好格式，方便跨文档和路由引用。
            throw new IllegalArgumentException("invalid candidate key: " + candidateKey);
        }
        return normalized;
    }

    /**
     * 校验文本字段必须存在且不能只包含空白字符。
     *
     * @param value 待校验文本
     * @param message 校验失败时使用的异常消息
     * @throws IllegalArgumentException 当文本为空或空白时抛出
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
