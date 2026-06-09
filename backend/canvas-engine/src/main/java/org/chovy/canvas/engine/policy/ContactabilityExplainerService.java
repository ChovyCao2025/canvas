package org.chovy.canvas.engine.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Composes read-only marketing policy checks into an operator-facing contactability report.
 */
@Service
@RequiredArgsConstructor
public class ContactabilityExplainerService {

    private final MarketingPolicyService policyService;

    /** Builds a full explanation without consuming frequency quota. */
    public Report explain(Request request) {
        // 准备本次处理所需的上下文和中间变量。
        String channel = normalize(request.channel());
        List<Check> checks = List.of(
                check("CONSENT", policyService.consentAllowed(
                        request.userId(), channel, request.requireExplicitConsent())),
                check("SUPPRESSION", policyService.suppressionAllowed(request.userId(), channel)),
                check("CHANNEL", policyService.channelAvailable(request.userId(), channel)),
                check("QUIET_HOURS", policyService.quietHoursAllowed(
                        request.userId(), request.quietStart(), request.quietEnd(), request.quietTimezone())),
                check("FREQUENCY", policyService.previewFrequency(
                        request.userId(),
                        request.canvasId(),
                        request.nodeId(),
                        request.frequencyScope(),
                        channel,
                        request.frequencyMax(),
                        request.frequencyWindow()))
        );
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        boolean allowed = checks.stream().allMatch(Check::allowed);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new Report(request.userId(), channel, allowed, checks);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param checkKey 业务键，用于在同一租户下定位资源。
     * @param decision decision 参数，用于 check 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private Check check(String checkKey, MarketingPolicyService.PolicyDecision decision) {
        return new Check(checkKey, decision.allowed(), decision.reasonCode(), decision.reasonMessage());
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param channel channel 参数，用于 normalize 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String channel) {
        return channel == null || channel.isBlank() ? "ALL" : channel.toUpperCase(Locale.ROOT);
    }

    /**
     * Request 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record Request(
            String userId,
            String channel,
            boolean requireExplicitConsent,
            String quietStart,
            String quietEnd,
            String quietTimezone,
            Long canvasId,
            String nodeId,
            String frequencyScope,
            int frequencyMax,
            Duration frequencyWindow) {
    }

    /**
     * Report 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record Report(
            String userId,
            String channel,
            boolean allowed,
            List<Check> checks) {
    }

    /**
     * Check 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record Check(
            String checkKey,
            boolean allowed,
            String reasonCode,
            String reasonMessage) {
    }
}
