package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.query.CanvasListQuery;

/**
 * CanvasListQuerySupport 承载对应领域的业务规则、流程编排和结果转换。
 */
final class CanvasListQuerySupport {

    /**
     * 初始化 CanvasListQuerySupport 实例。
     */
    private CanvasListQuerySupport() {
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param query query 参数，用于 build 流程中的校验、计算或对象转换。
     * @param examplesEnabled examples enabled 参数，用于 build 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    static LambdaQueryWrapper<CanvasDO> build(CanvasListQuery query, boolean examplesEnabled) {
        return apply(new LambdaQueryWrapper<>(), query, examplesEnabled);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param wrapper wrapper 参数，用于 apply 流程中的校验、计算或对象转换。
     * @param query query 参数，用于 apply 流程中的校验、计算或对象转换。
     * @param examplesEnabled examples enabled 参数，用于 apply 流程中的校验、计算或对象转换。
     * @return 返回 apply 流程生成的业务结果。
     */
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
