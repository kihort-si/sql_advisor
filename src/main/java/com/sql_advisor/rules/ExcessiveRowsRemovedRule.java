package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import com.sql_advisor.engine.RuleSupport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ExcessiveRowsRemovedRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        double returned = node.actualRows() == null ? 0 : node.effectiveActualRows();
        return node.rowsRemoved() > 10_000 && node.rowsRemoved() / Math.max(1, returned + node.rowsRemoved()) > .9;
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        return new OptimizationIssue("ExcessiveRowsRemovedRule", Severity.WARNING, "Most scanned rows are discarded",
                "The filter removes more than 90% of rows inspected by this node.", node.id(),
                Map.of("Rows removed", RuleSupport.number(node.rowsRemoved()), "Rows returned", RuleSupport.number(node.effectiveActualRows())),
                List.of("Push selective predicates earlier where possible.", "Check whether the filter can be supported by an index."), null);
    }
}
