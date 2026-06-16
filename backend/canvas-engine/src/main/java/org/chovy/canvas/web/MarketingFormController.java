package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.MarketingFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@RestController
/**
 * MarketingFormController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class MarketingFormController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final MarketingFormService service;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 初始化 MarketingFormController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingFormController(MarketingFormService service) {
        this(service, null);
    }

    @Autowired
    /**
     * 初始化 MarketingFormController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public MarketingFormController(MarketingFormService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/canvas/marketing-forms")
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<MarketingFormService.FormDefinitionView>>> list() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.list(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/canvas/marketing-forms/{id}")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 get 流程生成的业务结果。
     */
    public Mono<R<MarketingFormService.FormDefinitionView>> get(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.get(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/canvas/marketing-forms")
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<MarketingFormService.FormDefinitionView>> create(@RequestBody FormDefinitionReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.create(tenantId, toCommand(req))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/canvas/marketing-forms/{id}")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<MarketingFormService.FormDefinitionView>> update(
            @PathVariable Long id,
            @RequestBody FormDefinitionReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.update(tenantId, id, toCommand(req))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/canvas/marketing-forms/{id}/status")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 setStatus 流程生成的业务结果。
     */
    public Mono<R<MarketingFormService.FormDefinitionView>> setStatus(
            @PathVariable Long id,
            @RequestBody StatusReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.setStatus(
                        tenantId, id, Boolean.TRUE.equals(req.active()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/canvas/marketing-forms/submissions")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param formId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 submissions 汇总后的集合、分页或映射视图。
     */
    public Mono<R<List<MarketingFormService.SubmissionView>>> submissions(
            @RequestParam(required = false) Long formId,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.submissions(tenantId, formId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/public/marketing-forms/{publicKey}")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param publicKey 业务键，用于在同一租户下定位资源。
     * @return 返回 publicForm 流程生成的业务结果。
     */
    public Mono<R<MarketingFormService.PublicFormView>> publicForm(@PathVariable String publicKey) {
        return Mono.fromCallable(() -> R.ok(service.publicForm(publicKey)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/public/marketing-forms/{publicKey}/submit")
    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param publicKey 业务键，用于在同一租户下定位资源。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 submit 流程生成的业务结果。
     */
    public Mono<R<MarketingFormService.SubmitResult>> submit(
            ServerHttpRequest request,
            @PathVariable String publicKey,
            @RequestBody PublicSubmitReq req) {
        return Mono.fromCallable(() -> R.ok(service.submit(publicKey, toPublicCommand(req, request))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingFormService.FormDefinitionCommand toCommand(FormDefinitionReq req) {
        return new MarketingFormService.FormDefinitionCommand(
                req.publicKey(),
                req.name(),
                req.description(),
                req.fieldSchemaJson(),
                req.submitActionJson(),
                req.successMessage(),
                req.active(),
                req.createdBy());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingFormService.PublicSubmitCommand toPublicCommand(PublicSubmitReq req, ServerHttpRequest request) {
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String ip = request.getRemoteAddress() == null ? null : request.getRemoteAddress().getAddress().getHostAddress();
        return new MarketingFormService.PublicSubmitCommand(
                req.response(),
                req.utm(),
                req.anonymousId(),
                req.idempotencyKey(),
                req.consentChannel(),
                req.consentStatus(),
                userAgent,
                hashText(ip));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private String hashText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            /**
             * 执行 illegalstateexception 对应的内部处理流程。
             *
             * @param unavailable" unavailable"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 current tenant id 计算得到的数量、金额或指标值。
     */
    private Mono<Long> currentTenantId() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return tenantContextResolver.current()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }

    /**
     * FormDefinitionReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class FormDefinitionReq {

        /**
         * publicKey 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("publicKey")
        private final String publicKey;

        /**
         * 名称。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("name")
        private final String name;

        /**
         * description 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("description")
        private final String description;

        /**
         * fieldSchemaJson 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("fieldSchemaJson")
        private final String fieldSchemaJson;

        /**
         * submitActionJson 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("submitActionJson")
        private final String submitActionJson;

        /**
         * successMessage 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("successMessage")
        private final String successMessage;

        /**
         * 启用状态。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("active")
        private final Boolean active;

        /**
         * createdBy 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("createdBy")
        private final String createdBy;

        /**
         * 创建 FormDefinitionReq 实例。
         *
         * @param publicKey publicKey 字段值
         * @param name 名称
         * @param description description 字段值
         * @param fieldSchemaJson fieldSchemaJson 字段值
         * @param submitActionJson submitActionJson 字段值
         * @param successMessage successMessage 字段值
         * @param active 启用状态
         * @param createdBy createdBy 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public FormDefinitionReq(@com.fasterxml.jackson.annotation.JsonProperty("publicKey") String publicKey, @com.fasterxml.jackson.annotation.JsonProperty("name") String name, @com.fasterxml.jackson.annotation.JsonProperty("description") String description, @com.fasterxml.jackson.annotation.JsonProperty("fieldSchemaJson") String fieldSchemaJson, @com.fasterxml.jackson.annotation.JsonProperty("submitActionJson") String submitActionJson, @com.fasterxml.jackson.annotation.JsonProperty("successMessage") String successMessage, @com.fasterxml.jackson.annotation.JsonProperty("active") Boolean active, @com.fasterxml.jackson.annotation.JsonProperty("createdBy") String createdBy) {
            this.publicKey = publicKey;
            this.name = name;
            this.description = description;
            this.fieldSchemaJson = fieldSchemaJson;
            this.submitActionJson = submitActionJson;
            this.successMessage = successMessage;
            this.active = active;
            this.createdBy = createdBy;
        }

        /**
         * 返回publicKey 字段值。
         *
         * @return publicKey 字段值
         */
        public String publicKey() {
            return publicKey;
        }

        /**
         * 返回名称。
         *
         * @return 名称
         */
        public String name() {
            return name;
        }

        /**
         * 返回description 字段值。
         *
         * @return description 字段值
         */
        public String description() {
            return description;
        }

        /**
         * 返回fieldSchemaJson 字段值。
         *
         * @return fieldSchemaJson 字段值
         */
        public String fieldSchemaJson() {
            return fieldSchemaJson;
        }

        /**
         * 返回submitActionJson 字段值。
         *
         * @return submitActionJson 字段值
         */
        public String submitActionJson() {
            return submitActionJson;
        }

        /**
         * 返回successMessage 字段值。
         *
         * @return successMessage 字段值
         */
        public String successMessage() {
            return successMessage;
        }

        /**
         * 返回启用状态。
         *
         * @return 启用状态
         */
        public Boolean active() {
            return active;
        }

        /**
         * 返回createdBy 字段值。
         *
         * @return createdBy 字段值
         */
        public String createdBy() {
            return createdBy;
        }

        /**
         * 判断两个 FormDefinitionReq 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FormDefinitionReq that)) {
                return false;
            }
            return java.util.Objects.equals(publicKey, that.publicKey) && java.util.Objects.equals(name, that.name) && java.util.Objects.equals(description, that.description) && java.util.Objects.equals(fieldSchemaJson, that.fieldSchemaJson) && java.util.Objects.equals(submitActionJson, that.submitActionJson) && java.util.Objects.equals(successMessage, that.successMessage) && java.util.Objects.equals(active, that.active) && java.util.Objects.equals(createdBy, that.createdBy);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(publicKey, name, description, fieldSchemaJson, submitActionJson, successMessage, active, createdBy);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "FormDefinitionReq[" + "publicKey=" + publicKey + ", " + "name=" + name + ", " + "description=" + description + ", " + "fieldSchemaJson=" + fieldSchemaJson + ", " + "submitActionJson=" + submitActionJson + ", " + "successMessage=" + successMessage + ", " + "active=" + active + ", " + "createdBy=" + createdBy + "]";
        }
    }

    /**
     * StatusReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class StatusReq {

        /**
         * 启用状态。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("active")
        private final Boolean active;

        /**
         * 创建 StatusReq 实例。
         *
         * @param active 启用状态
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public StatusReq(@com.fasterxml.jackson.annotation.JsonProperty("active") Boolean active) {
            this.active = active;
        }

        /**
         * 返回启用状态。
         *
         * @return 启用状态
         */
        public Boolean active() {
            return active;
        }

        /**
         * 判断两个 StatusReq 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof StatusReq that)) {
                return false;
            }
            return java.util.Objects.equals(active, that.active);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(active);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "StatusReq[" + "active=" + active + "]";
        }
    }

    /**
     * PublicSubmitReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static final class PublicSubmitReq {

        /**
         * response 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("response")
        private final Map<String, Object> response;

        /**
         * utm 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("utm")
        private final Map<String, Object> utm;

        /**
         * anonymousId 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("anonymousId")
        private final String anonymousId;

        /**
         * idempotencyKey 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("idempotencyKey")
        private final String idempotencyKey;

        /**
         * consentChannel 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("consentChannel")
        private final String consentChannel;

        /**
         * consentStatus 字段值。
         */
        @com.fasterxml.jackson.annotation.JsonProperty("consentStatus")
        private final String consentStatus;

        /**
         * 创建 PublicSubmitReq 实例。
         *
         * @param response response 字段值
         * @param utm utm 字段值
         * @param anonymousId anonymousId 字段值
         * @param idempotencyKey idempotencyKey 字段值
         * @param consentChannel consentChannel 字段值
         * @param consentStatus consentStatus 字段值
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public PublicSubmitReq(@com.fasterxml.jackson.annotation.JsonProperty("response") Map<String, Object> response, @com.fasterxml.jackson.annotation.JsonProperty("utm") Map<String, Object> utm, @com.fasterxml.jackson.annotation.JsonProperty("anonymousId") String anonymousId, @com.fasterxml.jackson.annotation.JsonProperty("idempotencyKey") String idempotencyKey, @com.fasterxml.jackson.annotation.JsonProperty("consentChannel") String consentChannel, @com.fasterxml.jackson.annotation.JsonProperty("consentStatus") String consentStatus) {
            this.response = response;
            this.utm = utm;
            this.anonymousId = anonymousId;
            this.idempotencyKey = idempotencyKey;
            this.consentChannel = consentChannel;
            this.consentStatus = consentStatus;
        }

        /**
         * 返回response 字段值。
         *
         * @return response 字段值
         */
        public Map<String, Object> response() {
            return response;
        }

        /**
         * 返回utm 字段值。
         *
         * @return utm 字段值
         */
        public Map<String, Object> utm() {
            return utm;
        }

        /**
         * 返回anonymousId 字段值。
         *
         * @return anonymousId 字段值
         */
        public String anonymousId() {
            return anonymousId;
        }

        /**
         * 返回idempotencyKey 字段值。
         *
         * @return idempotencyKey 字段值
         */
        public String idempotencyKey() {
            return idempotencyKey;
        }

        /**
         * 返回consentChannel 字段值。
         *
         * @return consentChannel 字段值
         */
        public String consentChannel() {
            return consentChannel;
        }

        /**
         * 返回consentStatus 字段值。
         *
         * @return consentStatus 字段值
         */
        public String consentStatus() {
            return consentStatus;
        }

        /**
         * 判断两个 PublicSubmitReq 实例是否包含相同字段值。
         *
         * @param o 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PublicSubmitReq that)) {
                return false;
            }
            return java.util.Objects.equals(response, that.response) && java.util.Objects.equals(utm, that.utm) && java.util.Objects.equals(anonymousId, that.anonymousId) && java.util.Objects.equals(idempotencyKey, that.idempotencyKey) && java.util.Objects.equals(consentChannel, that.consentChannel) && java.util.Objects.equals(consentStatus, that.consentStatus);
        }

        /**
         * 根据全部字段生成哈希值。
         *
         * @return 字段哈希值
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(response, utm, anonymousId, idempotencyKey, consentChannel, consentStatus);
        }

        /**
         * 返回与原记录形态一致的调试字符串。
         *
         * @return 字段调试字符串
         */
        @Override
        public String toString() {
            return "PublicSubmitReq[" + "response=" + response + ", " + "utm=" + utm + ", " + "anonymousId=" + anonymousId + ", " + "idempotencyKey=" + idempotencyKey + ", " + "consentChannel=" + consentChannel + ", " + "consentStatus=" + consentStatus + "]";
        }
    }
}
