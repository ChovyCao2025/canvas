package org.chovy.canvas.execution.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定义 ExecutionRerunAuditMapper 的执行上下文数据结构或业务契约。
 */
@Mapper
public interface ExecutionRerunAuditMapper extends BaseMapper<ExecutionRerunAuditDO> {
}
