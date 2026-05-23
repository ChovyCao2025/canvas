package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CanvasExecutionRequestMapper extends BaseMapper<CanvasExecutionRequest> {

    @Insert("""
            INSERT IGNORE INTO canvas_execution_request
            (id, canvas_id, user_id, perf_run_id, trigger_type, trigger_node_type, match_key,
             payload_json, source_msg_id, status, attempt_count, next_retry_at,
             last_error, result_json, created_at, updated_at)
            VALUES
            (#{id}, #{canvasId}, #{userId}, #{perfRunId}, #{triggerType}, #{triggerNodeType}, #{matchKey},
             #{payloadJson}, #{sourceMsgId}, #{status}, #{attemptCount}, #{nextRetryAt},
             #{lastError}, #{resultJson}, NOW(), NOW())
            """)
    int insertIgnore(CanvasExecutionRequest request);

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
    int markRunning(@Param("id") String id,
                    @Param("now") LocalDateTime now,
                    @Param("staleBefore") LocalDateTime staleBefore,
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
    int markFailed(@Param("id") String id,
                   @Param("error") String error,
                   @Param("now") LocalDateTime now,
                   @Param("runToken") String runToken);

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
    int markPendingForReplay(@Param("id") String id,
                             @Param("now") LocalDateTime now,
                             @Param("replayBy") String replayBy,
                             @Param("reason") String reason);

    @Select("""
            SELECT status, COUNT(*) AS count
            FROM canvas_execution_request
            WHERE status IN ('PENDING', 'RETRY', 'RUNNING')
            GROUP BY status
            """)
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
    List<String> selectDue(@Param("limit") int limit,
                           @Param("now") LocalDateTime now,
                           @Param("staleBefore") LocalDateTime staleBefore);
}
