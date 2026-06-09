package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.springframework.stereotype.Component;

/**
 * Single state machine for canvas lifecycle transitions.
 */
@Component
public class CanvasStateTransitionPolicy {

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param canvas canvas 参数，用于 assertTransition 流程中的校验、计算或对象转换。
     * @param target target 参数，用于 assertTransition 流程中的校验、计算或对象转换。
     */
    public void assertTransition(CanvasDO canvas, CanvasStatusEnum target) {
        CanvasStatusEnum source = statusOf(canvas);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (source == target) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (source == CanvasStatusEnum.KILLED || source == CanvasStatusEnum.ARCHIVED) {
            reject(source, target);
        }

        boolean allowed = switch (target) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            case PUBLISHED -> source == CanvasStatusEnum.DRAFT
                    || source == CanvasStatusEnum.PUBLISHED
                    || source == CanvasStatusEnum.OFFLINE;
            case OFFLINE -> source == CanvasStatusEnum.PUBLISHED;
            case KILLED -> source == CanvasStatusEnum.PUBLISHED;
            case ARCHIVED -> source == CanvasStatusEnum.DRAFT
                    || source == CanvasStatusEnum.PUBLISHED
                    || source == CanvasStatusEnum.OFFLINE;
            case DRAFT -> false;
        };
        if (!allowed) {
            reject(source, target);
        }
    }

    /**
     * assertDraftUpdateAllowed 处理 domain.canvas 场景的业务逻辑。
     * @param canvas canvas 参数，用于 assertDraftUpdateAllowed 流程中的校验、计算或对象转换。
     */
    public void assertDraftUpdateAllowed(CanvasDO canvas) {
        CanvasStatusEnum source = statusOf(canvas);
        if (source == CanvasStatusEnum.KILLED || source == CanvasStatusEnum.ARCHIVED) {
            reject(source, CanvasStatusEnum.DRAFT);
        }
    }

    /**
     * assertPublishedRuntimeMutationAllowed 处理 domain.canvas 场景的业务逻辑。
     * @param canvas canvas 参数，用于 assertPublishedRuntimeMutationAllowed 流程中的校验、计算或对象转换。
     */
    public void assertPublishedRuntimeMutationAllowed(CanvasDO canvas) {
        CanvasStatusEnum source = statusOf(canvas);
        if (source != CanvasStatusEnum.PUBLISHED) {
            reject(source, CanvasStatusEnum.PUBLISHED);
        }
    }

    /**
     * isPublished 校验或转换 domain.canvas 场景的数据。
     * @param canvas canvas 参数，用于 isPublished 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    public boolean isPublished(CanvasDO canvas) {
        return statusOf(canvas) == CanvasStatusEnum.PUBLISHED;
    }

    /**
     * 执行 statusOf 流程，围绕 status of 完成校验、计算或结果组装。
     *
     * @param canvas canvas 参数，用于 statusOf 流程中的校验、计算或对象转换。
     * @return 返回 statusOf 流程生成的业务结果。
     */
    private static CanvasStatusEnum statusOf(CanvasDO canvas) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (canvas == null) {
            throw new IllegalArgumentException("canvas must not be null");
        }
        Integer status = canvas.getStatus();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CanvasStatusEnum candidate : CanvasStatusEnum.values()) {
            if (candidate.getCode().equals(status)) {
                // 汇总前面计算出的状态和明细，返回给调用方。
                return candidate;
            }
        }
        throw new IllegalStateException("UNKNOWN canvas state: " + status);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param source source 参数，用于 reject 流程中的校验、计算或对象转换。
     * @param target target 参数，用于 reject 流程中的校验、计算或对象转换。
     */
    private static void reject(CanvasStatusEnum source, CanvasStatusEnum target) {
        throw new IllegalStateException("Illegal canvas state transition: "
                + source.name() + " -> " + target.name());
    }
}
