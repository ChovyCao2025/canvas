package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;

class CdpUserServiceTest {

    private CdpUserProfileMapper profileMapper;
    private CdpUserIdentityMapper identityMapper;
    private CdpUserService service;

    @BeforeEach
    void setUp() {
        profileMapper = Mockito.mock(CdpUserProfileMapper.class);
        identityMapper = Mockito.mock(CdpUserIdentityMapper.class);
        service = new CdpUserService(profileMapper, identityMapper);
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
}
