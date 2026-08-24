package com.sql_advisor.service;

import com.sql_advisor.api.*;
import com.sql_advisor.domain.*;
import com.sql_advisor.engine.RuleEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Instant;
import java.util.*;

@Service
public class AdvisorService {
    private final QuerySafetyService querySafetyService;
    private final PlanParser planParser;
    private final CatalogInspector catalogInspector;
    private final RuleEngine ruleEngine;
    private final int timeoutSeconds;

    public AdvisorService(QuerySafetyService querySafetyService,
                          PlanParser planParser,
                          CatalogInspector catalogInspector,
                          RuleEngine ruleEngine,
                          @Value("${advisor.statement-timeout-seconds:30}") int timeoutSeconds) {
        this.querySafetyService = querySafetyService;
        this.planParser = planParser;
        this.catalogInspector = catalogInspector;
        this.ruleEngine = ruleEngine;
        this.timeoutSeconds = timeoutSeconds;
    }

    public ConnectionTestResponse testConnection(ConnectionDetails details) {
        long started = System.nanoTime();
        try (Connection connection = open(details);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT version()")) {
            result.next();
            return new ConnectionTestResponse(true, compactVersion(result.getString(1)), elapsedMs(started));
        } catch (SQLException exception) {
            throw databaseError(exception);
        }
    }

    public AnalysisResponse analyze(AnalysisRequest request) {
        querySafetyService.requireReadOnlyQuery(request.sql());
        try (Connection connection = open(request.connection())) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            try {
                configureTransaction(connection);
                String version = databaseVersion(connection);
                String planJson = explain(connection, request.sql(), request.mode());
                PlanParser.ParsedPlan parsed = planParser.parse(planJson);
                Map<String, TableMetadata> tables = safeCatalogInspection(connection, parsed.root());
                var context = new AnalysisContext(request.sql(), request.mode(), tables,
                        parsed.executionTimeMs() == null ? 0 : parsed.executionTimeMs());
                List<OptimizationIssue> issues = ruleEngine.analyze(parsed.root(), context);
                Score score = calculateScore(parsed.root(), issues);
                connection.rollback();
                return new AnalysisResponse(
                        version, request.mode(), score.value(), score.items(), parsed.planningTimeMs(), parsed.executionTimeMs(),
                        parsed.root().totalCost(), parsed.root().actualRows() == null ? parsed.root().plannedRows() : parsed.root().effectiveActualRows(),
                        rowsInspected(parsed.root()), parsed.root().buffers(), parsed.root(), issues,
                        explainPlainly(parsed.root(), parsed.executionTimeMs()), Instant.now());
            } catch (Exception exception) {
                try { connection.rollback(); } catch (SQLException ignored) {}
                if (exception instanceof IllegalArgumentException illegalArgumentException) throw illegalArgumentException;
                if (exception instanceof SQLException sqlException) throw databaseError(sqlException);
                throw new IllegalStateException("Analysis failed", exception);
            }
        } catch (SQLException exception) {
            throw databaseError(exception);
        }
    }

    private Connection open(ConnectionDetails details) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", details.username());
        properties.setProperty("password", details.password() == null ? "" : details.password());
        properties.setProperty("connectTimeout", "8");
        properties.setProperty("socketTimeout", Integer.toString(timeoutSeconds + 5));
        return DriverManager.getConnection(details.jdbcUrl(), properties);
    }

    private void configureTransaction(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET TRANSACTION READ ONLY");
            statement.execute("SET LOCAL statement_timeout = '" + timeoutSeconds + "s'");
            statement.execute("SET LOCAL lock_timeout = '3s'");
        }
    }

    private String databaseVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SHOW server_version")) {
            rs.next();
            return "PostgreSQL " + rs.getString(1);
        }
    }

    private String explain(Connection connection, String sql, AnalysisMode mode) throws SQLException {
        String options = mode == AnalysisMode.ACTUAL
                ? "ANALYZE true, BUFFERS true, FORMAT JSON, SUMMARY true, TIMING true, VERBOSE true"
                : "ANALYZE false, FORMAT JSON, VERBOSE true";
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("EXPLAIN (" + options + ") " + sql)) {
            if (!rs.next()) throw new IllegalArgumentException("PostgreSQL did not return an execution plan");
            return rs.getString(1);
        }
    }

    private Map<String, TableMetadata> safeCatalogInspection(Connection connection, PlanNode root) {
        try {
            return catalogInspector.inspect(connection, root);
        } catch (SQLException ignored) {
            return Map.of();
        }
    }

    private Score calculateScore(PlanNode root, List<OptimizationIssue> issues) {
        List<ScoreItem> items = new ArrayList<>();
        int score = 100;
        for (OptimizationIssue issue : issues) {
            int penalty = issue.severity().scorePenalty();
            score -= penalty;
            items.add(new ScoreItem(issue.title(), -penalty, issue.severity().name().toLowerCase(Locale.ROOT)));
        }
        if (containsNodeType(root, "Index Scan") || containsNodeType(root, "Index Only Scan")) {
            score += 4;
            items.add(new ScoreItem("Useful index access", 4, "positive"));
        }
        return new Score(Math.max(0, Math.min(100, score)), List.copyOf(items));
    }

    private boolean containsNodeType(PlanNode node, String type) {
        return type.equals(node.nodeType()) || node.children().stream().anyMatch(child -> containsNodeType(child, type));
    }

    private double rowsInspected(PlanNode node) {
        if (node.children().isEmpty()) {
            double rows = node.actualRows() == null ? node.plannedRows() : node.effectiveActualRows();
            return rows + node.rowsRemoved() * Math.max(1, node.actualLoops());
        }
        return node.children().stream().mapToDouble(this::rowsInspected).sum();
    }

    private List<String> explainPlainly(PlanNode root, Double executionTimeMs) {
        List<String> result = new ArrayList<>();
        String duration = executionTimeMs == null ? "estimated cost " + format(root.totalCost()) : format(executionTimeMs) + " ms";
        result.add("PostgreSQL starts with “" + root.nodeType() + "”; the complete plan has " + countNodes(root) + " operations and " + duration + ".");
        describeSlowNodes(root, Math.max(1, root.effectiveTimeMs()), result);
        if (root.buffers().totalShared() > 0) {
            double hitRate = 100.0 * root.buffers().sharedHit() / Math.max(1, root.buffers().totalShared());
            result.add("The query touched " + format(root.buffers().totalShared()) + " shared buffers; " + format(hitRate) + "% were cache hits.");
        }
        return result.stream().distinct().limit(5).toList();
    }

    private void describeSlowNodes(PlanNode node, double total, List<String> result) {
        if (node.actualTotalTime() != null && node.effectiveTimeMs() / total >= .2 && node.relation() != null) {
            result.add(node.nodeType() + " on “" + node.relation() + "” accounts for roughly "
                    + format(100 * node.effectiveTimeMs() / total) + "% of measured plan time.");
        }
        node.children().forEach(child -> describeSlowNodes(child, total, result));
    }

    private int countNodes(PlanNode node) {
        return 1 + node.children().stream().mapToInt(this::countNodes).sum();
    }

    private static String compactVersion(String version) {
        int comma = version.indexOf(',');
        return comma > 0 ? version.substring(0, comma) : version;
    }

    private static int elapsedMs(long started) {
        return (int) ((System.nanoTime() - started) / 1_000_000);
    }

    private static String format(double value) {
        return value >= 1000 ? String.format("%,.0f", value) : String.format("%.1f", value);
    }

    private static IllegalArgumentException databaseError(SQLException exception) {
        String message = exception.getMessage() == null ? "Database connection failed" : exception.getMessage();
        return new IllegalArgumentException("PostgreSQL: " + message, exception);
    }

    private record Score(int value, List<ScoreItem> items) {}
}
