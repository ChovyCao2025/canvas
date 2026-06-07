package org.chovy.canvas.domain.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.springframework.stereotype.Component;

@Component
public class CanvasPublishApprovalAutoActionHandler implements ApprovalAutoActionHandler {

    private final CanvasVersionMapper canvasVersionMapper;
    private final CanvasService canvasService;

    public CanvasPublishApprovalAutoActionHandler(CanvasVersionMapper canvasVersionMapper,
                                                  CanvasService canvasService) {
        this.canvasVersionMapper = canvasVersionMapper;
        this.canvasService = canvasService;
    }

    @Override
    public boolean supports(String autoAction) {
        return CanvasPublishApprovalService.AUTO_ACTION.equals(autoAction);
    }

    @Override
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

    private Long parseCanvasId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("invalid canvas approval target: " + raw, ex);
        }
    }
}
