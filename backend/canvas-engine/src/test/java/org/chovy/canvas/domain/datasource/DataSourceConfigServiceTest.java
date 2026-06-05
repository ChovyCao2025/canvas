package org.chovy.canvas.domain.datasource;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSourceConfigServiceTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DataSourceConfigDO.class);
    }

    @Test
    void createEncryptsPasswordAndAssignsTenant() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        DataSourceCredentialCipher cipher =
                new DataSourceCredentialCipher("test-datasource-credential-secret-32b");
        DataSourceConfigService service = new DataSourceConfigService(mapper, cipher);
        DataSourceConfigDO body = jdbcConfig("secret-password");

        service.create(body, tenant(42L));

        ArgumentCaptor<DataSourceConfigDO> captor = ArgumentCaptor.forClass(DataSourceConfigDO.class);
        verify(mapper).insert(captor.capture());
        DataSourceConfigDO inserted = captor.getValue();
        assertThat(inserted.getTenantId()).isEqualTo(42L);
        assertThat(inserted.getPassword()).startsWith("enc:v1:");
        assertThat(inserted.getPassword()).doesNotContain("secret-password");
        assertThat(cipher.decrypt(inserted.getPassword())).isEqualTo("secret-password");
    }

    @Test
    void listScopesRowsToOperatorTenant() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        DataSourceConfigService service = new DataSourceConfigService(
                mapper, new DataSourceCredentialCipher("test-datasource-credential-secret-32b"));
        when(mapper.selectPage(any(), any())).thenReturn(new Page<>());

        service.list(1, 20, "JDBC", 1, tenant(42L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<DataSourceConfigDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectPage(any(), captor.capture());
        AbstractWrapper<DataSourceConfigDO, ?, ?> wrapper =
                (AbstractWrapper<DataSourceConfigDO, ?, ?>) captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("tenant_id");
        assertThat(wrapper.getParamNameValuePairs()).containsValue(42L);
    }

    @Test
    void updatePreservesExistingEncryptedPasswordWhenPatchOmitsPassword() {
        DataSourceConfigMapper mapper = mock(DataSourceConfigMapper.class);
        DataSourceCredentialCipher cipher =
                new DataSourceCredentialCipher("test-datasource-credential-secret-32b");
        String encrypted = cipher.encrypt("existing-password");
        DataSourceConfigDO existing = jdbcConfig(encrypted);
        existing.setId(7L);
        existing.setTenantId(42L);
        when(mapper.selectOne(any())).thenReturn(existing);
        DataSourceConfigService service = new DataSourceConfigService(mapper, cipher);

        DataSourceConfigDO patch = jdbcConfig(null);
        patch.setName("new name");
        service.update(7L, patch, tenant(42L));

        ArgumentCaptor<DataSourceConfigDO> captor = ArgumentCaptor.forClass(DataSourceConfigDO.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(42L);
        assertThat(captor.getValue().getPassword()).isEqualTo(encrypted);
    }

    private static DataSourceConfigDO jdbcConfig(String password) {
        DataSourceConfigDO config = new DataSourceConfigDO();
        config.setName("warehouse");
        config.setType("JDBC");
        config.setUrl("jdbc:mysql://127.0.0.1:3306/canvas");
        config.setUsername("canvas_app");
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setEnabled(1);
        return config;
    }

    private static TenantContext tenant(Long tenantId) {
        return new TenantContext(tenantId, RoleNames.TENANT_ADMIN, "operator");
    }
}
