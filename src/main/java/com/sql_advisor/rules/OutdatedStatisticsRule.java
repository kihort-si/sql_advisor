package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import com.sql_advisor.engine.RuleSupport;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class OutdatedStatisticsRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        if (node.relation() == null) return false;
        return context.table(node.relation()).map(table -> table.estimatedRows() > 100_000
                && (table.lastAnalyze() == null || Duration.between(table.lastAnalyze(), Instant.now()).toDays() > 14)
                && (node.actualRows() == null || RuleSupport.estimationError(node) > 5)).orElse(false);
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        var table = context.table(node.relation()).orElseThrow();
        String last = table.lastAnalyze() == null ? "Never / unavailable" : table.lastAnalyze().toString();
        return new OptimizationIssue("OutdatedStatisticsRule", Severity.WARNING, "Statistics may be outdated",
                "Planner statistics for this large relation are old or unavailable and the row estimate is unreliable.", node.id(),
                Map.of("Table", node.relation(), "Last ANALYZE", last,
                        "Estimated rows", RuleSupport.number(table.estimatedRows())),
                List.of("Check autovacuum/analyze settings and recent bulk data changes."),
                "ANALYZE " + node.relation().replaceAll("[^a-zA-Z0-9_]", "") + ";");
    }
}
