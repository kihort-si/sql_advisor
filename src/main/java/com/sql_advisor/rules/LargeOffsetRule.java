package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class LargeOffsetRule implements OptimizationRule {
    private static final Pattern LARGE_OFFSET = Pattern.compile("\\boffset\\s+([1-9][0-9]{4,})\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        return "Limit".equals(node.nodeType()) && LARGE_OFFSET.matcher(context.sql()).find();
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        var matcher = LARGE_OFFSET.matcher(context.sql());
        matcher.find();
        String offset = matcher.group(1);
        return new OptimizationIssue("LargeOffsetRule", Severity.WARNING, "Large OFFSET pagination",
                "PostgreSQL still has to produce and discard every row before the requested page.", node.id(),
                Map.of("Offset", offset, "Returned rows", String.format("%,.0f", node.plannedRows())),
                List.of("Use keyset pagination with the last seen stable sort key.",
                        "Add a deterministic tie-breaker to the ORDER BY clause."), null);
    }
}
