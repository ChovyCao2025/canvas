package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseIncidentDO;

import java.time.LocalDateTime;

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
    int resolveConsumerAvailabilityByKey(@Param("tenantId") Long tenantId,
                                         @Param("incidentKey") String incidentKey,
                                         @Param("operator") String operator,
                                         @Param("now") LocalDateTime now);
}
