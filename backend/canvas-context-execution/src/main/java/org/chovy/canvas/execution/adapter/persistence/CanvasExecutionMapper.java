package org.chovy.canvas.execution.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 定义 CanvasExecutionMapper 的执行上下文数据结构或业务契约。
 */
@Mapper
public interface CanvasExecutionMapper extends BaseMapper<CanvasExecutionDO> {

    int updateContextSnapshot(@Param("executionId") String executionId,
                              @Param("contextSnapshotJson") String contextSnapshotJson);

    CanvasExecutionDO selectLatestPausedContextSnapshot(@Param("canvasId") Long canvasId,
                                                        @Param("userId") String userId);
}
