package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.SystemOptionDO;
import org.chovy.canvas.dal.mapper.SystemOptionMapper;
import org.chovy.canvas.dto.StubOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemOptionTenantScopeTest {

    @Mock
    private SystemOptionMapper mapper;

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                SystemOptionDO.class);
    }

    @Test
    void activeOptionsPrefersTenantOverrideAndFallsBackToGlobalDefaults() {
        SystemOptionDO tenantOverride = option(9L, "execution_governance", "max_batch_replay", "50");
        tenantOverride.setSortOrder(10);
        SystemOptionDO globalDefault = option(null, "execution_governance", "max_batch_replay", "100");
        globalDefault.setSortOrder(20);
        SystemOptionDO globalFallback = option(null, "execution_governance", "replay_qps", "20");
        globalFallback.setSortOrder(30);
        when(mapper.selectList(any())).thenReturn(List.of(tenantOverride, globalDefault, globalFallback));

        SystemOptionService service = new SystemOptionService(mapper);

        List<SystemOptionDO> rows = service.activeSystemOptions("execution_governance", 9L);
        List<StubOption> options = service.activeOptions("execution_governance", 9L);

        assertThat(rows).containsExactly(tenantOverride, globalFallback);
        assertThat(options).extracting(StubOption::getKey)
                .containsExactly("max_batch_replay", "replay_qps");

        ArgumentCaptor<Wrapper<SystemOptionDO>> captor = wrapperCaptor();
        verify(mapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("tenant_id");
    }

    @Test
    void tenantAdminCannotEditGlobalSystemOptions() {
        SystemOptionDO existing = option(null, "execution_governance", "max_batch_replay", "100");
        when(mapper.selectById(1L)).thenReturn(existing);
        SystemOptionService service = new SystemOptionService(mapper);

        SystemOptionDO patch = new SystemOptionDO();
        patch.setLabel("租户改名");

        assertThatThrownBy(() -> service.updateEditable(1L, patch, tenantAdmin()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("当前租户");
    }

    @Test
    void superAdminCanEditTenantScopedOption() {
        SystemOptionDO existing = option(9L, "execution_governance", "max_batch_replay", "100");
        existing.setId(1L);
        when(mapper.selectById(1L)).thenReturn(existing);
        SystemOptionService service = new SystemOptionService(mapper);

        SystemOptionDO patch = new SystemOptionDO();
        patch.setLabel("单次批量重放上限");
        patch.setDescription("限制一次批量重放最多处理多少条执行请求");
        patch.setSortOrder(10);
        patch.setEnabled(1);

        service.updateEditable(1L, patch, superAdmin());

        assertThat(existing.getLabel()).isEqualTo("单次批量重放上限");
        verify(mapper).updateById(existing);
    }

    private SystemOptionDO option(Long tenantId, String category, String optionKey, String label) {
        SystemOptionDO option = new SystemOptionDO();
        option.setTenantId(tenantId);
        option.setCategory(category);
        option.setOptionKey(optionKey);
        option.setLabel(label);
        option.setEnabled(1);
        option.setSortOrder(10);
        return option;
    }

    private TenantContext tenantAdmin() {
        return new TenantContext(9L, RoleNames.TENANT_ADMIN, "tenant_admin");
    }

    private TenantContext superAdmin() {
        return new TenantContext(1L, RoleNames.SUPER_ADMIN, "root");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<Wrapper<T>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }
}
