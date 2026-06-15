package org.chovy.canvas.web.meta;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.marketing.api.MetaOptionFacade;
import org.chovy.canvas.marketing.api.MetaOptionView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class MetaOptionController {

    private static final Long DEFAULT_TENANT_ID = 7L;

    private final MetaOptionFacade facade;

    public MetaOptionController(MetaOptionFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/meta/options")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> options(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam String category) {
        return envelope(() -> facade.options(tenantIdOrDefault(tenantId), category));
    }

    @GetMapping("/meta/options/batch")
    public Mono<CompatibilityEnvelope<Map<String, List<MetaOptionView>>>> optionsBatch(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam List<String> categories) {
        return envelope(() -> facade.optionsBatch(tenantIdOrDefault(tenantId), deduplicate(categories)));
    }

    @GetMapping("/meta/ab-experiments")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> abExperiments(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.abExperiments(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/meta/ab-experiments/{key}/groups")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> abExperimentGroups(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String key) {
        return envelope(() -> facade.abExperimentGroups(tenantIdOrDefault(tenantId), key));
    }

    @GetMapping("/meta/biz-lines")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> bizLines(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.bizLines(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/meta/biz-lines/{key}/apis")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> bizLineApis(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String key) {
        return envelope(() -> facade.bizLineApis(tenantIdOrDefault(tenantId), key));
    }

    @GetMapping("/meta/ai-providers")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> aiProviders(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.aiProviders(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/meta/ai-templates")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> aiTemplates(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.aiTemplates(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/meta/ai-models")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> aiModels(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long providerId) {
        return envelope(() -> facade.aiModels(tenantIdOrDefault(tenantId), providerId));
    }

    @GetMapping("/meta/coupon-types")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> couponTypes(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.options(tenantIdOrDefault(tenantId), "coupon_type"));
    }

    @GetMapping("/meta/behavior-strategy-types")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> behaviorStrategyTypes(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.options(tenantIdOrDefault(tenantId), "behavior_strategy_type"));
    }

    @GetMapping("/meta/reach-scenes")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> reachScenes(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.options(tenantIdOrDefault(tenantId), "reach_scene"));
    }

    @GetMapping("/meta/mq-topics")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> mqTopics(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.options(tenantIdOrDefault(tenantId), "mq_topic_legacy"));
    }

    @GetMapping("/meta/message-codes")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> messageCodes(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "IN_APP") String type) {
        String category = "MQ".equals(type) ? "message_code_mq" : "message_code_in_app";
        return envelope(() -> facade.options(tenantIdOrDefault(tenantId), category));
    }

    @GetMapping("/meta/identity-types")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> identityTypes(
            @RequestParam(defaultValue = "1") Integer allowImport) {
        return envelope(() -> facade.identityTypes(allowImport));
    }

    @GetMapping("/meta/api-definitions")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> apiDefinitions() {
        return envelope(facade::apiDefinitions);
    }

    @GetMapping("/meta/event-definitions")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> eventDefinitions() {
        return envelope(facade::eventDefinitions);
    }

    @GetMapping("/meta/context-fields")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> contextFields() {
        return envelope(facade::contextFields);
    }

    @GetMapping("/meta/canvas-context-fields")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> canvasContextFields(
            @RequestParam(required = false) List<String> eventCodes,
            @RequestParam(required = false) List<String> apiKeys,
            @RequestParam(required = false) List<String> outputPrefixes) {
        return envelope(() -> facade.canvasContextFields(eventCodes, apiKeys, outputPrefixes));
    }

    @GetMapping("/meta/mq-definitions")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> mqDefinitions() {
        return envelope(facade::mqDefinitions);
    }

    @GetMapping("/meta/tagger-tags")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> taggerTags(
            @RequestParam(defaultValue = "offline") String type) {
        return envelope(() -> facade.taggerTags(type));
    }

    @GetMapping("/meta/tagger-tag-values")
    public Mono<CompatibilityEnvelope<List<MetaOptionView>>> taggerTagValues(
            @RequestParam String tagCode) {
        return envelope(() -> facade.taggerTagValues(tagCode));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (IllegalArgumentException | IllegalStateException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
        });
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status, message));
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static List<String> deduplicate(List<String> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream().distinct().toList();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }

    private interface ThrowingSupplier<T> {
        T get();
    }
}
