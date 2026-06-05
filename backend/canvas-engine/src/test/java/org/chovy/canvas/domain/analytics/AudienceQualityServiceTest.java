package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.dal.dataobject.AudienceQualityCheckDO;
import org.chovy.canvas.dal.mapper.AudienceQualityCheckMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AudienceQualityServiceTest {

    @Test
    void returnsPassWhenFreshAndDriftIsSmall() {
        AudienceQualityCheckMapper mapper = mock(AudienceQualityCheckMapper.class);
        AudienceQualityService service = new AudienceQualityService(mapper);

        AudienceQualityService.QualityResult result = service.evaluate(new AudienceQualityService.QualityInput(
                7L,
                10L,
                1000L,
                1000L,
                998L,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 1, 10, 3),
                10,
                30,
                0.05,
                0.20));

        assertThat(result.verdict()).isEqualTo("PASS");
        verify(mapper).insert(any(AudienceQualityCheckDO.class));
    }

    @Test
    void returnsWarnWhenFreshnessLagExceedsWarnThreshold() {
        AudienceQualityService service = new AudienceQualityService(mock(AudienceQualityCheckMapper.class));

        AudienceQualityService.QualityResult result = service.evaluate(new AudienceQualityService.QualityInput(
                7L,
                10L,
                1000L,
                990L,
                990L,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 1, 10, 20),
                10,
                30,
                0.05,
                0.20));

        assertThat(result.verdict()).isEqualTo("WARN");
        assertThat(result.detailJson()).contains("freshnessLagMinutes");
    }

    @Test
    void returnsFailWhenBitmapDriftExceedsFailThreshold() {
        AudienceQualityService service = new AudienceQualityService(mock(AudienceQualityCheckMapper.class));

        AudienceQualityService.QualityResult result = service.evaluate(new AudienceQualityService.QualityInput(
                7L,
                10L,
                1000L,
                1000L,
                400L,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 1, 10, 1),
                10,
                30,
                0.05,
                0.20));

        assertThat(result.verdict()).isEqualTo("FAIL");
        assertThat(result.bitmapDriftRatio()).isGreaterThan(0.20);
    }
}
