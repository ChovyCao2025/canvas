package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseTableContractDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseTableInspectionDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseTableContractMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseTableInspectionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseTableGovernanceServiceTest {

    @Test
    void upsertContractNormalizesAndPersistsTenantContract() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        CdpWarehouseTableGovernanceService service = service(contractMapper,
                mock(CdpWarehouseTableInspectionMapper.class), asset -> "");

        CdpWarehouseTableGovernanceService.TableContractView result =
                service.upsertContract(9L, new CdpWarehouseTableGovernanceService.TableContractCommand(
                        "tenant_user_wide", "tenant_user_wide", "dws", "canvas_dws.tenant_user_wide",
                        null, "infrastructure/doris/custom.sql", "stat_date", null, 365,
                        3, 24, "tenant_id,user_id", null, null, "ops",
                        "tenant table", "{\"dynamic_partition\":true}"));

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.layer()).isEqualTo("DWS");
        assertThat(result.engineType()).isEqualTo("DORIS");
        assertThat(result.partitionGranularity()).isEqualTo("DAY");
        assertThat(result.lifecycleStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<CdpWarehouseTableContractDO> row =
                ArgumentCaptor.forClass(CdpWarehouseTableContractDO.class);
        verify(contractMapper).upsert(row.capture());
        assertThat(row.getValue().getPhysicalName()).isEqualTo("canvas_dws.tenant_user_wide");
        assertThat(row.getValue().getBucketCount()).isEqualTo(24);
    }

    @Test
    void upsertContractRejectsInvalidPhysicalShape() {
        CdpWarehouseTableGovernanceService service = service(mock(CdpWarehouseTableContractMapper.class),
                mock(CdpWarehouseTableInspectionMapper.class), asset -> "");

        assertThatThrownBy(() -> service.upsertContract(9L,
                new CdpWarehouseTableGovernanceService.TableContractCommand(
                        "bad", "bad", "DWS", "canvas_dws.bad", "DORIS", "asset.sql",
                        "stat_date", "DAY", 0, 3, 8, "tenant_id", null,
                        "ACTIVE", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retentionDays must be positive");
    }

    @Test
    void listContractsMergesBuiltInAndTenantOverride() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        when(contractMapper.selectList(any())).thenReturn(List.of(
                contract(1L, 0L, "canvas_daily_stats", "canvas_dws.canvas_daily_stats", 8, 730),
                contract(2L, 9L, "canvas_daily_stats", "tenant_dws.canvas_daily_stats", 16, 365),
                contract(3L, 0L, "node_daily_stats", "canvas_dws.node_daily_stats", 8, 730)
        ));
        CdpWarehouseTableGovernanceService service = service(contractMapper,
                mock(CdpWarehouseTableInspectionMapper.class), asset -> "");

        List<CdpWarehouseTableGovernanceService.TableContractView> rows =
                service.listContracts(9L, "dws", "active");

        assertThat(rows).extracting(CdpWarehouseTableGovernanceService.TableContractView::tableKey)
                .containsExactly("canvas_daily_stats", "node_daily_stats");
        assertThat(rows.get(0).tenantId()).isEqualTo(9L);
        assertThat(rows.get(0).physicalName()).isEqualTo("tenant_dws.canvas_daily_stats");
        assertThat(rows.get(0).bucketCount()).isEqualTo(16);
    }

    @Test
    void inspectContractPassesCompliantDdlAndPersistsEvidence() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        CdpWarehouseTableInspectionMapper inspectionMapper = mock(CdpWarehouseTableInspectionMapper.class);
        when(contractMapper.selectList(any())).thenReturn(List.of(
                contract(1L, 0L, "canvas_daily_stats", "canvas_dws.canvas_daily_stats", 8, 730)));
        when(inspectionMapper.insert(any(CdpWarehouseTableInspectionDO.class))).thenAnswer(invocation -> {
            CdpWarehouseTableInspectionDO row = invocation.getArgument(0);
            row.setId(101L);
            return 1;
        });
        CdpWarehouseTableGovernanceService service = service(contractMapper, inspectionMapper,
                asset -> compliantDdl());

        CdpWarehouseTableGovernanceService.InspectionReport report =
                service.inspectContract(9L, "canvas_daily_stats", "qa");

        assertThat(report.id()).isEqualTo(101L);
        assertThat(report.tenantId()).isEqualTo(9L);
        assertThat(report.status()).isEqualTo("PASS");
        assertThat(report.violationCount()).isZero();
        assertThat(report.checkedItems()).isEqualTo(8);

        ArgumentCaptor<CdpWarehouseTableInspectionDO> inspection =
                ArgumentCaptor.forClass(CdpWarehouseTableInspectionDO.class);
        verify(inspectionMapper).insert(inspection.capture());
        assertThat(inspection.getValue().getTenantId()).isEqualTo(9L);
        assertThat(inspection.getValue().getStatus()).isEqualTo("PASS");
        assertThat(inspection.getValue().getViolationsJson()).isEqualTo("[]");
        assertThat(inspection.getValue().getInspectedBy()).isEqualTo("qa");
        verify(contractMapper).updateInspection(eq(0L), eq("canvas_daily_stats"),
                any(LocalDateTime.class), eq("PASS"), eq("Physical table contract passed"));
    }

    @Test
    void inspectContractWarnsWhenDdlViolatesPhysicalContract() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        CdpWarehouseTableInspectionMapper inspectionMapper = mock(CdpWarehouseTableInspectionMapper.class);
        when(contractMapper.selectList(any())).thenReturn(List.of(
                contract(1L, 0L, "canvas_daily_stats", "canvas_dws.canvas_daily_stats", 8, 730)));
        CdpWarehouseTableGovernanceService service = service(contractMapper, inspectionMapper,
                asset -> """
                        CREATE TABLE IF NOT EXISTS canvas_dws.canvas_daily_stats (
                            stat_date DATE NOT NULL,
                            canvas_id BIGINT NOT NULL
                        )
                        AGGREGATE KEY(stat_date, canvas_id)
                        DISTRIBUTED BY HASH(canvas_id) BUCKETS 4
                        PROPERTIES ("replication_num" = "1");
                        """);

        CdpWarehouseTableGovernanceService.InspectionReport report =
                service.inspectContract(9L, "canvas_daily_stats", "qa");

        assertThat(report.status()).isEqualTo("WARN");
        assertThat(report.violations()).anyMatch(value -> value.contains("RANGE partition"));
        assertThat(report.violations()).anyMatch(value -> value.contains("replication_num"));
        assertThat(report.violations()).anyMatch(value -> value.contains("bucket count"));
        verify(contractMapper).updateInspection(eq(0L), eq("canvas_daily_stats"),
                any(LocalDateTime.class), eq("WARN"), org.mockito.ArgumentMatchers.contains("violation"));
    }

    @Test
    void inspectContractFailsWhenDdlAssetCannotBeRead() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        when(contractMapper.selectList(any())).thenReturn(List.of(
                contract(1L, 0L, "canvas_daily_stats", "canvas_dws.canvas_daily_stats", 8, 730)));
        CdpWarehouseTableGovernanceService service = service(contractMapper,
                mock(CdpWarehouseTableInspectionMapper.class), asset -> {
                    throw new IllegalArgumentException("missing");
                });

        CdpWarehouseTableGovernanceService.InspectionReport report =
                service.inspectContract(9L, "canvas_daily_stats", "qa");

        assertThat(report.status()).isEqualTo("FAIL");
        assertThat(report.violations()).contains("DDL asset could not be read: infrastructure/doris/trace-ddl.sql");
    }

    @Test
    void inspectLiveContractPassesCompliantDorisDdlAndPersistsEvidence() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        CdpWarehouseTableInspectionMapper inspectionMapper = mock(CdpWarehouseTableInspectionMapper.class);
        when(contractMapper.selectList(any())).thenReturn(List.of(
                contract(1L, 0L, "canvas_daily_stats", "canvas_dws.canvas_daily_stats", 8, 730)));
        when(inspectionMapper.insert(any(CdpWarehouseTableInspectionDO.class))).thenAnswer(invocation -> {
            CdpWarehouseTableInspectionDO row = invocation.getArgument(0);
            row.setId(202L);
            return 1;
        });
        CdpWarehouseTableGovernanceService service = service(contractMapper, inspectionMapper,
                asset -> "", physicalName -> compliantDdl());

        CdpWarehouseTableGovernanceService.InspectionReport report =
                service.inspectLiveContract(9L, "canvas_daily_stats", "qa");

        assertThat(report.id()).isEqualTo(202L);
        assertThat(report.status()).isEqualTo("PASS");
        assertThat(report.ddlAssetPath()).isEqualTo("LIVE:SHOW_CREATE_TABLE");
        assertThat(report.checkedItems()).isEqualTo(8);

        ArgumentCaptor<CdpWarehouseTableInspectionDO> inspection =
                ArgumentCaptor.forClass(CdpWarehouseTableInspectionDO.class);
        verify(inspectionMapper).insert(inspection.capture());
        assertThat(inspection.getValue().getDdlAssetPath()).isEqualTo("LIVE:SHOW_CREATE_TABLE");
        assertThat(inspection.getValue().getStatus()).isEqualTo("PASS");
        verify(contractMapper).updateInspection(eq(0L), eq("canvas_daily_stats"),
                any(LocalDateTime.class), eq("PASS"), eq("Physical table contract passed"));
    }

    @Test
    void inspectLiveContractFailsClosedWhenLiveDdlReaderIsMissing() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        CdpWarehouseTableInspectionMapper inspectionMapper = mock(CdpWarehouseTableInspectionMapper.class);
        when(contractMapper.selectList(any())).thenReturn(List.of(
                contract(1L, 0L, "canvas_daily_stats", "canvas_dws.canvas_daily_stats", 8, 730)));
        CdpWarehouseTableGovernanceService service = service(contractMapper, inspectionMapper,
                asset -> "");

        CdpWarehouseTableGovernanceService.InspectionReport report =
                service.inspectLiveContract(9L, "canvas_daily_stats", "qa");

        assertThat(report.status()).isEqualTo("FAIL");
        assertThat(report.ddlAssetPath()).isEqualTo("LIVE:SHOW_CREATE_TABLE");
        assertThat(report.violations()).contains("live Doris DDL reader is not configured");
        verify(contractMapper).updateInspection(eq(0L), eq("canvas_daily_stats"),
                any(LocalDateTime.class), eq("FAIL"), org.mockito.ArgumentMatchers.contains("live Doris DDL reader"));
    }

    @Test
    void inspectLiveAllSummarizesActiveContracts() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        CdpWarehouseTableInspectionMapper inspectionMapper = mock(CdpWarehouseTableInspectionMapper.class);
        CdpWarehouseTableContractDO row =
                contract(1L, 0L, "canvas_daily_stats", "canvas_dws.canvas_daily_stats", 8, 730);
        when(contractMapper.selectList(any())).thenReturn(List.of(row), List.of(row));
        CdpWarehouseTableGovernanceService service = service(contractMapper, inspectionMapper,
                asset -> "", physicalName -> compliantDdl());

        CdpWarehouseTableGovernanceService.InspectionSummary summary =
                service.inspectLiveAll(9L, "qa");

        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.passed()).isEqualTo(1);
        assertThat(summary.warned()).isZero();
        assertThat(summary.failed()).isZero();
        assertThat(summary.reports().get(0).ddlAssetPath()).isEqualTo("LIVE:SHOW_CREATE_TABLE");
    }

    @Test
    void planRemediationReturnsEmptyStepsForPassingInspection() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        CdpWarehouseTableInspectionMapper inspectionMapper = mock(CdpWarehouseTableInspectionMapper.class);
        when(contractMapper.selectList(any())).thenReturn(List.of(
                contract(1L, 0L, "canvas_daily_stats", "canvas_dws.canvas_daily_stats", 8, 730)));
        when(inspectionMapper.insert(any(CdpWarehouseTableInspectionDO.class))).thenAnswer(invocation -> {
            CdpWarehouseTableInspectionDO row = invocation.getArgument(0);
            row.setId(301L);
            return 1;
        });
        CdpWarehouseTableGovernanceService service = service(contractMapper, inspectionMapper,
                asset -> "", physicalName -> compliantDdl());

        CdpWarehouseTableGovernanceService.TableRemediationPlan plan =
                service.planRemediation(9L, "canvas_daily_stats", true, "qa");

        assertThat(plan.live()).isTrue();
        assertThat(plan.inspectionId()).isEqualTo(301L);
        assertThat(plan.status()).isEqualTo("PASS");
        assertThat(plan.steps()).isEmpty();
    }

    @Test
    void planRemediationGeneratesExecutableSqlForSafePropertyDrift() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        CdpWarehouseTableInspectionMapper inspectionMapper = mock(CdpWarehouseTableInspectionMapper.class);
        when(contractMapper.selectList(any())).thenReturn(List.of(
                contract(1L, 0L, "canvas_daily_stats", "canvas_dws.canvas_daily_stats", 8, 730)));
        CdpWarehouseTableGovernanceService service = service(contractMapper, inspectionMapper,
                asset -> propertyDriftDdl());

        CdpWarehouseTableGovernanceService.TableRemediationPlan plan =
                service.planRemediation(9L, "canvas_daily_stats", false, "qa");

        assertThat(plan.status()).isEqualTo("WARN");
        assertThat(plan.steps()).extracting(CdpWarehouseTableGovernanceService.RemediationStep::code)
                .containsExactlyInAnyOrder(
                        "DYNAMIC_PARTITION_ENABLE",
                        "DYNAMIC_PARTITION_TIME_UNIT",
                        "DYNAMIC_PARTITION_RETENTION",
                        "REPLICATION_NUM");
        assertThat(plan.steps()).allSatisfy(step -> {
            assertThat(step.executable()).isTrue();
            assertThat(step.riskLevel()).isEqualTo("MEDIUM");
            assertThat(step.recommendedSql()).startsWith("ALTER TABLE `canvas_dws`.`canvas_daily_stats` SET");
        });
        assertThat(plan.steps()).anySatisfy(step ->
                assertThat(step.recommendedSql()).isEqualTo(
                        "ALTER TABLE `canvas_dws`.`canvas_daily_stats` SET (\"replication_num\" = \"3\");"));
    }

    @Test
    void planRemediationKeepsStructuralDriftManual() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        CdpWarehouseTableInspectionMapper inspectionMapper = mock(CdpWarehouseTableInspectionMapper.class);
        when(contractMapper.selectList(any())).thenReturn(List.of(
                contract(1L, 0L, "canvas_daily_stats", "canvas_dws.canvas_daily_stats", 8, 730)));
        CdpWarehouseTableGovernanceService service = service(contractMapper, inspectionMapper,
                asset -> structuralDriftDdl());

        CdpWarehouseTableGovernanceService.TableRemediationPlan plan =
                service.planRemediation(9L, "canvas_daily_stats", false, "qa");

        assertThat(plan.status()).isEqualTo("WARN");
        assertThat(plan.steps()).extracting(CdpWarehouseTableGovernanceService.RemediationStep::code)
                .contains("PARTITION_REBUILD_REQUIRED", "DISTRIBUTION_REBUILD_REQUIRED", "BUCKET_REBUILD_REQUIRED");
        assertThat(plan.steps()).allSatisfy(step -> {
            assertThat(step.executable()).isFalse();
            assertThat(step.riskLevel()).isEqualTo("HIGH");
            assertThat(step.recommendedSql()).isNull();
        });
    }

    @Test
    void planAllRemediationSummarizesExecutableAndManualSteps() {
        CdpWarehouseTableContractMapper contractMapper = mock(CdpWarehouseTableContractMapper.class);
        CdpWarehouseTableInspectionMapper inspectionMapper = mock(CdpWarehouseTableInspectionMapper.class);
        CdpWarehouseTableContractDO row =
                contract(1L, 0L, "canvas_daily_stats", "canvas_dws.canvas_daily_stats", 8, 730);
        when(contractMapper.selectList(any())).thenReturn(List.of(row), List.of(row));
        CdpWarehouseTableGovernanceService service = service(contractMapper, inspectionMapper,
                asset -> propertyDriftDdl());

        CdpWarehouseTableGovernanceService.RemediationSummary summary =
                service.planAllRemediation(9L, false, "qa");

        assertThat(summary.tenantId()).isEqualTo(9L);
        assertThat(summary.live()).isFalse();
        assertThat(summary.total()).isEqualTo(1);
        assertThat(summary.tablesWithRemediation()).isEqualTo(1);
        assertThat(summary.executableSteps()).isEqualTo(4);
        assertThat(summary.manualSteps()).isZero();
        assertThat(summary.tables()).hasSize(1);
    }

    private CdpWarehouseTableGovernanceService service(CdpWarehouseTableContractMapper contractMapper,
                                                       CdpWarehouseTableInspectionMapper inspectionMapper,
                                                       CdpWarehouseTableGovernanceService.DdlAssetReader reader) {
        return new CdpWarehouseTableGovernanceService(contractMapper, inspectionMapper, reader);
    }

    private CdpWarehouseTableGovernanceService service(CdpWarehouseTableContractMapper contractMapper,
                                                       CdpWarehouseTableInspectionMapper inspectionMapper,
                                                       CdpWarehouseTableGovernanceService.DdlAssetReader reader,
                                                       CdpWarehouseTableGovernanceService.LiveDdlReader liveReader) {
        return new CdpWarehouseTableGovernanceService(contractMapper, inspectionMapper, reader, liveReader);
    }

    private CdpWarehouseTableContractDO contract(Long id, Long tenantId, String tableKey,
                                                 String physicalName, int buckets, int retentionDays) {
        CdpWarehouseTableContractDO row = new CdpWarehouseTableContractDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setTableKey(tableKey);
        row.setDatasetKey(tableKey);
        row.setLayer("DWS");
        row.setPhysicalName(physicalName);
        row.setEngineType("DORIS");
        row.setDdlAssetPath("infrastructure/doris/trace-ddl.sql");
        row.setPartitionColumn("stat_date");
        row.setPartitionGranularity("DAY");
        row.setRetentionDays(retentionDays);
        row.setReplicaCount(3);
        row.setBucketCount(buckets);
        row.setDistributionColumns("canvas_id");
        row.setLifecycleStatus("ACTIVE");
        row.setOwnerName("data-platform");
        row.setDescription("contract");
        row.setExpectedPropertiesJson("{}");
        return row;
    }

    private String compliantDdl() {
        return """
                CREATE TABLE IF NOT EXISTS canvas_dws.canvas_daily_stats (
                    stat_date DATE NOT NULL,
                    canvas_id BIGINT NOT NULL
                )
                AGGREGATE KEY(stat_date, canvas_id)
                PARTITION BY RANGE(stat_date) ()
                DISTRIBUTED BY HASH(canvas_id) BUCKETS 8
                PROPERTIES (
                    "replication_num" = "3",
                    "dynamic_partition.enable" = "true",
                    "dynamic_partition.time_unit" = "DAY",
                    "dynamic_partition.start" = "-730",
                    "dynamic_partition.buckets" = "8"
                );
                """;
    }

    private String propertyDriftDdl() {
        return """
                CREATE TABLE IF NOT EXISTS canvas_dws.canvas_daily_stats (
                    stat_date DATE NOT NULL,
                    canvas_id BIGINT NOT NULL
                )
                AGGREGATE KEY(stat_date, canvas_id)
                PARTITION BY RANGE(stat_date) ()
                DISTRIBUTED BY HASH(canvas_id) BUCKETS 8
                PROPERTIES (
                    "replication_num" = "1",
                    "dynamic_partition.enable" = "false",
                    "dynamic_partition.time_unit" = "HOUR",
                    "dynamic_partition.start" = "-30"
                );
                """;
    }

    private String structuralDriftDdl() {
        return """
                CREATE TABLE IF NOT EXISTS canvas_dws.canvas_daily_stats (
                    stat_date DATE NOT NULL,
                    canvas_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL
                )
                AGGREGATE KEY(stat_date, canvas_id)
                DISTRIBUTED BY HASH(user_id) BUCKETS 4
                PROPERTIES (
                    "replication_num" = "3",
                    "dynamic_partition.enable" = "true",
                    "dynamic_partition.time_unit" = "DAY",
                    "dynamic_partition.start" = "-730"
                );
                """;
    }
}
