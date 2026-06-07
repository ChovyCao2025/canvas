package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.dataobject.CdpComputedTagDependencyDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dal.mapper.CdpComputedTagDependencyMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdpLineageServiceTest {

    @Test
    void lineageFindsTagDependencyAudienceRuleAndCanvasGraphReferences() {
        CdpComputedTagDependencyMapper dependencyMapper = mock(CdpComputedTagDependencyMapper.class);
        AudienceDefinitionMapper audienceMapper = mock(AudienceDefinitionMapper.class);
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper canvasVersionMapper = mock(CanvasVersionMapper.class);
        when(dependencyMapper.selectList(any())).thenReturn(List.of(dependency("high_value_user", "vip_likely")));
        when(audienceMapper.selectList(any())).thenReturn(List.of(audience()));
        when(canvasMapper.selectList(any())).thenReturn(List.of(canvas()));
        when(canvasVersionMapper.selectById(20L)).thenReturn(version());

        CdpLineageService service = new CdpLineageService(
                dependencyMapper,
                audienceMapper,
                canvasMapper,
                canvasVersionMapper);

        List<CdpLineageService.LineageImpact> impacts = service.findTagLineage(42L, "vip_likely");

        assertThat(impacts).extracting(CdpLineageService.LineageImpact::objectType)
                .containsExactly("COMPUTED_TAG", "AUDIENCE", "CANVAS_VERSION");
        assertThat(impacts).extracting(CdpLineageService.LineageImpact::referencePath)
                .contains("cdp_computed_tag_dependency.depends_on_tag_code",
                        "audience_definition.rule_json",
                        "canvas_version.graph_json");
    }

    @Test
    void incompatibleTypeChangeReturnsBlockedImpactCheck() {
        CdpComputedTagDependencyMapper dependencyMapper = mock(CdpComputedTagDependencyMapper.class);
        when(dependencyMapper.selectList(any())).thenReturn(List.of(dependency("high_value_user", "vip_likely")));
        CdpLineageService service = new CdpLineageService(
                dependencyMapper,
                mock(AudienceDefinitionMapper.class),
                mock(CanvasMapper.class),
                mock(CanvasVersionMapper.class));

        CdpLineageService.ImpactCheck result = service.checkTypeChange(42L, "vip_likely", "BOOLEAN", "NUMBER");

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("INCOMPATIBLE_TYPE_CHANGE");
        assertThat(result.impacts()).hasSize(1);
    }

    private CdpComputedTagDependencyDO dependency(String tagCode, String dependsOn) {
        CdpComputedTagDependencyDO row = new CdpComputedTagDependencyDO();
        row.setTenantId(42L);
        row.setTagCode(tagCode);
        row.setDependsOnTagCode(dependsOn);
        return row;
    }

    private AudienceDefinitionDO audience() {
        AudienceDefinitionDO row = new AudienceDefinitionDO();
        row.setId(10L);
        row.setTenantId(42L);
        row.setName("VIP campaign audience");
        row.setRuleJson("{\"field\":\"tag.vip_likely\",\"op\":\"=\",\"value\":\"true\"}");
        return row;
    }

    private CanvasDO canvas() {
        CanvasDO row = new CanvasDO();
        row.setId(30L);
        row.setTenantId(42L);
        row.setName("VIP journey");
        row.setPublishedVersionId(20L);
        return row;
    }

    private CanvasVersionDO version() {
        CanvasVersionDO row = new CanvasVersionDO();
        row.setId(20L);
        row.setTenantId(42L);
        row.setCanvasId(30L);
        row.setGraphJson("{\"nodes\":[{\"id\":\"n1\",\"data\":{\"field\":\"tag.vip_likely\"}}]}");
        return row;
    }
}
