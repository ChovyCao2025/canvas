package org.chovy.canvas.engine.audience;

import org.chovy.canvas.dal.dataobject.CdpUserIndexDO;
import org.chovy.canvas.dal.mapper.CdpUserIndexMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StableUserIndexServiceTest {

    @Test
    void reusesExistingIndexForTenantUser() {
        CdpUserIndexMapper mapper = mock(CdpUserIndexMapper.class);
        CdpUserIndexDO existing = row(7L, "u1", 42L);
        when(mapper.selectByTenantAndUser(7L, "u1")).thenReturn(existing);
        StableUserIndexService service = new StableUserIndexService(mapper);

        long index = service.getOrCreateIndex(7L, " u1 ");

        assertThat(index).isEqualTo(42L);
    }

    @Test
    void allocatesNextIndexForNewUser() {
        CdpUserIndexMapper mapper = mock(CdpUserIndexMapper.class);
        when(mapper.selectByTenantAndUser(7L, "u2")).thenReturn(null);
        when(mapper.nextIndexForTenant(7L)).thenReturn(100L);
        StableUserIndexService service = new StableUserIndexService(mapper);

        long index = service.getOrCreateIndex(7L, "u2");

        assertThat(index).isEqualTo(100L);
        verify(mapper).insert(any(CdpUserIndexDO.class));
    }

    @Test
    void duplicateInsertRaceReloadsExistingIndex() {
        CdpUserIndexMapper mapper = mock(CdpUserIndexMapper.class);
        when(mapper.selectByTenantAndUser(7L, "u3"))
                .thenReturn(null)
                .thenReturn(row(7L, "u3", 101L));
        when(mapper.nextIndexForTenant(7L)).thenReturn(101L);
        when(mapper.insert(any(CdpUserIndexDO.class))).thenThrow(new DuplicateKeyException("race"));
        StableUserIndexService service = new StableUserIndexService(mapper);

        long index = service.getOrCreateIndex(7L, "u3");

        assertThat(index).isEqualTo(101L);
    }

    @Test
    void rejectsBlankUserId() {
        StableUserIndexService service = new StableUserIndexService(mock(CdpUserIndexMapper.class));

        assertThatThrownBy(() -> service.getOrCreateIndex(7L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    private CdpUserIndexDO row(Long tenantId, String userId, Long userIndex) {
        CdpUserIndexDO row = new CdpUserIndexDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setUserIndex(userIndex);
        return row;
    }
}
