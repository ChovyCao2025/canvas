package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.dto.datasource.DataSourceConfigReq;
import org.chovy.canvas.security.SecretCipher;
import org.chovy.canvas.web.DataSourceConfigController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSourceConfigControllerTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DataSourceConfigDO.class);
    }

    @Test
    void createEncryptsPasswordBeforePersistence() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        SecretCipher cipher = SecretCipher.fromBase64Key("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        DataSourceConfigController controller = new DataSourceConfigController(mapper, cipher);
        DataSourceConfigReq body = newDataSource();

        controller.create(body).block();

        ArgumentCaptor<DataSourceConfigDO> captor = ArgumentCaptor.forClass(DataSourceConfigDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getPassword()).startsWith("v1:");
        assertThat(captor.getValue().getPassword()).doesNotContain("db-password");
        assertThat(cipher.decrypt(captor.getValue().getPassword())).isEqualTo("db-password");
    }

    @Test
    void createForTenantAdminUsesCurrentTenant() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        TenantContextResolver tenantResolver = mockTenant(7L, RoleNames.TENANT_ADMIN);
        SecretCipher cipher = SecretCipher.fromBase64Key("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        DataSourceConfigController controller = new DataSourceConfigController(mapper, cipher, tenantResolver);
        DataSourceConfigReq body = newDataSource();

        controller.create(body).block();

        ArgumentCaptor<DataSourceConfigDO> captor = ArgumentCaptor.forClass(DataSourceConfigDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
    }

    @Test
    void listForTenantAdminAddsTenantPredicate() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        when(mapper.selectPage(any(), any())).thenReturn(new Page<>());
        TenantContextResolver tenantResolver = mockTenant(7L, RoleNames.TENANT_ADMIN);
        SecretCipher cipher = SecretCipher.fromBase64Key("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        DataSourceConfigController controller = new DataSourceConfigController(mapper, cipher, tenantResolver);

        controller.list(1, 20, null, null, null).block();

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<LambdaQueryWrapper<DataSourceConfigDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("tenant_id");
    }

    @Test
    void deleteRejectsCrossTenantDatasource() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        DataSourceConfigDO existing = existingDataSource();
        existing.setId(11L);
        existing.setTenantId(8L);
        when(mapper.selectById(11L)).thenReturn(existing);
        TenantContextResolver tenantResolver = mockTenant(7L, RoleNames.TENANT_ADMIN);
        SecretCipher cipher = SecretCipher.fromBase64Key("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        DataSourceConfigController controller = new DataSourceConfigController(mapper, cipher, tenantResolver);

        assertThatThrownBy(() -> controller.delete(11L).block())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("跨租户");
    }

    private TenantContextResolver mockTenant(Long tenantId, String role) {
        TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, role, "alice")));
        return tenantResolver;
    }

    private DataSourceConfigReq newDataSource() {
        return new DataSourceConfigReq(
                null,
                "warehouse",
                null,
                "jdbc:mysql://localhost:3306/cdp",
                "cdp_app",
                "db-password",
                null,
                null,
                null,
                null);
    }

    private DataSourceConfigDO existingDataSource() {
        DataSourceConfigDO body = new DataSourceConfigDO();
        body.setName("warehouse");
        body.setUrl("jdbc:mysql://localhost:3306/cdp");
        body.setUsername("cdp_app");
        body.setPassword("db-password");
        return body;
    }
}
