package org.chovy.canvas.marketing.domain;

import java.math.BigDecimal;
import java.util.Locale;

public record CampaignBudget(BigDecimal amount, String currency) {

    public CampaignBudget {
        amount = amount == null ? BigDecimal.ZERO : amount;
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("budgetAmount must be non-negative");
        }
        currency = normalizeCurrency(currency);
    }

    public static CampaignBudget of(BigDecimal amount, String currency) {
        return new CampaignBudget(amount, currency);
    }

    private static String normalizeCurrency(String value) {
        String trimmed = value == null ? "" : value.trim();
        String currency = trimmed.isBlank() ? "CNY" : trimmed.toUpperCase(Locale.ROOT);
        if (currency.length() > 16) {
            throw new IllegalArgumentException("currency is too long");
        }
        return currency;
    }
}
