package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.BiQuickEngineQueueJobDO;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueBacklogView;
import org.chovy.canvas.domain.bi.dataset.BiQuickEngineQueueStatusCount;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BiQuickEngineQueueJobMapper extends BaseMapper<BiQuickEngineQueueJobDO> {

    @Update("""
            UPDATE bi_quick_engine_queue_job
            SET status = 'BLOCKED',
                blocked_reason = 'queue wait timed out',
                completed_at = #{now},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND status = 'QUEUED'
              AND expires_at <= #{now}
            """)
    int expireTimedOut(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE bi_quick_engine_queue_job
            SET status = 'BLOCKED',
                blocked_reason = 'queue wait timed out',
                completed_at = #{now},
                updated_at = #{now}
            WHERE status = 'QUEUED'
              AND expires_at <= #{now}
            """)
    int expireTimedOutAll(@Param("now") LocalDateTime now);

    @Update("""
            UPDATE bi_quick_engine_queue_job
            SET status = 'CLAIMED',
                claimed_by = #{workerId},
                claimed_at = #{now},
                attempt_count = attempt_count + 1,
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND pool_key = #{poolKey}
              AND status = 'QUEUED'
              AND expires_at > #{now}
            ORDER BY queued_at ASC, id ASC
            LIMIT #{limit}
            """)
    int claimReady(@Param("tenantId") Long tenantId,
                   @Param("poolKey") String poolKey,
                   @Param("workerId") String workerId,
                   @Param("now") LocalDateTime now,
                   @Param("limit") int limit);

    @Update("""
            UPDATE bi_quick_engine_queue_job
            SET status = 'COMPLETED',
                completed_at = #{now},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND id = #{jobId}
              AND claimed_by = #{workerId}
              AND status = 'CLAIMED'
            """)
    int completeClaimed(@Param("tenantId") Long tenantId,
                        @Param("jobId") Long jobId,
                        @Param("workerId") String workerId,
                        @Param("now") LocalDateTime now);

    @Update("""
            UPDATE bi_quick_engine_queue_job
            SET status = 'COMPLETED',
                completed_at = #{now},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND id = #{jobId}
              AND status = 'QUEUED'
            """)
    int completeQueuedAdmission(@Param("tenantId") Long tenantId,
                                @Param("jobId") Long jobId,
                                @Param("now") LocalDateTime now);

    @Update("""
            UPDATE bi_quick_engine_queue_job
            SET status = 'BLOCKED',
                blocked_reason = #{reason},
                completed_at = #{now},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND id = #{jobId}
              AND claimed_by = #{workerId}
              AND status = 'CLAIMED'
            """)
    int blockClaimed(@Param("tenantId") Long tenantId,
                     @Param("jobId") Long jobId,
                     @Param("workerId") String workerId,
                     @Param("reason") String reason,
                     @Param("now") LocalDateTime now);

    @Update("""
            UPDATE bi_quick_engine_queue_job
            SET status = 'BLOCKED',
                blocked_reason = #{reason},
                completed_at = #{now},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND id = #{jobId}
              AND status = 'QUEUED'
            """)
    int blockQueuedAdmission(@Param("tenantId") Long tenantId,
                             @Param("jobId") Long jobId,
                             @Param("reason") String reason,
                             @Param("now") LocalDateTime now);

    @Update("""
            UPDATE bi_quick_engine_queue_job
            SET status = 'BLOCKED',
                blocked_reason = 'claimed queue job expired',
                completed_at = #{now},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND pool_key = #{poolKey}
              AND status = 'CLAIMED'
              AND expires_at <= #{now}
            """)
    int expireStaleClaimed(@Param("tenantId") Long tenantId,
                           @Param("poolKey") String poolKey,
                           @Param("now") LocalDateTime now);

    @Update("""
            UPDATE bi_quick_engine_queue_job
            SET status = 'QUEUED',
                claimed_by = NULL,
                claimed_at = NULL,
                blocked_reason = NULL,
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND pool_key = #{poolKey}
              AND status = 'CLAIMED'
              AND claimed_at <= #{cutoff}
              AND expires_at > #{now}
            """)
    int recoverStaleClaims(@Param("tenantId") Long tenantId,
                           @Param("poolKey") String poolKey,
                           @Param("cutoff") LocalDateTime cutoff,
                           @Param("now") LocalDateTime now);

    @Select("""
            SELECT id, tenant_id, pool_key, sql_hash, dataset_key, requested_by, status,
                   attempt_count, queued_at, expires_at, claimed_by, claimed_at,
                   completed_at, blocked_reason, created_at, updated_at
            FROM bi_quick_engine_queue_job
            WHERE tenant_id = #{tenantId}
              AND claimed_by = #{workerId}
              AND status = 'CLAIMED'
            ORDER BY claimed_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<BiQuickEngineQueueJobDO> findClaimed(@Param("tenantId") Long tenantId,
                                              @Param("workerId") String workerId,
                                              @Param("limit") int limit);

    @Select("""
            SELECT tenant_id AS tenantId,
                   pool_key AS poolKey,
                   COUNT(*) AS readyCount,
                   MIN(queued_at) AS oldestQueuedAt
            FROM bi_quick_engine_queue_job
            WHERE status = 'QUEUED'
              AND expires_at > #{now}
            GROUP BY tenant_id, pool_key
            ORDER BY MIN(queued_at) ASC, tenant_id ASC, pool_key ASC
            LIMIT #{limit}
            """)
    List<BiQuickEngineQueueBacklogView> findReadyBacklogs(@Param("now") LocalDateTime now,
                                                          @Param("limit") int limit);

    @Select("""
            SELECT id, tenant_id, pool_key, sql_hash, dataset_key, requested_by, status,
                   attempt_count, queued_at, expires_at, claimed_by, claimed_at,
                   completed_at, blocked_reason, created_at, updated_at
            FROM bi_quick_engine_queue_job
            WHERE claimed_by = #{workerId}
              AND claimed_at = #{claimedAt}
              AND status = 'CLAIMED'
            ORDER BY claimed_at ASC, tenant_id ASC, pool_key ASC, queued_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<BiQuickEngineQueueJobDO> findClaimedByWorker(@Param("workerId") String workerId,
                                                      @Param("claimedAt") LocalDateTime claimedAt,
                                                      @Param("limit") int limit);

    @Select("""
            <script>
            SELECT status, COUNT(*) AS count
            FROM bi_quick_engine_queue_job
            WHERE tenant_id = #{tenantId}
            <if test="poolKey != null">
              AND pool_key = #{poolKey}
            </if>
            GROUP BY status
            </script>
            """)
    List<BiQuickEngineQueueStatusCount> countByStatus(@Param("tenantId") Long tenantId,
                                                      @Param("poolKey") String poolKey);

    @Select("""
            <script>
            SELECT id, tenant_id, pool_key, sql_hash, dataset_key, requested_by, status,
                   attempt_count, queued_at, expires_at, claimed_by, claimed_at,
                   completed_at, blocked_reason, created_at, updated_at
            FROM bi_quick_engine_queue_job
            WHERE tenant_id = #{tenantId}
            <if test="poolKey != null">
              AND pool_key = #{poolKey}
            </if>
            <if test="status != null">
              AND status = #{status}
            </if>
            ORDER BY queued_at DESC, id DESC
            LIMIT #{limit}
            </script>
            """)
    List<BiQuickEngineQueueJobDO> findRecent(@Param("tenantId") Long tenantId,
                                             @Param("poolKey") String poolKey,
                                             @Param("status") String status,
                                             @Param("limit") int limit);
}
