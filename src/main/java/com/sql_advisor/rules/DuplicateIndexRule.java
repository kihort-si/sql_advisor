package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import com.sql_advisor.engine.OptimizationRule;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DuplicateIndexRule implements OptimizationRule {
    @Override
    public boolean matches(PlanNode node, AnalysisContext context) {
        if (node.relation() == null || !node.isScan()) return false;
        return context.table(node.relation()).map(table -> {
            Set<List<String>> seen = new HashSet<>();
            return table.indexes().stream().map(IndexMetadata::columns).anyMatch(columns -> !seen.add(columns));
        }).orElse(false);
    }

    @Override
    public OptimizationIssue analyze(PlanNode node, AnalysisContext context) {
        var table = context.table(node.relation()).orElseThrow();
        Map<List<String>, List<String>> grouped = new LinkedHashMap<>();
        table.indexes().forEach(index -> grouped.computeIfAbsent(index.columns(), ignored -> new ArrayList<>()).add(index.name()));
        var duplicate = grouped.entrySet().stream().filter(entry -> entry.getValue().size() > 1).findFirst().orElseThrow();
        return new OptimizationIssue("DuplicateIndexRule", Severity.INFO, "Overlapping indexes detected",
                "Multiple indexes share the same complete column sequence, increasing write and vacuum work.", node.id(),
                Map.of("Columns", String.join(", ", duplicate.getKey()), "Indexes", String.join(", ", duplicate.getValue())),
                List.of("Compare constraints, predicates, included columns and usage before removing anything.",
                        "Use DROP INDEX CONCURRENTLY only after an observation period."), null);
    }
}
