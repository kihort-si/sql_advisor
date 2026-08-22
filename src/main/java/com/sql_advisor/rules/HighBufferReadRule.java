package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import com.sql_advisor.engine.RuleSupport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HighBufferReadRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        long total = node.buffers().totalShared();
        // Parent buffer counters are cumulative, so reporting them as well as their
        // leaf scan would duplicate the same physical I/O in the recommendations.
        return node.children().isEmpty() && node.buffers().sharedRead() > 1_000
                && total > 0 && node.buffers().sharedRead() / (double) total > .2;
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        double hitRate = 100.0 * node.buffers().sharedHit() / Math.max(1, node.buffers().totalShared());
        return new OptimizationIssue("HighBufferReadRule", Severity.WARNING, "High physical I/O",
                "This operation fetched a significant number of PostgreSQL pages from storage instead of shared buffers.", node.id(),
                Map.of("Disk reads", RuleSupport.number(node.buffers().sharedRead()), "Cache hits", RuleSupport.number(node.buffers().sharedHit()),
                        "Buffer hit rate", RuleSupport.number(hitRate) + "%"),
                List.of("Confirm with multiple warm and cold runs.", "Reduce pages touched with a selective index or narrower result set."), null);
    }
}
