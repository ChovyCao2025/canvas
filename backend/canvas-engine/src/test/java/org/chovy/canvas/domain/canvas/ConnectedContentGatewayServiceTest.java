package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.dal.dataobject.ConnectedContentCacheDO;
import org.chovy.canvas.dal.mapper.ConnectedContentCacheMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectedContentGatewayServiceTest {

    @Test
    void findFreshReturnsCachedBodyWhenRowExists() {
        ConnectedContentCacheMapper mapper = mock(ConnectedContentCacheMapper.class);
        ConnectedContentCacheDO row = new ConnectedContentCacheDO();
        row.setResponseJson("{\"title\":\"cached\"}");
        when(mapper.selectOne(any())).thenReturn(row);
        ConnectedContentGatewayService service = new ConnectedContentGatewayService(
                mapper, mock(WebClient.Builder.class));

        var result = service.findFresh(7L, "cache-key", LocalDateTime.of(2026, 6, 4, 9, 0));

        assertThat(result).isPresent();
        assertThat(result.get().body()).isEqualTo("{\"title\":\"cached\"}");
    }

    @Test
    void saveInsertsNewCacheRowWhenMissing() {
        ConnectedContentCacheMapper mapper = mock(ConnectedContentCacheMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        ConnectedContentGatewayService service = new ConnectedContentGatewayService(
                mapper, mock(WebClient.Builder.class));
        LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 4, 9, 5);

        service.save(7L, "cache-key", "url-hash", "request-hash", "{}", expiresAt);

        ArgumentCaptor<ConnectedContentCacheDO> captor = ArgumentCaptor.forClass(ConnectedContentCacheDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getCacheKey()).isEqualTo("cache-key");
        assertThat(captor.getValue().getUrlHash()).isEqualTo("url-hash");
        assertThat(captor.getValue().getRequestHash()).isEqualTo("request-hash");
        assertThat(captor.getValue().getResponseJson()).isEqualTo("{}");
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void saveUpdatesExistingCacheRow() {
        ConnectedContentCacheMapper mapper = mock(ConnectedContentCacheMapper.class);
        ConnectedContentCacheDO row = new ConnectedContentCacheDO();
        row.setId(1L);
        when(mapper.selectOne(any())).thenReturn(row);
        ConnectedContentGatewayService service = new ConnectedContentGatewayService(
                mapper, mock(WebClient.Builder.class));
        LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 4, 9, 5);

        service.save(7L, "cache-key", "url-hash", "request-hash", "{}", expiresAt);

        ArgumentCaptor<ConnectedContentCacheDO> captor = ArgumentCaptor.forClass(ConnectedContentCacheDO.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getUrlHash()).isEqualTo("url-hash");
        assertThat(captor.getValue().getRequestHash()).isEqualTo("request-hash");
        assertThat(captor.getValue().getResponseJson()).isEqualTo("{}");
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
    }
}
