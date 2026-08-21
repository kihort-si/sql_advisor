package com.sql_advisor.engine;

import com.sql_advisor.domain.PlanNode;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RuleSupport {
    private static final Pattern FILTER_COLUMN = Pattern.compile(
            "(?:\\(|\\s|^)(?:[a-zA-Z_][a-zA-Z0-9_]*\\.)?([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:=|<|>|~~|IN\\s*\\()",
            Pattern.CASE_INSENSITIVE);
    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0.##");

    private RuleSupport() {}

    public static Optional<String> filterColumn(PlanNode node) {
        String expression = node.indexCondition() != null ? node.indexCondition() : node.filter();
        if (expression == null) return Optional.empty();
        Matcher matcher = FILTER_COLUMN.matcher(expression.replace("\"", ""));
        return matcher.find() ? Optional.of(matcher.group(1).toLowerCase(Locale.ROOT)) : Optional.empty();
    }

    public static String number(double number) {
        return NUMBER.format(number);
    }

    public static double estimationError(PlanNode node) {
        if (node.actualRows() == null) return 1;
        double actual = Math.max(1, node.effectiveActualRows());
        double planned = Math.max(1, node.plannedRows() * Math.max(1, node.actualLoops()));
        return Math.max(actual, planned) / Math.min(actual, planned);
    }
}
