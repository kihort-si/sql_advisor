package com.sql_advisor.service;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Service;

@Service
public class QuerySafetyService {
    public void requireReadOnlyQuery(String sql) {
        if (sql == null || sql.isBlank()) throw new IllegalArgumentException("SQL query is required");
        try {
            var statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                throw new IllegalArgumentException("Only a single SELECT or WITH query can be analyzed");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("SQL could not be parsed: " + exception.getMessage(), exception);
        }
    }
}
