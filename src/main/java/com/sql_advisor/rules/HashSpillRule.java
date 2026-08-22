package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import com.sql_advisor.engine.RuleSupport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HashSpillRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        return node.nodeType() != null && node.nodeType().contains("Hash") && node.buffers().tempWritten() > 0;
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        return new OptimizationIssue("HashSpillRule", Severity.WARNING, "Hash operation spilled to disk",
                "The hash table exceeded available memory and used temporary PostgreSQL blocks.", node.id(),
                Map.of("Temporary writes", RuleSupport.number(node.buffers().tempWritten()),
                        "Temporary reads", RuleSupport.number(node.buffers().tempRead())),
                List.of("Fix cardinality estimation errors before tuning memory.",
                        "Reduce the build-side result or review work_mem for this workload."), null);
    }
}
