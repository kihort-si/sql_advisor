package com.sql_advisor.engine;

import com.sql_advisor.domain.AnalysisContext;
import com.sql_advisor.domain.OptimizationIssue;
import com.sql_advisor.domain.PlanNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RuleEngine {
    private final List<OptimizationRule> rules;

    public RuleEngine(List<OptimizationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<OptimizationIssue> analyze(PlanNode root, AnalysisContext context) {
        var issues = new ArrayList<OptimizationIssue>();
        walk(root, context, issues);
        return issues.stream()
                .sorted(Comparator.comparing(OptimizationIssue::severity).reversed())
                .toList();
    }

    private void walk(PlanNode node, AnalysisContext context, List<OptimizationIssue> issues) {
        rules.stream().filter(rule -> rule.matches(node, context)).map(rule -> rule.analyze(node, context)).forEach(issues::add);
        node.children().forEach(child -> walk(child, context, issues));
    }
}
