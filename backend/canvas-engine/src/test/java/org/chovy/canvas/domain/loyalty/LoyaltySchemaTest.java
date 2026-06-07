package org.chovy.canvas.domain.loyalty;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LoyaltySchemaTest {

    @Test
    void migrationCreatesProductionLoyaltyTables() throws Exception {
        String sql = new String(getClass().getResourceAsStream(
                "/db/migration/V277__loyalty_accounts_rules_redemption.sql").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS loyalty_member_account");
        assertThat(sql).contains("UNIQUE KEY uk_loyalty_account_user (tenant_id, user_id)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS loyalty_rule");
        assertThat(sql).contains("rule_type VARCHAR(32) NOT NULL");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS loyalty_transaction_journal");
        assertThat(sql).contains("UNIQUE KEY uk_loyalty_journal_transaction (tenant_id, transaction_key)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS loyalty_redemption");
        assertThat(sql).contains("UNIQUE KEY uk_loyalty_redemption_key (tenant_id, redemption_key)");
    }
}
