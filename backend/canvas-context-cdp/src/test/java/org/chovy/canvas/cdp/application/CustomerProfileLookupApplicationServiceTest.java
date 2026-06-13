package org.chovy.canvas.cdp.application;

import org.chovy.canvas.cdp.api.CdpCustomerProfileView;
import org.chovy.canvas.cdp.domain.CustomerProfile;
import org.chovy.canvas.cdp.domain.CustomerProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerProfileLookupApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void ensureUserCreatesActiveProfileAndUserIdIdentityWhenMissing() {
        FakeProfileRepository repository = new FakeProfileRepository();
        CustomerProfileLookupApplicationService service = new CustomerProfileLookupApplicationService(repository, CLOCK);

        CdpCustomerProfileView view = service.ensureUser(7L, " u1 ", "CDP_EVENT", "OrderComplete");

        assertThat(view.userId()).isEqualTo("u1");
        assertThat(view.displayName()).isEqualTo("u1");
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.firstSeenAt()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
        assertThat(repository.identities).containsEntry("7:USER_ID:u1", "u1");
    }

    @Test
    void ensureUserByIdentityReusesExistingIdentityOrCreatesStableGeneratedUser() {
        FakeProfileRepository repository = new FakeProfileRepository();
        repository.saveProfile(profile(7L, "u-existing"));
        repository.identities.put("7:EMAIL:alice@example.com", "u-existing");
        CustomerProfileLookupApplicationService service = new CustomerProfileLookupApplicationService(repository, CLOCK);

        CdpCustomerProfileView existing = service.ensureUserByIdentity(
                7L,
                " email ",
                " alice@example.com ",
                "IMPORT",
                "job-1");
        CdpCustomerProfileView created = service.ensureUserByIdentity(
                7L,
                " phone ",
                " 13812345678 ",
                "IMPORT",
                "job-1");

        assertThat(existing.userId()).isEqualTo("u-existing");
        assertThat(created.userId()).isEqualTo("phone:13812345678");
        assertThat(repository.identities).containsEntry("7:PHONE:13812345678", "phone:13812345678");
        assertThat(repository.verifiedFlags).containsEntry("7:PHONE:13812345678", false);
    }

    @Test
    void getRequiredProfileEnforcesTenantOwnership() {
        FakeProfileRepository repository = new FakeProfileRepository();
        repository.saveProfile(profile(7L, "u1"));
        CustomerProfileLookupApplicationService service = new CustomerProfileLookupApplicationService(repository, CLOCK);

        assertThat(service.getRequiredProfile(7L, "u1").userId()).isEqualTo("u1");

        assertThatThrownBy(() -> service.getRequiredProfile(8L, "u1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CDP user does not exist");
    }

    private static CustomerProfile profile(Long tenantId, String userId) {
        return new CustomerProfile(
                10L,
                tenantId,
                userId,
                userId,
                null,
                null,
                "ACTIVE",
                Map.of(),
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00"),
                "seed",
                null,
                null);
    }

    private static final class FakeProfileRepository implements CustomerProfileRepository {
        private final Map<String, CustomerProfile> profiles = new LinkedHashMap<>();
        private final Map<String, String> identities = new LinkedHashMap<>();
        private final Map<String, Boolean> verifiedFlags = new LinkedHashMap<>();

        @Override
        public CustomerProfile findProfile(Long tenantId, String userId) {
            return profiles.get(tenantId + ":" + userId);
        }

        @Override
        public CustomerProfile saveProfile(CustomerProfile profile) {
            CustomerProfile saved = profile.id() == null ? profile.withId(100L) : profile;
            profiles.put(saved.tenantId() + ":" + saved.userId(), saved);
            return saved;
        }

        @Override
        public String findUserIdByIdentity(Long tenantId, String identityType, String identityValue) {
            return identities.get(key(tenantId, identityType, identityValue));
        }

        @Override
        public void saveIdentity(Long tenantId, String userId, String identityType, String identityValue,
                                 String sourceType, String sourceRefId, boolean verified) {
            String key = key(tenantId, identityType, identityValue);
            identities.put(key, userId);
            verifiedFlags.put(key, verified);
        }

        private static String key(Long tenantId, String identityType, String identityValue) {
            return tenantId + ":" + identityType + ":" + identityValue;
        }
    }
}
