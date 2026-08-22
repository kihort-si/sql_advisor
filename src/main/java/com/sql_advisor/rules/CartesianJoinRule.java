package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CartesianJoinRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        return "Nested Loop".equals(node.nodeType()) && node.joinFilter() == null && node.children().size() == 2
                && node.children().get(0).indexCondition() == null && node.children().get(1).indexCondition() == null
                && node.plannedRows() > 100_000;
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        return new OptimizationIssue("CartesianJoinRule", Severity.CRITICAL, "Possible Cartesian join",
                "This join has no visible join condition and produces a large intermediate result.", node.id(),
                Map.of("Planned rows", String.format("%,.0f", node.plannedRows())),
                List.of("Verify that a JOIN ... ON predicate was not omitted.", "If intentional, constrain one side before the cross join."), null);
    }
}
