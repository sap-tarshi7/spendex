package com.expensetracker.model;

public enum InvestmentType {
    STOCKS("Stocks"),
    MUTUAL_FUNDS("Mutual Funds"),
    ETFS("ETFs"),
    FIXED_DEPOSITS("Fixed Deposits"),
    GOLD("Gold"),
    OTHER("Other");

    private final String displayName;

    InvestmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static InvestmentType fromString(String text) {
        for (InvestmentType b : InvestmentType.values()) {
            if (b.name().equalsIgnoreCase(text) || b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return OTHER;
    }
}
