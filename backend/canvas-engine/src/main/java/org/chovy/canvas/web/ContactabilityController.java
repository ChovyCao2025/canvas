package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.policy.ContactabilityExplainerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * ContactabilityController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/contactability")
@RequiredArgsConstructor
public class ContactabilityController {

    /**
     * defaultquietstart常量，用于保持控制器内部规则一致。
     */
    private static final String DEFAULT_QUIET_START = "22:00";
    /**
     * defaultquietend常量，用于保持控制器内部规则一致。
     */
    private static final String DEFAULT_QUIET_END = "08:00";
    /**
     * defaultquiettimezone常量，用于保持控制器内部规则一致。
     */
    private static final String DEFAULT_QUIET_TIMEZONE = "USER_LOCAL";
    /**
     * defaultnode标识常量，用于保持控制器内部规则一致。
     */
    private static final String DEFAULT_NODE_ID = "preflight";
    /**
     * defaultfrequencyscope常量，用于保持控制器内部规则一致。
     */
    private static final String DEFAULT_FREQUENCY_SCOPE = "JOURNEY";
    /**
     * defaultfrequencymax常量，用于保持控制器内部规则一致。
     */
    private static final int DEFAULT_FREQUENCY_MAX = 1;
    /**
     * defaultfrequencywindowseconds常量，用于保持控制器内部规则一致。
     */
    private static final long DEFAULT_FREQUENCY_WINDOW_SECONDS = 86_400L;

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final ContactabilityExplainerService service;
    /**
     * 处理 Contactability 请求接口，对应 GET /explain。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 主要委托 ContactabilityExplainerService.Request 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param userId user ID。
     * @param channel 渠道过滤条件。
     * @param requireExplicitConsent 请求参数，可选。
     * @param quietStart 请求参数，可选。
     * @param quietEnd 请求参数，可选。
     * @param quietTimezone 请求参数，可选。
     * @param canvasId 画布 ID，可选。
     * @param nodeId node ID，可选。
     * @param frequencyScope 请求参数，可选。
     * @param frequencyMax 请求参数，可选。
     * @param frequencyWindowSeconds 请求参数，可选。
     * @return 异步返回统一响应，包含处理 Contactability 请求后的业务数据。
     */
    @GetMapping("/explain")
    public Mono<R<ContactabilityExplainerService.Report>> explain(
            @RequestParam String userId,
            @RequestParam String channel,
            @RequestParam(required = false) Boolean requireExplicitConsent,
            @RequestParam(required = false) String quietStart,
            @RequestParam(required = false) String quietEnd,
            @RequestParam(required = false) String quietTimezone,
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String frequencyScope,
            @RequestParam(required = false) Integer frequencyMax,
            @RequestParam(required = false) Long frequencyWindowSeconds) {
        ContactabilityExplainerService.Request request = new ContactabilityExplainerService.Request(
                userId,
                channel,
                requireExplicitConsent == null || requireExplicitConsent,
                safeTime(quietStart, DEFAULT_QUIET_START),
                safeTime(quietEnd, DEFAULT_QUIET_END),
                safeTimezone(quietTimezone),
                canvasId == null ? 0L : canvasId,
                defaultString(nodeId, DEFAULT_NODE_ID),
                defaultString(frequencyScope, DEFAULT_FREQUENCY_SCOPE),
                safeFrequencyMax(frequencyMax),
                Duration.ofSeconds(safeWindowSeconds(frequencyWindowSeconds)));
        return Mono.fromCallable(() -> R.ok(service.explain(request)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 safeTime 流程中的校验、计算或对象转换。
     * @return 返回 safe time 生成的文本或业务键。
     */
    private static String safeTime(String value, String fallback) {
        String candidate = defaultString(value, fallback);
        try {
            LocalTime.parse(candidate);
            return candidate;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe timezone 生成的文本或业务键。
     */
    private static String safeTimezone(String value) {
        String candidate = defaultString(value, DEFAULT_QUIET_TIMEZONE);
        if (DEFAULT_QUIET_TIMEZONE.equalsIgnoreCase(candidate)) {
            return DEFAULT_QUIET_TIMEZONE;
        }
        try {
            ZoneId.of(candidate);
            return candidate;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ignored) {
            return DEFAULT_QUIET_TIMEZONE;
        }
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe frequency max 计算得到的数量、金额或指标值。
     */
    private static int safeFrequencyMax(Integer value) {
        return value == null || value <= 0 ? DEFAULT_FREQUENCY_MAX : value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe window seconds 计算得到的数量、金额或指标值。
     */
    private static long safeWindowSeconds(Long value) {
        return value == null || value <= 0 ? DEFAULT_FREQUENCY_WINDOW_SECONDS : value;
    }
}
