package org.chovy.canvas.canvas.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * 封装CanvasProjectFolderPersistenceMappingTest相关的业务逻辑。
 */
class CanvasProjectFolderPersistenceMappingTest {

    /**
     * 处理projectIdCanBeClearedWhenProjectAssignmentIsRemoved。
     */
    @Test
    void projectIdCanBeClearedWhenProjectAssignmentIsRemoved() throws Exception {
        Field field = CanvasProjectFolderDO.class.getDeclaredField("projectId");
        TableField tableField = field.getAnnotation(TableField.class);

        assertThat(tableField).isNotNull();
        assertThat(tableField.updateStrategy()).isEqualTo(FieldStrategy.ALWAYS);
    }
}
