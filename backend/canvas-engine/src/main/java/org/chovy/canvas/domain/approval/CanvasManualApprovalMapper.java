package org.chovy.canvas.domain.approval;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人工审批记录 Mapper（表：canvas_manual_approval）。
 *
 * <p>主要由 `ManualApprovalHandler` 与审批超时补偿流程使用。
 */
@Mapper
public interface CanvasManualApprovalMapper extends BaseMapper<CanvasManualApproval> {
    // BaseMapper 提供审批记录的增删改查，业务筛选逻辑在 Service 层完成。
    // 审批超时扫描、状态流转由 watchdog/handler 协同完成。
    // Mapper 不感知审批人权限校验，权限在接口层统一处理。
}
