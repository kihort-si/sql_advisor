package com.sql_advisor.domain;

import java.util.Map;
import java.util.Optional;

public record AnalysisContext(
        String sql,
        AnalysisMode mode,
        Map<String, TableMetadata> tables,
        double totalExecutionTimeMs
) {
    public Optional<TableMetadata> table(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(tables.get(name.toLowerCase()));
    }
}
