package org.chovy.canvas.execution.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 定义 PersistenceMappingAssertions 的执行上下文数据结构或业务契约。
 */
final class PersistenceMappingAssertions {

    /**
     * 执行 PersistenceMappingAssertions 对应的业务处理。
     */
    private PersistenceMappingAssertions() {
    }

    /**
     * 执行 assertTable 对应的业务处理。
     * @param type type 参数
     * @param tableName tableName 参数
     */
    static void assertTable(Class<?> type, String tableName) {
        assertThat(type.getAnnotation(TableName.class))
                .extracting(TableName::value)
                .isEqualTo(tableName);
    }

    /**
     * 执行 assertTableId 对应的业务处理。
     * @param type type 参数
     * @param fieldName fieldName 参数
     * @param idType idType 参数
     */
    static void assertTableId(Class<?> type, String fieldName, IdType idType) {
        TableId tableId = field(type, fieldName).getAnnotation(TableId.class);
        assertThat(tableId).isNotNull();
        assertThat(tableId.type()).isEqualTo(idType);
    }

    /**
     * 执行 assertTableId 对应的业务处理。
     * @param type type 参数
     * @param fieldName fieldName 参数
     */
    static void assertTableId(Class<?> type, String fieldName) {
        assertThat(field(type, fieldName).getAnnotation(TableId.class)).isNotNull();
    }

    /**
     * 执行 assertTableField 对应的业务处理。
     * @param type type 参数
     * @param fieldName fieldName 参数
     * @param columnName columnName 参数
     */
    static void assertTableField(Class<?> type, String fieldName, String columnName) {
        TableField tableField = field(type, fieldName).getAnnotation(TableField.class);
        assertThat(tableField).isNotNull();
        assertThat(tableField.value()).isEqualTo(columnName);
    }

    /**
     * 执行 field 对应的业务处理。
     * @param type type 参数
     * @param fieldName fieldName 参数
     * @return 处理后的结果
     */
    private static Field field(Class<?> type, String fieldName) {
        try {
            return type.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("missing field " + fieldName + " on " + type.getName(), e);
        }
    }
}
