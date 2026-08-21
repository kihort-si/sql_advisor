package com.sql_advisor.domain;

public enum Severity {
    INFO(3), WARNING(8), CRITICAL(18);

    private final int scorePenalty;

    Severity(int scorePenalty) {
        this.scorePenalty = scorePenalty;
    }

    public int scorePenalty() {
        return scorePenalty;
    }
}
