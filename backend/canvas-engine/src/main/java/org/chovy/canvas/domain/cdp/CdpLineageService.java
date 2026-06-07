package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.dataobject.CdpComputedTagDependencyDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dal.mapper.CdpComputedTagDependencyMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CdpLineageService {
    private final CdpComputedTagDependencyMapper dependencyMapper;
    private final AudienceDefinitionMapper audienceMapper;
    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;

    public record LineageImpact(String objectType, String objectId, String objectName, String referencePath) {
    }

    public record ImpactCheck(boolean allowed, String reason, List<LineageImpact> impacts) {
    }

    public List<LineageImpact> findTagLineage(Long tenantId, String tagCode) {
        Long normalizedTenantId = normalizeTenantId(tenantId);
        String normalizedTagCode = requireText(tagCode, "tagCode");
        List<LineageImpact> impacts = new ArrayList<>();

        dependencyMapper.selectList(new LambdaQueryWrapper<CdpComputedTagDependencyDO>()
                        .eq(CdpComputedTagDependencyDO::getTenantId, normalizedTenantId)
                        .eq(CdpComputedTagDependencyDO::getDependsOnTagCode, normalizedTagCode))
                .forEach(edge -> impacts.add(new LineageImpact(
                        "COMPUTED_TAG",
                        edge.getTagCode(),
                        edge.getTagCode(),
                        "cdp_computed_tag_dependency.depends_on_tag_code")));

        audienceMapper.selectList(new LambdaQueryWrapper<AudienceDefinitionDO>()
                        .eq(AudienceDefinitionDO::getTenantId, normalizedTenantId))
                .stream()
                .filter(audience -> referencesTag(audience.getRuleJson(), normalizedTagCode))
                .forEach(audience -> impacts.add(new LineageImpact(
                        "AUDIENCE",
                        String.valueOf(audience.getId()),
                        audience.getName(),
                        "audience_definition.rule_json")));

        canvasMapper.selectList(new LambdaQueryWrapper<CanvasDO>()
                        .eq(CanvasDO::getTenantId, normalizedTenantId)
                        .isNotNull(CanvasDO::getPublishedVersionId))
                .forEach(canvas -> {
                    CanvasVersionDO version = canvasVersionMapper.selectById(canvas.getPublishedVersionId());
                    if (version != null && referencesTag(version.getGraphJson(), normalizedTagCode)) {
                        impacts.add(new LineageImpact(
                                "CANVAS_VERSION",
                                String.valueOf(version.getId()),
                                canvas.getName(),
                                "canvas_version.graph_json"));
                    }
                });

        return impacts;
    }

    public ImpactCheck checkTypeChange(Long tenantId, String tagCode, String oldValueType, String newValueType) {
        if (normalizeType(oldValueType).equals(normalizeType(newValueType))) {
            return new ImpactCheck(true, null, List.of());
        }
        List<LineageImpact> impacts = findTagLineage(tenantId, tagCode);
        if (impacts.isEmpty()) {
            return new ImpactCheck(true, null, impacts);
        }
        return new ImpactCheck(false, "INCOMPATIBLE_TYPE_CHANGE", impacts);
    }

    public ImpactCheck checkDelete(Long tenantId, String tagCode) {
        List<LineageImpact> impacts = findTagLineage(tenantId, tagCode);
        return impacts.isEmpty()
                ? new ImpactCheck(true, null, impacts)
                : new ImpactCheck(false, "DELETE_HAS_DOWNSTREAM_IMPACT", impacts);
    }

    private boolean referencesTag(String json, String tagCode) {
        return json != null && (json.contains("\"tag." + tagCode + "\"") || json.contains("\"" + tagCode + "\""));
    }

    private String normalizeType(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toUpperCase();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }
}
