package com.sql_advisor.domain;

import java.util.List;
import java.util.Map;

public record OptimizationIssue(
        String rule,
        Severity severity,
        String title,
        String description,
        String nodeId,
        Map<String, String> evidence,
        List<String> suggestions,
        String suggestedSql
) {
}
