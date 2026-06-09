package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestStatusCount;

/**
 * 画布执行请求 MyBatis-Plus Mapper。
 *
 * <p>继承 BaseMapper 为 {@code CanvasExecutionRequestDO} 提供基础 CRUD 能力，复杂查询可在同名 XML 中扩展。
 * <p>该接口只定义数据访问边界，不承载业务编排或跨表事务逻辑。
 */
@Mapper
public interface CanvasExecutionRequestMapper extends BaseMapper<CanvasExecutionRequestDO> {

    /**
     * 创建或新增 insert Ignore 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param request 请求对象，承载调用方提交的业务参数
     * @return 计算得到的数值结果
     */
    @Insert("""
            INSERT IGNORE INTO canvas_execution_request
            (id, tenant_id, canvas_id, user_id, perf_run_id, trigger_type, trigger_node_type, match_key,
             payload_json, source_msg_id, status, attempt_count, next_retry_at,
             last_error, result_json, created_at, updated_at)
            VALUES
            (#{id}, #{tenantId}, #{canvasId}, #{userId}, #{perfRunId}, #{triggerType}, #{triggerNodeType}, #{matchKey},
             #{payloadJson}, #{sourceMsgId}, #{status}, #{attemptCount}, #{nextRetryAt},
             #{lastError}, #{resultJson}, NOW(), NOW())
            """)
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 insert ignore 计算得到的数量、金额或指标值。
     */
    int insertIgnore(CanvasExecutionRequestDO request);

    @Update("""
            UPDATE canvas_execution_request
            SET status = 'RUNNING',
                attempt_count = attempt_count + 1,
                run_token = #{runToken},
                last_error = NULL,
                updated_at = #{now}
            WHERE id = #{id}
              AND (
                    status IN ('PENDING', 'RETRY')
                    OR (status = 'RUNNING' AND updated_at < #{staleBefore})
                  )
            """)
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param staleBefore stale before 参数，用于 markRunning 流程中的校验、计算或对象转换。
     * @param runToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回 mark running 计算得到的数量、金额或指标值。
     */
    int markRunning(@Param("id") String id,
                    @Param("now") LocalDateTime now,
                    @Param("staleBefore") LocalDateTime staleBefore,
                    @Param("runToken") String runToken);

    @Update("""
            UPDATE canvas_execution_request
            SET updated_at = #{now}
            WHERE id = #{id}
              AND status = 'RUNNING'
              AND run_token = #{runToken}
            """)
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param runToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回组装或转换后的结果对象。
     */
    int touchRunning(@Param("id") String id,
                     @Param("now") LocalDateTime now,
                     @Param("runToken") String runToken);

    @Update("""
            UPDATE canvas_execution_request
            SET status = 'SUCCEEDED',
                result_json = #{resultJson},
                last_error = NULL,
                next_retry_at = NULL,
                run_token = NULL,
                updated_at = #{now}
            WHERE id = #{id}
              AND run_token = #{runToken}
            """)
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param resultJson JSON 字符串，承载结构化配置或明细。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param runToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回 mark succeeded 计算得到的数量、金额或指标值。
     */
    int markSucceeded(@Param("id") String id,
                      @Param("resultJson") String resultJson,
                      @Param("now") LocalDateTime now,
                      @Param("runToken") String runToken);

    @Update("""
            UPDATE canvas_execution_request
            SET status = 'RETRY',
                last_error = #{error},
                next_retry_at = #{nextRetryAt},
                run_token = NULL,
                updated_at = #{now}
            WHERE id = #{id}
              AND run_token = #{runToken}
            """)
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param error error 参数，用于 markRetry 流程中的校验、计算或对象转换。
     * @param nextRetryAt 时间参数，用于计算窗口、过期或审计时间。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param runToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回 mark retry 计算得到的数量、金额或指标值。
     */
    int markRetry(@Param("id") String id,
                  @Param("error") String error,
                  @Param("nextRetryAt") LocalDateTime nextRetryAt,
                  @Param("now") LocalDateTime now,
                  @Param("runToken") String runToken);

    @Update("""
            UPDATE canvas_execution_request
            SET status = 'FAILED',
                last_error = #{error},
                next_retry_at = NULL,
                run_token = NULL,
                updated_at = #{now}
            WHERE id = #{id}
              AND run_token = #{runToken}
            """)
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param error error 参数，用于 markFailed 流程中的校验、计算或对象转换。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param runToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回 mark failed 计算得到的数量、金额或指标值。
     */
    int markFailed(@Param("id") String id,
                   @Param("error") String error,
                   @Param("now") LocalDateTime now,
                   @Param("runToken") String runToken);

    @Update("""
            UPDATE canvas_execution_request
            SET status = 'FAILED',
                last_error = 'FORCE_CANCELLED',
                result_json = '{"error":"FORCE_CANCELLED"}',
                next_retry_at = NULL,
                run_token = NULL,
                updated_at = #{now}
            WHERE canvas_id = #{canvasId}
              AND status IN ('PENDING', 'RETRY', 'RUNNING')
            """)
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 mark force cancelled by canvas 计算得到的数量、金额或指标值。
     */
    int markForceCancelledByCanvas(@Param("canvasId") Long canvasId,
                                   @Param("now") LocalDateTime now);

    @Update("""
            UPDATE canvas_execution_request
            SET status = 'FAILED',
                last_error = 'FORCE_CANCELLED',
                result_json = '{"error":"FORCE_CANCELLED"}',
                next_retry_at = NULL,
                run_token = NULL,
                updated_at = #{now}
            WHERE canvas_id = #{canvasId}
              AND tenant_id = #{tenantId}
              AND status IN ('PENDING', 'RETRY', 'RUNNING')
            """)
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 mark force cancelled by canvas and tenant 计算得到的数量、金额或指标值。
     */
    int markForceCancelledByCanvasAndTenant(@Param("canvasId") Long canvasId,
                                            @Param("tenantId") Long tenantId,
                                            @Param("now") LocalDateTime now);

    @Update("""
            UPDATE canvas_execution_request
            SET status = 'PENDING',
                attempt_count = 0,
                next_retry_at = NULL,
                last_error = NULL,
                result_json = NULL,
                run_token = NULL,
                replay_count = COALESCE(replay_count, 0) + 1,
                last_replay_at = #{now},
                last_replay_by = #{replayBy},
                last_replay_reason = #{reason},
                updated_at = #{now}
            WHERE id = #{id}
            """)
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param replayBy replay by 参数，用于 markPendingForReplay 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回 mark pending for replay 计算得到的数量、金额或指标值。
     */
    int markPendingForReplay(@Param("id") String id,
                             @Param("now") LocalDateTime now,
                             @Param("replayBy") String replayBy,
                             @Param("reason") String reason);

    /**
     * 计算或统计 count By Status 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 查询、转换或计算得到的结果集合
     */
    @Select("""
    SELECT status, COUNT(*) AS count
            FROM canvas_execution_request
            WHERE status IN ('PENDING', 'RETRY', 'RUNNING')
            GROUP BY status
            """)
    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @return 返回统计数量。
     */
    List<CanvasExecutionRequestStatusCount> countByStatus();

    @Select("""
            SELECT id
            FROM canvas_execution_request
            WHERE (
                    status IN ('PENDING', 'RETRY')
                    AND (next_retry_at IS NULL OR next_retry_at <= #{now})
                  )
               OR (
                    status = 'RUNNING'
                    AND updated_at < #{staleBefore}
                  )
            ORDER BY updated_at ASC
            LIMIT #{limit}
            """)
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param staleBefore stale before 参数，用于 selectDue 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    List<String> selectDue(@Param("limit") int limit,
                           @Param("now") LocalDateTime now,
                           @Param("staleBefore") LocalDateTime staleBefore);

    @Select("""
            SELECT id, tenant_id, canvas_id, trigger_type, trigger_node_type, status, attempt_count
            FROM canvas_execution_request
            WHERE (
                    status IN ('PENDING', 'RETRY')
                    AND (next_retry_at IS NULL OR next_retry_at <= #{now})
                  )
               OR (
                    status = 'RUNNING'
                    AND updated_at < #{staleBefore}
                  )
            ORDER BY updated_at ASC
            LIMIT #{limit}
            """)
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param staleBefore stale before 参数，用于 selectDueRequests 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    List<CanvasExecutionRequestDO> selectDueRequests(@Param("limit") int limit,
                                                   @Param("now") LocalDateTime now,
                                                   @Param("staleBefore") LocalDateTime staleBefore);
}
