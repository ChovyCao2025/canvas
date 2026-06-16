package org.chovy.canvas.execution.adapter.persistence;

import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTable;
import static org.chovy.canvas.execution.adapter.persistence.PersistenceMappingAssertions.assertTableId;
import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;

/**
 * 定义 UserInputResumeAuditPersistenceMappingTest 的执行上下文数据结构或业务契约。
 */
class UserInputResumeAuditPersistenceMappingTest {

    /**
     * 执行 mapsUserInputResumeAuditTableAndMapper 对应的业务处理。
     */
    @Test
    void mapsUserInputResumeAuditTableAndMapper() {
        assertTable(UserInputResumeAuditDO.class, "user_input_resume_audit");
        assertTableId(UserInputResumeAuditDO.class, "id", IdType.AUTO);
        assertThat(UserInputResumeAuditMapper.class.getAnnotation(Mapper.class)).isNotNull();
        assertThat(BaseMapper.class).isAssignableFrom(UserInputResumeAuditMapper.class);
    }
}
