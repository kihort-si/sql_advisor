package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import com.sql_advisor.engine.RuleSupport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MissingIndexRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        if (!"Seq Scan".equals(node.nodeType()) || node.relation() == null || node.filter() == null) return false;
        var table = context.table(node.relation());
        var column = RuleSupport.filterColumn(node);
        if (table.isEmpty() || column.isEmpty() || table.get().hasIndexStartingWith(column.get())) return false;
        double scanned = node.actualRows() == null
                ? table.get().estimatedRows()
                : node.effectiveActualRows() + node.rowsRemoved() * Math.max(1, node.actualLoops());
        double returned = node.actualRows() == null ? node.plannedRows() : node.effectiveActualRows();
        return table.get().estimatedRows() > 100_000 && scanned > 10_000 && returned / Math.max(1, scanned) < .01;
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        String column = RuleSupport.filterColumn(node).orElse("filtered_column");
        String safeTable = node.relation().replaceAll("[^a-zA-Z0-9_]", "");
        String safeColumn = column.replaceAll("[^a-zA-Z0-9_]", "");
        double scanned = node.effectiveActualRows() + node.rowsRemoved() * Math.max(1, node.actualLoops());
        return new OptimizationIssue("MissingIndexRule", Severity.CRITICAL, "Potential missing index",
                "A large table is filtered down to a tiny result and no index starts with the filter column.", node.id(),
                Map.of("Table", node.relation(), "Rows inspected", RuleSupport.number(scanned),
                        "Rows returned", RuleSupport.number(node.effectiveActualRows()), "Filter", node.filter()),
                List.of("Validate this index against write overhead and production data distribution.",
                        "Run EXPLAIN ANALYZE again after creating it to confirm the plan change."),
                "CREATE INDEX CONCURRENTLY idx_" + safeTable + "_" + safeColumn + "\nON " + safeTable + " (" + safeColumn + ");");
    }
}
