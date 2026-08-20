package com.sql_advisor;

import org.springframework.boot.SpringApplication;

public class TestSqlAdvisorApplication {

    public static void main(String[] args) {
        SpringApplication.from(SqlAdvisorApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
