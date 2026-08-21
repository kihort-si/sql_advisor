package com.sql_advisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SqlAdvisorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlAdvisorApplication.class, args);
    }

}
