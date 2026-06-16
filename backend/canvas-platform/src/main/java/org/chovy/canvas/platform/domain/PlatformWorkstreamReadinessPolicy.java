package org.chovy.canvas.platform.domain;

/**
 * 判断平台工作流是否具备进入子规格执行阶段的策略。
 */
public final class PlatformWorkstreamReadinessPolicy {

    /**
     * 表示工作流被缺失子规格阻塞。
     */
    public static final String BLOCKED_CHILD_SPEC_REQUIRED = "BLOCKED_CHILD_SPEC_REQUIRED";

    /**
     * 表示工作流已经具备子规格执行条件。
     */
    public static final String READY_FOR_CHILD_EXECUTION = "READY_FOR_CHILD_EXECUTION";

    /**
     * 工具类不允许实例化。
     */
    private PlatformWorkstreamReadinessPolicy() {
    }

    /**
     * 计算工作流当前准入状态。
     *
     * @param workstream 平台工作流定义
     * @return 工作流准入状态
     */
    public static String statusFor(PlatformWorkstream workstream) {
        return requiresMissingChildSpec(workstream)
                ? BLOCKED_CHILD_SPEC_REQUIRED
                : READY_FOR_CHILD_EXECUTION;
    }

    /**
     * 判断工作流是否要求但缺少子规格。
     *
     * @param workstream 平台工作流定义
     * @return 缺少必需子规格时返回 true
     */
    public static boolean requiresMissingChildSpec(PlatformWorkstream workstream) {
        return workstream.requiresChildSpec()
                && (workstream.childSpecPath() == null || workstream.childSpecPath().isBlank());
    }
}
