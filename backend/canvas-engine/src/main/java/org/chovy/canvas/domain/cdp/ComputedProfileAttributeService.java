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

@Service
@RequiredArgsConstructor
public class ComputedProfileAttributeService {
    private final CdpComputedProfileAttributeMapper attributeMapper;
    private final CdpComputedProfileRunMapper runMapper;
    private final CdpProfileAttributeChangeLogMapper changeLogMapper;
    private final CdpUserProfileMapper profileMapper;
    private final CdpRuleEvaluator ruleEvaluator;

    public record PreviewSample(String userId, String oldValue, String newValue) {
    }

    public record PreviewResult(long scannedCount, long matchedCount, long changedCount, List<PreviewSample> samples) {
    }

    public record RunResult(Long runId, String status, long scannedCount, long matchedCount,
                            long changedCount, long unchangedCount) {
    }

    public List<CdpComputedProfileAttributeDO> list(Long tenantId) {
        return attributeMapper.selectList(new LambdaQueryWrapper<CdpComputedProfileAttributeDO>()
                .eq(CdpComputedProfileAttributeDO::getTenantId, normalizeTenantId(tenantId))
                .orderByDesc(CdpComputedProfileAttributeDO::getUpdatedAt));
    }

    public CdpComputedProfileAttributeDO create(Long tenantId, CdpComputedProfileAttributeDO body, String createdBy) {
        ruleEvaluator.validate(body.getExpressionJson());
        body.setTenantId(normalizeTenantId(tenantId));
        body.setAttrCode(requireText(body.getAttrCode(), "attrCode"));
        body.setDisplayName(body.getDisplayName() == null || body.getDisplayName().isBlank()
                ? body.getAttrCode()
                : body.getDisplayName().trim());
        body.setValueType(defaultText(body.getValueType(), "STRING"));
        body.setComputeType(defaultText(body.getComputeType(), "RULE"));
        body.setRefreshMode(defaultText(body.getRefreshMode(), "MANUAL"));
        body.setStatus(CdpComputedProfileAttributeDO.DRAFT);
        body.setCreatedBy(createdBy);
        attributeMapper.insert(body);
        return body;
    }

    public void activate(Long tenantId, Long attrId) {
        CdpComputedProfileAttributeDO def = requireDefinition(tenantId, attrId, false);
        ruleEvaluator.validate(def.getExpressionJson());
        def.setStatus(CdpComputedProfileAttributeDO.ACTIVE);
        attributeMapper.updateById(def);
    }

    public PreviewResult preview(Long tenantId, Long attrId) {
        CdpComputedProfileAttributeDO def = requireDefinition(tenantId, attrId, false);
        return evaluateProfiles(def, false, null).preview();
    }

    @Transactional
    public RunResult runNow(Long tenantId, Long attrId, String operator) {
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
            runMapper.updateById(run);
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

    private EvaluationResult evaluateProfiles(CdpComputedProfileAttributeDO def, boolean mutate, Long runId) {
        List<CdpUserProfileDO> profiles = profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                .eq(CdpUserProfileDO::getStatus, "ACTIVE"));
        EvaluationResult result = new EvaluationResult();
        for (CdpUserProfileDO profile : profiles) {
            result.scannedCount++;
            Map<String, Object> properties = new LinkedHashMap<>(ruleEvaluator.readProperties(profile.getPropertiesJson()));
            CdpRuleEvaluator.Evaluation evaluation = ruleEvaluator.evaluate(def.getExpressionJson(), properties);
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

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static final class EvaluationResult {
        long scannedCount;
        long matchedCount;
        long changedCount;
        long unchangedCount;
        final List<PreviewSample> samples = new ArrayList<>();

        PreviewResult preview() {
            return new PreviewResult(scannedCount, matchedCount, changedCount, samples);
        }
    }
}
