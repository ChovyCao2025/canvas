package org.chovy.canvas.domain.analytics;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.AudienceQualityCheckDO;
import org.chovy.canvas.dal.mapper.AudienceQualityCheckMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AudienceQualityService {

    private static final String PASS = "PASS";
    private static final String WARN = "WARN";
    private static final String FAIL = "FAIL";

    private final AudienceQualityCheckMapper mapper;

    public QualityResult evaluate(QualityInput input) {
        if (input == null) {
            throw new IllegalArgumentException("quality input is required");
        }

        long mysqlCount = value(input.mysqlCount());
        long dorisCount = value(input.dorisCount());
        long bitmapCount = value(input.bitmapCount());
        long freshnessLagMinutes = freshnessLagMinutes(input.sourceEventTime(), input.checkedAt());
        double dorisDriftRatio = driftRatio(mysqlCount, dorisCount);
        double bitmapDriftRatio = driftRatio(dorisCount, bitmapCount);

        String verdict = PASS;
        if (freshnessLagMinutes >= input.failLagMinutes()
                || dorisDriftRatio >= input.failDriftRatio()
                || bitmapDriftRatio >= input.failDriftRatio()) {
            verdict = FAIL;
        } else if (freshnessLagMinutes >= input.warnLagMinutes()
                || dorisDriftRatio >= input.warnDriftRatio()
                || bitmapDriftRatio >= input.warnDriftRatio()) {
            verdict = WARN;
        }

        String detailJson = detailJson(freshnessLagMinutes, dorisDriftRatio, bitmapDriftRatio);
        AudienceQualityCheckDO row = new AudienceQualityCheckDO();
        row.setTenantId(input.tenantId());
        row.setAudienceId(input.audienceId());
        row.setMysqlCount(mysqlCount);
        row.setDorisCount(dorisCount);
        row.setBitmapCount(bitmapCount);
        row.setFreshnessLagMinutes(freshnessLagMinutes);
        row.setBitmapDriftRatio(bitmapDriftRatio);
        row.setVerdict(verdict);
        row.setDetailJson(detailJson);
        row.setCheckedAt(input.checkedAt() == null ? LocalDateTime.now() : input.checkedAt());
        mapper.insert(row);

        return new QualityResult(verdict, detailJson, bitmapDriftRatio);
    }

    private long freshnessLagMinutes(LocalDateTime sourceEventTime, LocalDateTime checkedAt) {
        if (sourceEventTime == null || checkedAt == null) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, Duration.between(sourceEventTime, checkedAt).toMinutes());
    }

    private long value(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    private double driftRatio(long expected, long actual) {
        if (expected == 0L) {
            return actual == 0L ? 0D : 1D;
        }
        return Math.abs(expected - actual) / (double) expected;
    }

    private String detailJson(long freshnessLagMinutes, double dorisDriftRatio, double bitmapDriftRatio) {
        return String.format(Locale.ROOT,
                "{\"freshnessLagMinutes\":%d,\"dorisDriftRatio\":%.6f,\"bitmapDriftRatio\":%.6f}",
                freshnessLagMinutes,
                dorisDriftRatio,
                bitmapDriftRatio);
    }

    public record QualityInput(
            Long tenantId,
            Long audienceId,
            Long mysqlCount,
            Long dorisCount,
            Long bitmapCount,
            LocalDateTime sourceEventTime,
            LocalDateTime checkedAt,
            int warnLagMinutes,
            int failLagMinutes,
            double warnDriftRatio,
            double failDriftRatio) {
    }

    public record QualityResult(String verdict, String detailJson, double bitmapDriftRatio) {
    }
}
