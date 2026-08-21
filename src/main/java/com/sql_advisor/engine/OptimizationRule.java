package com.sql_advisor.engine;

import com.sql_advisor.domain.AnalysisContext;
import com.sql_advisor.domain.OptimizationIssue;
import com.sql_advisor.domain.PlanNode;

public interface OptimizationRule {
    boolean matches(PlanNode node, AnalysisContext context);
    OptimizationIssue analyze(PlanNode node, AnalysisContext context);
}
