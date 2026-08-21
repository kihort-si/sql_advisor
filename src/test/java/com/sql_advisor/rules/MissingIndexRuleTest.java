package com.sql_advisor.rules;

import com.sql_advisor.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MissingIndexRuleTest {
    private final MissingIndexRule rule = new MissingIndexRule();

    @Test
    void recommendsIndexOnlyForLargeSelectiveUnindexedScan() {
        var node = new PlanNode("plan-1", "Seq Scan", "users", "u", null,
                "(users.email = 'someone@example.com'::text)", null, null,
                0, 48_182, 3, 0.01, 60.4, 3.0, 1, 2_399_997,
                new BufferStats(100, 14_392, 0, 0, 0, 0), null, null, null, List.of());
        var users = new TableMetadata("public", "users", 2_400_000, 280_000_000,
                Instant.now(), List.of(new IndexMetadata("users_pkey", List.of("id"), true, 50)));
        var context = new AnalysisContext("select * from users where email = ?", AnalysisMode.ACTUAL,
                Map.of("users", users), 60.4);

        assertThat(rule.matches(node, context)).isTrue();
        assertThat(rule.analyze(node, context).suggestedSql()).contains("idx_users_email", "ON users (email)");
    }

    @Test
    void doesNotFlagScanThatReturnsTheWholeTable() {
        var node = new PlanNode("plan-1", "Seq Scan", "users", "u", null,
                null, null, null, 0, 48_182, 2_400_000, null, null, null, 1, 0,
                BufferStats.EMPTY, null, null, null, List.of());
        var context = new AnalysisContext("select * from users", AnalysisMode.ESTIMATED, Map.of(), 0);

        assertThat(rule.matches(node, context)).isFalse();
    }
}
