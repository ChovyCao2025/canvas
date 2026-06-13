package org.chovy.canvas.canvas.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

class CanvasSecondaryPersistenceMappingTest {

    @Test
    void ownsTemplateProjectFolderAndApprovalRows() {
        assertThat(CanvasTemplateDO.class.getAnnotation(TableName.class).value()).isEqualTo("canvas_template");
        assertThat(CanvasProjectDO.class.getAnnotation(TableName.class).value()).isEqualTo("canvas_project");
        assertThat(CanvasProjectFolderDO.class.getAnnotation(TableName.class).value()).isEqualTo("canvas_project_folder");
        assertThat(CanvasManualApprovalDO.class.getAnnotation(TableName.class).value()).isEqualTo("canvas_manual_approval");
    }
}
