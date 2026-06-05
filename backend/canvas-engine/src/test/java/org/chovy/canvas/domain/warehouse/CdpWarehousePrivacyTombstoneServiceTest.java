package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyErasureRequestDO;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacySubjectTombstoneDO;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacyErasureRequestMapper;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacySubjectTombstoneMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehousePrivacyTombstoneServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-05T04:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);

    @Test
    void createHashesAndMasksSubjectWithoutExposingRawValue() {
        CdpWarehousePrivacySubjectTombstoneMapper mapper =
                mock(CdpWarehousePrivacySubjectTombstoneMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        doAnswer(invocation -> {
            CdpWarehousePrivacySubjectTombstoneDO row = invocation.getArgument(0);
            row.setId(101L);
            row.setCreatedAt(NOW);
            row.setUpdatedAt(NOW);
            return 1;
        }).when(mapper).insert(any(CdpWarehousePrivacySubjectTombstoneDO.class));
        CdpWarehousePrivacyTombstoneService service =
                new CdpWarehousePrivacyTombstoneService(mapper, CLOCK);

        CdpWarehousePrivacyTombstoneService.TombstoneView view = service.create(9L,
                new CdpWarehousePrivacyTombstoneService.TombstoneCommand(
                        "user_id", "user-123456", 201L, "dsr-201",
                        "GDPR delete", "privacy-ops"));

        assertThat(view.id()).isEqualTo(101L);
        assertThat(view.subjectType()).isEqualTo("USER_ID");
        assertThat(view.subjectHash()).hasSize(64);
        assertThat(view.subjectHash()).doesNotContain("user-123456");
        assertThat(view.subjectRefMasked()).isEqualTo("us***56");
        assertThat(view.subjectRefMasked()).doesNotContain("user-123456");
        assertThat(view.status()).isEqualTo("ACTIVE");
        ArgumentCaptor<CdpWarehousePrivacySubjectTombstoneDO> rowCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacySubjectTombstoneDO.class);
        verify(mapper).insert(rowCaptor.capture());
        assertThat(rowCaptor.getValue().getSubjectHash()).hasSize(64);
        assertThat(rowCaptor.getValue().getSubjectHash()).doesNotContain("user-123456");
        assertThat(rowCaptor.getValue().getSubjectRefMasked()).isEqualTo("us***56");
    }

    @Test
    void activeDecisionBlocksAndRecordsBlockedAttempt() {
        CdpWarehousePrivacySubjectTombstoneMapper mapper =
                mock(CdpWarehousePrivacySubjectTombstoneMapper.class);
        CdpWarehousePrivacySubjectTombstoneDO row = tombstone(101L, "ACTIVE");
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row));
        CdpWarehousePrivacyTombstoneService service =
                new CdpWarehousePrivacyTombstoneService(mapper, CLOCK);

        CdpWarehousePrivacyTombstoneService.TombstoneDecision decision =
                service.decide(9L, "user_id", "user-123456");

        assertThat(decision.blocked()).isTrue();
        assertThat(decision.subjectType()).isEqualTo("USER_ID");
        assertThat(decision.subjectHash()).hasSize(64);
        assertThat(decision.subjectRefMasked()).isEqualTo("us***56");
        assertThat(decision.tombstoneId()).isEqualTo(101L);

        assertThatThrownBy(() -> service.enforceNotBlocked(9L, "USER_ID", "user-123456", "CDP_EVENT_INGESTION"))
                .isInstanceOf(CdpWarehousePrivacyTombstoneService.PrivacyTombstoneViolationException.class)
                .hasMessageContaining("privacy tombstone blocks CDP_EVENT_INGESTION")
                .hasMessageContaining("us***56")
                .hasMessageNotContaining("user-123456");
        verify(mapper).recordBlocked(eq(9L), eq("USER_ID"), anyString(), eq(NOW));
    }

    @Test
    void createFromPassErasureRequestUsesStoredHashAndMaskedReference() {
        CdpWarehousePrivacySubjectTombstoneMapper mapper =
                mock(CdpWarehousePrivacySubjectTombstoneMapper.class);
        CdpWarehousePrivacyErasureRequestMapper erasureRequestMapper =
                mock(CdpWarehousePrivacyErasureRequestMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(erasureRequestMapper.selectById(201L)).thenReturn(erasureRequest("PASS"));
        doAnswer(invocation -> {
            CdpWarehousePrivacySubjectTombstoneDO row = invocation.getArgument(0);
            row.setId(101L);
            row.setCreatedAt(NOW);
            row.setUpdatedAt(NOW);
            return 1;
        }).when(mapper).insert(any(CdpWarehousePrivacySubjectTombstoneDO.class));
        CdpWarehousePrivacyTombstoneService service =
                new CdpWarehousePrivacyTombstoneService(mapper, erasureRequestMapper, CLOCK);

        CdpWarehousePrivacyTombstoneService.TombstoneView view =
                service.createFromErasureRequest(9L,
                        new CdpWarehousePrivacyTombstoneService.ErasureRequestTombstoneCommand(
                                201L, null, "privacy-ops"));

        assertThat(view.id()).isEqualTo(101L);
        assertThat(view.subjectHash()).isEqualTo("erasure-hash-201");
        assertThat(view.subjectRefMasked()).isEqualTo("us***56");
        assertThat(view.sourceRequestId()).isEqualTo(201L);
        assertThat(view.sourceRequestKey()).isEqualTo("dsr-201");
        ArgumentCaptor<CdpWarehousePrivacySubjectTombstoneDO> rowCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacySubjectTombstoneDO.class);
        verify(mapper).insert(rowCaptor.capture());
        assertThat(rowCaptor.getValue().getSubjectHash()).isEqualTo("erasure-hash-201");
        assertThat(rowCaptor.getValue().getSubjectRefMasked()).isEqualTo("us***56");
        assertThat(rowCaptor.getValue().getSubjectRefMasked()).doesNotContain("user-123456");
    }

    @Test
    void createFromErasureRequestRejectsRequestsThatAreNotPass() {
        CdpWarehousePrivacySubjectTombstoneMapper mapper =
                mock(CdpWarehousePrivacySubjectTombstoneMapper.class);
        CdpWarehousePrivacyErasureRequestMapper erasureRequestMapper =
                mock(CdpWarehousePrivacyErasureRequestMapper.class);
        when(erasureRequestMapper.selectById(201L)).thenReturn(erasureRequest("RUNNING"));
        CdpWarehousePrivacyTombstoneService service =
                new CdpWarehousePrivacyTombstoneService(mapper, erasureRequestMapper, CLOCK);

        assertThatThrownBy(() -> service.createFromErasureRequest(9L,
                new CdpWarehousePrivacyTombstoneService.ErasureRequestTombstoneCommand(
                        201L, null, "privacy-ops")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be PASS");
        verify(mapper, never()).insert(any(CdpWarehousePrivacySubjectTombstoneDO.class));
    }

    @Test
    void revokedDecisionDoesNotBlockOrRecordAuditAttempt() {
        CdpWarehousePrivacySubjectTombstoneMapper mapper =
                mock(CdpWarehousePrivacySubjectTombstoneMapper.class);
        CdpWarehousePrivacySubjectTombstoneDO row = tombstone(101L, "REVOKED");
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row));
        CdpWarehousePrivacyTombstoneService service =
                new CdpWarehousePrivacyTombstoneService(mapper, CLOCK);

        CdpWarehousePrivacyTombstoneService.TombstoneDecision decision =
                service.enforceNotBlocked(9L, "USER_ID", "user-123456", "CDP_EVENT_INGESTION");

        assertThat(decision.blocked()).isFalse();
        verify(mapper, never()).recordBlocked(any(), anyString(), anyString(), any());
    }

    private CdpWarehousePrivacySubjectTombstoneDO tombstone(Long id, String status) {
        CdpWarehousePrivacySubjectTombstoneDO row = new CdpWarehousePrivacySubjectTombstoneDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setSubjectType("USER_ID");
        row.setSubjectHash("hash-" + id);
        row.setSubjectRefMasked("us***56");
        row.setStatus(status);
        row.setSourceRequestId(201L);
        row.setSourceRequestKey("dsr-201");
        row.setReason("GDPR delete");
        row.setBlockedEventCount(0L);
        row.setCreatedBy("privacy-ops");
        row.setCreatedAt(NOW);
        row.setUpdatedAt(NOW);
        return row;
    }

    private CdpWarehousePrivacyErasureRequestDO erasureRequest(String status) {
        CdpWarehousePrivacyErasureRequestDO row = new CdpWarehousePrivacyErasureRequestDO();
        row.setId(201L);
        row.setTenantId(9L);
        row.setRequestKey("dsr-201");
        row.setSubjectType("USER_ID");
        row.setSubjectHash("erasure-hash-201");
        row.setSubjectRefMasked("us***56");
        row.setReason("GDPR delete");
        row.setRequestedBy("privacy-requester");
        row.setStatus(status);
        row.setCreatedAt(NOW);
        row.setUpdatedAt(NOW);
        return row;
    }
}
