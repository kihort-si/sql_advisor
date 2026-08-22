package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import com.sql_advisor.engine.RuleSupport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NestedLoopRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        if (!"Nested Loop".equals(node.nodeType()) || node.children().size() < 2) return false;
        PlanNode inner = node.children().get(1);
        return inner.actualLoops() > 1_000 && (inner.effectiveTimeMs() > 50 || RuleSupport.estimationError(node) > 10);
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        PlanNode inner = node.children().get(1);
        return new OptimizationIssue("NestedLoopRule", Severity.WARNING, "Expensive nested loop",
                "The inner side is executed many times. A nested loop is excellent for small indexed lookups, but risky at this scale.", node.id(),
                Map.of("Inner loops", RuleSupport.number(inner.actualLoops()), "Node time", RuleSupport.number(node.effectiveTimeMs()) + " ms"),
                List.of("Check cardinality estimates feeding this join.", "Ensure the inner join key has a useful index.",
                        "Do not force a join type globally; fix statistics and access paths first."), null);
    }
}
