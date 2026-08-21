package com.sql_advisor.api;

public record ConnectionTestResponse(boolean connected, String databaseVersion, int latencyMs) {
}
