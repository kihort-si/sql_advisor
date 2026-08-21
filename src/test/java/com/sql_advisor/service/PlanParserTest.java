package com.sql_advisor.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class PlanParserTest {
    private final PlanParser parser = new PlanParser(JsonMapper.builder().build());

    @Test
    void parsesPostgresJsonPlanAndBufferMetrics() {
        String json = """
                [{
                  "Plan": {
                    "Node Type": "Seq Scan",
                    "Relation Name": "users",
                    "Startup Cost": 0.0,
                    "Total Cost": 48182.0,
                    "Plan Rows": 100,
                    "Actual Total Time": 60.4,
                    "Actual Rows": 1,
                    "Actual Loops": 1,
                    "Rows Removed by Filter": 2483411,
                    "Shared Hit Blocks": 14392,
                    "Shared Read Blocks": 6400
                  },
                  "Planning Time": 1.2,
                  "Execution Time": 60.8
                }]
                """;

        var result = parser.parse(json);

        assertThat(result.root().nodeType()).isEqualTo("Seq Scan");
        assertThat(result.root().relation()).isEqualTo("users");
        assertThat(result.root().rowsRemoved()).isEqualTo(2_483_411);
        assertThat(result.root().buffers().sharedRead()).isEqualTo(6_400);
        assertThat(result.executionTimeMs()).isEqualTo(60.8);
    }
}
