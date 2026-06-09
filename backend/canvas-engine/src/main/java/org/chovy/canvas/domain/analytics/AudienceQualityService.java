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
/**
 * AudienceQualityService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class AudienceQualityService {

    private static final String PASS = "PASS";
    private static final String WARN = "WARN";
    private static final String FAIL = "FAIL";

    private final AudienceQualityCheckMapper mapper;

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param input 输入数据，用于驱动规则判断或对象转换。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    public QualityResult evaluate(QualityInput input) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        mapper.insert(row);

        // 汇总前面计算出的状态和明细，返回给调用方。
        return new QualityResult(verdict, detailJson, bitmapDriftRatio);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sourceEventTime 时间参数，用于计算窗口、过期或审计时间。
     * @param checkedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 freshness lag minutes 计算得到的数量、金额或指标值。
     */
    private long freshnessLagMinutes(LocalDateTime sourceEventTime, LocalDateTime checkedAt) {
        if (sourceEventTime == null || checkedAt == null) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, Duration.between(sourceEventTime, checkedAt).toMinutes());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private long value(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param expected 待处理业务值，用于规则计算、转换或外部调用。
     * @param actual actual 参数，用于 driftRatio 流程中的校验、计算或对象转换。
     * @return 返回 drift ratio 计算得到的数量、金额或指标值。
     */
    private double driftRatio(long expected, long actual) {
        if (expected == 0L) {
            return actual == 0L ? 0D : 1D;
        }
        return Math.abs(expected - actual) / (double) expected;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param freshnessLagMinutes freshness lag minutes 参数，用于 detailJson 流程中的校验、计算或对象转换。
     * @param dorisDriftRatio doris drift ratio 参数，用于 detailJson 流程中的校验、计算或对象转换。
     * @param bitmapDriftRatio bitmap drift ratio 参数，用于 detailJson 流程中的校验、计算或对象转换。
     * @return 返回 detail json 生成的文本或业务键。
     */
    private String detailJson(long freshnessLagMinutes, double dorisDriftRatio, double bitmapDriftRatio) {
        return String.format(Locale.ROOT,
                "{\"freshnessLagMinutes\":%d,\"dorisDriftRatio\":%.6f,\"bitmapDriftRatio\":%.6f}",
                freshnessLagMinutes,
                dorisDriftRatio,
                bitmapDriftRatio);
    }

    /**
     * QualityInput 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * QualityResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record QualityResult(String verdict, String detailJson, double bitmapDriftRatio) {
    }
}
