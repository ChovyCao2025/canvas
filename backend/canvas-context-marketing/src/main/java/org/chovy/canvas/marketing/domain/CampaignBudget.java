package org.chovy.canvas.marketing.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * 表示CampaignBudget的数据结构。
 */
public final class CampaignBudget {

    /**
     * 预算金额。
     */
    private final BigDecimal amount;

    /**
     * 币种代码。
     */
    private final String currency;

    /**
     * 创建CampaignBudget实例。
     */
    public CampaignBudget(BigDecimal amount, String currency) {
        amount = amount == null ? BigDecimal.ZERO : amount;
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("budgetAmount must be non-negative");
        }
        currency = normalizeCurrency(currency);

        this.amount = amount;
        this.currency = currency;
    }

    /**
     * 返回预算金额。
     */
    public BigDecimal amount() {
        return amount;
    }

    /**
     * 返回币种代码。
     */
    public String currency() {
        return currency;
    }




    /**
     * 执行of业务操作。
     */
    public static CampaignBudget of(BigDecimal amount, String currency) {
        return new CampaignBudget(amount, currency);
    }

    /**
     * 规范化currency输入值。
     */
    private static String normalizeCurrency(String value) {
        String trimmed = value == null ? "" : value.trim();
        String currency = trimmed.isBlank() ? "CNY" : trimmed.toUpperCase(Locale.ROOT);
        if (currency.length() > 16) {
            throw new IllegalArgumentException("currency is too long");
        }
        return currency;
    }

    /**
     * 比较两个实例的组件值是否一致。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CampaignBudget that = (CampaignBudget) o;
        return                 Objects.equals(amount, that.amount) &&
                Objects.equals(currency, that.currency);
    }

    /**
     * 根据组件值计算哈希值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    /**
     * 返回与记录类型一致的组件展示文本。
     */
    @Override
    public String toString() {
        return "CampaignBudget[amount=" + amount + ", currency=" + currency + "]";
    }
}
