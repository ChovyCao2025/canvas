package org.chovy.canvas.domain.collaboration;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * CanvasCollaborationSummaryService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CanvasCollaborationSummaryService {

    private final SummaryRepository repository;

    /**
     * 初始化 CanvasCollaborationSummaryService 实例。
     */
    public CanvasCollaborationSummaryService() {
        this((tenantId, canvasId) -> new Summary(canvasId, List.of(), 0, 0, 0));
    }

    /**
     * 初始化 CanvasCollaborationSummaryService 实例。
     *
     * @param repository 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CanvasCollaborationSummaryService(SummaryRepository repository) {
        this.repository = repository;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回 summary 流程生成的业务结果。
     */
    public Summary summary(Long tenantId, Long canvasId) {
        Summary summary = repository.summary(tenantId, canvasId);
        return summary == null ? new Summary(canvasId, List.of(), 0, 0, 0) : summary;
    }

    /**
     * Summary 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record Summary(Long canvasId,
                          List<Presence> presence,
                          int activeLockCount,
                          int openCommentCount,
                          int unreadNotificationCount) {
        public Summary {
            presence = presence == null ? List.of() : List.copyOf(presence);
        }
    }

    /**
     * Presence 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record Presence(String userId, String displayName, String state) {
    }

    /**
     * SummaryRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    public interface SummaryRepository {
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param canvasId 业务对象 ID，用于定位具体记录。
         * @return 返回 summary 流程生成的业务结果。
         */
        Summary summary(Long tenantId, Long canvasId);
    }
}
