package org.chovy.canvas.domain.execution;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 画布执行请求 Status Count 后端组件。
 *
 * <p>承载所属包内的核心职责，并与相邻的控制器、服务、Mapper 或执行引擎组件协作。
 * <p>该类应保持单一职责，公共行为变化时需要同步检查调用方和测试覆盖。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanvasExecutionRequestStatusCount {

    /** 执行请求状态编码。 */
    private String status;
    /** 该状态下的执行请求数量。 */
    private Long count;
}
