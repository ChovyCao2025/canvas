package org.chovy.canvas.domain.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AiDecisionModelSchemaTest {

    @Test
    void migrationCreatesDecisionRunRecommendationAndFeedbackTables() throws IOException {
        String sql = new String(getClass().getResourceAsStream(
                "/db/migration/V309__ai_decision_models.sql").readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("ai_decision_run");
        assertThat(sql).contains("ai_user_decision_recommendation");
        assertThat(sql).contains("ai_decision_feedback");
        assertThat(sql).contains("uk_ai_decision_run");
        assertThat(sql).contains("idx_ai_decision_reco_type_rank");
        assertThat(sql).contains("idx_ai_decision_feedback_reco");
    }
}
