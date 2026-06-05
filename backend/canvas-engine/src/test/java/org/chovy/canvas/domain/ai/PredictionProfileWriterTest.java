package org.chovy.canvas.domain.ai;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PredictionProfileWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mergesPredictionFieldsIntoExistingProfileProperties() throws Exception {
        CdpUserProfileMapper profileMapper = mock(CdpUserProfileMapper.class);
        PredictionProfileWriter writer = new PredictionProfileWriter(profileMapper, objectMapper);
        CdpUserProfileDO profile = new CdpUserProfileDO();
        profile.setId(1L);
        profile.setUserId("u1");
        profile.setPropertiesJson("{\"plan\":\"vip\"}");
        when(profileMapper.selectOne(any(Wrapper.class))).thenReturn(profile);

        writer.write("u1", new BigDecimal("0.81234"), "HIGH", 20,
                LocalDateTime.of(2026, 6, 4, 10, 30));

        ArgumentCaptor<CdpUserProfileDO> captor = ArgumentCaptor.forClass(CdpUserProfileDO.class);
        verify(profileMapper).updateById(captor.capture());
        Map<String, Object> properties = objectMapper.readValue(
                captor.getValue().getPropertiesJson(),
                new TypeReference<>() {});
        assertThat(properties)
                .containsEntry("plan", "vip")
                .containsEntry(PredictionProfileWriter.CHURN_RISK_BAND, "HIGH")
                .containsEntry(PredictionProfileWriter.BEST_SEND_HOUR, 20)
                .containsEntry(PredictionProfileWriter.PREDICTION_UPDATED_AT, "2026-06-04T10:30");
        assertThat((Double) properties.get(PredictionProfileWriter.CHURN_PROBABILITY)).isEqualTo(0.81234d);
    }

    @Test
    void missingProfileIsIgnored() {
        CdpUserProfileMapper profileMapper = mock(CdpUserProfileMapper.class);
        PredictionProfileWriter writer = new PredictionProfileWriter(profileMapper, objectMapper);
        when(profileMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        writer.write("missing", new BigDecimal("0.50000"), "MEDIUM", 20,
                LocalDateTime.of(2026, 6, 4, 10, 30));

        verify(profileMapper, never()).updateById(any(CdpUserProfileDO.class));
    }
}
