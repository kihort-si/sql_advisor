package com.sql_advisor.service;

import com.sql_advisor.domain.IndexMetadata;
import com.sql_advisor.domain.PlanNode;
import com.sql_advisor.domain.TableMetadata;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

@Component
public class CatalogInspector {
    public Map<String, TableMetadata> inspect(Connection connection, PlanNode root) throws SQLException {
        Set<String> relations = new LinkedHashSet<>();
        collectRelations(root, relations);
        Map<String, TableMetadata> result = new HashMap<>();
        for (String relation : relations) result.put(relation.toLowerCase(Locale.ROOT), inspectTable(connection, relation));
        return result;
    }

    private TableMetadata inspectTable(Connection connection, String relation) throws SQLException {
        String sql = """
                SELECT n.nspname, c.relname, GREATEST(c.reltuples, 0)::bigint,
                       pg_total_relation_size(c.oid), s.last_analyze
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                LEFT JOIN pg_stat_user_tables s ON s.relid = c.oid
                WHERE c.relname = ? AND c.relkind IN ('r', 'p')
                ORDER BY CASE WHEN n.nspname = current_schema() THEN 0 ELSE 1 END
                LIMIT 1
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, relation);
            try (var rs = statement.executeQuery()) {
                if (!rs.next()) return new TableMetadata(null, relation, 0, 0, null, List.of());
                String schema = rs.getString(1);
                String table = rs.getString(2);
                long rows = rs.getLong(3);
                long bytes = rs.getLong(4);
                var timestamp = rs.getTimestamp(5);
                return new TableMetadata(schema, table, rows, bytes,
                        timestamp == null ? null : timestamp.toInstant(), indexes(connection, schema, table));
            }
        }
    }

    private List<IndexMetadata> indexes(Connection connection, String schema, String table) throws SQLException {
        String sql = """
                SELECT i.relname, ix.indisunique, COALESCE(si.idx_scan, 0),
                       array_agg(a.attname ORDER BY keys.ordinality) FILTER (WHERE a.attname IS NOT NULL)
                FROM pg_class t
                JOIN pg_namespace n ON n.oid = t.relnamespace
                JOIN pg_index ix ON ix.indrelid = t.oid
                JOIN pg_class i ON i.oid = ix.indexrelid
                LEFT JOIN pg_stat_user_indexes si ON si.indexrelid = i.oid
                LEFT JOIN LATERAL unnest(ix.indkey) WITH ORDINALITY keys(attnum, ordinality) ON true
                LEFT JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = keys.attnum
                WHERE n.nspname = ? AND t.relname = ?
                GROUP BY i.relname, ix.indisunique, si.idx_scan
                """;
        List<IndexMetadata> result = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (var rs = statement.executeQuery()) {
                while (rs.next()) {
                    var array = rs.getArray(4);
                    List<String> columns = array == null ? List.of() : Arrays.asList((String[]) array.getArray());
                    result.add(new IndexMetadata(rs.getString(1), columns, rs.getBoolean(2), rs.getLong(3)));
                }
            }
        }
        return List.copyOf(result);
    }

    private void collectRelations(PlanNode node, Set<String> relations) {
        if (node.relation() != null) relations.add(node.relation());
        node.children().forEach(child -> collectRelations(child, relations));
    }
}
