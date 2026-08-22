package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UnusedIndexRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        if (node.relation() == null || !node.isScan()) return false;
        return context.table(node.relation()).map(table -> table.indexes().stream()
                .anyMatch(index -> !index.unique() && index.scans() == 0)).orElse(false);
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        var names = context.table(node.relation()).orElseThrow().indexes().stream()
                .filter(index -> !index.unique() && index.scans() == 0).map(IndexMetadata::name).toList();
        return new OptimizationIssue("UnusedIndexRule", Severity.INFO, "Index has no recorded scans",
                "pg_stat_user_indexes reports zero scans since the statistics reset. This is an observation, not a drop recommendation.", node.id(),
                Map.of("Table", node.relation(), "Indexes", String.join(", ", names)),
                List.of("Check pg_stat_database.stats_reset and observe a full business cycle.",
                        "Verify replicas, maintenance jobs and constraint requirements before removal."), null);
    }
}
