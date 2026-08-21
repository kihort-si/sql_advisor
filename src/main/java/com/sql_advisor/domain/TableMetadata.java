package com.sql_advisor.domain;

import java.time.Instant;
import java.util.List;

public record TableMetadata(
        String schema,
        String table,
        long estimatedRows,
        long totalBytes,
        Instant lastAnalyze,
        List<IndexMetadata> indexes
) {
    public boolean hasIndexStartingWith(String column) {
        return indexes.stream().anyMatch(index -> !index.columns().isEmpty()
                && index.columns().getFirst().equalsIgnoreCase(column));
    }
}
