package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseIncidentDO;

import java.time.LocalDateTime;

/**
 * CdpWarehouseIncidentMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseIncidentMapper extends BaseMapper<CdpWarehouseIncidentDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_incident
            (tenant_id, incident_key, source_type, source_id, severity, status, title, description,
             occurrence_count, first_seen_at, last_seen_at)
            VALUES
            (#{row.tenantId}, #{row.incidentKey}, #{row.sourceType}, #{row.sourceId}, #{row.severity}, 'OPEN',
             #{row.title}, #{row.description}, 1, #{row.firstSeenAt}, #{row.lastSeenAt})
            ON DUPLICATE KEY UPDATE
                source_id = VALUES(source_id),
                severity = VALUES(severity),
                status = 'OPEN',
                title = VALUES(title),
                description = VALUES(description),
                occurrence_count = occurrence_count + 1,
                last_seen_at = VALUES(last_seen_at),
                resolved_by = NULL,
                resolved_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsertOpen(@Param("row") CdpWarehouseIncidentDO row);

    @Update("""
            UPDATE cdp_warehouse_incident
            SET status = 'ACKNOWLEDGED',
                acknowledged_by = #{operator},
                acknowledged_at = #{now},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND id = #{id}
              AND status = 'OPEN'
            """)
    /**
     * 执行 acknowledge 流程，围绕 acknowledge 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 acknowledge 计算得到的数量、金额或指标值。
     */
    int acknowledge(@Param("tenantId") Long tenantId,
                    @Param("id") Long id,
                    @Param("operator") String operator,
                    @Param("now") LocalDateTime now);

    @Update("""
            UPDATE cdp_warehouse_incident
            SET status = 'RESOLVED',
                resolved_by = #{operator},
                resolved_at = #{now},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND id = #{id}
              AND status IN ('OPEN', 'ACKNOWLEDGED')
            """)
    /**
     * 解析业务依赖或上下文值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 resolve 计算得到的数量、金额或指标值。
     */
    int resolve(@Param("tenantId") Long tenantId,
                @Param("id") Long id,
                @Param("operator") String operator,
                @Param("now") LocalDateTime now);

    @Update("""
            UPDATE cdp_warehouse_incident
            SET status = 'RESOLVED',
                resolved_by = #{operator},
                resolved_at = #{now},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND incident_key = #{incidentKey}
              AND source_type = 'WAREHOUSE_TABLE_DRIFT'
              AND status IN ('OPEN', 'ACKNOWLEDGED')
            """)
    /**
     * 解析业务依赖或上下文值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param incidentKey 业务键，用于在同一租户下定位资源。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 resolve table drift by key 计算得到的数量、金额或指标值。
     */
    int resolveTableDriftByKey(@Param("tenantId") Long tenantId,
                               @Param("incidentKey") String incidentKey,
                               @Param("operator") String operator,
                               @Param("now") LocalDateTime now);

    @Update("""
            UPDATE cdp_warehouse_incident
            SET status = 'RESOLVED',
                resolved_by = #{operator},
                resolved_at = #{now},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND incident_key = #{incidentKey}
              AND source_type = 'WAREHOUSE_AVAILABILITY'
              AND status IN ('OPEN', 'ACKNOWLEDGED')
            """)
    /**
     * 解析业务依赖或上下文值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param incidentKey 业务键，用于在同一租户下定位资源。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 resolve availability by key 计算得到的数量、金额或指标值。
     */
    int resolveAvailabilityByKey(@Param("tenantId") Long tenantId,
                                 @Param("incidentKey") String incidentKey,
                                 @Param("operator") String operator,
                                 @Param("now") LocalDateTime now);

    @Update("""
            UPDATE cdp_warehouse_incident
            SET status = 'RESOLVED',
                resolved_by = #{operator},
                resolved_at = #{now},
                updated_at = #{now}
            WHERE tenant_id = #{tenantId}
              AND incident_key = #{incidentKey}
              AND source_type = 'WAREHOUSE_CONSUMER_AVAILABILITY'
              AND status IN ('OPEN', 'ACKNOWLEDGED')
            """)
    /**
     * 解析业务依赖或上下文值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param incidentKey 业务键，用于在同一租户下定位资源。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 resolve consumer availability by key 计算得到的数量、金额或指标值。
     */
    int resolveConsumerAvailabilityByKey(@Param("tenantId") Long tenantId,
                                         @Param("incidentKey") String incidentKey,
                                         @Param("operator") String operator,
                                         @Param("now") LocalDateTime now);
}
