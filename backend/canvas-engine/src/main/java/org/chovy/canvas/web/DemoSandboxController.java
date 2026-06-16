package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationAdapterCatalog;
import org.chovy.canvas.domain.conversation.ConversationAdapterHarness;
import org.chovy.canvas.domain.conversation.ConversationIngressResp;
import org.chovy.canvas.domain.conversation.ConversationIngressService;
import org.chovy.canvas.domain.conversation.SandboxConversationReplyAdapter;
import org.chovy.canvas.domain.conversation.SandboxConversationReplyPayload;
import org.chovy.canvas.domain.demo.DemoSandboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/demo-sandboxes")
/**
 * DemoSandboxController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class DemoSandboxController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final DemoSandboxService service;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * conversationadapterharness，用于保存请求处理过程中需要的业务数据。
     */
    private final ConversationAdapterHarness conversationAdapterHarness;

    /**
     * 初始化 DemoSandboxController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param conversationIngressService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public DemoSandboxController(DemoSandboxService service,
                                 TenantContextResolver tenantContextResolver,
                                 ConversationIngressService conversationIngressService) {
        this(service, tenantContextResolver, new ConversationAdapterHarness(
                conversationIngressService,
                new ConversationAdapterCatalog(List.of(new SandboxConversationReplyAdapter()))));
    }

    @Autowired
    /**
     * 初始化 DemoSandboxController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param conversationAdapterHarness conversation adapter harness 参数，用于 DemoSandboxController 流程中的校验、计算或对象转换。
     */
    public DemoSandboxController(DemoSandboxService service,
                                 TenantContextResolver tenantContextResolver,
                                 ConversationAdapterHarness conversationAdapterHarness) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
        this.conversationAdapterHarness = conversationAdapterHarness;
    }

    @PostMapping
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 install 流程生成的业务结果。
     */
    public Mono<R<DemoSandboxService.Sandbox>> install(@RequestBody InstallRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() ->
                                R.ok(service.install(request.tenantId(), request.demoName(), request.ttlDays())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tenantId}/reset")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 reset 流程生成的业务结果。
     */
    public Mono<R<DemoSandboxService.ResetResult>> reset(@PathVariable Long tenantId) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() ->
                                R.ok(service.reset(tenantId, context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/expired")
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @return 返回 expired 汇总后的集合、分页或映射视图。
     */
    public Mono<R<List<DemoSandboxService.Sandbox>>> expired() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.expired()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{tenantId}/conversation-replies")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 reply 流程生成的业务结果。
     */
    public Mono<R<ConversationIngressResp>> reply(@PathVariable Long tenantId,
                                                  @RequestBody ConversationReplyRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(conversationAdapterHarness.ingest(
                                tenantId,
                                "SANDBOX",
                                toPayload(request),
                                context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回组装或转换后的结果对象。
     */
    private SandboxConversationReplyPayload toPayload(ConversationReplyRequest request) {
        if (request == null) {
            /**
             * 执行 illegalargumentexception 对应的内部处理流程。
             *
             * @param required" required"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalArgumentException("conversation reply request is required");
        }
        return new SandboxConversationReplyPayload(
                request.canvasId(),
                request.versionId(),
                request.executionId(),
                request.userId(),
                request.externalMessageId(),
                request.eventId(),
                request.text(),
                request.intent(),
                request.attributes());
    }

    /**
     * InstallRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class InstallRequest {

        /**
         * 租户标识。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("tenantId")
        private final Long tenantId;

        /**
         * demoName 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("demoName")
        private final String demoName;

        /**
         * ttlDays 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("ttlDays")
        private final int ttlDays;

        /**
         * 创建 InstallRequest 实例。
         *
         * @param tenantId 租户标识
         * @param demoName demoName 字段值
         * @param ttlDays ttlDays 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public InstallRequest(@com.fasterxml.jackson.annotation.JsonProperty("tenantId") Long tenantId, @com.fasterxml.jackson.annotation.JsonProperty("demoName") String demoName, @com.fasterxml.jackson.annotation.JsonProperty("ttlDays") int ttlDays) {
            this.tenantId = tenantId;
            this.demoName = demoName;
            this.ttlDays = ttlDays;
        }

        /**
         * 返回租户标识。
         *
         * @return 租户标识
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回demoName 字段值。
         *
         * @return demoName 字段值
         */
        public String demoName() {
            return demoName;
        }

        /**
         * 返回ttlDays 字段值。
         *
         * @return ttlDays 字段值
         */
        public int ttlDays() {
            return ttlDays;
        }

        /**
         * 判断两个 InstallRequest 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof InstallRequest that)) {
                return false;
            }
            return java.util.Objects.equals(tenantId, that.tenantId) && java.util.Objects.equals(demoName, that.demoName) && java.util.Objects.equals(ttlDays, that.ttlDays);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, demoName, ttlDays);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "InstallRequest[" + "tenantId=" + tenantId + ", " + "demoName=" + demoName + ", " + "ttlDays=" + ttlDays + "]";
        }
    }

    /**
     * ConversationReplyRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class ConversationReplyRequest {

        /**
         * 画布标识。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("canvasId")
        private final Long canvasId;

        /**
         * versionId 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("versionId")
        private final Long versionId;

        /**
         * executionId 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("executionId")
        private final String executionId;

        /**
         * 用户标识。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("userId")
        private final String userId;

        /**
         * externalMessageId 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("externalMessageId")
        private final String externalMessageId;

        /**
         * eventId 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("eventId")
        private final String eventId;

        /**
         * text 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("text")
        private final String text;

        /**
         * intent 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("intent")
        private final String intent;

        /**
         * attributes 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("attributes")
        private final Map<String, Object> attributes;

        /**
         * 创建 ConversationReplyRequest 实例。
         *
         * @param canvasId 画布标识
         * @param versionId versionId 字段值
         * @param executionId executionId 字段值
         * @param userId 用户标识
         * @param externalMessageId externalMessageId 字段值
         * @param eventId eventId 字段值
         * @param text text 字段值
         * @param intent intent 字段值
         * @param attributes attributes 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public ConversationReplyRequest(@com.fasterxml.jackson.annotation.JsonProperty("canvasId") Long canvasId, @com.fasterxml.jackson.annotation.JsonProperty("versionId") Long versionId, @com.fasterxml.jackson.annotation.JsonProperty("executionId") String executionId, @com.fasterxml.jackson.annotation.JsonProperty("userId") String userId, @com.fasterxml.jackson.annotation.JsonProperty("externalMessageId") String externalMessageId, @com.fasterxml.jackson.annotation.JsonProperty("eventId") String eventId, @com.fasterxml.jackson.annotation.JsonProperty("text") String text, @com.fasterxml.jackson.annotation.JsonProperty("intent") String intent, @com.fasterxml.jackson.annotation.JsonProperty("attributes") Map<String, Object> attributes) {
            this.canvasId = canvasId;
            this.versionId = versionId;
            this.executionId = executionId;
            this.userId = userId;
            this.externalMessageId = externalMessageId;
            this.eventId = eventId;
            this.text = text;
            this.intent = intent;
            this.attributes = attributes;
        }

        /**
         * 返回画布标识。
         *
         * @return 画布标识
         */
        public Long canvasId() {
            return canvasId;
        }

        /**
         * 返回versionId 字段值。
         *
         * @return versionId 字段值
         */
        public Long versionId() {
            return versionId;
        }

        /**
         * 返回executionId 字段值。
         *
         * @return executionId 字段值
         */
        public String executionId() {
            return executionId;
        }

        /**
         * 返回用户标识。
         *
         * @return 用户标识
         */
        public String userId() {
            return userId;
        }

        /**
         * 返回externalMessageId 字段值。
         *
         * @return externalMessageId 字段值
         */
        public String externalMessageId() {
            return externalMessageId;
        }

        /**
         * 返回eventId 字段值。
         *
         * @return eventId 字段值
         */
        public String eventId() {
            return eventId;
        }

        /**
         * 返回text 字段值。
         *
         * @return text 字段值
         */
        public String text() {
            return text;
        }

        /**
         * 返回intent 字段值。
         *
         * @return intent 字段值
         */
        public String intent() {
            return intent;
        }

        /**
         * 返回attributes 字段值。
         *
         * @return attributes 字段值
         */
        public Map<String, Object> attributes() {
            return attributes;
        }

        /**
         * 判断两个 ConversationReplyRequest 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConversationReplyRequest that)) {
                return false;
            }
            return java.util.Objects.equals(canvasId, that.canvasId) && java.util.Objects.equals(versionId, that.versionId) && java.util.Objects.equals(executionId, that.executionId) && java.util.Objects.equals(userId, that.userId) && java.util.Objects.equals(externalMessageId, that.externalMessageId) && java.util.Objects.equals(eventId, that.eventId) && java.util.Objects.equals(text, that.text) && java.util.Objects.equals(intent, that.intent) && java.util.Objects.equals(attributes, that.attributes);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(canvasId, versionId, executionId, userId, externalMessageId, eventId, text, intent, attributes);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "ConversationReplyRequest[" + "canvasId=" + canvasId + ", " + "versionId=" + versionId + ", " + "executionId=" + executionId + ", " + "userId=" + userId + ", " + "externalMessageId=" + externalMessageId + ", " + "eventId=" + eventId + ", " + "text=" + text + ", " + "intent=" + intent + ", " + "attributes=" + attributes + "]";
        }
    }
}
