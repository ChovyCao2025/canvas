package org.chovy.canvas.infrastructure.bi;

import org.chovy.canvas.dal.dataobject.BiDatasourceHealthSnapshotDO;
import org.chovy.canvas.dal.mapper.BiDatasourceHealthSnapshotMapper;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class JdbcBiQueryExecutorTest {

    @Test
    void healthHistorySamplesCurrentHealthWhenNoPreviousSnapshotsExist() {
        ObjectProvider<JdbcTemplate> emptyProvider = mockProvider(null);
        JdbcBiQueryExecutor executor = new JdbcBiQueryExecutor(emptyProvider, emptyProvider, 30, 1000);

        List<BiDatasourceHealthSnapshot> snapshots = executor.healthHistory(4);

        assertThat(snapshots)
                .extracting(BiDatasourceHealthSnapshot::sourceKey)
                .containsExactly("primary", "doris");
        assertThat(snapshots)
                .extracting(BiDatasourceHealthSnapshot::available)
                .containsExactly(false, false);
    }

    @Test
    void healthPersistsDatasourceSnapshots() {
        ObjectProvider<JdbcTemplate> emptyProvider = mockProvider(null);
        BiDatasourceHealthSnapshotMapper mapper = mock(BiDatasourceHealthSnapshotMapper.class);
        JdbcBiQueryExecutor executor = new JdbcBiQueryExecutor(emptyProvider, emptyProvider, 30, 1000, mapper);

        executor.health();

        ArgumentCaptor<BiDatasourceHealthSnapshotDO> snapshot = ArgumentCaptor.forClass(BiDatasourceHealthSnapshotDO.class);
        verify(mapper, org.mockito.Mockito.times(2)).insert(snapshot.capture());
        assertThat(snapshot.getAllValues()).extracting(BiDatasourceHealthSnapshotDO::getSourceKey)
                .containsExactly("doris", "primary");
        assertThat(snapshot.getAllValues()).extracting(BiDatasourceHealthSnapshotDO::getAvailable)
                .containsExactly(false, false);
    }

    @Test
    void healthHistoryReadsPersistedSnapshotsNewestFirst() {
        ObjectProvider<JdbcTemplate> emptyProvider = mockProvider(null);
        BiDatasourceHealthSnapshotMapper mapper = mock(BiDatasourceHealthSnapshotMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                snapshotRow("primary", "MYSQL", true, "available", "2026-06-05T08:05:00"),
                snapshotRow("doris", "DORIS", false, "timeout", "2026-06-05T08:10:00"),
                snapshotRow("primary", "MYSQL", true, "available", "2026-06-05T08:15:00")
        ));
        JdbcBiQueryExecutor executor = new JdbcBiQueryExecutor(emptyProvider, emptyProvider, 30, 1000, mapper);

        List<BiDatasourceHealthSnapshot> snapshots = executor.healthHistory(2);

        assertThat(snapshots).extracting(BiDatasourceHealthSnapshot::sourceKey)
                .containsExactly("primary", "doris");
        assertThat(snapshots).extracting(BiDatasourceHealthSnapshot::checkedAt)
                .containsExactly(
                        LocalDateTime.parse("2026-06-05T08:15:00"),
                        LocalDateTime.parse("2026-06-05T08:10:00"));
    }

    private static ObjectProvider<JdbcTemplate> mockProvider(JdbcTemplate jdbcTemplate) {
        @SuppressWarnings("unchecked")
        ObjectProvider<JdbcTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(jdbcTemplate);
        return provider;
    }

    private BiDatasourceHealthSnapshotDO snapshotRow(String sourceKey,
                                                     String sourceType,
                                                     boolean available,
                                                     String message,
                                                     String checkedAt) {
        BiDatasourceHealthSnapshotDO row = new BiDatasourceHealthSnapshotDO();
        row.setSourceKey(sourceKey);
        row.setSourceType(sourceType);
        row.setAvailable(available);
        row.setMessage(message);
        row.setCheckedAt(LocalDateTime.parse(checkedAt));
        return row;
    }
}
