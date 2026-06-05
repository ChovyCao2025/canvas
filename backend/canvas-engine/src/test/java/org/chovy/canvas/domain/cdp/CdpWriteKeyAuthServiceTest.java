package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.dal.dataobject.CdpWriteKeyDO;
import org.chovy.canvas.dal.mapper.CdpWriteKeyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdpWriteKeyAuthServiceTest {
    private CdpWriteKeyMapper mapper;
    private BCryptPasswordEncoder encoder;
    private CdpWriteKeyAuthService service;

    @BeforeEach
    void setUp() {
        mapper = mock(CdpWriteKeyMapper.class);
        encoder = new BCryptPasswordEncoder();
        service = new CdpWriteKeyAuthService(mapper, encoder);
    }

    @Test
    void authenticateResolvesActiveWriteKeyFromBasicAuth() {
        String raw = "ck_test_0123456789abcdef";
        when(mapper.selectOne(any())).thenReturn(row(raw, CdpWriteKeyDO.ACTIVE));

        var result = service.authenticate(headers(raw));

        assertThat(result.tenantId()).isEqualTo(42L);
        assertThat(result.writeKeyId()).isEqualTo(7L);
        assertThat(result.platform()).isEqualTo("WEB");
    }

    @Test
    void authenticateRejectsMalformedBasicAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer token");

        assertThatThrownBy(() -> service.authenticate(headers))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CDP write key is required");
    }

    @Test
    void authenticateRejectsDisabledWriteKey() {
        String raw = "ck_test_disabled";
        when(mapper.selectOne(any())).thenReturn(row(raw, CdpWriteKeyDO.DISABLED));

        assertThatThrownBy(() -> service.authenticate(headers(raw)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CDP write key is disabled");
    }

    @Test
    void authenticateRejectsHashMismatch() {
        when(mapper.selectOne(any())).thenReturn(row("ck_test_real", CdpWriteKeyDO.ACTIVE));

        assertThatThrownBy(() -> service.authenticate(headers("ck_test_wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CDP write key is invalid");
    }

    private CdpWriteKeyDO row(String raw, String status) {
        CdpWriteKeyDO row = new CdpWriteKeyDO();
        row.setId(7L);
        row.setTenantId(42L);
        row.setKeyPrefix(raw.substring(0, Math.min(raw.length(), 12)));
        row.setKeyHash(encoder.encode(raw));
        row.setPlatform("WEB");
        row.setStatus(status);
        row.setRateLimitQps(100);
        return row;
    }

    private HttpHeaders headers(String raw) {
        String token = Base64.getEncoder()
                .encodeToString((raw + ":").getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + token);
        return headers;
    }
}
