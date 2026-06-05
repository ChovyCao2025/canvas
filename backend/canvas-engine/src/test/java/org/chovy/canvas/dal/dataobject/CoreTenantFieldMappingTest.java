package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreTenantFieldMappingTest {

    @Test
    void coreCanvasRuntimeEntitiesExposeTenantId() throws Exception {
        assertTenantField(CanvasDO.class);
        assertTenantField(CanvasVersionDO.class);
        assertTenantField(CanvasExecutionDO.class);
        assertTenantField(CanvasExecutionTraceDO.class);
        assertTenantField(CanvasExecutionRequestDO.class);
        assertTenantField(DataSourceConfigDO.class);
    }

    private void assertTenantField(Class<?> type) throws Exception {
        var field = type.getDeclaredField("tenantId");
        TableField tableField = field.getAnnotation(TableField.class);

        assertThat(field.getType()).isEqualTo(Long.class);
        assertThat(tableField).isNotNull();
        assertThat(tableField.value()).isEqualTo("tenant_id");
    }
}
