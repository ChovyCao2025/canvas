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

/**
 * BiQuickEngineQueueJobMapper 定义 dal.mapper 场景中的扩展契约。
 */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 expire timed out 计算得到的数量、金额或指标值。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 expire timed out all 计算得到的数量、金额或指标值。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 claim ready 计算得到的数量、金额或指标值。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 complete claimed 计算得到的数量、金额或指标值。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 complete queued admission 计算得到的数量、金额或指标值。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 block claimed 计算得到的数量、金额或指标值。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 block queued admission 计算得到的数量、金额或指标值。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 expire stale claimed 计算得到的数量、金额或指标值。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param cutoff cutoff 参数，用于 recoverStaleClaims 流程中的校验、计算或对象转换。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 recover stale claims 计算得到的数量、金额或指标值。
     */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
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
    /**
     * 查询或读取业务数据。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
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
    /**
     * 查询或读取业务数据。
     *
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param claimedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @return 返回统计数量。
     */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    List<BiQuickEngineQueueJobDO> findRecent(@Param("tenantId") Long tenantId,
                                             @Param("poolKey") String poolKey,
                                             @Param("status") String status,
                                             @Param("limit") int limit);
}
