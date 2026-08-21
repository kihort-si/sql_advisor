package com.sql_advisor.domain;

import java.util.List;

public record PlanNode(
        String id,
        String nodeType,
        String relation,
        String alias,
        String indexName,
        String filter,
        String indexCondition,
        String joinFilter,
        double startupCost,
        double totalCost,
        double plannedRows,
        Double actualStartupTime,
        Double actualTotalTime,
        Double actualRows,
        double actualLoops,
        double rowsRemoved,
        BufferStats buffers,
        String sortMethod,
        Long sortSpaceUsedKb,
        String sortSpaceType,
        List<PlanNode> children
) {
    public double effectiveActualRows() {
        return actualRows == null ? 0 : actualRows * Math.max(1, actualLoops);
    }

    public double effectiveTimeMs() {
        return actualTotalTime == null ? 0 : actualTotalTime * Math.max(1, actualLoops);
    }

    public boolean isScan() {
        return nodeType != null && nodeType.contains("Scan");
    }
}
