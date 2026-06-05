package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.domain.compliance.PiiMaskingService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyTombstoneService;

/**
 * CDP 用户 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CdpUserServiceTest {

    private CdpUserProfileMapper profileMapper;
    private CdpUserIdentityMapper identityMapper;
    private CdpUserService service;

    @BeforeEach
    void setUp() {
        profileMapper = Mockito.mock(CdpUserProfileMapper.class);
        identityMapper = Mockito.mock(CdpUserIdentityMapper.class);
        service = new CdpUserService(profileMapper, identityMapper, new PiiMaskingService());
    }

    @Test
    void ensureUserCreatesProfileAndUserIdIdentityWhenMissing() {
        when(profileMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        service.ensureUser("u1", "CANVAS_EXECUTION", "exec-1");

        ArgumentCaptor<CdpUserProfileDO> profileCaptor = ArgumentCaptor.forClass(CdpUserProfileDO.class);
        ArgumentCaptor<CdpUserIdentityDO> identityCaptor = ArgumentCaptor.forClass(CdpUserIdentityDO.class);
        verify(profileMapper).insert(profileCaptor.capture());
        verify(identityMapper).insert(identityCaptor.capture());

        assertThat(profileCaptor.getValue().getUserId()).isEqualTo("u1");
        assertThat(profileCaptor.getValue().getDisplayName()).isEqualTo("u1");
        assertThat(profileCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(identityCaptor.getValue().getIdentityType()).isEqualTo("USER_ID");
        assertThat(identityCaptor.getValue().getIdentityValue()).isEqualTo("u1");
        assertThat(identityCaptor.getValue().getSourceType()).isEqualTo("CANVAS_EXECUTION");
        assertThat(identityCaptor.getValue().getSourceRefId()).isEqualTo("exec-1");
    }

    @Test
    void ensureUserUpdatesLastSeenWhenProfileExists() {
        CdpUserProfileDO existing = new CdpUserProfileDO();
        existing.setId(9L);
        existing.setUserId("u1");
        when(profileMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        service.ensureUser("u1", "MANUAL", "req-1");

        verify(profileMapper).updateById(existing);
        verify(identityMapper, never()).insert(any(CdpUserIdentityDO.class));
    }

    @Test
    void ensureUserBlocksTombstonedUserBeforeProfileMutation() {
        CdpWarehousePrivacyTombstoneService tombstoneService =
                Mockito.mock(CdpWarehousePrivacyTombstoneService.class);
        doThrow(new CdpWarehousePrivacyTombstoneService.PrivacyTombstoneViolationException("blocked user"))
                .when(tombstoneService)
                .enforceNotBlocked(9L, "USER_ID", "u1", "CDP_USER_ENSURE");
        CdpUserService guardedService = new CdpUserService(
                profileMapper, identityMapper, new PiiMaskingService(), provider(tombstoneService));

        assertThatThrownBy(() -> guardedService.ensureUser(9L, " u1 ", "CDP_EVENT", "OrderPaid"))
                .isInstanceOf(CdpWarehousePrivacyTombstoneService.PrivacyTombstoneViolationException.class)
                .hasMessageContaining("blocked user");

        verify(tombstoneService).enforceNotBlocked(9L, "USER_ID", "u1", "CDP_USER_ENSURE");
        verifyNoInteractions(profileMapper, identityMapper);
    }

    @Test
    void ensureUserByIdentityBlocksTombstonedExternalIdentityBeforeProfileMutation() {
        CdpWarehousePrivacyTombstoneService tombstoneService =
                Mockito.mock(CdpWarehousePrivacyTombstoneService.class);
        doThrow(new CdpWarehousePrivacyTombstoneService.PrivacyTombstoneViolationException("blocked email"))
                .when(tombstoneService)
                .enforceNotBlocked(9L, "EMAIL", "alice@example.com", "CDP_USER_ENSURE_IDENTITY");
        CdpUserService guardedService = new CdpUserService(
                profileMapper, identityMapper, new PiiMaskingService(), provider(tombstoneService));

        assertThatThrownBy(() -> guardedService.ensureUserByIdentity(
                9L, " email ", " alice@example.com ", "IMPORT", "job-1"))
                .isInstanceOf(CdpWarehousePrivacyTombstoneService.PrivacyTombstoneViolationException.class)
                .hasMessageContaining("blocked email");

        verify(tombstoneService)
                .enforceNotBlocked(9L, "EMAIL", "alice@example.com", "CDP_USER_ENSURE_IDENTITY");
        verifyNoInteractions(profileMapper, identityMapper);
    }

    @Test
    void toDetailMasksPhoneAndEmail() {
        CdpUserProfileDO profile = new CdpUserProfileDO();
        profile.setUserId("u1");
        profile.setDisplayName("Alice");
        profile.setPhone("13812345678");
        profile.setEmail("alice@example.com");
        profile.setStatus("ACTIVE");

        var detail = service.toDetail(profile);

        assertThat(detail.phone()).isEqualTo("138****5678");
        assertThat(detail.email()).isEqualTo("a***e@example.com");
    }

    @Test
    void toDetailMasksOpenIdAndSecretsInsidePropertiesJson() {
        CdpUserProfileDO profile = new CdpUserProfileDO();
        profile.setUserId("u1");
        profile.setDisplayName("Alice");
        profile.setStatus("ACTIVE");
        profile.setPropertiesJson("{\"open_id\":\"wx_open_id_abcdef\",\"apiKey\":\"secret-token-123456\",\"safe\":\"visible\"}");

        var detail = service.toDetail(profile);

        assertThat(detail.propertiesJson()).contains("wx_o**********cdef");
        assertThat(detail.propertiesJson()).contains("****3456");
        assertThat(detail.propertiesJson()).contains("visible");
        assertThat(detail.propertiesJson()).doesNotContain("wx_open_id_abcdef");
        assertThat(detail.propertiesJson()).doesNotContain("secret-token-123456");
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<CdpWarehousePrivacyTombstoneService> provider(
            CdpWarehousePrivacyTombstoneService value) {
        ObjectProvider<CdpWarehousePrivacyTombstoneService> provider = Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
