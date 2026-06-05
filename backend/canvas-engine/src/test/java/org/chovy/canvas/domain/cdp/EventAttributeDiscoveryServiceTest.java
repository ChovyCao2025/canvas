package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.dal.dataobject.EventAttrDefinitionDO;
import org.chovy.canvas.dal.mapper.EventAttrDefinitionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventAttributeDiscoveryServiceTest {

    @Test
    void discoverCreatesPendingReviewRowsWithInferredType() {
        EventAttrDefinitionMapper attrMapper = mock(EventAttrDefinitionMapper.class);
        EventAttributeDiscoveryService service = new EventAttributeDiscoveryService(attrMapper);
        when(attrMapper.selectOne(any())).thenReturn(null);

        service.discover(42L, "OrderComplete", Map.of("amount", 99.9, "paid", true));

        ArgumentCaptor<EventAttrDefinitionDO> captor = ArgumentCaptor.forClass(EventAttrDefinitionDO.class);
        verify(attrMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(EventAttrDefinitionDO::getStatus)
                .containsOnly(EventAttrDefinitionDO.PENDING_REVIEW);
        assertThat(captor.getAllValues()).extracting(EventAttrDefinitionDO::getAttrType)
                .contains("NUMBER", "BOOLEAN");
    }

    @Test
    void discoverUpdatesLastSeenWithoutChangingStatus() {
        EventAttrDefinitionMapper attrMapper = mock(EventAttrDefinitionMapper.class);
        EventAttrDefinitionDO existing = new EventAttrDefinitionDO();
        existing.setId(9L);
        existing.setStatus(EventAttrDefinitionDO.APPROVED);
        when(attrMapper.selectOne(any())).thenReturn(existing);

        new EventAttributeDiscoveryService(attrMapper).discover(42L, "OrderComplete", Map.of("amount", 99.9));

        verify(attrMapper).updateById(argThat(row -> row.getId().equals(9L)
                && row.getStatus().equals(EventAttrDefinitionDO.APPROVED)
                && row.getLastSeenAt() != null));
    }

    @Test
    void inferTypeClassifiesJsonAndDateLikeStrings() {
        EventAttrDefinitionMapper attrMapper = mock(EventAttrDefinitionMapper.class);
        EventAttributeDiscoveryService service = new EventAttributeDiscoveryService(attrMapper);

        assertThat(service.inferType(Map.of("nested", true))).isEqualTo("JSON");
        assertThat(service.inferType("2026-05-30T10:00:00Z")).isEqualTo("DATE");
        assertThat(service.inferType("CNY")).isEqualTo("STRING");
    }
}
