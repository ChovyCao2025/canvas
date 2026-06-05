package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.query.CanvasListQuery;

final class CanvasListQuerySupport {

    private CanvasListQuerySupport() {
    }

    static LambdaQueryWrapper<CanvasDO> build(CanvasListQuery query, boolean examplesEnabled) {
        return apply(new LambdaQueryWrapper<>(), query, examplesEnabled);
    }

    static LambdaQueryWrapper<CanvasDO> apply(
            LambdaQueryWrapper<CanvasDO> wrapper,
            CanvasListQuery query,
            boolean examplesEnabled) {
        CanvasListQuery q = query == null ? new CanvasListQuery() : query;
        return wrapper
                .eq(q.getStatus() != null, CanvasDO::getStatus, q.getStatus())
                .ne(q.getStatus() == null, CanvasDO::getStatus, CanvasStatusEnum.ARCHIVED.getCode())
                .eq(!examplesEnabled, CanvasDO::getIsExample, 0)
                .like(q.getName() != null && !q.getName().isBlank(), CanvasDO::getName, q.getName())
                .eq(q.getProjectKey() != null && !q.getProjectKey().isBlank(), CanvasDO::getProjectKey, q.getProjectKey())
                .eq(q.getFolderKey() != null && !q.getFolderKey().isBlank(), CanvasDO::getFolderKey, q.getFolderKey())
                .orderByDesc(CanvasDO::getCreatedAt);
    }
}
