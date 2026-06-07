package org.chovy.canvas.domain.cdp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpComputedTagDefinitionDO;
import org.chovy.canvas.dal.dataobject.CdpComputedTagDependencyDO;
import org.chovy.canvas.dal.dataobject.CdpComputedTagRunDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.mapper.CdpComputedTagDefinitionMapper;
import org.chovy.canvas.dal.mapper.CdpComputedTagDependencyMapper;
import org.chovy.canvas.dal.mapper.CdpComputedTagRunMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComputedTagServiceTest {

    private CdpComputedTagDefinitionMapper definitionMapper;
    private CdpComputedTagDependencyMapper dependencyMapper;
    private CdpComputedTagRunMapper runMapper;
    private CdpUserProfileMapper profileMapper;
    private CdpTagService cdpTagService;
    private ComputedTagService service;

    @BeforeEach
    void setUp() {
        definitionMapper = mock(CdpComputedTagDefinitionMapper.class);
        dependencyMapper = mock(CdpComputedTagDependencyMapper.class);
        runMapper = mock(CdpComputedTagRunMapper.class);
        profileMapper = mock(CdpUserProfileMapper.class);
        cdpTagService = mock(CdpTagService.class);
        service = new ComputedTagService(
                definitionMapper,
                dependencyMapper,
                runMapper,
                profileMapper,
                cdpTagService,
                new CdpRuleEvaluator(new ObjectMapper()));
    }

    @Test
    void activateRejectsDependencyCycleWithPath() {
        when(definitionMapper.selectOne(any())).thenReturn(definition("tag_a", CdpComputedTagDefinitionDO.DRAFT));
        when(dependencyMapper.selectList(any())).thenReturn(List.of(
                dependency("tag_a", "tag_b"),
                dependency("tag_b", "tag_a")));

        assertThatThrownBy(() -> service.activate(42L, "tag_a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag_a -> tag_b -> tag_a");
    }

    @Test
    void previewDoesNotWriteUserTags() {
        when(definitionMapper.selectOne(any())).thenReturn(definition("vip_likely", CdpComputedTagDefinitionDO.DRAFT));
        when(profileMapper.selectList(any())).thenReturn(List.of(
                profile(42L, "u1", "{\"paidCount\":2}"),
                profile(42L, "u2", "{\"paidCount\":0}")));

        ComputedTagService.PreviewResult result = service.preview(42L, "vip_likely");

        assertThat(result.scannedCount()).isEqualTo(2);
        assertThat(result.matchedCount()).isEqualTo(1);
        assertThat(result.samples()).extracting(ComputedTagService.PreviewSample::userId).containsExactly("u1");
        verify(cdpTagService, never()).setTag(eq(42L), eq("u1"), any(CdpTagWriteReq.class));
    }

    @Test
    void runNowWritesTagsThroughCdpTagServiceWithDeterministicIdempotencyKey() {
        when(definitionMapper.selectOne(any())).thenReturn(definition("vip_likely", CdpComputedTagDefinitionDO.ACTIVE));
        when(profileMapper.selectList(any())).thenReturn(List.of(profile(42L, "u1", "{\"paidCount\":2}")));
        assignRunId(88L);

        ComputedTagService.RunResult result = service.runNow(42L, "vip_likely", "operator-1");

        ArgumentCaptor<CdpTagWriteReq> request = ArgumentCaptor.forClass(CdpTagWriteReq.class);
        verify(cdpTagService).setTag(eq(42L), eq("u1"), request.capture());
        assertThat(result.runId()).isEqualTo(88L);
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(request.getValue().tagCode()).isEqualTo("vip_likely");
        assertThat(request.getValue().tagValue()).isEqualTo("true");
        assertThat(request.getValue().sourceType()).isEqualTo("COMPUTED_TAG");
        assertThat(request.getValue().sourceRefId()).isEqualTo("88");
        assertThat(request.getValue().operator()).isEqualTo("operator-1");
        assertThat(request.getValue().idempotencyKey()).isEqualTo("computed-tag:88:u1:vip_likely");
    }

    @Test
    void pausedDefinitionDoesNotRun() {
        when(definitionMapper.selectOne(any())).thenReturn(definition("vip_likely", CdpComputedTagDefinitionDO.PAUSED));

        assertThatThrownBy(() -> service.runNow(42L, "vip_likely", "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ACTIVE");
    }

    private void assignRunId(Long id) {
        doAnswer(invocation -> {
            CdpComputedTagRunDO run = invocation.getArgument(0);
            run.setId(id);
            return 1;
        }).when(runMapper).insert(any(CdpComputedTagRunDO.class));
    }

    private CdpComputedTagDefinitionDO definition(String tagCode, String status) {
        CdpComputedTagDefinitionDO row = new CdpComputedTagDefinitionDO();
        row.setId(7L);
        row.setTenantId(42L);
        row.setTagCode(tagCode);
        row.setDisplayName(tagCode);
        row.setValueType("BOOLEAN");
        row.setComputeType("RULE");
        row.setExpressionJson("{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2}");
        row.setRefreshMode("MANUAL");
        row.setStatus(status);
        return row;
    }

    private CdpComputedTagDependencyDO dependency(String tagCode, String dependsOn) {
        CdpComputedTagDependencyDO row = new CdpComputedTagDependencyDO();
        row.setTenantId(42L);
        row.setTagCode(tagCode);
        row.setDependsOnTagCode(dependsOn);
        return row;
    }

    private CdpUserProfileDO profile(Long tenantId, String userId, String propertiesJson) {
        CdpUserProfileDO row = new CdpUserProfileDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setStatus("ACTIVE");
        row.setPropertiesJson(propertiesJson);
        return row;
    }
}
