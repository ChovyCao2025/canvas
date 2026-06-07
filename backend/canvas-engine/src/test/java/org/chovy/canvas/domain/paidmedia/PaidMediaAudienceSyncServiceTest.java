package org.chovy.canvas.domain.paidmedia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.PaidMediaAudienceDestinationDO;
import org.chovy.canvas.dal.dataobject.PaidMediaAudienceMemberDO;
import org.chovy.canvas.dal.dataobject.PaidMediaAudienceSyncRunDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceDestinationMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceMemberMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceSyncRunMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaidMediaAudienceSyncServiceTest {

    @Test
    void upsertsDestinationWithNormalizedProviderAndIdentifierTypes() {
        PaidMediaAudienceDestinationMapper destinationMapper = mock(PaidMediaAudienceDestinationMapper.class);
        doAnswer(invocation -> {
            invocation.<PaidMediaAudienceDestinationDO>getArgument(0).setId(10L);
            return 1;
        }).when(destinationMapper).insert(any(PaidMediaAudienceDestinationDO.class));
        PaidMediaAudienceSyncService service = service(destinationMapper);

        PaidMediaAudienceDestinationView view = service.upsertDestination(7L, destinationCommand(), "alice");

        assertThat(view.id()).isEqualTo(10L);
        assertThat(view.provider()).isEqualTo("META");
        assertThat(view.identifierTypes()).containsExactly("EMAIL", "PHONE");
        ArgumentCaptor<PaidMediaAudienceDestinationDO> captor =
                ArgumentCaptor.forClass(PaidMediaAudienceDestinationDO.class);
        verify(destinationMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getProvider()).isEqualTo("META");
        assertThat(captor.getValue().getIdentifierTypesJson()).contains("EMAIL", "PHONE");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("alice");
    }

    @Test
    void syncHashesEligibleIdentifiersAndAuditsSkippedUsers() {
        PaidMediaAudienceDestinationMapper destinationMapper = mock(PaidMediaAudienceDestinationMapper.class);
        PaidMediaAudienceMemberMapper memberMapper = mock(PaidMediaAudienceMemberMapper.class);
        PaidMediaAudienceSyncRunMapper runMapper = mock(PaidMediaAudienceSyncRunMapper.class);
        AudienceDefinitionMapper audienceMapper = mock(AudienceDefinitionMapper.class);
        CdpUserProfileMapper profileMapper = mock(CdpUserProfileMapper.class);
        MarketingConsentMapper consentMapper = mock(MarketingConsentMapper.class);
        when(destinationMapper.selectById(10L)).thenReturn(destination());
        when(audienceMapper.selectById(20L)).thenReturn(audience(1));
        when(profileMapper.selectOne(any()))
                .thenReturn(profile("u1", " Alice@Example.COM ", "+1 (555) 123-4567"))
                .thenReturn(profile("u2", null, " "))
                .thenReturn(profile("u3", "denied@example.com", null))
                .thenReturn(null);
        when(consentMapper.selectOne(any()))
                .thenReturn(consent())
                .thenReturn(consent())
                .thenReturn(null);
        doAnswer(invocation -> {
            invocation.<PaidMediaAudienceSyncRunDO>getArgument(0).setId(900L);
            return 1;
        }).when(runMapper).insert(any(PaidMediaAudienceSyncRunDO.class));
        PaidMediaAudienceSyncService service = service(
                destinationMapper, memberMapper, runMapper, audienceMapper, profileMapper, consentMapper);

        PaidMediaAudienceSyncRunView view = service.syncAudience(7L, new PaidMediaAudienceSyncCommand(
                10L,
                20L,
                List.of("u1", "u2", "u3", "u4", "u1"),
                "sandbox-upload-1",
                Map.of("source", "manual")), "operator-1");

        assertThat(view.status()).isEqualTo("SUCCESS");
        assertThat(view.requestedCount()).isEqualTo(4);
        assertThat(view.eligibleCount()).isEqualTo(1);
        assertThat(view.skippedCount()).isEqualTo(3);
        ArgumentCaptor<PaidMediaAudienceMemberDO> memberCaptor =
                ArgumentCaptor.forClass(PaidMediaAudienceMemberDO.class);
        verify(memberMapper, org.mockito.Mockito.times(5)).insert(memberCaptor.capture());
        List<PaidMediaAudienceMemberDO> members = memberCaptor.getAllValues();
        assertThat(members).filteredOn(member -> "ELIGIBLE".equals(member.getEligibilityStatus()))
                .extracting(PaidMediaAudienceMemberDO::getIdentifierHash)
                .containsExactly(sha256("alice@example.com"), sha256("15551234567"));
        assertThat(members).filteredOn(member -> "SKIPPED".equals(member.getEligibilityStatus()))
                .extracting(PaidMediaAudienceMemberDO::getReason)
                .contains("MISSING_IDENTIFIER", "CONSENT_DENIED", "PROFILE_NOT_FOUND");
        assertThat(members.toString()).doesNotContain("Alice@Example.COM", "555");
        verify(runMapper).updateById(org.mockito.ArgumentMatchers.<PaidMediaAudienceSyncRunDO>argThat(run ->
                "SUCCESS".equals(run.getStatus())
                        && run.getRequestedCount() == 4
                        && run.getEligibleCount() == 1
                        && run.getSkippedCount() == 3
                        && "sandbox-upload-1".equals(run.getExternalOperationId())));
    }

    @Test
    void disabledDestinationRecordsFailedRun() {
        PaidMediaAudienceDestinationMapper destinationMapper = mock(PaidMediaAudienceDestinationMapper.class);
        PaidMediaAudienceSyncRunMapper runMapper = mock(PaidMediaAudienceSyncRunMapper.class);
        PaidMediaAudienceDestinationDO destination = destination();
        destination.setEnabled(0);
        when(destinationMapper.selectById(10L)).thenReturn(destination);
        doAnswer(invocation -> {
            invocation.<PaidMediaAudienceSyncRunDO>getArgument(0).setId(901L);
            return 1;
        }).when(runMapper).insert(any(PaidMediaAudienceSyncRunDO.class));
        PaidMediaAudienceSyncService service = service(destinationMapper, mock(PaidMediaAudienceMemberMapper.class),
                runMapper, mock(AudienceDefinitionMapper.class), mock(CdpUserProfileMapper.class),
                mock(MarketingConsentMapper.class));

        assertThatThrownBy(() -> service.syncAudience(7L, new PaidMediaAudienceSyncCommand(
                10L,
                20L,
                List.of("u1"),
                null,
                Map.of()), "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");

        verify(runMapper).updateById(org.mockito.ArgumentMatchers.<PaidMediaAudienceSyncRunDO>argThat(run ->
                "FAILED".equals(run.getStatus())
                        && run.getFailedCount() == 1
                        && run.getErrorMessage().contains("disabled")));
    }

    @Test
    void queriesRunsAndMembersWithTenantScopeAndBoundedLimits() {
        PaidMediaAudienceSyncRunMapper runMapper = mock(PaidMediaAudienceSyncRunMapper.class);
        PaidMediaAudienceMemberMapper memberMapper = mock(PaidMediaAudienceMemberMapper.class);
        PaidMediaAudienceSyncRunDO ownRun = run();
        PaidMediaAudienceSyncRunDO otherRun = run();
        otherRun.setTenantId(99L);
        when(runMapper.selectList(any())).thenReturn(List.of(ownRun, otherRun));
        PaidMediaAudienceMemberDO ownMember = member("u1", "EMAIL", "ELIGIBLE", null);
        PaidMediaAudienceMemberDO otherMember = member("u2", "EMAIL", "ELIGIBLE", null);
        otherMember.setTenantId(99L);
        when(memberMapper.selectList(any())).thenReturn(List.of(ownMember, otherMember));
        PaidMediaAudienceSyncService service = service(mock(PaidMediaAudienceDestinationMapper.class),
                memberMapper, runMapper, mock(AudienceDefinitionMapper.class), mock(CdpUserProfileMapper.class),
                mock(MarketingConsentMapper.class));

        List<PaidMediaAudienceSyncRunView> runs = service.runs(7L, new PaidMediaAudienceRunQuery(
                10L, 20L, "SUCCESS", 500));
        List<PaidMediaAudienceMemberView> members = service.members(7L, new PaidMediaAudienceMemberQuery(
                900L, "ELIGIBLE", 500));

        assertThat(runs).singleElement().satisfies(view -> assertThat(view.id()).isEqualTo(900L));
        assertThat(members).singleElement().satisfies(view -> assertThat(view.userId()).isEqualTo("u1"));
    }

    private PaidMediaAudienceSyncService service(PaidMediaAudienceDestinationMapper destinationMapper) {
        return service(
                destinationMapper,
                mock(PaidMediaAudienceMemberMapper.class),
                mock(PaidMediaAudienceSyncRunMapper.class),
                mock(AudienceDefinitionMapper.class),
                mock(CdpUserProfileMapper.class),
                mock(MarketingConsentMapper.class));
    }

    private PaidMediaAudienceSyncService service(PaidMediaAudienceDestinationMapper destinationMapper,
                                                 PaidMediaAudienceMemberMapper memberMapper,
                                                 PaidMediaAudienceSyncRunMapper runMapper,
                                                 AudienceDefinitionMapper audienceMapper,
                                                 CdpUserProfileMapper profileMapper,
                                                 MarketingConsentMapper consentMapper) {
        return new PaidMediaAudienceSyncService(
                destinationMapper,
                memberMapper,
                runMapper,
                audienceMapper,
                profileMapper,
                consentMapper,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneId.of("UTC")));
    }

    private PaidMediaDestinationCommand destinationCommand() {
        return new PaidMediaDestinationCommand(
                "meta",
                "vip-meta",
                "VIP Meta Audience",
                "act_123",
                "aud_456",
                List.of("email", "phone"),
                "PAID_MEDIA",
                true,
                true,
                Map.of("policy", "customer-match"));
    }

    private PaidMediaAudienceDestinationDO destination() {
        PaidMediaAudienceDestinationDO row = new PaidMediaAudienceDestinationDO();
        row.setId(10L);
        row.setTenantId(7L);
        row.setProvider("META");
        row.setDestinationKey("vip-meta");
        row.setDisplayName("VIP Meta Audience");
        row.setAccountId("act_123");
        row.setExternalAudienceId("aud_456");
        row.setIdentifierTypesJson("[\"EMAIL\",\"PHONE\"]");
        row.setConsentChannel("PAID_MEDIA");
        row.setEnforceConsent(1);
        row.setEnabled(1);
        row.setCreatedBy("alice");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private AudienceDefinitionDO audience(Integer enabled) {
        AudienceDefinitionDO row = new AudienceDefinitionDO();
        row.setId(20L);
        row.setTenantId(7L);
        row.setName("VIP");
        row.setEnabled(enabled);
        return row;
    }

    private CdpUserProfileDO profile(String userId, String email, String phone) {
        CdpUserProfileDO row = new CdpUserProfileDO();
        row.setTenantId(7L);
        row.setUserId(userId);
        row.setEmail(email);
        row.setPhone(phone);
        row.setStatus("ACTIVE");
        return row;
    }

    private MarketingConsentDO consent() {
        MarketingConsentDO row = new MarketingConsentDO();
        row.setTenantId(7L);
        row.setUserId("u1");
        row.setChannel("PAID_MEDIA");
        row.setConsentStatus(MarketingConsentDO.OPT_IN);
        return row;
    }

    private PaidMediaAudienceSyncRunDO run() {
        PaidMediaAudienceSyncRunDO row = new PaidMediaAudienceSyncRunDO();
        row.setId(900L);
        row.setTenantId(7L);
        row.setDestinationId(10L);
        row.setAudienceId(20L);
        row.setProvider("META");
        row.setStatus("SUCCESS");
        row.setRequestedCount(4);
        row.setEligibleCount(1);
        row.setSkippedCount(3);
        row.setFailedCount(0);
        row.setCreatedBy("operator-1");
        row.setStartedAt(now());
        row.setFinishedAt(now());
        row.setCreatedAt(now());
        return row;
    }

    private PaidMediaAudienceMemberDO member(String userId, String identifierType, String status, String reason) {
        PaidMediaAudienceMemberDO row = new PaidMediaAudienceMemberDO();
        row.setId(100L);
        row.setTenantId(7L);
        row.setRunId(900L);
        row.setDestinationId(10L);
        row.setAudienceId(20L);
        row.setProvider("META");
        row.setUserId(userId);
        row.setIdentifierType(identifierType);
        row.setIdentifierHash("hash-" + userId);
        row.setEligibilityStatus(status);
        row.setReason(reason);
        row.setSyncedAt(now());
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 0, 0);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
