package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 画布执行记录 Mapper（表：canvas_execution）。
 *
 * <p>记录一次 execution 的主状态、触发类型与耗时信息。
 */
@Mapper
public interface CanvasExecutionMapper extends BaseMapper<CanvasExecutionDO> {
    // 节点级轨迹不在该表，详见 CanvasExecutionTraceMapper。
    // 该表更偏“一次执行汇总”，用于列表和概览指标。
    // 超时/失败后的补偿状态更新也会落在该表。

    int updateContextSnapshot(@Param("executionId") String executionId,
                              @Param("contextSnapshotJson") String contextSnapshotJson);

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    CanvasExecutionDO selectLatestPausedContextSnapshot(@Param("canvasId") Long canvasId,
                                                        @Param("userId") String userId);
}
