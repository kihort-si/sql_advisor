package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import com.sql_advisor.engine.RuleSupport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RowEstimationRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        return node.actualRows() != null && RuleSupport.estimationError(node) >= 10;
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        double error = RuleSupport.estimationError(node);
        boolean under = node.effectiveActualRows() > node.plannedRows() * Math.max(1, node.actualLoops());
        Severity severity = error >= 100 ? Severity.CRITICAL : Severity.WARNING;
        return new OptimizationIssue("RowEstimationRule", severity, "Cardinality estimate is off by ~" + RuleSupport.number(error) + "×",
                "PostgreSQL " + (under ? "underestimated" : "overestimated") + " this result. Large errors can lead to an inefficient join or scan strategy.", node.id(),
                Map.of("Expected rows", RuleSupport.number(node.plannedRows()), "Actual rows", RuleSupport.number(node.effectiveActualRows()),
                        "Estimation error", RuleSupport.number(error) + "×"),
                List.of(node.relation() == null ? "Run ANALYZE on the involved tables." : "Run ANALYZE " + node.relation() + ";",
                        "Check correlated predicates and consider extended statistics (CREATE STATISTICS)."),
                node.relation() == null ? null : "ANALYZE " + node.relation().replaceAll("[^a-zA-Z0-9_]", "") + ";");
    }
}
