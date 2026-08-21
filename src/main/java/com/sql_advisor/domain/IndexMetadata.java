package com.sql_advisor.domain;

import java.util.List;

public record IndexMetadata(String name, List<String> columns, boolean unique, long scans) {
}
