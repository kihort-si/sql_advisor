package com.sql_advisor.service;

import com.sql_advisor.domain.BufferStats;
import com.sql_advisor.domain.PlanNode;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PlanParser {
    private final JsonMapper objectMapper;

    public PlanParser(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedPlan parse(String json) {
        try {
            JsonNode envelope = objectMapper.readTree(json).get(0);
            var sequence = new AtomicInteger();
            return new ParsedPlan(
                    parseNode(envelope.path("Plan"), sequence),
                    number(envelope, "Planning Time", 0),
                    envelope.has("Execution Time") ? envelope.get("Execution Time").asDouble() : null);
        } catch (Exception exception) {
            throw new IllegalArgumentException("PostgreSQL returned an unreadable JSON plan", exception);
        }
    }

    private PlanNode parseNode(JsonNode json, AtomicInteger sequence) {
        List<PlanNode> children = new ArrayList<>();
        json.path("Plans").forEach(child -> children.add(parseNode(child, sequence)));
        return new PlanNode(
                "plan-" + sequence.incrementAndGet(),
                text(json, "Node Type"),
                text(json, "Relation Name"),
                text(json, "Alias"),
                text(json, "Index Name"),
                text(json, "Filter"),
                text(json, "Index Cond"),
                text(json, "Join Filter"),
                number(json, "Startup Cost", 0),
                number(json, "Total Cost", 0),
                number(json, "Plan Rows", 0),
                nullableNumber(json, "Actual Startup Time"),
                nullableNumber(json, "Actual Total Time"),
                nullableNumber(json, "Actual Rows"),
                number(json, "Actual Loops", 1),
                number(json, "Rows Removed by Filter", 0) + number(json, "Rows Removed by Join Filter", 0),
                new BufferStats(
                        longNumber(json, "Shared Hit Blocks"),
                        longNumber(json, "Shared Read Blocks"),
                        longNumber(json, "Shared Dirtied Blocks"),
                        longNumber(json, "Shared Written Blocks"),
                        longNumber(json, "Temp Read Blocks"),
                        longNumber(json, "Temp Written Blocks")),
                text(json, "Sort Method"),
                json.has("Sort Space Used") ? json.get("Sort Space Used").asLong() : null,
                text(json, "Sort Space Type"),
                List.copyOf(children));
    }

    private static String text(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : null;
    }

    private static double number(JsonNode node, String field, double fallback) {
        return node.has(field) ? node.get(field).asDouble() : fallback;
    }

    private static Double nullableNumber(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asDouble() : null;
    }

    private static long longNumber(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asLong() : 0;
    }

    public record ParsedPlan(PlanNode root, double planningTimeMs, Double executionTimeMs) {}
}
