package org.chovy.canvas.domain.bi.ai;

/**
 * BiAskDataPlanner 定义 domain.bi.ai 场景中的扩展契约。
 */
public interface BiAskDataPlanner {

    /**
     * 执行 plan 流程，围绕 plan 完成校验、计算或结果组装。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 plan 流程生成的业务结果。
     */
    BiAskDataPlanningResult plan(BiAskDataPlanningContext context);
}
