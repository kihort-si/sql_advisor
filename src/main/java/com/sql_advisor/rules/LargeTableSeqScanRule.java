package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import com.sql_advisor.engine.RuleSupport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LargeTableSeqScanRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        if (!"Seq Scan".equals(node.nodeType()) || node.filter() == null) return false;
        return context.table(node.relation()).map(t -> {
            double returned = node.actualRows() == null ? node.plannedRows() : node.effectiveActualRows();
            double inspected = node.actualRows() == null ? t.estimatedRows() : returned + node.rowsRemoved();
            return t.estimatedRows() > 100_000 && inspected > 50_000 && returned / Math.max(1, inspected) < .15;
        }).orElse(false);
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        var table = context.table(node.relation()).orElseThrow();
        return new OptimizationIssue("LargeTableSeqScanRule", Severity.WARNING, "Selective sequential scan",
                "The sequential scan is not inherently bad, but this one reads a large relation while returning a small fraction of it.", node.id(),
                Map.of("Table rows", RuleSupport.number(table.estimatedRows()), "Rows returned", RuleSupport.number(node.effectiveActualRows()),
                        "Cost", RuleSupport.number(node.totalCost())),
                List.of("Check whether the filter is selective in production.", "Consider a partial or composite index if this access pattern is common."), null);
    }
}
