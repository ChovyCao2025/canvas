package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CdpComputedProfileAttributeDO;
import org.chovy.canvas.dal.dataobject.CdpComputedProfileRunDO;
import org.chovy.canvas.dal.dataobject.CdpProfileAttributeChangeLogDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.mapper.CdpComputedProfileAttributeMapper;
import org.chovy.canvas.dal.mapper.CdpComputedProfileRunMapper;
import org.chovy.canvas.dal.mapper.CdpProfileAttributeChangeLogMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
/**
 * ComputedProfileAttributeService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class ComputedProfileAttributeService {
    private static final Set<String> SUPPORTED_VALUE_TYPES = Set.of("STRING", "NUMBER", "BOOLEAN", "JSON");
    private static final Set<String> SUPPORTED_COMPUTE_TYPES = Set.of("RULE", "EXPR");

    private final CdpComputedProfileAttributeMapper attributeMapper;
    private final CdpComputedProfileRunMapper runMapper;
    private final CdpProfileAttributeChangeLogMapper changeLogMapper;
    private final CdpUserProfileMapper profileMapper;
    private final CdpRuleEvaluator ruleEvaluator;

    /**
     * PreviewSample 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PreviewSample(String userId, String oldValue, String newValue) {
    }

    /**
     * PreviewResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PreviewResult(long scannedCount, long matchedCount, long changedCount,
                                long unchangedCount, List<PreviewSample> samples) {
    }

    /**
     * RunResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RunResult(Long runId, String status, long scannedCount, long matchedCount,
                            long changedCount, long unchangedCount) {
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<CdpComputedProfileAttributeDO> list(Long tenantId) {
        return attributeMapper.selectList(new LambdaQueryWrapper<CdpComputedProfileAttributeDO>()
                .eq(CdpComputedProfileAttributeDO::getTenantId, normalizeTenantId(tenantId))
                .orderByDesc(CdpComputedProfileAttributeDO::getUpdatedAt));
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @param createdBy created by 参数，用于 create 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public CdpComputedProfileAttributeDO create(Long tenantId, CdpComputedProfileAttributeDO body, String createdBy) {
        body.setTenantId(normalizeTenantId(tenantId));
        body.setAttrCode(requireText(body.getAttrCode(), "attrCode"));
        body.setDisplayName(body.getDisplayName() == null || body.getDisplayName().isBlank()
                ? body.getAttrCode()
                : body.getDisplayName().trim());
        body.setValueType(validateEnum(defaultText(body.getValueType(), "STRING").toUpperCase(), "valueType", SUPPORTED_VALUE_TYPES));
        body.setComputeType(validateEnum(defaultText(body.getComputeType(), "RULE").toUpperCase(), "computeType", SUPPORTED_COMPUTE_TYPES));
        ruleEvaluator.validate(body.getExpressionJson(), body.getComputeType());
        body.setRefreshMode(defaultText(body.getRefreshMode(), "MANUAL"));
        body.setStatus(CdpComputedProfileAttributeDO.DRAFT);
        body.setCreatedBy(createdBy);
        attributeMapper.insert(body);
        return body;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attrId 业务对象 ID，用于定位具体记录。
     */
    public void activate(Long tenantId, Long attrId) {
        CdpComputedProfileAttributeDO def = requireDefinition(tenantId, attrId, false);
        validateDefinition(def);
        def.setStatus(CdpComputedProfileAttributeDO.ACTIVE);
        attributeMapper.updateById(def);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attrId 业务对象 ID，用于定位具体记录。
     */
    public void pause(Long tenantId, Long attrId) {
        CdpComputedProfileAttributeDO def = requireDefinition(tenantId, attrId, false);
        def.setStatus(CdpComputedProfileAttributeDO.PAUSED);
        attributeMapper.updateById(def);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attrId 业务对象 ID，用于定位具体记录。
     * @return 返回 preview 流程生成的业务结果。
     */
    public PreviewResult preview(Long tenantId, Long attrId) {
        CdpComputedProfileAttributeDO def = requireDefinition(tenantId, attrId, false);
        return evaluateProfiles(def, false, null).preview();
    }

    @Transactional
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attrId 业务对象 ID，用于定位具体记录。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public RunResult runNow(Long tenantId, Long attrId, String operator) {
        // 准备本次处理所需的上下文和中间变量。
        CdpComputedProfileAttributeDO def = requireDefinition(tenantId, attrId, true);
        CdpComputedProfileRunDO run = startRun(normalizeTenantId(tenantId), attrId, null);
        try {
            EvaluationResult evaluated = evaluateProfiles(def, true, run.getId());
            run.setStatus(CdpComputedProfileRunDO.SUCCESS);
            run.setScannedCount(evaluated.scannedCount);
            run.setMatchedCount(evaluated.matchedCount);
            run.setChangedCount(evaluated.changedCount);
            run.setUnchangedCount(evaluated.unchangedCount);
            run.setFinishedAt(LocalDateTime.now());
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            runMapper.updateById(run);
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new RunResult(run.getId(), run.getStatus(), evaluated.scannedCount,
                    evaluated.matchedCount, evaluated.changedCount, evaluated.unchangedCount);
        } catch (RuntimeException ex) {
            run.setStatus(CdpComputedProfileRunDO.FAILED);
            run.setErrorMessage(ex.getMessage());
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            throw ex;
        }
    }

    @Transactional
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attrId 业务对象 ID，用于定位具体记录。
     * @param sourceEventId 业务对象 ID，用于定位具体记录。
     * @return 返回流程执行后的业务结果。
     */
    public RunResult runForEvent(Long tenantId, Long attrId, String sourceEventId) {
        try {
            CdpComputedProfileRunDO run = startRun(normalizeTenantId(tenantId), attrId, sourceEventId);
            CdpComputedProfileAttributeDO def = requireDefinition(tenantId, attrId, true);
            EvaluationResult evaluated = evaluateProfiles(def, true, run.getId());
            run.setStatus(CdpComputedProfileRunDO.SUCCESS);
            run.setScannedCount(evaluated.scannedCount);
            run.setMatchedCount(evaluated.matchedCount);
            run.setChangedCount(evaluated.changedCount);
            run.setUnchangedCount(evaluated.unchangedCount);
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            return new RunResult(run.getId(), run.getStatus(), evaluated.scannedCount,
                    evaluated.matchedCount, evaluated.changedCount, evaluated.unchangedCount);
        } catch (DuplicateKeyException duplicate) {
            return new RunResult(null, CdpComputedProfileRunDO.DUPLICATED, 0, 0, 0, 0);
        }
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attrId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<CdpComputedProfileRunDO> listRuns(Long tenantId, Long attrId, Integer limit) {
        CdpComputedProfileAttributeDO def = requireDefinition(tenantId, attrId, false);
        return runMapper.selectList(new LambdaQueryWrapper<CdpComputedProfileRunDO>()
                .eq(CdpComputedProfileRunDO::getTenantId, def.getTenantId())
                .eq(CdpComputedProfileRunDO::getAttrId, def.getId())
                .orderByDesc(CdpComputedProfileRunDO::getStartedAt)
                .last("LIMIT " + normalizeLimit(limit)));
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attrId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<CdpProfileAttributeChangeLogDO> listChangeLogs(Long tenantId, Long attrId, String userId, Integer limit) {
        CdpComputedProfileAttributeDO def = requireDefinition(tenantId, attrId, false);
        LambdaQueryWrapper<CdpProfileAttributeChangeLogDO> wrapper = new LambdaQueryWrapper<CdpProfileAttributeChangeLogDO>()
                .eq(CdpProfileAttributeChangeLogDO::getTenantId, def.getTenantId())
                .eq(CdpProfileAttributeChangeLogDO::getAttrCode, def.getAttrCode())
                .orderByDesc(CdpProfileAttributeChangeLogDO::getChangedAt)
                .last("LIMIT " + normalizeLimit(limit));
        if (userId != null && !userId.isBlank()) {
            wrapper.eq(CdpProfileAttributeChangeLogDO::getUserId, userId.trim());
        }
        return changeLogMapper.selectList(wrapper);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param def def 参数，用于 evaluateProfiles 流程中的校验、计算或对象转换。
     * @param mutate mutate 参数，用于 evaluateProfiles 流程中的校验、计算或对象转换。
     * @param runId 业务对象 ID，用于定位具体记录。
     * @return 返回 evaluateProfiles 流程生成的业务结果。
     */
    private EvaluationResult evaluateProfiles(CdpComputedProfileAttributeDO def, boolean mutate, Long runId) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<CdpUserProfileDO> profiles = profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                .eq(CdpUserProfileDO::getTenantId, def.getTenantId())
                .eq(CdpUserProfileDO::getStatus, "ACTIVE"));
        EvaluationResult result = new EvaluationResult();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpUserProfileDO profile : profiles) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (!def.getTenantId().equals(profile.getTenantId())) {
                continue;
            }
            result.scannedCount++;
            Map<String, Object> properties = new LinkedHashMap<>(ruleEvaluator.readProperties(profile.getPropertiesJson()));
            CdpRuleEvaluator.Evaluation evaluation = ruleEvaluator.evaluate(def.getExpressionJson(), def.getComputeType(), properties);
            if (!evaluation.matched()) {
                continue;
            }
            result.matchedCount++;
            String oldValue = stringify(properties.get(def.getAttrCode()));
            String newValue = stringify(evaluation.value());
            if (Objects.equals(oldValue, newValue)) {
                result.unchangedCount++;
                continue;
            }
            result.changedCount++;
            if (result.samples.size() < 20) {
                result.samples.add(new PreviewSample(profile.getUserId(), oldValue, newValue));
            }
            if (mutate) {
                properties.put(def.getAttrCode(), evaluation.value());
                profile.setPropertiesJson(ruleEvaluator.writeProperties(properties));
                profileMapper.updateById(profile);
                insertChangeLog(def, profile.getUserId(), oldValue, newValue, runId);
            }
        }
        return result;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param def def 参数，用于 insertChangeLog 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param oldValue 待处理值，用于规则计算或转换。
     * @param newValue 待处理值，用于规则计算或转换。
     * @param runId 业务对象 ID，用于定位具体记录。
     */
    private void insertChangeLog(CdpComputedProfileAttributeDO def, String userId,
                                 String oldValue, String newValue, Long runId) {
        CdpProfileAttributeChangeLogDO change = new CdpProfileAttributeChangeLogDO();
        change.setTenantId(def.getTenantId());
        change.setAttrCode(def.getAttrCode());
        change.setUserId(userId);
        change.setOldValue(oldValue);
        change.setNewValue(newValue);
        change.setSourceRunId(runId == null ? 0L : runId);
        change.setChangedAt(LocalDateTime.now());
        changeLogMapper.insert(change);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attrId 业务对象 ID，用于定位具体记录。
     * @param sourceEventId 业务对象 ID，用于定位具体记录。
     * @return 返回 startRun 流程生成的业务结果。
     */
    private CdpComputedProfileRunDO startRun(Long tenantId, Long attrId, String sourceEventId) {
        CdpComputedProfileRunDO run = new CdpComputedProfileRunDO();
        run.setTenantId(tenantId);
        run.setAttrId(attrId);
        run.setSourceEventId(sourceEventId);
        run.setStatus(CdpComputedProfileRunDO.RUNNING);
        run.setScannedCount(0L);
        run.setMatchedCount(0L);
        run.setChangedCount(0L);
        run.setUnchangedCount(0L);
        run.setStartedAt(LocalDateTime.now());
        runMapper.insert(run);
        return run;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attrId 业务对象 ID，用于定位具体记录。
     * @param activeOnly active only 参数，用于 requireDefinition 流程中的校验、计算或对象转换。
     * @return 返回 requireDefinition 流程生成的业务结果。
     */
    private CdpComputedProfileAttributeDO requireDefinition(Long tenantId, Long attrId, boolean activeOnly) {
        CdpComputedProfileAttributeDO def = attributeMapper.selectById(attrId);
        if (def == null || !normalizeTenantId(tenantId).equals(def.getTenantId())) {
            throw new IllegalArgumentException("computed profile attribute not found: " + attrId);
        }
        if (activeOnly && !CdpComputedProfileAttributeDO.ACTIVE.equals(def.getStatus())) {
            throw new IllegalStateException("computed profile attribute is not ACTIVE");
        }
        return def;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 stringify 生成的文本或业务键。
     */
    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultText 流程中的校验、计算或对象转换。
     * @return 返回 default text 生成的文本或业务键。
     */
    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @param supported supported 参数，用于 validateEnum 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private String validateEnum(String value, String fieldName, Set<String> supported) {
        if (!supported.contains(value)) {
            throw new IllegalArgumentException(fieldName + " is not supported: " + value);
        }
        return value;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param def def 参数，用于 validateDefinition 流程中的校验、计算或对象转换。
     */
    private void validateDefinition(CdpComputedProfileAttributeDO def) {
        validateEnum(defaultText(def.getValueType(), "STRING").toUpperCase(), "valueType", SUPPORTED_VALUE_TYPES);
        validateEnum(defaultText(def.getComputeType(), "RULE").toUpperCase(), "computeType", SUPPORTED_COMPUTE_TYPES);
        ruleEvaluator.validate(def.getExpressionJson(), def.getComputeType());
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 500);
    }

    /**
     * EvaluationResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class EvaluationResult {
        long scannedCount;
        long matchedCount;
        long changedCount;
        long unchangedCount;
        final List<PreviewSample> samples = new ArrayList<>();

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 preview 流程生成的业务结果。
         */
        PreviewResult preview() {
            return new PreviewResult(scannedCount, matchedCount, changedCount, unchangedCount, samples);
        }
    }
}
