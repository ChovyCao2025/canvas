package org.chovy.canvas.domain.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingProviderChangeDO;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingProviderChangeMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * SearchMarketingReconciliationService 编排 domain.search 场景的领域业务规则。
 */
@Service
public class SearchMarketingReconciliationService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final SearchMarketingMutationMapper mutationMapper;
    private final SearchMarketingProviderChangeMapper providerChangeMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 SearchMarketingReconciliationService 实例并注入 domain.search 场景依赖。
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param providerChangeMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public SearchMarketingReconciliationService(SearchMarketingMutationMapper mutationMapper,
                                                SearchMarketingProviderChangeMapper providerChangeMapper,
                                                ObjectMapper objectMapper) {
        this(mutationMapper, providerChangeMapper, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 查询或读取业务数据。
     *
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param providerChangeMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    SearchMarketingReconciliationService(SearchMarketingMutationMapper mutationMapper,
                                         SearchMarketingProviderChangeMapper providerChangeMapper,
                                         ObjectMapper objectMapper,
                                         Clock clock) {
        this.mutationMapper = mutationMapper;
        this.providerChangeMapper = providerChangeMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行业务操作 reconcile，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param mutationId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public SearchMarketingReconciliationView reconcile(Long tenantId, Long mutationId, String actor) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SearchMarketingMutationDO mutation = mutationMapper.selectById(requiredId(mutationId, "mutationId"));
        validateTenant(scopedTenantId, mutation == null ? null : mutation.getTenantId(), "mutation");
        LocalDateTime changedAt = now();
        Map<String, Object> providerResponse = map(mutation.getProviderResponseJson());
        String providerOperationId = providerOperationId(providerResponse);
        boolean confirmed = "APPLIED".equals(mutation.getStatus()) && providerOperationId != null;
        SearchMarketingProviderChangeDO change = new SearchMarketingProviderChangeDO();
        change.setTenantId(scopedTenantId);
        change.setSourceId(mutation.getSourceId());
        change.setMutationId(mutation.getId());
        change.setProvider(mutation.getProvider());
        change.setExternalResourceId(defaultString(mutation.getExternalEntityId(), providerOperationId));
        change.setChangeType(mutation.getMutationType());
        change.setChangedFieldsJson(json(map(mutation.getPayloadJson())));
        change.setProviderActor(defaultString(mutation.getExecutedBy(), defaultString(actor, "system")));
        change.setProviderChangedAt(mutation.getExecutedAt() == null ? changedAt : mutation.getExecutedAt());
        change.setReconciliationStatus(confirmed ? "CONFIRMED" : "FAILED");
        change.setEvidenceJson(json(evidence(mutation, providerResponse, providerOperationId, confirmed)));
        change.setCreatedAt(changedAt);
        change.setUpdatedAt(changedAt);
        providerChangeMapper.insert(change);

        mutation.setStatus(confirmed ? "RECONCILED" : "RECONCILE_FAILED");
        if (!confirmed) {
            mutation.setErrorCode("SEARCH_RECONCILIATION_NOT_CONFIRMED");
            mutation.setErrorMessage("search provider state did not confirm the applied mutation");
        }
        mutation.setUpdatedAt(changedAt);
        mutationMapper.updateById(mutation);

        // 汇总前面计算出的状态和明细，返回给调用方。
        return new SearchMarketingReconciliationView(
                scopedTenantId,
                mutation.getId(),
                change.getId(),
                mutation.getStatus(),
                providerOperationId,
                map(change.getEvidenceJson()),
                changedAt);
    }

    /**
     * 查询业务列表，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 list 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingProviderChangeView> list(Long tenantId, SearchMarketingProviderChangeQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long sourceId = query == null ? null : query.sourceId();
        Long mutationId = query == null ? null : query.mutationId();
        String provider = query == null ? null : normalizeOptional(query.provider());
        String reconciliationStatus = query == null ? null : normalizeOptional(query.reconciliationStatus());
        int limit = Math.max(1, Math.min(query == null || query.limit() == null ? 50 : query.limit(), 100));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(providerChangeMapper.selectList(new LambdaQueryWrapper<SearchMarketingProviderChangeDO>()
                .eq(SearchMarketingProviderChangeDO::getTenantId, scopedTenantId)
                .eq(sourceId != null, SearchMarketingProviderChangeDO::getSourceId, sourceId)
                .eq(mutationId != null, SearchMarketingProviderChangeDO::getMutationId, mutationId)
                .eq(provider != null, SearchMarketingProviderChangeDO::getProvider, provider)
                .eq(reconciliationStatus != null, SearchMarketingProviderChangeDO::getReconciliationStatus,
                        reconciliationStatus)
                .orderByDesc(SearchMarketingProviderChangeDO::getProviderChangedAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> mutationId == null || Objects.equals(mutationId, row.getMutationId()))
                .filter(row -> provider == null || provider.equals(normalize(row.getProvider())))
                .filter(row -> reconciliationStatus == null
                        || reconciliationStatus.equals(normalize(row.getReconciliationStatus())))
                .limit(limit)
                .map(this::toProviderChangeView)
                .toList();
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SearchMarketingProviderChangeView toProviderChangeView(SearchMarketingProviderChangeDO row) {
        return new SearchMarketingProviderChangeView(
                row.getId(),
                row.getTenantId(),
                row.getSourceId(),
                row.getMutationId(),
                row.getProvider(),
                row.getExternalResourceId(),
                row.getChangeType(),
                map(row.getChangedFieldsJson()),
                row.getProviderActor(),
                row.getProviderChangedAt(),
                row.getReconciliationStatus(),
                map(row.getEvidenceJson()),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 执行 evidence 流程，围绕 evidence 完成校验、计算或结果组装。
     *
     * @param mutation mutation 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @param providerResponse provider response 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @param providerOperationId 业务对象 ID，用于定位具体记录。
     * @param confirmed confirmed 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @return 返回 evidence 流程生成的业务结果。
     */
    private Map<String, Object> evidence(SearchMarketingMutationDO mutation,
                                         Map<String, Object> providerResponse,
                                         String providerOperationId,
                                         boolean confirmed) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("mutationId", mutation.getId());
        evidence.put("mutationStatus", mutation.getStatus());
        if (providerOperationId != null) {
            evidence.put("providerOperationId", providerOperationId);
        }
        evidence.put("confirmed", confirmed);
        evidence.put("providerResponse", providerResponse);
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    /**
     * 执行 providerOperationId 流程，围绕 provider operation id 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 providerOperationId 流程中的校验、计算或对象转换。
     * @param providerResponse provider response 参数，用于 providerOperationId 流程中的校验、计算或对象转换。
     * @return 返回 provider operation id 生成的文本或业务键。
     */
    private String providerOperationId(Map<String, Object> providerResponse) {
        Object direct = firstNonNull(providerResponse, "providerOperationId", "operationId", "batchJobId");
        return direct == null ? null : trimToNull(String.valueOf(direct));
    }

    /**
     * 执行 firstNonNull 流程，围绕 first non null 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 firstNonNull 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 firstNonNull 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstNonNull 流程中的校验、计算或对象转换。
     * @return 返回 firstNonNull 流程生成的业务结果。
     */
    private Object firstNonNull(Map<String, Object> values, String... keys) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (values == null || values.isEmpty()) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null) {
                return value;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required id 计算得到的数量、金额或指标值。
     */
    private Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param expectedTenantId 业务对象 ID，用于定位具体记录。
     * @param actualTenantId 业务对象 ID，用于定位具体记录。
     * @param resource resource 参数，用于 validateTenant 流程中的校验、计算或对象转换。
     */
    private void validateTenant(Long expectedTenantId, Long actualTenantId, String resource) {
        if (!Objects.equals(expectedTenantId, actualTenantId)) {
            throw new IllegalArgumentException(resource + " does not belong to current tenant");
        }
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptional(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("search marketing reconciliation evidence is not JSON serializable", ex);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(json, OBJECT_MAP);
            return values == null ? Map.of() : ProviderWriteEvidenceSanitizer.sanitizeMap(values);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }
}
