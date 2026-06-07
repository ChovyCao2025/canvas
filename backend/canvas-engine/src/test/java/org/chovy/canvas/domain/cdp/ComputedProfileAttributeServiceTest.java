package org.chovy.canvas.domain.cdp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpComputedProfileAttributeDO;
import org.chovy.canvas.dal.dataobject.CdpComputedProfileRunDO;
import org.chovy.canvas.dal.dataobject.CdpProfileAttributeChangeLogDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.mapper.CdpComputedProfileAttributeMapper;
import org.chovy.canvas.dal.mapper.CdpComputedProfileRunMapper;
import org.chovy.canvas.dal.mapper.CdpProfileAttributeChangeLogMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComputedProfileAttributeServiceTest {

    private CdpComputedProfileAttributeMapper attributeMapper;
    private CdpComputedProfileRunMapper runMapper;
    private CdpProfileAttributeChangeLogMapper changeLogMapper;
    private CdpUserProfileMapper profileMapper;
    private ComputedProfileAttributeService service;

    @BeforeEach
    void setUp() {
        attributeMapper = mock(CdpComputedProfileAttributeMapper.class);
        runMapper = mock(CdpComputedProfileRunMapper.class);
        changeLogMapper = mock(CdpProfileAttributeChangeLogMapper.class);
        profileMapper = mock(CdpUserProfileMapper.class);
        service = new ComputedProfileAttributeService(
                attributeMapper,
                runMapper,
                changeLogMapper,
                profileMapper,
                new CdpRuleEvaluator(new ObjectMapper()));
    }

    @Test
    void previewDoesNotMutateProfilesAndReportsUnchangedCount() {
        when(attributeMapper.selectById(1L)).thenReturn(attribute(
                1L,
                42L,
                "lifecycle_stage",
                CdpComputedProfileAttributeDO.DRAFT,
                "{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2,\"then\":\"VIP\"}"));
        when(profileMapper.selectList(any())).thenReturn(List.of(
                profile(10L, 42L, "u1", "{\"paidCount\":2,\"city\":\"Shanghai\"}"),
                profile(11L, 42L, "u2", "{\"paidCount\":2,\"lifecycle_stage\":\"VIP\"}"),
                profile(12L, 42L, "u3", "{\"paidCount\":0}")));

        ComputedProfileAttributeService.PreviewResult result = service.preview(42L, 1L);

        assertThat(result.scannedCount()).isEqualTo(3);
        assertThat(result.matchedCount()).isEqualTo(2);
        assertThat(result.changedCount()).isEqualTo(1);
        assertThat(result.unchangedCount()).isEqualTo(1);
        assertThat(result.samples()).extracting(ComputedProfileAttributeService.PreviewSample::userId)
                .containsExactly("u1");
        verify(profileMapper, never()).updateById(any(CdpUserProfileDO.class));
        verify(changeLogMapper, never()).insert(any(CdpProfileAttributeChangeLogDO.class));
    }

    @Test
    void runNowWritesComputedValueAndChangeLogWhilePreservingUnrelatedProperties() {
        when(attributeMapper.selectById(1L)).thenReturn(attribute(
                1L,
                42L,
                "lifecycle_stage",
                CdpComputedProfileAttributeDO.ACTIVE,
                "{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2,\"then\":\"VIP\"}"));
        when(profileMapper.selectList(any())).thenReturn(List.of(
                profile(10L, 42L, "u1", "{\"paidCount\":2,\"city\":\"Shanghai\"}")));
        assignRunId(99L);

        ComputedProfileAttributeService.RunResult result = service.runNow(42L, 1L, "operator-1");

        assertThat(result.runId()).isEqualTo(99L);
        assertThat(result.changedCount()).isEqualTo(1);
        ArgumentCaptor<CdpUserProfileDO> profileCaptor = ArgumentCaptor.forClass(CdpUserProfileDO.class);
        verify(profileMapper).updateById(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getPropertiesJson())
                .contains("\"city\":\"Shanghai\"")
                .contains("\"lifecycle_stage\":\"VIP\"");

        ArgumentCaptor<CdpProfileAttributeChangeLogDO> changeCaptor =
                ArgumentCaptor.forClass(CdpProfileAttributeChangeLogDO.class);
        verify(changeLogMapper).insert(changeCaptor.capture());
        assertThat(changeCaptor.getValue().getTenantId()).isEqualTo(42L);
        assertThat(changeCaptor.getValue().getUserId()).isEqualTo("u1");
        assertThat(changeCaptor.getValue().getAttrCode()).isEqualTo("lifecycle_stage");
        assertThat(changeCaptor.getValue().getNewValue()).isEqualTo("VIP");
        assertThat(changeCaptor.getValue().getSourceRunId()).isEqualTo(99L);
    }

    @Test
    void runNowDoesNotMutateProfilesFromOtherTenants() {
        when(attributeMapper.selectById(1L)).thenReturn(attribute(
                1L,
                42L,
                "lifecycle_stage",
                CdpComputedProfileAttributeDO.ACTIVE,
                "{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2,\"then\":\"VIP\"}"));
        when(profileMapper.selectList(any())).thenReturn(List.of(
                profile(10L, 42L, "u1", "{\"paidCount\":2}"),
                profile(11L, 77L, "u2", "{\"paidCount\":2}")));
        assignRunId(100L);

        ComputedProfileAttributeService.RunResult result = service.runNow(42L, 1L, "operator-1");

        assertThat(result.scannedCount()).isEqualTo(1);
        ArgumentCaptor<CdpUserProfileDO> profileCaptor = ArgumentCaptor.forClass(CdpUserProfileDO.class);
        verify(profileMapper).updateById(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getUserId()).isEqualTo("u1");
    }

    @Test
    void activateRejectsInvalidExpression() {
        when(attributeMapper.selectById(1L)).thenReturn(attribute(
                1L,
                42L,
                "bad_attr",
                CdpComputedProfileAttributeDO.DRAFT,
                "{\"field\":\"paidCount\",\"op\":\"UNKNOWN\",\"value\":2}"));

        assertThatThrownBy(() -> service.activate(42L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported rule operator");
    }

    @Test
    void eventDrivenRunIsIdempotentBySourceEventId() {
        doThrow(new DuplicateKeyException("duplicate"))
                .when(runMapper)
                .insert(any(CdpComputedProfileRunDO.class));

        ComputedProfileAttributeService.RunResult result = service.runForEvent(42L, 1L, "evt-1");

        assertThat(result.status()).isEqualTo(CdpComputedProfileRunDO.DUPLICATED);
        verify(profileMapper, never()).updateById(any(CdpUserProfileDO.class));
    }

    @Test
    void pausedDefinitionDoesNotRun() {
        when(attributeMapper.selectById(1L)).thenReturn(attribute(
                1L,
                42L,
                "lifecycle_stage",
                CdpComputedProfileAttributeDO.PAUSED,
                "{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2,\"then\":\"VIP\"}"));

        assertThatThrownBy(() -> service.runNow(42L, 1L, "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    void runHistoryAndChangeHistoryExposeAuditRows() {
        CdpComputedProfileRunDO run = new CdpComputedProfileRunDO();
        run.setId(99L);
        run.setAttrId(1L);
        CdpProfileAttributeChangeLogDO change = new CdpProfileAttributeChangeLogDO();
        change.setUserId("u1");
        change.setOldValue("Lead");
        change.setNewValue("VIP");
        when(attributeMapper.selectById(1L)).thenReturn(attribute(
                1L,
                42L,
                "lifecycle_stage",
                CdpComputedProfileAttributeDO.ACTIVE,
                "{\"field\":\"paidCount\",\"op\":\">=\",\"value\":2,\"then\":\"VIP\"}"));
        when(runMapper.selectList(any())).thenReturn(List.of(run));
        when(changeLogMapper.selectList(any())).thenReturn(List.of(change));

        assertThat(service.listRuns(42L, 1L, 50)).containsExactly(run);
        assertThat(service.listChangeLogs(42L, 1L, "u1", 50)).containsExactly(change);
    }

    private void assignRunId(Long id) {
        doAnswer(invocation -> {
            CdpComputedProfileRunDO run = invocation.getArgument(0);
            run.setId(id);
            return 1;
        }).when(runMapper).insert(any(CdpComputedProfileRunDO.class));
    }

    private CdpComputedProfileAttributeDO attribute(Long id, Long tenantId, String attrCode, String status, String expressionJson) {
        CdpComputedProfileAttributeDO row = new CdpComputedProfileAttributeDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setAttrCode(attrCode);
        row.setDisplayName(attrCode);
        row.setValueType("STRING");
        row.setComputeType("RULE");
        row.setRefreshMode("MANUAL");
        row.setStatus(status);
        row.setExpressionJson(expressionJson);
        return row;
    }

    private CdpUserProfileDO profile(Long id, Long tenantId, String userId, String propertiesJson) {
        CdpUserProfileDO row = new CdpUserProfileDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setStatus("ACTIVE");
        row.setPropertiesJson(propertiesJson);
        return row;
    }
}
