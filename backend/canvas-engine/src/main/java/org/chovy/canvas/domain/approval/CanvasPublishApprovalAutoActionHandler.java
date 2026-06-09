package org.chovy.canvas.domain.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.springframework.stereotype.Component;

/**
 * CanvasPublishApprovalAutoActionHandler 编排 domain.approval 场景的领域业务规则。
 */
@Component
public class CanvasPublishApprovalAutoActionHandler implements ApprovalAutoActionHandler {

    private final CanvasVersionMapper canvasVersionMapper;
    private final CanvasService canvasService;

    /**
     * 创建 CanvasPublishApprovalAutoActionHandler 实例并注入 domain.approval 场景依赖。
     * @param canvasVersionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CanvasPublishApprovalAutoActionHandler(CanvasVersionMapper canvasVersionMapper,
                                                  CanvasService canvasService) {
        this.canvasVersionMapper = canvasVersionMapper;
        this.canvasService = canvasService;
    }

    @Override
    /**
     * 判断该处理器是否负责 Canvas 发布审批的自动动作。
     */
    public boolean supports(String autoAction) {
        return CanvasPublishApprovalService.AUTO_ACTION.equals(autoAction);
    }

    @Override
    /**
     * 在审批通过后自动发布被审批的 Canvas 草稿。
     * 执行前会确认当前草稿版本仍等于审批时锁定的版本，防止审批通过后草稿被修改仍被自动发布。
     */
    public void execute(ApprovalInstanceDO instance, String actor) {
        Long canvasId = parseCanvasId(instance.getTargetId());
        CanvasVersionDO currentDraft = canvasVersionMapper.selectOne(new LambdaQueryWrapper<CanvasVersionDO>()
                .eq(CanvasVersionDO::getCanvasId, canvasId)
                .eq(instance.getTenantId() != null, CanvasVersionDO::getTenantId, instance.getTenantId())
                .eq(CanvasVersionDO::getStatus, VersionStatus.DRAFT.getCode())
                .orderByDesc(CanvasVersionDO::getVersion)
                .orderByDesc(CanvasVersionDO::getId)
                .last("LIMIT 1"));
        if (currentDraft == null || currentDraft.getId() == null) {
            throw new IllegalStateException("canvas draft no longer exists: " + canvasId);
        }
        if (!currentDraft.getId().equals(instance.getTargetVersionId())) {
            throw new IllegalStateException("approved canvas draft changed: approved="
                    + instance.getTargetVersionId() + " current=" + currentDraft.getId());
        }
        canvasService.publish(canvasId, actor == null || actor.isBlank() ? "system" : actor.trim());
    }

    /**
     * 解析并校验输入数据。
     *
     * @param raw raw 参数，用于 parseCanvasId 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long parseCanvasId(String raw) {
        try {
            return Long.parseLong(raw);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            throw new IllegalStateException("invalid canvas approval target: " + raw, ex);
        }
    }
}
