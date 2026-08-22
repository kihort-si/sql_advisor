package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SortSpillRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        return "Sort".equals(node.nodeType()) && (node.buffers().tempWritten() > 0
                || "Disk".equalsIgnoreCase(node.sortSpaceType())
                || (node.sortMethod() != null && node.sortMethod().toLowerCase().contains("external")));
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        long kb = node.sortSpaceUsedKb() == null ? node.buffers().tempWritten() * 8 : node.sortSpaceUsedKb();
        return new OptimizationIssue("SortSpillRule", Severity.WARNING, "Sort spilled to disk",
                "The sort used temporary disk storage, which is much slower than an in-memory sort.", node.id(),
                Map.of("Sort method", String.valueOf(node.sortMethod()), "Temporary storage", String.format("%.1f MB", kb / 1024.0)),
                List.of("Check whether an index can provide the requested order.",
                        "Reduce the intermediate result set before sorting.",
                        "Review work_mem for this session; avoid raising it globally without capacity analysis."), null);
    }
}
