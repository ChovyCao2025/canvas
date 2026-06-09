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
/**
 * CdpLineageService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpLineageService {
    private final CdpComputedTagDependencyMapper dependencyMapper;
    private final AudienceDefinitionMapper audienceMapper;
    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;

    /**
     * LineageImpact 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record LineageImpact(String objectType, String objectId, String objectName, String referencePath) {
    }

    /**
     * ImpactCheck 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ImpactCheck(boolean allowed, String reason, List<LineageImpact> impacts) {
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<LineageImpact> findTagLineage(Long tenantId, String tagCode) {
        Long normalizedTenantId = normalizeTenantId(tenantId);
        String normalizedTagCode = requireText(tagCode, "tagCode");
        List<LineageImpact> impacts = new ArrayList<>();

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        dependencyMapper.selectList(new LambdaQueryWrapper<CdpComputedTagDependencyDO>()
                        .eq(CdpComputedTagDependencyDO::getTenantId, normalizedTenantId)
                        .eq(CdpComputedTagDependencyDO::getDependsOnTagCode, normalizedTagCode))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
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

        // 汇总前面计算出的状态和明细，返回给调用方。
        return impacts;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @param oldValueType 类型标识，用于选择对应处理分支。
     * @param newValueType 类型标识，用于选择对应处理分支。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回布尔判断结果。
     */
    public ImpactCheck checkDelete(Long tenantId, String tagCode) {
        List<LineageImpact> impacts = findTagLineage(tenantId, tagCode);
        return impacts.isEmpty()
                ? new ImpactCheck(true, null, impacts)
                : new ImpactCheck(false, "DELETE_HAS_DOWNSTREAM_IMPACT", impacts);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @param tagCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 references tag 的布尔判断结果。
     */
    private boolean referencesTag(String json, String tagCode) {
        return json != null && (json.contains("\"tag." + tagCode + "\"") || json.contains("\"" + tagCode + "\""));
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeType(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toUpperCase();
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
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }
}
