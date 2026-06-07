package org.chovy.canvas.domain.bi.query;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BiDatasourceHealthSloSummary(
        int totalChecks,
        int availableChecks,
        int unavailableChecks,
        double availabilityRate,
        List<SourceSlo> sources
) {

    public static BiDatasourceHealthSloSummary from(List<BiDatasourceHealthSnapshot> snapshots) {
        List<BiDatasourceHealthSnapshot> rows = snapshots == null ? List.of() : snapshots;
        int total = rows.size();
        int available = (int) rows.stream().filter(BiDatasourceHealthSnapshot::available).count();
        Map<String, SourceAccumulator> bySource = new LinkedHashMap<>();
        for (BiDatasourceHealthSnapshot row : rows) {
            String sourceKey = row.sourceKey() == null ? "-" : row.sourceKey();
            SourceAccumulator accumulator = bySource.computeIfAbsent(sourceKey,
                    ignored -> new SourceAccumulator(sourceKey, row.sourceType()));
            accumulator.add(row);
        }
        return new BiDatasourceHealthSloSummary(
                total,
                available,
                total - available,
                rate(available, total),
                bySource.values().stream()
                        .map(SourceAccumulator::toSlo)
                        .sorted((left, right) -> {
                            int rateCompare = Double.compare(left.availabilityRate(), right.availabilityRate());
                            if (rateCompare != 0) {
                                return rateCompare;
                            }
                            return left.sourceKey().compareTo(right.sourceKey());
                        })
                        .toList());
    }

    private static double rate(int available, int total) {
        if (total <= 0) {
            return 100.0;
        }
        return Math.round((available * 10000.0) / total) / 100.0;
    }

    public record SourceSlo(
            String sourceKey,
            String sourceType,
            int totalChecks,
            int availableChecks,
            int unavailableChecks,
            double availabilityRate,
            LocalDateTime lastCheckedAt,
            String lastMessage
    ) {
    }

    private static final class SourceAccumulator {
        private final String sourceKey;
        private final String sourceType;
        private int total;
        private int available;
        private LocalDateTime lastCheckedAt;
        private String lastMessage;

        private SourceAccumulator(String sourceKey, String sourceType) {
            this.sourceKey = sourceKey;
            this.sourceType = sourceType == null ? "-" : sourceType;
        }

        private void add(BiDatasourceHealthSnapshot row) {
            total++;
            if (row.available()) {
                available++;
            }
            if (lastCheckedAt == null || (row.checkedAt() != null && row.checkedAt().isAfter(lastCheckedAt))) {
                lastCheckedAt = row.checkedAt();
                lastMessage = row.message();
            }
        }

        private SourceSlo toSlo() {
            return new SourceSlo(
                    sourceKey,
                    sourceType,
                    total,
                    available,
                    total - available,
                    rate(available, total),
                    lastCheckedAt,
                    lastMessage == null || lastMessage.isBlank() ? "-" : lastMessage);
        }
    }
}
